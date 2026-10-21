package in.jecrcu.placement.service.integration;

import in.jecrcu.placement.dto.integration.*;
import in.jecrcu.placement.entity.DriveRegistration;
import in.jecrcu.placement.entity.UpcomingDrive;
import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.entity.crt.CrtAttendanceRecord;
import in.jecrcu.placement.entity.crt.CrtAttendanceStatus;
import in.jecrcu.placement.entity.crt.CrtEnrollment;
import in.jecrcu.placement.entity.crt.CrtSession;
import in.jecrcu.placement.entity.erp.AttendanceRecord;
import in.jecrcu.placement.entity.erp.AttendanceSession;
import in.jecrcu.placement.entity.erp.AttendanceStatus;
import in.jecrcu.placement.entity.erp.StudentEnrollment;
import in.jecrcu.placement.entity.erp.Timetable;
import in.jecrcu.placement.entity.integration.PlacementDutyExemption;
import in.jecrcu.placement.entity.integration.PlacementIntegrationAuditLog;
import in.jecrcu.placement.entity.integration.PlacementParticipation;
import in.jecrcu.placement.entity.integration.PlacementParticipationStatus;
import in.jecrcu.placement.entity.integration.PlacementScheduleConflict;
import in.jecrcu.placement.repository.DriveRegistrationRepository;
import in.jecrcu.placement.repository.UpcomingDriveRepository;
import in.jecrcu.placement.repository.UserRepository;
import in.jecrcu.placement.repository.crt.CrtAttendanceRecordRepository;
import in.jecrcu.placement.repository.crt.CrtEnrollmentRepository;
import in.jecrcu.placement.repository.crt.CrtSessionRepository;
import in.jecrcu.placement.repository.erp.AttendanceRecordRepository;
import in.jecrcu.placement.repository.erp.AttendanceSessionRepository;
import in.jecrcu.placement.repository.erp.StudentEnrollmentRepository;
import in.jecrcu.placement.repository.erp.TimetableRepository;
import in.jecrcu.placement.repository.integration.PlacementDutyExemptionRepository;
import in.jecrcu.placement.repository.integration.PlacementIntegrationAuditLogRepository;
import in.jecrcu.placement.repository.integration.PlacementParticipationRepository;
import in.jecrcu.placement.repository.integration.PlacementScheduleConflictRepository;
import in.jecrcu.placement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlacementIntegrationServiceImpl implements PlacementIntegrationService {

    private final UpcomingDriveRepository upcomingDriveRepository;
    private final DriveRegistrationRepository driveRegistrationRepository;
    private final UserRepository userRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final TimetableRepository timetableRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final CrtEnrollmentRepository crtEnrollmentRepository;
    private final CrtSessionRepository crtSessionRepository;
    private final CrtAttendanceRecordRepository crtAttendanceRecordRepository;
    private final PlacementParticipationRepository participationRepository;
    private final PlacementScheduleConflictRepository conflictRepository;
    private final PlacementDutyExemptionRepository exemptionRepository;
    private final PlacementIntegrationAuditLogRepository auditLogRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public List<ScheduleConflictDto> detectAndRecordConflicts(Long upcomingDriveId) {
        UpcomingDrive drive = upcomingDriveRepository.findById(upcomingDriveId)
                .orElseThrow(() -> new IllegalArgumentException("Upcoming drive not found: " + upcomingDriveId));

        List<DriveRegistration> registrations = driveRegistrationRepository.findByUpcomingDriveIdOrderByRegisteredAtDesc(upcomingDriveId);
        List<ScheduleConflictDto> result = new ArrayList<>();

        for (DriveRegistration reg : registrations) {
            User student = reg.getUser();
            if (student == null) continue;

            // Ensure participation record exists
            ensureParticipationExists(drive, student);

            List<ScheduleConflictDto> studentConflicts = detectAndRecordConflictsForStudent(drive, student);
            result.addAll(studentConflicts);
        }

        return result;
    }

    @Override
    @Transactional
    public List<ScheduleConflictDto> detectAndRecordConflictsForStudent(Long upcomingDriveId, Long studentUserId) {
        UpcomingDrive drive = upcomingDriveRepository.findById(upcomingDriveId)
                .orElseThrow(() -> new IllegalArgumentException("Upcoming drive not found: " + upcomingDriveId));
        User student = userRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentUserId));

        ensureParticipationExists(drive, student);
        return detectAndRecordConflictsForStudent(drive, student);
    }

    private List<ScheduleConflictDto> detectAndRecordConflictsForStudent(UpcomingDrive drive, User student) {
        List<ScheduleConflictDto> conflicts = new ArrayList<>();
        LocalDate driveDate = drive.getDriveDate();
        if (driveDate == null) {
            log.info("Upcoming drive {} has no drive date set yet; skipping conflict detection", drive.getId());
            return conflicts;
        }

        LocalTime driveStart = drive.getStartTime() != null ? drive.getStartTime() : LocalTime.of(9, 0);
        LocalTime driveEnd = drive.getEndTime() != null ? drive.getEndTime() : LocalTime.of(17, 0);

        // 1. Regular class conflicts via student section timetable
        Optional<StudentEnrollment> enrollmentOpt = studentEnrollmentRepository.findByUserId(student.getId());
        if (enrollmentOpt.isPresent()) {
            StudentEnrollment enrollment = enrollmentOpt.get();
            if (enrollment.getSection() != null) {
                String dayOfWeek = driveDate.getDayOfWeek().name();
                List<Timetable> sectionTimetables = timetableRepository
                        .findBySectionIdOrderByDayOfWeekAscStartTimeAsc(enrollment.getSection().getId());

                for (Timetable tt : sectionTimetables) {
                    if (tt.getDayOfWeek().equalsIgnoreCase(dayOfWeek)) {
                        // Check time overlap: startA < endB && endA > startB
                        if (tt.getStartTime().isBefore(driveEnd) && tt.getEndTime().isAfter(driveStart)) {
                            boolean exists = conflictRepository.existsByUpcomingDriveIdAndStudentIdAndConflictDateAndConflictType(
                                    drive.getId(), student.getId(), driveDate, "REGULAR_CLASS");
                            if (!exists) {
                                String desc = "Lecture: " + tt.getSubject().getName() + " (" + tt.getSubject().getCode() +
                                        ") " + tt.getStartTime() + "-" + tt.getEndTime();
                                PlacementScheduleConflict conflict = PlacementScheduleConflict.builder()
                                        .upcomingDrive(drive)
                                        .student(student)
                                        .conflictType("REGULAR_CLASS")
                                        .sessionDescription(desc)
                                        .conflictDate(driveDate)
                                        .conflictStart(tt.getStartTime())
                                        .conflictEnd(tt.getEndTime())
                                        .faculty(tt.getFacultyUser())
                                        .studentNotified(false)
                                        .facultyNotified(false)
                                        .detectedAt(LocalDateTime.now())
                                        .build();
                                conflict = conflictRepository.save(conflict);
                                conflicts.add(mapToConflictDto(conflict));
                            }
                        }
                    }
                }
            }
        }

        // 2. CRT Training conflicts via CRT enrollment & scheduled sessions
        Optional<CrtEnrollment> crtEnrollmentOpt = crtEnrollmentRepository.findVerifiedByStudentId(student.getId());
        if (crtEnrollmentOpt.isPresent() && crtEnrollmentOpt.get().getBatch() != null) {
            Long batchId = crtEnrollmentOpt.get().getBatch().getId();
            List<CrtSession> crtSessions = crtSessionRepository.findByBatchIdAndSessionDate(batchId, driveDate);

            for (CrtSession crtSession : crtSessions) {
                if (crtSession.getStartTime().isBefore(driveEnd) && crtSession.getEndTime().isAfter(driveStart)) {
                    boolean exists = conflictRepository.existsByUpcomingDriveIdAndStudentIdAndConflictDateAndConflictType(
                            drive.getId(), student.getId(), driveDate, "CRT_TRAINING");
                    if (!exists) {
                        String desc = "CRT Session: " + crtSession.getTopic() + " (" + crtSession.getTrack().getName() +
                                ") " + crtSession.getStartTime() + "-" + crtSession.getEndTime();
                        PlacementScheduleConflict conflict = PlacementScheduleConflict.builder()
                                .upcomingDrive(drive)
                                .student(student)
                                .conflictType("CRT_TRAINING")
                                .sessionDescription(desc)
                                .conflictDate(driveDate)
                                .conflictStart(crtSession.getStartTime())
                                .conflictEnd(crtSession.getEndTime())
                                .faculty(crtSession.getTrainer())
                                .studentNotified(false)
                                .facultyNotified(false)
                                .detectedAt(LocalDateTime.now())
                                .build();
                        conflict = conflictRepository.save(conflict);
                        conflicts.add(mapToConflictDto(conflict));
                    }
                }
            }
        }

        return conflicts;
    }

    private void ensureParticipationExists(UpcomingDrive drive, User student) {
        participationRepository.findByUpcomingDriveIdAndStudentId(drive.getId(), student.getId())
                .orElseGet(() -> {
                    PlacementParticipation p = PlacementParticipation.builder()
                            .upcomingDrive(drive)
                            .student(student)
                            .verificationStatus(PlacementParticipationStatus.REGISTERED)
                            .verificationMethod("DRIVE_REGISTRATION")
                            .remarks("Student registered for drive")
                            .build();
                    return participationRepository.save(p);
                });
    }

    @Override
    @Transactional
    public int sendAdvanceConflictNotifications(Long upcomingDriveId, boolean forceResend) {
        List<PlacementScheduleConflict> conflicts = conflictRepository.findByUpcomingDriveId(upcomingDriveId);
        int sentCount = 0;

        for (PlacementScheduleConflict conflict : conflicts) {
            boolean shouldNotifyStudent = forceResend || !Boolean.TRUE.equals(conflict.getStudentNotified());
            boolean shouldNotifyFaculty = forceResend || !Boolean.TRUE.equals(conflict.getFacultyNotified());

            if (shouldNotifyStudent && conflict.getStudent() != null) {
                String title = "Placement Schedule Conflict: " + conflict.getUpcomingDrive().getCompanyName();
                String msg = String.format("Advance notice: Your registered placement drive for %s on %s (%s to %s) conflicts with your scheduled %s. " +
                                "If you attend the drive, official duty exemption will be credited upon check-in verification.",
                        conflict.getUpcomingDrive().getCompanyName(),
                        conflict.getConflictDate(),
                        conflict.getConflictStart(),
                        conflict.getConflictEnd(),
                        conflict.getSessionDescription());

                notificationService.sendNotification(
                        conflict.getStudent(),
                        title,
                        msg,
                        "/erp",
                        "SCHEDULE_CONFLICT");
                conflict.setStudentNotified(true);
                sentCount++;
            }

            if (shouldNotifyFaculty && conflict.getFaculty() != null) {
                String title = "Student Placement Drive Overlap: " + conflict.getStudent().getFullName();
                String msg = String.format("Advance notice: Student %s (%s) is registered for %s placement drive on %s (%s to %s) overlapping with %s.",
                        conflict.getStudent().getFullName(),
                        getStudentRollNumber(conflict.getStudent()),
                        conflict.getUpcomingDrive().getCompanyName(),
                        conflict.getConflictDate(),
                        conflict.getConflictStart(),
                        conflict.getConflictEnd(),
                        conflict.getSessionDescription());

                notificationService.sendNotification(
                        conflict.getFaculty(),
                        title,
                        msg,
                        "/erp",
                        "STUDENT_DRIVE_CONFLICT");
                conflict.setFacultyNotified(true);
                sentCount++;
            }

            conflictRepository.save(conflict);
        }

        return sentCount;
    }

    @Override
    @Transactional
    public boolean sendNotificationForConflict(Long conflictId) {
        PlacementScheduleConflict conflict = conflictRepository.findById(conflictId)
                .orElseThrow(() -> new IllegalArgumentException("Conflict not found: " + conflictId));

        if (conflict.getStudent() != null) {
            String title = "Placement Schedule Conflict: " + conflict.getUpcomingDrive().getCompanyName();
            String msg = String.format("Your registered placement drive for %s on %s (%s to %s) conflicts with %s.",
                    conflict.getUpcomingDrive().getCompanyName(),
                    conflict.getConflictDate(),
                    conflict.getConflictStart(),
                    conflict.getConflictEnd(),
                    conflict.getSessionDescription());

            notificationService.sendNotification(conflict.getStudent(), title, msg, "/erp", "SCHEDULE_CONFLICT");
            conflict.setStudentNotified(true);
        }

        if (conflict.getFaculty() != null) {
            String title = "Student Placement Drive Overlap: " + conflict.getStudent().getFullName();
            String msg = String.format("Student %s is registered for %s drive overlapping %s.",
                    conflict.getStudent().getFullName(),
                    conflict.getUpcomingDrive().getCompanyName(),
                    conflict.getSessionDescription());

            notificationService.sendNotification(conflict.getFaculty(), title, msg, "/erp", "STUDENT_DRIVE_CONFLICT");
            conflict.setFacultyNotified(true);
        }

        conflictRepository.save(conflict);
        return true;
    }

    @Override
    @Transactional
    public PlacementParticipationDto verifyStudentParticipation(ParticipationVerifyRequestDto request, Long verifiedByUserId) {
        PlacementParticipation participation;
        if (request.getParticipationId() != null) {
            participation = participationRepository.findById(request.getParticipationId())
                    .orElseThrow(() -> new IllegalArgumentException("Participation record not found: " + request.getParticipationId()));
        } else if (request.getUpcomingDriveId() != null && request.getStudentId() != null) {
            participation = participationRepository.findByUpcomingDriveIdAndStudentId(request.getUpcomingDriveId(), request.getStudentId())
                    .orElseGet(() -> {
                        UpcomingDrive drive = upcomingDriveRepository.findById(request.getUpcomingDriveId())
                                .orElseThrow(() -> new IllegalArgumentException("Drive not found: " + request.getUpcomingDriveId()));
                        User student = userRepository.findById(request.getStudentId())
                                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + request.getStudentId()));
                        return PlacementParticipation.builder()
                                .upcomingDrive(drive)
                                .student(student)
                                .build();
                    });
        } else {
            throw new IllegalArgumentException("Must specify either participationId or upcomingDriveId and studentId");
        }

        User verifiedBy = userRepository.findById(verifiedByUserId)
                .orElseThrow(() -> new IllegalArgumentException("VerifiedBy user not found: " + verifiedByUserId));

        String prevState = participation.getVerificationStatus() != null ? participation.getVerificationStatus().name() : "NONE";
        PlacementParticipationStatus newStatus = request.getStatus() != null ? request.getStatus() : PlacementParticipationStatus.ATTENDED_VERIFIED;

        participation.setVerificationStatus(newStatus);
        participation.setVerificationSource(request.getVerificationSource() != null ? request.getVerificationSource() : "PLACEMENT_OFFICE_PORTAL");
        participation.setVerifiedAt(LocalDateTime.now());
        participation.setVerifiedBy(verifiedBy);
        if (request.getRemarks() != null) {
            participation.setRemarks(request.getRemarks());
        }

        participation = participationRepository.save(participation);

        // Audit log
        PlacementIntegrationAuditLog auditLog = PlacementIntegrationAuditLog.builder()
                .entityType("PARTICIPATION")
                .entityId(participation.getId())
                .action("VERIFIED_" + newStatus.name())
                .previousState(prevState)
                .newState(newStatus.name())
                .performedBy(verifiedBy)
                .reason(request.getRemarks())
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        // If verified attended and autoApply is requested, trigger duty exemption
        if (request.isAutoApplyDutyExemption() && newStatus == PlacementParticipationStatus.ATTENDED_VERIFIED) {
            applyDutyExemptionForParticipation(participation.getId(), verifiedByUserId, "Auto-applied on check-in verification");
        }

        return mapToParticipationDto(participation);
    }

    @Override
    @Transactional
    public List<DutyExemptionDto> applyDutyExemptionForParticipation(Long participationId, Long approvedByUserId, String remarks) {
        PlacementParticipation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new IllegalArgumentException("Participation record not found: " + participationId));

        // Step 3 & 4: Registration alone must not prove participation!
        if (participation.getVerificationStatus() != PlacementParticipationStatus.ATTENDED_VERIFIED) {
            throw new IllegalStateException("Cannot grant placement duty exemption: Student participation is not verified (Current status: "
                    + participation.getVerificationStatus() + ")");
        }

        User approvedBy = userRepository.findById(approvedByUserId)
                .orElseThrow(() -> new IllegalArgumentException("ApprovedBy user not found: " + approvedByUserId));

        UpcomingDrive drive = participation.getUpcomingDrive();
        User student = participation.getStudent();
        LocalDate driveDate = drive.getDriveDate();

        List<PlacementScheduleConflict> conflicts = conflictRepository.findByStudentIdAndConflictDate(student.getId(), driveDate);
        List<DutyExemptionDto> exemptions = new ArrayList<>();

        for (PlacementScheduleConflict conflict : conflicts) {
            if ("REGULAR_CLASS".equalsIgnoreCase(conflict.getConflictType())) {
                // Find regular class attendance session and record
                Optional<StudentEnrollment> enrollmentOpt = studentEnrollmentRepository.findByUserId(student.getId());
                if (enrollmentOpt.isPresent()) {
                    StudentEnrollment enrollment = enrollmentOpt.get();
                    List<AttendanceSession> sessions = attendanceSessionRepository.findBySectionIdAndSessionDate(
                            enrollment.getSection().getId(), driveDate);

                    for (AttendanceSession session : sessions) {
                        // Check if session falls within conflict window
                        if (session.getStartTime().isBefore(conflict.getConflictEnd()) &&
                                session.getEndTime().isAfter(conflict.getConflictStart())) {

                            AttendanceRecord record = attendanceRecordRepository.findBySessionIdAndStudentEnrollmentId(
                                    session.getId(), enrollment.getId()).orElse(null);

                            if (record != null) {
                                // Check if exemption already granted
                                Optional<PlacementDutyExemption> existingExemption = exemptionRepository.findByAttendanceRecordId(record.getId());
                                if (existingExemption.isPresent() && "GRANTED".equals(existingExemption.get().getExemptionStatus())) {
                                    exemptions.add(mapToExemptionDto(existingExemption.get()));
                                    continue;
                                }

                                String originalStatus = record.getStatus().name();
                                // Crucial rule: Do not falsely mark physically present! Set to OFFICIAL_DUTY.
                                record.setStatus(AttendanceStatus.OFFICIAL_DUTY);
                                record.setRemarks("Placement Duty - " + drive.getCompanyName());
                                record.setUpdatedByUser(approvedBy);
                                record = attendanceRecordRepository.save(record);

                                PlacementDutyExemption exemption = PlacementDutyExemption.builder()
                                        .participation(participation)
                                        .student(student)
                                        .targetType("REGULAR_CLASS")
                                        .attendanceRecord(record)
                                        .conflictDate(driveDate)
                                        .startTime(conflict.getConflictStart())
                                        .endTime(conflict.getConflictEnd())
                                        .originalStatus(originalStatus)
                                        .exemptionStatus("GRANTED")
                                        .dutyPolicyCode("INST_PLACEMENT_DUTY_V1")
                                        .approvedBy(approvedBy)
                                        .approvedAt(LocalDateTime.now())
                                        .remarks(remarks != null ? remarks : "Authorized placement drive duty exemption")
                                        .createdAt(LocalDateTime.now())
                                        .build();
                                exemption = exemptionRepository.save(exemption);

                                // Audit log
                                auditLogRepository.save(PlacementIntegrationAuditLog.builder()
                                        .entityType("EXEMPTION")
                                        .entityId(exemption.getId())
                                        .action("EXEMPTION_GRANTED")
                                        .previousState(originalStatus)
                                        .newState("OFFICIAL_DUTY")
                                        .performedBy(approvedBy)
                                        .reason("Placement Duty for " + drive.getCompanyName())
                                        .createdAt(LocalDateTime.now())
                                        .build());

                                exemptions.add(mapToExemptionDto(exemption));
                            }
                        }
                    }
                }
            } else if ("CRT_TRAINING".equalsIgnoreCase(conflict.getConflictType())) {
                // Step 4: For CRT conflicts, check verified CRT enrollment
                Optional<CrtEnrollment> crtEnrollmentOpt = crtEnrollmentRepository.findVerifiedByStudentId(student.getId());
                if (crtEnrollmentOpt.isPresent() && crtEnrollmentOpt.get().getBatch() != null) {
                    List<CrtSession> crtSessions = crtSessionRepository.findByBatchIdAndSessionDate(
                            crtEnrollmentOpt.get().getBatch().getId(), driveDate);

                    for (CrtSession crtSession : crtSessions) {
                        if (crtSession.getStartTime().isBefore(conflict.getConflictEnd()) &&
                                crtSession.getEndTime().isAfter(conflict.getConflictStart())) {

                            Optional<CrtAttendanceRecord> crtRecordOpt = crtAttendanceRecordRepository.findBySessionIdAndStudentId(
                                    crtSession.getId(), student.getId());

                            if (crtRecordOpt.isPresent()) {
                                CrtAttendanceRecord crtRecord = crtRecordOpt.get();

                                Optional<PlacementDutyExemption> existingExemption = exemptionRepository.findByCrtAttendanceRecordId(crtRecord.getId());
                                if (existingExemption.isPresent() && "GRANTED".equals(existingExemption.get().getExemptionStatus())) {
                                    exemptions.add(mapToExemptionDto(existingExemption.get()));
                                    continue;
                                }

                                String originalStatus = crtRecord.getStatus().name();
                                // Crucial rule: Never grant CRT attendance (PRESENT) solely because a placement drive occurred!
                                // Mark as EXCUSED with approved duty policy.
                                crtRecord.setStatus(CrtAttendanceStatus.EXCUSED);
                                crtRecord.setRemarks("Placement Drive Exemption - " + drive.getCompanyName());
                                crtRecord = crtAttendanceRecordRepository.save(crtRecord);

                                PlacementDutyExemption exemption = PlacementDutyExemption.builder()
                                        .participation(participation)
                                        .student(student)
                                        .targetType("CRT_TRAINING")
                                        .crtAttendanceRecord(crtRecord)
                                        .conflictDate(driveDate)
                                        .startTime(conflict.getConflictStart())
                                        .endTime(conflict.getConflictEnd())
                                        .originalStatus(originalStatus)
                                        .exemptionStatus("GRANTED")
                                        .dutyPolicyCode("INST_CRT_PLACEMENT_DUTY_V1")
                                        .approvedBy(approvedBy)
                                        .approvedAt(LocalDateTime.now())
                                        .remarks(remarks != null ? remarks : "Authorized CRT exemption for placement drive")
                                        .createdAt(LocalDateTime.now())
                                        .build();
                                exemption = exemptionRepository.save(exemption);

                                // Audit log
                                auditLogRepository.save(PlacementIntegrationAuditLog.builder()
                                        .entityType("EXEMPTION")
                                        .entityId(exemption.getId())
                                        .action("EXEMPTION_GRANTED")
                                        .previousState(originalStatus)
                                        .newState("EXCUSED")
                                        .performedBy(approvedBy)
                                        .reason("CRT Exemption for " + drive.getCompanyName())
                                        .createdAt(LocalDateTime.now())
                                        .build());

                                exemptions.add(mapToExemptionDto(exemption));
                            }
                        }
                    }
                }
            }
        }

        return exemptions;
    }

    @Override
    @Transactional
    public DutyExemptionDto revokeDutyExemption(Long exemptionId, Long revokedByUserId, String reason) {
        PlacementDutyExemption exemption = exemptionRepository.findById(exemptionId)
                .orElseThrow(() -> new IllegalArgumentException("Duty exemption not found: " + exemptionId));

        User actor = userRepository.findById(revokedByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Actor user not found: " + revokedByUserId));

        if ("REVOKED".equals(exemption.getExemptionStatus())) {
            return mapToExemptionDto(exemption);
        }

        // Restore original regular attendance status
        if ("REGULAR_CLASS".equalsIgnoreCase(exemption.getTargetType()) && exemption.getAttendanceRecord() != null) {
            AttendanceRecord record = exemption.getAttendanceRecord();
            AttendanceStatus original = AttendanceStatus.ABSENT;
            try {
                if (exemption.getOriginalStatus() != null) {
                    original = AttendanceStatus.valueOf(exemption.getOriginalStatus());
                }
            } catch (Exception ignored) {}

            record.setStatus(original);
            record.setRemarks("Placement duty exemption revoked: " + (reason != null ? reason : "Administrative correction"));
            record.setUpdatedByUser(actor);
            attendanceRecordRepository.save(record);
        }

        // Restore original CRT attendance status
        if ("CRT_TRAINING".equalsIgnoreCase(exemption.getTargetType()) && exemption.getCrtAttendanceRecord() != null) {
            CrtAttendanceRecord record = exemption.getCrtAttendanceRecord();
            CrtAttendanceStatus original = CrtAttendanceStatus.ABSENT;
            try {
                if (exemption.getOriginalStatus() != null) {
                    original = CrtAttendanceStatus.valueOf(exemption.getOriginalStatus());
                }
            } catch (Exception ignored) {}

            record.setStatus(original);
            record.setRemarks("CRT exemption revoked: " + (reason != null ? reason : "Administrative correction"));
            crtAttendanceRecordRepository.save(record);
        }

        exemption.setExemptionStatus("REVOKED");
        exemption.setRemarks("Revoked: " + (reason != null ? reason : "Administrative review"));
        exemption = exemptionRepository.save(exemption);

        // Audit log
        auditLogRepository.save(PlacementIntegrationAuditLog.builder()
                .entityType("EXEMPTION")
                .entityId(exemption.getId())
                .action("EXEMPTION_REVOKED")
                .previousState("GRANTED")
                .newState("REVOKED")
                .performedBy(actor)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build());

        return mapToExemptionDto(exemption);
    }

    @Override
    public IntegrationOverviewDto getOverview() {
        long totalDrives = upcomingDriveRepository.count();
        long totalRegistrations = driveRegistrationRepository.count();
        long totalVerified = participationRepository.findAll().stream()
                .filter(p -> p.getVerificationStatus() == PlacementParticipationStatus.ATTENDED_VERIFIED)
                .count();
        long totalConflicts = conflictRepository.count();
        long totalExemptions = exemptionRepository.findAll().stream()
                .filter(e -> "GRANTED".equals(e.getExemptionStatus()))
                .count();

        List<PlacementParticipationDto> recentParticipations = participationRepository.findAll().stream()
                .sorted(Comparator.comparing(PlacementParticipation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(this::mapToParticipationDto)
                .collect(Collectors.toList());

        List<ScheduleConflictDto> upcomingConflicts = conflictRepository.findAll().stream()
                .sorted(Comparator.comparing(PlacementScheduleConflict::getConflictDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(this::mapToConflictDto)
                .collect(Collectors.toList());

        List<DutyExemptionDto> recentExemptions = exemptionRepository.findAll().stream()
                .sorted(Comparator.comparing(PlacementDutyExemption::getApprovedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(this::mapToExemptionDto)
                .collect(Collectors.toList());

        return IntegrationOverviewDto.builder()
                .totalDrivesWithTimings(totalDrives)
                .totalRegistrations(totalRegistrations)
                .totalVerifiedParticipants(totalVerified)
                .totalConflictsDetected(totalConflicts)
                .totalDutyExemptionsGranted(totalExemptions)
                .recentParticipations(recentParticipations)
                .upcomingConflicts(upcomingConflicts)
                .recentExemptions(recentExemptions)
                .build();
    }

    @Override
    public List<ScheduleConflictDto> getConflicts(Long driveId, Long studentId, Long facultyId) {
        List<PlacementScheduleConflict> list;
        if (driveId != null) {
            list = conflictRepository.findByUpcomingDriveId(driveId);
        } else if (studentId != null) {
            list = conflictRepository.findByStudentId(studentId);
        } else if (facultyId != null) {
            list = conflictRepository.findByFacultyId(facultyId);
        } else {
            list = conflictRepository.findAll();
        }

        return list.stream()
                .sorted(Comparator.comparing(PlacementScheduleConflict::getConflictDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToConflictDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PlacementParticipationDto> getParticipationsByDrive(Long driveId) {
        return participationRepository.findByUpcomingDriveId(driveId).stream()
                .map(this::mapToParticipationDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PlacementParticipationDto> getParticipationsByStudent(Long studentId) {
        return participationRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(this::mapToParticipationDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<DutyExemptionDto> getExemptionsByStudent(Long studentId) {
        return exemptionRepository.findByStudentIdOrderByConflictDateDesc(studentId).stream()
                .map(this::mapToExemptionDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<DutyExemptionDto> getAllExemptions() {
        return exemptionRepository.findAll().stream()
                .sorted(Comparator.comparing(PlacementDutyExemption::getApprovedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToExemptionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void handleDriveTimingChangeOrCancellation(Long upcomingDriveId, String action, String reason, Long actorUserId) {
        UpcomingDrive drive = upcomingDriveRepository.findById(upcomingDriveId)
                .orElseThrow(() -> new IllegalArgumentException("Drive not found: " + upcomingDriveId));

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Actor not found: " + actorUserId));

        List<PlacementParticipation> participations = participationRepository.findByUpcomingDriveId(upcomingDriveId);

        for (PlacementParticipation p : participations) {
            if ("CANCELLED".equalsIgnoreCase(action)) {
                String title = "Placement Drive Cancelled: " + drive.getCompanyName();
                String msg = "The placement drive for " + drive.getCompanyName() + " scheduled on " + drive.getDriveDate() +
                        " has been cancelled. Reason: " + (reason != null ? reason : "Operational schedule change");
                notificationService.sendNotification(p.getStudent(), title, msg, "/drives", "DRIVE_CANCELLED");
            } else if ("RESCHEDULED".equalsIgnoreCase(action)) {
                String title = "Placement Drive Rescheduled: " + drive.getCompanyName();
                String msg = "The placement drive for " + drive.getCompanyName() + " has been rescheduled to " +
                        drive.getDriveDate() + " (" + drive.getStartTime() + " to " + drive.getEndTime() + "). Venue: " + drive.getVenue();
                notificationService.sendNotification(p.getStudent(), title, msg, "/drives", "DRIVE_RESCHEDULED");
            }
        }

        // Log audit
        auditLogRepository.save(PlacementIntegrationAuditLog.builder()
                .entityType("UPCOMING_DRIVE")
                .entityId(drive.getId())
                .action("DRIVE_" + action.toUpperCase())
                .performedBy(actor)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build());

        // Re-detect conflicts if rescheduled
        if ("RESCHEDULED".equalsIgnoreCase(action)) {
            detectAndRecordConflicts(upcomingDriveId);
        }
    }

    private ScheduleConflictDto mapToConflictDto(PlacementScheduleConflict c) {
        return ScheduleConflictDto.builder()
                .id(c.getId())
                .upcomingDriveId(c.getUpcomingDrive() != null ? c.getUpcomingDrive().getId() : null)
                .companyName(c.getUpcomingDrive() != null ? c.getUpcomingDrive().getCompanyName() : null)
                .studentId(c.getStudent() != null ? c.getStudent().getId() : null)
                .studentName(c.getStudent() != null ? c.getStudent().getFullName() : null)
                .studentRollNumber(getStudentRollNumber(c.getStudent()))
                .conflictType(c.getConflictType())
                .sessionDescription(c.getSessionDescription())
                .conflictDate(c.getConflictDate())
                .conflictStart(c.getConflictStart())
                .conflictEnd(c.getConflictEnd())
                .facultyId(c.getFaculty() != null ? c.getFaculty().getId() : null)
                .facultyName(c.getFaculty() != null ? c.getFaculty().getFullName() : null)
                .studentNotified(c.getStudentNotified())
                .facultyNotified(c.getFacultyNotified())
                .detectedAt(c.getDetectedAt())
                .build();
    }

    private PlacementParticipationDto mapToParticipationDto(PlacementParticipation p) {
        UpcomingDrive drive = p.getUpcomingDrive();
        User student = p.getStudent();
        User verifiedBy = p.getVerifiedBy();

        return PlacementParticipationDto.builder()
                .id(p.getId())
                .upcomingDriveId(drive != null ? drive.getId() : null)
                .companyName(drive != null ? drive.getCompanyName() : null)
                .jobRole(drive != null ? drive.getRolesOffered() : null)
                .driveDate(drive != null ? drive.getDriveDate() : null)
                .startTime(drive != null ? drive.getStartTime() : null)
                .endTime(drive != null ? drive.getEndTime() : null)
                .venue(drive != null ? drive.getVenue() : null)
                .studentId(student != null ? student.getId() : null)
                .studentName(student != null ? student.getFullName() : null)
                .studentRollNumber(getStudentRollNumber(student))
                .studentEmail(student != null ? student.getEmail() : null)
                .verificationStatus(p.getVerificationStatus())
                .verificationSource(p.getVerificationSource())
                .verifiedAt(p.getVerifiedAt())
                .verifiedByName(verifiedBy != null ? verifiedBy.getFullName() : null)
                .remarks(p.getRemarks())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private DutyExemptionDto mapToExemptionDto(PlacementDutyExemption e) {
        PlacementParticipation p = e.getParticipation();
        UpcomingDrive drive = (p != null) ? p.getUpcomingDrive() : null;
        User student = e.getStudent();
        User approvedBy = e.getApprovedBy();

        return DutyExemptionDto.builder()
                .id(e.getId())
                .participationId(p != null ? p.getId() : null)
                .upcomingDriveId(drive != null ? drive.getId() : null)
                .companyName(drive != null ? drive.getCompanyName() : null)
                .studentId(student != null ? student.getId() : null)
                .studentName(student != null ? student.getFullName() : null)
                .studentRollNumber(getStudentRollNumber(student))
                .targetType(e.getTargetType())
                .attendanceRecordId(e.getAttendanceRecord() != null ? e.getAttendanceRecord().getId() : null)
                .crtAttendanceRecordId(e.getCrtAttendanceRecord() != null ? e.getCrtAttendanceRecord().getId() : null)
                .conflictDate(e.getConflictDate())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .originalStatus(e.getOriginalStatus())
                .exemptionStatus(e.getExemptionStatus())
                .dutyPolicyCode(e.getDutyPolicyCode())
                .approvedById(approvedBy != null ? approvedBy.getId() : null)
                .approvedByName(approvedBy != null ? approvedBy.getFullName() : null)
                .approvedAt(e.getApprovedAt())
                .remarks(e.getRemarks())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private String getStudentRollNumber(User student) {
        if (student == null) return null;
        return studentEnrollmentRepository.findByUserId(student.getId())
                .map(StudentEnrollment::getRollNumber)
                .orElse(student.getEmail());
    }
}
