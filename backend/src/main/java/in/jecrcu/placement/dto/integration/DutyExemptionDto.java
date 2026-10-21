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
public class DutyExemptionDto {
    private Long id;
    private Long participationId;
    private Long upcomingDriveId;
    private String companyName;
    private Long studentId;
    private String studentName;
    private String studentRollNumber;
    private String targetType; // REGULAR_CLASS or CRT_TRAINING
    private Long attendanceRecordId;
    private Long crtAttendanceRecordId;
    private LocalDate conflictDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String originalStatus;
    private String exemptionStatus; // GRANTED or REVOKED
    private String dutyPolicyCode;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String remarks;
    private LocalDateTime createdAt;
}
