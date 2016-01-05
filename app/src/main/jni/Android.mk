LOCAL_PATH := $(call my-dir)

#--------caffe_opencv-----------------------------------------------
include $(CLEAR_VARS)
LOCAL_MODULE    := libcaffe
LOCAL_SRC_FILES := prebuild/caffe_opencv/$(TARGET_ARCH_ABI)/libcaffe.so
include $(PREBUILT_SHARED_LIBRARY)
#--------------------------------------------------------------------

include $(CLEAR_VARS)

LOCAL_MODULE    := FaceBeauty
LOCAL_SRC_FILES := FaceBeauty.cpp 

LOCAL_CFLAGS += -DUNICODE
LOCAL_CPPFLAGS += -fexceptions -frtti -std=c++11 -DUSE_EIGEN -DCPU_ONLY

LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include/eigen3
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include/google_inc
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include/opencv_inc
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include/our_inc

LOCAL_LDLIBS += -lm -llog

LOCAL_ARM_MODE := arm
ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
LOCAL_ARM_NEON := true
endif

LOCAL_SHARED_LIBRARIES := caffe
    
include $(BUILD_SHARED_LIBRARY)


