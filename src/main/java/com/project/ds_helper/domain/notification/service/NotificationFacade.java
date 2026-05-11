package com.project.ds_helper.domain.notification.service;

import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.comment.entity.Comment;
import com.project.ds_helper.domain.notification.entity.Notification;
import com.project.ds_helper.domain.notification.entity.NotificationDelivery;
import com.project.ds_helper.domain.notification.entity.PushToken;
import com.project.ds_helper.domain.notification.enums.NotificationType;
import com.project.ds_helper.domain.notification.repository.NotificationDeliveryRepository;
import com.project.ds_helper.domain.notification.repository.NotificationRepository;
import com.project.ds_helper.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationFacade {

    private static final String BOARD_COMMENT_NOTIFICATION_CONTENT = "내 게시글에 댓글이 작성되었습니다.";
    private static final String BOARD_REPLY_NOTIFICATION_CONTENT = "내 게시글에 답글이 작성되었습니다.";
    private static final String COMMENT_REPLY_NOTIFICATION_CONTENT = "내 댓글에 답글이 작성되었습니다.";

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final PushTokenService pushTokenService;
    private final PushDispatchService pushDispatchService;

    @Transactional
    public void createCommentNotifications(Comment comment) {
        // 1. 댓글 작성 이벤트에서 알림 수신 대상과 알림 유형을 계산한다.
        User commentWriter = comment.getUser();
        Board board = comment.getBoard();
        Comment parentComment = comment.getParent();
        log.debug("NotificationFacade.createCommentNotifications started. commentId={}, boardId={}, parentCommentId={}",
                comment.getId(), board.getId(), parentComment == null ? null : parentComment.getId());

        List<Notification> notifications = new ArrayList<>();
        Set<String> recipientIds = new HashSet<>();

        if (parentComment != null) {
            addNotificationIfNeeded(
                    notifications,
                    recipientIds,
                    parentComment.getUser(),
                    commentWriter,
                    NotificationType.COMMENT_REPLY,
                    board.getId(),
                    parentComment.getId(),
                    COMMENT_REPLY_NOTIFICATION_CONTENT
            );
        }

        addNotificationIfNeeded(
                notifications,
                recipientIds,
                board.getUser(),
                commentWriter,
                NotificationType.BOARD_COMMENT,
                board.getId(),
                comment.getId(),
                parentComment == null ? BOARD_COMMENT_NOTIFICATION_CONTENT : BOARD_REPLY_NOTIFICATION_CONTENT
        );

        if (notifications.isEmpty()) {
            log.debug("NotificationFacade.createCommentNotifications completed with no recipients");
            return;
        }

        // 2. 인앱 알림함에 저장할 Notification 엔티티를 먼저 영속화한다.
        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
        log.debug("NotificationFacade.createCommentNotifications saved notifications. count={}", savedNotifications.size());

        // 3. 수신자의 활성 푸시 토큰을 조회해 시스템 푸시 발송 대상을 결정한다.
        List<PushToken> activePushTokens = pushTokenService.getActivePushTokensByUserIds(recipientIds);
        if (activePushTokens.isEmpty()) {
            log.debug("NotificationFacade.createCommentNotifications completed without active push tokens");
            return;
        }

        // 4. 저장된 각 알림에 대해 푸시 발송을 위임하고, 발송 이력을 별도 테이블에 저장한다.
        List<NotificationDelivery> deliveries = new ArrayList<>();
        for (Notification savedNotification : savedNotifications) {
            List<PushToken> targetPushTokens = activePushTokens.stream()
                    .filter(pushToken -> pushToken.getUser().getId().equals(savedNotification.getUser().getId()))
                    .toList();

            deliveries.addAll(pushDispatchService.dispatch(savedNotification, targetPushTokens));
        }

        if (!deliveries.isEmpty()) {
            notificationDeliveryRepository.saveAll(deliveries);
            log.debug("NotificationFacade.createCommentNotifications saved deliveries. count={}", deliveries.size());
        }

        log.debug("NotificationFacade.createCommentNotifications completed");
    }

    private void addNotificationIfNeeded(
            List<Notification> notifications,
            Set<String> recipientIds,
            User recipient,
            User commentWriter,
            NotificationType type,
            String boardId,
            String commentId,
            String content
    ) {
        // 1. 수신자 또는 작성자 정보가 비정상이면 알림 생성 자체를 건너뛴다.
        if (recipient == null || commentWriter == null) {
            log.debug("NotificationFacade.addNotificationIfNeeded skipped. recipient or commentWriter is null");
            return;
        }

        // 2. 본인이 쓴 댓글/답글에는 자기 자신에게 알림을 만들지 않는다.
        if (recipient.getId().equals(commentWriter.getId())) {
            log.debug("NotificationFacade.addNotificationIfNeeded skipped. self notification. recipientId={}", recipient.getId());
            return;
        }

        // 3. 동일 이벤트에서 같은 사용자가 중복 수신자가 되면 한 번만 생성한다.
        if (!recipientIds.add(recipient.getId())) {
            log.debug("NotificationFacade.addNotificationIfNeeded skipped. duplicated recipientId={}", recipient.getId());
            return;
        }

        // 4. 인앱 알림함에 들어갈 Notification 엔티티를 메모리에서 먼저 구성한다.
        notifications.add(Notification.builder()
                .user(recipient)
                .type(type)
                .boardId(boardId)
                .commentId(commentId)
                .content(content)
                .build());
        log.debug("NotificationFacade.addNotificationIfNeeded added notification. recipientId={}, type={}", recipient.getId(), type);
    }
}
