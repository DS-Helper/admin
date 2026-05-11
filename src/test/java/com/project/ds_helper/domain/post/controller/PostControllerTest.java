package com.project.ds_helper.domain.post.controller;

import com.project.ds_helper.domain.post.dto.request.CreatePostReqDto;
import com.project.ds_helper.domain.post.dto.request.UpdatePostReqDto;
import com.project.ds_helper.domain.post.dto.response.GetAllPostOfUserResDto;
import com.project.ds_helper.domain.post.dto.response.GetPostResDto;
import com.project.ds_helper.domain.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

    @Test
    @DisplayName("게시글 목록 조회는 서비스 결과를 반환한다")
    void getAllPostOfUser_returnsServiceResult() {
        GetAllPostOfUserResDto dto = new GetAllPostOfUserResDto(List.of(), null);
        when(postService.getAllPostOfUser(null, 0, 10, "desc", "createdAt")).thenReturn(dto);

        ResponseEntity<GetAllPostOfUserResDto> response = postController.getAllPostOfUser(null, 0, 10, "desc", "createdAt");

        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("게시글 목록 조회 시 keyword를 서비스에 전달한다")
    void getAllPostOfUser_passesKeywordToService() {
        GetAllPostOfUserResDto dto = new GetAllPostOfUserResDto(List.of(), null);
        when(postService.getAllPostOfUser("검색", 0, 10, "desc", "createdAt")).thenReturn(dto);

        ResponseEntity<GetAllPostOfUserResDto> response = postController.getAllPostOfUser("검색", 0, 10, "desc", "createdAt");

        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("게시글 단건 조회는 서비스 결과를 반환한다")
    void getOnePost_returnsServiceResult() {
        GetPostResDto dto = GetPostResDto.builder().postId("post-1").title("제목").build();
        when(postService.getOnePost("post-1")).thenReturn(dto);

        ResponseEntity<GetPostResDto> response = postController.getOnePost("post-1");

        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("게시글 생성은 서비스 호출 후 201을 반환한다")
    void createPost_returnsCreated() throws Exception {
        CreatePostReqDto dto = new CreatePostReqDto("제목", "내용");

        ResponseEntity<?> response = postController.createPost(null, dto, null);

        verify(postService).createPost(null, dto, null);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    @DisplayName("게시글 수정은 서비스 호출 후 200을 반환한다")
    void updatePost_returnsOk() throws Exception {
        UpdatePostReqDto dto = new UpdatePostReqDto("post-1", "수정 제목", "수정 내용", List.of());

        ResponseEntity<?> response = postController.updatePost(null, dto, null);

        verify(postService).updatePost(null, dto, null);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("게시글 삭제는 서비스 호출 후 204를 반환한다")
    void deletePost_returnsNoContent() throws Exception {
        ResponseEntity<?> response = postController.deletePost(null, "post-1");

        verify(postService).deletePost(null, "post-1");
        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }
}
