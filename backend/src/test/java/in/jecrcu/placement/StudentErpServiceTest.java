package in.jecrcu.placement;

import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.entity.enums.Role;
import in.jecrcu.placement.entity.erp.*;
import in.jecrcu.placement.repository.UserRepository;
import in.jecrcu.placement.repository.erp.*;
import in.jecrcu.placement.service.erp.StudentErpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentErpServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private SemesterRepository semesterRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private StudentEnrollmentRepository studentEnrollmentRepository;
    @Mock
    private StudentSubjectEnrollmentRepository studentSubjectEnrollmentRepository;
    @Mock
    private FacultyAssignmentRepository facultyAssignmentRepository;
    @Mock
    private TimetableRepository timetableRepository;

    @InjectMocks
    private StudentErpService studentErpService;

    private User student1;
    private User student2;
    private User faculty;
    private User admin;
    private Section sectionA;
    private StudentEnrollment enrollment1;

    @BeforeEach
    void setUp() {
        student1 = User.builder().id(101L).email("student1@jecrcu.edu.in").fullName("Student One").role(Role.ROLE_STUDENT).build();
        student2 = User.builder().id(102L).email("student2@jecrcu.edu.in").fullName("Student Two").role(Role.ROLE_STUDENT).build();
        faculty = User.builder().id(201L).email("faculty@jecrcu.edu.in").fullName("Faculty Sharma").role(Role.ROLE_FACULTY).build();
        admin = User.builder().id(1L).email("admin@jecrcu.edu.in").fullName("Admin Officer").role(Role.ROLE_ADMIN).build();

        Department dept = Department.builder().id(1L).code("CSE").name("Computer Science").build();
        AcademicYear year = AcademicYear.builder().id(1L).yearName("2025-2026").build();
        Semester sem = Semester.builder().id(1L).academicYear(year).semesterNumber(7).termName("Autumn 2025").build();
        sectionA = Section.builder().id(1L).department(dept).semester(sem).name("Sec-A").capacity(60).build();

        enrollment1 = StudentEnrollment.builder()
                .id(1L)
                .user(student1)
                .section(sectionA)
                .rollNumber("22BCSE101")
                .enrollmentStatus("ACTIVE")
                .build();
    }

    @Test
    void testStudentCanViewOwnProfile() {
        when(studentEnrollmentRepository.findByUserId(101L)).thenReturn(Optional.of(enrollment1));
        when(studentSubjectEnrollmentRepository.findByStudentEnrollmentId(1L)).thenReturn(Collections.emptyList());

        var profileOpt = studentErpService.getStudentProfile(101L, student1);

        assertTrue(profileOpt.isPresent());
        assertEquals("22BCSE101", profileOpt.get().getRollNumber());
        assertEquals("Sec-A", profileOpt.get().getSectionName());
        assertEquals("CSE", profileOpt.get().getDepartmentCode());
    }

    @Test
    void testCrossStudentAccessIsRejected() {
        assertThrows(AccessDeniedException.class, () ->
                studentErpService.getStudentProfile(101L, student2)
        );
    }

    @Test
    void testCrossFacultyAccessIsRejected() {
        assertThrows(AccessDeniedException.class, () ->
                studentErpService.getFacultyAssignments(201L, student1)
        );
    }

    @Test
    void testAdminCanViewAnyStudentProfile() {
        when(studentEnrollmentRepository.findByUserId(101L)).thenReturn(Optional.of(enrollment1));
        when(studentSubjectEnrollmentRepository.findByStudentEnrollmentId(1L)).thenReturn(Collections.emptyList());

        var profileOpt = studentErpService.getStudentProfile(101L, admin);

        assertTrue(profileOpt.isPresent());
        assertEquals("22BCSE101", profileOpt.get().getRollNumber());
    }

    @Test
    void testNonAdminCannotEnrollStudent() {
        assertThrows(AccessDeniedException.class, () ->
                studentErpService.enrollStudent(102L, 1L, "22BCSE102", student1)
        );
    }

    @Test
    void testAdminCanEnrollStudent() {
        when(userRepository.findById(102L)).thenReturn(Optional.of(student2));
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(sectionA));
        when(studentEnrollmentRepository.findByRollNumberIgnoreCase("22BCSE102")).thenReturn(Optional.empty());
        when(studentEnrollmentRepository.save(any(StudentEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentEnrollment enr = studentErpService.enrollStudent(102L, 1L, "22BCSE102", admin);

        assertNotNull(enr);
        assertEquals("22BCSE102", enr.getRollNumber());
        assertEquals(student2, enr.getUser());
        assertEquals(sectionA, enr.getSection());
    }
}
