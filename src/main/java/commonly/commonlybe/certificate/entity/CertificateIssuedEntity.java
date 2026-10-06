package commonly.commonlybe.certificate.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발급된 경력증명서 1건. certificate(재직 이력 한 줄)와 다른 것이다.
 * 한 번 발급에 재직 이력 여러 줄이 들어가고, 같은 이력으로 여러 번 발급될 수 있다.
 */
@Entity
@Table(name = "certificates_issued")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CertificateIssuedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certificate_issued_id")
    private Long certificateIssuedId;

    @Column(name = "human_id", nullable = false)
    private Long humanId;

    @Column(name = "document_no", nullable = false, unique = true, length = 32)
    private String documentNo;

    @Column(name = "purpose")
    private String purpose;

    @Column(name = "other_matters", columnDefinition = "TEXT")
    private String otherMatters;

    /**
     * 담당자가 발급 2단계에서 적는 발급 사유. purpose(용도)와 달리 서식에 찍히지 않고 발급 이력에만 남는다 (#46).
     * certificate.reason(퇴직사유)과는 아무 관계가 없다.
     *
     * nullable이다. #46 이전에 발급된 건에는 값이 없고, NOT NULL로 올리면 기존 데이터가 validate를 통과하지 못한다.
     *
     * ponytail: purpose와 같은 값인지 FE 확인 대기 중이다. 같은 값이라면 이 컬럼을 지우고 purpose를 쓰면 된다.
     */
    @Column(name = "issue_reason", columnDefinition = "TEXT")
    private String issueReason;

    @Column(name = "total_months", nullable = false)
    private int totalMonths;

    @Column(name = "total_days", nullable = false)
    private int totalDays;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    /** S3 object key. #34 이전에 발급된 건은 PDF 없이 저장돼 null이다. */
    @Column(name = "file_path", length = 512)
    private String filePath;

    /**
     * 서식 재직사항 표에 찍히는 순서 그대로. @OrderColumn이 line_no를 채운다.
     * 전용 엔티티 대신 값 컬렉션으로 둔다 - 이 목록은 발급 건 밖에서 조회될 일이 없다.
     */
    @ElementCollection
    @CollectionTable(name = "certificate_issued_items",
            joinColumns = @JoinColumn(name = "certificate_issued_id"))
    @OrderColumn(name = "line_no")
    @Column(name = "certificate_id", nullable = false)
    private List<Long> certificateIds = new ArrayList<>();

    @Builder
    public CertificateIssuedEntity(Long humanId, String documentNo, String purpose, String otherMatters,
                                    String issueReason, int totalMonths, int totalDays, LocalDateTime issuedAt,
                                    String filePath, List<Long> certificateIds) {
        this.humanId = humanId;
        this.documentNo = documentNo;
        this.purpose = purpose;
        this.otherMatters = otherMatters;
        this.issueReason = issueReason;
        this.totalMonths = totalMonths;
        this.totalDays = totalDays;
        this.issuedAt = issuedAt;
        this.filePath = filePath;
        this.certificateIds = new ArrayList<>(certificateIds);
    }
}
