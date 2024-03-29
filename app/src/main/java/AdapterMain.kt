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
import org.json.JSONObject
import java.io.File

class AdapterMain(private val context: Context, private val modelList: List<JSONObject>, private val clickListener: OnItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), View.OnClickListener {
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
            holderView.text.text = item.getString("title")
            holderView.progress.text = item.getString("duration")
            val thumbnail=File(item.getString("thumbnail"))
            if (thumbnail.exists()){
                val bitMapImage = BitmapFactory.decodeFile(item.getString("thumbnail"))
                holderView.images.setImageBitmap(bitMapImage)
            }else{
                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_foreground)
                holderView.images.setImageBitmap(bitmap)
            }

        }

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
