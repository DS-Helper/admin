package com.project.ds_helper.domain.board.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Builder
@Setter
@AllArgsConstructor
@Table(name = "tb_board")
@Slf4j
public class Board extends BaseTime {

    @PrePersist
    private void prePersistGenerateId(){
        this.id = String.valueOf(UUID.randomUUID());
    }

    @Id
    @Column(name = "board_id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "category")
    private String category;

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(name = "like_count")
    private int likeCount = 0;


    @Builder.Default
    @Column(name = "view_count")
    private int viewCount = 0;

    @Builder.Default
    @Column(name = "comment_count")
    private int commentCount = 0;

    @Builder.Default
    @OneToMany(mappedBy = "board", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL) // 연관관계 부모
    private List<BoardImage> boardImages = new ArrayList<>();

    @Builder.Default
    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    public void increaseLikeCount() {this.likeCount++;}

    public void decreaseLikeCount() {this.likeCount--;}

    public void increaseViewCount() {this.viewCount++;}

    public void decreaseViewCount() {this.viewCount--;}

    public void increaseCommentCount() {this.commentCount++;}

    public void decreaseCommentCount() {this.commentCount--;}

    public void addImage(BoardImage boardImage){
        log.info("addImage called: {}", boardImage.getStoredName());
        boardImages.add(boardImage);
        boardImage.setBoard(this);
    }

    public void removeImage(BoardImage boardImage){
        boardImages.remove(boardImage);
        boardImage.setBoard(null);
    }

    public void update(String title, String content){
        this.title = title;
        this.content = content;
    }

    public void softDelete(){
        this.isDeleted = true;
    }
}

/**
 Board와 Comment는 OrphanRemoval이나 Cascade 관게에 있지 않다.(직접 제어)

 Board 삭제 시 BoardImage 삭제 필수
* */

