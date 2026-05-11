package com.project.ds_helper.domain.post.util;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.domain.post.entity.PostImage;
import com.project.ds_helper.domain.post.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ImageUtil {

    private final S3Util s3Util;
    private final ImageRepository imageRepository;

    /**
     * 이미지 엔티티 빌드
     * **/
    public PostImage toPostImage(S3ImageUploadRequestDto imageUploadRequestDto, String s3Key){
            PostImage postImage = PostImage.builder()
                    .originalName(imageUploadRequestDto.getOriginalFilename())
                    .storedName(imageUploadRequestDto.getStoredFilename())
                    .url(s3Util.toS3UrlByS3Key(s3Key))
                    .size(imageUploadRequestDto.getSize())
                    .contentType(imageUploadRequestDto.getFileExtension())
                    .build();
            log.info("PostImage Successfully Built");
            return postImage;
    }

    /**
     * 이미지 엔티티 리스트 빌드
     * **/
    public List<PostImage> toPostImages(List<MultipartFile> images){
        List<PostImage> result = new ArrayList<>();

        if(!images.isEmpty()) {
            images.forEach( image -> {
                String originalFilename = image.getOriginalFilename();
                String storedFilename = this.toStoredFilename();
                String s3Key = s3Util.buildS3Key(storedFilename);
                log.info("originalFilename : {}, storedFilename : {}, s3Key : {}", originalFilename, storedFilename, s3Key);

                PostImage postImage = PostImage.builder()
                        .originalName(image.getOriginalFilename())
                        .storedName(storedFilename)
                        .url(s3Util.toS3UrlByStoredFilename(storedFilename))
                        .size(image.getSize())
                        .contentType(extractContentTypeFromRawContentType(image.getContentType()))
                        .build();
                result.add(postImage);
            });
        };
        return result;
    }
    
    /**
     * OriginalFilename을 StoredFilename으로 변경하고(단순 중복 방지용)
     * 필요 시
     * **/
    public String toStoredFilename(){
            String timeStamp = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String uuid = UUID.randomUUID().toString();
            log.info("timeStamp : {}, uuid : {}", timeStamp, uuid);
            String storedFilename = timeStamp + "_" + uuid;
            log.info("storedFilename : {}", storedFilename);
            return storedFilename;
    }

    public String extractContentTypeFromRawContentType(String rawContentType) {
        return Objects.requireNonNull(rawContentType).split("/")[1];
    }
}
