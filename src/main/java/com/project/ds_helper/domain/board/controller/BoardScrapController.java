package com.project.ds_helper.domain.board.controller;

import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.board.dto.response.GetScrappedBoardsResponseDto;
import com.project.ds_helper.domain.board.service.BoardScrapService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
@Slf4j
@Tag(name = SwaggerTagName.COMMUNITY_SCRAP)
public class BoardScrapController {

    private final BoardScrapService boardScrapService;

    @Tag(name = SwaggerTagName.COMMUNITY_SCRAP)
    @Operation(summary = "게시글 스크랩 수 조회 (JWT 인증 필요)", description = "게시글 ID를 기준으로 해당 게시글의 스크랩 수를 조회합니다.")
    @GetMapping("/{boardId}/scrap/count")
    public ResponseEntity<ResponseVo<Long>> countScrap(@PathVariable String boardId) {
        log.debug("BoardScrapController.countScrap called. boardId={}", boardId);
        long count = boardScrapService.countScrap(boardId);
        ResponseVo<Long> responseVo = new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), count);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Tag(name = SwaggerTagName.MY_SCRAP)
    @Operation(summary = "내 스크랩 게시글 조회 (JWT 인증 필요)", description = "로그인한 사용자가 스크랩한 게시글 목록을 페이지 조건에 맞게 조회합니다.")
    @GetMapping("/boards/scrap")
    public ResponseEntity<ResponseVo<GetScrappedBoardsResponseDto>> getMyScraps(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        log.debug("BoardScrapController.getMyScraps called. page={}, size={}, sort={}, sortBy={}", page, size, sort, sortBy);
        GetScrappedBoardsResponseDto response =
                boardScrapService.getMyScrappedBoards(authentication, page, size, sort, sortBy);

        ResponseVo<GetScrappedBoardsResponseDto> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), response);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Tag(name = SwaggerTagName.COMMUNITY_SCRAP)
    @Operation(summary = "게시글 스크랩 토글 (JWT 인증 필요)", description = "게시글 스크랩 상태를 추가 또는 취소합니다.")
    @PostMapping("/board/{boardId}/scrap")
    public ResponseEntity<ResponseVo<Boolean>> toggleScrap(
            Authentication authentication,
            @PathVariable String boardId
    ) {
        log.debug("BoardScrapController.toggleScrap called. boardId={}", boardId);
        boolean result = boardScrapService.toggleBoardScrap(authentication, boardId);
        ResponseVo<Boolean> responseVo = new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), result);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }
}
