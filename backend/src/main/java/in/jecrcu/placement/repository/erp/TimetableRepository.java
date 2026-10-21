package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findBySectionIdOrderByDayOfWeekAscStartTimeAsc(Long sectionId);
    List<Timetable> findByFacultyUserIdOrderByDayOfWeekAscStartTimeAsc(Long facultyUserId);
}
