package com.project.ds_helper.domain.volunteer.history.service;

import com.project.ds_helper.domain.volunteer.common.enums.VolunteerHistoryTargetType;
import com.project.ds_helper.domain.volunteer.history.entity.VolunteerStatusHistory;
import com.project.ds_helper.domain.volunteer.history.repository.VolunteerStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VolunteerHistoryService {

    private final VolunteerStatusHistoryRepository historyRepository;

    public void record(
            VolunteerHistoryTargetType targetType,
            String targetId,
            String previousStatus,
            String nextStatus,
            String changedBy,
            String reason
    ) {
        historyRepository.save(VolunteerStatusHistory.builder()
                .targetType(targetType)
                .targetId(targetId)
                .previousStatus(previousStatus)
                .nextStatus(nextStatus)
                .changedBy(changedBy)
                .changeReason(reason)
                .build());
    }
}
