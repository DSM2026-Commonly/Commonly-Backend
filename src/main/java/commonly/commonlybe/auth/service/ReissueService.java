package commonly.commonlybe.auth.service;

import commonly.commonlybe.auth.controller.dto.ReissueRequest;
import commonly.commonlybe.auth.controller.dto.TokenResponse;
import commonly.commonlybe.auth.entity.RefreshToken;
import commonly.commonlybe.auth.repository.RefreshTokenRepository;
import commonly.commonlybe.global.jwt.JwtParser;
import commonly.commonlybe.global.jwt.exception.RefreshTokenNotFoundException;
import commonly.commonlybe.user.exception.UserNotFoundException;
import commonly.commonlybe.user.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리프레시 토큰으로 액세스 토큰을 재발급한다.
 *
 * 쓴 리프레시 토큰은 그 자리에서 폐기하고 새로 발급한다(회전).
 * 같은 토큰을 두 번 쓸 수 없으므로 탈취된 토큰이 무한정 쓰이지 않는다.
 */
@Service
@RequiredArgsConstructor
public class ReissueService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtParser jwtParser;
    private final TokenIssuer tokenIssuer;

    @Transactional
    public TokenResponse execute(ReissueRequest request) {
        // 서명·만료·타입 검증. 실패하면 여기서 401이 난다.
        String accountId = jwtParser.parseRefreshTokenSubject(request.getRefreshToken());

        // 서명이 살아 있어도 DB에 행이 없으면 무효다(로그아웃, 이미 회전됨, 강제 폐기).
        RefreshToken saved = refreshTokenRepository.findByToken(request.getRefreshToken())
            .orElseThrow(RefreshTokenNotFoundException::new);

        refreshTokenRepository.delete(saved);

        // 삭제된 계정의 토큰으로는 재발급하지 않는다. 위에서 이미 행을 지웠으므로 재사용도 불가능하다.
        if (!userRepository.existsByAccountId(accountId)) {
            throw new UserNotFoundException();
        }

        refreshTokenRepository.deleteAllByAccountIdAndExpiresAtBefore(accountId, LocalDateTime.now());

        return tokenIssuer.issue(accountId);
    }
}
