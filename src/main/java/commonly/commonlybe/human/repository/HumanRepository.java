package commonly.commonlybe.human.repository;

import commonly.commonlybe.human.entity.HumanEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HumanRepository extends JpaRepository<HumanEntity, Long>,
        JpaSpecificationExecutor<HumanEntity> {

    boolean existsByNameAndBirthDate(String name, LocalDate birthDate);

    /** 민원인 계정 ↔ 인적사항 매칭. uk_humans_name_birth_date가 1:1을 보장한다. */
    Optional<HumanEntity> findByNameAndBirthDate(String name, LocalDate birthDate);

    boolean existsByNameAndBirthDateAndHumanIdNot(String name, LocalDate birthDate, Long humanId);
}
