package commonly.commonlybe.domain.admin.presentation;

import commonly.commonlybe.domain.admin.presentation.dto.request.ChangeInitialPasswordRequest;
import commonly.commonlybe.domain.admin.presentation.dto.request.CreateUserRequest;
import commonly.commonlybe.domain.admin.presentation.dto.response.UserListResponse;
import commonly.commonlybe.domain.admin.service.ChangeInitialPasswordService;
import commonly.commonlybe.domain.admin.service.CreateUserService;
import commonly.commonlybe.domain.admin.service.DeleteUserService;
import commonly.commonlybe.domain.admin.service.QueryUserListService;
import commonly.commonlybe.global.security.auth.AuthDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdminController {
    private final ChangeInitialPasswordService changeInitialPasswordService;
    private final CreateUserService createUserService;
    private final QueryUserListService queryUserListService;
    private final DeleteUserService deleteUserService;

    @PatchMapping("/admin/password")
    public void changeInitialPassword(@AuthenticationPrincipal AuthDetails authDetails,
                                      @RequestBody @Valid ChangeInitialPasswordRequest request) {
        changeInitialPasswordService.execute(authDetails.user().getId(), request);
    }

    @PostMapping("/admin/users")
    @ResponseStatus(HttpStatus.CREATED)
    public void createUser(@RequestBody @Valid CreateUserRequest request) {
        createUserService.execute(request);
    }

    @GetMapping("/admins")
    public List<UserListResponse> queryUserList(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                @RequestParam(defaultValue = "10") @Min(1) int size,
                                                @RequestParam(required = false) String keyword) {
        return queryUserListService.execute(page, size, keyword);
    }

    @DeleteMapping("/admin/users/{userId}")
    public void deleteUser(@PathVariable Long userId) {
        deleteUserService.execute(userId);
    }
}
