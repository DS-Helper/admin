package com.project.ds_helper.domain.board.service;

import com.project.ds_helper.common.dto.response.PageResponseDto;
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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoardScrapService {

    private final UserUtil userUtil;
    private final BoardRepository boardRepository;
    private final BoardScrapRepository boardScrapRepository;
    private final BoardImageRepository boardImageRepository;
    private final S3Util s3Util;
    private final CommentRepository commentRepository;

    @Transactional
    public boolean toggleBoardScrap(Authentication authentication, String boardId) {
        log.debug("BoardScrapService.toggleBoardScrap started. boardId={}", boardId);
        String userId = userUtil.extractUserId(authentication);
        User user = userUtil.findUserById(userId);
        Board board = findBoardById(boardId);

        validateBoardNotDeleted(board);

        return boardScrapRepository.findByUser_IdAndBoard_Id(userId, boardId)
                .map(existingScrap -> {
                    log.debug("BoardScrapService.toggleBoardScrap removing scrap. boardId={}, userId={}", boardId, userId);
                    boardScrapRepository.delete(existingScrap);
                    return false;
                })
                .orElseGet(() -> {
                    log.debug("BoardScrapService.toggleBoardScrap creating scrap. boardId={}, userId={}", boardId, userId);
                    BoardScrap boardScrap = BoardScrap.builder()
                            .board(board)
                            .user(user)
                            .build();

                    boardScrapRepository.save(boardScrap);
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public GetScrappedBoardsResponseDto getMyScrappedBoards(
            Authentication authentication,
            int page,
            int size,
            String sort,
            String sortBy
    ) {
        String userId = userUtil.extractUserId(authentication);
        log.debug("BoardScrapService.getMyScrappedBoards started. userId={}, page={}, size={}, sort={}, sortBy={}",
                userId, page, size, sort, sortBy);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sort.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy)
        );

        Page<Board> boardPage = boardScrapRepository.findScrappedBoards(userId, pageable);
        List<Board> boards = boardPage.getContent();
        log.debug("BoardScrapService.getMyScrappedBoards boards fetched. boardCount={}, totalElements={}",
                boards.size(), boardPage.getTotalElements());

        if (boards.isEmpty()) {
            log.debug("BoardScrapService.getMyScrappedBoards completed with empty result. userId={}", userId);
            return GetScrappedBoardsResponseDto.builder()
                    .boards(List.of())
                    .page(PageResponseDto.toDto(boardPage))
                    .build();
        }

        List<String> boardIds = boards.stream().map(Board::getId).toList();
        List<BoardImage> images = boardImageRepository.findBoardThumbnailsByBoardIds(boardIds);

        Map<String, String> thumbnailMap = new HashMap<>();
        for (BoardImage image : images) {
            thumbnailMap.putIfAbsent(
                    image.getBoard().getId(),
                    s3Util.toS3UrlByS3Key(image.getS3Key())
            );
        }

        List<GetScrappedBoardsResponseDto.Board> responseBoards = boards.stream()
                .map(board -> GetScrappedBoardsResponseDto.Board.toBoard(
                        board,
                        toVisibleCommentCount(board.getId()),
                        thumbnailMap.get(board.getId())
                ))
                .toList();
        log.debug("BoardScrapService.getMyScrappedBoards completed. responseBoardCount={}, thumbnailCount={}",
                responseBoards.size(), thumbnailMap.size());

        return GetScrappedBoardsResponseDto.builder()
                .boards(responseBoards)
                .page(PageResponseDto.toDto(boardPage))
                .build();
    }

    @Transactional(readOnly = true)
    public long countScrap(String boardId) {
        log.debug("BoardScrapService.countScrap called. boardId={}", boardId);
        return boardScrapRepository.countByBoard_Id(boardId);
    }

    private void validateBoardNotDeleted(Board board) {
        if (board.isDeleted()) {
            log.debug("BoardScrapService.validateBoardNotDeleted found deleted board. boardId={}", board.getId());
            throw new IllegalStateException("Deleted Board");
        }
    }

    private Board findBoardById(String boardId) {
        log.debug("BoardScrapService.findBoardById called. boardId={}", boardId);
        return boardRepository.findById(boardId)
                .orElseThrow(() ->
                        new EntityNotFoundException(String.format("Board Not Found : %s", boardId))
                );
    }

    private int toVisibleCommentCount(String boardId) {
        return Math.toIntExact(commentRepository.countByBoard_IdAndIsDeletedFalse(boardId));
    }
}
