package com.zhang.lamemp3;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private Mp3Player recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final boolean permission = PermissionTool.checkPermission(getApplicationContext(), "android.permission.WRITE_EXTERNAL_STORAGE", 100);
        if (permission) {
            init();
        }

    }

    private void init() {
        recorder = new Mp3Player(this, 8000);
        final String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/zhang";
        Log.e("------", dir);
        final String fileName = "test.mp3";
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }

        recorder.setFilePath(dir + "/" + fileName);//录音保存目录
        final TextView tv = (TextView) findViewById(R.id.sample_text);
        Button btn_stop = (Button) findViewById(R.id.btn_stop);
        Button btn_record = (Button) findViewById(R.id.btn_record);
        Button btn_pause = (Button) findViewById(R.id.btn_pause);
        Button btn_continue = (Button) findViewById(R.id.btn_continue);
        Button btn_play_stop = (Button) findViewById(R.id.btn_play_stop);
        Button btn_play = (Button) findViewById(R.id.btn_play);

        btn_play_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.stopPlay();
            }
        });
        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.play(dir + "/" + fileName);
            }
        });
        recorder.setOnPlayListener(new Mp3Player.AudioPlayerListener() {
            @Override
            public void AudioUpdate(int level, int time) {
                tv.setText(String.valueOf("播放  \n level = " + level + "\n time = " + time + "s"));
            }

            @Override
            public void finish() {
                Toast.makeText(MainActivity.this, "播放结束", Toast.LENGTH_SHORT).show();
            }
        });
        btn_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.start();
            }
        });
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.stop();
            }
        });
        btn_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.pause();
            }
        });
        btn_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.restore();
            }
        });
        recorder.setAudioUpdateListener(new Mp3Player.AudioUpdateListener() {
            @Override
            public void AudioUpdate(int level, int time) {
                tv.setText(String.valueOf("录制  \nlevel = " + level + "\n time = " + time + "s"));
            }

            @Override
            public void finish() {
                Toast.makeText(MainActivity.this, "录制结束", Toast.LENGTH_SHORT).show();
            }
        });
        recorder.setOnErrorListener(new Mp3Player.OnErrorListener() {
            @Override
            public void onErrorInfo(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "请开启手机读写存储权限", Toast.LENGTH_SHORT).show();
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                init();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recorder.onDestory();
    }
}
