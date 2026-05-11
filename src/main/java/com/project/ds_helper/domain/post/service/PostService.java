package com.project.ds_helper.domain.post.service;

import com.amazonaws.services.s3.model.AmazonS3Exception;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

        private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "viewCount");

        private final PostRepository postRepository;
        private final UserUtil userUtil;
        private final ImageCompressionUtil imageCompressionUtil;
        private final ImageUtil imageUtil;
        private final PostUtil postUtil;
        private final S3Util s3Util;

        /**
         * ?좎???紐⑤뱺 寃뚯떆湲 議고쉶
         * **/
        public GetAllPostOfUserResDto getAllPostOfUser(String keyword, int page, int size, String sort, String sortBy) { // Authentication authentication,
            // 1. 잘못된 정렬 요청을 먼저 차단한다.
            validateSortBy(sortBy);
            // 2. soft delete 회원의 게시글은 repository 레벨에서 제외된 결과만 조회한다.
            Pageable pageRequest = PageRequest.of(page, size, sort.equalsIgnoreCase("desc")? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
            Page<Post> posts = findPostsByKeyword(keyword, pageRequest);
            GetAllPostOfUserResDto response = GetAllPostOfUserResDto.toDtoList(posts);
            log.debug("寃뚯떆湲 由ъ뒪??議고쉶");
            return response;
        }

        private Page<Post> findPostsByKeyword(String keyword, Pageable pageRequest) {
            if (keyword == null || keyword.isBlank()) {
                return postRepository.findAll(pageRequest);
            }
            return postRepository.findByTitleContaining(keyword, pageRequest);
        }

        private void validateSortBy(String sortBy) {
            if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
                throw new IllegalArgumentException("Invalid sortBy");
            }
        }

        /**
         * ?④굔 寃뚯떆臾?議고쉶
         * **/
        @Transactional
        public GetPostResDto getOnePost(String postId) {

            // 寃뚯떆湲 議고쉶??利앷?
            int resultToIncreaseViewCount = postRepository.increaseViewCount(postId);
            if(resultToIncreaseViewCount != 1){
                throw new RuntimeException("View Count Increase Failed");
            }

            // 寃뚯떆湲 議고쉶
            Post post = postRepository.findVisibleById(postId).orElseThrow(() -> new IllegalArgumentException("寃뚯떆臾?議고쉶 ?ㅽ뙣, 寃뚯떆臾?ID : " + postId));
            GetPostResDto responseDto = GetPostResDto.toDto(post);
            log.debug("寃뚯떆湲 議고쉶 ?꾨즺");
            return responseDto;
        }

        /**
         * ?좉퇋 寃뚯떆湲 ?묒꽦
         * 諛섑솚 ????섏젙 ?꾩슂
         * **/
        @Transactional
        public void createPost(Authentication authentication, CreatePostReqDto dto, List<MultipartFile> images) throws IOException {
            if(dto.getTitle() == null || dto.getContent() == null){throw new IllegalArgumentException("dto is invalid");}
            String title = dto.getTitle();
            String content = dto.getContent();
            log.debug("title : {}, content : {}, imagesSize : {}",title, content, images == null? 0 : images.size());

            // ?좎? id 異붿텧
            String userId = userUtil.extractUserId(authentication);

            // ?좎? 議고쉶
            User user = userUtil.findUserById(userId);

            log.debug("userRole : {}", user.getRole());
            // if(user.getRole() != UserRole.ADMIN ) throw new AccessDeniedException("Access Denied");

            // 寃뚯떆湲 ?뷀떚??鍮뚮뱶
            Post post = dto.toPost(dto, user);
            log.debug("Post built successfully");

            if(images != null && !images.isEmpty()){
                log.debug("images size : {}", images.size());

                List<PostImage> postImages = new ArrayList<>();
                List<S3ImageUploadRequestDto> s3ImageUploadRequestDtos = new ArrayList<>();
                for (MultipartFile image : images) {
                    String originalFilename = image.getOriginalFilename();
                    String storedFilename = imageUtil.toStoredFilename();
                    String s3Key = s3Util.buildS3Key(storedFilename);
                    log.debug("originalFilename : {}, storedFilename : {}, s3Key : {}", originalFilename, storedFilename, s3Key);
                    S3ImageUploadRequestDto compressedImage = imageCompressionUtil.compressImage(image, storedFilename);
                    postImages.add(imageUtil.toPostImage(compressedImage, s3Key));
                    s3ImageUploadRequestDtos.add(compressedImage);
                }

                log.debug("PostImages Successfully Built. size : {}", postImages.size());

                postImages.forEach(post::addImage);

                postRepository.save(post);
                log.debug("post saved");

                s3Util.uploadImages(s3ImageUploadRequestDtos);
                log.debug("S3 Image Saved");
            }else{
                postRepository.save(post);
                log.debug("Post Saved");
            }
        }
        
        /**
         * 寃뚯떆湲 ?섏젙
         * ?대?吏 由ъ뒪???섏젙 湲곕뒫 ?뺤씤
         * Dto ?섏젙 ?꾩슂 (?꾨줎?몄륫 ?듬? ?ㅻ㈃ )
         * 寃뚯떆湲 ?묒꽦?먭? ?꾨땲硫??덉쇅
         * ??젣, ?좉퇋 異붽? ???대?吏 泥섎━
         * s3 ?대?吏 泥섎━
         * 
         * 湲곗〈 ?대?吏 url ?덈컺怨?
         * ?좉퇋 ?대?吏濡??꾨? 泥섎━ 媛??( 怨듭쑀 ?붾쭩 )
         * **/
        @Transactional
        public void updatePost(Authentication authentication, UpdatePostReqDto dto, List<MultipartFile> images) throws IOException {
            if(dto.getTitle() == null || dto.getContent() == null || dto.getPostId() == null){throw new IllegalArgumentException("dto is invalid");}
            String title = dto.getTitle();
            String content = dto.getContent();
            List<String> imageUrls = dto.getImageUrls();
            List<String> storedFilenamesExtractedFromS3Urls = imageUrls != null? imageUrls.stream().map(s3Util::extractFilenameFromS3Url).toList() : new ArrayList<>();
            log.debug("title : {}, content : {}, imageUrlsSize : {}, imagesSize : {}, storedFilenamesExtractedFromS3Urls : {}",title, content, dto.getImageUrls() == null? 0 : dto.getImageUrls().size(), images == null? 0 : images.size(), storedFilenamesExtractedFromS3Urls);

            String userId = userUtil.extractUserId(authentication);
            User user = userUtil.findUserById(userId);

            String postId = dto.getPostId();
            log.debug("postId : {}", postId);

            Post post = postUtil.findPostById(postId);

            // 1. 작성자 본인 또는 관리자만 게시글을 수정할 수 있다.
            if(!post.getUser().getId().equals(userId) && user.getRole() != UserRole.ADMIN){
                log.debug("post update rejected. requester is neither writer nor admin. userId={}, writerId={}, role={}", userId, post.getUser().getId(), user.getRole());
                throw new IllegalArgumentException("Only Writer Can Update Post");
            }

            if(images != null && !images.isEmpty()) {
                List<PostImage> oldPostImagesOfPost = post.getPostImages();
                List<PostImage> imagesToDelete = new ArrayList<>();
                oldPostImagesOfPost.forEach(postImage -> {
                    log.debug("postImage.storedFilename : {}", postImage.getStoredName());
                    if (!storedFilenamesExtractedFromS3Urls.contains(postImage.getStoredName())) {
                        imagesToDelete.add(postImage);
                        log.debug("postImage is added to imagesToDelete. postImage.id : {}", postImage.getId());
                    }
                });

                log.debug("updatePost image sync started. deleteTargetCount={}, newImageCount={}", imagesToDelete.size(), images.size());

                List<S3ImageUploadRequestDto> compressedImages = new ArrayList<>();
                List<PostImage> newPostImages = new ArrayList<>();
                for (MultipartFile image : images) {
                    String storedFilename = imageUtil.toStoredFilename();
                    S3ImageUploadRequestDto compressedImage = imageCompressionUtil.compressImage(image, storedFilename);
                    compressedImages.add(compressedImage);

                    newPostImages.add(PostImage.builder()
                            .originalName(compressedImage.getOriginalFilename())
                            .storedName(storedFilename)
                            .post(post)
                            .size(compressedImage.getSize())
                            .url(s3Util.toS3UrlByS3Key(s3Util.buildS3Key(storedFilename)))
                            .contentType(compressedImage.getFileExtension())
                            .build());
                }
                log.debug("newPostImages is successfully built. size : {}", newPostImages.size());

                newPostImages.forEach(post::addImage);
                log.debug("newPostImages is added to Post");

                if(!imagesToDelete.isEmpty()) {
                    imagesToDelete.forEach(post::removeImage);
                    log.debug("postimages removed from post");

                    Object deleteS3ImagesResult = s3Util.deleteImages(imagesToDelete.stream().map(PostImage::getStoredName).toList());
                    if(deleteS3ImagesResult == null){
                        log.debug("deleteS3ImageResult is null. result : {}", deleteS3ImagesResult);
                        throw new AmazonS3Exception("Delete S3 Images Result is Null");
                    }
                    log.debug("successfully deleted images from s3");
                }

                s3Util.uploadImages(compressedImages);
                log.debug("Images uploaded to S3 successfully");
            }

            postRepository.save(dto.toUpdatedPost(dto, post));
        }
        /**
         * 寃뚯떆湲 ??젣 
         * S3 ?대?吏 ??젣
         * **/
        @Transactional
        public void deletePost(Authentication authentication, String postId) throws AccessDeniedException {
            
            String userId = userUtil.extractUserId(authentication);
            User user = userUtil.findUserById(userId);

            if(user.getRole() != UserRole.ADMIN){throw new AccessDeniedException("Only Admin May Access");}
    
            Post post = postUtil.findPostById(postId);
            List<String> storedFilenamesToDelete = post.getPostImages().stream().map(PostImage::getStoredName).toList();

            s3Util.deleteImages(storedFilenamesToDelete);
            postUtil.deletePostById(postId);
        }

}
