package com.project.ds_helper.domain.board.controller;

import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.board.service.BoardLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("")
@Tag(name = SwaggerTagName.COMMUNITY_LIKE)
public class BoardLikeController {

    private final BoardLikeService boardLikeService;

    @Operation(summary = "게시글 좋아요 수 조회 (JWT 인증 필요)", description = "게시글 ID를 기준으로 해당 게시글의 좋아요 수를 조회합니다.")
    @GetMapping("/{boardId}/like/count")
    public ResponseEntity<ResponseVo<Integer>> countLike(@PathVariable String boardId) {
        log.debug("BoardLikeController.countLike called. boardId={}", boardId);
        int count = boardLikeService.countLike(boardId);
        ResponseVo<Integer> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), count);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(summary = "게시글 좋아요 토글 (JWT 인증 필요)", description = "게시글 좋아요 상태를 추가 또는 취소합니다.")
    @PostMapping("/board/{boardId}/like")
    public ResponseEntity<ResponseVo<Boolean>> toggleBoardLike(
            Authentication authentication,
            @PathVariable String boardId
    ) {
        log.debug("BoardLikeController.toggleBoardLike called. boardId={}", boardId);
        boolean liked = boardLikeService.toggleBoardLike(authentication, boardId);

        ResponseVo<Boolean> responseVo = new ResponseVo<>(
                true,
                SuccessCode.OK,
                liked ? "Board liked." : "Board like canceled.",
                liked
        );

        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }
}
