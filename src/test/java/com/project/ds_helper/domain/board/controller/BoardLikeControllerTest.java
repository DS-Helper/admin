package com.project.ds_helper.domain.board.controller;

import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.domain.board.service.BoardLikeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardLikeControllerTest {

    @Mock
    private BoardLikeService boardLikeService;

    @InjectMocks
    private BoardLikeController boardLikeController;

    @Test
    @DisplayName("좋아요 수 조회는 서비스 결과를 그대로 반환한다")
    void countLike_returnsLikeCount() {
        when(boardLikeService.countLike("board-1")).thenReturn(5);

        ResponseEntity<ResponseVo<Integer>> response = boardLikeController.countLike("board-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(5L);
    }

    @Test
    @DisplayName("좋아요 토글 성공 시 true를 반환한다")
    void toggleBoardLike_returnsTrueWhenLiked() {
        when(boardLikeService.toggleBoardLike(null, "board-1")).thenReturn(true);

        ResponseEntity<ResponseVo<Boolean>> response = boardLikeController.toggleBoardLike(null, "board-1");

        verify(boardLikeService).toggleBoardLike(null, "board-1");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isTrue();
    }

    @Test
    @DisplayName("좋아요 취소 시 false를 반환한다")
    void toggleBoardLike_returnsFalseWhenCanceled() {
        when(boardLikeService.toggleBoardLike(null, "board-1")).thenReturn(false);

        ResponseEntity<ResponseVo<Boolean>> response = boardLikeController.toggleBoardLike(null, "board-1");

        assertThat(response.getBody().getData()).isFalse();
        assertThat(response.getBody().getMessage()).contains("canceled");
    }
}
