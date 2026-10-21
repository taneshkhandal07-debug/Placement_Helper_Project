package in.jecrcu.placement.repository.erp;

import in.jecrcu.placement.entity.erp.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByDepartmentId(Long departmentId);
    Optional<Subject> findByCodeIgnoreCase(String code);
}
