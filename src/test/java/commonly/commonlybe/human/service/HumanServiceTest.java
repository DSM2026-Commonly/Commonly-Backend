package commonly.commonlybe.human.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import commonly.commonlybe.certificate.repository.CertificateIssuedRepository;
import commonly.commonlybe.certificate.repository.CertificateRepository;
import commonly.commonlybe.human.exception.HumanErrorCode;
import commonly.commonlybe.human.exception.HumanException;
import commonly.commonlybe.human.repository.HumanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class HumanServiceTest {

    private static final Long HUMAN_ID = 1L;

    @Mock
    private HumanRepository humanRepository;

    @Mock
    private CertificateIssuedRepository certificateIssuedRepository;

    @Mock
    private CertificateRepository certificateRepository;

    @InjectMocks
    private HumanService humanService;

    @Test
    void 연결된_데이터가_없으면_인적사항을_삭제한다() {
        given(humanRepository.existsById(HUMAN_ID)).willReturn(true);
        given(certificateIssuedRepository.existsByHumanId(HUMAN_ID)).willReturn(false);
        given(certificateRepository.existsByHumanId(HUMAN_ID)).willReturn(false);

        humanService.delete(HUMAN_ID);

        verify(humanRepository).deleteById(HUMAN_ID);
    }

    @Test
    void 인적사항이_없으면_삭제를_거부한다() {
        given(humanRepository.existsById(HUMAN_ID)).willReturn(false);

        assertThatThrownBy(() -> humanService.delete(HUMAN_ID))
                .isInstanceOf(HumanException.class)
                .extracting(e -> ((HumanException) e).getErrorProperty())
                .isEqualTo(HumanErrorCode.HUMAN_NOT_FOUND);

        verify(humanRepository, never()).deleteById(HUMAN_ID);
    }

    /**
     * 발급 건 확인이 재직 이력 확인보다 앞이라는 것까지 고정한다.
     * 둘 다 있을 때 담당자가 받아야 할 안내는 "발급 이력이 있다" 쪽이다.
     */
    @Test
    void 발급_건이_있으면_삭제를_막고_재직_이력은_보지_않는다() {
        given(humanRepository.existsById(HUMAN_ID)).willReturn(true);
        given(certificateIssuedRepository.existsByHumanId(HUMAN_ID)).willReturn(true);

        assertThatThrownBy(() -> humanService.delete(HUMAN_ID))
                .isInstanceOf(HumanException.class)
                .extracting(e -> ((HumanException) e).getErrorProperty())
                .isEqualTo(HumanErrorCode.HUMAN_HAS_ISSUED_CERTIFICATE);

        verify(humanRepository, never()).deleteById(HUMAN_ID);
        verifyNoInteractions(certificateRepository);
    }

    @Test
    void 재직_이력이_있으면_삭제를_막는다() {
        given(humanRepository.existsById(HUMAN_ID)).willReturn(true);
        given(certificateIssuedRepository.existsByHumanId(HUMAN_ID)).willReturn(false);
        given(certificateRepository.existsByHumanId(HUMAN_ID)).willReturn(true);

        assertThatThrownBy(() -> humanService.delete(HUMAN_ID))
                .isInstanceOf(HumanException.class)
                .extracting(e -> ((HumanException) e).getErrorProperty())
                .isEqualTo(HumanErrorCode.HUMAN_HAS_CERTIFICATE);

        verify(humanRepository, never()).deleteById(HUMAN_ID);
    }

    /** FE가 두 상황을 다르게 안내해야 하므로 코드가 겹치면 안 된다. */
    @Test
    void 삭제_불가_사유는_서로_다른_409_코드로_구분된다() {
        assertThat(HumanErrorCode.HUMAN_HAS_ISSUED_CERTIFICATE.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(HumanErrorCode.HUMAN_HAS_CERTIFICATE.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(HumanErrorCode.HUMAN_HAS_ISSUED_CERTIFICATE.getCode())
                .isNotEqualTo(HumanErrorCode.HUMAN_HAS_CERTIFICATE.getCode());
    }
}
