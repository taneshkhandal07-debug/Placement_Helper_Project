package in.jecrcu.placement.dto.erp;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentDto {
    private Long id;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Long sectionId;
    private String sectionName;
    private Long facultyUserId;
    private String facultyName;
    private String title;
    private String description;
    private String attachmentUrl;
    private LocalDateTime dueDate;
    private String formattedDueDate;
    private BigDecimal maxMarks;
    private Boolean allowLateSubmission;
    private BigDecimal latePenaltyPercentage;
    private String status; // PUBLISHED, CLOSED
    private boolean isPastDue;
    private boolean submittedByCurrentUser;
    private Long submissionId;
    private String submissionStatus;
    private BigDecimal marksObtained;
    private LocalDateTime createdAt;
}
