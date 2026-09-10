package commonly.commonlybe.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import commonly.commonlybe.certificate.controller.dto.CertificateCreateRequest;
import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.entity.Gender;
import commonly.commonlybe.certificate.repository.CertificateRepository;
import commonly.commonlybe.human.entity.HumanEntity;
import commonly.commonlybe.human.exception.HumanErrorCode;
import commonly.commonlybe.human.exception.HumanException;
import commonly.commonlybe.human.repository.HumanRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    private static final Long HUMAN_ID = 1L;
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 1);

    @Mock
    private HumanRepository humanRepository;

    @Mock
    private CertificateRepository certificateRepository;

    @InjectMocks
    private CertificateService certificateService;

    @Test
    void 인적사항이_없으면_등록을_거부한다() {
        given(humanRepository.findById(HUMAN_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> certificateService.create(createRequest()))
                .isInstanceOf(HumanException.class)
                .extracting(e -> ((HumanException) e).getErrorProperty())
                .isEqualTo(HumanErrorCode.HUMAN_NOT_FOUND);

        verify(certificateRepository, never()).save(any());
    }

    /**
     * 성명/생년월일/성별은 humans가 단일 출처다. 성별 enum이 두 벌이라 humans의 MALE이
     * certificate의 MALE로 넘어와야 한다.
     * 근무부서는 반대로 상속하면 안 된다 — 사람당 하나뿐이라 전보 이력에 틀린 값이 찍힌다 (§1-2).
     */
    @Test
    void 인적사항은_humans에서_복사하되_근무부서는_상속하지_않는다() {
        given(humanRepository.findById(HUMAN_ID)).willReturn(Optional.of(human()));
        given(certificateRepository.save(any(CertificateEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        certificateService.create(createRequest());

        ArgumentCaptor<CertificateEntity> saved = ArgumentCaptor.forClass(CertificateEntity.class);
        verify(certificateRepository).save(saved.capture());
        assertThat(saved.getValue().getHumanId()).isEqualTo(HUMAN_ID);
        assertThat(saved.getValue().getName()).isEqualTo("홍길동");
        assertThat(saved.getValue().getBirthDate()).isEqualTo(BIRTH_DATE);
        assertThat(saved.getValue().getGender()).isEqualTo(Gender.MALE);
        assertThat(saved.getValue().getDepartment()).isNull();
        assertThat(saved.getValue().getDivision()).isEqualTo("채용");
    }

    private HumanEntity human() {
        HumanEntity human = HumanEntity.builder()
                .name("홍길동")
                .gender(commonly.commonlybe.human.entity.Gender.MALE)
                .birthDate(BIRTH_DATE)
                .address("대전광역시 유성구")
                .department("총무과")
                .build();
        ReflectionTestUtils.setField(human, "humanId", HUMAN_ID);
        return human;
    }

    private CertificateCreateRequest createRequest() {
        return new CertificateCreateRequest(HUMAN_ID, "주무관", "민원 접수",
                LocalDate.of(2020, 1, 1), LocalDate.of(2022, 3, 14), null,
                "채용", null, null, "기간제", null);
    }
}
