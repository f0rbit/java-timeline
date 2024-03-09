package dev.forbit.blog.api.reddit;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RedditLink extends RedditPost {
    String destinationURL;
}
