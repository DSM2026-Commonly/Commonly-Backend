package commonly.commonlybe.certificate.repository;

import commonly.commonlybe.certificate.controller.dto.IssuanceHistoryResponse;
import commonly.commonlybe.certificate.entity.CertificateIssuedEntity;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CertificateIssuedRepository extends JpaRepository<CertificateIssuedEntity, Long> {

    @Query("""
            select new commonly.commonlybe.certificate.controller.dto.IssuanceHistoryResponse(
                i.certificateIssuedId, i.documentNo, i.humanId, h.name, i.purpose,
                i.totalMonths, i.totalDays, i.issuedAt)
            from CertificateIssuedEntity i
            join HumanEntity h on h.humanId = i.humanId
            where h.name like concat('%', :keyword, '%')
              and i.issuedAt >= :start and i.issuedAt < :end
            order by i.issuedAt desc
            """)
    Page<IssuanceHistoryResponse> searchHistories(@Param("keyword") String keyword,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end,
                                                  Pageable pageable);
}
