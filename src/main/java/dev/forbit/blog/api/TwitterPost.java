package dev.forbit.blog.api;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public @Data
class TwitterPost extends Post {

    private String tweetContent;
    private String tweetID;
    private int retweetCount;
    private int likeCount;
    private int replyCount;

    @Override public Category getCategory() { return Category.TWITTER; }
}
