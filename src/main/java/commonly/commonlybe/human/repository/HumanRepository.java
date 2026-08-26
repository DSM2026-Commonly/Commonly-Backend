package commonly.commonlybe.human.repository;

import commonly.commonlybe.human.entity.HumanEntity;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HumanRepository extends JpaRepository<HumanEntity, Long>,
        JpaSpecificationExecutor<HumanEntity> {

    boolean existsByNameAndBirthDate(String name, LocalDate birthDate);

    boolean existsByNameAndBirthDateAndHumanIdNot(String name, LocalDate birthDate, Long humanId);
}
