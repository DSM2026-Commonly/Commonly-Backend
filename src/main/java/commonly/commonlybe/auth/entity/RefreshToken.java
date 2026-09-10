package commonly.commonlybe.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발급된 리프레시 토큰 1건. 재발급 때 이 행을 지우고 새로 넣는다(회전).
 *
 * 토큰 자체가 JWT라 서명만으로도 검증이 되지만, 그러면 로그아웃이나 탈취 대응으로
 * 무효화할 방법이 없다. 행이 있어야 유효한 것으로 보고, 지우면 즉시 무효가 되게 한다.
 *
 * 한 계정에 여러 행이 있을 수 있다(기기별 로그인).
 */
@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long refreshTokenId;

    @Column(name = "account_id", nullable = false, length = 50)
    private String accountId;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    public RefreshToken(String accountId, String token, LocalDateTime expiresAt) {
        this.accountId = accountId;
        this.token = token;
        this.expiresAt = expiresAt;
    }
}
