package com.project.ds_helper.domain.post.dto.request;

import com.project.ds_helper.domain.post.entity.Post;
import com.project.ds_helper.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostReqDto {

    @NotBlank
    @Schema(example = "정말 도움 많이 받았습니다 ㅎㅎ")
    private String title;

    @NotBlank
    @Schema(example = "감사해요 도움 잘 받았습니다! 또 신청 할게요 ^^!")
    private String content;
    
    public Post toPost(CreatePostReqDto dto, User user){
        return Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .user(user)
                .build();
    }

    @Deprecated
    public Post toPostWithoutImages(CreatePostReqDto dto, User user){
        return Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .user(user)
                .build();
    }


}
