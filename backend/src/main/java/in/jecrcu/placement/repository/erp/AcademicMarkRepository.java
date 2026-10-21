package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.AcademicMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicMarkRepository extends JpaRepository<AcademicMark, Long> {
    List<AcademicMark> findByAssessmentId(Long assessmentId);
    List<AcademicMark> findByStudentEnrollmentId(Long studentEnrollmentId);
    Optional<AcademicMark> findByAssessmentIdAndStudentEnrollmentId(Long assessmentId, Long studentEnrollmentId);

    @Query("SELECT m FROM AcademicMark m " +
           "JOIN m.assessment a " +
           "WHERE m.studentEnrollment.id = :studentEnrollmentId " +
           "AND a.isPublished = true " +
           "AND a.semester.id = :semesterId " +
           "ORDER BY a.subject.code ASC, a.assessmentDate ASC")
    List<AcademicMark> findPublishedMarksByStudentAndSemester(
            @Param("studentEnrollmentId") Long studentEnrollmentId,
            @Param("semesterId") Long semesterId);

    @Query("SELECT m FROM AcademicMark m " +
           "JOIN m.assessment a " +
           "WHERE m.studentEnrollment.id = :studentEnrollmentId " +
           "AND a.isPublished = true " +
           "AND a.subject.id = :subjectId " +
           "ORDER BY a.assessmentDate ASC")
    List<AcademicMark> findPublishedMarksByStudentAndSubject(
            @Param("studentEnrollmentId") Long studentEnrollmentId,
            @Param("subjectId") Long subjectId);
}
