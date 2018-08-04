package com.example.advanceDemo.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.example.advanceDemo.view.CameraProgressBar;
import com.example.advanceDemo.view.FocusImageView;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CameraLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.SubLayer;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.videoeditor.DrawPadCameraView;
import com.lansosdk.videoeditor.DrawPadCameraView.doFousEventListener;
import com.lansosdk.videoeditor.DrawPadCameraView.onViewAvailable;
import com.lansosdk.videoeditor.LanSongUtil;
import com.lansosdk.videoeditor.SDKFileUtils;

import jp.co.cyberagent.lansongsdk.gpuimage.IF1977Filter;
import jp.co.cyberagent.lansongsdk.gpuimage.IFAmaroFilter;
import jp.co.cyberagent.lansongsdk.gpuimage.IFEarlybirdFilter;
import jp.co.cyberagent.lansongsdk.gpuimage.IFNashvilleFilter;

public class CameraSubLayerDemo1Activity extends Activity implements
        OnClickListener {

    private static final int RECORD_CAMERA_MAX = 15 * 1000 * 1000; // 定义录制的时间为30s

    private static final int RECORD_CAMERA_MIN = 2 * 1000 * 1000; // 定义最小2秒

    private static final String TAG = "CameraSubLayerDemo1Activity";

    private DrawPadCameraView mDrawPadCamera;

    private CameraLayer mCamLayer = null;

    private String dstPath = null; // 用于录制完成后的目标视频路径.

    private FocusImageView focusView;

    private PowerManager.WakeLock mWakeLock;
    private TextView tvTime;
    private Context mContext = null;

    private ImageView btnOk;
    private CameraProgressBar mProgressBar = null;

    private TextView tvSubLayerHint;
    private onDrawPadProgressListener drawPadProgressListener = new onDrawPadProgressListener() {

        @Override
        public void onProgress(DrawPad v, long currentTimeUs) {
            // TODO Auto-generated method stub

            if (currentTimeUs >= RECORD_CAMERA_MIN && btnOk != null) {
                btnOk.setVisibility(View.VISIBLE);
            }

            if (currentTimeUs >= RECORD_CAMERA_MAX) {
                stopDrawPad();
                playVideo();
            }
            if (tvTime != null) {
                float timeF = ((float) currentTimeUs / 1000000);
                float b = (float) (Math.round(timeF * 10)) / 10; // 保留一位小数.

                if (b >= 0)
                    tvTime.setText(String.valueOf(b));
            }
            if (mProgressBar != null) {
                mProgressBar.setProgress((int) (currentTimeUs / 1000));
            }
        }
    };
    private BitmapLayer bmpLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏模式下, 隐藏底部的虚拟按键.
        LanSongUtil.hideBottomUIMenu(this);
        mContext = getApplicationContext();

        if (LanSongUtil.checkRecordPermission(getBaseContext()) == false) {
            Toast.makeText(getApplicationContext(), "当前无权限,请打开权限后,重试!!!",
                    Toast.LENGTH_LONG).show();
            finish();
        }

        setContentView(R.layout.camera_sublayerdemo_layout);
        mDrawPadCamera = (DrawPadCameraView) findViewById(R.id.id_fullrecord_padview);

        initView();
        mProgressBar.setMaxProgress(RECORD_CAMERA_MAX / 1000);
        mProgressBar
                .setOnProgressTouchListener(new CameraProgressBar.OnProgressTouchListener() {
                    @Override
                    public void onClick(CameraProgressBar progressBar) {

                        if (mDrawPadCamera != null) {
                            /**
                             * 这里只是暂停和恢复录制, 可以录制多段,但不可以删除录制好的每一段,
                             *
                             * 如果你要分段录制,并支持回删,则可以采用SegmentStart和SegmentStop;
                             */
                            if (mDrawPadCamera.isRecording()) {
                                mDrawPadCamera.pauseRecord(); // 暂停录制,如果要停止录制
                            } else {
                                mDrawPadCamera.startRecord();
                            }
                        }
                    }
                });
        dstPath = SDKFileUtils.newMp4PathInBox();
        initDrawPad(); // 开始录制.
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        LanSongUtil.hideBottomUIMenu(this);
        super.onResume();
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                    TAG);
            mWakeLock.acquire();
        }
        startDrawPad();
    }

    /**
     * Step1: 开始运行 DrawPad 容器
     */
    private void initDrawPad() {
        int padWidth = 544;
        int padHeight = 960;
        int bitrate = 3000 * 1024;
        /**
         * 设置录制时的一些参数.
         */
        mDrawPadCamera.setRealEncodeEnable(padWidth, padHeight, bitrate,
                (int) 25, dstPath);
        /**
         * 录制的同时,录制外面的声音.
         */
        mDrawPadCamera.setRecordMic(true);
        /**
         * 设置录制处理进度监听.
         */
        mDrawPadCamera.setOnDrawPadProgressListener(drawPadProgressListener);

        /**
         * 相机前后置.是否设置滤镜.
         */
        mDrawPadCamera.setCameraParam(false, null, true);

        /**
         * 当手动聚焦的时候, 返回聚焦点的位置,让focusView去显示一个聚焦的动画.
         */
        mDrawPadCamera.setCameraFocusListener(new doFousEventListener() {

            @Override
            public void onFocus(int x, int y) {
                // TODO Auto-generated method stub
                focusView.startFocus(x, y);
            }
        });
        /**
         *
         * UI界面有效后, 开始开启DrawPad线程, 来预览画面.
         */
        mDrawPadCamera.setOnViewAvailable(new onViewAvailable() {

            @Override
            public void viewAvailable(DrawPadCameraView v) {
                // TODO Auto-generated method stub
                startDrawPad();
            }
        });
        mDrawPadCamera.setOnDrawPadErrorListener(new onDrawPadErrorListener() {

            @Override
            public void onError(DrawPad d, int what) {
                // TODO Auto-generated method stub
                Log.e(TAG, "DrawPad容器线程运行出错!!!" + what);
            }
        });
    }

    /**
     * Step2: 开始运行 Drawpad线程.
     */
    private void startDrawPad() {
        // 如果是屏幕比例大于16:9,则需要重新设置编码参数, 从而画面不变形
        if (LanSongUtil.isFullScreenRatio(mDrawPadCamera.getViewWidth(),
                mDrawPadCamera.getViewHeight())) {
            mDrawPadCamera.setRealEncodeEnable(544, 1088, 3500 * 1024,
                    (int) 25, dstPath);
        }
        if (mDrawPadCamera.setupDrawpad()) {
            mCamLayer = mDrawPadCamera.getCameraLayer();
            mDrawPadCamera.startPreview(); // 容器开始预览

            // 增加一个子图层;
            SubLayer layer1 = mCamLayer.addSubLayer();
            SubLayer layer2 = mCamLayer.addSubLayer();
            SubLayer layer3 = mCamLayer.addSubLayer();
            SubLayer layer4 = mCamLayer.addSubLayer();

            layer1.setScale(0.5f);
            layer2.setScale(0.5f);
            layer3.setScale(0.5f);
            layer4.setScale(0.5f);

            // 左上角为0,0;, 设置每个子图层中心点的位置
            int x1 = layer1.getPadWidth() / 4;
            int y1 = layer1.getPadHeight() / 4;

            int x2 = layer2.getPadWidth() / 4;
            int y2 = layer2.getPadHeight() * 3 / 4;

            int x3 = layer3.getPadWidth() * 3 / 4;
            int y3 = layer3.getPadHeight() / 4;

            int x4 = layer4.getPadWidth() * 3 / 4;
            int y4 = layer4.getPadHeight() * 3 / 4;

            layer1.setPosition(x1, y1);
            layer2.setPosition(x2, y2);
            layer3.setPosition(x3, y3);
            layer4.setPosition(x4, y4);

            // 第一个增加一个边框
            // layer1.setVisibleRect(0.02f,0.98f,0.02f,0.98f); //这里0.02和0.98,
            // 是为了上下左右边框留出0.02的边框;
            // layer1.setVisibleRectBorder(0.02f, 1.0f, 0.0f, 0.0f, 1.0f);
            // //设置边框;

            // 增加不同的滤镜来显示效果
            layer1.switchFilterTo(new IF1977Filter(mContext));
            layer2.switchFilterTo(new IFAmaroFilter(mContext));
            layer3.switchFilterTo(new IFEarlybirdFilter(mContext));
            layer4.switchFilterTo(new IFNashvilleFilter(mContext));
            // 增加滤镜, 应该从当前图层就可以增加滤镜!!!!
        } else {
            Log.i(TAG, "建立drawpad线程失败.");
        }
    }

    /**
     * Step3: 停止容器, 停止后,为新的视频文件增加上音频部分.
     */
    private void stopDrawPad() {
        if (mDrawPadCamera != null && mDrawPadCamera.isRunning()) {
            mDrawPadCamera.stopDrawPad();
            mCamLayer = null;
        }
    }

    /**
     * 选择滤镜效果,
     */
    private void selectFilter() {
        Toast.makeText(mContext, "当前演示子图层,主图层滤镜暂时屏蔽", Toast.LENGTH_SHORT)
                .show();
        // if(mDrawPadCamera!=null && mDrawPadCamera.isRunning()){
        // GPUImageFilterTools.showDialog(this, new
        // OnGpuImageFilterChosenListener() {
        //
        // @Override
        // public void onGpuImageFilterChosenListener(final GPUImageFilter
        // filter) {
        // /**
        // * 通过DrawPad线程去切换 filterLayer的滤镜
        // * 有些Filter是可以调节的,这里为了代码简洁,暂时没有演示, 可以在CameraeLayerDemoActivity中查看.
        // */
        // if(mCamLayer!=null)
        // {
        // mCamLayer.switchFilterTo(filter);
        // }
        // }
        // });
        // }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        super.onDestroy();
        stopDrawPad();

        if (SDKFileUtils.fileExist(dstPath)) {
            SDKFileUtils.deleteFile(dstPath);
            dstPath = null;
        }
    }

    // -------------------------------------------一下是UI界面和控制部分.---------------------------------------------------
    private void initView() {
        findViewById(R.id.id_fullrecord_cancel).setOnClickListener(this);

        tvTime = (TextView) findViewById(R.id.id_fullscreen_timetv);

        tvSubLayerHint = (TextView) findViewById(R.id.id_camera_sublayer_hint);
        tvSubLayerHint.setText("演示增加4个子图层,并分别增加滤镜");

        btnOk = (ImageView) findViewById(R.id.id_fullrecord_ok);
        btnOk.setOnClickListener(this);

        focusView = (FocusImageView) findViewById(R.id.id_fullrecord_focusview);

        findViewById(R.id.id_fullrecord_flashlight).setOnClickListener(this);
        findViewById(R.id.id_fullrecord_frontcamera).setOnClickListener(this);
        findViewById(R.id.id_fullrecord_filter).setOnClickListener(this);
        mProgressBar = (CameraProgressBar) findViewById(R.id.id_fullrecord_progress);
    }

    private void playVideo() {
        if (SDKFileUtils.fileExist(dstPath)) {
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra("videopath", dstPath);
            startActivity(intent);
        } else {
            Toast.makeText(this, "目标文件不存在", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.id_fullrecord_cancel:

                this.finish();
                break;
            case R.id.id_fullrecord_ok:
                stopDrawPad();
                playVideo();
                break;
            case R.id.id_fullrecord_frontcamera:
                if (mCamLayer != null) {
                    if (mDrawPadCamera.isRunning()
                            && CameraLayer.isSupportFrontCamera()) {
                        // 先把DrawPad暂停运行.
                        mDrawPadCamera.pausePreview();
                        mCamLayer.changeCamera();
                        mDrawPadCamera.resumePreview(); // 再次开启.
                    }
                }
                break;
            case R.id.id_fullrecord_flashlight:
                if (mCamLayer != null) {
                    mCamLayer.changeFlash();
                }
                break;
            case R.id.id_fullrecord_filter:
                selectFilter();
                break;
            default:
                break;
        }
    }
}