package in.jecrcu.placement.dto.erp;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordMarkRequestDto {
    private Long assessmentId;
    private Long studentEnrollmentId;
    private BigDecimal marksObtained;
    private Boolean isAbsent;
    private String remarks;
}
