package com.project.ds_helper.domain.board.dto.response;

import com.project.ds_helper.common.dto.response.PageResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class GetLikedBoardsResponseDto {

    private List<Board> boards;
    private PageResponseDto page;

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Board {

        @Schema(description = "Board ID", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
        private String id;

        @Schema(description = "Board title", example = "Example title")
        private String title;

        @Schema(description = "Board content", example = "Example content")
        private String content;

        @Schema(description = "Like count", example = "14")
        private int likeCount;

        @Schema(description = "Comment count", example = "15")
        private int commentCount;

        @Schema(description = "Thumbnail URL", example = "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/images/2026/02/21/uuid2.jpg")
        private String thumbNailUrl;

        public static Board toBoard(com.project.ds_helper.domain.board.entity.Board board, String thumbNailUrl) {
            return Board.builder()
                    .id(board.getId())
                    .title(board.getTitle())
                    .content(board.getContent())
                    .likeCount(board.getLikeCount())
                    .commentCount(board.getCommentCount())
                    .thumbNailUrl(thumbNailUrl)
                    .build();
        }
    }
}
