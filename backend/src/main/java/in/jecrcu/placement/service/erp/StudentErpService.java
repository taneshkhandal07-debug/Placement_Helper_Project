package in.jecrcu.placement.service.erp;

import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.entity.enums.Role;
import in.jecrcu.placement.entity.erp.*;
import in.jecrcu.placement.repository.UserRepository;
import in.jecrcu.placement.repository.erp.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentErpService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentSubjectEnrollmentRepository studentSubjectEnrollmentRepository;
    private final FacultyAssignmentRepository facultyAssignmentRepository;
    private final TimetableRepository timetableRepository;

    // DTOs
    @Data
    @Builder
    public static class StudentAcademicProfileDto {
        private Long enrollmentId;
        private Long userId;
        private String studentName;
        private String email;
        private String rollNumber;
        private String departmentCode;
        private String departmentName;
        private Integer semesterNumber;
        private String termName;
        private Long sectionId;
        private String sectionName;
        private String status;
        private int totalCredits;
        private int enrolledSubjectsCount;
    }

    @Data
    @Builder
    public static class EnrolledSubjectDto {
        private Long subjectId;
        private String code;
        private String name;
        private int credits;
        private String type;
        private String enrollmentStatus;
        private String instructorName;
    }

    @Data
    @Builder
    public static class TimetableSlotDto {
        private Long id;
        private Long sectionId;
        private String sectionName;
        private String subjectCode;
        private String subjectName;
        private String facultyName;
        private String dayOfWeek;
        private String startTime;
        private String endTime;
        private String roomNumber;
    }

    @Data
    @Builder
    public static class FacultyAssignmentDto {
        private Long id;
        private Long sectionId;
        private String sectionName;
        private Long subjectId;
        private String subjectCode;
        private String subjectName;
        private String roleTitle;
        private int studentCount;
    }

    // -------------------------------------------------------------
    // Student Academic Data Access (Restricted to self or Admin)
    // -------------------------------------------------------------
    @Transactional(readOnly = true)
    public Optional<StudentAcademicProfileDto> getStudentProfile(Long targetUserId, User requestingUser) {
        validateStudentAccess(targetUserId, requestingUser);

        return studentEnrollmentRepository.findByUserId(targetUserId).map(enr -> {
            Section sec = enr.getSection();
            Semester sem = sec.getSemester();
            Department dept = sec.getDepartment();

            List<StudentSubjectEnrollment> subjects = studentSubjectEnrollmentRepository
                    .findByStudentEnrollmentId(enr.getId());

            int totalCredits = subjects.stream()
                    .mapToInt(s -> s.getSubject().getCredits() != null ? s.getSubject().getCredits() : 0)
                    .sum();

            return StudentAcademicProfileDto.builder()
                    .enrollmentId(enr.getId())
                    .userId(enr.getUser().getId())
                    .studentName(enr.getUser().getFullName())
                    .email(enr.getUser().getEmail())
                    .rollNumber(enr.getRollNumber())
                    .departmentCode(dept.getCode())
                    .departmentName(dept.getName())
                    .semesterNumber(sem.getSemesterNumber())
                    .termName(sem.getTermName())
                    .sectionId(sec.getId())
                    .sectionName(sec.getName())
                    .status(enr.getEnrollmentStatus())
                    .totalCredits(totalCredits)
                    .enrolledSubjectsCount(subjects.size())
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public List<EnrolledSubjectDto> getStudentSubjects(Long targetUserId, User requestingUser) {
        validateStudentAccess(targetUserId, requestingUser);

        StudentEnrollment enr = studentEnrollmentRepository.findByUserId(targetUserId).orElse(null);
        if (enr == null) {
            return Collections.emptyList();
        }

        List<FacultyAssignment> facultyAssignments = facultyAssignmentRepository.findBySectionId(enr.getSection().getId());
        Map<Long, String> instructorBySubjectId = facultyAssignments.stream()
                .collect(Collectors.toMap(
                        fa -> fa.getSubject().getId(),
                        fa -> fa.getFacultyUser().getFullName(),
                        (existing, replacement) -> existing
                ));

        return studentSubjectEnrollmentRepository.findByStudentEnrollmentId(enr.getId()).stream()
                .map(sse -> {
                    Subject s = sse.getSubject();
                    return EnrolledSubjectDto.builder()
                            .subjectId(s.getId())
                            .code(s.getCode())
                            .name(s.getName())
                            .credits(s.getCredits())
                            .type(s.getType())
                            .enrollmentStatus(sse.getEnrollmentStatus())
                            .instructorName(instructorBySubjectId.getOrDefault(s.getId(), "Faculty TBA"))
                            .build();
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimetableSlotDto> getStudentTimetable(Long targetUserId, User requestingUser) {
        validateStudentAccess(targetUserId, requestingUser);

        StudentEnrollment enr = studentEnrollmentRepository.findByUserId(targetUserId).orElse(null);
        if (enr == null) {
            return Collections.emptyList();
        }

        return timetableRepository.findBySectionIdOrderByDayOfWeekAscStartTimeAsc(enr.getSection().getId())
                .stream()
                .map(t -> TimetableSlotDto.builder()
                        .id(t.getId())
                        .sectionId(t.getSection().getId())
                        .sectionName(t.getSection().getName())
                        .subjectCode(t.getSubject().getCode())
                        .subjectName(t.getSubject().getName())
                        .facultyName(t.getFacultyUser() != null ? t.getFacultyUser().getFullName() : "TBA")
                        .dayOfWeek(t.getDayOfWeek())
                        .startTime(t.getStartTime().toString())
                        .endTime(t.getEndTime().toString())
                        .roomNumber(t.getRoomNumber() != null ? t.getRoomNumber() : "TBA")
                        .build())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------
    // Faculty Academic Data Access (Restricted to self or Admin)
    // -------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<FacultyAssignmentDto> getFacultyAssignments(Long facultyUserId, User requestingUser) {
        validateFacultyAccess(facultyUserId, requestingUser);

        List<FacultyAssignment> list = facultyAssignmentRepository.findByFacultyUserId(facultyUserId);
        return list.stream().map(fa -> {
            int studentCount = studentEnrollmentRepository.findBySectionId(fa.getSection().getId()).size();
            return FacultyAssignmentDto.builder()
                    .id(fa.getId())
                    .sectionId(fa.getSection().getId())
                    .sectionName(fa.getSection().getName() + " (" + fa.getSection().getDepartment().getCode() + ")")
                    .subjectId(fa.getSubject().getId())
                    .subjectCode(fa.getSubject().getCode())
                    .subjectName(fa.getSubject().getName())
                    .roleTitle(fa.getRoleTitle())
                    .studentCount(studentCount)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimetableSlotDto> getFacultyTimetable(Long facultyUserId, User requestingUser) {
        validateFacultyAccess(facultyUserId, requestingUser);

        return timetableRepository.findByFacultyUserIdOrderByDayOfWeekAscStartTimeAsc(facultyUserId)
                .stream()
                .map(t -> TimetableSlotDto.builder()
                        .id(t.getId())
                        .sectionId(t.getSection().getId())
                        .sectionName(t.getSection().getName() + " (" + t.getSection().getDepartment().getCode() + ")")
                        .subjectCode(t.getSubject().getCode())
                        .subjectName(t.getSubject().getName())
                        .facultyName(t.getFacultyUser() != null ? t.getFacultyUser().getFullName() : "Self")
                        .dayOfWeek(t.getDayOfWeek())
                        .startTime(t.getStartTime().toString())
                        .endTime(t.getEndTime().toString())
                        .roomNumber(t.getRoomNumber())
                        .build())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------
    // Academic Administration (Admin Only)
    // -------------------------------------------------------------
    @Transactional
    public StudentEnrollment enrollStudent(Long userId, Long sectionId, String rollNumber, User requestingUser) {
        validateAdmin(requestingUser);

        User student = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));

        if (studentEnrollmentRepository.findByRollNumberIgnoreCase(rollNumber).isPresent()) {
            throw new IllegalArgumentException("Roll number already assigned: " + rollNumber);
        }

        StudentEnrollment enr = StudentEnrollment.builder()
                .user(student)
                .section(section)
                .rollNumber(rollNumber.trim())
                .enrollmentStatus("ACTIVE")
                .build();

        return studentEnrollmentRepository.save(enr);
    }

    @Transactional
    public StudentSubjectEnrollment enrollSubject(Long studentEnrollmentId, Long subjectId, User requestingUser) {
        validateAdmin(requestingUser);

        StudentEnrollment enr = studentEnrollmentRepository.findById(studentEnrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + studentEnrollmentId));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));

        return studentSubjectEnrollmentRepository.findByStudentEnrollmentIdAndSubjectId(enr.getId(), subject.getId())
                .orElseGet(() -> studentSubjectEnrollmentRepository.save(
                        StudentSubjectEnrollment.builder()
                                .studentEnrollment(enr)
                                .subject(subject)
                                .enrollmentStatus("ENROLLED")
                                .build()
                ));
    }

    @Transactional
    public FacultyAssignment assignFaculty(Long facultyUserId, Long subjectId, Long sectionId, String roleTitle, User requestingUser) {
        validateAdmin(requestingUser);

        User faculty = userRepository.findById(facultyUserId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty user not found: " + facultyUserId));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));

        return facultyAssignmentRepository.findByFacultyUserIdAndSubjectIdAndSectionId(faculty.getId(), subject.getId(), section.getId())
                .orElseGet(() -> facultyAssignmentRepository.save(
                        FacultyAssignment.builder()
                                .facultyUser(faculty)
                                .subject(subject)
                                .section(section)
                                .roleTitle(roleTitle != null ? roleTitle : "PRIMARY_INSTRUCTOR")
                                .build()
                ));
    }

    @Transactional
    public Timetable createTimetableSlot(Long sectionId, Long subjectId, Long facultyUserId,
                                         String dayOfWeek, LocalTime startTime, LocalTime endTime,
                                         String roomNumber, User requestingUser) {
        validateAdmin(requestingUser);

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
        User faculty = facultyUserId != null ? userRepository.findById(facultyUserId).orElse(null) : null;

        Timetable slot = Timetable.builder()
                .section(section)
                .subject(subject)
                .facultyUser(faculty)
                .dayOfWeek(dayOfWeek.toUpperCase())
                .startTime(startTime)
                .endTime(endTime)
                .roomNumber(roomNumber)
                .build();

        return timetableRepository.save(slot);
    }

    // -------------------------------------------------------------
    // Security & Authorization Guards
    // -------------------------------------------------------------
    private void validateStudentAccess(Long targetUserId, User requestingUser) {
        if (requestingUser == null) {
            throw new AccessDeniedException("Authentication required to access student academic profile");
        }
        boolean isAdmin = requestingUser.getRole() == Role.ROLE_ADMIN;
        boolean isSelf = requestingUser.getId().equals(targetUserId);

        if (!isAdmin && !isSelf) {
            log.warn("Access denied: User {} attempted to access academic records of student {}", requestingUser.getEmail(), targetUserId);
            throw new AccessDeniedException("Unauthorized: You may only view your own student academic records");
        }
    }

    private void validateFacultyAccess(Long targetFacultyId, User requestingUser) {
        if (requestingUser == null) {
            throw new AccessDeniedException("Authentication required to access faculty academic profile");
        }
        boolean isAdmin = requestingUser.getRole() == Role.ROLE_ADMIN;
        boolean isSelf = requestingUser.getId().equals(targetFacultyId);

        if (!isAdmin && !isSelf) {
            log.warn("Access denied: User {} attempted to access faculty schedule of user {}", requestingUser.getEmail(), targetFacultyId);
            throw new AccessDeniedException("Unauthorized: You may only view your own faculty assignments and timetable");
        }
    }

    private void validateAdmin(User requestingUser) {
        if (requestingUser == null || requestingUser.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Administrator role required to perform academic administrative operations");
        }
    }
}
