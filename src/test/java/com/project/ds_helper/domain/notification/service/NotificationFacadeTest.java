package com.project.ds_helper.domain.notification.service;

import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.comment.entity.Comment;
import com.project.ds_helper.domain.notification.entity.Notification;
import com.project.ds_helper.domain.notification.entity.NotificationDelivery;
import com.project.ds_helper.domain.notification.entity.PushToken;
import com.project.ds_helper.domain.notification.enums.NotificationType;
import com.project.ds_helper.domain.notification.enums.PushPlatform;
import com.project.ds_helper.domain.notification.enums.PushTokenDeviceType;
import com.project.ds_helper.domain.notification.repository.NotificationDeliveryRepository;
import com.project.ds_helper.domain.notification.repository.NotificationRepository;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationFacadeTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Mock
    private PushTokenService pushTokenService;

    @Mock
    private PushDispatchService pushDispatchService;

    @InjectMocks
    private NotificationFacade notificationFacade;

    @Test
    @DisplayName("댓글 알림 생성 시 인앱 알림 저장 후 활성 토큰으로 발송 이력을 저장한다")
    void createCommentNotifications_savesNotificationsAndDeliveries() {
        User boardWriter = user("board-user", "게시글작성자");
        User commentWriter = user("comment-user", "댓글작성자");
        Board board = board("board-1", boardWriter);
        Comment comment = comment("comment-1", board, commentWriter, null, "댓글");

        PushToken pushToken = PushToken.builder()
                .id("push-token-1")
                .user(boardWriter)
                .platform(PushPlatform.WEB)
                .deviceType(PushTokenDeviceType.CHROME)
                .token("token-value")
                .build();

        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pushTokenService.getActivePushTokensByUserIds(any())).thenReturn(List.of(pushToken));
        when(pushDispatchService.dispatch(any(Notification.class), any()))
                .thenAnswer(invocation -> List.of(
                        NotificationDelivery.builder()
                                .notification(invocation.getArgument(0))
                                .pushToken(pushToken)
                                .build()
                ));

        notificationFacade.createCommentNotifications(comment);

        ArgumentCaptor<List> notificationCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(notificationCaptor.capture());
        List<Notification> notifications = castNotifications(notificationCaptor.getValue());
        assertThat(notifications).hasSize(1);
        assertThat(notifications.getFirst().getUser().getId()).isEqualTo("board-user");
        assertThat(notifications.getFirst().getType()).isEqualTo(NotificationType.BOARD_COMMENT);

        verify(notificationDeliveryRepository).saveAll(any());
    }

    @Test
    @DisplayName("수신자가 본인이면 알림과 발송 이력을 만들지 않는다")
    void createCommentNotifications_skipsSelfNotification() {
        User writer = user("user-1", "본인");
        Board board = board("board-1", writer);
        Comment comment = comment("comment-1", board, writer, null, "댓글");

        notificationFacade.createCommentNotifications(comment);

        verify(notificationRepository, never()).saveAll(any());
        verify(notificationDeliveryRepository, never()).saveAll(any());
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
}
