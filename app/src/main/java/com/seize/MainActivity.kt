package com.seize


import Interact
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.webkit.URLUtil
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.regex.Pattern


class MainActivity : AppCompatActivity(),AdapterMain.OnItemClickListener {
    var recyclerView:RecyclerView?=null
    var adapter:AdapterMain?=null
    var model_list_= mutableListOf<JSONObject>()
    val scrapper=Interact()
    var progressBar:ProgressBar?=null
    var canMakeRequest=true
    var continuationToken:String?=null
    var queryTerm: String? =null
    var canLoadMore=false
    var isPlaylist=false
    var itemsL = mutableListOf<JSONObject>()
    var goToDownloadsActivity:ImageButton?=null
    private val videoIdAdded= mutableListOf<String>()
    private val coroutToSearch = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutineToGetJson = CoroutineScope(Dispatchers.IO)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbarMain)
        recyclerView=findViewById(R.id.recy)
        progressBar=findViewById(R.id.pgb)
        goToDownloadsActivity=findViewById(R.id.goToDownloads)
        val layoutManager = LinearLayoutManager(this)
        recyclerView!!.layoutManager=layoutManager
        adapter= AdapterMain(this,model_list_,this)
        recyclerView!!.adapter=adapter
        setSupportActionBar(toolbar)
        val searchView: SearchView = toolbar.findViewById(R.id.searchView)
        searchView.setIconifiedByDefault(false)
        goToDownloadsActivity!!.setOnClickListener{
            val intent = Intent(this, DownloadsActivity::class.java)
            startActivity(intent)
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                continuationToken=null
                queryTerm=query.toString()
                model_list_.clear()
                adapter!!.notifyDataSetChanged()
                search()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.toString().isNotEmpty()&&canMakeRequest){
                    model_list_.clear()
                    adapter!!.notifyDataSetChanged()
                    continuationToken=null
                    val newTextString = newText.toString()
                    val isValid = URLUtil.isValidUrl(newTextString) && Patterns.WEB_URL.matcher(newTextString).matches()
                    if (isValid){
                        println("url")
                        val isId=mks(newTextString)
                        if (isId==null){
                            println("playlist/channel")
                            progressBar!!.visibility = View.VISIBLE
                            coroutineToGetJson.launch {
                                val videos=executeRequest(newTextString)
                                withContext(Dispatchers.Main){
                                    if (videos!=null){
                                        if (videos.has("videos")){
                                            val mkv=videos.getJSONArray("videos")
                                            isPlaylist=true
                                            videoIdAdded.clear()
                                            searchView.setQuery("", false)
                                            println(mkv)
                                            setToUi(mkv)
                                        }
                                        if (videos.has("nextContinuation")){
                                            continuationToken = videos.getString("nextContinuation")
                                        }
                                    }
                                }
                            }
                        }else{
                            searchView.setQuery("", false)
                        }
                    }else{
                        progressBar!!.visibility = View.VISIBLE
                        model_list_.clear()
                        videoIdAdded.clear()
                        adapter!!.notifyDataSetChanged()
                        println("getting results")
                        canMakeRequest = false
                        isPlaylist=false
                        coroutToSearch.launch {
                            try {
                                val results = scrapper.search(newTextString, null)
                                if (results!=null){
                                    if (results.has("suggestion")){
                                        val suggestions = results.getJSONArray("suggestion")
                                        for (vu in 0..<suggestions.length()) {
                                            val itemSuggestion = JSONObject()
                                            itemSuggestion.put("suggestion", suggestions.get(vu))
                                            model_list_.add(itemSuggestion)

                                        }
                                        withContext(Dispatchers.Main) {
                                            adapter!!.notifyDataSetChanged()
                                            progressBar!!.visibility = View.GONE
                                        }
                                    }
                                }

                            }catch (e:Exception){
                                runOnUiThread {
                                    Toast.makeText(applicationContext, "Failed to get result", Toast.LENGTH_SHORT).show()
                                    searchView.setQuery("", false)
                                    progressBar!!.visibility = View.GONE
                                }

                            }
                        }
                    }





                }
                return true
            }
        })
        recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    && firstVisibleItemPosition >= 0
                ) {
                    println("loading more")
                    println(continuationToken)
                    if (continuationToken!=null){
                        if (isPlaylist){
                            playlistFromCotninuation()
                        }else{
                            search()
                        }
                    }
                }
            }
        })

    }
    fun executeRequest(url: String): JSONObject? {
        val videosFromPLaylist=scrapper.getPlayListItemsFromHtml(url)
        if (videosFromPLaylist!=null){
            return videosFromPLaylist
        }
        val videoFromChannel=scrapper.getFromChannelHtml(url)
        if (videoFromChannel!=null){
            return videoFromChannel
        }
        return null
    }
    fun mks(newTextString: String): Boolean? {
        val instaId = extractInstagramVideoId(newTextString)
        val youtubeId = scrapper.videoId(newTextString)
        if (instaId!=null){
            println("instagram video")
            progressBar!!.visibility = View.VISIBLE
            coroutineToGetJson.launch{
                try {
                    val infoJson=instagram(instaId)
                    withContext(Dispatchers.Main) {
                        progressBar!!.visibility=View.GONE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            DownloadsActivity.InstagramVideo(infoJson,applicationContext)
                            Toast.makeText(applicationContext,"Added To Downloads",Toast.LENGTH_SHORT).show()
                        }else{
                            if (ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                itemsL=infoJson
                                val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), 1)
                            }else{
                                DownloadsActivity.InstagramVideo(infoJson,applicationContext)
                                Toast.makeText(applicationContext,"Added To Downloads",Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }catch (e:Exception){
                    runOnUiThread {
                        progressBar!!.visibility=View.GONE
                        Toast.makeText(applicationContext,"Streaming data not found",Toast.LENGTH_SHORT).show()
                    }
                }
            }
            return true
        }
        if (youtubeId!=null){
            println(youtubeId)
            progressBar!!.visibility=View.VISIBLE
            coroutineToGetJson.launch{
                try {
                    val infoJson=scrapper.getStreamingData(youtubeId)
                    withContext(Dispatchers.Main) {
                        progressBar!!.visibility=View.GONE
                        if (infoJson!=null){
                            askResolution(infoJson)
                        }
                    }
                }catch (e:Exception){
                    runOnUiThread {
                        progressBar!!.visibility=View.GONE
                        Toast.makeText(applicationContext,"Streaming data not found",Toast.LENGTH_SHORT).show()
                    }
                }
            }
            return true
        }
        return null
    }
    fun playlistFromCotninuation(){
        progressBar!!.visibility=View.VISIBLE
        coroutToSearch.launch {
            var videos: JSONArray? = null
            try {
                val result = withContext(Dispatchers.IO) {
                    scrapper.playlist(continuationToken)
                }
                if (result == null) {
                    withContext(Dispatchers.Main){
                        progressBar!!.visibility = View.GONE
                        println("Reached end")
                        Toast.makeText(applicationContext, "Reached End", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    if (result.has("videos")){
                       videos=result.getJSONArray("videos")
                    }

                }
                if (result != null) {
                    if (result.has("nextContinuation")){
                        continuationToken = result.getString("nextContinuation")
                    }
                }
               withContext(Dispatchers.Main){
                   if (videos != null) {
                       setToUi(videos)
                   }
               }
            }catch (e:Exception){
                runOnUiThread {
                    Toast.makeText(applicationContext, "Failed to get result", Toast.LENGTH_SHORT).show()
                    progressBar!!.visibility = View.GONE
                }
            }
        }
    }
    fun fromPlaylist(url: String){
        progressBar!!.visibility=View.VISIBLE
        model_list_.clear()
        adapter?.notifyDataSetChanged()
        coroutToSearch.launch {
            var videos: JSONArray? = null
            try {
                val result = withContext(Dispatchers.IO) {
                    scrapper.getPlayListItemsFromHtml(url)
                }
                if (result != null) {
                    if (result.has("videos")){
                        videos=result.getJSONArray("videos")
                    }
                    if (result.has("nextContinuation")){
                        continuationToken = result.getString("nextContinuation")
                    }
                }
                /*if (videos != null) {
                    for (video in videos) {
                        video.put("result", true)
                        model_list_.add(video)
                    }
                }*/
                withContext(Dispatchers.Main){
                    if (videos != null) {
                        setToUi(videos)
                    }
                }
            }catch (e:Exception){
                runOnUiThread {
                    Toast.makeText(applicationContext, "Failed to get result", Toast.LENGTH_SHORT).show()
                    progressBar!!.visibility = View.GONE
                }
            }
        }
    }

    private fun getPlaylistId(newTextString: String): String? {
        val parsedUrl = URL(newTextString)
        val query = parsedUrl.query ?: return null
        val parameters = query.split("&").associate {
            val (key, value) = it.split("=")
            key to value
        }
        return parameters["list"]

    }

    fun isUrl(url: String): Boolean {
        try {
            URL(url)
            return true
        }catch (e:Exception){
            println(e)
        }
       return false
    }
    override fun onItemClick(position: Int, item: JSONObject) {
        if (item.has("suggestion")){
            continuationToken=null
            queryTerm=item.getString("suggestion")
            model_list_.clear()
            adapter!!.notifyDataSetChanged()
            search()
        }else{
            Toast.makeText(this,"Play feature not yet implemented",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode==1){
            if (itemsL[0].has("mimeType")){
                DownloadsActivity.myStaticFunction(applicationContext,itemsL)
                Toast.makeText(applicationContext,"Added to downloads",Toast.LENGTH_SHORT).show()
            }else{
                DownloadsActivity.InstagramVideo(itemsL,applicationContext)
                Toast.makeText(applicationContext,"Added to downloads",Toast.LENGTH_SHORT).show()
            }
        }

    }

    fun askResolution(infoJson:JSONObject){
        if (infoJson.toString()==""){
            println("failed to get json")
        }else{
            try {
                val adaptiveFormats=infoJson.getJSONObject("streamingData").getJSONArray("adaptiveFormats")
                var audio_length: Long? =null
                var audio_url:String?=null
                val map= HashMap<String,JSONObject>()
                val dataList = mutableListOf<String>()
                var c720:Long?=null
                var c360:Long?=null
                val dialogView = layoutInflater.inflate(R.layout.custom_dialog_layout, null)
                val listView = dialogView.findViewById<ListView>(R.id.listView)
                val isMuxingSupported = android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.N
                for (i in adaptiveFormats.length() - 1 downTo 0) {
                    val json2=JSONObject(adaptiveFormats[i].toString())
                    println(json2)
                    if ("audio/mp4" in json2.getString("mimeType").lowercase()){
                        var text="(${scrapper.formatSpeed(json2.getString("contentLength").toLong())}) Audio bitrate(${scrapper.formatSpeed(json2.getInt("bitrate").toLong())}/s)"
                        if (json2.getInt("itag")==140){
                            audio_length=json2.getString("contentLength").toLong()
                            if (json2.has("signatureCipher")){
                                val cipher=json2.getString("signatureCipher")
                                val url = cipher.split("&url=")[1]
                                audio_url= URLDecoder.decode(url, "UTF-8")
                            }else{
                                audio_url=json2.getString("url")
                            }
                            audio_url=json2.getString("url")
                            val thumbsUpEmoji = "\uD83D\uDC4D"
                            text="(${scrapper.formatSpeed(json2.getString("contentLength").toLong())}) Audio bitrate(${scrapper.formatSpeed(json2.getInt("bitrate").toLong())}/s)$thumbsUpEmoji"
                        }
                        dataList.add(text)
                        map.put(text,json2)
                    }
                    if("video/mp4" in json2.getString("mimeType").lowercase()){
                        if ("avc" in json2.getString("mimeType")){
                            val text="${json2.get("qualityLabel")}  (${scrapper.formatSpeed(json2.getString("contentLength").toLong()+ audio_length!!)}) video&audio"
                            if (isMuxingSupported){
                                dataList.add(text)
                            }
                            map.put(text,json2)
                            if (json2.getString("qualityLabel").contains("360")){
                                c360= json2.getString("contentLength").toLong()
                            }
                            if (json2.getString("qualityLabel").contains("720")){
                                c720= json2.getString("contentLength").toLong()
                            }
                        }
                    }
                    if("video/webm" in json2.getString("mimeType").lowercase()){
                        val resolutions = listOf(
                            "144p",
                            "240p",
                            "360p",
                            "480p",
                            "720p",
                            "1080p"
                        )
                        println(json2.get("qualityLabel"))
                        if (json2.getString("qualityLabel")!in resolutions){
                            val text="${json2.get("qualityLabel")}  (${scrapper.formatSpeed(json2.getString("contentLength").toLong()+ audio_length!!)}) vlc/mx player"
                            if (isMuxingSupported){
                                dataList.add(text)
                            }
                            map[text] = json2
                        }
                    }
                }
                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, dataList)
                listView.adapter = adapter
                val progressiveFormats=infoJson.getJSONObject("streamingData").getJSONArray("formats")
                for (i in 0 until progressiveFormats.length()) {
                    val json2=JSONObject(progressiveFormats[i].toString())
                    println(json2)
                    if (json2.getString("qualityLabel").contains("360")){
                        val thumbsUpEmoji = "\uD83D\uDC4D"
                        val text="${json2.get("qualityLabel")}  (${scrapper.formatSpeed(c360!!+ audio_length!!)}) Video+Audio$thumbsUpEmoji"
                        dataList.add(0,text)
                        map.put(text,json2)
                        adapter.notifyDataSetChanged()
                    }else{
                        if (json2.getString("qualityLabel").contains("720")){
                            val thumbsUpEmoji = "\uD83D\uDC4D"
                            val text="${json2.get("qualityLabel")}  (${scrapper.formatSpeed(c720!!+ audio_length!!)}) Video+Audio$thumbsUpEmoji"
                            dataList.add(0,text)
                            map.put(text,json2)
                            adapter.notifyDataSetChanged()
                        }else{
                            val thumbsUpEmoji = "\uD83D\uDC4D"
                            val text="${json2.get("qualityLabel")}  (Unknown Size) Video+Audio$thumbsUpEmoji"
                            dataList.add(0,text)
                            map.put(text,json2)
                            adapter.notifyDataSetChanged()
                        }
                    }


                }
                val infol= mutableListOf<JSONObject>()
                var alertDialog: AlertDialog? = null
                val title2=scrapper.txt2filename(infoJson.getJSONObject("videoDetails").getString("title"))
                val thumbnail=infoJson.getJSONObject("videoDetails").getJSONObject("thumbnail").getJSONArray("thumbnails").getJSONObject(2).getString("url")
                listView.setOnItemClickListener { parent, view, position, id ->
                    alertDialog?.dismiss()
                    val selectedItem = dataList[position]
                    val mjs=map.get(selectedItem)
                    if (mjs != null) {
                        mjs.put("audio",audio_url)
                        mjs.put("title",title2)
                        mjs.put("thumbnail",thumbnail)
                        mjs.put("audiolength",audio_length)
                        mjs.put("videoId",infoJson.getJSONObject("videoDetails").getString("videoId"))
                        infol.add(mjs)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            DownloadsActivity.myStaticFunction(applicationContext,infol)
                            Toast.makeText(applicationContext,"Added To Downloads",Toast.LENGTH_SHORT).show()
                        }else{
                            if (ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                itemsL=infol
                                val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                                Toast.makeText(applicationContext,"Give Permission and try again",Toast.LENGTH_SHORT).show()
                                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), 1)
                            }else{
                                DownloadsActivity.myStaticFunction(applicationContext,infol)
                                Toast.makeText(applicationContext,"Added To Downloads",Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                }
                val builder = AlertDialog.Builder(this@MainActivity)
                var tile="Muxing not supported on this device"
                if (isMuxingSupported){
                    tile="* Require merging"
                }
                builder.setView(dialogView as View)
                    .setTitle(tile)
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        Toast.makeText(applicationContext,"Paste url and again and select format",Toast.LENGTH_SHORT).show()
                    }
                alertDialog = builder.create()
                alertDialog.show()
            }catch (e:Exception){
                e.printStackTrace()
            }
        }

    }

    override fun onImageViewClick(position: Int, item: JSONObject) {
        progressBar!!.visibility=View.VISIBLE
        coroutineToGetJson.launch{
            try {
                val infoJson=scrapper.getStreamingData(item.getString("videoId"))
                withContext(Dispatchers.Main) {
                    progressBar!!.visibility=View.GONE
                    if (infoJson!=null){
                        askResolution(infoJson)
                    }else{
                        Toast.makeText(this@MainActivity,"Streaming data not found",Toast.LENGTH_SHORT)
                    }

                }
            }catch (e:Exception){
                runOnUiThread {
                    Toast.makeText(applicationContext,"Failed to get streaming data",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun setToUi(videos:JSONArray){
        val startIndex=model_list_.size
        val folder = File("${this@MainActivity.filesDir}/thumbnail")
        if (!folder.exists()) {
            folder.mkdir()
        }
        for (su in 0..<videos.length()) {
            val v=videos.getJSONObject(su)
            val thumb=v.getString("thumbnail")
            val cleanedUrl = thumb.replace("\\/", "/")
            v.put("thumbnail",cleanedUrl)
            v.put("result",true)
            if (!videoIdAdded.contains(v.getString("videoId"))){
                model_list_.add(v)
                videoIdAdded.add(v.getString("videoId"))
            }
        }
        adapter?.notifyItemRangeInserted(startIndex, videos.length())
        progressBar!!.visibility = View.GONE
    }
    private fun search() {
        if (queryTerm != null) {
            progressBar!!.visibility=View.VISIBLE
            coroutToSearch.launch {
                var videos: JSONArray? = null
                println(continuationToken)
                try {
                    val result = withContext(Dispatchers.IO) {
                        scrapper.search(queryTerm!!, continuationToken)

                    }
                    canLoadMore=true
                    if (result != null) {
                        if (result.has("videos")){
                            videos = result.getJSONArray("videos")
                        }
                        if (result.has("nextContinuation")){
                            continuationToken = result.getString("nextContinuation")
                        }

                    }

                    withContext(Dispatchers.Main){
                        if (videos != null) {
                            setToUi(videos)
                        }
                    }
                }catch (e:Exception){
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Failed to get result", Toast.LENGTH_SHORT).show()
                        progressBar!!.visibility = View.GONE
                    }
                }
            }

        }
    }
    fun extractInstagramVideoId(url: String): String? {
        val pattern = "(?:https?:\\/\\/)?(?:www\\.)?instagram\\.com\\/?([a-zA-Z0-9\\.\\_\\-]+)?\\/([p]+)?([reel]+)?([tv]+)?([stories]+)?\\/([a-zA-Z0-9\\-\\_\\.]+)\\/?([0-9]+)?"
        val matcher = Pattern.compile(pattern).matcher(url)
        return if (matcher.find()) {
            matcher.group(6)
        } else {
            null
        }
    }
    fun getInstagramJson(vid: String): JSONObject? {
        var stm:JSONObject?=null
        val baseUrl = "https://www.instagram.com/graphql/query/"

        /*2b0673e0dc4580674a88d426fe00ea90*/
        /*9f8827793ef34641b2fb195d4d41151c*/
        /* val queryHash = "2b0673e0dc4580674a88d426fe00ea90"*/
        try {
            val queryHash = "9f8827793ef34641b2fb195d4d41151c"

            val variables = mapOf("shortcode" to vid)
            val variablesJson = Gson().toJson(variables)

            val query = mapOf(
                "query_hash" to queryHash,
                "variables" to variablesJson
            )
            val updatedUrl = updateUrl(baseUrl, query)
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(updatedUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 11; Samsung Galaxy S20)")
                .build()
            val response=client.newCall(request).execute()
            val jsonResponse = response.body.string()
            val js=JSONObject(jsonResponse)
            val data=js.getJSONObject("data")
            stm=data.getJSONObject("shortcode_media")
        }catch (e:Exception){
            val queryHash = "2b0673e0dc4580674a88d426fe00ea90"

            val variables = mapOf("shortcode" to vid)
            val variablesJson = Gson().toJson(variables)

            val query = mapOf(
                "query_hash" to queryHash,
                "variables" to variablesJson
            )
            val updatedUrl = updateUrl(baseUrl, query)
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(updatedUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 11; Oppo Find X3 Pro; Build/RKQ1.210522.002) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.9999.99 Mobile Safari/537.36")
                .build()
            val response=client.newCall(request).execute()
            val jsonResponse = response.body.string()
            val js=JSONObject(jsonResponse)
            val data=js.getJSONObject("data")
            stm=data.getJSONObject("shortcode_media")
        }
        return stm

    }
    fun instagram(vid: String): MutableList<JSONObject> {
        val items = mutableListOf<JSONObject>()
        val stm=getInstagramJson(vid)
        if (stm.toString().contains("edge_sidecar_to_children")){
            val edgeSidecarToChildren = stm?.getJSONObject("edge_sidecar_to_children")
            val edgesArray = edgeSidecarToChildren?.getJSONArray("edges")
            val edge= stm?.getJSONObject("edge_media_to_caption")
            val edges= edge?.getJSONArray("edges")
            if (edgesArray != null) {
                for (i in 0..<edgesArray.length()) {
                    val item=JSONObject()
                    val edgeObject = edgesArray.getJSONObject(i)
                    /* if (edges != null) {
                         if(edges.length()!=0){
                             val ege1=edges.getJSONObject(0)
                             val nd=ege1.getJSONObject("node")
                             item.put("title","InstagramVideo($vid)$i")
                             *//* if (nd.toString().contains("text")){
                                                 item.put("title","InstagramVideo($vid)")
                                                 *//**//*item.put("title",nd.get("text"))*//**//*
                                                }*//**//*else{
                                                    item.put("title","InstagramVideo($vid)")
                                                }*//*
                        }
                    }*/
                    val nodeObject = edgeObject.getJSONObject("node")
                    item.put("title","InstagramVideo$i Id($vid)")
                    /* if (nodeObject.toString().contains("accessibility_caption")){
                             if (nodeObject.get("accessibility_caption")!=null){
                                 item.put("title","InstagramVideo($vid)")
                                 *//*item.put("title",nodeObject.get("accessibility_caption"))*//*
                                }*//*else{
                                    item.put("title","InstagramVideo($vid)")
                                }*//*
                            }*/
                    if (nodeObject.getBoolean("is_video")){
                        item.put("is_video",true)
                        item.put("video_url",nodeObject.get("video_url"))
                    }else{
                        item.put("is_video",false)
                        item.put("display_url",nodeObject.get("display_url"))
                    }
                    items.add(item)


                }
            }
        }else{
            val item=JSONObject()
            val edge= stm?.getJSONObject("edge_media_to_caption")
            val edges= edge?.getJSONArray("edges")
            /* if(edges.length()!=0){
                 val ege1=edges.getJSONObject(0)
                 val nd=ege1.getJSONObject("node")
                 if (nd.toString().contains("text")){
                     item.put("title",nd.get("text"))
                     println(nd.get("text"))
                 }else{
                     item.put("title","InstagramVideo($vid)")
                 }
             }*/
            if (stm != null) {
                if(stm.getBoolean("is_video")){
                    println(stm.get("video_url"))
                    item.put("is_video",true)
                    item.put("video_url",stm.get("video_url"))
                    item.put("title","InstagramVideoId($vid)")

                }else{
                    println(stm.get("display_url"))
                    item.put("is_video",false)
                    item.put("display_url",stm.get("display_url"))
                    item.put("title","InstagramVideoId($vid)")

                }
            }
            items.add(item)

        }
        return items

    }
    fun updateUrl(baseUrl: String, query: Map<String, Any>): String {
        val queryString = query.entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value.toString(), "UTF-8")}"
        }
        return "$baseUrl?$queryString"
    }
}
