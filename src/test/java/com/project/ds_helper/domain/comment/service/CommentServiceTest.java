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
import com.project.ds_helper.domain.comment.dto.response.UpdateCommentResponseDto;
import com.project.ds_helper.domain.comment.entity.Comment;
import com.project.ds_helper.domain.comment.repository.CommentRepository;
import com.project.ds_helper.domain.notification.service.NotificationFacade;
import com.project.ds_helper.domain.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserUtil userUtil;

    @Mock
    private NotificationFacade notificationFacade;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("Depth1 댓글 생성 시 댓글 저장과 알림 생성을 함께 수행한다")
    void createComment_savesDepth1CommentAndCreatesNotification() {
        User boardWriter = user("user-1", "게시글작성자");
        User commentWriter = user("user-2", "댓글작성자");
        Board board = board("board-1", false, boardWriter);
        CreateCommentRequestDto dto = createCommentRequest("board-1", null, "댓글 내용");

        when(userUtil.extractUserId(authentication)).thenReturn("user-2");
        when(userUtil.findUserById("user-2")).thenReturn(commentWriter);
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCommentResponseDto result = commentService.createComment(authentication, dto);

        verify(commentRepository).save(any(Comment.class));
        verify(boardRepository).increaseCommentCount("board-1");
        verify(notificationFacade).createCommentNotifications(any(Comment.class));
        assertThat(result.getContent()).isEqualTo("댓글 내용");
        assertThat(result.getWriterName()).isEqualTo("댓글작성자");
        assertThat(result.getParentId()).isNull();
    }

    @Test
    @DisplayName("Depth2 댓글 생성 시 부모 댓글과 게시글 관계를 검증한다")
    void createComment_savesDepth2Comment() {
        User user = user("user-1", "홍길동");
        Board board = board("board-1", false, user);
        Comment parent = comment("parent-1", board, user, null, "부모 댓글");
        CreateCommentRequestDto dto = createCommentRequest("board-1", "parent-1", "대댓글 내용");

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));
        when(commentRepository.findById("parent-1")).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCommentResponseDto result = commentService.createComment(authentication, dto);

        verify(commentRepository).save(any(Comment.class));
        verify(boardRepository).increaseCommentCount("board-1");
        verify(notificationFacade).createCommentNotifications(any(Comment.class));
        assertThat(result.getContent()).isEqualTo("대댓글 내용");
        assertThat(result.getWriterName()).isEqualTo("홍길동");
        assertThat(result.getParentId()).isEqualTo("parent-1");
    }

    @Test
    @DisplayName("대댓글의 부모 댓글 게시글이 다르면 예외가 발생한다")
    void createComment_throwsWhenParentBoardMismatch() {
        User user = user("user-1", "홍길동");
        Board board = board("board-1", false, user);
        Board otherBoard = board("board-2", false, user);
        Comment parent = comment("parent-1", otherBoard, user, null, "부모 댓글");
        CreateCommentRequestDto dto = createCommentRequest("board-1", "parent-1", "대댓글");

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));
        when(commentRepository.findById("parent-1")).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> commentService.createComment(authentication, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boardIdOfParentComment and boardIdOfNewComment Not Equal");
    }

    @Test
    @DisplayName("댓글 수정은 작성자의 댓글 내용만 변경한다")
    void updateComment_updatesContent() {
        User user = user("user-1", "홍길동");
        Board board = board("board-1", false, user);
        Comment comment = comment("comment-1", board, user, null, "기존 내용");
        UpdateCommentRequestDto dto = updateCommentRequest("comment-1", "수정 내용");

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(commentRepository.findByIdAndUser_Id("comment-1", "user-1")).thenReturn(Optional.of(comment));

        UpdateCommentResponseDto result = commentService.updateComment(authentication, dto);

        assertThat(result.getContent()).isEqualTo("수정 내용");
        assertThat(comment.getContent()).isEqualTo("수정 내용");
    }

    @Test
    @DisplayName("댓글 삭제 시 자식 댓글까지 soft delete 한다")
    void deleteComment_softDeletesChildren() {
        User user = user("user-1", "홍길동");
        Board board = board("board-1", false, user);
        Comment child = comment("child-1", board, user, null, "자식 댓글");
        Comment parent = comment("parent-1", board, user, null, "부모 댓글");
        parent.getChildren().add(child);
        DeleteCommentRequestDto dto = deleteCommentRequest("parent-1");

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(commentRepository.findWithChildren("parent-1", "user-1")).thenReturn(Optional.of(parent));

        commentService.deleteComment(authentication, dto);

        assertThat(parent.isDeleted()).isTrue();
        assertThat(child.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("관리자는 작성자가 아니어도 댓글을 삭제할 수 있다")
    void deleteComment_allowsAdmin() {
        User writer = user("user-1", "writer");
        User admin = user("admin-1", "admin");
        admin.setRole(com.project.ds_helper.domain.user.enums.UserRole.ADMIN);
        Board board = board("board-1", false, writer);
        Comment child = comment("child-1", board, writer, null, "child");
        Comment parent = comment("parent-1", board, writer, null, "parent");
        parent.getChildren().add(child);
        DeleteCommentRequestDto dto = deleteCommentRequest("parent-1");

        when(userUtil.extractUserId(authentication)).thenReturn("admin-1");
        when(userUtil.findUserById("admin-1")).thenReturn(admin);
        when(commentRepository.findWithChildren("parent-1")).thenReturn(Optional.of(parent));

        commentService.deleteComment(authentication, dto);

        assertThat(parent.isDeleted()).isTrue();
        assertThat(child.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("부모 댓글 조회는 커서와 hasNext를 계산한다")
    void getParentComments_returnsCursorResponse() {
        User user = user("user-1", "홍길동");
        Board board = board("board-1", false, user);
        Comment first = comment("comment-1", board, user, null, "첫 댓글");
        Comment second = comment("comment-2", board, user, null, "두 번째 댓글");
        ReflectionTestUtils.setField(first, "createdAt", LocalDateTime.of(2026, 3, 27, 9, 0, 0));
        ReflectionTestUtils.setField(second, "createdAt", LocalDateTime.of(2026, 3, 27, 10, 0, 0));

        when(commentRepository.findParentCommentsWithCursor(anyString(), any(), any(), any()))
                .thenReturn(new ArrayList<>(List.of(first, second)));

        CursorResponseDto<?> result = commentService.getParentComments("board-1", null, null, 1);

        assertThat(result.content()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.cursorId()).isEqualTo("comment-1");
    }

    @Test
    @DisplayName("대댓글 조회는 부모 댓글 존재 여부를 먼저 검증한다")
    void getChildComments_throwsWhenParentMissing() {
        when(commentRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> commentService.getChildComments("missing", null, null, 10))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Parent comment not found");
    }

    @Test
    @DisplayName("대댓글 조회는 DTO 목록과 커서를 반환한다")
    void getChildComments_returnsCursorResponse() {
        User user = user("user-1", "홍길동");
        Board board = board("board-1", false, user);
        Comment child = comment("child-1", board, user, null, "대댓글");
        ReflectionTestUtils.setField(child, "createdAt", LocalDateTime.of(2026, 3, 27, 11, 0, 0));

        when(commentRepository.existsById("parent-1")).thenReturn(true);
        when(commentRepository.findChildComments(anyString(), any(), any(), any()))
                .thenReturn(new ArrayList<>(List.of(child)));

        CursorResponseDto<GetChildCommentsResponseDto> result =
                commentService.getChildComments("parent-1", null, null, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.cursorId()).isEqualTo("child-1");
        assertThat(result.content().getFirst().userName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("조회 size는 1 이상이어야 한다")
    void getParentComments_throwsWhenSizeInvalid() {
        assertThatThrownBy(() -> commentService.getParentComments("board-1", null, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be positive");
    }

    @Test
    @DisplayName("cursorTime만 전달되면 예외가 발생한다")
    void getChildComments_throwsWhenCursorIdMissing() {
        when(commentRepository.existsById("parent-1")).thenReturn(true);

        assertThatThrownBy(() -> commentService.getChildComments("parent-1", LocalDateTime.now(), null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cursorTime and cursorId must be provided together");
    }

    @Test
    @DisplayName("cursorId만 전달되면 예외가 발생한다")
    void getParentComments_throwsWhenCursorTimeMissing() {
        assertThatThrownBy(() -> commentService.getParentComments("board-1", null, "comment-1", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cursorTime and cursorId must be provided together");
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
        return User.builder()
                .id(id)
                .name(name)
                .profileImageUrl("https://cdn.example.com/profile.png")
                .build();
    }

    private Board board(String id, boolean deleted, User user) {
        return Board.builder()
                .id(id)
                .user(user)
                .title("title")
                .content("content")
                .category("FREE")
                .isDeleted(deleted)
                .build();
    }

    private Comment comment(String id, Board board, User user, Comment parent, String content) {
        return Comment.builder()
                .id(id)
                .board(board)
                .user(user)
                .parent(parent)
                .content(content)
                .children(new ArrayList<>())
                .build();
    }
}
