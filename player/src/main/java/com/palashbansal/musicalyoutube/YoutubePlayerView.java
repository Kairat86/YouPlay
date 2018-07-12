package com.palashbansal.musicalyoutube;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class YoutubePlayerView extends WebView {

    public boolean isDragging = false;
    private Runnable onPlayerReadyRunnable = null;
    private NumberReceivedListener durationListener, VideoLoadedFractionListener;
    private NumberReceivedListener currentTimeListener;

    public YoutubePlayerView(Context context) {
        super(context);
    }

    public YoutubePlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public YoutubePlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize() {
        String data = init();
        setTag("YTPlayer");
        clearCache(true);
        clearHistory();
        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(this, "Android");
        loadDataWithBaseURL("https://www.youtube.com", data, "text/html", "UTF-8", "http://www.youtube.com");
    }

    private String init() {
        return "<!DOCTYPE html>" + "<html>" + "<head> <style type=\"text/css\">" + "html {\n" + "        height: 100%;\n" + "      }\n" + "body {\n" + "        min-height: 100%;\n" + "        margin: 0;\n" + "      }\n" + "iframe {\n" + "        position: absolute;\n" + "        border: none;\n" + "        height: 100%;\n" + "        width: 100%;\n" + "        top: 0;\n" + "        left: 0;\n" + "        bottom: 0;\n" + "        right: 0;\n" + "      }\n" + "#overlay { position: absolute; z-index: 3; opacity: 0.5; filter: alpha(opacity = 50); top: 0; bottom: 0; left: 0; right: 0; width: 100%; height: 100%; background-color: Black; color: White; display: none;}\n" + "    </style>\n" + "</head>\n" + "<body>\n" + "<div id=\"player\"></div>\n" + "<script>\n" + "var tag = document.createElement('script');\n" + "tag.src = \"https://www.youtube.com/iframe_api\";\n" + "var firstScriptTag = document.getElementsByTagName('script')[0];\n" + "firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);\n" + "var player;\n" + "function onYouTubeIframeAPIReady() {\n" + "player = new YT.Player('player', {\n" + "playerVars: {'controls': 0, 'iv_load_policy': 3 },\n" + "events: {\n" + "'onReady': onPlayerReady,\n" + "            'onStateChange': onPlayerStateChange}});" + "}" + "function onPlayerReady(event) {" + "setTimeout(function(){" + "Android.playerReady();" + "player.setVolume(100)" + "}, 200);" + "}" + "var done = false;" + "function onPlayerStateChange(event) {" + "Android.playerStateChange(event.data);" + "}" + "function stopVideo() {" + "player.stopVideo();" + "}" + "function showOverlay() {" + "document.getElementById('overlay').style.display = 'block';" + "}" + "function hideOverlay() {" + "document.getElementById('overlay').style.display = 'none';" + "}" + "function playFullscreen (){" + "iframe = document.getElementById('player');" + "var requestFullScreen = iframe.webkitRequestFullScreen;" + "Android.log(\"Playing fullscreen\");" + "if (requestFullScreen) {" + "requestFullScreen.bind(iframe)();" + "}" + "}" + "function getCurrentTime(){" + "Android.notifyCurrentTime(player.getCurrentTime());" + "}" + "function getDuration(){" + "Android.notifyDuration(player.getDuration());" + "}" + "function getVideoLoadedFraction(){" + "Android.notifyVideoLoadedFraction(player.getVideoLoadedFraction());" + "}" + "     " + " window.setInterval(getCurrentTime, 1000);" + "window.setInterval(getDuration, 1000);\n" + //TODO: Test performance of this vs the function calling approach.
                "window.setInterval(getVideoLoadedFraction, 1000);" + "</script>" + "<div id=\"overlay\">" + "</div>" + "  " + "</body>" + "</html>";
    }

    public void pause() {
        loadUrl("javascript:player.pauseVideo();");
    }

    public void play() {
        loadUrl("javascript:player.playVideo();");
    }

    @JavascriptInterface
    public void playerReady() {
        if (onPlayerReadyRunnable != null) {
            post(onPlayerReadyRunnable);
        }
        Log.d("Player", "Ready");
    }

    @JavascriptInterface
    public void log(String s) {
        Log.d("Javascript", s);
    }

    @JavascriptInterface
    public void notifyCurrentTime(int secs) {
        if (currentTimeListener != null) currentTimeListener.onReceive(secs);
    }

    @JavascriptInterface
    public void notifyDuration(int secs) {
        if (durationListener != null) durationListener.onReceive(secs);
    }

    @JavascriptInterface
    public void notifyVideoLoadedFraction(float fraction) {
        if (VideoLoadedFractionListener != null) VideoLoadedFractionListener.onReceive(fraction);
    }

    public void getCurrentTime() {
        loadUrl("javascript:getCurrentTime()");
    }

    public void showOverlay() {
        isDragging = true;
        loadUrl("javascript:showOverlay()");
    }

    public void hideOverlay() {
        isDragging = false;
        loadUrl("javascript:hideOverlay()");
    }

    public void getVideoLoadedFraction() {
        loadUrl("javascript:getVideoLoadedFraction()");
    }

    public void getDuration() {
        loadUrl("javascript:getDuration()");
    }

    public void setVolume(float volume) {
        loadUrl("javascript:player.setVolume(" + volume + ")");
    }

    public void mute() {
        loadUrl("javascript:player.mute()");
    }

    public void unMute() {
        loadUrl("javascript:player.unMute()");
    }

    public void seekTo(float seconds, boolean allowSeekAhead) {
        loadUrl("javascript:player.seekTo(" + seconds + "," + allowSeekAhead + ")");
    }

    public void loadVideoById(String videoId, float startSeconds, String suggestedQuality) {
        loadUrl("javascript:player.loadVideoById(\"" + videoId + "\"," + startSeconds + ",\"" + suggestedQuality + "\")");
    }

    public void loadVideoById(String videoId) {
        loadVideoById(videoId, 0, "default");
    }

    public void setOnPlayerReadyRunnable(Runnable onPlayerReadyRunnable) {
        this.onPlayerReadyRunnable = onPlayerReadyRunnable;
    }

    public void setDurationListener(NumberReceivedListener durationListener) {
        this.durationListener = durationListener;
    }

    public void setVideoLoadedFractionListener(NumberReceivedListener videoLoadedFractionListener) {
        VideoLoadedFractionListener = videoLoadedFractionListener;
    }

    public void removePage() {
        loadUrl("about:blank");
    }

    public void setCurrentTimeListener(NumberReceivedListener currentTimeListener) {
        this.currentTimeListener = currentTimeListener;
    }

    public interface NumberReceivedListener {
        /**
         * Called when a number has been received.
         *
         * @param n The number received.
         */
        void onReceive(float n);
    }
}
