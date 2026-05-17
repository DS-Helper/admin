package com.project.ds_helper.domain.post;

import com.project.ds_helper.domain.post.dto.request.CreatePostReqDto;
import com.project.ds_helper.domain.post.dto.request.UpdatePostReqDto;
import com.project.ds_helper.domain.post.dto.response.GetAllPostOfUserResDto;
import com.project.ds_helper.domain.post.dto.response.GetPostResDto;
import com.project.ds_helper.domain.post.entity.Post;
import com.project.ds_helper.domain.post.entity.PostImage;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostDtoEntityTest {

    @Test
    @DisplayName("CreatePostReqDto는 Post Entity로 변환된다")
    void createPostReqDto_convertsToPost() {
        CreatePostReqDto dto = new CreatePostReqDto("제목", "내용");
        User user = user();

        Post post = dto.toPost(dto, user);
        Post postWithoutImages = dto.toPostWithoutImages(dto, user);

        assertThat(post.getTitle()).isEqualTo("제목");
        assertThat(post.getContent()).isEqualTo("내용");
        assertThat(post.getUser()).isEqualTo(user);
        assertThat(postWithoutImages.getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("UpdatePostReqDto는 기존 Post Entity를 수정한다")
    void updatePostReqDto_updatesPost() {
        Post post = post();
        UpdatePostReqDto dto = new UpdatePostReqDto("post-1", "수정 제목", "수정 내용", List.of());

        Post result = dto.toUpdatedPost(dto, post);

        assertThat(result).isSameAs(post);
        assertThat(post.getTitle()).isEqualTo("수정 제목");
        assertThat(post.getContent()).isEqualTo("수정 내용");
    }

    @Test
    @DisplayName("Post는 이미지 양방향 관계를 추가/제거한다")
    void post_addAndRemoveImageMaintainsRelation() {
        Post post = post();
        PostImage image = PostImage.builder().storedName("stored.webp").build();

        post.addImage(image);
        post.removeImage(image);

        assertThat(post.getPostImages()).isEmpty();
        assertThat(image.getPost()).isNull();
    }

    @Test
    @DisplayName("Post와 PostImage는 PrePersist에서 ID를 생성한다")
    void prePersist_generatesIds() {
        Post post = post();
        PostImage image = PostImage.builder().build();
        post.setId(null);

        ReflectionTestUtils.invokeMethod(post, "perPersistGenerateId");
        image.generatedId();

        assertThat(post.getId()).isNotBlank();
        assertThat(image.getId()).isNotBlank();
    }

    @Test
    @DisplayName("GetPostResDto는 이미지가 있는 게시글과 빈 목록을 변환한다")
    void getPostResDto_convertsPostAndEmptyList() {
        Post post = post();
        post.addImage(PostImage.builder().storedName("stored.webp").url("https://bucket/images/stored.webp").build());

        GetPostResDto dto = GetPostResDto.toDto(post);
        List<GetPostResDto> emptyDtos = GetPostResDto.toDtoList(List.of());
        List<GetPostResDto> dtos = GetPostResDto.toDtoList(List.of(post));

        assertThat(dto.getImageUrls()).containsExactly("https://bucket/images/stored.webp");
        assertThat(dto.getCreatedAt()).isEqualTo("2026-03-27");
        assertThat(emptyDtos).isEmpty();
        assertThat(dtos).hasSize(1);
    }

    @Test
    @DisplayName("GetAllPostOfUserResDto는 빈 페이지와 이미지가 있는 페이지를 변환한다")
    void getAllPostOfUserResDto_convertsPages() {
        Post post = post();
        post.addImage(PostImage.builder().storedName("stored.webp").url("https://bucket/images/stored.webp").build());

        GetAllPostOfUserResDto empty = GetAllPostOfUserResDto.toDtoList(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        GetAllPostOfUserResDto filled = GetAllPostOfUserResDto.toDtoList(new PageImpl<>(List.of(post), PageRequest.of(0, 10), 1));
        GetPostResDto single = GetAllPostOfUserResDto.toDto(post);
        GetPostResDto singleWithoutImages = GetAllPostOfUserResDto.toDto(post());

        assertThat(empty.posts()).isEmpty();
        assertThat(filled.posts()).hasSize(1);
        assertThat(filled.page().totalElements()).isEqualTo(1);
        assertThat(single.getImageUrls()).containsExactly("https://bucket/images/stored.webp");
        assertThat(singleWithoutImages.getImageUrls()).isEmpty();
    }

    private Post post() {
        Post post = Post.builder()
                .id("post-1")
                .title("제목")
                .content("내용")
                .user(user())
                .build();
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.of(2026, 3, 27, 10, 0));
        return post;
    }

    private User user() {
        return User.builder()
                .id("user-1")
                .name("tester")
                .email("user@test.com")
                .role(UserRole.USER)
                .build();
    }
}
