package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.AcademicAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicAssessmentRepository extends JpaRepository<AcademicAssessment, Long> {
    List<AcademicAssessment> findBySemesterIdOrderByAssessmentDateDesc(Long semesterId);
    List<AcademicAssessment> findBySemesterIdAndSubjectIdOrderByAssessmentDateDesc(Long semesterId, Long subjectId);
    List<AcademicAssessment> findByFacultyUserIdOrderByCreatedAtDesc(Long facultyUserId);
    List<AcademicAssessment> findBySubjectIdAndIsPublishedTrueOrderByAssessmentDateDesc(Long subjectId);
    List<AcademicAssessment> findBySemesterIdAndIsPublishedTrueOrderByAssessmentDateDesc(Long semesterId);
}
