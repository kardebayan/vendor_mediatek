/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2017. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.internal.telephony.imsphone.op;

import android.content.Intent;
import android.util.Log;
import android.os.Bundle;
import android.telephony.ims.ImsCallProfile;

import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCall;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.internal.telephony.Call;
import com.android.ims.ImsCall;

public class OpCommonImsPhoneCallTrackerBase implements OpCommonImsPhoneCallTracker {
    private static final String TAG = "OpImsPhoneCallTrackerBase";

    @Override
    public void onTextCapabilityChanged(ImsPhoneConnection conn,
            int localCapability, int remoteCapability,
            int localTextStatus, int realRemoteCapability) {
        printDefaultLog("onTextCapabilityChanged");
    }

    @Override
    public void onRttEventReceived(ImsPhoneConnection conn, int event) {
        printDefaultLog("onRttEventReceived");
    }

    void printDefaultLog(String funcName) {
        Log.d(TAG, funcName + " call to op base");
    }

    public void initRtt(ImsPhone imsPhone) {
        printDefaultLog("initRtt");
    }

    public void disposeRtt(ImsPhone imsPhone, ImsPhoneCall foregroundCall,
        Call.SrvccState srvccState) {
        printDefaultLog("disposeRtt");
    }

    public void stopRttEmcGuardTimer() {
        printDefaultLog("stopRttEmcGuardTimer");
    }

    public void checkRttCallType(ImsPhone imsPhone, ImsPhoneCall foregroundCall,
        Call.SrvccState srvccState) {
        printDefaultLog("checkRttCallType");
    }

    public void setRttMode(Bundle intentExtras, ImsCallProfile profile) {
        printDefaultLog("setRttMode");
    }

    public void sendRttSrvccOrCsfbEvent(ImsPhoneCall call) {
        printDefaultLog("sendRttSrvccOrCsfbEvent");
    }

    public void checkIncomingCallInRttEmcGuardTime(ImsPhoneConnection conn) {
        printDefaultLog("checkIncomingCallInRttEmcGuardTime");
    }

    public void processRttModifyFailCase(ImsCall imsCall, int status,
        ImsPhoneConnection conn){
        printDefaultLog("processRttModifyFailCase");
    }

    public void processRttModifySuccessCase(ImsCall imsCall, int status,
        ImsPhoneConnection conn){
        printDefaultLog("processRttModifySuccessCase");
    }

    public void startRttEmcGuardTimer(ImsPhone imsPhone) {
        printDefaultLog("startRttEmcGuardTimer");
    }

    public boolean isRttCallInvolved(ImsCall fgImsCall, ImsCall bgImsCall) {
        printDefaultLog("isRttCallInvolved");
        return false;
    }

    public boolean isAllowMergeRttCallToVoiceOnly() {
        printDefaultLog("isAllowMergeRttCallToVoiceOnly");
        return true;
    }
}
