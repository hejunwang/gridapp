package com.autotestlab.gridactivity.QK_AutoTestLab_Phone;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.Method;

public class PhoneService extends Service {
    private String tag =getClass().getSimpleName();

    private  TelephonyManager telephony = null;

    private String phoneNumber = null;

     IBinder myBinder = new Binderlmpl();

   class Binderlmpl extends Binder implements IService
   {
       @Override
       public String getInterfaceDescriptor() {
           return "接听电话:>"+PhoneService.this.phoneNumber+"<设置成功,其他电话拒绝";
       }
   }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
         phoneNumber = intent.getStringExtra("phonenumber");

        telephony  = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        telephony.listen(new PhoneStateListenerlmpn(),PhoneStateListener.LISTEN_CALL_STATE);

        return  myBinder;
    }


   int n = 0;
   int m = 0;
    class PhoneStateListenerlmpn extends PhoneStateListener
    {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state)
            {

                case TelephonyManager.CALL_STATE_IDLE:

                    Log.e(tag,"call  idle ");
                    break;

                case TelephonyManager.CALL_STATE_RINGING:
                    /**只接听指定的号码,其他的一律挂断.*/
                    Log.e(tag,"电话.号码.."+incomingNumber+"来电话了 ...");
                    ITelephony iTelephony1 =getITelephony();
                    if (incomingNumber.equals(phoneNumber))
                    {

//                        if (iTelephony1!=null)
//                        {
                            Log.e(tag,"接听...itelephony"+iTelephony1+";");
                            try {
                                iTelephony1.answerRingingCall();

                                Log.e(tag,"接听来电号码...");
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }

                            Intent intent = new Intent();
                            intent.setAction(AnswerCallActivity.BROADCAST);
                            intent.putExtra("data",incomingNumber+"电话接入第"+n+++"次");
                            sendBroadcast(intent);

                     //   }
                    }else
                    {
                           Log.e(tag,"直接挂断了...17727591015");
                            try {
                                iTelephony1.endCall();

                                Log.e(tag,"直接挂断了.");
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }

                            Intent intent = new Intent();
                            intent.setAction(AnswerCallActivity.BROADCAST);
                            intent.putExtra("data",incomingNumber+"拒绝接听第"+m+++"次");
                            sendBroadcast(intent);
                    }

                    break;

            }
        }
    }


    /**
     * @param
     * @return
     */
    public ITelephony getITelephony() {
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
