package com.project.ds_helper.domain.post.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tb_post_image")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class PostImage extends BaseTime {

    @PrePersist
    public void generatedId(){
        this.id = String.valueOf(UUID.randomUUID());
    }

    /**
     *  식별자 직접 할당
     * **/
    @Id
    @Column(name = "post_image_id")
    private String id;

    @Column(name = "original_name")
    private String originalName; // 원본 파일명

    @Column(name = "stored_name")
    private String storedName; // 저장 파일명 = s3Key

    @Column(name = "url")
    private String url; // S3 경로

    @Column(name = "size")
    private Long size; // 사이즈

    @Column(name = "content_type")
    private String contentType; // 확장자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;
    
    // setPost 메소드 (FK 세팅) 는 @Setter에 의해 자동 생성
    // 컬렉션 관리는 Post에서
}

