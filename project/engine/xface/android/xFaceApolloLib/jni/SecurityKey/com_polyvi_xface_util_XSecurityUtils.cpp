#include <jni.h>
#include "com_polyvi_xface_util_XSecurityUtils.h"
#include <stdlib.h>
#include <string.h>

/**
*@param pSeed 产生密钥的种子，目前调用是用的固定的字符串
*@return 生成的密钥
**/
char* generateKey(char *pSeed)
  {
    int size = strlen(pSeed) + 1;
    char* key = (char *) malloc(size);
    if(!key)
    {
        return NULL;
    }
    memset(key,0,size);
    unsigned int keyNumber = 0;
    for(int i = 0; i < size-1;i++)
    {
        keyNumber = ( pSeed[i] * 8 + 5) % 26 + 'a';
        key[i] = keyNumber;
    }
    return key;
  }
 
JNIEXPORT jstring JNICALL Java_com_polyvi_xface_util_XKeyGenerator_generateKey(JNIEnv *pEnv, jclass)
  {
        //TODO:生成密钥的种子可能由服务器或者配置文件指定
        char *pSeed = "10csa6kldhvpulfqs7b";
        char * key = generateKey(pSeed);
        if(NULL == key)
        {
            return NULL;
        }
        jstring keyStr = pEnv->NewStringUTF(key);
        free(key);
        return keyStr;
  }