package in.jecrcu.placement.entity.integration;

import in.jecrcu.placement.entity.User;
import in.jecrcu.placement.entity.crt.CrtAttendanceRecord;
import in.jecrcu.placement.entity.erp.AttendanceRecord;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "placement_duty_exemptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementDutyExemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false)
    private PlacementParticipation participation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType; // REGULAR_CLASS or CRT_TRAINING

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_record_id")
    private AttendanceRecord attendanceRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crt_attendance_record_id")
    private CrtAttendanceRecord crtAttendanceRecord;

    @Column(name = "conflict_date", nullable = false)
    private LocalDate conflictDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "original_status", length = 30)
    private String originalStatus;

    @Column(name = "exemption_status", nullable = false, length = 30)
    @Builder.Default
    private String exemptionStatus = "GRANTED"; // GRANTED or REVOKED

    @Column(name = "duty_policy_code", length = 50)
    @Builder.Default
    private String dutyPolicyCode = "INST_PLACEMENT_DUTY_V1";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Column(name = "approved_at")
    @Builder.Default
    private LocalDateTime approvedAt = LocalDateTime.now();

    @Column(length = 255)
    private String remarks;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
