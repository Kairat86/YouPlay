package play.zhet.you.singleton

import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube

object YouPlay {

    val instance: YouTube = YouTube.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), HttpRequestInitializer { }).setApplicationName("YouPlay").build()

}