package com.project.ds_helper.domain.board.controller;

import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.board.dto.request.CreateBoardReqDto;
import com.project.ds_helper.domain.board.dto.request.UpdateBoardRequestDto;
import com.project.ds_helper.domain.board.dto.response.CreateBoardResponseDto;
import com.project.ds_helper.domain.board.dto.response.GetBoardByIdResponseDto;
import com.project.ds_helper.domain.board.dto.response.GetBoardsByCategoryResponseDto;
import com.project.ds_helper.domain.board.dto.response.GetMyBoardsResponseDto;
import com.project.ds_helper.domain.board.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping()
@Tag(name = SwaggerTagName.COMMUNITY_BOARD)
public class BoardController {

    private final BoardService boardService;

    @Operation(summary = "게시글 목록 조회", description = "카테고리, 제목 검색어, 페이지, 정렬 조건을 기준으로 게시글 목록을 조회합니다.")
    @GetMapping(value = "/boards")
    public ResponseEntity<ResponseVo<GetBoardsByCategoryResponseDto>> getBoardsByCategory(
            Authentication authentication,
            @RequestParam(defaultValue = "전체", required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int size,
            @RequestParam(defaultValue = "desc", required = false) String sort,
            @RequestParam(defaultValue = "createdAt", required = false) String sortBy
    ) {
        log.debug("BoardController.getBoardsByCategory called. category={}, keyword={}, page={}, size={}, sort={}, sortBy={}",
                category, keyword, page, size, sort, sortBy);
        GetBoardsByCategoryResponseDto boards =
                boardService.getBoardsByCategory(authentication, category, keyword, page, size, sort, sortBy);
        ResponseVo<GetBoardsByCategoryResponseDto> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), boards);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(summary = "게시글 단건 조회", description = "게시글 ID를 기준으로 게시글 상세 정보를 조회합니다.")
    @GetMapping(value = "/board/{boardId}")
    public ResponseEntity<ResponseVo<GetBoardByIdResponseDto>> getBoardById(
            Authentication authentication,
            @PathVariable("boardId") String boardId
    ) {
        log.debug("BoardController.getBoardById called. boardId={}", boardId);
        GetBoardByIdResponseDto board = boardService.getBoardById(authentication, boardId);
        ResponseVo<GetBoardByIdResponseDto> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), board);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(summary = "내가 작성한 게시글 조회 (JWT 인증 필요)", description = "내가 작성한 게시글을 커서 기반으로 무한 스크롤 조회합니다.")
    @GetMapping(value = "/boards/me")
    public ResponseEntity<ResponseVo<CursorResponseDto<GetMyBoardsResponseDto>>> getMyBoards(
            Authentication authentication,
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            @RequestParam(name = "cursor-time", required = false) LocalDateTime cursorTime,
            @RequestParam(name = "cursor-id", required = false) String cursorId,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        log.debug("BoardController.getMyBoards called. cursorTime={}, cursorId={}, size={}", cursorTime, cursorId, size);
        CursorResponseDto<GetMyBoardsResponseDto> responseDto =
                boardService.getMyBoards(authentication, cursorTime, cursorId, size);
        ResponseVo<CursorResponseDto<GetMyBoardsResponseDto>> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), responseDto);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(summary = "게시글 생성 (JWT 인증 필요)", description = "게시글 정보와 첨부 이미지를 받아 새 게시글을 생성합니다.")
    @PostMapping(value = "/boards", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseVo<CreateBoardResponseDto>> createBoard(
            Authentication authentication,
            @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            @RequestPart(value = "dto") @Valid CreateBoardReqDto dto,
            @Parameter(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))
                    )
            )
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        log.debug("BoardController.createBoard called. title={}, category={}, imageCount={}",
                dto.getTitle(), dto.getCategory(), images == null ? 0 : images.size());
        if (images == null || images.isEmpty()) {
            images = new ArrayList<>();
        }
        CreateBoardResponseDto createdBoard = boardService.createBoard(authentication, dto, images);
        ResponseVo<CreateBoardResponseDto> responseVo =
                new ResponseVo<>(true, SuccessCode.CREATED, SuccessCode.CREATED.getMessage(), createdBoard);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(summary = "게시글 수정 (JWT 인증 필요)", description = "게시글 정보와 첨부 이미지를 수정합니다.")
    @PatchMapping(value = "/boards")
    public ResponseEntity<ResponseVo<Void>> updateBoard(
            Authentication authentication,
            @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            @RequestPart(value = "dto") @Valid UpdateBoardRequestDto dto,
            @Parameter(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        log.debug("BoardController.updateBoard called. boardId={}, keepImageCount={}, newImageCount={}",
                dto.getBoardId(),
                dto.getKeepImageUrls() == null ? 0 : dto.getKeepImageUrls().size(),
                images == null ? 0 : images.size());
        if (images == null || images.isEmpty()) {
            images = new ArrayList<>();
        }
        boardService.updateBoard(authentication, dto, images);
        ResponseVo<Void> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), null);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(summary = "게시글 삭제 (JWT 인증 필요)", description = "게시글 ID를 기준으로 게시글을 삭제합니다.")
    @DeleteMapping("/board/{boardId}")
    public ResponseEntity<ResponseVo<Void>> deleteBoard(
            Authentication authentication,
            @PathVariable("boardId") String boardId
    ) {
        log.debug("BoardController.deleteBoard called. boardId={}", boardId);
        boardService.deleteBoard(authentication, boardId);
        ResponseVo<Void> responseVo =
                new ResponseVo<>(true, SuccessCode.NO_CONTENT, SuccessCode.NO_CONTENT.getMessage(), null);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }
}

