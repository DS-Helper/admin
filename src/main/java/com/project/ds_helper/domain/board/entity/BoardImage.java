package com.project.ds_helper.domain.board.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tb_board_image")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class BoardImage extends BaseTime {

    @PrePersist
    public void generatedId(){
        this.id = String.valueOf(UUID.randomUUID());
    }

    /**
     *  식별자 직접 할당
     * **/
    @Id
    @Column(name = "board_image_id")
    private String id;

    @Column(name = "original_name")
    private String originalName; // 원본 파일명

    @Column(name = "stored_name")
    private String storedName; // 저장 파일명 = s3Key

    @Column(name = "s3_key")
    private String s3Key; // S3 경로

    @Column(name = "size")
    private Long size; // 사이즈

    @Column(name = "content_type")
    private String contentType; // 확장자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    // setPost 메소드 (FK 세팅) 는 @Setter에 의해 자동 생성
    // 컬렉션 관리는 Post에서

}
