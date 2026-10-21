package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.FacultyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyAssignmentRepository extends JpaRepository<FacultyAssignment, Long> {
    List<FacultyAssignment> findByFacultyUserId(Long facultyUserId);
    List<FacultyAssignment> findBySectionId(Long sectionId);
    List<FacultyAssignment> findBySubjectId(Long subjectId);
    Optional<FacultyAssignment> findByFacultyUserIdAndSubjectIdAndSectionId(Long facultyUserId, Long subjectId, Long sectionId);
}
