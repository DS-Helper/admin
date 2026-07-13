package com.project.ds_helper.domain.comment.service;

import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.board.repository.BoardRepository;
import com.project.ds_helper.domain.comment.dto.request.CreateCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.request.DeleteCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.request.UpdateCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.response.CreateCommentResponseDto;
import com.project.ds_helper.domain.comment.dto.response.UpdateCommentResponseDto;
import com.project.ds_helper.domain.comment.entity.Comment;
import com.project.ds_helper.domain.comment.repository.CommentRepository;
import com.project.ds_helper.domain.notification.service.NotificationFacade;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentCommandService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final com.project.ds_helper.common.util.UserUtil userUtil;
    private final NotificationFacade notificationFacade;

    @Transactional
    public CreateCommentResponseDto createComment(Authentication authentication, CreateCommentRequestDto dto) {
        User user = userUtil.findUserById(userUtil.extractUserId(authentication));
        Board board = findBoardById(dto.getBoardId());
        checkIfDeletedBoard(board);
        Comment parent = dto.getParentId() == null ? null : findCommentById(dto.getParentId());
        if (parent != null && parent.getParent() != null) throw new IllegalArgumentException("Depth of Comment Cannot Exceed 2");
        if (parent != null && !parent.getBoard().getId().equals(dto.getBoardId())) {
            throw new IllegalArgumentException(String.format("boardIdOfParentComment and boardIdOfNewComment Not Equal. boardIdOfParentComment : %s, boardIdOfNewComment : %s", parent.getBoard().getId(), dto.getBoardId()));
        }
        Comment comment = Comment.builder().board(board).user(user).parent(parent).content(dto.getContent()).build();
        commentRepository.save(comment);
        boardRepository.increaseCommentCount(dto.getBoardId());
        notificationFacade.createCommentNotifications(comment);
        return CreateCommentResponseDto.toDto(comment, parent, user);
    }

    @Transactional
    public UpdateCommentResponseDto updateComment(Authentication authentication, UpdateCommentRequestDto dto) {
        String userId = userUtil.extractUserId(authentication);
        Comment comment = commentRepository.findByIdAndUser_Id(dto.getCommentId(), userId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Comment Not Found or Not a Comment Writer. CommentId : %s, WriterId : %s", dto.getCommentId(), userId)));
        if (!comment.getContent().equals(dto.getContent())) comment.updateContent(dto.getContent());
        return UpdateCommentResponseDto.toDto(comment);
    }

    @Transactional
    public void deleteComment(Authentication authentication, DeleteCommentRequestDto dto) {
        String userId = userUtil.extractUserId(authentication);
        User user = userUtil.findUserById(userId);
        Comment comment = findCommentWithChildren(dto.getCommentId(), userId, user);
        comment.softDeleteWithChildren();
    }

    private Board findBoardById(String boardId) {
        return boardRepository.findById(boardId).orElseThrow(() -> new EntityNotFoundException(String.format("Board Not Found : %s", boardId)));
    }

    private Comment findCommentById(String commentId) {
        return commentRepository.findById(commentId).orElseThrow(() -> new EntityNotFoundException(String.format("Comment Not Found : %s", commentId)));
    }

    private void checkIfDeletedBoard(Board board) {
        if (board.isDeleted()) throw new IllegalArgumentException(String.format("Board Is Deleted : %s", board.getId()));
    }

    private Comment findCommentWithChildren(String commentId, String userId, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return commentRepository.findWithChildren(commentId).orElseThrow(() -> new EntityNotFoundException("Comment Not Found"));
        }
        return commentRepository.findWithChildren(commentId, userId).orElseThrow(() -> new EntityNotFoundException("Comment Not Found"));
    }
}
