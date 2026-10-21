package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByStudentEnrollmentId(Long studentEnrollmentId);
    List<AttendanceRecord> findBySessionId(Long sessionId);
    Optional<AttendanceRecord> findBySessionIdAndStudentEnrollmentId(Long sessionId, Long studentEnrollmentId);

    @Query("SELECT ar FROM AttendanceRecord ar " +
           "JOIN ar.session s " +
           "WHERE ar.studentEnrollment.id = :studentEnrollmentId " +
           "AND s.subject.id = :subjectId " +
           "ORDER BY s.sessionDate DESC, s.startTime DESC")
    List<AttendanceRecord> findByStudentEnrollmentIdAndSubjectId(
            @Param("studentEnrollmentId") Long studentEnrollmentId,
            @Param("subjectId") Long subjectId);
}
