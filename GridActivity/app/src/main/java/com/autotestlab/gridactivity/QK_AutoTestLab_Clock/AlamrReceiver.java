package com.autotestlab.gridactivity.QK_AutoTestLab_Clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AlamrReceiver extends BroadcastReceiver {
    public AlamrReceiver() {
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // throw new UnsupportedOperationException("Not yet implemented");
        Uri ringUri;

        if ("qiku.alert.start".equals(intent.getAction())) {
         //   String msg = intent.getStringExtra("msg");
            long time = System.currentTimeMillis();//long now = android.os.SystemClock.uptimeMillis();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date d1 = new Date(time);
            String t1 = format.format(d1);
            Log.e("闹钟时间到,广播接收当前时间>>>", t1);

            Toast.makeText(context, "闹钟时间到,广播闹钟接收到,启动铃声", Toast.LENGTH_LONG).show();

            context.startService(new Intent(context, MusicServer.class));
            Log.e("闹钟广播到,铃声响起启动音乐服务", t1);

        }

        Intent fanhuiint = new Intent();
        fanhuiint.setAction(AlarmActivity.ACTION_UPUI);
        context.sendBroadcast(fanhuiint);
        Log.e("AlamrReceiver", "广播接收后发一个回执给主activity");


    }
}
