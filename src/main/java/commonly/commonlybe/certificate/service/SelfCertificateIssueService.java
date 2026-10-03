package commonly.commonlybe.certificate.service;

import commonly.commonlybe.certificate.controller.dto.CertificateIssueRequest;
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

    @Transactional
    public CertificateIssueResponse issue(AuthDetails authDetails, SelfCertificateIssueRequest request) {
        if (!selfIssueEnabled) {
            throw new CertificateException(CertificateErrorCode.SELF_ISSUE_DISABLED);
        }

        HumanEntity human = petitionerHumanResolver.resolve(authDetails);

        List<Long> certificateIds =
                certificateRepository.findAllByHumanIdOrderByHireDateAscCertificateIdAsc(human.getHumanId())
                        .stream()
                        .map(CertificateEntity::getCertificateId)
                        .toList();

        if (certificateIds.isEmpty()) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_NOT_FOUND);
        }
        // 본인 발급은 이력을 고를 수 없다. 넘치면 담당자에게 넘긴다.
        if (certificateIds.size() > MAX_CERTIFICATES) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_LIMIT_EXCEEDED);
        }

        return certificateIssueService.issue(new CertificateIssueRequest(
                human.getHumanId(), certificateIds, request.purpose(), request.otherMatters()));
    }
}
