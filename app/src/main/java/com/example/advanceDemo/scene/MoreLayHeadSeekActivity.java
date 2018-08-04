package com.example.advanceDemo.scene;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.Layer;
import com.lansosdk.box.ScaleAnimation;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadRunTimeListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKFileUtils;

import java.io.IOException;

/**
 * 多个图层设置好各自的时间段, 不用生成视频,既可以直接预览, 在预览中, 可以手动拖动进度条. 手动seek的演示.
 */
public class MoreLayHeadSeekActivity extends Activity {
    private static final String TAG = "MoreLayHeadSeekActivity";

    private final static long BITMAP_DURATION = 3 * 1000 * 1000;
    int videoWidth, videoHeight;
    private String mVideoPath;
    private String videoPath2;
    private DrawPadView drawPadView;
    private MediaPlayer mplayer = null;
    private MediaPlayer mplayer2 = null;
    private BitmapLayer bmpLayer = null;
    private VideoLayer videoLayer1 = null;
    private VideoLayer videoLayer2 = null;
    private MediaInfo firstInfo = null;
    private MediaInfo secondInfo;
    private LinearLayout btnPlay;
    private String dstPath = null;
    private ScaleAnimation scaleAnim;
    private Context mContext;
    private long bmpLayerStartTime = 0;
    private SeekBar skbTime;
    private long totalTimeUs;
    private Thread copyThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videolay_transform_layout);

        mContext = getApplicationContext();

        copySecondVideoAsync();
        initView();

        mVideoPath = getIntent().getStringExtra("videopath");
        drawPadView = (DrawPadView) findViewById(R.id.id_videolayer_drawpad);

        dstPath = SDKFileUtils.newMp4PathInBox();

        firstInfo = new MediaInfo(mVideoPath, false);
        if (firstInfo.prepare() == false) {
            finish();
        }
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                setupDrawPad();
            }
        }, 500);
    }

    /**
     * Step1: 设置DrawPad 容器的尺寸.并设置是否实时录制容器上的内容.
     */
    private void setupDrawPad() {
        videoWidth = firstInfo.vWidth;
        videoHeight = firstInfo.vHeight;
        if (firstInfo.vRotateAngle == 90 || firstInfo.vRotateAngle == 270) {
            videoWidth = firstInfo.vHeight;
            videoHeight = firstInfo.vWidth;
        }
        joinCopySecondVideo();

        secondInfo = new MediaInfo(videoPath2, false);
        secondInfo.prepare();

        bmpLayerStartTime = (long) (firstInfo.vDuration * 1000 * 1000);
        // 设置最大值.
        totalTimeUs = (long) (firstInfo.vDuration * 1000 * 1000)
                + BITMAP_DURATION + (long) (secondInfo.vDuration * 1000 * 1000);
        skbTime.setMax((int) totalTimeUs);

        drawPadView.setRealEncodeEnable(videoWidth, videoHeight, 1500000,
                (int) 25, dstPath);
        drawPadView.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH, 25);// 25是帧率.
        drawPadView.setDrawPadSize(videoWidth, videoHeight,
                new onDrawPadSizeChangedListener() {
                    @Override
                    public void onSizeChanged(int viewWidth, int viewHeight) {
                        drawPadView.pauseDrawPad();
                        startDrawPad();
                    }
                });
        drawPadView.setOnDrawPadRunTimeListener(new onDrawPadRunTimeListener() {

            @Override
            public void onRunTime(DrawPad v, long curerntTimeUs) {

                playVideo1(curerntTimeUs);
                playVideo2(curerntTimeUs);
                playPicture(curerntTimeUs);
            }
        });
        drawPadView
                .setOnDrawPadProgressListener(new onDrawPadProgressListener() {

                    @Override
                    public void onProgress(DrawPad v, long currentTimeUs) {

                    }
                });
    }

    private void startDrawPad() {
        drawPadView.pauseDrawPad();
        if (drawPadView.startDrawPad()) {
            prepareVideo();
            drawPadView.resumeDrawPad();
        }
    }

    private void prepareVideo() {
        mplayer = new MediaPlayer();
        try {
            mplayer.setDataSource(mVideoPath);
            mplayer.setOnPreparedListener(new OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {

                    // 增加一个图层,但先不显示.
                    videoLayer1 = drawPadView.addVideoLayer(
                            mplayer.getVideoWidth(), mplayer.getVideoHeight(),
                            null);
                    videoLayer1.setVisibility(Layer.INVISIBLE);
                    // 设置到MediaPlayer中.
                    mplayer.setSurface(new Surface(videoLayer1
                            .getVideoTexture())); // 视频
                    mplayer.start();
                    mplayer.pause();
                }
            });
            mplayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                }
            });
            mplayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // ----bitmap
        String bmpPath = CopyFileFromAssets.copyAssets(getApplicationContext(),
                "girl.jpg");
        Bitmap bmp = BitmapFactory.decodeFile(bmpPath);
        bmpLayer = drawPadView.addBitmapLayer(bmp, null);
        bmpLayer.setVisibility(Layer.INVISIBLE);

        scaleAnim = new ScaleAnimation(bmpLayerStartTime, 2 * 1000 * 1000,
                1.0f, 2.0f);

        // -----------------video2
        mplayer2 = new MediaPlayer();
        try {
            mplayer2.setDataSource(videoPath2);
            mplayer2.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    // 增加一个图层, 先不显示.
                    videoLayer2 = drawPadView.addVideoLayer(
                            mplayer2.getVideoWidth(),
                            mplayer2.getVideoHeight(), null);
                    videoLayer2.setVisibility(Layer.INVISIBLE);
                    mplayer2.setSurface(new Surface(videoLayer2
                            .getVideoTexture())); // 视频
                    mplayer2.start();
                    mplayer2.pause();
                }
            });
            mplayer2.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                }
            });
            mplayer2.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playVideo1(long currentTimeUs) {
        // 在视频1的范围内.
        if (currentTimeUs >= 0 && currentTimeUs < (bmpLayerStartTime)) {
            if (mplayer != null && mplayer.isPlaying() == false) {
                videoLayer1.setVisibility(Layer.VISIBLE);
                if (videoLayer2 != null)
                    videoLayer2.setVisibility(Layer.INVISIBLE);

                if (bmpLayer != null)
                    bmpLayer.setVisibility(Layer.INVISIBLE);

                mplayer.start();
            }
        }
    }

    private void playPicture(long currentTimeUs) {
        // 在图片范围内.
        if (currentTimeUs > bmpLayerStartTime
                && currentTimeUs < (bmpLayerStartTime + BITMAP_DURATION)) {
            videoLayer1.setVisibility(Layer.INVISIBLE);
            videoLayer2.setVisibility(Layer.INVISIBLE);
            bmpLayer.setVisibility(Layer.VISIBLE);
            if (scaleAnim != null) {
                scaleAnim.run(bmpLayer, currentTimeUs);
            }
        }
    }

    private void playVideo2(long currentTimeUs) {
        long start = bmpLayerStartTime + BITMAP_DURATION;
        long end = start + (long) (secondInfo.vDuration * 1000 * 1000);
        // 在视频2的范围内.
        if (currentTimeUs > start && currentTimeUs < end) {
            if (mplayer2 != null && mplayer2.isPlaying() == false) {
                videoLayer1.setVisibility(Layer.INVISIBLE);
                bmpLayer.setVisibility(Layer.INVISIBLE);
                videoLayer2.setVisibility(Layer.VISIBLE);
                mplayer2.start();
            }
        }
    }

    /**
     * 这里因为是自动刷新.
     */
    private void stopDrawPad() {
        if (drawPadView != null && drawPadView.isRunning()) {
            drawPadView.stopDrawPad();
            toastStop();

            if (SDKFileUtils.fileExist(dstPath)) {
                btnPlay.setVisibility(View.VISIBLE);
            }
            if (mplayer != null) {
                mplayer.stop();
                mplayer.release();
                mplayer = null;
            }
            if (mplayer2 != null) {
                mplayer2.stop();
                mplayer2.release();
                mplayer2 = null;
            }
        }
    }

    private void initView() {
        btnPlay = (LinearLayout) findViewById(R.id.id_videoLayer_saveplay);
        btnPlay.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MoreLayHeadSeekActivity.this,
                        VideoPlayerActivity.class);
                intent.putExtra("videopath", dstPath);
                startActivity(intent);
            }
        });
        btnPlay.setVisibility(View.GONE);

        skbTime = (SeekBar) findViewById(R.id.id_transform2_seekbar);
        skbTime.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if (fromUser) {
                    Log.i(TAG, "seekbar  progress " + progress);
                    seekTo(progress);
                }
            }
        });
    }

    private void seekTo(int currentTimeUs) {
        mplayer.pause();
        mplayer2.pause();
        videoLayer1.setVisibility(Layer.INVISIBLE);
        bmpLayer.setVisibility(Layer.INVISIBLE);
        videoLayer2.setVisibility(Layer.INVISIBLE);

        drawPadView.resetDrawPadRunTime(currentTimeUs);

        if (currentTimeUs >= 0 && currentTimeUs < (bmpLayerStartTime)) {
            videoLayer1.setVisibility(Layer.VISIBLE);
            mplayer.seekTo(currentTimeUs / 1000);
            mplayer.start();
        } else if (currentTimeUs > bmpLayerStartTime
                && currentTimeUs < (bmpLayerStartTime + BITMAP_DURATION)) {
            bmpLayer.setVisibility(Layer.VISIBLE);
            if (scaleAnim != null) {
                scaleAnim.run(bmpLayer, currentTimeUs);
            }
        } else {
            videoLayer2.setVisibility(Layer.VISIBLE);
            long start = bmpLayerStartTime + BITMAP_DURATION;
            long end = start + (long) (secondInfo.vDuration * 1000 * 1000);
            if (currentTimeUs > start && currentTimeUs < end)// 在视频2的范围内.
            {
                mplayer2.seekTo(currentTimeUs / 1000);
                mplayer2.start();
            }
        }
    }

    private void toastStop() {
        Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT)
                .show();
    }

    private void copySecondVideoAsync() {
        copyThread = new Thread(new Runnable() {

            @Override
            public void run() {
                videoPath2 = CopyFileFromAssets.copyAssets(
                        getApplicationContext(), "ping25s.mp4");
                copyThread = null;
            }
        });
        copyThread.start();
    }

    private void joinCopySecondVideo() {
        if (copyThread != null) {
            try {
                copyThread.join();
                copyThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mplayer != null) {
            mplayer.stop();
            mplayer.release();
            mplayer = null;
        }
        if (mplayer2 != null) {
            mplayer2.stop();
            mplayer2.release();
            mplayer2 = null;
        }
        if (drawPadView != null) {
            drawPadView.stopDrawPad();
            drawPadView = null;
        }
        SDKFileUtils.deleteFile(dstPath);
    }
}
