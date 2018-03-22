package com.autotestlab.gridactivity;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class AgentServer extends Service {

    private String TAG = getClass().getSimpleName();
    private static final int PORTNUMER = 60000; // port
    Context context = AgentServer.this;
    private AcceptThread macceptThread; // thread
    private ReceiveThread mreceiveThread;
    private boolean isStop = true;
    Socket client = null;

    public AgentServer() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.e(TAG,"onCreate!");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        Log.e(TAG,"onStartCommand!");
        macceptThread = new AcceptThread();
        macceptThread.setPriority(Thread.MAX_PRIORITY);
        macceptThread.start();

        return Service.START_NOT_STICKY;
    }

    /**
     * 启动server后,一直的进行等待client的连接
     */
    class AcceptThread extends Thread {
        public AcceptThread() {
            Log.e(TAG,"AcceptThread");
        }

        @Override
        public void run() {
            //  super.run();
            try {
                ServerSocket serverSocket = new ServerSocket(PORTNUMER);
                Log.e(TAG, "Server socket wait");

                client = serverSocket.accept();
                Log.e(TAG, "client-->" + client + ",是否连接成功-->" + client.isConnected());

                isStop = false;

                mreceiveThread = new ReceiveThread(client);
                mreceiveThread.start();


            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 接收数据,然后进行处理
     */
    class ReceiveThread extends Thread {
        private InputStream mInputStream = null;
        private byte[] buf;
        private String readResult = null;
        private Socket socket = null;
        BufferedReader bReader = null;

        public ReceiveThread(Socket socket) {
            this.socket = socket;
            Log.e(TAG, "ReceiveThread");
            try {
                this.mInputStream = socket.getInputStream();
                this.bReader = new BufferedReader(new InputStreamReader(mInputStream));


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();

            int n = 99;
            while (!isStop) {

                this.buf = new byte[512];

                try {
                    this.mInputStream.read(buf);
                    this.readResult = new String(buf, "utf-8").trim();
                    Log.i(TAG, "read result is ->>>" + readResult);
                    n = Integer.parseInt(readResult);
                    switch (n) {
                        case 1:

                            Log.e(TAG, " do  case 1");
                            break;

                        default:

                            break;
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }


            }


        }
    }


}
