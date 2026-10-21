package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {
    Optional<StudentEnrollment> findByUserId(Long userId);
    Optional<StudentEnrollment> findByRollNumberIgnoreCase(String rollNumber);
    List<StudentEnrollment> findBySectionId(Long sectionId);
}
