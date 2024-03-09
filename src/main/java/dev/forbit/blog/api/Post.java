package dev.forbit.blog.api;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

interface PostInterface {

    Category getCategory();

    String getTitle();

    String getImageURL();

    long getDate();
}

public abstract class Post implements PostInterface {

    // POST VARIABLES
    @Getter @Setter String title;
    @Getter @Setter String imageURL;
    @Getter @Setter long date;

    // id
    @Getter @Setter UUID id;
    @Getter @Setter int number;

}
