package com.example.advanceDemo.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.CameraLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.onCameraLayerPreviewListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.DrawPadCameraView;
import com.lansosdk.videoeditor.DrawPadCameraView.onViewAvailable;
import com.lansosdk.videoeditor.FilterLibrary;
import com.lansosdk.videoeditor.FilterLibrary.FilterAdjuster;
import com.lansosdk.videoeditor.FilterLibrary.OnGpuImageFilterChosenListener;
import com.lansosdk.videoeditor.LanSongUtil;
import com.lansosdk.videoeditor.SDKFileUtils;

import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageFilter;

public class CameraLayerRectActivity2 extends Activity implements
        Handler.Callback, OnClickListener {

    public static final int REQUEST_VIDEOPROCESS = 5;
    private static final long RECORD_CAMERA_TIME = 10 * 1000 * 1000; // 定义录制的时间为20s
    private static final String TAG = "CameraLayerDemoActivity";
    private static final int MSG_CHANGE_FLASH = 66;
    private static final int MSG_CHANGE_CAMERA = 8;
    private DrawPadCameraView mDrawPadCamera;
    private CameraLayer mCameraLayer = null;
    private String dstPath = null;
    private PowerManager.WakeLock mWakeLock;
    private FilterAdjuster mFilterAdjuster;
    // -------------------------------------------一下是UI界面和控制部分.---------------------------------------------------
    private SeekBar AdjusterFilter;
    private TextView tvTime;
    private LinearLayout playVideo;
    private onDrawPadProgressListener drawPadProgressListener = new onDrawPadProgressListener() {

        @Override
        public void onProgress(DrawPad v, long currentTimeUs) {
            if (currentTimeUs >= RECORD_CAMERA_TIME) {
                stopDrawPad();
            }
            if (tvTime != null) {
                tvTime.setVisibility(View.VISIBLE);
                long left = RECORD_CAMERA_TIME - currentTimeUs;
                float leftF = ((float) left / 1000000);
                float b = (float) (Math.round(leftF * 10)) / 10; // 保留一位小数.

                tvTime.setText(String.valueOf(b));
            }
        }
    };
    // -----------------------------------------------------------------------
    private Handler handler;
    private boolean mAllowTouchFocus = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cameralayer_demo_layout);

        if (LanSongUtil.checkRecordPermission(getBaseContext()) == false) {
            Toast.makeText(getApplicationContext(), "请打开权限后,重试!!!",
                    Toast.LENGTH_LONG).show();
            finish();
        }

        mDrawPadCamera = (DrawPadCameraView) findViewById(R.id.id_cameralayer_padview);

        initView();

        dstPath = SDKFileUtils.newMp4PathInBox();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initDrawPad(); // 开始录制.
            }
        }, 200);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                    TAG);
            mWakeLock.acquire();
        }
        tvTime.setVisibility(View.INVISIBLE);
        playVideo.setVisibility(View.INVISIBLE);
    }

    /**
     * Step1: 开始运行 DrawPad 容器
     */
    private void initDrawPad() {
        /**
         * 当前CameraLayer 支持全屏和 正方形的宽高比,
         */
        int padWidth = 480;
        int padHeight = 480;

        mDrawPadCamera.setRecordMic(true);
//        mDrawPadCamera.setCameraParam(true, null, true);
        mDrawPadCamera.setCameraParam(true,null,720,1280);

        mDrawPadCamera.setRealEncodeEnable(padWidth, padHeight, 1000000,(int) 25, dstPath);
        mDrawPadCamera.setOnDrawPadProgressListener(drawPadProgressListener);

        // 设置当前DrawPad的宽度和高度,并把宽度自动缩放到父view的宽度,然后等比例调整高度.
        mDrawPadCamera.setDrawPadSize(padWidth, padHeight,
                new onDrawPadSizeChangedListener() {
                    @Override
                    public void onSizeChanged(int viewWidth, int viewHeight) {
                        startDrawPad();
                    }
                });
        mDrawPadCamera.setOnViewAvailable(new onViewAvailable() {

            @Override
            public void viewAvailable(DrawPadCameraView v) {
                startDrawPad();
            }
        });
        mDrawPadCamera.setOnDrawPadErrorListener(new onDrawPadErrorListener() {

            @Override
            public void onError(DrawPad d, int what) {
                Log.e(TAG, "DrawPad容器线程运行出错!!!" + what);
            }
        });
    }

    /**
     * Step2: 开始录制
     */
    private void startDrawPad() {
        if (mDrawPadCamera.setupDrawpad()) {
            mCameraLayer = mDrawPadCamera.getCameraLayer();

            /**
             * 可以在这里增加别的图层.
             */
            mDrawPadCamera.startPreview();
            mDrawPadCamera.startRecord();
        }
    }

    /**
     * Step3: 停止容器 停止后,为新的视频文件增加上音频部分.
     */
    private void stopDrawPad() {
        if (mDrawPadCamera != null && mDrawPadCamera.isRunning()) {
            mDrawPadCamera.stopDrawPad();
            mCameraLayer = null;
            playVideo.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 选择滤镜效果,
     */
    private void selectFilter() {
        if (mDrawPadCamera != null && mDrawPadCamera.isRunning()) {
            FilterLibrary.showDialog(this,
                    new OnGpuImageFilterChosenListener() {

                        @Override
                        public void onGpuImageFilterChosenListener(
                                final GPUImageFilter filter, String name) {
                            mFilterAdjuster = new FilterAdjuster(filter);
                            if (mCameraLayer != null) {
                                mCameraLayer.switchFilterTo(filter);
                                findViewById(R.id.id_cameralayer_demo_seek1)
                                        .setVisibility(
                                                mFilterAdjuster.canAdjust() ? View.VISIBLE
                                                        : View.GONE);
                            }
                        }
                    });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDrawPadCamera != null) {
            mDrawPadCamera.stopDrawPad();
        }
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SDKFileUtils.deleteFile(dstPath);
        dstPath = null;
    }

    private void initView() {
        tvTime = (TextView) findViewById(R.id.id_cameralayer_timetv);
        playVideo = (LinearLayout) findViewById(R.id.id_cameralayer_saveplay);

        playVideo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (SDKFileUtils.fileExist(dstPath)) {
                    Intent intent = new Intent(CameraLayerRectActivity2.this,
                            VideoPlayerActivity.class);
                    intent.putExtra("videopath", dstPath);
                    startActivity(intent);
                } else {
                    Toast.makeText(CameraLayerRectActivity2.this, "目标文件不存在",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        playVideo.setVisibility(View.GONE);
        findViewById(R.id.id_cameralayer_flashlight).setOnClickListener(this);
        findViewById(R.id.id_cameralayer_frontcamera).setOnClickListener(this);
        findViewById(R.id.id_camerape_demo_selectbtn).setOnClickListener(this);

        handler = new Handler(this);

        AdjusterFilter = (SeekBar) findViewById(R.id.id_cameralayer_demo_seek1);
        AdjusterFilter
                .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        if (mFilterAdjuster != null) {
                            mFilterAdjuster.adjust(progress);
                        }
                    }
                });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_cameralayer_frontcamera:
                if (mCameraLayer != null && CameraLayer.isSupportFrontCamera()) {
                    handler.sendEmptyMessage(MSG_CHANGE_CAMERA);
                }
                break;
            case R.id.id_cameralayer_flashlight:
                if (mCameraLayer != null) {
                    mCameraLayer.changeFlash();
                }
                break;
            case R.id.id_camerape_demo_selectbtn:
                selectFilter();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_CHANGE_CAMERA:
                mCameraLayer.changeCamera();
                break;
            case MSG_CHANGE_FLASH:
                mCameraLayer.changeFlash();
                break;
        }
        return false;
    }
}
