import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.UUID

class Youtubep {
    fun search(term: String, conti: String?): Triple<MutableList<JSONObject>, String?,MutableList<String>> {
        val queryUrl = "https://www.youtube.com/youtubei/v1/search?${encodeParams(mapOf("query" to term, "key" to "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8", "contentCheckOk" to true, "racyCheckOk" to true))}"
        val query: MutableMap<String, Any> = mutableMapOf("context" to mutableMapOf("client" to mutableMapOf("clientName" to "WEB", "clientVersion" to "2.20200720.00.02")))
        if (conti != null) {
            query.put("continuation", conti)
        }
        val baseHeaders = mapOf("User-Agent" to "Mozilla/5.0", "accept-language" to "en-US,en", "Content-Type" to "application/json")
        val data = JSONObject(query as Map<*, *>).toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(queryUrl)
            .post(data)

            .build()

        val client = OkHttpClient()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw RuntimeException("Failed : HTTP error code : ${response.code}")
        }

        val jsonResponse = response.body.string()
        val json = JSONObject(jsonResponse)
        val (videos, continuation) = praser(json)

        val suggestions= mutableListOf<String>()
        if (json.has("refinements")){
            val sug=json.getJSONArray("refinements")
            for (su in 0 until sug.length()) {
                suggestions.add(0,sug.getString(su))
            }
        }

        return Triple(videos, continuation,suggestions)

    }
    private fun encodeParams(params: Map<String, Any>): String {
        return params.entries.joinToString("&") { "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value.toString(), "UTF-8")}" }
    }
    fun praser(resp: JSONObject): Pair<MutableList<JSONObject>, String?> {
        val rawResults = resp
        val sections = try {
            rawResults.getJSONObject("contents").getJSONObject("twoColumnSearchResultsRenderer")
                .getJSONObject("primaryContents").getJSONObject("sectionListRenderer").getJSONArray("contents")
        } catch (e: Exception) {
            rawResults.getJSONArray("onResponseReceivedCommands").getJSONObject(0)
                .getJSONObject("appendContinuationItemsAction").getJSONArray("continuationItems")
        }
        /*[hideBottomSeparator, trackingParams, subMenu, targetId, contents]====sectionListRendere*/
        val items=sections.getJSONObject(0).getJSONObject("itemSectionRenderer").getJSONArray("contents")


        var continuationRenderer: JSONObject? =null
        if (items.length()>1){
            continuationRenderer=sections.getJSONObject(1)
        }
        val itemsVideo = mutableListOf<JSONObject>()
        for (i in 0 until items.length()) {
            val s = items.getJSONObject(i)
            /*if (s.has("showingResultsForRenderer")){
                println(s)
            }
            println(s.keys())*/
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
                itemsVideo.add(ite)
            }
            if (s.has("reelShelfRenderer")){
                val reels=s.getJSONObject("reelShelfRenderer").getJSONArray("items")
                for (reel in 0 until reels.length()) {
                    val ite=JSONObject()
                    val rl=reels.getJSONObject(reel)
                    ite.put("videoId",rl.getJSONObject("reelItemRenderer").getString("videoId"))
                    ite.put("title",txt2filename(rl.getJSONObject("reelItemRenderer").getJSONObject("headline").getString("simpleText")))
                    ite.put("thumbnail",rl.getJSONObject("reelItemRenderer").getJSONObject("thumbnail").getJSONArray("thumbnails").getJSONObject(0).getString("url"))
                    ite.put("duration","Shorts")
                    itemsVideo.add(ite)
                }
            }
            if (s.has("shelfRenderer")){
               val vitems=s.getJSONObject("shelfRenderer").getJSONObject("content")
                if (vitems.has("verticalListRenderer")){
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

            }
          /*  if (s.has("horizontalCardListRenderer")){
                println(s)
            }*/

        }

        val nextContinuation = if (continuationRenderer != null) {
            continuationRenderer.getJSONObject("continuationItemRenderer").getJSONObject("continuationEndpoint").getJSONObject("continuationCommand").getString("token")
        } else {
            null
        }

        val uniqueItemsVideo: MutableList<JSONObject> = itemsVideo.distinctBy { it.get("videoId") }.toMutableList()

        return Pair(uniqueItemsVideo, nextContinuation)

    }
    fun makeValidFilename(input: String): String {
        val validFilename = input.replace(Regex("[^a-zA-Z0-9._-]"), "")
        val maxLength = 250
        return if (validFilename.length > maxLength) {
            validFilename.substring(0, maxLength)
        } else {
            validFilename
        }
    }
    fun formatSpeed(speed: Long): String {
        return when {
            speed > 1e9 -> String.format("%.2f GB", speed / 1e9)
            speed > 1e6 -> String.format("%.2f MB", speed / 1e6)
            speed > 1e3 -> String.format("%.2f KB", speed / 1e3)
            else -> String.format("%.2f B", speed)
        }
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
    data class RequestVariant(
        val url: String? = null,
        val data: JSONObject,
        val query: Map<String, String>,
        val headers: Map<String, String>
    )

    suspend fun getJson(videoId: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            val baseApiUrl = "https://www.youtube.com/youtubei/v1/player"
            val commonHeaders = mapOf(
                "Origin" to "https://www.youtube.com",
                "content-type" to "application/json"
            )
            val commonQueryParams = mapOf("prettyPrint" to "false")
            val commonData = JSONObject()
            commonData.put("contentCheckOk", true)
            commonData.put("racyCheckOk", true)
            val variants = listOf(RequestVariant(
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
                        put("videoId", videoId)
                        put("playbackContext", JSONObject(mapOf("contentPlaybackContext" to mapOf("html5Preference" to "HTML5_PREF_WANTS"))))
                    },
                    headers = commonHeaders + mapOf(
                        "X-YouTube-Client-Name" to "55",
                        "X-YouTube-Client-Version" to "17.31.35"
                    ),
                    query = commonQueryParams+ mapOf("key" to "AIzaSyCjc_pVEDi4qsv5MtC2dMXzpIaDoRFLsxw")
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
                        put("videoId", videoId)
                        put("playbackContext", JSONObject(mapOf("contentPlaybackContext" to mapOf("html5Preference" to "HTML5_PREF_WANTS","signatureTimestamp" to  19788))))
                    },
                    headers = commonHeaders + mapOf(
                        "X-YouTube-Client-Name" to "85",
                        "X-YouTube-Client-Version" to "2.0"
                    ),
                    query = commonQueryParams+ mapOf("key" to "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8")
                ),RequestVariant(
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
                        put("videoId", videoId)
                        put("playbackContext", JSONObject(mapOf("contentPlaybackContext" to mapOf("html5Preference" to "HTML5_PREF_WANTS"))))
                    },
                    headers = commonHeaders + mapOf(
                        "X-YouTube-Client-Name" to "14",
                        "X-YouTube-Client-Version" to "22.30.100"
                    ),
                    query = commonQueryParams+ mapOf("key" to "AIzaSyD_qjV8zaaUMehtLkrKFgVeSX_Iqbtyws8")
                ),RequestVariant(
                    data = JSONObject().apply {
                        put("context", JSONObject(mapOf(
                            "client" to mapOf(
                                "clientName" to "WEB_CREATOR",
                                "clientVersion" to "1.20220726.00.00",
                                "hl" to "en",
                                "timeZone" to "UTC",
                                "utcOffsetMinutes" to 0
                            )
                        )))
                        put("videoId", videoId)
                        put("playbackContext", JSONObject(mapOf("contentPlaybackContext" to mapOf("html5Preference" to "HTML5_PREF_WANTS"))))
                    },
                    headers = commonHeaders + mapOf(
                        "X-YouTube-Client-Name" to "62",
                        "X-YouTube-Client-Version" to "1.20220726.00.00"
                    ),
                    query = commonQueryParams+ mapOf("key" to "AIzaSyBUPetSUmoZL-OhlxA7wSac5XinrygCqMo")
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
                        put("videoId", videoId)
                        put("playbackContext", JSONObject(mapOf("contentPlaybackContext" to mapOf("html5Preference" to "HTML5_PREF_WANTS","signatureTimestamp" to  19788))))
                    },
                    headers = commonHeaders + mapOf(
                        "X-YouTube-Client-Name" to "2",
                        "X-YouTube-Client-Version" to "2.20220801.00.00"
                    ),
                    query = commonQueryParams+ mapOf("key" to "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8")
                ), RequestVariant(
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
                        put("videoId", videoId)
                        put("playbackContext", JSONObject(mapOf("contentPlaybackContext" to mapOf("html5Preference" to "HTML5_PREF_WANTS"))))
                        put("contentCheckOk", true)
                        put("racyCheckOk", true)
                    },
                    headers = commonHeaders + mapOf(
                        "X-YouTube-Client-Name" to "15",
                        "X-YouTube-Client-Version" to "22.33.101",
                        "userAgent" to "com.google.ios.ytcreator/22.33.101 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)"
                    ),
                    query = commonQueryParams+ mapOf("key" to "AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8")
                ),
                RequestVariant(
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
                        put("videoId", videoId)
                        put("playbackContext", JSONObject(mapOf("contentPlaybackContext" to mapOf("html5Preference" to "HTML5_PREF_WANTS"))))
                    },
                    headers = commonHeaders + mapOf(
                        "X-YouTube-Client-Name" to "66",
                        "X-YouTube-Client-Version" to "17.33.2",
                        "userAgent" to "com.google.ios.youtube/17.33.2 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)"
                    ),
                    query = commonQueryParams+ mapOf("key" to "AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8")
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
                        put("videoId", videoId)
                        put("playbackContext", JSONObject(mapOf("contentPlaybackContext" to mapOf("html5Preference" to "HTML5_PREF_WANTS"))))
                        put("contentCheckOk", true)
                        put("racyCheckOk", true)
                    },
                    headers = commonHeaders + mapOf(
                        "X-YouTube-Client-Name" to "5",
                        "X-YouTube-Client-Version" to "17.33.2",
                        "userAgent" to "com.google.ios.youtube/17.33.2 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)",
                    ),
                    query = commonQueryParams+ mapOf("key" to "AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8")
                ), RequestVariant(
                    data = JSONObject().apply {
                        put("context", JSONObject(mapOf(
                            "client" to mapOf(
                                "clientName" to "WEB_REMIX",
                                "clientVersion" to "1.20220727.01.00"
                            )
                        )))
                    },
                    headers =mapOf( "Content-Type" to "application/json",
                        "User-Agent" to "Mozilla/5.0",
                    ),
                    query =mapOf("videoId" to videoId,"key" to "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8","contentCheckOk" to "true","racyCheckOk" to "true")
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
                    headers =mapOf( "Content-Type" to "application/json",
                        "User-Agent" to "com.google.android.apps.youtube.music/",
                    ),
                    query =mapOf("videoId" to videoId,"key" to "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8","contentCheckOk" to "true","racyCheckOk" to "true")
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
                    headers =mapOf( "Content-Type" to "application/json",
                        "User-Agent" to "com.google.ios.youtubemusic/",
                    ),
                    query =mapOf("videoId" to videoId,"key" to "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8","contentCheckOk" to "true","racyCheckOk" to "true")
                )
            )
            for(variantk in variants){
                val variant=variants[0]
                val client = OkHttpClient()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = variant.data.toString().toRequestBody(mediaType)
                val urlWithQuery = StringBuilder(variant.url ?: baseApiUrl)
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
                    .post(requestBody)
                    .build()
                val response = client.newCall(request).execute()
                response.body.use { responseBody ->
                    val jsonResponse = JSONObject(responseBody.string())
                    if (jsonResponse.has("streamingData")) {
                        response.close()
                        return@withContext jsonResponse
                    }
                }
                if (!response.isSuccessful){
                    response.close()
                }

            }
            null
        }
    }
}