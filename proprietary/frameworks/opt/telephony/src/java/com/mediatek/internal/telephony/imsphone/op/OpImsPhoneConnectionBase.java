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

import android.telephony.Rlog;

import java.util.Set;

import com.android.internal.telephony.Connection.Listener;
import com.mediatek.internal.telephony.imsphone.MtkImsPhoneConnection;

import com.android.internal.telephony.imsphone.ImsRttTextHandler;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;

import com.mediatek.ims.MtkImsCall;
import com.android.ims.ImsCall;

public class OpImsPhoneConnectionBase implements OpImsPhoneConnection {
    private static final String TAG = "OpImsPhoneConnectionBase";

    @Override
    public void updateTextCapability(Set<Listener> listeners,
            int localCapability, int remoteCapability,
            int localTextStatus, int realRemoteTextCapability) {
        printDefaultLog("updateTextCapability");
    }

    void printDefaultLog(String funcName) {
        Rlog.d(TAG, funcName + " call to op base");
    }
    public void stopRttTextProcessing(ImsRttTextHandler rttTextHandler,
        ImsPhoneConnection imsPhoneConnection) {
        printDefaultLog("stopRttTextProcessing");
    }
    public void sendRttDowngradeRequest(MtkImsCall imsCall,
        ImsRttTextHandler rttTextHandler, ImsPhoneConnection imsPhoneConnection) {
        printDefaultLog("sendRttDowngradeRequest");
    }
    public void setRttIncomingCall(boolean isIncomingRtt) {
        printDefaultLog("setRttIncomingCall");
    }
    public boolean isIncomingRtt() {
        return false;
    }
    public void setIncomingRttDuringEmcGuard(boolean isIncomingDuringRttGuard) {
        printDefaultLog("setIncomingRttDuringEmcGuard");
    }
    public boolean isIncomingRttDuringEmcGuard() {
        printDefaultLog("isIncomingRttDuringEmcGuard");
        return false;
    }

    @Override
    public void setImsCall(ImsCall imsCall) {
    }

    @Override
    public boolean onSendRttModifyRequest(ImsPhoneConnection conn) {
        return false;
    }
}
