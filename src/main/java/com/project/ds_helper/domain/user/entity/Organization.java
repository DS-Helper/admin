package com.project.ds_helper.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_organization")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Organization {

    @PrePersist
    private void prePersistGenerateId(){
        this.id = String.valueOf(UUID.randomUUID());
    }

    @Id
    @Column(name = "organization_id")
    private String id; // 식별자

    @Column(name = "name")
    private String organizationName;

    @Column(name = "phone_number")
    private String organizationPhoneNumber;

    @Column(name = "certification_url")
    private List<String> certificationUrl;

    @Column(name = "user_id")
    private String userId;
}
