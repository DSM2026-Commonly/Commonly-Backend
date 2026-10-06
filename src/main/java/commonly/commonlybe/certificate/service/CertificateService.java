package commonly.commonlybe.certificate.service;

import commonly.commonlybe.certificate.controller.dto.CertificateCreateRequest;
import commonly.commonlybe.certificate.controller.dto.CertificateCreateResponse;
import commonly.commonlybe.certificate.controller.dto.CertificateDetailResponse;
import commonly.commonlybe.certificate.controller.dto.CertificateHumanDto;
import commonly.commonlybe.certificate.controller.dto.CertificateItemDto;
import commonly.commonlybe.certificate.controller.dto.CertificateUpdateRequest;
import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.entity.CertificateIssuedEntity;
import commonly.commonlybe.certificate.entity.Gender;
import commonly.commonlybe.certificate.exception.CertificateErrorCode;
import commonly.commonlybe.certificate.exception.CertificateException;
import commonly.commonlybe.certificate.repository.CertificateIssuedRepository;
import commonly.commonlybe.certificate.repository.CertificateRepository;
import commonly.commonlybe.global.s3.S3Uploader;
import commonly.commonlybe.global.security.auth.AuthDetails;
import commonly.commonlybe.human.entity.HumanEntity;
import commonly.commonlybe.human.exception.HumanErrorCode;
import commonly.commonlybe.human.exception.HumanException;
import commonly.commonlybe.human.repository.HumanRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final HumanRepository humanRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateIssuedRepository certificateIssuedRepository;
    private final S3Uploader s3Uploader;
    private final PetitionerHumanResolver petitionerHumanResolver;

    @Transactional(readOnly = true)
    public CertificateDetailResponse findIssued(Long certificateIssuedId) {
        CertificateIssuedEntity issued = getIssued(certificateIssuedId);

        HumanEntity human = humanRepository.findById(issued.getHumanId())
                .orElseThrow(() -> new HumanException(HumanErrorCode.HUMAN_NOT_FOUND));

        List<CertificateItemDto> items =
                certificateRepository.findAllByCertificateIdInOrderByHireDateAscCertificateIdAsc(
                                issued.getCertificateIds()).stream()
                        .map(CertificateItemDto::from)
                        .toList();

        return CertificateDetailResponse.of(issued, CertificateHumanDto.from(human), items);
    }

    /**
     * 인적사항은 있는데 재직 이력이 없으면 404가 아니라 200 + 빈 배열이다.
     */
    @Transactional(readOnly = true)
    public List<CertificateItemDto> findAllByHuman(Long humanId) {
        if (!humanRepository.existsById(humanId)) {
            throw new HumanException(HumanErrorCode.HUMAN_NOT_FOUND);
        }
        return certificateRepository.findAllByHumanIdOrderByHireDateAscCertificateIdAsc(humanId).stream()
                .map(CertificateItemDto::from)
                .toList();
    }

    /**
     * 개별 등록. 인적사항은 humans가 단일 출처라 요청 본문이 아니라 조회한 행에서 복사한다
     * (CertificateCreateRequest 주석 참고).
     *
     * humans.department는 일부러 안 가져온다. 사람당 하나뿐이라 전보 이력에 반복해 찍으면
     * 증명서에 틀린 근무부서가 인쇄된다 (certificate-domain.md §1-2).
     */
    @Transactional
    public CertificateCreateResponse create(CertificateCreateRequest request) {
        HumanEntity human = humanRepository.findById(request.humanId())
                .orElseThrow(() -> new HumanException(HumanErrorCode.HUMAN_NOT_FOUND));

        CertificateEntity certificate = certificateRepository.save(CertificateEntity.builder()
                .humanId(human.getHumanId())
                .name(human.getName())
                .birthDate(human.getBirthDate())
                // 이름만 같은 별개 enum이다. human은 M/F로 직렬화하고 certificate는 MALE/FEMALE이다.
                .gender(Gender.valueOf(human.getGender().name()))
                .jobTitle(request.jobTitle())
                .keyResponsibilities(request.keyResponsibilities())
                .hireDate(request.hireDate())
                .expirationDate(request.expirationDate())
                .retirementDate(request.retirementDate())
                .division(request.division())
                .department(request.department())
                .reason(request.reason())
                .employmentType(request.employmentType())
                .note(request.note())
                .build());

        return new CertificateCreateResponse(certificate.getCertificateId());
    }

    @Transactional
    public void update(Long certificateId, CertificateUpdateRequest request) {
        CertificateEntity certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new CertificateException(CertificateErrorCode.CERTIFICATE_NOT_FOUND));

        certificate.update(request.name(), request.birthDate(), request.gender(), request.jobTitle(),
                request.keyResponsibilities(), request.hireDate(), request.expirationDate(),
                request.retirementDate(), request.division(), request.department(), request.reason(),
                request.employmentType(), request.note());
    }

    /**
     * 발급된 증명서는 불변이다. 원본이 나중에 수정돼도 이미 발급된 PDF는 그대로여야 하므로
     * 다시 렌더하지 않고 저장된 파일을 그대로 내려준다.
     *
     * 담당자는 전부, 민원인은 본인 발급 건만 받을 수 있다.
     */
    @Transactional(readOnly = true)
    public IssuedFile download(Long certificateIssuedId, AuthDetails authDetails) {
        CertificateIssuedEntity issued = getIssued(certificateIssuedId);

        if (petitionerHumanResolver.isPetitioner(authDetails)
                && !issued.getHumanId().equals(petitionerHumanResolver.resolve(authDetails).getHumanId())) {
            throw new CertificateException(CertificateErrorCode.NOT_OWN_CERTIFICATE);
        }

        if (issued.getFilePath() == null) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_FILE_NOT_FOUND);
        }
        return new IssuedFile(issued.getDocumentNo() + ".pdf", s3Uploader.download(issued.getFilePath()));
    }

    private CertificateIssuedEntity getIssued(Long certificateIssuedId) {
        return certificateIssuedRepository.findById(certificateIssuedId)
                .orElseThrow(() -> new CertificateException(
                        CertificateErrorCode.CERTIFICATE_ISSUED_NOT_FOUND));
    }

    public record IssuedFile(String fileName, byte[] content) {
    }
}
