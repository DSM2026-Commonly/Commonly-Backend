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

    @Column(name = "total_months", nullable = false)
    private int totalMonths;

    @Column(name = "total_days", nullable = false)
    private int totalDays;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    /** S3 object key. PDF 생성 전까지는 null. */
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
                                    int totalMonths, int totalDays, LocalDateTime issuedAt,
                                    List<Long> certificateIds) {
        this.humanId = humanId;
        this.documentNo = documentNo;
        this.purpose = purpose;
        this.otherMatters = otherMatters;
        this.totalMonths = totalMonths;
        this.totalDays = totalDays;
        this.issuedAt = issuedAt;
        this.certificateIds = new ArrayList<>(certificateIds);
    }
}
