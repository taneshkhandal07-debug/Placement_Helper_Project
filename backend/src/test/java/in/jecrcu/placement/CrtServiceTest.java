package in.jecrcu.placement;

import in.jecrcu.placement.dto.crt.*;
import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.entity.crt.*;
import in.jecrcu.placement.entity.enums.Role;
import in.jecrcu.placement.entity.erp.*;
import in.jecrcu.placement.repository.UserRepository;
import in.jecrcu.placement.repository.crt.*;
import in.jecrcu.placement.repository.erp.AcademicYearRepository;
import in.jecrcu.placement.repository.erp.StudentEnrollmentRepository;
import in.jecrcu.placement.service.crt.CrtServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrtServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentEnrollmentRepository studentEnrollmentRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private CrtTrackRepository crtTrackRepository;
    @Mock
    private CrtBatchRepository crtBatchRepository;
    @Mock
    private CrtBatchTrainerRepository crtBatchTrainerRepository;
    @Mock
    private CrtEnrollmentRepository crtEnrollmentRepository;
    @Mock
    private CrtSessionRepository crtSessionRepository;
    @Mock
    private CrtAttendanceRecordRepository crtAttendanceRecordRepository;
    @Mock
    private CrtAttendanceCorrectionRepository crtAttendanceCorrectionRepository;
    @Mock
    private CrtAttendanceAuditLogRepository crtAttendanceAuditLogRepository;
    @Mock
    private CrtAssessmentRepository crtAssessmentRepository;
    @Mock
    private CrtAssessmentScoreRepository crtAssessmentScoreRepository;

    @InjectMocks
    private CrtServiceImpl crtService;

    private User fourthYearStudent;
    private User secondYearStudent;
    private User trainer;
    private AcademicYear currentYear;
    private CrtBatch batchAlpha;
    private CrtTrack dsaTrack;
    private StudentEnrollment fourthYearEnrollment;
    private StudentEnrollment secondYearEnrollment;

    @BeforeEach
    void setUp() {
        fourthYearStudent = User.builder()
                .id(2L)
                .email("student4@jecrcu.edu.in")
                .fullName("Rahul 4thYear")
                .role(Role.ROLE_STUDENT)
                .graduationYear(2026)
                .build();

        secondYearStudent = User.builder()
                .id(5L)
                .email("student2@jecrcu.edu.in")
                .fullName("Sneha 2ndYear")
                .role(Role.ROLE_STUDENT)
                .graduationYear(2028)
                .build();

        trainer = User.builder()
                .id(7L)
                .email("trainer@jecrcu.edu.in")
                .fullName("Dr. Trainer")
                .role(Role.ROLE_FACULTY)
                .build();

        currentYear = AcademicYear.builder()
                .id(1L)
                .yearName("2025-2026")
                .isCurrent(true)
                .build();

        Semester sem7 = Semester.builder().id(1L).semesterNumber(7).termName("Autumn 2025").build();
        Section secA = Section.builder().id(1L).name("Sec-A").semester(sem7).build();
        fourthYearEnrollment = StudentEnrollment.builder().id(1L).user(fourthYearStudent).section(secA).build();

        Semester sem3 = Semester.builder().id(2L).semesterNumber(3).termName("Autumn 2025").build();
        Section secB = Section.builder().id(2L).name("Sec-B").semester(sem3).build();
        secondYearEnrollment = StudentEnrollment.builder().id(2L).user(secondYearStudent).section(secB).build();

        batchAlpha = CrtBatch.builder()
                .id(1L)
                .batchCode("CRT-2026-ALPHA")
                .name("Batch Alpha")
                .academicYear(currentYear)
                .isActive(true)
                .build();

        dsaTrack = CrtTrack.builder()
                .id(2L)
                .code("CRT-DSA")
                .name("Data Structures & Algorithms")
                .build();
    }

    @Test
    @DisplayName("Eligibility: 4th year student in Semester 7 is eligible")
    void testFourthYearEligibility_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(fourthYearStudent));
        when(studentEnrollmentRepository.findByUserId(2L)).thenReturn(Optional.of(fourthYearEnrollment));
        when(crtEnrollmentRepository.findByStudentId(2L)).thenReturn(Optional.empty());

        CrtEligibilityDto result = crtService.checkStudentEligibility(2L);
        assertTrue(result.isFourthYearEligible());
        assertTrue(result.getEligibilityReason().contains("Verified fourth-year student"));
        assertNull(result.getEnrollmentStatus());
    }

    @Test
    @DisplayName("Eligibility: 2nd year student in Semester 3 is not eligible")
    void testSecondYearEligibility_Ineligible() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(secondYearStudent));
        when(studentEnrollmentRepository.findByUserId(5L)).thenReturn(Optional.of(secondYearEnrollment));
        when(crtEnrollmentRepository.findByStudentId(5L)).thenReturn(Optional.empty());

        CrtEligibilityDto result = crtService.checkStudentEligibility(5L);
        assertFalse(result.isFourthYearEligible());
        assertTrue(result.getEligibilityReason().contains("Ineligible"));
    }

    @Test
    @DisplayName("Opt-In: 4th year student can submit opt-in request")
    void testOptIn_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(fourthYearStudent));
        when(studentEnrollmentRepository.findByUserId(2L)).thenReturn(Optional.of(fourthYearEnrollment));
        when(crtEnrollmentRepository.findByStudentId(2L)).thenReturn(Optional.empty());
        when(academicYearRepository.findByIsCurrentTrue()).thenReturn(Optional.of(currentYear));
        when(crtEnrollmentRepository.findByStudentIdAndAcademicYearId(2L, 1L)).thenReturn(Optional.empty());
        when(crtBatchRepository.findByIsActiveTrue()).thenReturn(List.of(batchAlpha));
        when(crtEnrollmentRepository.save(any(CrtEnrollment.class))).thenAnswer(i -> {
            CrtEnrollment saved = i.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        CrtOptInRequestDto req = CrtOptInRequestDto.builder()
                .optInNotes("Interested in product companies")
                .build();

        CrtEnrollment result = crtService.optInForCrt(2L, req);
        assertNotNull(result);
        assertEquals(CrtEnrollmentStatus.REQUESTED, result.getStatus());
        assertEquals("Interested in product companies", result.getOptInNotes());
        verify(crtEnrollmentRepository, times(1)).save(any(CrtEnrollment.class));
    }

    @Test
    @DisplayName("Opt-In: Ineligible student is blocked from opting in")
    void testOptIn_IneligibleStudentThrows() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(secondYearStudent));
        when(studentEnrollmentRepository.findByUserId(5L)).thenReturn(Optional.of(secondYearEnrollment));
        when(crtEnrollmentRepository.findByStudentId(5L)).thenReturn(Optional.empty());

        CrtOptInRequestDto req = CrtOptInRequestDto.builder().optInNotes("I want CRT").build();

        assertThrows(IllegalStateException.class, () -> crtService.optInForCrt(5L, req));
    }

    @Test
    @DisplayName("Verification: Coordinator approves opt-in and assigns batch")
    void testVerifyEnrollment_Approval() {
        CrtEnrollment pending = CrtEnrollment.builder()
                .id(10L)
                .student(fourthYearStudent)
                .academicYear(currentYear)
                .status(CrtEnrollmentStatus.REQUESTED)
                .build();

        when(crtEnrollmentRepository.findById(10L)).thenReturn(Optional.of(pending));
        when(userRepository.findById(7L)).thenReturn(Optional.of(trainer));
        when(crtBatchRepository.findById(1L)).thenReturn(Optional.of(batchAlpha));
        when(crtEnrollmentRepository.save(any(CrtEnrollment.class))).thenAnswer(i -> i.getArgument(0));

        CrtVerificationActionDto action = CrtVerificationActionDto.builder()
                .action("APPROVE")
                .assignedBatchId(1L)
                .remarks("Eligible 4th year; verified by T&P.")
                .build();

        CrtEnrollment verified = crtService.verifyEnrollment(10L, 7L, action);
        assertEquals(CrtEnrollmentStatus.VERIFIED, verified.getStatus());
        assertEquals(trainer, verified.getVerifiedBy());
        assertEquals(batchAlpha, verified.getBatch());
    }

    @Test
    @DisplayName("Dashboard: Non-verified student receives empty session & attendance lists")
    void testStudentDashboard_UnverifiedStudentIsolation() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(fourthYearStudent));
        when(studentEnrollmentRepository.findByUserId(2L)).thenReturn(Optional.of(fourthYearEnrollment));
        // Enrollment is only REQUESTED, not VERIFIED
        CrtEnrollment pending = CrtEnrollment.builder()
                .id(10L)
                .student(fourthYearStudent)
                .status(CrtEnrollmentStatus.REQUESTED)
                .build();
        when(crtEnrollmentRepository.findByStudentId(2L)).thenReturn(Optional.of(pending));

        CrtStudentDashboardDto dashboard = crtService.getStudentDashboard(2L);
        assertNotNull(dashboard.getEligibility());
        assertEquals(CrtEnrollmentStatus.REQUESTED, dashboard.getEligibility().getEnrollmentStatus());
        assertNull(dashboard.getAttendanceSummary());
        assertTrue(dashboard.getUpcomingSessions().isEmpty());
        assertTrue(dashboard.getRecentAttendance().isEmpty());
        // Verify no session repository or attendance queries were made for unverified student
        verify(crtSessionRepository, never()).findByBatchIdAndStatusOrderBySessionDateAscStartTimeAsc(anyLong(), any());
    }

    @Test
    @DisplayName("Attendance: Calculate independent CRT attendance percentage")
    void testCalculateStudentAttendance() {
        CrtSession s1 = CrtSession.builder().id(1L).batch(batchAlpha).track(dsaTrack).trainer(trainer).sessionDate(LocalDate.now()).startTime(LocalTime.of(16, 0)).endTime(LocalTime.of(17, 30)).topic("T1").build();
        CrtSession s2 = CrtSession.builder().id(2L).batch(batchAlpha).track(dsaTrack).trainer(trainer).sessionDate(LocalDate.now()).startTime(LocalTime.of(16, 0)).endTime(LocalTime.of(17, 30)).topic("T2").build();
        CrtSession s3 = CrtSession.builder().id(3L).batch(batchAlpha).track(dsaTrack).trainer(trainer).sessionDate(LocalDate.now()).startTime(LocalTime.of(16, 0)).endTime(LocalTime.of(17, 30)).topic("T3").build();
        CrtSession s4 = CrtSession.builder().id(4L).batch(batchAlpha).track(dsaTrack).trainer(trainer).sessionDate(LocalDate.now()).startTime(LocalTime.of(16, 0)).endTime(LocalTime.of(17, 30)).topic("T4").build();

        CrtAttendanceRecord r1 = CrtAttendanceRecord.builder().id(1L).session(s1).student(fourthYearStudent).status(CrtAttendanceStatus.PRESENT).build();
        CrtAttendanceRecord r2 = CrtAttendanceRecord.builder().id(2L).session(s2).student(fourthYearStudent).status(CrtAttendanceStatus.PRESENT).build();
        CrtAttendanceRecord r3 = CrtAttendanceRecord.builder().id(3L).session(s3).student(fourthYearStudent).status(CrtAttendanceStatus.LATE).build();
        CrtAttendanceRecord r4 = CrtAttendanceRecord.builder().id(4L).session(s4).student(fourthYearStudent).status(CrtAttendanceStatus.ABSENT).build();

        when(crtAttendanceRecordRepository.findByStudentIdAndBatchId(2L, 1L)).thenReturn(List.of(r1, r2, r3, r4));

        CrtAttendanceSummaryDto summary = crtService.calculateStudentAttendance(2L, 1L);
        assertEquals(4, summary.getTotalSessions());
        assertEquals(3, summary.getAttendedSessions()); // 2 PRESENT + 1 LATE = 3
        assertEquals(1, summary.getAbsentSessions());
        assertEquals(75.0, summary.getAttendancePercentage());
        assertFalse(summary.isDebarmentRisk());
        assertEquals("GOOD", summary.getStandingStatus());
    }

    @Test
    @DisplayName("Attendance: Non-enrolled student cannot receive CRT attendance mark")
    void testMarkAttendance_NonEnrolledStudentBlocked() {
        CrtSession session = CrtSession.builder().id(1L).batch(batchAlpha).track(dsaTrack).trainer(trainer).status(CrtSessionStatus.SCHEDULED).build();
        when(crtSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(7L)).thenReturn(Optional.of(trainer));
        when(userRepository.findById(5L)).thenReturn(Optional.of(secondYearStudent));
        // Student 5 is NOT verified
        when(crtEnrollmentRepository.findVerifiedByStudentId(5L)).thenReturn(Optional.empty());

        CrtAttendanceRecordDto input = CrtAttendanceRecordDto.builder()
                .studentId(5L)
                .status(CrtAttendanceStatus.PRESENT)
                .build();

        assertThrows(AccessDeniedException.class, () ->
                crtService.markSessionAttendance(1L, 7L, List.of(input)));
    }
}
