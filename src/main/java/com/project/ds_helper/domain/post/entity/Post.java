package com.project.ds_helper.domain.post.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "tb_post",
        indexes = {
                @Index(name = "idx_tb_post_view_count", columnList = "view_count"),
                @Index(name = "idx_tb_post_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Slf4j
public class Post extends BaseTime {

    @PrePersist
    private void perPersistGenerateId(){
        this.id = String.valueOf(UUID.randomUUID());
    }

    @Id
    @Column(name = "post_id")
    private String id;

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {})
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @Column(name = "view_count")
    private int viewCount = 0;

    @Builder.Default
    @OneToMany(mappedBy = "post", fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.ALL) // 연관관계 부모
    private List<PostImage> postImages = new ArrayList<>();

    public void addImage(PostImage postImage){
        log.info("addImage called: {}", postImage.getStoredName());
        postImages.add(postImage);
        postImage.setPost(this);
    }

    public void removeImage(PostImage postImage){
        postImages.remove(postImage);
        postImage.setPost(null);
    }



}
