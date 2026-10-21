package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.AcademicSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicSubmissionRepository extends JpaRepository<AcademicSubmission, Long> {
    List<AcademicSubmission> findByAssignmentIdOrderBySubmittedAtAsc(Long assignmentId);
    List<AcademicSubmission> findByStudentEnrollmentIdOrderBySubmittedAtDesc(Long studentEnrollmentId);
    Optional<AcademicSubmission> findByAssignmentIdAndStudentEnrollmentId(Long assignmentId, Long studentEnrollmentId);
}
