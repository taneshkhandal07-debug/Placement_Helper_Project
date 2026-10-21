package in.jecrcu.placement.dto.crt;

import in.jecrcu.placement.entity.crt.CrtAttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrtCorrectionRequestDto {
    @NotNull(message = "Record ID is required")
    private Long recordId;

    @NotNull(message = "Requested status is required")
    private CrtAttendanceStatus requestedStatus;

    @NotBlank(message = "Reason is required")
    private String reason;
}
