package com.seize

import Youtubep
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream


class DownloadsActivity : AppCompatActivity(),Adapter.OnItemClickListener {

    var adapter:Adapter?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)
        val recyclerView = findViewById<RecyclerView>(R.id.downloads_recycle)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        adapter=Adapter(this,modelList,this)
        recyclerView.adapter=adapter
        adapter!!.notifyDataSetChanged()
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                for (i in modelList) {
                    val previousProgerss=i.inRam-i.progressInPreviousSecond
                    i.progressInPreviousSecond=i.inRam
                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = i.start_time?.let { currentTime - it } ?: 0
                    val total =i.onWeb+i.onDisk
                    val suffix = i.suffix
                    val speed = if (elapsedTime > 0) (i.inRam / elapsedTime * 1000) else 0
                    val eta = if (speed > 0) ((i.onWeb - i.inRam) / speed) else 0
                    val progress = "${formatSpeed(i.onDisk + i.inRam)} / ${formatSpeed(total)}"
                    val etaText = secondsToDHMS(eta)
                    val downloaded=i.inRam+i.onDisk
                    val percent = if (total > 0) ((downloaded.toFloat() / total) * 100).toInt() else 0
                    val progressText: String = if (previousProgerss>0){
                        "$progress   $percent%  ${formatSpeed(speed)}/s  $etaText  $suffix"
                    }else{
                        "$progress   $percent%   0/s  \u221E s  $suffix"
                    }
                    val holder: RecyclerView.ViewHolder? =
                        recyclerView.findViewHolderForAdapterPosition(modelList.indexOf(i))
                    if (holder != null) {
                        val pgt = holder.itemView.findViewById<TextView>(R.id.textView_progress)
                        if (percent == 100) {
                            if (suffix != null) {
                                if (suffix.contains("Merging")){
                                    pgt.text=suffix
                                }
                                else{
                                    pgt.text = "$progress   $percent%   Finished  $suffix"
                                }
                            }

                        } else {
                            pgt.text = progressText
                        }
                    }

                }
                handler.postDelayed(this, 1000)
            }
        }, 1000)

    }
    fun formatSpeed(speedT: Long): String {
        val speed=speedT.toDouble()
        return when {
            speed > 1e9 -> String.format("%.2f GB", speed / 1e9)
            speed > 1e6 -> String.format("%.2f MB", speed / 1e6)
            speed > 1e3 -> String.format("%.2f KB", speed / 1e3)
            else -> String.format("%.2f B", speed)
        }
    }
    fun secondsToDHMS(totalSeconds: Long): String {
        val seconds = totalSeconds % 60
        val totalMinutes = totalSeconds / 60
        val totalHours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val days = totalHours / 24
        val hours = totalHours % 24

        return when (days) {
            0L -> {
                when (hours) {
                    0L -> {
                        when (minutes) {
                            0L -> "$seconds s"
                            else -> "$minutes m $seconds s"
                        }
                    }
                    else -> "$hours h $minutes m $seconds s"
                }
            }
            else -> "$days d $hours h $minutes m $seconds s"
        }
    }

    companion object {
        fun secondsToDHMS(totalSeconds:Double): String {
            val seconds = totalSeconds % 60
            val totalMinutes = totalSeconds / 60
            val totalHours = totalMinutes / 60
            val minutes = totalMinutes % 60
            val days = totalHours / 24
            val hours = totalHours % 24

            return when (days) {
                0.0 -> {
                    when (hours) {
                        0.0 -> {
                            when (minutes) {
                                0.0 -> "$seconds s"
                                else -> "$minutes m $seconds s"
                            }
                        }
                        else -> "$hours h $minutes m $seconds s"
                    }
                }
                else -> "$days d $hours h $minutes m $seconds s"
            }
        }
        private val coroutToDownload = CoroutineScope(Dispatchers.IO + SupervisorJob())
        var modelList= arrayListOf<model>()
        val MAX_SAMPLE_SIZE = 256 * 1024
        fun myStaticFunction(context: Context, items:MutableList<JSONObject>) {
            for(item in items) {
                if (item.has("indexRange")) {
                    if (item.getString("mimeType").contains("audio/mp4")) {
                        val ts = item.getString("contentLength").toLong()
                        val model = model()
                        model.title = item.getString("title")
                        model.thumbnail = item.getString("thumbnail")
                        model.suffix = "Audio"
                        var audioFos: FileOutputStream? = null
                        var mks:String?=null
                        val final= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                        val fileName=File(final,model.title+".mp3")
                        val nmt= "YoutubeVideoId"+scrapper.txt2filename(item.getString("videoId"))
                        val fileComman=File(final,"$nmt.mp3")
                        if (fileComman.exists()){
                            audioFos= FileOutputStream(fileComman,true)
                            model.onDisk=fileComman.length()
                            model.onWeb=ts-model.onDisk
                            mks=fileComman.absolutePath
                        }
                        if (fileName.exists()){
                            audioFos= FileOutputStream(fileName,true)
                            model.onDisk=fileName.length()
                            model.onWeb=ts-model.onDisk
                            mks=fileName.absolutePath
                        }else{
                            try {
                                audioFos=FileOutputStream(fileName)
                                model.onDisk=0
                                model.onWeb=ts
                                mks=fileName.absolutePath
                            }catch (e:Exception){
                                audioFos= FileOutputStream(fileComman)
                                model.onDisk=0
                                model.onWeb=ts
                                mks=fileComman.absolutePath
                            }
                        }
                        coroutToDownload.launch {
                            model.start_time=System.currentTimeMillis()
                            modelList.add(model)
                            partDownloader(item.getString("url"), audioFos!!,model)
                            MediaScannerConnection.scanFile(
                                context, arrayOf(mks),null
                            ) { path, uri -> println("scanned") }
                            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.parse(mks)))
                            model.fileName=mks
                        }

                    } else {
                            val ts = item.getString("contentLength").toLong()
                            val model = model()
                            model.title = item.getString("title")+item.getString("qualityLabel")
                            model.thumbnail = item.getString("thumbnail")
                            model.suffix = "Video"
                            var videoFos: FileOutputStream? = null
                            val videoFile =File("${context.filesDir}/${item.getString("videoId")}${item.getString("qualityLabel")}temp.mp4")
                            if (videoFile.exists()) {
                                model.onDisk = videoFile.length()
                                videoFos = FileOutputStream(videoFile, true)
                                model.onWeb = ts - videoFile.length()
                            } else {
                                videoFos = FileOutputStream(videoFile)
                                model.onWeb = ts
                            }
                            var audioFos: FileOutputStream?=null
                            val audioFile = File("${context.filesDir}/${item.getString("videoId")}temp.mp3")
                            modelList.add(model)
                            coroutToDownload.launch {
                                model.start_time = System.currentTimeMillis()
                                partDownloader(item.getString("url"), videoFos, model)
                                model.suffix = "Audio"
                                model.inRam=0
                                model.onWeb=0
                                model.onDisk=0
                                model.progressInPreviousSecond=0
                                if (audioFile.exists()) {
                                    println("exists")
                                    model.onDisk = audioFile.length()
                                    audioFos = FileOutputStream(audioFile, true)
                                    model.onWeb = item.getLong("audiolength") - audioFile.length()
                                } else {
                                    audioFos = FileOutputStream(audioFile)
                                    model.onWeb = item.getLong("audiolength")
                                }
                                println("Downloading audio")
                                partDownloader(item.getString("audio"), audioFos!!, model)
                                model.suffix = "Merging"
                                println("merging")
                                val final= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                                var finalFile:String?=null
                                val file_name=File(final.absoluteFile.toString()+"/"+model.title+"(${item.getString("qualityLabel")}).mp4")
                                if (file_name.exists()){
                                    file_name.delete()
                                }
                                try {
                                    if (file_name.createNewFile()){
                                        finalFile=file_name.absolutePath
                                    }
                                }catch (e:Exception){
                                    val nmt= "YoutubeVideoId"+scrapper.txt2filename(item.getString("videoId"))
                                    finalFile=final.absoluteFile.toString()+"/$nmt(${item.getString("qualityLabel")}).mp4"
                                }
                                try {
                                    println(videoFile.absolutePath)
                                    println(audioFile.absolutePath)
                                    muxUsingFfmpeg(videoFile.absolutePath, audioFile.absolutePath, finalFile!!, model,
                                        onSuccess = { success ->
                                            if (success) {
                                                videoFile.delete()
                                                audioFile.delete()
                                                model.suffix="Video"
                                            } else {
                                                println("Muxing failed")
                                            }
                                        },
                                        onFailure = { errorMessage ->
                                            println("Error: $errorMessage")
                                        }
                                    )
                                    MediaScannerConnection.scanFile(
                                        context, arrayOf(finalFile),null
                                    ) { path, uri -> println("scanned") }
                                    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                        Uri.parse(finalFile)))
                                    model.suffix=""
                                    model.fileName=finalFile
                                }catch (e:Exception){
                                    println(e)
                                    model.suffix="Merging failed"
                                }
                            }
                    }
                } else {
                    if (item.getString("mimeType").contains("video/mp4")) {
                        val folder =
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                        val mModel = model()
                        mModel.title = item.getString("title")+item.getString("qualityLabel")
                        mModel.thumbnail = item.getString("thumbnail")
                        mModel.suffix = item.getString("qualityLabel")
                        val fileName =File( folder.absolutePath + "/" + item.getString("title") + "(${
                            item.getString("qualityLabel")
                        }).mp4")
                        modelList.add(mModel)
                        var mks:String?=null
                        var fos:FileOutputStream?=null
                        val nmt= "YoutubeVideoId"+scrapper.txt2filename(item.getString("videoId"))
                        val fileComman=File(folder,"$nmt.mp4")
                        if (fileComman.exists()){
                            mModel.onDisk=fileComman.length()
                            mks=fileComman.absolutePath
                            fos= FileOutputStream(fileComman,true)
                        }
                        if (fileName.exists()){
                            mModel.onDisk=fileName.length()
                            mks=fileName.absolutePath
                            fos= FileOutputStream(fileName,true)
                        }else{
                            try {
                                fileName.createNewFile()
                                mks=fileName.absolutePath
                                fos= FileOutputStream(fileName)
                            }catch (e:Exception){
                                fos= FileOutputStream(fileComman)
                                mks=fileComman.absolutePath
                            }
                        }
                        mModel.fileName=mks
                        coroutToDownload.launch {
                            println(mks)
                            println(" regiteredfilename${mModel.fileName}")
                            progressiveDownloader(mModel, item.getString("url"), fos!!)
                            MediaScannerConnection.scanFile(context, arrayOf(mModel.fileName),null
                            ) { path, uri -> println("scanned") }
                            context.sendBroadcast(
                                Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                    Uri.parse(mModel.fileName))
                            )

                        }
                    }
                }
            }
        }
        private fun partDownloader(videoUrl: String,fos:FileOutputStream,model: model){
            val client = OkHttpClient()
            val endByte = model.onDisk+ minOf(
                (model.onDisk + model.inRam.plus(9437184))
                    ?: model.onWeb
            )
            val request = videoUrl.let {
                Request.Builder()
                    .url(it)
                    .addHeader("Range", "bytes=${model.onDisk+model.inRam}-$endByte")
                    .build()
            }
            try {
                val response: Response = request.let { client.newCall(it).execute() }
                response.body.byteStream().use { inputStream ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                        model.inRam= model.inRam.plus(bytesRead)
                    }
                }
            } catch (e: IOException) {
                println(e)
            }
            if (model.onDisk+ model.inRam < model.onDisk+ model.onWeb){
                partDownloader(videoUrl,fos,model)
            }else{
                fos.close()

            }

        }
        private fun progressiveDownloader(model: model, videoUrl: String, fos: FileOutputStream){
            val client = OkHttpClient()
            val bfo=BufferedOutputStream(fos)
            val request = videoUrl.let {
                val builder = Request.Builder().url(it)
                model.onDisk.let { downloaded ->
                    if (downloaded > 0) {
                        builder.addHeader("Range", "bytes=$downloaded-")
                    }
                }

                builder.build()
            }
            try {
                val response: Response = request.let { client.newCall(it).execute() }
                model.onWeb= response.headers["Content-Length"]?.toLong()!!
                model.start_time=System.currentTimeMillis()
                response.body.byteStream().use { inputStream ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        bfo.write(buffer, 0, bytesRead)
                        model.inRam= model.inRam.plus(bytesRead)
                    }
                }
                fos.close()
                bfo.close()


            } catch (e: IOException) {
                println("Download failed")
            }

        }
        fun getDuration(videoFilePath: String): String {
            val cmd = mutableListOf(
                videoFilePath
            )
            val commandString = cmd.joinToString(" ")
            val session = FFprobeKit.execute(commandString)
            val output: String = session.output
            val durationRegex = Regex("Duration: (\\d\\d):(\\d\\d):(\\d\\d\\.\\d\\d),")
            val durationMatchResult = durationRegex.find(output)

            return durationMatchResult?.value ?: ""

        }
      /*  private fun muxUsingFfmpeg(
            video: String,
            audio: String,
            final: String,
            model: model,
            onSuccess: (Boolean) -> Unit,
            onFailure: (String) -> Unit
        ) {
            println(final)
            val cmd = mutableListOf(
                "-i", video,
                "-i", audio,
                "-c:v", "copy",
                "-y",
                "\"$final\""
            )
            val commandString = cmd.joinToString(" ")
            val duration= getDuration(video).replace("Duration:","").replace(",","")

            val session = FFmpegKit.executeAsync(commandString,
                { session ->
                    val returnCode = session.returnCode
                    if (returnCode?.value == 0) {
                        onSuccess(true)
                    } else {
                        onFailure("FFmpeg process exited with return code ${returnCode?.value}. ${session.failStackTrace}")
                    }
                },
                { log ->
                    val timeRegex = Regex("time=(\\d\\d:\\d\\d:\\d\\d\\.\\d\\d)")
                    val matchResult = timeRegex.find(log.message)
                    val progress = matchResult?.groups?.get(1)?.value
                    if (progress != null) {
                        val mg = "Merging $progress/${getDuration(video).replace("Duration:", "").replace(",", "")}"
                        model.suffix = mg
                    }
                },
                { _ -> }
            )
        }*/
         private fun muxUsingFfmpeg(video:String, audio:String, final:String,model:model,onSuccess: (Boolean) -> Unit,
                                    onFailure: (String) -> Unit)  {
             println(final)
            val cmd = mutableListOf(
                "-i", video,
                "-i", audio,
                "-c:v", "copy",
                "-y",
                "\"$final\""
            )
            val commandString = cmd.joinToString(" ")
            /* val mediaInformation = FFprobeKit.getMediaInformation(video)
             val info=mediaInformation.mediaInformation*/
             val duration= getDuration(video).replace("Duration:","").replace(",","")

         /* val probe= FFprobeKit.execute(commandString)
            val session = FFmpegKit.execute(commandString)*/
          val session = FFmpegKit.executeAsync(commandString,
              { session ->
                  val state = session.state
                  val returnCode = session.returnCode
                  if (returnCode?.value == 0) {
                     model.suffix="Video"
                      onSuccess(true)

                  } else {
                      println("FFmpeg process exited with state $state and return code ${returnCode?.value}.${session.failStackTrace}")
                  }
              },
              { log ->
                  println(log.message)
                  val timeRegex = Regex("time=(\\d\\d:\\d\\d:\\d\\d\\.\\d\\d)")
                  val matchResult = timeRegex.find(log.message)
                  val progress=matchResult?.groups?.get(1)?.value
                  if (progress!=null){
                      val mg="Merging $progress/${duration}"
                      model.suffix=mg
                  }
              },
              { _ ->

              }
          )
        }
        private val  scrapper=Youtubep()
        fun InstagramVideo(video:MutableList<JSONObject>,context: Context){
            for(v in video){
                if(v.getBoolean("is_video")){
                    val folder= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    val mModel=model()
                    mModel.title=v.getString("title")
                    mModel.thumbnail="instagram"
                    mModel.suffix="Video"
                    val fileName=folder.absolutePath+"/"+v.getString("title")+".mp4"
                    mModel.fileName=fileName
                    modelList.add(mModel)
                    var fos:FileOutputStream?=null
                    val file=File(fileName)
                    if (file.exists()){
                        fos= FileOutputStream(file,true)
                        mModel.onDisk=file.length()
                    }else{
                        fos=FileOutputStream(file)
                    }
                    coroutToDownload.launch {
                        progressiveDownloader(mModel, v.getString("video_url"), fos!!)
                        MediaScannerConnection.scanFile(context, arrayOf(fileName),null
                        ) { path, uri -> println("scanned") }
                        context.sendBroadcast(
                            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.parse(fileName))
                        )
                    }
                }else{
                    val folder= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val mModel=model()
                    mModel.title=v.getString("title")
                    mModel.thumbnail="instagram"
                    mModel.suffix="Video"
                    val fileName=folder.absolutePath+"/"+v.getString("title")+".jpg"
                    mModel.fileName=fileName
                    modelList.add(mModel)
                    var fos:FileOutputStream?=null
                    val file=File(fileName)
                    if (file.exists()){
                        fos= FileOutputStream(file,true)
                        mModel.onDisk=file.length()
                    }else{
                        fos=FileOutputStream(file)
                    }
                    coroutToDownload.launch {
                        progressiveDownloader(mModel, v.getString("display_url"), fos)
                        MediaScannerConnection.scanFile(context, arrayOf(fileName),null
                        ) { path, uri -> println("scanned") }
                        context.sendBroadcast(
                            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.parse(fileName))
                        )
                    }
                }
                
            }
        }

    }
    override fun onItemClick(position: Int, fileName: String) {
       if (fileName.contains(".mp4")){
           val fileUri = Uri.parse(fileName)
           val intent = Intent(Intent.ACTION_VIEW)
           intent.setDataAndType(fileUri, "video/*")
           intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

           try {
               startActivity(intent)
           } catch (e: Exception) {
               Toast.makeText(this,"NO apps found to open the file",Toast.LENGTH_SHORT).show()
           }
       }
        if (fileName.contains(".mp3")){
            val fileUri = Uri.parse(fileName)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(fileUri, "audio/*")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this,"NO apps found to open the file",Toast.LENGTH_SHORT).show()
            }

        }
        if (fileName.contains(".jpg")){
            val fileUri = Uri.parse(fileName)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(fileUri, "image/*")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this,"NO apps found to open the file",Toast.LENGTH_SHORT).show()
            }
        }
    }


}