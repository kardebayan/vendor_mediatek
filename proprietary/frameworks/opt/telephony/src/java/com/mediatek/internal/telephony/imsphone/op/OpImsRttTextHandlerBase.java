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

import android.os.Message;
import android.telephony.Rlog;

import com.android.internal.telephony.imsphone.ImsRttTextHandler;
import com.mediatek.ims.MtkImsCall;

public class OpImsRttTextHandlerBase implements OpImsRttTextHandler {
    private static final String TAG = "OpImsRttTextHandlerBase";

    void printDefaultLog(String funcName) {
        Rlog.d(TAG, funcName + " call to op base");
    }

    public void appendToNetworkBuffer(Message msg, ImsRttTextHandler imsRttTextHandler) {
// FIXME
//        printDefaultLog("appendToNetworkBuffer");
        // First, append the text-to-send to the string buffer
//        imsRttTextHandler.mBufferedTextToNetwork.append((String) msg.obj);
        // Check to see how many codepoints we have buffered. If we have more than 5,
        // send immediately, otherwise, wait until a timeout happens.
/*
        int numCodepointsBuffered = imsRttTextHandler.mBufferedTextToNetwork
                .codePointCount(0, imsRttTextHandler.mBufferedTextToNetwork.length());
        if (numCodepointsBuffered >= imsRttTextHandler.MAX_BUFFERED_CHARACTER_COUNT) {
            imsRttTextHandler.sendMessageAtFrontOfQueue(
                imsRttTextHandler.obtainMessage(imsRttTextHandler.ATTEMPT_SEND_TO_NETWORK));
        } else {
            imsRttTextHandler.sendEmptyMessageDelayed(
                    imsRttTextHandler.ATTEMPT_SEND_TO_NETWORK,
                    imsRttTextHandler.MAX_BUFFERING_DELAY_MILLIS);
        }
*/
    }
    public void attemptSendToNetwork(Message msg, ImsRttTextHandler imsRttTextHandler) {
// FIXME
//        printDefaultLog("attemptSendToNetwork");
        // Check to see how many codepoints we can send, and send that many.
/*
        int numCodePointsAvailableInBuffer =
            imsRttTextHandler.mBufferedTextToNetwork.codePointCount(0,
            imsRttTextHandler.mBufferedTextToNetwork.length());
        int numCodePointsSent = Math.min(numCodePointsAvailableInBuffer,
                imsRttTextHandler.mCodepointsAvailableForTransmission);
        if (numCodePointsSent == 0) {
            return;
        }
        int endSendIndex = imsRttTextHandler.mBufferedTextToNetwork.offsetByCodePoints(0,
                numCodePointsSent);

        String stringToSend = imsRttTextHandler.mBufferedTextToNetwork.substring(0,
            endSendIndex);

        imsRttTextHandler.mBufferedTextToNetwork.delete(0, endSendIndex);
        imsRttTextHandler.mNetworkWriter.write(stringToSend);
        imsRttTextHandler.mCodepointsAvailableForTransmission -= numCodePointsSent;
        imsRttTextHandler.sendMessageDelayed(
                imsRttTextHandler.obtainMessage(
                imsRttTextHandler.EXPIRE_SENT_CODEPOINT_COUNT, numCodePointsSent, 0),
                imsRttTextHandler.MILLIS_PER_SECOND);
*/
    }
}

