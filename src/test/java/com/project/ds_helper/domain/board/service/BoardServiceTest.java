package com.project.ds_helper.domain.board.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.board.dto.request.CreateBoardReqDto;
import com.project.ds_helper.domain.board.dto.request.UpdateBoardRequestDto;
import com.project.ds_helper.domain.board.dto.response.GetBoardByIdResponseDto;
import com.project.ds_helper.domain.board.dto.response.GetBoardsByCategoryResponseDto;
import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.board.entity.BoardImage;
import com.project.ds_helper.domain.board.enums.BoardCategory;
import com.project.ds_helper.domain.board.repository.BoardImageRepository;
import com.project.ds_helper.domain.board.repository.BoardLikeRepository;
import com.project.ds_helper.domain.board.repository.BoardRepository;
import com.project.ds_helper.domain.board.repository.BoardScrapRepository;
import com.project.ds_helper.domain.comment.repository.CommentRepository;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.ImageUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoardServiceTest {

    @Mock
    private UserUtil userUtil;

    @Mock
    private ImageUtil imageUtil;

    @Mock
    private ImageCompressionUtil imageCompressionUtil;

    @Mock
    private S3Util s3Util;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardImageRepository boardImageRepository;

    @Mock
    private BoardLikeRepository boardLikeRepository;

    @Mock
    private BoardScrapRepository boardScrapRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BoardService boardService;

    @BeforeEach
    void setUpAuthentication() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("user-1");
    }

    @Test
    @DisplayName("게시글 생성 시 게시글과 이미지 정보를 저장한다")
    void createBoard_savesBoardAndImages() throws Exception {
        User user = user("user-1");
        CreateBoardReqDto dto = new CreateBoardReqDto(BoardCategory.DAILY.getKorean(), "제목", "내용");
        MockMultipartFile image = new MockMultipartFile("images", "a.png", "image/png", "hello".getBytes());

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(imageUtil.toStoredFilename()).thenReturn("stored-file");
        when(imageCompressionUtil.compressImage(image, "stored-file"))
                .thenReturn(compressedImage("stored-file", "a.png", "png", "hello".getBytes()));
        when(s3Util.buildS3Key("stored-file")).thenReturn("boards/stored-file");

        boardService.createBoard(authentication, dto, List.of(image));

        ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepository).save(boardCaptor.capture());
        verify(boardImageRepository).saveAll(any());
        verify(s3Util).uploadImages(any());

        Board savedBoard = boardCaptor.getValue();
        assertThat(savedBoard.getTitle()).isEqualTo("제목");
        assertThat(savedBoard.getContent()).isEqualTo("내용");
        assertThat(savedBoard.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("게시글 생성 시 이미지 개수가 제한을 초과하면 예외가 발생한다")
    void createBoard_throwsWhenImageCountExceeded() {
        User user = user("user-1");
        CreateBoardReqDto dto = new CreateBoardReqDto(BoardCategory.DAILY.getKorean(), "제목", "내용");
        MockMultipartFile image1 = new MockMultipartFile("images", "a.png", "image/png", "a".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "b.png", "image/png", "b".getBytes());
        MockMultipartFile image3 = new MockMultipartFile("images", "c.png", "image/png", "c".getBytes());

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        assertThatThrownBy(() -> boardService.createBoard(authentication, dto, List.of(image1, image2, image3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Images Size Exceed Limitation");
    }

    @Test
    @DisplayName("이미지 업로드 중 예외가 발생하면 업로드한 S3 키를 롤백한다")
    void createBoard_rollsBackUploadedImagesWhenUploadFails() throws Exception {
        User user = user("user-1");
        CreateBoardReqDto dto = new CreateBoardReqDto(BoardCategory.DAILY.getKorean(), "제목", "내용");
        MockMultipartFile image = new MockMultipartFile("images", "a.png", "image/png", "hello".getBytes());

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(imageUtil.toStoredFilename()).thenReturn("stored-file");
        when(imageCompressionUtil.compressImage(image, "stored-file"))
                .thenReturn(compressedImage("stored-file", "a.png", "png", "hello".getBytes()));
        when(s3Util.buildS3Key("stored-file")).thenReturn("boards/stored-file");
        doThrow(new IOException("upload fail")).when(s3Util).uploadImages(any());

        assertThatThrownBy(() -> boardService.createBoard(authentication, dto, List.of(image)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("upload fail");

        verify(s3Util).deleteImagesByS3Key(List.of("boards/stored-file"));
    }

    @Test
    @DisplayName("카테고리별 게시글 조회 시 썸네일 URL과 좋아요 여부를 포함한다")
    void getBoardsByCategory_returnsThumbnailUrlAndLikeState() {
        User user = user("user-1");
        Board board = board("board-1", false, user);
        BoardImage image = BoardImage.builder().board(board).s3Key("s3/key.png").build();
        Page<Board> page = new PageImpl<>(List.of(board));

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(boardRepository.findByCategoryAndIsDeletedFalse(eq(BoardCategory.DAILY.name()), any(Pageable.class)))
                .thenReturn(page);
        when(boardLikeRepository.findLikedBoardIdsByUserIdAndBoardIds("user-1", List.of("board-1")))
                .thenReturn(List.of("board-1"));
        when(boardImageRepository.findBoardThumbnailsByBoardIds(List.of("board-1"))).thenReturn(List.of(image));
        when(s3Util.toS3UrlByS3Key("s3/key.png")).thenReturn("https://cdn.example.com/s3/key.png");
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("board-1")).thenReturn(0L);

        GetBoardsByCategoryResponseDto result =
                boardService.getBoardsByCategory(authentication, BoardCategory.DAILY.getKorean(), null, 0, 10, "desc", "createdAt");

        assertThat(result.getBoards()).hasSize(1);
        assertThat(result.getBoards().getFirst().getThumbNailUrl()).isEqualTo("https://cdn.example.com/s3/key.png");
        assertThat(result.getBoards().getFirst().isLiked()).isTrue();
    }

    @Test
    @DisplayName("비로그인 사용자의 게시글 목록 조회는 좋아요 여부를 false로 반환한다")
    void getBoardsByCategory_returnsFalseLikeStateForAnonymousUser() {
        User user = user("user-1");
        Board board = board("board-1", false, user);
        BoardImage image = BoardImage.builder().board(board).s3Key("s3/key.png").build();
        Page<Board> page = new PageImpl<>(List.of(board));

        when(boardRepository.findByCategoryAndIsDeletedFalse(eq(BoardCategory.DAILY.name()), any(Pageable.class)))
                .thenReturn(page);
        when(boardImageRepository.findBoardThumbnailsByBoardIds(List.of("board-1"))).thenReturn(List.of(image));
        when(s3Util.toS3UrlByS3Key("s3/key.png")).thenReturn("https://cdn.example.com/s3/key.png");
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("board-1")).thenReturn(0L);

        GetBoardsByCategoryResponseDto result =
                boardService.getBoardsByCategory(null, BoardCategory.DAILY.getKorean(), null, 0, 10, "desc", "createdAt");

        assertThat(result.getBoards()).hasSize(1);
        assertThat(result.getBoards().getFirst().isLiked()).isFalse();
    }

    @Test
    @DisplayName("게시글 목록 조회 시 keyword가 있으면 제목 LIKE 검색을 수행한다")
    void getBoardsByCategory_searchesByKeyword() {
        User user = user("user-1");
        Board board = board("board-2", false, user);
        BoardImage image = BoardImage.builder().board(board).s3Key("s3/search.png").build();
        Page<Board> page = new PageImpl<>(List.of(board));

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(boardRepository.findByTitleContainingAndIsDeletedFalse(eq("검색"), any(Pageable.class)))
                .thenReturn(page);
        when(boardLikeRepository.findLikedBoardIdsByUserIdAndBoardIds("user-1", List.of("board-2")))
                .thenReturn(List.of("board-2"));
        when(boardImageRepository.findBoardThumbnailsByBoardIds(List.of("board-2"))).thenReturn(List.of(image));
        when(s3Util.toS3UrlByS3Key("s3/search.png")).thenReturn("https://cdn.example.com/s3/search.png");
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("board-2")).thenReturn(0L);

        GetBoardsByCategoryResponseDto result =
                boardService.getBoardsByCategory(authentication, "전체", "검색", 0, 10, "desc", "createdAt");

        assertThat(result.getBoards()).hasSize(1);
        assertThat(result.getBoards().getFirst().isLiked()).isTrue();
        assertThat(result.getBoards().getFirst().getThumbNailUrl()).isEqualTo("https://cdn.example.com/s3/search.png");
    }

    @Test
    @DisplayName("게시글 목록 조회 시 카테고리와 keyword를 함께 검색할 수 있다")
    void getBoardsByCategory_searchesByCategoryAndKeyword() {
        User user = user("user-1");
        Board board = board("board-3", false, user);
        Page<Board> page = new PageImpl<>(List.of(board));

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(boardRepository.findByCategoryAndTitleContainingAndIsDeletedFalse(
                eq(BoardCategory.DAILY.name()),
                eq("검색"),
                any(Pageable.class)
        )).thenReturn(page);
        when(boardLikeRepository.findLikedBoardIdsByUserIdAndBoardIds("user-1", List.of("board-3")))
                .thenReturn(List.of());
        when(boardImageRepository.findBoardThumbnailsByBoardIds(List.of("board-3"))).thenReturn(List.of());
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("board-3")).thenReturn(0L);

        GetBoardsByCategoryResponseDto result =
                boardService.getBoardsByCategory(authentication, BoardCategory.DAILY.getKorean(), "검색", 0, 10, "desc", "createdAt");

        assertThat(result.getBoards()).hasSize(1);
        assertThat(result.getBoards().getFirst().isLiked()).isFalse();
    }

    @Test
    @DisplayName("게시글 상세 조회 시 좋아요, 스크랩, 이미지 URL을 포함한다")
    void getBoardById_returnsDetailDto() {
        User user = user("user-1");
        Board board = board("board-1", false, user);
        BoardImage image = BoardImage.builder().board(board).s3Key("s3/key.png").build();
        ReflectionTestUtils.setField(board, "createdAt", LocalDateTime.of(2026, 4, 9, 12, 0, 0));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("user-1");
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));
        when(boardLikeRepository.existsByUser_IdAndBoard_Id("user-1", "board-1")).thenReturn(true);
        when(boardScrapRepository.existsByUser_IdAndBoard_Id("user-1", "board-1")).thenReturn(false);
        when(boardImageRepository.findByBoard_Id("board-1")).thenReturn(List.of(image));
        when(s3Util.toS3UrlByS3Key("s3/key.png")).thenReturn("https://cdn.example.com/s3/key.png");
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("board-1")).thenReturn(0L);

        GetBoardByIdResponseDto result = boardService.getBoardById(authentication, "board-1");

        assertThat(result.id()).isEqualTo("board-1");
        assertThat(result.isLiked()).isTrue();
        assertThat(result.imageUrls()).containsExactly("https://cdn.example.com/s3/key.png");
        assertThat(board.getViewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("비로그인 사용자의 게시글 상세 조회는 좋아요와 스크랩 여부를 false로 반환한다")
    void getBoardById_returnsFalseStatesForAnonymousUser() {
        User user = user("user-1");
        Board board = board("board-1", false, user);
        BoardImage image = BoardImage.builder().board(board).s3Key("s3/key.png").build();
        ReflectionTestUtils.setField(board, "createdAt", LocalDateTime.of(2026, 4, 9, 12, 0, 0));

        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));
        when(boardImageRepository.findByBoard_Id("board-1")).thenReturn(List.of(image));
        when(s3Util.toS3UrlByS3Key("s3/key.png")).thenReturn("https://cdn.example.com/s3/key.png");
        when(commentRepository.countByBoard_IdAndIsDeletedFalse("board-1")).thenReturn(0L);

        GetBoardByIdResponseDto result = boardService.getBoardById(null, "board-1");

        assertThat(result.id()).isEqualTo("board-1");
        assertThat(result.isLiked()).isFalse();
        assertThat(result.isScrapped()).isFalse();
        assertThat(result.imageUrls()).containsExactly("https://cdn.example.com/s3/key.png");
    }

    @Test
    @DisplayName("삭제된 게시글은 상세 조회할 수 없다")
    void getBoardById_throwsWhenDeleted() {
        Board board = board("board-1", true, user("user-1"));

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));

        assertThatThrownBy(() -> boardService.getBoardById(authentication, "board-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Deleted Board");
    }

    @Test
    @DisplayName("게시글 삭제는 작성자만 수행할 수 있다")
    void deleteBoard_throwsWhenRequesterIsNotOwner() {
        Board board = board("board-1", false, user("writer-1"));
        User requester = user("user-1");

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(requester);
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));

        assertThatThrownBy(() -> boardService.deleteBoard(authentication, "board-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No Authority");
    }

    @Test
    @DisplayName("관리자는 작성자가 아니어도 게시글을 삭제할 수 있다")
    void deleteBoard_allowsAdmin() {
        Board board = board("board-1", false, user("writer-1"));
        User admin = user("admin-1");
        admin.setRole(com.project.ds_helper.domain.user.enums.UserRole.ADMIN);

        when(userUtil.extractUserId(authentication)).thenReturn("admin-1");
        when(userUtil.findUserById("admin-1")).thenReturn(admin);
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));

        boardService.deleteBoard(authentication, "board-1");

        assertThat(board.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("게시글 수정 시 유지하지 않는 이미지를 삭제하고 새 이미지를 저장한다")
    void updateBoard_deletesRemovedImagesAndUploadsNewOnes() throws Exception {
        User user = user("user-1");
        Board board = board("board-1", false, user);
        BoardImage keepImage = BoardImage.builder().board(board).s3Key("s3/keep.png").build();
        BoardImage deleteImage = BoardImage.builder().board(board).s3Key("s3/delete.png").build();
        UpdateBoardRequestDto dto = new UpdateBoardRequestDto(
                "board-1",
                "수정 제목",
                "수정 내용",
                List.of("https://cdn.example.com/s3/keep.png")
        );
        MockMultipartFile newImage = new MockMultipartFile("images", "new.png", "image/png", "new".getBytes());

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(boardRepository.findById("board-1")).thenReturn(Optional.of(board));
        when(boardImageRepository.findByBoard_Id("board-1")).thenReturn(List.of(keepImage, deleteImage));
        when(s3Util.toS3UrlByS3Key("s3/keep.png")).thenReturn("https://cdn.example.com/s3/keep.png");
        when(s3Util.toS3UrlByS3Key("s3/delete.png")).thenReturn("https://cdn.example.com/s3/delete.png");
        when(imageUtil.toStoredFilename()).thenReturn("stored-new");
        when(imageCompressionUtil.compressImage(newImage, "stored-new"))
                .thenReturn(compressedImage("stored-new", "new.png", "png", "new".getBytes()));
        when(s3Util.buildS3Key("stored-new")).thenReturn("s3/new.png");

        boardService.updateBoard(authentication, dto, List.of(newImage));

        assertThat(board.getTitle()).isEqualTo("수정 제목");
        assertThat(board.getContent()).isEqualTo("수정 내용");
        verify(boardImageRepository).deleteAllInBatch(List.of(deleteImage));
        verify(s3Util).deleteImagesByS3Key(List.of("s3/delete.png"));
        verify(boardImageRepository).saveAll(any());
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 예외가 발생한다")
    void findBoardById_throwsWhenBoardMissing() {
        when(boardRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.findBoardById("missing"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Board Not Found");
    }

    private User user(String id) {
        return User.builder().id(id).name("tester").profileImageUrl("https://cdn.example.com/profile.png").build();
    }

    private Board board(String id, boolean deleted, User user) {
        return Board.builder()
                .id(id)
                .user(user)
                .category("FREE")
                .title("title")
                .content("content")
                .isDeleted(deleted)
                .build();
    }

    private com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto compressedImage(
            String storedFilename,
            String originalFilename,
            String fileExtension,
            byte[] bytes
    ) {
        return com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto.builder()
                .storedFilename(storedFilename)
                .originalFilename(originalFilename)
                .bytes(bytes)
                .size((long) bytes.length)
                .s3ContentType("image/" + fileExtension)
                .fileExtension(fileExtension)
                .build();
    }
}
