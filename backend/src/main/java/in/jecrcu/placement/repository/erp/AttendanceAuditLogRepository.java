package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.AttendanceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceAuditLogRepository extends JpaRepository<AttendanceAuditLog, Long> {
    List<AttendanceAuditLog> findByAttendanceRecordIdOrderByCreatedAtDesc(Long attendanceRecordId);
}
