package com.autotestlab.gridactivity.QK_AutoTestLab_VedioRecord;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Administrator on 2016/7/16.
 */
public class VedioRecord extends Activity implements SurfaceHolder.Callback, View.OnClickListener {

    private String TAG = getClass().getSimpleName();
    protected static final int GUI_DOING = 1;
    protected static final int GUI_SUCCESS = 0;


    private EditText media_times = null; // 次数
    private EditText media_duration = null; // 测试时长
    private EditText media_delay = null; // 下次执行间隔
    private ToggleButton media_toggleButton = null; // 前后切换
    private Button media_startButton = null; // 开始
    private Button media_stopButton = null; // 停止
    private int cameraPostion = 1; // 1代表后置 ,0 代表前置摄像头
    private ProgressBar progressBar = null;
    private TextView resultTextView = null;

    private Camera mCamera = null;

    File vedioFile;

    MediaRecorder mRecorder;
    private SurfaceView media_surfaceView = null; // 画面
    private SurfaceHolder surfaceHolder;
    // 是否在录制
    private boolean isRecording = false;
    private Handler handler;


    // TODO: 2016/7/16 测试时间,测试时长,间隔
    private String textTime;
    private String delayString;
    private String durationString;


    // 延时时间
    private int DELAYTIME;
    /**执行次数*/
    // TODO: 2016/10/20 执行次数
    private int COUNTTIME;
    // 时长
    private int DURATIONTIME;
    // 临时计数器
    private int counttmp = 0;

    private int minute = 0; // 分钟
    private int second = 0; // 毫秒
    private String time = ""; // 耗时
    String medianame;
    int testduration; // 测试时间
    Long long1, long2; // 计时点
    int cameraIndex; // 摄像头索引

    private SqliteHelper helper;
    private MytabOpera mytabOpera;


    /**
     * 消息处理
     */
    Handler recHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case GUI_SUCCESS:
                    Object obj = msg.obj;

                    Log.e(TAG, (String) obj);

                    resultTextView.setText((CharSequence) obj);
                    resultTextView.setGravity(Gravity.CENTER);

                    mytabOpera = new MytabOpera(helper.getWritableDatabase());
                    mytabOpera.insert(Build.MODEL, TAG, Build.VERSION.INCREMENTAL, COUNTTIME, "SUCCESS");
                    Log.e(TAG, "SUCCESS---添加数据到sqldb中");

                    DatabaseDump databaseDump = new DatabaseDump(helper.getWritableDatabase());
                    databaseDump.writeExcel(SqliteHelper.TB_NAME);
                    Log.e(TAG, "执行完成,导出数据库到内置存储中");
                    Toast.makeText(VedioRecord.this, "测试完成,正从数据库中导出到EXCEL表格中,请稍等",
                            Toast.LENGTH_SHORT)
                            .show();

                    break;

                case GUI_DOING:
                    Object obj1 = msg.obj;
                    resultTextView.setText((CharSequence) obj1);
                    resultTextView.setGravity(Gravity.CENTER);

//                    mytabOpera = new MytabOpera(helper.getWritableDatabase());
//                    mytabOpera.insert(Build.MODEL,TAG,COUNTTIME,"SUCCESS");
                    Log.e(TAG, "DOING----执行中");


                    break;


            }
        }
    };


    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
// TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        Log.e(TAG, "MediaRecordAct  onCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
        setContentView(R.layout.mediavedio);


        findview();
        this.helper = new SqliteHelper(this);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        surfaceHolder = media_surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        handler = new Handler();
        // 设置Surface不需要维护自己的缓冲区
        media_surfaceView.getHolder().setType(
                SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        // 设置该组件不会让屏幕自动关闭
        media_surfaceView.getHolder().setKeepScreenOn(true);


    }


    private void findview() {
// TODO Auto-generated method stub
        this.media_times = (EditText) findViewById(R.id.media_times);
        this.media_duration = (EditText) findViewById(R.id.media_duration);
        this.media_delay = (EditText) findViewById(R.id.media_delay);

        media_times.setText("1");
        media_delay.setText("3");
        media_duration.setText("10");

        media_duration.setGravity(Gravity.CENTER);
        media_times.setGravity(Gravity.CENTER);
        media_delay.setGravity(Gravity.CENTER);

        this.resultTextView = (TextView) findViewById(R.id.media_result);

        resultTextView.setText("当前剩余存储:" + getSDAvailableSize()+
                "请设置合适的次数");
        resultTextView.setGravity(Gravity.CENTER);

        this.media_toggleButton = (ToggleButton) findViewById(R.id.mediatogButton);
        media_toggleButton.setOnClickListener(this);

        this.media_startButton = (Button) findViewById(R.id.media_start);
        this.media_startButton.setOnClickListener(this);

        this.media_stopButton = (Button) findViewById(R.id.media_stop);
        this.media_stopButton.setOnClickListener(this);
        this.media_stopButton.setEnabled(false);
// this.media_stopButton.setEnabled(false);
        this.media_surfaceView = (SurfaceView) findViewById(R.id.media_surface);
        media_surfaceView.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
// TODO Auto-generated method stub
                mCamera.autoFocus(mAutoFocusCallBack);
            }
        });

        this.progressBar = (ProgressBar) findViewById(R.id.vediao_progress);

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
        return Formatter.formatFileSize(VedioRecord.this, blockSize * availableBlocks);
    }


    /*sendmessage*/
    private void msgSend(int what, Object obj) {

        Message msg = new Message();
        msg.what = what;
        msg.obj = obj;
        recHandler.sendMessage(msg);
    }


    // 自动对焦功能
    private Camera.AutoFocusCallback mAutoFocusCallBack = new Camera.AutoFocusCallback() {


        @SuppressWarnings("deprecation")
        @Override
        public void onAutoFocus(boolean success, Camera camera) {

            if (success) {
                Log.e("AutoFocusCallback", "AutoFocusCallback" + success);
                Camera.Parameters Parameters = mCamera.getParameters();
                Parameters.setPictureFormat(PixelFormat.JPEG);// 设置图片格式
                mCamera.setParameters(Parameters);
            } else {
                Log.e(TAG, "对焦失败.......");
            }

        }
    };


    @Override
    public void onClick(View v) {
// TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.media_start:
                if (mRecorder != null) {
                    releaseMediaRecorder();
                    minute = 0;
                    second = 0;
                }
// ---------------------------------------
                textTime = media_times.getText().toString();
                delayString = media_delay.getText().toString();
                durationString = media_duration.getText().toString();
                /**
                 * 判断要执行的次数,测试的时间长度 ,间隔时间 ;
                 * */
                if ((textTime.equals("")) || textTime.length()== 0) {
                    COUNTTIME = 1;
                    Toast.makeText(VedioRecord.this, "请输入正确的数值", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    COUNTTIME = Integer.parseInt(textTime);
                    Log.i(TAG, "执行次数 -->" + COUNTTIME);
                }

                //间隔
                if (delayString.equals("") || delayString.length()==0) {
                    DELAYTIME = 3;
                } else {
                    DELAYTIME = Integer.parseInt(delayString);
                    if (DELAYTIME < 3) {
                        Log.e(TAG, "间隔时间少于3S,默认是3");
                        DELAYTIME = 3;
                    }
                    Log.i(TAG, "间隔时间--->" + DELAYTIME);
                }

                //测试时长
                if (durationString.equals("") || (durationString.length()==0)) {
                    DURATIONTIME = 10;
                    testduration = DURATIONTIME;

                } else {
                    DURATIONTIME = Integer.parseInt(durationString);

                    testduration = DURATIONTIME;
                    Log.i(TAG, "录制时长-->" + DURATIONTIME);
                }

                Log.v(TAG, "执行次数-->" + COUNTTIME + " 间隔--->" + DELAYTIME
                        + "  时长--->" + DURATIONTIME);


                if (COUNTTIME == 1) { // 如果执行一次 ,设置
                    resultTextView.setText("开始执行");
                    resultTextView.setGravity(Gravity.CENTER);
                    recorder();
                    //处理结果显示和进度条
                    handler.postDelayed(runOneTime, 1000*DELAYTIME);
                    Toast.makeText(getApplicationContext(), "录制视频开始",
                            Toast.LENGTH_SHORT).show();
                    // isRecording = true;
                } else {
                  //  recorder();
                    counttmp = 0;
                    isRun =true ;
                    handler.postDelayed(durationRun, 1000*DELAYTIME);
                    Toast.makeText(getApplicationContext(), "录制视频开始",
                            Toast.LENGTH_SHORT).show();
                }

                media_startButton.setEnabled(false);
                media_stopButton.setEnabled(true);
// ---------------------------------------------------------------

                break;


            case R.id.media_stop:
                if (mRecorder != null) {
                    releaseMediaRecorder();
                    minute = 0;
                    second = 0;
                    handler.removeCallbacks(runOneTime);
                    handler.removeCallbacks(durationRun);
                    handler.removeCallbacks(secondRunnable);
                    isRecording = false;
                    Log.v(TAG, " STOP  removeCallbacks  ");
                }
                Log.v(TAG, " media  Stop ");
                releaseMediaRecorder();
                handler.removeCallbacks(runOneTime);
                handler.removeCallbacks(durationRun);
                handler.removeCallbacks(secondRunnable);


                media_startButton.setEnabled(true);
                media_stopButton.setEnabled(false);
                resultTextView.setText("停止摄像");
                resultTextView.setGravity(Gravity.CENTER);
                minute = 0;
                second = 0;

                progSet = 0;
                progSetone = 0;
                break;


            case R.id.mediatogButton:
// 切换前后摄像头
                int cameraCount = 0;
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                cameraCount = Camera.getNumberOfCameras();// 得到摄像头的个数
                for (int i = 0; i < cameraCount; i++) {
                    Camera.getCameraInfo(i, cameraInfo);// 得到每一个摄像头的信息
                    if (cameraPostion == 1) {
// 现在是后置，变更为前置
                        Log.v(TAG, "判断是否是前置");
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
/** * 记得释放camera，方便其他应用调用 */
                            releaseCamera();
// 打开当前选中的摄像头
                            mCamera = Camera.open(i);
                            Log.v(TAG, "打开摄像头的值 i--->" + i); // 前置是1,后置是0
// 通过surfaceview显示取景画面
                            setStartPreview(mCamera, surfaceHolder);
                            mCamera.setDisplayOrientation(90);
                            cameraPostion = 0;
                            cameraIndex = 1;
                            break;
                        }
                    } else {
                        Log.v(TAG, "判断是否是后置");
// 现在是前置， 变更为后置
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
/** * 记得释放camera，方便其他应用调用 */
                            releaseCamera();
                            mCamera = Camera.open(i);
                            Log.v(TAG, "打开摄像头的值 i--->" + i);
                            setStartPreview(mCamera, surfaceHolder);
                            mCamera.setDisplayOrientation(270);
                            cameraPostion = 1;
                            cameraIndex = 0;
                            break;
                        }


                    }


                }
        }


    }


    // TODO: 2016/7/16 设置进度条的进度为0
    int progSetone = 0;
    /**
     * 录制过程,时间变化,大小变化
     */
    Runnable runOneTime = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            Log.e(TAG, "RUNTIME一次执行 ");
            progressBar.setMax(testduration);
            progressBar.setProgress(progSetone);
            second++;
            Log.e(TAG,"测试时间:"+second);
            progSetone++;
            if (second == 60) {
                minute++;
                second = 0;
            }
            if (progSetone > testduration) {      //结束
                second = 0;
                minute = 0;
                progSetone = 0;
                Log.e(TAG, "testduration  : " + testduration);
                releaseMediaRecorder();
                handler.removeCallbacks(runOneTime);
                long2 = System.currentTimeMillis();
                long durtime = (long2 - long1) / 1000;
                Log.i(TAG, "long time " + durtime);

                Object obj = "录制视频次数:" + COUNTTIME + "  时长:"
                        + testduration + " 间隔时间:" + DELAYTIME + "  time:"
                        + time + ";\nSD卡剩余容量:" + getSDAvailableSize();
                msgSend(GUI_SUCCESS, obj);
                Log.e(TAG, "vedioRecord  GUI_SUCCESS");

                media_startButton.setEnabled(true); // 开始置灰
                media_stopButton.setEnabled(false);
            } else {       //按照每秒钟的来进行执行的          继续
                Log.e(TAG, "minute:  " + minute + "  second : " + second);
                time = String.format("%02d:%02d", minute, second);
                Object obj = "录制视频次数:" + COUNTTIME + "  时长:"
                        + testduration + " 间隔时间:" + DELAYTIME + "  time:"
                        + time + ";\nSD卡剩余容量:" + getSDAvailableSize();
                Log.e(TAG, "vedioRecord  GUI_Doing");
                msgSend(GUI_DOING, obj);

                media_startButton.setEnabled(false); // 开始置灰
                media_stopButton.setEnabled(true);

                handler.postDelayed(runOneTime, 1000); // 一秒间隔
            }


        }
    };




    // TODO: 2016/10/20 是否让录制线程执行
    boolean isRun = false ;
    /**
     * 执行次数大于1
     */
    int progSet = 0;
    Runnable durationRun = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            Log.e(TAG, "多次执行线程调用");
            if (isRun)
            {
                recorder();
            }
            // progressbar进度
            media_startButton.setEnabled(false);
            media_stopButton.setEnabled(true);

            progressBar.setMax(testduration);
            progressBar.setProgress(progSet);
            second++;
            Log.e(TAG,"jishu--->"+second);
            progSet++;
            if (second == 60) {
                minute++;
                second = 0;
            }
            if (progSet > testduration) {    //完成其中的一次
                second = 0;
                minute = 0;
                progSet = 0;
                Log.e(TAG, "testduration  : " + testduration);
                releaseMediaRecorder();
                handler.removeCallbacks(durationRun);

                media_startButton.setEnabled(true);
                media_stopButton.setEnabled(false);

                Log.e(TAG, "执行完成");
                counttmp++;
                Log.e(TAG, "COUNTTIME 执行" + counttmp + "次");

                String string = "录制视频次数:" + counttmp + "  时长:"
                        + testduration + " 间隔时间:" + DELAYTIME + "  time:"
                        + time;
                msgSend(GUI_SUCCESS, string);

                Toast.makeText(getApplicationContext(), "成功",
                        Toast.LENGTH_SHORT).show();


                // TODO: 2016/10/20 当前执行次数比总次数少,继续执行
                if (counttmp < COUNTTIME) {
                    Log.e(TAG, "结束完成一次");
                        isRun =true ;
                    handler.postDelayed(durationRun, DELAYTIME * 1000L);
                }
            } else {      //录制的过程中
                Log.e(TAG, "minute:  " + minute + "  second : " + second);
                time = String.format("%02d:%02d", minute, second);

                resultTextView.setGravity(Gravity.CENTER);

                String string = "录制视频次数:" + counttmp + "  时长:"
                        + testduration + " 间隔时间:" + DELAYTIME + "  time:"
                        + time;
                Log.e(TAG, "多次任务执行中," + string);
                // TODO: 2016/10/20 不执行录制
                isRun =false ;
                handler.postDelayed(durationRun,1000L);

                Log.v(TAG, "progset++ " + progSet);
            }
        }


    };


    /**
     * 执行次数大于1
     */
    Runnable secondRunnable = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            Log.e(TAG, "多次执行 线程2调用");

            recorder();
            handler.removeCallbacks(secondRunnable);
            handler.post(durationRun);
        }


    };

    // TODO: 2016/7/16 停止录像
    private void releaseMediaRecorder() {
        // TODO Auto-generated method stub
        if (mRecorder != null) {
            Log.e(TAG, "releaseMediaRecorder");
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
    }


    // TODO: 2016/7/18 videoRecord
    public void recorder() {
        if (!isRecording) {
            try {
                if (mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                }

                mRecorder = new MediaRecorder();
                mRecorder.reset();

                String savePath = Environment.getExternalStorageDirectory()
                        .getPath() + File.separator + "moveRecord";
                Log.e(TAG, "录制视频存储位置 :" + savePath);
                File folder = new File(savePath);
                if (!folder.exists()) // 如果文件夹不存在则创建
                {
                    folder.mkdir();
                }
                long dataTake = System.currentTimeMillis();
                medianame = savePath + File.separator + dataTake + ".mp4";
                Log.e(TAG, "medianame-->" + medianame);
                vedioFile = new File(medianame);
                if (!vedioFile.exists()) {
                    vedioFile.createNewFile();
                }
                camerSet();


// mCamera = Camera.open();
                mCamera.unlock();
                mRecorder.setCamera(mCamera);

// 设置从麦克风采集声音
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
// 设置从摄像头采集图像
                mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
// 设置视频、音频的输出格式
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
// 设置音频的编码格式、
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
// 设置图像编码格式
                mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
// mRecorder.setOrientationHint(270);


                mRecorder.setOutputFile(vedioFile.getAbsolutePath());
// mRecorder.setMaxDuration(DURATIONTIME);
                mRecorder.setPreviewDisplay(media_surfaceView.getHolder()
                        .getSurface());
                mRecorder.prepare();


                mRecorder.start();
                long1 = System.currentTimeMillis();
                SimpleDateFormat formatter = new SimpleDateFormat(
                        "yyyy年-MM月dd日-HH时mm分ss秒");
                Date date = new Date(long1);
// System.out.println(formatter.format(date));
                String osName = System.getProperty("os.name");
                String user = System.getProperty("user.name");
                Log.e(TAG, "系统用户-->" + osName + "  用户名字-->" + user
                        + "  当前时间-->" + formatter.format(date));


            } catch (IllegalStateException e) {
            // TODO: handle exception
                e.printStackTrace();
                releaseMediaRecorder();
// handler.removeCallbacks(runOneTime);
                minute = 0;
                second = 0;
// isRecording = false;
                media_startButton.setEnabled(true);
            } catch (IOException e) {
// TODO: handle exception
                releaseMediaRecorder();
// handler.removeCallbacks(runOneTime);
                minute = 0;
                second = 0;
// isRecording = false;
                media_startButton.setEnabled(true);

                e.printStackTrace();
            }
        }
    }

    // TODO: 2016/7/18 摄像头的设置 前置或者是后置
    private void camerSet() {
// TODO Auto-generated method stub
        if (cameraIndex == 0) {
            Log.e(TAG, "摄像头选择的是 0,是后置 ");
            mCamera = Camera.open(cameraIndex);
            mCamera.setDisplayOrientation(90);
        } else {
            Log.e(TAG, "摄像头选择的是 1,是前置 ");
            mCamera = Camera.open(cameraIndex);
            mCamera.setDisplayOrientation(90);
        }


    }


    private void setStartPreview(Camera mCamera2, SurfaceHolder surfaceHolder2) {
// TODO Auto-generated method stub
        try {
            mCamera2.setPreviewDisplay(surfaceHolder2);
            mCamera2.startPreview();
        } catch (IOException e) {
// TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * 释放摄像头
     */
    private void releaseCamera() {
// TODO Auto-generated method stub
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();// 停掉原来摄像头的预览
            mCamera.release();
            mCamera = null;


        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
// TODO Auto-generated method stub
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.e(TAG, "KEYCODE_BACK");
            mytabOpera = new MytabOpera(helper.getWritableDatabase());
            mytabOpera.delettable();
            VedioRecord.this.finish();
        }
        return false;
    }


    // 判断前置摄像头是否存在
    private int FindFrontCamera() {
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number


        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
// 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }


    // 判断后置摄像头是否存在
    private int FindBackCamera() {
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number


        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
// 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                Log.v(TAG, "camIdx  back --> " + camIdx);
                return camIdx;
            }
        }
        return -1;
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
// TODO Auto-generated method stub
        mCamera.startPreview();
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        mCamera = Camera.open(); // 打开摄像头 ,不考虑是前置还是后置问题


        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.setDisplayOrientation(90);
        } catch (IOException e) {
// TODO Auto-generated catch block
            e.printStackTrace();
            mCamera.release();
        }
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
// TODO Auto-generated method stub
// 关闭预览并释放资源
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


}


