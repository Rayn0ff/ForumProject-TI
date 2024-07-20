package com.project.services.contracts;

import com.project.models.Post;
import com.project.models.dtos.PostDtoTopComments;

import java.util.List;

public interface PostService {
    List<Post> getAllUsersPosts(int userId);

    List<Post> getAllPosts();

    Post getPostById(int postId);

    List<PostDtoTopComments> getMostLikedPosts();

    List<PostDtoTopComments> getMostCommentedPosts();

    void createPost(Post post);
}
