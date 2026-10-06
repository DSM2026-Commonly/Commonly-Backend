package commonly.commonlybe.user.controller;

import commonly.commonlybe.global.security.auth.AuthDetails;
import commonly.commonlybe.user.controller.dto.MeResponse;
import commonly.commonlybe.user.service.QueryMeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final QueryMeService queryMeService;

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthDetails authDetails) {
        return queryMeService.execute(authDetails);
    }
}
