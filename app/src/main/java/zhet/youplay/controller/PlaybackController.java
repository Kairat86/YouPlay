package zhet.youplay.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.widget.Toast;

import zhet.player.VideoItem;

import java.util.ArrayList;

import zhet.youplay.R;
import zhet.youplay.adapter.PlaybackQueueAdapter;
import zhet.youplay.entity.Playlist;
import zhet.youplay.service.YoutubePlayerService;

public class PlaybackController {
    public static final String CURRENT_POSITION_STRING = "CURRENT_POSITION";
    public static final int REPEAT = 0;
    public static final int REPEAT_ONE = 1;
    public static final int NO_REPEAT = 2;
    public static final int NO_POSITION = -1;
    public static final String CURRENT_QUEUE_NAME = "CURRENT_QUEUE";
    private static PlaybackController controller;
    private Playlist playlist = null;
    private int currentPosition = NO_POSITION;
    private int mode = NO_REPEAT;
    private PlaybackQueueAdapter playbackQueueAdapter;

    private Handler playbackRetryHandler = new Handler(); //To try playback, whenever the player is ready.

    private Context context;

    private Runnable onCurrentVideoChanged;
    private SharedPreferences sharedPreferences;

    private PlaybackController(SharedPreferences sharedPrefs) {
        sharedPreferences = sharedPrefs;
        currentPosition = sharedPrefs.getInt(CURRENT_POSITION_STRING, 0);
        playlist = new Playlist(new ArrayList<VideoItem>(), CURRENT_QUEUE_NAME);

        context = null;
    }

    public synchronized static PlaybackController getInstance(Context context) {
        if (controller == null && context != null) {
            controller = new PlaybackController(context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE));
        }
        if (context != null) {
            if (!YoutubePlayerService.checkServiceAndStart(context)) {
                Toast.makeText(context, R.string.starting, Toast.LENGTH_SHORT).show();
            }
            controller.context = context;
        }
        return controller;
    }

    public void notifyVideoEnd(boolean fromUser) {
        if (currentPosition == NO_POSITION || playlist == null || playlist.getVideos() == null)
            return;
        if (mode != REPEAT_ONE) {
            currentPosition++;
            if (playbackQueueAdapter != null) {
                playbackQueueAdapter.notifyItemChanged(currentPosition - 1);
            }
        }
        if (currentPosition >= playlist.getVideos().size()) {
            if (mode == NO_REPEAT && !fromUser) {
                currentPosition--;
                return;
            }
            currentPosition = 0;
        }
        if (playbackQueueAdapter != null) {
            playbackQueueAdapter.notifyItemChanged(currentPosition);
        }
        callVideoChanged();
    }

    public void playAtPosition(int position) {
        if (playlist == null || playlist.getVideos() == null) return;
        int last = currentPosition;
        currentPosition = position;
        if (playbackQueueAdapter != null) {
            playbackQueueAdapter.notifyItemChanged(currentPosition);
            playbackQueueAdapter.notifyItemChanged(last);
        }
        if (currentPosition >= playlist.getVideos().size() || currentPosition < 0) {
            currentPosition = 0;
        }
        callVideoChanged();
    }

    public void notifyVideoEnd() {
        notifyVideoEnd(false);
    }

    public VideoItem getCurrent() {
        if (currentPosition == NO_POSITION || playlist == null || playlist.getVideos() == null || currentPosition >= playlist.getVideos().size()) {
            currentPosition = NO_POSITION;
            return null;
        }
        return playlist.getVideos().get(currentPosition);
    }

    public void playNow(VideoItem video) {
        if (playlist == null) {
            playlist = new Playlist(new ArrayList<VideoItem>(), CURRENT_QUEUE_NAME);
            currentPosition = 0;
        }
        playlist.setName(CURRENT_QUEUE_NAME);
        if (currentPosition == NO_POSITION || currentPosition >= playlist.getVideos().size())
            currentPosition = 0;
        playlist.getVideos().add(currentPosition, video);
        callVideoChanged();
        if (playbackQueueAdapter != null) {
            playbackQueueAdapter.notifyItemInserted(currentPosition);
        }
    }

    public boolean saveCurrentAsPlaylist(String name) {
        if (!playlist.getName().equals(CURRENT_QUEUE_NAME)) return false;
        playlist.setName(name);
        playlist.saveAsNew(currentPosition, sharedPreferences);
        return true;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    public void setOnCurrentVideoChanged(Runnable onCurrentVideoChanged) {
        this.onCurrentVideoChanged = onCurrentVideoChanged;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void notifyServiceExiting() {
        onCurrentVideoChanged = null;
        saveCurrent();
    }

    public void saveCurrent() {
        saveCurrentAsPlaylist(CURRENT_QUEUE_NAME);
    }

    public boolean callVideoChanged() {
        playbackRetryHandler.removeCallbacksAndMessages(null);
        if (YoutubePlayerService.isPlayerReady) {
            if (onCurrentVideoChanged != null) onCurrentVideoChanged.run();
            return true;
        } else {
            playbackRetryHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!callVideoChanged()) playbackRetryHandler.postDelayed(this, 400);
                }
            }, 400);
            return false;
        }

    }
}
