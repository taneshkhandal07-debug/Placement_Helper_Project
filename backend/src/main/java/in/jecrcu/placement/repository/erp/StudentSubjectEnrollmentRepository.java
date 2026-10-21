package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.StudentSubjectEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentSubjectEnrollmentRepository extends JpaRepository<StudentSubjectEnrollment, Long> {
    List<StudentSubjectEnrollment> findByStudentEnrollmentId(Long studentEnrollmentId);
    Optional<StudentSubjectEnrollment> findByStudentEnrollmentIdAndSubjectId(Long studentEnrollmentId, Long subjectId);
}
