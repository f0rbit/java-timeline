package dev.forbit.blog.server;

import dev.forbit.blog.api.Category;
import dev.forbit.blog.api.Post;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class APIController {

    @GetMapping("/posts") @ResponseBody @CrossOrigin(origins = "http://localhost:3000")
    public List<Post> getPosts(@RequestParam(name = "category", required = false) String category) {
        Category postCategory = null;
        try {
            postCategory = Category.valueOf(category.toUpperCase());
        } catch (Exception ignored) {}
        return PostManager.getInstance().getPosts(postCategory);
    }
}
