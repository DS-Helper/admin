package com.project.ds_helper.domain.post.service;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.common.util.FileUtil;
import com.project.ds_helper.common.util.PostUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.post.dto.request.CreatePostReqDto;
import com.project.ds_helper.domain.post.dto.request.UpdatePostReqDto;
import com.project.ds_helper.domain.post.dto.response.GetAllPostOfUserResDto;
import com.project.ds_helper.domain.post.dto.response.GetPostResDto;
import com.project.ds_helper.domain.post.entity.Post;
import com.project.ds_helper.domain.post.entity.PostImage;
import com.project.ds_helper.domain.post.repository.PostRepository;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.ImageUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserUtil userUtil;

    @Mock
    private ImageCompressionUtil imageCompressionUtil;

    @Mock
    private ImageUtil imageUtil;

    @Mock
    private PostUtil postUtil;

    @Mock
    private S3Util s3Util;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("게시글 목록 조회는 페이지 DTO를 반환한다")
    void getAllPostOfUser_returnsPagedDto() {
        Post post = post("post-1", user("user-1", UserRole.USER));
        when(postRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(post)));

        GetAllPostOfUserResDto result = postService.getAllPostOfUser(null, 0, 10, "desc", "createdAt");

        assertThat(result.posts()).hasSize(1);
        assertThat(result.posts().getFirst().getPostId()).isEqualTo("post-1");
    }

    @Test
    @DisplayName("게시글 목록 조회는 viewCount 정렬을 허용한다")
    void getAllPostOfUser_allowsViewCountSort() {
        Post post = post("post-1", user("user-1", UserRole.USER));
        when(postRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(post)));

        postService.getAllPostOfUser(null, 0, 10, "desc", "viewCount");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue())
                .isEqualTo(PageRequest.of(0, 10, Sort.Direction.DESC, "viewCount"));
    }

    @Test
    @DisplayName("게시글 목록 조회는 허용되지 않은 정렬 기준이면 예외를 던진다")
    void getAllPostOfUser_throwsWhenSortByInvalid() {
        assertThatThrownBy(() -> postService.getAllPostOfUser(null, 0, 10, "desc", "title"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sortBy");
    }

    @Test
    @DisplayName("게시글 목록 조회 시 keyword가 있으면 제목 LIKE 검색을 수행한다")
    void getAllPostOfUser_searchesByKeyword() {
        Post post = post("post-1", user("user-1", UserRole.USER));
        when(postRepository.findByTitleContaining(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(post)));

        GetAllPostOfUserResDto result = postService.getAllPostOfUser("검색", 0, 10, "desc", "createdAt");

        assertThat(result.posts()).hasSize(1);
        verify(postRepository).findByTitleContaining("검색", PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt"));
    }

    @Test
    @DisplayName("게시글 단건 조회는 조회수 증가 후 DTO를 반환한다")
    void getOnePost_returnsDto() {
        Post post = post("post-1", user("user-1", UserRole.USER));
        when(postRepository.increaseViewCount("post-1")).thenReturn(1);
        when(postRepository.findVisibleById("post-1")).thenReturn(java.util.Optional.of(post));

        GetPostResDto result = postService.getOnePost("post-1");

        assertThat(result.getPostId()).isEqualTo("post-1");
    }

    @Test
    @DisplayName("삭제된 회원의 게시글은 목록 조회에서 제외된 결과만 반환한다")
    void getAllPostOfUser_ignoresSoftDeletedUsers() {
        Post post = post("post-1", user("user-1", UserRole.USER));
        when(postRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(post)));

        GetAllPostOfUserResDto result = postService.getAllPostOfUser(null, 0, 10, "desc", "createdAt");

        assertThat(result.posts()).extracting(GetPostResDto::getPostId).containsExactly("post-1");
        verify(postRepository).findAll(PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt"));
    }

    @Test
    @DisplayName("조회수 증가에 실패하면 예외가 발생한다")
    void getOnePost_throwsWhenViewCountUpdateFails() {
        when(postRepository.increaseViewCount("post-1")).thenReturn(0);

        assertThatThrownBy(() -> postService.getOnePost("post-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("View Count Increase Failed");
    }

    @Test
    @DisplayName("게시글 생성은 제목이나 내용이 없으면 예외가 발생한다")
    void createPost_throwsWhenDtoInvalid() {
        CreatePostReqDto dto = new CreatePostReqDto(null, "내용");

        assertThatThrownBy(() -> postService.createPost(authentication, dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dto is invalid");
    }

    @Test
    @DisplayName("게시글 삭제는 관리자만 수행할 수 있다")
    void deletePost_throwsWhenUserIsNotAdmin() {
        User user = user("user-1", UserRole.USER);
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        assertThatThrownBy(() -> postService.deletePost(authentication, "post-1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only Admin May Access");
    }

    @Test
    @DisplayName("게시글 삭제는 관리자일 때 S3 삭제 후 게시글 삭제를 호출한다")
    void deletePost_deletesS3AndPostWhenAdmin() throws Exception {
        User admin = user("admin-1", UserRole.ADMIN);
        Post post = post("post-1", admin);
        when(userUtil.extractUserId(authentication)).thenReturn("admin-1");
        when(userUtil.findUserById("admin-1")).thenReturn(admin);
        when(postUtil.findPostById("post-1")).thenReturn(post);

        postService.deletePost(authentication, "post-1");

        verify(s3Util).deleteImages(List.of());
        verify(postUtil).deletePostById("post-1");
    }

    @Test
    @DisplayName("게시글 수정 시 삭제 대상이 없어도 새 이미지는 추가 업로드한다")
    void updatePost_uploadsNewImagesEvenWhenNothingDeleted() throws Exception {
        User user = user("user-1", UserRole.USER);
        Post post = post("post-1", user);
        PostImage keepImage = PostImage.builder()
                .storedName("keep.png")
                .post(post)
                .build();
        post.addImage(keepImage);

        UpdatePostReqDto dto = new UpdatePostReqDto(
                "post-1",
                "수정 제목",
                "수정 내용",
                List.of("https://bucket.s3.ap-northeast-2.amazonaws.com/images/keep.png")
        );
        MockMultipartFile newImage = new MockMultipartFile("images", "new.png", "image/png", "new".getBytes());
        S3ImageUploadRequestDto compressedImage = S3ImageUploadRequestDto.builder()
                .storedFilename("stored-new")
                .originalFilename("new.png")
                .bytes("compressed".getBytes())
                .size(10L)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(postUtil.findPostById("post-1")).thenReturn(post);
        when(s3Util.extractFilenameFromS3Url("https://bucket.s3.ap-northeast-2.amazonaws.com/images/keep.png"))
                .thenReturn("keep.png");
        when(imageUtil.toStoredFilename()).thenReturn("stored-new");
        when(imageCompressionUtil.compressImage(newImage, "stored-new")).thenReturn(compressedImage);
        when(s3Util.buildS3Key("stored-new")).thenReturn("images/stored-new");
        when(s3Util.toS3UrlByS3Key("images/stored-new")).thenReturn("https://bucket.s3.ap-northeast-2.amazonaws.com/images/stored-new");

        postService.updatePost(authentication, dto, List.of(newImage));

        verify(s3Util).uploadImages(List.of(compressedImage));
        verify(postRepository).save(any(Post.class));
        assertThat(post.getPostImages()).hasSize(2);
    }

    @Test
    @DisplayName("관리자는 작성자가 아니어도 게시글을 수정할 수 있다")
    void updatePost_allowsAdminEvenWhenNotWriter() throws Exception {
        User admin = user("admin-1", UserRole.ADMIN);
        User writer = user("writer-1", UserRole.USER);
        Post post = post("post-1", writer);
        UpdatePostReqDto dto = new UpdatePostReqDto("post-1", "관리자 수정", "관리자 내용", List.of());

        when(userUtil.extractUserId(authentication)).thenReturn("admin-1");
        when(userUtil.findUserById("admin-1")).thenReturn(admin);
        when(postUtil.findPostById("post-1")).thenReturn(post);

        postService.updatePost(authentication, dto, null);

        verify(postRepository).save(post);
        assertThat(post.getTitle()).isEqualTo("관리자 수정");
        assertThat(post.getContent()).isEqualTo("관리자 내용");
    }

    private User user(String id, UserRole role) {
        return User.builder().id(id).name("tester").email("user@test.com").role(role).build();
    }

    private Post post(String id, User user) {
        Post post = Post.builder()
                .id(id)
                .title("제목")
                .content("내용")
                .user(user)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.of(2026, 3, 27, 10, 0));
        return post;
    }
}
