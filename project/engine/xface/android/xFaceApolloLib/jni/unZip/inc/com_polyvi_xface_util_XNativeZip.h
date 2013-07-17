#include <jni.h>
#ifndef _Included_com_polyvi_xface_util_XNativeZip
#define _Included_com_polyvi_xface_util_XNativeZip
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT int JNICALL Java_com_polyvi_xface_util_XNativeZip_unZip
  (JNIEnv *, jobject,jstring,jstring);

int
unzip(const unsigned short* filePath,const unsigned short* fileDir);

int UCS2Strlen(unsigned short *unicodeStr);
#ifdef __cplusplus
}
#endif
#endif
