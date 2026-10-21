package in.jecrcu.placement.repository.integration;

import in.jecrcu.placement.entity.integration.PlacementDutyExemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlacementDutyExemptionRepository extends JpaRepository<PlacementDutyExemption, Long> {
    List<PlacementDutyExemption> findByStudentIdOrderByConflictDateDesc(Long studentUserId);
    List<PlacementDutyExemption> findByParticipationId(Long participationId);
    Optional<PlacementDutyExemption> findByAttendanceRecordId(Long attendanceRecordId);
    Optional<PlacementDutyExemption> findByCrtAttendanceRecordId(Long crtAttendanceRecordId);
    List<PlacementDutyExemption> findByExemptionStatusOrderByApprovedAtDesc(String exemptionStatus);
}
