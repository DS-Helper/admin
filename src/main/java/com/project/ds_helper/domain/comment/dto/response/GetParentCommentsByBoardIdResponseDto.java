package com.project.ds_helper.domain.comment.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.ds_helper.domain.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(
        description = "부모 댓글 조회 응답 DTO",
        example = """
                {
                  "commentId": "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01",
                  "writerName": "김도움",
                  "writerProfileImageUrl": "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/profile/user-001.jpg",
                  "content": "저도 같은 프로그램 추천합니다.",
                  "createdAt": "2025-12-31 18:22:50"
                }
                """
)
public record GetParentCommentsByBoardIdResponseDto(

        @Schema(description = "댓글 ID", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
        String commentId,

        @Schema(description = "댓글 작성자 이름", example = "김도움")
        String writerName,

        @Schema(description = "작성자 유저 ID", example = "88fa8c94-ad1a-4351-9fd6-cdd002ecf167")
        String writerId,

        @Schema(description = "댓글 작성자 프로필 이미지 URL", example = "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/profile/user-001.jpg")
        String writerProfileImageUrl,

        @Schema(description = "댓글 내용", example = "저도 같은 프로그램 추천합니다.")
        String content,

        @Schema(description = "댓글 작성 일시", example = "2025-12-31 18:22:50")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {
    public static GetParentCommentsByBoardIdResponseDto toDto(Comment comment){
        return new GetParentCommentsByBoardIdResponseDto(
                comment.getId(),
                comment.getUser().getName(),
                comment.getUser().getId(),
                comment.getUser().getProfileImageUrl(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
