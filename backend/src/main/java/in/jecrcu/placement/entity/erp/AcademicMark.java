package in.jecrcu.placement.entity.erp;

import in.jecrcu.placement.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "academic_marks", uniqueConstraints = {
    @UniqueConstraint(name = "uq_assessment_student_mark", columnNames = {"assessment_id", "student_enrollment_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private AcademicAssessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_enrollment_id", nullable = false)
    private StudentEnrollment studentEnrollment;

    @Column(name = "marks_obtained", nullable = false, precision = 5, scale = 2)
    private BigDecimal marksObtained;

    @Column(name = "is_absent")
    @Builder.Default
    private Boolean isAbsent = false;

    @Column(length = 255)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by_user_id")
    private User gradedByUser;

    @Column(name = "graded_at")
    @Builder.Default
    private LocalDateTime gradedAt = LocalDateTime.now();
}
