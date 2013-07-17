#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <android/log.h>
#include "XNativeBridge.h"

char * getUtfCharsOfJstring(JNIEnv *pEnv, jstring str) {
    const int strLen = pEnv->GetStringUTFLength(str);
    char *pUtfChars = (char *) malloc(strLen + 1);
    pEnv->GetStringUTFRegion(str, 0, strLen, pUtfChars);
    pUtfChars[strLen] = '\0';
    return pUtfChars;
}

JNIEXPORT void JNICALL Java_com_polyvi_xface_util_XNativeBridge_chmod(
        JNIEnv *pEnv, jclass, jstring path, jint mode) {
    char *pFilePath = getUtfCharsOfJstring(pEnv, path);
    chmod(pFilePath, mode);
    free(pFilePath);
}
