package com.project.ds_helper.domain.user.restTemplate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean(name = "customRestTemplate")
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
