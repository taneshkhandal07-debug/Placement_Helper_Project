package in.jecrcu.placement.dto.integration;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DutyExemptionActionDto {
    private Long exemptionId;
    private String action; // REVOKE or REINSTATE
    private String remarks;
}
