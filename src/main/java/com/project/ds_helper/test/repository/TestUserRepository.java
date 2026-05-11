package com.project.ds_helper.test.repository;

import com.project.ds_helper.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


/**
 * 실제 Prod User Table 사용하므로 매우 주의 요망
 * **/

@Repository
public interface TestUserRepository extends JpaRepository<User, String> {

    Optional<User> findByRole(String admin);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
