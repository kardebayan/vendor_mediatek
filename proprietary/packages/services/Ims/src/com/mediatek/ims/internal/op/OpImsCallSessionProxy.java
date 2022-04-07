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
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Message;
import android.telephony.ims.ImsCallProfile;
import android.telephony.Rlog;

import com.mediatek.ims.ImsCallSessionProxy;
import com.mediatek.ims.internal.IMtkImsCallSessionListener;
import android.telephony.ims.ImsCallSessionListener;
import com.mediatek.ims.internal.IMtkImsCallSession;
import com.mediatek.ims.ril.ImsCommandsInterface;

import com.mediatek.ims.ril.ImsRILAdapter;
import com.mediatek.ims.ImsCallSessionProxy;
import com.android.ims.internal.IImsCallSession;

public interface OpImsCallSessionProxy {

    /**
     * Handle GTT Capapbility Indication
     * @param ar Async result
     * @param proxy MtkImsCallSessionProxy object
     * @param callid Call Id
     */
    void handleGttCapabilityIndication(AsyncResult ar,
                                       IMtkImsCallSession proxy,
                                       String callId,
                                       int callState);

    void handleRttECCRedialEvent(IMtkImsCallSession proxy);

    /**
     * Handle RTT text receive event
     * @param ar Async result
     * @param callid Call Id
     * @param listener ImsCallSessionListener
     */
    void handleRttTextReceive(AsyncResult ar, String callId,
                              ImsCallSessionListener listener);

    /**
     * Handle RTT modify response event
     * @param ar Async result
     * @param callid Call Id
     * @param listener ImsCallSessionListener
     */
    void handleRttModifyResponse(AsyncResult ar, String callId,
                                 ImsCallSessionListener listener);

    /**
     * Handle RTT modify request receive event
     * @param ar Async result
     * @param proxy ImsCallSessionProxy object
     * @param callid Call Id
     * @param listener ImsCallSessionListener
     */
    void handleRttModifyRequestReceive(AsyncResult ar, IImsCallSession proxy,
                                       String callId, ImsCallSessionListener listener,
                                       ImsCommandsInterface imsRILAdapter,
                                       boolean wasVideoCall);

    /**
     * Notify text capability changed event
     * @param listener MtkImsCallSessionListener
     * @param mtkImsCallSessionProxy MtkImsCallSessionProxy
     * @param localCapability Local Capability
     * @param remoteCapability Remote Capability
     * @param localTextStatus Local text status
     * @param realRemoteCapability Real remote capability
     * @param callid Call Id
     */
    void notifyTextCapabilityChanged(IMtkImsCallSessionListener mtkListener,
                                     IMtkImsCallSession mtkImsCallSessionProxy,
                                     int localCapability, int remoteCapability,
                                     int localTextStatus, int realRemoteCapability);

    void notifyRttECCRedialEvent(IMtkImsCallSessionListener mtkListener,
                                     IMtkImsCallSession mtkImsCallSessionProxy);


    /**
     * Send RTT message
     * @param callIdString Call id string
     * @param ci RIL commands interface
     * @param rttMessage RTT message
     */
    void sendRttMessage(String callIdString, ImsCommandsInterface ci,
                        String rttMessage);

    /**
     * Send RTT modify request
     * @param callIdString Call id string
     * @param ci RIL commands interface
     * @param to Destination
     */
    void sendRttModifyRequest(String callIdString, ImsCommandsInterface ci,
                              ImsCallProfile to);

    /**
     * Send RTT modify response
     * @param callIdString Call id string
     * @param ci RIL commands interface
     * @param response Is response
     */
    void sendRttModifyResponse(String callIdString, ImsCommandsInterface ci,
                               boolean response);

    /**
     * Set RTT mode for dial
     * @param callIdString Call id string
     * @param ci RIL commands interface
     * @param isRtt Is RTT
     */
    void setRttModeForDial(String callIdString, ImsCommandsInterface ci,
                           boolean isRtt);

    /**
     * Set RTT mode for incoming call
     * @param intent intent
     */
    void checkIncomingRttCallType(Intent intent);

    /**
     * Is current callsession RTT enabled or not.
     * @return mIsRttEnabledForCallSession
     */
    public boolean isRttEnabledForCallSession();

    /**
     * enable RTT audio indication
     * @param callId call id
     * @param ci RIL commands interface
     */
    void enableRttAudioIndication(String callId, ImsCommandsInterface ci);

    /**
     * handle remote audio capability indication
     * @param proxy
     * @param speech Is audio on
     */
    void handleRttSpeechEvent(IMtkImsCallSession proxy, int speech);

    /**
     * notify remote audio capability indication
     * @param mtkListener
     * @param mtkImsCallSessionProxy
     * @param speech Is audio on
     */
    public void notifyRttSpeechEvent(IMtkImsCallSessionListener mtkListener,
            IMtkImsCallSession mtkImsCallSessionProxy, int speech);
}
