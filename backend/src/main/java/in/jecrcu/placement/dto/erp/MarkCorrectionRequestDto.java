package in.jecrcu.placement.dto.erp;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkCorrectionRequestDto {
    private Long markId;
    private BigDecimal newMarks;
    private String reason;
}
