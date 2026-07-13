package com.project.ds_helper.domain.comment.service;

import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.domain.comment.dto.response.GetChildCommentsResponseDto;
import com.project.ds_helper.domain.comment.dto.response.GetParentCommentsByBoardIdResponseDto;
import com.project.ds_helper.domain.comment.entity.Comment;
import com.project.ds_helper.domain.comment.repository.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentQueryService {

    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public CursorResponseDto<GetParentCommentsByBoardIdResponseDto> getParentComments(String boardId, LocalDateTime cursorTime, String cursorId, int size) {
        validateSize(size);
        validateCursor(cursorTime, cursorId);
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Comment> parentComments = commentRepository.findParentCommentsWithCursor(boardId, cursorTime, cursorId, pageable);
        boolean hasNext = parentComments.size() > size;
        if (hasNext) parentComments.remove(size);
        String newCursorId = null;
        LocalDateTime newCursorTime = null;
        if (!parentComments.isEmpty()) {
            Comment last = parentComments.getLast();
            newCursorId = last.getId();
            newCursorTime = last.getCreatedAt();
        }
        List<GetParentCommentsByBoardIdResponseDto> content = parentComments.stream().map(GetParentCommentsByBoardIdResponseDto::toDto).toList();
        return CursorResponseDto.toDto(content, newCursorTime, newCursorId, hasNext);
    }

    @Transactional(readOnly = true)
    public CursorResponseDto<GetChildCommentsResponseDto> getChildComments(String parentId, LocalDateTime cursorTime, String cursorId, int size) {
        if (!commentRepository.existsById(parentId)) throw new EntityNotFoundException("Parent comment not found");
        validateSize(size);
        validateCursor(cursorTime, cursorId);
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Comment> comments = commentRepository.findChildComments(parentId, cursorTime, cursorId, pageable);
        boolean hasNext = comments.size() > size;
        if (hasNext) comments.remove(size);
        List<GetChildCommentsResponseDto> content = comments.stream().map(GetChildCommentsResponseDto::toDto).toList();
        LocalDateTime nextCursorTime = null;
        String nextCursorId = null;
        if (!comments.isEmpty()) {
            Comment last = comments.getLast();
            nextCursorTime = last.getCreatedAt();
            nextCursorId = last.getId();
        }
        return CursorResponseDto.toDto(content, nextCursorTime, nextCursorId, hasNext);
    }

    private void validateCursor(LocalDateTime cursorTime, String cursorId) {
        if ((cursorTime == null) != (cursorId == null)) throw new IllegalArgumentException("cursorTime and cursorId must be provided together");
    }

    private void validateSize(int size) {
        if (size <= 0) throw new IllegalArgumentException("size must be positive");
    }
}
