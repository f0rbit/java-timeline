package dev.forbit.blog.api.git;

import dev.forbit.blog.api.Category;
import dev.forbit.blog.api.Post;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public @Data
class Commit extends Post {

    String project;
    String description;
    String sha;
    String permalink;

    @Override public Category getCategory() {
        return Category.GITHUB;
    }
}
