#include <jni.h>


#ifndef _Included_com_zhang_MP3Recorder
#define _Included_com_zhang_MP3Recorder
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_com_zhang_lamemp3_jni_LameMp3_init__IIIII(JNIEnv *env, jclass type, jint inSamplerate,
                                               jint outChannel, jint outSamplerate, jint outBitrate,
                                               jint quality);

JNIEXPORT jint JNICALL
Java_com_zhang_lamemp3_jni_LameMp3_encode(JNIEnv *env, jclass type, jshortArray buffer_l_,
                                          jshortArray buffer_r_, jint samples, jbyteArray mp3buf_);

JNIEXPORT jint JNICALL
Java_com_zhang_lamemp3_jni_LameMp3_flush(JNIEnv *env, jclass type, jbyteArray mp3buf_);

JNIEXPORT void JNICALL
Java_com_zhang_lamemp3_jni_LameMp3_close(JNIEnv *env, jclass type) ;
#ifdef __cplusplus
}
#endif
#endif