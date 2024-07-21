package com.project.repositories.contracts;

import com.project.models.FilteredPostsOptions;
import com.project.models.Post;
import com.project.models.dtos.PostDtoTop;

import java.util.List;

public interface PostRepository {
    List<Post> getAllUsersPosts(int userId, FilteredPostsOptions postFilterOptions);

    List<Post> getAllPosts(FilteredPostsOptions filteredPostsOptions);

    Post getPostById(int postId);

    List<PostDtoTop> getMostLikedPosts();

    List<PostDtoTop> getMostCommentedPosts();

    List<PostDtoTop> getMostRecentPosts();

    Post createPost(Post post);

    Post getPostByTitle(String title);

    void updatePost(Post post);

    void deletePost(Post post);
}
