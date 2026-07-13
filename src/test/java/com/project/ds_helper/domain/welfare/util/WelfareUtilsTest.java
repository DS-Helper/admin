package com.project.ds_helper.domain.welfare.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WelfareUtilsTest {

    @Test
    @DisplayName("나이에 따라 생애주기 코드가 반환된다")
    void convertAgeToLifeCycleCode_mapsAge() {
        assertThat(WelfareUtils.convertAgeToLifeCycleCode(3)).isEqualTo("001");
        assertThat(WelfareUtils.convertAgeToLifeCycleCode(10)).isEqualTo("002");
        assertThat(WelfareUtils.convertAgeToLifeCycleCode(15)).isEqualTo("003");
        assertThat(WelfareUtils.convertAgeToLifeCycleCode(25)).isEqualTo("004");
        assertThat(WelfareUtils.convertAgeToLifeCycleCode(50)).isEqualTo("005");
        assertThat(WelfareUtils.convertAgeToLifeCycleCode(70)).isEqualTo("006");
    }

    @Test
    @DisplayName("생애주기 코드는 한글 이름으로 변환된다")
    void getLifeCycleName_mapsCode() {
        assertThat(WelfareUtils.getLifeCycleName("001")).isEqualTo("영유아");
        assertThat(WelfareUtils.getLifeCycleName("007")).isEqualTo("임신/출산");
        assertThat(WelfareUtils.getLifeCycleName("999")).isEqualTo("알 수 없음");
    }
}
