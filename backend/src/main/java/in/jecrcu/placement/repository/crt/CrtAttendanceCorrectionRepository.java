package in.jecrcu.placement.repository.crt;

import in.jecrcu.placement.entity.crt.CrtAttendanceCorrection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrtAttendanceCorrectionRepository extends JpaRepository<CrtAttendanceCorrection, Long> {
    List<CrtAttendanceCorrection> findByStudentIdOrderByCreatedAtDesc(Long studentUserId);
    List<CrtAttendanceCorrection> findByStatusOrderByCreatedAtAsc(String status);
}
