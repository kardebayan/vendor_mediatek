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

package com.mediatek.internal.telephony;

import android.content.Context;
import android.os.ServiceManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;

import com.mediatek.internal.telephony.IMtkTelephonyRegistryEx;

/**
 * Implementation of the IMtkTelephonyRegistryEx interface.
 */
public class MtkTelephonyRegistryEx extends IMtkTelephonyRegistryEx.Stub {
    private static final String LOG_TAG = "MtkTelephonyRegistryEx";
    private static final boolean DBG = false; // STOPSHIP if true
    private static final boolean DBG_LOC = false; // STOPSHIP if true
    private static final boolean VDBG = false; // STOPSHIP if true

    private static MtkTelephonyRegistryEx sInstance;

    static MtkTelephonyRegistryEx init() {
        synchronized (MtkTelephonyRegistryEx.class) {
            if (sInstance == null) {
                sInstance = new MtkTelephonyRegistryEx();
            } else {
                Rlog.e(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    protected MtkTelephonyRegistryEx() {
        publish();
    }

    private void publish() {
        Rlog.d(LOG_TAG, "publish: " + this);

        ServiceManager.addService("telephony.mtkregistry", this);
    }

    /// M: CC: Notify Call state with phoneType @{
    public void notifyCallStateForPhoneInfo(int phoneType, int phoneId, int subId, int state,
                String incomingNumber) {
        // TODO: porting required.
    }
    /// @}

    private static boolean idMatchEx(int rSubId, int subId, int dSubId, int rPhoneId,
            int phoneId) {
        Rlog.d(LOG_TAG, "idMatchEx: rSubId=" + rSubId + ", subId=" + subId + ", dSubId=" + dSubId
                + ", rPhoneId=" + rPhoneId + ", phoneId=" + phoneId);

        if(subId < 0) {
            // Invalid case, we need compare phoneId with default one.
            return (rPhoneId == phoneId);
        }
        if(rSubId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
            return (subId == dSubId);
        } else {
            return (rSubId == subId);
        }
    }
}