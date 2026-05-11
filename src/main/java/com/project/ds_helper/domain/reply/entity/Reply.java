package com.project.ds_helper.domain.reply.entity;

import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tb_reply")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Setter
public class Reply {

    @PrePersist
    private void perPersistGenerateId(){
        this.id = String.valueOf(UUID.randomUUID());
    }

    @Id
    @Column(name = "reply_id")
    private String id;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {})
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 관리자

    @OneToOne(mappedBy = "reply", fetch = FetchType.LAZY, optional = true, cascade = {}, orphanRemoval = false)
    @JoinColumn(name = "inquiry_id")
    private Inquiry inquiry; // 문의
}
