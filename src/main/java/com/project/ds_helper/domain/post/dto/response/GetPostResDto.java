package com.project.ds_helper.domain.post.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.ds_helper.domain.post.entity.PostImage;
import com.project.ds_helper.domain.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
@Schema(description = "게시글 조회 응답 DTO", example = "{\"postId\":\"post-001\",\"title\":\"복지 서비스 후기\",\"content\":\"상담이 정말 친절했어요.\",\"writerName\":\"홍길동\",\"viewCount\":23,\"imageUrls\":[\"https://cdn.dshelper.kr/post/1.png\"],\"createdAt\":\"2026-03-23\"}")
public class GetPostResDto {

    private String postId;

    private String title;

    private String content;

    private String writerName;

    private int viewCount;

    private List<String> imageUrls;

    private String createdAt;

    public static GetPostResDto toDto(Post post){
        log.info("createdAt : {}", post.getCreatedAt());
        log.info("createdAt is null? : {}", post.getCreatedAt() == null);
        log.info("createdAt convert to LocalDate : {}", post.getCreatedAt().toLocalDate().toString());
        return GetPostResDto.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .writerName(post.getUser().getName())
                .imageUrls(post.getPostImages().stream().map(PostImage::getUrl).toList())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt() == null? null : post.getCreatedAt().toLocalDate().toString())
                .build();
    }

    public static List<GetPostResDto> toDtoList(List<Post> posts){
        if(posts.isEmpty()){
            return new ArrayList<GetPostResDto>();
        }
        return posts.stream().map(GetPostResDto::toDto).toList();
    }

}
