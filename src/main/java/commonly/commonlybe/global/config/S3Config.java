package commonly.commonlybe.global.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${app.s3.region}")
    private String region;

    @Value("${app.s3.endpoint:}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder().region(Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            // 로컬 S3 대체 구현(LocalStack 등)은 가상 호스트 방식(bucket.host)을 해석하지 못한다.
            // 엔드포인트를 재정의하는 경우에만 path-style을 강제하고, 실제 AWS에서는 기본값을 유지한다.
            builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        }
        return builder.build();
    }
}
