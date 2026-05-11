package com.project.ds_helper.domain.inquiry.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.inquiry.enums.InquiryStatus;
import com.project.ds_helper.domain.inquiry.enums.InquiryType;
import com.project.ds_helper.domain.reply.entity.Reply;
import com.project.ds_helper.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_inquiry")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Inquiry extends BaseTime {

    @PrePersist
    private void perPersistGenerateId(){
        this.id = String.valueOf(UUID.randomUUID());
    }

    @Id
    @Column(name = "inquiry_id")
    private String id;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // 내용

    @Builder.Default
    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InquiryImage> images = new ArrayList<>();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private InquiryType type = InquiryType.OTHER; // 유형

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private InquiryStatus status = InquiryStatus.UNANSWERED; // 답변 상태

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {})
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 작성한 유저

    @OneToOne(fetch = FetchType.LAZY, optional = true, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "reply_id")
    private Reply reply; // 답변

    public void addReply(Reply reply){
        if(reply.getInquiry() != null && !reply.getInquiry().equals(this)){return;} // 다른 문의의 답변이면 바로 리턴
        if(this.reply != null){ // 이미 응답이 된 문의면 바로 리턴
            this.reply = null;
        }
        this.reply = reply;
        reply.setInquiry(this);
    }
}
