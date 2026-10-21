package in.jecrcu.placement.dto.crt;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrtVerificationActionDto {
    @NotBlank(message = "Action must be APPROVE or REJECT")
    private String action; // APPROVE or REJECT

    private Long assignedBatchId;

    private String remarks;
}
