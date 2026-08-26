package commonly.commonlybe.global.s3;

import commonly.commonlybe.file.exception.FileException;
import commonly.commonlybe.global.error.error_code.FileErrorCode;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private static final DateTimeFormatter KEY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile file) {
        String key = generateKey(file.getOriginalFilename());
        try {
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(key).contentType(file.getContentType()).build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | SdkException e) {
            log.error("S3 업로드 실패 : bucket={}, key={}", bucket, key, e);
            throw new FileException(FileErrorCode.STORAGE_FAILURE);
        }
        return key;
    }

    public byte[] download(String key) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build())
                    .asByteArray();
        } catch (SdkException e) {
            log.error("S3 다운로드 실패 : bucket={}, key={}", bucket, key, e);
            throw new FileException(FileErrorCode.STORAGE_FAILURE);
        }
    }

    /**
     * 후속 처리가 실패했을 때 업로드된 객체를 정리한다. 삭제 실패는 원래 예외를 가리지 않도록 로그만 남긴다.
     */
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            log.error("S3 삭제 실패 : bucket={}, key={}", bucket, key, e);
        }
    }

    private String generateKey(String originalFilename) {
        String datePrefix = KEY_DATE_FORMATTER.format(LocalDate.now());
        return "uploads/certificates/%s/%s%s".formatted(datePrefix, UUID.randomUUID(), extractExtension(originalFilename));
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        return dotIndex == -1 ? "" : originalFilename.substring(dotIndex);
    }
}
