package com.project.ds_helper.domain.board.dto.response;

import com.project.ds_helper.domain.board.entity.Board;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "내가 작성한 게시글 조회 응답 DTO")
public record GetMyBoardsResponseDto(
        @Schema(description = "게시글 ID", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
        String id,
        @Schema(description = "게시글 제목", example = "복지관 프로그램 추천 부탁드려요")
        String title,
        @Schema(description = "게시글 내용", example = "부모님과 함께 참여할 수 있는 프로그램이 있으면 추천 부탁드립니다.")
        String content,
        @Schema(description = "작성자 이름", example = "김민지")
        String writerName,
        @Schema(description = "작성자 유저 ID", example = "88fa8c94-ad1a-4351-9fd6-cdd002ecf167")
        String writerId,
        @Schema(description = "작성자 프로필 이미지 URL", example = "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/profile/user-001.jpg")
        String writerProfileImageUrl,
        @Schema(description = "좋아요 수", example = "14")
        int likeCount,
        @Schema(description = "댓글 수", example = "15")
        int commentCount,
        @Schema(description = "좋아요 여부", example = "true")
        boolean isLiked,
        @Schema(description = "썸네일 이미지 URL", example = "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/images/2026/02/21/uuid2.jpg")
        String thumbNailUrl
) {
    public static GetMyBoardsResponseDto from(Board board, int commentCount, boolean isLiked, String thumbNailUrl) {
        return GetMyBoardsResponseDto.builder()
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
