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

import android.os.Bundle;
import android.hardware.radio.V1_0.ApnTypes;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.uicc.IccRecords;

import com.mediatek.internal.telephony.MtkPhoneConstants;
import com.mediatek.internal.telephony.OpTelephonyCustomizationFactoryBase;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
import com.mediatek.internal.telephony.uicc.MtkSIMRecords;

import java.util.List;
import vendor.mediatek.hardware.radio.V3_0.MtkApnTypes;

public class MtkApnSetting extends ApnSetting {
    private static final String LOG_TAG = "MtkApnSetting";
    private static final boolean DBG = true;
    private static final boolean VDBG = SystemProperties.get("ro.build.type").equals("eng") ?
            true : false;

    // M: For OP plug-in
    private static IDataConnectionExt sDataConnectionExt = null;

    /**
      * The use of inactive timer is to define the timer to disconnect PDN
      * if there's no TX/RX within the period.
      * The default value will be integer 0 if it's undefined in apns-conf.xml.
      */
    public final int inactiveTimer;

    public MtkApnSetting(int id, String numeric, String carrier, String apn,
            String proxy, String port,
            String mmsc, String mmsProxy, String mmsPort,
            String user, String password, int authType, String[] types,
            String protocol, String roamingProtocol, boolean carrierEnabled, int bearer,
            int bearerBitmask, int profileId, boolean modemCognitive, int maxConns, int waitTime,
            int maxConnsTime, int mtu, String mvnoType, String mvnoMatchData, int inactiveTimer) {
        super(id, numeric, carrier, apn, proxy, port, mmsc, mmsProxy, mmsPort, user, password,
                authType, types, protocol, roamingProtocol, carrierEnabled, bearer,
                bearerBitmask, profileId, modemCognitive, maxConns, waitTime, maxConnsTime,
                mtu, mvnoType, mvnoMatchData);
        this.inactiveTimer = inactiveTimer;
    }

    public MtkApnSetting(int id, String numeric, String carrier, String apn,
            String proxy, String port,
            String mmsc, String mmsProxy, String mmsPort,
            String user, String password, int authType, String[] types,
            String protocol, String roamingProtocol, boolean carrierEnabled,
            int networkTypeBitmask, int profileId, boolean modemCognitive, int maxConns,
            int waitTime, int maxConnsTime, int mtu, String mvnoType,
            String mvnoMatchData, int inactiveTimer) {
        super(id, numeric, carrier, apn, proxy, port, mmsc, mmsProxy, mmsPort, user, password,
                authType, types, protocol, roamingProtocol, carrierEnabled, networkTypeBitmask,
                profileId, modemCognitive, maxConns, waitTime, maxConnsTime, mtu, mvnoType,
                mvnoMatchData);
        this.inactiveTimer = inactiveTimer;
    }

    public MtkApnSetting(int id, String numeric, String carrier, String apn,
            String proxy, String port,
            String mmsc, String mmsProxy, String mmsPort,
            String user, String password, int authType, String[] types,
            String protocol, String roamingProtocol, boolean carrierEnabled,
            int networkTypeBitmask, int profileId, boolean modemCognitive, int maxConns,
            int waitTime, int maxConnsTime, int mtu, String mvnoType,
            String mvnoMatchData, int apnSetId, int inactiveTimer) {
        super(id, numeric, carrier, apn, proxy, port, mmsc, mmsProxy, mmsPort, user, password,
                authType, types, protocol, roamingProtocol, carrierEnabled, networkTypeBitmask,
                profileId, modemCognitive, maxConns, waitTime, maxConnsTime, mtu, mvnoType,
                mvnoMatchData, apnSetId);
        this.inactiveTimer = inactiveTimer;
    }

    public MtkApnSetting(MtkApnSetting apn) {
        this(apn.id, apn.numeric, apn.carrier, apn.apn, apn.proxy, apn.port, apn.mmsc, apn.mmsProxy,
                apn.mmsPort, apn.user, apn.password, apn.authType, apn.types, apn.protocol,
                apn.roamingProtocol, apn.carrierEnabled, apn.networkTypeBitmask, apn.profileId,
                apn.modemCognitive, apn.maxConns, apn.waitTime, apn.maxConnsTime,
                apn.mtu, apn.mvnoType, apn.mvnoMatchData, apn.apnSetId, apn.inactiveTimer);
    }

    // reflection for ApnSetting.fromString
    private static ApnSetting fromStringEx(String[] a, int authType, String[] typeArray,
            String protocol, String roamingProtocol, boolean carrierEnabled, int networkTypeBitmask,
            int profileId, boolean modemCognitive, int maxConns, int waitTime, int maxConnsTime,
            int mtu, String mvnoType, String mvnoMatchData, int apnSetId) {
        int inactiveTimer = 0;
        if (a.length > 28) {
            try {
                inactiveTimer = Integer.parseInt(a[28]);
            } catch (NumberFormatException e) {
                Rlog.e(LOG_TAG, "NumberFormatException, inactive timer = " + a[28]);
            }
        }

        return new MtkApnSetting(-1, a[10] + a[11], a[0], a[1], a[2], a[3], a[7], a[8], a[9],
                a[4], a[5], authType, typeArray, protocol, roamingProtocol, carrierEnabled,
                networkTypeBitmask, profileId, modemCognitive, maxConns, waitTime, maxConnsTime,
                mtu, mvnoType, mvnoMatchData, apnSetId, inactiveTimer);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(", ").append(inactiveTimer);
        return sb.toString();
    }

    @Override
    public boolean canHandleType(String type) {
        if (!carrierEnabled) return false;
        boolean wildcardable = true;
        if (PhoneConstants.APN_TYPE_IA.equalsIgnoreCase(type)) wildcardable = false;
        for (String t : types) {
            if (VDBG) {
                Log.v(LOG_TAG, "canHandleType(): entry in types=" + t + ", reqType=" + type);
            }

            // When the request type is DUN and the types only inlucde DUN type,
            // then return true.
            // Fix the issue, CTNET will configure types as default,dun,xcap, when
            // default data in in SIM1 and setup DUN in SIM1, send MMS in SIM2 will
            // fail as DUN's priority is same with MMS.
            // For this scenario, will treat CTNET not support DUN and then Tethering
            // module will request DEFAULT type instead of DUN.
            if (type.equalsIgnoreCase(PhoneConstants.APN_TYPE_DUN)) {
                if (t.equalsIgnoreCase(PhoneConstants.APN_TYPE_DUN)) {
                    return (types.length == 1);
                }
            } else {
                if (t.equalsIgnoreCase(type) ||
                        (wildcardable && t.equalsIgnoreCase(PhoneConstants.APN_TYPE_ALL) &&
                        !(type.equalsIgnoreCase(PhoneConstants.APN_TYPE_IMS) ||
                        type.equalsIgnoreCase(PhoneConstants.APN_TYPE_EMERGENCY)))) {
                    // M: Let the apn *  skip the "IMS" & "EMERGENCY" apn
                    return true;
                } else if (t.equalsIgnoreCase(PhoneConstants.APN_TYPE_DEFAULT) &&
                        type.equalsIgnoreCase(PhoneConstants.APN_TYPE_HIPRI)) {
                    Log.d(LOG_TAG, "canHandleType(): use DEFAULT for HIPRI type");
                    return true;
                }
            }
        }

        return false;
    }

    // reflection for ApnSetting.mvnoMatches
    private static boolean mvnoMatchesEx(IccRecords r, String mvnoType, String mvnoMatchData) {
        if (mvnoType.equalsIgnoreCase("pnn")) {
            //M: Support MVNO pnn type
            if ((r.isOperatorMvnoForEfPnn() != null) &&
                    r.isOperatorMvnoForEfPnn().equalsIgnoreCase(mvnoMatchData)) {
                return true;
           }
        }
        return false;
    }

    // reflection for ApnSetting.isMeteredApnType
    private static Bundle isMeteredApnTypeEx(String type, Phone phone) {
        boolean isRoaming = phone.getServiceState().getDataRoaming();
        boolean useEx = false;
        boolean result = false;

        if (sDataConnectionExt == null) {
            try {
                OpTelephonyCustomizationFactoryBase factoryBase =
                        OpTelephonyCustomizationUtils.getOpFactory(phone.getContext());
                sDataConnectionExt = factoryBase.makeDataConnectionExt(phone.getContext());
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "sDataConnectionExt init fail. e: " + e);
                sDataConnectionExt = null;
            }
        }

        if (sDataConnectionExt != null && sDataConnectionExt.isMeteredApnTypeByLoad()) {
            useEx = true;
            result = sDataConnectionExt.isMeteredApnType(type, isRoaming);
        }

        if (TextUtils.equals(type, MtkPhoneConstants.APN_TYPE_PREEMPT)) {
            useEx = true;
            result = true;
        }

        Bundle b = new Bundle();
        b.putBoolean("useEx", useEx);
        b.putBoolean("result", result);
        return b;
    }

    /**
     * M: Override this funciton for using MtkApnSetting.isMeteredApnType static function
     */
    @Override
    public boolean isMetered(Phone phone) {
        if (phone == null) {
            return true;
        }

        for (String type : types) {
            // If one of the APN type is metered, then this APN setting is metered.
            if (isMeteredApnType(type, phone)) {
                return true;
            }
        }
        return false;
    }

    /**
     * M: get APN string except name since we do not care about it.
     * {@hide}
     * @param ignoreName ignore name or not
     * @return APN string value
     */
    public String toStringIgnoreName(boolean ignoreName) {
        StringBuilder sb = new StringBuilder();
        sb.append(id);
        if (!ignoreName) {
            sb.append(", ").append(carrier);
        }
        sb.append(", ").append(numeric)
        .append(", ").append(apn)
        .append(", ").append(proxy)
        .append(", ").append(mmsc)
        .append(", ").append(mmsProxy)
        .append(", ").append(mmsPort)
        .append(", ").append(port)
        .append(", ").append(authType).append(", ");
        for (int i = 0; i < types.length; i++) {
            sb.append(types[i]);
            if (i < types.length - 1) {
                sb.append(" | ");
            }
        }
        sb.append(", ").append(protocol);
        sb.append(", ").append(roamingProtocol);
        sb.append(", ").append(carrierEnabled);
        sb.append(", ").append(bearer);
        sb.append(", ").append(bearerBitmask);
        sb.append(", ").append(profileId);
        sb.append(", ").append(modemCognitive);
        sb.append(", ").append(maxConns);
        sb.append(", ").append(waitTime);
        sb.append(", ").append(maxConnsTime);
        sb.append(", ").append(mtu);
        sb.append(", ").append(mvnoType);
        sb.append(", ").append(mvnoMatchData);
        sb.append(", ").append(permanentFailed);
        sb.append(", ").append(networkTypeBitmask);
        sb.append(", ").append(apnSetId);
        sb.append(", ").append(user);
        Rlog.d(LOG_TAG, "toStringIgnoreName: sb = " + sb.toString() + ", ignoreName: " +
                ignoreName);
        sb.append(", ").append(password);
        return sb.toString();
    }

    /**
     * M: Add to string with parameter ignoreName.
     * {@hide}
     * @param apnSettings apnSettings for APN list
     * @param ignoreName ignore name or not
     * @return APN string value
     */
    public static String toStringIgnoreNameForList(List<ApnSetting> apnSettings,
            boolean ignoreName) {
        if (apnSettings == null || apnSettings.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (ApnSetting t : apnSettings) {
            sb.append(((MtkApnSetting) t).toStringIgnoreName(ignoreName));
        }
        return sb.toString();
    }

    // Extend getApnBitmask() in ApnSetting.java for convert more APN type strings.
    public static int getApnBitmaskEx(String apn) {
        switch (apn) {
            case PhoneConstants.APN_TYPE_DEFAULT: return ApnTypes.DEFAULT;
            case PhoneConstants.APN_TYPE_MMS: return ApnTypes.MMS;
            case PhoneConstants.APN_TYPE_SUPL: return ApnTypes.SUPL;
            case PhoneConstants.APN_TYPE_DUN: return ApnTypes.DUN;
            case PhoneConstants.APN_TYPE_HIPRI: return ApnTypes.HIPRI;
            case PhoneConstants.APN_TYPE_FOTA: return ApnTypes.FOTA;
            case PhoneConstants.APN_TYPE_IMS: return ApnTypes.IMS;
            case PhoneConstants.APN_TYPE_CBS: return ApnTypes.CBS;
            case PhoneConstants.APN_TYPE_IA: return ApnTypes.IA;
            case PhoneConstants.APN_TYPE_EMERGENCY: return ApnTypes.EMERGENCY;
            case MtkPhoneConstants.APN_TYPE_WAP: return MtkApnTypes.WAP;
            case MtkPhoneConstants.APN_TYPE_XCAP: return MtkApnTypes.XCAP;
            case MtkPhoneConstants.APN_TYPE_RCS: return MtkApnTypes.RCS;
            case MtkPhoneConstants.APN_TYPE_BIP: return MtkApnTypes.BIP;
            case MtkPhoneConstants.APN_TYPE_VSIM: return MtkApnTypes.VSIM;
            case PhoneConstants.APN_TYPE_ALL: return MtkApnTypes.MTKALL;
            default: return ApnTypes.NONE;
        }
    }
}
