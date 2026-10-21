package in.jecrcu.placement.repository.crt;

import in.jecrcu.placement.entity.crt.CrtAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrtAssessmentRepository extends JpaRepository<CrtAssessment, Long> {
    List<CrtAssessment> findByBatchIdOrderByAssessmentDateDesc(Long batchId);
}
