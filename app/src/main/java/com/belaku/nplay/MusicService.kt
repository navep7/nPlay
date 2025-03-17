package com.belaku.nplay

import android.R
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.palette.graphics.Palette
import com.belaku.nplay.MainActivity.Companion.dataList
import com.belaku.nplay.MainActivity.Companion.imageArtAlbum
import com.belaku.nplay.MainActivity.Companion.makeToast
import com.belaku.nplay.MainActivity.Companion.relativeLayoutMain
import com.belaku.nplay.MainActivity.Companion.txNow
import com.belaku.nplay.MainActivity.Companion.txSongName
import com.belaku.nplay.MainActivity.Companion.wfs
import java.net.URL
import java.util.Random
import java.util.Timer


class MusicService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {


    var handler: Handler = Handler()
    private lateinit var serviceNotification: Notification
    private lateinit var sendIntent: Intent
    private lateinit var scontext: MusicService


    companion object {
        lateinit var timerInService: Timer
        var songIndex: Int = 0
        lateinit var mediaPlayer: MediaPlayer
        fun isMPInitialised() = ::mediaPlayer.isInitialized
        lateinit var notificationManager: NotificationManager
        lateinit var noteContentView: RemoteViews
        var songsUrlList: ArrayList<String> = ArrayList()
        var songsNameList: ArrayList<String> = ArrayList()
        var songsAlbumArtList: ArrayList<String> = ArrayList()
    }



    override fun onCreate() {
        super.onCreate()
        serviceNotify("")

    }

    private fun serviceNotify(str:String) {

        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "my_channel_01"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )

            serviceNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(str)
                .setOngoing(true)
                .setContentText("").build()

            startForeground(1, serviceNotification)

        }
    }


    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }


    @SuppressLint("RemoteViewLayout")
    private fun notifySong(sIndex: Int) {

     //   serviceNotify(MainActivity.dataList[sIndex].title)
        val intent = Intent(
            applicationContext,
            MainActivity::class.java
        )
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )


        noteContentView = MainActivity.contentView
        var str = " ﮩـﮩﮩ٨ـ ♡ ﮩ٨ـﮩﮩ٨ـ" + "<b>" + songsNameList[sIndex] + "</b> " + " ﮩـﮩﮩ٨ـ ♡ ﮩ٨ـﮩﮩ٨ـ" + "..,";
        noteContentView.setTextViewText(com.belaku.nplay.R.id.note_song_name, Html.fromHtml(str))


        val channelId = "some_channel_id"
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, channelId)
                .setSilent(true)
                .setContent(noteContentView)
                .setSmallIcon(R.drawable.ic_media_play)
                .setAutoCancel(true)
                .setSound(null)
                .setOngoing(true)
                .setContentIntent(pendingIntent)

        notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager


        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_LOW
            )
            checkNotNull(notificationManager)
            notificationManager.createNotificationChannel(channel)
        }

        checkNotNull(notificationManager)
        notificationManager.notify(0,  notificationBuilder.build())
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        scontext = this;
        songsUrlList.clear()
        songsNameList.clear()
        songsAlbumArtList.clear()
        songIndex = 0
        var size: Int = intent.extras?.size()!!.toInt()
        for (i in 0 until size) {
                var splits = intent.extras?.get(i.toString()).toString().split(" - ")
                songsUrlList.add(splits[0])
                songsNameList.add(splits.get(1))
                songsAlbumArtList.add(splits.get(2))
        }
        notifySong(songIndex)
        Toast.makeText(scontext, "P - " + songsNameList[0], Toast.LENGTH_LONG).show()

        try {
            val uri = Uri.parse(songsUrlList[songIndex])
            wfs.visibility = View.VISIBLE
            txSongName.visibility = View.VISIBLE
            txNow.visibility = View.VISIBLE
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(applicationContext, uri)
                prepare() // might take long! (for buffering, etc)
                start()
                fadeIN()
                saveIndex(songIndex)
                //      startFadeIn()
                //       saveIndex(0)
            }
            mediaPlayer.setOnCompletionListener(this)
            mediaPlayer.setOnErrorListener(this)
        } catch (e: Exception) {
            println(e.toString())
            Toast.makeText(applicationContext, "P ex - " + e, Toast.LENGTH_LONG).show()
        }

        sendIntent = intent

        updateActivity(0)

        return START_STICKY

    }

    private fun fadeIN() {

        var vl = 1
        var vr = 1

        val handlerIN = Handler(Looper.getMainLooper())
        val runnableIN: Runnable = object : Runnable {
            override fun run() {
                //do something here
                if (vl < 10) {
                    mediaPlayer.setVolume(vl++/10f, vr++/10f)
                    handlerIN.postDelayed(this, 1000)
                } else {
                    makeToast("MAXnow")
                    handlerIN.removeCallbacks(this)
                }
            }
        }
        handlerIN.post(runnableIN)



    }


    private fun updateActivity(sIn: Int) {
        Log.d("BR21", "Broadcasting message")
        val intent = Intent("nPlay_Events")

        // You can also include some extra data.
        intent.putExtra("song_index", sIn )

        Log.d("BR21", "sending")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }



    @SuppressLint("ResourceType")
    override fun onCompletion(p0: MediaPlayer?) {

        if (songsUrlList.size > 1 && songsUrlList.size > songIndex + 1) {
            songIndex++

        notifySong(songIndex)

        if (songIndex < songsUrlList.size) {
            val uri = Uri.parse(songsUrlList[songIndex])
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(applicationContext, uri)
                prepare() // might take long! (for buffering, etc)
                start()
                fadeIN()
                saveIndex(songIndex)
            }

            mediaPlayer.setOnCompletionListener(this)
            mediaPlayer.setOnErrorListener(this)
        }


            Thread {
                try {

                    wfs.setSampleFrom(dataList[songIndex].preview)
                } catch (e: Exception) {
                    Log.d("ExcpSeek - ", e.toString())
                    e.printStackTrace()
                }
            }.start()

            txSongName.text = songsNameList[songIndex]
            Thread {
                try {
                    // Your code goes here
                    val url = URL(songsAlbumArtList[songIndex])
                    var bitmapAlbum =
                        BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    imageArtAlbum = BitmapDrawable(applicationContext.resources, bitmapAlbum)

                    relativeLayoutMain.background = imageArtAlbum


                    Palette.from(imageArtAlbum.bitmap).generate { palette ->
                        // Do something with colors...
                        if (palette != null) {
                            wfs.waveBackgroundColor = palette.getLightMutedColor(com.belaku.nplay.R.color.white)
                            wfs.waveProgressColor = palette.getDarkMutedColor(com.belaku.nplay.R.color.black)

                            val rnd: Random = Random()
                            val color = Color.argb(
                                255,
                                rnd.nextInt(256),
                                rnd.nextInt(256),
                                rnd.nextInt(256)
                            )

                        //    txSongName.setTextColor(color)

                        }
                    }

                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    Log.d("updateUI exception - ", e.toString())
                }
            }.start()

            if (dataList[songIndex].title.equals(songsNameList.get(songIndex)))
            MainActivity.recyclerview.smoothScrollToPosition(songIndex)
         //   else MainActivity.makeToast(dataList[songIndex].title + " vs " + (songsNameList.get(songIndex)))

        //    updateActivity()
    } else {
            super.stopSelf()
            txSongName.text = "Finished Playing!"
            MainActivity.fabPlayPause.setImageResource(android.R.drawable.ic_media_play)
            songsUrlList.clear()
        }
    }

    private fun saveIndex(songIndex: Int) {
        // Storing data into SharedPreferences
        var sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)
        // Creating an Editor object to edit(write to the file)
        var sharedPreferencesEditor = sharedPreferences.edit()
        // Storing the key and its value as the data fetched from edittext
        sharedPreferencesEditor.putInt("playingIndex", songIndex)

        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }


    override fun onDestroy() {
        super.onDestroy()
        notificationManager.cancelAll()
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
          }
        mediaPlayer.release();
        Log.i("OnDestroyMS", "onDestroy: MS OnDestroy called");
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        TODO("Not yet implemented")
    //    Toast.makeText(applicationContext, "Err - " + p1.toString(), Toast.LENGTH_LONG).show()
        Log.d("onErrorMusService", p1.toString())
    }




}