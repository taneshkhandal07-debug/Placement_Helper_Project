package in.jecrcu.placement.entity.crt;

import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.entity.erp.AcademicYear;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crt_enrollments", uniqueConstraints = {
    @UniqueConstraint(name = "uq_student_year_crt", columnNames = {"student_user_id", "academic_year_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrtEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private CrtBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CrtEnrollmentStatus status = CrtEnrollmentStatus.REQUESTED;

    @Column(name = "opt_in_notes", columnDefinition = "TEXT")
    private String optInNotes;

    @Column(name = "opted_in_at")
    @Builder.Default
    private LocalDateTime optedInAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_remarks", length = 255)
    private String verificationRemarks;
}
