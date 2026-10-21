package in.jecrcu.placement.dto.erp;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentMarkDto {
    private Long id;
    private Long assessmentId;
    private String assessmentName;
    private String assessmentType;
    private BigDecimal maxMarks;
    private BigDecimal passingMarks;
    private BigDecimal weightagePercent;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Long studentEnrollmentId;
    private Long studentUserId;
    private String studentName;
    private String studentRollNumber;
    private BigDecimal marksObtained;
    private Boolean isAbsent;
    private boolean isPassed;
    private String remarks;
    private Long gradedByUserId;
    private String gradedByName;
    private LocalDateTime gradedAt;
}
