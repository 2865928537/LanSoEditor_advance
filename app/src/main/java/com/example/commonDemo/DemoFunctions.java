package com.example.commonDemo;

import android.content.Context;
import android.util.Log;

import com.lansosdk.videoeditor.CopyDefaultVideoAsyncTask;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoEditor;

/**
 * 注意: 此代码仅作为视频处理的演示使用, 不属于sdk的一部分.
 */
public class DemoFunctions {

    public final static String TAG = "DemoFunctions";

    /**
     * 演示音频和视频合成, 也可以认为是音频替换.
     * <p>
     * 音视频合成:\n把一个纯音频文件和纯视频文件合并成一个mp4格式的多媒体文件, 如果源视频之前有音频,会首先删除音频部分. \n\n
     */
    public static int demoAVMerge(Context ctx, VideoEditor editor,
                                  String srcVideo, String dstPath) {
        int ret = -1;
        MediaInfo info = new MediaInfo(srcVideo, false);
        if (info.prepare()) {
            String video2 = srcVideo;
            String video3 = null;
            // 如果源视频中有音频,则先删除音频
            if (info.isHaveAudio()) {
                video3 = SDKFileUtils.createFileInBox(info.fileSuffix);
                editor.executeDeleteAudio(video2, video3);

                video2 = video3;
            }

            String audio = CopyDefaultVideoAsyncTask
                    .copyFile(ctx, "aac20s.aac");
            ret = editor.executeVideoMergeAudio(video2, audio, dstPath);
            SDKFileUtils.deleteFile(video3);
        }

        return ret;
    }

    /**
     * 演示音视频分离
     * <p>
     * 音视频分离:\n把多媒体文件如mp4,flv等中的的音频和视频分离开,形成独立的音频文件和视频文件 \n\n
     */
    public static int demoAVSplite(VideoEditor editor, String srcVideo,
                                   String dstVideo, String dstAudio) {
        MediaInfo info = new MediaInfo(srcVideo);
        int ret = -1;
        if (info.prepare()) {
            ret = editor.executeDeleteAudio(srcVideo, dstVideo);
            ret = editor.executeDeleteVideo(srcVideo, dstAudio);
        }
        return ret;
    }

    /**
     * 演示 视频截取
     * <p>
     * 视频剪切:\n剪切视频的长度,可以指定开始位置,指定结束位置.\n这里演示截取视频的前20秒或时长的一半,形成新的视频文件.
     */
    public static int demoVideoCut(VideoEditor editor, String srcVideo,
                                   String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        if (info.prepare()) {
            if (info.vDuration > 20)
                return editor.executeVideoCutOut(srcVideo, dstVideo, 0, 20);
            else
                return editor.executeVideoCutOut(srcVideo, dstVideo, 0,
                        info.vDuration / 2);
        }
        return -1;
    }

    /**
     * 演示音频截取
     * <p>
     * 音频剪切:\n剪切音频的长度,可以指定开始位置,指定结束位置.\n这里演示截取音频的前20秒或时长的一半,形成新的音频文件.
     */
    public static int demoAudioCut(Context ctx, VideoEditor editor,
                                   String dstAudio) {
        String srcAudio = CopyDefaultVideoAsyncTask.copyFile(ctx,
                "honor30s2.m4a");
        MediaInfo info = new MediaInfo(srcAudio);
        if (info.prepare() && info.aCodecName != null) {
            if (info.aDuration > 15)
                return editor.executeAudioCutOut(srcAudio, dstAudio, 0, 15);
            else
                return editor.executeAudioCutOut(srcAudio, dstAudio, 0,
                        info.aDuration / 2);
        } else {
            return -1;
        }
    }

    /**
     * 视频拼接 ,
     * <p>
     * 为了方便演示,需要您的视频大于20秒(或用默认视频).先把原视频从(0到1/3处)和(2/3到结束)截取成两段,这样就有了两个独立的视频,
     * 然后把这两段拼接在一起,来演示视频的拼接, 您实际可任意的组合,注意尽量视频的宽高比等参数一致,不然合成是可以,但有些播放器无法播放.
     */
    public static int demoVideoConcat(VideoEditor editor, String srcVideo,
                                      String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        int ret = -1;
        if (info.prepare() && info.vDuration > 20) {
            // 第一步:先创建三个视频文件,并剪切好视频.
            String seg1 = SDKFileUtils.createFileInBox(info.fileSuffix);
            String seg2 = SDKFileUtils.createFileInBox(info.fileSuffix);

            String segTs1 = SDKFileUtils.createFileInBox("ts");
            String segTs2 = SDKFileUtils.createFileInBox("ts");

            ret = editor.executeVideoCutOut(srcVideo, seg1, 0,
                    info.vDuration / 3);
            ret = editor.executeVideoCutOut(srcVideo, seg2,
                    info.vDuration * 2 / 3, info.vDuration);

            // 第二步: 把他们转换为ts格式.
            ret = editor.executeConvertMp4toTs(seg1, segTs1);
            ret = editor.executeConvertMp4toTs(seg2, segTs2);

            // 第三步: 把ts文件拼接成mp4
            ret = editor.executeConvertTsToMp4(new String[]{segTs1, segTs2},
                    dstVideo);

            // 第四步: 删除之前的临时文件.

            SDKFileUtils.deleteFile(segTs2);
            SDKFileUtils.deleteFile(segTs1);
            SDKFileUtils.deleteFile(seg2);
            SDKFileUtils.deleteFile(seg1);
        }
        return ret;
    }

    /**
     * 两个音频在混合时,第二个延时一定时间.
     */
    public static int demoAudioDelayMix(Context ctx, VideoEditor editor,
                                        String dstAudio) {
        String audiostr1 = CopyDefaultVideoAsyncTask
                .copyFile(ctx, "aac20s.aac");
        String audiostr2 = CopyDefaultVideoAsyncTask.copyFile(ctx,
                "honor30s2.m4a");

        return editor.executeAudioDelayMix(audiostr1, audiostr2, 3000, 3000,
                dstAudio);
    }

    /**
     * 两个音频在混合时调整音量
     */
    public static int demoAudioVolumeMix(Context ctx, VideoEditor editor,
                                         String dstAudio) {
        String audiostr1 = CopyDefaultVideoAsyncTask
                .copyFile(ctx, "aac20s.aac");
        String audiostr2 = CopyDefaultVideoAsyncTask.copyFile(ctx,
                "honor30s2.m4a");

        return editor.executeAudioVolumeMix(audiostr1, audiostr2, 0.5f, 4,
                dstAudio);
    }

    /**
     * 垂直平镜像
     */
    public static int demoVideoMirrorH(VideoEditor editor, String srcVideo,
                                       String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        if (info.prepare()) {
            int bitrate = (int) (info.vBitRate * 1.5f);
            if (bitrate > 2000 * 1000)
                bitrate = 2000 * 1000; // 2M

            return editor.executeVideoMirrorH(srcVideo, info.vCodecName,
                    bitrate, dstVideo);
        } else {
            return -1;
        }
    }

    /**
     * 水平镜像
     */
    public static int demoVideoMirrorV(VideoEditor editor, String srcVideo,
                                       String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        if (info.prepare()) {
            int bitrate = (int) (info.vBitRate * 1.5f);
            if (bitrate > 2000 * 1000)
                bitrate = 2000 * 1000; // 2M

            return editor.executeVideoMirrorV(srcVideo, info.vCodecName,
                    bitrate, dstVideo);
        } else {
            return -1;
        }
    }

    /**
     * 视频逆向
     */
    public static int demoVideoReverse(VideoEditor editor, String srcVideo,
                                       String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        if (info.prepare()) {
            int bitrate = (int) (info.vBitRate * 1.5f);
            if (bitrate > 2000 * 1000)
                bitrate = 2000 * 1000; // 2M

            return editor.executeVideoReverse(srcVideo, info.vCodecName,
                    bitrate, dstVideo);
        } else {
            return -1;
        }
    }

    /**
     * 视频增加边框
     */
    public static int demoPaddingVideo(VideoEditor editor, String srcVideo,
                                       String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        if (info.prepare()) {
            int bitrate = (int) (info.vBitRate * 1.5f);
            if (bitrate > 2000 * 1000)
                bitrate = 2000 * 1000; // 2M

            int width = info.vCodecWidth + 32; // 向外padding32个像素
            int height = info.vCodecHeight + 32;

            return editor.executePadingVideo(srcVideo, info.vCodecName, width,
                    height, 0, 0, dstVideo, (int) (info.vBitRate * 1.5f));
        } else {
            return -1;
        }
    }

    /**
     * 获取一张图片
     */
    public static int demoGetOneFrame(VideoEditor editor, String srcVideo,
                                      String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        if (info.prepare()) {
            String picPath = SDKFileUtils.createFileInBox("png"); // 这里是保存的路径
            Log.i("lansosdk", "picture save at " + picPath);
            return editor.executeGetOneFrame(srcVideo, info.vCodecName,
                    info.vDuration / 2, picPath);
        } else {
            return -1;
        }
    }

    /**
     * 视频垂直方向反转
     *
     * @return
     */
    public static int demoVideoRotateVertically(VideoEditor editor,
                                                String srcVideo, String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        if (info.prepare()) {
            return editor.executeVideoRotateVertically(srcVideo,
                    info.vCodecName, (int) (info.vBitRate * 1.5f), dstVideo);
        } else {
            return -1;
        }
    }

    /**
     * 视频水平方向反转
     */
    public static int demoVideoRotateHorizontally(VideoEditor editor,
                                                  String srcVideo, String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        if (info.prepare()) {
            int bitrate = (int) (info.vBitRate * 1.5f);
            if (bitrate > 2000 * 1000)
                bitrate = 2000 * 1000; // 2M

            return editor.executeVideoRotateHorizontally(srcVideo,
                    info.vCodecName, (int) (info.vBitRate * 1.5f), dstVideo);
        } else {
            return -1;
        }
    }

    /**
     * 视频顺时针旋转９０度
     */
    public static int demoVideoRotate90Clockwise(VideoEditor editor,
                                                 String srcVideo, String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        if (info.prepare()) {
            int bitrate = (int) (info.vBitRate * 1.5f);
            if (bitrate > 2000 * 1000)
                bitrate = 2000 * 1000; // 2M

            int ret = editor.executeVideoRotate90Clockwise(srcVideo,
                    info.vCodecName, bitrate, dstVideo);
            if (ret != 0) {
                ret = editor.executeVideoRotate90Clockwise(srcVideo, "h264",
                        bitrate, dstVideo);
            }
            return ret;
        } else {
            return -1;
        }
    }

    /**
     * 视频逆时针旋转９０度,也即使顺时针旋转270度.
     */
    public static int demoVideoRotate90CounterClockwise(VideoEditor editor,
                                                        String srcVideo, String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        if (info.prepare()) {
            int bitrate = (int) (info.vBitRate * 1.5f);
            if (bitrate > 2000 * 1000)
                bitrate = 2000 * 1000; // 2M

            return editor.executeVideoRotate90CounterClockwise(srcVideo,
                    info.vCodecName, bitrate, dstVideo);
        } else {
            return -1;
        }
    }

    /**
     * 视频和音频都逆序
     */
    public static int demoAVReverse(VideoEditor editor, String srcVideo,
                                    String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        if (info.prepare()) {
            int bitrate = (int) (info.vBitRate * 1.5f);
            if (bitrate > 2000 * 1000)
                bitrate = 2000 * 1000; // 2M

            return editor.executeAVReverse(srcVideo, info.vCodecName, bitrate,
                    dstVideo);
        } else {
            return -1;
        }
    }

    /**
     * 调整视频的播放速度.
     */
    public static int demoVideoAdjustSpeed(VideoEditor editor, String srcVideo,
                                           String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        if (info.prepare()) {
            int bitrate = (int) (info.vBitRate * 1.5f);
            if (bitrate > 3000 * 1000)
                bitrate = 3000 * 1000; // 3M

            return editor.executeVideoAdjustSpeed2(srcVideo, info.vCodecName,
                    0.5f, bitrate, dstVideo);
        } else {
            return -1;
        }
    }

    /**
     * 矫正视频的角度.
     */
    public static int demoVideoZeroAngle(VideoEditor editor, String srcVideo,
                                         String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        if (info.prepare() && info.vRotateAngle != 0) {
            int bitrate = (int) (info.vBitRate * 1.5f);
            if (bitrate > 2000 * 1000)
                bitrate = 2000 * 1000; // 2M

            return editor.executeVideoZeroAngle(srcVideo, info.vCodecName,
                    bitrate, dstVideo);
        } else {
            return -1;
        }
    }

    /**
     * 设置视频角度元数据.
     */
    public static int demoSetVideoMetaAngle(VideoEditor editor,
                                            String srcVideo, String dstVideo) {
        MediaInfo info = new MediaInfo(srcVideo);
        if (info.prepare()) {
            return editor.executeSetVideoMetaAngle(srcVideo, 270, dstVideo);
        } else {
            return -1;
        }
    }

}
