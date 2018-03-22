package com.autotestlab.gridactivity.QK_AutoTestLab_CameraStress;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.DatabaseDump;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.MytabOpera;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.SqliteHelper;
import com.autotestlab.gridactivity.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraAct extends AppCompatActivity implements SurfaceHolder.Callback, OnClickListener {

    private String TAG = getClass().getSimpleName();
    private EditText time = null;
    private EditText jiange = null;
    private Button startButton = null;
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder holder;
    /**
     * 是否在运行
     */
    private boolean mPreviewRunning = false;
    Context mContext = CameraAct.this;

    private ToggleButton tButton = null;
    private ToggleButton changButton = null;
    private Button pauseButton = null;
    private Button stopButton = null;

    /**
     * 成功 以及失败的次数总和
     */
    int succCount = 0;
    int failCount = 0;
    Bitmap bitmap;
    private int cameraPostion = 1; // 1代表后置 ,0 代表前置摄像头
    //进度条
    private ProgressBar progressBar = null;
    /**
     * 结果文本
     */
    private TextView resultTextView = null;
    private int COUNT = 1; // 次数
    private int DELAY = 1; // 间隔延时


    // 自定义Handler信息代码，用以作为识别事件处理
    protected static final int GUI_NOTIFIER = 0;
    protected static final int GUI_STOP = 1;
    protected static final int GUI_SUCCESS = 2;
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
    /**
     * 文本信息
     */
    private String text = null;
    /**
     * 间隔时间
     */
    private String jiangeString = null;
    Camera.CameraInfo cameraInfo;

    SqliteHelper helper;
    MytabOpera mytabOpera;

    /*手机名字*/
    private String phoneName = null;
    // 消息处理
    @SuppressLint("HandlerLeak")
    Handler msgHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case GUI_NOTIFIER:

                    progressBar.setProgress(countall - intcount);
                    progressBar.setMax(countall);

                    String res = "总执行次数:" + countall + " 当前执行到:" + (countall - intcount) +
                            "\n成功次数:" + succCount + ";失败次数:" + failCount + "间隔时间:" + DELAY + "S"
                            +
                            ";内存:"+getAvailableMemory(mContext)+"M";
                    Log.i(TAG, "GUI_STOP_NOTIFIER 的intcount-->" + intcount+","+res);
                    resultTextView.setText(res);


                    mytabOpera = new MytabOpera(helper.getWritableDatabase());
                    mytabOpera.insert(phoneName, TAG, Build.VERSION.INCREMENTAL, countall - intcount, "SUCCESS");
                    Log.e(TAG, "添加数据到sqldb中");

                    DatabaseDump databaseDump1 = new DatabaseDump(helper.getWritableDatabase());
                    databaseDump1.writeExcel(SqliteHelper.TB_NAME);


                    break;


                case GUI_SUCCESS:

                    progressBar.setProgress(countall);
                    progressBar.setMax(countall);
                    String res1 = "总执行次数:" + countall + " 当前执行到:" + (countall - intcount) +
                            "\n成功次数:" + succCount + ";失败次数:" + failCount +
                            "间隔时间:" + DELAY + "S"
                            +
                            ";内存:"+getAvailableMemory(mContext)+"M";

                    resultTextView.setText(res1);

                    mytabOpera = new MytabOpera(helper.getWritableDatabase());
                    mytabOpera.insert(phoneName, TAG, Build.VERSION.INCREMENTAL, countall - intcount, "SUCCESS");
                    Log.e(TAG, "添加数据到sqldb中");

                    DatabaseDump databaseDump = new DatabaseDump(helper.getWritableDatabase());
                    databaseDump.writeExcel(SqliteHelper.TB_NAME);


                    resultTextView.setTextColor(Color.GREEN);
                    resultTextView.setGravity(Gravity.CENTER);

                    stopButton.setEnabled(false);
                    startButton.setEnabled(true);

                    Log.e(TAG, "执行完成,导出数据库到内置存储中");
                    Toast.makeText(mContext, "测试完成,正从数据库中导出到EXCEL表格中,请稍等",
                            Toast.LENGTH_SHORT)
                            .show();

                    break;


                case GUI_STOP:
                    threadHandler.removeCallbacks(startRunnable);
                    threadHandler.removeCallbacks(autoRunnable);
                    resultTextView.setText("停止拍照");
                    resultTextView.setGravity(Gravity.CENTER);
                    Log.v(TAG, "Runnable线程停止");
                    break;
            }
        }

        ;


    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        Log.e(TAG, "loadered the xml file ");
        Log.d(TAG, "开始相机的程序");
        findView();

        phoneName = Build.MODEL;
        helper = new SqliteHelper(mContext);
        mytabOpera = new MytabOpera(helper.getWritableDatabase());

        holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        this.threadHandler = new Handler();

        /** 自动前后切换 */
        changButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (changButton.isChecked()) {
                    Toast.makeText(getApplicationContext(), "手动状态切换到自动状态", Toast.LENGTH_SHORT).show();
                    tButton.setEnabled(false);
                    // resultTextView.setText("前后摄像头切换拍照");

                } else {
                    Toast.makeText(getApplicationContext(), "自动状态切换到手动状态", Toast.LENGTH_SHORT).show();
                    tButton.setEnabled(true);
                    // changButton.setEnabled(false);
                }
            }
        });

    }

    /*pause*/
    @Override
    protected void onPause() {
        super.onPause();
    }



    /**
     * 获取当前可用内存，返回数据以字节为单位。
     *
     * @param context 可传入应用程序上下文。
     * @return 当前可用内存单位为B。
     */
    public static long getAvailableMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem/1024/1024;
    }



    /**
     * 获得sd卡剩余容量，即可用大小
     *
     * @return
     */
    private String getSDAvailableSize() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return Formatter.formatFileSize(CameraAct.this, blockSize * availableBlocks);
    }

    // 控件
    private void findView() {
        /** 前后置摄像头切换 */
        this.tButton = (ToggleButton) findViewById(R.id.toggleButton1);
        tButton.setOnClickListener(this);


        this.jiange = (EditText) findViewById(R.id.jiange_ET); // 间隔
        this.time = (EditText) findViewById(R.id.times_ET); // 次数
        time.setText("1");
        time.setGravity(Gravity.CENTER);
        jiange.setText("3");
        jiange.setGravity(Gravity.CENTER);


        this.progressBar = (ProgressBar) findViewById(R.id.camera_progress);
        progressBar.setProgress(0);


        this.resultTextView = (TextView) findViewById(R.id.result);

        resultTextView.setText("相机压力测试,当前内置存储:" + getSDAvailableSize() +
                ";\n内存大小:"+getAvailableMemory(mContext)+"M");
        resultTextView.setGravity(Gravity.CENTER);


        this.startButton = (Button) findViewById(R.id.start1); // 开始拍照
        this.startButton.setOnClickListener(this);


        this.stopButton = (Button) findViewById(R.id.stop); // 停止
        stopButton.setOnClickListener(this);
        stopButton.setEnabled(false);


        this.changButton = (ToggleButton) findViewById(R.id.toggleButton2);
// changButton.setVisibility(View.INVISIBLE);
        changButton.setOnClickListener(this); // 自动切换

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView1);

        // 点击对焦
//        mSurfaceView.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(final View v) {
//                mCamera.autoFocus(mAutoFocusCallBack);
//            }
//        });
    }


    /**
     * 按钮监听
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start1:

                succCount = 0;
                failCount = 0;
                text = time.getText().toString();
                jiangeString = jiange.getText().toString();
                if (text.equals(" ") || text.equals("0")) {
                    COUNT = 1;
                    Toast.makeText(mContext, "请输入正确的数值", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    COUNT = Integer.parseInt(text);
                    countall = COUNT;
                    Log.i(TAG, "count的值确定-->" + COUNT);
                }
                if (jiangeString.equals(" ") || jiangeString.equals("0")) {
                    DELAY = 3;
                } else {
                    DELAY = Integer.parseInt(jiangeString);
                    Log.i(TAG, "DELAY:" + DELAY);
                    if (DELAY < 3) {
                        DELAY = 3;
                    }
                    Log.i(TAG, "DELAY的值是-->" + DELAY);
                }
                /** 判断是否处于自动切换状态 */
                if (tButton.isEnabled()) {
                    // 添加一个输入键盘隐藏
                    InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    im.hideSoftInputFromWindow(getCurrentFocus().getApplicationWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
                    Log.e(TAG, "摄像头拍照");
                    threadHandler.postDelayed(startRunnable, DELAY * 1000L);


                } else {
                    // 自动前后切换
                    Log.e(TAG, "前后摄像头自动拍照");
                    threadHandler.postDelayed(autoRunnable, DELAY * 1000L);
                }


                startButton.setEnabled(false);
                stopButton.setEnabled(true);


                break;
            case R.id.toggleButton1:
                // 切换前后摄像头
                int cameraCount = 0;
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                cameraCount = Camera.getNumberOfCameras();// 得到摄像头的个数
                for (int i = 0; i < cameraCount; i++) {
                    Camera.getCameraInfo(i, cameraInfo); // 得到每一个摄像头的信息
                    if (cameraPostion == 1) {
                        // 现在是后置，变更为前置
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        /** * 记得释放camera，方便其他应用调用 */
                            releaseCamera();
                                // 打开当前选中的摄像头
                            mCamera = Camera.open(i);
                            // 通过surfaceview显示取景画面
                            setStartPreview(mCamera, holder);
                            mCamera.setDisplayOrientation(90);
                            cameraPostion = 0;
                            Log.e(TAG, "当前是前置,打开的摄像头是-->" + i);
                            break;
                        }
                    } else {
                    // 现在是前置， 变更为后置
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            /** * 记得释放camera，方便其他应用调用 */
                            releaseCamera();
                            mCamera = Camera.open(i);
                            setStartPreview(mCamera, holder);
                            mCamera.setDisplayOrientation(90);
                            cameraPostion = 1;
                            Log.e(TAG, "后置摄像头,打开摄像头的是-->" + i);
                            break;
                        }
                    }

                }
                break;

            case R.id.stop:

                Toast.makeText(getApplicationContext(), "stop", Toast.LENGTH_SHORT).show();
                Message message = new Message();
                message.what = GUI_STOP;
                msgHandler.sendMessage(message);
                threadHandler.removeCallbacks(autoRunnable);
                Log.i(TAG, " startRun  & autoRun  stop");

                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                break;


        }


    }


    /**
     * 单摄像头测试线程
     **/
    Runnable startRunnable = new Runnable() {
        @Override
        public void run() {

            try {
                takepicture();
            } catch (InterruptedException e) {
                failCount++;
                e.printStackTrace();
            }

            succCount++;
            intcount = COUNT - 1;
            COUNT--;
            Log.e(TAG, "  startRun  intcount的值是:" + intcount);
            Message msg = new Message();
            COUNT = intcount;

            // TODO: 2016/8/5  次数大于1的情况下执行线程
            if (intcount > 0) {
                threadHandler.postDelayed(startRunnable, DELAY * 1000);

                msg.what = GUI_NOTIFIER;
                msgHandler.sendMessage(msg);

                return;
            } else {
                // TODO: 2016/8/5 如果是一次执行完成 ,就发送消息
                msg.what = GUI_SUCCESS;
                //  msg.obj = sucMsg;
                msgHandler.sendMessage(msg);


            }
            threadHandler.removeCallbacks(startRunnable);
        }


    };


    // 拍照
    private void takepicture() throws InterruptedException {
        if (mPreviewRunning && mCamera != null) {
            Log.e(TAG, "takephoto拍照调用");
            // mCamera.autoFocus(mAutoFocusCallBack);
            mCamera.takePicture(shutterCallback, null, jpegCallback);

        } else {
            Log.e(TAG,"takephoto 调用失败");
            failCount++;
            Toast.makeText(getApplicationContext(), "takephoto 调用失败", Toast.LENGTH_SHORT).show();
        }
    }


    // 释放 相机
    private void releaseCamera() {


        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();// 停掉原来摄像头的预览
            mCamera.release();
            mCamera = null;


        }


    }

    /**
     * 设置camera显示取景画面,并预览
     *
     * @param camera
     */
    private void setStartPreview(Camera camera, SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }


    /**
     * 自动切换到后摄并拍照
     */
    Runnable autoRunnable = new Runnable() {

        @Override
        public void run() {
            Log.e(TAG, "自动切换拍照  AutoRunable  Start ");
            autotakepic();
            intcount = COUNT - 1;
            COUNT--;
            Log.e(TAG, "AutoRunnable  intcount的值是:" + intcount);


            threadHandler.postDelayed(autoRunnable2, DELAY * 1000L);
            threadHandler.removeCallbacks(autoRunnable);
        }


    };

    /**
     * 自动切换到前摄并拍照
     */
    Runnable autoRunnable2 = new Runnable() {


        @Override
        public void run() {
            autotakepic2();
            Message msg = new Message();

            succCount++;
            Log.e(TAG, "当前执行到-->" + succCount + "次");
            if (intcount > 0) {
                threadHandler.postDelayed(autoRunnable, DELAY * 1000);
                msg.what = GUI_NOTIFIER;
                msgHandler.sendMessage(msg);
                return;
            } else {
                msg.what = GUI_SUCCESS;
                msgHandler.sendMessage(msg);
                threadHandler.removeCallbacks(autoRunnable2);
            }


        }


    };


    /**
     * 自动切换到后摄拍照
     */
    private void autotakepic() {
/** camera数量 */
        cameraPostion = 1;
        cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(0, cameraInfo);

        releaseCamera();

        mCamera = Camera.open(0);
        setStartPreview(mCamera, holder);
        for (int i = 0; i < 3; i++) {
            try {
                Log.e(TAG, "设置开始预览的时间-->" + i);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mCamera.setDisplayOrientation(90);
        sleep(2);
//           mCamera.autoFocus(mAutoFocusCallBack);
        sleep(1);

        try {
            takepicture();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    private void sleep(int i) {
        for (int n = 0; n < i; n++) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * 自动切换到前摄拍照
     */
    private void autotakepic2() {
/** camera数量 */
        cameraInfo = new Camera.CameraInfo();
        cameraPostion = 0;
        releaseCamera();
        Camera.getCameraInfo(1, cameraInfo);
        sleep(1);
        mCamera = Camera.open(1);
        setStartPreview(mCamera, holder);
        mCamera.setDisplayOrientation(90);

        sleep(1);
        //  mCamera.autoFocus(mAutoFocusCallBack);


        sleep(3);
        try {
            takepicture();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        initCamera();
    }


    /**
     * 初始化摄像头
     */
    private void initCamera() {
        if (mPreviewRunning) {
            mCamera.stopPreview(); // 释放相机资源
        }

        if (null != mCamera) {
            Camera.Parameters params = mCamera.getParameters();
            params.setPictureFormat(PixelFormat.JPEG);// 设置图片格式
            mCamera.setDisplayOrientation(90);


            mCamera.setParameters(params);
            try {
                mCamera.setPreviewDisplay(holder); // 设置相机获取的图片在holder中显示
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();


            mPreviewRunning = true;
        }


    }


    // 自动对焦功能
//    private Camera.AutoFocusCallback mAutoFocusCallBack = new Camera.AutoFocusCallback() {
//
//
//        @Override
//        public void onAutoFocus(boolean success, Camera camera) {
//
//
//            if (success) {
//                Log.e("AutoFocusCallback", "AutoFocusCallback" + success);
//                Camera.Parameters Parameters = mCamera.getParameters();
//                Parameters.setPictureFormat(PixelFormat.JPEG);// 设置图片格式
//                mCamera.setParameters(Parameters);
//            } else {
//                Log.e(TAG, "对焦失败.......");
//            }
//
//
//        }
//    };


    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
        //------------------------新增加验证
       // mCamera.unlock();

        //------------------------

        Log.e(TAG, "打开摄像头");
        try {
            mCamera.setPreviewDisplay(holder);
            Log.e(TAG, "SurfaceHolder.Callback: surfaceCreated!");


        } catch (IOException e) {
            if (null != mCamera) {
                //----------------
           //     mCamera.lock();
                //----------------

                mCamera.release();
                mCamera = null;
            }
            e.printStackTrace();
        }
    }


    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(TAG, "surfacedestoryed");
        mCamera.stopPreview();
        mPreviewRunning = false;
        mCamera.release();
        mCamera = null;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            threadHandler.removeCallbacks(startRunnable);
            threadHandler.removeCallbacks(autoRunnable);
            threadHandler.removeCallbacks(autoRunnable2);
            Log.v(TAG, "onkeydown keycode-back");
            CameraAct.this.finish();

        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onDestroy() {

        mytabOpera = new MytabOpera(helper.getWritableDatabase());
        mytabOpera.delettable();
        CameraAct.this.finish();
        Log.e(TAG, TAG + "ondestroy");
        super.onDestroy();
    }


    private Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            Log.e("ShutterCallback", "…onShutter…");

        }
    };


//     给定一个Bitmap，进行保存

    public void saveJpeg(Bitmap bm) {
        String savePath = null;
        if (intcount <= 1500) {
            savePath = Environment.getExternalStorageDirectory().getPath() + File.separator + "testphoto";
            Log.e(TAG, "save bitmap的存储路径是 :" + savePath + "," + "incount的值是:" + intcount);
            File folder = new File(savePath);
            if (!folder.exists()) // 如果文件夹不存在则创建
            {
                folder.mkdir();
            }
        } else if (intcount >= 1501) {
            savePath = Environment.getExternalStorageDirectory().getPath() + File.separator + "testphoto"
                    + String.valueOf(1000);
            Log.e(TAG, "save bitmap的存储路径是 :" + savePath + "," + "incount的值是:" + intcount);
            File folder2 = new File(savePath);
            if (!folder2.exists()) // 如果文件夹不存在则创建
            {
                folder2.mkdir();
            }


        }
        long dataTake = System.currentTimeMillis();
        String jpegName = savePath + File.separator + dataTake + ".jpg";
        Log.e(TAG, "saveJpeg:jpegName--" + jpegName);
// File jpegFile = new File(jpegName);
        try {
            FileOutputStream fout = new FileOutputStream(jpegName);
            BufferedOutputStream bos = new BufferedOutputStream(fout);


            bm.compress(Bitmap.CompressFormat.JPEG, 100, bos); // 压缩比例
            bos.flush();
            bos.close();
            mCamera.stopPreview();
            mCamera.startPreview();
            bm.recycle();


            // succCount = succCount + 1;
            Log.e(TAG, "saveJpeg：存储完毕！-->" + succCount);
        } catch (IOException e) {


            Log.e(TAG, "saveJpeg:存储失败！-->" + failCount);
            e.printStackTrace();
        }
    }


    /**
     * jpeg照片的回调函数
     */
    private Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] _data, Camera _camera) {
            Log.e(TAG, "…onPictureTaken…");
            if (_data != null) {
                bitmap = BitmapFactory.decodeByteArray(_data, 0, _data.length);
                if (mPreviewRunning) {
                    mCamera.stopPreview();
                    mPreviewRunning = false;
                }
            }


            Log.e(TAG, "_data!:" + (_data != null));
            Log.e(TAG, "bitmap:" + (bitmap != null));


            Matrix matrix = new Matrix();
            matrix.postRotate((float) 90.0);


            if (cameraPostion == 0) {
                matrix.postRotate((float) -180.0);
            }

            Bitmap rotaBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, false);

            // 保存图片到sdcard
            if (null != rotaBitmap) {
                saveJpeg(rotaBitmap);
            }

            Log.e(TAG, "rotaBitmap!:  " + (rotaBitmap != null)+
                    "内存大小:"+getAvailableMemory(mContext)+"M");

            // 再次进入预览
            mCamera.startPreview();
            mPreviewRunning = true;


        }

    };


}
