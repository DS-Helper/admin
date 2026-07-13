package com.project.ds_helper.common.s3;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

class S3ConfigTest {

    @Test
    @DisplayName("S3 설정은 자격 증명 공급자와 리전 공급자를 만든다")
    void createsBeans() {
        S3Config config = new S3Config();
        ReflectionTestUtils.setField(config, "accessKey", "access");
        ReflectionTestUtils.setField(config, "secretKey", "secret");
        ReflectionTestUtils.setField(config, "region", "ap-northeast-2");

        AwsCredentialsProvider credentialsProvider = config.customAwsCredentialsProvider();
        AwsRegionProvider regionProvider = config.customAwsRegionProvider();
        S3Client client = config.s3Client();

        assertThat(credentialsProvider.resolveCredentials().accessKeyId()).isEqualTo("access");
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("secret");
        assertThat(regionProvider.getRegion().id()).isEqualTo("ap-northeast-2");
        assertThat(client).isNotNull();
    }
}
