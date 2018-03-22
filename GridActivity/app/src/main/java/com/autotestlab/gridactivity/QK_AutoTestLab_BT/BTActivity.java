package com.autotestlab.gridactivity.QK_AutoTestLab_BT;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.DatabaseDump;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.MytabOpera;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.SqliteHelper;
import com.autotestlab.gridactivity.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by hjw on 2016/7/20.
 */
public class BTActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = getClass().getSimpleName();

    private static final int DOING = 0;
    private static final int FINISH = 1;

    private EditText btNumber = null;
    private Button startBtn = null;
    private Button stopBtn = null;
    //  private ListView btListview = null;
    private TextView btResult = null;
    private TextView btResultBroad = null;


    private ProgressBar progressBar = null;
    //  private TextView listadd  = null;

    ArrayAdapter<String> arrayAdapter;
    List<String> devicelist = new ArrayList<String>();

    private BluetoothAdapter bluetoothAdapter;

    private boolean hasregister = false;

    Context mContext = BTActivity.this;
    Set<BluetoothDevice> bondDevices;


    /*手机名字*/
    private String phoneName = null;

    /*成功失败次数*//**/
    int succCount = 0;
    int failCount = 0;
    /*结果*/// TODO: 2016/7/28  结果
    String res = null;

    /*数据库的helper*/
    private SqliteHelper helper = null;

    /*数据库的操作*/
    private MytabOpera mytabOpera;

    Handler threadHandler = new Handler();

    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DOING:

                    String str = (String) msg.obj;
                    btResult.setText("总执行次数:" + time + "成功次数:" + succCount +
                            ";\n失败次数:" + failCount);
                    btResult.append(str);

                    progressBar.setProgress(time - timeint);

                    mytabOpera = new MytabOpera(helper.getWritableDatabase());
                    mytabOpera.insert(phoneName, TAG, Build.VERSION.INCREMENTAL, timeCount, res);
                    Log.e(TAG, "添加数据到sqldb中");

                    break;

                case FINISH:
                    progressBar.setProgress(time - timeint);
                    String obj = (String) msg.obj;
                    btResult.setText("总执行次数:" + time + "成功次数:" + succCount + ";\n失败次数:" + failCount);
                    btResult.append(obj);

                    mytabOpera = new MytabOpera(helper.getWritableDatabase());
                    mytabOpera.insert(phoneName, TAG, Build.VERSION.INCREMENTAL, timeCount, res);
                    Log.e(TAG, "添加数据到sqldb中");

                    DatabaseDump databaseDump = new DatabaseDump(helper.getWritableDatabase());
                    databaseDump.writeExcel(SqliteHelper.TB_NAME);
                    Log.e(TAG, "执行完成,导出数据库到内置存储中");

                    Toast.makeText(mContext, "测试完成,正从数据库中导出到EXCEL表格中,请稍等",
                            Toast.LENGTH_SHORT)
                            .show();
                    break;
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.btlayout);
        Log.e(TAG, "BTActivity oncreate");
        init();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, devicelist);
        //   btListview.setAdapter(arrayAdapter);

//        btListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(context, "暂时还没有补充哦", Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    @Override
    protected void onStart() {

        if (!hasregister) {
            hasregister = true;
            IntentFilter intentFilterFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            IntentFilter intentFilterEnd = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, intentFilterFound);
            registerReceiver(mReceiver, intentFilterEnd);
            Log.e(TAG, "register");
        }

        super.onStart();
    }


    // TODO: 2016/7/21  init findview
    private void init() {

        this.btNumber = (EditText) findViewById(R.id._BTnumber);
        btNumber.setText("1");
        btNumber.setGravity(Gravity.CENTER);

        this.startBtn = (Button) findViewById(R.id._btstartBtn);
        startBtn.setOnClickListener(this);

        this.stopBtn = (Button) findViewById(R.id._btstop);
        stopBtn.setOnClickListener(this);
        stopBtn.setEnabled(false);

        this.progressBar = (ProgressBar) findViewById(R.id.bt_progressbar);

        this.helper = new SqliteHelper(this);

        // TODO: 2016/8/1  手机名称
        phoneName = Build.MODEL;

        // TODO: 2016/11/23  广播结果
        this.btResultBroad = (TextView) findViewById(R.id.textView4);

        this.btResult = (TextView) findViewById(R.id._btresult);

        this.btResult.setText("测试结果");
        this.btResult.setGravity(Gravity.CENTER);

//        this.btListview = (ListView) findViewById(R.id._btlistView);
//        btListview.setVisibility(View.GONE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }


    //计数
    int time;
    //次数
    int timeCount;

    int pro;
    int timeint;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id._btstartBtn:
                Log.e(TAG, "start");

                succCount = 0;
                failCount = 0;
                progressBar.setProgress(0);

                if ((btNumber.getText().toString().length() == 0)
                        || (btNumber.getText().toString().equals(""))) {
                    timeCount = 1;
                    Toast.makeText(mContext, "请输入正确的数值", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    timeCount = Integer.parseInt(btNumber.getText().toString());
                    time = timeCount;
                }

                pro = timeCount;
                progressBar.setMax(pro);
                Log.e(TAG, "pro-->" + pro);

                if (timeCount > 0) {
                    startBtn.setEnabled(false);
                    stopBtn.setEnabled(true);
                    //   devicelist.clear();
                    handler.postDelayed(startRtrun, 3000);
                }
                break;

            case R.id._btstop:
                bluetoothAdapter.cancelDiscovery();
                bluetoothAdapter.disable();
                handler.removeCallbacks(startRtrun);


                // TODO: 2016/8/1 停止后 要把数据导入到数据库中
                DatabaseDump databaseDump = new DatabaseDump(helper.getWritableDatabase());
                databaseDump.writeExcel(SqliteHelper.TB_NAME);
                Log.e(TAG, "执行完成,导出数据库到内置存储中");

                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                Log.e(TAG, "close");
                break;
        }
    }

    int pre;


    int n = 1;
    Runnable btSearchRun = new Runnable() {
        @Override
        public void run() {
            //判断是否有权限
            if (ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //请求权限
                ActivityCompat.requestPermissions(BTActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        123);
            }
          //  btResultBroad.setText("");
            n++;
            if (n == 1) {
                if (Build.VERSION.SDK_INT >= 23) {
                    Log.e(TAG, "target>=23");
                    bluetoothAdapter.enable();
                }
            }
            if (n == 3) {
                if (bluetoothAdapter.isEnabled()) {
                    Log.e(TAG, " bluetoothAdapter.isEnabled  and then start discovery");
                    bluetoothAdapter.startDiscovery();


                    succCount++;
                    res = "SUC";

                } else {
                    failCount++;
                    res = "FAIL";
                }

            }
            pre++;

            timeCount--;
            timeint = timeCount;
            timeCount = timeint;

            Message msg = new Message();

            if (timeint > 0) {
                // TODO: 2016/10/20 延迟扫描功能
                for (int i = 0; i < 5; i++) {
                    try {
                        Thread.sleep(1000);
                        System.out.println("r1 -> i=" + i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                //取消扫描
                bluetoothAdapter.cancelDiscovery();
                Log.e(TAG, ";\n执行结果:" + res);

                msg.what = DOING;
                msg.obj = ";\n执行结果:" + res;
                handler.sendMessage(msg);

                handler.postDelayed(btSearchRun, 8000);
            } else {
                Log.e(TAG, "1次结束");
                bluetoothAdapter.cancelDiscovery();

                msg.what = FINISH;
                msg.obj = ";\n执行结果:" + res;
                handler.sendMessage(msg);


                stopBtn.setEnabled(false);
                startBtn.setEnabled(true);
               handler.postDelayed(stoprun, 8000);

            }


        }

    };


    Runnable stoprun = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(btSearchRun);
        }
    };



    /*开始执行线程*/
    Runnable startRtrun = new Runnable() {
        @Override
        public void run() {

            btResultBroad.setText("");
            // TODO: 2016/10/20 search
            search();

            pre++;


            timeCount--;
            timeint = timeCount;
            timeCount = timeint;

            Message msg = new Message();

            if (timeint > 0) {
                // TODO: 2016/10/20 延迟扫描功能
                for (int i = 0; i < 5; i++) {
                    try {
                        Thread.sleep(1000);
                        System.out.println("r1 -> i=" + i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                //取消扫描
                bluetoothAdapter.cancelDiscovery();
                Log.e(TAG, ";\n执行结果:" + res);

                msg.what = DOING;
                msg.obj = ";\n执行结果:" + res;
                handler.sendMessage(msg);

                handler.postDelayed(startRtrun, 8000);
            } else {
                Log.e(TAG, "1次结束");
                bluetoothAdapter.cancelDiscovery();

                msg.what = FINISH;
                msg.obj = ";\n执行结果:" + res;
                handler.sendMessage(msg);


                stopBtn.setEnabled(false);
                startBtn.setEnabled(true);

                handler.removeCallbacks(startRtrun);
            }


        }
    };

    /*搜索*/
    private void search() {

        // devicelist.clear();

        if (Build.VERSION.SDK_INT >= 23) {
            Log.e(TAG, "target>=23");
            bluetoothAdapter.enable();   //打开蓝牙,延迟三秒
            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Log.e(TAG, "target>=23 thread sleep 3s");
            if (bluetoothAdapter.isEnabled()) {
                Log.e(TAG, " bluetoothAdapter.isEnabled  and then start discovery");
                bluetoothAdapter.startDiscovery();
                for (int i = 0; i < 3; i++) {

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                succCount++;
                res = "SUC";

            } else {
                failCount++;
                res = "FAIL";
            }


            //判断是否有权限
            if (ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //请求权限
                ActivityCompat.requestPermissions(BTActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        123);
            }

        } else {
            bluetoothAdapter.enable();

            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            Log.e(TAG, "thread sleep 3s");
            bluetoothAdapter.startDiscovery();
        }
    }


    /*搜索
    * 这是第二次修改的线程使用 搜索
    * */
    private void search1() {



        if (Build.VERSION.SDK_INT >= 23) {
            Log.e(TAG, "target>=23");
            bluetoothAdapter.enable();   //打开蓝牙,延迟三秒
//            for (int i = 0; i < 3; i++) {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }

            Log.e(TAG, "target>=23 thread sleep 3s");
            if (bluetoothAdapter.isEnabled()) {
                Log.e(TAG, " bluetoothAdapter.isEnabled  and then start discovery");
                bluetoothAdapter.startDiscovery();
                for (int i = 0; i < 3; i++) {

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                succCount++;
                res = "SUC";

            } else {
                failCount++;
                res = "FAIL";
            }


            //判断是否有权限
            if (ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //请求权限
                ActivityCompat.requestPermissions(BTActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        123);
            }

        } else {
            bluetoothAdapter.enable();

            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            Log.e(TAG, "thread sleep 3s");
            bluetoothAdapter.startDiscovery();
        }
    }



    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            BluetoothDevice device = null;
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.e(TAG, "broadcast device found");

                    String str = "未配对: " + device.getName() + "--------MAC: " + device.getAddress() + "\n";
                    /*去掉重复*/
                    if (devicelist.indexOf(str) == -1) {
                        Log.e(TAG, "显示在textview上");
                        //// TODO: 2016/10/20 显示在textview上
                        btResultBroad.append(str);
                    }
                    // arrayAdapter.notifyDataSetChanged();
                }
                bondDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice bondev : bondDevices) {
                    String s = "已配对:" + bondev.getName() + " --------MAC:" + bondev.getAddress() + "\n";
                     /*去掉重复*/
                    if (devicelist.indexOf(s) == -1) {
                        btResultBroad.append(s);
                    }
                    arrayAdapter.notifyDataSetChanged();
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // TODO: 2016/7/27  取消扫描
                Log.e(TAG, "ACTION_DISCOVERY_FINISHED");
                bluetoothAdapter.disable();
                bluetoothAdapter.cancelDiscovery();

                //  arrayAdapter.notifyDataSetChanged();
            }


        }
    };

    @Override
    protected void onDestroy() {
        if (hasregister) {
            hasregister = false;
            Log.e(TAG, "unregister");
            unregisterReceiver(mReceiver);
        }

        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        BTActivity.this.finish();
        super.onDestroy();
    }


}
