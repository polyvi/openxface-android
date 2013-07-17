#include <stdlib.h>
#include <string.h>
#include <wchar.h>
#include <pthread.h>
#include "utils.h"
#include "MemoryManager.h"

static JavaVM *pJVM = NULL;
static pthread_key_t key;
static pthread_once_t keyOnce = PTHREAD_ONCE_INIT;

void initJVM(JNIEnv *pEnv) {
	if (pJVM == NULL) {
		pEnv->GetJavaVM(&pJVM);
		setVMEnvironment(pEnv);
	}
}

static void makeKey() {
	pthread_key_create(&key, NULL);
}

void setVMEnvironment(JNIEnv *pEnv) {
	pthread_once(&keyOnce, makeKey);
	pthread_setspecific(key, pEnv);
}

JNIEnv* getVMEnvironment() {
	JNIEnv* pEnv = NULL;
	pEnv = (JNIEnv*) pthread_getspecific(key);
	if (pEnv == NULL) {
		pJVM->AttachCurrentThread(&pEnv, NULL);
		setVMEnvironment(pEnv);
	}

	return pEnv;
}

int ucsToWcs(const unsigned short *pUniStr, wchar_t *pWcharStr, int charCount) {
	if (pWcharStr == NULL || pUniStr == NULL) {
		return 0;
	}

	int ucsLen = getUcsLen(pUniStr);
	if (ucsLen == 0) {
		return 0;
	}

	int index = 0;
	for (; index < ucsLen; index++) {
		pWcharStr[index] = pUniStr[index];
	}

	pWcharStr[index] = 0;
	return index + 1;
}

int ucsToMbs(const unsigned short *pUniStr, char *pMbsStr, int destLen) {
	if ((pUniStr == NULL) || (pMbsStr == NULL)) {
		return 0;
	}

	size_t ucsLen = getUcsLen(pUniStr);
	if (ucsLen == 0) {
		return 0;
	}

	JNIEnv *pEnv = getVMEnvironment();
	jstring strObj = pEnv->NewString(pUniStr, ucsLen);
	int utfLen = pEnv->GetStringUTFLength(strObj);
	if (utfLen >= destLen) {
		pEnv->DeleteLocalRef(strObj);
		return 0;
	}

	pEnv->GetStringUTFRegion(strObj, 0, ucsLen, pMbsStr);
	pMbsStr[utfLen] = '\0';

	pEnv->DeleteLocalRef(strObj);
	return utfLen + 1;
}

int getUcsLen(const unsigned short * pUniStr) {
	int len = 0;

	if (pUniStr == NULL) {
		return 0;
	}

	while (*pUniStr != 0) {
		len++;
		pUniStr++;
	}
	return len;
}

unsigned short * getUniCharsOfJstring(JNIEnv *pEnv, jstring str) {
	if (str == NULL) {
		return NULL;
	}

	const int strLen = pEnv->GetStringLength(str);
	unsigned short *pUniChars = (unsigned short *) XF_MEM_malloc((strLen + 1)
			* sizeof(unsigned short));
	pEnv->GetStringRegion(str, 0, strLen, pUniChars);
	pUniChars[strLen] = '\0';
	return pUniChars;
}
