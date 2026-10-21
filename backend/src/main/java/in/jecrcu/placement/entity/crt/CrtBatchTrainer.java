package in.jecrcu.placement.entity.crt;

import in.jecrcu.placement.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crt_batch_trainers", uniqueConstraints = {
    @UniqueConstraint(name = "uq_crt_batch_trainer_track", columnNames = {"batch_id", "trainer_user_id", "track_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrtBatchTrainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private CrtBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_user_id", nullable = false)
    private User trainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private CrtTrack track;

    @Column(name = "role_title", length = 50)
    @Builder.Default
    private String roleTitle = "LEAD_TRAINER";

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
