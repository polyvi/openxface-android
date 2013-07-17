#include "com_polyvi_xface_util_XNativeZip.h"
#include "utils.h"
#include "UnZip.h"
#include "FileSystem.h"
#include <jni.h>
#include <stdlib.h>
#define XF_OK 0
#define XF_ERROR -1

static JavaVM *pJVM = NULL;

JNIEXPORT int JNICALL Java_com_polyvi_xface_util_XNativeZip_unZip
  (JNIEnv *env, jobject,jstring filePath,jstring destPath) {
	initJVM(env);
    unsigned short *path = NULL;
	unsigned short *dir = NULL;
    path = getUniCharsOfJstring(env, filePath);
	dir = getUniCharsOfJstring(env, destPath);
	return unzip(path,dir);
  }

 int unzip(const unsigned short* filePath,const unsigned short* fileDir) {
	UnZip *pPkgHandle = new UnZip(filePath);
	if(XF_FILE_mkdir(fileDir)== XF_ERROR) {
	     return XF_ERROR;
	}
	int filenum = pPkgHandle->getPkgItemNumber();

	for(int i = 0;i < filenum;i++)
	{
		unsigned short* filename = pPkgHandle->getFileNameByIndex(i);
		if(filename == NULL) {
		return XF_ERROR;
		}
		int fileNameLength = UCS2Strlen(filename);
		int dirLength =  UCS2Strlen((unsigned short*) fileDir);
		unsigned short lastchar = filename[fileNameLength-1];
		
		unsigned short* tempStr = new unsigned short[fileNameLength + dirLength + 1 - pPkgHandle->getDirLen()];
		memcpy(tempStr, fileDir, dirLength << 1);
		memcpy(tempStr+dirLength, filename + pPkgHandle->getDirLen(), ((fileNameLength - pPkgHandle->getDirLen()) <<1) + 2);
		if (lastchar != '/') {
		    pPkgHandle->unpackFileToPath(tempStr, i);
		} else {
			if(XF_FILE_mkdir(tempStr) == XF_ERROR) {
			return XF_ERROR;
			}
		}
		delete tempStr;
	}
	delete pPkgHandle;
	return XF_OK;
}

int UCS2Strlen(unsigned short *unicodeStr)
{
	unsigned short* temp = unicodeStr;
	while(*temp++);
	return temp-unicodeStr-1;
}
