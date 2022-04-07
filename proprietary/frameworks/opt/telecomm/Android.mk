
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/src/java
LOCAL_SRC_FILES := $(call all-java-files-under, src/java) \
        $(call all-Iaidl-files-under, src/java)

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := mediatek-telephony-base

LOCAL_MODULE := mediatek-telecom-common

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_JAVA_LIBRARY)

# Include subdirectory makefiles
# ============================================================
include $(call all-makefiles-under,$(LOCAL_PATH))
