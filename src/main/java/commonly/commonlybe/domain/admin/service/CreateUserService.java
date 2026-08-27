package commonly.commonlybe.domain.admin.service;

import commonly.commonlybe.domain.admin.domain.Admin;
import commonly.commonlybe.domain.admin.domain.AdminRole;
import commonly.commonlybe.domain.admin.domain.repository.AdminRepository;
import commonly.commonlybe.domain.admin.presentation.dto.request.CreateUserRequest;
import commonly.commonlybe.domain.user.domain.User;
import commonly.commonlybe.domain.user.domain.repository.UserRepository;
import commonly.commonlybe.domain.user.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateUserService {
    static final String INITIAL_PASSWORD = "abcd1234";

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void execute(CreateUserRequest request) {
        if (userRepository.existsByAccountId(request.getAccountId())) {
            throw new UserAlreadyExistsException();
        }

        User user = User.builder()
            .accountId(request.getAccountId())
            .password(passwordEncoder.encode(INITIAL_PASSWORD))
            .name(request.getName())
            .build();
        user.requirePasswordChange();
        userRepository.save(user);

        adminRepository.save(
            Admin.builder()
                .user(user)
                .department(request.getDepartment())
                .role(AdminRole.USER)
                .build()
        );
    }
}
