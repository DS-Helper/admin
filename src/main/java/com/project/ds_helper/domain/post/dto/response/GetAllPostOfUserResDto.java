package com.project.ds_helper.domain.post.dto.response;

import com.project.ds_helper.common.dto.response.PageResponseDto;
import com.project.ds_helper.domain.post.entity.PostImage;
import com.project.ds_helper.domain.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Schema(description = "사용자 게시글 목록 응답 DTO", example = "{\"posts\":[{\"postId\":\"post-001\",\"title\":\"복지 서비스 후기\",\"content\":\"상담이 정말 친절했어요.\",\"writerName\":\"홍길동\",\"viewCount\":23,\"imageUrls\":[\"https://cdn.dshelper.kr/post/1.png\"],\"createdAt\":\"2026-03-23\"}],\"page\":{\"page\":0,\"size\":10,\"totalElements\":1,\"totalPages\":1,\"first\":true,\"last\":true,\"hasNext\":false,\"hasPrevious\":false,\"sort\":{\"sorted\":true,\"unsorted\":false,\"empty\":false}}}")
public record GetAllPostOfUserResDto(
        List<GetPostResDto> posts,
        PageResponseDto page
) {
    public static PageResponseDto toPage(Page<Post> posts){
        return new PageResponseDto(posts.getNumber(), posts.getSize(), posts.getTotalElements(), posts.getTotalPages(), posts.isFirst(), posts.isLast(), posts.hasNext(), posts.hasPrevious(), posts.getPageable().getSort());
    }

    public static GetPostResDto toDto(Post post){
        String postId = post.getId(); String title = post.getTitle(); String content = post.getContent(); String writerName = post.getUser().getName(); List<String> imageUrls = !post.getPostImages().isEmpty()? post.getPostImages().stream().map(PostImage::getUrl).toList() : new ArrayList<>();
        log.info("postId : {}, title : {}, content : {}, writerName : {}, imageUrls : {}", postId, title, content, writerName, imageUrls);
        return GetPostResDto.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .writerName(post.getUser().getName())
                .viewCount(post.getViewCount())
                .imageUrls(post.getPostImages().stream().map(PostImage::getUrl).toList())
                .build();
    }

    public static GetAllPostOfUserResDto toDtoList(Page<Post> posts){
        if(posts.isEmpty()){
            log.info("Posts is Empty");
            return new GetAllPostOfUserResDto(new ArrayList<>(), new PageResponseDto(0,0,0L,0,false,false,false,false, posts.getSort()));
        }
        log.info("posts size : {}", posts.getSize());
        return new GetAllPostOfUserResDto(posts.stream().map(GetPostResDto::toDto).toList(), toPage(posts));
    }


}
