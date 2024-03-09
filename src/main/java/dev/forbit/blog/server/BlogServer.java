package dev.forbit.blog.server;

import dev.forbit.blog.api.BlogPost;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class BlogServer extends Thread {

    private ServerSocket serverSocket;
    private int port;
    private boolean running = false;

    public BlogServer(int port) {
        this.port = port;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            this.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        running = false;
        this.interrupt();
    }

    @Override public void run() {
        running = true;
        while (running) {
            try {
                System.out.println("Listening for a connection");

                // Call accept() to receive the next connection
                Socket socket = serverSocket.accept();

                // Pass the socket to the RequestHandler thread for processing
                RequestHandler requestHandler = new RequestHandler(socket);
                requestHandler.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class RequestHandler extends Thread {

    private Socket socket;

    RequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override public void run() {
        try {
            System.out.println("Received a connection");
            if (!socket.isConnected()) {return;}
            // Get input and output streams
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            // Echo lines back to the client until the client closes the connection or we receive an empty line
            char ignored = in.readChar();
            int length = in.readInt();
            String message = "";
            byte[] messageByte = new byte[length];
            System.out.println("length: "+length);
            boolean end = false;
            StringBuilder dataString = new StringBuilder(length);
            int totalBytesRead = 0;
            while (!end) {
                int currentBytesRead = in.read(messageByte);
                totalBytesRead = currentBytesRead + totalBytesRead;
                if (totalBytesRead <= length) {
                    dataString.append(new String(messageByte, 0, currentBytesRead, StandardCharsets.UTF_8));
                } else {
                    dataString.append(new String(messageByte, 0, length - totalBytesRead + currentBytesRead,
                                                 StandardCharsets.UTF_8));
                }
                if (dataString.length() >= length) {
                    end = true;
                }
            }
            message = dataString.toString();
            // add message to blog post!
            BlogPost post = new BlogPost();
            post.setDate(System.currentTimeMillis());
            String[] contents = message.split("\n");
            post.setTitle(contents[0]);
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i < contents.length; i++) {
                builder.append(contents[i]);
            }
            post.setBlogContents(builder.toString().trim());
            PostManager.getInstance().addPost(post);

            // Close our connection
            in.close();
            socket.close();

            System.out.println("Connection closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
