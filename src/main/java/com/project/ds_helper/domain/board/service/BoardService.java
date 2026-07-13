package com.project.ds_helper.domain.board.service;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.common.dto.response.PageResponseDto;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.board.dto.request.CreateBoardReqDto;
import com.project.ds_helper.domain.board.dto.request.UpdateBoardRequestDto;
import com.project.ds_helper.domain.board.dto.response.CreateBoardResponseDto;
import com.project.ds_helper.domain.board.dto.response.GetBoardByIdResponseDto;
import com.project.ds_helper.domain.board.dto.response.GetBoardsByCategoryResponseDto;
import com.project.ds_helper.domain.board.dto.response.GetMyBoardsResponseDto;
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
import com.project.ds_helper.domain.user.enums.UserRole;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private static final int MAX_BOARD_IMAGE_COUNT = 2;

    private final UserUtil userUtil;
    private final ImageCompressionUtil imageCompressionUtil;
    private final ImageUtil imageUtil;
    private final S3Util s3Util;
    private final BoardRepository boardRepository;
    private final BoardImageRepository boardImageRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final BoardScrapRepository boardScrapRepository;
    private final CommentRepository commentRepository;
    private final BoardQueryService boardQueryService;

    public CreateBoardResponseDto createBoard(Authentication authentication, @Valid CreateBoardReqDto dto, List<MultipartFile> images) throws IOException {
        if (images == null) {
            images = List.of();
        }

        log.debug("BoardService.createBoard started. title={}, category={}, imageCount={}",
                dto.getTitle(), dto.getCategory(), images.size());
        User user = userUtil.findUserById(userUtil.extractUserId(authentication));
        validateImageCount(images.size());

        Board newBoard = Board.builder()
                .user(user)
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(resolveBoardCategory(dto.getCategory()))
                .build();
        boardRepository.save(newBoard);
        log.debug("BoardService.createBoard board saved. boardId={}, userId={}", newBoard.getId(), user.getId());

        List<String> uploadedS3Keys = new ArrayList<>();

        try {
            ImageUploadBatch uploadBatch = prepareBoardImages(newBoard, images, uploadedS3Keys);
            persistBoardImages(uploadBatch);
            log.debug("BoardService.createBoard completed. boardId={}, uploadedImageCount={}",
                    newBoard.getId(), uploadBatch.boardImages().size());
            List<String> imageUrls = uploadBatch.boardImages().stream()
                    .map(BoardImage::getS3Key)
                    .map(s3Util::toS3UrlByS3Key)
                    .toList();
            String createdAt = newBoard.getCreatedAt() == null ? null : newBoard.getCreatedAt().toString();
            return new CreateBoardResponseDto(
                    newBoard.getId(),
                    newBoard.getTitle(),
                    newBoard.getContent(),
                    newBoard.getUser().getName(),
                    newBoard.getUser().getProfileImageUrl(),
                    newBoard.getViewCount(),
                    newBoard.getLikeCount(),
                    newBoard.getCommentCount(),
                    false,
                    false,
                    imageUrls,
                    createdAt
            );
        } catch (Exception e) {
            log.debug("BoardService.createBoard failed. boardId={}, uploadedKeyCount={}, errorType={}",
                    newBoard.getId(), uploadedS3Keys.size(), e.getClass().getSimpleName());
            rollbackUploadedImages(uploadedS3Keys);
            throw rethrowBoardWriteException(e);
        }
    }

    public GetBoardsByCategoryResponseDto getBoardsByCategory(Authentication authentication, String category, String keyword, int page, int size, String sort, String sortBy) {
        var pageable = org.springframework.data.domain.PageRequest.of(
                page,
                size,
                org.springframework.data.domain.Sort.by(sort.equalsIgnoreCase("desc") ? org.springframework.data.domain.Sort.Direction.DESC : org.springframework.data.domain.Sort.Direction.ASC, sortBy)
        );
        var boards = findBoardsByCategoryAndKeyword(category, keyword, pageable);
        return buildBoardListResponse(authentication, boards);
    }

    public GetBoardByIdResponseDto getBoardById(Authentication authentication, String boardId) {
        Board board = findActiveBoardById(boardId);
        boolean isLiked = false;
        boolean isScrapped = false;
        if (hasAuthenticatedUser(authentication)) {
            String userId = userUtil.extractUserId(authentication);
            isLiked = boardLikeRepository.existsByUser_IdAndBoard_Id(userId, boardId);
            isScrapped = boardScrapRepository.existsByUser_IdAndBoard_Id(userId, boardId);
        }
        List<String> boardImageUrls = boardImageRepository.findByBoard_Id(boardId).stream()
                .map(BoardImage::getS3Key)
                .map(s3Util::toS3UrlByS3Key)
                .toList();
        boardRepository.increaseViewCount(boardId);
        board.increaseViewCount();
        return GetBoardByIdResponseDto.toDto(board, toVisibleCommentCount(boardId), isLiked, isScrapped, boardImageUrls);
    }

    public CursorResponseDto<GetMyBoardsResponseDto> getMyBoards(Authentication authentication, LocalDateTime cursorTime, String cursorId, int size) {
        validateCursor(cursorTime, cursorId);
        String userId = userUtil.extractUserId(authentication);
        var pageable = org.springframework.data.domain.PageRequest.of(0, size + 1);
        List<Board> boards = boardRepository.findMyBoardsWithCursor(userId, cursorTime, cursorId, pageable);
        boolean hasNext = boards.size() > size;
        if (hasNext) {
            boards = boards.subList(0, size);
        }

        List<String> boardIds = boards.stream().map(Board::getId).toList();
        Set<String> likedBoardIds = boardIds.isEmpty() ? Set.of() : new HashSet<>(boardLikeRepository.findLikedBoardIdsByUserIdAndBoardIds(userId, boardIds));
        HashMap<String, String> thumbnails = buildThumbnailMapByBoardIds(boardIds);
        List<GetMyBoardsResponseDto> content = boards.stream()
                .map(board -> GetMyBoardsResponseDto.from(board, toVisibleCommentCount(board.getId()), likedBoardIds.contains(board.getId()), thumbnails.get(board.getId())))
                .toList();

        LocalDateTime nextCursorTime = null;
        String nextCursorId = null;
        if (!content.isEmpty()) {
            Board lastBoard = boards.get(boards.size() - 1);
            nextCursorTime = lastBoard.getCreatedAt();
            nextCursorId = lastBoard.getId();
        }

        return CursorResponseDto.toDto(content, nextCursorTime, nextCursorId, hasNext);
    }



    public void deleteBoard(Authentication authentication, String boardId) {
        log.debug("BoardService.deleteBoard started. boardId={}", boardId);

        String userId = userUtil.extractUserId(authentication);
        User requester = userUtil.findUserById(userId);
        Board board = findBoardById(boardId);
        validateBoardOwner(board, userId, requester, "Delete");

        board.softDelete();
        log.debug("BoardService.deleteBoard completed. boardId={}, requesterId={}", boardId, userId);
    }

    public void updateBoard(Authentication authentication, @Valid UpdateBoardRequestDto dto, List<MultipartFile> newImages) throws IOException {
        String boardId = dto.getBoardId();
        log.debug("BoardService.updateBoard started. boardId={}, keepImageCount={}, newImageCount={}",
                boardId,
                dto.getKeepImageUrls() == null ? 0 : dto.getKeepImageUrls().size(),
                newImages.size());

        // 1. 수정 대상 게시글을 조회하고 작성자 권한을 검증한다.
        Board board = loadBoardForUpdate(authentication, boardId);

        // 2. 제목/내용 수정처럼 이미지와 무관한 본문 변경을 먼저 반영한다.
        applyBoardUpdate(board, dto);

        // 3. 어떤 이미지를 유지/삭제/추가할지 사전 계획을 계산한다.
        BoardImageUpdatePlan updatePlan = createBoardImageUpdatePlan(boardId, dto.getKeepImageUrls(), newImages);

        List<String> uploadedS3Keys = new ArrayList<>();

        try {
            // 4. 삭제 대상 이미지를 정리하고 새 이미지를 업로드/저장한다.
            executeBoardImageUpdate(board, newImages, updatePlan, uploadedS3Keys);
            log.debug("BoardService.updateBoard completed. boardId={}, uploadedImageCount={}",
                    boardId, updatePlan.newImageCount());
        } catch (Exception e) {
            log.debug("BoardService.updateBoard failed. boardId={}, uploadedKeyCount={}, errorType={}",
                    boardId, uploadedS3Keys.size(), e.getClass().getSimpleName());

            // 5. 업로드 도중 실패했다면 이미 올라간 S3 파일만 롤백한다.
            rollbackUploadedImages(uploadedS3Keys);
            throw rethrowBoardWriteException(e);
        }
    }

    private Board loadBoardForUpdate(Authentication authentication, String boardId) {
        String userId = userUtil.extractUserId(authentication);
        log.debug("BoardService.loadBoardForUpdate started. boardId={}, requesterId={}", boardId, userId);

        Board board = findActiveBoardById(boardId);
        validateBoardOwner(board, userId, "Update");

        log.debug("BoardService.loadBoardForUpdate completed. boardId={}, writerId={}, requesterId={}",
                boardId, board.getUser().getId(), userId);
        return board;
    }

    private void applyBoardUpdate(Board board, UpdateBoardRequestDto dto) {
        log.debug("BoardService.applyBoardUpdate started. boardId={}, newTitleLength={}, newContentLength={}",
                board.getId(),
                dto.getTitle() == null ? 0 : dto.getTitle().length(),
                dto.getContent() == null ? 0 : dto.getContent().length());
        board.update(dto.getTitle(), dto.getContent());
        log.debug("BoardService.applyBoardUpdate completed. boardId={}", board.getId());
    }

    private BoardImageUpdatePlan createBoardImageUpdatePlan(String boardId, List<String> keepImageUrls, List<MultipartFile> newImages) {
        log.debug("BoardService.createBoardImageUpdatePlan started. boardId={}, keepImageCount={}, newImageCount={}",
                boardId,
                keepImageUrls == null ? 0 : keepImageUrls.size(),
                newImages.size());
        List<BoardImage> existingImages = boardImageRepository.findByBoard_Id(boardId);
        Set<String> keepS3Keys = resolveKeepS3Keys(keepImageUrls);
        List<BoardImage> deleteTargets = findDeleteTargets(existingImages, keepS3Keys);

        int remainCount = existingImages.size() - deleteTargets.size();
        validateImageCount(remainCount + newImages.size());
        log.debug("BoardService.updateBoard image plan resolved. boardId={}, existingImageCount={}, deleteTargetCount={}, remainCount={}",
                boardId, existingImages.size(), deleteTargets.size(), remainCount);
        return new BoardImageUpdatePlan(deleteTargets, newImages.size());
    }

    private void executeBoardImageUpdate(
            Board board,
            List<MultipartFile> newImages,
            BoardImageUpdatePlan updatePlan,
            List<String> uploadedS3Keys
    ) throws IOException {
        log.debug("BoardService.executeBoardImageUpdate started. boardId={}, deleteTargetCount={}, newImageCount={}",
                board.getId(), updatePlan.deleteTargets().size(), updatePlan.newImageCount());
        deleteBoardImages(updatePlan.deleteTargets());
        ImageUploadBatch uploadBatch = prepareBoardImages(board, newImages, uploadedS3Keys);
        persistBoardImages(uploadBatch);
        log.debug("BoardService.executeBoardImageUpdate completed. boardId={}, persistedImageCount={}, uploadedKeyCount={}",
                board.getId(), uploadBatch.boardImages().size(), uploadedS3Keys.size());
    }

    private Set<String> resolveKeepS3Keys(List<String> keepImageUrls) {
        Set<String> keepS3Keys = keepImageUrls == null ? Set.of() : keepImageUrls.stream()
                .map(s3Util::extractS3KeyFromS3Url)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        log.debug("BoardService.resolveKeepS3Keys completed. keepKeyCount={}", keepS3Keys.size());
        return keepS3Keys;
    }

    private void validateBoardOwner(Board board, String requesterId, User requester, String action) {
        if (requester.getRole() == UserRole.ADMIN) {
            return;
        }

        String writerId = board.getUser().getId();
        if (!writerId.equals(requesterId)) {
            log.debug("BoardService.validateBoardOwner denied. boardId={}, action={}, writerId={}, requesterId={}",
                    board.getId(), action, writerId, requesterId);
            throw new IllegalStateException(
                    String.format("No Authority To %s Board. writerId : %s, requester : %s", action, writerId, requesterId)
            );
        }
    }

    private void validateBoardOwner(Board board, String requesterId, String action) {
        String writerId = board.getUser().getId();
        if (!writerId.equals(requesterId)) {
            log.debug("BoardService.validateBoardOwner denied. boardId={}, action={}, writerId={}, requesterId={}",
                    board.getId(), action, writerId, requesterId);
            throw new IllegalStateException(
                    String.format("No Authority To %s Board. writerId : %s, requester : %s", action, writerId, requesterId)
            );
        }
    }

    private void validateImageCount(int imageCount) {
        log.debug("BoardService.validateImageCount called. imageCount={}, maxImageCount={}", imageCount, MAX_BOARD_IMAGE_COUNT);
        if (imageCount > MAX_BOARD_IMAGE_COUNT) {
            throw new IllegalArgumentException(String.format("Images Size Exceed Limitation : %d", imageCount));
        }
    }

    private Board findActiveBoardById(String boardId) {
        Board board = findBoardById(boardId);
        if (board.isDeleted()) {
            throw new IllegalStateException("Deleted Board");
        }
        return board;
    }

    private String resolveBoardCategory(String category) {
        return BoardCategory.findByKorean(category).name();
    }

    private List<BoardImage> findDeleteTargets(List<BoardImage> existingImages, Set<String> keepS3Keys) {
        return existingImages.stream()
                .filter(image -> !keepS3Keys.contains(image.getS3Key()))
                .toList();
    }

    private void deleteBoardImages(List<BoardImage> deleteTargets) {
        if (deleteTargets.isEmpty()) {
            log.debug("BoardService.deleteBoardImages skipped. deleteTargetCount=0");
            return;
        }

        log.debug("BoardService.deleteBoardImages deleting. deleteTargetCount={}", deleteTargets.size());
        boardImageRepository.deleteAllInBatch(deleteTargets);
        s3Util.deleteImagesByS3Key(deleteTargets.stream().map(BoardImage::getS3Key).toList());
    }

    private ImageUploadBatch prepareBoardImages(Board board, List<MultipartFile> images, List<String> uploadedS3Keys) throws IOException {
        if (images.isEmpty()) {
            log.debug("BoardService.prepareBoardImages skipped. boardId={}, imageCount=0", board.getId());
            return new ImageUploadBatch(List.of(), List.of());
        }

        log.debug("BoardService.prepareBoardImages started. boardId={}, imageCount={}", board.getId(), images.size());
        List<S3ImageUploadRequestDto> uploadRequests = new ArrayList<>(images.size());
        List<BoardImage> boardImages = new ArrayList<>(images.size());

        for (MultipartFile image : images) {
            String storedFilename = imageUtil.toStoredFilename();
            S3ImageUploadRequestDto compressedImage = imageCompressionUtil.compressImage(image, storedFilename);
            String contentType = compressedImage.getFileExtension();
            String s3Key = s3Util.buildS3Key(storedFilename);
            uploadedS3Keys.add(s3Key);
            uploadRequests.add(compressedImage);
            boardImages.add(BoardImage.builder()
                    .board(board)
                    .contentType(contentType)
                    .size(compressedImage.getSize())
                    .originalName(compressedImage.getOriginalFilename())
                    .storedName(storedFilename)
                    .s3Key(s3Key)
                    .build());
        }

        log.debug("BoardService.prepareBoardImages completed. boardId={}, preparedImageCount={}", board.getId(), boardImages.size());
        return new ImageUploadBatch(uploadRequests, boardImages);
    }

    private void persistBoardImages(ImageUploadBatch uploadBatch) throws IOException {
        if (uploadBatch.boardImages().isEmpty()) {
            log.debug("BoardService.persistBoardImages skipped. imageCount=0");
            return;
        }

        log.debug("BoardService.persistBoardImages started. imageCount={}", uploadBatch.boardImages().size());
        boardImageRepository.saveAll(uploadBatch.boardImages());
        s3Util.uploadImages(uploadBatch.uploadRequests());
        log.debug("BoardService.persistBoardImages completed. imageCount={}", uploadBatch.boardImages().size());
    }

    private void rollbackUploadedImages(List<String> uploadedS3Keys) {
        if (!uploadedS3Keys.isEmpty()) {
            log.debug("BoardService.rollbackUploadedImages started. uploadedKeyCount={}", uploadedS3Keys.size());
            s3Util.deleteImagesByS3Key(uploadedS3Keys);
        }
    }

    private RuntimeException rethrowBoardWriteException(Exception e) throws IOException {
        if (e instanceof IOException ioException) {
            throw ioException;
        }
        if (e instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(e);
    }

    public Board findBoardById(String boardId) {
        log.debug("BoardService.findBoardById called. boardId={}", boardId);
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Board Not Found %s", boardId)));
    }

    private int toVisibleCommentCount(String boardId) {
        return Math.toIntExact(commentRepository.countByBoard_IdAndIsDeletedFalse(boardId));
    }

    private boolean hasAuthenticatedUser(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() != null && !"anonymousUser".equals(authentication.getPrincipal());
    }

    private boolean isAllCategory(String category) {
        return "전체".equals(category);
    }

    private void validateCursor(LocalDateTime cursorTime, String cursorId) {
        if ((cursorTime == null) != (cursorId == null)) {
            throw new IllegalArgumentException("cursorTime and cursorId must be provided together");
        }
    }

    private org.springframework.data.domain.Page<Board> findBoardsByCategoryAndKeyword(String category, String keyword, org.springframework.data.domain.Pageable pageable) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (isAllCategory(category)) {
            return hasKeyword ? boardRepository.findByTitleContainingAndIsDeletedFalse(keyword, pageable) : boardRepository.findByIsDeletedFalse(pageable);
        }
        String resolvedCategory = BoardCategory.findByKorean(category).name();
        return hasKeyword ? boardRepository.findByCategoryAndTitleContainingAndIsDeletedFalse(resolvedCategory, keyword, pageable) : boardRepository.findByCategoryAndIsDeletedFalse(resolvedCategory, pageable);
    }

    private GetBoardsByCategoryResponseDto buildBoardListResponse(Authentication authentication, org.springframework.data.domain.Page<Board> boards) {
        List<String> boardIds = boards.getContent().stream().map(Board::getId).toList();
        Set<String> likedBoardIds = !hasAuthenticatedUser(authentication) || boardIds.isEmpty()
                ? Set.of()
                : new HashSet<>(boardLikeRepository.findLikedBoardIdsByUserIdAndBoardIds(userUtil.extractUserId(authentication), boardIds));
        HashMap<String, String> thumbnails = buildThumbnailMapByBoardIds(boardIds);
        List<GetBoardsByCategoryResponseDto.Board> boardsDto = boards.getContent().stream()
                .map(board -> GetBoardsByCategoryResponseDto.Board.toBoard(board, toVisibleCommentCount(board.getId()), likedBoardIds.contains(board.getId()), thumbnails.get(board.getId())))
                .toList();
        return new GetBoardsByCategoryResponseDto(boardsDto, PageResponseDto.toDto(boards));
    }

    private HashMap<String, String> buildThumbnailMapByBoardIds(List<String> boardIds) {
        HashMap<String, String> thumbnails = new HashMap<>();
        if (boardIds.isEmpty()) {
            return thumbnails;
        }
        boardImageRepository.findBoardThumbnailsByBoardIds(boardIds)
                .forEach(image -> thumbnails.put(image.getBoard().getId(), s3Util.toS3UrlByS3Key(image.getS3Key())));
        return thumbnails;
    }

    private record ImageUploadBatch(
            List<S3ImageUploadRequestDto> uploadRequests,
            List<BoardImage> boardImages
    ) {
    }

    private record BoardImageUpdatePlan(
            List<BoardImage> deleteTargets,
            int newImageCount
    ) {
    }
}
