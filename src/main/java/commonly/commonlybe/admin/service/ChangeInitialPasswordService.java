package commonly.commonlybe.admin.service;

import commonly.commonlybe.admin.controller.dto.ChangeInitialPasswordRequest;
import commonly.commonlybe.user.entity.User;
import commonly.commonlybe.user.repository.UserRepository;
import commonly.commonlybe.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeInitialPasswordService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void execute(Long userId, ChangeInitialPasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        user.updatePassword(passwordEncoder.encode(request.getPassword()));
    }
}
