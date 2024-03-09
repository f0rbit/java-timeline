package dev.forbit.blog.server;

public class Updater extends Thread {
    long updateTime = 300000L;
    long lastUpdate = 0L;

    public void update() {
        lastUpdate = System.currentTimeMillis();
        try {
            Server.updateReddit();
            Server.updateGitHub();
            Server.updateTweets();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (this.isAlive()) {
            if (System.currentTimeMillis()-lastUpdate > updateTime) {
                update();
            }
        }
    }
}
