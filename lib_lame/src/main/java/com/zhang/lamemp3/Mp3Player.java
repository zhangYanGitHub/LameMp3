package com.zhang.lamemp3;


import android.app.Service;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Create by ZhangYan on 2017/12/19.
 * 类功能描述：
 * MP3实时录制功能,可暂停
 */
public class Mp3Player {
    private final AudioManager aManger;
    private Context context;
    private int sampleRate;

    public final static String TAG = Mp3Player.class.getName();
    /**
     * 开始录音
     */
    public static final int MSG_STARTED = 1;

    /**
     * 结束录音
     */
    public static final int MSG_FINISH = 2;


    /**
     * 录音回调
     */
    public static final int MSG_SECONMD_CALLBACK = 3;


    /**
     * 播放回调
     */
    public static final int MSG_SECOND_PLAY_CALLBACK = 4;
    /**
     * 开始播放
     */
    public static final int MSG_SECOND_PLAY_START = 5;
    /**
     * 结束播放
     */
    public static final int MSG_SECOND_PLAY_FINISH = 6;

    /**
     * 缓冲区挂了,采样率手机不支持
     */
    public static final int MSG_ERROR_GET_MIN_BUFFERSIZE = -1;

    /**
     * 创建文件时出错
     */
    public static final int MSG_ERROR_CREATE_FILE = -2;

    /**
     * 初始化录音器时出错
     */
    public static final int MSG_ERROR_REC_START = -3;

    /**
     * 录紧音的时候出错
     */
    public static final int MSG_ERROR_AUDIO_RECORD = -4;

    /**
     * 编码时挂了
     */
    public static final int MSG_ERROR_AUDIO_ENCODE = -5;

    /**
     * 写文件时挂了
     */
    public static final int MSG_ERROR_WRITE_FILE = -6;

    /**
     * 没法关闭文件流
     */
    public static final int MSG_ERROR_CLOSE_FILE = -7;

    /**
     * 录制时间过短
     */
    public static final int MSG_ERROR_TIME = -8;
    /**
     * 播放音频出错
     */
    public static final int MSG_ERROR_PLAYER = -9;

    /**
     * 录音开始时间
     */
    private long startTime;
    private final MThread mThread;
    private MThread.MHandler mThreadHandler;
    private final Handler mainHandler;
    private long pauseTime;
    private long startPlayTime;

    /**
     * 采样率：音频的采样频率，每秒钟能够采样的次数，
     * 采样率越高，音质越高。给出的实例是44100、22050、11025但不限于这几个参数。
     * 例如要采集低质量的音频就可以使用4000、8000等低采样率。
     *
     * @param sampleRate 采样率
     */
    public Mp3Player(Context context, int sampleRate) {
        this.context = context;
        this.sampleRate = sampleRate;
        mainHandler = new MHandler(this);
        mThread = new MThread(this, mainHandler, sampleRate);
        aManger = (AudioManager) context.getSystemService(Service.AUDIO_SERVICE);
    }

    public void setFilePath(String filePath) {
        mThread.setFilePath(filePath);
    }

    /**
     * 开片
     */
    public void start() {
        pauseTimeList.clear();

        mThread.getHandler().sendEmptyMessage(MThread.MSG_START);
    }

    /**
     * 停止录音
     */
    public void stop() {
        final boolean recording = mThread.isRecording();
        if (mainHandler.hasMessages(Mp3Player.MSG_SECONMD_CALLBACK)) {
            mainHandler.removeMessages(Mp3Player.MSG_SECONMD_CALLBACK);
        }

        if (recording) {
            mThread.restoreRecording();
        }
    }

    /**
     * 暂停录制
     */
    public void pause() {
        if (mThread.isPause()) {
            return;
        }
        if (getTime() < 1) {
            mainHandler.sendEmptyMessage(MSG_ERROR_TIME);
            stop();
        }

        if (mainHandler.hasMessages(Mp3Player.MSG_SECONMD_CALLBACK)) {
            mainHandler.removeMessages(Mp3Player.MSG_SECONMD_CALLBACK);
        }

        //记录当前暂停时间
        pauseTime = System.currentTimeMillis();
        mThread.setPause(true);
    }

    List<Integer> pauseTimeList = new ArrayList<>();

    /**
     * 恢复录制
     */
    public void restore() {
        if (!mThread.isPause()) {
            return;
        }
        if (!mainHandler.hasMessages(Mp3Player.MSG_SECONMD_CALLBACK)) {
            mainHandler.sendEmptyMessage(Mp3Player.MSG_SECONMD_CALLBACK);
        }
        int s = (int) (System.currentTimeMillis() - pauseTime);
        if (s > 0) {
            pauseTimeList.add(s);
        }
        mThread.setPause(false);
    }

    /**
     * 获取录音状态
     *
     * @return
     */
    public boolean isRecording() {
        return mThread.isRecording();
    }

    /**
     * 获取暂停状态
     *
     * @return
     */
    public boolean isPause() {
        if (!mThread.isRecording()) {
            return false;
        }
        return mThread.isPause();
    }


    public int getVoiceLevel() {
        return mThread.getVoiceLevel();
    }

    public interface AudioStageListener {
        void onWellPrepared();
    }

    public interface OnErrorListener {
        void onErrorInfo(String message);
    }

    public interface AudioUpdateListener {
        /**
         * @param level 音量大小
         * @param time  录音时间 单位 秒
         */
        void AudioUpdate(int level, int time);

        /**
         * 录音完成回调
         */
        void finish();
    }

    public AudioStageListener mListener;
    public AudioUpdateListener audioUpdateListener;
    public OnErrorListener onErrorListener;

    /**
     * @param audioUpdateListener
     */
    public void setAudioUpdateListener(AudioUpdateListener audioUpdateListener) {
        this.audioUpdateListener = audioUpdateListener;
    }

    /**
     * 录制开始监听
     *
     * @param listener
     */
    public void setOnAudioStageListener(AudioStageListener listener) {
        mListener = listener;
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    public void onDestory() {
        mThread.onDestory();

    }

    /**
     * 声音大小 及时间 回调
     */
    private int TIME_CALLBACK = 500;
    private int MAX_TIME = 30;

    /**
     * '设置最大录制时间
     *
     * @param MAX_TIME 默认30s
     */
    public void setMAX_TIME(int MAX_TIME) {
        this.MAX_TIME = MAX_TIME;
    }

    /**
     * 设置回调时间间隔
     *
     * @param TIME_CALLBACK 默认500ms
     */
    public void setTIME_CALLBACK(int TIME_CALLBACK) {
        this.TIME_CALLBACK = TIME_CALLBACK;
    }

    private AudioPlayerListener onPlayListener;

    public void setOnPlayListener(AudioPlayerListener onPlayListener) {
        this.onPlayListener = onPlayListener;
    }

    public interface AudioPlayerListener {
        /**
         * @param level 音量大小
         * @param time  录音时间 单位 秒
         */
        void AudioUpdate(int level, int time);

        /**
         * 录音完成回调
         */
        void finish();
    }

    static class MHandler extends Handler {

        private final WeakReference<Mp3Player> recorder;

        public MHandler(Mp3Player mp3Player) {
            recorder = new WeakReference<>(mp3Player);
        }

        int level = 1;

        @Override
        public void handleMessage(Message msg) {
            if (recorder != null && recorder.get() != null) {
                Mp3Player mp3Player = recorder.get();
                String errorMessage = null;
                switch (msg.what) {
                    case MSG_ERROR_GET_MIN_BUFFERSIZE:
                        errorMessage = "缓冲区出错,采样率手机不支持";
                        break;
                    case MSG_ERROR_CREATE_FILE:
                        errorMessage = "创建文件时出错";
                        break;
                    case MSG_ERROR_REC_START:
                        errorMessage = "初始化录音器时出错";
                        break;
                    case MSG_ERROR_AUDIO_RECORD:
                        errorMessage = "录音的时候出错";
                        break;
                    case MSG_ERROR_AUDIO_ENCODE:
                        errorMessage = "编码时出错";
                        break;
                    case MSG_ERROR_WRITE_FILE:
                        errorMessage = "写文件时出错";
                        break;
                    case MSG_ERROR_CLOSE_FILE:
                        errorMessage = "关闭文件流出错";
                        break;
                    case MSG_ERROR_TIME:
                        errorMessage = "录制时间过短";
                        break;
                    case MSG_ERROR_PLAYER:
                        errorMessage = "播放音频出错";
                        break;
                    case MSG_STARTED:
                        if (mp3Player.mListener != null) {
                            mp3Player.mListener.onWellPrepared();
                        }
                        mp3Player.startTime = System.currentTimeMillis();
                        if (!hasMessages(MSG_SECONMD_CALLBACK)) {
                            sendEmptyMessage(MSG_SECONMD_CALLBACK);
                        }
                        break;
                    case MSG_FINISH:
                        if (mp3Player.audioUpdateListener != null) {
                            mp3Player.audioUpdateListener.finish();
                        }
                        break;
                    case MSG_SECONMD_CALLBACK:
                        if (this.hasMessages(Mp3Player.MSG_SECONMD_CALLBACK)) {
                            this.removeMessages(Mp3Player.MSG_SECONMD_CALLBACK);
                        }
                        int time = mp3Player.getTime();
                        int voiceLevel = mp3Player.getVoiceLevel();
                        if (mp3Player.audioUpdateListener != null) {
                            mp3Player.audioUpdateListener.AudioUpdate(voiceLevel, time);
                        }
                        if (time == mp3Player.MAX_TIME) {
                            mp3Player.mThread.restoreRecording();
                        } else {
                            sendEmptyMessageDelayed(MSG_SECONMD_CALLBACK, mp3Player.TIME_CALLBACK);
                        }
                        break;
                    case MSG_SECOND_PLAY_CALLBACK://音乐播放回调
                        if (this.hasMessages(Mp3Player.MSG_SECOND_PLAY_CALLBACK)) {
                            this.removeMessages(Mp3Player.MSG_SECOND_PLAY_CALLBACK);
                        }
                        int playTime = (int) (System.currentTimeMillis() - mp3Player.startPlayTime);
                        level++;
                        if (level > 5) {
                            level = 1;
                        }
                        if (mp3Player.onPlayListener != null) {
                            mp3Player.onPlayListener.AudioUpdate(level, playTime / 1000);
                        }
                        this.sendEmptyMessageDelayed(Mp3Player.MSG_SECOND_PLAY_CALLBACK, mp3Player.TIME_CALLBACK);
                        break;
                    case MSG_SECOND_PLAY_START://音乐开始播放
                        if (!this.hasMessages(Mp3Player.MSG_SECOND_PLAY_CALLBACK)) {
                            this.removeMessages(Mp3Player.MSG_SECOND_PLAY_CALLBACK);
                        }
                        mp3Player.startPlayTime = System.currentTimeMillis();
                        sendEmptyMessageDelayed(MSG_SECOND_PLAY_CALLBACK, mp3Player.TIME_CALLBACK);
                        break;
                    case MSG_SECOND_PLAY_FINISH://音乐结束播放
                        if (this.hasMessages(Mp3Player.MSG_SECOND_PLAY_CALLBACK)) {
                            this.removeMessages(Mp3Player.MSG_SECOND_PLAY_CALLBACK);
                        }
                        if (mp3Player.onPlayListener != null) {
                            mp3Player.onPlayListener.finish();
                        }
                        break;
                }

                if (!TextUtils.isEmpty(errorMessage) && mp3Player.onErrorListener != null) {
                    mp3Player.onErrorListener.onErrorInfo(errorMessage);
                }

            }
        }
    }

    /**
     * 获取当前录制时间 单位秒
     *
     * @return
     */
    public int getTime() {
        long time = (System.currentTimeMillis() - startTime);
        for (int i : pauseTimeList) {
            time -= i;
        }
        return (int) (time / 1000);
    }

    /**
     * 获取当前播放时间 单位秒
     *
     * @return
     */
    public int getPlayTime() {
        long time = (System.currentTimeMillis() - startPlayTime);

        return (int) (time / 1000);
    }

    /**
     * 播放音频
     *
     * @param path 文件路径
     */
    public void play(String path) {
        if (TextUtils.isEmpty(path)) {
            Log.e(TAG, "filePath == null || filePath==\"\" 请先设置音频播放路径");
            return;
        }
        mThread.setFilePlayPath(path);
        mThread.getHandler().sendEmptyMessage(MThread.MSG_PLAY);
    }

    /**
     * 停止播放音频
     */
    public void stopPlay() {
        mThread.stopPlay();

        if (mainHandler.hasMessages(Mp3Player.MSG_SECOND_PLAY_CALLBACK)) {
            mainHandler.removeMessages(Mp3Player.MSG_SECOND_PLAY_CALLBACK);
        }
    }

    /**
     * 暂停播放
     */
    public void playPause() {
        mThread.playPause();
        if (mainHandler.hasMessages(Mp3Player.MSG_SECOND_PLAY_CALLBACK)) {
            mainHandler.removeMessages(Mp3Player.MSG_SECOND_PLAY_CALLBACK);
        }
    }

    /**
     * 恢复播放
     */
    public void playReStore() {
        mThread.playReStore();
        if (!mainHandler.hasMessages(Mp3Player.MSG_SECOND_PLAY_CALLBACK)) {
            mainHandler.sendEmptyMessage(Mp3Player.MSG_SECOND_PLAY_CALLBACK);
        }
    }

    /**
     * 设置扬声器 听筒模式
     *
     * @param speakerphoneOn
     */
    public void setPlayMode(boolean speakerphoneOn) {
        if (!speakerphoneOn) {
            aManger.setSpeakerphoneOn(false);
            aManger.setMode(AudioManager.MODE_IN_COMMUNICATION);
            mThread.getMediaPlayer().setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
        } else {
            aManger.setMode(AudioManager.MODE_NORMAL);
            aManger.setSpeakerphoneOn(true);
            mThread.getMediaPlayer().setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

    }

}