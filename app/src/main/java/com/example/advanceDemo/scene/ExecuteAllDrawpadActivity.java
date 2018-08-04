package com.example.advanceDemo.scene;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.Animation;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.CanvasRunnable;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadAllExecute;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MoveAnimation;
import com.lansosdk.box.ScaleAnimation;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoEditor;

import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageSwirlFilter;

/**
 * 后台执行.
 */
public class ExecuteAllDrawpadActivity extends Activity {

    private static final String TAG = "ExecuteAllDrawpadActivity";
    private String videoPath = null;
    private TextView tvProgressHint;
    private TextView tvHint;
    private String dstPath = null;
    private String videoPath2 = null;

    private DrawPadAllExecute mDrawPad = null;
    private VideoLayer videoLayer1 = null;
    private VideoLayer videoLayer2 = null;
    private BitmapLayer bmpLayer;
    private CanvasLayer canvasLayer;

    private boolean isExecuting = false;
    private Context mContext = null;
    private MediaInfo mInfo = null;
    private String video3S; // 3秒的视频.
    private int rectFactor = 0;
    private GPUImageSwirlFilter swirlFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.execute_edit_demo_layout);

        mContext = getApplicationContext();
        videoPath = getIntent().getStringExtra("videopath");

        mInfo = new MediaInfo(videoPath, false);

        initView();

        // 在手机的默认路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        dstPath = SDKFileUtils.newMp4PathInBox();
        new Thread(new Runnable() {
            @Override
            public void run() {
                videoPath2 = CopyFileFromAssets.copyAssets(
                        getApplicationContext(), "ping25s.mp4");
            }
        }).start();

        if (mInfo.prepare() && mInfo.vDuration > 3.0f) // 为了很快演示, 这里只要前3秒.
        {
            VideoEditor editor = new VideoEditor();
            video3S = SDKFileUtils.createFileInBox(mInfo.fileSuffix);
            editor.executeVideoCutOut(videoPath, video3S, 0, 3.0f);
        } else {
            video3S = videoPath;
        }
    }

    /**
     * 从这里开始演示.
     */
    private void startDrawPad() {
        if (isExecuting)
            return;

        isExecuting = true;
        /**
         *
         * @param ctx
         * @param padwidth
         *            容器宽度/生成视频的宽度
         * @param padheight
         *            容器高度/生成视频的高度
         * @param framerate
         *            生成视频的帧率
         * @param bitrate
         *            生成视频的码率
         * @param dstPath
         *            生成视频保存的完整路径 .mp4结尾.
         */
        mDrawPad = new DrawPadAllExecute(mContext, 480, 480, 25, 1000 * 1000,
                dstPath);
        /**
         * 设置错误监听
         */
        mDrawPad.setDrawPadErrorListener(new onDrawPadErrorListener() {

            @Override
            public void onError(DrawPad d, int what) {
                mDrawPad.stopDrawPad();
                Log.e(TAG, "后台容器线程 运行失败,您请检查下是否码率分辨率设置过大,或者联系我们!...");
            }
        });
        /**
         * 设置DrawPad处理的进度监听, 回传的currentTimeUs单位是微秒.
         */
        mDrawPad.setDrawPadProgressListener(new onDrawPadProgressListener() {

            @Override
            public void onProgress(DrawPad v, long currentTimeUs) {
                tvProgressHint.setText(String.valueOf(currentTimeUs));

                if (currentTimeUs > 18 * 1000 * 1000) { // 18秒的时候停止.
                    mDrawPad.stopDrawPad();
                } else if (currentTimeUs > 15 * 1000 * 1000) { // 显示第4个图层.
                    showFourLayer();
                } else if (currentTimeUs > 8 * 1000 * 1000
                        && videoLayer2 == null) { // 8秒的时候增加一个视频图层
                    showThreeLayer(currentTimeUs);
                } else if (currentTimeUs > 3 * 1000 * 1000 && bmpLayer == null) { // 3秒的时候,
                    // 增加图片图层
                    showSecondLayer(currentTimeUs);
                }
            }
        });
        /**
         * 设置DrawPad完成后的监听.
         */
        mDrawPad.setDrawPadCompletedListener(new onDrawPadCompletedListener() {

            @Override
            public void onCompleted(DrawPad v) {
                tvProgressHint.setText("DrawPadExecute Completed!!!");
                isExecuting = false;
                findViewById(R.id.id_video_edit_btn2).setEnabled(true);
            }
        });
        /**
         * 开始执行这个DrawPad
         */
        if (mDrawPad.startDrawPad()) {
            // 增加背景图片
            mDrawPad.addBitmapLayer(BitmapFactory.decodeResource(
                    getResources(), R.drawable.pad_bg), null);
            // 增加第一个视频
            videoLayer1 = mDrawPad.addVideoLayer(videoPath, null);
        } else {
            Log.e(TAG,
                    "后台容器线程  运行失败,您请检查下是否是路径设置有无, 请用MediaInfo.checkFile执行查看下....");
        }
    }

    /**
     * 停止第一个图层, 并开启第二个图层.
     */
    private void showSecondLayer(long currentTimeUs) {
        if (videoLayer1 != null) {
            if (rectFactor > 100) // 等到100时,结束动画, 删除视频图层,并增加图片图层.
            { // 停止.
                mDrawPad.removeLayer(videoLayer1);
                videoLayer1 = null;
                rectFactor = 0;
                addBitmapLayer(currentTimeUs);
            } else { // 有个动画效果
                float rect = (100 - rectFactor); // 因为java的小数点不是很精确, 这里用整数表示
                rectFactor = rectFactor + 5;
                rect /= 2;
                rect /= 100;// 再次转换为0--1.0的范围
                videoLayer1
                        .setVisibleRect(0.5f - rect, 0.5f + rect, 0.0f, 1.0f);
            }
        }
    }

    /**
     * 停止第二个图层,开启第三个图层
     *
     * @param currentTimeUs
     */
    private void showThreeLayer(long currentTimeUs) {
        if (bmpLayer != null) {
            if (rectFactor > 100) {
                mDrawPad.removeLayer(bmpLayer);
                bmpLayer = null;
                addOtherVideoLayer(currentTimeUs);
                rectFactor = 0;
            } else { // 淡淡的消失.
                float rect = (100 - rectFactor); // 因为java的小数点不是很精确, 这里用整数表示
                rect /= 100f; // 转换为0--1.0

                bmpLayer.setAlphaPercent(rect);
                bmpLayer.setRedPercent(rect);
                bmpLayer.setGreenPercent(rect);
                bmpLayer.setBluePercent(rect);
                rectFactor = rectFactor + 5;
            }
        }
    }

    private void showFourLayer() {
        if (videoLayer2 != null) {
            if (rectFactor > 120) {
                mDrawPad.removeLayer(videoLayer2);
                videoLayer2 = null;
                rectFactor = 0;
                addCanvasLayer();
            } else { // 增加滤镜动画
                float rect = (float) rectFactor; // 因为java的小数点不是很精确, 这里用整数表示
                rect /= 100f; // 转换为0--1.0

                if (swirlFilter == null) {
                    swirlFilter = new GPUImageSwirlFilter();
                    videoLayer2.switchFilterTo(swirlFilter);
                }
                swirlFilter.setAngle(rect);
                swirlFilter.setRadius(1.0f); // 设置半径是整个纹理.
                rectFactor = rectFactor + 5;
            }
        }
    }

    /**
     * 增加图片
     */
    private void addBitmapLayer(long currentTimeUs) {
        String bmpPath = CopyFileFromAssets.copyAssets(getApplicationContext(),
                "girl.jpg");
        Bitmap bmp = BitmapFactory.decodeFile(bmpPath);
        bmpLayer = mDrawPad.addBitmapLayer(bmp, null);
        bmpLayer.setVisibility(Layer.INVISIBLE);
        ScaleAnimation scaleAnim = new ScaleAnimation(
                currentTimeUs + 1000 * 1000, 2 * 1000 * 1000, 0.0f, 1.0f);
        bmpLayer.addAnimation(scaleAnim);
    }

    /**
     * 增加视频.
     *
     * @param currentTimeUs
     */
    private void addOtherVideoLayer(long currentTimeUs) {
        if (videoPath2 == null) {
            videoPath2 = CopyFileFromAssets.copyAssets(getApplicationContext(),
                    "ping25s.mp4");
        }
        videoLayer2 = mDrawPad.addVideoLayer(videoPath2, null);

        videoLayer2.setVisibility(Layer.INVISIBLE);
        Animation move = new MoveAnimation(currentTimeUs + 1000 * 1000,
                1 * 1000 * 1000, 0, 0, videoLayer2.getPadWidth() / 2,
                videoLayer2.getPadHeight() / 2);
        Animation scale = new ScaleAnimation(currentTimeUs + 1000 * 1000,
                1 * 1000 * 1000, 0.0f, 1.0f);
        videoLayer2.addAnimation(move);
        videoLayer2.addAnimation(scale);
    }

    /**
     * 增加canvas图层.
     */
    private void addCanvasLayer() {
        canvasLayer = mDrawPad.addCanvasLayer();
        if (canvasLayer != null) {
            canvasLayer.addCanvasRunnable(new CanvasRunnable() {
                @Override
                public void onDrawCanvas(CanvasLayer layer, Canvas canvas,
                                         long currentTimeUs) {
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setAntiAlias(true);
                    paint.setTextSize(30);
                    canvas.drawColor(Color.YELLOW); // 背景设置为黄色.
                    canvas.drawText("蓝松短视频演示之【转场】", 20,
                            canvasLayer.getPadHeight() / 2, paint);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        if (mDrawPad != null) {
            mDrawPad.releaseDrawPad();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mDrawPad = null;
        }

        SDKFileUtils.deleteFile(dstPath);
        SDKFileUtils.deleteFile(video3S);
    }

    private void initView() {
        tvHint = (TextView) findViewById(R.id.id_video_editor_hint);

        tvHint.setText(R.string.videolayer_transform_hints);
        tvProgressHint = (TextView) findViewById(R.id.id_video_edit_progress_hint);

        findViewById(R.id.id_video_edit_btn).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startDrawPad();
                    }
                });

        findViewById(R.id.id_video_edit_btn2).setEnabled(false);

        findViewById(R.id.id_video_edit_btn2).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (SDKFileUtils.fileExist(dstPath)) {
                            Intent intent = new Intent(mContext,
                                    VideoPlayerActivity.class);
                            String audioPath = CopyFileFromAssets.copyAssets(
                                    mContext, "bgMusic20s.m4a");
                            String ret = VideoEditor.mp4AddAudio(dstPath,
                                    audioPath);
                            intent.putExtra("videopath", ret);
                            startActivity(intent);
                        } else {
                            Toast.makeText(ExecuteAllDrawpadActivity.this,
                                    "目标文件不存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}