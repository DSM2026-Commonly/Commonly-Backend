package commonly.commonlybe.file.service;

import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.repository.CertificateRepository;
import commonly.commonlybe.file.controller.dto.ColumnMapping;
import commonly.commonlybe.file.controller.dto.FailedRowDto;
import commonly.commonlybe.file.controller.dto.MappingConfirmRequest;
import commonly.commonlybe.file.controller.dto.MappingConfirmResponse;
import commonly.commonlybe.file.entity.FileEntity;
import commonly.commonlybe.file.excel.ColumnMappingTable;
import commonly.commonlybe.file.excel.ExcelParser;
import commonly.commonlybe.file.excel.HeaderNormalizer;
import commonly.commonlybe.file.excel.ParsedExcel;
import commonly.commonlybe.file.excel.ParsedRow;
import commonly.commonlybe.file.excel.RowResult;
import commonly.commonlybe.file.excel.RowValidator;
import commonly.commonlybe.file.exception.FileException;
import commonly.commonlybe.file.repository.FileRepository;
import commonly.commonlybe.global.error.error_code.FileErrorCode;
import commonly.commonlybe.global.s3.S3Uploader;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MappingConfirmService {

    private final FileRepository fileRepository;
    private final CertificateRepository certificateRepository;
    private final S3Uploader s3Uploader;

    @Value("${app.file.max-rows}")
    private int maxRows;

    @Transactional
    public MappingConfirmResponse confirm(Long fileId, MappingConfirmRequest request) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));

        ParsedExcel parsedExcel = downloadAndParse(fileEntity.getFilePath());
        Map<String, Integer> columnIndex = buildColumnIndex(parsedExcel.headers());
        validateMappings(request.mappings(), columnIndex.keySet());

        if (!request.confirmed()) {
            return new MappingConfirmResponse(false, 0, List.of());
        }

        // 확정은 파일당 한 번만 허용한다. 조건부 UPDATE라 동시 요청 중 하나만 통과한다.
        if (fileRepository.markConfirmed(fileId) == 0) {
            throw new FileException(FileErrorCode.ALREADY_CONFIRMED);
        }

        List<CertificateEntity> toInsert = new ArrayList<>();
        List<FailedRowDto> failedRows = new ArrayList<>();

        for (ParsedRow row : parsedExcel.rows()) {
            Map<String, String> fieldValues = extractFieldValues(row, request.mappings(), columnIndex);
            switch (RowValidator.validate(fieldValues)) {
                case RowResult.Success success -> toInsert.add(success.certificate());
                case RowResult.Failure failure -> failedRows.add(new FailedRowDto(row.rowIndex(), failure.reason()));
                case RowResult.Skip skip -> { }
            }
        }

        certificateRepository.saveAll(toInsert);

        return new MappingConfirmResponse(true, toInsert.size(), failedRows);
    }

    private ParsedExcel downloadAndParse(String objectKey) {
        byte[] content = s3Uploader.download(objectKey);
        return ExcelParser.parse(new ByteArrayInputStream(content), maxRows);
    }

    private Map<String, Integer> buildColumnIndex(List<String> rawHeaders) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < rawHeaders.size(); i++) {
            index.put(HeaderNormalizer.normalize(rawHeaders.get(i)), i);
        }
        return index;
    }

    private void validateMappings(List<ColumnMapping> mappings, Set<String> availableColumns) {
        Set<String> mappedTargetFields = new HashSet<>();
        for (ColumnMapping mapping : mappings) {
            if (!availableColumns.contains(mapping.sourceColumn())) {
                throw new FileException(FileErrorCode.SOURCE_COLUMN_NOT_FOUND,
                        "존재하지 않는 열입니다: '%s'".formatted(mapping.sourceColumn()));
            }
            if (!ColumnMappingTable.isValidTargetField(mapping.targetField())) {
                throw new FileException(FileErrorCode.TARGET_FIELD_NOT_FOUND,
                        "존재하지 않는 필드입니다: '%s'".formatted(mapping.targetField()));
            }
            if (!mappedTargetFields.add(mapping.targetField())) {
                throw new FileException(FileErrorCode.DUPLICATE_TARGET_FIELD,
                        "같은 필드에 두 개 이상의 열을 매핑할 수 없습니다: '%s'".formatted(mapping.targetField()));
            }
        }
        for (String required : ColumnMappingTable.requiredTargetFields()) {
            if (!mappedTargetFields.contains(required)) {
                throw new FileException(FileErrorCode.REQUIRED_FIELD_NOT_MAPPED,
                        "필수 필드가 매핑되지 않았습니다: '%s'".formatted(required));
            }
        }
    }

    private Map<String, String> extractFieldValues(ParsedRow row, List<ColumnMapping> mappings,
                                                     Map<String, Integer> columnIndex) {
        Map<String, String> values = new HashMap<>();
        for (ColumnMapping mapping : mappings) {
            int idx = columnIndex.get(mapping.sourceColumn());
            String cellValue = idx < row.cells().size() ? row.cells().get(idx) : "";
            values.put(mapping.targetField(), cellValue);
        }
        return values;
    }
}
