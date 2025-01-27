package com.belaku.nplay

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
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import java.util.Timer
import java.util.TimerTask


class MusicService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private lateinit var serviceNotification: Notification
    private lateinit var sendIntent: Intent
    private lateinit var scontext: MusicService
    private var songsUrlList: ArrayList<String> = ArrayList()

    companion object {
        lateinit var timerInService: Timer
        var songIndex: Int = 0
        lateinit var mediaPlayer: MediaPlayer
        lateinit var notificationManager: NotificationManager
    }





    override fun onCreate() {
        super.onCreate()
        serviceNotify("")
    //    notifySong(0)
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


        val channelId = "some_channel_id"
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, channelId)
                .setSilent(true)
                .setSmallIcon(android.R.drawable.ic_media_play) //                        .setContentTitle(getString(R.string.app_name)
                .setContentTitle(MainActivity.dataList[sIndex].title)
                    .setContentText(MainActivity.dataList[sIndex].album.title + " | \n" + MainActivity.dataList[sIndex].artist.name)
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

        if (intent.extras?.get("1") != null)
        for (i in 0 until 30) {
            if (intent.extras?.get(i.toString()) != null)
                songsUrlList.add(intent.extras?.get(i.toString()).toString())
            else break
        }
        else  songsUrlList.add(intent.extras?.get("0").toString())

     //   serviceNotify(MainActivity.dataList[songIndex].title)

        println("S21 - rSize" + songsUrlList.size)
        for (item in songsUrlList)
            println("S21 - received" + item)

        if (songsUrlList.size > 0) {

            for (i in MainActivity.dataList.indices) {
                if (MainActivity.dataList[i].preview.equals(songsUrlList[0]))
                    notifySong(i)
            }
         //   notifySong(songIndex)

            try {
                val uri = Uri.parse(songsUrlList[songIndex])
                MainActivity.wfs.visibility = View.VISIBLE
                MainActivity.txSongName.visibility = View.VISIBLE
                MainActivity.txNow.visibility = View.VISIBLE
                MainActivity.txSongName.text = MainActivity.dataList[songIndex].title
         //       MainActivity.wfs.setSampleFrom(songsUrlList[songIndex])
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
              //      startFadeIn()
                    saveIndex(songIndex)
                }
                mediaPlayer.setOnCompletionListener(this)
                mediaPlayer.setOnErrorListener(this)
            } catch (e: Exception) {
                println(e.toString())
                Toast.makeText(applicationContext, "P ex - " + e, Toast.LENGTH_LONG).show()
            }



        }

        if (intent != null) {
            sendIntent = intent
        }

        updateActivity()


        return START_STICKY
    }

    var volume: Float = 0f


    private fun startFadeOut() {
        volume = 1f
        val FADE_DURATION = 15000 //The duration of the fade
        //The amount of time between volume changes. The smaller this is, the smoother the fade
        val FADE_INTERVAL = 100
        val MIN_VOLUME = 0 //The volume will increase from 0 to 1
        val numberOfSteps = FADE_DURATION / FADE_INTERVAL //Calculate the number of fade steps
        //Calculate by how much the volume changes each step
        val deltaVolume = MIN_VOLUME / numberOfSteps.toFloat()

        //Create a new Timer and Timer task to run the fading outside the main UI thread
        timerInService = Timer(true)
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                fadeOutStep(deltaVolume) //Do a fade step
                //Cancel and Purge the Timer if the desired volume has been reached
                if (volume <= 0f) {
                    mediaPlayer.setVolume(1f, 1f)
                    timerInService.cancel()
                    timerInService.purge()
                }
            }
        }

        timerInService.schedule(timerTask, FADE_INTERVAL.toLong(), FADE_INTERVAL.toLong())
    }

    private fun fadeOutStep(deltaVolume: Float) {
        mediaPlayer.setVolume(volume, volume)
        volume -= deltaVolume
    }

    private fun startFadeIn() {
        volume = 0f
        val FADE_DURATION = 15000 //The duration of the fade
        //The amount of time between volume changes. The smaller this is, the smoother the fade
        val FADE_INTERVAL = 100
        val MAX_VOLUME = 1 //The volume will increase from 0 to 1
        val numberOfSteps = FADE_DURATION / FADE_INTERVAL //Calculate the number of fade steps
        //Calculate by how much the volume changes each step
        val deltaVolume = MAX_VOLUME / numberOfSteps.toFloat()

        //Create a new Timer and Timer task to run the fading outside the main UI thread
        timerInService = Timer(true)
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                fadeInStep(deltaVolume) //Do a fade step
                //Cancel and Purge the Timer if the desired volume has been reached
                if (volume >= 1f) {
                    timerInService.cancel()
                    timerInService.purge()
                    startFadeOut()
                }
            }
        }

        timerInService.schedule(timerTask, FADE_INTERVAL.toLong(), FADE_INTERVAL.toLong())
    }

    private fun fadeInStep(deltaVolume: Float) {
        mediaPlayer.setVolume(volume, volume)
        volume += deltaVolume
    }

    private fun updateActivity() {
        Log.d("BR21", "Broadcasting message")
        val intent = Intent("nPlay_Events")

        // You can also include some extra data.
        intent.putExtra("song_index", songIndex)

        Log.d("BR21", "sending")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }/*{

        val messengerSongInfo = sendIntent.getParcelableExtra("songInfo") as Messenger?
        val messageSongInfo: Message = Message.obtain(null, songIndex)

        try {
            messengerSongInfo!!.send(messageSongInfo)
        } catch (exception: RemoteException) {
            exception.printStackTrace()
        }


        val messengerSeekInfo = sendIntent.getParcelableExtra("seekInfo") as Messenger?
        val messageSeekInfo: Message = Message.obtain(null, mediaPlayer.duration/1000)

        try {
            messengerSeekInfo!!.send(messageSeekInfo)
        } catch (exception: RemoteException) {
            exception.printStackTrace()
        }

        mSeekUpdateTimer = Timer()
        mSeekUpdateTimer.schedule(object : TimerTask() {
            override fun run() {

                if (mediaPlayer != null)
                try {
                        val messengerSeekInfo =
                            sendIntent.getParcelableExtra("seekInfo") as Messenger?
                        val messageSeekInfo: Message =
                            Message.obtain(null, mediaPlayer.currentPosition / 1000)
                        messengerSeekInfo!!.send(messageSeekInfo)

                } catch (exception: IllegalStateException) {
                    exception.printStackTrace()
                }        // do your periodic task
            }
        }, 0, 1000)
    }*/



    override fun onCompletion(p0: MediaPlayer?) {

        if (songsUrlList.size > 1) {
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
                //    startFadeIn()
                saveIndex(songIndex)
            }

            mediaPlayer.setOnCompletionListener(this)
            mediaPlayer.setOnErrorListener(this)
        }

        updateActivity()
    } else {
            super.stopSelf()
            MainActivity.fabPlayAlbum.setImageResource(android.R.drawable.ic_media_play)
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