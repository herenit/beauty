#ifndef _FACEBEAUTY_H_
#define _FACEBEAUTY_H_

#include <jni.h>
#include <android/log.h>


#define LOG_TAG "facebeatuy"
#define msg_Dbg(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define msg_Err(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define NAME1(CLZ, FUN) Java_##CLZ##_##FUN
#define NAME2(CLZ, FUN) NAME1(CLZ, FUN)
#define NAME(FUN) NAME2(CLASS, FUN)

#define CLASS com_phhc_beauty_JNILib

#endif //_FACEBEAUTY_H_
