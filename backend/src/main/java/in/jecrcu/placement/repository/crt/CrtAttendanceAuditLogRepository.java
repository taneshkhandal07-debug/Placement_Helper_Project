package in.jecrcu.placement.repository.crt;

import in.jecrcu.placement.entity.crt.CrtAttendanceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrtAttendanceAuditLogRepository extends JpaRepository<CrtAttendanceAuditLog, Long> {
    List<CrtAttendanceAuditLog> findByRecordIdOrderByCreatedAtDesc(Long recordId);
}
