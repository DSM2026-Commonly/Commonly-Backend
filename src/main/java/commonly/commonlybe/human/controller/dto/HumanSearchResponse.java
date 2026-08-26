package commonly.commonlybe.human.controller.dto;

import commonly.commonlybe.human.entity.HumanEntity;
import java.util.List;
import org.springframework.data.domain.Page;

public record HumanSearchResponse(
        List<HumanDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static HumanSearchResponse from(Page<HumanEntity> page) {
        return new HumanSearchResponse(
                page.getContent().stream().map(HumanDto::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
