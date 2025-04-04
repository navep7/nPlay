package com.parvatha.music;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;


public class NotifyActivityHandler extends Activity {
    public static final String PERFORM_NOTIFICATION_BUTTON = "perform_notification_button";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = (String) getIntent().getExtras().get("do_action");


        if (action != null) {
            if (action.equals("notePlay")) {
                Toast.makeText(this, "PlayNOTE", Toast.LENGTH_SHORT).show();
                MusicService.mediaPlayer1.pause();
                // for example play a music
            } else if (action.equals("notePause")) {
                // close current notification
                MusicService.mediaPlayer1.start();
            }
        }

        finish();
    }
}