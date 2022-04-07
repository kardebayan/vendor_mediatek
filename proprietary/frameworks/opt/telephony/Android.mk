# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include $(LOCAL_PATH)/hidl.mk

LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/src/java frameworks/opt/telephony/src/java
LOCAL_SRC_FILES := $(call all-java-files-under, src/java) \
        $(call all-Iaidl-files-under, src/java) \
        $(call all-logtags-files-under, src/java)

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := telephony-common mediatek-framework mediatek-telephony-base
LOCAL_JAVA_LIBRARIES += mediatek-common ims-common mediatek-telecom-common mediatek-ims-common
LOCAL_JAVA_LIBRARIES += services

LOCAL_STATIC_JAVA_LIBRARIES += wfo-common
LOCAL_MODULE := mediatek-telephony-common

include $(BUILD_JAVA_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := mediatek-ims-fundament-telephony

LOCAL_SRC_FILES += \
    src/java/com/mediatek/internal/telephony/MtkCallFailCause.java \
    src/java/com/mediatek/internal/telephony/RadioCapabilitySwitchUtil.java \
    src/java/com/mediatek/internal/telephony/ratconfiguration/RatConfiguration.java \
    src/java/com/mediatek/internal/telephony/MtkCallForwardInfo.java \

LOCAL_MODULE_TAGS := optional
LOCAL_JAVA_LIBRARIES := framework telephony-common mediatek-telephony-base ims-common

include $(BUILD_STATIC_JAVA_LIBRARY)

