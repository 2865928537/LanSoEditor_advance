package com.example.advanceDemo;

import android.app.Activity;
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
import android.widget.Toast;

import com.example.advanceDemo.utils.DemoUtil;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.Layer;
import com.lansosdk.box.SubLayer;
import com.lansosdk.box.VideoLayer2;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.box.onDrawPadThreadProgressListener;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.LanSongMergeAV;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKFileUtils;

import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageColorInvertFilter;
import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageLaplacianFilter;
import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageToonFilter;
import jp.co.cyberagent.lansongsdk.gpuimage.LanSongBlurFilter;
import jp.co.cyberagent.lansongsdk.gpuimage.LanSongColorEdgeFilter;

public class DouYinDemoActivity extends Activity implements OnClickListener {
    private static final String TAG = "DouYinDemoActivity";
    private static final int ONESCALE_FRAMES = 6;
    private static final int SCALE_STATUS_NONE = 0;
    private static final int SCALE_STATUS_ADD = 1;
    private static final int SCALE_STATUS_DEL = 2;  //减去;
    private static final int OUTBODY_FRAMES = 15;
    int testCnt = 0;
    boolean isPaused = false;
    private String videoPath;
    private DrawPadView drawPadView;
    private MediaPlayer mplayer = null;
    private VideoLayer2 videoLayer = null;
    private String editTmpPath = null;
    private String dstPath = null;
    private LinearLayout playVideo;
    private MediaInfo mInfo = null;
    private int colorEdgeCnt = 0;
    private int colorScaleStatus = SCALE_STATUS_NONE;
    private boolean colorScaleEnable = false;
    private LanSongColorEdgeFilter colorEdgeFilter;
    //--------------------------------------------------
    private int outBodyCnt = 0;
    //
    private float outBodySacle = 1.0f;  //缩放是从1.0到最大.
    private SubLayer outBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_effect_layout);

        videoPath = getIntent().getStringExtra("videopath");

        mInfo = new MediaInfo(videoPath, false);
        if (mInfo.prepare() == false) {
            Toast.makeText(this, "传递过来的视频文件错误", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        drawPadView = (DrawPadView) findViewById(R.id.id_videoeffect_drawpadview);
        initView();

        // 在手机的默认路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        editTmpPath = SDKFileUtils.newMp4PathInBox();
        dstPath = SDKFileUtils.newMp4PathInBox();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startPlayVideo();
            }
        }, 300);
    }

    private void startPlayVideo() {
        if (videoPath != null) {
            mplayer = new MediaPlayer();
            try {
                mplayer.setDataSource(videoPath);
                mplayer.setOnPreparedListener(new OnPreparedListener() {

                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        initDrawPad(mp.getVideoWidth(), mp.getVideoHeight());
                    }
                });
                mplayer.setOnCompletionListener(new OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        stopDrawPad();
                    }
                });
                mplayer.setLooping(true);
                mplayer.prepareAsync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            finish();
            return;
        }
    }

    /**
     * 第一步: init DrawPad 初始化
     */
    private void initDrawPad(int w, int h) {
        int padWidth = w;
        int padHeight = h;
        /**
         * 设置当前DrawPad的宽度和高度,并把宽度自动缩放到父view的宽度,然后等比例调整高度.
         */
        drawPadView.setDrawPadSize(padWidth, padHeight, new onDrawPadSizeChangedListener() {
            @Override
            public void onSizeChanged(int viewWidth, int viewHeight) {
                startDrawPad();
            }
        });
        drawPadView.setOnDrawPadThreadProgressListener(new onDrawPadThreadProgressListener() {

            @Override
            public void onThreadProgress(DrawPad v, long currentTimeUs) {
                videoColorEndge();
                videoOutBody();
            }
        });
        drawPadView.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH, 25);
    }

    /**
     * Step2: 开始运行 Drawpad
     */
    private void startDrawPad() {
        drawPadView.pauseDrawPad();

        if (drawPadView.isRunning() == false && drawPadView.startDrawPad()) {
            videoLayer = drawPadView.addVideoLayer2(mplayer.getVideoWidth(), mplayer.getVideoHeight(), null);
            if (videoLayer != null) {
                mplayer.setSurface(new Surface(videoLayer.getVideoTexture()));
                mplayer.start();
            }
            drawPadView.resumeDrawPad();
        }
    }

    /**
     * Step3: stop DrawPad
     */
    private void stopDrawPad() {
        if (drawPadView != null && drawPadView.isRunning()) {

            drawPadView.stopDrawPad();
            DemoUtil.showToast(getApplicationContext(), "录制已停止!!");

            if (SDKFileUtils.fileExist(editTmpPath)) {
                dstPath=LanSongMergeAV.mergeAVDirectly(videoPath, editTmpPath,true);
                playVideo.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, " player completion, but file not exist" + editTmpPath);
            }
        }
    }

    @Override
    public void onClick(View v) {
        videoNOEffect();

        switch (v.getId()) {
            case R.id.id_videoeffect_noeffect:
                videoNOEffect();
                break;
            case R.id.id_videoeffect_outbody:
                if (outBody == null) {
                    outBody = videoLayer.addSubLayer();
                }
                break;
            case R.id.id_videoeffect_coloredge:
                videoColorStart();
                break;
            case R.id.id_videoeffect_cuowei:
                videoCuoWei();
                break;
            case R.id.id_videoeffect_image_mirror:
                videoEffectMirror();
                break;
            case R.id.id_videoeffect_16sublay:
                video16Image();
                break;
            case R.id.id_videoeffect_bg_blur:
                videoBackGroundBlur();
                break;
            case R.id.id_videoeffect_invert:
                videoColorInvert();
                break;
            case R.id.id_videoeffect_toon:
                videoColorToon();
                break;
            case R.id.id_videoeffect_fudiao:
                videoColorLaplacian();
                break;
            default:
                break;
        }
    }

    /**
     * 视频镜像
     */
    private void videoEffectMirror() {
        if (videoLayer != null) {
            videoLayer.setScale(0.5f);
            videoLayer.setPosition(videoLayer.getScaleX() / 2, videoLayer.getPositionY());


            SubLayer layer = videoLayer.addSubLayer();  //子图层默认缩小一倍,以方便参考.
            layer.setScale(0.5f);
            layer.setPosition(videoLayer.getPositionX() + videoLayer.getScaleX(), videoLayer.getPositionY());
            layer.setLayerMirror(true, false);
        }
    }

    private void videoBackGroundBlur() {
        if (videoLayer != null) {

            videoLayer.setScaledValue(videoLayer.getPadWidth(), videoLayer.getPadHeight());
            videoLayer.switchFilterTo(new LanSongBlurFilter());

            //两个画面, 缩放第二个;
            SubLayer layer = videoLayer.addSubLayer();
            layer.setScale(0.75f);  //作为演示;
        }
    }

    private void video16Image() {
        if (videoLayer != null) {

            //放左上;
            videoLayer.setScale(0.25f);
            videoLayer.setPosition(videoLayer.getScaleX() / 2, videoLayer.getScaleY() / 2);

            SubLayer layer1 = videoLayer.addSubLayer();
            layer1.setScale(0.25F);
            layer1.setPosition(videoLayer.getPositionX() + videoLayer.getScaleX(), videoLayer.getPositionY());

            SubLayer layer2 = videoLayer.addSubLayer();
            layer2.setScale(0.25F);
            layer2.setPosition(layer1.getPositionX() + videoLayer.getScaleX(), videoLayer.getPositionY());

            SubLayer layer3 = videoLayer.addSubLayer();
            layer3.setScale(0.25F);
            layer3.setPosition(layer2.getPositionX() + videoLayer.getScaleX(), videoLayer.getPositionY());

            video16ImageLayout();
        }
    }

    private void video16ImageLayout() {
        for (int i = 1; i < 4; i++) {

            SubLayer layer0 = videoLayer.addSubLayer();
            layer0.setScale(0.25F);
            layer0.setPosition(videoLayer.getPositionX(), videoLayer.getPositionY() + videoLayer.getScaleY() * i);

            SubLayer layer1 = videoLayer.addSubLayer();
            layer1.setScale(0.25F);
            layer1.setPosition(videoLayer.getPositionX() + videoLayer.getScaleX(), videoLayer.getPositionY() +
                    videoLayer.getScaleY() * i);

            SubLayer layer2 = videoLayer.addSubLayer();
            layer2.setScale(0.25F);
            layer2.setPosition(layer1.getPositionX() + videoLayer.getScaleX(), videoLayer.getPositionY() + videoLayer
                    .getScaleY() * i);

            SubLayer layer3 = videoLayer.addSubLayer();
            layer3.setScale(0.25F);
            layer3.setPosition(layer2.getPositionX() + videoLayer.getScaleX(), videoLayer.getPositionY() + videoLayer
                    .getScaleY() * i);
        }
    }

    /**
     * 增加两个子图层, 左侧一个, 右侧一个;
     */
    private void videoCuoWei() {
        if (videoLayer != null) {


            SubLayer layer = videoLayer.addSubLayer();
            layer.setPosition(videoLayer.getPositionX() - 20, videoLayer.getPositionY());
            layer.setRGBAPercent(0.3f);

            //放右边;
            SubLayer layer2 = videoLayer.addSubLayer();
            layer2.setPosition(videoLayer.getPositionX() + 20, videoLayer.getPositionY());
            layer2.setRGBAPercent(0.3f);
        }
    }

    private void videoColorStart() {
        colorScaleEnable = true;
        colorScaleStatus = SCALE_STATUS_ADD;
    }

    /**
     * 画面放大一下,然后缩小一下;
     * 因为是运动的效果, 要放到progressListener中; 当点击时候, 调用videoColorStart();
     */
    private void videoColorEndge() {
        if (colorScaleEnable) {
            if (colorEdgeFilter == null) {
                colorEdgeFilter = new LanSongColorEdgeFilter();
                videoLayer.switchFilterTo(colorEdgeFilter);
            }

            if (colorScaleStatus == SCALE_STATUS_ADD) {
                colorEdgeCnt += 2;

            } else if (colorScaleStatus == SCALE_STATUS_DEL) {
                colorEdgeCnt -= 2;
            }

            float scale = 1.0f + colorEdgeCnt * 0.06f;
            videoLayer.setScale(scale); //调整缩放

            if (colorEdgeFilter != null) {
                float value = 1.0f - colorEdgeCnt * 0.08f;
                colorEdgeFilter.setVolume(value);  //蓝色轮廓微调;
            }

            if (colorEdgeCnt >= ONESCALE_FRAMES) {
                colorScaleStatus = SCALE_STATUS_DEL;
            } else if (colorEdgeCnt <= 0) {
                resetVideo();
            }
        }
    }

    private void resetVideo() {
        videoLayer.switchFilterTo(null);
        colorEdgeFilter = null;
        colorEdgeCnt = 0;
        colorScaleEnable = false;
        videoLayer.setScale(1.0f);
        colorScaleStatus = SCALE_STATUS_NONE;
    }

    /**
     * 视频子图层的每一帧, 要放到进度回调中;
     * 是一种运动效果;, 放到进度中
     */
    private void videoOutBody() {
        if (outBody != null && videoLayer != null) {
            outBodyCnt++;
            if (outBodyCnt > OUTBODY_FRAMES) {
                videoLayer.removeSubLayer(outBody);
                outBody = null;
                outBodyCnt = 0;
                outBodySacle = 1.0f;
            } else {
                outBody.setVisibility(Layer.VISIBLE);
            }
            if (outBody != null) {
                outBody.setRGBAPercent(0.3f);
                outBody.setScaledValue(outBody.getPadWidth() * outBodySacle, outBody.getPadHeight() * outBodySacle);
                outBodySacle += 0.15f;
            }
        }
    }

    /**
     * 视频负片;
     */
    private void videoColorInvert() {
        if (videoLayer != null) {
            videoLayer.switchFilterTo(new GPUImageColorInvertFilter());
        }
    }

    /**
     * 卡通
     */
    private void videoColorToon() {
        if (videoLayer != null) {
            videoLayer.switchFilterTo(new GPUImageToonFilter());
        }
    }

    /**
     * 浮雕效果;
     */
    private void videoColorLaplacian() {
        if (videoLayer != null) {
            videoLayer.switchFilterTo(new GPUImageLaplacianFilter());
        }
    }

    /**
     * 无效果
     */
    private void videoNOEffect() {
        if (videoLayer != null) {
            videoLayer.setScale(1.0f);
            videoLayer.setPosition(videoLayer.getPadWidth() / 2, videoLayer.getPadHeight() / 2);
            videoLayer.removeAllSubLayer();
            videoLayer.switchFilterTo(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mplayer != null) {
            mplayer.stop();
            mplayer.release();
            mplayer = null;
        }
        if (drawPadView != null) {
            drawPadView.stopDrawPad();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SDKFileUtils.deleteFile(dstPath);
        SDKFileUtils.deleteFile(editTmpPath);
    }

    private void initView() {
        findViewById(R.id.id_videoeffect_noeffect).setOnClickListener(this);
        findViewById(R.id.id_videoeffect_outbody).setOnClickListener(this);
        findViewById(R.id.id_videoeffect_coloredge).setOnClickListener(this);
        findViewById(R.id.id_videoeffect_cuowei).setOnClickListener(this);
        findViewById(R.id.id_videoeffect_image_mirror).setOnClickListener(this);
        findViewById(R.id.id_videoeffect_16sublay).setOnClickListener(this);
        findViewById(R.id.id_videoeffect_bg_blur).setOnClickListener(this);
        findViewById(R.id.id_videoeffect_invert).setOnClickListener(this);
        findViewById(R.id.id_videoeffect_toon).setOnClickListener(this);
        findViewById(R.id.id_videoeffect_fudiao).setOnClickListener(this);
    }
}
