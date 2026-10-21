package in.jecrcu.placement.dto.crt;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrtAttendanceSummaryDto {
    private long totalSessions;
    private long attendedSessions;
    private long lateSessions;
    private long absentSessions;
    private long excusedSessions;
    private double attendancePercentage;
    private boolean debarmentRisk; // true if attendancePercentage < 75.0
    private String standingStatus; // EXCELLENT (>=85%), GOOD (>=75%), DEBARMENT_RISK (<75%)
}
