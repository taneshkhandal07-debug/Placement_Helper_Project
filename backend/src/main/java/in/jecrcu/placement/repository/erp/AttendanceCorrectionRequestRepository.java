package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.AttendanceCorrectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceCorrectionRequestRepository extends JpaRepository<AttendanceCorrectionRequest, Long> {
    List<AttendanceCorrectionRequest> findByRequestedByUserIdOrderByCreatedAtDesc(Long requestedByUserId);
    List<AttendanceCorrectionRequest> findByStatusOrderByCreatedAtDesc(String status);
}
