package com.belaku.nplay

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone


class MusicAdapter(
    private val data: List<Data>,
    private val listener: RecyclerViewEvent
) : RecyclerView.Adapter<MusicAdapter.ItemViewHolder>() {

    //Setup variables to hold the instance of the views defined in your recyclerView item layout
    //Kinda like the onCreate method in an Activity
    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener,
        View.OnLongClickListener {
        val sname: TextView = view.findViewById(R.id.tx_sname)
        val aname: TextView = view.findViewById(R.id.tx_aname)
        val dur: TextView = view.findViewById(R.id.tx_duration)
        val imageView: ImageView = view.findViewById(R.id.imgv_art)

        init {
            view.setOnClickListener(this)
            view.setOnLongClickListener(this)
        }

        override fun onClick(p0: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(position)
            }
        }

        override fun onLongClick(p0: View?): Boolean {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemLongClick(position)
            }
            return true
        }
    }

    //This is where you inflate the layout (Give each entry/row its look)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflatedView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.rv_item, parent, false)
        return ItemViewHolder(inflatedView)
    }

    // Set values to the views we pulled out of the recycler_view_row
    // layout file based on the position of the recyclerView
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val songdata: Data = data[position]

        holder.sname.text = songdata.title
        holder.aname.text = songdata.album.title
        /*val d: Date = Date(songdata.duration * 1000L)
        val df: SimpleDateFormat = SimpleDateFormat("mm:ss") // HH for 0-23
        df.setTimeZone(TimeZone.getTimeZone("GMT"))
        val time: kotlin.String = df.format(d)
        holder.dur.text = time*/
        Picasso.get().load(songdata.album.cover).into(holder.imageView)
    }


    //The recyclerView just wants to know how many items are currently in your dataset
    override fun getItemCount(): Int {
        return data.size
    }

    interface RecyclerViewEvent{
        fun onItemClick(position: Int)
        fun onItemLongClick(position: Int)
    }
}