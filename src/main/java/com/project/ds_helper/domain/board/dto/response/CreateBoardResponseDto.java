package com.project.ds_helper.domain.board.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        description = "게시글 생성 응답 DTO",
        example = """
                {
                  "id": "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01",
                  "title": "복지관 프로그램 추천 부탁드려요",
                  "content": "부모님과 함께 참여할 수 있는 프로그램이 있으면 추천 부탁드립니다.",
                  "writerName": "김민지",
                  "writerProfileImageUrl": "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/profile/user-001.jpg",
                  "viewCount": 0,
                  "likeCount": 0,
                  "commentCount": 0,
                  "isLiked": false,
                  "isScrapped": false,
                  "imageUrls": [
                    "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/images/2026/02/21/uuid1.jpg",
                    "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/images/2026/02/21/uuid2.jpg"
                  ],
                  "createdAt": "2026-01-01 16:25:38"
                }
                """
)
public record CreateBoardResponseDto(
        @Schema(description = "게시글 ID", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
        String id,
        @Schema(description = "게시글 제목", example = "복지관 프로그램 추천 부탁드려요")
        String title,
        @Schema(description = "게시글 내용", example = "부모님과 함께 참여할 수 있는 프로그램이 있으면 추천 부탁드립니다.")
        String content,
        @Schema(description = "작성자 이름", example = "김민지")
        String writerName,
        @Schema(description = "작성자 프로필 이미지 URL", example = "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/profile/user-001.jpg")
        String writerProfileImageUrl,
        @Schema(description = "조회수", example = "0")
        int viewCount,
        @Schema(description = "좋아요 수", example = "0")
        int likeCount,
        @Schema(description = "댓글 수", example = "0")
        int commentCount,
        @Schema(description = "좋아요 여부", example = "false")
        boolean isLiked,
        @Schema(description = "스크랩 여부", example = "false")
        boolean isScrapped,
        @Schema(description = "게시글 이미지 URL 목록")
        List<String> imageUrls,
        @Schema(description = "생성 일시", example = "2026-01-01 16:25:38")
        String createdAt
) {
}

