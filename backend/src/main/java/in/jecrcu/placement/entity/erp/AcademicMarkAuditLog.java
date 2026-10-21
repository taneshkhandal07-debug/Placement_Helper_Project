package in.jecrcu.placement.entity.erp;

import in.jecrcu.placement.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "academic_marks_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicMarkAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mark_id", nullable = false)
    private AcademicMark mark;

    @Column(name = "previous_marks", precision = 5, scale = 2)
    private BigDecimal previousMarks;

    @Column(name = "new_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal newMarks;

    @Column(nullable = false, length = 255)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedByUser;

    @Column(name = "changed_at")
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();
}
