package com.project.ds_helper.domain.trashbin.controller;

import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.domain.trashbin.dto.response.GetTrashBinsResponseDto;
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinImageResponseDto;
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinsResponseDto;
import com.project.ds_helper.domain.trashbin.service.TrashBinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/trash-bins")
@Tag(name = "공공 쓰레기통")
public class TrashBinController {

    private final TrashBinService trashBinService;

    @Operation(summary = "쓰레기통 CSV 업로드", description = "CSV 파일을 업로드하여 쓰레기통 데이터를 저장합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseVo<UploadTrashBinsResponseDto>> uploadTrashBins(
            @Parameter(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        log.debug("TrashBinController.uploadTrashBins called. originalFilename={}", file.getOriginalFilename());
        UploadTrashBinsResponseDto responseDto = trashBinService.uploadTrashBins(file);
        ResponseVo<UploadTrashBinsResponseDto> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), responseDto);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(summary = "쓰레기통 목록 조회", description = "저장된 쓰레기통 목록을 페이지 단위로 조회합니다.")
    @GetMapping
    public ResponseEntity<ResponseVo<GetTrashBinsResponseDto>> getTrashBins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        log.debug("TrashBinController.getTrashBins called. page={}, size={}, sort={}, sortBy={}",
                page, size, sort, sortBy);
        GetTrashBinsResponseDto responseDto = trashBinService.getTrashBins(page, size, sort, sortBy);
        ResponseVo<GetTrashBinsResponseDto> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), responseDto);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(
            summary = "쓰레기통 이미지 업로드",
            description = "이미지 파일명에서 위도를 파싱해 일치하는 쓰레기통 데이터를 찾고, 이미 저장된 이미지가 없을 때만 S3에 업로드합니다. 예: 35.808057.png"
    )
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseVo<UploadTrashBinImageResponseDto>> uploadTrashBinImage(
            @Parameter(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart("image") MultipartFile image
    ) throws IOException {
        log.debug("TrashBinController.uploadTrashBinImage called. originalFilename={}", image.getOriginalFilename());

        UploadTrashBinImageResponseDto responseDto = trashBinService.uploadTrashBinImage(image);
        ResponseVo<UploadTrashBinImageResponseDto> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), responseDto);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }
}
