package com.project.ds_helper.domain.trashbin.service;

import com.project.ds_helper.common.exception.BusinessException;
import com.project.ds_helper.domain.trashbin.entity.TrashBin;
import com.project.ds_helper.domain.trashbin.repository.TrashBinRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrashBinCsvServiceTest {

    @Mock
    private TrashBinRepository trashBinRepository;

    @InjectMocks
    private TrashBinCsvService trashBinCsvService;

    @Test
    @DisplayName("CSV 파서는 쓰레기통 엔티티 목록을 만든다")
    void parseCsv_returnsTrashBins() throws Exception {
        String csv = """
                시도명,설치주소,사진,위치 설명,설치지점,쓰레기통 종류,관리기관명,관리기관전화번호,도시구군명,위도,경도,데이터기준일자,존재여부
                대구광역시,화원읍 비슬로 2679,http://image,설명,아파트안,일반쓰레기/재활용,달성군청,053-668-2714,달성군,35.808057,128.508827,2026-02-20,Y
                """;
        MockMultipartFile file = new MockMultipartFile("file", "trash.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        List<TrashBin> result = trashBinCsvService.parseCsv(file);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDataReferenceDate()).isEqualTo(LocalDate.of(2026, 2, 20));
    }

    @Test
    @DisplayName("중복 좌표가 있으면 예외가 발생한다")
    void validateNoDuplicateCoordinates_throwsWhenDuplicated() {
        TrashBin trashBin = TrashBin.builder().latitude(35.1).longitude(128.1).address("addr").build();
        when(trashBinRepository.existsByLatitudeAndLongitude(35.1, 128.1)).thenReturn(false);

        assertThatThrownBy(() -> trashBinCsvService.validateNoDuplicateCoordinates(List.of(trashBin, trashBin)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("DB에 이미 같은 좌표가 있으면 예외가 발생한다")
    void validateNoDuplicateCoordinates_throwsWhenExistsInDb() {
        TrashBin trashBin = TrashBin.builder().latitude(35.1).longitude(128.1).address("addr").build();
        when(trashBinRepository.existsByLatitudeAndLongitude(35.1, 128.1)).thenReturn(true);

        assertThatThrownBy(() -> trashBinCsvService.validateNoDuplicateCoordinates(List.of(trashBin)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("CSV 헤더가 비어 있으면 예외가 발생한다")
    void parseCsv_throwsWhenHeaderBlank() {
        MockMultipartFile file = new MockMultipartFile("file", "trash.csv", "text/csv", "\n".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> trashBinCsvService.parseCsv(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CSV header is empty");
    }

    @Test
    @DisplayName("빈 행은 건너뛴다")
    void parseCsv_skipsBlankLine() throws Exception {
        String csv = """
                시도명,설치주소,사진,위치 설명,설치지점,쓰레기통 종류,관리기관명,관리기관전화번호,도시구군명,위도,경도,데이터기준일자,존재여부

                대구광역시,화원읍 비슬로 2679,http://image,설명,아파트안,일반쓰레기/재활용,달성군청,053-668-2714,달성군,35.808057,128.508827,2026-02-20,Y
                """;
        MockMultipartFile file = new MockMultipartFile("file", "trash.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        List<TrashBin> result = trashBinCsvService.parseCsv(file);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("좌표가 비어 있으면 예외가 발생한다")
    void validateNoDuplicateCoordinates_throwsWhenCoordinateMissing() {
        TrashBin trashBin = TrashBin.builder().address("addr").build();

        assertThatThrownBy(() -> trashBinCsvService.validateNoDuplicateCoordinates(List.of(trashBin)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Trash bin coordinate is required");
    }

    @Test
    @DisplayName("CSV 행 파서는 따옴표와 이스케이프 따옴표를 처리한다")
    void parseCsvLine_parsesQuotedValues() {
        List<String> values = ReflectionTestUtils.invokeMethod(trashBinCsvService, "parseCsvLine", "\"a,1\",\"b\"\"2\"");

        assertThat(values).containsExactly("a,1", "b\"2");
    }

    @Test
    @DisplayName("값 조회는 헤더 누락과 공백을 null로 처리한다")
    void getValue_returnsNullForMissingOrBlankValues() {
        String missing = ReflectionTestUtils.invokeMethod(
                trashBinCsvService,
                "getValue",
                List.of("A"),
                List.of("value"),
                "B"
        );
        String blank = ReflectionTestUtils.invokeMethod(
                trashBinCsvService,
                "getValue",
                List.of("A"),
                List.of("   "),
                "A"
        );

        assertThat(missing).isNull();
        assertThat(blank).isNull();
    }

    @Test
    @DisplayName("숫자와 날짜 파서는 공백 입력을 null로 처리한다")
    void parseNumberAndDate_returnNullForBlank() {
        Double number = ReflectionTestUtils.invokeMethod(trashBinCsvService, "parseDouble", "   ");
        LocalDate date = ReflectionTestUtils.invokeMethod(trashBinCsvService, "parseLocalDate", "   ");

        assertThat(number).isNull();
        assertThat(date).isNull();
    }

    @Test
    @DisplayName("좌표 키는 위도와 경도로 생성된다")
    void toCoordinateKey_buildsKey() {
        String key = ReflectionTestUtils.invokeMethod(
                trashBinCsvService,
                "toCoordinateKey",
                TrashBin.builder().latitude(35.1).longitude(128.1).build()
        );

        assertThat(key).isEqualTo("35.1|128.1");
    }
}
