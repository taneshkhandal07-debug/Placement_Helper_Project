package in.jecrcu.placement.dto.integration;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleConflictDto {
    private Long id;
    private Long upcomingDriveId;
    private String companyName;
    private Long studentId;
    private String studentName;
    private String studentRollNumber;
    private String conflictType; // REGULAR_CLASS or CRT_TRAINING
    private String sessionDescription;
    private LocalDate conflictDate;
    private LocalTime conflictStart;
    private LocalTime conflictEnd;
    private Long facultyId;
    private String facultyName;
    private Boolean studentNotified;
    private Boolean facultyNotified;
    private LocalDateTime detectedAt;
}
