package in.jecrcu.placement.repository.integration;

import in.jecrcu.placement.entity.integration.PlacementIntegrationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlacementIntegrationAuditLogRepository extends JpaRepository<PlacementIntegrationAuditLog, Long> {
    List<PlacementIntegrationAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
    List<PlacementIntegrationAuditLog> findTop50ByOrderByCreatedAtDesc();
}
