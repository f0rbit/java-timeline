package dev.forbit.blog.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.forbit.blog.api.Category;
import dev.forbit.blog.api.Post;
import dev.forbit.blog.api.PostDatabase;
import dev.forbit.blog.api.TwitterPost;
import dev.forbit.blog.api.git.Commit;
import dev.forbit.blog.api.reddit.RedditPost;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class PostManager {

    @Getter public static PostManager instance;
    @Getter private final HashMap<Integer, Post> posts = new HashMap<>();

    PostManager() {
        instance = this;
    }

    public void addPost(Post post) {
        int id = getPosts().size()+1;
        post.setId(UUID.randomUUID());
        post.setNumber(id);
        getPosts().put(id, post);
        System.out.println("added post "+post);
    }

    public void load(File databaseFile) throws FileNotFoundException {
        Scanner scanner = new Scanner(databaseFile);
        String content = scanner.useDelimiter("\\Z").next();
        scanner.close();

        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        JsonObject object = JsonParser.parseString(content).getAsJsonObject();
        JsonObject json = object.getAsJsonObject("posts");
        List<Post> postList = PostDatabase.createFrom(gson, json);
        for (Post p : postList) {
            int id = p.getNumber();
            setPost(id, p);
        }
        System.out.println("loaded databases");
    }

    public void setPost(int id, Post p) {
        posts.put(id, p);
    }

    public boolean containsTweet(String tweetID) {
        for (int i : getPosts().keySet()) {
            Post p = getPosts().get(i);
            if (p instanceof TwitterPost twitterPost) {
                if (twitterPost.getTweetID().equals(tweetID)) return true;
            }
        }
        return false;
    }

    public boolean containsRedditPost(String permalink) {
        for (int i : getPosts().keySet()) {
            Post p = getPosts().get(i);
            if (p instanceof RedditPost post) {
                if (post.getPermalink().equals(permalink)) return true;
            }
        }
        return false;
    }

    public boolean containsCommit(String sha) {
        for (int i : getPosts().keySet()) {
            Post p = getPosts().get(i);
            if (p instanceof Commit post) {
                if (post.getSha().equals(sha)) return true;
            }
        }
        return false;
    }

    public void save(File databaseFile) throws IOException {
        PostDatabase database = new PostDatabase();
        database.loadAll(getPosts(null));
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        String json = gson.toJson(database);
        if (!databaseFile.exists()) {
            databaseFile.getParentFile().mkdirs();
            databaseFile.createNewFile();
        }
        FileWriter writer = new FileWriter(databaseFile);
        writer.write(json);
        writer.flush();
        writer.close();
        System.out.println("Saved Database!");
    }

    public List<Post> getPosts(Category category) {
        ArrayList<Post> list = new ArrayList<>();
        int count = 0;
        for (int id : getPosts().keySet()) {
            Post p = getPost(id);
            if (p == null) { continue; }
            if (category == null || category.equals(p.getCategory())) { list.add(p); }
            count++;
        }
        System.out.println("returned "+count+" posts");
        list.sort(Comparator.comparingLong(Post::getDate).reversed());
        return list;
    }

    @Nullable public Post getPost(int id) {
        return getPosts().getOrDefault(id, null);
    }

    public Optional<RedditPost> getRedditPost(String permalink) {
        for (Post post : getPosts().values()) {
            if (post instanceof RedditPost redditPost) {
                if (redditPost.getPermalink().equals(permalink)) {
                    return Optional.of(redditPost);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<TwitterPost> getTweet(String tweetID) {
        for (var p : getPosts().values()) {
            if (p instanceof TwitterPost twitterPost) {
                if (twitterPost.getTweetID().equals(tweetID)) {
                    return Optional.of(twitterPost);
                }
            }
        }
        return Optional.empty();
    }
}
