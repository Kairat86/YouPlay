package zhet.youplay.adapter

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.api.services.youtube.model.SearchResult
import com.palashbansal.musicalyoutube.VideoItem
import com.squareup.picasso.Picasso
import zhet.youplay.R
import zhet.youplay.controller.PlaybackController

class SearchResultAdapter(private val searchResultList: List<SearchResult>) : androidx.recyclerview.widget.RecyclerView.Adapter<SearchResultAdapter.SearchResultHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultHolder {
        return SearchResultHolder(LayoutInflater.from(parent.context).inflate(R.layout.playlist_item, parent, false))
    }

    override fun getItemCount(): Int {
        return searchResultList.size
    }

    override fun onBindViewHolder(holder: SearchResultHolder, position: Int) {
        val searchResult = searchResultList[position]
        val snippet = searchResult.snippet
        holder.videoTitle.text = snippet.title
        val thumbnails = searchResult.snippet.thumbnails
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
        holder.setCurrentItem(searchResult)
    }

    class SearchResultHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {

        val videoImg: ImageView = view.findViewById(R.id.videoImg)
        val videoTitle: TextView = view.findViewById(R.id.videoTitle)
        private lateinit var searchResult: SearchResult

        init {
            view.setOnClickListener {
                PlaybackController.getInstance(view.context)
                        .playNow(VideoItem(searchResult.snippet.title, searchResult.snippet.thumbnails.default.url, searchResult.id.videoId))
            }
        }

        fun setCurrentItem(item: SearchResult) {
            this.searchResult = item
        }
    }

    override fun equals(other: Any?): Boolean {
        return javaClass.simpleName.equals(other!!::class.java.simpleName)
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
