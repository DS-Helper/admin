package com.project.ds_helper.domain.volunteer.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class VolunteerConfig {

    @Bean
    public Clock volunteerClock() {
        return Clock.systemUTC();
    }
}
