package commonly.commonlybe.file.service;

import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.entity.Gender;
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
import commonly.commonlybe.human.entity.HumanEntity;
import commonly.commonlybe.human.repository.HumanRepository;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final HumanRepository humanRepository;
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
            return new MappingConfirmResponse(false, 0, 0, List.of());
        }

        // 확정은 파일당 한 번만 허용한다. 조건부 UPDATE라 동시 요청 중 하나만 통과한다.
        if (fileRepository.markConfirmed(fileId) == 0) {
            throw new FileException(FileErrorCode.ALREADY_CONFIRMED);
        }

        List<CertificateEntity> toInsert = new ArrayList<>();
        List<FailedRowDto> failedRows = new ArrayList<>();
        // 한 사람의 이력이 여러 행에 걸치므로 (성명, 생년월일)당 한 번만 조회·생성한다.
        Map<HumanKey, HumanEntity> humans = new HashMap<>();
        int createdHumanCount = 0;

        for (ParsedRow row : parsedExcel.rows()) {
            Map<String, String> fieldValues = extractFieldValues(row, request.mappings(), columnIndex);
            switch (RowValidator.validate(fieldValues)) {
                case RowResult.Success success -> {
                    CertificateEntity certificate = success.certificate();
                    HumanKey key = new HumanKey(certificate.getName(), certificate.getBirthDate());

                    HumanEntity human = humans.get(key);
                    if (human == null) {
                        human = humanRepository.findByNameAndBirthDate(key.name(), key.birthDate())
                                .orElse(null);
                        if (human == null) {
                            human = createHuman(certificate, trimToNull(fieldValues.get("address")));
                            createdHumanCount++;
                        }
                        humans.put(key, human);
                    }

                    linkHuman(certificate, human);
                    toInsert.add(certificate);
                }
                case RowResult.Failure failure -> failedRows.add(new FailedRowDto(row.rowIndex(), failure.reason()));
                case RowResult.Skip skip -> { }
            }
        }

        certificateRepository.saveAll(toInsert);

        return new MappingConfirmResponse(true, toInsert.size(), createdHumanCount, failedRows);
    }

    /**
     * 엑셀에만 있는 대상자는 인적사항을 만들어 준다. 전에는 행을 실패시켰는데,
     * 그러면 개별 등록으로 사람을 먼저 만들어 두지 않은 엑셀은 통째로 올라가지 않았다 (#81).
     *
     * 엑셀 필수 매핑(성명/생년월일/성별)이 humans의 NOT NULL 컬럼과 정확히 일치하므로
     * 추가 입력 없이 만들 수 있다. 주소는 선택 매핑이고, 없으면 null로 두고
     * 증명서 서식의 주소 칸은 공란으로 찍힌다 — 틀린 주소를 인쇄하는 것보다 낫다.
     *
     * department는 채우지 않는다. humans.department는 사람당 한 개라
     * 기간별 부서를 표현할 수 없고, 재직 이력의 부서는 certificate.department가 따로 갖는다.
     *
     * ponytail: 같은 사람이 든 엑셀을 두 담당자가 동시에 확정하면 uk_humans_name_birth_date
     * 위반으로 트랜잭션이 깨진다. 창구 운영에서는 발생하지 않는다고 보고 재시도를 넣지 않았다.
     */
    private HumanEntity createHuman(CertificateEntity certificate, String address) {
        return humanRepository.save(HumanEntity.builder()
                .name(certificate.getName())
                .birthDate(certificate.getBirthDate())
                // 이름만 같은 별개 enum이다. 상수 이름(MALE/FEMALE)은 양쪽이 같다.
                .gender(commonly.commonlybe.human.entity.Gender.valueOf(certificate.getGender().name()))
                .address(address)
                .build());
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 조회·발급·수정이 human_id로만 대상을 찾으므로 연결 없이 저장하면 어디에도 안 보인다 (#35).
     * 엑셀의 성별이 humans와 달라도 humans를 따른다 — 개별 등록(CertificateService.create)과 같은 기준.
     */
    private void linkHuman(CertificateEntity certificate, HumanEntity human) {
        // 이름만 같은 별개 enum이다. human은 M/F로 직렬화하고 certificate는 MALE/FEMALE이다.
        certificate.linkHuman(human.getHumanId(), human.getName(), human.getBirthDate(),
                Gender.valueOf(human.getGender().name()));
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

    private record HumanKey(String name, LocalDate birthDate) {
    }
}
