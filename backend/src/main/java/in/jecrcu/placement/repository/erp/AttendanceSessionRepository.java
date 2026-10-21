package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findBySectionIdAndSubjectIdOrderBySessionDateDescStartTimeDesc(Long sectionId, Long subjectId);
    List<AttendanceSession> findByFacultyUserIdOrderBySessionDateDescStartTimeDesc(Long facultyUserId);
    List<AttendanceSession> findBySectionIdOrderBySessionDateDescStartTimeDesc(Long sectionId);
    List<AttendanceSession> findBySectionIdAndSessionDate(Long sectionId, LocalDate sessionDate);
    Optional<AttendanceSession> findBySectionIdAndSubjectIdAndSessionDateAndStartTime(Long sectionId, Long subjectId, LocalDate sessionDate, LocalTime startTime);
}
