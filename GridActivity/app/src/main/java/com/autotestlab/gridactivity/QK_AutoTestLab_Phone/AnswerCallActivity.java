package com.autotestlab.gridactivity.QK_AutoTestLab_Phone;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.autotestlab.gridactivity.R;

import static com.autotestlab.gridactivity.R.id.phone_backupbtn;

/**
 * activit 主要是接收用户要接听的电话号码或者是黑名单号码,用户可以直接通过文件输入要过滤的号码或者是接听的号码
 * 而后将次号码设置到phonerservice中,之后进行拦截或者是接听.
 */
public class AnswerCallActivity extends AppCompatActivity implements View.OnClickListener {

    private Button answerBtn = null;

    private Button backUpBtn = null;

    public static String BROADCAST = "AUTOTESTLAB";

    MyBroadcastRec myBroadcastRec = new MyBroadcastRec();
    private EditText anCallEdit = null;
    private TextView resultTextview = null;
    private String tag = getClass().getSimpleName();
    private Context context = AnswerCallActivity.this;

    IService iservice = null;

    ServiceConnection serviceConnect = new serviceConnectlnmp();

    class serviceConnectlnmp implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iservice = (PhoneService.Binderlmpl) service;
            Log.e(tag, " onServiceConnected");
            try {
                Toast.makeText(context, service.getInterfaceDescriptor(), Toast.LENGTH_SHORT).show();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer_call);
        Log.e(tag, " answercall oncreate");
        initview();

        register();

        TelephonyManager mTelephonyManager =(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);




    }

    /**
     * 动态注册广播
     **/
    private void register() {

        Log.e(tag,"动态注册广播");
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST);
        registerReceiver(myBroadcastRec, filter);

    }


    private void initview() {
        Log.d(tag, " initview");
        anCallEdit = (EditText) findViewById(R.id.editText2);
        resultTextview = (TextView) findViewById(R.id.answercallResult);
        answerBtn = (Button) findViewById(R.id.phone_answerbtn);
        answerBtn.setOnClickListener(this);


        backUpBtn = (Button) findViewById(phone_backupbtn);
        backUpBtn.setOnClickListener(this);
        backUpBtn.setEnabled(false);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.phone_answerbtn:
                Log.d(tag, "auto answercall btn");

                String callnumber = anCallEdit.getText().toString();
                if (callnumber.equals("")) {
                    Toast.makeText(context, "请输入要自动接听的号码", Toast.LENGTH_SHORT).show();
                    return;
                }

                /**
                 * 启动后台的服务 ,接收从前台指定的号码
                 * */
                Intent intent = new Intent(context, PhoneService.class);
                intent.putExtra("phonenumber", anCallEdit.getText().toString());
                bindService(intent, serviceConnect, Context.BIND_AUTO_CREATE);

                answerBtn.setEnabled(false);
                backUpBtn.setEnabled(true);

                break;


            case phone_backupbtn:
                Log.d(tag, " phone_backupbtn");
                if (iservice != null) {
                    unbindService(serviceConnect);
                    stopService(new Intent(context, PhoneService.class));

                    Toast.makeText(context, "接听名单已经取消", Toast.LENGTH_SHORT).show();

                    unregisterReceiver(myBroadcastRec);

                    iservice = null;
                }

                answerBtn.setEnabled(true);
                backUpBtn.setEnabled(false);
                break;
        }

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (event.getAction() == KeyEvent.KEYCODE_BACK) {
            Log.e(tag, "key back");
            startActivity(new Intent(AnswerCallActivity.this, PhoneMainActivity.class));
            AnswerCallActivity.this.finish();
            //  return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    class MyBroadcastRec extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra("data");
            resultTextview.append(data + "\n");
            resultTextview.setGravity(Gravity.CENTER);

        }
    }


    @Override
    protected void onDestroy() {

      //  unregisterReceiver(myBroadcastRec);
        super.onDestroy();
    }
}
