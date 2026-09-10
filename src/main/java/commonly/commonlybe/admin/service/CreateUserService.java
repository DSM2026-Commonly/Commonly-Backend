package commonly.commonlybe.admin.service;

import commonly.commonlybe.admin.entity.Admin;
import commonly.commonlybe.admin.entity.AdminRole;
import commonly.commonlybe.admin.repository.AdminRepository;
import commonly.commonlybe.admin.controller.dto.CreateUserRequest;
import commonly.commonlybe.user.entity.User;
import commonly.commonlybe.user.repository.UserRepository;
import commonly.commonlybe.user.exception.UserAlreadyExistsException;
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
