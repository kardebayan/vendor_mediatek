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

import static com.android.internal.telephony.DctConstants.*;
import static com.mediatek.internal.telephony.MtkDctConstants.*;

import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.net.NetworkRequest;
import android.net.ConnectivityManager;
import android.os.SystemProperties;

import com.android.internal.telephony.dataconnection.DcRequest;

public class MtkDcRequest extends DcRequest {
    public MtkDcRequest(NetworkRequest nr, Context context) {
        super(nr, context);
    }

    @Override
    protected int apnIdForNetworkRequest(NetworkRequest nr) {
        NetworkCapabilities nc = nr.networkCapabilities;
        // For now, ignore the bandwidth stuff
        if (nc.getTransportTypes().length > 0 &&
                nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == false) {
            return APN_INVALID_ID;
        }

        // in the near term just do 1-1 matches.
        // TODO - actually try to match the set of capabilities
        int apnId = super.apnIdForNetworkRequest(nr);

        boolean error = false;
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_EIMS)) {
            if (apnId != APN_INVALID_ID) error = true;
            apnId = APN_EMERGENCY_ID;
        }
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_WAP)) {
            if (apnId != APN_INVALID_ID) error = true;
            apnId = APN_WAP_ID;
        }
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_XCAP)) {
            if (apnId != APN_INVALID_ID) error = true;
            apnId = APN_XCAP_ID;
        }
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_RCS)) {
            if (apnId != APN_INVALID_ID) error = true;
            apnId = APN_RCS_ID;
        }
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_BIP)) {
            if (apnId != APN_INVALID_ID) error = true;
            apnId = APN_BIP_ID;
        }
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VSIM)) {
            if (apnId != APN_INVALID_ID) error = true;
            apnId = APN_VSIM_ID;
        }
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_PREEMPT)) {
            if (apnId != APN_INVALID_ID) error = true;
            apnId = APN_PREEMPT_ID;
        }

        if (error) {
            // TODO: If this error condition is removed, the framework's handling of
            // NET_CAPABILITY_NOT_RESTRICTED will need to be updated so requests for
            // say FOTA and INTERNET are marked as restricted.  This is not how
            // NetworkCapabilities.maybeMarkCapabilitiesRestricted currently works.
            loge("Multiple apn types specified in request - result is unspecified!");
        }
        if (apnId == APN_INVALID_ID) {
            loge("Unsupported NetworkRequest in Telephony: nr=" + nr);
        }
        return apnId;
    }

    @Override
    protected void initApnPriorities(Context context) {
        synchronized (sApnPriorityMap) {
            if (sApnPriorityMap.isEmpty()) {
                String[] networkConfigStrings = context.getResources().getStringArray(
                        com.android.internal.R.array.networkAttributes);
                for (String networkConfigString : networkConfigStrings) {
                    NetworkConfig networkConfig = new NetworkConfig(networkConfigString);
                    final int apnId = MtkApnContext.apnIdForType(networkConfig.type);

                    boolean isMuitlpleImsSupport =
                        (SystemProperties.getInt("persist.vendor.mims_support", 1) > 1)? true : false;

                    if((isMuitlpleImsSupport == true) &&
                        (networkConfig.type == ConnectivityManager.TYPE_MOBILE_EMERGENCY)){
                        loge("Force change emergency type APN priority to -1");
                        sApnPriorityMap.put(apnId, -1);
                    } else{
                        sApnPriorityMap.put(apnId, networkConfig.priority);
                    }
                }
            }
        }
    }
}
