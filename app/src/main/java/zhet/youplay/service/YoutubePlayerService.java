package zhet.youplay.service;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.palashbansal.musicalyoutube.VideoItem;
import com.palashbansal.musicalyoutube.YoutubePlayerView;

import zhet.youplay.R;
import zhet.youplay.controller.PlaybackController;
import zhet.youplay.receiver.OpenAppReceiver;

import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_USER_PRESENT;
import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.CENTER;
import static com.palashbansal.musicalyoutube.YoutubePlayerView.GONE;
import static com.palashbansal.musicalyoutube.YoutubePlayerView.OnClickListener;
import static com.palashbansal.musicalyoutube.YoutubePlayerView.VISIBLE;

/**
 * Created by Palash on 23-Nov-16.
 * This is the main service that holds the window manager for the Youtube popup player.
 * This will always be running in the background when the video is playing
 * This service can only be stopped by itself, when the queue finishes or user presses the x button.
 */

public class YoutubePlayerService extends Service {

    private static final int FOREGROUND_NOTIFICATION_ID = 4822678;
    private static final int CONTROL_HIDE_TIMEOUT = 4000;
    public static final String BROADCAST_OPEN_ACTIVITY_FROM_POPUP = "BROADCAST_OPEN_ACTIVITY_FROM_POPUP";
    private static final String TAG = YoutubePlayerService.class.getSimpleName();
    public static boolean isRunning = false, isPlayerReady = false;
    private VideoItem currentVideo;
    private boolean isControlsVisible = false;
    private int currentSeconds = 0, videoDuration = 1;
    private Handler secondsHandler = new Handler();
    private float videoBuffered = 0;
    private boolean isUserChangingTouch = false;
    private WindowManager windowManager;
    private YoutubePlayerView playerView;
    private WindowManager.LayoutParams params;
    private FrameLayout container;
    private View seekBarContainer;
    private SeekBar seekBar;
    private long lastTouchTime;
    private PlaybackController controller;
    private ImageView closeButton;
    private ImageView openAppBtn;
    private ProgressBar bufferingIndicator;
    private OpenAppReceiver receiver;
    private BroadcastReceiver screenEventReciever;
    private FrameLayout closeContainer;
    private WindowManager.LayoutParams closeParams;
    private boolean shouldClose;

    public static float dipToPixels(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    /**
     * Static helper method to start the service if stopped
     */
    public static boolean checkServiceAndStart(Context context) {
        if (!isRunning) {
            Log.d("Player", "StartingService");
            context.startService(new Intent(context, YoutubePlayerService.class));
            return false;
        }
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        controller = PlaybackController.getInstance(this);

        container = new FrameLayout(this) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                lastTouchTime = System.currentTimeMillis();
                if (!isControlsVisible) {
                    showControlContainer();
                }
                return super.onInterceptTouchEvent(ev);
            }
        };
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(com.palashbansal.musicalyoutube.R.layout.popup_player_layout, container);

        playerView = view.findViewById(com.palashbansal.musicalyoutube.R.id.player_view);
        seekBarContainer = view.findViewById(com.palashbansal.musicalyoutube.R.id.slider_container);
        seekBar = view.findViewById(com.palashbansal.musicalyoutube.R.id.seekBar);
        closeButton = view.findViewById(com.palashbansal.musicalyoutube.R.id.close_button);
        openAppBtn = view.findViewById(com.palashbansal.musicalyoutube.R.id.to_app);
        bufferingIndicator = view.findViewById(com.palashbansal.musicalyoutube.R.id.buffer_loading_indicator);

        playerView.initialize();

        isPlayerReady = false;
        isControlsVisible = true;

        lastTouchTime = System.currentTimeMillis();

        controller.setOnCurrentVideoChanged(new Runnable() {
            @Override
            public void run() {
                if (!isControlsVisible) showControlContainer();
                if (controller.getCurrent() == null) {
                    playerView.seekTo(0, true);
                    playerView.pause();
                    return;
                }
                if (currentVideo != null && controller.getCurrent().getId().equals(currentVideo.getId())) {
                    playerView.seekTo(0, true);
                    playerView.play();
                    return;
                }
                currentVideo = controller.getCurrent();
                playerView.play();
                playerView.loadVideoById(currentVideo.getId());
                container.setVisibility(View.VISIBLE);
                NotificationCompat.Builder mBuilder = getBuilder();
                startForeground(FOREGROUND_NOTIFICATION_ID, mBuilder.build());
                Log.d("Service", "Video Changed");
            }
        });

        int type = WindowManager.LayoutParams.TYPE_PHONE;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        params = new WindowManager.LayoutParams((int) (Resources.getSystem().getDisplayMetrics().widthPixels / 2.05), ViewGroup.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.END | CENTER;

        setDragListeners();

        playerView.setCurrentTimeListener(new YoutubePlayerView.NumberReceivedListener() {
            @Override
            public void onReceive(float n) {
                currentSeconds = (int) n;
            }
        });
        playerView.setDurationListener(new YoutubePlayerView.NumberReceivedListener() {
            @Override
            public void onReceive(float n) {
                videoDuration = (int) n;
            }
        }); //TODO: Put this in player view itself
        playerView.setVideoLoadedFractionListener(new YoutubePlayerView.NumberReceivedListener() {
            @Override
            public void onReceive(float n) {
                videoBuffered = n;
            }
        }); //TODO: Put this in player view itself

        Runnable r = new Runnable() {
            @Override
            public void run() {
                isControlsVisible = seekBarContainer.getVisibility() == View.VISIBLE;
                if (!isUserChangingTouch && isControlsVisible) {
                    updateSeekUI();
                }
                if (System.currentTimeMillis() - lastTouchTime > CONTROL_HIDE_TIMEOUT) {
                    hideControlContainer();
                }
                secondsHandler.postDelayed(this, 1000);
            }
        };
        secondsHandler.postDelayed(r, 200);
        windowManager.addView(container, params);
        container.setVisibility(View.GONE);
        closeContainer = new FrameLayout(this);
        inflater.inflate(R.layout.close_bar, closeContainer);
        closeParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        closeParams.gravity = BOTTOM;
        windowManager.addView(closeContainer, closeParams);
        closeContainer.setVisibility(GONE);
        setupPlayerView();

        setButtonListeners();
        receiver = new OpenAppReceiver();
        registerReceiver(receiver, new IntentFilter(BROADCAST_OPEN_ACTIVITY_FROM_POPUP));
        screenEventReciever = new ScreenEventReceiver();
        IntentFilter filter = new IntentFilter(ACTION_USER_PRESENT);
        filter.addAction(ACTION_SCREEN_OFF);
        registerReceiver(screenEventReciever, filter);
    }

    private NotificationCompat.Builder getBuilder() {
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel();
        }
        return new NotificationCompat.Builder(YoutubePlayerService.this, channelId).setSmallIcon(com.palashbansal.musicalyoutube.R.drawable.headset).setContentTitle(currentVideo.getTitle()).setContentText(currentVideo.getChannelTitle()).setChannelId(getString(com.palashbansal.musicalyoutube.R.string.app_name));
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String channelId = getString(R.string.app_name);
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
    }


    /**
     * It initializes all the buttons and the seekbar, also holds the listeners
     */
    private void setButtonListeners() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && videoDuration != 0) {
                    lastTouchTime = System.currentTimeMillis();
                    currentSeconds = (int) ((progress / 100f) * videoDuration);
                    if (isUserChangingTouch) {
                        playerView.seekTo(currentSeconds, false);
                    } else {
                        playerView.seekTo(currentSeconds, true);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserChangingTouch = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserChangingTouch = false;
                currentSeconds = (int) ((seekBar.getProgress() / 100f) * videoDuration);
                playerView.seekTo(currentSeconds, true);
            }
        });
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRunning = false;
                YoutubePlayerService.this.stopSelf();
            }
        });

        openAppBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick");
                Intent intent = new Intent(BROADCAST_OPEN_ACTIVITY_FROM_POPUP);
                sendBroadcast(intent);
            }
        });
    }

    /**
     * Sets up the player view
     */
    public void setupPlayerView() {
        playerView.setOnPlayerReadyRunnable(new Runnable() {
            @Override
            public void run() {
                controller.callVideoChanged();
                playerView.pause();
                bufferingIndicator.setVisibility(GONE);
                isPlayerReady = true;
            }
        });
    }

    /**
     * It is used to update seek bar UI
     */
    private void updateSeekUI() {
        Log.d("Service", "Updating SeekUI, " + videoDuration);
        if (isUserChangingTouch || videoDuration == 0) {
            seekBar.setProgress(0);
            seekBar.setSecondaryProgress(0);
            currentSeconds = 0;
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            seekBar.setProgress(100 * currentSeconds / videoDuration, true);
        } else {
            seekBar.setProgress(100 * currentSeconds / videoDuration);
        }
        seekBar.setSecondaryProgress((int) (100 * videoBuffered));
    }

    /**
     * Animates to hide the Control container and the seekbar container
     */
    private void hideControlContainer() {
        isControlsVisible = false;
        seekBarContainer.animate().translationY(-dipToPixels(YoutubePlayerService.this, 80)).setDuration(200).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                seekBarContainer.setVisibility(View.GONE);
                seekBarContainer.setTranslationY(0);
            }
        }).setStartDelay(80).start();
    }

    /**
     * Animates to show the Control container and the seekbar container
     */
    private void showControlContainer() {
        if (isControlsVisible || seekBarContainer.getVisibility() == View.VISIBLE) {
            isControlsVisible = true;
            return;
        }
        isControlsVisible = true;
        seekBarContainer.setTranslationY(-dipToPixels(YoutubePlayerService.this, 80));
        seekBarContainer.setVisibility(View.VISIBLE);
        seekBarContainer.animate().translationY(0).setDuration(200).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (container != null) {
            playerView.removePage();
            windowManager.removeView(container);
        }
        if (secondsHandler != null) secondsHandler.removeCallbacksAndMessages(null);
        stopForeground(true);
        controller.notifyServiceExiting();
        isRunning = false;
        isPlayerReady = false;
        unregisterReceiver(receiver);
        unregisterReceiver(screenEventReciever);
        Log.d("Player", "Destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Player", "startCommand");

        if (intent == null) return super.onStartCommand(null, flags, startId);

        VideoItem video = (VideoItem) intent.getSerializableExtra("Video");
        if (video != null) controller.playNow(video);


        return START_STICKY;
    }

    /**
     * Defines the logic of what happens when we drag the player on the screen
     */
    private void setDragListeners() {

        playerView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        playerView.showOverlay();
                        initialX = -params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        closeContainer.setVisibility(VISIBLE);
                        return false;
                    case MotionEvent.ACTION_UP:
                        playerView.hideOverlay();
                        closeContainer.setVisibility(GONE);
                        if (shouldClose) stopSelf();
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        if (!playerView.isDragging) {
                            return false;
                        }
                        params.x = -(initialX + (int) (event.getRawX() - initialTouchX));
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(container, params);
                        int[] containerLocs = new int[2];
                        container.getLocationOnScreen(containerLocs);
                        int[] closeContainerLocs = new int[2];
                        closeContainer.getLocationOnScreen(closeContainerLocs);
                        if (closeContainerLocs[1] < containerLocs[1] + container.getHeight()) {
                            Log.i(TAG, "top>bottom");
                            closeContainer.setBackgroundColor(getResources().getColor(R.color.intersect_color));
                            shouldClose = true;
                        } else {
                            closeContainer.setBackgroundColor(getResources().getColor(R.color.close_bar_color));
                            shouldClose = false;
                        }
                        windowManager.updateViewLayout(closeContainer, closeParams);
                        return true;
                }
                return false;
            }
        });
    }

    class ScreenEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) {
            switch (i.getAction()) {
                case ACTION_SCREEN_OFF:
                    playerView.pause();
                    break;

                case ACTION_USER_PRESENT:
                    playerView.play();
            }
        }
    }

}
