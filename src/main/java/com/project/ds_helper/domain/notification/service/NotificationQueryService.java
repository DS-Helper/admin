package com.project.ds_helper.domain.notification.service;

import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.notification.dto.response.GetNotificationsResponseDto;
import com.project.ds_helper.domain.notification.dto.response.UnreadNotificationCountResponseDto;
import com.project.ds_helper.domain.notification.entity.Notification;
import com.project.ds_helper.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final UserUtil userUtil;

    @Transactional(readOnly = true)
    public CursorResponseDto<GetNotificationsResponseDto> getMyNotifications(Authentication authentication, LocalDateTime cursorTime, String cursorId, int size) {
        validateSize(size);
        validateCursor(cursorTime, cursorId);

        String userId = userUtil.extractUserId(authentication);
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Notification> notifications = notificationRepository.findNotificationsWithCursor(userId, cursorTime, cursorId, pageable);

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

        List<GetNotificationsResponseDto> content = notifications.stream().map(GetNotificationsResponseDto::toDto).toList();
        return CursorResponseDto.toDto(content, nextCursorTime, nextCursorId, hasNext);
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponseDto getUnreadNotificationCount(Authentication authentication) {
        String userId = userUtil.extractUserId(authentication);
        long unreadCount = notificationRepository.countByUser_IdAndIsReadFalse(userId);
        return UnreadNotificationCountResponseDto.toDto(unreadCount);
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
