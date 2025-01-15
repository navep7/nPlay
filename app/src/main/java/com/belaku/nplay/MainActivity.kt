package com.belaku.nplay

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Messenger
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.belaku.nplay.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL


class MainActivity : AppCompatActivity(), MusicAdapter.RecyclerViewEvent {

    private var noOfSearch: Int = 0
    private var arraylistArtists = ArrayList<String>()
    private var gotDuration: Boolean = false
    private lateinit var updateUIReciver: BroadcastReceiver
    private lateinit var handlerSongInfo: Handler
    private lateinit var handlerSeekInfo: Handler
    private lateinit var btnPlay: ImageButton
    private lateinit var btnPlayPause: ImageButton
    private lateinit var playIntent: Intent
    private lateinit var bitmapAlbum: Bitmap
    private var songs: ArrayList<String> = ArrayList()
    private lateinit var handlerForBG: Handler
    private var songIndex: Int = 0
    private lateinit var player: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var image: BitmapDrawable

    companion object {
        lateinit var dataList: List<Data>
        lateinit var dataList1: List<Data>
        lateinit var dataList2: List<Data>
    }
    private lateinit var query: String
    private lateinit var editTextQuery: EditText
    private lateinit var recyclerview : RecyclerView
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var playerBg: CoordinatorLayout


    var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //do something based on the intent's action
            // UPDATE YOUR UI FROM HERE
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playIntent = Intent(
            this,
            MusicService::class.java
        )

        arraylistArtists.add("Linkin Park")
        arraylistArtists.add("Coldplay")
        arraylistArtists.add("Sanjith Hegde")

        handlerForBG = Handler()

        seekBar = findViewById(R.id.seekbar)
        playerBg = findViewById(R.id.player_bg)
        editTextQuery = findViewById(R.id.edtx_query)
        editTextQuery.setInputType(InputType.TYPE_CLASS_TEXT)
        recyclerview = findViewById(R.id.rv)
        btnPlay = findViewById(R.id.imgbtn_play)
        btnPlayPause = findViewById(R.id.imgbtn_playpause)

        if (isMyServiceRunning(MusicService::class.java)) {
            btnPlay.setImageResource(android.R.drawable.ic_media_pause)
        }


        query = "Linkin Park"
        editTextQuery.setText(query)
        Getdata()

        editTextQuery.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                Toast.makeText(this@MainActivity, editTextQuery.getText(), Toast.LENGTH_SHORT).show()
                query = editTextQuery.getText().toString()
                Getdata()
                handled = true
            }
            handled
        })

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }

        btnPlayPause.setOnClickListener(View.OnClickListener {

        })


        btnPlay.setOnClickListener(View.OnClickListener {

            if(!isMyServiceRunning(MusicService::class.java)) {
                btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                seekBar.thumb = resources.getDrawable(android.R.drawable.ic_media_pause)

                var i: Int = 0;
                for (item in songs) {
                    playIntent.putExtra(i.toString(), item)
                    i++
                }

                 handlerSongInfo = object : Handler() {
                    override fun handleMessage(msg: Message) {
                        updateUI(msg.what)
                    }
                }

                handlerSeekInfo = object : Handler() {
                    override fun handleMessage(msg: Message) {
                        updateSeek(msg.what)
                    }
                }

                playIntent.putExtra("songInfo", Messenger(handlerSongInfo));
                playIntent.putExtra("seekInfo", Messenger(handlerSeekInfo))

            //    updateUI(0)
                startForegroundService(playIntent)

            } else {
                btnPlay.setImageResource(android.R.drawable.ic_media_play)
                seekBar.thumb = resources.getDrawable(android.R.drawable.ic_media_play)
                val myService = Intent(
                    this@MainActivity,
                    MusicService::class.java
                )
                stopService(myService)
            }

        })




    }

    override fun onDestroy() {
        super.onDestroy()
     }

    private fun updateSeek(what: Int) {

        if (!gotDuration) {
            gotDuration = true
            seekBar.max = what
        } else {
            seekBar.setProgress(what, true)
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
      //  if (isMyServiceRunning(MusicService::class.java))
         //   Toast.makeText(applicationContext, recyclerview.focusedChild.toString(), Toast.LENGTH_LONG).show()
    }


    private fun updateUI(what: Int) {

        recyclerview.scrollToPosition(what)
        findViewById<TextView>(R.id.tx_songname).text = dataList[what].title
        val thread = Thread {
            try {
                // Your code goes here
                val url = URL(dataList[what].album.cover)
                bitmapAlbum = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                image = BitmapDrawable(applicationContext.getResources(), bitmapAlbum)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Log.d("updateUI exception - ", e.toString())
            }
        }

        thread.start()


        handlerForBG.postDelayed(Runnable {  playerBg.background = image }, 1000)
    }


    private fun Getdata() {
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl("https://deezerdevs-deezer.p.rapidapi.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)

        val retrofitData = retrofitBuilder.getDate(query)

        retrofitData.enqueue(object : Callback<MusicData?> {
            override fun onResponse(
                call: Call<MusicData?>,
                response: Response<MusicData?>
            ) {
                dataList = response.body()?.data!!


                songs.clear()
                for (item in dataList)
                    songs.add(item.preview)

                for (item in dataList)
                    Log.d("DATA7", "p - " + item.preview + "\n l - " + item.link)


                var rvAdapter = MusicAdapter(dataList, this@MainActivity)
                recyclerview.adapter = rvAdapter
            //    recyclerview.layoutManager = LinearLayoutManager(this@MainActivity)
                recyclerview.setLayoutManager(
                    LinearLayoutManager(
                        this@MainActivity,
                        LinearLayoutManager.HORIZONTAL,false
                    )
                )

            }

            override fun onFailure(call: Call<MusicData?>, t: Throwable) {
                Toast.makeText(applicationContext, "not Found", Toast.LENGTH_LONG).show()
            }

        })

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override
    fun onItemClick(position: Int) {
        val sData = dataList[position]

        try {
            findViewById<TextView>(R.id.tx_songname).text = dataList[songIndex].title
            val uri = Uri.parse(dataList.get(songIndex++).preview)
            player = MediaPlayer()
            player.setAudioStreamType(AudioManager.STREAM_MUSIC)
            player.setDataSource(this, uri)
            player.prepare()
            player.start()
        } catch (e: Exception) {
            println(e.toString())
            Toast.makeText(applicationContext, "P ex - " + e, Toast.LENGTH_LONG).show()
        }

      //  player.setOnCompletionListener(this)

        val thread = Thread {
            try {
                // Your code goes here
                val url = URL(sData.album.cover)
                val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                image = BitmapDrawable(applicationContext.getResources(), bitmap)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_LONG).show()
            }
        }

        thread.start()


        handlerForBG.postDelayed(Runnable {  playerBg.background = image }, 1000)


        Toast.makeText(
            this,
            sData.title,
            Toast.LENGTH_SHORT
        ).show()
    }





}