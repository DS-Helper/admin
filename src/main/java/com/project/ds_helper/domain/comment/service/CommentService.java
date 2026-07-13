package com.project.ds_helper.domain.comment.service;

import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.domain.comment.dto.request.CreateCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.request.DeleteCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.request.UpdateCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.response.CreateCommentResponseDto;
import com.project.ds_helper.domain.comment.dto.response.GetChildCommentsResponseDto;
import com.project.ds_helper.domain.comment.dto.response.GetParentCommentsByBoardIdResponseDto;
import com.project.ds_helper.domain.comment.dto.response.UpdateCommentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentQueryService commentQueryService;
    private final CommentCommandService commentCommandService;

    public CreateCommentResponseDto createComment(Authentication authentication, CreateCommentRequestDto dto) {
        return commentCommandService.createComment(authentication, dto);
    }

    public UpdateCommentResponseDto updateComment(Authentication authentication, UpdateCommentRequestDto dto) {
        return commentCommandService.updateComment(authentication, dto);
    }

    public void deleteComment(Authentication authentication, DeleteCommentRequestDto dto) {
        commentCommandService.deleteComment(authentication, dto);
    }

    public CursorResponseDto<GetParentCommentsByBoardIdResponseDto> getParentComments(String boardId, LocalDateTime cursorTime, String cursorId, int size) {
        return commentQueryService.getParentComments(boardId, cursorTime, cursorId, size);
    }

    public CursorResponseDto<GetChildCommentsResponseDto> getChildComments(String parentId, LocalDateTime cursorTime, String cursorId, int size) {
        return commentQueryService.getChildComments(parentId, cursorTime, cursorId, size);
    }
}
