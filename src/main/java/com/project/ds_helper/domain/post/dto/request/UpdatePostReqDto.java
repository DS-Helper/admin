package com.project.ds_helper.domain.post.dto.request;

import com.project.ds_helper.domain.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostReqDto {

    @NotBlank
    @Schema(example = "8f14e45f-ea9d-4c3b-b7c8-9f8c6a12d3f1\n")
    private String postId;

    @NotBlank
    @Schema(example = "정말 도움 많이 받았습니다 ㅎㅎ")
    private String title;

    @NotBlank
    @Schema(example = "도움을 잘 받아서 주변에 추천 중이에요!! ㅎㅎ 번창하세요")
    private String content;

    @Schema(description = "게시글 수정 시 유지할 기존 이미지 URL 리스트")
    private List<String> imageUrls;


    public Post toUpdatedPost(UpdatePostReqDto dto, Post post){
    // 수정 필요
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        return post;
    }
}