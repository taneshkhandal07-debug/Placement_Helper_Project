package in.jecrcu.placement.repository.crt;

import in.jecrcu.placement.entity.crt.CrtTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CrtTrackRepository extends JpaRepository<CrtTrack, Long> {
    Optional<CrtTrack> findByCode(String code);
}
