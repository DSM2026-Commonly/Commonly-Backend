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

        String objectKey = s3Uploader.upload(file);
        FileEntity fileEntity = fileRepository.save(FileEntity.builder()
                .originalName(file.getOriginalFilename())
                .savedName(extractSavedName(objectKey))
                .filePath(objectKey)
                .fileSize(file.getSize())
                .build());

        ParsedExcel parsedExcel = parseExcel(file);
        if (parsedExcel.rows().size() > maxRows) {
            throw new FileException(FileErrorCode.ROW_COUNT_EXCEEDED);
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
            return ExcelParser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new FileException(FileErrorCode.UNPROCESSABLE_FILE);
        }
    }

    private String extractSavedName(String objectKey) {
        int lastSlash = objectKey.lastIndexOf('/');
        return lastSlash == -1 ? objectKey : objectKey.substring(lastSlash + 1);
    }
}
