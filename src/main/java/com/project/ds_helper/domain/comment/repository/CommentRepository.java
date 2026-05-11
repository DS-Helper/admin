package com.project.ds_helper.domain.comment.repository;

import com.project.ds_helper.domain.comment.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

    long countByBoard_IdAndIsDeletedFalse(String boardId);

    // 작성자 검증 포함 조회
    Optional<Comment> findByIdAndUser_Id(String commentId, String userId);

    @Query("""
    SELECT c FROM Comment c
    LEFT JOIN FETCH c.children
    WHERE c.id = :commentId
    AND c.user.id = :userId
    """)
    Optional<Comment> findWithChildren(@Param("commentId") String commentId,
                                       @Param("userId") String userId);

    @Query("""
    SELECT c FROM Comment c
    LEFT JOIN FETCH c.children
    WHERE c.id = :commentId
    """)
    Optional<Comment> findWithChildren(@Param("commentId") String commentId);

    @Query("""
        SELECT c FROM Comment c
        JOIN FETCH c.user
        WHERE c.board.id = :boardId
        AND c.parent IS NULL
        AND c.isDeleted = false
        AND (
            :cursorTime IS NULL
            OR
            c.createdAt < :cursorTime
            OR (
                c.createdAt = :cursorTime
                AND c.id < :cursorId
            )
        )
        ORDER BY c.createdAt DESC, c.id DESC
    """)
    List<Comment> findParentCommentsWithCursor(
            @Param("boardId") String boardId,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorId") String cursorId,
            Pageable pageable
    );

    @Query("""
        SELECT c FROM Comment c
        JOIN FETCH c.user
        WHERE c.parent.id = :parentId
        AND c.isDeleted = false
        AND (
            :cursorTime IS NULL
            OR c.createdAt < :cursorTime
            OR (c.createdAt = :cursorTime AND c.id < :cursorId)
        )
        ORDER BY c.createdAt DESC, c.id DESC
    """)
    List<Comment> findChildComments(
            @Param("parentId") String parentId,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorId") String cursorId,
            Pageable pageable
    );

}
