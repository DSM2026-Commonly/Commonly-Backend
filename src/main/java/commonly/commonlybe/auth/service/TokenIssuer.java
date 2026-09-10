package commonly.commonlybe.auth.service;

import commonly.commonlybe.auth.controller.dto.TokenResponse;
import commonly.commonlybe.auth.entity.RefreshToken;
import commonly.commonlybe.auth.repository.RefreshTokenRepository;
import commonly.commonlybe.global.jwt.JwtGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 액세스 + 리프레시 토큰 한 쌍을 발급하고 리프레시 토큰을 저장한다.
 * 로그인·회원가입·재발급 세 곳이 같은 절차를 쓴다.
 */
@Component
@RequiredArgsConstructor
public class TokenIssuer {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtGenerator jwtGenerator;

    public TokenResponse issue(String accountId) {
        String refreshToken = jwtGenerator.generateRefreshToken(accountId);

        refreshTokenRepository.save(RefreshToken.builder()
            .accountId(accountId)
            .token(refreshToken)
            .expiresAt(jwtGenerator.refreshTokenExpiresAt())
            .build());

        return TokenResponse.builder()
            .accessToken(jwtGenerator.generateAccessToken(accountId))
            .refreshToken(refreshToken)
            .build();
    }
}
