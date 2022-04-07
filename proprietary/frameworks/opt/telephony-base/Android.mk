LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_AIDL_INCLUDES += $(LOCAL_PATH)/java
LOCAL_AIDL_INCLUDES += frameworks/native/aidl/gui

LOCAL_SRC_FILES := $(call all-java-files-under, java/)
LOCAL_SRC_FILES += \
    java/com/mediatek/internal/telephony/IMtkTelephonyEx.aidl \
    java/com/mediatek/internal/telephony/IMtkPhoneSubInfoEx.aidl \
    java/com/mediatek/internal/telephony/IMtkSms.aidl \
    java/com/mediatek/internal/telephony/IMtkSub.aidl \
    java/com/mediatek/ims/internal/IMtkImsConfig.aidl \
    java/com/mediatek/ims/internal/IMtkImsService.aidl \
    java/com/mediatek/ims/internal/IMtkImsCallSession.aidl \
    java/com/mediatek/ims/internal/IMtkImsCallSessionListener.aidl \
    java/com/mediatek/gba/IGbaService.aidl \
    java/com/mediatek/dm/IDmService.aidl \
    java/com/mediatek/ims/internal/IMtkImsUt.aidl \
    java/com/mediatek/ims/internal/IMtkImsUtListener.aidl \


LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := mediatek-common framework mediatek-framework

LOCAL_MODULE := mediatek-telephony-base

include $(BUILD_JAVA_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := mediatek-ims-fundament-telephony-base

LOCAL_SRC_FILES += \
    java/com/mediatek/ims/MtkImsCallForwardInfo.java \
    java/com/mediatek/gba/NafSessionKey.java \
    java/com/mediatek/ims/internal/IMtkImsConfig.aidl \
    java/com/mediatek/ims/internal/IMtkImsUt.aidl \
    java/com/mediatek/ims/internal/IMtkImsUtListener.aidl \
    java/com/mediatek/ims/internal/IMtkImsService.aidl \
    java/com/mediatek/ims/internal/IMtkImsCallSession.aidl \
    java/com/mediatek/ims/internal/IMtkImsCallSessionListener.aidl \
    java/com/mediatek/ims/internal/IMtkImsUtListener.aidl \
    java/com/mediatek/internal/telephony/MtkPhoneConstants.java \
    java/com/mediatek/internal/telephony/MtkRILConstants.java \
    java/com/mediatek/internal/telephony/MtkTelephonyProperties.java \
    java/com/mediatek/ims/MtkImsReasonInfo.java \
    java/com/mediatek/ims/MtkImsConstants.java \

LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/java

LOCAL_MODULE_TAGS := optional
LOCAL_JAVA_LIBRARIES := framework

include $(BUILD_STATIC_JAVA_LIBRARY)


# Include subdirectory makefiles
# ============================================================
include $(call all-makefiles-under,$(LOCAL_PATH))

