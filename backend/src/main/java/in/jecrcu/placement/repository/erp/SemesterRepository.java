package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    List<Semester> findByAcademicYearId(Long academicYearId);
    Optional<Semester> findByAcademicYearIdAndSemesterNumber(Long academicYearId, Integer semesterNumber);
    Optional<Semester> findFirstByIsActiveTrue();
    List<Semester> findByIsActiveTrue();
}
