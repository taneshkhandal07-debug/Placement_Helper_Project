package in.jecrcu.placement.dto.erp;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentDto {
    private Long id;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Long semesterId;
    private String semesterTerm;
    private Long sectionId;
    private String sectionName;
    private Long facultyUserId;
    private String facultyName;
    private String name;
    private String assessmentType; // INTERNAL_1, INTERNAL_2, PRACTICAL, END_TERM
    private BigDecimal weightagePercent;
    private BigDecimal maxMarks;
    private BigDecimal passingMarks;
    private LocalDate assessmentDate;
    private Boolean isPublished;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private int evaluatedCount;
}
