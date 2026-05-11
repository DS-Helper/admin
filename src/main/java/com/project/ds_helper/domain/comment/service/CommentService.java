package com.project.ds_helper.domain.comment.service;

import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.board.repository.BoardRepository;
import com.project.ds_helper.domain.comment.dto.request.CreateCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.request.DeleteCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.request.UpdateCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.response.CreateCommentResponseDto;
import com.project.ds_helper.domain.comment.dto.response.GetChildCommentsResponseDto;
import com.project.ds_helper.domain.comment.dto.response.GetParentCommentsByBoardIdResponseDto;
import com.project.ds_helper.domain.comment.dto.response.UpdateCommentResponseDto;
import com.project.ds_helper.domain.comment.entity.Comment;
import com.project.ds_helper.domain.comment.repository.CommentRepository;
import com.project.ds_helper.domain.notification.service.NotificationFacade;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final UserUtil userUtil;
    private final NotificationFacade notificationFacade;

    @Transactional
    public CreateCommentResponseDto createComment(
            Authentication authentication,
            CreateCommentRequestDto dto
    ) {

        String boardId = dto.getBoardId();
        String parentId = dto.getParentId();
        String content = dto.getContent();
        log.debug("CommentService.createComment started. boardId={}, parentId={}, contentLength={}", boardId, parentId, content == null ? 0 : content.length());

        // 유저 조회
        User user = userUtil.findUserById(userUtil.extractUserId(authentication));

        // 게시글 조회
        Board board = findBoardById(boardId);
        checkIfDeletedBoard(board);

        // 부모 댓글 조회 (대댓글인 경우)
        Comment parent = null;
        if (parentId != null) {
            parent = findCommentById(parentId);
        }

        // Depth가 2를 초과할 경우 예외 던짐
        if(parent != null && parent.getParent() != null){
            throw new IllegalArgumentException("Depth of Comment Cannot Exceed 2");
        }
    
        // 부모 댓글의 게시글ID와 생성할 댓글의 게시글ID가 불일치 할 시 예외 던짐
        if (parent != null) {
            String boardIdOfParentComment = parent.getBoard().getId();
            String boardIdOfNewComment = boardId;
            if(!boardIdOfParentComment.equals(boardIdOfNewComment)){
                throw new IllegalArgumentException(String.format("boardIdOfParentComment and boardIdOfNewComment Not Equal." +
                        " boardIdOfParentComment : %s, boardIdOfNewComment : %s", boardIdOfParentComment, boardIdOfNewComment));
            }
        }

        // 댓글 생성
        Comment comment = Comment.builder()
                .board(board)
                .user(user)
                .parent(parent)
                .content(content)
                .build();

        commentRepository.save(comment);

        // 게시글 commentCount 증가
        boardRepository.increaseCommentCount(boardId);
        // 1. 댓글 저장이 완료된 뒤 인앱 알림 생성과 푸시 발송 오케스트레이션을 호출한다.
        notificationFacade.createCommentNotifications(comment);
        return CreateCommentResponseDto.toDto(comment, parent, user);
    }
    
    /**
     * 댓글 수정
     * **/
    @Transactional
    public UpdateCommentResponseDto updateComment(Authentication authentication,
                                                  UpdateCommentRequestDto dto) {

        String commentId = dto.getCommentId();
        String content = dto.getContent();
        log.debug("CommentService.updateComment started. commentId={}, contentLength={}", commentId, content == null ? 0 : content.length());

        String userId = userUtil.extractUserId(authentication);

        // 댓글 조회 및 작성자 여부 검증
        Comment comment = findCommentByCommentIdAndUserId(commentId, userId);

        // 기존 내용과 다를 경우에만 update, Dirty Checking 활용 (save() 필요 없음)
        if(!comment.getContent().equals(content)){
            comment.updateContent(dto.getContent());
        }

        return UpdateCommentResponseDto.toDto(comment);
    }

    @Transactional
    public void deleteComment(Authentication authentication, DeleteCommentRequestDto dto) {

        String commentId = dto.getCommentId();
        log.debug("CommentService.deleteComment started. commentId={}", commentId);

        String userId = userUtil.extractUserId(authentication);
        User user = userUtil.findUserById(userId);
        
        // 댓글 작성자 여부 검증 및 댓글(대댓글 포함) 조회
        Comment comment = findCommentWithChildren(commentId, userId, user);
    
        // 댓글 삭제
        comment.softDeleteWithChildren();
    }

    @Transactional(readOnly = true)
    public CursorResponseDto getParentComments(
            String boardId,
            LocalDateTime cursorTime,
            String cursorId,
            int size
    ) {
        // 페이징 size 검증
        validateSize(size);

        // 커서 검증
        validateCursor(cursorTime, cursorId);

        Pageable pageable = PageRequest.of(0, size + 1); // hasNext를 계산하기 위해 size + 1

        List<Comment> parentComments = commentRepository.findParentCommentsWithCursor(
                boardId,
                cursorTime,
                cursorId,
                pageable
        );

        // cursor 데이터 빌드
        String newCursorId = null;
        LocalDateTime newCursorTime = null;

        // hasNext 계산 후 마지막 1개 제거
        boolean hasNext = parentComments.size() > size;
        if (hasNext) {
            parentComments.remove(size);
        }
        
        // 마지막 커서 구하기
        if(!parentComments.isEmpty()){
            Comment lastParentComment = parentComments.getLast();

            newCursorId = lastParentComment.getId();
            newCursorTime = lastParentComment.getCreatedAt();
        }

        // comment용 dto 빌드
        List<GetParentCommentsByBoardIdResponseDto> content = parentComments
                .stream()
                .map(GetParentCommentsByBoardIdResponseDto::toDto)
                .toList();

        return CursorResponseDto.toDto(content,
                newCursorTime,
                newCursorId,
                hasNext);
    }

    /**
     * 대댓글 조회
     * **/
    public CursorResponseDto<GetChildCommentsResponseDto> getChildComments(
            String parentId,
            LocalDateTime cursorTime,
            String cursorId,
            int size
    ) {
        // 부모 댓글 유무 검증
        if (!commentRepository.existsById(parentId)) {
            throw new EntityNotFoundException("Parent comment not found");
        }

        // 페이징 size 검증
        validateSize(size);
            
        // 커서 검증
        validateCursor(cursorTime, cursorId);

        Pageable pageable = PageRequest.of(0, size + 1);

        List<Comment> comments =
                commentRepository.findChildComments(
                        parentId,
                        cursorTime,
                        cursorId,
                        pageable
                );

        boolean hasNext = comments.size() > size;
        if (hasNext) {
            comments.remove(size);
        }

        List<GetChildCommentsResponseDto> content =
                comments.stream()
                        .map(GetChildCommentsResponseDto::toDto)
                        .toList();

        LocalDateTime nextCursorTime = null;
        String nextCursorId = null;

        if (!comments.isEmpty()) {
            Comment last = comments.getLast();
            nextCursorTime = last.getCreatedAt();
            nextCursorId = last.getId();
        }

        return CursorResponseDto.toDto(
                content,
                nextCursorTime,
                nextCursorId,
                hasNext
        );
    }


    /**
     * Util Method
     * **/




    // 게시글 조회 (boardId 기반)
    private Board findBoardById(String boardId){
        return boardRepository.findById(boardId).orElseThrow(()-> new EntityNotFoundException(String.format("Board Not Found : %s", boardId)));
    }

    // 댓글 조회 (commentId 기반)
    private Comment findCommentById(String commentId){
        return commentRepository.findById(commentId).orElseThrow(()-> new EntityNotFoundException(String.format("Comment Not Found : %s", commentId)));
    }
    
    // 삭제된 게시글 여부 확인
    private void checkIfDeletedBoard(Board board){
        if(board.isDeleted()){
            throw new IllegalArgumentException(String.format("Board Is Deleted : %s", board.getId()));
        }
    }
    
    // 댓글의 작성자인지 확인 및 댓글 조회
    private Comment findCommentByCommentIdAndUserId(String commentId, String userId){
        return commentRepository
                .findByIdAndUser_Id(commentId, userId)
                .orElseThrow(() ->
                        new EntityNotFoundException(String.format("Comment Not Found or Not a Comment Writer. CommentId : %s, WriterId : %s", commentId, userId))
                );
    }

    // 댓글 작성자 여부 검증
    private void validateOwner(Comment comment, User user) {
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("No Authority To Delete Comment");
        }
    }

    private Comment findCommentWithChildren(String commentId, String userId, User user){
        if (user.getRole() == UserRole.ADMIN) {
            return commentRepository.findWithChildren(commentId)
                    .orElseThrow(() -> new EntityNotFoundException("Comment Not Found"));
        }

        return commentRepository.findWithChildren(commentId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Comment Not Found"));
    }

    private void validateCursor(LocalDateTime cursorTime, String cursorId){
        if ((cursorTime == null) != (cursorId == null)) {
            throw new IllegalArgumentException("cursorTime and cursorId must be provided together");
        }
    }

    private void validateSize(int size){
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
    }
}
