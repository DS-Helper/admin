package com.project.ds_helper.domain.comment.controller;

import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.comment.dto.request.CreateCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.request.DeleteCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.request.UpdateCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.response.CreateCommentResponseDto;
import com.project.ds_helper.domain.comment.dto.response.GetChildCommentsResponseDto;
import com.project.ds_helper.domain.comment.dto.response.GetParentCommentsByBoardIdResponseDto;
import com.project.ds_helper.domain.comment.dto.response.UpdateCommentResponseDto;
import com.project.ds_helper.domain.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = SwaggerTagName.COMMUNITY_COMMENT)
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "게시글 부모 댓글 목록 조회")
    @GetMapping("/comments/{boardId}/comments")
    public ResponseEntity<ResponseVo<CursorResponseDto<GetParentCommentsByBoardIdResponseDto>>> getParentComments(
            @PathVariable String boardId,
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") @RequestParam(name = "cursor-time", required = false) LocalDateTime cursorTime,
            @Parameter(description = "커서용 ID. Comment Entity의 commentId와 동일한 UUID 문자열입니다.", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
            @RequestParam(name = "cursor-id", required = false) String cursorId,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("CommentController.getParentComments called. boardId={}, cursorTime={}, cursorId={}, size={}", boardId, cursorTime, cursorId, size);
        CursorResponseDto<GetParentCommentsByBoardIdResponseDto> responseDto =
                commentService.getParentComments(boardId, cursorTime, cursorId, size);
        ResponseVo<CursorResponseDto<GetParentCommentsByBoardIdResponseDto>> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), responseDto);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(summary = "부모 댓글의 대댓글 목록 조회")
    @GetMapping("/comments/{parentId}/children")
    public ResponseEntity<ResponseVo<CursorResponseDto<GetChildCommentsResponseDto>>> getChildComments(
            @PathVariable String parentId,
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") @RequestParam(name = "cursor-time", required = false) LocalDateTime cursorTime,
            @Parameter(description = "커서용 ID. Comment Entity의 commentId와 동일한 UUID 문자열입니다.", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
            @RequestParam(name = "cursor-id", required = false) String cursorId,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size
    ) {
        log.debug("CommentController.getChildComments called. parentId={}, cursorTime={}, cursorId={}, size={}", parentId, cursorTime, cursorId, size);
        CursorResponseDto<GetChildCommentsResponseDto> responseDto =
                commentService.getChildComments(parentId, cursorTime, cursorId, size);
        ResponseVo<CursorResponseDto<GetChildCommentsResponseDto>> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), responseDto);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(summary = "댓글 생성 (JWT 인증 필요)")
    @PostMapping("/comment")
    public ResponseEntity<ResponseVo<CreateCommentResponseDto>> createComment(
            Authentication authentication,
            @Valid @RequestBody CreateCommentRequestDto requestDto
    ) {
        log.debug("CommentController.createComment called. boardId={}, parentId={}", requestDto.getBoardId(), requestDto.getParentId());
        CreateCommentResponseDto responseDto = commentService.createComment(authentication, requestDto);
        ResponseVo<CreateCommentResponseDto> responseVo =
                new ResponseVo<>(true, SuccessCode.CREATED, SuccessCode.CREATED.getMessage(), responseDto);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(summary = "댓글 수정 (JWT 인증 필요)")
    @PatchMapping("/comment")
    public ResponseEntity<ResponseVo<UpdateCommentResponseDto>> updateComment(
            Authentication authentication,
            @Valid @RequestBody UpdateCommentRequestDto requestDto
    ) {
        log.debug("CommentController.updateComment called. commentId={}", requestDto.getCommentId());
        UpdateCommentResponseDto responseDto = commentService.updateComment(authentication, requestDto);
        ResponseVo<UpdateCommentResponseDto> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), responseDto);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(summary = "댓글 삭제 (JWT 인증 필요)")
    @DeleteMapping("/comment")
    public ResponseEntity<ResponseVo<Void>> deleteComment(
            Authentication authentication,
            @Valid @RequestBody DeleteCommentRequestDto dto
    ) {
        log.debug("CommentController.deleteComment called. commentId={}", dto.getCommentId());
        commentService.deleteComment(authentication, dto);
        ResponseVo<Void> responseVo =
                new ResponseVo<>(true, SuccessCode.NO_CONTENT, SuccessCode.NO_CONTENT.getMessage(), null);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }
}

