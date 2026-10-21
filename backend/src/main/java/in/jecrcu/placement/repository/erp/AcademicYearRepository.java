package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    Optional<AcademicYear> findByYearName(String yearName);
    Optional<AcademicYear> findByIsCurrentTrue();
}
