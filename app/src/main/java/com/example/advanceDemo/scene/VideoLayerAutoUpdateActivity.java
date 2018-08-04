package com.example.advanceDemo.scene;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.example.advanceDemo.view.PaintConstants;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.ViewLayer;
import com.lansosdk.box.ViewLayerRelativeLayout;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKFileUtils;

import java.io.IOException;

/**
 * 采用自动刷新模式 增加视频VideoLayer 到DrawPad中.
 */
public class VideoLayerAutoUpdateActivity extends Activity {
    private static final String TAG = "VideoLayerAutoUpdateDemoActivity";

    private String mVideoPath;

    private DrawPadView mDrawPad;

    private MediaPlayer mplayer = null;

    private VideoLayer mLayerMain = null;
    private ViewLayer mViewLayer = null;

    private LinearLayout playVideo;
    private String dstPath = null;

    private ViewLayerRelativeLayout mLayerRelativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videolayer_autoupdate_demo_layout);

        initView();

        mVideoPath = getIntent().getStringExtra("videopath");
        mDrawPad = (DrawPadView) findViewById(R.id.id_vauto_demo_drawpad_view);

        // 在手机的默认路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        dstPath = SDKFileUtils.newMp4PathInBox();

        // 演示例子用到的.
        PaintConstants.SELECTOR.COLORING = true;
        PaintConstants.SELECTOR.KEEP_IMAGE = true;

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                startPlayVideo();
            }
        }, 500);
    }

    private void startPlayVideo() {
        if (mVideoPath != null) {
            mplayer = new MediaPlayer();
            try {
                mplayer.setDataSource(mVideoPath);

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mplayer.setOnPreparedListener(new OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    initDrawPad();
                }
            });
            mplayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopDrawPad();
                }
            });
            mplayer.prepareAsync();
        } else {
            Log.e(TAG, "Null Data Source\n");
            finish();
            return;
        }
    }

    // Step1: 设置DrawPad 容器的尺寸.并设置是否实时录制容器上的内容.
    private void initDrawPad() {
        MediaInfo info = new MediaInfo(mVideoPath, false);
        if (info.prepare()) {

            mDrawPad.setRealEncodeEnable(480, 480, 1000000,
                    (int) info.vFrameRate, dstPath);

            mDrawPad.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH, 25);// 25是帧率.

            mDrawPad.setDrawPadSize(480, 480,
                    new onDrawPadSizeChangedListener() {

                        @Override
                        public void onSizeChanged(int viewWidth, int viewHeight) {
                            startDrawPad();
                        }
                    });
        }
    }

    /**
     * Step2: Drawpad设置好后, 开始容器线程运行,并增加一个ViewLayer图层
     */
    private void startDrawPad() {
        mDrawPad.startDrawPad();

        mLayerMain = mDrawPad.addMainVideoLayer(mplayer.getVideoWidth(),
                mplayer.getVideoHeight(), null);
        if (mLayerMain != null) {
            mplayer.setSurface(new Surface(mLayerMain.getVideoTexture()));
        }
        mplayer.start();

        addViewLayer();
    }

    /**
     * Step3: 做好后, 停止容器, 因为容器里没有声音, 这里增加上原来的声音.
     */
    private void stopDrawPad() {
        if (mDrawPad != null && mDrawPad.isRunning()) {
            mDrawPad.stopDrawPad();
            toastStop();

            if (SDKFileUtils.fileExist(dstPath)) {
                playVideo.setVisibility(View.VISIBLE);
            }
        }
    }

    private void addViewLayer() {
        if (mDrawPad != null && mDrawPad.isRunning()) {
            mViewLayer = mDrawPad.addViewLayer();
            mLayerRelativeLayout.bindViewLayer(mViewLayer);
            mLayerRelativeLayout.invalidate();

            ViewGroup.LayoutParams params = mLayerRelativeLayout
                    .getLayoutParams();
            params.height = mViewLayer.getPadHeight(); // 因为布局时, 宽度一致,
            // 这里调整高度,让他们一致.

            mLayerRelativeLayout.setLayoutParams(params);
        }
    }

    private void initView() {
        mLayerRelativeLayout = (ViewLayerRelativeLayout) findViewById(R.id.id_vauto_demo_viewpenayout);
        playVideo = (LinearLayout) findViewById(R.id.id_vauto_demo_saveplay);
        playVideo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (SDKFileUtils.fileExist(dstPath)) {
                    Intent intent = new Intent(
                            VideoLayerAutoUpdateActivity.this,
                            VideoPlayerActivity.class);
                    intent.putExtra("videopath", dstPath);
                    startActivity(intent);
                } else {
                    Toast.makeText(VideoLayerAutoUpdateActivity.this,
                            "目标文件不存在", Toast.LENGTH_SHORT).show();
                }
            }
        });

        playVideo.setVisibility(View.GONE);

        findViewById(R.id.id_vauto_demo_pausevideo).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (mplayer != null && mplayer.isPlaying()) {
                            mplayer.pause();
                        }
                    }
                });
        findViewById(R.id.id_vauto_demo_startvideo).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (mplayer != null && mplayer.isPlaying() == false) {
                            mplayer.start();
                        }
                    }
                });
    }

    private void toastStop() {
        Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (mplayer != null) {
            mplayer.stop();
            mplayer.release();
            mplayer = null;
        }

        if (mDrawPad != null) {
            mDrawPad.stopDrawPad();
            mDrawPad = null;
        }
        if (SDKFileUtils.fileExist(dstPath)) {
            SDKFileUtils.deleteFile(dstPath);
        }
    }
}
