package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByDepartmentId(Long departmentId);
    List<Section> findBySemesterId(Long semesterId);
    Optional<Section> findByDepartmentIdAndSemesterIdAndNameIgnoreCase(Long departmentId, Long semesterId, String name);
}
