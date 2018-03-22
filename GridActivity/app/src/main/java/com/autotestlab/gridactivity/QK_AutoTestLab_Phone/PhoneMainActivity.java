package com.autotestlab.gridactivity.QK_AutoTestLab_Phone;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.DatabaseDump;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.MytabOpera;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.SqliteHelper;
import com.autotestlab.gridactivity.R;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class PhoneMainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = getClass().getSimpleName();
    private EditText phonenumber = null;
    private EditText time = null;

    private TextView resultText = null;
    private ProgressBar progressBar = null;

    private Button startBtn = null;
    private Button stopBtn = null;

    SqliteHelper helper;
    MytabOpera mytabOpera;
    /**
     * 成功 以及失败的次数总和
     */
    int succCount = 0;
    int failCount = 0;

    /**
     * 进度条计数器
     **/
    private int intcount = 0;

    /**
     * thread handler
     */
    Handler threadHandler;
    /**
     * 总计次数
     */
    private int countall = 0;

    Context mContext = PhoneMainActivity.this;

    // 自定义Handler信息代码，用以作为识别事件处理
    protected static final int GUI_NOTIFIER = 0;
    protected static final int GUI_STOP = 1;
    protected static final int GUI_SUCCESS = 2;

    private String phoneName;

    private Spinner SpinnerChose = null;

    ArrayAdapter<String> adapter;

    private List<String> list = new ArrayList<String>();

    int typeChose = 0;

    //ITelephony iTelephony = getITelephony();

    Handler msgHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case GUI_NOTIFIER:

                    progressBar.setProgress(numberCoun - countall+1);
                    String textString = msg.obj.toString();
                    Log.i(TAG, "GUI_STOP_NOTIFIER 的intcount-->" + intcount);
                    resultText.setText(textString);


                    resultText.setTextColor(Color.BLUE);
                    resultText.setGravity(Gravity.CENTER);

                    mytabOpera = new MytabOpera(helper.getWritableDatabase());
                    mytabOpera.insert(phoneName, TAG, Build.VERSION.INCREMENTAL, countall - intcount,
                            "SUCCESS");
                    Log.e(TAG, "添加数据到sqldb中");

                    DatabaseDump databaseDump1 = new DatabaseDump(helper.getWritableDatabase());
                    databaseDump1.writeExcel(SqliteHelper.TB_NAME);


                    break;


                case GUI_SUCCESS:

                    progressBar.setProgress(numberCoun - countall+1);

                    String textString1 = msg.obj.toString();
                    resultText.setText(textString1);

                    mytabOpera = new MytabOpera(helper.getWritableDatabase());
                    mytabOpera.insert(phoneName, TAG, Build.VERSION.INCREMENTAL, countall - intcount, "SUCCESS");
                    Log.e(TAG, "添加数据到sqldb中");

                    DatabaseDump databaseDump = new DatabaseDump(helper.getWritableDatabase());
                    databaseDump.writeExcel(SqliteHelper.TB_NAME);


                    resultText.setTextColor(Color.GREEN);
                    resultText.setGravity(Gravity.CENTER);
//
//                    stopButton.setEnabled(false);
//                    startButton.setEnabled(true);

                    Log.e(TAG, "执行完成,导出数据库到内置存储中");
                    Toast.makeText(mContext, "测试完成,正从数据库中导出到EXCEL表格中,请稍等",
                            Toast.LENGTH_SHORT)
                            .show();

                    break;


                case GUI_STOP:
//                    threadHandler.removeCallbacks(autoRunnable);
//                    resultTextView.setText("停止拍照");
//                    resultTextView.setGravity(Gravity.CENTER);
//                    Log.v(TAG, "Runnable线程停止");
//                    threadHandler.removeCallbacks(startRunnable);

                    break;
            }
        }

        ;


    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_main_xml);
        // TODO: 2016/10/28  init
        init();
        phoneName = Build.MODEL;
        helper = new SqliteHelper(mContext);
        mytabOpera = new MytabOpera(helper.getWritableDatabase());
    }

    private void init() {
        SpinnerChose = (Spinner) findViewById(R.id.spinner2Phone);
        list.add("拨打电话");
        list.add("接听电话");
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        SpinnerChose.setAdapter(adapter);

        SpinnerChose.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                typeChose = position;
                Log.e(TAG, "您选择的是：" + adapter.getItem(position) + "typechose-->>>>" + typeChose);
                if (typeChose == 1) {
                    startActivity(new Intent(PhoneMainActivity.this
                            , AnswerCallActivity.class));
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.e(TAG, "您没有选择");
            }
        });

        phonenumber = (EditText) findViewById(R.id._phoneEdit);
        time = (EditText) findViewById(R.id._timeEditphone);

        resultText = (TextView) findViewById(R.id._resultPhone);
        // resultText.setText("请选择接听电话,拨打电话功能暂时未开放");

        progressBar = (ProgressBar) findViewById(R.id._progressBarPhone);
        this.startBtn = (Button) findViewById(R.id._phoneStart);
        startBtn.setOnClickListener(this);
        // startBtn.setEnabled(false);

        this.stopBtn = (Button) findViewById(R.id._phoneStop);
        stopBtn.setOnClickListener(this);
        stopBtn.setEnabled(false);


    }


    protected void onStop() {
        //  unregisterReceiver(mybroadcast);
        super.onStop();
    }


    String getPhoneNum;
    String getTime;

    //次数
    int DELAY;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id._phoneStart:
                succCount = 0;
                failCount = 0;

                if (phonenumber.equals(" ") || phonenumber.length() == 0) {

                    Toast.makeText(mContext, "请输入正确的电话号码", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    getPhoneNum = phonenumber.getText().toString();

                    Log.d(TAG, "Phone的number-->" + getPhoneNum);
                }

                getTime = time.getText().toString();
                if (getTime.equals(" ") || getTime.length() == 0) {

                    DELAY = 1;
                    numberCoun = DELAY;
                } else {
                    DELAY = Integer.parseInt(getTime);
                    numberCoun = DELAY;
                    Log.i(TAG, "DELAY:" + DELAY);

                }
                progressBar.setMax(numberCoun);
                msgHandler.postDelayed(dialHanl, 2 * 1000L);


                break;
            case R.id._phoneStop:

                msgHandler.removeCallbacks(dialHanl);
                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);

                break;
        }

    }

    int numberCoun = 0;
    // TODO: 2016/11/1 拨打电话的线程处理
    Runnable dialHanl = new Runnable() {
        @Override
        public void run() {
            dialNumber();

            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(1000);
                    Log.e(TAG, "通话时间--->" + i);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            /**
             这里添加一个挂断的电话*/
//            ITelephony iTelephony = getITelephony1();
//            try {
//                iTelephony.endCall();
//                succCount++;
//            } catch (RemoteException e) {
//                e.printStackTrace();
//                failCount++;
//            }


            countall = DELAY;
            intcount = DELAY--;
            if (intcount > 1) {


                Message msg = new Message();
                msg.what = GUI_NOTIFIER;
                msg.obj = "总执行次数:" + numberCoun + " 当前执行到:" + (numberCoun - countall+1) +
                        "\n成功次数:" + succCount + ";失败次数:" + failCount;
                msgHandler.sendMessage(msg);

                msgHandler.removeCallbacks(dialHanl);
                stopBtn.setEnabled(true);
                startBtn.setEnabled(false);

                msgHandler.postDelayed(dialHanl, 3 * 1000L);

            } else {
                progressBar.setProgress(numberCoun - countall);
                Message msg = new Message();
                msg.what = GUI_SUCCESS;
                msg.obj = "总执行次数:" + numberCoun + " 当前执行到:" + (numberCoun - countall+1) +
                        "\n成功次数:" + succCount + ";失败次数:" + failCount;
                msgHandler.sendMessage(msg);

                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);


            }


        }
    };


    // TODO: 2016/11/1 拨打电话
    private void dialNumber() {

        Intent intent = new Intent(Intent.ACTION_CALL);
        Uri data = Uri.parse("tel:" + getPhoneNum);
        intent.setData(data);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        startActivity(intent);
    }


    /**
     * @param
     * @return
     */
    public ITelephony getITelephony1() {
        TelephonyManager mTelephonyManager = (TelephonyManager)
                getSystemService(TELEPHONY_SERVICE);
        Class c = TelephonyManager.class;
        Method getITelephonyMethod = null;
        try {
            getITelephonyMethod = c.getDeclaredMethod("getITelephony",
                    (Class[]) null); // 获取声明的方法
            getITelephonyMethod.setAccessible(true);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        ITelephony iTelephony = null;
        try {
            iTelephony = (ITelephony) getITelephonyMethod.invoke(
                    mTelephonyManager, (Object[]) null); // 获取实例
            return iTelephony;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return iTelephony;
    }


}
