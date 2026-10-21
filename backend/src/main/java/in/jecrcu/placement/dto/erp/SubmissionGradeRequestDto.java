package in.jecrcu.placement.dto.erp;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionGradeRequestDto {
    private BigDecimal marksObtained;
    private String feedback;
}
