package in.jecrcu.placement.service.erp;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import in.jecrcu.placement.dto.erp.*;
import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.entity.enums.Role;
import in.jecrcu.placement.entity.erp.*;
import in.jecrcu.placement.repository.UserRepository;
import in.jecrcu.placement.repository.erp.*;
import in.jecrcu.placement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicCourseworkServiceImpl implements AcademicCourseworkService {

    private final AcademicAssignmentRepository assignmentRepository;
    private final AcademicSubmissionRepository submissionRepository;
    private final AcademicAssessmentRepository assessmentRepository;
    private final AcademicMarkRepository markRepository;
    private final AcademicMarkAuditLogRepository markAuditLogRepository;
    private final SubjectRepository subjectRepository;
    private final SectionRepository sectionRepository;
    private final SemesterRepository semesterRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentSubjectEnrollmentRepository studentSubjectEnrollmentRepository;
    private final FacultyAssignmentRepository facultyAssignmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // -------------------------------------------------------------------------
    // Assignments
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AssignmentDto createAssignment(AssignmentCreateRequestDto request, Long facultyUserId) {
        User faculty = userRepository.findById(facultyUserId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + facultyUserId));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + request.getSubjectId()));

        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + request.getSectionId()));

        // Authorization check: verify faculty assignment unless admin
        verifyFacultyAssignment(faculty, subject.getId(), section.getId());

        AcademicAssignment assignment = AcademicAssignment.builder()
                .subject(subject)
                .section(section)
                .facultyUser(faculty)
                .title(request.getTitle())
                .description(request.getDescription())
                .attachmentUrl(request.getAttachmentUrl())
                .dueDate(request.getDueDate())
                .maxMarks(request.getMaxMarks() != null ? request.getMaxMarks() : BigDecimal.valueOf(100.00))
                .allowLateSubmission(request.getAllowLateSubmission() != null ? request.getAllowLateSubmission() : true)
                .latePenaltyPercentage(request.getLatePenaltyPercentage() != null ? request.getLatePenaltyPercentage() : BigDecimal.valueOf(10.00))
                .status("PUBLISHED")
                .build();

        assignment = assignmentRepository.save(assignment);

        // Notify enrolled students in section
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findBySectionId(section.getId());
        String dueStr = assignment.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
        for (StudentEnrollment se : enrollments) {
            notificationService.sendNotification(
                    se.getUser(),
                    "New Assignment: " + assignment.getTitle(),
                    String.format("Assignment published for %s (%s). Due date: %s.", subject.getName(), subject.getCode(), dueStr),
                    "/erp",
                    "ASSIGNMENT_NEW"
            );
        }

        return mapToAssignmentDto(assignment, null);
    }

    @Override
    public List<AssignmentDto> getAssignmentsForSection(Long sectionId, Long currentUserId) {
        List<AcademicAssignment> assignments = assignmentRepository.findBySectionIdOrderByDueDateDesc(sectionId);
        Long enrollmentId = studentEnrollmentRepository.findByUserId(currentUserId)
                .map(StudentEnrollment::getId).orElse(null);

        return assignments.stream()
                .map(a -> mapToAssignmentDto(a, enrollmentId))
                .collect(Collectors.toList());
    }

    @Override
    public List<AssignmentDto> getAssignmentsForFaculty(Long facultyUserId) {
        return assignmentRepository.findByFacultyUserIdOrderByCreatedAtDesc(facultyUserId).stream()
                .map(a -> mapToAssignmentDto(a, null))
                .collect(Collectors.toList());
    }

    @Override
    public AssignmentDto getAssignmentDetail(Long assignmentId, Long currentUserId) {
        AcademicAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        Long enrollmentId = studentEnrollmentRepository.findByUserId(currentUserId)
                .map(StudentEnrollment::getId).orElse(null);

        return mapToAssignmentDto(assignment, enrollmentId);
    }

    // -------------------------------------------------------------------------
    // Submissions & Grading
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SubmissionDto submitAssignment(Long assignmentId, SubmissionSubmitRequestDto request, Long studentUserId) {
        AcademicAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        StudentEnrollment enrollment = studentEnrollmentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student academic enrollment not found for user: " + studentUserId));

        if (!enrollment.getSection().getId().equals(assignment.getSection().getId())) {
            throw new AccessDeniedException("Student is not enrolled in the section for this assignment.");
        }

        if ("CLOSED".equalsIgnoreCase(assignment.getStatus())) {
            throw new IllegalStateException("This assignment is closed and no longer accepting submissions.");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isLate = now.isAfter(assignment.getDueDate());

        if (isLate && !Boolean.TRUE.equals(assignment.getAllowLateSubmission())) {
            throw new IllegalStateException("Late submissions are not permitted for this assignment.");
        }

        AcademicSubmission submission = submissionRepository
                .findByAssignmentIdAndStudentEnrollmentId(assignment.getId(), enrollment.getId())
                .orElseGet(() -> AcademicSubmission.builder()
                        .assignment(assignment)
                        .studentEnrollment(enrollment)
                        .build());

        submission.setSubmissionText(request.getSubmissionText());
        submission.setFileUrl(request.getFileUrl());
        submission.setSubmittedAt(now);
        submission.setIsLate(isLate);
        submission.setStatus("SUBMITTED");

        submission = submissionRepository.save(submission);

        // Notify faculty instructor
        notificationService.sendNotification(
                assignment.getFacultyUser(),
                "Assignment Submission: " + enrollment.getUser().getFullName(),
                String.format("Student %s submitted assignment '%s'%s.",
                        enrollment.getUser().getFullName(), assignment.getTitle(), isLate ? " (LATE)" : ""),
                "/erp",
                "ASSIGNMENT_SUBMITTED"
        );

        return mapToSubmissionDto(submission);
    }

    @Override
    public List<SubmissionDto> getSubmissionsForAssignment(Long assignmentId, Long facultyUserId) {
        AcademicAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        User user = userRepository.findById(facultyUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + facultyUserId));

        verifyFacultyAssignment(user, assignment.getSubject().getId(), assignment.getSection().getId());

        return submissionRepository.findByAssignmentIdOrderBySubmittedAtAsc(assignmentId).stream()
                .map(this::mapToSubmissionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubmissionDto gradeSubmission(Long submissionId, SubmissionGradeRequestDto request, Long facultyUserId) {
        AcademicSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + submissionId));

        AcademicAssignment assignment = submission.getAssignment();
        User faculty = userRepository.findById(facultyUserId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + facultyUserId));

        verifyFacultyAssignment(faculty, assignment.getSubject().getId(), assignment.getSection().getId());

        BigDecimal max = assignment.getMaxMarks();
        BigDecimal marks = request.getMarksObtained();

        if (marks.compareTo(BigDecimal.ZERO) < 0 || marks.compareTo(max) > 0) {
            throw new IllegalArgumentException(String.format("Marks obtained (%s) must be between 0.00 and maximum marks (%s)", marks, max));
        }

        submission.setMarksObtained(marks);
        submission.setFeedback(request.getFeedback());
        submission.setGradedByUser(faculty);
        submission.setGradedAt(LocalDateTime.now());
        submission.setStatus("GRADED");

        submission = submissionRepository.save(submission);

        // Notify student of grading feedback
        notificationService.sendNotification(
                submission.getStudentEnrollment().getUser(),
                "Assignment Graded: " + assignment.getTitle(),
                String.format("Your submission for '%s' has been graded: %s / %s marks.", assignment.getTitle(), marks, max),
                "/erp",
                "ASSIGNMENT_GRADED"
        );

        return mapToSubmissionDto(submission);
    }

    @Override
    public List<SubmissionDto> getStudentSubmissions(Long studentUserId) {
        StudentEnrollment enrollment = studentEnrollmentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student enrollment not found"));

        return submissionRepository.findByStudentEnrollmentIdOrderBySubmittedAtDesc(enrollment.getId()).stream()
                .map(this::mapToSubmissionDto)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Assessments & Marks
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AssessmentDto createAssessment(AssessmentCreateRequestDto request, Long facultyUserId) {
        User faculty = userRepository.findById(facultyUserId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + facultyUserId));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + request.getSubjectId()));

        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new IllegalArgumentException("Semester not found: " + request.getSemesterId()));

        Section section = request.getSectionId() != null
                ? sectionRepository.findById(request.getSectionId()).orElse(null)
                : null;

        if (section != null) {
            verifyFacultyAssignment(faculty, subject.getId(), section.getId());
        } else if (faculty.getRole() != Role.ROLE_ADMIN) {
            // Verify faculty teaches this subject in at least one section
            boolean teaches = facultyAssignmentRepository.findByFacultyUserId(faculty.getId()).stream()
                    .anyMatch(fa -> fa.getSubject().getId().equals(subject.getId()));
            if (!teaches) {
                throw new AccessDeniedException("Faculty is not authorized to create assessments for this subject.");
            }
        }

        AcademicAssessment assessment = AcademicAssessment.builder()
                .subject(subject)
                .semester(semester)
                .section(section)
                .facultyUser(faculty)
                .name(request.getName())
                .assessmentType(request.getAssessmentType())
                .weightagePercent(request.getWeightagePercent() != null ? request.getWeightagePercent() : BigDecimal.valueOf(20.00))
                .maxMarks(request.getMaxMarks())
                .passingMarks(request.getPassingMarks())
                .assessmentDate(request.getAssessmentDate())
                .isPublished(false)
                .build();

        assessment = assessmentRepository.save(assessment);
        return mapToAssessmentDto(assessment);
    }

    @Override
    public List<AssessmentDto> getAssessmentsForFaculty(Long facultyUserId) {
        return assessmentRepository.findByFacultyUserIdOrderByCreatedAtDesc(facultyUserId).stream()
                .map(this::mapToAssessmentDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssessmentDto> getAssessmentsForSemester(Long semesterId, Long currentUserId) {
        User user = userRepository.findById(currentUserId).orElse(null);
        boolean isStudent = (user != null && user.getRole() == Role.ROLE_STUDENT);

        List<AcademicAssessment> list = isStudent
                ? assessmentRepository.findBySemesterIdAndIsPublishedTrueOrderByAssessmentDateDesc(semesterId)
                : assessmentRepository.findBySemesterIdOrderByAssessmentDateDesc(semesterId);

        return list.stream()
                .map(this::mapToAssessmentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AssessmentDto publishAssessmentResults(Long assessmentId, Long facultyUserId) {
        AcademicAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));

        User user = userRepository.findById(facultyUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + facultyUserId));

        if (user.getRole() != Role.ROLE_ADMIN && !assessment.getFacultyUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the assessment examiner or administrator can release results.");
        }

        assessment.setIsPublished(true);
        assessment.setPublishedAt(LocalDateTime.now());
        assessment = assessmentRepository.save(assessment);

        // Notify enrolled students who have marks in this assessment
        List<AcademicMark> marks = markRepository.findByAssessmentId(assessmentId);
        for (AcademicMark m : marks) {
            notificationService.sendNotification(
                    m.getStudentEnrollment().getUser(),
                    "Assessment Results Released: " + assessment.getName(),
                    String.format("Results for '%s' (%s) have been officially published. Your score: %s / %s.",
                            assessment.getName(), assessment.getSubject().getName(), m.getMarksObtained(), assessment.getMaxMarks()),
                    "/erp",
                    "RESULTS_PUBLISHED"
            );
        }

        return mapToAssessmentDto(assessment);
    }

    @Override
    @Transactional
    public StudentMarkDto recordStudentMark(RecordMarkRequestDto request, Long facultyUserId) {
        AcademicAssessment assessment = assessmentRepository.findById(request.getAssessmentId())
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + request.getAssessmentId()));

        User faculty = userRepository.findById(facultyUserId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + facultyUserId));

        StudentEnrollment enrollment = studentEnrollmentRepository.findById(request.getStudentEnrollmentId())
                .orElseThrow(() -> new IllegalArgumentException("Student enrollment not found: " + request.getStudentEnrollmentId()));

        BigDecimal max = assessment.getMaxMarks();
        BigDecimal marks = Boolean.TRUE.equals(request.getIsAbsent()) ? BigDecimal.ZERO : request.getMarksObtained();

        if (marks.compareTo(BigDecimal.ZERO) < 0 || marks.compareTo(max) > 0) {
            throw new IllegalArgumentException(String.format("Marks obtained (%s) must be between 0.00 and max marks (%s)", marks, max));
        }

        AcademicMark mark = markRepository.findByAssessmentIdAndStudentEnrollmentId(assessment.getId(), enrollment.getId())
                .orElseGet(() -> AcademicMark.builder()
                        .assessment(assessment)
                        .studentEnrollment(enrollment)
                        .build());

        mark.setMarksObtained(marks);
        mark.setIsAbsent(Boolean.TRUE.equals(request.getIsAbsent()));
        mark.setRemarks(request.getRemarks());
        mark.setGradedByUser(faculty);
        mark.setGradedAt(LocalDateTime.now());

        mark = markRepository.save(mark);
        return mapToMarkDto(mark);
    }

    @Override
    @Transactional
    public int batchRecordMarks(Long assessmentId, List<RecordMarkRequestDto> marks, Long facultyUserId) {
        int count = 0;
        for (RecordMarkRequestDto req : marks) {
            req.setAssessmentId(assessmentId);
            recordStudentMark(req, facultyUserId);
            count++;
        }
        return count;
    }

    @Override
    public List<StudentMarkDto> getMarksForAssessment(Long assessmentId, Long facultyUserId) {
        return markRepository.findByAssessmentId(assessmentId).stream()
                .map(this::mapToMarkDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudentMarkDto correctStudentMark(MarkCorrectionRequestDto request, Long facultyUserId) {
        AcademicMark mark = markRepository.findById(request.getMarkId())
                .orElseThrow(() -> new IllegalArgumentException("Mark record not found: " + request.getMarkId()));

        User user = userRepository.findById(facultyUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + facultyUserId));

        BigDecimal max = mark.getAssessment().getMaxMarks();
        BigDecimal newMarks = request.getNewMarks();

        if (newMarks.compareTo(BigDecimal.ZERO) < 0 || newMarks.compareTo(max) > 0) {
            throw new IllegalArgumentException(String.format("Corrected marks (%s) must be between 0.00 and max (%s)", newMarks, max));
        }

        BigDecimal oldMarks = mark.getMarksObtained();

        // Audit Trail: Record change history
        AcademicMarkAuditLog auditLog = AcademicMarkAuditLog.builder()
                .mark(mark)
                .previousMarks(oldMarks)
                .newMarks(newMarks)
                .reason(request.getReason() != null ? request.getReason() : "Authorized faculty re-evaluation")
                .changedByUser(user)
                .changedAt(LocalDateTime.now())
                .build();
        markAuditLogRepository.save(auditLog);

        mark.setMarksObtained(newMarks);
        mark.setRemarks("Corrected: " + (request.getReason() != null ? request.getReason() : "Re-evaluated"));
        mark.setGradedByUser(user);
        mark.setGradedAt(LocalDateTime.now());

        mark = markRepository.save(mark);
        return mapToMarkDto(mark);
    }

    @Override
    public List<StudentMarkDto> getStudentPublishedMarks(Long studentUserId, Long semesterId) {
        StudentEnrollment enrollment = studentEnrollmentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student enrollment not found"));

        return markRepository.findPublishedMarksByStudentAndSemester(enrollment.getId(), semesterId).stream()
                .map(this::mapToMarkDto)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Report Cards & Grading
    // -------------------------------------------------------------------------

    @Override
    public SemesterReportCardDto generateSemesterReportCard(Long studentUserId, Long semesterId) {
        StudentEnrollment enrollment = studentEnrollmentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student enrollment not found"));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found: " + semesterId));

        List<StudentSubjectEnrollment> enrolledSubjects = studentSubjectEnrollmentRepository
                .findByStudentEnrollmentId(enrollment.getId());

        List<SubjectResultDto> subjectResults = new ArrayList<>();
        int totalCreditsRegistered = 0;
        int totalCreditsEarned = 0;
        BigDecimal totalMarksObtained = BigDecimal.ZERO;
        BigDecimal totalMaxMarks = BigDecimal.ZERO;
        BigDecimal weightedGradePoints = BigDecimal.ZERO;
        boolean hasFailedAny = false;

        for (StudentSubjectEnrollment sse : enrolledSubjects) {
            Subject subject = sse.getSubject();
            int credits = subject.getCredits();
            totalCreditsRegistered += credits;

            List<AcademicMark> publishedMarks = markRepository
                    .findPublishedMarksByStudentAndSubject(enrollment.getId(), subject.getId());

            BigDecimal internalObtained = BigDecimal.ZERO;
            BigDecimal internalMax = BigDecimal.ZERO;
            BigDecimal endTermObtained = BigDecimal.ZERO;
            BigDecimal endTermMax = BigDecimal.ZERO;
            BigDecimal subjectTotalObtained = BigDecimal.ZERO;
            BigDecimal subjectTotalMax = BigDecimal.ZERO;

            for (AcademicMark m : publishedMarks) {
                AcademicAssessment a = m.getAssessment();
                BigDecimal obt = m.getMarksObtained();
                BigDecimal max = a.getMaxMarks();

                if ("END_TERM".equalsIgnoreCase(a.getAssessmentType())) {
                    endTermObtained = endTermObtained.add(obt);
                    endTermMax = endTermMax.add(max);
                } else {
                    internalObtained = internalObtained.add(obt);
                    internalMax = internalMax.add(max);
                }

                subjectTotalObtained = subjectTotalObtained.add(obt);
                subjectTotalMax = subjectTotalMax.add(max);
            }

            BigDecimal percentage;
            if (subjectTotalMax.compareTo(BigDecimal.ZERO) > 0) {
                percentage = subjectTotalObtained.divide(subjectTotalMax, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            } else {
                percentage = BigDecimal.ZERO;
            }

            // Standard approved Indian 10-point scale:
            String letterGrade;
            int gradePoint;
            if (percentage.compareTo(BigDecimal.valueOf(90.00)) >= 0) {
                letterGrade = "O";
                gradePoint = 10;
            } else if (percentage.compareTo(BigDecimal.valueOf(80.00)) >= 0) {
                letterGrade = "A+";
                gradePoint = 9;
            } else if (percentage.compareTo(BigDecimal.valueOf(70.00)) >= 0) {
                letterGrade = "A";
                gradePoint = 8;
            } else if (percentage.compareTo(BigDecimal.valueOf(60.00)) >= 0) {
                letterGrade = "B+";
                gradePoint = 7;
            } else if (percentage.compareTo(BigDecimal.valueOf(50.00)) >= 0) {
                letterGrade = "B";
                gradePoint = 6;
            } else if (percentage.compareTo(BigDecimal.valueOf(40.00)) >= 0) {
                letterGrade = "C";
                gradePoint = 5;
            } else {
                letterGrade = "F";
                gradePoint = 0;
                hasFailedAny = true;
            }

            int creditPts = credits * gradePoint;
            if (gradePoint > 0) {
                totalCreditsEarned += credits;
            }
            weightedGradePoints = weightedGradePoints.add(BigDecimal.valueOf(creditPts));

            totalMarksObtained = totalMarksObtained.add(subjectTotalObtained);
            totalMaxMarks = totalMaxMarks.add(subjectTotalMax);

            subjectResults.add(SubjectResultDto.builder()
                    .subjectId(subject.getId())
                    .subjectCode(subject.getCode())
                    .subjectName(subject.getName())
                    .credits(credits)
                    .subjectType(subject.getType())
                    .internalMarksObtained(internalObtained)
                    .internalMaxMarks(internalMax)
                    .endTermMarksObtained(endTermObtained)
                    .endTermMaxMarks(endTermMax)
                    .totalMarksObtained(subjectTotalObtained)
                    .totalMaxMarks(subjectTotalMax)
                    .percentage(percentage)
                    .letterGrade(letterGrade)
                    .gradePoint(gradePoint)
                    .creditPoints(creditPts)
                    .result(gradePoint > 0 ? "PASS" : "FAIL")
                    .build());
        }

        BigDecimal sgpa = BigDecimal.ZERO;
        if (totalCreditsRegistered > 0) {
            sgpa = weightedGradePoints.divide(BigDecimal.valueOf(totalCreditsRegistered), 2, RoundingMode.HALF_UP);
        }

        BigDecimal overallPercentage = BigDecimal.ZERO;
        if (totalMaxMarks.compareTo(BigDecimal.ZERO) > 0) {
            overallPercentage = totalMarksObtained.divide(totalMaxMarks, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        }

        String resultStatus;
        if (hasFailedAny || totalCreditsEarned < totalCreditsRegistered) {
            resultStatus = "FAIL / BACKLOG";
        } else if (sgpa.compareTo(BigDecimal.valueOf(8.50)) >= 0) {
            resultStatus = "FIRST CLASS WITH DISTINCTION";
        } else if (sgpa.compareTo(BigDecimal.valueOf(6.50)) >= 0) {
            resultStatus = "FIRST CLASS";
        } else {
            resultStatus = "PASS";
        }

        User student = enrollment.getUser();
        Section section = enrollment.getSection();
        Department department = section.getDepartment();

        return SemesterReportCardDto.builder()
                .studentEnrollmentId(enrollment.getId())
                .studentUserId(student.getId())
                .studentName(student.getFullName())
                .studentRollNumber(enrollment.getRollNumber())
                .branch(student.getBranch() != null ? student.getBranch() : department.getCode())
                .departmentName(department.getName())
                .semesterId(semester.getId())
                .semesterNumber(semester.getSemesterNumber())
                .termName(semester.getTermName())
                .academicYearName(semester.getAcademicYear().getYearName())
                .subjectResults(subjectResults)
                .totalCreditsRegistered(totalCreditsRegistered)
                .totalCreditsEarned(totalCreditsEarned)
                .totalMarksObtained(totalMarksObtained)
                .totalMaxMarks(totalMaxMarks)
                .overallPercentage(overallPercentage)
                .sgpa(sgpa)
                .resultStatus(resultStatus)
                .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")))
                .build();
    }

    @Override
    public byte[] exportReportCardPdf(Long studentUserId, Long semesterId) throws Exception {
        SemesterReportCardDto rc = generateSemesterReportCard(studentUserId, semesterId);

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // Institutional Header
        Font universityFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(15, 34, 64));
        Paragraph uni = new Paragraph("JECRC UNIVERSITY, JAIPUR", universityFont);
        uni.setAlignment(Element.ALIGN_CENTER);
        document.add(uni);

        Font deptFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY);
        Paragraph dept = new Paragraph("DEPARTMENT OF " + rc.getDepartmentName().toUpperCase(), deptFont);
        dept.setAlignment(Element.ALIGN_CENTER);
        document.add(dept);

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(24, 76, 120));
        Paragraph title = new Paragraph("OFFICIAL SEMESTER GRADE REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15);
        document.add(title);

        // Student Info Block
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(15);

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font valFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        addInfoCell(infoTable, "Student Name:", rc.getStudentName(), labelFont, valFont);
        addInfoCell(infoTable, "Roll Number:", rc.getStudentRollNumber(), labelFont, valFont);
        addInfoCell(infoTable, "Academic Year:", rc.getAcademicYearName(), labelFont, valFont);
        addInfoCell(infoTable, "Semester:", rc.getTermName(), labelFont, valFont);
        addInfoCell(infoTable, "Degree / Branch:", rc.getBranch(), labelFont, valFont);
        addInfoCell(infoTable, "Report Date:", rc.getGeneratedAt(), labelFont, valFont);
        document.add(infoTable);

        // Subject Results Table
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 4.0f, 1.2f, 1.5f, 1.5f, 1.2f, 1.2f, 1.5f});

        Font thFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        String[] headers = {"Code", "Course Title", "Credits", "Internal", "End-Term", "Grade", "GP", "Result"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, thFont));
            cell.setBackgroundColor(new Color(24, 76, 120));
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        Font boldRowFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);

        for (SubjectResultDto s : rc.getSubjectResults()) {
            table.addCell(new PdfPCell(new Phrase(s.getSubjectCode(), boldRowFont)));
            table.addCell(new PdfPCell(new Phrase(s.getSubjectName(), rowFont)));
            table.addCell(new PdfPCell(new Phrase(s.getCredits().toString(), rowFont)));
            table.addCell(new PdfPCell(new Phrase(s.getInternalMarksObtained() + "/" + s.getInternalMaxMarks(), rowFont)));
            table.addCell(new PdfPCell(new Phrase(s.getEndTermMarksObtained() + "/" + s.getEndTermMaxMarks(), rowFont)));
            table.addCell(new PdfPCell(new Phrase(s.getLetterGrade(), boldRowFont)));
            table.addCell(new PdfPCell(new Phrase(s.getGradePoint().toString(), rowFont)));
            table.addCell(new PdfPCell(new Phrase(s.getResult(), boldRowFont)));
        }
        document.add(table);

        // Summary Performance Block
        Paragraph summaryTitle = new Paragraph("\nSemester Performance Summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK));
        document.add(summaryTitle);

        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(5);

        addSummaryBox(summaryTable, "Credits Registered", rc.getTotalCreditsRegistered().toString());
        addSummaryBox(summaryTable, "Credits Earned", rc.getTotalCreditsEarned().toString());
        addSummaryBox(summaryTable, "Semester GPA (SGPA)", rc.getSgpa().toString());
        addSummaryBox(summaryTable, "Result Status", rc.getResultStatus());
        document.add(summaryTable);

        // Institutional Certification Footer
        Paragraph footer = new Paragraph("\nThis is a system-generated official academic transcript issued under institutional grading regulations.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private void addInfoCell(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label + " " + value, valFont));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(3);
        table.addCell(c1);
    }

    private void addSummaryBox(PdfPTable table, String label, String val) {
        PdfPCell c = new PdfPCell();
        c.setPadding(8);
        c.setBackgroundColor(new Color(241, 245, 249));
        Font lFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
        Font vFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(15, 34, 64));
        c.addElement(new Phrase(label, lFont));
        c.addElement(new Phrase(val, vFont));
        table.addCell(c);
    }

    // -------------------------------------------------------------------------
    // Helper & Mapping Methods
    // -------------------------------------------------------------------------

    private void verifyFacultyAssignment(User faculty, Long subjectId, Long sectionId) {
        if (faculty.getRole() == Role.ROLE_ADMIN) {
            return; // Admins have global academic oversight
        }
        Optional<FacultyAssignment> assignment = facultyAssignmentRepository
                .findByFacultyUserIdAndSubjectIdAndSectionId(faculty.getId(), subjectId, sectionId);
        if (assignment.isEmpty()) {
            throw new AccessDeniedException("Access denied: You are not assigned to instruct this subject in this section.");
        }
    }

    private AssignmentDto mapToAssignmentDto(AcademicAssignment a, Long currentStudentEnrollmentId) {
        boolean submitted = false;
        Long submissionId = null;
        String submissionStatus = "UNSUBMITTED";
        BigDecimal marks = null;

        if (currentStudentEnrollmentId != null) {
            Optional<AcademicSubmission> sub = submissionRepository
                    .findByAssignmentIdAndStudentEnrollmentId(a.getId(), currentStudentEnrollmentId);
            if (sub.isPresent()) {
                submitted = true;
                submissionId = sub.get().getId();
                submissionStatus = sub.get().getStatus();
                marks = sub.get().getMarksObtained();
            }
        }

        return AssignmentDto.builder()
                .id(a.getId())
                .subjectId(a.getSubject().getId())
                .subjectCode(a.getSubject().getCode())
                .subjectName(a.getSubject().getName())
                .sectionId(a.getSection().getId())
                .sectionName(a.getSection().getName())
                .facultyUserId(a.getFacultyUser().getId())
                .facultyName(a.getFacultyUser().getFullName())
                .title(a.getTitle())
                .description(a.getDescription())
                .attachmentUrl(a.getAttachmentUrl())
                .dueDate(a.getDueDate())
                .formattedDueDate(a.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")))
                .maxMarks(a.getMaxMarks())
                .allowLateSubmission(a.getAllowLateSubmission())
                .latePenaltyPercentage(a.getLatePenaltyPercentage())
                .status(a.getStatus())
                .isPastDue(LocalDateTime.now().isAfter(a.getDueDate()))
                .submittedByCurrentUser(submitted)
                .submissionId(submissionId)
                .submissionStatus(submissionStatus)
                .marksObtained(marks)
                .createdAt(a.getCreatedAt())
                .build();
    }

    private SubmissionDto mapToSubmissionDto(AcademicSubmission s) {
        AcademicAssignment a = s.getAssignment();
        StudentEnrollment se = s.getStudentEnrollment();
        User student = se.getUser();
        User gradedBy = s.getGradedByUser();

        return SubmissionDto.builder()
                .id(s.getId())
                .assignmentId(a.getId())
                .assignmentTitle(a.getTitle())
                .maxMarks(a.getMaxMarks())
                .studentEnrollmentId(se.getId())
                .studentUserId(student.getId())
                .studentName(student.getFullName())
                .studentRollNumber(se.getRollNumber())
                .submissionText(s.getSubmissionText())
                .fileUrl(s.getFileUrl())
                .submittedAt(s.getSubmittedAt())
                .isLate(s.getIsLate())
                .marksObtained(s.getMarksObtained())
                .feedback(s.getFeedback())
                .gradedByUserId(gradedBy != null ? gradedBy.getId() : null)
                .gradedByName(gradedBy != null ? gradedBy.getFullName() : null)
                .gradedAt(s.getGradedAt())
                .status(s.getStatus())
                .build();
    }

    private AssessmentDto mapToAssessmentDto(AcademicAssessment a) {
        int evaluated = markRepository.findByAssessmentId(a.getId()).size();

        return AssessmentDto.builder()
                .id(a.getId())
                .subjectId(a.getSubject().getId())
                .subjectCode(a.getSubject().getCode())
                .subjectName(a.getSubject().getName())
                .semesterId(a.getSemester().getId())
                .semesterTerm(a.getSemester().getTermName())
                .sectionId(a.getSection() != null ? a.getSection().getId() : null)
                .sectionName(a.getSection() != null ? a.getSection().getName() : "All Sections")
                .facultyUserId(a.getFacultyUser().getId())
                .facultyName(a.getFacultyUser().getFullName())
                .name(a.getName())
                .assessmentType(a.getAssessmentType())
                .weightagePercent(a.getWeightagePercent())
                .maxMarks(a.getMaxMarks())
                .passingMarks(a.getPassingMarks())
                .assessmentDate(a.getAssessmentDate())
                .isPublished(a.getIsPublished())
                .publishedAt(a.getPublishedAt())
                .createdAt(a.getCreatedAt())
                .evaluatedCount(evaluated)
                .build();
    }

    private StudentMarkDto mapToMarkDto(AcademicMark m) {
        AcademicAssessment a = m.getAssessment();
        StudentEnrollment se = m.getStudentEnrollment();
        User student = se != null ? se.getUser() : null;
        User gradedBy = m.getGradedByUser();
        boolean passed = !Boolean.TRUE.equals(m.getIsAbsent())
                && m.getMarksObtained() != null
                && (a.getPassingMarks() == null || m.getMarksObtained().compareTo(a.getPassingMarks()) >= 0);

        return StudentMarkDto.builder()
                .id(m.getId())
                .assessmentId(a.getId())
                .assessmentName(a.getName())
                .assessmentType(a.getAssessmentType())
                .maxMarks(a.getMaxMarks())
                .passingMarks(a.getPassingMarks())
                .weightagePercent(a.getWeightagePercent())
                .subjectId(a.getSubject() != null ? a.getSubject().getId() : null)
                .subjectCode(a.getSubject() != null ? a.getSubject().getCode() : null)
                .subjectName(a.getSubject() != null ? a.getSubject().getName() : null)
                .studentEnrollmentId(se != null ? se.getId() : null)
                .studentUserId(student != null ? student.getId() : null)
                .studentName(student != null ? student.getFullName() : null)
                .studentRollNumber(se != null ? se.getRollNumber() : null)
                .marksObtained(m.getMarksObtained())
                .isAbsent(m.getIsAbsent())
                .isPassed(passed)
                .remarks(m.getRemarks())
                .gradedByUserId(gradedBy != null ? gradedBy.getId() : null)
                .gradedByName(gradedBy != null ? gradedBy.getFullName() : null)
                .gradedAt(m.getGradedAt())
                .build();
    }
}
