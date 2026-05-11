package com.project.ds_helper.domain.board.entity;

import com.project.ds_helper.domain.base.entity.CreateBaseTime;
import com.project.ds_helper.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "tb_board_scrap",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"board_id", "user_id"}
        )
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardScrap extends CreateBaseTime {

    @PrePersist
    private void prePersistGenerateId(){
        this.id = String.valueOf(UUID.randomUUID());
    }

    @Id
    @Column(name = "board_scrap_id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
