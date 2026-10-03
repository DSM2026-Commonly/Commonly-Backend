package commonly.commonlybe.petitioner.repository;

import commonly.commonlybe.petitioner.entity.Petitioner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetitionerRepository extends JpaRepository<Petitioner, Long> {
}
