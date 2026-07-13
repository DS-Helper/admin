package com.project.ds_helper.domain.board.service;

import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.common.dto.response.PageResponseDto;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.board.dto.response.GetBoardByIdResponseDto;
import com.project.ds_helper.domain.board.dto.response.GetBoardsByCategoryResponseDto;
import com.project.ds_helper.domain.board.dto.response.GetMyBoardsResponseDto;
import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.board.enums.BoardCategory;
import com.project.ds_helper.domain.board.repository.BoardLikeRepository;
import com.project.ds_helper.domain.board.repository.BoardRepository;
import com.project.ds_helper.domain.board.repository.BoardScrapRepository;
import com.project.ds_helper.domain.comment.repository.CommentRepository;
import com.project.ds_helper.domain.post.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoardQueryService {

    private final UserUtil userUtil;
    private final BoardRepository boardRepository;
    private final BoardImageService boardImageService;
    private final BoardLikeRepository boardLikeRepository;
    private final BoardScrapRepository boardScrapRepository;
    private final CommentRepository commentRepository;
    private final S3Util s3Util;

    @Transactional(readOnly = true)
    public GetBoardsByCategoryResponseDto getBoardsByCategory(Authentication authentication, String category, String keyword, int page, int size, String sort, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));
        Page<Board> boards = findBoardsByCategoryAndKeyword(category, keyword, pageable);
        return buildBoardListResponse(authentication, boards);
    }

    @Transactional(readOnly = true)
    public GetBoardByIdResponseDto getBoardById(Authentication authentication, String boardId) {
        Board board = findActiveBoardById(boardId);
        boolean isLiked = false;
        boolean isScrapped = false;
        if (hasAuthenticatedUser(authentication)) {
            String userId = userUtil.extractUserId(authentication);
            isLiked = boardLikeRepository.existsByUser_IdAndBoard_Id(userId, boardId);
            isScrapped = boardScrapRepository.existsByUser_IdAndBoard_Id(userId, boardId);
        }
        List<String> boardImageUrls = boardImageService.toImageUrls(boardImageService.findByBoardId(boardId));
        boardRepository.increaseViewCount(boardId);
        board.increaseViewCount();
        return GetBoardByIdResponseDto.toDto(board, toVisibleCommentCount(boardId), isLiked, isScrapped, boardImageUrls);
    }

    @Transactional(readOnly = true)
    public CursorResponseDto<GetMyBoardsResponseDto> getMyBoards(Authentication authentication, LocalDateTime cursorTime, String cursorId, int size) {
        validateCursor(cursorTime, cursorId);
        String userId = userUtil.extractUserId(authentication);
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Board> boards = boardRepository.findMyBoardsWithCursor(userId, cursorTime, cursorId, pageable);
        boolean hasNext = boards.size() > size;
        if (hasNext) {
            boards = boards.subList(0, size);
        }

        List<String> boardIds = boards.stream().map(Board::getId).toList();
        Set<String> likedBoardIds = boardIds.isEmpty() ? Set.of() : new HashSet<>(boardLikeRepository.findLikedBoardIdsByUserIdAndBoardIds(userId, boardIds));
        HashMap<String, String> thumbnails = boardImageService.buildThumbnailMapByBoardIds(boardIds);
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

    private GetBoardsByCategoryResponseDto buildBoardListResponse(Authentication authentication, Page<Board> boards) {
        List<String> boardIds = boards.getContent().stream().map(Board::getId).toList();
        Set<String> likedBoardIds = !hasAuthenticatedUser(authentication) || boardIds.isEmpty()
                ? Set.of()
                : new HashSet<>(boardLikeRepository.findLikedBoardIdsByUserIdAndBoardIds(userUtil.extractUserId(authentication), boardIds));
        HashMap<String, String> thumbnails = boardImageService.buildThumbnailMapByBoardIds(boardIds);
        List<GetBoardsByCategoryResponseDto.Board> boardsDto = boards.getContent().stream()
                .map(board -> GetBoardsByCategoryResponseDto.Board.toBoard(board, toVisibleCommentCount(board.getId()), likedBoardIds.contains(board.getId()), thumbnails.get(board.getId())))
                .toList();
        return new GetBoardsByCategoryResponseDto(boardsDto, PageResponseDto.toDto(boards));
    }

    private Page<Board> findBoardsByCategoryAndKeyword(String category, String keyword, Pageable pageable) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (isAllCategory(category)) {
            return hasKeyword ? boardRepository.findByTitleContainingAndIsDeletedFalse(keyword, pageable) : boardRepository.findByIsDeletedFalse(pageable);
        }
        String resolvedCategory = BoardCategory.findByKorean(category).name();
        return hasKeyword ? boardRepository.findByCategoryAndTitleContainingAndIsDeletedFalse(resolvedCategory, keyword, pageable) : boardRepository.findByCategoryAndIsDeletedFalse(resolvedCategory, pageable);
    }

    private Board findActiveBoardById(String boardId) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("Board Not Found " + boardId));
        if (board.isDeleted()) {
            throw new IllegalStateException("Deleted Board");
        }
        return board;
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

    private int toVisibleCommentCount(String boardId) {
        return Math.toIntExact(commentRepository.countByBoard_IdAndIsDeletedFalse(boardId));
    }
}
