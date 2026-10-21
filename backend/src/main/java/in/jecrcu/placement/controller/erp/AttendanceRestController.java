package in.jecrcu.placement.controller.erp;

import in.jecrcu.placement.config.CustomUserDetails;
import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.entity.erp.AttendanceStatus;
import in.jecrcu.placement.service.UserService;
import in.jecrcu.placement.service.erp.AttendanceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/erp/attendance")
@RequiredArgsConstructor
@Slf4j
public class AttendanceRestController {

    private final AttendanceService attendanceService;
    private final UserService userService;

    @Data
    public static class CreateSessionRequest {
        private Long sectionId;
        private Long subjectId;
        private LocalDate sessionDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String topicCovered;
    }

    @Data
    public static class BatchAttendanceRequest {
        private List<AttendanceService.RecordEntryInput> entries;
    }

    @Data
    public static class CorrectionRequestInput {
        private Long attendanceRecordId;
        private AttendanceStatus requestedStatus;
        private String reason;
        private String documentUrl;
    }

    @Data
    public static class ReviewCorrectionInput {
        private boolean approve;
        private String remarks;
    }

    // -------------------------------------------------------------
    // Student Endpoints
    // -------------------------------------------------------------

    @GetMapping("/summary")
    public ResponseEntity<?> getAttendanceSummary(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        try {
            var summary = attendanceService.getStudentAttendanceSummary(user.getId(), user);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getAttendanceHistory(
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        try {
            var history = attendanceService.getStudentAttendanceHistory(user.getId(), subjectId, user);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/correction")
    public ResponseEntity<?> submitCorrection(
            @RequestBody CorrectionRequestInput input,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        var req = attendanceService.requestCorrection(
                input.getAttendanceRecordId(),
                input.getRequestedStatus(),
                input.getReason(),
                input.getDocumentUrl(),
                user
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Attendance correction request submitted for faculty review",
                "requestId", req.getId(),
                "status", req.getStatus()
        ));
    }

    // -------------------------------------------------------------
    // Faculty Endpoints
    // -------------------------------------------------------------

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(
            @RequestBody CreateSessionRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        var session = attendanceService.createSession(
                req.getSectionId(),
                req.getSubjectId(),
                req.getSessionDate(),
                req.getStartTime(),
                req.getEndTime(),
                req.getTopicCovered(),
                user
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Attendance session scheduled successfully",
                "sessionId", session.getId()
        ));
    }

    @PostMapping("/sessions/{sessionId}/records")
    public ResponseEntity<?> recordBatch(
            @PathVariable("sessionId") Long sessionId,
            @RequestBody BatchAttendanceRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        var records = attendanceService.recordBatchAttendance(sessionId, req.getEntries(), user);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Attendance saved for " + records.size() + " students",
                "count", records.size()
        ));
    }

    @PostMapping("/corrections/{requestId}/review")
    public ResponseEntity<?> reviewCorrection(
            @PathVariable("requestId") Long requestId,
            @RequestBody ReviewCorrectionInput input,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        var reviewed = attendanceService.reviewCorrection(requestId, input.isApprove(), input.getRemarks(), user);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Correction request " + reviewed.getStatus().toLowerCase(),
                "status", reviewed.getStatus()
        ));
    }
}
