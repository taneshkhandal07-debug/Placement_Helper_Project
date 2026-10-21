package in.jecrcu.placement;

import in.jecrcu.placement.dto.integration.ParticipationVerifyRequestDto;
import in.jecrcu.placement.dto.integration.PlacementParticipationDto;
import in.jecrcu.placement.dto.integration.ScheduleConflictDto;
import in.jecrcu.placement.dto.integration.DutyExemptionDto;
import in.jecrcu.placement.entity.DriveRegistration;
import in.jecrcu.placement.entity.UpcomingDrive;
import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.entity.crt.*;
import in.jecrcu.placement.entity.enums.Role;
import in.jecrcu.placement.entity.erp.*;
import in.jecrcu.placement.entity.integration.PlacementDutyExemption;
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
import in.jecrcu.placement.service.integration.PlacementIntegrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlacementIntegrationServiceTest {

    @Mock private UpcomingDriveRepository upcomingDriveRepository;
    @Mock private DriveRegistrationRepository driveRegistrationRepository;
    @Mock private UserRepository userRepository;
    @Mock private StudentEnrollmentRepository studentEnrollmentRepository;
    @Mock private TimetableRepository timetableRepository;
    @Mock private AttendanceSessionRepository attendanceSessionRepository;
    @Mock private AttendanceRecordRepository attendanceRecordRepository;
    @Mock private CrtEnrollmentRepository crtEnrollmentRepository;
    @Mock private CrtSessionRepository crtSessionRepository;
    @Mock private CrtAttendanceRecordRepository crtAttendanceRecordRepository;
    @Mock private PlacementParticipationRepository participationRepository;
    @Mock private PlacementScheduleConflictRepository conflictRepository;
    @Mock private PlacementDutyExemptionRepository exemptionRepository;
    @Mock private PlacementIntegrationAuditLogRepository auditLogRepository;

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
    private PlacementIntegrationServiceImpl integrationService;

    private User student;
    private User faculty;
    private User admin;
    private UpcomingDrive drive;
    private Section sectionA;
    private Subject subject;
    private StudentEnrollment enrollment;

    @BeforeEach
    void setUp() {
        notificationService = new FakeNotificationService();
        integrationService = new PlacementIntegrationServiceImpl(
                upcomingDriveRepository,
                driveRegistrationRepository,
                userRepository,
                studentEnrollmentRepository,
                timetableRepository,
                attendanceSessionRepository,
                attendanceRecordRepository,
                crtEnrollmentRepository,
                crtSessionRepository,
                crtAttendanceRecordRepository,
                participationRepository,
                conflictRepository,
                exemptionRepository,
                auditLogRepository,
                notificationService
        );

        student = User.builder().id(101L).fullName("Rahul Sharma").email("rahul@jecrcu.edu.in").role(Role.ROLE_STUDENT).build();
        faculty = User.builder().id(201L).fullName("Dr. Sharma").email("faculty@jecrcu.edu.in").role(Role.ROLE_FACULTY).build();
        admin = User.builder().id(301L).fullName("Placement Officer").email("tpo@jecrcu.edu.in").role(Role.ROLE_ADMIN).build();

        // Target drive date: 2026-11-02 (A Monday)
        drive = UpcomingDrive.builder()
                .id(1L)
                .companyName("Microsoft")
                .rolesOffered("Software Engineer")
                .driveDate(LocalDate.of(2026, 11, 2))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .venue("Auditorium Hall 1")
                .build();

        sectionA = Section.builder().id(10L).name("Section A").build();
        subject = Subject.builder().id(20L).name("Cloud Computing").code("CS701").build();
        enrollment = StudentEnrollment.builder().id(30L).user(student).section(sectionA).build();
    }

    @Test
    @DisplayName("Should detect regular class conflict when timetable overlaps with drive hours")
    void testDetectAndRecordConflicts_RegularClass() {
        DriveRegistration reg = DriveRegistration.builder().id(1L).upcomingDrive(drive).user(student).status("REGISTERED").build();
        when(upcomingDriveRepository.findById(1L)).thenReturn(Optional.of(drive));
        when(driveRegistrationRepository.findByUpcomingDriveIdOrderByRegisteredAtDesc(1L)).thenReturn(List.of(reg));
        when(participationRepository.findByUpcomingDriveIdAndStudentId(1L, 101L)).thenReturn(Optional.empty());
        when(participationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(studentEnrollmentRepository.findByUserId(101L)).thenReturn(Optional.of(enrollment));

        Timetable tt = Timetable.builder()
                .id(50L)
                .section(sectionA)
                .subject(subject)
                .facultyUser(faculty)
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build();
        when(timetableRepository.findBySectionIdOrderByDayOfWeekAscStartTimeAsc(10L)).thenReturn(List.of(tt));
        when(conflictRepository.existsByUpcomingDriveIdAndStudentIdAndConflictDateAndConflictType(
                eq(1L), eq(101L), eq(drive.getDriveDate()), eq("REGULAR_CLASS"))).thenReturn(false);

        when(conflictRepository.save(any())).thenAnswer(inv -> {
            PlacementScheduleConflict c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        when(crtEnrollmentRepository.findVerifiedByStudentId(101L)).thenReturn(Optional.empty());

        List<ScheduleConflictDto> conflicts = integrationService.detectAndRecordConflicts(1L);

        assertEquals(1, conflicts.size());
        assertEquals("REGULAR_CLASS", conflicts.get(0).getConflictType());
        assertEquals(101L, conflicts.get(0).getStudentId());
        assertEquals(201L, conflicts.get(0).getFacultyId());
        verify(conflictRepository, times(1)).save(any(PlacementScheduleConflict.class));
    }

    @Test
    @DisplayName("Should detect CRT conflict for verified CRT enrolled student")
    void testDetectAndRecordConflicts_CrtSession() {
        DriveRegistration reg = DriveRegistration.builder().id(1L).upcomingDrive(drive).user(student).status("REGISTERED").build();
        when(upcomingDriveRepository.findById(1L)).thenReturn(Optional.of(drive));
        when(driveRegistrationRepository.findByUpcomingDriveIdOrderByRegisteredAtDesc(1L)).thenReturn(List.of(reg));
        when(participationRepository.findByUpcomingDriveIdAndStudentId(1L, 101L)).thenReturn(Optional.empty());
        when(participationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(studentEnrollmentRepository.findByUserId(101L)).thenReturn(Optional.empty());

        CrtBatch batch = CrtBatch.builder().id(500L).name("CRT Batch 1").build();
        CrtEnrollment crtEnrollment = CrtEnrollment.builder().id(501L).student(student).batch(batch).status(CrtEnrollmentStatus.VERIFIED).build();
        when(crtEnrollmentRepository.findVerifiedByStudentId(101L)).thenReturn(Optional.of(crtEnrollment));

        CrtTrack track = CrtTrack.builder().id(1L).name("Quantitative Aptitude").code("APT-101").build();
        CrtSession crtSession = CrtSession.builder()
                .id(600L)
                .batch(batch)
                .track(track)
                .trainer(faculty)
                .sessionDate(drive.getDriveDate())
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .topic("Advanced Number Systems")
                .build();
        when(crtSessionRepository.findByBatchIdAndSessionDate(500L, drive.getDriveDate())).thenReturn(List.of(crtSession));
        when(conflictRepository.existsByUpcomingDriveIdAndStudentIdAndConflictDateAndConflictType(
                eq(1L), eq(101L), eq(drive.getDriveDate()), eq("CRT_TRAINING"))).thenReturn(false);

        when(conflictRepository.save(any())).thenAnswer(inv -> {
            PlacementScheduleConflict c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        List<ScheduleConflictDto> conflicts = integrationService.detectAndRecordConflicts(1L);

        assertEquals(1, conflicts.size());
        assertEquals("CRT_TRAINING", conflicts.get(0).getConflictType());
        verify(conflictRepository, times(1)).save(any(PlacementScheduleConflict.class));
    }

    @Test
    @DisplayName("Should send advance notifications and avoid duplicates")
    void testSendAdvanceConflictNotifications() {
        PlacementScheduleConflict conflict = PlacementScheduleConflict.builder()
                .id(1L)
                .upcomingDrive(drive)
                .student(student)
                .faculty(faculty)
                .conflictType("REGULAR_CLASS")
                .sessionDescription("Cloud Computing Lecture")
                .conflictDate(drive.getDriveDate())
                .conflictStart(LocalTime.of(10, 0))
                .conflictEnd(LocalTime.of(11, 0))
                .studentNotified(false)
                .facultyNotified(false)
                .build();

        when(conflictRepository.findByUpcomingDriveId(1L)).thenReturn(List.of(conflict));

        int sent = integrationService.sendAdvanceConflictNotifications(1L, false);

        assertEquals(2, sent); // 1 to student + 1 to faculty
        assertEquals(2, notificationService.sentCount);
        assertTrue(conflict.getStudentNotified());
        assertTrue(conflict.getFacultyNotified());

        // Second call with forceResend = false should NOT send any more notifications (deduplication)
        int secondSent = integrationService.sendAdvanceConflictNotifications(1L, false);
        assertEquals(0, secondSent);
        assertEquals(2, notificationService.sentCount);
    }

    @Test
    @DisplayName("Registration alone must not grant duty exemption")
    void testRegistrationAloneDoesNotGrantExemption() {
        PlacementParticipation registeredParticipation = PlacementParticipation.builder()
                .id(10L)
                .upcomingDrive(drive)
                .student(student)
                .verificationStatus(PlacementParticipationStatus.REGISTERED)
                .build();

        when(participationRepository.findById(10L)).thenReturn(Optional.of(registeredParticipation));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                integrationService.applyDutyExemptionForParticipation(10L, 301L, "Attempt duty"));

        assertTrue(ex.getMessage().contains("Student participation is not verified"));
        verifyNoInteractions(attendanceRecordRepository);
    }

    @Test
    @DisplayName("Verification updates participation status and is logged in audit trail")
    void testVerifyStudentParticipation() {
        PlacementParticipation participation = PlacementParticipation.builder()
                .id(10L)
                .upcomingDrive(drive)
                .student(student)
                .verificationStatus(PlacementParticipationStatus.REGISTERED)
                .build();

        when(participationRepository.findById(10L)).thenReturn(Optional.of(participation));
        when(userRepository.findById(301L)).thenReturn(Optional.of(admin));
        when(participationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ParticipationVerifyRequestDto request = ParticipationVerifyRequestDto.builder()
                .participationId(10L)
                .status(PlacementParticipationStatus.ATTENDED_VERIFIED)
                .verificationSource("PLACEMENT_OFFICER_DESK")
                .remarks("Student checked in at auditorium desk")
                .autoApplyDutyExemption(false)
                .build();

        PlacementParticipationDto dto = integrationService.verifyStudentParticipation(request, 301L);

        assertEquals(PlacementParticipationStatus.ATTENDED_VERIFIED, dto.getVerificationStatus());
        assertEquals("PLACEMENT_OFFICER_DESK", dto.getVerificationSource());
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Verified participation grants OFFICIAL_DUTY for regular class - never marks PRESENT")
    void testApplyDutyExemption_RegularClass_SetsOfficialDuty_NeverPresent() {
        PlacementParticipation verifiedParticipation = PlacementParticipation.builder()
                .id(10L)
                .upcomingDrive(drive)
                .student(student)
                .verificationStatus(PlacementParticipationStatus.ATTENDED_VERIFIED)
                .build();

        when(participationRepository.findById(10L)).thenReturn(Optional.of(verifiedParticipation));
        when(userRepository.findById(301L)).thenReturn(Optional.of(admin));

        PlacementScheduleConflict conflict = PlacementScheduleConflict.builder()
                .id(1L)
                .upcomingDrive(drive)
                .student(student)
                .conflictType("REGULAR_CLASS")
                .conflictDate(drive.getDriveDate())
                .conflictStart(LocalTime.of(10, 0))
                .conflictEnd(LocalTime.of(11, 0))
                .build();

        when(conflictRepository.findByStudentIdAndConflictDate(101L, drive.getDriveDate())).thenReturn(List.of(conflict));
        when(studentEnrollmentRepository.findByUserId(101L)).thenReturn(Optional.of(enrollment));

        AttendanceSession session = AttendanceSession.builder()
                .id(80L)
                .section(sectionA)
                .subject(subject)
                .sessionDate(drive.getDriveDate())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build();
        when(attendanceSessionRepository.findBySectionIdAndSessionDate(10L, drive.getDriveDate())).thenReturn(List.of(session));

        AttendanceRecord record = AttendanceRecord.builder()
                .id(90L)
                .session(session)
                .studentEnrollment(enrollment)
                .status(AttendanceStatus.ABSENT) // originally absent
                .build();
        when(attendanceRecordRepository.findBySessionIdAndStudentEnrollmentId(80L, 30L)).thenReturn(Optional.of(record));
        when(exemptionRepository.findByAttendanceRecordId(90L)).thenReturn(Optional.empty());
        when(attendanceRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exemptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<DutyExemptionDto> exemptions = integrationService.applyDutyExemptionForParticipation(10L, 301L, "Verified check-in");

        assertEquals(1, exemptions.size());
        // Verify regular class record was updated to OFFICIAL_DUTY and NOT PRESENT
        assertEquals(AttendanceStatus.OFFICIAL_DUTY, record.getStatus());
        assertNotEquals(AttendanceStatus.PRESENT, record.getStatus());
        assertEquals("ABSENT", exemptions.get(0).getOriginalStatus());
        assertEquals("GRANTED", exemptions.get(0).getExemptionStatus());
        assertEquals("INST_PLACEMENT_DUTY_V1", exemptions.get(0).getDutyPolicyCode());
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("CRT conflict grants EXCUSED status - never marks CRT as PRESENT")
    void testApplyDutyExemption_CrtSession_SetsExcused_NeverPresent() {
        PlacementParticipation verifiedParticipation = PlacementParticipation.builder()
                .id(10L)
                .upcomingDrive(drive)
                .student(student)
                .verificationStatus(PlacementParticipationStatus.ATTENDED_VERIFIED)
                .build();

        when(participationRepository.findById(10L)).thenReturn(Optional.of(verifiedParticipation));
        when(userRepository.findById(301L)).thenReturn(Optional.of(admin));

        PlacementScheduleConflict conflict = PlacementScheduleConflict.builder()
                .id(2L)
                .upcomingDrive(drive)
                .student(student)
                .conflictType("CRT_TRAINING")
                .conflictDate(drive.getDriveDate())
                .conflictStart(LocalTime.of(14, 0))
                .conflictEnd(LocalTime.of(16, 0))
                .build();

        when(conflictRepository.findByStudentIdAndConflictDate(101L, drive.getDriveDate())).thenReturn(List.of(conflict));

        CrtBatch batch = CrtBatch.builder().id(500L).name("CRT Batch 1").build();
        CrtEnrollment crtEnrollment = CrtEnrollment.builder().id(501L).student(student).batch(batch).status(CrtEnrollmentStatus.VERIFIED).build();
        when(crtEnrollmentRepository.findVerifiedByStudentId(101L)).thenReturn(Optional.of(crtEnrollment));

        CrtSession crtSession = CrtSession.builder()
                .id(600L)
                .batch(batch)
                .sessionDate(drive.getDriveDate())
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .build();
        when(crtSessionRepository.findByBatchIdAndSessionDate(500L, drive.getDriveDate())).thenReturn(List.of(crtSession));

        CrtAttendanceRecord crtRecord = CrtAttendanceRecord.builder()
                .id(700L)
                .session(crtSession)
                .student(student)
                .status(CrtAttendanceStatus.ABSENT)
                .build();
        when(crtAttendanceRecordRepository.findBySessionIdAndStudentId(600L, 101L)).thenReturn(Optional.of(crtRecord));
        when(exemptionRepository.findByCrtAttendanceRecordId(700L)).thenReturn(Optional.empty());
        when(crtAttendanceRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exemptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<DutyExemptionDto> exemptions = integrationService.applyDutyExemptionForParticipation(10L, 301L, "Drive attended");

        assertEquals(1, exemptions.size());
        // Verify CRT attendance record is EXCUSED, never PRESENT
        assertEquals(CrtAttendanceStatus.EXCUSED, crtRecord.getStatus());
        assertNotEquals(CrtAttendanceStatus.PRESENT, crtRecord.getStatus());
        assertEquals("INST_CRT_PLACEMENT_DUTY_V1", exemptions.get(0).getDutyPolicyCode());
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Revoking duty exemption safely restores original regular attendance status")
    void testRevokeDutyExemption_RestoresOriginalStatus() {
        AttendanceRecord record = AttendanceRecord.builder()
                .id(90L)
                .status(AttendanceStatus.OFFICIAL_DUTY)
                .build();

        PlacementDutyExemption exemption = PlacementDutyExemption.builder()
                .id(55L)
                .student(student)
                .targetType("REGULAR_CLASS")
                .attendanceRecord(record)
                .originalStatus("ABSENT")
                .exemptionStatus("GRANTED")
                .build();

        when(exemptionRepository.findById(55L)).thenReturn(Optional.of(exemption));
        when(userRepository.findById(301L)).thenReturn(Optional.of(admin));
        when(attendanceRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exemptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DutyExemptionDto result = integrationService.revokeDutyExemption(55L, 301L, "Student was marked absent erroneously");

        assertEquals("REVOKED", result.getExemptionStatus());
        assertEquals(AttendanceStatus.ABSENT, record.getStatus());
        verify(attendanceRecordRepository, times(1)).save(record);
        verify(auditLogRepository, times(1)).save(any());
    }
}
