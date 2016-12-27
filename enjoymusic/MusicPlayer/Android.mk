
LOCAL_PATH:= $(call my-dir)

#include $(LOCAL_PATH)/jni/player/Android.mk


include $(CLEAR_VARS)

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 android-support-v13 jsr305

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := EnjoyHifiPlayer
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := false
LOCAL_JNI_SHARED_LIBRARIES := libffmpeg_mediaplayer_jni libswresample libavcodec libavformat libavutil libcutils libutils 

LOCAL_PROGUARD_ENABLED := disabled 
# := proguard.flagLOCAL_PROGUARD_FLAG_FILESs

LOCAL_AAPT_FLAGS += -c zz_ZZ

include $(BUILD_PACKAGE)

include $(call all-makefiles-under, jni)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
