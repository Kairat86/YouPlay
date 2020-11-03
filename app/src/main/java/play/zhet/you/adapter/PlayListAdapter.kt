package play.zhet.you.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.SearchResult
import com.squareup.picasso.Picasso
import play.zhet.you.BuildConfig
import play.zhet.you.R
import play.zhet.you.singleton.YouPlay
import java.util.*


class PlayListAdapter(private val playLists: List<SearchResult>, val prgrBar: View, val stack: Stack<androidx.recyclerview.widget.RecyclerView.Adapter<out androidx.recyclerview.widget.RecyclerView.ViewHolder>>) : androidx.recyclerview.widget.RecyclerView.Adapter<PlayListAdapter.VH>() {

    private lateinit var rv: androidx.recyclerview.widget.RecyclerView

    companion object {
        val TAG: String = PlayListAdapter::class.java.simpleName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.playlist, parent, false), prgrBar)
    }

    override fun getItemCount(): Int {
        return playLists.size
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val result = playLists[position]
        val thumbnails = result.snippet.thumbnails
        val maxres = thumbnails.maxres
        val high = thumbnails.high
        val default = thumbnails.default
        val url = when {
            maxres != null -> maxres.url
            high != null -> high.url
            default != null -> default.url
            else -> null
        }
        Picasso.get().load(url.toString()).into(holder.img)
        holder.name.text = result.snippet.title
        holder.setCurrentResult(result)
    }

    override fun onAttachedToRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        this.rv = recyclerView
    }

    inner class VH(view: View, prgrBar: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.playlistThumb)
        val name: TextView = view.findViewById(R.id.playlistName)
        private lateinit var result: SearchResult

        init {
            view.setOnClickListener {
                Log.i(TAG, "onClick")
                prgrBar.visibility = VISIBLE
                Thread {
                    val list = loadPlayList()
                    rv.post {
                        val playListItemAdapter = PlayListItemAdapter(list)
                        if (!stack.contains(playListItemAdapter)) {
                            stack.push(playListItemAdapter)
                        }
                        rv.adapter = playListItemAdapter
                        prgrBar.visibility = GONE
                    }
                }.start()
            }
        }

        private fun loadPlayList(): MutableList<PlaylistItem> {
            val playList = YouPlay.instance.playlistItems()
                    .list("snippet,contentDetails")

            playList.setKey(BuildConfig.KEY)
                    .setPlaylistId(result.id.playlistId).setMaxResults(25).fields = "items(contentDetails/videoId,snippet/title,snippet/thumbnails/*/url)"
            return playList.execute().items
        }

        fun setCurrentResult(result: SearchResult) {
            this.result = result
        }
    }
}