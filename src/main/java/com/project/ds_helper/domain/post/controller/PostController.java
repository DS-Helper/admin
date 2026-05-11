package com.project.ds_helper.domain.post.controller;

import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.post.dto.request.CreatePostReqDto;
import com.project.ds_helper.domain.post.dto.request.UpdatePostReqDto;
import com.project.ds_helper.domain.post.dto.response.GetAllPostOfUserResDto;
import com.project.ds_helper.domain.post.dto.response.GetPostResDto;
import com.project.ds_helper.domain.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @Tag(name = SwaggerTagName.STORY)
    @Operation(summary = "전체 게시글 조회(도와드린 이야기: 고객센터)")
    @ApiResponse(responseCode = "200", useReturnTypeSchema = true)
    @GetMapping("")
    public ResponseEntity<GetAllPostOfUserResDto> getAllPostOfUser(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        return ResponseEntity.ok(postService.getAllPostOfUser(keyword, page, size, sort, sortBy));
    }

    @Tag(name = SwaggerTagName.STORY)
    @Operation(summary = "단건 게시글 조회(도와드린 이야기: 고객센터)")
    @GetMapping("/{postId}")
    public ResponseEntity<GetPostResDto> getOnePost(@PathVariable("postId") String postId) {
        return ResponseEntity.ok(postService.getOnePost(postId));
    }

    @Tag(name = SwaggerTagName.STORY)
    @Operation(summary = "일반 게시글 생성(고객센터: 관리자) (JWT 인증 필요)")
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createPost(
            Authentication authentication,
            @Parameter(
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE),
                    schema = @Schema(implementation = CreatePostReqDto.class)
            )
            @RequestPart(value = "dto", required = false) @Valid CreatePostReqDto dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        postService.createPost(authentication, dto, images);
        return new ResponseEntity<>(null, HttpStatus.CREATED);
    }

    @Tag(name = SwaggerTagName.STORY)
    @Operation(summary = "게시글 수정(고객센터: 관리자) (JWT 인증 필요)")
    @PutMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updatePost(
            Authentication authentication,
            @Parameter(
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE),
                    schema = @Schema(implementation = UpdatePostReqDto.class)
            )
            @RequestPart("dto") @Valid UpdatePostReqDto dto,
            @Parameter(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))
                    )
            )
            @RequestPart(name = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        postService.updatePost(authentication, dto, images);
        return ResponseEntity.ok(null);
    }

    @Tag(name = SwaggerTagName.STORY)
    @Operation(summary = "게시글 삭제(고객센터: 관리자) (JWT 인증 필요)")
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(
            Authentication authentication,
            @PathVariable("postId") String postId
    ) throws AccessDeniedException {
        postService.deletePost(authentication, postId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
