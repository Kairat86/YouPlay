package zhet.youplay.entity;

import android.content.SharedPreferences;

import org.apache.pig.impl.util.ObjectSerializer;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import zhet.player.VideoItem;

import static zhet.youplay.controller.PlaybackController.CURRENT_POSITION_STRING;
import static zhet.youplay.controller.PlaybackController.CURRENT_QUEUE_NAME;

public class Playlist implements Serializable {
    private ArrayList<VideoItem> videos;
    private String name;


    public Playlist(ArrayList<VideoItem> videos, String name) {
        this.videos = videos;
        this.name = name;
    }

    public ArrayList<VideoItem> getVideos() {
        return videos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void saveAsNew(int currentPosition, SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putInt(CURRENT_POSITION_STRING, currentPosition).apply(); //save current position
        if (getName().equals(CURRENT_QUEUE_NAME)) {
            try {
                sharedPreferences.edit().putString(CURRENT_QUEUE_NAME, ObjectSerializer.serialize(getVideos())).apply();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
