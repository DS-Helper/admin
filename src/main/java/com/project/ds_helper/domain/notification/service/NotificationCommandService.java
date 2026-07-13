package com.project.ds_helper.domain.notification.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.notification.entity.Notification;
import com.project.ds_helper.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final UserUtil userUtil;

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
}
