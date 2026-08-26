package commonly.commonlybe.domain.auth.service;

import commonly.commonlybe.domain.auth.presentation.dto.request.SignupRequest;
import commonly.commonlybe.domain.auth.presentation.dto.response.TokenResponse;
import commonly.commonlybe.domain.petitioner.domain.Petitioner;
import commonly.commonlybe.domain.petitioner.domain.repository.PetitionerRepository;
import commonly.commonlybe.domain.user.domain.User;
import commonly.commonlybe.domain.user.domain.repository.UserRepository;
import commonly.commonlybe.domain.user.exception.UserAlreadyExistsException;
import commonly.commonlybe.global.jwt.JwtGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupService {
    private final UserRepository userRepository;
    private final PetitionerRepository petitionerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtGenerator jwtGenerator;

    @Transactional
    public TokenResponse execute(SignupRequest request) {
        if (userRepository.existsByAccountId(request.getAccountId())) {
            throw new UserAlreadyExistsException();
        }

        User user = userRepository.save(
            User.builder()
                .accountId(request.getAccountId())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build()
        );

        petitionerRepository.save(
            Petitioner.builder()
                .user(user)
                .phoneNumber(request.getPhoneNumber())
                .birthDate(request.getBirthDate())
                .build()
        );

        return TokenResponse.builder()
            .accessToken(jwtGenerator.generateAccessToken(user.getAccountId()))
            .build();
    }
}
