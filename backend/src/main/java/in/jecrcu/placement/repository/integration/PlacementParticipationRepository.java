package in.jecrcu.placement.repository.integration;

import in.jecrcu.placement.entity.integration.PlacementParticipation;
import in.jecrcu.placement.entity.integration.PlacementParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlacementParticipationRepository extends JpaRepository<PlacementParticipation, Long> {
    Optional<PlacementParticipation> findByUpcomingDriveIdAndStudentId(Long upcomingDriveId, Long studentUserId);
    List<PlacementParticipation> findByUpcomingDriveId(Long upcomingDriveId);
    List<PlacementParticipation> findByStudentIdOrderByCreatedAtDesc(Long studentUserId);
    List<PlacementParticipation> findByUpcomingDriveIdAndVerificationStatus(Long upcomingDriveId, PlacementParticipationStatus status);
}
