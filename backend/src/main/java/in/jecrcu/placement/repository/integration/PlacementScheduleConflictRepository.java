package in.jecrcu.placement.repository.integration;

import in.jecrcu.placement.entity.integration.PlacementScheduleConflict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlacementScheduleConflictRepository extends JpaRepository<PlacementScheduleConflict, Long> {
    List<PlacementScheduleConflict> findByUpcomingDriveId(Long upcomingDriveId);
    List<PlacementScheduleConflict> findByStudentId(Long studentUserId);
    List<PlacementScheduleConflict> findByFacultyId(Long facultyUserId);
    List<PlacementScheduleConflict> findByStudentIdAndConflictDate(Long studentUserId, LocalDate conflictDate);
    boolean existsByUpcomingDriveIdAndStudentIdAndConflictDateAndConflictType(
            Long upcomingDriveId, Long studentUserId, LocalDate conflictDate, String conflictType);
}
