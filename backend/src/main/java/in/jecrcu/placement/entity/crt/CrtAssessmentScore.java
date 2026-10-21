package in.jecrcu.placement.entity.crt;

import in.jecrcu.placement.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "crt_assessment_scores", uniqueConstraints = {
    @UniqueConstraint(name = "uq_crt_assessment_student", columnNames = {"assessment_id", "student_user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrtAssessmentScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private CrtAssessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @Column(name = "marks_obtained", nullable = false, precision = 5, scale = 2)
    private BigDecimal marksObtained;

    @Column(length = 255)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by_user_id")
    private User gradedBy;

    @Column(name = "graded_at")
    @Builder.Default
    private LocalDateTime gradedAt = LocalDateTime.now();
}
