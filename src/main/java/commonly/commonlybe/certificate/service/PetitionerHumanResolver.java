package commonly.commonlybe.certificate.service;

import commonly.commonlybe.certificate.exception.CertificateErrorCode;
import commonly.commonlybe.certificate.exception.CertificateException;
import commonly.commonlybe.petitioner.entity.Petitioner;
import commonly.commonlybe.petitioner.repository.PetitionerRepository;
import commonly.commonlybe.global.security.auth.AuthDetails;
import commonly.commonlybe.human.entity.HumanEntity;
import commonly.commonlybe.human.repository.HumanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 민원인 계정 ↔ 인적사항 매칭.
 *
 * Petitioner에 human_id FK가 없어 (성명, 생년월일)로 잇는다.
 * humans의 uk_humans_name_birth_date가 1:1을 보장하므로 조회 결과는 최대 1건이다.
 *
 * 주의: 회원가입에 신원 검증이 없다(SignupService). 이 매칭은 "가입자가 본인이다"를
 * 증명하지 못한다. 본인인증이 붙기 전까지 /self를 운영에 열면 안 된다.
 */
@Component
@RequiredArgsConstructor
public class PetitionerHumanResolver {

    private static final String PETITIONER_AUTHORITY = "PETITIONER";

    private final PetitionerRepository petitionerRepository;
    private final HumanRepository humanRepository;

    public boolean isPetitioner(AuthDetails authDetails) {
        return PETITIONER_AUTHORITY.equals(authDetails.authority());
    }

    public HumanEntity resolve(AuthDetails authDetails) {
        Petitioner petitioner = petitionerRepository.findById(authDetails.user().getId())
                .orElseThrow(() -> new CertificateException(
                        CertificateErrorCode.PETITIONER_HUMAN_NOT_MATCHED));

        return humanRepository.findByNameAndBirthDate(
                        authDetails.user().getName(), petitioner.getBirthDate())
                .orElseThrow(() -> new CertificateException(
                        CertificateErrorCode.PETITIONER_HUMAN_NOT_MATCHED));
    }
}
