package in.jecrcu.placement.dto.erp;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentCreateRequestDto {
    private Long subjectId;
    private Long sectionId;
    private String title;
    private String description;
    private String attachmentUrl;
    private LocalDateTime dueDate;
    private BigDecimal maxMarks;
    private Boolean allowLateSubmission;
    private BigDecimal latePenaltyPercentage;
}
