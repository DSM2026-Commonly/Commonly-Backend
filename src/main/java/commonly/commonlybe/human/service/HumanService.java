package commonly.commonlybe.human.service;

import commonly.commonlybe.human.exception.HumanErrorCode;
import commonly.commonlybe.human.controller.dto.HumanCreateRequest;
import commonly.commonlybe.human.controller.dto.HumanCreateResponse;
import commonly.commonlybe.human.controller.dto.HumanSearchRequest;
import commonly.commonlybe.human.controller.dto.HumanSearchResponse;
import commonly.commonlybe.human.controller.dto.HumanUpdateRequest;
import commonly.commonlybe.human.entity.HumanEntity;
import commonly.commonlybe.human.exception.HumanException;
import commonly.commonlybe.human.repository.HumanRepository;
import commonly.commonlybe.human.repository.HumanSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HumanService {

    private final HumanRepository humanRepository;

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

    @Transactional
    public void delete(Long humanId) {
        if (!humanRepository.existsById(humanId)) {
            throw new HumanException(HumanErrorCode.HUMAN_NOT_FOUND);
        }
        humanRepository.deleteById(humanId);
    }

    @Transactional(readOnly = true)
    public HumanSearchResponse search(HumanSearchRequest request) {
        // 정렬이 없으면 페이지 간 순서가 보장되지 않는다.
        PageRequest pageRequest = PageRequest.of(request.page(), request.size(), Sort.by("humanId"));
        return HumanSearchResponse.from(
                humanRepository.findAll(HumanSpecifications.search(request), pageRequest));
    }
}
