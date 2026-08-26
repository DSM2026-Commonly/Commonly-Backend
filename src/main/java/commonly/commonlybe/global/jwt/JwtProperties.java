package commonly.commonlybe.global.jwt;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import java.util.Base64;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties (
    String secret,
    String header,
    String prefix,
    Integer accessExp
) {
    public JwtProperties(String secret, String header, String prefix, Integer accessExp) {
        this.secret = Base64.getEncoder().encodeToString(secret.getBytes());
        this.header = header;
        this.prefix = prefix;
        this.accessExp = accessExp;
    }

    public SecretKey secretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
