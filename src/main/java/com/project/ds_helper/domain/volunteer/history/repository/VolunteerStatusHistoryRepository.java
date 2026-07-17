package com.project.ds_helper.domain.volunteer.history.repository;

import com.project.ds_helper.domain.volunteer.history.entity.VolunteerStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerHistoryTargetType;

public interface VolunteerStatusHistoryRepository extends JpaRepository<VolunteerStatusHistory, String> {
    List<VolunteerStatusHistory> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(VolunteerHistoryTargetType targetType, String targetId);
}
