package commonly.commonlybe.domain.petitioner.domain.repository;

import commonly.commonlybe.domain.petitioner.domain.Petitioner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetitionerRepository extends JpaRepository<Petitioner, Long> {
}
