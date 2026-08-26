package commonly.commonlybe.certificate.repository;

import commonly.commonlybe.certificate.entity.CertificateEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<CertificateEntity, Long> {

    /**
     * 서식 재직사항 표가 시간순이므로 조회도 같은 순서로 준다.
     * Postgres는 ASC 기본이 NULLS LAST라 채용일이 빈 행은 뒤로 간다.
     */
    List<CertificateEntity> findAllByHumanIdOrderByHireDateAscCertificateIdAsc(Long humanId);

    /**
     * humanId 조건이 핵심이다. 빼면 남의 재직 이력이 증명서에 찍힌다.
     */
    List<CertificateEntity> findAllByCertificateIdInAndHumanIdOrderByHireDateAscCertificateIdAsc(
            Collection<Long> certificateIds, Long humanId);

    List<CertificateEntity> findAllByCertificateIdInOrderByHireDateAscCertificateIdAsc(
            Collection<Long> certificateIds);
}
