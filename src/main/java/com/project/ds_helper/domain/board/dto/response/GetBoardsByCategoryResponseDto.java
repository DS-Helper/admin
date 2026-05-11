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
@Schema(
        description = "카테고리별 게시글 목록 조회 응답 DTO",
        example = """
                {
                  "boards": [
                    {
                      "id": "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01",
                      "title": "복지관 프로그램 추천 부탁드려요",
                      "content": "부모님과 함께 참여할 수 있는 프로그램이 있으면 추천 부탁드립니다.",
                      "writerName": "김도움",
                      "writerProfileImageUrl": "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/profile/user-001.jpg",
                      "likeCount": 14,
                      "commentCount": 15,
                      "thumbNailUrl": "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/images/2026/02/21/uuid2.jpg"
                    }
                  ],
                  "page": {
                    "page": 0,
                    "size": 10,
                    "totalElements": 1,
                    "totalPages": 1,
                    "first": true,
                    "last": true,
                    "hasNext": false,
                    "hasPrevious": false,
                    "sort": null
                  }
                }
                """
)
public class GetBoardsByCategoryResponseDto {

    private List<Board> boards;
    private PageResponseDto page;

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Board{

        @Schema(description = "게시글 ID", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
        private String id;

        @Schema(description = "게시글 제목", example = "복지관 프로그램 추천 부탁드려요")
        private String title;

        @Schema(description = "게시글 내용", example = "부모님과 함께 참여할 수 있는 프로그램이 있으면 추천 부탁드립니다.")
        private String content;

        @Schema(description = "작성자 이름", example = "김도움")
        private String writerName;

        @Schema(description = "작성자 유저 ID", example = "88fa8c94-ad1a-4351-9fd6-cdd002ecf167")
        private String writerId;

        @Schema(description = "작성자 프로필 이미지 URL", example = "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/profile/user-001.jpg")
        private String writerProfileImageUrl;

        @Schema(description = "좋아요 수", example = "14")
        private int likeCount;

        @Schema(description = "댓글 수", example = "15")
        private int commentCount;

        @Schema(description = "좋아요 여부", example = "true")
        private boolean isLiked;

        @Schema(description = "썸네일 이미지 URL", example = "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/images/2026/02/21/uuid2.jpg")
        private String thumbNailUrl;

        public static Board toBoard(
                com.project.ds_helper.domain.board.entity.Board board,
                int commentCount,
                boolean isLiked,
                String thumbNailUrl
        ){
            return Board.builder()
                    .id(board.getId())
                    .title(board.getTitle())
                    .content(board.getContent())
                    .writerName(board.getUser().getName())
                    .writerId(board.getUser().getId())
                    .writerProfileImageUrl(board.getUser().getProfileImageUrl())
                    .likeCount(board.getLikeCount())
                    .commentCount(commentCount)
                    .isLiked(isLiked)
                    .thumbNailUrl(thumbNailUrl)
                    .build();
        }
    }
}
