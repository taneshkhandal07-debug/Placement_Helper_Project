package in.jecrcu.placement.dto.crt;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrtOptInRequestDto {
    @Size(max = 1000, message = "Opt-in notes must not exceed 1000 characters")
    private String optInNotes;

    private Long preferredBatchId;
}
