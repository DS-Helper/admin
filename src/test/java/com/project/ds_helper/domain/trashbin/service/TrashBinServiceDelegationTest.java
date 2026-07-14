package com.project.ds_helper.domain.trashbin.service;

import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.ImageUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinImageResponseDto;
import com.project.ds_helper.domain.trashbin.entity.TrashBin;
import com.project.ds_helper.domain.trashbin.repository.TrashBinImageRepository;
import com.project.ds_helper.domain.trashbin.repository.TrashBinRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrashBinServiceDelegationTest {

    @Mock private TrashBinRepository trashBinRepository;
    @Mock private TrashBinImageRepository trashBinImageRepository;
    @Mock private ImageUtil imageUtil;
    @Mock private ImageCompressionUtil imageCompressionUtil;
    @Mock private S3Util s3Util;
    @Mock private TrashBinCsvService trashBinCsvService;
    @Mock private TrashBinImageService trashBinImageService;

    @Test
    void delegatesCsvAndImageUploadsToDedicatedServices() throws Exception {
        TrashBinService service = new TrashBinService(
                trashBinRepository,
                trashBinImageRepository,
                imageUtil,
                imageCompressionUtil,
                s3Util,
                trashBinCsvService,
                trashBinImageService
        );
        MockMultipartFile csv = new MockMultipartFile("file", "bins.csv", "text/csv", "data".getBytes());
        MockMultipartFile image = new MockMultipartFile("image", "35.png", "image/png", "image".getBytes());
        List<TrashBin> bins = List.of(TrashBin.builder().build());
        UploadTrashBinImageResponseDto uploaded = org.mockito.Mockito.mock(UploadTrashBinImageResponseDto.class);
        when(uploaded.alreadyExists()).thenReturn(false);
        var uploadBatch = com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinImagesResponseDto.from(List.of(uploaded));

        when(trashBinCsvService.parseCsv(csv)).thenReturn(bins);
        doNothing().when(trashBinCsvService).validateNoDuplicateCoordinates(bins);
        when(trashBinImageService.uploadTrashBinImage(image)).thenReturn(uploaded);
        when(trashBinImageService.uploadTrashBinImages(List.of(image))).thenReturn(uploadBatch);

        assertThat(service.uploadTrashBins(csv).savedCount()).isEqualTo(1);
        assertThat(service.uploadTrashBinImage(image)).isSameAs(uploaded);
        assertThat(service.uploadTrashBinImages(List.of(image)).uploadedCount()).isEqualTo(1);

        verify(trashBinRepository).saveAll(bins);
        verify(trashBinImageService).uploadTrashBinImage(image);
        verify(trashBinImageService).uploadTrashBinImages(List.of(image));
    }
}
