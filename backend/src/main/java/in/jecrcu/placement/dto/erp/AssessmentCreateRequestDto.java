package in.jecrcu.placement.dto.erp;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentCreateRequestDto {
    private Long subjectId;
    private Long semesterId;
    private Long sectionId;
    private String name;
    private String assessmentType;
    private BigDecimal weightagePercent;
    private BigDecimal maxMarks;
    private BigDecimal passingMarks;
    private LocalDate assessmentDate;
}
