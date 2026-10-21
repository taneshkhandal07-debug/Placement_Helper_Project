package in.jecrcu.placement.dto.erp;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemesterReportCardDto {
    private Long studentEnrollmentId;
    private Long studentUserId;
    private String studentName;
    private String studentRollNumber;
    private String branch;
    private String departmentName;
    private Long semesterId;
    private Integer semesterNumber;
    private String termName;
    private String academicYearName;
    private List<SubjectResultDto> subjectResults;
    private Integer totalCreditsRegistered;
    private Integer totalCreditsEarned;
    private BigDecimal totalMarksObtained;
    private BigDecimal totalMaxMarks;
    private BigDecimal overallPercentage;
    private BigDecimal sgpa;
    private String resultStatus; // PASS, FAIL, FIRST CLASS WITH DISTINCTION
    private String generatedAt;
}
