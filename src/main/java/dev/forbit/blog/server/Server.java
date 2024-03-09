package dev.forbit.blog.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.forbit.blog.api.TwitterPost;
import dev.forbit.blog.api.git.Commit;
import dev.forbit.blog.api.reddit.RedditLink;
import dev.forbit.blog.api.reddit.RedditPost;
import dev.forbit.blog.api.reddit.RedditText;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.endpoints.AdditionalParameters;
import io.github.redouane59.twitter.dto.tweet.Tweet;
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.tweet.TweetType;
import io.github.redouane59.twitter.dto.user.UserV2;
import io.github.redouane59.twitter.signature.TwitterCredentials;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.HttpResponse;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;

@SpringBootApplication
public class Server {

	private static Dotenv dotenv = Dotenv.load();	
    private static final File databaseFile = new File("resources/database.json");

    public static void main(String[] args) throws IOException {
        PostManager manager = new PostManager();
        try {
            manager.load(databaseFile);
        } catch (FileNotFoundException ignored) {
            System.out.println("Database file not found!");
        }
        Updater updater = new Updater();
        updater.update();
        updater.start();

        ConfigurableApplicationContext context = SpringApplication.run(Server.class, args);

        BlogServer blogServer = new BlogServer(42069);
        blogServer.startServer();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping server");
            blogServer.stopServer();
            try {
                manager.save(databaseFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            context.close();
        }));
    }

    public static void updateTweets() throws IOException {
        int count = 0;
		TwitterClient twitterClient = new TwitterClient(new TwitterCredentials(
			dotenv.get("TWITTER_API_KEY"),
			dotenv.get("TWITTER_API_SECRET"),
			dotenv.get("TWITTER_ACCESS_TOKEN"),
			dotenv.get("TWITTER_ACCESS_SECRET")
		));
        UserV2 user = twitterClient.getUserFromUserName(dotenv.get("TWITTER_USERNAME"));
        TweetList list = twitterClient.getUserTimeline(user.getId(), AdditionalParameters.builder().maxResults(30).build());
        for (Tweet tweetData : list.getData()) {
            if (tweetData.getTweetType().equals(TweetType.DEFAULT)) {
                TwitterPost post = new TwitterPost();
                post.setTweetContent(tweetData.getText());
                post.setTweetID(tweetData.getId());
                post.setReplyCount(tweetData.getReplyCount());
                post.setLikeCount(tweetData.getLikeCount());
                post.setRetweetCount(tweetData.getRetweetCount());
                if (!PostManager.getInstance().containsTweet(post.getTweetID())) {
                    post.setTitle("Tweet");
                    post.setDate(tweetData.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli());
                    post.setImageURL(null);
                    PostManager.getInstance().addPost(post);
                    count++;
                } else {
                    // update reply, likes, and comment count
                    PostManager.getInstance().getTweet(post.getTweetID()).ifPresent((p) -> {
                        p.setLikeCount(post.getLikeCount());
                        p.setReplyCount(post.getReplyCount());
                        p.setRetweetCount(post.getRetweetCount());
                    });
                }
            }
        }
        System.out.println("Updated tweets, added " + count);
    }

    public static void updateReddit() {
        int count = 0;
        // Assuming we have a 'script' reddit app
        // Credentials oauthCreds = Credentials.script(REDDIT_USERNAME, REDDIT_PASSWORD, REDDIT_CLIENT_ID, REDDIT_SECRET);
		Credentials oauthCreds = Credentials.script(
			dotenv.get("REDDIT_USERNAME"),
			dotenv.get("REDDIT_PASSWORD"),
			dotenv.get("REDDIT_CLIENT_ID"),
			dotenv.get("REDDIT_CLIENT_SECRET")
		);

        // Create a unique User-Agent for our bot
        UserAgent userAgent = new UserAgent("bot", "dev.forbit.blog", "1.0.0", dotenv.get("REDDIT_USERNAME"));

        // Authenticate our client
        RedditClient reddit = OAuthHelper.automatic(new OkHttpNetworkAdapter(userAgent), oauthCreds);
        HttpResponse response = reddit.request(reddit.requestStub().path("user/"+dotenv.get("REDDIT_USERNAME")+"/submitted").build());

        JsonObject json = JsonParser.parseString(response.getBody()).getAsJsonObject();

        JsonArray posts = json.getAsJsonObject("data").getAsJsonArray("children");
        for (JsonElement e : posts) {
            JsonObject object = e.getAsJsonObject().getAsJsonObject("data");
            boolean isSelf = object.get("is_self").getAsBoolean();
            RedditPost post;
            if (isSelf) {
                RedditText textPost = new RedditText();
                textPost.setText(object.get("selftext").getAsString());
                post = textPost;
            } else {
                RedditLink linkPost = new RedditLink();
                linkPost.setDestinationURL(object.get("url").getAsString());
                post = linkPost;
            }
            post.setComments(object.get("num_comments").getAsInt());
            post.setScore(object.get("score").getAsInt());
            post.setPermalink(object.get("permalink").getAsString());
            post.setCreationTime((long) (object.get("created").getAsDouble() * 1000L));
            post.setSubreddit(object.get("subreddit").getAsString());

            if (!PostManager.getInstance().containsRedditPost(post.getPermalink())) {
                post.setTitle(object.get("title").getAsString());
                post.setDate(post.getCreationTime());
                PostManager.getInstance().addPost(post);
                count++;
            } else {
                // update comment and score count
                PostManager.getInstance().getRedditPost(post.getPermalink()).ifPresent((p) -> {
                    p.setScore(post.getScore());
                    p.setComments(post.getComments());
                });
            }
        }
        System.out.println("Updated reddit posts, added " + count);
    }

    public static void updateGitHub() throws IOException {
        GitHub github = new GitHubBuilder().withOAuthToken(dotenv.get("GITHUB_OAUTH_TOKEN")).build();
        int count = 0;
        Map<String, GHRepository> repos = github.getMyself().getAllRepositories();
        //System.out.println("repositories: "+repos.keySet());
        for (String repoName : repos.keySet()) {
            GHRepository repo = repos.get(repoName);
            //System.out.println("looking at repo: "+repoName);
            if (repo.isPrivate()) {continue;}
            final Set<GHCommit> commits = new HashSet<>();
            repo.getBranches().keySet().forEach((name) -> {
                if (!(name.contains("docs"))) {
                    try {
                        commits.addAll(repo.queryCommits().from(name).author("f0rbit").list().toSet());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            System.out.println("found commits: " + commits.size());
            for (GHCommit ghCommit : commits) {
                if (PostManager.getInstance().containsCommit(ghCommit.getSHA1())) {
                    //System.out.println("commit already added: "+ghCommit.getSHA1());
                    continue;
                }
                if (ghCommit.getAuthor() == null) {
                    //System.out.println("commit's author is null"+ghCommit.getSHA1());
                    continue;
                }
                if (!("f0rbit".equals(ghCommit.getAuthor().getLogin()))) {
                    //System.out.println("commit's author is not f0rbit, "+ghCommit.getAuthor().getLogin()+", "+ghCommit.getHtmlUrl());
                    continue;
                }
                Commit commit = new Commit();
                commit.setSha(ghCommit.getSHA1());
                commit.setDate(ghCommit.getCommitDate().toInstant().toEpochMilli());
                commit.setPermalink(ghCommit.getHtmlUrl().toString());
                commit.setProject(repoName);
                String message = ghCommit.getCommitShortInfo().getMessage();
                String[] parts = message.split("\n");
                commit.setTitle(parts[0]);
                if (parts.length > 1) {
                    // concatinate rest of parts
                    StringBuilder builder = new StringBuilder();
                    for (int i = 1; i < parts.length; i++) {
                        builder.append(parts[i]);
                    }
                    commit.setDescription(builder.toString().trim());
                }
                PostManager.getInstance().addPost(commit);
                System.out.println("added commit " + commit);
                count++;
            }
        }
        System.out.println("Updated github commits, added " + count);
    }
}
