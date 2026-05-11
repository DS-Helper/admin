package com.project.ds_helper.domain.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.domain.comment.dto.request.CreateCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.request.DeleteCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.request.UpdateCommentRequestDto;
import com.project.ds_helper.domain.comment.dto.response.CreateCommentResponseDto;
import com.project.ds_helper.domain.comment.dto.response.GetChildCommentsResponseDto;
import com.project.ds_helper.domain.comment.dto.response.GetParentCommentsByBoardIdResponseDto;
import com.project.ds_helper.domain.comment.dto.response.UpdateCommentResponseDto;
import com.project.ds_helper.domain.comment.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CommentService commentService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CommentController commentController;

    @Test
    @DisplayName("getParentComments includes writer info")
    void getParentComments_includesWriterInfo() {
        GetParentCommentsByBoardIdResponseDto comment = new GetParentCommentsByBoardIdResponseDto(
                "comment-1",
                "writer",
                "user-1",
                "https://cdn.example.com/profiles/writer.png",
                "parent comment",
                LocalDateTime.of(2026, 3, 26, 9, 0, 0)
        );

        CursorResponseDto<GetParentCommentsByBoardIdResponseDto> responseDto = CursorResponseDto.toDto(
                List.of(comment),
                LocalDateTime.of(2026, 3, 26, 9, 0, 0),
                "comment-1",
                false
        );

        when(commentService.getParentComments(anyString(), isNull(), isNull(), anyInt()))
                .thenReturn(responseDto);

        ResponseEntity<ResponseVo<CursorResponseDto<GetParentCommentsByBoardIdResponseDto>>> response =
                commentController.getParentComments("board-1", null, null, 10);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().content()).hasSize(1);
        assertThat(response.getBody().getData().content().getFirst().writerName()).isEqualTo("writer");
        assertThat(response.getBody().getData().content().getFirst().writerId()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("getChildComments includes writer info")
    void getChildComments_includesWriterInfo() {
        GetChildCommentsResponseDto comment = new GetChildCommentsResponseDto(
                "child-1",
                "child comment",
                "child-writer",
                "user-2",
                "https://cdn.example.com/profiles/child.png",
                LocalDateTime.of(2026, 3, 26, 9, 30, 0)
        );

        CursorResponseDto<GetChildCommentsResponseDto> responseDto = CursorResponseDto.toDto(
                List.of(comment),
                LocalDateTime.of(2026, 3, 26, 9, 30, 0),
                "child-1",
                false
        );

        when(commentService.getChildComments(anyString(), isNull(), isNull(), anyInt()))
                .thenReturn(responseDto);

        ResponseEntity<ResponseVo<CursorResponseDto<GetChildCommentsResponseDto>>> response =
                commentController.getChildComments("parent-1", null, null, 10);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().content()).hasSize(1);
        assertThat(response.getBody().getData().content().getFirst().userName()).isEqualTo("child-writer");
        assertThat(response.getBody().getData().content().getFirst().userId()).isEqualTo("user-2");
    }

    @Test
    @DisplayName("createComment returns created dto")
    void createComment_returnsCreatedComment() throws Exception {
        CreateCommentRequestDto requestDto = readRequest(
                """
                {
                  "boardId": "board-1",
                  "content": "댓글 내용"
                }
                """,
                CreateCommentRequestDto.class
        );

        CreateCommentResponseDto responseDto = CreateCommentResponseDto.builder()
                .commentId("comment-1")
                .content("댓글 내용")
                .writerName("writer")
                .build();

        when(commentService.createComment(any(), any(CreateCommentRequestDto.class))).thenReturn(responseDto);

        ResponseEntity<ResponseVo<CreateCommentResponseDto>> response =
                commentController.createComment(authentication, requestDto);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getCommentId()).isEqualTo("comment-1");
    }

    @Test
    @DisplayName("updateComment returns updated dto")
    void updateComment_returnsUpdatedComment() throws Exception {
        UpdateCommentRequestDto requestDto = readRequest(
                """
                {
                  "commentId": "comment-1",
                  "content": "수정 내용"
                }
                """,
                UpdateCommentRequestDto.class
        );

        UpdateCommentResponseDto responseDto = UpdateCommentResponseDto.builder()
                .commentId("comment-1")
                .content("수정 내용")
                .build();

        when(commentService.updateComment(any(), any(UpdateCommentRequestDto.class))).thenReturn(responseDto);

        ResponseEntity<ResponseVo<UpdateCommentResponseDto>> response =
                commentController.updateComment(authentication, requestDto);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getCommentId()).isEqualTo("comment-1");
    }

    @Test
    @DisplayName("deleteComment returns no content")
    void deleteComment_returnsNoContent() throws Exception {
        DeleteCommentRequestDto requestDto = readRequest(
                """
                {
                  "commentId": "comment-1"
                }
                """,
                DeleteCommentRequestDto.class
        );
        doNothing().when(commentService).deleteComment(any(), any(DeleteCommentRequestDto.class));

        ResponseEntity<ResponseVo<Void>> response = commentController.deleteComment(authentication, requestDto);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(commentService).deleteComment(authentication, requestDto);
    }

    private <T> T readRequest(String json, Class<T> type) throws Exception {
        return objectMapper.readValue(json, type);
    }
}

