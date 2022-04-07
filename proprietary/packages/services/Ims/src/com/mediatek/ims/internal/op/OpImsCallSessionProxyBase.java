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

package com.mediatek.ims.internal.op;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import android.content.Intent;
import android.telephony.ims.ImsCallProfile;

import android.telephony.ims.ImsCallSessionListener;
import com.android.ims.internal.IImsCallSession;

import com.mediatek.ims.ImsCallSessionProxy;
import com.mediatek.ims.internal.IMtkImsCallSessionListener;
import com.mediatek.ims.MtkImsCallSessionProxy;
import com.mediatek.ims.internal.IMtkImsCallSession;
import com.mediatek.ims.ril.ImsCommandsInterface;

import com.mediatek.ims.ril.ImsRILAdapter;
import com.mediatek.ims.ImsCallSessionProxy;

public class OpImsCallSessionProxyBase implements OpImsCallSessionProxy {

    // Tag
    private static final String TAG = "OpImsCallSessionProxyBase";

    protected void printDefaultLog(String funcName) {
        Rlog.d(TAG, funcName + " call to OP base");
    }

    @Override
    public void handleGttCapabilityIndication(AsyncResult ar,
                                              IMtkImsCallSession proxy,
                                              String callId,
                                              int callState) {
        printDefaultLog("handleGttCapabilityIndication");
    }

    @Override
    public void handleRttECCRedialEvent(IMtkImsCallSession proxy) {
        printDefaultLog("handleRttECCRedialEvent");

    }

    @Override
    public void handleRttTextReceive(AsyncResult ar, String callId,
                                     ImsCallSessionListener listener) {
        printDefaultLog("handleRttTextReceive");
    }

    @Override
    public void handleRttModifyResponse(AsyncResult ar, String callId,
                                        ImsCallSessionListener listener) {
        printDefaultLog("handleRttModifyResponse");
    }

    public void handleRttModifyRequestReceive(AsyncResult ar, IImsCallSession proxy,
                                              String callId, ImsCallSessionListener listener,
                                              ImsCommandsInterface imsRILAdapter,
                                              boolean wasVideoCall) {
        printDefaultLog("handleRttModifyRequestReceive");
    }

    @Override
    public void notifyTextCapabilityChanged(IMtkImsCallSessionListener mtkListener,
                                            IMtkImsCallSession mtkImsCallSessionProxy,
                                            int localCapability, int remoteCapability,
                                            int localTextStatus, int realRemoteCapability) {
        printDefaultLog("notifyTextCapabilityChanged");

    }
    @Override
    public void notifyRttECCRedialEvent(IMtkImsCallSessionListener mtkListener,
                                            IMtkImsCallSession mtkImsCallSessionProxy) {
        printDefaultLog("notifyRttECCRedialEvent");
    }

    @Override
    public void sendRttMessage(String callIdString,
                               ImsCommandsInterface ci,
                               String rttMessage) {
        printDefaultLog("sendRttMessage");
    }

    @Override
    public void sendRttModifyRequest(String callIdString, ImsCommandsInterface ci,
                                     ImsCallProfile to) {
        printDefaultLog("sendRttModifyRequest");
    }

    @Override
    public void sendRttModifyResponse(String callIdString, ImsCommandsInterface ci,
                                      boolean response) {
        printDefaultLog("sendRttModifyResponse");
    }

    @Override
    public void setRttModeForDial(String callIdString,
                                  ImsCommandsInterface ci, boolean isRtt) {
        printDefaultLog("setRttModeForDial + isRtt: " + isRtt);
    }

    @Override
    public void checkIncomingRttCallType(Intent intent) {
        printDefaultLog("checkIncomingRttCallType");
    }

    @Override
    public boolean isRttEnabledForCallSession() {
        printDefaultLog("isRttEnabledForCallSession");
        return false;
    }

    @Override
    public void handleRttSpeechEvent(IMtkImsCallSession proxy, int speech) {
        printDefaultLog("handleRttSpeechEvent");

    }

    @Override
    public void enableRttAudioIndication(String callIdString,
            ImsCommandsInterface imsRILAdapter) {
        printDefaultLog("enableRttAudioIndication");
    }

    @Override
    public void notifyRttSpeechEvent(IMtkImsCallSessionListener mtkListener,
            IMtkImsCallSession mtkImsCallSessionProxy, int speech) {
        printDefaultLog("notifyRttSpeechEvent");
    }
}
