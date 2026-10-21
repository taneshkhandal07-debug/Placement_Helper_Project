package in.jecrcu.placement.dto.crt;

import in.jecrcu.placement.entity.crt.CrtSessionStatus;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrtSessionDto {
    private Long id;
    private Long batchId;
    private String batchName;
    private String batchCode;
    private Long trackId;
    private String trackCode;
    private String trackName;
    private Long trainerId;
    private String trainerName;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String topic;
    private String locationOrLink;
    private CrtSessionStatus status;
}
