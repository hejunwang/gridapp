package com.autotestlab.gridactivity.QK_AutoTestLab_Clock;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.util.Log;

public class MusicServer extends Service {
    MediaPlayer mediaPlayer = null ;

    public MusicServer() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        mediaPlayer=MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
      //  mediaPlayer.setLooping(true);
        mediaPlayer.start();
        Log.e("music server","onstart");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("music server","onDestroy");
        if (mediaPlayer!=null)
        {
            mediaPlayer.stop();
            mediaPlayer = null;
        }
    }
}
