package com.example.advanceDemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.TextureLayer;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.box.onDrawPadThreadProgressListener;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.FilterLibrary;
import com.lansosdk.videoeditor.FilterLibrary.OnGpuImageFilterChosenListener;

import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageFilter;
import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageSepiaFilter;

public class TextureLayerDemoActivity extends Activity {
    public static final int NO_TEXTURE = -1;
    private static final String TAG = "TextureLayerDemo";
    private DrawPadView drawPadView;
    private TextureLayer textureLayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.texturelayer_demo_layout);

        drawPadView = (DrawPadView) findViewById(R.id.id_texturelayer_drawpadview);

        findViewById(R.id.id_texturelayer_testbutton).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectFilter();
                    }
                });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initDrawPad();
            }
        }, 200);

    }

    private void initDrawPad() {
        drawPadView.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH, 30);
        drawPadView
                .setOnDrawPadThreadProgressListener(new onDrawPadThreadProgressListener() {

                    @Override
                    public void onThreadProgress(DrawPad v, long currentTimeUs) {
                        addTextureLayer();
                    }
                });
        drawPadView.setDrawPadSize(480, 480,
                new onDrawPadSizeChangedListener() {

                    @Override
                    public void onSizeChanged(int viewWidth, int viewHeight) {
                        startDrawPad();
                    }
                });
    }

    private void startDrawPad() {
        drawPadView.pauseDrawPad();
        if (drawPadView.startDrawPad()) {
            // 当前已经需要增加
            String picPath = CopyFileFromAssets.copyAssets(
                    getApplicationContext(), "pic720x720.jpg");
            BitmapLayer bgLayer = drawPadView.addBitmapLayer(BitmapFactory
                    .decodeFile(picPath));
            bgLayer.setScaledValue(bgLayer.getPadWidth(),
                    bgLayer.getPadHeight());

            drawPadView.resumeDrawPad();
        }
    }

    /**
     * 增加一个纹理.
     */
    private void addTextureLayer() {
        if (textureLayer == null) {
            Bitmap bmp = BitmapFactory.decodeFile("/sdcard/b2.jpg");
            int textureId = loadTexture(bmp, NO_TEXTURE, false); // 需要在我们的DrawPad线程中创建纹理.
            textureLayer = drawPadView.addTextureLayer(textureId,
                    bmp.getWidth(), bmp.getHeight(), new GPUImageSepiaFilter());
            Log.i(TAG, "增加一个纹理图层");
        }
    }

    /**
     * 选择滤镜效果,
     */
    private void selectFilter() {
        FilterLibrary.showDialog(this, new OnGpuImageFilterChosenListener() {

            @Override
            public void onGpuImageFilterChosenListener(
                    final GPUImageFilter filter, String name) {
                if (textureLayer != null) {
                    textureLayer.switchFilterTo(filter);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (drawPadView != null) {
            drawPadView.stopDrawPad();
            drawPadView = null;
            textureLayer = null;
        }
    }

    public int loadTexture(final Bitmap img, final int usedTexId,
                           final boolean recycle) {
        int textures[] = new int[1];
        if (usedTexId == NO_TEXTURE) {

            GLES20.glGenTextures(1, textures, 0);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, img, 0);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, img);
            textures[0] = usedTexId;
        }
        if (recycle) {
            img.recycle();
        }
        return textures[0];
    }
}
