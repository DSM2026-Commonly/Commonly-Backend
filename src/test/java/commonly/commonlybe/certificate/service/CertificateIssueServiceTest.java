package commonly.commonlybe.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import commonly.commonlybe.certificate.controller.dto.CertificateIssueRequest;
import commonly.commonlybe.certificate.controller.dto.CertificateIssueResponse;
import commonly.commonlybe.certificate.document.CertificatePdfRenderer;
import commonly.commonlybe.certificate.document.DocumentNumberGenerator;
import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.entity.CertificateIssuedEntity;
import commonly.commonlybe.certificate.exception.CertificateErrorCode;
import commonly.commonlybe.certificate.exception.CertificateException;
import commonly.commonlybe.certificate.repository.CertificateIssuedRepository;
import commonly.commonlybe.certificate.repository.CertificateRepository;
import commonly.commonlybe.file.exception.FileException;
import commonly.commonlybe.global.error.error_code.FileErrorCode;
import commonly.commonlybe.global.s3.S3Uploader;
import commonly.commonlybe.human.entity.HumanEntity;
import commonly.commonlybe.human.exception.HumanErrorCode;
import commonly.commonlybe.human.exception.HumanException;
import commonly.commonlybe.human.repository.HumanRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class CertificateIssueServiceTest {

    private static final Long HUMAN_ID = 1L;
    private static final Long ISSUED_ID = 7L;
    private static final byte[] PDF = "%PDF-1.4".getBytes();

    @Mock
    private HumanRepository humanRepository;

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private CertificateIssuedRepository certificateIssuedRepository;

    @Mock
    private DocumentNumberGenerator documentNumberGenerator;

    @Mock
    private CertificatePdfRenderer certificatePdfRenderer;

    @Mock
    private S3Uploader s3Uploader;

    @InjectMocks
    private CertificateIssueService certificateIssueService;

    @Test
    void 인적사항이_없으면_발급을_거부한다() {
        given(humanRepository.findById(HUMAN_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> certificateIssueService.issue(request(List.of(1L))))
                .isInstanceOf(HumanException.class)
                .extracting(e -> ((HumanException) e).getErrorProperty())
                .isEqualTo(HumanErrorCode.HUMAN_NOT_FOUND);

        verify(certificateIssuedRepository, never()).save(any());
    }

    @Test
    void 남의_재직_이력을_섞으면_발급을_거부한다() {
        // 2건을 요청했는데 humanId 조건으로 걸러 1건만 나왔다 = 나머지는 남의 것이거나 없는 것.
        given(humanRepository.findById(HUMAN_ID)).willReturn(Optional.of(human()));
        given(certificateRepository.findAllByCertificateIdInAndHumanIdOrderByHireDateAscCertificateIdAsc(
                List.of(1L, 2L), HUMAN_ID)).willReturn(List.of(certificate(1L)));

        assertThatThrownBy(() -> certificateIssueService.issue(request(List.of(1L, 2L))))
                .isInstanceOf(CertificateException.class)
                .extracting(e -> ((CertificateException) e).getErrorProperty())
                .isEqualTo(CertificateErrorCode.CERTIFICATE_NOT_FOUND);

        verify(certificateIssuedRepository, never()).save(any());
    }

    @Test
    void 같은_id를_중복으로_보내도_한_건으로_보고_통과시킨다() {
        given(humanRepository.findById(HUMAN_ID)).willReturn(Optional.of(human()));
        given(certificateRepository.findAllByCertificateIdInAndHumanIdOrderByHireDateAscCertificateIdAsc(
                List.of(1L, 1L), HUMAN_ID)).willReturn(List.of(certificate(1L)));
        given(documentNumberGenerator.generate(anyInt())).willReturn("유성구-2026-000001");

        CertificateIssueResponse response = certificateIssueService.issue(request(List.of(1L, 1L)));

        assertThat(response.documentNo()).isEqualTo("유성구-2026-000001");
        verify(certificateIssuedRepository).save(any(CertificateIssuedEntity.class));
    }

    @Test
    void 총_근무기간을_계산해_발급_건에_저장한다() {
        given(humanRepository.findById(HUMAN_ID)).willReturn(Optional.of(human()));
        given(certificateRepository.findAllByCertificateIdInAndHumanIdOrderByHireDateAscCertificateIdAsc(
                List.of(1L), HUMAN_ID)).willReturn(List.of(certificate(1L)));
        given(documentNumberGenerator.generate(anyInt())).willReturn("유성구-2026-000001");

        certificateIssueService.issue(request(List.of(1L)));

        verify(certificateIssuedRepository).save(argThatMatches());
    }

    @Test
    void 발급하면_PDF를_올리고_file_path에_key를_남긴다() {
        CertificateIssuedEntity issued = issueSuccessfully();

        String expectedKey = "issued/certificates/%d/%d.pdf".formatted(issued.getIssuedAt().getYear(), ISSUED_ID);
        verify(s3Uploader).upload(eq(expectedKey), eq(PDF), eq("application/pdf"));
        assertThat(issued.getFilePath()).isEqualTo(expectedKey);
    }

    @Test
    void PDF_렌더가_실패하면_발급도_실패한다() {
        stubIssuable();
        given(certificatePdfRenderer.render(any(), any(), any()))
                .willThrow(new CertificateException(CertificateErrorCode.CERTIFICATE_PDF_RENDER_FAILED));

        assertThatThrownBy(() -> certificateIssueService.issue(request(List.of(1L))))
                .isInstanceOf(CertificateException.class)
                .extracting(e -> ((CertificateException) e).getErrorProperty())
                .isEqualTo(CertificateErrorCode.CERTIFICATE_PDF_RENDER_FAILED);

        verify(s3Uploader, never()).upload(anyString(), any(), anyString());
    }

    @Test
    void PDF_업로드가_실패하면_발급도_실패한다() {
        stubIssuable();
        given(certificatePdfRenderer.render(any(), any(), any())).willReturn(PDF);
        willThrow(new FileException(FileErrorCode.STORAGE_FAILURE))
                .given(s3Uploader).upload(anyString(), any(), anyString());

        assertThatThrownBy(() -> certificateIssueService.issue(request(List.of(1L))))
                .isInstanceOf(FileException.class);
    }

    @Test
    void 업로드_뒤_트랜잭션이_롤백되면_올린_PDF를_지운다() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            CertificateIssuedEntity issued = issueSuccessfully();

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            verify(s3Uploader).delete(issued.getFilePath());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 커밋되면_올린_PDF를_지우지_않는다() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            issueSuccessfully();

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

            verify(s3Uploader, never()).delete(anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private CertificateIssuedEntity issueSuccessfully() {
        stubIssuable();
        given(certificatePdfRenderer.render(any(), any(), any())).willReturn(PDF);
        CertificateIssuedEntity[] saved = new CertificateIssuedEntity[1];
        given(certificateIssuedRepository.save(any(CertificateIssuedEntity.class))).willAnswer(invocation -> {
            saved[0] = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved[0], "certificateIssuedId", ISSUED_ID);
            return saved[0];
        });

        certificateIssueService.issue(request(List.of(1L)));
        return saved[0];
    }

    private void stubIssuable() {
        given(humanRepository.findById(HUMAN_ID)).willReturn(Optional.of(human()));
        given(certificateRepository.findAllByCertificateIdInAndHumanIdOrderByHireDateAscCertificateIdAsc(
                List.of(1L), HUMAN_ID)).willReturn(List.of(certificate(1L)));
        given(documentNumberGenerator.generate(anyInt())).willReturn("유성구-2026-000001");
    }

    private HumanEntity human() {
        return HumanEntity.builder().name("홍길동").birthDate(LocalDate.of(1990, 1, 1)).build();
    }

    private CertificateIssuedEntity argThatMatches() {
        return org.mockito.ArgumentMatchers.argThat(issued ->
                issued.getHumanId().equals(HUMAN_ID)
                        && issued.getTotalMonths() == 0
                        && issued.getTotalDays() == 10
                        && issued.getCertificateIds().equals(List.of(1L)));
    }

    private CertificateIssueRequest request(List<Long> certificateIds) {
        return new CertificateIssueRequest(HUMAN_ID, certificateIds, "은행 제출용", null);
    }

    private CertificateEntity certificate(Long certificateId) {
        CertificateEntity certificate = CertificateEntity.builder()
                .humanId(HUMAN_ID)
                .name("홍길동")
                .hireDate(LocalDate.of(2022, 1, 1))
                .retirementDate(LocalDate.of(2022, 1, 10))
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(
                certificate, "certificateId", certificateId);
        return certificate;
    }
}
