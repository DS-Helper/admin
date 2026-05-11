package com.project.ds_helper.domain.board.controller;

import com.project.ds_helper.common.dto.response.PageResponseDto;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.domain.board.dto.response.GetScrappedBoardsResponseDto;
import com.project.ds_helper.domain.board.service.BoardScrapService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardScrapControllerTest {

    @Mock
    private BoardScrapService boardScrapService;

    @InjectMocks
    private BoardScrapController boardScrapController;

    @Test
    @DisplayName("스크랩 목록 조회는 스크랩 게시글 응답을 반환한다")
    void getMyScraps_returnsScrappedBoards() {
        GetScrappedBoardsResponseDto.Board board = GetScrappedBoardsResponseDto.Board.builder()
                .id("board-1")
                .title("title")
                .content("content")
                .likeCount(3)
                .commentCount(2)
                .thumbNailUrl("https://cdn.example.com/thumb.png")
                .build();

        GetScrappedBoardsResponseDto responseDto = GetScrappedBoardsResponseDto.builder()
                .boards(List.of(board))
                .page(new PageResponseDto(0, 10, 1L, 1, true, true, false, false, null))
                .build();

        when(boardScrapService.getMyScrappedBoards(null, 0, 10, "desc", "createdAt"))
                .thenReturn(responseDto);

        ResponseEntity<?> response = boardScrapController.getMyScraps(null, 0, 10, "desc", "createdAt");
        ResponseVo<?> body = (ResponseVo<?>) response.getBody();
        GetScrappedBoardsResponseDto data = (GetScrappedBoardsResponseDto) body.getData();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(data.getBoards()).hasSize(1);
        assertThat(data.getBoards().getFirst())
                .extracting("id", "likeCount")
                .containsExactly("board-1", 3);
    }

    @Test
    @DisplayName("스크랩 수 조회는 서비스 결과를 그대로 반환한다")
    void countScrap_returnsScrapCount() {
        when(boardScrapService.countScrap("board-1")).thenReturn(5L);

        ResponseEntity<?> response = boardScrapController.countScrap("board-1");
        ResponseVo<?> body = (ResponseVo<?>) response.getBody();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(body.getData()).isEqualTo(5L);
    }

    @Test
    @DisplayName("스크랩 토글은 서비스 호출 결과를 반환한다")
    void toggleScrap_returnsToggleResult() {
        when(boardScrapService.toggleBoardScrap(null, "board-1")).thenReturn(true);

        ResponseEntity<?> response = boardScrapController.toggleScrap(null, "board-1");
        ResponseVo<?> body = (ResponseVo<?>) response.getBody();

        verify(boardScrapService).toggleBoardScrap(null, "board-1");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(body.getData()).isEqualTo(true);
    }
}
