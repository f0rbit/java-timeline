package dev.forbit.blog.api;

import lombok.Getter;
import lombok.Setter;

public class BlogPost extends Post {

    /** File in markdown */
    @Getter @Setter String blogContents;

    @Override public Category getCategory() { return Category.BLOG; }
}
