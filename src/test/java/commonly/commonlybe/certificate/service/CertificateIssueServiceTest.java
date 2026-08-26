package commonly.commonlybe.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import commonly.commonlybe.certificate.controller.dto.CertificateIssueRequest;
import commonly.commonlybe.certificate.controller.dto.CertificateIssueResponse;
import commonly.commonlybe.certificate.document.DocumentNumberGenerator;
import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.entity.CertificateIssuedEntity;
import commonly.commonlybe.certificate.exception.CertificateErrorCode;
import commonly.commonlybe.certificate.exception.CertificateException;
import commonly.commonlybe.certificate.repository.CertificateIssuedRepository;
import commonly.commonlybe.certificate.repository.CertificateRepository;
import commonly.commonlybe.human.exception.HumanErrorCode;
import commonly.commonlybe.human.exception.HumanException;
import commonly.commonlybe.human.repository.HumanRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CertificateIssueServiceTest {

    private static final Long HUMAN_ID = 1L;

    @Mock
    private HumanRepository humanRepository;

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private CertificateIssuedRepository certificateIssuedRepository;

    @Mock
    private DocumentNumberGenerator documentNumberGenerator;

    @InjectMocks
    private CertificateIssueService certificateIssueService;

    @Test
    void 인적사항이_없으면_발급을_거부한다() {
        given(humanRepository.existsById(HUMAN_ID)).willReturn(false);

        assertThatThrownBy(() -> certificateIssueService.issue(request(List.of(1L))))
                .isInstanceOf(HumanException.class)
                .extracting(e -> ((HumanException) e).getErrorProperty())
                .isEqualTo(HumanErrorCode.HUMAN_NOT_FOUND);

        verify(certificateIssuedRepository, never()).save(any());
    }

    @Test
    void 남의_재직_이력을_섞으면_발급을_거부한다() {
        // 2건을 요청했는데 humanId 조건으로 걸러 1건만 나왔다 = 나머지는 남의 것이거나 없는 것.
        given(humanRepository.existsById(HUMAN_ID)).willReturn(true);
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
        given(humanRepository.existsById(HUMAN_ID)).willReturn(true);
        given(certificateRepository.findAllByCertificateIdInAndHumanIdOrderByHireDateAscCertificateIdAsc(
                List.of(1L, 1L), HUMAN_ID)).willReturn(List.of(certificate(1L)));
        given(documentNumberGenerator.generate(anyInt())).willReturn("유성구-2026-000001");

        CertificateIssueResponse response = certificateIssueService.issue(request(List.of(1L, 1L)));

        assertThat(response.documentNo()).isEqualTo("유성구-2026-000001");
        verify(certificateIssuedRepository).save(any(CertificateIssuedEntity.class));
    }

    @Test
    void 총_근무기간을_계산해_발급_건에_저장한다() {
        given(humanRepository.existsById(HUMAN_ID)).willReturn(true);
        given(certificateRepository.findAllByCertificateIdInAndHumanIdOrderByHireDateAscCertificateIdAsc(
                List.of(1L), HUMAN_ID)).willReturn(List.of(certificate(1L)));
        given(documentNumberGenerator.generate(anyInt())).willReturn("유성구-2026-000001");

        certificateIssueService.issue(request(List.of(1L)));

        verify(certificateIssuedRepository).save(argThatMatches());
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
