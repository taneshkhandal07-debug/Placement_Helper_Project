package in.jecrcu.placement.entity.integration;

import in.jecrcu.placement.entity.UpcomingDrive;
import in.jecrcu.placement.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "placement_participations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_drive_student", columnNames = {"upcoming_drive_id", "student_user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upcoming_drive_id", nullable = false)
    private UpcomingDrive upcomingDrive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    @Builder.Default
    private PlacementParticipationStatus verificationStatus = PlacementParticipationStatus.REGISTERED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_method", length = 50)
    private String verificationMethod;

    public String getVerificationSource() {
        return verificationMethod;
    }

    public void setVerificationSource(String source) {
        this.verificationMethod = source;
    }

    @Column(length = 255)
    private String remarks;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
