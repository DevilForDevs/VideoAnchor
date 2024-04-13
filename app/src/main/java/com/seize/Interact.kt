import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URLEncoder
import java.util.regex.Pattern

class Interact {
    data class RequestVariant(
        val data: JSONObject,
        val query: Map<String, String>,
        val headers: Map<String, String>
    )
    private val variants = listOf(
        RequestVariant(
            data = JSONObject().apply {
                put("context", JSONObject(mapOf(
                    "client" to mapOf(
                        "clientName" to "WEB",
                        "clientVersion" to "2.20200720.00.02",
                    )
                )))
            },
            headers =mapOf(
                "Content-Type" to "application/json",
                "User-Agent" to "Mozilla/5.0",
                "Origin" to "https://www.youtube.com",
            ),
            query =mapOf("key" to "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"),

            ),
        RequestVariant(
            data = JSONObject().apply {
                put("context", JSONObject(mapOf(
                    "client" to mapOf(
                        "clientName" to "ANDROID_EMBEDDED_PLAYER",
                        "clientVersion" to "17.31.35",
                        "androidSdkVersion" to 30,
                        "userAgent" to "com.google.android.youtube/17.31.35 (Linux; U; Android 11) gzip",
                        "hl" to "en",
                        "timeZone" to "UTC",
                        "utcOffsetMinutes" to 0
                    ),
                    "thirdParty" to mapOf("embedUrl" to "https://www.youtube.com/")
                )))
            },
            headers =mapOf(
                "X-YouTube-Client-Name" to "55",
                "X-YouTube-Client-Version" to "17.31.35",
                "Origin" to "https://www.youtube.com",
                "content-type" to "application/json"
            ),
            query =mapOf("key" to "AIzaSyCjc_pVEDi4qsv5MtC2dMXzpIaDoRFLsxw")
        ),
        RequestVariant(
            data = JSONObject().apply {
                put("context", JSONObject(mapOf(
                    "client" to mapOf(
                        "clientName" to "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
                        "clientVersion" to "2.0",
                        "hl" to "en",
                        "timeZone" to "UTC",
                        "utcOffsetMinutes" to 0
                    ),
                    "thirdParty" to mapOf("embedUrl" to "https://www.youtube.com/")
                )))
            },
            headers =mapOf(
                "X-YouTube-Client-Name" to "85",
                "X-YouTube-Client-Version" to "2.0",
                "Origin" to "https://www.youtube.com",
                "content-type" to "application/json"
            ),
            query =mapOf("key" to "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8")
        )
        ,RequestVariant(
            data = JSONObject().apply {
                put("context", JSONObject(mapOf(
                    "client" to mapOf(
                        "clientName" to "WEB_CREATOR",
                        "clientVersion" to "1.20220726.00.00", /*can't be used for search but for playlist browsing*/
                        "hl" to "en",
                        "timeZone" to "UTC",
                        "utcOffsetMinutes" to 0
                    )
                )))
            },
            headers =mapOf(
                "X-YouTube-Client-Name" to "62",
                "X-YouTube-Client-Version" to "1.20220726.00.00",
                "Origin" to "https://www.youtube.com",
                "content-type" to "application/json",
                "User-Agent" to "Mozilla/5.0"
            ),
            query = mapOf("key" to "AIzaSyBUPetSUmoZL-OhlxA7wSac5XinrygCqMo")
        ),RequestVariant(
            data = JSONObject().apply {
                put("context", JSONObject(mapOf(
                    "client" to mapOf(
                        "clientName" to "MWEB",
                        "clientVersion" to "2.20220801.00.00",
                        "hl" to "en",
                        "timeZone" to "UTC",
                        "utcOffsetMinutes" to 0
                    ),
                    "thirdParty" to mapOf("embedUrl" to "https://www.youtube.com/")
                )))
            },
            headers =mapOf(
                "X-YouTube-Client-Name" to "2",
                "X-YouTube-Client-Version" to "2.20220801.00.00",
                "Origin" to "https://www.youtube.com",
                "content-type" to "application/json",
            ),
            query = mapOf("key" to "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8")
        ),
        RequestVariant(
            data = JSONObject().apply {
                put("context", JSONObject(mapOf(
                    "client" to mapOf(
                        "clientName" to "ANDROID_CREATOR",
                        "clientVersion" to "22.30.100",
                        "androidSdkVersion" to 30,
                        "userAgent" to "com.google.android.apps.youtube.creator/22.30.100 (Linux; U; Android 11) gzip",
                        "hl" to "en",
                        "timeZone" to "UTC",
                        "utcOffsetMinutes" to 0
                    )
                )))
            },
            headers =mapOf(
                "X-YouTube-Client-Name" to "14",
                "X-YouTube-Client-Version" to "22.30.100",
                "content-type" to "application/json",
                "Origin" to "https://www.youtube.com",
            ),
            query =mapOf("key" to "AIzaSyD_qjV8zaaUMehtLkrKFgVeSX_Iqbtyws8")
        ),
        RequestVariant(
            data = JSONObject().apply {
                put("context", JSONObject(mapOf(
                    "client" to mapOf(
                        "clientName" to "IOS_CREATOR",
                        "clientVersion" to "22.33.101",
                        "deviceModel" to "iPhone14,3",
                        "userAgent" to "com.google.ios.ytcreator/22.33.101 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)",
                        "hl" to "en",
                        "timeZone" to "UTC",
                        "utcOffsetMinutes" to 0
                    )
                )))
            },
            headers = mapOf(
                "X-YouTube-Client-Name" to "15",
                "X-YouTube-Client-Version" to "22.33.101",
                "userAgent" to "com.google.ios.ytcreator/22.33.101 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)",
                "content-type" to "application/json",
                "Origin" to "https://www.youtube.com",
            ),
            query =mapOf("key" to "AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8")
        ),RequestVariant(
            data = JSONObject().apply {
                put("context", JSONObject(mapOf(
                    "client" to mapOf(
                        "clientName" to "IOS_MESSAGES_EXTENSION",
                        "clientVersion" to "17.33.2",
                        "deviceModel" to "iPhone14,3",
                        "userAgent" to "com.google.ios.youtube/17.33.2 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)",
                        "hl" to "en",
                        "timeZone" to "UTC",
                        "utcOffsetMinutes" to 0
                    ),
                    "thirdParty" to mapOf("embedUrl" to "https://www.youtube.com/")
                )))
            },
            headers = mapOf(
                "X-YouTube-Client-Name" to "66",
                "X-YouTube-Client-Version" to "17.33.2",
                "userAgent" to "com.google.ios.youtube/17.33.2 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)",
                "content-type" to "application/json",
                "Origin" to "https://www.youtube.com",
            ),
            query = mapOf("key" to "AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8")
        ),RequestVariant(
            data = JSONObject().apply {
                put("context", JSONObject(mapOf(
                    "client" to mapOf(
                        "clientName" to "IOS",
                        "clientVersion" to "17.33.2",
                        "deviceModel" to "iPhone14,3",
                        "userAgent" to "com.google.ios.youtube/17.33.2 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)",
                        "hl" to "en",
                        "timeZone" to "UTC",
                        "utcOffsetMinutes" to 0
                    )
                )))
            },
            headers = mapOf(
                "X-YouTube-Client-Name" to "5",
                "X-YouTube-Client-Version" to "17.33.2",
                "userAgent" to "com.google.ios.youtube/17.33.2 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)",
                "content-type" to "application/json",
                "Origin" to "https://www.youtube.com",
            ),
            query =mapOf("key" to "AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8")
        ), RequestVariant(
            data = JSONObject().apply {
                put("context", JSONObject(mapOf(
                    "client" to mapOf(
                        "clientName" to "WEB_REMIX",
                        "clientVersion" to "1.20220727.01.00"
                    )
                )))
            },
            headers =mapOf(
                "Origin" to "https://www.youtube.com",
                "Content-Type" to "application/json",
                "User-Agent" to "Mozilla/5.0",
            ),
            query =mapOf("key" to "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8")
        ),RequestVariant(
            data = JSONObject().apply {
                put("context", JSONObject(mapOf(
                    "client" to mapOf(
                        "clientName" to "ANDROID_MUSIC",
                        "clientVersion" to "5.16.51",
                        "androidSdkVersion" to 30
                    )
                )))
            },
            headers =mapOf(
                "Content-Type" to "application/json",
                "User-Agent" to "com.google.android.apps.youtube.music/",
                "Origin" to "https://www.youtube.com",
            ),
            query =mapOf("key" to "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8")
        ), RequestVariant(
            data = JSONObject().apply {
                put("context", JSONObject(mapOf(
                    "client" to mapOf(
                        "clientName" to "IOS_MUSIC",
                        "clientVersion" to "5.21",
                        "deviceModel" to "iPhone14,3"
                    )
                )))
            },
            headers =mapOf(
                "Content-Type" to "application/json",
                "User-Agent" to "com.google.ios.youtubemusic/",
                "Origin" to "https://www.youtube.com",
            ),
            query =mapOf("key" to "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"),

            )
    )
    fun formatSpeed(speed: Long): String {
        return when {
            speed > 1e9 -> String.format("%.2f GB", speed / 1e9)
            speed > 1e6 -> String.format("%.2f MB", speed / 1e6)
            speed > 1e3 -> String.format("%.2f KB", speed / 1e3)
            else -> String.format("%.2f B", speed)
        }
    }
    private fun encodeParams(params: Map<String, Any>): String {
        return params.entries.joinToString("&") { "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value.toString(), "UTF-8")}" }
    }
    fun getStreamingData(videoId:String): JSONObject? {
        for ( variant in variants){
            val keY= variant.query["key"].toString()
            /*not provides 4k ANDROID_CREATOR,IOS_CREATOR,IOS_MESSAGES_EXTENSION,IOS,android music,IOS_MUSIC,web remix*/
            /*unciphered urls IOS_MESSAGES_EXTENSION,ios,android music,IOS_MUSIC,IOS_CREATOR,ANDROID_CREATOR,ANDROID_EMBEDDED_PLAYER*/
            val url = "https://www.youtube.com/youtubei/v1/player?${encodeParams(mapOf("videoId" to videoId, "key" to keY, "contentCheckOk" to true, "racyCheckOk" to true))}"
            val requestBody = variant.data.toString()
            val request = Request.Builder()
                .url(url)
                .apply {
                    variant.headers.forEach { (key, value) ->
                        addHeader(key, value)
                    }
                }
                .post(requestBody.toRequestBody())
                .build()
            println(url)
            println(variant.data.get("context"))
            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            response.body?.use { responseBody ->
                val jsonResponse = JSONObject(responseBody.string())
                if (jsonResponse.has("streamingData")){
                    val streamingData=jsonResponse.getJSONObject("streamingData")
                    if (streamingData.has("adaptiveFormats")){
                        val adaptiveFormats=jsonResponse.getJSONObject("streamingData").getJSONArray("adaptiveFormats")
                        for (index in 0..<adaptiveFormats.length()) {
                            if(adaptiveFormats.getJSONObject(index).has("url")){
                                return jsonResponse
                            }else{
                                println("ciphered url")

                            }
                        }
                    }
                    if (streamingData.has("formats")){
                        val adaptiveFormats=jsonResponse.getJSONObject("streamingData").getJSONArray("formats")
                        for (index in 0..<adaptiveFormats.length()) {
                            if(adaptiveFormats.getJSONObject(index).has("url")){
                                return jsonResponse
                            }else{
                                println("ciphered url")

                            }
                        }
                    }
                }else{
                    return null
                }

            }

        }
        return null

    }
    private fun getTitle(source: JSONObject): String? {
        return source.getJSONArray("runs").getJSONObject(0).getString("text")

    }
    private fun getDuration(source: JSONObject): String? {
        return source.getJSONArray("runs").getJSONObject(0).getString("text")
    }
    private fun getContinuation(source: JSONObject): String? {
       return source.getJSONObject("continuationEndpoint").getJSONObject("continuationCommand").getString("token")

    }
    private fun videoId(url: String): String? {
        val regex = """^.*(?:(?:youtu\.be\/|v\/|vi\/|u\/\w\/|embed\/|shorts\/|live\/)|(?:(?:watch)?\?v(?:i)?=|\&v(?:i)?=))([^#\&\?]*).*""".toRegex()
        val matchResult = regex.find(url)
        if (matchResult != null) {
            val videoId = matchResult.groupValues[1]
            return videoId
        }
        return null
    }
    private fun getThumbnail(source: JSONObject): String? {
        val thumbs=source.getJSONArray("thumbnails")
        if (thumbs.length()>2){
            return thumbs.getJSONObject(1).getString("url")
        }
        return source.getJSONArray("thumbnails").getJSONObject(0).getString("url")
    }
    fun txt2filename(txt: String): String {
        val specialCharacters = listOf(
            "@", "#", "$", "*", "&", "<", ">", "/", "\\b", "|", "?", "CON", "PRN", "AUX", "NUL",
            "COM0", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT0",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9", ":", "\"", "'"
        )

        var normalString = txt
        for (sc in specialCharacters) {
            normalString = normalString.replace(sc, "")
        }

        return normalString
    }
    fun  getshelfRenderer(source: JSONObject): MutableList<JSONObject> {
        val itemsVideo = mutableListOf<JSONObject>()
        val vitems=source.getJSONObject("shelfRenderer").getJSONObject("content")
        if (vitems.has("verticalListRenderer")){
            println("verticalListRenderer")
            val ivds=vitems.getJSONObject("verticalListRenderer").getJSONArray("items")
            for (z in 0 until ivds.length()) {
                val ite=JSONObject()
                val rl=ivds.getJSONObject(z)
                ite.put("videoId",rl.getJSONObject("videoRenderer").getString("videoId"))
                ite.put("title",txt2filename(rl.getJSONObject("videoRenderer").getJSONObject("title").getJSONArray("runs").getJSONObject(0).getString("text")))
                if (rl.getJSONObject("videoRenderer").has("lengthText")){
                    ite.put("duration",rl.getJSONObject("videoRenderer").getJSONObject("lengthText").get("simpleText").toString())
                }else{
                    ite.put("duration","Unknown")
                }
                ite.put("thumbnail",rl.getJSONObject("videoRenderer").getJSONObject("thumbnail").getJSONArray("thumbnails").getJSONObject(0).getString("url"))
                itemsVideo.add(ite)

            }
        }
        if (vitems.has("horizontalListRenderer")){
            println("horizontalListRenderer")
            val hzi=vitems.getJSONObject("horizontalListRenderer").getJSONArray("items")
            for (l in 0 until hzi.length()) {
                val ite=JSONObject()
                val rl=hzi.getJSONObject(l)
                if (rl.has("gridVideoRenderer")){
                    ite.put("videoId",rl.getJSONObject("gridVideoRenderer").getString("videoId"))
                    ite.put("title",txt2filename(rl.getJSONObject("gridVideoRenderer").getJSONObject("title").getJSONArray("runs").getJSONObject(0).getString("text")))
                    if (rl.getJSONObject("gridVideoRenderer").has("lengthText")){
                        ite.put("duration",rl.getJSONObject("gridVideoRenderer").getJSONObject("lengthText").get("simpleText").toString())
                    }else{
                        ite.put("duration","Unknown")
                    }
                    ite.put("thumbnail",rl.getJSONObject("gridVideoRenderer").getJSONObject("thumbnail").getJSONArray("thumbnails").getJSONObject(0).getString("url"))
                    itemsVideo.add(ite)
                }
            }
        }
         return itemsVideo


    }
    fun getreelShelfRenderer(source: JSONObject): MutableList<JSONObject> {
        val itemsVideo = mutableListOf<JSONObject>()
        val reels=source.getJSONObject("reelShelfRenderer").getJSONArray("items")
        for (reel in 0 until reels.length()) {
            val ite=JSONObject()
            val rl=reels.getJSONObject(reel)
            ite.put("videoId",rl.getJSONObject("reelItemRenderer").getString("videoId"))
            try {
                ite.put("title",txt2filename(rl.getJSONObject("reelItemRenderer").getJSONObject("headline").getString("simpleText")))
            }catch (e:Exception){
                ite.put("title",txt2filename(rl.getJSONObject("reelItemRenderer").getJSONObject("headline").getJSONArray("runs").getJSONObject(0).getString("text")))
            }
            ite.put("thumbnail",rl.getJSONObject("reelItemRenderer").getJSONObject("thumbnail").getJSONArray("thumbnails").getJSONObject(0).getString("url"))
            ite.put("duration","Shorts")
            itemsVideo.add(ite)
        }
        return itemsVideo
    }
    fun elementRenderer(source: JSONObject): JSONObject {
        val jsonObject=JSONObject()
        if (source.getJSONObject("elementRenderer").getJSONObject("newElement").getJSONObject("type").getJSONObject("componentType").getJSONObject("model").has("compactVideoModel")){
            val videoDetails=source.getJSONObject("elementRenderer").getJSONObject("newElement").getJSONObject("type").getJSONObject("componentType").getJSONObject("model").getJSONObject("compactVideoModel").getJSONObject("compactVideoData").getJSONObject("videoData")
            val thumb=videoDetails.getJSONObject("thumbnail").getJSONObject("image").getJSONArray("sources").getJSONObject(0).getString("url")
            jsonObject.put("title",videoDetails.getJSONObject("metadata").getString("title"))
            jsonObject.put("duration",videoDetails.getJSONObject("thumbnail").get("timestampText"))
            jsonObject.put("thumbnail",thumb)
            jsonObject.put("videoId",videoId(videoDetails.getString("dragAndDropUrl")))
        }else{
            println(source.getJSONObject("elementRenderer").getJSONObject("newElement").getJSONObject("type").getJSONObject("componentType").getJSONObject("model"))
        }
        return jsonObject

    }
    private fun compactVideoRenderer(source: JSONObject): JSONObject {
        val itemTo=JSONObject()
        val js=source.getJSONObject("compactVideoRenderer")
        itemTo.put("title",getTitle(js.getJSONObject("title")))
        itemTo.put("duration",getDuration(js.getJSONObject("lengthText")))
        itemTo.put("thumbnail",getThumbnail(js.getJSONObject("thumbnail")))
        itemTo.put("videoId",js.getString("videoId"))
        return itemTo
    }
    fun search(term:String,continuation:String?): Triple<MutableList<JSONObject>, String?, JSONArray>? {
        val indexes= mutableListOf(0,1,4,7,8)
        val videosCollected = mutableListOf<JSONObject>()
        var nextContinuation=""
        var sugggestion=  JSONArray()
        for (index in indexes){
            val variant=variants[index]
            val client = OkHttpClient()
            val requestBody = variant.data
            if (continuation!=null){
                requestBody.put("continuation",continuation)
            }
            val keY= variant.query["key"].toString()
            val queryUrl = "https://www.youtube.com/youtubei/v1/search?${encodeParams(mapOf("query" to term, "key" to keY, "contentCheckOk" to true, "racyCheckOk" to true))}"
            val urlWithQuery = StringBuilder(queryUrl)
            println(urlWithQuery)
            println(requestBody.get("context"))
            val request = Request.Builder()
                .url(urlWithQuery.toString())
                .apply {
                    variant.headers.forEach { (key, value) ->
                        addHeader(key, value)
                    }
                }
                .post(requestBody.toString().toRequestBody())
                .build()
            val response = client.newCall(request).execute()
            response.body.use { responseBody ->
                val jsonResponse = JSONObject(responseBody.string())
                if(jsonResponse.has("refinements")){
                    sugggestion=jsonResponse.getJSONArray("refinements")
                }
                if (jsonResponse.has("onResponseReceivedCommands")){
                    if (jsonResponse.getJSONArray("onResponseReceivedCommands").getJSONObject(0).has("appendContinuationItemsAction")){
                        val sections=jsonResponse.getJSONArray("onResponseReceivedCommands").getJSONObject(0).getJSONObject("appendContinuationItemsAction").getJSONArray("continuationItems")
                        val collections=sections.getJSONObject(0).getJSONObject("itemSectionRenderer").getJSONArray("contents")
                        for (su in 0..<collections.length()) {
                            val s = collections.getJSONObject(su)
                            if (s.has("videoRenderer")){
                                val ite=JSONObject()
                                ite.put("videoId",s.getJSONObject("videoRenderer").getString("videoId"))
                                ite.put("title",txt2filename(s.getJSONObject("videoRenderer").getJSONObject("title").getJSONArray("runs").getJSONObject(0).getString("text")))
                                if (s.getJSONObject("videoRenderer").has("lengthText")){
                                    ite.put("duration",s.getJSONObject("videoRenderer").getJSONObject("lengthText").get("simpleText").toString())
                                }else{
                                    ite.put("duration","Unknown")
                                }
                                ite.put("thumbnail",s.getJSONObject("videoRenderer").getJSONObject("thumbnail").getJSONArray("thumbnails").getJSONObject(0).getString("url"))
                                videosCollected.add(ite)


                            }
                            if (s.has("reelShelfRenderer")){
                                val videos=getreelShelfRenderer(s)
                                videosCollected.addAll(videos)
                            }
                            if (s.has("shelfRenderer")){
                                val videos=getshelfRenderer(s)
                                videosCollected.addAll(videos)
                            }

                        }
                        if (sections.length()>1){
                            nextContinuation=sections.getJSONObject(1).getJSONObject("continuationItemRenderer").getJSONObject("continuationEndpoint").getJSONObject("continuationCommand").getString("token")
                        }
                    }

                }
                if (jsonResponse.has("contents")){
                    if (jsonResponse.getJSONObject("contents").has("sectionListRenderer")){
                        val conts=jsonResponse.getJSONObject("contents").getJSONObject("sectionListRenderer").getJSONArray("contents")
                        for (vu in 0..<conts.length()) {
                            val kitem=conts.getJSONObject(vu)
                            if (kitem.has("continuationItemRenderer")){
                                val cotni=getContinuation(kitem.getJSONObject("continuationItemRenderer"))
                                if (cotni!=null){
                                    nextContinuation=cotni
                                }
                            }
                            if (kitem.has("itemSectionRenderer")){
                                if(kitem.getJSONObject("itemSectionRenderer").has("continuations")){
                                    nextContinuation=kitem.getJSONObject("itemSectionRenderer").getJSONArray("continuations").getJSONObject(0).getJSONObject("nextContinuationData").getString("continuation")
                                }
                                val compactVideoRendere=jsonResponse.getJSONObject("contents").getJSONObject("sectionListRenderer").getJSONArray("contents").getJSONObject(0).getJSONObject("itemSectionRenderer").getJSONArray("contents")
                                for (su in 0..<compactVideoRendere.length()) {
                                    val itemTo=JSONObject()
                                    if (compactVideoRendere.getJSONObject(su).has("compactVideoRenderer")){
                                        val js=compactVideoRenderer(compactVideoRendere.getJSONObject(su))
                                        videosCollected.add(js)
                                    }
                                    if (compactVideoRendere.getJSONObject(su).has("videoWithContextRenderer")){
                                        val item=compactVideoRendere.getJSONObject(su).getJSONObject("videoWithContextRenderer")
                                        if (item.has("lengthText")){
                                            itemTo.put("duration",getDuration(item.getJSONObject("lengthText")))
                                        }else{
                                            itemTo.put("duration","Unknown")
                                        }
                                        itemTo.put("thumbnail",getThumbnail(item.getJSONObject("thumbnail")))
                                        itemTo.put("videoId",item.getString("videoId"))
                                        itemTo.put("title",getTitle(item.getJSONObject("headline")))
                                        videosCollected.add(itemTo)
                                    }
                                    if (compactVideoRendere.getJSONObject(su).has("elementRenderer")){
                                        if (compactVideoRendere.getJSONObject(su).getJSONObject("elementRenderer").getJSONObject("newElement").getJSONObject("type").has("componentType")){
                                            val item=elementRenderer(compactVideoRendere.getJSONObject(su))
                                            videosCollected.add(item)

                                        }

                                    }
                                }

                            }
                            if (kitem.has("elementRenderer")){
                                val item=elementRenderer((kitem))
                                videosCollected.add(item)
                            }
                            if (kitem.has("shelfRenderer")){
                                val items=kitem.getJSONObject("shelfRenderer").getJSONObject("content").getJSONObject("verticalListRenderer").getJSONArray("items")
                                for (su in 0..<items.length()) {
                                    if (items.getJSONObject(su).has("elementRenderer")){
                                        val item=elementRenderer(items.getJSONObject(su))
                                        videosCollected.add(item)
                                    }

                                }
                            }

                        }
                    }
                    if(jsonResponse.getJSONObject("contents").has("twoColumnSearchResultsRenderer")){
                        val sections=jsonResponse.getJSONObject("contents").getJSONObject("twoColumnSearchResultsRenderer").getJSONObject("primaryContents").getJSONObject("sectionListRenderer").getJSONArray("contents")
                        val collections=sections.getJSONObject(0).getJSONObject("itemSectionRenderer").getJSONArray("contents")
                        for (su in 0..<collections.length()) {
                            val s = collections.getJSONObject(su)
                            if (s.has("videoRenderer")){
                                val ite=JSONObject()
                                ite.put("videoId",s.getJSONObject("videoRenderer").getString("videoId"))
                                ite.put("title",txt2filename(s.getJSONObject("videoRenderer").getJSONObject("title").getJSONArray("runs").getJSONObject(0).getString("text")))
                                if (s.getJSONObject("videoRenderer").has("lengthText")){
                                    ite.put("duration",s.getJSONObject("videoRenderer").getJSONObject("lengthText").get("simpleText").toString())
                                }else{
                                    ite.put("duration","Unknown")
                                }
                                ite.put("thumbnail",s.getJSONObject("videoRenderer").getJSONObject("thumbnail").getJSONArray("thumbnails").getJSONObject(0).getString("url"))
                                videosCollected.add(ite)

                            }
                            if (s.has("reelShelfRenderer")){
                                val videos=getreelShelfRenderer(s)
                                videosCollected.addAll(videos)
                            }
                            if (s.has("shelfRenderer")){
                                val videos=getshelfRenderer(s)
                                videosCollected.addAll(videos)
                            }

                        }
                        if (sections.length()>1){
                            nextContinuation=sections.getJSONObject(1).getJSONObject("continuationItemRenderer").getJSONObject("continuationEndpoint").getJSONObject("continuationCommand").getString("token")
                        }
                    }

                }
                if (jsonResponse.has("continuationContents")){
                    val items=jsonResponse.getJSONObject("continuationContents").getJSONObject("sectionListContinuation").getJSONArray("contents").getJSONObject(0).getJSONObject("itemSectionRenderer").getJSONArray("contents")
                    for (su in 0..<items.length()) {
                        val itemTo=JSONObject()
                        if (items.getJSONObject(su).has("compactVideoRenderer")){
                            val js=compactVideoRenderer(items.getJSONObject(su))
                            videosCollected.add(js)
                        }
                        if (items.getJSONObject(su).has("elementRenderer")){
                            val item=elementRenderer(items.getJSONObject(su))
                            videosCollected.add(item)
                        }
                    }
                    if(jsonResponse.getJSONObject("continuationContents").getJSONObject("sectionListContinuation").has("continuations")){
                        nextContinuation=jsonResponse.getJSONObject("continuationContents").getJSONObject("sectionListContinuation").getJSONArray("continuations").getJSONObject(0).getJSONObject("reloadContinuationData").getString("continuation")
                    }
                }

            }
            if (!response.isSuccessful){
               response.close()
            }
            if (videosCollected.size!=0){
                val uniqueItemsVideo: MutableList<JSONObject> =videosCollected.distinctBy { it.get("videoId") }.toMutableList()
                return Triple(uniqueItemsVideo,nextContinuation,sugggestion)
            }
        }
        return null

    }
    fun playlistVideoRendrer(source: JSONArray): Pair<MutableList<JSONObject>, String?>  {
        val videos=mutableListOf<JSONObject>()
        var continuation=""
        for (su in 0..<source.length()) {
            val ite=JSONObject()
            val item=source.getJSONObject(su)
            if (item.has("playlistVideoRenderer")){
                if (item.getJSONObject("playlistVideoRenderer").getJSONObject("lengthText").has("simpleText")){
                    ite.put("duration",item.getJSONObject("playlistVideoRenderer").getJSONObject("lengthText").get("simpleText"))
                }
                if (item.getJSONObject("playlistVideoRenderer").getJSONObject("lengthText").has("runs")){
                    ite.put("duration",item.getJSONObject("playlistVideoRenderer").getJSONObject("lengthText").getJSONArray("runs").getJSONObject(0).getString("text"))
                }
                ite.put("thumbnail",getThumbnail(item.getJSONObject("playlistVideoRenderer").getJSONObject("thumbnail")))
                ite.put("title",getTitle(item.getJSONObject("playlistVideoRenderer").getJSONObject("title")))
                ite.put("videoId",item.getJSONObject("playlistVideoRenderer").get("videoId"))
            }
            if (item.has("continuationItemRenderer")){
               val conti=getContinuation(item.getJSONObject("continuationItemRenderer"))
                if (conti!=null){
                    continuation=conti
                }
            }
            videos.add(ite)
        }
        return Pair(videos,continuation)
    }
    fun streamingDataFromHtml(url: String){
        try {
            val doc = Jsoup.connect(url).get()
            val scriptTags = doc.select("script")
            for (scriptTag in scriptTags) {
                val scriptContent = scriptTag.data().trim()
                if (scriptContent.startsWith("var ytInitialData")) {
                    val jsonString = scriptContent.substringAfter("{").substringBeforeLast("}")
                    val jsonObject = JSONObject("{$jsonString}")
                    /*secondaryResults, results, autoplay*/
                    println(jsonObject)

                }
            }

        } catch (e: IOException) {
            println("Error fetching the web page: ${e.message}")

        }

    }
    fun getPlayListItemsFromHtml(url: String):Pair<MutableList<JSONObject>, String?>?{
        try {
            val doc = Jsoup.connect(url).get()
            val scriptTags = doc.select("script")
            for (scriptTag in scriptTags) {
                val scriptContent = scriptTag.data().trim()
                if (scriptContent.startsWith("var ytInitialData")) {
                    val jsonString = scriptContent.substringAfter("{").substringBeforeLast("}")
                    val jsonObject = JSONObject("{$jsonString}")
                    val contAndItems=jsonObject.getJSONObject("contents").getJSONObject("twoColumnBrowseResultsRenderer").getJSONArray("tabs").getJSONObject(0).getJSONObject("tabRenderer").getJSONObject("content").getJSONObject("sectionListRenderer").getJSONArray("contents").getJSONObject(0).getJSONObject("itemSectionRenderer").getJSONArray("contents").getJSONObject(0).getJSONObject("playlistVideoListRenderer").getJSONArray("contents")
                    val videos=playlistVideoRendrer(contAndItems)
                   if (videos.first.size!=0){
                       return Pair(videos.first,videos.second)
                   }

                }
            }

        } catch (e: IOException) {
            println("Error fetching the web page: ${e.message}")
            return null
        }
        return null

    }
    fun playlist(continuation: String?): Pair<MutableList<JSONObject>, String?>? {
        val videosCollected = mutableListOf<JSONObject>()
        var nextContinuation:String?=null
        val indedxes= mutableListOf(0,1,4,7,8)
        for (index in indedxes){
            val variant=variants[index]
            val client = OkHttpClient()
            val baseApiUrl = "https://www.youtube.com/youtubei/v1/browse"
            val requestBody = variant.data
            if (continuation!=null){
                requestBody.put("continuation",continuation)
            }
            val urlWithQuery = StringBuilder(baseApiUrl)
            if (variant.query.isNotEmpty()) {
                urlWithQuery.append("?")
                variant.query.forEach { (key, value) ->
                    urlWithQuery.append("$key=$value&")
                }
                urlWithQuery.deleteCharAt(urlWithQuery.length - 1)
            }
            val request = Request.Builder()
                .url(urlWithQuery.toString())
                .apply {
                    variant.headers.forEach { (key, value) ->
                        addHeader(key, value)
                    }
                }
                .post(requestBody.toString().toRequestBody())
                .build()
            val response = client.newCall(request).execute()
            response.body?.use { responseBody ->
                val jsonResponse = JSONObject(responseBody.string())
                if (jsonResponse.has("onResponseReceivedActions")){
                    val sections=jsonResponse.getJSONArray("onResponseReceivedActions").getJSONObject(0).getJSONObject("appendContinuationItemsAction").getJSONArray("continuationItems")
                    val videos=playlistVideoRendrer(sections)
                    videosCollected.addAll(videos.first)
                    nextContinuation=videos.second
                }
                if (jsonResponse.has("continuationContents")){
                    val cmpvi=jsonResponse.getJSONObject("continuationContents").getJSONObject("playlistVideoListContinuation")
                    val videos=playlistVideoRendrer(cmpvi.getJSONArray("contents"))
                    videosCollected.addAll(videos.first)
                    nextContinuation=cmpvi.getJSONArray("continuations").getJSONObject(0).getJSONObject("nextContinuationData").getString("continuation")
                }

            }
            if (!response.isSuccessful){
                println("failed to get json")
                response.close()
                return null
            }
            if (videosCollected.size!=0){
                return Pair(videosCollected,nextContinuation)
            }
        }

        return null

    }


}