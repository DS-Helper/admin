package com.project.ds_helper.domain.volunteer.file.repository;

import com.project.ds_helper.domain.volunteer.file.entity.VolunteerFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VolunteerFileRepository extends JpaRepository<VolunteerFile, String> {
}
