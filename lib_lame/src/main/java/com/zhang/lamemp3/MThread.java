package com.zhang.lamemp3;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.zhang.lamemp3.jni.LameMp3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by 张俨 on 2017/12/21.
 */

class MThread extends Thread {

    public final static String TAG = MThread.class.getName();
    private String filePath;
    private String filePlayPath;
    private int sampleRate;
    private boolean isRecording = false;
    private boolean isPause = false;

    private Mp3Player mp3Player;
    private Handler mainHandler;
    private MHandler mHandler;
    private int voiceLevel;
    private MediaPlayer mediaPlayer;

    public MThread(Mp3Player mp3Player, Handler handler, int sampleRate) {
        super("录制音频线程");
        this.sampleRate = sampleRate;
        this.mp3Player = mp3Player;
        mainHandler = handler;
        this.start();
    }

    /**
     * 录音文件保存路径
     *
     * @param filePath
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * 获取暂停状态
     *
     * @return
     */
    public boolean isPause() {
        return isPause;
    }

    /**
     * 暂停 恢复 录制
     *
     * @param pause true 暂停录制
     *              false 恢复录制
     */
    public void setPause(boolean pause) {
        isPause = pause;
    }

    /**
     * 获取录制状态
     *
     * @return
     */
    public boolean isRecording() {
        return isRecording;
    }

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new MHandler(this);
        Looper.loop();
    }

    public MHandler getHandler() {
        return mHandler;
    }

    /**
     * 开始录制
     */
    private void _start() {
        if (isRecording) {
            return;
        }
        android.os.Process
                .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        // 根据定义好的几个配置，来获取合适的缓冲大小
        final int minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (minBufferSize < 0) {
            if (mainHandler != null) {
                mainHandler.sendEmptyMessage(Mp3Player.MSG_ERROR_GET_MIN_BUFFERSIZE);
            }
            return;
        }
        AudioRecord audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, minBufferSize * 2);
        // 5秒的缓冲
        short[] buffer = new short[sampleRate * (16 / 8) * 1 * 5];
        byte[] mp3buffer = new byte[(int) (7200 + buffer.length * 2 * 1.25)];

        FileOutputStream output = null;
        try {
            File file = createSDFile(filePath);
            output = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            if (mainHandler != null) {
                mainHandler.sendEmptyMessage(Mp3Player.MSG_ERROR_CREATE_FILE);
            }
            return;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        LameMp3.init(sampleRate, 1, sampleRate, 32);
        isRecording = true; // 录音状态
        isPause = false; // 录音状态
        try {
            try {
                audioRecord.startRecording(); // 开启录音获取音频数据
            } catch (IllegalStateException e) {
                // 不给录音...
                if (mainHandler != null) {
                    mainHandler.sendEmptyMessage(Mp3Player.MSG_ERROR_REC_START);
                }
                return;
            }

            try {
                // 开始录音
                if (mainHandler != null) {
                    mainHandler.sendEmptyMessage(Mp3Player.MSG_STARTED);
                }

                int readSize = 0;
                boolean pause = false;
                while (isRecording) {
                            /*--暂停--*/
                    if (isPause) {
                        if (!pause) {
                            pause = true;
                        }
                        continue;
                    }
                    if (pause) {
                        pause = false;
                    }
                            /*--End--*/
                            /*--实时录音写数据--*/
                    readSize = audioRecord.read(buffer, 0,
                            minBufferSize);
                    voiceLevel = getVoiceSize(readSize, buffer);
                    if (readSize < 0) {
                        if (mainHandler != null) {
                            mainHandler.sendEmptyMessage(Mp3Player.MSG_ERROR_AUDIO_RECORD);
                        }
                        break;
                    } else if (readSize == 0) {
                        ;
                    } else {
                        int encResult = LameMp3.encode(buffer,
                                buffer, readSize, mp3buffer);
                        if (encResult < 0) {
                            if (mainHandler != null) {
                                mainHandler.sendEmptyMessage(Mp3Player.MSG_ERROR_AUDIO_ENCODE);
                            }
                            break;
                        }
                        if (encResult != 0) {
                            try {
                                output.write(mp3buffer, 0, encResult);
                            } catch (IOException e) {
                                if (mainHandler != null) {
                                    mainHandler.sendEmptyMessage(Mp3Player.MSG_ERROR_WRITE_FILE);
                                }
                                break;
                            }
                        }
                    }
                            /*--End--*/
                }
                        /*--录音完--*/
                int flushResult = LameMp3.flush(mp3buffer);
                if (flushResult < 0) {
                    if (mainHandler != null) {
                        mainHandler.sendEmptyMessage(Mp3Player.MSG_ERROR_AUDIO_ENCODE);
                    }
                }
                if (flushResult != 0) {
                    try {
                        output.write(mp3buffer, 0, flushResult);
                    } catch (IOException e) {
                        if (mainHandler != null) {
                            mainHandler.sendEmptyMessage(Mp3Player.MSG_ERROR_WRITE_FILE);
                        }
                    }
                }
                try {
                    output.close();
                } catch (IOException e) {
                    if (mainHandler != null) {
                        mainHandler.sendEmptyMessage(Mp3Player.MSG_ERROR_CLOSE_FILE);
                    }
                }
                        /*--End--*/
            } finally {
                audioRecord.stop();
                audioRecord.release();
            }
        } finally {
            LameMp3.close();
            isRecording = false;
        }
        if (mainHandler != null) {
            if (mp3Player.getTime() > 1) {
                mainHandler.sendEmptyMessage(Mp3Player.MSG_FINISH);
            } else {
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
            }

        }
    }

    public int getVoiceLevel() {
        return voiceLevel;
    }

    // 获得声音的level
    public int getVoiceSize(int r, short[] buffer) {
        if (isRecording) {
            try {
                long v = 0;
                // 将 buffer 内容取出，进行平方和运算
                for (int i = 0; i < buffer.length; i++) {
                    v += buffer[i] * buffer[i];
                }
                // 平方和除以数据总长度，得到音量大小。
                double mean = v / (double) r;
                double volume = 10 * Math.log10(mean);
                return (((int) volume / 10) - 1);
            } catch (Exception e) {
                // TODO Auto-generated catch block

            }
        }

        return 1;
    }

    public void onDestory() {
        stopPlay();
        if (!mHandler.hasMessages(MSG_DESTORY)) {
            mHandler.sendEmptyMessage(MSG_DESTORY);
        }
    }

    /**
     * 录制结束
     */
    public void restoreRecording() {

        isRecording = false;
    }

    //mp3 录制
    public static final int MSG_START = 1;
    public static final int MSG_DESTORY = 2;
    // mp3 播放
    public static final int MSG_PLAY = 3;
    public static final int MSG_PAUSE = 4;
    public static final int MSG_RESTORE = 5;

    /**
     * 音乐播放路径
     *
     * @param filePlayPath
     */
    public void setFilePlayPath(String filePlayPath) {
        this.filePlayPath = filePlayPath;
    }

    public void getPlayTime() {
        if (mediaPlayer != null) {
        }
    }

    /**
     * 播放音频
     *
     * @return
     */
    public boolean play(String filePath) {
        if (isRecording) {
            Log.e(TAG, "请先关闭录音");
            return false;
        }
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        try {
            //设置要播放的文件
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepareAsync();
            //播放
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mainHandler.sendEmptyMessage(Mp3Player.MSG_SECOND_PLAY_START);
                    mediaPlayer.start();
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.e(TAG,"onCompletion");
                    mainHandler.sendEmptyMessage(Mp3Player.MSG_SECOND_PLAY_FINISH);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            mainHandler.sendEmptyMessage(Mp3Player.MSG_ERROR_PLAYER);
            stopPlay();
        }

        return false;
    }

    /**
     * 暂停播放
     */
    public void playPause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public MediaPlayer getMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        return mediaPlayer;
    }

    /**
     * 恢复播放
     */
    public void playReStore() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    /**
     * 音频播放
     * 释放资源
     *
     * @return
     */
    public boolean stopPlay() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        return false;
    }

    /**
     * 录音状态管理
     */
    static class MHandler extends Handler {

        private final WeakReference<MThread> recorder;

        public MHandler(MThread mp3Recorder) {
            recorder = new WeakReference<>(mp3Recorder);
        }

        @Override
        public void handleMessage(Message msg) {
            if (recorder != null && recorder.get() != null) {

                MThread mp3Recorder = recorder.get();
                switch (msg.what) {
                    case MSG_START:
                        mp3Recorder._start();
                        break;
                    case MSG_PLAY:
                        mp3Recorder.play(mp3Recorder.filePlayPath);
                        break;
                    case MSG_PAUSE:
                        mp3Recorder.playPause();
                        break;
                    case MSG_RESTORE:
                        mp3Recorder.playReStore();
                        break;
                    case MSG_DESTORY:
                        mp3Recorder.restoreRecording();
                        this.removeCallbacksAndMessages(null);
                        Looper.myLooper().quit();
                        break;


                }
            }
        }
    }


    /**
     * 在SD卡上创建文件
     *
     * @throws IOException
     */
    public static File createSDFile(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists())
            if (file.isDirectory()) {
                file.mkdirs();
            } else {
                file.createNewFile();
            }
        return file;
    }
}
