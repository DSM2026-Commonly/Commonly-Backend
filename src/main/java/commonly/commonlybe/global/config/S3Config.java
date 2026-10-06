package commonly.commonlybe.global.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
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
            // SDK 2.30+는 업로드에 CRC32 체크섬을 기본으로 붙여 aws-chunked 트레일러로 보낸다.
            // Garage 등 S3 호환 저장소는 이를 처리하지 못해 Invalid payload signature를 반환하므로,
            // 체크섬은 API가 요구할 때만 계산·검증한다.
            builder.requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                    .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        }
        return builder.build();
    }
}
