package com.project.ds_helper.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class ClockConfigTest {

    @Test
    @DisplayName("Clock Bean은 Asia/Seoul 시간대를 사용한다")
    void clock_usesAsiaSeoulZone() {
        Clock clock = new ClockConfig().clock();

        assertThat(clock.getZone().getId()).isEqualTo("Asia/Seoul");
    }
}
