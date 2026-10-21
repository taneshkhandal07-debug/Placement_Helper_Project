package in.jecrcu.placement.dto.integration;

import in.jecrcu.placement.entity.integration.PlacementParticipationStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationVerifyRequestDto {
    private Long participationId;
    private Long upcomingDriveId;
    private Long studentId;
    private PlacementParticipationStatus status; // ATTENDED_VERIFIED, ABSENT_UNVERIFIED, EXCUSED
    private String verificationSource; // e.g., "PLACEMENT_OFFICER_PORTAL", "CHECKIN_KIOSK"
    private String remarks;
    private boolean autoApplyDutyExemption; // If true, automatically generates duty exemption for detected conflicts
}
