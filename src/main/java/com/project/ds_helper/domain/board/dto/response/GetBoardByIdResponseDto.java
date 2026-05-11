package com.project.ds_helper.domain.board.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.ds_helper.domain.board.entity.Board;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(
        description = "게시글 상세 조회 응답 DTO",
        example = """
                {
                  "id": "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01",
                  "title": "복지관 프로그램 추천 부탁드려요",
                  "content": "부모님과 함께 참여할 수 있는 프로그램이 있으면 추천 부탁드립니다.",
                  "writerName": "김도움",
                  "writerProfileImageUrl": "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/profile/user-001.jpg",
                  "viewCount": 13,
                  "likeCount": 14,
                  "commentCount": 15,
                  "isLiked": true,
                  "isScrapped": false,
                  "imageUrls": [
                    "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/images/2026/02/21/uuid1.jpg",
                    "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/images/2026/02/21/uuid2.jpg"
                  ],
                  "createdAt": "2026-01-01 16:25:38"
                }
                """
)
public record GetBoardByIdResponseDto(

        @Schema(description = "게시글 ID", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
        String id,

        @Schema(description = "게시글 제목", example = "복지관 프로그램 추천 부탁드려요")
        String title,

        @Schema(description = "게시글 내용", example = "부모님과 함께 참여할 수 있는 프로그램이 있으면 추천 부탁드립니다.")
        String content,

        @Schema(description = "작성자 이름", example = "김도움")
        String writerName,

        @Schema(description = "작성자 유저 ID", example = "88fa8c94-ad1a-4351-9fd6-cdd002ecf167")
        String writerId,

        @Schema(description = "작성자 프로필 이미지 URL", example = "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/profile/user-001.jpg")
        String writerProfileImageUrl,

        @Schema(description = "조회수", example = "13")
        int viewCount,

        @Schema(description = "좋아요 수", example = "14")
        int likeCount,

        @Schema(description = "댓글 수", example = "15")
        int commentCount,

        @Schema(description = "좋아요 여부", example = "true")
        boolean isLiked,

        @Schema(description = "스크랩 여부", example = "false")
        boolean isScrapped,

        @Schema(
                description = "게시글 이미지 URL 목록",
                example = """
                        [
                          "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/images/2026/02/21/uuid1.jpg",
                          "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/images/2026/02/21/uuid2.jpg"
                        ]
                        """
        )
        List<String> imageUrls,

        @Schema(description = "생성 일시", example = "2026-01-01 16:25:38")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        String createdAt
) {
    public static GetBoardByIdResponseDto toDto(
            Board board,
            int commentCount,
            boolean isLiked,
            boolean isScrapped,
            List<String> imageUrls
    ){
        return new GetBoardByIdResponseDto(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getUser().getName(),
                board.getUser().getId(),
                board.getUser().getProfileImageUrl(),
                board.getViewCount(),
                board.getLikeCount(),
                commentCount,
                isLiked,
                isScrapped,
                imageUrls,
                board.getCreatedAt().toString()
        );
    }
}
