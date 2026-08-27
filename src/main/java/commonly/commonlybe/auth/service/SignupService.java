package commonly.commonlybe.auth.service;

import commonly.commonlybe.auth.controller.dto.SignupRequest;
import commonly.commonlybe.auth.controller.dto.TokenResponse;
import commonly.commonlybe.petitioner.entity.Petitioner;
import commonly.commonlybe.petitioner.repository.PetitionerRepository;
import commonly.commonlybe.user.entity.User;
import commonly.commonlybe.user.repository.UserRepository;
import commonly.commonlybe.user.exception.UserAlreadyExistsException;
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
