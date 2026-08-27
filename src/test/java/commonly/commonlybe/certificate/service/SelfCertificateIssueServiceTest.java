package commonly.commonlybe.certificate.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import commonly.commonlybe.certificate.controller.dto.CertificateIssueRequest;
import commonly.commonlybe.certificate.controller.dto.SelfCertificateIssueRequest;
import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.exception.CertificateErrorCode;
import commonly.commonlybe.certificate.exception.CertificateException;
import commonly.commonlybe.certificate.repository.CertificateRepository;
import commonly.commonlybe.global.security.auth.AuthDetails;
import commonly.commonlybe.human.entity.HumanEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SelfCertificateIssueServiceTest {

    private static final Long HUMAN_ID = 7L;

    @Mock
    private PetitionerHumanResolver petitionerHumanResolver;

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private CertificateIssueService certificateIssueService;

    @InjectMocks
    private SelfCertificateIssueService selfCertificateIssueService;

    private final AuthDetails authDetails = new AuthDetails(null, "PETITIONER");

    @Test
    void 본인의_재직_이력_전체로_발급한다() {
        givenCertificates(2);

        selfCertificateIssueService.issue(authDetails, new SelfCertificateIssueRequest("은행 제출용", null));

        ArgumentCaptor<CertificateIssueRequest> captor =
                ArgumentCaptor.forClass(CertificateIssueRequest.class);
        verify(certificateIssueService).issue(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue())
                .isEqualTo(new CertificateIssueRequest(HUMAN_ID, List.of(1L, 2L), "은행 제출용", null));
    }

    @Test
    void 재직_이력이_없으면_발급을_거부한다() {
        givenCertificates(0);

        assertThatThrownBy(() -> selfCertificateIssueService.issue(
                authDetails, new SelfCertificateIssueRequest("은행 제출용", null)))
                .isInstanceOf(CertificateException.class)
                .extracting(e -> ((CertificateException) e).getErrorProperty())
                .isEqualTo(CertificateErrorCode.CERTIFICATE_NOT_FOUND);

        verify(certificateIssueService, never()).issue(any());
    }

    @Test
    void 재직_이력이_서식_10행을_넘으면_담당자에게_넘긴다() {
        givenCertificates(11);

        assertThatThrownBy(() -> selfCertificateIssueService.issue(
                authDetails, new SelfCertificateIssueRequest("은행 제출용", null)))
                .isInstanceOf(CertificateException.class)
                .extracting(e -> ((CertificateException) e).getErrorProperty())
                .isEqualTo(CertificateErrorCode.CERTIFICATE_LIMIT_EXCEEDED);

        verify(certificateIssueService, never()).issue(any());
    }

    private void givenCertificates(int count) {
        HumanEntity human = HumanEntity.builder()
                .name("홍길동")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        ReflectionTestUtils.setField(human, "humanId", HUMAN_ID);
        given(petitionerHumanResolver.resolve(authDetails)).willReturn(human);

        List<CertificateEntity> certificates = IntStream.rangeClosed(1, count)
                .mapToObj(i -> {
                    CertificateEntity certificate = CertificateEntity.builder().name("홍길동").build();
                    ReflectionTestUtils.setField(certificate, "certificateId", (long) i);
                    return certificate;
                })
                .toList();
        given(certificateRepository.findAllByHumanIdOrderByHireDateAscCertificateIdAsc(HUMAN_ID))
                .willReturn(certificates);
    }
}
