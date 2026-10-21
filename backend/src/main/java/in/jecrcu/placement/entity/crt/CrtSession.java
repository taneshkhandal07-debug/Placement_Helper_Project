package in.jecrcu.placement.entity.crt;

import in.jecrcu.placement.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "crt_sessions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_crt_session", columnNames = {"batch_id", "track_id", "session_date", "start_time"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrtSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private CrtBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private CrtTrack track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_user_id", nullable = false)
    private User trainer;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, length = 255)
    private String topic;

    @Column(name = "location_or_link", length = 255)
    private String locationOrLink;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private CrtSessionStatus status = CrtSessionStatus.SCHEDULED;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
