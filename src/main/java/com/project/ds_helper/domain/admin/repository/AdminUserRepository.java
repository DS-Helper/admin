package com.project.ds_helper.domain.admin.repository;

import com.project.ds_helper.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminUserRepository extends JpaRepository<User, String> {

    int countByRole(String role);
}
