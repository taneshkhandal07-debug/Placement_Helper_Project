package in.jecrcu.placement.controller.erp;

import in.jecrcu.placement.config.CustomUserDetails;
import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.service.UserService;
import in.jecrcu.placement.service.erp.StudentErpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Student ERP module.
 * Exposes student academic profiles, subject enrollments,
 * faculty assignments, and section timetables.
 */
@RestController
@RequestMapping("/api/erp")
@RequiredArgsConstructor
@Slf4j
public class StudentErpRestController {

    private final StudentErpService studentErpService;
    private final UserService userService;

    // -------------------------------------------------------------
    // Student Endpoints
    // -------------------------------------------------------------

    @GetMapping("/student/profile")
    public ResponseEntity<?> getStudentProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        return studentErpService.getStudentProfile(user.getId(), user)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No active academic enrollment found for student")));
    }

    @GetMapping("/student/subjects")
    public ResponseEntity<?> getStudentSubjects(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        List<StudentErpService.EnrolledSubjectDto> list = studentErpService.getStudentSubjects(user.getId(), user);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/student/timetable")
    public ResponseEntity<?> getStudentTimetable(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        List<StudentErpService.TimetableSlotDto> list = studentErpService.getStudentTimetable(user.getId(), user);
        return ResponseEntity.ok(list);
    }

    // -------------------------------------------------------------
    // Faculty Endpoints
    // -------------------------------------------------------------

    @GetMapping("/faculty/assignments")
    public ResponseEntity<?> getFacultyAssignments(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        List<StudentErpService.FacultyAssignmentDto> list = studentErpService.getFacultyAssignments(user.getId(), user);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/faculty/timetable")
    public ResponseEntity<?> getFacultyTimetable(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        List<StudentErpService.TimetableSlotDto> list = studentErpService.getFacultyTimetable(user.getId(), user);
        return ResponseEntity.ok(list);
    }
}
