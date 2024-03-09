package dev.forbit.blog.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PostDatabase {

    HashMap<String, List<Post>> posts = new HashMap<>();

    public static List<Post> createFrom(Gson gson, JsonObject object) {
        List<Post> postList = new ArrayList<>();
        for (String className : object.keySet()) {
            JsonArray array = object.getAsJsonArray(className);
            for (JsonElement e : array) {
                try {
                    postList.add(gson.fromJson(e, getPostClass(className).getClass()));
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException exception) {
                    exception.printStackTrace();
                }
            }
        }
        return postList;
    }

    public static Post getPostClass(
            String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return (Post) Class.forName(name).newInstance();
    }

    public void loadAll(List<Post> posts) {
        for (Post p : posts) {
            String name = p.getClass().getName();
            addPost(name, p);
        }
    }

    private void addPost(String name, Post p) {
        if (!posts.containsKey(name)) {
            posts.put(name, new ArrayList<>());
        }
        posts.get(name).add(p);
    }
}
