package in.jecrcu.placement.entity.integration;

import in.jecrcu.placement.entity.UpcomingDrive;
import in.jecrcu.placement.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "placement_schedule_conflicts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementScheduleConflict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upcoming_drive_id", nullable = false)
    private UpcomingDrive upcomingDrive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @Column(name = "conflict_type", nullable = false, length = 30)
    private String conflictType; // REGULAR_CLASS or CRT_TRAINING

    @Column(name = "session_description", nullable = false, length = 255)
    private String sessionDescription;

    @Column(name = "conflict_date", nullable = false)
    private LocalDate conflictDate;

    @Column(name = "conflict_start", nullable = false)
    private LocalTime conflictStart;

    @Column(name = "conflict_end", nullable = false)
    private LocalTime conflictEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_user_id")
    private User faculty;

    @Column(name = "student_notified")
    @Builder.Default
    private Boolean studentNotified = false;

    @Column(name = "faculty_notified")
    @Builder.Default
    private Boolean facultyNotified = false;

    @Column(name = "detected_at")
    @Builder.Default
    private LocalDateTime detectedAt = LocalDateTime.now();
}
