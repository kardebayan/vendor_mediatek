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

package com.mediatek.internal.telephony.dataconnection;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.telephony.Rlog;
import android.util.LocalLog;

import com.android.internal.telephony.dataconnection.ApnContext;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;

import com.mediatek.internal.telephony.MtkPhoneConstants;
import com.mediatek.internal.telephony.MtkDctConstants;

import java.util.ArrayList;

public class MtkApnContext extends ApnContext {
    private final static String SLOG_TAG = "MtkApnContext";

    // M: [OD over ePDG] start
    private ArrayList<ApnSetting> mWifiApns = null;
    // M: [OD over ePDG] end

    /**
      * To decrease the time of unused apn type.
      */
    private boolean mNeedNotify;

    /**
     * ApnContext constructor
     * @param phone phone object
     * @param apnType APN type (e.g. default, supl, mms, etc...)
     * @param logTag Tag for logging
     * @param config Network configuration
     * @param tracker Data call tracker
     */
    public MtkApnContext(Phone phone, String apnType, String logTag, NetworkConfig config,
            DcTracker tracker) {
        super(phone, apnType, logTag, config, tracker);
        mNeedNotify = needNotifyType(apnType);
    }

    // M: [OD over ePDG] start
    public synchronized void setWifiApns(ArrayList<ApnSetting> wifiApns) {
        mWifiApns = wifiApns;
    }

    public synchronized ArrayList<ApnSetting> getWifiApns() {
        return mWifiApns;
    }
    // M: [OD over ePDG] end

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mNeedNotify = true;
    }

    // reflection for ApnContext.apnIdForType
    private static int apnIdForTypeEx(int networkType) {
        switch (networkType) {
        case ConnectivityManager.TYPE_MOBILE_WAP:
            return MtkDctConstants.APN_WAP_ID;
        case ConnectivityManager.TYPE_MOBILE_XCAP:
            return MtkDctConstants.APN_XCAP_ID;
        case ConnectivityManager.TYPE_MOBILE_RCS:
            return MtkDctConstants.APN_RCS_ID;
        case ConnectivityManager.TYPE_MOBILE_BIP:
            return MtkDctConstants.APN_BIP_ID;
        case ConnectivityManager.TYPE_MOBILE_VSIM:
            return MtkDctConstants.APN_VSIM_ID;
        case ConnectivityManager.TYPE_MOBILE_PREEMPT:
            return MtkDctConstants.APN_PREEMPT_ID;
        default:
            return DctConstants.APN_INVALID_ID;
        }
    }

    // reflection for ApnContext.apnIdForNetworkRequest
    private static Bundle apnIdForNetworkRequestEx(NetworkCapabilities nc, int apnId,
            boolean error) {
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_EIMS)) {
            if (apnId != MtkDctConstants.APN_INVALID_ID) error = true;
            // M : Google issue that use wrong apnId for EIMS capability.
            apnId = MtkDctConstants.APN_EMERGENCY_ID;
        }
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_WAP)) {
            if (apnId != MtkDctConstants.APN_INVALID_ID) error = true;
            apnId = MtkDctConstants.APN_WAP_ID;
        }
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_XCAP)) {
            if (apnId != MtkDctConstants.APN_INVALID_ID) error = true;
            apnId = MtkDctConstants.APN_XCAP_ID;
        }
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_RCS)) {
            if (apnId != MtkDctConstants.APN_INVALID_ID) error = true;
            apnId = MtkDctConstants.APN_RCS_ID;
        }
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_BIP)) {
            if (apnId != MtkDctConstants.APN_INVALID_ID) error = true;
            apnId = MtkDctConstants.APN_BIP_ID;
        }
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VSIM)) {
            if (apnId != MtkDctConstants.APN_INVALID_ID) error = true;
            apnId = MtkDctConstants.APN_VSIM_ID;
        }
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_PREEMPT)) {
            if (apnId != MtkDctConstants.APN_INVALID_ID) error = true;
            apnId = MtkDctConstants.APN_PREEMPT_ID;
        }

        Bundle b = new Bundle();
        b.putInt("apnId", apnId);
        b.putBoolean("error", error);
        return b;
    }

    // reflection for ApnContext.apnIdForApnName
    private static int apnIdForApnNameEx(String type) {
        switch (type) {
            case MtkPhoneConstants.APN_TYPE_WAP:
                return MtkDctConstants.APN_WAP_ID;
            case MtkPhoneConstants.APN_TYPE_XCAP:
                return MtkDctConstants.APN_XCAP_ID;
            case MtkPhoneConstants.APN_TYPE_RCS:
                return MtkDctConstants.APN_RCS_ID;
            case MtkPhoneConstants.APN_TYPE_BIP:
                return MtkDctConstants.APN_BIP_ID;
            case MtkPhoneConstants.APN_TYPE_VSIM:
                return MtkDctConstants.APN_VSIM_ID;
            case MtkPhoneConstants.APN_TYPE_PREEMPT:
                return MtkDctConstants.APN_PREEMPT_ID;
            default:
                return DctConstants.APN_INVALID_ID;
        }
    }

    private boolean needNotifyType(String apnTypes) {
        if (apnTypes.equals(MtkPhoneConstants.APN_TYPE_WAP)
                || apnTypes.equals(MtkPhoneConstants.APN_TYPE_XCAP)
                || apnTypes.equals(MtkPhoneConstants.APN_TYPE_RCS)
                || apnTypes.equals(MtkPhoneConstants.APN_TYPE_BIP)
                || apnTypes.equals(MtkPhoneConstants.APN_TYPE_VSIM)
                ) {
            return false;
        }
        return true;
    }

    public boolean isNeedNotify() {
        if (DBG) {
            log("Current apn tpye:" + mApnType + " isNeedNotify" + mNeedNotify);
        }
        return mNeedNotify;
    }

    // M: [OD over ePDG] start
    @Override
    public synchronized String toString() {
        return super.toString() + " mWifiApns={" + mWifiApns + "}";
    }
    // M: [OD over ePDG] end

    public int getRefCount() {
        return mNetworkRequests.size();
    }
}
