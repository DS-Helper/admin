package com.project.ds_helper.domain.trashbin.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "tb_trash_bin_image")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrashBinImage extends BaseTime {

    @PrePersist
    private void prePersistGenerateId() {
        this.id = String.valueOf(UUID.randomUUID());
    }

    /**
     * 쓰레기통 이미지 식별자.
     */
    @Id
    @Column(name = "trash_bin_image_id")
    private String id;

    /**
     * 사용자가 업로드한 원본 파일명.
     */
    @Column(name = "original_name")
    private String originalName;

    /**
     * S3 저장 시 충돌 방지를 위해 생성한 파일명.
     */
    @Column(name = "stored_name")
    private String storedName;

    /**
     * S3 bucket 내부 object key.
     */
    @Column(name = "s3_key")
    private String s3Key;

    /**
     * 프론트에서 이미지를 표시할 때 사용하는 공개 URL.
     */
    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    /**
     * 압축 후 S3에 저장된 파일 크기.
     */
    @Column(name = "size")
    private Long size;

    /**
     * 압축 후 S3에 저장된 content type.
     */
    @Column(name = "content_type")
    private String contentType;

    /**
     * 이 이미지가 연결된 쓰레기통 데이터.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trash_bin_id")
    private TrashBin trashBin;
}
