package com.autotestlab.gridactivity.QK_AutoTestLab_WifiStress;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.DatabaseDump;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.MytabOpera;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.SqliteHelper;
import com.autotestlab.gridactivity.R;

import java.util.ArrayList;
import java.util.List;

public class WifiStressActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int DOING = 0;
    private static final int FINISH = 1;
    private String TAG = getClass().getSimpleName();
    private Button wifiStartBtn = null;
    private Button wifiStopBtn = null;
    private EditText wifiTimeEt = null;
    private ListView wifiListView = null;

    private ProgressBar progressbar = null;
    private TextView wifiResult = null;

    private WifiAdmin mWifiAdmin;
    // 扫描结果列表
    private List<ScanResult> list;
    private ScanResult mScanResult;
    private StringBuffer sb = new StringBuffer();
    private Context mContext = WifiStressActivity.this;

    SqliteHelper helper;
    MytabOpera mytabopera;

    String phonename;
    ArrayAdapter<String> arrayAdapter;

    List<String> arraylist = new ArrayList<String>();

    WifiManager wifiManager ;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // super.handleMessage(msg);
            switch (msg.what) {
                case DOING:

                    String str = (String) msg.obj;
                    wifiResult.setText("总执行次数:" + time + "成功次数:" + succCount +
                            ";失败次数:" + failCount);
                    wifiResult.append(str);
                    wifiResult.setTextSize(14);

                    progressbar.setProgress(time - timeCount);

                    mytabopera = new MytabOpera(helper.getWritableDatabase());
                    mytabopera.insert(Build.MODEL, TAG,Build.VERSION.INCREMENTAL, timeCount, res);
                    Log.e(TAG, "添加数据到sqldb中");


                    break;

                case FINISH:

                    progressbar.setProgress(time - timeCount);

                    String obj = (String) msg.obj;
                    wifiResult.setText("总执行次数:" + time + "成功次数:" + succCount +
                            ";失败次数:" + failCount);
                    wifiResult.append(obj);
                    wifiResult.setGravity(Gravity.CENTER);

                    mytabopera = new MytabOpera(helper.getWritableDatabase());
                    mytabopera.insert(phonename, TAG,Build.VERSION.INCREMENTAL, timeint, res);
                    Log.e(TAG, "添加数据到sqldb中");

                    DatabaseDump databaseDump = new DatabaseDump(helper.getWritableDatabase());
                    databaseDump.writeExcel(SqliteHelper.TB_NAME);
                    Log.e(TAG, "执行完成,导出数据库到内置存储中");
//                    Toast.makeText(mContext, "测试完成,正从数据库中导出到EXCEL表格中,请稍等", Toast.LENGTH_SHORT)
//                            .show();

                    break;


            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_stress_layout);
        mWifiAdmin = new WifiAdmin(WifiStressActivity.this);
        mContext = WifiStressActivity.this;
        init();

        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arraylist);
        wifiListView.setAdapter(arrayAdapter);

    }

    private void init() {

        this.wifiTimeEt = (EditText) findViewById(R.id._wifitime);
        this.wifiTimeEt.setText("1");
        this.wifiTimeEt.setGravity(Gravity.CENTER);

        this.wifiStartBtn = (Button) findViewById(R.id._wifistart);
        this.wifiStartBtn.setOnClickListener(this);
        this.wifiStopBtn = (Button) findViewById(R.id._wifistop);
        this.wifiStopBtn.setOnClickListener(this);

        wifiStopBtn.setEnabled(false);

        this.wifiListView = (ListView) findViewById(R.id._wifilistView);


        this.progressbar = (ProgressBar) findViewById(R.id.wifi_Progress);


        this.wifiResult = (TextView) findViewById(R.id._wifiResult);
        wifiResult.setText("Wifi_Stress");
        wifiResult.setGravity(Gravity.CENTER);

        helper = new SqliteHelper(this);

        phonename = Build.MODEL;

        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

    }


    /*次数*/
    int time;
    /*次数计数*/
    int timeCount;
    int timeint;


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id._wifistart:

                progressbar.setProgress(0);
                arraylist.clear();
                arrayAdapter.notifyDataSetChanged();
                succCount = 0;
                failCount = 0;
                String str = wifiTimeEt.getText().toString();
                if ((str.length() == 0) || (str.equals("0"))) {
                    Toast.makeText(mContext, "请输入正确的数值", Toast.LENGTH_SHORT).show();
                    timeCount = 1;
                    return;
                } else {
                    timeCount = Integer.parseInt(str);
                    Log.e(TAG, "timecount--->" + timeCount);
                    time = timeCount;
                    Log.e(TAG, "time--->" + time);
                }

                progressbar.setMax(time);

                if (timeCount > 0) {
                    wifiStartBtn.setEnabled(false);
                    wifiStopBtn.setEnabled(true);

                    handler.postDelayed(openWifiRun, 3000L);

                }

                break;
            case R.id._wifistop:
                wifiStartBtn.setEnabled(true);
                wifiStopBtn.setEnabled(false);
                arraylist.clear();
                handler.removeCallbacks(openWifiRun);

                mytabopera = new MytabOpera(helper.getWritableDatabase());
                mytabopera.insert(phonename, TAG,Build.VERSION.INCREMENTAL, timeint, res);
                Log.e(TAG, "添加数据到sqldb中");

                DatabaseDump databaseDump = new DatabaseDump(helper.getWritableDatabase());
                databaseDump.writeExcel(SqliteHelper.TB_NAME);
                Log.e(TAG, "执行完成,导出数据库到内置存储中");



                break;

        }

    }

    /*成功失败次数*//**/
    int succCount = 0;
    int failCount = 0;
    /*结果*/
    String res = null;

    int recheckaga;

    Runnable openWifiRun = new Runnable() {
        // TODO: 2016/8/8 打开关闭wifi
        @Override
        public void run() {

            arraylist.clear();
            arrayAdapter.notifyDataSetChanged();
            /**
             * 检测当前WIFI的状态,打开wifi*/
            wifiOpera();

            /**进行扫描*/
            getAllNetWorkList();
            timeCount--;
            timeint = timeCount;
            timeCount = timeint;

            int recheck = mWifiAdmin.checkState();
            /**验证是否打开状态 是否打开或者是状态未知*/
            if ((recheck == 2) || (recheck == 3)) {
                succCount++;
                res = "SUCCESS";
                Log.e(TAG, "wifi recheck 状态 " + recheck);

            } else if ((recheck == 0) || (recheck == 1) || (recheck == 4)) {
                Toast.makeText(mContext, "WIFI_STATE_DISABLING", Toast.LENGTH_SHORT).show();
                failCount++;
                res = "FAID";
                Log.e(TAG, "wifi recheck 状态 !" + recheck);
            }

            Message msg = new Message();
            if (timeint > 0) {

                sleep(6);
                wifiManager.setWifiEnabled(false);
                sleep(6);
                clockReck();
                sleep(6);
                //   Log.e(TAG, "wifi关闭后的状态:-->" + mWifiAdmin.checkState());

                msg.what = DOING;
                msg.obj = "\n当前正在执行:" + timeCount + ";执行结果:" + res;
                handler.sendMessage(msg);

//                arraylist.clear();
//                arrayAdapter.notifyDataSetChanged();

                handler.removeCallbacks(openWifiRun);
                handler.postDelayed(openWifiRun, 5000L);
            } else {

                Log.e(TAG, "1次结束");
                sleep(6);

             //   mWifiAdmin.closeWifi();
                wifiManager.setWifiEnabled(false);
                sleep(6);

                clockReck();

                sleep(6);

                msg.what = FINISH;
                msg.obj = "\n当前正在执行:" + timeint + ";执行结果:" + res;
                handler.sendMessage(msg);

                wifiStartBtn.setEnabled(true);
                wifiStopBtn.setEnabled(false);


                Log.e(TAG, "1次执行结束");
                handler.removeCallbacks(openWifiRun);
            }


        }
    };

    /*closecheck*/
    private void clockReck() {
        recheckaga = mWifiAdmin.checkState();
        if ((recheckaga == 0) || (recheckaga == 1) || (recheckaga == 4)) {
            Toast.makeText(mContext, "WIFI_STATE_DISABLING", Toast.LENGTH_SHORT).show();
            //  failCount++;
            // res = "FAID";
            Log.e(TAG, "ClockReck  wifi recheck 已经关闭 !-->" + recheckaga);
        } else if ((recheckaga == 2) || (recheckaga == 3)) {
            //   mWifiAdmin.startScan();
            failCount++;
            res = "FAID";
            Log.e(TAG, "clockReckwifi当前的状态关闭失败!---->"+recheckaga);


        }


    }


    // TODO: 2016/8/8  wifi operation
    private void wifiOpera() {

        int check = mWifiAdmin.checkState();
        Log.e(TAG, "wifiOpera当前的状态:-->" + check);

        if ((check == 2) || (check == 3)) {
            //   mWifiAdmin.startScan();
            Log.e(TAG, "wifi当前的状态正在打开或 已经打开 !");


        } else if ((check == 0) || (check == 1) || (check == 4)) {
            Toast.makeText(mContext, "WIFI_STATE_DISABLING", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "wifi当前的状态正在关闭或者已经关闭 !");
            /**打开wifi*/
            mWifiAdmin.openWifi();
            sleep(6);

        }


    }

    /**
     * 延时
     **/
    private void sleep(int longtime) {
        try {
            Thread.sleep(longtime * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /*获取网络清单*/
    public void getAllNetWorkList() {
        // 每次点击扫描之前清空上一次的扫描结果
        if (sb != null) {
            sb = new StringBuffer();
        }
        //开始扫描网络
        mWifiAdmin.startScan();
        list = mWifiAdmin.getWifiList();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                //得到扫描结果
                mScanResult = list.get(i);
                sb = sb.append(mScanResult.BSSID + "  ").append(mScanResult.SSID + "   ")
                        .append(mScanResult.capabilities + "   ").append(mScanResult.frequency + "   ")
                        .append(mScanResult.level + "\n\n");
                String str = mScanResult.BSSID + "  " + mScanResult.SSID;
                if (arraylist.indexOf(str) == -1) {
                    arraylist.add(str);
                    Log.e(TAG, "扫描结果add");
                }

                arrayAdapter.notifyDataSetChanged();

            }
            Log.e(TAG, "扫描结果:\n" + sb);
            //     wifiScan.setText("扫描到的wifi网络：\n"+sb.toString());
        }
    }


    @Override
    protected void onDestroy() {

        WifiStressActivity.this.finish();
        super.onDestroy();
    }
}
