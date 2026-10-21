package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.AcademicMarkAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicMarkAuditLogRepository extends JpaRepository<AcademicMarkAuditLog, Long> {
    List<AcademicMarkAuditLog> findByMarkIdOrderByChangedAtDesc(Long markId);
    List<AcademicMarkAuditLog> findTop50ByOrderByChangedAtDesc();
}
