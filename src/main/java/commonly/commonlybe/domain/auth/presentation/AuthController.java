package commonly.commonlybe.domain.auth.presentation;

import commonly.commonlybe.domain.auth.presentation.dto.request.ChangePasswordRequest;
import commonly.commonlybe.domain.auth.presentation.dto.request.LoginRequest;
import commonly.commonlybe.domain.auth.presentation.dto.request.SignupRequest;
import commonly.commonlybe.domain.auth.presentation.dto.response.TokenResponse;
import commonly.commonlybe.domain.auth.service.ChangePasswordService;
import commonly.commonlybe.domain.auth.service.LoginService;
import commonly.commonlybe.domain.auth.service.SignupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auths")
@RequiredArgsConstructor
public class AuthController {
    private final LoginService loginService;
    private final SignupService signupService;
    private final ChangePasswordService changePasswordService;

    @PostMapping("/login")
    public TokenResponse login(@RequestBody @Valid LoginRequest request) {
        return loginService.execute(request);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@RequestBody @Valid SignupRequest request) {
        return signupService.execute(request);
    }

    @PatchMapping("/password/{userId}")
    public void changePassword(@PathVariable Long userId, @RequestBody @Valid ChangePasswordRequest request) {
        changePasswordService.execute(userId, request);
    }
}
