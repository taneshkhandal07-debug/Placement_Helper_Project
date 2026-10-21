package in.jecrcu.placement.dto.erp;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionDto {
    private Long id;
    private Long assignmentId;
    private String assignmentTitle;
    private BigDecimal maxMarks;
    private Long studentEnrollmentId;
    private Long studentUserId;
    private String studentName;
    private String studentRollNumber;
    private String submissionText;
    private String fileUrl;
    private LocalDateTime submittedAt;
    private Boolean isLate;
    private BigDecimal marksObtained;
    private String feedback;
    private Long gradedByUserId;
    private String gradedByName;
    private LocalDateTime gradedAt;
    private String status; // SUBMITTED, GRADED, RESUBMISSION_REQUESTED
}
