ifeq ($(TARGET_PROVIDES_MTK_PIE_IMS),true)

MTK_PATH := $(call my-dir)

# MTK Proprietary
include $(call all-makefiles-under, $(MTK_PATH))

endif # TARGET_PROVIDES_MTK_PIE_IMS

