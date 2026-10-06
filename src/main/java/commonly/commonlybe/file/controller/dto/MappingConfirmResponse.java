package commonly.commonlybe.file.controller.dto;

import java.util.List;

/**
 * @param createdHumanCount 엑셀에만 있던 대상자를 새로 만든 수.
 *                          성명 오타 한 글자가 별개 인물을 만들 수 있어 담당자가 확인할 수 있게 내려준다.
 */
public record MappingConfirmResponse(boolean saved,
                                     int insertedCount,
                                     int createdHumanCount,
                                     List<FailedRowDto> failedRows) {
}
