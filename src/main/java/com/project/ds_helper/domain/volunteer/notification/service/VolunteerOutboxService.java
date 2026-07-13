package com.project.ds_helper.domain.volunteer.notification.service;

import com.project.ds_helper.domain.volunteer.common.enums.VolunteerOutboxEventType;
import com.project.ds_helper.domain.volunteer.notification.entity.VolunteerNotificationOutbox;
import com.project.ds_helper.domain.volunteer.notification.repository.VolunteerNotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VolunteerOutboxService {

    private final VolunteerNotificationOutboxRepository outboxRepository;

    public void enqueue(
            VolunteerOutboxEventType eventType,
            String eventKey,
            String receiverId,
            String aggregateId,
            String payload
    ) {
        outboxRepository.save(VolunteerNotificationOutbox.builder()
                .eventType(eventType)
                .eventKey(eventKey)
                .receiverId(receiverId)
                .aggregateId(aggregateId)
                .payload(payload)
                .build());
    }
}
