package com.project.ds_helper.domain.board.controller;

import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.common.dto.response.PageResponseDto;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.domain.board.dto.request.CreateBoardReqDto;
import com.project.ds_helper.domain.board.dto.request.UpdateBoardRequestDto;
import com.project.ds_helper.domain.board.dto.response.CreateBoardResponseDto;
import com.project.ds_helper.domain.board.dto.response.GetBoardByIdResponseDto;
import com.project.ds_helper.domain.board.dto.response.GetBoardsByCategoryResponseDto;
import com.project.ds_helper.domain.board.dto.response.GetMyBoardsResponseDto;
import com.project.ds_helper.domain.board.service.BoardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardControllerTest {

    @Mock
    private BoardService boardService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BoardController boardController;

    @Test
    @DisplayName("getBoardById includes writer info")
    void getBoardById_includesWriterInfo() {
        GetBoardByIdResponseDto responseDto = new GetBoardByIdResponseDto(
                "board-1",
                "title",
                "content",
                "writer",
                "user-1",
                "https://cdn.example.com/profiles/writer.png",
                11,
                7,
                3,
                true,
                false,
                List.of("https://cdn.example.com/boards/1.png"),
                "2026-03-26 10:30:00"
        );

        when(boardService.getBoardById(authentication, "board-1")).thenReturn(responseDto);

        ResponseEntity<ResponseVo<GetBoardByIdResponseDto>> response =
                boardController.getBoardById(authentication, "board-1");
        GetBoardByIdResponseDto body = response.getBody().getData();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(body.writerName()).isEqualTo("writer");
        assertThat(body.writerProfileImageUrl()).isEqualTo("https://cdn.example.com/profiles/writer.png");
    }

    @Test
    @DisplayName("getBoardsByCategory includes writer info and like state")
    void getBoardsByCategory_includesWriterInfoAndLikeState() {
        GetBoardsByCategoryResponseDto.Board board = GetBoardsByCategoryResponseDto.Board.builder()
                .id("board-1")
                .title("title")
                .content("content")
                .writerName("writer")
                .writerId("user-1")
                .writerProfileImageUrl("https://cdn.example.com/profiles/writer.png")
                .likeCount(5)
                .commentCount(2)
                .isLiked(true)
                .thumbNailUrl("https://cdn.example.com/thumbs/board-1.png")
                .build();

        GetBoardsByCategoryResponseDto responseDto = new GetBoardsByCategoryResponseDto(
                List.of(board),
                new PageResponseDto(0, 10, 1L, 1, true, true, false, false, null)
        );

        when(boardService.getBoardsByCategory(authentication, "전체", null, 0, 10, "desc", "createdAt"))
                .thenReturn(responseDto);

        ResponseEntity<ResponseVo<GetBoardsByCategoryResponseDto>> response =
                boardController.getBoardsByCategory(authentication, "전체", null, 0, 10, "desc", "createdAt");
        GetBoardsByCategoryResponseDto body = response.getBody().getData();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(body.getBoards()).hasSize(1);
        assertThat(body.getBoards().getFirst().getWriterName()).isEqualTo("writer");
        assertThat(body.getBoards().getFirst().isLiked()).isTrue();
    }

    @Test
    @DisplayName("getBoardsByCategory supports keyword")
    void getBoardsByCategory_withKeyword_returnsSearchResult() {
        GetBoardsByCategoryResponseDto.Board board = GetBoardsByCategoryResponseDto.Board.builder()
                .id("board-2")
                .title("searched title")
                .content("content")
                .writerName("writer")
                .writerId("user-1")
                .writerProfileImageUrl("https://cdn.example.com/profiles/writer.png")
                .likeCount(1)
                .commentCount(0)
                .isLiked(false)
                .thumbNailUrl(null)
                .build();

        GetBoardsByCategoryResponseDto responseDto = new GetBoardsByCategoryResponseDto(
                List.of(board),
                new PageResponseDto(0, 10, 1L, 1, true, true, false, false, null)
        );

        when(boardService.getBoardsByCategory(authentication, "전체", "검색", 0, 10, "desc", "createdAt"))
                .thenReturn(responseDto);

        ResponseEntity<ResponseVo<GetBoardsByCategoryResponseDto>> response =
                boardController.getBoardsByCategory(authentication, "전체", "검색", 0, 10, "desc", "createdAt");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getBoards()).hasSize(1);
        assertThat(response.getBody().getData().getBoards().getFirst().getTitle()).isEqualTo("searched title");
    }

    @Test
    @DisplayName("createBoard delegates with empty images")
    void createBoard_withNullImages_delegatesWithEmptyList() throws Exception {
        CreateBoardReqDto dto = new CreateBoardReqDto("전체", "제목", "내용");
        CreateBoardResponseDto created = new CreateBoardResponseDto(
                "board-1",
                "title",
                "content",
                "writer",
                "https://cdn.example.com/profiles/writer.png",
                0,
                0,
                0,
                false,
                false,
                List.of(),
                "2026-04-12 12:00:00"
        );
        when(boardService.createBoard(eq(authentication), eq(dto), anyList())).thenReturn(created);

        ResponseEntity<ResponseVo<CreateBoardResponseDto>> response = boardController.createBoard(authentication, dto, null);

        verify(boardService).createBoard(eq(authentication), eq(dto), anyList());
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().id()).isEqualTo("board-1");
    }

    @Test
    @DisplayName("createBoard delegates with provided images")
    void createBoard_withImages_delegatesOriginalList() throws Exception {
        CreateBoardReqDto dto = new CreateBoardReqDto("?꾩껜", "?쒕ぉ", "?댁슜");
        MultipartFile image = new MockMultipartFile("images", "a.png", "image/png", "a".getBytes());
        CreateBoardResponseDto created = new CreateBoardResponseDto(
                "board-1", "title", "content", "writer", null,
                0, 0, 0, false, false, List.of("url"), null
        );
        when(boardService.createBoard(authentication, dto, List.of(image))).thenReturn(created);

        ResponseEntity<ResponseVo<CreateBoardResponseDto>> response =
                boardController.createBoard(authentication, dto, List.of(image));

        verify(boardService).createBoard(authentication, dto, List.of(image));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    @DisplayName("createBoard delegates with empty image list")
    void createBoard_withEmptyImages_delegatesWithEmptyList() throws Exception {
        CreateBoardReqDto dto = new CreateBoardReqDto("?꾩껜", "?쒕ぉ", "?댁슜");
        CreateBoardResponseDto created = new CreateBoardResponseDto(
                "board-1", "title", "content", "writer", null,
                0, 0, 0, false, false, List.of(), null
        );
        when(boardService.createBoard(eq(authentication), eq(dto), anyList())).thenReturn(created);

        ResponseEntity<ResponseVo<CreateBoardResponseDto>> response =
                boardController.createBoard(authentication, dto, List.of());

        verify(boardService).createBoard(eq(authentication), eq(dto), anyList());
        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    @DisplayName("getMyBoards returns cursor response")
    void getMyBoards_returnsCursorResponse() {
        GetMyBoardsResponseDto board = GetMyBoardsResponseDto.builder()
                .id("board-1")
                .title("title")
                .content("content")
                .writerName("writer")
                .writerId("user-1")
                .writerProfileImageUrl("https://cdn.example.com/profiles/writer.png")
                .likeCount(3)
                .commentCount(2)
                .isLiked(true)
                .thumbNailUrl("https://cdn.example.com/thumbs/1.png")
                .build();
        LocalDateTime cursorTime = LocalDateTime.of(2026, 4, 12, 10, 0, 0);
        CursorResponseDto<GetMyBoardsResponseDto> responseDto =
                CursorResponseDto.toDto(List.of(board), cursorTime, "board-1", true);
        when(boardService.getMyBoards(authentication, null, null, 10)).thenReturn(responseDto);

        ResponseEntity<ResponseVo<CursorResponseDto<GetMyBoardsResponseDto>>> response =
                boardController.getMyBoards(authentication, null, null, 10);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().content()).hasSize(1);
        assertThat(response.getBody().getData().cursorId()).isEqualTo("board-1");
        assertThat(response.getBody().getData().hasNext()).isTrue();
    }

    @Test
    @DisplayName("updateBoard delegates with empty images")
    void updateBoard_withNullImages_delegatesWithEmptyList() throws Exception {
        UpdateBoardRequestDto dto = new UpdateBoardRequestDto("board-1", "updated title", "updated content", List.of());

        ResponseEntity<ResponseVo<Void>> response = boardController.updateBoard(authentication, dto, null);

        verify(boardService).updateBoard(eq(authentication), eq(dto), anyList());
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("updateBoard delegates with provided images")
    void updateBoard_withImages_delegatesOriginalList() throws Exception {
        UpdateBoardRequestDto dto = new UpdateBoardRequestDto("board-1", "updated title", "updated content", List.of());
        MultipartFile image = new MockMultipartFile("images", "a.png", "image/png", "a".getBytes());

        ResponseEntity<ResponseVo<Void>> response = boardController.updateBoard(authentication, dto, List.of(image));

        verify(boardService).updateBoard(authentication, dto, List.of(image));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("updateBoard delegates with empty image list")
    void updateBoard_withEmptyImages_delegatesWithEmptyList() throws Exception {
        UpdateBoardRequestDto dto = new UpdateBoardRequestDto("board-1", "updated title", "updated content", List.of());

        ResponseEntity<ResponseVo<Void>> response = boardController.updateBoard(authentication, dto, List.of());

        verify(boardService).updateBoard(eq(authentication), eq(dto), anyList());
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("updateBoard handles null keep image URLs")
    void updateBoard_withNullKeepImageUrls_delegates() throws Exception {
        UpdateBoardRequestDto dto = new UpdateBoardRequestDto("board-1", "updated title", "updated content", null);

        ResponseEntity<ResponseVo<Void>> response = boardController.updateBoard(authentication, dto, List.of());

        verify(boardService).updateBoard(eq(authentication), eq(dto), anyList());
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("deleteBoard returns no content")
    void deleteBoard_returnsNoContent() {
        ResponseEntity<ResponseVo<Void>> response = boardController.deleteBoard(authentication, "board-1");

        verify(boardService).deleteBoard(authentication, "board-1");
        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }
}
