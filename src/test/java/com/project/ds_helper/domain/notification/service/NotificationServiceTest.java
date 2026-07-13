package com.project.ds_helper.domain.notification.service;

import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.comment.entity.Comment;
import com.project.ds_helper.domain.notification.dto.response.GetNotificationsResponseDto;
import com.project.ds_helper.domain.notification.dto.response.UnreadNotificationCountResponseDto;
import com.project.ds_helper.domain.notification.entity.Notification;
import com.project.ds_helper.domain.notification.enums.NotificationType;
import com.project.ds_helper.domain.notification.repository.NotificationRepository;
import com.project.ds_helper.domain.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserUtil userUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Depth1 댓글이 작성되면 게시글 작성자에게 게시글 댓글 알림을 생성한다")
    void createNotificationsForComment_createsBoardNotificationForDepth1() {
        User boardWriter = user("board-user", "게시글작성자");
        User commentWriter = user("comment-user", "댓글작성자");
        Board board = board("board-1", boardWriter);
        Comment comment = comment("comment-1", board, commentWriter, null, "댓글");

        notificationService.createNotificationsForComment(comment);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = castNotifications(captor.getValue());
        assertThat(notifications).hasSize(1);
        assertThat(notifications.getFirst().getUser().getId()).isEqualTo("board-user");
        assertThat(notifications.getFirst().getType()).isEqualTo(NotificationType.BOARD_COMMENT);
        assertThat(notifications.getFirst().getBoardId()).isEqualTo("board-1");
        assertThat(notifications.getFirst().getCommentId()).isEqualTo("comment-1");
        assertThat(notifications.getFirst().getContent()).isEqualTo("내 게시글에 댓글이 작성되었습니다.");
        assertThat(notifications.getFirst().isRead()).isFalse();
    }

    @Test
    @DisplayName("게시글 작성자가 직접 댓글을 작성하면 게시글 알림을 생성하지 않는다")
    void createNotificationsForComment_skipsBoardNotificationForBoardWriter() {
        User boardWriter = user("board-user", "게시글작성자");
        Board board = board("board-1", boardWriter);
        Comment comment = comment("comment-1", board, boardWriter, null, "댓글");

        notificationService.createNotificationsForComment(comment);

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Depth2 댓글이 작성되면 게시글 작성자와 부모 댓글 작성자에게 각각 알림을 생성한다")
    void createNotificationsForComment_createsBoardAndParentNotificationsForDepth2() {
        User boardWriter = user("board-user", "게시글작성자");
        User parentWriter = user("parent-user", "부모댓글작성자");
        User replyWriter = user("reply-user", "대댓글작성자");
        Board board = board("board-1", boardWriter);
        Comment parent = comment("parent-1", board, parentWriter, null, "부모 댓글");
        Comment reply = comment("reply-1", board, replyWriter, parent, "대댓글");

        notificationService.createNotificationsForComment(reply);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = castNotifications(captor.getValue());
        assertThat(notifications).hasSize(2);
        assertThat(notifications)
                .extracting(Notification::getType)
                .containsExactlyInAnyOrder(NotificationType.COMMENT_REPLY, NotificationType.BOARD_COMMENT);
        assertThat(notifications)
                .extracting(Notification::getBoardId)
                .containsOnly("board-1");
        assertThat(notifications)
                .extracting(Notification::getCommentId)
                .containsExactlyInAnyOrder("parent-1", "reply-1");
    }

    @Test
    @DisplayName("게시글 작성자와 부모 댓글 작성자가 같으면 중복 알림은 한 번만 생성한다")
    void createNotificationsForComment_deduplicatesSameRecipient() {
        User receiver = user("receiver-user", "수신자");
        User replyWriter = user("reply-user", "대댓글작성자");
        Board board = board("board-1", receiver);
        Comment parent = comment("parent-1", board, receiver, null, "부모 댓글");
        Comment reply = comment("reply-1", board, replyWriter, parent, "대댓글");

        notificationService.createNotificationsForComment(reply);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = castNotifications(captor.getValue());
        assertThat(notifications).hasSize(1);
        assertThat(notifications.getFirst().getType()).isEqualTo(NotificationType.COMMENT_REPLY);
        assertThat(notifications.getFirst().getCommentId()).isEqualTo("parent-1");
    }

    @Test
    @DisplayName("내 알림 목록은 커서 기준 최신순 응답을 반환한다")
    void getMyNotifications_returnsCursorResponse() {
        Notification first = notification("notification-1", user("user-1", "홍길동"),
                NotificationType.BOARD_COMMENT, "board-1", "comment-1", "첫 알림", false);
        Notification second = notification("notification-2", user("user-1", "홍길동"),
                NotificationType.COMMENT_REPLY, "board-1", "comment-2", "두 번째 알림", false);
        ReflectionTestUtils.setField(first, "createdAt", LocalDateTime.of(2026, 4, 3, 10, 0, 0));
        ReflectionTestUtils.setField(second, "createdAt", LocalDateTime.of(2026, 4, 3, 11, 0, 0));

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(notificationRepository.findNotificationsWithCursor(anyString(), isNull(), isNull(), any()))
                .thenReturn(new ArrayList<>(List.of(first, second)));

        CursorResponseDto<GetNotificationsResponseDto> result =
                notificationService.getMyNotifications(authentication, null, null, 1);

        assertThat(result.content()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.cursorId()).isEqualTo("notification-1");
        assertThat(result.content().getFirst().type()).isEqualTo(NotificationType.BOARD_COMMENT);
        assertThat(result.content().getFirst().boardId()).isEqualTo("board-1");
    }

    @Test
    @DisplayName("알림 읽음 처리 시 본인 알림이면 읽음 상태로 변경한다")
    void markAsRead_marksNotificationAsRead() {
        Notification notification = notification("notification-1", user("user-1", "홍길동"),
                NotificationType.BOARD_COMMENT, "board-1", "comment-1", "알림", false);

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(notificationRepository.findByIdAndUser_Id("notification-1", "user-1"))
                .thenReturn(Optional.of(notification));

        notificationService.markAsRead(authentication, "notification-1");

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 알림 읽음 처리 시 예외가 발생한다")
    void markAsRead_throwsWhenNotificationMissing() {
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(notificationRepository.findByIdAndUser_Id("notification-1", "user-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(authentication, "notification-1"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Notification Not Found");
    }

    @Test
    @DisplayName("전체 알림 읽음 처리는 로그인 사용자 기준으로 수행한다")
    void markAllAsRead_marksAllNotificationsAsRead() {
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");

        notificationService.markAllAsRead(authentication);

        verify(notificationRepository).markAllAsReadByUserId("user-1");
    }

    @Test
    @DisplayName("미읽음 알림 개수를 반환한다")
    void getUnreadNotificationCount_returnsUnreadCount() {
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(notificationRepository.countByUser_IdAndIsReadFalse("user-1")).thenReturn(3L);

        UnreadNotificationCountResponseDto result = notificationService.getUnreadNotificationCount(authentication);

        assertThat(result.unreadCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("커서 조회는 서비스 위임 경로가 없어도 동작한다")
    void getMyNotifications_usesFallbackServicePath() {
        NotificationService service = new NotificationService(notificationRepository, userUtil, null, null);
        Notification first = notification("notification-1", user("user-1", "홍길동"),
                NotificationType.BOARD_COMMENT, "board-1", "comment-1", "첫 알림", false);
        ReflectionTestUtils.setField(first, "createdAt", LocalDateTime.of(2026, 4, 3, 10, 0, 0));

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(notificationRepository.findNotificationsWithCursor(anyString(), isNull(), isNull(), any()))
                .thenReturn(new ArrayList<>(List.of(first)));

        CursorResponseDto<GetNotificationsResponseDto> result =
                service.getMyNotifications(authentication, null, null, 1);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("읽음 처리는 위임 서비스가 없어도 동작한다")
    void markAsRead_usesFallbackServicePath() {
        NotificationService service = new NotificationService(notificationRepository, userUtil, null, null);
        Notification notification = notification("notification-1", user("user-1", "홍길동"),
                NotificationType.BOARD_COMMENT, "board-1", "comment-1", "알림", false);

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(notificationRepository.findByIdAndUser_Id("notification-1", "user-1"))
                .thenReturn(Optional.of(notification));

        service.markAsRead(authentication, "notification-1");

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    @DisplayName("cursorId만 전달되면 예외가 발생한다")
    void getMyNotifications_throwsWhenCursorTimeMissing() {
        assertThatThrownBy(() -> notificationService.getMyNotifications(authentication, null, "notification-1", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cursorTime and cursorId must be provided together");
    }

    @Test
    @DisplayName("size는 1 이상이어야 한다")
    void getMyNotifications_throwsWhenSizeInvalid() {
        assertThatThrownBy(() -> notificationService.getMyNotifications(authentication, null, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be positive");
    }

    @SuppressWarnings("unchecked")
    private List<Notification> castNotifications(List captured) {
        return (List<Notification>) captured;
    }

    private User user(String id, String name) {
        return User.builder().id(id).name(name).build();
    }

    private Board board(String id, User user) {
        return Board.builder()
                .id(id)
                .user(user)
                .title("title")
                .content("content")
                .category("FREE")
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

    private Notification notification(
            String id,
            User user,
            NotificationType type,
            String boardId,
            String commentId,
            String content,
            boolean isRead
    ) {
        return Notification.builder()
                .id(id)
                .user(user)
                .type(type)
                .boardId(boardId)
                .commentId(commentId)
                .content(content)
                .isRead(isRead)
                .build();
    }
}
