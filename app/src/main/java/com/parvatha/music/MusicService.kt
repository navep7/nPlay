package com.parvatha.music

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.text.Html
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.parvatha.music.MainActivity.Companion.appContext
import com.parvatha.music.MainActivity.Companion.crossFadeNeeded
import com.parvatha.music.MainActivity.Companion.dataList
import com.parvatha.music.MainActivity.Companion.mainActivity
import com.parvatha.music.MainActivity.Companion.makeToast
import com.parvatha.music.MainActivity.Companion.txSongName


class MusicService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {


    var handler: Handler = Handler()
    private lateinit var serviceNotification: Notification
    private lateinit var sendIntent: Intent
    private lateinit var scontext: MusicService


    companion object {

        var songIndex: Int = 0
        lateinit var mediaPlayer1: MediaPlayer
        lateinit var mediaPlayer2: MediaPlayer
        fun isMP1Initialised() = ::mediaPlayer1.isInitialized
        fun isMP2Initialised() = ::mediaPlayer2.isInitialized



        fun saveIndex(songIndex: Int) {
            // Storing data into SharedPreferences
            var sharedPreferences = appContext.getSharedPreferences("MySharedPref", MODE_PRIVATE)
            // Creating an Editor object to edit(write to the file)
            var sharedPreferencesEditor = sharedPreferences.edit()
            // Storing the key and its value as the data fetched from edittext
            sharedPreferencesEditor.putInt("playingIndex", songIndex)

            sharedPreferencesEditor.apply()
            sharedPreferencesEditor.commit()
        }


        @SuppressLint("RemoteViewLayout")
        fun notifySong(sIndex: Int) {

            //   serviceNotify(MainActivity.dataList[sIndex].title)
            val intent = Intent(
                appContext,
                MainActivity::class.java
            )
            val pendingIntent = PendingIntent.getActivity(
                mainActivity, 0, intent,
                PendingIntent.FLAG_IMMUTABLE
            )


            noteContentView = MainActivity.contentView
            var str = " ﮩـﮩﮩ٨ـ ♡ ﮩ٨ـﮩﮩ٨ـ" + "<b>" + songsNameList[sIndex] + "</b> " + " ﮩـﮩﮩ٨ـ ♡ ﮩ٨ـﮩﮩ٨ـ" + "..,";
            noteContentView.setTextViewText(com.parvatha.music.R.id.note_song_name, Html.fromHtml(str))


            val channelId = "some_channel_id"
            val notificationBuilder: NotificationCompat.Builder =
                NotificationCompat.Builder(appContext, channelId)
                    .setSilent(true)
                    .setContent(noteContentView)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setAutoCancel(true)
                    .setSound(null)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)

            notificationManager =
                appContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager


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




        lateinit var notificationManager: NotificationManager
        lateinit var noteContentView: RemoteViews
        var songsUrlList: ArrayList<String> = ArrayList()
        var songsNameList: ArrayList<String> = ArrayList()
        var songsAlbumArtList: ArrayList<String> = ArrayList()
    }


    private fun updateActivity() {
        Log.d("BR21", "Broadcasting message")
        val intent = Intent("nPlay_Events")

        // You can also include some extra data.
        intent.putExtra("song_index", songIndex)

        Log.d("BR21", "sending")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
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




    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        scontext = this;
        songsUrlList.clear()
        songsNameList.clear()
        songsAlbumArtList.clear()

        var size: Int = intent.extras?.size()!!.toInt()
        for (i in 0 until size) {
                var splits = intent.extras?.get(i.toString()).toString().split(" - ")
                songsUrlList.add(splits[0])
                songsNameList.add(splits.get(1))
                songsAlbumArtList.add(splits.get(2))
        }
        if (songsNameList.size > songIndex)
        notifySong(songIndex)

        songIndex = -1
        MainActivity.initializeMPs()

        if (!crossFadeNeeded)
            mediaPlayer1.setOnCompletionListener(this)

    //    crossFade(mediaPlayer1, mediaPlayer2)

        sendIntent = intent

        updateActivity()

        return START_STICKY

    }



    override fun onCompletion(p0: MediaPlayer?) {

        makeToast("onCompletion")
        songIndex++

        if (songsNameList.size > songIndex) {
            notifySong(songIndex)
        } else txSongName.text = "End of Playback!"

        if (songIndex < songsUrlList.size) {
            val uri = Uri.parse(songsUrlList[songIndex])
            mediaPlayer2 = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(applicationContext, uri)
                prepare() // might take long! (for buffering, etc)
                start()
                //    startFadeIn()
                saveIndex(songIndex)
            }

            mediaPlayer2.setOnCompletionListener(this)
            mediaPlayer2.setOnErrorListener(this)

            updateActivity()

        }

    }






    override fun onDestroy() {
        super.onDestroy()
        notificationManager.cancelAll()
        try {
            if (mediaPlayer1.isPlaying()) {
                mediaPlayer1.stop();
                mediaPlayer1.release();
            } else if (mediaPlayer2.isPlaying()) {
                mediaPlayer2.stop();
                mediaPlayer2.release();
            }
        } catch (ex: Exception) {
            try {
                if(mediaPlayer2.isPlaying()){
                    mediaPlayer2.stop();
                    mediaPlayer2.release();
                }
            } catch (ex: Exception) {

            }
        }


        Log.i("OnDestroyMS", "onDestroy: MS OnDestroy called");
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        TODO("Not yet implemented")
    //    Toast.makeText(applicationContext, "Err - " + p1.toString(), Toast.LENGTH_LONG).show()
        Log.d("onErrorMusService", p1.toString())
    }




}