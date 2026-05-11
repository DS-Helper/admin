package com.project.ds_helper.common.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Component
// Component로 변경.......... 느낌상 설정이 맞는데... Bean객체가......음.... Compoenet로 사용 되긴 하는데... 음.... Componenet로 하는게 맞겠다...
public class S3Config  {

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${spring.cloud.aws.region.static}")
    private String region;


    @Bean
    @Primary
    public AwsCredentialsProvider customAwsCredentialsProvider() {
        return () -> new AwsCredentials() {
            @Override
            public String accessKeyId() {
                return accessKey;
            }

            @Override
            public String secretAccessKey() {
                return secretKey;
            }
        };
    }

    @Bean
    @Primary
    public S3Client s3Client() {
        return S3Client.builder()
                .credentialsProvider(customAwsCredentialsProvider())
//                .endpointOverride(URI.create("https://s3.ap-northeast-2.amazonaws.com"))
                .region(Region.AP_NORTHEAST_2)
                .build();
    }

    //region 객체 주입
    @Bean
    public AwsRegionProvider customAwsRegionProvider() {
        return () -> Region.of(region);
    }

}

