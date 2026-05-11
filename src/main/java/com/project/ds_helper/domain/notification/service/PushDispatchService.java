package com.project.ds_helper.domain.notification.service;

import com.project.ds_helper.domain.notification.entity.Notification;
import com.project.ds_helper.domain.notification.entity.NotificationDelivery;
import com.project.ds_helper.domain.notification.entity.PushToken;

import java.util.List;

public interface PushDispatchService {

    List<NotificationDelivery> dispatch(Notification notification, List<PushToken> pushTokens);
}
