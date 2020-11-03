package play.zhet.you.activity

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import kotlinx.android.synthetic.main.activity_main.*
import play.zhet.you.BuildConfig
import play.zhet.you.R
import play.zhet.you.adapter.PlayListAdapter
import play.zhet.you.adapter.SearchResultAdapter
import play.zhet.you.singleton.YouPlay
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this).setTitle(R.string.permission_required).setMessage(R.string.msg_perm_overlay).setPositiveButton(android.R.string.ok) { _, _ -> startSettingsActivity() }.show()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startSettingsActivity()
        }
        ad = InterstitialAd(this)
        ad?.adUnitId = getString(R.string.int_id)
        ad?.adListener = object : AdListener() {
            override fun onAdFailedToLoad(p0: Int) {
                search(getString(R.string.popular_music) + " ${Calendar.getInstance().get(Calendar.YEAR)}", "playlist", 6, null)
            }

            override fun onAdLoaded() {
                search(getString(R.string.popular_music) + " ${Calendar.getInstance().get(Calendar.YEAR)}", "playlist", 6, null)
                ad?.show()
            }
        }
        ad?.loadAd(AdRequest.Builder().build())
    }

    private fun startSettingsActivity() {
        val intent = Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:$packageName"))
        startActivity(intent)
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
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            val manager = recyclerView.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                            if (manager.findLastVisibleItemPosition() == manager.itemCount - 1) {
                                val nextPageToken = searchResponse.nextPageToken
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

    private fun isNetworkConnected() = (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo != null

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (stack.isEmpty()) {
                    super.onBackPressed()
                } else {
                    stack.pop()
                    if (!stack.isEmpty()) {
                        rv.adapter = stack.peek()
                    } else {
                        finish()
                    }
                }

            }
        }
        return true
    }
}
