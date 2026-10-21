package in.jecrcu.placement.repository.crt;

import in.jecrcu.placement.entity.crt.CrtEnrollment;
import in.jecrcu.placement.entity.crt.CrtEnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrtEnrollmentRepository extends JpaRepository<CrtEnrollment, Long> {
    Optional<CrtEnrollment> findByStudentIdAndAcademicYearId(Long studentUserId, Long academicYearId);
    Optional<CrtEnrollment> findByStudentId(Long studentUserId);
    List<CrtEnrollment> findByBatchId(Long batchId);
    List<CrtEnrollment> findByStatus(CrtEnrollmentStatus status);

    @Query("SELECT e FROM CrtEnrollment e WHERE e.student.id = :studentId AND e.status = 'VERIFIED'")
    Optional<CrtEnrollment> findVerifiedByStudentId(@Param("studentId") Long studentId);
}
