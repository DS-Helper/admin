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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String BOARD_COMMENT_NOTIFICATION_CONTENT = "내 게시글에 댓글이 작성되었습니다.";
    private static final String BOARD_REPLY_NOTIFICATION_CONTENT = "내 게시글에 대댓글이 작성되었습니다.";
    private static final String COMMENT_REPLY_NOTIFICATION_CONTENT = "내 댓글에 대댓글이 작성되었습니다.";

    private final NotificationRepository notificationRepository;
    private final UserUtil userUtil;

    @Transactional
    public void createNotificationsForComment(Comment comment) {
        User commentWriter = comment.getUser();
        Board board = comment.getBoard();
        Comment parentComment = comment.getParent();

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

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    @Transactional(readOnly = true)
    public CursorResponseDto<GetNotificationsResponseDto> getMyNotifications(
            Authentication authentication,
            LocalDateTime cursorTime,
            String cursorId,
            int size
    ) {
        validateSize(size);
        validateCursor(cursorTime, cursorId);

        String userId = userUtil.extractUserId(authentication);
        Pageable pageable = PageRequest.of(0, size + 1);

        List<Notification> notifications = notificationRepository.findNotificationsWithCursor(
                userId,
                cursorTime,
                cursorId,
                pageable
        );

        boolean hasNext = notifications.size() > size;
        if (hasNext) {
            notifications.remove(size);
        }

        LocalDateTime nextCursorTime = null;
        String nextCursorId = null;
        if (!notifications.isEmpty()) {
            Notification lastNotification = notifications.getLast();
            nextCursorTime = lastNotification.getCreatedAt();
            nextCursorId = lastNotification.getId();
        }

        List<GetNotificationsResponseDto> content = notifications.stream()
                .map(GetNotificationsResponseDto::toDto)
                .toList();

        return CursorResponseDto.toDto(content, nextCursorTime, nextCursorId, hasNext);
    }

    @Transactional
    public void markAsRead(Authentication authentication, String notificationId) {
        String userId = userUtil.extractUserId(authentication);
        Notification notification = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification Not Found"));
        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Authentication authentication) {
        String userId = userUtil.extractUserId(authentication);
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponseDto getUnreadNotificationCount(Authentication authentication) {
        String userId = userUtil.extractUserId(authentication);
        long unreadCount = notificationRepository.countByUser_IdAndIsReadFalse(userId);
        return UnreadNotificationCountResponseDto.toDto(unreadCount);
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
        if (recipient == null || commentWriter == null) {
            return;
        }

        if (recipient.getId().equals(commentWriter.getId())) {
            return;
        }

        if (!recipientIds.add(recipient.getId())) {
            return;
        }

        notifications.add(Notification.builder()
                .user(recipient)
                .type(type)
                .boardId(boardId)
                .commentId(commentId)
                .content(content)
                .build());
    }

    private void validateCursor(LocalDateTime cursorTime, String cursorId) {
        if ((cursorTime == null) != (cursorId == null)) {
            throw new IllegalArgumentException("cursorTime and cursorId must be provided together");
        }
    }

    private void validateSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
    }
}
