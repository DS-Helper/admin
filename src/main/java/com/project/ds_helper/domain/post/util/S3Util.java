package com.project.ds_helper.domain.post.util;

import com.amazonaws.services.cloudformation.model.OperationInProgressException;
import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Util {

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;
    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;
    @Value("${spring.cloud.aws.region.static}")
    private String region;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${spring.cloud.aws.s3.presigned-url-expiration-minutes:15}")
    private long presignedUrlExpirationMinutes;

    private final String ABSOLUTE_PATH_FOR_S3_IMAGE_KEY = "images/";
    private final String ABSOLUTE_PATH_FOR_S3_CERTIFICATION_KEY = "certifications/";

    private String ABSOLUTE_PATH_FOR_AWS_S3;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @PostConstruct
    public void init() {
        this.ABSOLUTE_PATH_FOR_AWS_S3 =
                "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        log.info("ABSOLUTE_PATH_FOR_AWS_S3 initialized: {}", ABSOLUTE_PATH_FOR_AWS_S3);
    }

    public void uploadImage(S3ImageUploadRequestDto dto) throws IOException {
        String storedFilename = dto.getStoredFilename();
        log.info("S3Util.uploadImage started. storedFilename={}, s3ContentType={}, fileSize={}",
                storedFilename, dto.getS3ContentType(), dto.getSize());

        validateUploadDto(dto);

        String imageUrl = toS3UrlByStoredFilename(storedFilename);
        log.info("S3Util.uploadImage targetUrl={}", imageUrl);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(buildS3Key(storedFilename))
                .contentType(dto.getS3ContentType())
                .build();

        log.info("bucket : {}, region : {}, accessKey : {}, secretKey : {}", bucket, region, accessKey, secretKey);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(dto.getBytes())) {
            if (!s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, dto.getSize())).sdkHttpResponse().isSuccessful()) {
                throw new IOException("S3 image save failed");
            }
        }

        log.info("S3Util.uploadImage completed. storedFilename={}", storedFilename);
    }

    /**
     * 복수개 이미지 업로드
     */
    public List<String> uploadImages(List<S3ImageUploadRequestDto> imageFiles) throws IOException {
        if (imageFiles.isEmpty()) {
            throw new IllegalArgumentException("no image files.");
        }

        log.info("S3Util.uploadImages started. imageCount={}", imageFiles.size());

        List<String> urls = new ArrayList<>();
        for (S3ImageUploadRequestDto imageFile : imageFiles) {
            validateUploadDto(imageFile);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(buildS3Key(imageFile.getStoredFilename()))
                    .contentType(imageFile.getS3ContentType())
                    .build();

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageFile.getBytes())) {
                if (!s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, imageFile.getSize())).sdkHttpResponse().isSuccessful()) {
                    throw new IOException("S3 image save failed");
                }
            }

            urls.add(request.key());
        }

        log.info("S3Util.uploadImages completed. uploadedCount={}", urls.size());
        return urls;
    }

    private void validateUploadDto(S3ImageUploadRequestDto dto) {
        if (dto == null
                || dto.getStoredFilename() == null || dto.getStoredFilename().isBlank()
                || dto.getBytes() == null
                || dto.getSize() == null
                || dto.getS3ContentType() == null || dto.getS3ContentType().isBlank()) {
            log.info("S3Util.validateUploadDto failed. dto={}", dto);
            throw new IllegalArgumentException("no files.");
        }
    }

    public void deleteImage(String storedFilename) {
        if (storedFilename.isEmpty()) {
            log.info("No storedFilename to delete.");
            throw new IllegalArgumentException("No storedFilename to delete.");
        }

        String builtS3Key = buildS3Key(storedFilename);
        log.info("builtS3Key to delete from s3 : {}", builtS3Key);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(builtS3Key)
                .build();

        if (!s3Client.deleteObject(request).sdkHttpResponse().isSuccessful()) {
            throw new OperationInProgressException("Image Delete Failed. Image s3Key : " + builtS3Key);
        }
    }

    public Object deleteImages(List<String> storedFilenames) {
        if (storedFilenames.isEmpty()) {
            log.info("No storedFilenames to delete");
            return false;
        }

        log.info("storedFilenames to delete from s3 is not empty. size : {}", storedFilenames.size());

        try {
            List<ObjectIdentifier> objectIdentifiers = storedFilenames.stream()
                    .map(filename -> {
                        String key = buildS3Key(filename);
                        return ObjectIdentifier.builder().key(key).build();
                    })
                    .toList();

            Delete delete = Delete.builder().objects(objectIdentifiers).build();

            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(delete)
                    .build();

            DeleteObjectsResponse response = s3Client.deleteObjects(request);

            List<String> errorKeys = response.errors().stream()
                    .map(S3Error::key)
                    .toList();

            if (!errorKeys.isEmpty()) {
                log.error("S3 delete failed for keys: {}", errorKeys);
                return false;
            }

            if (response.deleted().size() != storedFilenames.size()) {
                log.error("Deleted count mismatch. request={}, deleted={}",
                        storedFilenames.size(), response.deleted().size());
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("S3 delete error", e);
            return false;
        }
    }

    public Object deleteImagesByS3Key(List<String> s3Keys) {
        if (s3Keys.isEmpty()) {
            log.info("No s3Keys to delete");
            return false;
        }

        log.info("s3Keys to delete from s3 is not empty. size : {}", s3Keys.size());

        try {
            List<ObjectIdentifier> objectIdentifiers = s3Keys.stream()
                    .map(s3Key -> ObjectIdentifier.builder().key(s3Key).build())
                    .toList();

            Delete delete = Delete.builder().objects(objectIdentifiers).build();

            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(delete)
                    .build();

            DeleteObjectsResponse response = s3Client.deleteObjects(request);

            List<String> errorKeys = response.errors().stream()
                    .map(S3Error::key)
                    .toList();

            if (!errorKeys.isEmpty()) {
                log.error("S3 delete failed for keys: {}", errorKeys);
                return false;
            }

            if (response.deleted().size() != s3Keys.size()) {
                log.error("Deleted count mismatch. request={}, deleted={}",
                        s3Keys.size(), response.deleted().size());
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("S3 delete error", e);
            return false;
        }
    }

    public String toS3UrlByS3Key(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) throw new IllegalArgumentException("no s3Key to generate presigned url.");
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(s3Key).build();
        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                        .getObjectRequest(getObjectRequest)
                        .build())
                .url()
                .toString();
    }

    public String toS3UrlByStoredFilename(String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()) {
            throw new IllegalArgumentException("no storedFilename to generate tos3url.");
        }
        return toS3UrlByS3Key(buildS3Key(storedFilename));
    }

    public String buildS3Key(String storedFilename) {
        String s3Key = ABSOLUTE_PATH_FOR_S3_IMAGE_KEY + storedFilename;
        log.info("s3Key : {}", s3Key);
        return s3Key;
    }

    public String extractStoredFilenameFromS3Key(String s3Key) {
        String extractedStoredFilename = s3Key.split(ABSOLUTE_PATH_FOR_S3_IMAGE_KEY)[1];
        log.info("extractedStoredFilename : {}", extractedStoredFilename);
        return extractedStoredFilename;
    }

    /**
     * (Client로부터 S3Url을 받아 StoredFilename(S3Key)을 추출
     */
    public String extractFilenameFromS3Url(String imageUrl) {
        String extractedS3Key = extractS3KeyFromS3Url(imageUrl);
        String extractedStoredFilename = extractedS3Key.substring(ABSOLUTE_PATH_FOR_S3_IMAGE_KEY.length());
        log.info("extractedStoredFilename : {}", extractedStoredFilename);
        return extractedStoredFilename;
    }

    public boolean isManagedS3Url(String imageUrl) {
        return imageUrl != null && !imageUrl.isBlank() && imageUrl.startsWith(ABSOLUTE_PATH_FOR_AWS_S3);
    }

    public String extractS3KeyFromS3Url(String imageUrl) {
        if (!isManagedS3Url(imageUrl)) {
            throw new IllegalArgumentException("invalid managed s3 url");
        }

        String path = URI.create(imageUrl).getPath();
        if (path == null || path.length() <= 1) throw new IllegalArgumentException("invalid managed s3 url");
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
