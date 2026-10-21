package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.AcademicAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicAssignmentRepository extends JpaRepository<AcademicAssignment, Long> {
    List<AcademicAssignment> findBySectionIdOrderByDueDateDesc(Long sectionId);
    List<AcademicAssignment> findBySectionIdAndSubjectIdOrderByDueDateDesc(Long sectionId, Long subjectId);
    List<AcademicAssignment> findByFacultyUserIdOrderByCreatedAtDesc(Long facultyUserId);
    List<AcademicAssignment> findBySubjectIdOrderByDueDateDesc(Long subjectId);
}
