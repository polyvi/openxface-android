#ifndef UTILS_H
#define UTILS_H

#ifdef __cplusplus
extern "C" {
#endif
#include <jni.h>

#define MAX(a, b) ((a) > (b) ? (a) : (b))
#define MIN(a, b) ((a) > (b) ? (b) : (a))

#define MAX_PATH 256

void initJVM(JNIEnv *pEnv);

void setVMEnvironment(JNIEnv *pEnv);

JNIEnv* getVMEnvironment();

int ucsToMbs(const unsigned short *pUniStr, char *pMbsStr, int destLen);

int getUcsLen(const unsigned short * pUniStr);

unsigned short * getUniCharsOfJstring(JNIEnv *pEnv, jstring str);

#ifdef __cplusplus
}
#endif

#endif
