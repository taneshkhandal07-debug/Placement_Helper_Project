package in.jecrcu.placement.dto.crt;

import in.jecrcu.placement.entity.crt.CrtAttendanceStatus;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrtAttendanceRecordDto {
    private Long id;
    private Long sessionId;
    private String trackCode;
    private String trackName;
    private String topic;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String trainerName;
    private Long studentId;
    private String studentName;
    private String studentRollNumber;
    private CrtAttendanceStatus status;
    private String remarks;
    private LocalDateTime recordedAt;
}
