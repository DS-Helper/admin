package com.project.ds_helper.common.util;

import com.project.ds_helper.domain.post.entity.Post;
import com.project.ds_helper.domain.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostUtilTest {

    @Test
    @DisplayName("보이는 게시글을 조회한다")
    void findPostById_returnsPost() {
        PostRepository repository = mock(PostRepository.class);
        when(repository.findVisibleById("post-1")).thenReturn(Optional.of(Post.builder().id("post-1").build()));
        PostUtil postUtil = new PostUtil(repository);

        assertThat(postUtil.findPostById("post-1").getId()).isEqualTo("post-1");
    }

    @Test
    @DisplayName("게시글이 없으면 예외가 발생한다")
    void findPostById_throwsWhenMissing() {
        PostRepository repository = mock(PostRepository.class);
        when(repository.findVisibleById("post-1")).thenReturn(Optional.empty());
        PostUtil postUtil = new PostUtil(repository);

        assertThatThrownBy(() -> postUtil.findPostById("post-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("게시글 삭제를 repository에 위임한다")
    void deletePostById_delegates() {
        PostRepository repository = mock(PostRepository.class);
        PostUtil postUtil = new PostUtil(repository);

        postUtil.deletePostById("post-1");

        verify(repository).deleteById("post-1");
    }
}
