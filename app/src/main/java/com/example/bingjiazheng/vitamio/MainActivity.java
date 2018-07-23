package com.example.bingjiazheng.vitamio;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.Vitamio;
import io.vov.vitamio.utils.Log;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

public class MainActivity extends AppCompatActivity implements Runnable {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };
    private static final int TIME = 0;
    private static final int BATTERY = 1;

    String path1 = Environment.getExternalStorageDirectory()+ "/1.mp4";
    String path2 = "http://10.100.8.204/2.mp4";
    String path3 = "http://10.100.8.204/2.mp4";
    private VideoView videoView;
    private MediaController mediaController;
    private MyMediaController myMediaController;
    private FrameLayout frameLayout;
    private  int videoHeight;
    private RelativeLayout is_show_channel_layout;
    public static Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case Content.Show_Channels:

                    break;
                case Content.Hide_Channels:

                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        verifyStoragePermissions(this);
        //定义全屏参数
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        //获得当前窗体对象
        Window window = MainActivity.this.getWindow();
        //设置当前窗体为全屏显示
        window.setFlags(flag, flag);
        Vitamio.isInitialized(getApplicationContext());
        getWindow().getDecorView().setSystemUiVisibility(View.INVISIBLE);//消除状态栏

        setContentView(R.layout.activity_main);
        frameLayout = (FrameLayout)findViewById(R.id.framelayout);
        is_show_channel_layout = findViewById(R.id.is_show_channel_layout);
        is_show_channel_layout.setVisibility(View.INVISIBLE);
        registerBoradcastReceiver();
        new Thread(this).start();
        playfunction();
        myOnClick();
        /*videoView = findViewById(R.id.surface_view);
        mediaController = new MediaController(this);
//        videoView.setVideoPath(path1);
        videoView.setVideoURI(Uri.parse(path2));

        videoView.setMediaController(mediaController);
        videoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);//高画质
        mediaController.show(5000);
        videoView.setVideoLayout(2,0);
        videoView.requestFocus();*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(batteryBroadcastReceiver);
        } catch (IllegalArgumentException ex) {

        }
    }
    public void registerBoradcastReceiver() {
        //注册电量广播监听
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryBroadcastReceiver, intentFilter);

    }
    @Override
    public void run() {
        // TODO Auto-generated method stub
        while (true) {
            //时间读取线程
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            String str = sdf.format(new Date());
            Message msg = new Message();
            msg.obj = str;
            msg.what = TIME;
            mHandler.sendMessage(msg);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 监听
     */
    private void myOnClick(){
        //设置一下监听：播放完成的监听，播放准备好的监听，播放出错的监听

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                // TODO Auto-generated method stub
                //开始播放
                videoView.start();
//                progressDialog2.dismiss();
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                Toast.makeText(getApplicationContext(), "视频播放完成了", Toast.LENGTH_SHORT).show();
                finish();//退出播放器
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(getApplicationContext(), "视频播放出错了",  Toast.LENGTH_SHORT).show();
                return true;
            }
        });
/**
 * 缓冲设置
 *
 */
        videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        if (videoView.isPlaying()) {
                            videoView.pause();

                        }
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        videoView.start();

                        videoHeight = videoView.getHeight();
                        RelativeLayout.LayoutParams params = new
                                RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                                ,videoHeight);
                        /*LinearLayout.LayoutParams params = new
                                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                                , videoHeight);*/
                        frameLayout.setLayoutParams(params);
                        break;
                    case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:

                        break;
                }
                return true;
            }

        });
//        videoView.setBufferSize(512);
        myMediaController = new MyMediaController(this,videoView,this,true,frameLayout);

        videoView.setMediaController(myMediaController);

        videoView.requestFocus();
        videoView.setBufferSize(1024);
    }
    public 	void playfunction() {
        String path = "";
//		path = "http://gslb.miaopai.com/stream/oxX3t3Vm5XPHKUeTS-zbXA__.mp4";
//        path = "http://gslb.miaopai.com/stream/oxX3t3Vm5XPHKUeTS-zbXA__.mp4";
        path = Environment.getExternalStorageDirectory()+ "/1.mp4";
//        path = "http://10.100.8.204/2.mp4";
        videoView = (VideoView) findViewById(R.id.surface_view);
        if (path == "") {
            // Tell the user to provide a media file URL/path.
            Toast.makeText(this, "路径错误", Toast.LENGTH_LONG)
                    .show();
            return;
        } else {
            videoView.setVideoPath(path);
//            videoView.setVideoURI(Uri.parse(path3));

            videoView
                    .setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            // optional need Vitamio 4.0
                            mediaPlayer.setPlaybackSpeed(1.0f);

                        }
                    });

        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // 检测屏幕的方向：纵向或横向
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //当前为横屏， 在此处添加额外的处理代码
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.INVISIBLE);//消除状态栏


            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);

            int width = dm.widthPixels;
            int height = dm.heightPixels;
//            vWidth = width;
            ViewGroup.LayoutParams lp = videoView.getLayoutParams();
            lp.width = width;
            lp.height = height;
            LinearLayout.LayoutParams params = new
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                    ,height);
            frameLayout.setLayoutParams(params);
            videoView.setLayoutParams(lp);
            //     getWindow().getDecorView().setSystemUiVisibility(View.INVISIBLE);//显示状态栏

        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            //当前为竖屏， 在此处添加额外的处理代码
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);

            int width = dm.widthPixels;
            int height = dm.widthPixels*9/16;
            ViewGroup.LayoutParams lp = videoView.getLayoutParams();
            lp.width = width;
            lp.height = height;
            LinearLayout.LayoutParams params = new
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                    ,height);
            frameLayout.setLayoutParams(params);
            videoView.setLayoutParams(lp);
//            getWindow().getDecorView().setSystemUiVisibility(View.VISIBLE);//显示状态栏

        }
    }
    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.READ_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private BroadcastReceiver batteryBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())){
                //获取当前电量
                int level = intent.getIntExtra("level", 0);
                //电量的总刻度
                int scale = intent.getIntExtra("scale", 100);
                //把它转成百分比
                //tv.setText("电池电量为"+((level*100)/scale)+"%");
                Message msg = new Message();
                msg.obj = (level*100)/scale+"";
                msg.what = BATTERY;
                mHandler.sendMessage(msg);
            }
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TIME:
                    myMediaController.setTime(msg.obj.toString());
                    break;
                case BATTERY:
                    myMediaController.setBattery(msg.obj.toString());
                    break;
            }
        }
    };
}
