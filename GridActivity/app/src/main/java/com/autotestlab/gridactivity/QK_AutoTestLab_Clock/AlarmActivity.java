package com.autotestlab.gridactivity.QK_AutoTestLab_Clock;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.DatabaseDump;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.MytabOpera;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.SqliteHelper;
import com.autotestlab.gridactivity.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class AlarmActivity extends AppCompatActivity implements View.OnClickListener {

    // TODO: 2016/8/22  hanler  msg what
    private static final int GUI_STOP = 0;
    private static final int GUI_FINSH = 1;
    private static final int GUI_DOING = 2;

    // TODO: 2016/8/22 action
    private static final String ALARM_BROAD = "qiku.alert.start";

    private TextView tv = null;
    private TextView resultTV = null;

    private Button btn_set = null;
    private Button btn_cel = null;

    private EditText timeET = null;
    private EditText delayET = null;

    /*context*/
    Context mContext = AlarmActivity.this;

    private Calendar c = null;

    public static String ACTION_UPUI = "action.upui";

    private String tag = getClass().getSimpleName();

    private MytabOpera mytabOpera;
    private SqliteHelper helper;

    /**handler message */

    Handler alarmHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case GUI_DOING:
                    String res = "\n总执行次数:" + middleTime + " 当前执行到:" + (middleTime - time) +
                            "\n成功次数" + success + ";  失败次数:  " + fail;

                    tv.setText(res);
                    Log.e(tag, "doing-->"+res);
                    btn_cel.setEnabled(true);
                    btn_set.setEnabled(false);

                    mytabOpera = new MytabOpera(helper.getWritableDatabase());
                    mytabOpera.insert(Build.MODEL, tag, Build.VERSION.INCREMENTAL, middleTime - time,
                            "SUCCESS");
                    Log.e(tag, "添加数据到sqldb中");

                    break;


                case GUI_FINSH:

                    String res1 = "\n总执行次数:" + middleTime + " 当前执行到:" + (middleTime - time) +
                            "\n成功次数" + success + ";  失败次数:  " + fail;

                    tv.setText(res1);
                    resultTV.append("本次闹钟测试结束");
                    btn_set.setEnabled(true);
                    btn_cel.setEnabled(false);

                    mytabOpera = new MytabOpera(helper.getWritableDatabase());
                    mytabOpera.insert(Build.MODEL, tag, Build.VERSION.INCREMENTAL, middleTime - time,
                            "SUCCESS");


                    DatabaseDump databaseDump = new DatabaseDump(helper.getWritableDatabase());
                    databaseDump.writeExcel(SqliteHelper.TB_NAME);
                    Log.e(tag, "添加数据到sqldb中,导出到excel表格中");
                    Log.e(tag, "GUI_FINSH-->"+res1);
                    break;
                case GUI_STOP:
                    String stopstring = "\n总执行次数:" + middleTime + " 当前执行到:" + (middleTime - time) +
                            "\n当前状态 >>>>停止";
                    tv.setText(stopstring);
                    btn_set.setEnabled(true);
                    btn_cel.setEnabled(false);
                    Log.e(tag, "GUI_STOP-->"+stopstring);

                    break;
                default:

                    break;

            }
        }
    };


    AlarmManager am;
    PendingIntent pi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        findview();

        // TODO: 2016/8/19 内部类广播使用代码注册
        regist();

        helper = new SqliteHelper(mContext);
        mytabOpera = new MytabOpera(helper.getWritableDatabase());

        am = (AlarmManager) getSystemService(Activity.ALARM_SERVICE);

        resultTV.setGravity(Gravity.BOTTOM);
        resultTV.setMovementMethod(ScrollingMovementMethod.getInstance());

    }

    // TODO: 2016/8/19 发送闹钟的广播
    private void sendAlarmBroad() {

        Log.e(tag, "开始发送广播");
        resultTV.append("\n开始发送广播");

        Intent intent1 = new Intent(AlarmActivity.this, AlamrReceiver.class);
        intent1.setAction(ALARM_BROAD);
        pi = PendingIntent.getBroadcast(AlarmActivity.this, 0, intent1,
                PendingIntent.FLAG_CANCEL_CURRENT);

        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.SECOND, delaytime);


        am.setExact(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
        Log.e(tag, "闹钟设定时间--->" + delaytime + "s");

        startTime = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d1 = new Date(startTime);
        String t1 = format.format(d1);

        Log.e(tag, "广播开始时间---->" + t1);
      //  resultTV.append("\n广播开始时间--->"+t1);

    }


    // TODO: 2016/8/19 取消闹钟广播
    private void cancleAlarmBroad() {

         am.cancel(pi);

        stopService(new Intent(this, MusicServer.class));
        Log.e(tag, "cancleAlarmBroad && 关闭musicserver");
        resultTV.append("\n闹铃音乐关闭");

    }


    // TODO: 2016/8/22 开始时间和结束时间
    long startTime;
    long endTime;


    // TODO: 2016/8/19 控件
    private void findview() {
        c = Calendar.getInstance();

        tv = (TextView) this.findViewById(R.id._alarmtoast);
        resultTV = (TextView) findViewById(R.id._alarmResult);

        timeET = (EditText) findViewById(R.id._alarmTime);
        delayET = (EditText) findViewById(R.id._alarmDelay);

        btn_set = (Button) this.findViewById(R.id._AlarmStrat);
        btn_set.setOnClickListener(this);

        btn_cel = (Button) this.findViewById(R.id._AlarmStop);
        btn_cel.setOnClickListener(this);
        btn_cel.setEnabled(false);

    }

    // TODO: 2016/8/19 内部广播,只能进行代码注册, 不能使用静态注册
    private void regist() {

        IntentFilter intfilter = new IntentFilter();
        intfilter.addAction(ACTION_UPUI);
        registerReceiver(testbroad, intfilter);
        Log.e(tag, "register 内部广播类");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*注销广播类*/
        unregisterReceiver(testbroad);
    }


    // TODO: 2016/8/19  内部类只能通过代码注册registerReceiver(),不能在.xml文件中注册
    BroadcastReceiver testbroad = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (ACTION_UPUI.equals(intent.getAction())) {

                endTime = System.currentTimeMillis();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date d1 = new Date(endTime);
                String t1 = format.format(d1);

                Log.e(tag, "广播回传时间>>>>" + t1);
                resultTV.append("\n广播已经接收到了");

                success++;

            }

        }
    };

    /*次数*/
    int time;
    /*计数*/
    int timeCount;
    /*中间数*/
    int middleTime;
    /*间隔时间*/
    int delaytime;
    /*成功次数*/
    int success;
    /*失败次数*/
    int fail;


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id._AlarmStrat:       // TODO: 2016/8/22 点击开始后
                Log.e(tag, "alarmstart");

                tv.setText(" ");
                resultTV.setText(" ");
                success = 0;
                fail = 0;

                btn_set.setEnabled(false);
                btn_cel.setEnabled(true);

                String stringtime = timeET.getText().toString();
                String stringdelay = delayET.getText().toString();

                if ((stringtime.length() < 0) || (stringtime.equals(" "))) {
                    Toast.makeText(mContext, "请输入正确的数值", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    middleTime = time = Integer.parseInt(stringtime);
                    Log.e(tag, "time>>" + time);
                }

                if ((stringdelay.length() < 0) || (stringdelay.equals(" "))) {
                    Toast.makeText(mContext, "请输入正确的数值", Toast.LENGTH_SHORT).show();
                    return;
                } else {

                    delaytime = Integer.parseInt(stringdelay);
                    if (delaytime < 20) {
                        delayET.setText("20");
                        Log.e(tag, "delaytime小于 20S>>,最低设定20");
                        delaytime = 20;
                    }
                    Log.e(tag, "delaytime>>" + delaytime);
                }

                String str = "执行次数:" + time + ",间隔时间:" + delaytime + "s\n";
                Log.e(tag, str);
                resultTV.setText(str);


                alarmHandler.postDelayed(alarmRun,2000L);


                break;

            case R.id._AlarmStop:    //点击停止之后

                //alarmHandler.post(alarmStop);
                am.cancel(pi);

                stopService(new Intent(this, MusicServer.class));

                sendStop();
                resultTV.append("\n手动停止闹钟");

                alarmHandler.removeCallbacks(alarmRun);
                alarmHandler.removeCallbacks(alarmAutoFinsh);

                // TODO: 2016/8/22 在多次执行的过程中,出现点击停止的情况
                if (middleTime>1)
                {
                    alarmHandler.removeCallbacks(alarmDoing);
                }


                btn_set.setEnabled(true);
                btn_cel.setEnabled(false);

                break;


            default:


                break;
        }
    }

    /*开始广播*/
    Runnable alarmRun = new Runnable() {
        @Override
        public void run() {

            //  isRuning = true;
            Log.e(tag, "启动alarm线程");
            sendAlarmBroad();

            time--;
            System.out.println("time- 1->" + time);
            timeCount = time;
            time = timeCount;
            System.out.println("time- 2->" + time);
            resultTV.append("\n当前执行次数:"+time);
            if (time > 0) {
                alarmHandler.postDelayed(alarmDoing, (delaytime + 10) * 1000);
                resultTV.append("\n开始多次执行,闹钟响铃10S,取消闹钟");

                alarmHandler.postDelayed(alarmRun, (delaytime + 20) * 1000);
                resultTV.append("\n开始多次执行,闹钟关闭10S后 ,重新打开闹钟设置");

            } else {

                Log.e(tag, "闹钟设定的时间"+delaytime+"后开始" );
                resultTV.append("\n闹钟设定的时间"+delaytime+"后开始");

                alarmHandler.postDelayed(alarmAutoFinsh, (delaytime + 10) * 1000);
                Log.e(tag, "闹钟响铃后, 10S 以后alarmAutoFinsh闹钟关闭");

            }
        }


    };

    // TODO: 2016/8/22 自动结束
    Runnable alarmAutoFinsh = new Runnable() {
        @Override
        public void run() {
            cancleAlarmBroad();
            sendfinish();
        }
    };


    // TODO: 2016/8/22 多次执行广播
    Runnable alarmDoing = new Runnable() {
        @Override
        public void run() {
            Log.e(tag,"启动alarmdoing线程");
            am.cancel(pi);
            stopService(new Intent(AlarmActivity.this, MusicServer.class));

            resultTV.append("\n正在执行多次");

            Message message = new Message();
            message.what = GUI_DOING;
            alarmHandler.sendMessage(message);


        }
    };


    private void sendStop() {
        Message message = new Message();
        message.what = GUI_STOP;
        alarmHandler.sendMessage(message);
    }

    private void sendfinish() {
        Message message = new Message();
        message.what = GUI_FINSH;
        alarmHandler.sendMessage(message);
    }

}

