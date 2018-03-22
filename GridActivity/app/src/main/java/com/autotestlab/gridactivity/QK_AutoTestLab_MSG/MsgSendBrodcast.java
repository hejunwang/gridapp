package com.autotestlab.gridactivity.QK_AutoTestLab_MSG;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class MsgSendBrodcast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        String action = intent.getAction();
        if (action.equals("MSG_SEND_ACTION")) {
            switch (super.getResultCode()) {
                case Activity.RESULT_OK:
                  //  Toast.makeText(context, "sms send over", Toast.LENGTH_SHORT).show();
                    Log.e("MSGsendBroadcast", "短信已经发送");

                    MsgActivity.successCount++;

                    Toast.makeText(context, "发送成功次数--->" + MsgActivity.successCount,
                            Toast.LENGTH_SHORT).show();


                    break;

                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Toast.makeText(context, "发送失败", Toast.LENGTH_SHORT).show();
                    Log.e("MSGsendBroadcast", "sms send fail");

                    MsgActivity.failCount++;
                    break;

                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    Toast.makeText(context, "无服务状态", Toast.LENGTH_SHORT).show();
                    Log.e("MSGsendBroadcast", "RESULT_ERROR_NO_SERVICE");
                    break;
            }


        }


    }
}
