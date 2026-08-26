package commonly.commonlybe.domain.auth.service;

import commonly.commonlybe.domain.auth.presentation.dto.request.ChangePasswordRequest;
import commonly.commonlybe.domain.user.domain.User;
import commonly.commonlybe.domain.user.domain.repository.UserRepository;
import commonly.commonlybe.domain.user.exception.ForbiddenUserException;
import commonly.commonlybe.domain.user.exception.PasswordMismatchException;
import commonly.commonlybe.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void execute(Long currentUserId, Long userId, ChangePasswordRequest request) {
        if (!currentUserId.equals(userId)) {
            throw new ForbiddenUserException();
        }

        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new PasswordMismatchException();
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }
}
