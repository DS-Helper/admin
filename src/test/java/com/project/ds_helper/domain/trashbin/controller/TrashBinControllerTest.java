package com.project.ds_helper.domain.trashbin.controller;

import com.project.ds_helper.common.dto.response.PageResponseDto;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.domain.trashbin.dto.response.GetTrashBinsResponseDto;
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinImageResponseDto;
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinsResponseDto;
import com.project.ds_helper.domain.trashbin.service.TrashBinService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrashBinControllerTest {

    @Mock
    private TrashBinService trashBinService;

    @InjectMocks
    private TrashBinController trashBinController;

    @Test
    @DisplayName("CSV 업로드 요청 시 저장 결과를 반환한다")
    void uploadTrashBins_returnsSavedCount() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "trash-bins.csv", "text/csv", "csv".getBytes());
        when(trashBinService.uploadTrashBins(file)).thenReturn(new UploadTrashBinsResponseDto(3));

        ResponseEntity<ResponseVo<UploadTrashBinsResponseDto>> response = trashBinController.uploadTrashBins(file);

        verify(trashBinService).uploadTrashBins(file);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().savedCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("쓰레기통 목록 조회 요청 시 목록을 반환한다")
    void getTrashBins_returnsPagedList() {
        GetTrashBinsResponseDto.TrashBinItem item = GetTrashBinsResponseDto.TrashBinItem.builder()
                .id("trash-bin-1")
                .provinceName("대구광역시")
                .address("화원읍 비슬로 2679")
                .build();

        GetTrashBinsResponseDto responseDto = GetTrashBinsResponseDto.builder()
                .trashBins(List.of(item))
                .page(new PageResponseDto(0, 10, 1L, 1, true, true, false, false, null))
                .build();

        when(trashBinService.getTrashBins(0, 10, "desc", "createdAt")).thenReturn(responseDto);

        ResponseEntity<ResponseVo<GetTrashBinsResponseDto>> response =
                trashBinController.getTrashBins(0, 10, "desc", "createdAt");

        verify(trashBinService).getTrashBins(0, 10, "desc", "createdAt");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getTrashBins()).hasSize(1);
        assertThat(response.getBody().getData().getTrashBins().getFirst().getProvinceName()).isEqualTo("대구광역시");
    }

    @Test
    @DisplayName("쓰레기통 이미지 업로드 요청 시 업로드 결과를 반환한다")
    void uploadTrashBinImage_returnsUploadedImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "35.808057.png", "image/png", "image".getBytes());
        UploadTrashBinImageResponseDto responseDto = UploadTrashBinImageResponseDto.builder()
                .trashBinId("trash-bin-1")
                .latitude(35.808057)
                .longitude(128.508827)
                .alreadyExists(false)
                .message("Image uploaded successfully")
                .imageId("image-1")
                .imageUrl("https://s3/images/stored-webp")
                .contentType("image/webp")
                .size(10L)
                .build();
        when(trashBinService.uploadTrashBinImage(image)).thenReturn(responseDto);

        ResponseEntity<ResponseVo<UploadTrashBinImageResponseDto>> response =
                trashBinController.uploadTrashBinImage(image);

        verify(trashBinService).uploadTrashBinImage(image);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().imageId()).isEqualTo("image-1");
        assertThat(response.getBody().getData().alreadyExists()).isFalse();
        assertThat(response.getBody().getData().contentType()).isEqualTo("image/webp");
    }
}
