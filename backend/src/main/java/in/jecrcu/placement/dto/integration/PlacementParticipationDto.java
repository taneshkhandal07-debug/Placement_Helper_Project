package in.jecrcu.placement.dto.integration;

import in.jecrcu.placement.entity.integration.PlacementParticipationStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementParticipationDto {
    private Long id;
    private Long upcomingDriveId;
    private String companyName;
    private String jobRole;
    private LocalDate driveDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String venue;
    private Long studentId;
    private String studentName;
    private String studentRollNumber;
    private String studentEmail;
    private PlacementParticipationStatus verificationStatus;
    private String verificationSource;
    private LocalDateTime verifiedAt;
    private String verifiedByName;
    private String remarks;
    private LocalDateTime createdAt;
}
