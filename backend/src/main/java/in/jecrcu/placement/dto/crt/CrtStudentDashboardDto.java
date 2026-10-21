package in.jecrcu.placement.dto.crt;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrtStudentDashboardDto {
    private CrtEligibilityDto eligibility;
    private CrtAttendanceSummaryDto attendanceSummary;
    private List<CrtSessionDto> upcomingSessions;
    private List<CrtAttendanceRecordDto> recentAttendance;
    private List<CrtAssessmentReportDto> assessmentReports;
}
