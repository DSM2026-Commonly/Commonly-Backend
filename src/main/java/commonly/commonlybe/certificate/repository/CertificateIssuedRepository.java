package commonly.commonlybe.certificate.repository;

import commonly.commonlybe.certificate.entity.CertificateIssuedEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateIssuedRepository extends JpaRepository<CertificateIssuedEntity, Long> {
}
