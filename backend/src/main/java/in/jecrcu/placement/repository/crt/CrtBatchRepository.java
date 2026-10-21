package in.jecrcu.placement.repository.crt;

import in.jecrcu.placement.entity.crt.CrtBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrtBatchRepository extends JpaRepository<CrtBatch, Long> {
    Optional<CrtBatch> findByBatchCode(String batchCode);
    List<CrtBatch> findByIsActiveTrue();
    List<CrtBatch> findByAcademicYearId(Long academicYearId);
}
