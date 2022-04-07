build_vilte =
ifeq ($(strip $(MTK_VILTE_SUPPORT)),yes)
    build_vilte = yes
endif

ifeq ($(strip $(MTK_VIWIFI_SUPPORT)),yes)
    build_vilte = yes
endif

ifdef build_vilte

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

PROCESS_SRC_PATH:=src
PROCESS_INC_PATH:=$(LOCAL_PATH)/inc

LOCAL_SRC_FILES:= \
    $(PROCESS_SRC_PATH)/main_vtservice.cpp

LOCAL_SHARED_LIBRARIES:= \
    libbinder \
    libutils \
    libcutils \
    liblog

LOCAL_C_INCLUDES := \
    $(TOP)/$(MTK_PATH_SOURCE)/frameworks/base/include\
    $(TOP)/system/core/include/private\
    $(PROCESS_INC_PATH)

LOCAL_SHARED_LIBRARIES += libmtk_vt_service

LOCAL_MODULE:= vtservice
LOCAL_INIT_RC := init.vtservice.rc

include $(MTK_EXECUTABLE)

############################################################

include $(CLEAR_VARS)

PROCESS_SRC_PATH:=src
PROCESS_INC_PATH:=$(LOCAL_PATH)/inc

HAL_INC_PATH:=vendor/mediatek/proprietary/frameworks/opt/vtservice/hal/inc

LOCAL_SRC_FILES:= \
	$(PROCESS_SRC_PATH)/main_vtservice_hidl.cpp 

LOCAL_C_INCLUDES := \
    $(HAL_INC_PATH) \
    $(SKTBIND_PATH) \
    $(PROCESS_INC_PATH)

LOCAL_HEADER_LIBRARIES := libbinder_headers

LOCAL_SHARED_LIBRARIES:= \
  libutils \
  libcutils \
  liblog \
  libbase \
  libfmq \
  vendor.mediatek.hardware.videotelephony@1.0 \
  libhardware \
  libhwbinder \
  libhidlbase \
  libhidltransport

LOCAL_MODULE:= vtservice_hidl
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_OWNER := mtk
LOCAL_MODULE_RELATIVE_PATH := hw
LOCAL_INIT_RC := init.vtservice.rc

include $(MTK_EXECUTABLE)

endif
