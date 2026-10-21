package in.jecrcu.placement.dto.integration;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationOverviewDto {
    private long totalDrivesWithTimings;
    private long totalRegistrations;
    private long totalVerifiedParticipants;
    private long totalConflictsDetected;
    private long totalDutyExemptionsGranted;
    private List<PlacementParticipationDto> recentParticipations;
    private List<ScheduleConflictDto> upcomingConflicts;
    private List<DutyExemptionDto> recentExemptions;
}
