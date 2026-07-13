package com.project.ds_helper.domain.volunteer.history.repository;

import com.project.ds_helper.domain.volunteer.history.entity.VolunteerStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VolunteerStatusHistoryRepository extends JpaRepository<VolunteerStatusHistory, String> {
}
