package in.jecrcu.placement.controller.crt;

import in.jecrcu.placement.config.CustomUserDetails;
import in.jecrcu.placement.dto.crt.*;
import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.entity.crt.*;
import in.jecrcu.placement.entity.enums.Role;
import in.jecrcu.placement.service.UserService;
import in.jecrcu.placement.service.crt.CrtService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crt")
@RequiredArgsConstructor
@Slf4j
public class CrtRestController {

    private final CrtService crtService;
    private final UserService userService;

    @Data
    public static class RecordScoreRequest {
        private Long studentId;
        private BigDecimal marksObtained;
        private String remarks;
    }

    @Data
    public static class ReviewCorrectionRequest {
        private boolean approve;
        private String remarks;
    }

    // -------------------------------------------------------------------------
    // Student Endpoints
    // -------------------------------------------------------------------------

    @GetMapping("/eligibility")
    public ResponseEntity<?> getEligibility(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        CrtEligibilityDto dto = crtService.checkStudentEligibility(userDetails.getId());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/opt-in")
    public ResponseEntity<?> optIn(
            @Valid @RequestBody CrtOptInRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        try {
            CrtEnrollment enrollment = crtService.optInForCrt(userDetails.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "CRT opt-in application submitted successfully. Awaiting coordinator verification.",
                    "status", enrollment.getStatus(),
                    "enrollmentId", enrollment.getId()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        // Student Dashboard
        if (user.getRole() == Role.ROLE_STUDENT) {
            CrtStudentDashboardDto dashboard = crtService.getStudentDashboard(user.getId());
            return ResponseEntity.ok(dashboard);
        }

        // Trainer / Faculty / Admin Dashboard View
        List<CrtBatch> batches = crtService.getActiveBatches();
        List<CrtEnrollment> pendingEnrollments = crtService.getPendingEnrollments();
        List<CrtAttendanceCorrection> pendingCorrections = crtService.getPendingCorrections();

        return ResponseEntity.ok(Map.of(
                "coordinatorView", true,
                "role", user.getRole().name(),
                "activeBatches", batches,
                "pendingEnrollmentsCount", pendingEnrollments.size(),
                "pendingCorrectionsCount", pendingCorrections.size()
        ));
    }

    @PostMapping("/corrections")
    public ResponseEntity<?> requestCorrection(
            @Valid @RequestBody CrtCorrectionRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        try {
            CrtAttendanceCorrection correction = crtService.requestCorrection(userDetails.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Attendance correction request submitted for trainer review.",
                    "correctionId", correction.getId()
            ));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Shared / Batch / Session Endpoints
    // -------------------------------------------------------------------------

    @GetMapping("/batches")
    public ResponseEntity<?> getActiveBatches(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        return ResponseEntity.ok(crtService.getActiveBatches());
    }

    @GetMapping("/batches/{batchId}/sessions")
    public ResponseEntity<?> getBatchSessions(
            @PathVariable Long batchId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        // If student, verify they are verified enrollees in this batch
        if (user.getRole() == Role.ROLE_STUDENT) {
            CrtEligibilityDto eligibility = crtService.checkStudentEligibility(user.getId());
            if (eligibility.getEnrollmentStatus() != CrtEnrollmentStatus.VERIFIED ||
                    eligibility.getBatchId() == null ||
                    !eligibility.getBatchId().equals(batchId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "Access restricted: Only verified CRT enrollees assigned to this batch can access training sessions."
                ));
            }
        }

        return ResponseEntity.ok(crtService.getSessionsForBatch(batchId));
    }

    @PostMapping("/batches/{batchId}/sessions")
    public ResponseEntity<?> createSession(
            @PathVariable Long batchId,
            @RequestParam Long trackId,
            @Valid @RequestBody CrtSessionDto sessionDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null || (user.getRole() != Role.ROLE_FACULTY && user.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only authorized CRT trainers or administrators can schedule sessions."));
        }

        try {
            CrtSession created = crtService.createSession(batchId, trackId, user.getId(), sessionDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "CRT training session scheduled successfully",
                    "sessionId", created.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sessions/{sessionId}/attendance")
    public ResponseEntity<?> getSessionAttendance(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null || (user.getRole() != Role.ROLE_FACULTY && user.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only trainers or administrators can view full session attendance registers."));
        }
        return ResponseEntity.ok(crtService.getSessionAttendance(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/attendance")
    public ResponseEntity<?> markSessionAttendance(
            @PathVariable Long sessionId,
            @RequestBody List<CrtAttendanceRecordDto> records,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null || (user.getRole() != Role.ROLE_FACULTY && user.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only authorized trainers or administrators can record attendance."));
        }

        try {
            crtService.markSessionAttendance(sessionId, user.getId(), records);
            return ResponseEntity.ok(Map.of("message", "CRT session attendance saved and audit log updated successfully."));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Coordinator / Admin Workflow Endpoints
    // -------------------------------------------------------------------------

    @GetMapping("/enrollments/pending")
    public ResponseEntity<?> getPendingEnrollments(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null || (user.getRole() != Role.ROLE_FACULTY && user.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Coordinator or administrator role required"));
        }

        List<CrtEnrollment> pending = crtService.getPendingEnrollments();
        List<Map<String, Object>> result = pending.stream().map(e -> Map.of(
                "id", (Object) e.getId(),
                "studentId", e.getStudent().getId(),
                "studentName", e.getStudent().getFullName(),
                "studentEmail", e.getStudent().getEmail(),
                "branch", e.getStudent().getBranch() != null ? e.getStudent().getBranch() : "N/A",
                "graduationYear", e.getStudent().getGraduationYear() != null ? e.getStudent().getGraduationYear() : 0,
                "optInNotes", e.getOptInNotes() != null ? e.getOptInNotes() : "",
                "optedInAt", e.getOptedInAt(),
                "status", e.getStatus().name()
        )).toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/enrollments/{id}/verify")
    public ResponseEntity<?> verifyEnrollment(
            @PathVariable Long id,
            @Valid @RequestBody CrtVerificationActionDto action,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null || (user.getRole() != Role.ROLE_FACULTY && user.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Coordinator or administrator role required"));
        }

        try {
            CrtEnrollment verified = crtService.verifyEnrollment(id, user.getId(), action);
            return ResponseEntity.ok(Map.of(
                    "message", "Enrollment status updated to " + verified.getStatus(),
                    "enrollmentId", verified.getId(),
                    "status", verified.getStatus().name()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/corrections/pending")
    public ResponseEntity<?> getPendingCorrections(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null || (user.getRole() != Role.ROLE_FACULTY && user.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Coordinator or administrator role required"));
        }

        List<CrtAttendanceCorrection> pending = crtService.getPendingCorrections();
        List<Map<String, Object>> result = pending.stream().map(c -> Map.of(
                "id", (Object) c.getId(),
                "recordId", c.getRecord().getId(),
                "studentName", c.getStudent().getFullName(),
                "studentEmail", c.getStudent().getEmail(),
                "sessionTopic", c.getRecord().getSession().getTopic(),
                "sessionDate", c.getRecord().getSession().getSessionDate(),
                "currentStatus", c.getRecord().getStatus().name(),
                "requestedStatus", c.getRequestedStatus().name(),
                "reason", c.getReason(),
                "createdAt", c.getCreatedAt()
        )).toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/corrections/{id}/review")
    public ResponseEntity<?> reviewCorrection(
            @PathVariable Long id,
            @RequestBody ReviewCorrectionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null || (user.getRole() != Role.ROLE_FACULTY && user.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Coordinator or administrator role required"));
        }

        try {
            crtService.reviewCorrection(id, user.getId(), request.isApprove(), request.getRemarks());
            return ResponseEntity.ok(Map.of(
                    "message", request.isApprove() ? "Correction approved and attendance record updated." : "Correction rejected."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Assessment Endpoints
    // -------------------------------------------------------------------------

    @GetMapping("/batches/{batchId}/assessments")
    public ResponseEntity<?> getBatchAssessments(
            @PathVariable Long batchId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        return ResponseEntity.ok(crtService.getAssessmentsForBatch(batchId));
    }

    @PostMapping("/assessments/{assessmentId}/scores")
    public ResponseEntity<?> recordAssessmentScore(
            @PathVariable Long assessmentId,
            @RequestBody RecordScoreRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null || (user.getRole() != Role.ROLE_FACULTY && user.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only trainers or administrators can record assessment marks."));
        }

        try {
            crtService.recordAssessmentScore(assessmentId, request.getStudentId(), request.getMarksObtained(), request.getRemarks(), user.getId());
            return ResponseEntity.ok(Map.of("message", "Assessment score recorded successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
