package commonly.commonlybe.human.repository;

import commonly.commonlybe.human.controller.dto.HumanSearchRequest;
import commonly.commonlybe.human.entity.HumanEntity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class HumanSpecifications {

    private HumanSpecifications() {
    }

    /**
     * null인 조건은 건너뛴다. 조건이 하나도 없으면 전체 조회가 된다.
     */
    public static Specification<HumanEntity> search(HumanSearchRequest request) {
        List<Specification<HumanEntity>> specs = new ArrayList<>();

        if (request.name() != null) {
            specs.add(contains("name", request.name()));
        }
        if (request.address() != null) {
            specs.add(contains("address", request.address()));
        }
        if (request.gender() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("gender"), request.gender()));
        }
        if (request.birthDateFrom() != null) {
            specs.add((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("birthDate"), request.birthDateFrom()));
        }
        if (request.birthDateTo() != null) {
            specs.add((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("birthDate"), request.birthDateTo()));
        }

        return Specification.allOf(specs);
    }

    private static Specification<HumanEntity> contains(String field, String value) {
        String pattern = "%" + value + "%";
        return (root, query, cb) -> cb.like(root.get(field), pattern);
    }
}
