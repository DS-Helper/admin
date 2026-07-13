package com.project.ds_helper.domain.board.service;

import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.board.entity.BoardImage;
import com.project.ds_helper.domain.board.repository.BoardImageRepository;
import com.project.ds_helper.domain.post.util.S3Util;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardImageServiceTest {

    @Mock private BoardImageRepository boardImageRepository;
    @Mock private S3Util s3Util;

    @Test
    @DisplayName("게시글 ID로 이미지 목록을 조회한다")
    void findByBoardId_returnsImages() {
        Board board = Board.builder().id("b-1").build();
        BoardImage image = BoardImage.builder().board(board).s3Key("key").build();
        BoardImageService service = new BoardImageService(boardImageRepository, s3Util);
        when(boardImageRepository.findByBoard_Id("b-1")).thenReturn(List.of(image));

        assertThat(service.findByBoardId("b-1")).hasSize(1);
    }

    @Test
    @DisplayName("썸네일 맵은 게시글별 첫 이미지를 사용한다")
    void buildThumbnailMapByBoardIds_buildsMap() {
        Board board = Board.builder().id("b-1").build();
        BoardImage image = BoardImage.builder().board(board).s3Key("key").build();
        BoardImageService service = new BoardImageService(boardImageRepository, s3Util);
        when(boardImageRepository.findBoardThumbnailsByBoardIds(List.of("b-1"))).thenReturn(List.of(image));
        when(s3Util.toS3UrlByS3Key("key")).thenReturn("url");

        assertThat(service.buildThumbnailMapByBoardIds(List.of("b-1")).get("b-1")).isEqualTo("url");
    }

    @Test
    @DisplayName("게시글 ID 목록이 비어 있으면 썸네일 조회를 하지 않는다")
    void findThumbnailsByBoardIds_returnsEmptyListWhenIdsEmpty() {
        BoardImageService service = new BoardImageService(boardImageRepository, s3Util);

        assertThat(service.findThumbnailsByBoardIds(List.of())).isEmpty();
    }

    @Test
    @DisplayName("썸네일 맵은 이미지가 없으면 비어 있다")
    void buildThumbnailMap_returnsEmptyMapWhenImagesEmpty() {
        BoardImageService service = new BoardImageService(boardImageRepository, s3Util);

        assertThat(service.buildThumbnailMap(List.of())).isEmpty();
    }

    @Test
    @DisplayName("이미지 URL 변환은 이미지가 없으면 비어 있다")
    void toImageUrls_returnsEmptyListWhenImagesEmpty() {
        BoardImageService service = new BoardImageService(boardImageRepository, s3Util);

        assertThat(service.toImageUrls(List.of())).isEmpty();
    }

    @Test
    @DisplayName("이미지 URL 변환은 여러 이미지를 모두 변환한다")
    void toImageUrls_convertsAllImages() {
        Board board = Board.builder().id("b-1").build();
        BoardImage image1 = BoardImage.builder().board(board).s3Key("key-1").build();
        BoardImage image2 = BoardImage.builder().board(board).s3Key("key-2").build();
        BoardImageService service = new BoardImageService(boardImageRepository, s3Util);
        when(s3Util.toS3UrlByS3Key("key-1")).thenReturn("url-1");
        when(s3Util.toS3UrlByS3Key("key-2")).thenReturn("url-2");

        assertThat(service.toImageUrls(List.of(image1, image2))).containsExactly("url-1", "url-2");
    }
}
