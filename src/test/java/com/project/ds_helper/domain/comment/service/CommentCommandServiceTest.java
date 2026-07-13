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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentCommandServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private com.project.ds_helper.common.util.UserUtil userUtil;
    @Mock private NotificationFacade notificationFacade;
    @Mock private Authentication authentication;

    @InjectMocks
    private CommentCommandService commentCommandService;

    @Test
    @DisplayName("댓글 생성 시 저장과 알림을 수행한다")
    void createComment_savesComment() {
        User writer = user("user-2", "댓글작성자");
        Board board = board("board-1", false, user("user-1", "게시글작성자"));
        CreateCommentRequestDto dto = createCommentRequest("board-1", null, "댓글 내용");
        when(userUtil.extractUserId(authentication)).thenReturn("user-2");
        when(userUtil.findUserById("user-2")).thenReturn(writer);
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCommentResponseDto result = commentCommandService.createComment(authentication, dto);

        assertThat(result.getContent()).isEqualTo("댓글 내용");
        verify(notificationFacade).createCommentNotifications(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 수정은 작성자의 댓글만 변경한다")
    void updateComment_updatesContent() {
        User user = user("user-1", "홍길동");
        Board board = board("board-1", false, user);
        Comment comment = comment("comment-1", board, user, null, "기존 내용");
        UpdateCommentRequestDto dto = updateCommentRequest("comment-1", "수정 내용");
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(commentRepository.findByIdAndUser_Id("comment-1", "user-1")).thenReturn(Optional.of(comment));

        UpdateCommentResponseDto result = commentCommandService.updateComment(authentication, dto);

        assertThat(result.getContent()).isEqualTo("수정 내용");
    }

    @Test
    @DisplayName("관리자는 댓글을 삭제할 수 있다")
    void deleteComment_allowsAdmin() {
        User admin = user("admin-1", "admin");
        admin.setRole(UserRole.ADMIN);
        Board board = board("board-1", false, user("user-1", "writer"));
        Comment parent = comment("parent-1", board, admin, null, "parent");
        Comment child = comment("child-1", board, admin, null, "child");
        parent.getChildren().add(child);
        DeleteCommentRequestDto dto = deleteCommentRequest("parent-1");
        when(userUtil.extractUserId(authentication)).thenReturn("admin-1");
        when(userUtil.findUserById("admin-1")).thenReturn(admin);
        when(commentRepository.findWithChildren("parent-1")).thenReturn(Optional.of(parent));

        commentCommandService.deleteComment(authentication, dto);

        assertThat(parent.isDeleted()).isTrue();
        assertThat(child.isDeleted()).isTrue();
    }

    private CreateCommentRequestDto createCommentRequest(String boardId, String parentId, String content) {
        CreateCommentRequestDto dto = new CreateCommentRequestDto();
        ReflectionTestUtils.setField(dto, "boardId", boardId);
        ReflectionTestUtils.setField(dto, "parentId", parentId);
        ReflectionTestUtils.setField(dto, "content", content);
        return dto;
    }

    private UpdateCommentRequestDto updateCommentRequest(String commentId, String content) {
        UpdateCommentRequestDto dto = new UpdateCommentRequestDto();
        ReflectionTestUtils.setField(dto, "commentId", commentId);
        ReflectionTestUtils.setField(dto, "content", content);
        return dto;
    }

    private DeleteCommentRequestDto deleteCommentRequest(String commentId) {
        DeleteCommentRequestDto dto = new DeleteCommentRequestDto();
        ReflectionTestUtils.setField(dto, "commentId", commentId);
        return dto;
    }

    private User user(String id, String name) {
        return User.builder().id(id).name(name).profileImageUrl("https://cdn.example.com/profile.png").build();
    }

    private Board board(String id, boolean deleted, User user) {
        return Board.builder().id(id).user(user).title("title").content("content").category("FREE").isDeleted(deleted).build();
    }

    private Comment comment(String id, Board board, User user, Comment parent, String content) {
        return Comment.builder().id(id).board(board).user(user).parent(parent).content(content).children(new ArrayList<>()).build();
    }
}
