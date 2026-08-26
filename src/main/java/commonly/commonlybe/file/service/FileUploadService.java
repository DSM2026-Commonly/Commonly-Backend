package commonly.commonlybe.file.service;

import commonly.commonlybe.file.FileValidator;
import commonly.commonlybe.file.controller.dto.ExcelRowDto;
import commonly.commonlybe.file.controller.dto.FileUploadResponse;
import commonly.commonlybe.file.entity.FileEntity;
import commonly.commonlybe.file.excel.ExcelParser;
import commonly.commonlybe.file.excel.HeaderNormalizer;
import commonly.commonlybe.file.excel.ParsedExcel;
import commonly.commonlybe.file.exception.FileException;
import commonly.commonlybe.file.repository.FileRepository;
import commonly.commonlybe.global.error.error_code.FileErrorCode;
import commonly.commonlybe.global.s3.S3Uploader;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final S3Uploader s3Uploader;
    private final FileRepository fileRepository;

    @Value("${app.file.max-rows}")
    private int maxRows;

    @Transactional
    public FileUploadResponse upload(MultipartFile file) {
        FileValidator.validate(file);

        // 저장소에 추적되지 않는 개인정보 파일이 남지 않도록 파싱·검증을 먼저 끝낸다.
        ParsedExcel parsedExcel = parseExcel(file);

        String objectKey = s3Uploader.upload(file);
        FileEntity fileEntity;
        try {
            fileEntity = fileRepository.save(FileEntity.builder()
                    .originalName(file.getOriginalFilename())
                    .savedName(extractSavedName(objectKey))
                    .filePath(objectKey)
                    .fileSize(file.getSize())
                    .build());
        } catch (RuntimeException e) {
            s3Uploader.delete(objectKey);
            throw e;
        }

        List<String> columns = parsedExcel.headers().stream()
                .map(HeaderNormalizer::normalize)
                .toList();
        List<ExcelRowDto> rows = parsedExcel.rows().stream()
                .map(row -> new ExcelRowDto(row.rowIndex(), row.cells()))
                .toList();

        return new FileUploadResponse(fileEntity.getFileId(), file.getOriginalFilename(), columns, rows);
    }

    private ParsedExcel parseExcel(MultipartFile file) {
        try {
            return ExcelParser.parse(file.getInputStream(), maxRows);
        } catch (IOException e) {
            throw new FileException(FileErrorCode.UNPROCESSABLE_FILE);
        }
    }

    private String extractSavedName(String objectKey) {
        int lastSlash = objectKey.lastIndexOf('/');
        return lastSlash == -1 ? objectKey : objectKey.substring(lastSlash + 1);
    }
}
