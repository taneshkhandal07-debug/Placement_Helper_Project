package in.jecrcu.placement.repository.crt;

import in.jecrcu.placement.entity.crt.CrtAttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrtAttendanceRecordRepository extends JpaRepository<CrtAttendanceRecord, Long> {
    List<CrtAttendanceRecord> findBySessionId(Long sessionId);
    List<CrtAttendanceRecord> findByStudentIdOrderBySessionSessionDateDesc(Long studentUserId);
    Optional<CrtAttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentUserId);

    @Query("SELECT r FROM CrtAttendanceRecord r WHERE r.student.id = :studentId AND r.session.batch.id = :batchId")
    List<CrtAttendanceRecord> findByStudentIdAndBatchId(@Param("studentId") Long studentId, @Param("batchId") Long batchId);

    @Query("SELECT COUNT(r) FROM CrtAttendanceRecord r WHERE r.session.batch.id = :batchId AND r.student.id = :studentId")
    long countTotalByStudentAndBatch(@Param("studentId") Long studentId, @Param("batchId") Long batchId);

    @Query("SELECT COUNT(r) FROM CrtAttendanceRecord r WHERE r.session.batch.id = :batchId AND r.student.id = :studentId AND r.status IN ('PRESENT', 'LATE')")
    long countAttendedByStudentAndBatch(@Param("studentId") Long studentId, @Param("batchId") Long batchId);
}
