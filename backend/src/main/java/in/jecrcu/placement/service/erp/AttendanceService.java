package in.jecrcu.placement.service.erp;

import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.entity.enums.Role;
import in.jecrcu.placement.entity.erp.*;
import in.jecrcu.placement.repository.erp.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AttendanceCorrectionRequestRepository correctionRequestRepository;
    private final AttendanceAuditLogRepository auditLogRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentSubjectEnrollmentRepository studentSubjectEnrollmentRepository;
    private final FacultyAssignmentRepository facultyAssignmentRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;

    @Value("${app.attendance.minimum-percentage:75.0}")
    private double minimumPercentage;

    @Value("${app.attendance.include-approved-leave:true}")
    private boolean includeApprovedLeave;

    @Value("${app.attendance.include-official-duty:true}")
    private boolean includeOfficialDuty;

    // -------------------------------------------------------------
    // Data Transfer Objects
    // -------------------------------------------------------------
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectAttendanceSummaryDto {
        private Long subjectId;
        private String subjectCode;
        private String subjectName;
        private int totalSessions;
        private int presentCount;
        private int absentCount;
        private int approvedLeaveCount;
        private int officialDutyCount;
        private double effectiveAttendedCount;
        private double attendancePercentage;
        private boolean debarred;
        private String statusText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentOverallAttendanceDto {
        private Long studentId;
        private String studentName;
        private String rollNumber;
        private String sectionName;
        private int totalSubjects;
        private int totalSessions;
        private int totalPresent;
        private double overallPercentage;
        private double requiredThreshold;
        private boolean anyDebarmentWarning;
        private List<SubjectAttendanceSummaryDto> subjectSummaries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceHistoryItemDto {
        private Long recordId;
        private Long sessionId;
        private String subjectCode;
        private String subjectName;
        private String sessionDate;
        private String sessionTime;
        private String topicCovered;
        private AttendanceStatus status;
        private String remarks;
        private String facultyName;
        private boolean hasPendingCorrection;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordEntryInput {
        private Long studentEnrollmentId;
        private AttendanceStatus status;
        private String remarks;
    }

    // -------------------------------------------------------------
    // Student Attendance Queries (Self or Admin)
    // -------------------------------------------------------------
    @Transactional(readOnly = true)
    public StudentOverallAttendanceDto getStudentAttendanceSummary(Long targetUserId, User requestingUser) {
        validateStudentAccess(targetUserId, requestingUser);

        StudentEnrollment enr = studentEnrollmentRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("No active student enrollment found for user: " + targetUserId));

        List<StudentSubjectEnrollment> enrolledSubs = studentSubjectEnrollmentRepository.findByStudentEnrollmentId(enr.getId());

        List<SubjectAttendanceSummaryDto> summaries = new ArrayList<>();
        int cumSessions = 0;
        int cumPresent = 0;
        double cumEffective = 0;
        boolean anyWarning = false;

        for (StudentSubjectEnrollment sse : enrolledSubs) {
            Subject sub = sse.getSubject();
            List<AttendanceRecord> records = recordRepository.findByStudentEnrollmentIdAndSubjectId(enr.getId(), sub.getId());

            int total = records.size();
            int present = 0;
            int absent = 0;
            int leave = 0;
            int duty = 0;

            for (AttendanceRecord r : records) {
                switch (r.getStatus()) {
                    case PRESENT -> present++;
                    case ABSENT -> absent++;
                    case APPROVED_LEAVE -> leave++;
                    case OFFICIAL_DUTY -> duty++;
                }
            }

            double effectiveAttended = present
                    + (includeApprovedLeave ? leave : 0)
                    + (includeOfficialDuty ? duty : 0);

            double pct = (total > 0) ? (effectiveAttended / total) * 100.0 : 100.0;
            boolean isDebarred = total > 0 && pct < minimumPercentage;
            if (isDebarred) anyWarning = true;

            cumSessions += total;
            cumPresent += present;
            cumEffective += effectiveAttended;

            summaries.add(SubjectAttendanceSummaryDto.builder()
                    .subjectId(sub.getId())
                    .subjectCode(sub.getCode())
                    .subjectName(sub.getName())
                    .totalSessions(total)
                    .presentCount(present)
                    .absentCount(absent)
                    .approvedLeaveCount(leave)
                    .officialDutyCount(duty)
                    .effectiveAttendedCount(effectiveAttended)
                    .attendancePercentage(Math.round(pct * 10.0) / 10.0)
                    .debarred(isDebarred)
                    .statusText(total == 0 ? "NO_SESSIONS" : (isDebarred ? "DEBARMENT_WARNING" : "GOOD_STANDING"))
                    .build());
        }

        double overallPct = (cumSessions > 0) ? (cumEffective / cumSessions) * 100.0 : 100.0;

        return StudentOverallAttendanceDto.builder()
                .studentId(enr.getUser().getId())
                .studentName(enr.getUser().getFullName())
                .rollNumber(enr.getRollNumber())
                .sectionName(enr.getSection().getName())
                .totalSubjects(enrolledSubs.size())
                .totalSessions(cumSessions)
                .totalPresent(cumPresent)
                .overallPercentage(Math.round(overallPct * 10.0) / 10.0)
                .requiredThreshold(minimumPercentage)
                .anyDebarmentWarning(anyWarning)
                .subjectSummaries(summaries)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AttendanceHistoryItemDto> getStudentAttendanceHistory(Long targetUserId, Long subjectId, User requestingUser) {
        validateStudentAccess(targetUserId, requestingUser);

        StudentEnrollment enr = studentEnrollmentRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("No active student enrollment found for user: " + targetUserId));

        List<AttendanceRecord> records;
        if (subjectId != null) {
            records = recordRepository.findByStudentEnrollmentIdAndSubjectId(enr.getId(), subjectId);
        } else {
            records = recordRepository.findByStudentEnrollmentId(enr.getId());
        }

        return records.stream().map(r -> {
            AttendanceSession s = r.getSession();
            return AttendanceHistoryItemDto.builder()
                    .recordId(r.getId())
                    .sessionId(s.getId())
                    .subjectCode(s.getSubject().getCode())
                    .subjectName(s.getSubject().getName())
                    .sessionDate(s.getSessionDate().toString())
                    .sessionTime(s.getStartTime().toString().substring(0, 5) + " - " + s.getEndTime().toString().substring(0, 5))
                    .topicCovered(s.getTopicCovered())
                    .status(r.getStatus())
                    .remarks(r.getRemarks())
                    .facultyName(s.getFacultyUser().getFullName())
                    .hasPendingCorrection(false)
                    .build();
        }).collect(Collectors.toList());
    }

    // -------------------------------------------------------------
    // Faculty Operations (Session creation & Attendance Marking)
    // -------------------------------------------------------------
    @Transactional
    public AttendanceSession createSession(Long sectionId, Long subjectId, LocalDate sessionDate,
                                           LocalTime startTime, LocalTime endTime, String topicCovered,
                                           User facultyUser) {
        validateFacultySectionSubject(facultyUser, sectionId, subjectId);

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));

        if (sessionRepository.findBySectionIdAndSubjectIdAndSessionDateAndStartTime(
                sectionId, subjectId, sessionDate, startTime).isPresent()) {
            throw new IllegalArgumentException("An attendance session already exists for this section, subject, date and time.");
        }

        AttendanceSession session = AttendanceSession.builder()
                .section(section)
                .subject(subject)
                .facultyUser(facultyUser)
                .sessionDate(sessionDate)
                .startTime(startTime)
                .endTime(endTime)
                .topicCovered(topicCovered)
                .sessionStatus("COMPLETED")
                .createdByUser(facultyUser)
                .build();

        return sessionRepository.save(session);
    }

    @Transactional
    public List<AttendanceRecord> recordBatchAttendance(Long sessionId, List<RecordEntryInput> entries, User facultyUser) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        validateFacultySectionSubject(facultyUser, session.getSection().getId(), session.getSubject().getId());

        List<AttendanceRecord> savedRecords = new ArrayList<>();

        for (RecordEntryInput entry : entries) {
            StudentEnrollment enr = studentEnrollmentRepository.findById(entry.getStudentEnrollmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Student enrollment not found: " + entry.getStudentEnrollmentId()));

            Optional<AttendanceRecord> existingOpt = recordRepository.findBySessionIdAndStudentEnrollmentId(sessionId, enr.getId());

            if (existingOpt.isPresent()) {
                AttendanceRecord existing = existingOpt.get();
                AttendanceStatus oldStatus = existing.getStatus();
                existing.setStatus(entry.getStatus());
                existing.setRemarks(entry.getRemarks());
                existing.setUpdatedByUser(facultyUser);
                AttendanceRecord updated = recordRepository.save(existing);
                savedRecords.add(updated);

                auditLogRepository.save(AttendanceAuditLog.builder()
                        .attendanceRecord(updated)
                        .action("UPDATED")
                        .previousStatus(oldStatus.name())
                        .newStatus(entry.getStatus().name())
                        .performedByUser(facultyUser)
                        .reason("Faculty attendance modification")
                        .build());
            } else {
                AttendanceRecord record = AttendanceRecord.builder()
                        .session(session)
                        .studentEnrollment(enr)
                        .status(entry.getStatus())
                        .remarks(entry.getRemarks())
                        .createdByUser(facultyUser)
                        .updatedByUser(facultyUser)
                        .build();

                AttendanceRecord saved = recordRepository.save(record);
                savedRecords.add(saved);

                auditLogRepository.save(AttendanceAuditLog.builder()
                        .attendanceRecord(saved)
                        .action("CREATED")
                        .previousStatus(null)
                        .newStatus(entry.getStatus().name())
                        .performedByUser(facultyUser)
                        .reason("Initial class roll-call")
                        .build());
            }
        }

        return savedRecords;
    }

    // -------------------------------------------------------------
    // Correction Workflow (Student submission & Faculty review)
    // -------------------------------------------------------------
    @Transactional
    public AttendanceCorrectionRequest requestCorrection(Long attendanceRecordId, AttendanceStatus requestedStatus,
                                                         String reason, String documentUrl, User studentUser) {
        AttendanceRecord record = recordRepository.findById(attendanceRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Attendance record not found: " + attendanceRecordId));

        if (!record.getStudentEnrollment().getUser().getId().equals(studentUser.getId()) && studentUser.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Unauthorized: You can only submit attendance corrections for your own records");
        }

        AttendanceCorrectionRequest req = AttendanceCorrectionRequest.builder()
                .attendanceRecord(record)
                .requestedByUser(studentUser)
                .requestedStatus(requestedStatus)
                .reason(reason)
                .supportingDocumentUrl(documentUrl)
                .status("PENDING")
                .build();

        return correctionRequestRepository.save(req);
    }

    @Transactional
    public AttendanceCorrectionRequest reviewCorrection(Long requestId, boolean approve, String remarks, User reviewerUser) {
        AttendanceCorrectionRequest req = correctionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Correction request not found: " + requestId));

        AttendanceRecord record = req.getAttendanceRecord();
        validateFacultySectionSubject(reviewerUser, record.getSession().getSection().getId(), record.getSession().getSubject().getId());

        req.setReviewedByUser(reviewerUser);
        req.setReviewerRemarks(remarks);
        req.setReviewedAt(LocalDateTime.now());

        if (approve) {
            req.setStatus("APPROVED");
            AttendanceStatus oldStatus = record.getStatus();
            record.setStatus(req.getRequestedStatus());
            record.setRemarks("Correction approved by " + reviewerUser.getFullName() + ": " + remarks);
            record.setUpdatedByUser(reviewerUser);
            recordRepository.save(record);

            auditLogRepository.save(AttendanceAuditLog.builder()
                    .attendanceRecord(record)
                    .action("CORRECTION_APPROVED")
                    .previousStatus(oldStatus.name())
                    .newStatus(req.getRequestedStatus().name())
                    .performedByUser(reviewerUser)
                    .reason(remarks != null ? remarks : "Approved student correction")
                    .build());
        } else {
            req.setStatus("REJECTED");
        }

        return correctionRequestRepository.save(req);
    }

    // -------------------------------------------------------------
    // Security & Authorization Guards
    // -------------------------------------------------------------
    private void validateStudentAccess(Long targetUserId, User requestingUser) {
        if (requestingUser == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (requestingUser.getRole() != Role.ROLE_ADMIN && !requestingUser.getId().equals(targetUserId)) {
            log.warn("Access denied: User {} tried to access attendance of {}", requestingUser.getEmail(), targetUserId);
            throw new AccessDeniedException("Unauthorized: You may only view your own attendance records");
        }
    }

    private void validateFacultySectionSubject(User facultyUser, Long sectionId, Long subjectId) {
        if (facultyUser == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (facultyUser.getRole() == Role.ROLE_ADMIN) {
            return; // Admin bypass
        }
        if (facultyUser.getRole() != Role.ROLE_FACULTY) {
            throw new AccessDeniedException("Faculty or Administrator privileges required");
        }

        boolean assigned = facultyAssignmentRepository
                .findByFacultyUserIdAndSubjectIdAndSectionId(facultyUser.getId(), subjectId, sectionId)
                .isPresent();

        if (!assigned) {
            log.warn("Unauthorized attendance operation: Faculty {} is not assigned to section {} and subject {}",
                    facultyUser.getEmail(), sectionId, subjectId);
            throw new AccessDeniedException("Unauthorized: You are not assigned to instruct this section and subject");
        }
    }
}
