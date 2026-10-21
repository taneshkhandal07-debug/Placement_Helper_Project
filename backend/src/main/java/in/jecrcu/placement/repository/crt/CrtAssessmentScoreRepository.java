package in.jecrcu.placement.repository.crt;

import in.jecrcu.placement.entity.crt.CrtAssessmentScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrtAssessmentScoreRepository extends JpaRepository<CrtAssessmentScore, Long> {
    List<CrtAssessmentScore> findByStudentIdOrderByAssessmentAssessmentDateDesc(Long studentUserId);
    Optional<CrtAssessmentScore> findByAssessmentIdAndStudentId(Long assessmentId, Long studentUserId);
    List<CrtAssessmentScore> findByAssessmentId(Long assessmentId);
}
