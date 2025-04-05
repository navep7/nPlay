package com.parvatha.music


import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.parvatha.music.MainActivity.Companion.appContext
import com.parvatha.music.MainActivity.Companion.mainActivity
import com.parvatha.music.MainActivity.Companion.makeToast
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone


class MusicAdapter(
    private val mActivity: Activity,
    private val data: List<Data>,
    private val listener: RecyclerViewEvent
) : RecyclerView.Adapter<MusicAdapter.ItemViewHolder>() {


    //Setup variables to hold the instance of the views defined in your recyclerView item layout
    //Kinda like the onCreate method in an Activity
    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val sname: TextView = view.findViewById(R.id.rv_tx_sname)
        val aname: TextView = view.findViewById(R.id.tx_aname)
        var rvItemLayout: RelativeLayout = view.findViewById(R.id.rv_item_layout)
        val imageView: ImageView = view.findViewById(R.id.imgv_art)
        val imageViewFavSong: ImageView = view.findViewById(R.id.imgv_fav_song)

        init {
            rvItemLayout.layoutParams = RelativeLayout.LayoutParams(
                MainActivity.displayMetrics.widthPixels - 135,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            view.setOnClickListener(this)

        }

        override fun onClick(p0: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(position)
            }
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
        val d: Date = Date(songdata.duration * 1000L)
        val df: SimpleDateFormat = SimpleDateFormat("mm:ss") // HH for 0-23
        df.setTimeZone(TimeZone.getTimeZone("GMT"))
        val time: kotlin.String = df.format(d)

        Picasso.get().load(songdata.album.cover).into(holder.imageView)

        holder.imageViewFavSong.setOnClickListener {
            showAddToDialog(holder.sname)
            //   (mActivity as MainActivity).addToFavoriteSongs(holder.sname.text.toString())
        }
    }

    private fun showAddToDialog(sname: TextView) {
        val dialog = Dialog(mainActivity)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.addto_layout)
        //    dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        var lvPlaylists: ListView = dialog.findViewById(R.id.lv_pls)
        var btnNewPl: Button = dialog.findViewById(R.id.btn_new_pl)
        var btnDone: Button = dialog.findViewById(R.id.btn_done)


        btnDone.setOnClickListener(View.OnClickListener {
            (mActivity as MainActivity).addToFavoriteSongs(sname.text.toString())
        })

        // here we adjust list elements choice mode
        lvPlaylists.setChoiceMode(ListView.CHOICE_MODE_SINGLE)

        // create adapter using array from resources file
        var arrayAdapter =
            ArrayAdapter(
                appContext,
                android.R.layout.simple_list_item_single_choice,
                MainActivity.arrayListplayLists
            )

        lvPlaylists.setOnItemClickListener { adapter, v, position, id ->
            val selItem = lvPlaylists.getItemAtPosition(position)
       //     val value = selItem.text
            makeToast("sPL - " + selItem)
            btnDone.isEnabled = true
        }

        lvPlaylists.setAdapter(arrayAdapter)



        dialog.show()
    }


    //The recyclerView just wants to know how many items are currently in your dataset
    override fun getItemCount(): Int {
        return data.size
    }

    interface RecyclerViewEvent {
        fun onItemClick(position: Int)
    }
}