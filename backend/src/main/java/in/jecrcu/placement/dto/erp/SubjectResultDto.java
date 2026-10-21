package in.jecrcu.placement.dto.erp;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectResultDto {
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer credits;
    private String subjectType;
    private BigDecimal internalMarksObtained;
    private BigDecimal internalMaxMarks;
    private BigDecimal endTermMarksObtained;
    private BigDecimal endTermMaxMarks;
    private BigDecimal totalMarksObtained;
    private BigDecimal totalMaxMarks;
    private BigDecimal percentage;
    private String letterGrade; // O, A+, A, B+, B, C, F
    private Integer gradePoint; // 10, 9, 8, 7, 6, 5, 0
    private Integer creditPoints; // credits * gradePoint
    private String result; // PASS, FAIL
}
