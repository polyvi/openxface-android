LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := jniNativeBridge

LOCAL_SRC_FILES := nativeBridge/XNativeBridge.cpp

include $(BUILD_SHARED_LIBRARY)



include $(CLEAR_VARS)

LOCAL_MODULE := jniunZip

LOCAL_C_INCLUDES := $(LOCAL_PATH)/unZip/inc

LOCAL_SRC_FILES := unZip/UnZip.cpp \
			    unZip/LiteUnzip.c\
			    unZip/utils.cpp \
                unZip/MemoryManager.cpp \
		        unZip/FileSystem.cpp \
                unZip/com_polyvi_xface_util_XNativeZip.cpp \

include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)

LOCAL_MODULE:=SecurityKey

LOCAL_SRC_FILES := SecurityKey/com_polyvi_xface_util_XSecurityUtils.cpp

include $(BUILD_SHARED_LIBRARY)