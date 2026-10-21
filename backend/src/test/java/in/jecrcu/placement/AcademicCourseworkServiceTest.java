package in.jecrcu.placement;

import in.jecrcu.placement.dto.erp.*;
import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.entity.enums.Role;
import in.jecrcu.placement.entity.erp.*;
import in.jecrcu.placement.repository.UserRepository;
import in.jecrcu.placement.repository.erp.*;
import in.jecrcu.placement.service.NotificationService;
import in.jecrcu.placement.service.erp.AcademicCourseworkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicCourseworkServiceTest {

    @Mock private AcademicAssignmentRepository assignmentRepository;
    @Mock private AcademicSubmissionRepository submissionRepository;
    @Mock private AcademicAssessmentRepository assessmentRepository;
    @Mock private AcademicMarkRepository markRepository;
    @Mock private AcademicMarkAuditLogRepository markAuditLogRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SemesterRepository semesterRepository;
    @Mock private StudentEnrollmentRepository studentEnrollmentRepository;
    @Mock private StudentSubjectEnrollmentRepository studentSubjectEnrollmentRepository;
    @Mock private FacultyAssignmentRepository facultyAssignmentRepository;
    @Mock private UserRepository userRepository;

    static class FakeNotificationService extends NotificationService {
        int sentCount = 0;
        public FakeNotificationService() {
            super(null, null, null, null);
        }
        @Override
        public void sendNotification(User user, String title, String message, String link, String type) {
            sentCount++;
        }
    }

    private FakeNotificationService notificationService;
    private AcademicCourseworkServiceImpl courseworkService;

    private User faculty;
    private User otherFaculty;
    private User student;
    private Department cseDept;
    private AcademicYear academicYear;
    private Semester semester7;
    private Section sectionA;
    private Subject cs701;
    private Subject cs702;
    private StudentEnrollment enrollment;

    @BeforeEach
    void setUp() {
        notificationService = new FakeNotificationService();
        courseworkService = new AcademicCourseworkServiceImpl(
                assignmentRepository,
                submissionRepository,
                assessmentRepository,
                markRepository,
                markAuditLogRepository,
                subjectRepository,
                sectionRepository,
                semesterRepository,
                studentEnrollmentRepository,
                studentSubjectEnrollmentRepository,
                facultyAssignmentRepository,
                userRepository,
                notificationService
        );

        faculty = User.builder().id(7L).fullName("Dr. Ramesh Sharma").email("faculty.sharma@jecrcu.edu.in").role(Role.ROLE_FACULTY).build();
        otherFaculty = User.builder().id(8L).fullName("Dr. Other").email("other@jecrcu.edu.in").role(Role.ROLE_FACULTY).build();
        student = User.builder().id(2L).fullName("Rahul Sharma").email("rahul.sharma@jecrcu.edu.in").role(Role.ROLE_STUDENT).build();

        cseDept = Department.builder().id(1L).code("CSE").name("Computer Science and Engineering").build();
        academicYear = AcademicYear.builder().id(1L).yearName("2025-2026").build();
        semester7 = Semester.builder().id(1L).academicYear(academicYear).semesterNumber(7).termName("Semester 7 (Autumn 2025)").isActive(true).build();
        sectionA = Section.builder().id(1L).department(cseDept).semester(semester7).name("Sec-A").build();

        cs701 = Subject.builder().id(1L).department(cseDept).code("CS701").name("Distributed Cloud Architecture").credits(4).type("THEORY").build();
        cs702 = Subject.builder().id(2L).department(cseDept).code("CS702").name("Machine Learning").credits(4).type("THEORY").build();

        enrollment = StudentEnrollment.builder().id(1L).user(student).section(sectionA).rollNumber("22BCSE101").build();
    }

    @Test
    @DisplayName("Authorized faculty can create an assignment and students are notified")
    void testCreateAssignment_AuthorizedFaculty_Success() {
        AssignmentCreateRequestDto req = AssignmentCreateRequestDto.builder()
                .subjectId(1L)
                .sectionId(1L)
                .title("Kubernetes Auto-scaling")
                .description("Implement HPA")
                .dueDate(LocalDateTime.now().plusDays(7))
                .maxMarks(BigDecimal.valueOf(50.00))
                .allowLateSubmission(true)
                .latePenaltyPercentage(BigDecimal.valueOf(10.00))
                .build();

        when(userRepository.findById(7L)).thenReturn(Optional.of(faculty));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(cs701));
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(sectionA));

        FacultyAssignment fa = FacultyAssignment.builder().id(1L).facultyUser(faculty).subject(cs701).section(sectionA).build();
        when(facultyAssignmentRepository.findByFacultyUserIdAndSubjectIdAndSectionId(7L, 1L, 1L)).thenReturn(Optional.of(fa));

        when(assignmentRepository.save(any())).thenAnswer(inv -> {
            AcademicAssignment a = inv.getArgument(0);
            a.setId(10L);
            return a;
        });

        when(studentEnrollmentRepository.findBySectionId(1L)).thenReturn(List.of(enrollment));

        AssignmentDto dto = courseworkService.createAssignment(req, 7L);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("Kubernetes Auto-scaling", dto.getTitle());
        assertEquals(1, notificationService.sentCount);
    }

    @Test
    @DisplayName("Unauthorized faculty cannot create an assignment for another faculty's subject")
    void testCreateAssignment_UnauthorizedFaculty_ThrowsAccessDenied() {
        AssignmentCreateRequestDto req = AssignmentCreateRequestDto.builder()
                .subjectId(1L)
                .sectionId(1L)
                .title("Unauthorized Test")
                .description("No assignment")
                .dueDate(LocalDateTime.now().plusDays(5))
                .build();

        when(userRepository.findById(8L)).thenReturn(Optional.of(otherFaculty));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(cs701));
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(sectionA));
        when(facultyAssignmentRepository.findByFacultyUserIdAndSubjectIdAndSectionId(8L, 1L, 1L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> courseworkService.createAssignment(req, 8L));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Student submits assignment on time successfully")
    void testSubmitAssignment_OnTime_Success() {
        AcademicAssignment assignment = AcademicAssignment.builder()
                .id(10L)
                .subject(cs701)
                .section(sectionA)
                .facultyUser(faculty)
                .title("Cloud Lab 1")
                .dueDate(LocalDateTime.now().plusDays(2)) // in future
                .allowLateSubmission(false)
                .status("PUBLISHED")
                .build();

        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(studentEnrollmentRepository.findByUserId(2L)).thenReturn(Optional.of(enrollment));
        when(submissionRepository.findByAssignmentIdAndStudentEnrollmentId(10L, 1L)).thenReturn(Optional.empty());
        when(submissionRepository.save(any())).thenAnswer(inv -> {
            AcademicSubmission s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        SubmissionSubmitRequestDto req = SubmissionSubmitRequestDto.builder()
                .submissionText("My cloud solution")
                .fileUrl("https://github.com/rahul/cloud")
                .build();

        SubmissionDto dto = courseworkService.submitAssignment(10L, req, 2L);

        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertFalse(dto.getIsLate());
        assertEquals("SUBMITTED", dto.getStatus());
        assertEquals(1, notificationService.sentCount); // faculty notified
    }

    @Test
    @DisplayName("Late submission rejected if allowLateSubmission is false")
    void testSubmitAssignment_LateNotPermitted_ThrowsIllegalState() {
        AcademicAssignment assignment = AcademicAssignment.builder()
                .id(10L)
                .subject(cs701)
                .section(sectionA)
                .facultyUser(faculty)
                .title("Strict Assignment")
                .dueDate(LocalDateTime.now().minusDays(2)) // past due
                .allowLateSubmission(false)
                .status("PUBLISHED")
                .build();

        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(studentEnrollmentRepository.findByUserId(2L)).thenReturn(Optional.of(enrollment));

        SubmissionSubmitRequestDto req = SubmissionSubmitRequestDto.builder()
                .submissionText("Late attempt")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                courseworkService.submitAssignment(10L, req, 2L));

        assertTrue(ex.getMessage().contains("Late submissions are not permitted"));
        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Authorized faculty grades submission and validates maximum marks")
    void testGradeSubmission_Success() {
        AcademicAssignment assignment = AcademicAssignment.builder()
                .id(10L)
                .subject(cs701)
                .section(sectionA)
                .facultyUser(faculty)
                .title("Cloud Project")
                .maxMarks(BigDecimal.valueOf(50.00))
                .build();

        AcademicSubmission submission = AcademicSubmission.builder()
                .id(100L)
                .assignment(assignment)
                .studentEnrollment(enrollment)
                .status("SUBMITTED")
                .build();

        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(7L)).thenReturn(Optional.of(faculty));
        FacultyAssignment fa = FacultyAssignment.builder().id(1L).facultyUser(faculty).subject(cs701).section(sectionA).build();
        when(facultyAssignmentRepository.findByFacultyUserIdAndSubjectIdAndSectionId(7L, 1L, 1L)).thenReturn(Optional.of(fa));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmissionGradeRequestDto gradeReq = SubmissionGradeRequestDto.builder()
                .marksObtained(BigDecimal.valueOf(45.50))
                .feedback("Great architectural diagrams")
                .build();

        SubmissionDto dto = courseworkService.gradeSubmission(100L, gradeReq, 7L);

        assertEquals("GRADED", dto.getStatus());
        assertEquals(BigDecimal.valueOf(45.50), dto.getMarksObtained());
        assertEquals(1, notificationService.sentCount); // student notified
    }

    @Test
    @DisplayName("Grading marks exceeding max marks is rejected")
    void testGradeSubmission_ExceedsMaxMarks_ThrowsIllegalArgument() {
        AcademicAssignment assignment = AcademicAssignment.builder()
                .id(10L)
                .subject(cs701)
                .section(sectionA)
                .facultyUser(faculty)
                .maxMarks(BigDecimal.valueOf(50.00))
                .build();

        AcademicSubmission submission = AcademicSubmission.builder()
                .id(100L)
                .assignment(assignment)
                .studentEnrollment(enrollment)
                .build();

        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(7L)).thenReturn(Optional.of(faculty));
        FacultyAssignment fa = FacultyAssignment.builder().id(1L).facultyUser(faculty).subject(cs701).section(sectionA).build();
        when(facultyAssignmentRepository.findByFacultyUserIdAndSubjectIdAndSectionId(7L, 1L, 1L)).thenReturn(Optional.of(fa));

        SubmissionGradeRequestDto gradeReq = SubmissionGradeRequestDto.builder()
                .marksObtained(BigDecimal.valueOf(55.00)) // exceeds 50.00
                .build();

        assertThrows(IllegalArgumentException.class, () -> courseworkService.gradeSubmission(100L, gradeReq, 7L));
    }

    @Test
    @DisplayName("Publishing assessment results notifies enrolled students")
    void testPublishAssessmentResults() {
        AcademicAssessment assessment = AcademicAssessment.builder()
                .id(1L)
                .subject(cs701)
                .semester(semester7)
                .facultyUser(faculty)
                .name("Mid-Term Exam")
                .maxMarks(BigDecimal.valueOf(100.00))
                .isPublished(false)
                .build();

        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(userRepository.findById(7L)).thenReturn(Optional.of(faculty));
        when(assessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AcademicMark mark = AcademicMark.builder()
                .id(10L)
                .assessment(assessment)
                .studentEnrollment(enrollment)
                .marksObtained(BigDecimal.valueOf(85.00))
                .build();
        when(markRepository.findByAssessmentId(1L)).thenReturn(List.of(mark));

        AssessmentDto dto = courseworkService.publishAssessmentResults(1L, 7L);

        assertTrue(dto.getIsPublished());
        assertEquals(1, notificationService.sentCount);
    }

    @Test
    @DisplayName("Correcting student marks creates an auditable change log")
    void testCorrectStudentMark_CreatesAuditLog() {
        AcademicAssessment assessment = AcademicAssessment.builder()
                .id(1L)
                .subject(cs701)
                .maxMarks(BigDecimal.valueOf(100.00))
                .passingMarks(BigDecimal.valueOf(40.00))
                .build();
        AcademicMark mark = AcademicMark.builder()
                .id(50L)
                .assessment(assessment)
                .studentEnrollment(enrollment)
                .marksObtained(BigDecimal.valueOf(70.00))
                .build();

        when(markRepository.findById(50L)).thenReturn(Optional.of(mark));
        when(userRepository.findById(7L)).thenReturn(Optional.of(faculty));
        when(markRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MarkCorrectionRequestDto req = MarkCorrectionRequestDto.builder()
                .markId(50L)
                .newMarks(BigDecimal.valueOf(78.00))
                .reason("Re-evaluation of question 4")
                .build();

        StudentMarkDto dto = courseworkService.correctStudentMark(req, 7L);

        assertEquals(BigDecimal.valueOf(78.00), dto.getMarksObtained());
        verify(markAuditLogRepository, times(1)).save(any(AcademicMarkAuditLog.class));
    }

    @Test
    @DisplayName("Semester report card correctly calculates SGPA and approved grading rules")
    void testGenerateSemesterReportCard_CalculatesSgpaAndApprovedGradingScale() {
        when(studentEnrollmentRepository.findByUserId(2L)).thenReturn(Optional.of(enrollment));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester7));

        StudentSubjectEnrollment sse1 = StudentSubjectEnrollment.builder().id(1L).subject(cs701).build(); // 4 credits
        StudentSubjectEnrollment sse2 = StudentSubjectEnrollment.builder().id(2L).subject(cs702).build(); // 4 credits
        when(studentSubjectEnrollmentRepository.findByStudentEnrollmentId(1L)).thenReturn(List.of(sse1, sse2));

        // CS701: 88 / 100 -> Grade A+ (GP 9)
        AcademicAssessment a1 = AcademicAssessment.builder().id(1L).maxMarks(BigDecimal.valueOf(100.00)).assessmentType("END_TERM").passingMarks(BigDecimal.valueOf(40.00)).build();
        AcademicMark m1 = AcademicMark.builder().id(1L).assessment(a1).marksObtained(BigDecimal.valueOf(88.00)).build();
        when(markRepository.findPublishedMarksByStudentAndSubject(1L, 1L)).thenReturn(List.of(m1));

        // CS702: 82 / 100 -> Grade A+ (GP 9)
        AcademicAssessment a2 = AcademicAssessment.builder().id(2L).maxMarks(BigDecimal.valueOf(100.00)).assessmentType("END_TERM").passingMarks(BigDecimal.valueOf(40.00)).build();
        AcademicMark m2 = AcademicMark.builder().id(2L).assessment(a2).marksObtained(BigDecimal.valueOf(82.00)).build();
        when(markRepository.findPublishedMarksByStudentAndSubject(1L, 2L)).thenReturn(List.of(m2));

        SemesterReportCardDto rc = courseworkService.generateSemesterReportCard(2L, 1L);

        assertNotNull(rc);
        assertEquals(8, rc.getTotalCreditsRegistered()); // 4 + 4
        assertEquals(8, rc.getTotalCreditsEarned());
        // SGPA: (4*9 + 4*9) / 8 = 72 / 8 = 9.00
        assertEquals(BigDecimal.valueOf(9.00).setScale(2), rc.getSgpa());
        assertEquals("FIRST CLASS WITH DISTINCTION", rc.getResultStatus());
        assertEquals(2, rc.getSubjectResults().size());
        assertEquals("A+", rc.getSubjectResults().get(0).getLetterGrade());
        assertEquals("A+", rc.getSubjectResults().get(1).getLetterGrade());
    }

    @Test
    @DisplayName("Official PDF Report Card export generates valid PDF bytes")
    void testExportReportCardPdf() throws Exception {
        when(studentEnrollmentRepository.findByUserId(2L)).thenReturn(Optional.of(enrollment));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester7));

        StudentSubjectEnrollment sse1 = StudentSubjectEnrollment.builder().id(1L).subject(cs701).build();
        when(studentSubjectEnrollmentRepository.findByStudentEnrollmentId(1L)).thenReturn(List.of(sse1));

        AcademicAssessment a1 = AcademicAssessment.builder().id(1L).maxMarks(BigDecimal.valueOf(100.00)).assessmentType("END_TERM").passingMarks(BigDecimal.valueOf(40.00)).build();
        AcademicMark m1 = AcademicMark.builder().id(1L).assessment(a1).marksObtained(BigDecimal.valueOf(91.00)).build();
        when(markRepository.findPublishedMarksByStudentAndSubject(1L, 1L)).thenReturn(List.of(m1));

        byte[] pdf = courseworkService.exportReportCardPdf(2L, 1L);

        assertNotNull(pdf);
        assertTrue(pdf.length > 500);
        // Standard PDF magic byte signature "%PDF"
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }
}
