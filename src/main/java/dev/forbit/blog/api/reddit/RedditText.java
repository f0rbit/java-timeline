package dev.forbit.blog.api.reddit;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public @Data
class RedditText extends RedditPost {

    String text;
}
