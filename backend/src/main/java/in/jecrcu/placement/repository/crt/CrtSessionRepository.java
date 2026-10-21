package in.jecrcu.placement.repository.crt;

import in.jecrcu.placement.entity.crt.CrtSession;
import in.jecrcu.placement.entity.crt.CrtSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CrtSessionRepository extends JpaRepository<CrtSession, Long> {
    List<CrtSession> findByBatchIdOrderBySessionDateDescStartTimeDesc(Long batchId);
    List<CrtSession> findByBatchIdAndStatusOrderBySessionDateAscStartTimeAsc(Long batchId, CrtSessionStatus status);
    List<CrtSession> findByTrainerIdOrderBySessionDateDesc(Long trainerUserId);
    List<CrtSession> findByBatchIdAndSessionDate(Long batchId, LocalDate sessionDate);
}
