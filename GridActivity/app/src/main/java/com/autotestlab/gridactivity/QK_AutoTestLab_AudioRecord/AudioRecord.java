package com.autotestlab.gridactivity.QK_AutoTestLab_AudioRecord;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
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
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.autotestlab.gridactivity.LogService;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.DatabaseDump;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.MytabOpera;
import com.autotestlab.gridactivity.QK_AutoTestLab_Sql.SqliteHelper;
import com.autotestlab.gridactivity.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
* @toby
 * 录制yinpin文件 ,设置次数,设置录制的时长,选择录制的格式,
*   显示文件存在的位置  输出测试的结果  ,并在底部显示出来以供查阅
* */

public class AudioRecord extends AppCompatActivity implements View.OnClickListener {


    public String TAG = getClass().getSimpleName();
    private static final int SUCCESS = 0;
    private static final int FAIL = 1;
    private static final int FINAL = 2;
    private static final int DOING = 3;

    /*下拉选择后缀的格式*/
    private Spinner spinner = null;

    private EditText time = null;
    private EditText delay = null;

    private Button startBtn = null;
    private Button stopBtn = null;

    private ProgressBar progressBar = null;

    /*保存路径*/
    private TextView savePath = null;
    /*结果显示*/
    private TextView resultText = null;
    // TODO: 2016/7/19 列表显示
    private ListView listView = null;


    private boolean sdcardExists = false;

    Context context = AudioRecord.this;
    //后缀名
    String typeLast = "";
    //    time
    int timeCount = 1;
    //    delaytime
    int delayTime = 10;
    // TODO: 2016/7/12 成功失败的次数记录
    int sucCount = 0;
    int failCount = 0;

    private File recordAudioSaveFile = null;
    private File recordAudioSaveFileDir = null;
    private String recordAudioSaveFileName = null;
    private String recDir = "MediaRecord";

    private List<Map<String, Object>> recordFiles = null;
    private SimpleAdapter recordSimpadapter = null;

    private MediaRecorder mediaRecord, mediaRecorder;


    private boolean isRecording = false;


    // TODO: 2016/7/12  下面一部分主要是aac录音方面的参数
    private static final int BUFFER_SIZE = 500000;

    private static final int rtphl = 0;

    private static final int AUDIO_BUFFER_SIZE = 15000;

    private byte[] audioBuffer = new byte[AUDIO_BUFFER_SIZE];

    private LocalSocket receiver, sender;
    private LocalServerSocket lss;
    private Thread audioThread;
    private boolean bAudioPlaying = false;

    private  MytabOpera mytabOpera ;
    private SqliteHelper helper ;
    private  String phoneName = Build.MODEL;

    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SUCCESS:
                    Object str = msg.obj;
                    sucCount++;
                    resultText.setText((CharSequence) str);
                    resultText.setGravity(Gravity.CENTER);

                    mytabOpera = new MytabOpera(helper.getWritableDatabase());
                    mytabOpera.insert(phoneName,Build.VERSION.INCREMENTAL,TAG,parTime,"SUC");
                    Log.e(TAG, "添加数据到sqldb中");

                    break;

                case FAIL:
                    Object str1 = msg.obj;
                    failCount++;
                    resultText.setText((CharSequence) str1);
                    resultText.setGravity(Gravity.CENTER);

                    mytabOpera = new MytabOpera(helper.getWritableDatabase());
                    mytabOpera.insert(phoneName,Build.VERSION.INCREMENTAL,TAG,parTime,"FAIL");
                    Log.e(TAG, "添加数据到sqldb中");

                    break;


                case FINAL:

                    Object str2 = msg.obj;
                    resultText.setText((CharSequence) str2 + ",\n成功次数:" + sucCount +
                            ",失败次数:" + failCount);
                    resultText.setGravity(Gravity.CENTER);


                    DatabaseDump databaseDump = new DatabaseDump(helper.getWritableDatabase());
                    databaseDump.writeExcel(SqliteHelper.TB_NAME);
                    Log.e(TAG, "执行完成,导出数据库到内置存储中");

                    Toast.makeText(AudioRecord.this, "测试完成,正从数据库中导出到EXCEL表格中,请稍等",
                            Toast.LENGTH_SHORT)
                            .show();

                    break;

                case DOING:
                    Object str3 = msg.obj;
                    resultText.setText((CharSequence) str3);
                    resultText.setGravity(Gravity.CENTER);


                    break;


            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.mediarecordstress);

        helper = new SqliteHelper(this);
        sdExist();
        if (sdcardExists) {
            this.recordAudioSaveFileDir = new File(Environment.getExternalStorageDirectory().toString()
                    + File.separator + recDir);
            if (!recordAudioSaveFileDir.exists()) {
                recordAudioSaveFileDir.mkdirs();

            }
        }
        init();

        getAllRecordFiles();

      //  startLog();
     //   initLocalSocket();

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
        return Formatter.formatFileSize(AudioRecord.this, blockSize * availableBlocks);
    }




    // TODO: 2016/7/5 send message
    private void sendMessage(int m, String str) {
        Message msg = new Message();
        msg.what = m;
        msg.obj = str;
        handler.sendMessage(msg);

    }


    /* 获取文件夹中的内文件*/
    private void getAllRecordFiles() {

        this.recordFiles = new ArrayList<Map<String, Object>>();

        File files[] = this.recordAudioSaveFileDir.listFiles();
        if (null == files)
            return;

        for (int x = 0; x < files.length; x++) {

            Map<String, Object> fileinfo = new HashMap<String, Object>();
            fileinfo.put("imageView", R.mipmap.ic_launcher);
            fileinfo.put("text_path", files[x].getName());
            fileinfo.put("text_size", "5");
            this.recordFiles.add(fileinfo);
        }

        this.recordSimpadapter = new SimpleAdapter(this, this.recordFiles, R.layout.file_list,
                new String[]{"imageView", "text_path", "text_size"},
                new int[]{R.id._filename, R.id.text_path, R.id.text_size});
        this.listView.setAdapter(this.recordSimpadapter);


    }

    /* start service logcat */
    private void startLog() {
        Intent intent = new Intent(AudioRecord.this, LogService.class);

        startService(intent);
        Log.e(TAG, "行数-->" + getLineNumber(new Exception()) + "," + Environment.getExternalStorageDirectory()
                .getAbsolutePath() + File.separator
                + "MyApp" + File.separator + "log");
    }

    // TODO: 2016/7/5 有没有SD卡
    private void sdExist() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

            sdcardExists = true;
            Log.e(TAG, "sdcardExist-->" + sdcardExists);
        } else {
            Toast.makeText(context, "SD card is not found ", Toast.LENGTH_SHORT).show();
            sdcardExists = false;
        }

    }


    // TODO: 2016/7/5  init
    private void init() {
        this.time = (EditText) findViewById(R.id.media_times);

        time.setText("1");
        time.setGravity(Gravity.CENTER);

        this.delay = (EditText) findViewById(R.id.media_delay);
        delay.setText("10");
        delay.setGravity(Gravity.CENTER);
        this.spinner = (Spinner) findViewById(R.id._spinner);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.e(TAG, "  postion  -->" + position + ",id-->" + id);

                switch ((int) id) {
                    case 0:
                        typeLast = ".amr";
                        Log.e(TAG, "后缀名是--->" + typeLast);
                        break;

                    case 1:
                        typeLast = ".3gp";
                        Log.e(TAG, "后缀名是--->" + typeLast);
                        break;

                }


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        this.startBtn = (Button) findViewById(R.id._start);
        this.startBtn.setOnClickListener(this);
        this.stopBtn = (Button) findViewById(R.id._stop);
        this.stopBtn.setOnClickListener(this);

        this.stopBtn.setEnabled(false);

        this.savePath = (TextView) findViewById(R.id._savePath);
        savePath.setText("文件存储路径:" + recordAudioSaveFileDir.getPath());


        this.resultText = (TextView) findViewById(R.id._result);
        resultText.setText("MediaRecord Stress Test");
        resultText.setGravity(Gravity.CENTER);
        resultText.setTextColor(Color.YELLOW);
        resultText.setBackgroundColor(Color.GRAY);


        this.listView = (ListView) findViewById(R.id.listView);
//        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                if (AudioRecord.this.recordSimpadapter.getItem(position) instanceof Map) {
//                    Map<?, ?> map = (Map<?, ?>) recordSimpadapter.getItem(position);
//                    Uri uri = Uri.fromFile(new File(recordAudioSaveFileDir.toString() + File.separator
//                            + map.get("text_path")));
//                    Log.e(TAG, "选择的位置.postion-->" + position);
//
//                    Intent intent = new Intent(Intent.ACTION_VIEW);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    intent.setDataAndType(uri, "audio/amr");
//                    Log.e(TAG, "选择后播放");
//                    startActivity(intent);
//
//
//                }
//            }
//        });


        this.progressBar = (ProgressBar) findViewById(R.id.media_progress);


        Log.e(TAG, "init..... ");
    }


    /* Log的行数*/
    public static int getLineNumber(Exception e) {
        StackTraceElement[] trace = e.getStackTrace();
        if (trace == null || trace.length == 0) return -1; //
        return trace[0].getLineNumber();
    }

    // TODO: 2016/7/12 记录数据使用
    int time_Handler;
    int delay_Handler;

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id._start:

                sucCount = 0;
                failCount = 0;
                setProg = 1;
                resultText.setText("Media Record Stress");

                String timeText = time.getText().toString();
                String delaytext = delay.getText().toString();

                if ((timeText.length()==0) || timeText.equals("0")) {
                    Toast.makeText(context, "please input the right Number", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (delaytext.length()==0 || delaytext.equals("0")) {
                    Toast.makeText(context, "please input the  right delaytime", Toast.LENGTH_SHORT).show();
                    return;

                }

                time_Handler = Integer.parseInt(timeText);
                if (timeCount > time_Handler) {
                    timeCount = 1;
                } else {
                    timeCount = time_Handler;

                    Log.e(TAG, "timeCount---->" + timeCount);
                }

                delay_Handler = Integer.parseInt(delaytext);
                if (delayTime > delay_Handler) {

                    delayTime = 10;
                } else {
                    delayTime = delay_Handler;
                    Log.e(TAG, "delayTime---->" + delayTime);
                }
               // Log.e(TAG, "startbtn");

                String sdStore = getSDAvailableSize();

                sendMessage(DOING, "总执行次数:" + timeCount + ";测试时长:" + delayTime + "\n开始执行," +
                        "内置存储剩余:"+sdStore
                );

                Log.e(TAG,"start doing");


                startBtn.setEnabled(false);
                stopBtn.setEnabled(true);

                progressBar.setMax(timeCount);


                handler.postDelayed(recordRun, 3000);

                break;

            case R.id._stop:
                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);

                listView.setEnabled(true);
                progressBar.setProgress(0);
                handler.removeCallbacks(recordRun);
                Log.e(TAG, "_stopbtn");
                break;

        }
    }


    // TODO: 2016/7/13 aac录音
    Runnable aacRun = new Runnable() {
        @Override
        public void run() {
            startPlayingAudio();
            getAllRecordFiles();
            int testtime = delayTime;
            while (testtime-- > 0) {
                try {
                    Thread.sleep(1000);

                    Log.e(TAG, "录制时间--" + testtime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            stopPlayingAudio();

        }
    };

    private void stopPlayingAudio() {
        stopAudioThread();

        if (null != mediaRecorder) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void stopAudioThread() {
        if (null == audioThread) {
            audioThread.interrupt();
            audioThread = null;
        }
    }

    // TODO: 2016/7/13 开启aac录制  前期设置
    private void startPlayingAudio() {

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(6);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioChannels(1);
        mediaRecorder.setAudioSamplingRate(48000);

        mediaRecorder.setOutputFile(sender.getFileDescriptor());

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        startAudioThread();
    }

    // TODO: 2016/7/13  开启音频线程
    private void startAudioThread() {
        audioThread = new Thread(new AudioDataThread());
        audioThread.start();
    }


    // TODO: 2016/7/13 执行线程
    private class AudioDataThread implements Runnable {

        @Override
        public void run() {
            final String TESTFILE = Environment.getExternalStorageDirectory().getPath() + "/audio-test.adts";
            RandomAccessFile randomAccessFile = null;

            InputStream audioInput = null;

            try {
                audioInput = receiver.getInputStream();
                File file = new File(TESTFILE);
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                randomAccessFile = new RandomAccessFile(file, "rw");
                while (!Thread.interrupted()) {
                    while (true) {
                        if ((audioInput.read() & 0xFF) == 0xFF) {
                            audioBuffer[rtphl + 1] = (byte) audioInput.read();
                            if ((audioBuffer[rtphl + 1] & 0xF0) == 0xF0)
                                break;
                        }
                    }
                    audioBuffer[0] = (byte) 0xFF; // 前12位为同步头，帧的开始 0xFFF

                    audioInput.read(audioBuffer, rtphl + 2, 5); // 读取音频头的后5个字节，之前读了两字节，一共7字节

                    //  boolean protection = (audioBuffer[rtphl + 1] & 0x01) > 0 ? true : false;

                    int frameLength = ((audioBuffer[rtphl + 3] & 0x03) << 11) | ((audioBuffer[rtphl + 4] & 0xFF) << 3)
                            | ((audioBuffer[rtphl + 5] & 0xFF) >> 5);

                    //frameLength -= (protection ? 7 : 9);


                    int length = audioInput.read(audioBuffer, rtphl + 7, frameLength - 7);
                    Log.i("audio length", "audio length = " + frameLength + ", " + length);
                    randomAccessFile.write(audioBuffer, rtphl, length + 7);
                }
            } catch (IOException e) {

                e.printStackTrace();
                return;
            } finally {
                try {
                    randomAccessFile.close();
                    audioInput.close();

                    receiver.close();
                    sender.close();
                    lss.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    int parTime;

    // TODO: 2016/7/5  录音线程
    private void mediaStress() {
        mediaRecord = new MediaRecorder();
        mediaRecord.setAudioSource(MediaRecorder.AudioSource.MIC);
        Log.e(TAG, "开始选择录制的格式");

        if (typeLast.equals(".amr")) {
            Log.e(TAG, "选择录制的格式--->" + typeLast);
            AudioRecord.this.mediaRecord.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        } else if (typeLast.equals(".3gp")) {
            Log.e(TAG, "选择录制的格式--->" + typeLast);
            AudioRecord.this.mediaRecord.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        }

        recordAudioSaveFileName = recordAudioSaveFileDir.toString() + File.separator + "MediaStress_"
                + System.currentTimeMillis() + "_" +
                timeCount + typeLast;
        Log.e(TAG, "录制的文件名称路径--->" + recordAudioSaveFileName);


        recordAudioSaveFile = new File(recordAudioSaveFileName);
        mediaRecord.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

        mediaRecord.setOutputFile(recordAudioSaveFileName);

        mediaRecord.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                mediaRecord.stop();
                mediaRecord.release();
                mediaRecord = null;
                isRecording = false;
                Log.e(TAG, "Media Record Make a Mistake");
                Toast.makeText(AudioRecord.this, "录音发生错误", Toast.LENGTH_SHORT).show();
            }
        });


        try {
            mediaRecord.prepare();

        } catch (IOException e) {
            e.printStackTrace();

            sendMessage(FAIL, "录音失败 !");
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaRecord.start();


        Toast.makeText(AudioRecord.this, "录音开始", Toast.LENGTH_SHORT).show();
        isRecording = true;

        // TODO: 2016/7/5 testtime用作录制时长的计数器
        int testtime = delayTime;
        while (testtime-- > 0) {
            try {
                Thread.sleep(1000);

                Log.e(TAG, "录制时间--" + testtime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mediaRecord.stop();
        mediaRecord.release();
        Log.e(TAG, "录制over--");
        isRecording = false;
        Toast.makeText(context, typeLast + "文件已经录制完成", Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onDestroy() {
        if (isRecording) {

            mediaRecord.stop();
            mediaRecord.release();
            mediaRecord = null;
        }

        AudioRecord.this.finish();
        Log.e(TAG,"deletetable  & destroy ");
        super.onDestroy();

    }

    /*隐藏输入框架*/
    private void hideinput() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        Log.e(TAG, "隐藏输入法了");
    }


    int setProg = 1;

    // TODO: 2016/7/5 录音线程
    Runnable recordRun = new Runnable() {
        @Override
        public void run() {
            // TODO: 2016/7/5 开始录制后,开始不能在点击,停止激活

            listView.setVisibility(View.INVISIBLE);
            /*开始进行音频的录制 start*/
            progressBar.setProgress(setProg++);
            mediaStress();
            getAllRecordFiles();

            /**
             * 显示
             * */
            listView.setVisibility(View.VISIBLE);

            parTime = timeCount;
            timeCount--;

            Log.e(TAG, "当前正在测试的次数-->" + parTime);
            String msgString = "总执行次数:" + time_Handler + ";测试时长:" + delayTime + ";选择格式:" + typeLast +
                    ";\n当前执行到:" + parTime + "次"+"内置存储剩余:"+getSDAvailableSize();
            sendMessage(SUCCESS, msgString);
            Log.e(TAG,"执行成功一次 ");

            if (parTime > 1) {
                parTime = timeCount;
                handler.postDelayed(recordRun, 3000);

            } else {
                handler.removeCallbacks(recordRun);
                String SUCC = msgString;
                sendMessage(FINAL, SUCC);
                listView.setEnabled(true);
                //   progressBar.setProgress(parTime);
                stopBtn.setEnabled(false);
                startBtn.setEnabled(true);

            }


        }
    };


}
