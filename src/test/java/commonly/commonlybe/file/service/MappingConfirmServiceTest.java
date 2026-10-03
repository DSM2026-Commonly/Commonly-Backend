package commonly.commonlybe.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.entity.Gender;
import commonly.commonlybe.certificate.repository.CertificateRepository;
import commonly.commonlybe.file.controller.dto.ColumnMapping;
import commonly.commonlybe.file.controller.dto.FailedRowDto;
import commonly.commonlybe.file.controller.dto.MappingConfirmRequest;
import commonly.commonlybe.file.controller.dto.MappingConfirmResponse;
import commonly.commonlybe.file.entity.FileEntity;
import commonly.commonlybe.file.exception.FileException;
import commonly.commonlybe.file.repository.FileRepository;
import commonly.commonlybe.global.error.error_code.FileErrorCode;
import commonly.commonlybe.global.s3.S3Uploader;
import commonly.commonlybe.human.entity.HumanEntity;
import commonly.commonlybe.human.repository.HumanRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MappingConfirmServiceTest {

    private static final Long FILE_ID = 1L;
    private static final Long HUMAN_ID = 10L;
    private static final String OBJECT_KEY = "excel/upload.xlsx";
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 1);
    private static final List<String> HEADERS = List.of("성명", "생년월일", "성별", "채용일");

    @Mock
    private FileRepository fileRepository;

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private HumanRepository humanRepository;

    @Mock
    private S3Uploader s3Uploader;

    @InjectMocks
    private MappingConfirmService mappingConfirmService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mappingConfirmService, "maxRows", 100);
    }

    /**
     * 조회·발급·수정은 human_id로만 대상을 찾는다. 연결 없이 저장하면 "성공"인데 어디에도 안 보인다 (#35).
     * 성별은 엑셀이 아니라 humans를 따른다.
     */
    @Test
    void 성명과_생년월일로_인적사항을_찾아_연결한다() throws IOException {
        givenUploadedExcel(List.of(
                List.of("홍길동", "1990-01-01", "여", "2020-01-01"),
                List.of("홍길동", "1990-01-01", "여", "2023-01-01")));
        given(humanRepository.findByNameAndBirthDate("홍길동", BIRTH_DATE)).willReturn(Optional.of(human()));

        MappingConfirmResponse response = mappingConfirmService.confirm(FILE_ID, confirmRequest());

        assertThat(response.insertedCount()).isEqualTo(2);
        assertThat(response.failedRows()).isEmpty();
        List<CertificateEntity> saved = captureSaved();
        assertThat(saved).allSatisfy(certificate -> {
            assertThat(certificate.getHumanId()).isEqualTo(HUMAN_ID);
            assertThat(certificate.getGender()).isEqualTo(Gender.MALE);
        });
        // 같은 사람의 행이 여러 개여도 조회는 한 번
        verify(humanRepository, times(1)).findByNameAndBirthDate(any(), any());
    }

    @Test
    void 인적사항이_없는_대상자는_행_오류로_돌려준다() throws IOException {
        givenUploadedExcel(List.of(
                List.of("홍길동", "1990-01-01", "남", "2020-01-01"),
                List.of("김철수", "1985-05-05", "남", "2021-01-01")));
        given(humanRepository.findByNameAndBirthDate("홍길동", BIRTH_DATE)).willReturn(Optional.of(human()));
        given(humanRepository.findByNameAndBirthDate("김철수", LocalDate.of(1985, 5, 5)))
                .willReturn(Optional.empty());

        MappingConfirmResponse response = mappingConfirmService.confirm(FILE_ID, confirmRequest());

        assertThat(response.insertedCount()).isEqualTo(1);
        assertThat(response.failedRows()).extracting(FailedRowDto::rowIndex).containsExactly(3);
        assertThat(captureSaved()).extracting(CertificateEntity::getHumanId).containsExactly(HUMAN_ID);
    }

    @Test
    void 생년월일이_빈_행은_매칭하지_않고_행_오류로_돌려준다() throws IOException {
        givenUploadedExcel(List.of(List.of("홍길동", "", "남", "2020-01-01")));

        MappingConfirmResponse response = mappingConfirmService.confirm(FILE_ID, confirmRequest());

        assertThat(response.insertedCount()).isZero();
        assertThat(response.failedRows()).extracting(FailedRowDto::reason)
                .containsExactly("생년월일이 비어 있습니다");
        verify(humanRepository, never()).findByNameAndBirthDate(any(), any());
    }

    /** macOS에서 만든 엑셀은 한글이 NFD로 들어올 수 있다. humans와 비교하기 전에 NFC로 맞춘다. */
    @Test
    void NFD_성명도_NFC로_맞춰_매칭한다() throws IOException {
        String nfdName = Normalizer.normalize("홍길동", Normalizer.Form.NFD);
        givenUploadedExcel(List.of(List.of(nfdName, "1990-01-01", "남", "2020-01-01")));
        given(humanRepository.findByNameAndBirthDate("홍길동", BIRTH_DATE)).willReturn(Optional.of(human()));

        MappingConfirmResponse response = mappingConfirmService.confirm(FILE_ID, confirmRequest());

        assertThat(response.insertedCount()).isEqualTo(1);
    }

    @Test
    void 생년월일_열을_매핑하지_않으면_거부한다() throws IOException {
        givenUploadedExcel(List.of(List.of("홍길동", "1990-01-01", "남", "2020-01-01")));
        MappingConfirmRequest request = new MappingConfirmRequest(List.of(
                new ColumnMapping("성명", "name"),
                new ColumnMapping("성별", "gender")), true);

        assertThatThrownBy(() -> mappingConfirmService.confirm(FILE_ID, request))
                .isInstanceOf(FileException.class)
                .extracting(e -> ((FileException) e).getErrorProperty())
                .isEqualTo(FileErrorCode.REQUIRED_FIELD_NOT_MAPPED);

        verify(certificateRepository, never()).saveAll(any());
    }

    private void givenUploadedExcel(List<List<String>> rows) throws IOException {
        FileEntity fileEntity = FileEntity.builder()
                .originalName("upload.xlsx")
                .savedName("upload.xlsx")
                .filePath(OBJECT_KEY)
                .fileSize(1L)
                .build();
        given(fileRepository.findById(FILE_ID)).willReturn(Optional.of(fileEntity));
        given(s3Uploader.download(OBJECT_KEY)).willReturn(excel(rows));
        lenient().when(fileRepository.markConfirmed(FILE_ID)).thenReturn(1);
    }

    private byte[] excel(List<List<String>> rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            writeRow(sheet.createRow(0), HEADERS);
            for (int i = 0; i < rows.size(); i++) {
                writeRow(sheet.createRow(i + 1), rows.get(i));
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void writeRow(Row row, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            row.createCell(i).setCellValue(values.get(i));
        }
    }

    private MappingConfirmRequest confirmRequest() {
        return new MappingConfirmRequest(List.of(
                new ColumnMapping("성명", "name"),
                new ColumnMapping("생년월일", "birthDate"),
                new ColumnMapping("성별", "gender"),
                new ColumnMapping("채용일", "hireDate")), true);
    }

    @SuppressWarnings("unchecked")
    private List<CertificateEntity> captureSaved() {
        ArgumentCaptor<List<CertificateEntity>> saved = ArgumentCaptor.forClass(List.class);
        verify(certificateRepository).saveAll(saved.capture());
        return saved.getValue();
    }

    private HumanEntity human() {
        HumanEntity human = HumanEntity.builder()
                .name("홍길동")
                .gender(commonly.commonlybe.human.entity.Gender.MALE)
                .birthDate(BIRTH_DATE)
                .build();
        ReflectionTestUtils.setField(human, "humanId", HUMAN_ID);
        return human;
    }
}
