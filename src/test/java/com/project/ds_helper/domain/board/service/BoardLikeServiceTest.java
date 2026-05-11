package com.project.ds_helper.domain.board.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.board.entity.BoardLike;
import com.project.ds_helper.domain.board.repository.BoardLikeRepository;
import com.project.ds_helper.domain.board.repository.BoardRepository;
import com.project.ds_helper.domain.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardLikeServiceTest {

    @Mock
    private UserUtil userUtil;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardLikeRepository boardLikeRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BoardLikeService boardLikeService;

    @Test
    @DisplayName("좋아요가 없으면 새 좋아요를 생성하고 true를 반환한다")
    void toggleBoardLike_createsLikeWhenNotExists() {
        User user = user("user-1");
        Board board = board("board-1", false, user);

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));
        when(boardLikeRepository.findByUser_IdAndBoard_Id("user-1", "board-1")).thenReturn(Optional.empty());
        when(boardRepository.increaseLikeCount("board-1")).thenReturn(1);

        boolean result = boardLikeService.toggleBoardLike(authentication, "board-1");

        assertThat(result).isTrue();
        verify(boardLikeRepository).save(any(BoardLike.class));
        verify(boardRepository).increaseLikeCount("board-1");
    }

    @Test
    @DisplayName("기존 좋아요가 있으면 삭제하고 false를 반환한다")
    void toggleBoardLike_deletesExistingLike() {
        User user = user("user-1");
        Board board = board("board-1", false, user);
        BoardLike boardLike = BoardLike.builder().board(board).user(user).build();

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));
        when(boardLikeRepository.findByUser_IdAndBoard_Id("user-1", "board-1")).thenReturn(Optional.of(boardLike));
        when(boardRepository.decreaseLikeCount("board-1")).thenReturn(1);

        boolean result = boardLikeService.toggleBoardLike(authentication, "board-1");

        assertThat(result).isFalse();
        verify(boardLikeRepository).delete(boardLike);
        verify(boardRepository).decreaseLikeCount("board-1");
    }

    @Test
    @DisplayName("삭제된 게시글은 좋아요할 수 없다")
    void toggleBoardLike_throwsWhenBoardDeleted() {
        User user = user("user-1");
        Board board = board("board-1", true, user);

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));

        assertThatThrownBy(() -> boardLikeService.toggleBoardLike(authentication, "board-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Deleted Board");
    }

    @Test
    @DisplayName("좋아요 수를 조회한다")
    void countLike_returnsRepositoryCount() {
        when(boardLikeRepository.countByBoard_Id("board-1")).thenReturn(7);

        long result = boardLikeService.countLike("board-1");

        assertThat(result).isEqualTo(7L);
    }

    @Test
    @DisplayName("좋아요 증가 대상 게시글이 없으면 예외가 발생한다")
    void toggleBoardLike_throwsWhenBoardCountUpdateFails() {
        User user = user("user-1");
        Board board = board("board-1", false, user);

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));
        when(boardLikeRepository.findByUser_IdAndBoard_Id("user-1", "board-1")).thenReturn(Optional.empty());
        when(boardRepository.increaseLikeCount("board-1")).thenReturn(0);

        assertThatThrownBy(() -> boardLikeService.toggleBoardLike(authentication, "board-1"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Board Not Found");
    }

    private User user(String id) {
        return User.builder().id(id).name("tester").build();
    }

    private Board board(String id, boolean deleted, User user) {
        return Board.builder()
                .id(id)
                .user(user)
                .title("title")
                .content("content")
                .category("DAILY")
                .isDeleted(deleted)
                .build();
    }
}
