package com.project.ds_helper.domain.board.service;

import com.project.ds_helper.domain.board.entity.BoardImage;
import com.project.ds_helper.domain.board.repository.BoardImageRepository;
import com.project.ds_helper.domain.post.util.S3Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardImageService {

    private final BoardImageRepository boardImageRepository;
    private final S3Util s3Util;

    public List<BoardImage> findByBoardId(String boardId) {
        return boardImageRepository.findByBoard_Id(boardId);
    }

    public List<BoardImage> findThumbnailsByBoardIds(List<String> boardIds) {
        return boardIds.isEmpty() ? List.of() : boardImageRepository.findBoardThumbnailsByBoardIds(boardIds);
    }

    public HashMap<String, String> buildThumbnailMap(List<BoardImage> boardImages) {
        HashMap<String, String> thumbnails = new HashMap<>();
        for (BoardImage boardImage : boardImages) {
            thumbnails.putIfAbsent(boardImage.getBoard().getId(), s3Util.toS3UrlByS3Key(boardImage.getS3Key()));
        }
        return thumbnails;
    }

    public HashMap<String, String> buildThumbnailMapByBoardIds(List<String> boardIds) {
        return buildThumbnailMap(findThumbnailsByBoardIds(boardIds));
    }

    public List<String> toImageUrls(List<BoardImage> boardImages) {
        return boardImages.isEmpty() ? List.of() : boardImages.stream().map(image -> s3Util.toS3UrlByS3Key(image.getS3Key())).toList();
    }
}
