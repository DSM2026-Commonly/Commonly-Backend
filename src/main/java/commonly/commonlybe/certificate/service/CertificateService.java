package commonly.commonlybe.certificate.service;

import commonly.commonlybe.certificate.controller.dto.CertificateDetailResponse;
import commonly.commonlybe.certificate.controller.dto.CertificateHumanDto;
import commonly.commonlybe.certificate.controller.dto.CertificateItemDto;
import commonly.commonlybe.certificate.controller.dto.CertificateUpdateRequest;
import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.entity.CertificateIssuedEntity;
import commonly.commonlybe.certificate.exception.CertificateErrorCode;
import commonly.commonlybe.certificate.exception.CertificateException;
import commonly.commonlybe.certificate.repository.CertificateIssuedRepository;
import commonly.commonlybe.certificate.repository.CertificateRepository;
import commonly.commonlybe.global.s3.S3Uploader;
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
     */
    @Transactional(readOnly = true)
    public IssuedFile download(Long certificateIssuedId) {
        CertificateIssuedEntity issued = getIssued(certificateIssuedId);
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
