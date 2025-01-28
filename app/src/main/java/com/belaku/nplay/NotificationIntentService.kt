package com.belaku.nplay

import android.app.IntentService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast


class NotificationIntentService
/**
 * Creates an IntentService.  Invoked by your subclass's constructor.
 */
    : IntentService("notificationIntentService") {
    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        when (intent!!.action) {
            "notePlay" -> {
                val leftHandler = Handler(Looper.getMainLooper())
                leftHandler.post {
                    Toast.makeText(
                        baseContext,
                        "You clicked the Play button", Toast.LENGTH_LONG
                    ).show()
                    MusicService.mediaPlayer.pause()
                }
            }

            "notePause" -> {
                val rightHandler = Handler(Looper.getMainLooper())
                rightHandler.post {
                    Toast.makeText(
                        baseContext,
                        "You clicked the Pause button",
                        Toast.LENGTH_LONG
                    ).show()
                    MusicService.mediaPlayer.start()

                }
            }
        }
    }
}
