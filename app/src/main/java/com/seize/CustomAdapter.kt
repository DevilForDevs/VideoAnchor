
package com.seize
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL


class Adapter(var context: Context, var modelList: List<model>, private val clickListener: Adapter.OnItemClickListener) :
    RecyclerView.Adapter<Adapter.ViewHolder>() {
    interface OnItemClickListener {
        fun onItemClick(position: Int, fileName: String)
    }

    var holders: MutableList<LinearLayout> = ArrayList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val title = modelList[position].title
        val url = modelList[position].thumbnail
        holder.text.text= title
        holder.progress.text="Starting Download..."
        if (url!="instagram"){
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val d_url = URL(url)
                    val inputStream = d_url.openStream()

                    val bitMapImage = BitmapFactory.decodeStream(inputStream)

                    withContext(Dispatchers.Main) {
                        holder.images.setImageBitmap(bitMapImage)
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main){
                        holder.images.setImageDrawable(context.getDrawable(R.drawable.warning))
                    }
                }
            }
        }else{
           holder.images.setImageDrawable(context.getDrawable(R.drawable.intsta))
        }
        holders.add(holder.cardView)
    }

    override fun getItemCount(): Int {
        return modelList.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var images: ImageView
        var text: TextView
        var cardView: LinearLayout
        var progress: TextView
        init {
            images = view.findViewById(R.id.imagv)
            text = view.findViewById(R.id.textView_listItem)
            cardView = view.findViewById(R.id.cardView)
            progress=view.findViewById(R.id.textView_progress)
            cardView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val clickedItem = modelList[position]
                    clickedItem.fileName?.let { it1 -> clickListener.onItemClick(position, it1) }
                }
            }
        }
    }
}
