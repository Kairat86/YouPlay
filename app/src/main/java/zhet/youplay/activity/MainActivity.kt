package zhet.youplay.activity

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.SearchView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_main.*
import zhet.youplay.BuildConfig
import zhet.youplay.R
import zhet.youplay.adapter.PlayListAdapter
import zhet.youplay.adapter.SearchResultAdapter
import zhet.youplay.singleton.YouPlay
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
    }

    private val stack = Stack<RecyclerView.Adapter<out RecyclerView.ViewHolder>>()
    private var ad: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isNetworkConnected()) {
            setTitle(R.string.network_error)
            setContentView(R.layout.activity_no_net)
            return
        }
        init()
    }


    private fun init() {
        setTitle(R.string.popular)
        setContentView(R.layout.activity_main)
        search(getString(R.string.popular_music) + " ${Calendar.getInstance().get(Calendar.YEAR)}", "playlist", 6, null)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        MobileAds.initialize(this, getString(R.string.app_id))
        ad = InterstitialAd(this)
        ad?.adUnitId = getString(R.string.int_id)
        ad?.loadAd(AdRequest.Builder().build())
    }

    override fun onBackPressed() {
        super.onBackPressed()
        ad?.show()
    }

    private fun search(query: String, type: String, maxResults: Long, category: String?) {
        prgrBar.visibility = VISIBLE
        val youTube = YouPlay.instance
        val search = youTube.search().list("id,snippet")
        search.key = BuildConfig.KEY
        search.q = query
        search.type = type
        search.fields = "items(id/playlistId,snippet/title,snippet/thumbnails/*/url,id/videoId),nextPageToken"
        search.maxResults = maxResults
        search.videoCategoryId = category
        Thread {
            var searchResponse = search.execute()
            val searchResultList = searchResponse.items
            runOnUiThread {
                var isRunning = false
                if (category == null) {
                    val playListAdapter = PlayListAdapter(searchResultList, prgrBar, stack)
                    rv.adapter = playListAdapter
                    if (!stack.contains(playListAdapter)) {
                        stack.push(playListAdapter)
                    }
                } else {
                    val searchResultAdapter = SearchResultAdapter(searchResultList)
                    rv.adapter = searchResultAdapter
                    rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                            val manager = recyclerView?.layoutManager as LinearLayoutManager
                            if (manager.findLastVisibleItemPosition() == manager.itemCount - 1) {
                                val nextPageToken = searchResponse.nextPageToken
                                Log.i(TAG, "nextPageToken=>$nextPageToken")
                                search.pageToken = nextPageToken
                                Thread {
                                    isRunning = true
                                    searchResponse = search.execute()
                                    val positionStart = searchResultList.size
                                    searchResultList.addAll(searchResponse.items)
                                    isRunning = false
                                    runOnUiThread {
                                        searchResultAdapter.notifyItemRangeInserted(positionStart, searchResponse.items.size)
                                        prgrBar.visibility = GONE
                                    }
                                }.start()

                            }
                        }
                    })
                    if (!stack.contains(searchResultAdapter)) {
                        stack.push(searchResultAdapter)
                    }
                }
                if (!isRunning) prgrBar.visibility = GONE
            }
        }.start()
    }

    fun refresh(view: View) {
        if (isNetworkConnected()) init()
    }

    private fun isNetworkConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null;
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        (menu?.findItem(R.id.action_search)?.actionView as SearchView).setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                search(query, "video", 25, "10")
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                stack.pop()
                if (!stack.isEmpty()) {
                    rv.adapter = stack.peek()
                } else {
                    finish()
                }
            }
        }
        return true
    }
}
