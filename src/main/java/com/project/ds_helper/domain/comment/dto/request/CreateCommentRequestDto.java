package com.project.ds_helper.domain.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;

@Getter
@NoArgsConstructor
@Slf4j
public class CreateCommentRequestDto {


    @Schema(
            description = "게시글 ID",
            example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01"
    )
    @NotBlank
    private String boardId;

    @Schema(
            description = "부모 댓글 ID (대댓글인 경우만 전달)",
            example = "b7a1d2e4-8f6c-4c3a-91ab-2e7f5d9c4b88",
            nullable = true
    )
    private String parentId; // null이면 depth1 댓글

    @Schema(
            description = "댓글 내용",
            example = "이 글 정말 도움이 많이 됐습니다. 추가로 관련 자료가 있다면 공유해주시면 감사하겠습니다!"
    )
    @NotBlank
    private String content;
}
