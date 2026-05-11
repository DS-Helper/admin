package com.project.ds_helper.domain.board.repository;

import com.project.ds_helper.domain.board.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, String> {
    Page<Board> findByIsDeletedFalse(Pageable pageable);

    Page<Board> findByCategoryAndIsDeletedFalse(String category, Pageable pageable);

    Page<Board> findByTitleContainingAndIsDeletedFalse(String keyword, Pageable pageable);

    Page<Board> findByCategoryAndTitleContainingAndIsDeletedFalse(String category, String keyword, Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Board b
            SET b.likeCount = :likeCount
            WHERE b.id = :boardId
            """)
    int updateLikeCount(@Param("boardId") String boardId, @Param("likeCount") int likeCount);

    @Modifying
    @Query("update Board b set b.likeCount = b.likeCount + 1 where b.id = :boardId")
    int increaseLikeCount(@Param("boardId") String boardId);

    @Modifying
    @Query("update Board b set b.likeCount = b.likeCount - 1 where b.id = :boardId and b.likeCount > 0")
    int decreaseLikeCount(@Param("boardId") String boardId);

    @Modifying
    @Query("update Board b set b.commentCount = b.commentCount + 1 where b.id = :boardId")
    void increaseCommentCount(@Param("boardId") String boardId);

    @Modifying
    @Query("update Board b set b.viewCount = b.viewCount + 1 where b.id = :boardId")
    void increaseViewCount(@Param("boardId") String boardId);

    @Query("""
            SELECT b FROM Board b
            JOIN FETCH b.user u
            WHERE u.id = :userId
            AND b.isDeleted = false
            AND (
                :cursorTime IS NULL
                OR b.createdAt < :cursorTime
                OR (b.createdAt = :cursorTime AND b.id < :cursorId)
            )
            ORDER BY b.createdAt DESC, b.id DESC
            """)
    List<Board> findMyBoardsWithCursor(
            @Param("userId") String userId,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorId") String cursorId,
            Pageable pageable
    );

}
