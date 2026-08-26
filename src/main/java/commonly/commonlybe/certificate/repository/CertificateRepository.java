package commonly.commonlybe.certificate.repository;

import commonly.commonlybe.certificate.entity.CertificateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<CertificateEntity, Long> {
}
