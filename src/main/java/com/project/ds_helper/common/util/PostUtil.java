package com.project.ds_helper.common.util;

import com.project.ds_helper.domain.post.entity.Post;
import com.project.ds_helper.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PostUtil {

    private final PostRepository postRepository;

    public Post findPostById(String postId){
        Post post = postRepository.findVisibleById(postId).orElseThrow( () -> new IllegalArgumentException("Post Not Found. Post ID : " + postId ));
        log.info("Post Found Successfully. Post ID : " + postId);
        return post;
    }

    public void deletePostById(String postId){
        postRepository.deleteById(postId);
        log.info("Post Deleted Successfully. Post ID : " + postId);
    }

}
