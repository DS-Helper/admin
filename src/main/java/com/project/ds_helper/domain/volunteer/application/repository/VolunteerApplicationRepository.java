package com.project.ds_helper.domain.volunteer.application.repository;

import com.project.ds_helper.domain.volunteer.application.entity.VolunteerApplication;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerApplicationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VolunteerApplicationRepository extends JpaRepository<VolunteerApplication, String> {

    @EntityGraph(attributePaths = {"photoFile", "preferredActivities", "user"})
    Optional<VolunteerApplication> findFirstByUser_IdOrderByCreatedAtDesc(String userId);

    @EntityGraph(attributePaths = {"photoFile", "preferredActivities", "user"})
    Optional<VolunteerApplication> findByIdAndUser_Id(String applicationId, String userId);

    boolean existsByUser_IdAndStatus(String userId, VolunteerApplicationStatus status);
}
