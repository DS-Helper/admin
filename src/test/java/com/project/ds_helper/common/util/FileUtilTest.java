package com.project.ds_helper.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUtilTest {

    @Test
    @DisplayName("파일 크기와 확장자가 유효하면 true를 반환한다")
    void isValidSizeAndExtension_returnsTrue() throws Exception {
        FileUtil fileUtil = new FileUtil();
        ReflectionTestUtils.setField(fileUtil, "maxSize", org.springframework.util.unit.DataSize.ofBytes(10));
        ReflectionTestUtils.setField(fileUtil, "allowedFileExtensions", new String[]{"png"});
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});

        assertThat(fileUtil.isValidSizeAndExtension(file)).isTrue();
    }

    @Test
    @DisplayName("파일 크기가 크면 예외가 발생한다")
    void isValidSizeAndExtension_throwsWhenTooLarge() {
        FileUtil fileUtil = new FileUtil();
        ReflectionTestUtils.setField(fileUtil, "maxSize", org.springframework.util.unit.DataSize.ofBytes(1));
        ReflectionTestUtils.setField(fileUtil, "allowedFileExtensions", new String[]{"png"});
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2});

        assertThatThrownBy(() -> fileUtil.isValidSizeAndExtension(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("File Is Empty Or Size Invalid");
    }

    @Test
    @DisplayName("허용 확장자가 아니면 false를 반환한다")
    void isValidSizeAndExtension_returnsFalseWhenExtensionInvalid() throws Exception {
        FileUtil fileUtil = new FileUtil();
        ReflectionTestUtils.setField(fileUtil, "maxSize", org.springframework.util.unit.DataSize.ofBytes(10));
        ReflectionTestUtils.setField(fileUtil, "allowedFileExtensions", new String[]{"png"});
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1});

        assertThat(fileUtil.isValidSizeAndExtension(file)).isFalse();
    }

    @Test
    @DisplayName("리스트 null/empty는 빈 리스트로 바뀐다")
    void checkIfListIsNull_returnsEmptyList() {
        FileUtil fileUtil = new FileUtil();

        assertThat(fileUtil.checkIfListIsNull(null)).isEmpty();
        assertThat(fileUtil.checkIfListIsNull(List.of())).isEmpty();
    }

    @Test
    @DisplayName("리스트 크기가 1보다 크면 예외가 발생한다")
    void checkIfListSizeNotBiggerThanOne_throws() {
        FileUtil fileUtil = new FileUtil();

        assertThatThrownBy(() -> fileUtil.checkIfListSizeNotBiggerThanOne(List.of(
                new MockMultipartFile("a", "a.png", "image/png", new byte[]{1}),
                new MockMultipartFile("b", "b.png", "image/png", new byte[]{1})
        ))).isInstanceOf(IllegalArgumentException.class);
    }
}
