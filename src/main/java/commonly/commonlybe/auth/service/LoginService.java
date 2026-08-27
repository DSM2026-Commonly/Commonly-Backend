package commonly.commonlybe.auth.service;

import commonly.commonlybe.auth.controller.dto.LoginRequest;
import commonly.commonlybe.auth.controller.dto.TokenResponse;
import commonly.commonlybe.user.entity.User;
import commonly.commonlybe.user.repository.UserRepository;
import commonly.commonlybe.user.exception.PasswordMismatchException;
import commonly.commonlybe.global.jwt.JwtGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtGenerator jwtGenerator;

    @Transactional(readOnly = true)
    public TokenResponse execute(LoginRequest request) {
        User user = userRepository.findByAccountId(request.getAccountId())
            .orElseThrow(PasswordMismatchException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new PasswordMismatchException();
        }

        return TokenResponse.builder()
            .accessToken(jwtGenerator.generateAccessToken(user.getAccountId()))
            .build();
    }
}
