package commonly.commonlybe.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import commonly.commonlybe.certificate.controller.dto.CertificateIssueRequest;
import commonly.commonlybe.certificate.controller.dto.CertificateIssueResponse;
import commonly.commonlybe.certificate.document.CertificatePdfRenderer;
import commonly.commonlybe.certificate.document.CertificatePdfRenderer.CertificateDocument;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class CertificateIssueServiceTest {

    private static final Long HUMAN_ID = 1L;
    private static final String DOCUMENT_NO = "유성구-2026-000001";
    private static final byte[] PDF = {'%', 'P', 'D', 'F'};

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

    /** 서비스가 롤백 시 S3 정리 콜백을 등록하므로 트랜잭션 동기화를 흉내 낸다. */
    @BeforeEach
    void initSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

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
        givenPdfUploaded();

        CertificateIssueResponse response = certificateIssueService.issue(request(List.of(1L, 1L)));

        assertThat(response.documentNo()).isEqualTo(DOCUMENT_NO);
        verify(certificateIssuedRepository).save(any(CertificateIssuedEntity.class));
    }

    @Test
    void 총_근무기간을_계산해_발급_건에_저장한다() {
        given(humanRepository.findById(HUMAN_ID)).willReturn(Optional.of(human()));
        given(certificateRepository.findAllByCertificateIdInAndHumanIdOrderByHireDateAscCertificateIdAsc(
                List.of(1L), HUMAN_ID)).willReturn(List.of(certificate(1L)));
        givenPdfUploaded();

        certificateIssueService.issue(request(List.of(1L)));

        verify(certificateIssuedRepository).save(argThatMatches());
    }

    @Test
    void PDF를_만들어_올리고_key를_발급_건에_저장한다() {
        givenIssuable();
        givenPdfUploaded();

        certificateIssueService.issue(request(List.of(1L)));

        String key = "certificates/issued/%d/%s.pdf".formatted(LocalDate.now().getYear(), DOCUMENT_NO);
        verify(s3Uploader).upload(PDF, key, "application/pdf");
        verify(certificateIssuedRepository).save(org.mockito.ArgumentMatchers.<CertificateIssuedEntity>argThat(
                issued -> key.equals(issued.getFilePath())));
    }

    @Test
    void PDF_생성에_실패하면_발급하지_않는다() {
        givenIssuable();
        given(documentNumberGenerator.generate(anyInt())).willReturn(DOCUMENT_NO);
        given(certificatePdfRenderer.render(any()))
                .willThrow(new CertificateException(CertificateErrorCode.CERTIFICATE_RENDER_FAILED));

        assertThatThrownBy(() -> certificateIssueService.issue(request(List.of(1L))))
                .isInstanceOf(CertificateException.class)
                .extracting(e -> ((CertificateException) e).getErrorProperty())
                .isEqualTo(CertificateErrorCode.CERTIFICATE_RENDER_FAILED);

        verify(s3Uploader, never()).upload(any(byte[].class), anyString(), anyString());
        verify(certificateIssuedRepository, never()).save(any());
    }

    @Test
    void PDF_업로드에_실패하면_발급하지_않는다() {
        givenIssuable();
        given(documentNumberGenerator.generate(anyInt())).willReturn(DOCUMENT_NO);
        given(certificatePdfRenderer.render(any())).willReturn(PDF);
        given(s3Uploader.upload(eq(PDF), anyString(), anyString()))
                .willThrow(new FileException(FileErrorCode.STORAGE_FAILURE));

        assertThatThrownBy(() -> certificateIssueService.issue(request(List.of(1L))))
                .isInstanceOf(FileException.class);

        verify(certificateIssuedRepository, never()).save(any());
    }

    @Test
    void 트랜잭션이_롤백되면_올린_PDF를_지운다() {
        givenIssuable();
        String key = givenPdfUploaded();

        certificateIssueService.issue(request(List.of(1L)));
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(s3Uploader).delete(key);
    }

    @Test
    void 트랜잭션이_커밋되면_PDF를_남긴다() {
        givenIssuable();
        givenPdfUploaded();

        certificateIssueService.issue(request(List.of(1L)));
        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        verify(s3Uploader, never()).delete(anyString());
    }

    @Test
    void 미리보기는_문서번호를_따지_않고_저장도_하지_않는다() {
        givenIssuable();
        given(certificatePdfRenderer.render(any())).willReturn(PDF);

        assertThat(certificateIssueService.preview(request(List.of(1L)))).isEqualTo(PDF);

        verify(documentNumberGenerator, never()).generate(anyInt());
        verify(s3Uploader, never()).upload(any(byte[].class), anyString(), anyString());
        verify(certificateIssuedRepository, never()).save(any());
    }

    @Test
    void 미리보기도_남의_재직_이력을_섞으면_거부한다() {
        given(humanRepository.findById(HUMAN_ID)).willReturn(Optional.of(human()));
        given(certificateRepository.findAllByCertificateIdInAndHumanIdOrderByHireDateAscCertificateIdAsc(
                List.of(1L, 2L), HUMAN_ID)).willReturn(List.of(certificate(1L)));

        assertThatThrownBy(() -> certificateIssueService.preview(request(List.of(1L, 2L))))
                .isInstanceOf(CertificateException.class);

        verify(certificatePdfRenderer, never()).render(any());
    }

    @Test
    void 발급_사유를_발급_건에_저장한다() {
        givenIssuable();
        givenPdfUploaded();

        certificateIssueService.issue(requestWithIssueReason("본인 요청 - 전화 접수"));

        verify(certificateIssuedRepository).save(org.mockito.ArgumentMatchers.<CertificateIssuedEntity>argThat(
                issued -> "본인 요청 - 전화 접수".equals(issued.getIssueReason())));
    }

    @Test
    void 발급_사유가_없어도_발급된다() {
        givenIssuable();
        givenPdfUploaded();

        CertificateIssueResponse response = certificateIssueService.issue(requestWithIssueReason(null));

        assertThat(response.documentNo()).isEqualTo(DOCUMENT_NO);
        verify(certificateIssuedRepository).save(org.mockito.ArgumentMatchers.<CertificateIssuedEntity>argThat(
                issued -> issued.getIssueReason() == null));
    }

    /** 서식에 발급 사유 칸이 없다. PDF에 찍히는 자유 입력은 purpose(용도)와 otherMatters(그 밖의 사항)뿐이다. */
    @Test
    void 발급_사유는_PDF로_넘어가지_않는다() {
        givenIssuable();
        givenPdfUploaded();

        certificateIssueService.issue(requestWithIssueReason("감사 자료 제출 요청"));

        verify(certificatePdfRenderer).render(org.mockito.ArgumentMatchers.<CertificateDocument>argThat(
                document -> "은행 제출용".equals(document.purpose()) && document.otherMatters() == null));
    }

    private void givenIssuable() {
        given(humanRepository.findById(HUMAN_ID)).willReturn(Optional.of(human()));
        given(certificateRepository.findAllByCertificateIdInAndHumanIdOrderByHireDateAscCertificateIdAsc(
                List.of(1L), HUMAN_ID)).willReturn(List.of(certificate(1L)));
    }

    /** 업로드는 받은 key를 그대로 돌려준다. 실제 key는 issue()가 정하므로 롤백 검증은 캡처한 값으로 한다. */
    private String givenPdfUploaded() {
        String key = "certificates/issued/%d/%s.pdf".formatted(LocalDate.now().getYear(), DOCUMENT_NO);
        given(documentNumberGenerator.generate(anyInt())).willReturn(DOCUMENT_NO);
        given(certificatePdfRenderer.render(any())).willReturn(PDF);
        given(s3Uploader.upload(eq(PDF), anyString(), eq("application/pdf"))).willAnswer(inv -> inv.getArgument(1));
        return key;
    }

    private void completeTransaction(int status) {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(status));
    }

    private HumanEntity human() {
        HumanEntity human = HumanEntity.builder()
                .name("홍길동")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(human, "humanId", HUMAN_ID);
        return human;
    }

    private CertificateIssuedEntity argThatMatches() {
        return org.mockito.ArgumentMatchers.argThat(issued ->
                issued.getHumanId().equals(HUMAN_ID)
                        && issued.getTotalMonths() == 0
                        && issued.getTotalDays() == 10
                        && issued.getCertificateIds().equals(List.of(1L)));
    }

    private CertificateIssueRequest request(List<Long> certificateIds) {
        return new CertificateIssueRequest(HUMAN_ID, certificateIds, "은행 제출용", null, null);
    }

    private CertificateIssueRequest requestWithIssueReason(String issueReason) {
        return new CertificateIssueRequest(HUMAN_ID, List.of(1L), "은행 제출용", null, issueReason);
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
