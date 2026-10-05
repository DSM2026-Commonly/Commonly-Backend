package commonly.commonlybe.certificate.service;

import commonly.commonlybe.certificate.controller.dto.CertificateIssueRequest;
import commonly.commonlybe.certificate.controller.dto.CertificateItemDto;
import commonly.commonlybe.certificate.controller.dto.CertificateIssueResponse;
import commonly.commonlybe.certificate.controller.dto.SelfCertificateIssueRequest;
import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.exception.CertificateErrorCode;
import commonly.commonlybe.certificate.exception.CertificateException;
import commonly.commonlybe.certificate.repository.CertificateRepository;
import commonly.commonlybe.global.security.auth.AuthDetails;
import commonly.commonlybe.human.entity.HumanEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 본인 발급은 담당자 발급(CertificateIssueService)의 얇은 래퍼다.
 * 다른 점은 humanId와 certificateIds를 요청이 아니라 인증 주체에서 끌어온다는 것뿐이다.
 */
@Service
@RequiredArgsConstructor
public class SelfCertificateIssueService {

    /** 서식 재직사항 표가 10행 고정이다. */
    private static final int MAX_CERTIFICATES = 10;

    /**
     * SecurityConfig가 이미 라우트를 막지만, 매처 순서가 바뀌거나 다른 경로가 생겨도
     * 새지 않도록 서비스에서도 같은 스위치를 본다. 기본값은 차단이다.
     */
    @Value("${app.certificate.self-issue-enabled:false}")
    private boolean selfIssueEnabled;

    private final PetitionerHumanResolver petitionerHumanResolver;
    private final CertificateRepository certificateRepository;
    private final CertificateIssueService certificateIssueService;

    /** 선택 발급용 본인 재직 이력 목록. 신원 노출 위험이 발급과 같아 같은 스위치를 본다. */
    @Transactional(readOnly = true)
    public List<CertificateItemDto> findMine(AuthDetails authDetails) {
        HumanEntity human = resolveHuman(authDetails);
        return certificateRepository.findAllByHumanIdOrderByHireDateAscCertificateIdAsc(human.getHumanId())
                .stream()
                .map(CertificateItemDto::from)
                .toList();
    }

    @Transactional
    public CertificateIssueResponse issue(AuthDetails authDetails, SelfCertificateIssueRequest request) {
        return certificateIssueService.issue(toIssueRequest(authDetails, request));
    }

    /** 발급과 같은 규칙으로 대상을 정하고 PDF만 만든다. 문서번호·발급 건은 생기지 않는다. */
    @Transactional(readOnly = true)
    public byte[] preview(AuthDetails authDetails, SelfCertificateIssueRequest request) {
        return certificateIssueService.preview(toIssueRequest(authDetails, request));
    }

    private CertificateIssueRequest toIssueRequest(AuthDetails authDetails, SelfCertificateIssueRequest request) {
        HumanEntity human = resolveHuman(authDetails);

        // 고른 게 있으면 그대로 넘긴다. 소유권은 CertificateIssueService가 humanId로 검사한다.
        if (request.certificateIds() != null && !request.certificateIds().isEmpty()) {
            return new CertificateIssueRequest(
                    human.getHumanId(), request.certificateIds(), request.purpose(), request.otherMatters());
        }

        List<Long> certificateIds =
                certificateRepository.findAllByHumanIdOrderByHireDateAscCertificateIdAsc(human.getHumanId())
                        .stream()
                        .map(CertificateEntity::getCertificateId)
                        .toList();

        if (certificateIds.isEmpty()) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_NOT_FOUND);
        }
        // 안 골랐는데 서식 10행을 넘으면 고르라고 돌려보낸다.
        if (certificateIds.size() > MAX_CERTIFICATES) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_LIMIT_EXCEEDED);
        }

        return new CertificateIssueRequest(
                human.getHumanId(), certificateIds, request.purpose(), request.otherMatters());
    }

    private HumanEntity resolveHuman(AuthDetails authDetails) {
        if (!selfIssueEnabled) {
            throw new CertificateException(CertificateErrorCode.SELF_ISSUE_DISABLED);
        }
        return petitionerHumanResolver.resolve(authDetails);
    }
}
