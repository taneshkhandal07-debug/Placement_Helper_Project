package in.jecrcu.placement.controller;

import in.jecrcu.placement.config.CustomUserDetails;
import in.jecrcu.placement.entity.DriveRegistration;
import in.jecrcu.placement.entity.UpcomingDrive;
import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.repository.DriveRegistrationRepository;
import in.jecrcu.placement.repository.UpcomingDriveRepository;
import in.jecrcu.placement.service.AuditService;
import in.jecrcu.placement.service.EligibilityService;
import in.jecrcu.placement.service.NotificationService;
import in.jecrcu.placement.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API controller providing evaluated upcoming placement drives
 * and registration capabilities to the React frontend.
 *
 * All business logic is delegated directly to the existing
 * EligibilityService, NotificationService, and AuditService.
 */
@RestController
@RequestMapping("/api/drives/upcoming")
@RequiredArgsConstructor
@Slf4j
public class UpcomingDriveRestController {

    private final UpcomingDriveRepository upcomingDriveRepository;
    private final DriveRegistrationRepository driveRegistrationRepository;
    private final EligibilityService eligibilityService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final in.jecrcu.placement.service.integration.PlacementIntegrationService placementIntegrationService;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingDriveDto {
        private Long id;
        private String companyName;
        private Long companyId;
        private String rolesOffered;
        private String ctcOrStipend;
        private String eligibleBranches;
        private BigDecimal minCgpa;
        private Integer maxBacklogs;
        private LocalDate driveDate;
        private String formattedDriveDate;
        private java.time.LocalTime startTime;
        private java.time.LocalTime endTime;
        private String venue;
        private String formattedTimings;
        private LocalDateTime deadline;
        private String formattedDeadline;
        private String registrationLink;
        private boolean eligible;
        private List<String> exclusionReasons;
        private boolean registered;
        private String registrationStatus;
        private String registrationNotes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        private String notes;
    }

    /**
     * GET /api/drives/upcoming
     * Returns all upcoming drives evaluated with the authenticated student's
     * branch, CGPA, and backlog statistics.
     */
    @GetMapping
    public ResponseEntity<List<UpcomingDriveDto>> getUpcomingDrives(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User user = (userDetails != null) ? userService.findById(userDetails.getId()).orElse(null) : null;
        String branch = (user != null && user.getBranch() != null) ? user.getBranch() : "CSE";
        BigDecimal cgpa = (user != null) ? user.getCgpa() : null;
        Integer backlogs = (user != null && user.getActiveBacklogs() != null) ? user.getActiveBacklogs() : 0;

        List<EligibilityService.EligibilityResult<UpcomingDrive>> evaluations =
                eligibilityService.evaluateAllUpcoming(branch, cgpa, backlogs);

        Map<Long, DriveRegistration> registrationsByDriveId = new HashMap<>();
        if (user != null) {
            driveRegistrationRepository.findByUserIdOrderByRegisteredAtDesc(user.getId())
                    .forEach(r -> registrationsByDriveId.put(r.getUpcomingDrive().getId(), r));
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd MMM yyyy");

        List<UpcomingDriveDto> dtos = evaluations.stream().map(eval -> {
            UpcomingDrive d = eval.getTarget();
            DriveRegistration reg = registrationsByDriveId.get(d.getId());
            boolean isReg = reg != null && "REGISTERED".equalsIgnoreCase(reg.getStatus());

            UpcomingDriveDto dto = new UpcomingDriveDto();
            dto.setId(d.getId());
            dto.setCompanyName(d.getCompanyName());
            dto.setCompanyId(d.getCompany() != null ? d.getCompany().getId() : null);
            dto.setRolesOffered(d.getRolesOffered());
            dto.setCtcOrStipend(d.getCtcOrStipend());
            dto.setEligibleBranches(d.getEligibleBranches());
            dto.setMinCgpa(d.getMinCgpa());
            dto.setMaxBacklogs(d.getMaxBacklogs());
            dto.setDriveDate(d.getDriveDate());
            dto.setFormattedDriveDate(d.getDriveDate() != null ? d.getDriveDate().format(df) : "TBD");
            dto.setStartTime(d.getStartTime());
            dto.setEndTime(d.getEndTime());
            dto.setVenue(d.getVenue());
            String timingStr = (d.getStartTime() != null && d.getEndTime() != null)
                    ? d.getStartTime().toString() + " - " + d.getEndTime().toString()
                    : "Full Day (09:00 - 17:00)";
            dto.setFormattedTimings(timingStr);
            dto.setDeadline(d.getDeadline());
            dto.setFormattedDeadline(d.getDeadline() != null ? d.getDeadline().format(dtf) : "N/A");
            dto.setRegistrationLink(d.getRegistrationLink());
            dto.setEligible(eval.isEligible());
            dto.setExclusionReasons(eval.getExclusionReasons() != null ? eval.getExclusionReasons() : Collections.emptyList());
            dto.setRegistered(isReg);
            dto.setRegistrationStatus(reg != null ? reg.getStatus() : "NOT_REGISTERED");
            dto.setRegistrationNotes(reg != null ? reg.getNotes() : null);
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/drives/upcoming/{id}
     * Returns individual upcoming drive detail with eligibility evaluation.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDriveDetail(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UpcomingDrive drive = upcomingDriveRepository.findById(id).orElse(null);
        if (drive == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Upcoming drive not found with id: " + id));
        }

        User user = (userDetails != null) ? userService.findById(userDetails.getId()).orElse(null) : null;
        String branch = (user != null && user.getBranch() != null) ? user.getBranch() : "CSE";
        BigDecimal cgpa = (user != null) ? user.getCgpa() : null;
        Integer backlogs = (user != null && user.getActiveBacklogs() != null) ? user.getActiveBacklogs() : 0;

        EligibilityService.EligibilityResult<UpcomingDrive> eval =
                eligibilityService.checkUpcomingEligibility(branch, cgpa, backlogs, drive);

        DriveRegistration reg = (user != null)
                ? driveRegistrationRepository.findByUserIdAndUpcomingDriveId(user.getId(), drive.getId()).orElse(null)
                : null;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd MMM yyyy");

        UpcomingDriveDto dto = new UpcomingDriveDto();
        dto.setId(drive.getId());
        dto.setCompanyName(drive.getCompanyName());
        dto.setCompanyId(drive.getCompany() != null ? drive.getCompany().getId() : null);
        dto.setRolesOffered(drive.getRolesOffered());
        dto.setCtcOrStipend(drive.getCtcOrStipend());
        dto.setEligibleBranches(drive.getEligibleBranches());
        dto.setMinCgpa(drive.getMinCgpa());
        dto.setMaxBacklogs(drive.getMaxBacklogs());
        dto.setDriveDate(drive.getDriveDate());
        dto.setFormattedDriveDate(drive.getDriveDate() != null ? drive.getDriveDate().format(df) : "TBD");
        dto.setStartTime(drive.getStartTime());
        dto.setEndTime(drive.getEndTime());
        dto.setVenue(drive.getVenue());
        String detailTimingStr = (drive.getStartTime() != null && drive.getEndTime() != null)
                ? drive.getStartTime().toString() + " - " + drive.getEndTime().toString()
                : "Full Day (09:00 - 17:00)";
        dto.setFormattedTimings(detailTimingStr);
        dto.setDeadline(drive.getDeadline());
        dto.setFormattedDeadline(drive.getDeadline() != null ? drive.getDeadline().format(dtf) : "N/A");
        dto.setRegistrationLink(drive.getRegistrationLink());
        dto.setEligible(eval.isEligible());
        dto.setExclusionReasons(eval.getExclusionReasons() != null ? eval.getExclusionReasons() : Collections.emptyList());
        dto.setRegistered(reg != null && "REGISTERED".equalsIgnoreCase(reg.getStatus()));
        dto.setRegistrationStatus(reg != null ? reg.getStatus() : "NOT_REGISTERED");
        dto.setRegistrationNotes(reg != null ? reg.getNotes() : null);

        return ResponseEntity.ok(dto);
    }

    /**
     * POST /api/drives/upcoming/{id}/register
     * Submits drive registration for the authenticated student.
     * Validates eligibility using EligibilityService.
     */
    @PostMapping("/{id}/register")
    public ResponseEntity<?> registerForDrive(
            @PathVariable("id") Long id,
            @RequestBody(required = false) RegisterRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required to register for drives"));
        }

        UpcomingDrive drive = upcomingDriveRepository.findById(id).orElse(null);
        if (drive == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Drive not found with id: " + id));
        }

        User user = userService.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User account not found"));
        }

        var eval = eligibilityService.checkUpcomingEligibility(
                user.getBranch(), user.getCgpa(), user.getActiveBacklogs(), drive);

        if (!eval.isEligible()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "You do not meet the minimum eligibility criteria for " + drive.getCompanyName(),
                            "exclusionReasons", eval.getExclusionReasons()
                    ));
        }

        String notes = (request != null) ? request.getNotes() : null;

        DriveRegistration reg = driveRegistrationRepository.findByUserIdAndUpcomingDriveId(user.getId(), drive.getId())
                .orElseGet(() -> DriveRegistration.builder()
                        .user(user)
                        .upcomingDrive(drive)
                        .build());

        reg.setStatus("REGISTERED");
        reg.setNotes(notes);
        driveRegistrationRepository.save(reg);

        String deadlineStr = drive.getDeadline() != null
                ? drive.getDeadline().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                : "TBA";

        notificationService.sendNotification(
                user,
                "Registered: " + drive.getCompanyName(),
                "You have successfully registered for the " + drive.getCompanyName() + " campus drive (" + drive.getRolesOffered() + "). Deadline: " + deadlineStr,
                "/upcoming/" + drive.getId() + "/register",
                "DRIVE_REGISTRATION"
        );

        auditService.logAction(
                user,
                "REGISTER_DRIVE",
                "UpcomingDrive",
                drive.getId(),
                null,
                "Student " + user.getFullName() + " registered for " + drive.getCompanyName() + " via React"
        );

        log.info("Drive registration recorded via React for user {} on drive {}", user.getEmail(), drive.getCompanyName());

        // Step 2 & 3: Run advance conflict detection and ensure participation record
        try {
            placementIntegrationService.detectAndRecordConflictsForStudent(drive.getId(), user.getId());
        } catch (Exception e) {
            log.warn("Non-fatal: failed to analyze timetable conflicts for student {} on drive {}: {}",
                    user.getEmail(), drive.getId(), e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Registration confirmed for " + drive.getCompanyName(),
                "driveId", drive.getId(),
                "status", "REGISTERED"
        ));
    }
}
