package in.jecrcu.placement.dto.crt;

import in.jecrcu.placement.entity.crt.CrtEnrollmentStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrtEligibilityDto {
    private boolean fourthYearEligible;
    private String eligibilityReason;
    private String currentAcademicSemester;
    private Integer graduationYear;
    private CrtEnrollmentStatus enrollmentStatus;
    private Long enrollmentId;
    private Long batchId;
    private String batchName;
    private String batchCode;
    private String optInNotes;
    private LocalDateTime optedInAt;
    private LocalDateTime verifiedAt;
    private String verificationRemarks;
}
