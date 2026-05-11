package com.project.ds_helper.domain.post.repository;

import com.project.ds_helper.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

    @Override
    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT p
            FROM Post p
            WHERE p.user.isDeleted = false
            """)
    Page<Post> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT p
            FROM Post p
            WHERE p.user.isDeleted = false
              AND p.title LIKE %:keyword%
            """)
    Page<Post> findByTitleContaining(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT p
            FROM Post p
            WHERE p.user.isDeleted = false
              AND p.user.id = :userId
            """)
    Page<Post> findAllByUser_Id(@Param("userId") String userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT p
            FROM Post p
            WHERE p.id = :postId
              AND p.user.isDeleted = false
            """)
    java.util.Optional<Post> findVisibleById(@Param("postId") String postId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE Post p
            SET p.viewCount = p.viewCount + 1
            WHERE p.id = :postId
            """)
    int increaseViewCount(@Param("postId") String postId);

//    @EntityGraph(attributePaths = {"postImages"})
//    Page<Post> findAllPost(Pageable pageRequest);
}
