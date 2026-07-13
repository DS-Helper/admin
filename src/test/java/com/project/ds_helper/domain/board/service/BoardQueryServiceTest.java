package com.project.ds_helper.domain.board.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.board.entity.BoardImage;
import com.project.ds_helper.domain.board.enums.BoardCategory;
import com.project.ds_helper.domain.board.repository.BoardImageRepository;
import com.project.ds_helper.domain.board.repository.BoardLikeRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardQueryServiceTest {

    @Mock private UserUtil userUtil;
    @Mock private BoardRepository boardRepository;
    @Mock private BoardImageService boardImageService;
    @Mock private BoardLikeRepository boardLikeRepository;
    @Mock private BoardScrapRepository boardScrapRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private S3Util s3Util;
    @Mock private Authentication authentication;

    @InjectMocks
    private BoardQueryService boardQueryService;

    @Test
    @DisplayName("카테고리 조회는 전체/키워드 분기를 탄다")
    void getBoardsByCategory_usesKeywordBranch() {
        User user = User.builder().id("u-1").name("name").build();
        Board board = Board.builder().id("b-1").user(user).category(BoardCategory.DAILY.name()).title("검색").content("c").build();
        Page<Board> page = new PageImpl<>(List.of(board));
        BoardImage image = BoardImage.builder().board(board).s3Key("key").build();

        when(boardRepository.findByTitleContainingAndIsDeletedFalse(eq("검색"), any())).thenReturn(page);
        when(boardImageService.buildThumbnailMapByBoardIds(List.of("b-1"))).thenReturn(new java.util.HashMap<>(java.util.Map.of("b-1", "url")));
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("b-1")).thenReturn(0L);

        var result = boardQueryService.getBoardsByCategory(null, "전체", "검색", 0, 10, "desc", "createdAt");

        assertThat(result.getBoards()).hasSize(1);
        assertThat(result.getBoards().getFirst().getThumbNailUrl()).isEqualTo("url");
    }

    @Test
    @DisplayName("게시글 목록 조회는 오름차순 정렬도 지원한다")
    void getBoardsByCategory_supportsAscendingSort() {
        User user = User.builder().id("u-1").name("name").build();
        Board board = Board.builder().id("b-1").user(user).category(BoardCategory.DAILY.name()).title("제목").content("c").build();
        Page<Board> page = new PageImpl<>(List.of(board));

        when(boardRepository.findByIsDeletedFalse(any())).thenReturn(page);
        when(boardImageService.buildThumbnailMapByBoardIds(List.of("b-1"))).thenReturn(new java.util.HashMap<>());
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("b-1")).thenReturn(0L);

        var result = boardQueryService.getBoardsByCategory(null, "전체", null, 0, 10, "asc", "createdAt");

        assertThat(result.getBoards()).hasSize(1);
    }

    @Test
    @DisplayName("전체 카테고리 조회는 키워드 없는 분기를 탄다")
    void getBoardsByCategory_usesAllCategoryBranch() {
        User user = User.builder().id("u-1").name("name").build();
        Board board = Board.builder().id("b-1").user(user).category(BoardCategory.DAILY.name()).title("제목").content("c").build();
        Page<Board> page = new PageImpl<>(List.of(board));

        when(boardRepository.findByIsDeletedFalse(any())).thenReturn(page);
        when(boardImageService.buildThumbnailMapByBoardIds(List.of("b-1"))).thenReturn(new java.util.HashMap<>(java.util.Map.of("b-1", "url")));
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("b-1")).thenReturn(0L);

        var result = boardQueryService.getBoardsByCategory(authentication, "전체", null, 0, 10, "desc", "createdAt");

        assertThat(result.getBoards()).hasSize(1);
    }

    @Test
    @DisplayName("전체가 아닌 카테고리 조회는 카테고리 분기를 탄다")
    void getBoardsByCategory_usesCategoryBranch() {
        User user = User.builder().id("u-1").name("name").build();
        Board board = Board.builder().id("b-2").user(user).category(BoardCategory.DAILY.name()).title("제목").content("c").build();
        Page<Board> page = new PageImpl<>(List.of(board));

        when(boardRepository.findByCategoryAndIsDeletedFalse(eq(BoardCategory.DAILY.name()), any())).thenReturn(page);
        when(boardImageService.buildThumbnailMapByBoardIds(List.of("b-2"))).thenReturn(new java.util.HashMap<>(java.util.Map.of("b-2", "url")));
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("b-2")).thenReturn(0L);

        var result = boardQueryService.getBoardsByCategory(null, BoardCategory.DAILY.getKorean(), null, 0, 10, "desc", "createdAt");

        assertThat(result.getBoards()).hasSize(1);
    }

    @Test
    @DisplayName("카테고리 조회는 인증 사용자일 때 좋아요 정보를 반영한다")
    void getBoardsByCategory_usesLikedBranchWhenAuthenticated() {
        User user = User.builder().id("u-1").name("name").build();
        Board board = Board.builder().id("b-3").user(user).category(BoardCategory.DAILY.name()).title("제목").content("c").build();
        Page<Board> page = new PageImpl<>(List.of(board));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("user-1");
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(boardRepository.findByIsDeletedFalse(any())).thenReturn(page);
        when(boardLikeRepository.findLikedBoardIdsByUserIdAndBoardIds("user-1", List.of("b-3"))).thenReturn(List.of("b-3"));
        when(boardImageService.buildThumbnailMapByBoardIds(List.of("b-3"))).thenReturn(new java.util.HashMap<>());
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("b-3")).thenReturn(0L);

        var result = boardQueryService.getBoardsByCategory(authentication, "전체", null, 0, 10, "desc", "createdAt");

        assertThat(result.getBoards().getFirst().isLiked()).isTrue();
    }

    @Test
    @DisplayName("게시글 상세 조회는 조회수와 이미지 URL을 반환한다")
    void getBoardById_returnsDetail() {
        User user = User.builder().id("u-1").name("name").build();
        Board board = Board.builder().id("b-1").user(user).title("t").content("c").build();
        ReflectionTestUtils.setField(board, "createdAt", LocalDateTime.now());
        BoardImage image = BoardImage.builder().board(board).s3Key("key").build();

        when(boardRepository.findById("b-1")).thenReturn(java.util.Optional.of(board));
        when(boardImageService.findByBoardId("b-1")).thenReturn(List.of(image));
        when(boardImageService.toImageUrls(List.of(image))).thenReturn(List.of("url"));
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("b-1")).thenReturn(0L);

        var result = boardQueryService.getBoardById(null, "b-1");

        assertThat(result.id()).isEqualTo("b-1");
        assertThat(result.imageUrls()).containsExactly("url");
    }

    @Test
    @DisplayName("게시글 상세 조회는 인증 사용자일 때 좋아요와 스크랩을 반영한다")
    void getBoardById_returnsLikedAndScrappedWhenAuthenticated() {
        User user = User.builder().id("u-1").name("name").build();
        Board board = Board.builder().id("b-1").user(user).title("t").content("c").build();
        ReflectionTestUtils.setField(board, "createdAt", LocalDateTime.now());

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("user-1");
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(boardRepository.findById("b-1")).thenReturn(java.util.Optional.of(board));
        when(boardImageService.findByBoardId("b-1")).thenReturn(List.of());
        when(boardImageService.toImageUrls(List.of())).thenReturn(List.of());
        when(boardLikeRepository.existsByUser_IdAndBoard_Id("user-1", "b-1")).thenReturn(true);
        when(boardScrapRepository.existsByUser_IdAndBoard_Id("user-1", "b-1")).thenReturn(true);
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("b-1")).thenReturn(0L);

        var result = boardQueryService.getBoardById(authentication, "b-1");

        assertThat(result.isLiked()).isTrue();
        assertThat(result.isScrapped()).isTrue();
    }

    @Test
    @DisplayName("삭제된 게시글은 조회할 수 없다")
    void getBoardById_throwsWhenBoardDeleted() {
        Board board = Board.builder().id("b-1").title("t").content("c").isDeleted(true).build();

        when(boardRepository.findById("b-1")).thenReturn(java.util.Optional.of(board));

        assertThatThrownBy(() -> boardQueryService.getBoardById(null, "b-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Deleted Board");
    }

    @Test
    @DisplayName("게시글 상세 조회는 게시글이 없으면 예외를 던진다")
    void getBoardById_throwsWhenBoardMissing() {
        when(boardRepository.findById("missing")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> boardQueryService.getBoardById(null, "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Board Not Found");
    }

    @Test
    @DisplayName("cursor가 모두 있으면 내 게시글 조회가 동작한다")
    void getMyBoards_returnsCursorResponse() {
        User user = User.builder().id("u-1").name("name").build();
        Board board = Board.builder().id("b-1").user(user).title("t").content("c").build();
        ReflectionTestUtils.setField(board, "createdAt", LocalDateTime.of(2026, 1, 1, 10, 0));

        when(userUtil.extractUserId(authentication)).thenReturn("u-1");
        when(boardRepository.findMyBoardsWithCursor(eq("u-1"), any(), any(), any())).thenReturn(List.of(board));
        when(boardLikeRepository.findLikedBoardIdsByUserIdAndBoardIds("u-1", List.of("b-1"))).thenReturn(List.of("b-1"));
        when(boardImageService.buildThumbnailMapByBoardIds(List.of("b-1"))).thenReturn(new java.util.HashMap<>());
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("b-1")).thenReturn(0L);

        var result = boardQueryService.getMyBoards(authentication, LocalDateTime.now(), "b-1", 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().isLiked()).isTrue();
    }

    @Test
    @DisplayName("cursor가 없으면 내 게시글 조회는 예외를 던진다")
    void getMyBoards_throwsWhenCursorTimeOnlyMissing() {
        assertThatThrownBy(() -> boardQueryService.getMyBoards(authentication, null, "b-1", 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("cursor 값이 한쪽만 있으면 예외가 발생한다")
    void getMyBoards_throwsWhenCursorMissing() {
        assertThatThrownBy(() -> boardQueryService.getMyBoards(authentication, LocalDateTime.now(), null, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("인증 정보가 없으면 좋아요/스크랩 조회를 생략한다")
    void getBoardById_skipsLikeAndScrapWhenUnauthenticated() {
        User user = User.builder().id("u-1").name("name").build();
        Board board = Board.builder().id("b-1").user(user).title("t").content("c").build();
        ReflectionTestUtils.setField(board, "createdAt", LocalDateTime.now());

        when(boardRepository.findById("b-1")).thenReturn(java.util.Optional.of(board));
        when(boardImageService.findByBoardId("b-1")).thenReturn(List.of());
        when(boardImageService.toImageUrls(List.of())).thenReturn(List.of());
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("b-1")).thenReturn(0L);

        var result = boardQueryService.getBoardById(authentication, "b-1");

        assertThat(result.isLiked()).isFalse();
        assertThat(result.isScrapped()).isFalse();
    }

    @Test
    @DisplayName("인증 principal이 anonymousUser이면 좋아요/스크랩 조회를 생략한다")
    void getBoardById_skipsLikeAndScrapWhenAnonymous() {
        User user = User.builder().id("u-1").name("name").build();
        Board board = Board.builder().id("b-1").user(user).title("t").content("c").build();
        ReflectionTestUtils.setField(board, "createdAt", LocalDateTime.now());

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");
        when(boardRepository.findById("b-1")).thenReturn(java.util.Optional.of(board));
        when(boardImageService.findByBoardId("b-1")).thenReturn(List.of());
        when(boardImageService.toImageUrls(List.of())).thenReturn(List.of());
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("b-1")).thenReturn(0L);

        var result = boardQueryService.getBoardById(authentication, "b-1");

        assertThat(result.isLiked()).isFalse();
        assertThat(result.isScrapped()).isFalse();
    }
}
