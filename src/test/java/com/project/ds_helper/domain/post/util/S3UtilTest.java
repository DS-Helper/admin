package com.project.ds_helper.domain.post.util;

import com.amazonaws.services.cloudformation.model.OperationInProgressException;
import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Error;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3UtilTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private S3Presigner s3Presigner;

    private S3Util s3Util;

    @BeforeEach
    void setUp() {
        s3Util = new S3Util(s3Client, s3Presigner);
        ReflectionTestUtils.setField(s3Util, "bucket", "bucket");
        ReflectionTestUtils.setField(s3Util, "region", "ap-northeast-2");
        ReflectionTestUtils.setField(s3Util, "accessKey", "access");
        ReflectionTestUtils.setField(s3Util, "secretKey", "secret");
        ReflectionTestUtils.setField(s3Util, "presignedUrlExpirationMinutes", 15L);
        s3Util.init();
        PresignedGetObjectRequest request = org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
        org.mockito.Mockito.lenient().when(s3Presigner.presignGetObject(org.mockito.ArgumentMatchers.<software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest>any()))
                .thenReturn(request);
        try {
            org.mockito.Mockito.lenient().when(request.url())
                    .thenReturn(new URL("https://bucket.s3.ap-northeast-2.amazonaws.com/images/stored.webp?X-Amz-Signature=test"));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    @DisplayName("단건 이미지를 S3에 업로드한다")
    void uploadImage_uploadsObject() throws Exception {
        PutObjectResponse response = putObjectResponse(200);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        s3Util.uploadImage(uploadDto("stored.webp"));

        assertThat(s3Util.toS3UrlByStoredFilename("stored.webp"))
                .contains("images/stored.webp")
                .contains("X-Amz-Signature");
    }

    @Test
    @DisplayName("단건 이미지 업로드는 S3 응답이 실패면 IOException을 던진다")
    void uploadImage_throwsWhenS3ResponseFails() {
        PutObjectResponse response = putObjectResponse(500);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> s3Util.uploadImage(uploadDto("stored.webp")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("S3 image save failed");
    }

    @Test
    @DisplayName("이미지 업로드 DTO가 유효하지 않으면 예외가 발생한다")
    void uploadImage_throwsWhenDtoInvalid() {
        assertThatThrownBy(() -> s3Util.uploadImage(S3ImageUploadRequestDto.builder()
                        .storedFilename("")
                        .bytes("image".getBytes())
                        .size(5L)
                        .s3ContentType("image/webp")
                        .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no files.");
    }

    @Test
    @DisplayName("복수 이미지를 S3에 업로드하고 key 목록을 반환한다")
    void uploadImages_uploadsObjects() throws Exception {
        PutObjectResponse response = putObjectResponse(200);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        List<String> result = s3Util.uploadImages(List.of(uploadDto("a.webp"), uploadDto("b.webp")));

        assertThat(result).containsExactly("images/a.webp", "images/b.webp");
    }

    @Test
    @DisplayName("복수 이미지 업로드는 빈 목록이면 예외가 발생한다")
    void uploadImages_throwsWhenEmpty() {
        assertThatThrownBy(() -> s3Util.uploadImages(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no image files.");
    }

    @Test
    @DisplayName("uploadImages rejects null DTO")
    void uploadImages_throwsWhenDtoIsNull() {
        assertThatThrownBy(() -> s3Util.uploadImages(java.util.Collections.singletonList(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no files.");
    }

    @Test
    @DisplayName("uploadImages rejects invalid DTO fields")
    void uploadImages_throwsWhenDtoFieldsInvalid() {
        assertThatThrownBy(() -> s3Util.uploadImages(List.of(invalidUploadDto(null, "image".getBytes(), 5L, "image/webp"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no files.");
        assertThatThrownBy(() -> s3Util.uploadImages(List.of(invalidUploadDto("   ", "image".getBytes(), 5L, "image/webp"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no files.");
        assertThatThrownBy(() -> s3Util.uploadImages(List.of(invalidUploadDto("stored.webp", null, 5L, "image/webp"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no files.");
        assertThatThrownBy(() -> s3Util.uploadImages(List.of(invalidUploadDto("stored.webp", "image".getBytes(), null, "image/webp"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no files.");
        assertThatThrownBy(() -> s3Util.uploadImages(List.of(invalidUploadDto("stored.webp", "image".getBytes(), 5L, null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no files.");
        assertThatThrownBy(() -> s3Util.uploadImages(List.of(invalidUploadDto("stored.webp", "image".getBytes(), 5L, "   "))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no files.");
    }

    @Test
    @DisplayName("uploadImages throws IOException when S3 response fails")
    void uploadImages_throwsWhenS3ResponseFails() {
        PutObjectResponse response = putObjectResponse(500);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> s3Util.uploadImages(List.of(uploadDto("a.webp"))))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("S3 image save failed");
    }

    @Test
    @DisplayName("단건 이미지를 S3에서 삭제한다")
    void deleteImage_deletesObject() {
        DeleteObjectResponse response = deleteObjectResponse(204);
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(response);

        s3Util.deleteImage("stored.webp");
    }

    @Test
    @DisplayName("단건 이미지 삭제는 파일명이 비어 있으면 예외가 발생한다")
    void deleteImage_throwsWhenFilenameEmpty() {
        assertThatThrownBy(() -> s3Util.deleteImage(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No storedFilename to delete.");
    }

    @Test
    @DisplayName("단건 이미지 삭제는 S3 응답이 실패면 예외가 발생한다")
    void deleteImage_throwsWhenS3ResponseFails() {
        DeleteObjectResponse response = deleteObjectResponse(500);
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(response);

        assertThatThrownBy(() -> s3Util.deleteImage("stored.webp"))
                .isInstanceOf(OperationInProgressException.class);
    }

    @Test
    @DisplayName("저장 파일명 목록으로 복수 이미지를 삭제한다")
    void deleteImages_returnsTrueWhenAllDeleted() {
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder()
                        .deleted(DeletedObject.builder().key("images/a.webp").build())
                        .build());

        Object result = s3Util.deleteImages(List.of("a.webp"));

        assertThat(result).isEqualTo(true);
    }

    @Test
    @DisplayName("저장 파일명 목록 삭제는 빈 목록이면 false를 반환한다")
    void deleteImages_returnsFalseWhenEmpty() {
        assertThat(s3Util.deleteImages(List.of())).isEqualTo(false);
    }

    @Test
    @DisplayName("저장 파일명 목록 삭제는 S3 에러가 있으면 false를 반환한다")
    void deleteImages_returnsFalseWhenErrorsExist() {
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder()
                        .errors(S3Error.builder().key("images/a.webp").build())
                        .build());

        assertThat(s3Util.deleteImages(List.of("a.webp"))).isEqualTo(false);
    }

    @Test
    @DisplayName("저장 파일명 목록 삭제는 삭제 개수가 다르면 false를 반환한다")
    void deleteImages_returnsFalseWhenDeletedCountMismatch() {
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder().build());

        assertThat(s3Util.deleteImages(List.of("a.webp"))).isEqualTo(false);
    }

    @Test
    @DisplayName("저장 파일명 목록 삭제는 S3 예외가 발생하면 false를 반환한다")
    void deleteImages_returnsFalseWhenExceptionOccurs() {
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenThrow(AwsServiceException.builder().message("error").build());

        assertThat(s3Util.deleteImages(List.of("a.webp"))).isEqualTo(false);
    }

    @Test
    @DisplayName("S3 key 목록으로 복수 이미지를 삭제한다")
    void deleteImagesByS3Key_returnsTrueWhenAllDeleted() {
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder()
                        .deleted(DeletedObject.builder().key("images/a.webp").build())
                        .build());

        assertThat(s3Util.deleteImagesByS3Key(List.of("images/a.webp"))).isEqualTo(true);
    }

    @Test
    @DisplayName("S3 key 목록 삭제는 빈 목록이면 false를 반환한다")
    void deleteImagesByS3Key_returnsFalseWhenEmpty() {
        assertThat(s3Util.deleteImagesByS3Key(List.of())).isEqualTo(false);
    }

    @Test
    @DisplayName("S3 key 목록 삭제는 S3 에러가 있으면 false를 반환한다")
    void deleteImagesByS3Key_returnsFalseWhenErrorsExist() {
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder()
                        .errors(S3Error.builder().key("images/a.webp").build())
                        .build());

        assertThat(s3Util.deleteImagesByS3Key(List.of("images/a.webp"))).isEqualTo(false);
    }

    @Test
    @DisplayName("S3 key 목록 삭제는 삭제 개수가 다르면 false를 반환한다")
    void deleteImagesByS3Key_returnsFalseWhenDeletedCountMismatch() {
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder().build());

        assertThat(s3Util.deleteImagesByS3Key(List.of("images/a.webp"))).isEqualTo(false);
    }

    @Test
    @DisplayName("S3 key 목록 삭제는 S3 예외가 발생하면 false를 반환한다")
    void deleteImagesByS3Key_returnsFalseWhenExceptionOccurs() {
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenThrow(AwsServiceException.builder().message("error").build());

        assertThat(s3Util.deleteImagesByS3Key(List.of("images/a.webp"))).isEqualTo(false);
    }

    @Test
    @DisplayName("S3 URL과 key 변환을 수행한다")
    void convertsS3UrlAndKey() {
        String url = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/stored.webp?X-Amz-Signature=test";

        assertThat(s3Util.buildS3Key("stored.webp")).isEqualTo("images/stored.webp");
        assertThat(s3Util.toS3UrlByS3Key("images/stored.webp")).contains("images/stored.webp");
        assertThat(s3Util.toS3UrlByStoredFilename("stored.webp")).contains("images/stored.webp");
        assertThat(s3Util.extractStoredFilenameFromS3Key("images/stored.webp")).isEqualTo("stored.webp");
        assertThat(s3Util.extractFilenameFromS3Url(url)).isEqualTo("stored.webp");
        assertThat(s3Util.isManagedS3Url(url)).isTrue();
        assertThat(s3Util.isManagedS3Url("https://other/images/stored.webp")).isFalse();
        assertThat(s3Util.isManagedS3Url("")).isFalse();
        assertThat(s3Util.isManagedS3Url(null)).isFalse();
        assertThat(s3Util.extractS3KeyFromS3Url(url)).isEqualTo("images/stored.webp");
    }

    @Test
    @DisplayName("빈 key나 URL 변환 요청은 예외가 발생한다")
    void conversionsThrowWhenInvalid() {
        assertThatThrownBy(() -> s3Util.toS3UrlByS3Key(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s3Util.toS3UrlByS3Key(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s3Util.toS3UrlByStoredFilename(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s3Util.toS3UrlByStoredFilename(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s3Util.extractS3KeyFromS3Url("https://other/images/stored.webp"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s3Util.extractS3KeyFromS3Url("https://bucket.s3.ap-northeast-2.amazonaws.com/"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private S3ImageUploadRequestDto uploadDto(String storedFilename) {
        return S3ImageUploadRequestDto.builder()
                .storedFilename(storedFilename)
                .originalFilename("original.png")
                .bytes("image".getBytes())
                .size(5L)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();
    }

    private S3ImageUploadRequestDto invalidUploadDto(String storedFilename, byte[] bytes, Long size, String s3ContentType) {
        return S3ImageUploadRequestDto.builder()
                .storedFilename(storedFilename)
                .originalFilename("original.png")
                .bytes(bytes)
                .size(size)
                .s3ContentType(s3ContentType)
                .fileExtension("webp")
                .build();
    }

    private PutObjectResponse putObjectResponse(int statusCode) {
        PutObjectResponse response = org.mockito.Mockito.mock(PutObjectResponse.class);
        when(response.sdkHttpResponse()).thenReturn(SdkHttpResponse.builder().statusCode(statusCode).build());
        return response;
    }

    private DeleteObjectResponse deleteObjectResponse(int statusCode) {
        DeleteObjectResponse response = org.mockito.Mockito.mock(DeleteObjectResponse.class);
        when(response.sdkHttpResponse()).thenReturn(SdkHttpResponse.builder().statusCode(statusCode).build());
        return response;
    }
}
