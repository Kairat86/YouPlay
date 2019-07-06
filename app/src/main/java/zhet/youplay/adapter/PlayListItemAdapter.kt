package zhet.youplay.adapter

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.api.services.youtube.model.PlaylistItem
import com.palashbansal.musicalyoutube.VideoItem
import com.squareup.picasso.Picasso
import zhet.youplay.R
import zhet.youplay.controller.PlaybackController

class PlayListItemAdapter(private val list: List<PlaylistItem>) : androidx.recyclerview.widget.RecyclerView.Adapter<PlayListItemAdapter.VH>() {

    companion object {
        private val TAG: String = PlayListItemAdapter::class.java.simpleName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.playlist_item, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]
        val thumbnails = item.snippet.thumbnails
        val standard = thumbnails.standard
        val high = thumbnails.high
        val medium = thumbnails.medium
        val default = thumbnails.default
        val url = when {
            standard != null -> standard.url
            high != null -> high.url
            medium != null -> medium.url
            default != null -> default.url
            else -> null
        }
        if (url != null) {
            Picasso.get().load(url).into(holder.videoImg)
        }
        val title = item.snippet.title
        holder.videoTitle.text = title
        holder.setCurrentItem(item)
    }

    inner class VH(val view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {

        private lateinit var item: PlaylistItem
        val videoImg: ImageView = view.findViewById(R.id.videoImg)
        val videoTitle: TextView = view.findViewById(R.id.videoTitle)

        fun setCurrentItem(item: PlaylistItem) {
            this.item = item
        }

        init {
            view.setOnClickListener {
                play()
            }
        }

        private fun play() {
            PlaybackController.getInstance(view.context)
                    .playNow(VideoItem(item.snippet.title, item.snippet.thumbnails.default.url, item.contentDetails.videoId))
        }
    }

    override fun equals(other: Any?): Boolean {
        return javaClass.simpleName.equals(other!!::class.java.simpleName)
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}