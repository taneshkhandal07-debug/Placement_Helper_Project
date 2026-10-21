package in.jecrcu.placement.dto.erp;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionSubmitRequestDto {
    private String submissionText;
    private String fileUrl;
}
