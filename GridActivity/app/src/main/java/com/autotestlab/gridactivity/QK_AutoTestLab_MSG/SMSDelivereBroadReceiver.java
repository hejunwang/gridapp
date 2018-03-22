package com.autotestlab.gridactivity.QK_AutoTestLab_MSG;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class SMSDelivereBroadReceiver extends BroadcastReceiver {
    public SMSDelivereBroadReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        if (intent.getAction().equals("SMS_DELIVERED_ACTION"))
        {
            switch (getResultCode())
            {
                case Activity.RESULT_OK :
                    Toast.makeText(context,"sms send over",Toast.LENGTH_SHORT).show();
                    Log.e("SMSDelivereBror","短信对方已经接收");
                    break;

                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Toast.makeText(context,"sms send fail",Toast.LENGTH_SHORT).show();
                    Log.e("SMSDelivereBror","sms send fail");
                    break;
            }
        }

    }
}
