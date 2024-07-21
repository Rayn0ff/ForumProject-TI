package com.project.services.contracts;

import com.project.models.FilteredPostsOptions;
import com.project.models.Post;
import com.project.models.User;
import com.project.models.dtos.PostDtoTop;

import java.util.List;

public interface PostService {
    List<Post> getAllUsersPosts(int userId, FilteredPostsOptions postFilterOptions);

    List<Post> getAllPosts(User user, FilteredPostsOptions filteredPostsOptions);

    Post getPostById(int postId);

    List<PostDtoTop> getMostLikedPosts();

    List<PostDtoTop> getMostCommentedPosts();

    List<PostDtoTop> getMostRecentPosts();

    int getTotalPostsCount();

    Post createPost(Post post);

    void updatePost(User user, Post post);

    void deletePost(User user, Post post);
}
