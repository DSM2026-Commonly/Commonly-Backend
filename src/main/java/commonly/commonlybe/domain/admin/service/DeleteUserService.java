package commonly.commonlybe.domain.admin.service;

import commonly.commonlybe.domain.admin.domain.Admin;
import commonly.commonlybe.domain.admin.domain.repository.AdminRepository;
import commonly.commonlybe.domain.user.domain.repository.UserRepository;
import commonly.commonlybe.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteUserService {
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    @Transactional
    public void execute(Long userId) {
        Admin admin = adminRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        adminRepository.delete(admin);
        userRepository.delete(admin.getUser());
    }
}
