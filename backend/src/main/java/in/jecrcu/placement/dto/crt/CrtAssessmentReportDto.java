package in.jecrcu.placement.dto.crt;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrtAssessmentReportDto {
    private Long assessmentId;
    private String title;
    private String trackName;
    private LocalDate assessmentDate;
    private BigDecimal maxMarks;
    private BigDecimal passPercentage;
    private BigDecimal marksObtained;
    private BigDecimal percentage;
    private boolean passed;
    private String remarks;
}
