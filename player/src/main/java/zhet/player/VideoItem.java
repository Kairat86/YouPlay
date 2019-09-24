package zhet.player;

import java.io.Serializable;


public class VideoItem implements Serializable {
    private String title;
    private String description;
    private String thumbnailURL;
    private String id;
    private String channelTitle;


    public VideoItem(String title, String thumbnailURL, String id) {
        this.title = title;
        this.thumbnailURL = thumbnailURL;
        this.id = id;
    }

    VideoItem() { }

    public String getChannelTitle() {
        return channelTitle;
    }

    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public void setThumbnailURL(String thumbnail) {
        this.thumbnailURL = thumbnail;
    }

}
