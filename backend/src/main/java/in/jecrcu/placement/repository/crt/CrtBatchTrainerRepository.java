package in.jecrcu.placement.repository.crt;

import in.jecrcu.placement.entity.crt.CrtBatchTrainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrtBatchTrainerRepository extends JpaRepository<CrtBatchTrainer, Long> {
    List<CrtBatchTrainer> findByBatchId(Long batchId);
    List<CrtBatchTrainer> findByTrainerId(Long trainerUserId);
}
