package in.jecrcu.placement.service.crt;

import in.jecrcu.placement.dto.crt.*;
import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.entity.crt.*;
import in.jecrcu.placement.entity.erp.AcademicYear;
import in.jecrcu.placement.entity.erp.StudentEnrollment;
import in.jecrcu.placement.repository.UserRepository;
import in.jecrcu.placement.repository.crt.*;
import in.jecrcu.placement.repository.erp.AcademicYearRepository;
import in.jecrcu.placement.repository.erp.StudentEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrtServiceImpl implements CrtService {

    private final UserRepository userRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final CrtTrackRepository crtTrackRepository;
    private final CrtBatchRepository crtBatchRepository;
    private final CrtBatchTrainerRepository crtBatchTrainerRepository;
    private final CrtEnrollmentRepository crtEnrollmentRepository;
    private final CrtSessionRepository crtSessionRepository;
    private final CrtAttendanceRecordRepository crtAttendanceRecordRepository;
    private final CrtAttendanceCorrectionRepository crtAttendanceCorrectionRepository;
    private final CrtAttendanceAuditLogRepository crtAttendanceAuditLogRepository;
    private final CrtAssessmentRepository crtAssessmentRepository;
    private final CrtAssessmentScoreRepository crtAssessmentScoreRepository;

    @Override
    @Transactional(readOnly = true)
    public CrtEligibilityDto checkStudentEligibility(Long studentUserId) {
        User user = userRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + studentUserId));

        boolean eligible = false;
        String reason = "";
        String semesterInfo = "Not Enrolled";

        // Criteria 1: Existing Academic ERP enrollment in a 4th-year semester (semester_number >= 7)
        Optional<StudentEnrollment> erpEnrollment = studentEnrollmentRepository.findByUserId(studentUserId);
        if (erpEnrollment.isPresent()) {
            StudentEnrollment se = erpEnrollment.get();
            int semNumber = se.getSection().getSemester().getSemesterNumber();
            semesterInfo = "Semester " + semNumber + " (" + se.getSection().getSemester().getTermName() + ")";
            if (semNumber >= 7) {
                eligible = true;
                reason = "Eligible: Verified fourth-year student in " + semesterInfo + ".";
            } else {
                reason = "Ineligible: Current semester is " + semesterInfo + ". CRT requires fourth-year standing (Semester 7 or 8).";
            }
        } else {
            // Criteria 2: Fallback to graduation year if ERP section enrollment is pending
            if (user.getGraduationYear() != null && user.getGraduationYear() <= 2026) {
                eligible = true;
                reason = "Eligible: Fourth-year candidate based on graduation batch " + user.getGraduationYear() + ".";
                semesterInfo = "Year 4 (Batch " + user.getGraduationYear() + ")";
            } else {
                reason = "Ineligible: No fourth-year academic enrollment or graduating year record found.";
            }
        }

        // Query CRT opt-in record
        Optional<CrtEnrollment> crtEnrollment = crtEnrollmentRepository.findByStudentId(studentUserId);

        CrtEligibilityDto.CrtEligibilityDtoBuilder builder = CrtEligibilityDto.builder()
                .fourthYearEligible(eligible)
                .eligibilityReason(reason)
                .currentAcademicSemester(semesterInfo)
                .graduationYear(user.getGraduationYear());

        crtEnrollment.ifPresent(enrollment -> {
            builder.enrollmentId(enrollment.getId())
                    .enrollmentStatus(enrollment.getStatus())
                    .optInNotes(enrollment.getOptInNotes())
                    .optedInAt(enrollment.getOptedInAt())
                    .verifiedAt(enrollment.getVerifiedAt())
                    .verificationRemarks(enrollment.getVerificationRemarks());

            if (enrollment.getBatch() != null) {
                builder.batchId(enrollment.getBatch().getId())
                        .batchName(enrollment.getBatch().getName())
                        .batchCode(enrollment.getBatch().getBatchCode());
            }
        });

        return builder.build();
    }

    @Override
    @Transactional
    public CrtEnrollment optInForCrt(Long studentUserId, CrtOptInRequestDto request) {
        CrtEligibilityDto eligibility = checkStudentEligibility(studentUserId);
        if (!eligibility.isFourthYearEligible()) {
            throw new IllegalStateException("Only verified fourth-year students may opt in for CRT. " + eligibility.getEligibilityReason());
        }

        User student = userRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentUserId));

        AcademicYear currentYear = academicYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new IllegalStateException("No active academic year configured"));

        Optional<CrtEnrollment> existingOptIn = crtEnrollmentRepository.findByStudentIdAndAcademicYearId(studentUserId, currentYear.getId());
        if (existingOptIn.isPresent()) {
            CrtEnrollment current = existingOptIn.get();
            if (current.getStatus() == CrtEnrollmentStatus.VERIFIED) {
                throw new IllegalStateException("You are already verified and enrolled in CRT.");
            }
            if (current.getStatus() == CrtEnrollmentStatus.REQUESTED) {
                throw new IllegalStateException("Your CRT opt-in application is already submitted and pending coordinator verification.");
            }
            // If previously rejected/withdrawn, update to REQUESTED
            current.setStatus(CrtEnrollmentStatus.REQUESTED);
            current.setOptInNotes(request.getOptInNotes());
            current.setOptedInAt(LocalDateTime.now());
            current.setVerifiedBy(null);
            current.setVerifiedAt(null);
            current.setVerificationRemarks(null);
            return crtEnrollmentRepository.save(current);
        }

        // Determine requested batch
        CrtBatch assignedBatch = null;
        if (request.getPreferredBatchId() != null) {
            assignedBatch = crtBatchRepository.findById(request.getPreferredBatchId()).orElse(null);
        }
        if (assignedBatch == null) {
            List<CrtBatch> activeBatches = crtBatchRepository.findByIsActiveTrue();
            if (!activeBatches.isEmpty()) {
                assignedBatch = activeBatches.get(0);
            }
        }

        CrtEnrollment enrollment = CrtEnrollment.builder()
                .student(student)
                .academicYear(currentYear)
                .batch(assignedBatch)
                .status(CrtEnrollmentStatus.REQUESTED)
                .optInNotes(request.getOptInNotes())
                .optedInAt(LocalDateTime.now())
                .build();

        return crtEnrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional
    public CrtEnrollment verifyEnrollment(Long enrollmentId, Long reviewerUserId, CrtVerificationActionDto action) {
        CrtEnrollment enrollment = crtEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("CRT enrollment not found: " + enrollmentId));

        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found: " + reviewerUserId));

        if ("APPROVE".equalsIgnoreCase(action.getAction())) {
            enrollment.setStatus(CrtEnrollmentStatus.VERIFIED);
            if (action.getAssignedBatchId() != null) {
                CrtBatch batch = crtBatchRepository.findById(action.getAssignedBatchId())
                        .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + action.getAssignedBatchId()));
                enrollment.setBatch(batch);
            }
        } else if ("REJECT".equalsIgnoreCase(action.getAction())) {
            enrollment.setStatus(CrtEnrollmentStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("Invalid action: " + action.getAction() + ". Must be APPROVE or REJECT.");
        }

        enrollment.setVerifiedBy(reviewer);
        enrollment.setVerifiedAt(LocalDateTime.now());
        enrollment.setVerificationRemarks(action.getRemarks());

        return crtEnrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CrtEnrollment> getPendingEnrollments() {
        return crtEnrollmentRepository.findByStatus(CrtEnrollmentStatus.REQUESTED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CrtBatch> getActiveBatches() {
        return crtBatchRepository.findByIsActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public CrtStudentDashboardDto getStudentDashboard(Long studentUserId) {
        CrtEligibilityDto eligibility = checkStudentEligibility(studentUserId);

        // If not verified, return only the eligibility breakdown and empty training lists
        if (eligibility.getEnrollmentStatus() != CrtEnrollmentStatus.VERIFIED) {
            return CrtStudentDashboardDto.builder()
                    .eligibility(eligibility)
                    .attendanceSummary(null)
                    .upcomingSessions(List.of())
                    .recentAttendance(List.of())
                    .assessmentReports(List.of())
                    .build();
        }

        Long batchId = eligibility.getBatchId();

        // 1. Calculate Attendance Summary
        CrtAttendanceSummaryDto summary = calculateStudentAttendance(studentUserId, batchId);

        // 2. Fetch upcoming sessions for this batch
        List<CrtSession> upcoming = (batchId != null)
                ? crtSessionRepository.findByBatchIdAndStatusOrderBySessionDateAscStartTimeAsc(batchId, CrtSessionStatus.SCHEDULED)
                : List.of();

        List<CrtSessionDto> sessionDtos = upcoming.stream().map(this::toSessionDto).toList();

        // 3. Fetch recent attendance records
        List<CrtAttendanceRecord> records = (batchId != null)
                ? crtAttendanceRecordRepository.findByStudentIdAndBatchId(studentUserId, batchId)
                : crtAttendanceRecordRepository.findByStudentIdOrderBySessionSessionDateDesc(studentUserId);

        List<CrtAttendanceRecordDto> recordDtos = records.stream().map(this::toRecordDto).toList();

        // 4. Fetch assessment reports
        List<CrtAssessmentScore> scores = crtAssessmentScoreRepository.findByStudentIdOrderByAssessmentAssessmentDateDesc(studentUserId);
        List<CrtAssessmentReportDto> assessmentReports = scores.stream().map(this::toAssessmentReportDto).toList();

        return CrtStudentDashboardDto.builder()
                .eligibility(eligibility)
                .attendanceSummary(summary)
                .upcomingSessions(sessionDtos)
                .recentAttendance(recordDtos)
                .assessmentReports(assessmentReports)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CrtSessionDto> getSessionsForBatch(Long batchId) {
        return crtSessionRepository.findByBatchIdOrderBySessionDateDescStartTimeDesc(batchId)
                .stream().map(this::toSessionDto).toList();
    }

    @Override
    @Transactional
    public CrtSession createSession(Long batchId, Long trackId, Long trainerUserId, CrtSessionDto sessionDto) {
        CrtBatch batch = crtBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        CrtTrack track = crtTrackRepository.findById(trackId)
                .orElseThrow(() -> new IllegalArgumentException("Track not found: " + trackId));

        User trainer = userRepository.findById(trainerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Trainer not found: " + trainerUserId));

        CrtSession session = CrtSession.builder()
                .batch(batch)
                .track(track)
                .trainer(trainer)
                .sessionDate(sessionDto.getSessionDate())
                .startTime(sessionDto.getStartTime())
                .endTime(sessionDto.getEndTime())
                .topic(sessionDto.getTopic())
                .locationOrLink(sessionDto.getLocationOrLink())
                .status(sessionDto.getStatus() != null ? sessionDto.getStatus() : CrtSessionStatus.SCHEDULED)
                .build();

        return crtSessionRepository.save(session);
    }

    @Override
    @Transactional
    public void markSessionAttendance(Long sessionId, Long trainerUserId, List<CrtAttendanceRecordDto> records) {
        CrtSession session = crtSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        User trainer = userRepository.findById(trainerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Trainer not found: " + trainerUserId));

        for (CrtAttendanceRecordDto recordDto : records) {
            User student = userRepository.findById(recordDto.getStudentId())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found: " + recordDto.getStudentId()));

            // Verify student is indeed a verified CRT enrollee
            Optional<CrtEnrollment> enrollment = crtEnrollmentRepository.findVerifiedByStudentId(student.getId());
            if (enrollment.isEmpty()) {
                throw new AccessDeniedException("Cannot record CRT attendance for non-enrolled or unverified student: " + student.getEmail());
            }

            Optional<CrtAttendanceRecord> existing = crtAttendanceRecordRepository.findBySessionIdAndStudentId(sessionId, student.getId());
            if (existing.isPresent()) {
                CrtAttendanceRecord r = existing.get();
                String prevStatus = r.getStatus().name();
                r.setStatus(recordDto.getStatus());
                r.setRemarks(recordDto.getRemarks());
                r.setUpdatedAt(LocalDateTime.now());
                crtAttendanceRecordRepository.save(r);

                // Audit log
                if (!prevStatus.equals(recordDto.getStatus().name())) {
                    CrtAttendanceAuditLog audit = CrtAttendanceAuditLog.builder()
                            .record(r)
                            .action("UPDATED")
                            .previousStatus(prevStatus)
                            .newStatus(recordDto.getStatus().name())
                            .performedBy(trainer)
                            .reason(recordDto.getRemarks() != null ? recordDto.getRemarks() : "Session attendance updated by trainer")
                            .build();
                    crtAttendanceAuditLogRepository.save(audit);
                }
            } else {
                CrtAttendanceRecord newRecord = CrtAttendanceRecord.builder()
                        .session(session)
                        .student(student)
                        .status(recordDto.getStatus())
                        .remarks(recordDto.getRemarks())
                        .recordedBy(trainer)
                        .recordedAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                crtAttendanceRecordRepository.save(newRecord);

                CrtAttendanceAuditLog audit = CrtAttendanceAuditLog.builder()
                        .record(newRecord)
                        .action("CREATED")
                        .previousStatus(null)
                        .newStatus(recordDto.getStatus().name())
                        .performedBy(trainer)
                        .reason("Initial session attendance marked")
                        .build();
                crtAttendanceAuditLogRepository.save(audit);
            }
        }

        // Auto-mark session as COMPLETED if it was SCHEDULED
        if (session.getStatus() == CrtSessionStatus.SCHEDULED) {
            session.setStatus(CrtSessionStatus.COMPLETED);
            crtSessionRepository.save(session);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CrtAttendanceRecordDto> getSessionAttendance(Long sessionId) {
        return crtAttendanceRecordRepository.findBySessionId(sessionId)
                .stream().map(this::toRecordDto).toList();
    }

    @Override
    @Transactional
    public CrtAttendanceCorrection requestCorrection(Long studentUserId, CrtCorrectionRequestDto request) {
        CrtAttendanceRecord record = crtAttendanceRecordRepository.findById(request.getRecordId())
                .orElseThrow(() -> new IllegalArgumentException("Attendance record not found: " + request.getRecordId()));

        if (!record.getStudent().getId().equals(studentUserId)) {
            throw new AccessDeniedException("You are not authorized to request correction on another student's record.");
        }

        User student = userRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentUserId));

        CrtAttendanceCorrection correction = CrtAttendanceCorrection.builder()
                .record(record)
                .student(student)
                .requestedStatus(request.getRequestedStatus())
                .reason(request.getReason())
                .status("PENDING")
                .build();

        return crtAttendanceCorrectionRepository.save(correction);
    }

    @Override
    @Transactional
    public void reviewCorrection(Long correctionId, Long reviewerUserId, boolean approved, String remarks) {
        CrtAttendanceCorrection correction = crtAttendanceCorrectionRepository.findById(correctionId)
                .orElseThrow(() -> new IllegalArgumentException("Correction request not found: " + correctionId));

        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found: " + reviewerUserId));

        correction.setStatus(approved ? "APPROVED" : "REJECTED");
        correction.setReviewedBy(reviewer);
        correction.setReviewedAt(LocalDateTime.now());
        correction.setReviewerRemarks(remarks);
        crtAttendanceCorrectionRepository.save(correction);

        if (approved) {
            CrtAttendanceRecord record = correction.getRecord();
            String prev = record.getStatus().name();
            record.setStatus(correction.getRequestedStatus());
            record.setUpdatedAt(LocalDateTime.now());
            record.setRemarks("Correction approved: " + remarks);
            crtAttendanceRecordRepository.save(record);

            CrtAttendanceAuditLog audit = CrtAttendanceAuditLog.builder()
                    .record(record)
                    .action("CORRECTION_APPROVED")
                    .previousStatus(prev)
                    .newStatus(correction.getRequestedStatus().name())
                    .performedBy(reviewer)
                    .reason(remarks)
                    .build();
            crtAttendanceAuditLogRepository.save(audit);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CrtAttendanceCorrection> getPendingCorrections() {
        return crtAttendanceCorrectionRepository.findByStatusOrderByCreatedAtAsc("PENDING");
    }

    @Override
    @Transactional(readOnly = true)
    public List<CrtAssessment> getAssessmentsForBatch(Long batchId) {
        return crtAssessmentRepository.findByBatchIdOrderByAssessmentDateDesc(batchId);
    }

    @Override
    @Transactional
    public void recordAssessmentScore(Long assessmentId, Long studentUserId, BigDecimal marks, String remarks, Long gradedByUserId) {
        CrtAssessment assessment = crtAssessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));

        User student = userRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentUserId));

        User grader = userRepository.findById(gradedByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Grader not found: " + gradedByUserId));

        Optional<CrtAssessmentScore> existing = crtAssessmentScoreRepository.findByAssessmentIdAndStudentId(assessmentId, studentUserId);
        if (existing.isPresent()) {
            CrtAssessmentScore score = existing.get();
            score.setMarksObtained(marks);
            score.setRemarks(remarks);
            score.setGradedBy(grader);
            score.setGradedAt(LocalDateTime.now());
            crtAssessmentScoreRepository.save(score);
        } else {
            CrtAssessmentScore newScore = CrtAssessmentScore.builder()
                    .assessment(assessment)
                    .student(student)
                    .marksObtained(marks)
                    .remarks(remarks)
                    .gradedBy(grader)
                    .build();
            crtAssessmentScoreRepository.save(newScore);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CrtAttendanceSummaryDto calculateStudentAttendance(Long studentUserId, Long batchId) {
        List<CrtAttendanceRecord> records = (batchId != null)
                ? crtAttendanceRecordRepository.findByStudentIdAndBatchId(studentUserId, batchId)
                : crtAttendanceRecordRepository.findByStudentIdOrderBySessionSessionDateDesc(studentUserId);

        long total = records.size();
        long present = 0;
        long late = 0;
        long absent = 0;
        long excused = 0;

        for (CrtAttendanceRecord r : records) {
            switch (r.getStatus()) {
                case PRESENT -> present++;
                case LATE -> late++;
                case ABSENT -> absent++;
                case EXCUSED -> excused++;
            }
        }

        // Present and Late are counted as attended
        long attended = present + late;
        double percentage = total > 0 ? (double) attended / total * 100.0 : 100.0;
        percentage = Math.round(percentage * 100.0) / 100.0;

        boolean debarmentRisk = percentage < 75.0 && total >= 3;
        String standing = "EXCELLENT";
        if (percentage < 75.0) {
            standing = "DEBARMENT_RISK";
        } else if (percentage < 85.0) {
            standing = "GOOD";
        }

        return CrtAttendanceSummaryDto.builder()
                .totalSessions(total)
                .attendedSessions(attended)
                .lateSessions(late)
                .absentSessions(absent)
                .excusedSessions(excused)
                .attendancePercentage(percentage)
                .debarmentRisk(debarmentRisk)
                .standingStatus(standing)
                .build();
    }

    // Helper mappers
    private CrtSessionDto toSessionDto(CrtSession s) {
        return CrtSessionDto.builder()
                .id(s.getId())
                .batchId(s.getBatch().getId())
                .batchName(s.getBatch().getName())
                .batchCode(s.getBatch().getBatchCode())
                .trackId(s.getTrack().getId())
                .trackCode(s.getTrack().getCode())
                .trackName(s.getTrack().getName())
                .trainerId(s.getTrainer().getId())
                .trainerName(s.getTrainer().getFullName())
                .sessionDate(s.getSessionDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .topic(s.getTopic())
                .locationOrLink(s.getLocationOrLink())
                .status(s.getStatus())
                .build();
    }

    private CrtAttendanceRecordDto toRecordDto(CrtAttendanceRecord r) {
        return CrtAttendanceRecordDto.builder()
                .id(r.getId())
                .sessionId(r.getSession().getId())
                .trackCode(r.getSession().getTrack().getCode())
                .trackName(r.getSession().getTrack().getName())
                .topic(r.getSession().getTopic())
                .sessionDate(r.getSession().getSessionDate())
                .startTime(r.getSession().getStartTime())
                .endTime(r.getSession().getEndTime())
                .trainerName(r.getSession().getTrainer().getFullName())
                .studentId(r.getStudent().getId())
                .studentName(r.getStudent().getFullName())
                .studentRollNumber(r.getStudent().getEmail())
                .status(r.getStatus())
                .remarks(r.getRemarks())
                .recordedAt(r.getRecordedAt())
                .build();
    }

    private CrtAssessmentReportDto toAssessmentReportDto(CrtAssessmentScore s) {
        BigDecimal percentage = s.getMarksObtained()
                .divide(s.getAssessment().getMaxMarks(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        boolean passed = percentage.compareTo(s.getAssessment().getPassPercentage()) >= 0;

        return CrtAssessmentReportDto.builder()
                .assessmentId(s.getAssessment().getId())
                .title(s.getAssessment().getTitle())
                .trackName(s.getAssessment().getTrack().getName())
                .assessmentDate(s.getAssessment().getAssessmentDate())
                .maxMarks(s.getAssessment().getMaxMarks())
                .passPercentage(s.getAssessment().getPassPercentage())
                .marksObtained(s.getMarksObtained())
                .percentage(percentage)
                .passed(passed)
                .remarks(s.getRemarks())
                .build();
    }
}
