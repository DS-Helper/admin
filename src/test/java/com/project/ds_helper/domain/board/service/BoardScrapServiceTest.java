package com.project.ds_helper.domain.board.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.board.dto.response.GetScrappedBoardsResponseDto;
import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.board.entity.BoardImage;
import com.project.ds_helper.domain.board.entity.BoardScrap;
import com.project.ds_helper.domain.board.repository.BoardImageRepository;
import com.project.ds_helper.domain.board.repository.BoardRepository;
import com.project.ds_helper.domain.board.repository.BoardScrapRepository;
import com.project.ds_helper.domain.comment.repository.CommentRepository;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardScrapServiceTest {

    @Mock
    private UserUtil userUtil;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardScrapRepository boardScrapRepository;

    @Mock
    private BoardImageRepository boardImageRepository;

    @Mock
    private S3Util s3Util;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BoardScrapService boardScrapService;

    @Test
    @DisplayName("스크랩이 없으면 새 스크랩을 생성하고 true를 반환한다")
    void toggleBoardScrap_createsScrap() {
        User user = user("user-1");
        Board board = board("board-1", false, user);

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));
        when(boardScrapRepository.findByUser_IdAndBoard_Id("user-1", "board-1")).thenReturn(Optional.empty());

        boolean result = boardScrapService.toggleBoardScrap(authentication, "board-1");

        assertThat(result).isTrue();
        verify(boardScrapRepository).save(any(BoardScrap.class));
    }

    @Test
    @DisplayName("기존 스크랩이 있으면 삭제하고 false를 반환한다")
    void toggleBoardScrap_deletesExistingScrap() {
        User user = user("user-1");
        Board board = board("board-1", false, user);
        BoardScrap scrap = BoardScrap.builder().board(board).user(user).build();

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));
        when(boardScrapRepository.findByUser_IdAndBoard_Id("user-1", "board-1")).thenReturn(Optional.of(scrap));

        boolean result = boardScrapService.toggleBoardScrap(authentication, "board-1");

        assertThat(result).isFalse();
        verify(boardScrapRepository).delete(scrap);
    }

    @Test
    @DisplayName("삭제된 게시글은 스크랩할 수 없다")
    void toggleBoardScrap_throwsWhenBoardDeleted() {
        User user = user("user-1");
        Board board = board("board-1", true, user);

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));

        assertThatThrownBy(() -> boardScrapService.toggleBoardScrap(authentication, "board-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Deleted Board");
    }

    @Test
    @DisplayName("내 스크랩 목록 조회 시 썸네일을 포함한 응답을 생성한다")
    void getMyScrappedBoards_returnsBoardsWithThumbnail() {
        User user = user("user-1");
        Board board = board("board-1", false, user);
        BoardImage image = BoardImage.builder().board(board).s3Key("s3/key.png").build();
        Page<Board> page = new PageImpl<>(List.of(board));

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(boardScrapRepository.findScrappedBoards(org.mockito.ArgumentMatchers.eq("user-1"), any(Pageable.class))).thenReturn(page);
        when(boardImageRepository.findBoardThumbnailsByBoardIds(List.of("board-1"))).thenReturn(List.of(image));
        when(s3Util.toS3UrlByS3Key("s3/key.png")).thenReturn("https://cdn.example.com/s3/key.png");
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("board-1")).thenReturn(0L);

        GetScrappedBoardsResponseDto result =
                boardScrapService.getMyScrappedBoards(authentication, 0, 10, "desc", "createdAt");

        assertThat(result.getBoards()).hasSize(1);
        assertThat(result.getBoards().getFirst())
                .extracting("thumbNailUrl")
                .isEqualTo("https://cdn.example.com/s3/key.png");
    }

    @Test
    @DisplayName("스크랩 목록이 비어 있으면 빈 목록을 반환한다")
    void getMyScrappedBoards_returnsEmptyContent() {
        Page<Board> emptyPage = new PageImpl<>(List.of());

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(boardScrapRepository.findScrappedBoards(org.mockito.ArgumentMatchers.eq("user-1"), any(Pageable.class))).thenReturn(emptyPage);

        GetScrappedBoardsResponseDto result =
                boardScrapService.getMyScrappedBoards(authentication, 0, 10, "desc", "createdAt");

        assertThat(result.getBoards()).isEmpty();
    }

    @Test
    @DisplayName("스크랩 수를 조회한다")
    void countScrap_returnsRepositoryCount() {
        when(boardScrapRepository.countByBoard_Id("board-1")).thenReturn(9L);

        long result = boardScrapService.countScrap("board-1");

        assertThat(result).isEqualTo(9L);
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
