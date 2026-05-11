package com.project.ds_helper.domain.admin.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tb_admin")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Admin extends BaseTime {

    @PrePersist
    private void prePersistGenerateId(){
        this.id = String.valueOf(UUID.randomUUID());
    }

    @Id
    @Column(name = "admin_id")
    private String id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;
}
