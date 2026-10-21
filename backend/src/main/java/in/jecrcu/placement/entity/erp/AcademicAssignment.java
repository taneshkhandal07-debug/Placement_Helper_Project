package in.jecrcu.placement.entity.erp;

import in.jecrcu.placement.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "academic_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_user_id", nullable = false)
    private User facultyUser;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "max_marks", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal maxMarks = BigDecimal.valueOf(100.00);

    @Column(name = "allow_late_submission")
    @Builder.Default
    private Boolean allowLateSubmission = true;

    @Column(name = "late_penalty_percentage", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal latePenaltyPercentage = BigDecimal.valueOf(10.00);

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PUBLISHED"; // DRAFT, PUBLISHED, CLOSED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
