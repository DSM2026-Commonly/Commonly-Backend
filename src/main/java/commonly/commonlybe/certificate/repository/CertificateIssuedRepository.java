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

    /**
     * 인적사항 삭제 가드용. human_id가 @ManyToOne이 아닌 raw 컬럼이라
     * DB/JPA가 참조 무결성을 봐주지 않아 직접 확인해야 한다.
     */
    boolean existsByHumanId(Long humanId);

    @Query("""
            select new commonly.commonlybe.certificate.controller.dto.IssuanceHistoryResponse(
                i.certificateIssuedId, i.documentNo, i.humanId, h.name, i.purpose,
                i.issueReason, i.totalMonths, i.totalDays, i.issuedAt)
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
