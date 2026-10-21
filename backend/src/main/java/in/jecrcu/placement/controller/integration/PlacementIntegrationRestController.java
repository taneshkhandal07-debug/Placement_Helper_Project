package in.jecrcu.placement.controller.integration;

import in.jecrcu.placement.config.CustomUserDetails;
import in.jecrcu.placement.dto.integration.*;
import in.jecrcu.placement.entity.enums.Role;
import in.jecrcu.placement.service.integration.PlacementIntegrationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/placement-integration")
@RequiredArgsConstructor
@Slf4j
public class PlacementIntegrationRestController {

    private final PlacementIntegrationService integrationService;

    @Data
    public static class ApplyExemptionRequest {
        private Long participationId;
        private String remarks;
    }

    @Data
    public static class RevokeExemptionRequest {
        private String reason;
    }

    @Data
    public static class DriveLifecycleRequest {
        private String action; // RESCHEDULED or CANCELLED
        private String reason;
    }

    /**
     * GET /api/placement-integration/overview
     * High-level summary of drives, participations, conflicts, and duty exemptions.
     */
    @GetMapping("/overview")
    public ResponseEntity<IntegrationOverviewDto> getOverview() {
        return ResponseEntity.ok(integrationService.getOverview());
    }

    /**
     * GET /api/placement-integration/conflicts
     * Lists detected timetable and CRT conflicts.
     */
    @GetMapping("/conflicts")
    public ResponseEntity<?> getConflicts(
            @RequestParam(value = "driveId", required = false) Long driveId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "facultyId", required = false) Long facultyId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails != null && userDetails.getRole() == Role.ROLE_STUDENT) {
            // Force student to only see their own conflicts
            studentId = userDetails.getId();
        }

        List<ScheduleConflictDto> conflicts = integrationService.getConflicts(driveId, studentId, facultyId);
        return ResponseEntity.ok(conflicts);
    }

    /**
     * POST /api/placement-integration/conflicts/detect/{driveId}
     * Runs conflict detection against regular class timetables and CRT schedules.
     */
    @PostMapping("/conflicts/detect/{driveId}")
    public ResponseEntity<?> detectConflicts(
            @PathVariable Long driveId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || (userDetails.getRole() != Role.ROLE_ADMIN && userDetails.getRole() != Role.ROLE_FACULTY)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Authorized staff access required"));
        }

        try {
            List<ScheduleConflictDto> conflicts = integrationService.detectAndRecordConflicts(driveId);
            return ResponseEntity.ok(Map.of(
                    "message", "Conflict detection completed successfully",
                    "totalDetected", conflicts.size(),
                    "conflicts", conflicts
            ));
        } catch (Exception e) {
            log.error("Conflict detection failed for drive: {}", driveId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/placement-integration/conflicts/notify-all/{driveId}
     * Sends advance conflict notices to affected students and faculty with deduplication.
     */
    @PostMapping("/conflicts/notify-all/{driveId}")
    public ResponseEntity<?> notifyConflictsForDrive(
            @PathVariable Long driveId,
            @RequestParam(value = "forceResend", defaultValue = "false") boolean forceResend,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getRole() != Role.ROLE_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
        }

        int sent = integrationService.sendAdvanceConflictNotifications(driveId, forceResend);
        return ResponseEntity.ok(Map.of(
                "message", "Advance notifications dispatched",
                "notificationsSent", sent
        ));
    }

    /**
     * POST /api/placement-integration/conflicts/{id}/notify
     * Sends advance conflict notice for a single detected conflict.
     */
    @PostMapping("/conflicts/{id}/notify")
    public ResponseEntity<?> notifySingleConflict(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getRole() != Role.ROLE_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
        }

        boolean success = integrationService.sendNotificationForConflict(id);
        return ResponseEntity.ok(Map.of("success", success));
    }

    /**
     * GET /api/placement-integration/participations/{driveId}
     * Lists registered students and their verification status for a drive.
     */
    @GetMapping("/participations/{driveId}")
    public ResponseEntity<?> getParticipationsByDrive(
            @PathVariable Long driveId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || (userDetails.getRole() != Role.ROLE_ADMIN && userDetails.getRole() != Role.ROLE_FACULTY)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Authorized staff access required"));
        }

        return ResponseEntity.ok(integrationService.getParticipationsByDrive(driveId));
    }

    /**
     * GET /api/placement-integration/my-participations
     * Returns student's personal drive participations and verification status.
     */
    @GetMapping("/my-participations")
    public ResponseEntity<?> getMyParticipations(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        return ResponseEntity.ok(integrationService.getParticipationsByStudent(userDetails.getId()));
    }

    /**
     * GET /api/placement-integration/my-exemptions
     * Returns student's personal granted duty exemptions for regular classes and CRT.
     */
    @GetMapping("/my-exemptions")
    public ResponseEntity<?> getMyExemptions(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        return ResponseEntity.ok(integrationService.getExemptionsByStudent(userDetails.getId()));
    }

    /**
     * POST /api/placement-integration/participations/verify
     * Verifies drive attendance (check-in) and optionally auto-applies duty exemptions.
     */
    @PostMapping("/participations/verify")
    public ResponseEntity<?> verifyParticipation(
            @RequestBody ParticipationVerifyRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || (userDetails.getRole() != Role.ROLE_ADMIN && userDetails.getRole() != Role.ROLE_FACULTY)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Authorized staff access required"));
        }

        try {
            PlacementParticipationDto dto = integrationService.verifyStudentParticipation(request, userDetails.getId());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Failed to verify student participation", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/placement-integration/exemptions
     * Lists all granted/revoked duty exemptions.
     */
    @GetMapping("/exemptions")
    public ResponseEntity<?> getAllExemptions(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || (userDetails.getRole() != Role.ROLE_ADMIN && userDetails.getRole() != Role.ROLE_FACULTY)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Authorized staff access required"));
        }

        return ResponseEntity.ok(integrationService.getAllExemptions());
    }

    /**
     * POST /api/placement-integration/exemptions/apply
     * Manually applies duty exemption for a verified participation.
     */
    @PostMapping("/exemptions/apply")
    public ResponseEntity<?> applyExemption(
            @RequestBody ApplyExemptionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getRole() != Role.ROLE_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
        }

        try {
            List<DutyExemptionDto> exemptions = integrationService.applyDutyExemptionForParticipation(
                    request.getParticipationId(), userDetails.getId(), request.getRemarks());
            return ResponseEntity.ok(Map.of(
                    "message", "Duty exemptions applied successfully",
                    "exemptions", exemptions
            ));
        } catch (Exception e) {
            log.error("Failed to apply duty exemption", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/placement-integration/exemptions/{id}/revoke
     * Revokes a duty exemption and restores the original regular/CRT attendance status.
     */
    @PostMapping("/exemptions/{id}/revoke")
    public ResponseEntity<?> revokeExemption(
            @PathVariable Long id,
            @RequestBody(required = false) RevokeExemptionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getRole() != Role.ROLE_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
        }

        String reason = (request != null) ? request.getReason() : "Administrative revocation";
        try {
            DutyExemptionDto dto = integrationService.revokeDutyExemption(id, userDetails.getId(), reason);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Failed to revoke duty exemption", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/placement-integration/drives/{driveId}/lifecycle
     * Handles changes to drive timings, rescheduling, or cancellations.
     */
    @PostMapping("/drives/{driveId}/lifecycle")
    public ResponseEntity<?> handleDriveLifecycle(
            @PathVariable Long driveId,
            @RequestBody DriveLifecycleRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getRole() != Role.ROLE_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
        }

        try {
            integrationService.handleDriveTimingChangeOrCancellation(
                    driveId, request.getAction(), request.getReason(), userDetails.getId());
            return ResponseEntity.ok(Map.of(
                    "message", "Drive lifecycle event processed successfully",
                    "action", request.getAction()
            ));
        } catch (Exception e) {
            log.error("Failed to process drive lifecycle event", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
