package com.project.ds_helper.domain.board.repository;

import com.project.ds_helper.domain.board.entity.BoardImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardImageRepository extends JpaRepository<BoardImage, String> {

    @Query(value = """
            SELECT bi
            FROM BoardImage bi
            WHERE bi.board.id IN :boardIds
            AND bi.createdAt = (
                SELECT MIN(innerBi.createdAt)
                FROM BoardImage innerBi
                WHERE innerBi.board.id = bi.board.id
            )
            """)
    List<BoardImage> findBoardThumbnailsByBoardIds(List<String> boardIds);

    List<BoardImage> findByBoard_Id(String boardId);
}
