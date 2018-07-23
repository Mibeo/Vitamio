package com.example.bingjiazheng.vitamio;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import io.vov.vitamio.utils.StringUtils;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

import static com.example.bingjiazheng.vitamio.Content.Hide_Channels;
import static com.example.bingjiazheng.vitamio.Content.Show_Channels;

/**
 * Created by Mibeo on 2018/1/22.
 */

public class MyMediaController extends MediaController implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private boolean firstScroll = false;// 每次触摸屏幕后，第一次scroll的标志
    private int GESTURE_FLAG = 0;// 1,调节进度，2，调节音量,3.调节亮度
    private static final int GESTURE_MODIFY_PROGRESS = 1;
    private static final int GESTURE_MODIFY_VOLUME = 2;
    private static final int GESTURE_MODIFY_BRIGHT = 3;
    private static final int HIDEFRAM = 0;//控制提示窗口的显示
    private static final int SHOW_PROGRESS = 2;
    private GestureDetector mGestureDetector;
    private ImageButton img_back;//返回键
    private ImageView img_Battery;//电池电量显示
    private TextView textViewTime;//时间提示
    private TextView textCurrentTime;//当前时间提示
    private TextView textViewBattery;//文字显示电池
    private VideoView videoView;
    private Activity activity;
    private Context context;
    private int controllerWidth = 0;//设置mediaController高度为了使横屏时top显示在屏幕顶端

    private View mVolumeBrightnessLayout;//提示窗口
    private ImageView mOperationBg;//提示图片
    private TextView mOperationTv;//提示文字
    private AudioManager mAudioManager;

    private TextView mCurrentTime;
    private SeekBar mProgress;
    private MediaPlayerControl player;
    //最大声音
    private int mMaxVolume;
    // 当前声音
    private int mVolume = -1;
    //当前亮度
    private float mBrightness = -1f;





    //返回监听
    private View.OnClickListener backListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (activity != null) {
                activity.finish();
            }
        }
    };
    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long pos;
            switch (msg.what) {
                case HIDEFRAM://隐藏提示窗口
                    mVolumeBrightnessLayout.setVisibility(View.GONE);
                    mOperationTv.setVisibility(View.GONE);
                    break;
            }
        }
    };
    private int progress;
    private boolean progress_turn = true;
    private boolean is_show =true;

    //videoview用于对视频进行控制的等，activity为了退出
    public MyMediaController(Context context, VideoView videoView, Activity activity,boolean fromXml,View container) {
        super(context);
        this.context = context;
        this.videoView = videoView;
        this.activity = activity;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        controllerWidth = wm.getDefaultDisplay().getWidth();
        mGestureDetector = new GestureDetector(context, new MyGestureListener());
        mFromXml = fromXml;
        mRoot = makeControllerView();
        if(container instanceof FrameLayout){
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            //在父View底部
            p.gravity = Gravity.BOTTOM;
            mRoot.setLayoutParams(p);
            ((FrameLayout)container).addView(mRoot);
        }
    }

    @Override
    protected View makeControllerView() {
        View v = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(getResources().getIdentifier("mymediacontroller", "layout", getContext().getPackageName()), this);
        v.setMinimumHeight(controllerWidth);
//        img_back = (ImageButton) v.findViewById(getResources().getIdentifier("mediacontroller_top_back", "id", context.getPackageName()));
        img_Battery = (ImageView) v.findViewById(getResources().getIdentifier("mediacontroller_imgBattery", "id", context.getPackageName()));
//        img_back.setOnClickListener(backListener);
        textViewBattery = (TextView) v.findViewById(getResources().getIdentifier("mediacontroller_Battery", "id", context.getPackageName()));
        textViewTime = (TextView) v.findViewById(getResources().getIdentifier("mediacontroller_time", "id", context.getPackageName()));
        textCurrentTime = (TextView) v.findViewById(getResources().getIdentifier("mediacontroller_time_current", "id", context.getPackageName()));

        img_back = v.findViewById(R.id.mediacontroller_top_back);
        img_back.setOnClickListener(this);
        mVolumeBrightnessLayout = (RelativeLayout) v.findViewById(R.id.operation_volume_brightness);
        mOperationBg = (ImageView) v.findViewById(R.id.operation_bg);
        mOperationTv = (TextView) v.findViewById(R.id.operation_tv);
        mOperationTv.setVisibility(View.GONE);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mProgress = (SeekBar) v.findViewById(getResources().getIdentifier("mediacontroller_seekbar", "id", context.getPackageName()));
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seekBar = (SeekBar) mProgress;
                seekBar.setOnSeekBarChangeListener(this);
            }
            mProgress.setMax(100);
        }
        return v;
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        System.out.println("MYApp-MyMediaController-dispatchKeyEvent");
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) return true;
        // 处理手势结束
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                endGesture();
                if (progress_turn) {
                    onFinishSeekBar();
                    progress_turn = false;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 获取进度条进度
     *
     * @return
     */
    public int getProgress() {
        if (mProgress != null)
            return mProgress.getProgress();
        return 0;
    }

    /**
     * 改变进度条进度
     *
     * @param progress
     * @return
     */
    public String setSeekBarChange(int progress) {
        if (mProgress == null) return "";
        mProgress.setProgress(progress);
        long newposition = (mDuration * progress) / 1000;
        String time = StringUtils.generateTime(newposition);
        if (mInstantSeeking)
            mPlayer.seekTo(newposition);
        if (mInfoView != null)
            mInfoView.setText(time);
        if (mCurrentTime != null)
            mCurrentTime.setText(time);
        return time;
    }

    /**
     * 进度条开始改变
     */
    public void onStartSeekBar() {
        if (mAM == null) return;
        mDragging = true;
        show(3600000);
        mHandler.removeMessages(SHOW_PROGRESS);
        if (mInstantSeeking)
            mAM.setStreamMute(AudioManager.STREAM_MUSIC, true);
        if (mInfoView != null) {
            mInfoView.setText("");
        }
    }

    /**
     * 进度条变化停止
     */
    public void onFinishSeekBar() {
        if (mProgress == null) return;
        if (!mInstantSeeking)
            mPlayer.seekTo((mDuration * mProgress.getProgress()) / 1000);
        System.out.println("MediaController-" + (mDuration * mProgress.getProgress()) / 1000);
        System.out.println("MediaController-" + mProgress.getProgress());
        if (mInfoView != null) {
            mInfoView.setText("");
            mInfoView.setVisibility(View.GONE);
        }
        show(sDefaultTimeout);
        mHandler.removeMessages(SHOW_PROGRESS);
        mAM.setStreamMute(AudioManager.STREAM_MUSIC, false);
        mDragging = false;
        mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 1000);
    }

    /**
     * 手势结束
     */
    private void endGesture() {
        mVolume = -1;
        mBrightness = -1f;
        GESTURE_FLAG = 0;
        // 隐藏
        myHandler.removeMessages(HIDEFRAM);
        myHandler.sendEmptyMessageDelayed(HIDEFRAM, 1);

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        textCurrentTime.setText(setSeekBarChange(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.mediacontroller_top_back:
                if(is_show){
                    MainActivity.handler.sendEmptyMessage(Show_Channels);
                    is_show = false;
                }else {
                    MainActivity.handler.sendEmptyMessage(Hide_Channels);
                    is_show = true;
                }

                break;
        }
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            //当收拾结束，并且是单击结束时，控制器隐藏/显示
            toggleMediaControlsVisiblity();
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            progress = getProgress();
            firstScroll = true;
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float beginX = e1.getX();
            float endX = e2.getX();
            float beginY = e1.getY();
            float endY = e2.getY();

            Display disp = activity.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            disp.getSize(size);
            int windowWidth = size.x;
            int windowHeight = size.y;
            if (firstScroll) {
                if (Math.abs(distanceX) >= Math.abs(distanceY)) {
                    GESTURE_FLAG = GESTURE_MODIFY_PROGRESS;
                } else {
                    if (beginX > windowWidth / 2) {
                        GESTURE_FLAG = GESTURE_MODIFY_VOLUME;
                    } else if (beginX < windowWidth / 2) {
                        GESTURE_FLAG = GESTURE_MODIFY_BRIGHT;
                    }
                }
            }
            if (GESTURE_FLAG == GESTURE_MODIFY_PROGRESS) {
                onSeekTo((endX - beginX) / 20);
            } else if (GESTURE_FLAG == GESTURE_MODIFY_VOLUME) {
                onVolumeSlide((beginY - endY) / windowHeight);
            } else if (GESTURE_FLAG == GESTURE_MODIFY_BRIGHT) {
                onBrightnessSlide((beginY - endY) / windowHeight);
            }
            /*if (Math.abs(endX - beginX) < Math.abs(beginY - endY)) {//上下滑动
                if (beginX > windowWidth * 3.0 / 4.0) {// 右边滑动 屏幕3/5
                    onVolumeSlide((beginY - endY) / windowHeight);
                } else if (beginX < windowWidth * 1.0 / 4.0) {// 左边滑动 屏幕2/5
                    onBrightnessSlide((beginY - endY) / windowHeight);
                }
            } else {//左右滑动
                onSeekTo((endX - beginX) / 20);
            }*/
            firstScroll = false;
            return super.onScroll(e1, e2, distanceX, distanceY);
        }


        //双击暂停或开始
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            playOrPause();
            return true;
        }
    }

    /**
     * 滑动改变声音大小
     *
     * @param percent
     */
    private void onVolumeSlide(float percent) {
        if (mVolume == -1) {
            mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (mVolume < 0)
                mVolume = 0;

            // 显示
            mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
            mOperationTv.setVisibility(VISIBLE);
        }

        int index = (int) (percent * mMaxVolume) + mVolume;
        if (index > mMaxVolume)
            index = mMaxVolume;
        else if (index < 0)
            index = 0;
        if (index >= 10) {
            mOperationBg.setImageResource(R.mipmap.volmn_100);
        } else if (index >= 5 && index < 10) {
            mOperationBg.setImageResource(R.mipmap.volmn_60);
        } else if (index > 0 && index < 5) {
            mOperationBg.setImageResource(R.mipmap.volmn_30);
        } else {
            mOperationBg.setImageResource(R.mipmap.volmn_no);
        }
        //DecimalFormat    df   = new DecimalFormat("######0.00");
        mOperationTv.setText((int) (((double) index / mMaxVolume) * 100) + "%");
        // 变更声音
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

    }

    /**
     * 滑动改变亮度
     * <p>
     * //     * @param percent
     */
    private void onBrightnessSlide(float percent) {
        if (mBrightness < 0) {
            mBrightness = activity.getWindow().getAttributes().screenBrightness;
            if (mBrightness <= 0.00f)
                mBrightness = 0.50f;
            if (mBrightness < 0.01f)
                mBrightness = 0.01f;

            // 显示
            //mOperationBg.setImageResource(R.drawable.video_brightness_bg);
            mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
            mOperationTv.setVisibility(VISIBLE);

        }


        WindowManager.LayoutParams lpa = activity.getWindow().getAttributes();
        lpa.screenBrightness = mBrightness + percent;
        if (lpa.screenBrightness > 1.0f)
            lpa.screenBrightness = 1.0f;
        else if (lpa.screenBrightness < 0.01f)
            lpa.screenBrightness = 0.01f;
        activity.getWindow().setAttributes(lpa);

        mOperationTv.setText((int) (lpa.screenBrightness * 100) + "%");
        if (lpa.screenBrightness * 100 >= 90) {
            mOperationBg.setImageResource(R.mipmap.light_100);
        } else if (lpa.screenBrightness * 100 >= 80 && lpa.screenBrightness * 100 < 90) {
            mOperationBg.setImageResource(R.mipmap.light_90);
        } else if (lpa.screenBrightness * 100 >= 70 && lpa.screenBrightness * 100 < 80) {
            mOperationBg.setImageResource(R.mipmap.light_80);
        } else if (lpa.screenBrightness * 100 >= 60 && lpa.screenBrightness * 100 < 70) {
            mOperationBg.setImageResource(R.mipmap.light_70);
        } else if (lpa.screenBrightness * 100 >= 50 && lpa.screenBrightness * 100 < 60) {
            mOperationBg.setImageResource(R.mipmap.light_60);
        } else if (lpa.screenBrightness * 100 >= 40 && lpa.screenBrightness * 100 < 50) {
            mOperationBg.setImageResource(R.mipmap.light_50);
        } else if (lpa.screenBrightness * 100 >= 30 && lpa.screenBrightness * 100 < 40) {
            mOperationBg.setImageResource(R.mipmap.light_40);
        } else if (lpa.screenBrightness * 100 >= 20 && lpa.screenBrightness * 100 < 20) {
            mOperationBg.setImageResource(R.mipmap.light_30);
        } else if (lpa.screenBrightness * 100 >= 10 && lpa.screenBrightness * 100 < 20) {
            mOperationBg.setImageResource(R.mipmap.light_20);
        }

    }


    public void setTime(String time) {
        if (textViewTime != null)
            textViewTime.setText(time);
    }

    //显示电量，
    public void setBattery(String stringBattery) {
        if(textViewTime != null && img_Battery != null){
            textViewBattery.setText(stringBattery + "%");
            int battery = Integer.valueOf(stringBattery);
            if(battery < 15)img_Battery.setImageDrawable(getResources().getDrawable(R.mipmap.battery_15));
            if(battery < 30 && battery >= 15)img_Battery.setImageDrawable(getResources().getDrawable(R.mipmap.battery_15));
            if(battery < 45 && battery >= 30)img_Battery.setImageDrawable(getResources().getDrawable(R.mipmap.battery_30));
            if(battery < 60 && battery >= 45)img_Battery.setImageDrawable(getResources().getDrawable(R.mipmap.battery_45));
            if(battery < 75 && battery >= 60)img_Battery.setImageDrawable(getResources().getDrawable(R.mipmap.battery_60));
            if(battery < 90 && battery >= 75)img_Battery.setImageDrawable(getResources().getDrawable(R.mipmap.battery_75));
            if(battery > 90 )img_Battery.setImageDrawable(getResources().getDrawable(R.mipmap.battery_90));
        }
    }

    //隐藏/显示
    private void toggleMediaControlsVisiblity() {
        if (isShowing()) {
            hide();
        } else {
            show();
        }
    }

    //播放与暂停
    private void playOrPause() {
        if (videoView != null)
            if (videoView.isPlaying()) {
                videoView.pause();
            } else {
                videoView.start();
            }
    }

    /**
     * 滑动改变播放进度
     *
     * @param percent
     */
    private void onSeekTo(float percent) {
        //计算并显示 前进后退
        if (!progress_turn) {
            onStartSeekBar();
            progress_turn = true;
        }
        int change = (int) (percent);
        if (change > 0) {
            mOperationBg.setImageResource(R.drawable.right);
        } else {
            mOperationBg.setImageResource(R.drawable.left);
        }
        mOperationTv.setVisibility(View.VISIBLE);

        mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
        if (progress + change > 0) {
            if ((progress + change < 1000))
                mOperationTv.setText(setSeekBarChange(progress + change) + "/" + StringUtils.generateTime(videoView.getDuration()));
            else
                mOperationTv.setText(setSeekBarChange(1000) + "/" + StringUtils.generateTime(videoView.getDuration()));
        } else {
            mOperationTv.setText(setSeekBarChange(0) + "/" + StringUtils.generateTime(videoView.getDuration()));

        }
    }
}
