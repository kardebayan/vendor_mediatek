/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2017. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.internal.telephony.datasub;

import android.content.Context;
import android.content.Intent;

import android.telephony.PreciseCallState;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import android.text.TextUtils;

import com.mediatek.internal.telephony.datasub.SmartDataSwitchAssistant;
import com.mediatek.internal.telephony.MtkSubscriptionManager;

public class SmartDataSwitchAssistantOpExt implements ISmartDataSwitchAssistantOpExt {
    private static boolean DBG = true;
    private static String LOG_TAG = "SmartDataSwitchOpExt";
    private static Context mContext = null;

    private static SmartDataSwitchAssistant mSmartData = null;

    private static final int IDLE = 0;
    private static final int INCALL_SWITCH = 1;
    private static final int INCALL_NOT_SWITCH = 2;

    protected int mVoiceNetworkType = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;

    public SmartDataSwitchAssistantOpExt(Context context) {
        mContext = context;
    }

    @Override
    public void init(SmartDataSwitchAssistant smartDataSwitchAssistant) {
        logd("init()");
        mSmartData = smartDataSwitchAssistant;
    }

    @Override
    public void onCallStarted(int phoneId) {
        // register event
        mSmartData.regServiceStateChangedEvent();
    }

    @Override
    public void onCallEnded() {
        // de-register event
        mSmartData.unregServiceStateChangedEvent();
    }

    @Override
    public void onSubChanged() {}

    @Override
    public void onTemporaryDataSettingsChanged() {}

    @Override
    public void onSrvccStateChanged() {}

    @Override
    public void onServiceStateChanged(int phoneId) {
        int voiceNwType = mSmartData.getVoiceNetworkType(phoneId);
        int inCallPhoneId = mSmartData.getInCallPhoneId();
        SmartDataSwitchAssistant.SwitchState mState = mSmartData.getSwitchState();

        if (phoneId == inCallPhoneId) {
            if (isNetworkTypeChanged(voiceNwType)) {
                if (mState == SmartDataSwitchAssistant.SwitchState.INCALL_SWITCH) {
                    if (mSmartData.is2gNetworkType(voiceNwType)) {
                        // network type changed to 2G
                        mSmartData.setCallType(mSmartData.CALL_TYPE_CS_2G);
                        transitionTo(SmartDataSwitchAssistant.SwitchState.INCALL_NOT_SWITCH);
                    }
                } else if (mState == SmartDataSwitchAssistant.SwitchState.INCALL_NOT_SWITCH) {
                    // For OM, INCALL_NOT_SWITCH state means this call is 2G or WFC
                    mSmartData.updateCallType(inCallPhoneId);
                    transitionTo(SmartDataSwitchAssistant.SwitchState.INCALL_SWITCH);
                } else {
                    loge("onServiceStateChanged: Unhandled case!");
                }
            } else {
                logd("onServiceStateChanged: voice network type not change");
            }
        } else {
            //logd("onServiceStateChanged: not changed for in call phone id");
        }
    }

    @Override
    public void onHandoverToWifi() {}

    @Override
    public void onHandoverToCellular() {}

    @Override
    public void transitionTo(SmartDataSwitchAssistant.SwitchState newState) {
        int mStateCode = (mSmartData.getSwitchState()).getCode();
        int newStateCode = newState.getCode();
        logd("transitionTo E: mStateCode=" + mStateCode + " newStateCode=" + newStateCode);
        switch (mStateCode) {
            case IDLE:
                if (newStateCode == INCALL_SWITCH) {
                    if (mSmartData.switchDataService(mSmartData.getInCallPhoneId())) {
                        mSmartData.setSwitchState(
                                SmartDataSwitchAssistant.SwitchState.INCALL_SWITCH);
                        logd("transitionTo X: final state=INCALL_SWITCH");
                    } else {
                        mSmartData.setSwitchState(
                                SmartDataSwitchAssistant.SwitchState.INCALL_NOT_SWITCH);
                        logd("transitionTo X: final state=INCALL_NOT_SWITCH");
                    }
                } else {
                    logd("no need to handle this case");
                }
                break;
            case INCALL_SWITCH:
                if (newStateCode == INCALL_NOT_SWITCH) {
                    mSmartData.releaseNetworkRequest();
                    mSmartData.setSwitchState(
                            SmartDataSwitchAssistant.SwitchState.INCALL_NOT_SWITCH);
                    logd("transitionTo X: final state=INCALL_NOT_SWITCH");
                } else if (newStateCode == IDLE) {
                    // must release network
                    mSmartData.releaseNetworkRequest();
                    // reset network type
                    mVoiceNetworkType = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
                    // switch state
                    mSmartData.setSwitchState(SmartDataSwitchAssistant.SwitchState.IDLE);
                    logd("transitionTo X: final state=IDLE");
                } else {
                    logd("no need to handle this case");
                }
                break;
            case INCALL_NOT_SWITCH:
                if (newStateCode == INCALL_SWITCH) {
                    // check with all switch condictions
                    if (mSmartData.switchDataService(mSmartData.getInCallPhoneId())) {
                        mSmartData.setSwitchState(
                                SmartDataSwitchAssistant.SwitchState.INCALL_SWITCH);
                        logd("transitionTo X: final state=INCALL_SWITCH");
                    } else {
                        logd("no need to handle this case");
                    }
                } else if (newStateCode == IDLE) {
                    // reset network type
                    mVoiceNetworkType = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
                    // switch state
                    mSmartData.setSwitchState(SmartDataSwitchAssistant.SwitchState.IDLE);
                    logd("transitionTo X: final state=IDLE");
                } else {
                    logd("no need to handle this case");
                }
                break;
            default:
                logd("transitionTo X: no need to try this case!");
                break;
        }
    }

    @Override
    public boolean preCheckByCallStateExt(Intent intent, boolean result) {
        return result;
    }

    @Override
    public boolean isNeedSwitchCallType(int callType) {
        logd("isNeedSwitchCallType() callType=" + callType);
        if (callType != SmartDataSwitchAssistant.CALL_TYPE_UNKNOW) {
            if ((callType != SmartDataSwitchAssistant.CALL_TYPE_CS_2G) &&
                (callType != SmartDataSwitchAssistant.CALL_TYPE_CS_CDMA) &&
                (callType != SmartDataSwitchAssistant.CALL_TYPE_PS_WIFI)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSmartDataSwtichAllowed() {
        return true;
    }


    private boolean isNetworkTypeChanged(int newVoiceNwType) {
        boolean result = false;

        logd("isNetworkTypeChanged: mVoiceNetworkType=" + mVoiceNetworkType +
                " newVoiceNwType=" + newVoiceNwType);

        if (mVoiceNetworkType != newVoiceNwType) {
            mVoiceNetworkType = newVoiceNwType;
            result = true;
        }
        return result;
    }

    protected static void logv(String s) {
        if (DBG) {
            Rlog.v(LOG_TAG, s);
        }
    }

    protected static void logd(String s) {
        if (DBG) {
            Rlog.d(LOG_TAG, s);
        }
    }

    protected static void loge(String s) {
        if (DBG) {
            Rlog.e(LOG_TAG, s);
        }
    }

    protected static void logi(String s) {
        if (DBG) {
            Rlog.i(LOG_TAG, s);
        }
    }
}
