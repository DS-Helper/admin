package com.project.ds_helper.domain.board.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.board.entity.BoardLike;
import com.project.ds_helper.domain.board.repository.BoardLikeRepository;
import com.project.ds_helper.domain.board.repository.BoardRepository;
import com.project.ds_helper.domain.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoardLikeService {

    private final UserUtil userUtil;
    private final BoardRepository boardRepository;
    private final BoardLikeRepository boardLikeRepository;

    @Transactional
    public boolean toggleBoardLike(Authentication authentication, String boardId) {
        log.debug("BoardLikeService.toggleBoardLike started. boardId={}", boardId);

        String userId = userUtil.extractUserId(authentication);
        User user = userUtil.findUserById(userId);
        Board board = findBoardById(boardId);

        checkIfBoardDeleted(board);

        return boardLikeRepository.findByUser_IdAndBoard_Id(userId, boardId)
                .map(existingLike -> {
                    log.debug("BoardLikeService.toggleBoardLike removing like. boardId={}, userId={}", boardId, userId);
                    boardLikeRepository.delete(existingLike);
                    decreaseLikeCount(boardId);
                    return false;
                })
                .orElseGet(() -> {
                    log.debug("BoardLikeService.toggleBoardLike creating like. boardId={}, userId={}", boardId, userId);
                    BoardLike boardLike = BoardLike.builder()
                            .board(board)
                            .user(user)
                            .build();

                    boardLikeRepository.save(boardLike);
                    increaseLikeCount(boardId);
                    return true;
                });
    }

//    @Transactional(readOnly = true)
//    public GetLikedBoardsResponseDto getMyLikedBoards(
//            Authentication authentication,
//            int page,
//            int size,
//            String sort,
//            String sortBy
//    ) {
//        String userId = userUtil.extractUserId(authentication);
//
//        Pageable pageable = PageRequest.of(
//                page,
//                size,
//                Sort.by(sort.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy)
//        );
//
//        Page<Board> boardPage = boardLikeRepository.findLikedBoards(userId, pageable);
//        List<Board> boards = boardPage.getContent();
//
//        if (boards.isEmpty()) {
//            return GetLikedBoardsResponseDto.builder()
//                    .boards(List.of())
//                    .page(PageResponseDto.toDto(boardPage))
//                    .build();
//        }
//
//        List<String> boardIds = boards.stream().map(Board::getId).toList();
//        List<BoardImage> images = boardImageRepository.findBoardThumbnailsByBoardIds(boardIds);
//
//        Map<String, String> thumbnailMap = new HashMap<>();
//        for (BoardImage image : images) {
//            thumbnailMap.putIfAbsent(
//                    image.getBoard().getId(),
//                    s3Util.toS3UrlByS3Key(image.getS3Key())
//            );
//        }
//
//        List<GetLikedBoardsResponseDto.Board> responseBoards = boards.stream()
//                .map(board -> GetLikedBoardsResponseDto.Board.toBoard(
//                        board,
//                        thumbnailMap.get(board.getId())
//                ))
//                .toList();
//
//        return GetLikedBoardsResponseDto.builder()
//                .boards(responseBoards)
//                .page(PageResponseDto.toDto(boardPage))
//                .build();
//    }

    @Transactional(readOnly = true)
    public int countLike(String boardId) {
        log.debug("BoardLikeService.countLike called. boardId={}", boardId);
        return boardLikeRepository.countByBoard_Id(boardId);
    }

    private void checkIfBoardDeleted(Board board) {
        if (board.isDeleted()) {
            log.debug("BoardLikeService.checkIfBoardDeleted found deleted board. boardId={}", board.getId());
            throw new IllegalStateException("Deleted Board");
        }
    }

    private void increaseLikeCount(String boardId) {
        log.debug("BoardLikeService.increaseLikeCount called. boardId={}", boardId);
        int updatedRowCount = boardRepository.increaseLikeCount(boardId);
        if (updatedRowCount == 0) {
            throw new EntityNotFoundException(String.format("Board Not Found : %s", boardId));
        }
    }

    private void decreaseLikeCount(String boardId) {
        log.debug("BoardLikeService.decreaseLikeCount called. boardId={}", boardId);
        int updatedRowCount = boardRepository.decreaseLikeCount(boardId);
        if (updatedRowCount == 0) {
            throw new EntityNotFoundException(String.format("Board Not Found : %s", boardId));
        }
    }

    private Board findBoardById(String boardId) {
        log.debug("BoardLikeService.findBoardById called. boardId={}", boardId);
        return boardRepository.findById(boardId)
                .orElseThrow(() ->
                        new EntityNotFoundException(String.format("Board Not Found : %s", boardId))
                );
    }
}
