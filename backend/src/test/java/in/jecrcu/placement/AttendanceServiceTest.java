package in.jecrcu.placement;

import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.entity.enums.Role;
import in.jecrcu.placement.entity.erp.*;
import in.jecrcu.placement.repository.erp.*;
import in.jecrcu.placement.service.erp.AttendanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceSessionRepository sessionRepository;
    @Mock
    private AttendanceRecordRepository recordRepository;
    @Mock
    private AttendanceCorrectionRequestRepository correctionRequestRepository;
    @Mock
    private AttendanceAuditLogRepository auditLogRepository;
    @Mock
    private StudentEnrollmentRepository studentEnrollmentRepository;
    @Mock
    private StudentSubjectEnrollmentRepository studentSubjectEnrollmentRepository;
    @Mock
    private FacultyAssignmentRepository facultyAssignmentRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private User student1;
    private User student2;
    private User faculty;
    private User admin;
    private Section sectionA;
    private Subject subjectCS701;
    private StudentEnrollment enrollment1;
    private AttendanceSession session1;
    private AttendanceRecord record1;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(attendanceService, "minimumPercentage", 75.0);
        ReflectionTestUtils.setField(attendanceService, "includeApprovedLeave", true);
        ReflectionTestUtils.setField(attendanceService, "includeOfficialDuty", true);

        student1 = User.builder().id(101L).email("student1@jecrcu.edu.in").fullName("Student One").role(Role.ROLE_STUDENT).build();
        student2 = User.builder().id(102L).email("student2@jecrcu.edu.in").fullName("Student Two").role(Role.ROLE_STUDENT).build();
        faculty = User.builder().id(201L).email("faculty@jecrcu.edu.in").fullName("Faculty Sharma").role(Role.ROLE_FACULTY).build();
        admin = User.builder().id(1L).email("admin@jecrcu.edu.in").fullName("Admin Officer").role(Role.ROLE_ADMIN).build();

        sectionA = Section.builder().id(1L).name("Sec-A").build();
        subjectCS701 = Subject.builder().id(10L).code("CS701").name("Distributed Systems").credits(4).build();

        enrollment1 = StudentEnrollment.builder()
                .id(1L)
                .user(student1)
                .section(sectionA)
                .rollNumber("22BCSE101")
                .enrollmentStatus("ACTIVE")
                .build();

        session1 = AttendanceSession.builder()
                .id(501L)
                .section(sectionA)
                .subject(subjectCS701)
                .facultyUser(faculty)
                .sessionDate(LocalDate.of(2025, 10, 15))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        record1 = AttendanceRecord.builder()
                .id(901L)
                .session(session1)
                .studentEnrollment(enrollment1)
                .status(AttendanceStatus.ABSENT)
                .build();
    }

    @Test
    void testAttendanceCalculationWithApprovedLeave() {
        when(studentEnrollmentRepository.findByUserId(101L)).thenReturn(Optional.of(enrollment1));

        StudentSubjectEnrollment sse = StudentSubjectEnrollment.builder()
                .id(1L)
                .studentEnrollment(enrollment1)
                .subject(subjectCS701)
                .build();
        when(studentSubjectEnrollmentRepository.findByStudentEnrollmentId(1L)).thenReturn(List.of(sse));

        AttendanceRecord r1 = AttendanceRecord.builder().id(1L).session(session1).status(AttendanceStatus.PRESENT).build();
        AttendanceRecord r2 = AttendanceRecord.builder().id(2L).session(session1).status(AttendanceStatus.PRESENT).build();
        AttendanceRecord r3 = AttendanceRecord.builder().id(3L).session(session1).status(AttendanceStatus.APPROVED_LEAVE).build();
        AttendanceRecord r4 = AttendanceRecord.builder().id(4L).session(session1).status(AttendanceStatus.ABSENT).build();

        when(recordRepository.findByStudentEnrollmentIdAndSubjectId(1L, 10L)).thenReturn(List.of(r1, r2, r3, r4));

        var summary = attendanceService.getStudentAttendanceSummary(101L, student1);

        assertNotNull(summary);
        assertEquals(1, summary.getSubjectSummaries().size());
        var subSummary = summary.getSubjectSummaries().get(0);
        assertEquals(4, subSummary.getTotalSessions());
        assertEquals(2, subSummary.getPresentCount());
        assertEquals(1, subSummary.getApprovedLeaveCount());
        assertEquals(1, subSummary.getAbsentCount());
        // 2 present + 1 leave = 3 / 4 = 75.0%
        assertEquals(75.0, subSummary.getAttendancePercentage());
        assertFalse(subSummary.isDebarred());
    }

    @Test
    void testDebarmentWarningTriggeredBelowThreshold() {
        when(studentEnrollmentRepository.findByUserId(101L)).thenReturn(Optional.of(enrollment1));

        StudentSubjectEnrollment sse = StudentSubjectEnrollment.builder()
                .id(1L)
                .studentEnrollment(enrollment1)
                .subject(subjectCS701)
                .build();
        when(studentSubjectEnrollmentRepository.findByStudentEnrollmentId(1L)).thenReturn(List.of(sse));

        AttendanceRecord r1 = AttendanceRecord.builder().id(1L).session(session1).status(AttendanceStatus.PRESENT).build();
        AttendanceRecord r2 = AttendanceRecord.builder().id(2L).session(session1).status(AttendanceStatus.ABSENT).build();

        when(recordRepository.findByStudentEnrollmentIdAndSubjectId(1L, 10L)).thenReturn(List.of(r1, r2));

        var summary = attendanceService.getStudentAttendanceSummary(101L, student1);

        var subSummary = summary.getSubjectSummaries().get(0);
        // 1 present out of 2 = 50.0% < 75.0%
        assertEquals(50.0, subSummary.getAttendancePercentage());
        assertTrue(subSummary.isDebarred());
        assertEquals("DEBARMENT_WARNING", subSummary.getStatusText());
        assertTrue(summary.isAnyDebarmentWarning());
    }

    @Test
    void testStudentCannotViewOtherStudentAttendance() {
        assertThrows(AccessDeniedException.class, () ->
                attendanceService.getStudentAttendanceSummary(101L, student2)
        );
    }

    @Test
    void testUnassignedFacultyCannotRecordAttendance() {
        when(sessionRepository.findById(501L)).thenReturn(Optional.of(session1));
        when(facultyAssignmentRepository.findByFacultyUserIdAndSubjectIdAndSectionId(201L, 10L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                attendanceService.recordBatchAttendance(501L, Collections.emptyList(), faculty)
        );
    }

    @Test
    void testCorrectionWorkflowApprovesAndAudits() {
        when(correctionRequestRepository.findById(77L)).thenReturn(Optional.of(
                AttendanceCorrectionRequest.builder()
                        .id(77L)
                        .attendanceRecord(record1)
                        .requestedByUser(student1)
                        .requestedStatus(AttendanceStatus.APPROVED_LEAVE)
                        .status("PENDING")
                        .build()
        ));
        when(facultyAssignmentRepository.findByFacultyUserIdAndSubjectIdAndSectionId(201L, 10L, 1L))
                .thenReturn(Optional.of(FacultyAssignment.builder().id(1L).build()));
        when(correctionRequestRepository.save(any(AttendanceCorrectionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        var reviewed = attendanceService.reviewCorrection(77L, true, "Approved medical certificate", faculty);

        assertEquals("APPROVED", reviewed.getStatus());
        assertEquals(AttendanceStatus.APPROVED_LEAVE, record1.getStatus());
        verify(auditLogRepository, times(1)).save(any(AttendanceAuditLog.class));
    }
}
