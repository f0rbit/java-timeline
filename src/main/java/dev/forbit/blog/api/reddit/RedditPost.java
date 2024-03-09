package dev.forbit.blog.api.reddit;

import dev.forbit.blog.api.Category;
import dev.forbit.blog.api.Post;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public @Data
abstract class RedditPost extends Post {

    String subreddit;
    int score;
    int comments;
    String permalink;
    long creationTime;

    @Override public Category getCategory() {return Category.REDDIT;}
}
