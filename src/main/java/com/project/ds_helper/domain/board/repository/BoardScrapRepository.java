package com.project.ds_helper.domain.board.repository;

import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.board.entity.BoardScrap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardScrapRepository extends JpaRepository<BoardScrap, String> {
    boolean existsByUser_IdAndBoard_Id(String userId, String boardId);

    Optional<BoardScrap> findByUser_IdAndBoard_Id(String userId, String boardId);

    long countByBoard_Id(String boardId);

    @Query("""
        SELECT bs.board
        FROM BoardScrap bs
        WHERE bs.user.id = :userId
        AND bs.board.isDeleted = false
        """)
    Page<Board> findScrappedBoards(
            @Param("userId") String userId,
            Pageable pageable
    );
}
