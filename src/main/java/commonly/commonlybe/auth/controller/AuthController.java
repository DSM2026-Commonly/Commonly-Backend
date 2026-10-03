package commonly.commonlybe.auth.controller;

import commonly.commonlybe.auth.controller.dto.ChangePasswordRequest;
import commonly.commonlybe.auth.controller.dto.LoginRequest;
import commonly.commonlybe.auth.controller.dto.SignupRequest;
import commonly.commonlybe.auth.controller.dto.UpdateAccountRequest;
import commonly.commonlybe.auth.controller.dto.TokenResponse;
import commonly.commonlybe.auth.service.ChangePasswordService;
import commonly.commonlybe.auth.service.LoginService;
import commonly.commonlybe.auth.service.SignupService;
import commonly.commonlybe.auth.service.UpdateAccountService;
import commonly.commonlybe.global.security.auth.AuthDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final UpdateAccountService updateAccountService;

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
    public void changePassword(@AuthenticationPrincipal AuthDetails authDetails,
                               @PathVariable Long userId,
                               @RequestBody @Valid ChangePasswordRequest request) {
        changePasswordService.execute(authDetails.user().getId(), userId, request);
    }

    @PatchMapping("/{userId}")
    public void updateAccount(@AuthenticationPrincipal AuthDetails authDetails,
                              @PathVariable Long userId,
                              @RequestBody @Valid UpdateAccountRequest request) {
        updateAccountService.execute(authDetails.user().getId(), userId, request);
    }
}
