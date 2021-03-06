/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.ims.ril;

import android.os.Message;
import android.os.Handler;
import android.telephony.Rlog;

/**
 * Default Implementation of OpImsCommandsInterface
 *
 */
public class DefaultOpImsRIL implements OpImsCommandsInterface {

    DefaultOpImsRIL(int slotId) {
        // TODO
    }

    @Override
    public void setRttMode(int mode, Message response) {
        // Nothing
    }

    @Override
    public void sendRttModifyRequest(int callId, int newMode,
            Message response) {
        // Nothing
    }

    @Override
    public void sendRttText(int callId, String text, int length, Message response) {
        // Nothing
    }

    @Override
    public void setRttModifyRequestResponse(int callId, int result,
                                            Message response) {
       // Nothing
    }

    @Override
    public void registerForGttCapabilityIndicator(Handler h, int what,
                                                  Object obj) {
        // Nothing
    }

    @Override
    public void unregisterForGttCapabilityIndicator(Handler h) {
        // Nothing
    }

    @Override
    public void registerForRttModifyResponse(Handler h, int what, Object obj) {
        // Nothing
    }

    @Override
    public void unregisterForRttModifyResponse(Handler h) {
        // Nothing
    }

    @Override
    public void registerForRttTextReceive(Handler h, int what, Object obj) {
        // Nothing
    }

    @Override
    public void unregisterForRttTextReceive(Handler h) {
        // Nothing
    }

    @Override
    public void registerForRttModifyRequestReceive(Handler h, int what,
                                                   Object obj) {
        // Nothing
    }

    @Override
    public void unregisterForRttModifyRequestReceive(Handler h) {
        // Nothing
    }

    @Override
    public void dialFrom(String address, String fromAddress, int clirMode, boolean isVideoCall,
                         Message result) {
        // Nothing
    }

    @Override
    public void sendUssiFrom(String from, int action, String ussi, Message response) {
        // Nothing
    }

    @Override
    public void cancelUssiFrom(String from, Message response) {
        // Nothing
    }

    @Override
    public void setEmergencyCallConfig(int category, boolean isForceEcc, Message response) {
        // Nothing
    }

    @Override
    public void deviceSwitch(String number, String deviceId, Message response) {
        // Nothing
    }

    @Override
    public void cancelDeviceSwitch(Message response) {
        // Nothing
    }

    @Override
    public void enableRttAudioIndication(int callId, int enable, Message response) {
        // Nothing
    }

    @Override
    public void registerForAudioIndication(Handler h, int what, Object obj) {
        // Nothing
    }

    @Override
    public void unregisterForAudioIndication(Handler h) {
        // Nothing
    }
}
