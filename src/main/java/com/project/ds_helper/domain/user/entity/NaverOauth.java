package com.project.ds_helper.domain.user.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "tb_naver_oauth")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class NaverOauth extends BaseTime {

    @PrePersist
    private void perPersistGenerateId(){
        this.id = String.valueOf(UUID.randomUUID());
    }

    @Id
    @Column(name = "naver_oauth_id")
    private String id;

    @OneToOne(optional = false, cascade = {})
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "social_oauth_id")
    private String socialOauthId;

    @Column(name = "oauth_email")
    private String oauthEmail;
}