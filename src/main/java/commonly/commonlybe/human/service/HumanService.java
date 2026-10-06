package commonly.commonlybe.human.service;

import commonly.commonlybe.certificate.repository.CertificateIssuedRepository;
import commonly.commonlybe.certificate.repository.CertificateRepository;
import commonly.commonlybe.human.exception.HumanErrorCode;
import commonly.commonlybe.human.controller.dto.HumanCreateRequest;
import commonly.commonlybe.human.controller.dto.HumanCreateResponse;
import commonly.commonlybe.human.controller.dto.HumanSearchRequest;
import commonly.commonlybe.human.controller.dto.HumanDto;
import commonly.commonlybe.global.page.PageNumber;
import commonly.commonlybe.global.page.PageResponse;
import commonly.commonlybe.human.controller.dto.HumanUpdateRequest;
import commonly.commonlybe.human.entity.HumanEntity;
import commonly.commonlybe.human.exception.HumanException;
import commonly.commonlybe.human.repository.HumanRepository;
import commonly.commonlybe.human.repository.HumanSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HumanService {

    private final HumanRepository humanRepository;
    private final CertificateIssuedRepository certificateIssuedRepository;
    private final CertificateRepository certificateRepository;

    @Transactional
    public HumanCreateResponse create(HumanCreateRequest request) {
        if (humanRepository.existsByNameAndBirthDate(request.name(), request.birthDate())) {
            throw new HumanException(HumanErrorCode.DUPLICATE_HUMAN);
        }

        HumanEntity human = HumanEntity.builder()
                .name(request.name())
                .gender(request.gender())
                .birthDate(request.birthDate())
                .address(request.address())
                .department(request.department())
                .build();

        // 선체크는 동시 요청을 막지 못한다. 유니크 제약 위반을 여기서 잡으려면 flush가 필요하다.
        try {
            humanRepository.saveAndFlush(human);
        } catch (DataIntegrityViolationException e) {
            throw new HumanException(HumanErrorCode.DUPLICATE_HUMAN);
        }

        return new HumanCreateResponse(human.getHumanId());
    }

    @Transactional
    public void update(Long humanId, HumanUpdateRequest request) {
        HumanEntity human = humanRepository.findById(humanId)
                .orElseThrow(() -> new HumanException(HumanErrorCode.HUMAN_NOT_FOUND));

        if (humanRepository.existsByNameAndBirthDateAndHumanIdNot(
                request.name(), request.birthDate(), humanId)) {
            throw new HumanException(HumanErrorCode.DUPLICATE_HUMAN);
        }

        human.update(request.name(), request.gender(), request.birthDate(),
                request.address(), request.department());

        try {
            humanRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new HumanException(HumanErrorCode.DUPLICATE_HUMAN);
        }
    }

    /**
     * 연결된 데이터가 있으면 지우지 않고 409로 막는다.
     *
     * certificate.human_id와 certificates_issued.human_id는 @ManyToOne이 아닌 raw Long 컬럼이라
     * JPA도 DB도 참조 무결성을 봐주지 않는다. humans 행만 사라지면
     * CertificateIssuedRepository.searchHistories가 HumanEntity와 inner join이라서
     * 그 사람의 발급 건이 발급 이력 목록에서 통째로 빠진다 — 공문서 발급 기록이 조용히 없어지는 것이다.
     * 그래서 cascade 삭제는 선택지가 아니고, 존재 여부를 직접 조회해 삭제를 거부한다.
     *
     * ponytail: 담당자가 이력 있는 대상자를 목록에서 치울 방법이 아직 없다. 장기적으로는
     * soft delete(폐기 플래그)가 맞지만, 우선 데이터가 사라지는 경로부터 막는다.
     */
    @Transactional
    public void delete(Long humanId) {
        if (!humanRepository.existsById(humanId)) {
            throw new HumanException(HumanErrorCode.HUMAN_NOT_FOUND);
        }

        // 발급 건을 먼저 본다. 재직 이력은 지우고 다시 넣을 수 있지만 발급 기록은 되돌릴 수 없어
        // 제약이 더 무겁고, 담당자에게 줄 안내도 더 분명하다.
        if (certificateIssuedRepository.existsByHumanId(humanId)) {
            throw new HumanException(HumanErrorCode.HUMAN_HAS_ISSUED_CERTIFICATE);
        }
        if (certificateRepository.existsByHumanId(humanId)) {
            throw new HumanException(HumanErrorCode.HUMAN_HAS_CERTIFICATE);
        }

        humanRepository.deleteById(humanId);
    }

    @Transactional(readOnly = true)
    public PageResponse<HumanDto> search(HumanSearchRequest request) {
        // 정렬이 없으면 페이지 간 순서가 보장되지 않는다.
        Pageable pageable = PageNumber.toPageable(request.page(), request.size(), Sort.by("humanId"));
        return PageResponse.of(
                humanRepository.findAll(HumanSpecifications.search(request), pageable),
                HumanDto::from);
    }
}
