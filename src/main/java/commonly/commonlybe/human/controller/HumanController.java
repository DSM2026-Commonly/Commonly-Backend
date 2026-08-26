package commonly.commonlybe.human.controller;

import commonly.commonlybe.human.controller.dto.HumanCreateRequest;
import commonly.commonlybe.human.controller.dto.HumanCreateResponse;
import commonly.commonlybe.human.controller.dto.HumanSearchRequest;
import commonly.commonlybe.human.controller.dto.HumanSearchResponse;
import commonly.commonlybe.human.controller.dto.HumanUpdateRequest;
import commonly.commonlybe.human.service.HumanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/human")
@RequiredArgsConstructor
public class HumanController {

    private final HumanService humanService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HumanCreateResponse create(@RequestBody @Valid HumanCreateRequest request) {
        return humanService.create(request);
    }

    @PutMapping("/{humanId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable Long humanId, @RequestBody @Valid HumanUpdateRequest request) {
        humanService.update(humanId, request);
    }

    @DeleteMapping("/{humanId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long humanId) {
        humanService.delete(humanId);
    }

    @PostMapping("/search")
    public HumanSearchResponse search(@RequestBody @Valid HumanSearchRequest request) {
        return humanService.search(request);
    }
}
