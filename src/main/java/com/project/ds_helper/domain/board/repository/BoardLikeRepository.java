package com.project.ds_helper.domain.board.repository;

import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.board.entity.BoardLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardLikeRepository extends JpaRepository<BoardLike, String> {
    boolean existsByUser_IdAndBoard_Id(String userId, String boardId);

    Optional<BoardLike> findByUser_IdAndBoard_Id(String userId, String boardId);

    int countByBoard_Id(String boardId);

    @Query("""
        SELECT bl.board
        FROM BoardLike bl
        WHERE bl.user.id = :userId
        AND bl.board.isDeleted = false
        """)
    Page<Board> findLikedBoards(
            @Param("userId") String userId,
            Pageable pageable
    );

    @Query("""
        SELECT bl.board.id
        FROM BoardLike bl
        WHERE bl.user.id = :userId
        AND bl.board.id IN :boardIds
        """)
    List<String> findLikedBoardIdsByUserIdAndBoardIds(
            @Param("userId") String userId,
            @Param("boardIds") List<String> boardIds
    );
}
