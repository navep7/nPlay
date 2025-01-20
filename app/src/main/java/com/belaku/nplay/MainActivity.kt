package com.belaku.nplay

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.util.StatsLog.logEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.TextView.VISIBLE
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.belaku.nplay.MusicService.Companion.mediaPlayer
import com.belaku.nplay.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.lang.reflect.Type
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import kotlin.concurrent.fixedRateTimer
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity(), MusicAdapter.RecyclerViewEvent {

    private var gson: Gson = Gson()
    private lateinit var linearLayoutFavs: LinearLayout
    private lateinit var arrayListFavsAdded: ArrayList<String>
    private lateinit var mSharedPreference: SharedPreferences
    private lateinit var relativeLayoutMain: RelativeLayout
    private lateinit var editTextSearch: EditText
    private lateinit var recyclerview : RecyclerView


    private var playingSongIndex by Delegates.notNull<Int>()
    private var playingSeekDuration by Delegates.notNull<Int>()
    private var playingSeekUpdate by Delegates.notNull<Int>()


    private lateinit var mMessageReceiver: BroadcastReceiver
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var sharedPreferences: SharedPreferences
    private var arraylistFavorites = ArrayList<String>()

    private lateinit var playIntent: Intent
    private lateinit var bitmapAlbum: Bitmap
    private var songs: ArrayList<String> = ArrayList()
    private lateinit var handlerForBG: Handler
    private var songIndex: Int = 0
    private lateinit var image: BitmapDrawable

    @SuppressLint("StaticFieldLeak")
    companion object {
        lateinit var dataList: ArrayList<Data>
        lateinit var seekBar: SeekBar
        lateinit var wfs:WaveformSeekBar
        lateinit var txNow: TextView
        lateinit var txTotal: TextView

    }
    private lateinit var query: String
    private lateinit var binding: ActivityMainBinding





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

        findViewByIds()
        initializeStuff()

      /*  val thread = Thread {
            try {
                // Your code goes here
                Spotify()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        thread.start()*/

        mSharedPreference = PreferenceManager.getDefaultSharedPreferences(applicationContext)

         sharedPreferencesEditor = mSharedPreference.edit()

        arraylistFavorites = populateFavorites("favorites")
        for (item in arraylistFavorites) {
            val tx: TextView =  TextView(applicationContext)
            tx.text = item + "\t\t\t"
            tx.setBackgroundResource(android.R.drawable.editbox_background)
            tx.setOnClickListener(View.OnClickListener {
                wfs.progress = 0f
                if (isMyServiceRunning(MusicService::class.java)) {
                    stopService(Intent(this@MainActivity, MusicService::class.java))
                }
                query = tx.text.toString()
                Getdata()
            })
            linearLayoutFavs.addView(tx)
        }



        editTextSearch.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEND) {
           //     Toast.makeText(this@MainActivity, editTextSearch.getText(), Toast.LENGTH_SHORT).show()
                query = editTextSearch.getText().toString()
                Getdata()
                handled = true
            }
            handled
        })



        handlerForBG = Handler()





        binding.fab.setOnClickListener { view ->


                if(!isMyServiceRunning(MusicService::class.java)) {

                    if (songs.size > 0) {
                        binding.fab.setImageResource(android.R.drawable.ic_media_pause)
                        var i: Int = 0;
                        for (item in songs) {
                            playIntent.putExtra(i.toString(), item)
                            i++
                        }
                        startForegroundService(playIntent)
                    }
                } else {
                    if (mediaPlayer.isPlaying) {
                        binding.fab.setImageResource(android.R.drawable.ic_media_play)
                        mediaPlayer.pause()

                    } else {
                        binding.fab.setImageResource(android.R.drawable.ic_media_pause)
                        mediaPlayer.start()
                    }
                }


        }




      /*  btnPlay.setOnClickListener(View.OnClickListener {

            if(!isMyServiceRunning(MusicService::class.java)) {

                if (songs.size > 0) {
                    btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                    seekBar.thumb = resources.getDrawable(android.R.drawable.ic_media_pause)
                var i: Int = 0;
                    for (item in songs) {
                        playIntent.putExtra(i.toString(), item)
                        i++
                    }
                    startForegroundService(playIntent)
                }
            } else {
                btnPlay.setImageResource(android.R.drawable.ic_media_play)
                seekBar.thumb = resources.getDrawable(android.R.drawable.ic_media_play)
                val myService = Intent(
                    this@MainActivity,
                    MusicService::class.java
                )
                stopService(myService)
            }

        })*/

        //BR
        mMessageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Get extra data included in the Intent
                playingSongIndex = intent.getIntExtra("song_index", 0)
                playingSeekDuration = intent.getIntExtra("seek_duration", 0)
                playingSeekUpdate = intent.getIntExtra("seek_update", 0)

                updateUI(playingSongIndex)

                Log.d("BR21", "Got message: $playingSongIndex - $playingSeekUpdate")
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
             IntentFilter("nPlay_Events")
        );




    }

    private fun Spotify() {

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://spotify23.p.rapidapi.com/search/?q=eminem&type=tracks&offset=0&limit=10&numberOfTopResults=5 HTTP/1.1")
            .get()
            .addHeader("x-rapidapi-key", "9e92cc4f67msh8bb4ede93f53bf7p1ecb22jsn26ea5014a6df")
            .addHeader("x-rapidapi-host", "spotify23.p.rapidapi.com")
            .build()

        val response = client.newCall(request).execute()

        response.body()?.let { Log.d("Spotify Response - ", it.string()) }
    }

    private fun initializeStuff() {
        arrayListFavsAdded = ArrayList()
    }


    fun saveFav(str: String) : Boolean {

        arraylistFavorites.add(str)
        val json: String = gson.toJson(arraylistFavorites)
        sharedPreferencesEditor.putString("favorites", json)
        sharedPreferencesEditor.apply()
        return true
    }

    fun populateFavorites(key: String?): ArrayList<String> {
        val json = mSharedPreference.getString(key, "[]")
        val type: Type = object : TypeToken<ArrayList<String?>?>() {}.type
        return gson.fromJson(json, type)
    }

    private fun findViewByIds() {

        relativeLayoutMain = findViewById(R.id.rl_main)
        editTextSearch = findViewById(R.id.edtx_search_query)
        recyclerview = findViewById(R.id.rv)
        linearLayoutFavs = findViewById(R.id.ll_dynamic)
        seekBar = findViewById(R.id.seek)
        wfs = findViewById(R.id.wfsb)

    //    wavePlay()
        txNow = findViewById(R.id.tx_current_time)
        txTotal = findViewById(R.id.tx_duration)
    }



    private fun makeToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
//        saveQuery(query)
        if (!MusicService.mediaPlayer.isPlaying) {
            stopService(Intent(this@MainActivity, MusicService::class.java))
            MusicService.notificationManager.cancelAll();
        }
        super.onDestroy()
     }



    private fun saveQuery(query: String) {
        // Storing data into SharedPreferences
        var sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)
        // Creating an Editor object to edit(write to the file)
        var sharedPreferencesEditor = sharedPreferences.edit()
        // Storing the key and its value as the data fetched from edittext
        sharedPreferencesEditor.putString("playingQuery", query)

        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
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

        playIntent = Intent(
            this,
            MusicService::class.java
        )
        if (isMyServiceRunning(MusicService::class.java)) {

            binding.fab.visibility = VISIBLE
            if (mediaPlayer.isPlaying)
            binding.fab.setImageResource(android.R.drawable.ic_media_pause)
            else binding.fab.setImageResource(android.R.drawable.ic_media_play)

            sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);

            songIndex = sharedPreferences.getInt("playingIndex", 0)
            var rvAdapter = MusicAdapter(dataList, this@MainActivity)
            recyclerview.adapter = rvAdapter
            recyclerview.setLayoutManager(
                LinearLayoutManager(
                    this@MainActivity,
                    LinearLayoutManager.HORIZONTAL,false
                )
            )

           updateUI(songIndex)

        }

    }




    private fun updateUI(what: Int) {

        wfs.visibility = View.VISIBLE
        txNow.visibility = View.VISIBLE
        txTotal.visibility = View.VISIBLE
        wfs.setSampleFrom(R.raw.abc)
        wfs.apply {
            onProgressChanged = object : SeekBarOnProgressChanged {
                override fun onProgressChanged(
                    waveformSeekBar: WaveformSeekBar,
                    progress: Float,
                    fromUser: Boolean
                ) {
                    if (mediaPlayer != null)
                        if (fromUser) {
                            mediaPlayer?.seekTo(progress.toInt())
                        }
                }
            }
        }

        recyclerview.scrollToPosition(what)
        val thread = Thread {
            try {
                // Your code goes here
                val url = URL(dataList[what].album.cover)
                bitmapAlbum = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                image = BitmapDrawable(applicationContext.getResources(), bitmapAlbum)
                handlerForBG.postDelayed(Runnable {  relativeLayoutMain.background = image }, 1000)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Log.d("updateUI exception - ", e.toString())
            }
        }

        thread.start()

        seekBar.max = mediaPlayer.duration
        wfs.maxProgress = mediaPlayer.duration.toFloat()


        fixedRateTimer("timer", false, 0L, 1000) {
            this@MainActivity.runOnUiThread {
                if(isMyServiceRunning(MusicService::class.java))
                if(mediaPlayer.isPlaying) {
                    seekBar.setProgress(mediaPlayer.currentPosition)
                    wfs.progress = mediaPlayer.currentPosition.toFloat()
                    Log.d("Time21 ", mediaPlayer.currentPosition.toString())
                    val duration = mediaPlayer.currentPosition
                    val d: Date = Date(duration.toLong())
                    val df: SimpleDateFormat = SimpleDateFormat("mm:ss") // HH for 0-23
                    df.setTimeZone(TimeZone.getTimeZone("GMT"))
                    val time: kotlin.String = df.format(d)
                    txNow.setText(time)
                }
            }
        }

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
                dataList = ArrayList()
                dataList = (response.body()?.data as ArrayList<Data>?)!!

                if (dataList.size > 0) {
                    binding.fab.setImageResource(android.R.drawable.ic_media_play)
                    binding.fab.visibility = VISIBLE
                }
                songs.clear()
                for (item in dataList)
                    songs.add(item.preview)

                for (item in dataList)
                    Log.d("DATA7", "p - " + item.preview + "\n l - " + item.link)


                var rvAdapter = MusicAdapter(dataList, this@MainActivity)
                recyclerview.adapter = rvAdapter
                recyclerview.setLayoutManager(
                    LinearLayoutManager(
                        this@MainActivity,
                        LinearLayoutManager.HORIZONTAL,false
                    )
                )

            }

            override fun onFailure(call: Call<MusicData?>, t: Throwable) {
        //        Toast.makeText(applicationContext, "not Found", Toast.LENGTH_LONG).show()
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

    @RequiresApi(Build.VERSION_CODES.O)
    override
    fun onItemClick(position: Int) {



    }

    @RequiresApi(Build.VERSION_CODES.O)
    override
    fun onItemLongClick(position: Int) {

        saveFav(editTextSearch.text.toString())
            makeToast("Added " + editTextSearch.text.toString() + " to Favs!")

    }




}