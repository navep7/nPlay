package com.parvatha.music


import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Dialog
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.TextView.VISIBLE
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.parvatha.music.MusicService.Companion.mediaPlayer1
import com.parvatha.music.MusicService.Companion.mediaPlayer2
import com.parvatha.music.MusicService.Companion.notifySong
import com.parvatha.music.MusicService.Companion.saveIndex
import com.parvatha.music.MusicService.Companion.songIndex
import com.parvatha.music.MusicService.Companion.songsAlbumArtList
import com.parvatha.music.MusicService.Companion.songsNameList
import com.parvatha.music.MusicService.Companion.songsUrlList
import com.parvatha.music.databinding.ActivityMainBinding
import com.google.android.ads.nativetemplates.NativeTemplateStyle
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import com.parvatha.music.MusicService.Companion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.TimeZone
import kotlin.concurrent.fixedRateTimer
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity(), MusicAdapter.RecyclerViewEvent {


    private var TxFavorites: ArrayList<TextView> = ArrayList()
    private lateinit var runnablePics: Runnable
    private lateinit var handlerPics: Handler

    private var songArts: java.util.ArrayList<String> = ArrayList()
    private lateinit var nativeAdLoader: AdLoader

    private var adLoaded: Boolean = false

    private var gson: Gson = Gson()
    private lateinit var linearLayoutFavs: LinearLayout
    private lateinit var arrayListFavsAdded: ArrayList<String>
    private lateinit var mSharedPreference: SharedPreferences
    private lateinit var editTextSearch: EditText
    private lateinit var textViewFeaturing: TextView


    private var playingSongIndex by Delegates.notNull<Int>()
    private var playingSeekDuration by Delegates.notNull<Int>()
    private var playingSeekUpdate by Delegates.notNull<Int>()


    private lateinit var mMessageReceiver: BroadcastReceiver
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var sharedPreferences: SharedPreferences
    private var arraylistFavorites = ArrayList<String>()

    private lateinit var playIntent: Intent
    private lateinit var handlerForBG: Handler
    private var songIndex: Int = 0
    private lateinit var imageButtonPlayAlbum: ImageButton
    private lateinit var fabFavorite: FloatingActionButton

    @SuppressLint("StaticFieldLeak")
    companion object {
        fun makeToast(s: String) {
            Log.d("Toast7ing", s)
            Toast.makeText(appContext, s, Toast.LENGTH_SHORT).show()
        }

        @SuppressLint("ResourceAsColor")
        @RequiresApi(Build.VERSION_CODES.O)
        fun initializeMPs() {
            songIndex++
            txSongName.text = songsNameList[songIndex]
            Thread {
                try {
                    wfs.setSampleFrom(MusicService.songsUrlList[songIndex])
                } catch (e: Exception) {
                    Log.d("ExcpSeek - ", e.toString())
                    e.printStackTrace()
                }
            }.start()

            Thread {
                try {
                    val url = URL(MusicService.songsAlbumArtList[songIndex])
                    var bitmapAlbum =
                        BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    imageArtAlbum = BitmapDrawable(appContext.resources, bitmapAlbum)

                    mainActivity.runOnUiThread {
                        relativeLayoutMain.background = imageArtAlbum

                    }
                    //    noteContentView.setImageViewBitmap(com.belaku.nplay.R.id.note_image, imageArtAlbum.bitmap)


                    Palette.from(imageArtAlbum.bitmap).generate { palette ->
                        // Do something with colors...
                        if (palette != null) {
                            wfs.waveBackgroundColor =
                                palette.getLightMutedColor(R.color.white)
                            wfs.waveProgressColor =
                                palette.getDarkMutedColor(R.color.black)
                        }
                    }

                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    Log.d("updateUI exception - ", e.toString())
                }
            }.start()
            //   updateUI(songIndex)
            try {
                val uri = Uri.parse(songsUrlList[songIndex])
                wfs.visibility = View.VISIBLE
                txSongName.visibility = View.VISIBLE
                txNow.visibility = View.VISIBLE
                mediaPlayer1 = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(appContext, uri)
                    prepare() // might take long! (for buffering, etc)
                    start()
                    saveIndex(songIndex)
                }


                //   mediaPlayer1.setOnErrorListener(this)
            } catch (e: Exception) {
                println(e.toString())
                Toast.makeText(appContext, "P ex - " + e, Toast.LENGTH_LONG).show()
            }

            try {

                val uri = Uri.parse(songsUrlList[songIndex])
                wfs.visibility = View.VISIBLE
                txSongName.visibility = View.VISIBLE
                txNow.visibility = View.VISIBLE
                mediaPlayer2 = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(appContext, uri)
                    prepare() // might take long! (for buffering, etc)
                    //     saveIndex(songIndex)
                }

            } catch (e: Exception) {
                println(e.toString())
                Toast.makeText(appContext, "P ex - " + e, Toast.LENGTH_LONG).show()
            }
        }

        fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
            val width = bm.width
            val height = bm.height
            val scaleWidth = (newWidth.toFloat()) / width
            val scaleHeight = (newHeight.toFloat()) / height
            // CREATE A MATRIX FOR THE MANIPULATION
            val matrix: Matrix = Matrix()
            // RESIZE THE BIT MAP
            matrix.postScale(scaleWidth, scaleHeight)

            // "RECREATE" THE NEW BITMAP
            val resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false
            )
            return resizedBitmap
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @SuppressLint("ResourceAsColor")
        private fun updateUI(what: Int) {

            for (item in dataList)
                if (playOnlyPreviews)
                    songs.add(item.preview + " - " + item.title + " - " + item.album.cover)
                else songs.add(item.link + " - " + item.title + " - " + item.album.cover)


            txSongName.text = songsNameList[what]

            template.visibility = INVISIBLE
            wfs.visibility = View.VISIBLE
            txSongName.visibility = VISIBLE
            txNow.visibility = View.VISIBLE


            Thread {
                try {
                    wfs.setSampleFrom(songsUrlList[what])
                } catch (e: Exception) {
                    Log.d("ExcpSeek - ", e.toString())
                    e.printStackTrace()
                }
            }.start()

            Thread {
                try {
                    val url = URL(songsAlbumArtList[what])
                    var bitmapAlbum =
                        BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    imageArtAlbum = BitmapDrawable(appContext.resources, bitmapAlbum)

                    mainActivity.runOnUiThread {
                        Handler().postDelayed(Runnable {
                            relativeLayoutMain.background = imageArtAlbum
                        }, 500)
                        if (setLockWall)
                            WallpaperManager.getInstance(appContext).setBitmap(
                                getResizedBitmap(
                                    imageArtAlbum.bitmap,
                                    displayMetrics.widthPixels,
                                    displayMetrics.heightPixels
                                ), null, true, WallpaperManager.FLAG_LOCK
                            )
                    }

                    Palette.from(imageArtAlbum.bitmap).generate { palette ->
                        // Do something with colors...
                        if (palette != null) {
                            wfs.waveBackgroundColor =
                                palette.getLightMutedColor(R.color.white)
                            wfs.waveProgressColor =
                                palette.getDarkMutedColor(R.color.black)
                        }
                    }

                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    Log.d("updateBGexception - ", e.toString())
                }
            }.start()



            if (MusicService.isMP1Initialised() || MusicService.isMP2Initialised()) {
                try {
                    wfs.maxProgress = mediaPlayer1.duration.toFloat()
                } catch (ex: Exception) {
                    wfs.maxProgress = mediaPlayer2.duration.toFloat()
                }
                fixedRateTimer("timer", false, 0L, 1000) {
                    mainActivity.runOnUiThread {
                        if (isMyServiceRunning(MusicService::class.java))
                            try {
                                if (mediaPlayer1.isPlaying) {
                                    wfs.apply {
                                        onProgressChanged = object : SeekBarOnProgressChanged {
                                            override fun onProgressChanged(
                                                waveformSeekBar: WaveformSeekBar,
                                                progress: Float,
                                                fromUser: Boolean
                                            ) {
                                                if (mediaPlayer1 != null)
                                                    if (fromUser) {
                                                        try {
                                                            mediaPlayer1?.seekTo(progress.toInt())
                                                        } catch (ex: Exception) {
                                                            mediaPlayer2?.seekTo(progress.toInt())
                                                        }
                                                    }
                                            }
                                        }
                                    }
                                    wfs.progress = mediaPlayer1.currentPosition.toFloat()
                                    Log.d("Time21 ", mediaPlayer1.currentPosition.toString())
                                    val duration = mediaPlayer1.currentPosition
                                    val d: Date = Date(duration.toLong())
                                    val df: SimpleDateFormat =
                                        SimpleDateFormat("mm:ss") // HH for 0-23
                                    df.setTimeZone(TimeZone.getTimeZone("GMT"))
                                    val time: kotlin.String = df.format(d)
                                    txNow.setText(time)


                                    if (time.equals("00:25")) {
                                        if (crossFadeNeeded) {
                                            initializeMPs()
                                            crossFade(mediaPlayer1, mediaPlayer2)
                                            notifySong(songIndex)
                                            saveIndex(songIndex)
                                        }
                                    }
                                } else if (mediaPlayer2.isPlaying) {
                                    wfs.apply {
                                        onProgressChanged = object : SeekBarOnProgressChanged {
                                            override fun onProgressChanged(
                                                waveformSeekBar: WaveformSeekBar,
                                                progress: Float,
                                                fromUser: Boolean
                                            ) {
                                                if (mediaPlayer1 != null)
                                                    if (fromUser) {
                                                        try {
                                                            mediaPlayer2?.seekTo(progress.toInt())
                                                        } catch (ex: Exception) {
                                                            mediaPlayer1?.seekTo(progress.toInt())
                                                        }
                                                    }
                                            }
                                        }
                                    }
                                    wfs.progress = mediaPlayer2.currentPosition.toFloat()
                                    Log.d(
                                        "Time21 ",
                                        mediaPlayer2.currentPosition.toString()
                                    )
                                    val duration = mediaPlayer2.currentPosition
                                    val d: Date = Date(duration.toLong())
                                    val df: SimpleDateFormat =
                                        SimpleDateFormat("mm:ss") // HH for 0-23
                                    df.setTimeZone(TimeZone.getTimeZone("GMT"))
                                    val time: kotlin.String = df.format(d)
                                    txNow.setText(time)


                                    if (time.equals("00:25")) {
                                        if (crossFadeNeeded) {
                                            initializeMPs()
                                            crossFade(mediaPlayer2, mediaPlayer1)
                                            notifySong(songIndex)
                                            saveIndex(songIndex)
                                        }
                                    }
                                }
                            } catch (ex: Exception) {
                                try {

                                    if (mediaPlayer2.isPlaying) {
                                        wfs.progress = mediaPlayer2.currentPosition.toFloat()
                                        Log.d(
                                            "Time21 ",
                                            mediaPlayer2.currentPosition.toString()
                                        )
                                        val duration = mediaPlayer2.currentPosition
                                        val d: Date = Date(duration.toLong())
                                        val df: SimpleDateFormat =
                                            SimpleDateFormat("mm:ss") // HH for 0-23
                                        df.setTimeZone(TimeZone.getTimeZone("GMT"))
                                        val time: kotlin.String = df.format(d)
                                        txNow.setText(time)


                                        if (time.equals("00:25")) {
                                            if (crossFadeNeeded) {
                                                initializeMPs()
                                                crossFade(mediaPlayer2, mediaPlayer1)
                                            }
                                        }
                                    }
                                } catch (ex: Exception) {
                                    //     txSongName.text = "End! - " + ex.getStackTrace()[0].getLineNumber();
                                    appContext.stopService(
                                        Intent(
                                            mainActivity,
                                            MusicService::class.java
                                        )
                                    )
                                }
                            }
                    }
                }
            }

        }

        private fun crossFade(musicPlayerOut: MediaPlayer, musicPlayerIn: MediaPlayer) {

            if (dataList[songIndex].title.equals(songsNameList[songIndex]))
                recyclerview.smoothScrollToPosition(songIndex)

            val CROSSFADE_DURATION = 5000
            fadeOut(musicPlayerOut, CROSSFADE_DURATION)

            fadeIn(musicPlayerIn, CROSSFADE_DURATION)

        }

        fun fadeOut(_player: MediaPlayer, duration: Int) {
            val deviceVolume: Float = 1.0f
            val h = Handler()
            h.postDelayed(object : Runnable {
                private var time = duration.toFloat()
                private var volume = 0.0f

                override fun run() {
                    // can call h again after work!
                    time -= 100f
                    volume = (deviceVolume * time) / duration
                    try {
                        if (_player.isPlaying)
                            _player.setVolume(volume, volume)
                    } catch (ex: Exception) {

                    }
                    if (time > 0) h.postDelayed(this, 100)
                    else {
                        _player.stop()
                        _player.release()
                    }
                }
            }, 100) // delay (takes millis)
        }

        fun fadeIn(_player: MediaPlayer, duration: Int) {
            val deviceVolume: Float = 1.0f
            val h = Handler()
            h.postDelayed(object : Runnable {
                private var time = 0.0f
                private var volume = 0.0f

                override fun run() {
                    if (!_player.isPlaying) _player.start()
                    time += 100f
                    volume = (deviceVolume * time) / duration
                    _player.setVolume(volume, volume)
                    if (time < duration) h.postDelayed(this, 100)
                }
            }, 100) // delay (takes millis)


        }

        private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
            val manager = appContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }

        var arrayListplayLists: ArrayList<String> = ArrayList()
        var crossFadeNeeded: Boolean = false
        var playOnlyPreviews: Boolean = true
        var setLockWall: Boolean = true
        lateinit var mainActivity: Activity
        private lateinit var template: TemplateView
        private var songs: ArrayList<String> = ArrayList()
        lateinit var appContext: Context

        //    lateinit var swCrossFade: MaterialSwitch
        //    lateinit var swPreview: MaterialSwitch
        lateinit var linearLayoutManager: LinearLayoutManager
        lateinit var rvAdapter: MusicAdapter

        //  var screenDimens by Delegates.notNull<Int>()
        lateinit var displayMetrics: DisplayMetrics
        lateinit var fabPlayPause: FloatingActionButton
        lateinit var dataList: ArrayList<Data>
        lateinit var wfs: WaveformSeekBar
        lateinit var txSongName: TextView
        lateinit var txNow: TextView
        lateinit var contentView: RemoteViews
        lateinit var imageArtAlbum: BitmapDrawable
        lateinit var relativeLayoutMain: RelativeLayout
        lateinit var recyclerview: RecyclerView
        lateinit var arraylistFavoriteSongs: ArrayList<Data>

    }

    private lateinit var plName: String
    private lateinit var binding: ActivityMainBinding

    private var mInterstitialAd: InterstitialAd? = null
    private final val TAG = "MainActivity"


    @SuppressLint("ResourceAsColor", "SetTextI18n", "ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appContext = applicationContext
        mainActivity = this@MainActivity

        displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        // screenDimens = displayMetrics.widthPixels


        nativeAdLoader =
            AdLoader.Builder(this, resources.getString(R.string.admob_native_adunit_id))
                .forNativeAd(object : NativeAd.OnNativeAdLoadedListener {
                    private val background: ColorDrawable? = null

                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        val styles =
                            NativeTemplateStyle.Builder().withMainBackgroundColor(background)
                                .build()

                        template.setStyles(styles)
                        template.setNativeAd(nativeAd)
                        adLoaded = true
                        // Showing a simple Toast message to user when Native an ad is Loaded and ready to show

                        template.setVisibility(VISIBLE)
                    }
                }).build()


        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(this@MainActivity) {}
        }

        PermissionCheck()

        contentView = RemoteViews(packageName, R.layout.notification_push)

        findViewByIds()
        initializeStuff()

        arrayListplayLists.add("Favorites")

        mSharedPreference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        sharedPreferencesEditor = mSharedPreference.edit()
        arraylistFavorites = populateFavorites("favorites")

        for (item in arraylistFavorites) {
            val tx: TextView = TextView(applicationContext)
            TxFavorites.add(tx)
            tx.text = "\t\t\t" + item.substring(0, 1)
                .uppercase(Locale.ROOT) + item.substring(1) + "\t\t\t"
            tx.setBackgroundResource(R.drawable.txlabel_bg_unselected)

            tx.setOnClickListener {

                for (item in TxFavorites)
                    item.setBackgroundResource(R.drawable.txlabel_bg_unselected)


                tx.setBackgroundResource(R.drawable.txlabel_bg_selected)
                showIntrAd()

                textViewFeaturing.text = "Featuring, " + tx.text.toString()
                wfs.progress = 0f
                plName = tx.text.toString()
                if (!tx.text.toString().strip().equals("Favorites"))
                    Getdata()
                else {
                    getFavorites()
                }
                checkFavoritesIcon()
            }
            linearLayoutFavs.addView(tx)



            if (!isMyServiceRunning(MusicService::class.java)) {
                wfs.progress = 0f
                if (isMyServiceRunning(MusicService::class.java)) {
                    stopService(Intent(this@MainActivity, MusicService::class.java))
                }

            }
        }





        editTextSearch.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                textViewFeaturing.text = "Featuring, " + editTextSearch.text.substring(0, 1)
                    .toUpperCase() + editTextSearch.text.substring(1)
                plName = editTextSearch.getText().toString()
                Getdata()
                handled = true
            }
            handled
        })



        handlerForBG = Handler()



        fabFavorite.setOnClickListener {

            showIntrAd()

            fabFavorite.setImageDrawable(resources.getDrawable(android.R.drawable.star_on))
            if (textViewFeaturing.text.length > 0)
                if (saveFav(textViewFeaturing.text.toString().split(",").get(1)))
                    Toast.makeText(
                        applicationContext,
                        "Added " + (textViewFeaturing.text.toString().split(",")
                            .get(1)) + " to Favs!", Toast.LENGTH_LONG
                    ).show()
                else Toast.makeText(applicationContext, "Already in Favs!", Toast.LENGTH_LONG)
                    .show()

        }

        var mp: MediaPlayer

        imageButtonPlayAlbum.setOnClickListener {


            if (this::handlerPics.isInitialized)
                handlerPics.removeCallbacks(runnablePics)

            if (isMyServiceRunning(MusicService::class.java))
                stopService(playIntent)


            imageButtonPlayAlbum.visibility = View.INVISIBLE
            fabPlayPause.visibility = View.VISIBLE
            fabPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            var i = 0;
            for (item in songs) {
                playIntent.putExtra(i.toString(), item)
                i++
            }

            if (!this::plName.isInitialized)
                plName = "Trending"
            saveQuery(plName)
            startForegroundService(playIntent)


        }

        var mp1: Boolean = true
        fabPlayPause.setOnClickListener { view ->

            try {
                if (mediaPlayer1.isPlaying) {
                    mp1 = true
                    fabPlayPause.setImageResource(android.R.drawable.ic_media_play)
                    mediaPlayer1.pause()
                    showNativeAd()
                } else if (mediaPlayer2.isPlaying) {
                    mp1 = false
                    fabPlayPause.setImageResource(android.R.drawable.ic_media_play)
                    mediaPlayer2.pause()
                    showNativeAd()
                } else {
                    fabPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                    if (mp1)
                        mediaPlayer1.start()
                    else mediaPlayer2.start()

                    template.visibility = INVISIBLE

                }
            } catch (ex: Exception) {
                try {
                    if (mediaPlayer2.isPlaying) {
                        mp1 = false
                        fabPlayPause.setImageResource(android.R.drawable.ic_media_play)
                        mediaPlayer2.pause()
                        showNativeAd()
                    } else {
                        fabPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                        if (mp1)
                            mediaPlayer1.start()
                        else mediaPlayer2.start()

                        template.visibility = INVISIBLE

                    }
                } catch (ex: Exception) {
                    stopService(Intent(this@MainActivity, MusicService::class.java))
                    //    txSongName.text = "End of Playback - " + ex.getStackTrace()[0].getLineNumber();
                    try {
                        if (mediaPlayer1.isPlaying) {
                            mp1 = false
                            fabPlayPause.setImageResource(android.R.drawable.ic_media_play)
                            mediaPlayer1.pause()
                            showNativeAd()
                        } else {
                            fabPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                            if (mp1)
                                mediaPlayer1.start()
                            else mediaPlayer2.start()

                            template.visibility = INVISIBLE

                        }
                    } catch (ex: Exception) {
                        stopService(Intent(this@MainActivity, MusicService::class.java))
                        txSongName.text =
                            "Didn't see this Com!ng" + ex.getStackTrace()[0].getLineNumber();
                    }
                }
            }

        }


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
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver,
            IntentFilter("nPlay_Events")
        );


    }


    private fun getTrending() {
        getFavorites()
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl("https://deezerdevs-deezer.p.rapidapi.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterfaceTrends::class.java)

        val retrofitData = retrofitBuilder.getTrending()

        retrofitData.enqueue(object : Callback<MusicData?> {


            override fun onResponse(call: Call<MusicData?>, response: Response<MusicData?>) {
                dataList = (response.body()?.data as ArrayList<Data>?)!!

                if (dataList.size > 0) {
                    imageButtonPlayAlbum.setImageResource(android.R.drawable.ic_media_play)
                    imageButtonPlayAlbum.visibility = VISIBLE
                    fabFavorite.visibility = INVISIBLE
                    textViewFeaturing.visibility = VISIBLE
                }
                songs.clear()
                for (item in dataList) {
                    songArts.add(item.album.cover)
                    if (playOnlyPreviews)
                        songs.add(item.preview + " - " + item.title + " - " + item.album.cover)
                    else songs.add(item.link + " - " + item.title + " - " + item.album.cover)
                }

                try {
                    if (!(mediaPlayer1.isPlaying || mediaPlayer2.isPlaying))
                        setPics(songs)
                } catch (ex: Exception) {
                    try {
                        if (MusicService.isMP2Initialised()) {
                            if (!mediaPlayer2.isPlaying)
                                setPics(songs)
                        } else setPics(songs)
                    } catch (ex: Exception) {
                        setPics(songs)
                    }
                }

                rvAdapter = MusicAdapter(this@MainActivity, dataList, this@MainActivity)
                linearLayoutManager = LinearLayoutManager(
                    this@MainActivity,
                    LinearLayoutManager.HORIZONTAL, false
                )
                recyclerview.adapter = rvAdapter
                recyclerview.setLayoutManager(
                    linearLayoutManager
                )

            }

            override fun onFailure(call: Call<MusicData?>, t: Throwable) {
                Toast.makeText(applicationContext, "onF - ", Toast.LENGTH_LONG).show()
            }

        })

        Handler().postDelayed(Runnable {
            for (i in 0 until dataList.size) {
                if (txSongName.text.equals(dataList.get(i).title))
                    recyclerview.smoothScrollToPosition(i)
            }
        }, 3000)

    }




    private fun setPics(songArts: java.util.ArrayList<String>) {

        handlerPics = Handler(Looper.getMainLooper())
        runnablePics = object : Runnable {
            override fun run() {
                //do something here
                changeBG()
                handlerPics.postDelayed(this, 3000)
            }

            private fun changeBG() {

                if (!songArts.isEmpty()) {
                    var splits = songArts[Random().nextInt(songArts.size) + 0].split(" - ")

                    Thread {
                        try {
                            val url = URL(splits.get(2))
                            var bitmapAlbum =
                                BitmapFactory.decodeStream(url.openConnection().getInputStream())
                            imageArtAlbum = BitmapDrawable(appContext.resources, bitmapAlbum)

                            mainActivity.runOnUiThread {

                                relativeLayoutMain.background = BitmapDrawable(
                                    resources,
                                    getResizedBitmap(
                                        imageArtAlbum.bitmap,
                                        displayMetrics.widthPixels * 3,
                                        displayMetrics.heightPixels * 3
                                    )
                                )

                            }
                        } catch (ex: Exception) {
                            //   makeToast(ex.toString())
                        }
                    }.start()

                }
            }
        }
        handlerPics.post(runnablePics)


    }

    private fun showNativeAd() {
    /*    if (adLoaded) {
            template.setVisibility(VISIBLE)
            adLoaded = false
            // Showing a simple Toast message to user when an Native ad is shown to the user

        } else {
            //Load the Native ad if it is not loaded
            loadNativeAd()
        }*/
    }

    private fun loadNativeAd() {
        // Creating  an Ad Request
        val adRequest = AdRequest.Builder().build()

        // load Native Ad with the Request
        nativeAdLoader.loadAd(adRequest)
        adLoaded = true

    }

    private fun showIntrAd() {
/*
   //     if (Random().nextInt() % 2 == 0) {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                this,
                resources.getString(R.string.admob_intr_adunit_id),
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        adError?.toString()?.let { }
                        mInterstitialAd = null
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        mInterstitialAd = interstitialAd
                        mInterstitialAd?.show(this@MainActivity)
                    }
                })
    //    }*/
    }

    private fun renderFavorites() {


        dataList.clear()
        for (i in arraylistFavoriteSongs.indices) {
            dataList.add(arraylistFavoriteSongs.get(i))
        }

        if (dataList.size > 0) {
            imageButtonPlayAlbum.setImageResource(android.R.drawable.ic_media_play)
            imageButtonPlayAlbum.visibility = VISIBLE
            fabFavorite.visibility = VISIBLE
            textViewFeaturing.visibility = VISIBLE
        }
        songs.clear()
        for (item in dataList)
            if (playOnlyPreviews)
                songs.add(item.preview + " - " + item.title + " - " + item.album.cover)
            else songs.add(item.link + " - " + item.title + " - " + item.album.cover)

        var rvAdapter = MusicAdapter(this@MainActivity, dataList, this@MainActivity)
        recyclerview.adapter = rvAdapter
        recyclerview.setLayoutManager(
            LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.HORIZONTAL, false
            )
        )

    }


    private fun PermissionCheck() {

        var permission_array = arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)
        if ((ContextCompat.checkSelfPermission(
                this,
                permission_array[0]
            )) == PackageManager.PERMISSION_DENIED
        ) {
            requestPermissions(permission_array, 0)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            //Do Your Operations Here

            //


        }
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
        val txTrending = TextView(applicationContext)
        TxFavorites.add(txTrending)
        txTrending.text = "\t\t\tTrending\t\t\t"
        txTrending.setBackgroundResource(R.drawable.txlabel_bg_selected)
        txTrending.setOnClickListener(View.OnClickListener {
            for (item in TxFavorites)
                item.setBackgroundResource(R.drawable.txlabel_bg_unselected)
            txTrending.setBackgroundResource(R.drawable.txlabel_bg_selected)
            getTrending()
            textViewFeaturing.text = "Trending..,"
        })
        linearLayoutFavs.addView(txTrending)
        sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)
        var playingALbum =  sharedPreferences.getString("playingQuery", "Trending").toString()
        makeToast("playingALbum - " + playingALbum)
        if (playingALbum.equals("Trending")) {
            getTrending()
            textViewFeaturing.text = "Trending..,"
        } else if (playingALbum.equals("\t\t\tFavorites\t\t\t")) {
            getFavorites()
            textViewFeaturing.text = "Favorites..,"
        } else {
            plName = playingALbum

            txTrending.setBackgroundResource(R.drawable.txlabel_bg_unselected)
            for (item in TxFavorites) {
                if (item.text.toString().equals(plName))
                    item.setBackgroundResource(R.drawable.txlabel_bg_selected)
            }
            Getdata()
            textViewFeaturing.text = "Featuring, " + plName
        }

    }


    fun saveFav(str: String): Boolean {

        if (!arraylistFavorites.contains(str.strip())) {
            arraylistFavorites.add(str.strip())
            val json: String = gson.toJson(arraylistFavorites)
            sharedPreferencesEditor.putString("favorites", json)
            sharedPreferencesEditor.apply()
            return true
        }
        return false
    }

    fun populateFavorites(key: String?): ArrayList<String> {
        val json = mSharedPreference.getString(key, "[]")
        val type: Type = object : TypeToken<ArrayList<String?>?>() {}.type
        return gson.fromJson(json, type)
    }

    private fun findViewByIds() {

        dataList = ArrayList()
        arraylistFavoriteSongs = ArrayList()

        relativeLayoutMain = findViewById(R.id.rl_main)
        editTextSearch = findViewById(R.id.edtx_search_query)
        recyclerview = findViewById(R.id.rv)
        linearLayoutFavs = findViewById(R.id.ll_dynamic)
        wfs = findViewById(R.id.wfsb)
        txSongName = findViewById(R.id.tx_sname)


        textViewFeaturing = findViewById(R.id.tx_featuring)
        txNow = findViewById(R.id.tx_current_time)
        fabPlayPause = findViewById(R.id.fab_play_pause)
        imageButtonPlayAlbum = findViewById(R.id.imgbtn_PlayAlbum)
        fabFavorite = findViewById(R.id.fab_favorite)
        template = findViewById(R.id.nativeTemplateView)

    }


    override fun onDestroy() {

        saveFavorites(arraylistFavoriteSongs)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        if (isMyServiceRunning(MusicService::class.java))
            try {
                if (!(mediaPlayer1.isPlaying || mediaPlayer2.isPlaying)) {
                    stopService(Intent(this@MainActivity, MusicService::class.java))
                    MusicService.notificationManager.cancelAll();
                }
            } catch (ex: Exception) {
                if (!mediaPlayer2.isPlaying) {
                    stopService(Intent(this@MainActivity, MusicService::class.java))
                    MusicService.notificationManager.cancelAll();
                }
            }

        super.onDestroy()
    }

    private fun saveFavorites(arraylistFavoriteSongs: ArrayList<Data>) {
        val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(arraylistFavoriteSongs)

        editor.putString("arraylistFavoriteSongs", json);
        editor.apply();
    }

    private fun getFavorites(): List<Data> {
        var arrayItems: List<Data> = ArrayList()
        val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val serializedObject = sharedPreferences.getString("arraylistFavoriteSongs", null)
        if (serializedObject != null) {
            val gson = Gson()
            val type = object : TypeToken<List<Data?>?>() {}.type
            arrayItems = gson.fromJson<List<Data>>(serializedObject, type)
        }

        arraylistFavoriteSongs = arrayItems as ArrayList<Data>
        renderFavorites()

        return arrayItems
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


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()

        playIntent = Intent(
            this,
            MusicService::class.java
        )
        sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)
        if (isMyServiceRunning(MusicService::class.java)) {
            fabPlayPause.visibility = VISIBLE
            fabPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            songIndex = sharedPreferences.getInt("playingIndex", 0)

            //    Toast.makeText(appContext, "onRplayinG - " + songsNameList[songIndex], Toast.LENGTH_LONG).show()
            var rvAdapter = MusicAdapter(this@MainActivity, dataList, this@MainActivity)
            recyclerview.adapter = rvAdapter
            recyclerview.setLayoutManager(
                LinearLayoutManager(
                    this@MainActivity,
                    LinearLayoutManager.HORIZONTAL, false
                )
            )


            updateUI(songIndex)

        }

    }


    private fun Getdata() {
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl("https://deezerdevs-deezer.p.rapidapi.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)

        val retrofitData = retrofitBuilder.getData(plName)

        retrofitData.enqueue(object : Callback<MusicData?> {
            override fun onResponse(
                call: Call<MusicData?>,
                response: Response<MusicData?>
            ) {
                dataList = ArrayList()
                dataList = (response.body()?.data as ArrayList<Data>?)!!

                if (dataList.size > 0) {
                    imageButtonPlayAlbum.setImageResource(android.R.drawable.ic_media_play)
                    imageButtonPlayAlbum.visibility = VISIBLE
                    fabFavorite.visibility = VISIBLE
                    textViewFeaturing.visibility = VISIBLE
                }
                songs.clear()
                for (item in dataList)
                    if (playOnlyPreviews)
                        songs.add(item.preview + " - " + item.title + " - " + item.album.cover)
                    else songs.add(item.link + " - " + item.title + " - " + item.album.cover)


                var rvAdapter = MusicAdapter(this@MainActivity, dataList, this@MainActivity)
                recyclerview.adapter = rvAdapter
                recyclerview.setLayoutManager(
                    LinearLayoutManager(
                        this@MainActivity,
                        LinearLayoutManager.HORIZONTAL, false
                    )
                )

            }

            override fun onFailure(call: Call<MusicData?>, t: Throwable) {
                Toast.makeText(applicationContext, "not Found", Toast.LENGTH_LONG).show()
            }

        })

        checkFavoritesIcon()

    }

    private fun checkFavoritesIcon() {

        if (textViewFeaturing.text.split(", ").size > 1)
        if (arraylistFavorites.contains(textViewFeaturing.text.split(", ").get(1).strip()))
            fabFavorite.setImageDrawable(resources.getDrawable(android.R.drawable.star_on))
        else fabFavorite.setImageDrawable(resources.getDrawable(android.R.drawable.star_off))
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
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            R.id.action_dj -> {
                showDJdialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    fun showDJdialog() {
        val dialog = Dialog(mainActivity)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dj_layout)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


        val djView = dialog.findViewById<View>(R.id.imgv_dj)
        djView.setOnTouchListener(object : View.OnTouchListener {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event!!.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        if (isMyServiceRunning(MusicService::class.java))
                            if (mediaPlayer1.isPlaying)
                                mediaPlayer1.playbackParams = PlaybackParams().setPitch(2.0f)
                            else if (mediaPlayer2.isPlaying)
                                mediaPlayer2.playbackParams = PlaybackParams().setPitch(2.0f)
                    }

                    MotionEvent.ACTION_MOVE -> {}
                    MotionEvent.ACTION_UP -> {
                        if (isMyServiceRunning(MusicService::class.java))
                            if (mediaPlayer1.isPlaying)
                                mediaPlayer1.playbackParams = PlaybackParams().setPitch(1.0f)
                            else if (mediaPlayer2.isPlaying)
                                mediaPlayer2.playbackParams = PlaybackParams().setPitch(1.0f)
                    }

                    else -> return true
                }
                return true
            }
        })

        dialog.show()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override
    fun onItemClick(position: Int) {

        if (this::handlerPics.isInitialized)
            handlerPics.removeCallbacks(runnablePics)
        makeToast(dataList[position].title)


        if (isMyServiceRunning(MusicService::class.java))
            stopService(playIntent)


        imageButtonPlayAlbum.visibility = View.INVISIBLE
        fabPlayPause.visibility = View.VISIBLE
        fabPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        var i = 0

        for (init in position until songs.size)
            playIntent.putExtra(i++.toString(), songs.get(init))

        startForegroundService(playIntent)

    }

    fun addToFavoriteSongs(text: String) {
        for (i in dataList.indices) {
            if (dataList.get(i).title.equals(text)) {

                if (arraylistFavoriteSongs.size == 0) {
                    saveFav("Favorites")
                }

                var arraylistFavoriteSongNames = ArrayList<String>()
                for (item in arraylistFavoriteSongs) {
                    arraylistFavoriteSongNames.add(item.title)
                }

                if (!arraylistFavoriteSongNames.contains(dataList.get(i).title)) {
                    Toast.makeText(
                        applicationContext,
                        "Added - " + dataList.get(i).title + " to Fav Songs!",
                        Toast.LENGTH_LONG
                    ).show()
                    arraylistFavoriteSongs.add(dataList.get(i))
                } else Toast.makeText(
                    applicationContext,
                    "Already exist in FavS",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        saveFavorites(arraylistFavoriteSongs)

    }


}

