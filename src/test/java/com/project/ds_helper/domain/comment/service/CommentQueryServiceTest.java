package com.project.ds_helper.domain.comment.service;

import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.comment.dto.response.GetChildCommentsResponseDto;
import com.project.ds_helper.domain.comment.dto.response.GetParentCommentsByBoardIdResponseDto;
import com.project.ds_helper.domain.comment.entity.Comment;
import com.project.ds_helper.domain.comment.repository.CommentRepository;
import com.project.ds_helper.domain.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentQueryServiceTest {

    @Mock private CommentRepository commentRepository;

    @InjectMocks
    private CommentQueryService commentQueryService;

    @Test
    @DisplayName("부모 댓글 조회는 커서와 hasNext를 계산한다")
    void getParentComments_returnsCursorResponse() {
        User user = user("user-1", "홍길동");
        Board board = board("board-1", false, user);
        Comment first = comment("comment-1", board, user, null, "첫 댓글");
        Comment second = comment("comment-2", board, user, null, "두 번째 댓글");
        org.springframework.test.util.ReflectionTestUtils.setField(first, "createdAt", LocalDateTime.of(2026, 3, 27, 9, 0));
        org.springframework.test.util.ReflectionTestUtils.setField(second, "createdAt", LocalDateTime.of(2026, 3, 27, 10, 0));
        when(commentRepository.findParentCommentsWithCursor(anyString(), any(), any(), any())).thenReturn(new ArrayList<>(List.of(first, second)));

        CursorResponseDto<GetParentCommentsByBoardIdResponseDto> result = commentQueryService.getParentComments("board-1", null, null, 1);

        assertThat(result.content()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("대댓글 조회는 부모 댓글이 없으면 예외가 발생한다")
    void getChildComments_throwsWhenParentMissing() {
        when(commentRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> commentQueryService.getChildComments("missing", null, null, 10))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("대댓글 조회는 DTO 목록과 커서를 반환한다")
    void getChildComments_returnsCursorResponse() {
        User user = user("user-1", "홍길동");
        Board board = board("board-1", false, user);
        Comment child = comment("child-1", board, user, null, "대댓글");
        org.springframework.test.util.ReflectionTestUtils.setField(child, "createdAt", LocalDateTime.of(2026, 3, 27, 11, 0));
        when(commentRepository.existsById("parent-1")).thenReturn(true);
        when(commentRepository.findChildComments(anyString(), any(), any(), any())).thenReturn(new ArrayList<>(List.of(child)));

        CursorResponseDto<GetChildCommentsResponseDto> result = commentQueryService.getChildComments("parent-1", null, null, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.cursorId()).isEqualTo("child-1");
    }

    private User user(String id, String name) {
        return User.builder().id(id).name(name).profileImageUrl("https://cdn.example.com/profile.png").build();
    }

    private Board board(String id, boolean deleted, User user) {
        return Board.builder().id(id).user(user).title("title").content("content").category("FREE").isDeleted(deleted).build();
    }

    private Comment comment(String id, Board board, User user, Comment parent, String content) {
        return Comment.builder().id(id).board(board).user(user).parent(parent).content(content).children(new ArrayList<>()).build();
    }
}
