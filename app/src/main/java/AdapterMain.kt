package com.seize

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class AdapterMain(private val context: Context, private val modelList: List<JSONObject>, private val clickListener: OnItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), View.OnClickListener {
    private val coroutineToGetJson = CoroutineScope(Dispatchers.IO)
    interface OnItemClickListener {
        fun onItemClick(position: Int, item: JSONObject)
        fun onImageViewClick(position: Int, item: JSONObject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.suggestions_layout, parent, false)
                Suggestions(view)
            }
            2-> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.results_layout, parent, false)
               Results(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType==1){
          val holderView=holder as Suggestions
          holderView.suggestionTitle.text=modelList[position].getString("suggestion")
        }

        if (holder.itemViewType==2){
            val item=modelList[position]
            val  holderView=holder as Results
            if (item.has("title")){
                holderView.text.text = item.getString("title")
                val videoId=item.getString("videoId")
                holderView.progress.text = item.getString("duration")
                val folder = File("${context.filesDir}/thumbnail")
                val f = File(folder, "$videoId.jpg")
                coroutineToGetJson.launch {
                    val thumbnail=getThumbnailFile(item.getString("thumbnail"),f.absolutePath)
                    withContext(Dispatchers.Main){
                        val ml= thumbnail?.let { File(it) }
                        if (ml != null) {
                            if (ml.exists()){
                                val bitMapImage = BitmapFactory.decodeFile(thumbnail)
                                holderView.images.setImageBitmap(bitMapImage)
                            }else{
                                println("not found")
                                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_foreground)
                                holderView.images.setImageBitmap(bitmap)
                            }
                        }
                        /*val bitMapImage = BitmapFactory.decodeFile(thumbnail)
                        holderView.images.setImageBitmap(bitMapImage)*/
                    }
                }
            }
        }

    }
    private fun getThumbnailFile(url: String,f:String): String? {
        val file=File(f)
        if (file.exists()){
            return f
        }
        try {
            val d_url = URL(url)
            val bis = BufferedInputStream(d_url.openStream())
            var count = 0
            val b = ByteArray(1024)
            val fos = FileOutputStream(file)
            while (bis.read(b).also { count = it } != -1) {
                fos.write(b, 0, count)
            }
            fos.close()
            return file.absolutePath
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun getItemCount(): Int {
        return modelList.size
    }
    override fun getItemViewType(position: Int): Int {
        val item = modelList[position]
        return when {
            item.has("suggestion") -> 1
            item.has("result") -> 2
            else -> throw IllegalArgumentException("Invalid item type")
        }

    }
    inner class Results(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var images: ImageView = itemView.findViewById(R.id.imagvsearchitem)
        var text: TextView = itemView.findViewById(R.id.textView_listItemsearchitem)
        var cardView: LinearLayout = itemView.findViewById(R.id.cardViewforsearchtem)
        var progress: TextView = itemView.findViewById(R.id.textView_progresssearchitem)
        var downloadb:ImageView=itemView.findViewById(R.id.downloadb)

        init {
            cardView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val clickedItem = modelList[position]
                    clickListener.onItemClick(position, clickedItem)
                }
            }
            downloadb.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val clickedItem = modelList[position]
                    clickListener.onImageViewClick(position, clickedItem)
                }
            }
        }

    }
    inner class Suggestions(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var suggestionTitle:TextView
        init {
            suggestionTitle=itemView.findViewById(R.id.textView)
            suggestionTitle.setOnClickListener{
                val position=adapterPosition
                clickListener.onItemClick(position,modelList[position])
            }
        }

    }
    override fun onClick(v: View?) {
        //
    }
}
