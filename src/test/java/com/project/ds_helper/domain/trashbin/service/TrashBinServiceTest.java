package com.project.ds_helper.domain.trashbin.service;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.common.exception.BusinessException;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.ImageUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.trashbin.dto.response.GetTrashBinsResponseDto;
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinImageResponseDto;
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinImagesResponseDto;
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinsResponseDto;
import com.project.ds_helper.domain.trashbin.entity.TrashBin;
import com.project.ds_helper.domain.trashbin.entity.TrashBinImage;
import com.project.ds_helper.domain.trashbin.repository.TrashBinImageRepository;
import com.project.ds_helper.domain.trashbin.repository.TrashBinRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrashBinServiceTest {

    @Mock
    private TrashBinRepository trashBinRepository;

    @Mock
    private TrashBinImageRepository trashBinImageRepository;

    @Mock
    private ImageUtil imageUtil;

    @Mock
    private ImageCompressionUtil imageCompressionUtil;

    @Mock
    private S3Util s3Util;

    @InjectMocks
    private TrashBinService trashBinService;

    @Test
    @DisplayName("CSV 업로드 시 쓰레기통 데이터를 저장한다")
    void uploadTrashBins_savesParsedTrashBins() throws Exception {
        String csv = """
                시도명,설치주소,사진,,,위치 설명,설치지점,쓰레기통 종류,관리기관명,관리기관전화번호,도시구군명,위도,경도,데이터기준일자,존재여부
                대구광역시,화원읍 비슬로 2679,,,,한우아파트 입구 좌측에 있어요.,아파트안,일반쓰레기/재활용,달성군청,053-668-2714,달성군,35.808057,128.508827,2026-02-20,
                대구광역시,옥포읍 신당리 1037-1,,,,(주)보성중기 건물 건너편에 있어요.,도로변,재활용,,,,"35.794500,",128.441637,2026-02-20,
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "trash-bins.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
        when(trashBinRepository.existsByLatitudeAndLongitude(35.808057, 128.508827)).thenReturn(false);
        when(trashBinRepository.existsByLatitudeAndLongitude(35.794500, 128.441637)).thenReturn(false);

        UploadTrashBinsResponseDto result = trashBinService.uploadTrashBins(file);

        ArgumentCaptor<List<TrashBin>> captor = ArgumentCaptor.forClass(List.class);
        verify(trashBinRepository).saveAll(captor.capture());

        assertThat(result.savedCount()).isEqualTo(2);
        assertThat(captor.getValue()).hasSize(2);
        TrashBin trashBin = captor.getValue().getFirst();
        assertThat(trashBin.getProvinceName()).isEqualTo("대구광역시");
        assertThat(trashBin.getAddress()).isEqualTo("화원읍 비슬로 2679");
        assertThat(trashBin.getLocationDescription()).isEqualTo("한우아파트 입구 좌측에 있어요.");
        assertThat(trashBin.getBinType()).isEqualTo("일반쓰레기/재활용");
        assertThat(trashBin.getLatitude()).isEqualTo(35.808057);
        assertThat(trashBin.getLongitude()).isEqualTo(128.508827);
        assertThat(trashBin.getDataReferenceDate()).isEqualTo(LocalDate.of(2026, 2, 20));

        TrashBin trashBinWithTrailingCommaLatitude = captor.getValue().get(1);
        assertThat(trashBinWithTrailingCommaLatitude.getLatitude()).isEqualTo(35.794500);
        assertThat(trashBinWithTrailingCommaLatitude.getLongitude()).isEqualTo(128.441637);
    }

    @Test
    @DisplayName("CSV 업로드 시 기존 DB와 같은 위도/경도 데이터가 있으면 전체 업로드를 실패시킨다")
    void uploadTrashBins_throwsWhenCoordinateAlreadyExistsInDb() {
        String csv = """
                시도명,설치주소,사진,,,위치 설명,설치지점,쓰레기통 종류,관리기관명,관리기관전화번호,도시구군명,위도,경도,데이터기준일자,존재여부
                대구광역시,화원읍 비슬로 2679,,,,한우아파트 입구 좌측에 있어요.,아파트안,일반쓰레기/재활용,달성군청,053-668-2714,달성군,35.808057,128.508827,2026-02-20,
                대구광역시,화원읍 비슬로 2674,,,,청구청탑아파트 입구에서 직진하면 보여요.,아파트안,일반쓰레기/재활용,달성군청,053-668-2714,달성군,35.807086,128.508388,2026-02-20,
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "trash-bins.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
        when(trashBinRepository.existsByLatitudeAndLongitude(35.808057, 128.508827)).thenReturn(true);

        assertThatThrownBy(() -> trashBinService.uploadTrashBins(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Trash bin coordinate already exists")
                .hasMessageContaining("35.808057")
                .hasMessageContaining("128.508827");

        verify(trashBinRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("CSV 업로드 파일 내부에 같은 위도/경도 데이터가 있으면 전체 업로드를 실패시킨다")
    void uploadTrashBins_throwsWhenCoordinateDuplicatedInCsv() {
        String csv = """
                시도명,설치주소,사진,,,위치 설명,설치지점,쓰레기통 종류,관리기관명,관리기관전화번호,도시구군명,위도,경도,데이터기준일자,존재여부
                대구광역시,화원읍 비슬로 2679,,,,한우아파트 입구 좌측에 있어요.,아파트안,일반쓰레기/재활용,달성군청,053-668-2714,달성군,35.808057,128.508827,2026-02-20,
                대구광역시,화원읍 비슬로 2679,,,,같은 CSV 안 중복 좌표,아파트안,일반쓰레기/재활용,달성군청,053-668-2714,달성군,35.808057,128.508827,2026-02-20,
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "trash-bins.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
        when(trashBinRepository.existsByLatitudeAndLongitude(35.808057, 128.508827)).thenReturn(false);

        assertThatThrownBy(() -> trashBinService.uploadTrashBins(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Duplicate trash bin coordinate in uploaded CSV")
                .hasMessageContaining("35.808057")
                .hasMessageContaining("128.508827");

        verify(trashBinRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("빈 CSV 파일 업로드 시 예외가 발생한다")
    void uploadTrashBins_throwsWhenFileEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "trash-bins.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> trashBinService.uploadTrashBins(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CSV file is required");
    }

    @Test
    @DisplayName("CSV 업로드 파일이 null이면 예외가 발생한다")
    void uploadTrashBins_throwsWhenFileNull() {
        assertThatThrownBy(() -> trashBinService.uploadTrashBins(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CSV file is required");
    }

    @Test
    @DisplayName("CSV 헤더가 비어 있으면 예외가 발생한다")
    void uploadTrashBins_throwsWhenHeaderEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "trash-bins.csv", "text/csv", "\n".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> trashBinService.uploadTrashBins(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CSV header is empty");
    }

    @Test
    @DisplayName("CSV 파서는 빈 줄과 따옴표 안의 쉼표 및 이스케이프 따옴표를 처리한다")
    void uploadTrashBins_parsesBlankLinesAndQuotedValues() throws Exception {
        String csv = """
                시도명,설치주소,사진,,,위치 설명,설치지점,쓰레기통 종류,관리기관명,관리기관전화번호,시군구명,위도,경도,데이터기준일자,존재여부

                대구광역시,"주소, 상세",,,,"설명 ""강조"" 완료",공원,일반,기관,053,수성구,35.8,128.5,2026-02-20,Y
                """;
        MockMultipartFile file = new MockMultipartFile("file", "trash-bins.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        when(trashBinRepository.existsByLatitudeAndLongitude(35.8, 128.5)).thenReturn(false);

        UploadTrashBinsResponseDto result = trashBinService.uploadTrashBins(file);

        ArgumentCaptor<List<TrashBin>> captor = ArgumentCaptor.forClass(List.class);
        verify(trashBinRepository).saveAll(captor.capture());
        assertThat(result.savedCount()).isEqualTo(1);
        assertThat(captor.getValue().getFirst().getAddress()).isEqualTo("주소, 상세");
        assertThat(captor.getValue().getFirst().getLocationDescription()).isEqualTo("설명 \"강조\" 완료");
        assertThat(captor.getValue().getFirst().getExistsYn()).isEqualTo("Y");
    }

    @Test
    @DisplayName("CSV 업로드 행에 위도 또는 경도가 없으면 전체 업로드를 실패시킨다")
    void uploadTrashBins_throwsWhenCoordinateMissing() {
        String csv = """
                시도명,설치주소,사진,,,위치 설명,설치지점,쓰레기통 종류,관리기관명,관리기관전화번호,시군구명,위도,경도,데이터기준일자,존재여부
                대구광역시,대상 주소,,,,좌표 누락 행,공원,일반쓰레기,달성군청,053-000-0000,달성군,,128.508827,2026-02-20,
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "trash-bins.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> trashBinService.uploadTrashBins(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Trash bin coordinate is required");

        verify(trashBinRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("쓰레기통 목록 조회 시 페이지 응답을 반환한다")
    void getTrashBins_returnsPagedResponse() {
        TrashBin trashBin = TrashBin.builder()
                .id("trash-bin-1")
                .provinceName("대구광역시")
                .cityCountyName("달성군")
                .address("화원읍 비슬로 2679")
                .binType("일반쓰레기/재활용")
                .managementAgencyName("달성군청")
                .managementAgencyPhoneNumber("053-668-2714")
                .latitude(35.808057)
                .longitude(128.508827)
                .dataReferenceDate(LocalDate.of(2026, 2, 20))
                .existsYn(null)
                .build();
        ReflectionTestUtils.setField(trashBin, "createdAt", LocalDateTime.of(2026, 5, 3, 12, 0, 0));

        when(trashBinRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(trashBin)));

        GetTrashBinsResponseDto result = trashBinService.getTrashBins(0, 10, "desc", "createdAt");

        assertThat(result.getTrashBins()).hasSize(1);
        assertThat(result.getTrashBins().getFirst().getId()).isEqualTo("trash-bin-1");
        assertThat(result.getTrashBins().getFirst().getProvinceName()).isEqualTo("대구광역시");
        assertThat(result.getPage().totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("쓰레기통 목록 조회 정렬 필드가 허용 목록에 없으면 예외가 발생한다")
    void getTrashBins_throwsWhenSortByUnsupported() {
        assertThatThrownBy(() -> trashBinService.getTrashBins(0, 10, "desc", "unsupported"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unsupported trash bin sortBy");
    }

    @Test
    @DisplayName("쓰레기통 목록 조회는 오름차순 정렬을 지원한다")
    void getTrashBins_supportsAscendingSort() {
        when(trashBinRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        GetTrashBinsResponseDto result = trashBinService.getTrashBins(0, 10, "asc", "createdAt");

        assertThat(result.getTrashBins()).isEmpty();
    }

    @Test
    @DisplayName("쓰레기통 목록 조회 정렬 방향이 asc/desc가 아니면 예외가 발생한다")
    void getTrashBins_throwsWhenSortDirectionUnsupported() {
        assertThatThrownBy(() -> trashBinService.getTrashBins(0, 10, "sideways", "createdAt"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unsupported trash bin sort direction");
    }

    @Test
    @DisplayName("이미지 파일명 위도와 일치하는 쓰레기통에 S3 업로드 후 이미지 메타데이터를 저장한다")
    void uploadTrashBinImage_uploadsToS3AndSavesImageMetadata() throws Exception {
        TrashBin trashBin = TrashBin.builder()
                .id("trash-bin-1")
                .latitude(35.808057)
                .longitude(128.508827)
                .build();
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "35.808057.png",
                "image/png",
                "image-bytes".getBytes(StandardCharsets.UTF_8)
        );
        S3ImageUploadRequestDto uploadRequestDto = S3ImageUploadRequestDto.builder()
                .storedFilename("stored-webp")
                .originalFilename("35.808057.png")
                .bytes("compressed".getBytes(StandardCharsets.UTF_8))
                .size(10L)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();

        when(trashBinRepository.findAllByLatitude(35.808057)).thenReturn(List.of(trashBin));
        when(trashBinImageRepository.findFirstByTrashBin_Id("trash-bin-1")).thenReturn(Optional.empty());
        when(imageUtil.toStoredFilename()).thenReturn("stored-webp");
        when(imageCompressionUtil.compressImage(image, "stored-webp")).thenReturn(uploadRequestDto);
        doNothing().when(s3Util).uploadImage(uploadRequestDto);
        when(s3Util.buildS3Key("stored-webp")).thenReturn("images/stored-webp");
        when(s3Util.toS3UrlByS3Key("images/stored-webp")).thenReturn("https://s3/images/stored-webp");
        when(trashBinImageRepository.save(any(TrashBinImage.class))).thenAnswer(invocation -> {
            TrashBinImage saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "image-1");
            return saved;
        });

        UploadTrashBinImageResponseDto result = trashBinService.uploadTrashBinImage(image);

        ArgumentCaptor<TrashBinImage> captor = ArgumentCaptor.forClass(TrashBinImage.class);
        verify(trashBinImageRepository).save(captor.capture());
        verify(s3Util).uploadImage(uploadRequestDto);

        assertThat(result.trashBinId()).isEqualTo("trash-bin-1");
        assertThat(result.imageId()).isEqualTo("image-1");
        assertThat(result.imageUrl()).isEqualTo("https://s3/images/stored-webp");
        assertThat(result.alreadyExists()).isFalse();
        assertThat(trashBin.getPhotoUrl()).isEqualTo("https://s3/images/stored-webp");
        assertThat(captor.getValue().getTrashBin()).isEqualTo(trashBin);
    }

    @Test
    @DisplayName("쓰레기통 이미지 복수 업로드는 각 이미지의 업로드 결과를 집계한다")
    void uploadTrashBinImages_uploadsMultipleImages() throws Exception {
        TrashBin trashBin1 = TrashBin.builder()
                .id("trash-bin-1")
                .latitude(35.808057)
                .longitude(128.508827)
                .build();
        TrashBin trashBin2 = TrashBin.builder()
                .id("trash-bin-2")
                .latitude(35.808058)
                .longitude(128.508828)
                .build();
        MockMultipartFile image1 = new MockMultipartFile("images", "35.808057.png", "image/png", "image-1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "35.808058.png", "image/png", "image-2".getBytes());
        S3ImageUploadRequestDto uploadRequestDto1 = S3ImageUploadRequestDto.builder()
                .storedFilename("stored-1")
                .originalFilename("35.808057.png")
                .bytes("compressed-1".getBytes(StandardCharsets.UTF_8))
                .size(12L)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();
        TrashBinImage existingImage = TrashBinImage.builder()
                .originalName("35.808058.png")
                .storedName("stored-2")
                .s3Key("images/stored-2")
                .url("https://s3/images/stored-2")
                .size(10L)
                .contentType("image/webp")
                .trashBin(trashBin2)
                .build();
        ReflectionTestUtils.setField(existingImage, "id", "image-2");

        when(trashBinRepository.findAllByLatitude(35.808057)).thenReturn(List.of(trashBin1));
        when(trashBinRepository.findAllByLatitude(35.808058)).thenReturn(List.of(trashBin2));
        when(trashBinImageRepository.findFirstByTrashBin_Id("trash-bin-1")).thenReturn(Optional.empty());
        when(trashBinImageRepository.findFirstByTrashBin_Id("trash-bin-2")).thenReturn(Optional.of(existingImage));
        when(imageUtil.toStoredFilename()).thenReturn("stored-1");
        when(imageCompressionUtil.compressImage(image1, "stored-1")).thenReturn(uploadRequestDto1);
        doNothing().when(s3Util).uploadImage(uploadRequestDto1);
        when(s3Util.buildS3Key("stored-1")).thenReturn("images/stored-1");
        when(s3Util.toS3UrlByS3Key("images/stored-1")).thenReturn("https://s3/images/stored-1");
        when(trashBinImageRepository.save(any(TrashBinImage.class))).thenAnswer(invocation -> {
            TrashBinImage saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "image-1");
            return saved;
        });

        UploadTrashBinImagesResponseDto result = trashBinService.uploadTrashBinImages(List.of(image1, image2));

        assertThat(result.requestedCount()).isEqualTo(2);
        assertThat(result.uploadedCount()).isEqualTo(1);
        assertThat(result.alreadyExistsCount()).isEqualTo(1);
        assertThat(result.images()).hasSize(2);
        assertThat(result.images().getFirst().alreadyExists()).isFalse();
        assertThat(result.images().get(1).alreadyExists()).isTrue();
    }

    @Test
    @DisplayName("쓰레기통 이미지 복수 업로드 요청이 비어 있으면 예외가 발생한다")
    void uploadTrashBinImages_throwsWhenImagesEmpty() {
        assertThatThrownBy(() -> trashBinService.uploadTrashBinImages(List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("images are required");
    }

    @Test
    @DisplayName("쓰레기통 이미지 복수 업로드 요청이 null이면 예외가 발생한다")
    void uploadTrashBinImages_throwsWhenImagesNull() {
        assertThatThrownBy(() -> trashBinService.uploadTrashBinImages(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("images are required");
    }

    @Test
    @DisplayName("쓰레기통 이미지 복수 업로드 중 실패하면 이전 신규 업로드 이미지를 S3에서 롤백한다")
    void uploadTrashBinImages_rollsBackPreviousUploadedImagesWhenLaterImageFails() throws Exception {
        TrashBin trashBin = TrashBin.builder()
                .id("trash-bin-1")
                .latitude(35.808057)
                .longitude(128.508827)
                .build();
        MockMultipartFile uploadedImage = new MockMultipartFile("images", "35.808057.png", "image/png", "image-1".getBytes());
        MockMultipartFile invalidImage = new MockMultipartFile("images", "invalid.png", "image/png", "image-2".getBytes());
        S3ImageUploadRequestDto uploadRequestDto = S3ImageUploadRequestDto.builder()
                .storedFilename("stored-1")
                .originalFilename("35.808057.png")
                .bytes("compressed".getBytes(StandardCharsets.UTF_8))
                .size(10L)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();

        when(trashBinRepository.findAllByLatitude(35.808057)).thenReturn(List.of(trashBin));
        when(trashBinImageRepository.findFirstByTrashBin_Id("trash-bin-1")).thenReturn(Optional.empty());
        when(imageUtil.toStoredFilename()).thenReturn("stored-1");
        when(imageCompressionUtil.compressImage(uploadedImage, "stored-1")).thenReturn(uploadRequestDto);
        doNothing().when(s3Util).uploadImage(uploadRequestDto);
        when(s3Util.buildS3Key("stored-1")).thenReturn("images/stored-1");
        when(s3Util.toS3UrlByS3Key("images/stored-1")).thenReturn("https://s3/images/stored-1");
        when(trashBinImageRepository.save(any(TrashBinImage.class))).thenAnswer(invocation -> {
            TrashBinImage saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "image-1");
            return saved;
        });

        assertThatThrownBy(() -> trashBinService.uploadTrashBinImages(List.of(uploadedImage, invalidImage)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("image filename must be latitude value");

        verify(s3Util).deleteImage("stored-1");
    }

    @Test
    @DisplayName("파일명 위도에 이미 저장된 이미지가 있으면 S3 업로드 없이 중복 응답을 반환한다")
    void uploadTrashBinImage_returnsAlreadyExistsWhenLatitudeImageExists() throws Exception {
        TrashBin trashBin = TrashBin.builder()
                .id("trash-bin-1")
                .latitude(35.808057)
                .longitude(128.508827)
                .build();
        TrashBinImage existingImage = TrashBinImage.builder()
                .originalName("35.808057.png")
                .storedName("stored-webp")
                .s3Key("images/stored-webp")
                .url("https://s3/images/stored-webp")
                .size(10L)
                .contentType("image/webp")
                .trashBin(trashBin)
                .build();
        ReflectionTestUtils.setField(existingImage, "id", "image-1");
        MockMultipartFile image = new MockMultipartFile("image", "35.808057.png", "image/png", "image".getBytes());

        when(trashBinRepository.findAllByLatitude(35.808057)).thenReturn(List.of(trashBin));
        when(trashBinImageRepository.findFirstByTrashBin_Id("trash-bin-1")).thenReturn(Optional.of(existingImage));

        UploadTrashBinImageResponseDto result = trashBinService.uploadTrashBinImage(image);

        verify(s3Util, never()).uploadImage(any());
        verify(trashBinImageRepository, never()).save(any());
        assertThat(result.alreadyExists()).isTrue();
        assertThat(result.message()).isEqualTo("Image already exists for latitude");
        assertThat(result.imageId()).isEqualTo("image-1");
    }

    @Test
    @DisplayName("이미지 업로드 요청이 null이면 예외가 발생한다")
    void uploadTrashBinImage_throwsWhenImageNull() {
        assertThatThrownBy(() -> trashBinService.uploadTrashBinImage(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("image is required");
    }

    @Test
    @DisplayName("이미지 업로드 요청이 비어 있으면 예외가 발생한다")
    void uploadTrashBinImage_throwsWhenImageEmpty() {
        MockMultipartFile image = new MockMultipartFile("image", "35.808057.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> trashBinService.uploadTrashBinImage(image))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("image is required");
    }

    @Test
    @DisplayName("이미지 파일명이 비어 있으면 예외가 발생한다")
    void uploadTrashBinImage_throwsWhenFilenameBlank() {
        MockMultipartFile image = new MockMultipartFile("image", "   ", "image/png", "image".getBytes());

        assertThatThrownBy(() -> trashBinService.uploadTrashBinImage(image))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("image filename is required");
    }

    @Test
    @DisplayName("이미지 파일명이 null이면 예외가 발생한다")
    void uploadTrashBinImage_throwsWhenFilenameNull() {
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(image.getOriginalFilename()).thenReturn(null);

        assertThatThrownBy(() -> trashBinService.uploadTrashBinImage(image))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("image filename is required");
    }

    @Test
    @DisplayName("확장자가 없는 이미지 파일명도 위도 값으로 파싱한다")
    void uploadTrashBinImage_parsesLatitudeWithoutExtension() throws Exception {
        TrashBin trashBin = TrashBin.builder()
                .id("trash-bin-1")
                .latitude(35.0)
                .longitude(128.508827)
                .build();
        MockMultipartFile image = new MockMultipartFile("image", "35", "image/png", "image".getBytes());
        TrashBinImage existingImage = TrashBinImage.builder()
                .storedName("stored-webp")
                .s3Key("images/stored-webp")
                .url("https://s3/images/stored-webp")
                .trashBin(trashBin)
                .build();
        ReflectionTestUtils.setField(existingImage, "id", "image-1");
        when(trashBinRepository.findAllByLatitude(35.0)).thenReturn(List.of(trashBin));
        when(trashBinImageRepository.findFirstByTrashBin_Id("trash-bin-1")).thenReturn(Optional.of(existingImage));

        UploadTrashBinImageResponseDto result = trashBinService.uploadTrashBinImage(image);

        assertThat(result.alreadyExists()).isTrue();
        assertThat(result.latitude()).isEqualTo(35.0);
    }

    @Test
    @DisplayName("파일명 위도와 일치하는 쓰레기통이 없으면 예외가 발생한다")
    void uploadTrashBinImage_throwsWhenTrashBinNotFoundByLatitude() {
        MockMultipartFile image = new MockMultipartFile("image", "35.808057.png", "image/png", "image".getBytes());
        when(trashBinRepository.findAllByLatitude(35.808057)).thenReturn(List.of());

        assertThatThrownBy(() -> trashBinService.uploadTrashBinImage(image))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No trash bin found by latitude");
    }

    @Test
    @DisplayName("S3 업로드 자체가 실패하면 S3 삭제 롤백을 호출하지 않는다")
    void uploadTrashBinImage_doesNotRollbackWhenS3UploadFails() throws Exception {
        TrashBin trashBin = TrashBin.builder()
                .id("trash-bin-1")
                .latitude(35.808057)
                .longitude(128.508827)
                .build();
        MockMultipartFile image = new MockMultipartFile("image", "35.808057.png", "image/png", "image".getBytes());
        S3ImageUploadRequestDto uploadRequestDto = S3ImageUploadRequestDto.builder()
                .storedFilename("stored-webp")
                .originalFilename("35.808057.png")
                .bytes("compressed".getBytes(StandardCharsets.UTF_8))
                .size(10L)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();
        when(trashBinRepository.findAllByLatitude(35.808057)).thenReturn(List.of(trashBin));
        when(trashBinImageRepository.findFirstByTrashBin_Id("trash-bin-1")).thenReturn(Optional.empty());
        when(imageUtil.toStoredFilename()).thenReturn("stored-webp");
        when(imageCompressionUtil.compressImage(image, "stored-webp")).thenReturn(uploadRequestDto);
        doThrow(new RuntimeException("s3 failed")).when(s3Util).uploadImage(uploadRequestDto);

        assertThatThrownBy(() -> trashBinService.uploadTrashBinImage(image))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("s3 failed");

        verify(s3Util, never()).deleteImage("stored-webp");
    }

    @Test
    @DisplayName("위도만으로 여러 쓰레기통이 조회되면 경도 입력을 요구한다")
    void uploadTrashBinImage_throwsWhenLatitudeIsAmbiguous() {
        MockMultipartFile image = new MockMultipartFile("image", "35.808057.png", "image/png", "image".getBytes());
        when(trashBinRepository.findAllByLatitude(35.808057)).thenReturn(List.of(
                TrashBin.builder().id("trash-bin-1").latitude(35.808057).build(),
                TrashBin.builder().id("trash-bin-2").latitude(35.808057).build()
        ));

        assertThatThrownBy(() -> trashBinService.uploadTrashBinImage(image))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Multiple trash bins found by latitude");
    }

    @Test
    @DisplayName("이미지 파일명이 위도 형식이 아니면 예외가 발생한다")
    void uploadTrashBinImage_throwsWhenFilenameIsNotLatitude() {
        MockMultipartFile image = new MockMultipartFile("image", "trash-bin.png", "image/png", "image".getBytes());

        assertThatThrownBy(() -> trashBinService.uploadTrashBinImage(image))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("image filename must be latitude value");
    }

    @Test
    @DisplayName("이미지 S3 업로드 후 DB 저장에 실패하면 업로드된 S3 파일을 삭제한다")
    void uploadTrashBinImage_rollsBackS3WhenDatabaseSaveFails() throws Exception {
        TrashBin trashBin = TrashBin.builder()
                .id("trash-bin-1")
                .latitude(35.808057)
                .longitude(128.508827)
                .build();
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "35.808057.png",
                "image/png",
                "image-bytes".getBytes(StandardCharsets.UTF_8)
        );
        S3ImageUploadRequestDto uploadRequestDto = S3ImageUploadRequestDto.builder()
                .storedFilename("stored-webp")
                .originalFilename("35.808057.png")
                .bytes("compressed".getBytes(StandardCharsets.UTF_8))
                .size(10L)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();

        when(trashBinRepository.findAllByLatitude(35.808057)).thenReturn(List.of(trashBin));
        when(trashBinImageRepository.findFirstByTrashBin_Id("trash-bin-1")).thenReturn(Optional.empty());
        when(imageUtil.toStoredFilename()).thenReturn("stored-webp");
        when(imageCompressionUtil.compressImage(image, "stored-webp")).thenReturn(uploadRequestDto);
        doNothing().when(s3Util).uploadImage(uploadRequestDto);
        when(s3Util.buildS3Key("stored-webp")).thenReturn("images/stored-webp");
        when(s3Util.toS3UrlByS3Key("images/stored-webp")).thenReturn("https://s3/images/stored-webp");
        doThrow(new RuntimeException("db save failed")).when(trashBinImageRepository).save(any(TrashBinImage.class));

        assertThatThrownBy(() -> trashBinService.uploadTrashBinImage(image))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db save failed");

        verify(s3Util).deleteImage("stored-webp");
    }

    @Test
    @DisplayName("S3 롤백 삭제가 실패해도 원래 예외를 유지한다")
    void uploadTrashBinImage_keepsOriginalExceptionWhenRollbackFails() throws Exception {
        TrashBin trashBin = TrashBin.builder()
                .id("trash-bin-1")
                .latitude(35.808057)
                .longitude(128.508827)
                .build();
        MockMultipartFile image = new MockMultipartFile("image", "35.808057.png", "image/png", "image".getBytes());
        S3ImageUploadRequestDto uploadRequestDto = S3ImageUploadRequestDto.builder()
                .storedFilename("stored-webp")
                .originalFilename("35.808057.png")
                .bytes("compressed".getBytes(StandardCharsets.UTF_8))
                .size(10L)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();
        when(trashBinRepository.findAllByLatitude(35.808057)).thenReturn(List.of(trashBin));
        when(trashBinImageRepository.findFirstByTrashBin_Id("trash-bin-1")).thenReturn(Optional.empty());
        when(imageUtil.toStoredFilename()).thenReturn("stored-webp");
        when(imageCompressionUtil.compressImage(image, "stored-webp")).thenReturn(uploadRequestDto);
        doNothing().when(s3Util).uploadImage(uploadRequestDto);
        when(s3Util.buildS3Key("stored-webp")).thenReturn("images/stored-webp");
        when(s3Util.toS3UrlByS3Key("images/stored-webp")).thenReturn("https://s3/images/stored-webp");
        doThrow(new RuntimeException("db save failed")).when(trashBinImageRepository).save(any(TrashBinImage.class));
        doThrow(new RuntimeException("rollback failed")).when(s3Util).deleteImage("stored-webp");

        assertThatThrownBy(() -> trashBinService.uploadTrashBinImage(image))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db save failed");
    }

    @Test
    @DisplayName("private 파서 유틸은 null, blank, 누락 값을 null로 처리한다")
    void privateParserUtilities_handleNullBlankAndMissingValues() {
        Double nullDouble = ReflectionTestUtils.invokeMethod(trashBinService, "parseDouble", (String) null);
        Double blankDouble = ReflectionTestUtils.invokeMethod(trashBinService, "parseDouble", "   ");
        Double commaOnlyDouble = ReflectionTestUtils.invokeMethod(trashBinService, "parseDouble", ",");
        LocalDate nullDate = ReflectionTestUtils.invokeMethod(trashBinService, "parseLocalDate", (String) null);
        LocalDate blankDate = ReflectionTestUtils.invokeMethod(trashBinService, "parseLocalDate", "   ");
        String missingHeaderValue = ReflectionTestUtils.invokeMethod(
                trashBinService,
                "getValue",
                List.of("A"),
                List.of("value"),
                "B"
        );
        String missingIndexValue = ReflectionTestUtils.invokeMethod(
                trashBinService,
                "getValue",
                List.of("A", "B"),
                List.of("value"),
                "B"
        );
        String blankValue = ReflectionTestUtils.invokeMethod(
                trashBinService,
                "getValue",
                List.of("A"),
                List.of("   "),
                "A"
        );
        String nullValue = ReflectionTestUtils.invokeMethod(
                trashBinService,
                "getValue",
                List.of("A"),
                Arrays.asList((String) null),
                "A"
        );
        String coordinateKeyWithNullLongitude = ReflectionTestUtils.invokeMethod(
                trashBinService,
                "toCoordinateKey",
                TrashBin.builder().latitude(35.8).longitude(null).build()
        );
        List<String> quotedValues = ReflectionTestUtils.invokeMethod(
                trashBinService,
                "parseCsvLine",
                "\"abc\",def"
        );
        List<String> quoteAtEndValues = ReflectionTestUtils.invokeMethod(
                trashBinService,
                "parseCsvLine",
                "\"abc\""
        );

        assertThat(nullDouble).isNull();
        assertThat(blankDouble).isNull();
        assertThat(commaOnlyDouble).isNull();
        assertThat(nullDate).isNull();
        assertThat(blankDate).isNull();
        assertThat(missingHeaderValue).isNull();
        assertThat(missingIndexValue).isNull();
        assertThat(blankValue).isNull();
        assertThat(nullValue).isNull();
        assertThat(coordinateKeyWithNullLongitude).isNull();
        assertThat(quotedValues).containsExactly("abc", "def");
        assertThat(quoteAtEndValues).containsExactly("abc");
    }

    @Test
    @DisplayName("CSV 파서는 헤더가 null이면 예외가 발생한다")
    void parseCsv_throwsWhenHeaderLineIsNull() {
        MockMultipartFile file = new MockMultipartFile("file", "trash-bins.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(trashBinService, "parseCsv", file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CSV header is empty");
    }

    @Test
    @DisplayName("복수 업로드 롤백은 중복 응답을 삭제하지 않는다")
    void rollbackUploadedImages_skipsAlreadyExistingImages() {
        UploadTrashBinImageResponseDto alreadyExists = UploadTrashBinImageResponseDto.builder()
                .alreadyExists(true)
                .storedName("stored-existing")
                .build();

        ReflectionTestUtils.invokeMethod(trashBinService, "rollbackUploadedImages", List.of(alreadyExists));

        verify(s3Util, never()).deleteImage("stored-existing");
    }
}
