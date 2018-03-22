package com.autotestlab.gridactivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.autotestlab.gridactivity.QK_AutoTestLab_AudioRecord.AudioRecord;
import com.autotestlab.gridactivity.QK_AutoTestLab_BT.BTActivity;
import com.autotestlab.gridactivity.QK_AutoTestLab_CameraStress.CameraAct;
import com.autotestlab.gridactivity.QK_AutoTestLab_Clock.AlarmActivity;
import com.autotestlab.gridactivity.QK_AutoTestLab_MSG.MsgActivity;
import com.autotestlab.gridactivity.QK_AutoTestLab_Phone.PhoneMainActivity;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.SqliteHelper;
import com.autotestlab.gridactivity.QK_AutoTestLab_VedioRecord.VedioRecord;
import com.autotestlab.gridactivity.QK_AutoTestLab_WifiStress.WifiStressActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public String TAG = getClass().getSimpleName();
    private List<Map<String, Object>> data_list;
    private SimpleAdapter sim_adapter;
    private int[] icon = {R.drawable.camera, R.drawable.soundrecorder, R.drawable.videocamera,
            R.drawable.bluetooth,R.drawable.wifi,R.drawable.sms,R.drawable.clock,R.drawable.phone};
    private String[] iconName = {"相机", "录音", "视频", "蓝牙","WIFI","MSG","Clock","Phone"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity_layout);

        GridView gridView = (GridView) findViewById(R.id.mygridview);

        data_list = new ArrayList<Map<String, Object>>();
        getData();

        // 新建适配器
        String[] from = {"imageItem", "image_item"};
        int[] to = {R.id.image_item, R.id.text_item};
        sim_adapter = new SimpleAdapter(this, data_list, R.layout.simpleadapter_layout,
                from, to);
        // 添加并且显示
        gridView.setAdapter(sim_adapter);
        // 添加消息处理
        gridView.setOnItemClickListener(new ItemClickListener());

        Log.e(TAG, "oncreate");

    }


    @Override
    protected void onStart() {
        // 启动logcat的服务
        startService(new Intent(getApplicationContext(), LogService.class));
        super.onStart();
    }

    public List<Map<String, Object>> getData() {
        // cion和iconName的长度是相同的，这里任选其一都可以
        for (int i = 0; i < icon.length; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("imageItem", icon[i]);
            map.put("image_item", iconName[i]);
            data_list.add(map);
        }

        return data_list;
    }


    class ItemClickListener implements AdapterView.OnItemClickListener {


        @SuppressWarnings("unchecked")
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int postion,
                                long id) {
            // TODO Auto-generated method stub
            HashMap<String, Object> item = (HashMap<String, Object>) parent
                    .getItemAtPosition(postion);
            Log.e(TAG, "id-->" + id + ",postion-->" + postion);

            if (id == 0) {
                Log.e(TAG, "postion==" + postion + "|  |id==  " + id);

                startActivity(new Intent(getApplicationContext(), CameraAct.class));
                Toast.makeText(MainActivity.this, "相机压力测试", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "进入到相机压力测试");

            }
            if (id == 1) {
                Intent intent = new Intent(getApplicationContext(),
                        AudioRecord.class);
                startActivity(intent);

                Log.e(TAG, "进入到录音压力测试");

            }
            if (id == 2) {
                startActivity(new Intent(MainActivity.this, VedioRecord.class));

                Log.e(TAG, "进入到录像压力测试");
            }
            if (id == 3) {
                startActivity(new Intent(MainActivity.this, BTActivity.class));

                Log.e(TAG, "进入到BT压力测试");
            }

            if (id == 4) {
                startActivity(new Intent(MainActivity.this, WifiStressActivity.class));

                Log.e(TAG, "进入到WIFI压力测试");
            }
            if (id == 5) {
                startActivity(new Intent(MainActivity.this, MsgActivity.class));

                Log.e(TAG, "进入到MSG压力测试");
            }

            if (id == 6) {
                startActivity(new Intent(MainActivity.this, AlarmActivity.class));

                Log.e(TAG, "进入到CLOCK压力测试");
            }

            if (id == 7) {
                startActivity(new Intent(MainActivity.this, PhoneMainActivity.class));

                Log.e(TAG, "进入到Phone压力测试");
            }

        }


    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
// TODO Auto-generated method stub
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            dialog();
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
         builder.setMessage("确认退出吗?");
        builder.setTitle("提示");
         builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
             @Override
            public void onClick(DialogInterface dialog, int which) {
                 dialog.dismiss();

                 // TODO: 2016/8/1 数据库的关闭
                 SqliteHelper sqliteHelper = new SqliteHelper(MainActivity.this);
                 sqliteHelper.close();
                 Log.e(TAG, " SqliteHelper  close ");
                 MainActivity.this.finish();
                 }
             });
         builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialog, int which) {
                 dialog.dismiss();
                 }
             });
         builder.create().show();
         }


    @Override
    protected void onDestroy() {

        Log.e(TAG, "ondestroy");
        MainActivity.this.finish();
        System.exit(0);
        super.onDestroy();
    }
}

