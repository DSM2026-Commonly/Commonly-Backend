package commonly.commonlybe.auth.repository;

import commonly.commonlybe.auth.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteAllByAccountIdAndExpiresAtBefore(String accountId, LocalDateTime at);
}
