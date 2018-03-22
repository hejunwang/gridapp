package com.autotestlab.gridactivity.QK_AutoTestLab_MSG;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.DatabaseDump;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.MytabOpera;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.SqliteHelper;
import com.autotestlab.gridactivity.R;

import java.util.ArrayList;
import java.util.List;

public class MsgActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView msgResult = null;

    private String tag = getClass().getSimpleName();

    private Spinner msgTypeChooseSpin = null;

    private Button msgStartBtn = null;
    private Button msgStopBtn = null;

    private EditText msgPhoneNum = null;
    private EditText msgTimeNum = null;


    SmsManager smsManager;
    private List<String> list = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    /*电话号码*/
    String phoneNume;

    /*执行次数*/
    int time;

    // TODO: 2016/8/15 信息发送广播消息,接收消息
    MsgSendBrodcast msgsendBrod;
    SMSDelivereBroadReceiver smsdeliver;


    // TODO: 2016/8/15 类型选择
    int typeChose = 0;

    // TODO: 2016/8/15 成功失败的次数计数
    public static int successCount = 0;
    public static int failCount = 0;

    //完成
    public static final int GUI_FINISH = 1;
    //继续执行
    public static final int GUI_DOING = 0;

    MytabOpera mytabOpera;
    SqliteHelper helper;


    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case GUI_DOING:

                    String res = "\n总执行次数:" + allTime + " ;当前执行到:" + (allTime - time) +
                            ";成功次数:"+successCount+";失败次数:"+failCount;

                    //  msgResult.append(sysinfo);
                    msgResult.append(res);

                    msgStartBtn.setEnabled(false);
                    msgStopBtn.setEnabled(true);

                    mytabOpera = new MytabOpera(helper.getWritableDatabase());
                    mytabOpera.insert(Build.MODEL, tag, Build.VERSION.INCREMENTAL, allTime - time,
                            "SUCCESS");
                    Log.e(tag, "添加数据到sqldb中");
                    break;

                case GUI_FINISH:

                    String res1 ="\n总执行次数:" + allTime + " ;当前执行到:" + (allTime - time) +
                            ";成功次数:"+successCount+";失败次数:"+failCount;

                    // msgResult.append(sysinfo);
                    msgResult.append(res1);

                    msgStartBtn.setEnabled(true);
                    msgStopBtn.setEnabled(false);


                    mytabOpera = new MytabOpera(helper.getWritableDatabase());
                    mytabOpera.insert(Build.MODEL, tag, Build.VERSION.INCREMENTAL, allTime - time,
                            "SUCCESS");
                    Log.e(tag, "添加数据到sqldb中");

                    DatabaseDump databaseDump = new DatabaseDump(helper.getWritableDatabase());
                    databaseDump.writeExcel(SqliteHelper.TB_NAME);
                    Log.e(tag, "导出到excel表格中");

                    unregisterReceiver(msgsendBrod);
                    unregisterReceiver(smsdeliver);

                    break;

            }
        }
    };

    String sysinfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_msg);

        initfindview();

        smsManager = SmsManager.getDefault();

        // TODO: 2016/8/15 数据库操作
        helper = new SqliteHelper(MsgActivity.this);
        mytabOpera = new MytabOpera(helper.getWritableDatabase());

        msgsendBrod = new MsgSendBrodcast();
        smsdeliver = new SMSDelivereBroadReceiver();

        Log.e(tag, "手机型号: " + android.os.Build.MODEL + ",\nSDK版本:"
                + android.os.Build.VERSION.SDK + ",\n系统版本:"
                + Build.VERSION.INCREMENTAL);
        sysinfo = "手机型号: " + android.os.Build.MODEL + ",\nSDK版本:"
                + android.os.Build.VERSION.SDK + ",\n系统版本:"
                + Build.VERSION.INCREMENTAL;
        msgResult.setText(sysinfo);

    }

    // TODO: 2016/8/15 初始化控件
    private void initfindview() {
        this.msgPhoneNum = (EditText) findViewById(R.id._MsgPhone);

        this.msgTimeNum = (EditText) findViewById(R.id._MsgTime);

        this.msgStartBtn = (Button) findViewById(R.id._MsgStar);
        this.msgStartBtn.setOnClickListener(this);

        this.msgStopBtn = (Button) findViewById(R.id._MsgStop);
        this.msgStopBtn.setOnClickListener(this);
        msgStopBtn.setEnabled(false);

        this.msgResult = (TextView) findViewById(R.id._MsgResult);
        msgResult.setMovementMethod(ScrollingMovementMethod.getInstance());

        list.add("短信");
        //  list.add("彩信");

        this.msgTypeChooseSpin = (Spinner) findViewById(R.id._MsgSpinner);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        msgTypeChooseSpin.setAdapter(adapter);

        msgTypeChooseSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                typeChose = position;
                Log.e(tag, "您选择的是：" + adapter.getItem(position) + "typechose-->>>>" + typeChose);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.e(tag, "您没有选择");
            }
        });
    }

    /**
     * 中间值,计数使用
     */
    int timeCount;

    // TODO: 2016/8/15 总次数
    int allTime = 0;

    boolean isRunning = true;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id._MsgStar:

                successCount = 0;
                failCount = 0;

                this.phoneNume = msgPhoneNum.getText().toString();
                if ((phoneNume.length() > 11) || (phoneNume.length() < 0)) {
                    Toast.makeText(MsgActivity.this, "输入普通的号码可以显示发送成功与否的结果哦",
                            Toast.LENGTH_SHORT).show();

                    return;
                }

                String msgtime = msgTimeNum.getText().toString();
                if ((msgtime.length() < 0) || (msgtime.equals("0"))) {
                    Toast.makeText(MsgActivity.this, "输入正确的次数", Toast.LENGTH_SHORT).show();
                    return;

                } else {
                    time = Integer.parseInt(msgtime);
                    Log.e(tag, "time-->" + time);
                    allTime = time;
                }

                msgStopBtn.setEnabled(true);
                msgStartBtn.setEnabled(false);

                handler.postDelayed(sendMsgRun, 3000L);

                break;

            case R.id._MsgStop:

                handler.removeCallbacks(sendMsgRun);
                isRunning = false;

                unregisterReceiver(msgsendBrod);
                unregisterReceiver(smsdeliver);

                msgStartBtn.setEnabled(true);
                msgStopBtn.setEnabled(false);


                DatabaseDump databaseDump = new DatabaseDump(helper.getWritableDatabase());
                databaseDump.writeExcel(SqliteHelper.TB_NAME);
                Log.e(tag, "导出到excel表格中");


                break;


        }
    }

    // TODO: 2016/8/15  send  message
    private void sendmsg() {

        String msg = "101";      //电信查询实时话费
        String msg1 = "701";      //电信查询已开通套餐
        String ye = "ye";
        Intent sentIntent = new Intent("MSG_SEND_ACTION");
        Intent deliverIntent = new Intent("SMS_DELIVERED_ACTION");

        PendingIntent sendPending = PendingIntent.getBroadcast(MsgActivity.this, 0, sentIntent, 0);
        registerReceiver(msgsendBrod, new IntentFilter("MSG_SEND_ACTION"));

        PendingIntent deliverPending = PendingIntent.getBroadcast(MsgActivity.this, 0, deliverIntent, 0);
        registerReceiver(smsdeliver, new IntentFilter("SMS_DELIVERED_ACTION"));

        smsManager.sendTextMessage(phoneNume, null, msg, sendPending, deliverPending);

        Log.e(tag, "send over ");

    }


    Runnable sendMsgRun = new Runnable() {
        @Override
        public void run() {
            if (typeChose == 0) {
                Log.e(tag, "短信发送");
                sendmsg();
            } else if (typeChose == 1) {
                Log.e(tag, "彩信发送");
                sendMMS();
            }


            time--;
            timeCount = time;
            time = timeCount;
            Log.e(tag, "timecount---->>>" + timeCount + ",time-->" + time);
            Message msg = new Message();
            if (time > 0) {

                msg.what = GUI_DOING;
//                msg.obj = "\n总执行次数:" + allTime + " ;当前执行到:" + (allTime - time) +
//                        ";成功次数:"+successCount+";失败次数:"+failCount;

                Log.e(tag, "\n总执行次数:" + allTime + " ;当前执行到:" + (allTime - time) +
                        ";成功次数:"+successCount+";失败次数:"+failCount);
                handler.sendMessage(msg);

                handler.removeCallbacks(sendMsgRun);

                msgStartBtn.setEnabled(false);
                msgStopBtn.setEnabled(true);


                handler.postDelayed(sendMsgRun, 5000L);
                Log.e(tag, "线程执行继续,延迟5S之后在执行线程");


            } else {

                msg.what = GUI_FINISH;
                handler.sendMessage(msg);

                handler.removeCallbacks(sendMsgRun);
                Log.e(tag, "线程执行完成");
            }

        }
    };

    // TODO: 2016/8/15 彩信发送
    private void sendMMS() {
    }


    @Override
    protected void onDestroy() {
        Log.e(tag, "onDestroy ");
        super.onDestroy();
    }


}
