package com.project.ds_helper.domain.trashbin;

import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinImageResponseDto;
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinImagesResponseDto;
import com.project.ds_helper.domain.trashbin.entity.TrashBin;
import com.project.ds_helper.domain.trashbin.entity.TrashBinImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrashBinDtoEntityTest {

    @Test
    @DisplayName("TrashBin 계열 엔티티는 PrePersist에서 ID를 생성한다")
    void trashBinEntities_generateIds() {
        TrashBin trashBin = TrashBin.builder().build();
        TrashBinImage trashBinImage = TrashBinImage.builder().build();

        ReflectionTestUtils.invokeMethod(trashBin, "prePersistGenerateId");
        ReflectionTestUtils.invokeMethod(trashBinImage, "prePersistGenerateId");

        assertThat(trashBin.getId()).isNotBlank();
        assertThat(trashBinImage.getId()).isNotBlank();
    }

    @Test
    @DisplayName("복수 업로드 응답은 신규 업로드와 중복 개수를 계산한다")
    void uploadTrashBinImagesResponseDto_countsResultTypes() {
        UploadTrashBinImageResponseDto uploaded = UploadTrashBinImageResponseDto.builder()
                .alreadyExists(false)
                .build();
        UploadTrashBinImageResponseDto alreadyExists = UploadTrashBinImageResponseDto.builder()
                .alreadyExists(true)
                .build();

        UploadTrashBinImagesResponseDto result = UploadTrashBinImagesResponseDto.from(List.of(uploaded, alreadyExists));

        assertThat(result.requestedCount()).isEqualTo(2);
        assertThat(result.uploadedCount()).isEqualTo(1);
        assertThat(result.alreadyExistsCount()).isEqualTo(1);
    }
}
