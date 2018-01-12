# LameMP3
####  采用lame开源库，通过JNI 调用本地方法 实现MP3格式的录制 https://sourceforge.net/projects/lame/files/lame/
 
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/zhangYanGitHub/LameMP3/pulls)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](https://github.com/zhangYanGitHub/LameMP3/blob/master/LICENSE) 

### 最新版本

模块|build-gradle|
---|---|
最新版本|[![Download](https://api.bintray.com/packages/zhangyan000/maven/lamemp3/images/download.svg)](https://bintray.com/zhangyan000/maven/lamemp3/_latestVersion)|


  * 开源lame  版本 3.100
  
  * 引入
        
        compile 'com.zhang.lamemp3:lib_lame:1.0.0'
  
  * 使用方法
  
        /**
         * 采样率：音频的采样频率，每秒钟能够采样的次数，
         *采样率越高，音质越高。给出的实例是44100、22050、11025但不限于这几个参数。
         */
        recorder = new Mp3Player(this, 8000);

        recorder.setFilePath(dir + "/" + fileName);//录音保存目录
        //停止播放监听
        btn_play_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.stopPlay();
            }
        });
        //播放本地音乐
        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.play(dir + "/" + fileName);
            }
        });
        //播放回调 可以按需要封装
        recorder.setOnPlayListener(new Mp3Player.AudioPlayerListener() {
            @Override
            public void AudioUpdate(int level, int time) {
                tv.setText(String.valueOf("播放  \n level = " + level + "\n time = " + time + "s"));
            }
        });
        //播放完成监听
        recorder.setOnPlayFinishListener(new Mp3Player.AudioPlayerFinishListener() {
            @Override
            public void finish() {
                Toast.makeText(MainActivity.this, "播放结束", Toast.LENGTH_SHORT).show();
            }
        });
        //开始录制语音
        btn_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.start();
            }
        });
        //停止录制
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.stop();
            }
        });
        //暂停录制
        btn_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.pause();
            }
        });
        //继续录制
        btn_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.restore();
            }
        });
        //录制时间回调
        recorder.setAudioUpdateListener(new Mp3Player.AudioUpdateListener() {
            @Override
            public void AudioUpdate(int level, int time) {
                tv.setText(String.valueOf("录制  \nlevel = " + level + "\n time = " + time + "s"));
            }
        });
        //录制结束监听
        recorder.setAudioFinishListener(new Mp3Player.AudioFinishListener() {
            @Override
            public void finish() {
                Toast.makeText(MainActivity.this, "录制结束", Toast.LENGTH_SHORT).show();
            }
        });
        //录制错误监听
        recorder.setOnErrorListener(new Mp3Player.OnErrorListener() {
            @Override
            public void onErrorInfo(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
* 最后记得释放子线程

        @Override
       protected void onDestroy() {
           super.onDestroy();
           recorder.onDestory();
       }
*  cmakeList.txt

            #1.cmake verson，指定cmake版本
            cmake_minimum_required(VERSION 3.4.1)

            # Creates and names a library, sets it as either STATIC
            # or SHARED, and provides the relative paths to its source code.
            # You can define multiple libraries, and CMake builds them for you.
            # Gradle automatically packages shared libraries with your APK.


            #2.project name，指定项目的名称，一般和项目的文件夹名称对应
            PROJECT(LaneMp3)

            #3.head file path，头文件目录
            INCLUDE_DIRECTORIES(
            src/main/cpp/include
            )

            #4.source directory，源文件目录
            AUX_SOURCE_DIRECTORY(src/main/cpp DIR_SRCS)

            #5.set environment variable，设置环境变量，编译用到的源文件全部都要放到这里，否则编译能够通过，但是执行的时候会出现各种问题，比如"symbol lookup error xxxxx , undefined symbol"
            SET(TEST_MATH
            ${DIR_SRCS}
            )
            add_library( # Sets the name of the library.
                         native_lame_mp3

                         # Sets the library as a shared library.
                         SHARED

                         # Provides a relative path to your source file(s).
                         ${DIR_SRCS} )

            #6.add executable file，添加要编译的可执行文件
            ADD_EXECUTABLE(${PROJECT_NAME} ${TEST_MATH})

            #7.add link library，添加可执行文件所需要的库，比如我们用到了libm.so（命名规则：lib+name+.so），就添加该库的名称
            target_link_libraries(${PROJECT_NAME} native_lame_mp3)
* 混淆配置

       -keep class com.zhang.lamemp3.jni.lameMp3

 * 常见问题

        FAILURE: Build failed with an exception.

         * What went wrong:
         Execution failed for task ':lib_lame:externalNativeBuildRelease'.
         > Build command failed.
           Error while executing process D:\android\cmake\3.6.3155560\bin\cmake.exe with arguments {--build F:\git\LameMp3Demo\lib_lame\.externalNativeBuild\cmake\release\x86 --target LaneMp3}
           [1/1] Linking C executable LaneMp3
           FAILED: cmd.exe /C "cd . && D:\android\ndk-bundle\toolchains\llvm\prebuilt\windows-x86_64\bin\clang.exe  --target=i686-none-linux-android --gcc-toolchain=D:/android/ndk-bundle/toolchains/x86-4.9/prebuilt/windows-x86_64 --sysroot=D:/android/ndk-bundle/sysroot -isystem D:/android/ndk-bundle/sysroot/usr
         /include/i686-linux-android -D__ANDROID_API__=17 -g -DANDROID -ffunction-sections -funwind-tables -fstack-protector-strong -no-canonical-prefixes -mstackrealign -Wa,--noexecstack -Wformat -Werror=format-security -DSTDC_HEADERS -O2 -DNDEBUG  -Wl,--exclude-libs,libgcc.a --sysroot D:/android/ndk-bundle/pl
         atforms/android-17/arch-x86 -Wl,--build-id -Wl,--warn-shared-textrel -Wl,--fatal-warnings -Wl,--no-undefined -Wl,-z,noexecstack -Qunused-arguments -Wl,-z,relro -Wl,-z,now -Wl,--gc-sections -Wl,-z,nocopyreloc -pie -fPIE CMakeFiles/LaneMp3.dir/src/main/cpp/bitstream.c.o CMakeFiles/LaneMp3.dir/src/main/cp
         p/encoder.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/fft.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/gain_analysis.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/id3tag.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/lame.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/mpglib_interface.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/native
         -lib.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/newmdct.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/presets.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/psymodel.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/quantize.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/quantize_pvt.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/reservoir.c
         .o CMakeFiles/LaneMp3.dir/src/main/cpp/set_get.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/tables.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/takehiro.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/util.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/vbrquantize.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/VbrTag.c.o CMakeFiles/L
         aneMp3.dir/src/main/cpp/version.c.o CMakeFiles/LaneMp3.dir/src/main/cpp/xmm_quantize_sub.c.o  -o LaneMp3  ../../../../build/intermediates/cmake/release/obj/x86/libnative_lame_mp3.so -lm && cd ."
           D:/android/ndk-bundle/platforms/android-17/arch-x86/usr/lib\crtbegin_dynamic.o:crtbegin.c:function _start: error: undefined reference to 'main'
           clang.exe: error: linker command failed with exit code 1 (use -v to see invocation)
           ninja: build stopped: subcommand failed.
