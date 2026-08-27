package commonly.commonlybe.admin.service;

import commonly.commonlybe.admin.entity.Admin;
import commonly.commonlybe.admin.repository.AdminRepository;
import commonly.commonlybe.user.repository.UserRepository;
import commonly.commonlybe.user.exception.UserNotFoundException;
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
