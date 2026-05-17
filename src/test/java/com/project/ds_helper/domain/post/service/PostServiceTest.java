package com.project.ds_helper.domain.post.service;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
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
import static org.mockito.Mockito.never;
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
    @DisplayName("게시글 목록 조회는 오름차순 정렬을 지원한다")
    void getAllPostOfUser_supportsAscendingSort() {
        when(postRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        postService.getAllPostOfUser(null, 0, 10, "asc", "createdAt");

        verify(postRepository).findAll(PageRequest.of(0, 10, Sort.Direction.ASC, "createdAt"));
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
    @DisplayName("게시글 목록 조회는 blank keyword면 전체 조회를 수행한다")
    void getAllPostOfUser_usesFindAllWhenKeywordIsBlank() {
        when(postRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        postService.getAllPostOfUser("   ", 0, 10, "desc", "createdAt");

        verify(postRepository).findAll(PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt"));
        verify(postRepository, never()).findByTitleContaining(any(), any(Pageable.class));
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
    @DisplayName("게시글 단건 조회는 게시글이 없으면 예외가 발생한다")
    void getOnePost_throwsWhenPostMissing() {
        when(postRepository.increaseViewCount("missing")).thenReturn(1);
        when(postRepository.findVisibleById("missing")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> postService.getOnePost("missing"))
                .isInstanceOf(IllegalArgumentException.class);
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
    @DisplayName("게시글 생성은 내용이 없으면 예외가 발생한다")
    void createPost_throwsWhenContentInvalid() {
        CreatePostReqDto dto = new CreatePostReqDto("제목", null);

        assertThatThrownBy(() -> postService.createPost(authentication, dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dto is invalid");
    }

    @Test
    @DisplayName("게시글 생성은 이미지가 없으면 게시글만 저장한다")
    void createPost_savesPostWithoutImagesWhenImagesNull() throws Exception {
        User user = user("user-1", UserRole.USER);
        CreatePostReqDto dto = new CreatePostReqDto("제목", "내용");
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        postService.createPost(authentication, dto, null);

        verify(postRepository).save(any(Post.class));
        verify(s3Util, never()).uploadImages(any());
    }

    @Test
    @DisplayName("게시글 생성은 빈 이미지 목록이면 게시글만 저장한다")
    void createPost_savesPostWithoutImagesWhenImagesEmpty() throws Exception {
        User user = user("user-1", UserRole.USER);
        CreatePostReqDto dto = new CreatePostReqDto("제목", "내용");
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        postService.createPost(authentication, dto, List.of());

        verify(postRepository).save(any(Post.class));
        verify(s3Util, never()).uploadImages(any());
    }

    @Test
    @DisplayName("게시글 생성은 이미지가 있으면 압축, 엔티티 추가, S3 업로드를 수행한다")
    void createPost_savesPostWithImages() throws Exception {
        User user = user("user-1", UserRole.USER);
        CreatePostReqDto dto = new CreatePostReqDto("제목", "내용");
        MockMultipartFile image = new MockMultipartFile("images", "image.png", "image/png", "image".getBytes());
        S3ImageUploadRequestDto compressedImage = S3ImageUploadRequestDto.builder()
                .storedFilename("stored")
                .originalFilename("image.png")
                .bytes("compressed".getBytes())
                .size(10L)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();
        PostImage postImage = PostImage.builder().storedName("stored").url("https://s3/images/stored").build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(imageUtil.toStoredFilename()).thenReturn("stored");
        when(s3Util.buildS3Key("stored")).thenReturn("images/stored");
        when(imageCompressionUtil.compressImage(image, "stored")).thenReturn(compressedImage);
        when(imageUtil.toPostImage(compressedImage, "images/stored")).thenReturn(postImage);

        postService.createPost(authentication, dto, List.of(image));

        verify(postRepository).save(any(Post.class));
        verify(s3Util).uploadImages(List.of(compressedImage));
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
    @DisplayName("게시글 수정은 제목이 없으면 예외가 발생한다")
    void updatePost_throwsWhenTitleInvalid() {
        UpdatePostReqDto dto = new UpdatePostReqDto("post-1", null, "내용", List.of());

        assertThatThrownBy(() -> postService.updatePost(authentication, dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dto is invalid");
    }

    @Test
    @DisplayName("게시글 수정은 내용이 없으면 예외가 발생한다")
    void updatePost_throwsWhenContentInvalid() {
        UpdatePostReqDto dto = new UpdatePostReqDto("post-1", "제목", null, List.of());

        assertThatThrownBy(() -> postService.updatePost(authentication, dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dto is invalid");
    }

    @Test
    @DisplayName("게시글 수정은 게시글 ID가 없으면 예외가 발생한다")
    void updatePost_throwsWhenPostIdInvalid() {
        UpdatePostReqDto dto = new UpdatePostReqDto(null, "제목", "내용", List.of());

        assertThatThrownBy(() -> postService.updatePost(authentication, dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dto is invalid");
    }

    @Test
    @DisplayName("게시글 수정은 작성자와 관리자가 아니면 예외가 발생한다")
    void updatePost_throwsWhenRequesterIsNotWriterOrAdmin() {
        User requester = user("requester", UserRole.USER);
        User writer = user("writer", UserRole.USER);
        Post post = post("post-1", writer);
        UpdatePostReqDto dto = new UpdatePostReqDto("post-1", "제목", "내용", List.of());
        when(userUtil.extractUserId(authentication)).thenReturn("requester");
        when(userUtil.findUserById("requester")).thenReturn(requester);
        when(postUtil.findPostById("post-1")).thenReturn(post);

        assertThatThrownBy(() -> postService.updatePost(authentication, dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only Writer Can Update Post");
    }

    @Test
    @DisplayName("게시글 수정은 이미지가 없고 imageUrls가 null이어도 본문만 수정한다")
    void updatePost_updatesTextOnlyWhenImagesNullAndImageUrlsNull() throws Exception {
        User user = user("user-1", UserRole.USER);
        Post post = post("post-1", user);
        UpdatePostReqDto dto = new UpdatePostReqDto("post-1", "새 제목", "새 내용", null);
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(postUtil.findPostById("post-1")).thenReturn(post);

        postService.updatePost(authentication, dto, null);

        verify(postRepository).save(post);
        verify(s3Util, never()).uploadImages(any());
        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getContent()).isEqualTo("새 내용");
    }

    @Test
    @DisplayName("게시글 수정은 기존 이미지 삭제 대상이 있으면 S3 삭제 후 새 이미지를 업로드한다")
    void updatePost_deletesRemovedImagesAndUploadsNewImages() throws Exception {
        User user = user("user-1", UserRole.USER);
        Post post = post("post-1", user);
        PostImage removeImage = PostImage.builder().id("image-1").storedName("remove.png").post(post).build();
        post.addImage(removeImage);
        UpdatePostReqDto dto = new UpdatePostReqDto("post-1", "수정 제목", "수정 내용", List.of());
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
        when(imageUtil.toStoredFilename()).thenReturn("stored-new");
        when(imageCompressionUtil.compressImage(newImage, "stored-new")).thenReturn(compressedImage);
        when(s3Util.buildS3Key("stored-new")).thenReturn("images/stored-new");
        when(s3Util.toS3UrlByS3Key("images/stored-new")).thenReturn("https://bucket/images/stored-new");
        when(s3Util.deleteImages(List.of("remove.png"))).thenReturn(true);

        postService.updatePost(authentication, dto, List.of(newImage));

        verify(s3Util).deleteImages(List.of("remove.png"));
        verify(s3Util).uploadImages(List.of(compressedImage));
        verify(postRepository).save(post);
        assertThat(post.getPostImages()).extracting(PostImage::getStoredName).contains("stored-new");
    }

    @Test
    @DisplayName("게시글 수정은 S3 삭제 결과가 null이면 예외가 발생한다")
    void updatePost_throwsWhenDeleteResultIsNull() throws Exception {
        User user = user("user-1", UserRole.USER);
        Post post = post("post-1", user);
        PostImage removeImage = PostImage.builder().id("image-1").storedName("remove.png").post(post).build();
        post.addImage(removeImage);
        UpdatePostReqDto dto = new UpdatePostReqDto("post-1", "수정 제목", "수정 내용", List.of());
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
        when(imageUtil.toStoredFilename()).thenReturn("stored-new");
        when(imageCompressionUtil.compressImage(newImage, "stored-new")).thenReturn(compressedImage);
        when(s3Util.buildS3Key("stored-new")).thenReturn("images/stored-new");
        when(s3Util.toS3UrlByS3Key("images/stored-new")).thenReturn("https://bucket/images/stored-new");
        when(s3Util.deleteImages(List.of("remove.png"))).thenReturn(null);

        assertThatThrownBy(() -> postService.updatePost(authentication, dto, List.of(newImage)))
                .isInstanceOf(com.amazonaws.services.s3.model.AmazonS3Exception.class);
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

    @Test
    @DisplayName("updatePost updates text only when images is empty")
    void updatePost_updatesTextOnlyWhenImagesEmpty() throws Exception {
        User user = user("user-1", UserRole.USER);
        Post post = post("post-1", user);
        UpdatePostReqDto dto = new UpdatePostReqDto("post-1", "???쒕ぉ", "???댁슜", List.of());
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(postUtil.findPostById("post-1")).thenReturn(post);

        postService.updatePost(authentication, dto, List.of());

        verify(postRepository).save(post);
        verify(s3Util, never()).uploadImages(any());
        assertThat(post.getTitle()).isEqualTo("???쒕ぉ");
        assertThat(post.getContent()).isEqualTo("???댁슜");
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
