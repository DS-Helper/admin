package com.project.ds_helper.domain.comment.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.ds_helper.domain.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(
        description = "대댓글 조회 응답 DTO",
        example = """
                {
                  "id": "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01",
                  "content": "저는 목요일 프로그램이 좋았어요.",
                  "userName": "박지원",
                  "userProfileImageUrl": "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/profile/user-002.jpg",
                  "createdAt": "2025-12-31 18:22:50"
                }
                """
)
public record GetChildCommentsResponseDto(

        @Schema(description = "대댓글 ID", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
        String id,

        @Schema(description = "대댓글 내용", example = "저는 목요일 프로그램이 좋았어요.")
        String content,

        @Schema(description = "대댓글 작성자 이름", example = "박지원")
        String userName,

        @Schema(description = "작성자 유저 ID", example = "88fa8c94-ad1a-4351-9fd6-cdd002ecf167")
        String userId,

        @Schema(description = "대댓글 작성자 프로필 이미지 URL", example = "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/profile/user-002.jpg")
        String userProfileImageUrl,

        @Schema(description = "대댓글 작성 일시", example = "2025-12-31 18:22:50")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {
    public static GetChildCommentsResponseDto toDto(Comment comment){
        return new GetChildCommentsResponseDto(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getName(),
                comment.getUser().getId(),
                comment.getUser().getProfileImageUrl(),
                comment.getCreatedAt()
        );
    }
}
