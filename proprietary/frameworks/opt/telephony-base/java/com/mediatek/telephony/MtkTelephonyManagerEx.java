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
package com.mediatek.telephony;

import android.annotation.RequiresPermission;
import android.app.ActivityThread;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;

import android.telephony.CellInfo;
import android.telephony.CellLocation;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IPhoneSubInfo;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;

import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.List;

import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.IMtkPhoneSubInfoEx;
import com.mediatek.internal.telephony.MtkIccCardConstants;

import com.mediatek.internal.telephony.PseudoCellInfo;

/**
 * Provides access to information about the telephony services on
 * the device, especially for multiple SIM cards device.
 *
 * Applications can use the methods in this class to
 * determine telephony services and states, as well as to access some
 * types of subscriber information. Applications can also register
 * a listener to receive notification of telephony state changes.
 *
 * Note that access to some telephony information is
 * permission-protected. Your application cannot access the protected
 * information unless it has the appropriate permissions declared in
 * its manifest file. Where permissions apply, they are noted in the
 * the methods through which you access the protected information.
 */
public class MtkTelephonyManagerEx {
    private static final String TAG = "MtkTelephonyManagerEx";

    private Context mContext = null;
    private ITelephonyRegistry mRegistry;
    private static final String PRLVERSION = "vendor.cdma.prl.version";

    //MTK-START: SIM
    /** @hide */
    public static final int APP_FAM_NONE = 0x00;
    /** @hide */
    public static final int APP_FAM_3GPP = 0x01;
    /** @hide */
    public static final int APP_FAM_3GPP2 = 0x02;
    /** @hide */
    public static final int CARD_TYPE_NONE = 0x00;
    /** @hide */
    public static final int CARD_TYPE_SIM = 0x01;
    /** @hide */
    public static final int CARD_TYPE_USIM = 0x02;
    /** @hide */
    public static final int CARD_TYPE_CSIM = 0x04;
    /** @hide */
    public static final int CARD_TYPE_RUIM = 0x08;

    // [SIM-C2K] @{
    /**
     * The property is used to get supported card type of each SIM card in the slot.
     */
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE  = {
        "vendor.gsm.ril.fulluicctype",
        "vendor.gsm.ril.fulluicctype.2",
        "vendor.gsm.ril.fulluicctype.3",
        "vendor.gsm.ril.fulluicctype.4",
    };

    /**
     * The property is used to check if the card is cdma 3G dual mode card in the slot.
     */
    private static final String[] PROPERTY_RIL_CT3G = {
        "vendor.gsm.ril.ct3g",
        "vendor.gsm.ril.ct3g.2",
        "vendor.gsm.ril.ct3g.3",
        "vendor.gsm.ril.ct3g.4",
    };

    /**
     * The property shows cdma card type that is only for cdma card.
     */
    private static final String[] PROPERTY_RIL_CDMA_CARD_TYPE = {
        "vendor.ril.cdma.card.type.1",
        "vendor.ril.cdma.card.type.2",
        "vendor.ril.cdma.card.type.3",
        "vendor.ril.cdma.card.type.4",
    };
    // [SIM-C2K] @}
    // SIM ME LOCK - Start
    /**
     * The property shows SIM ME LOCK mode
     */
    private static final String PROPERTY_SML_MODE = "ro.vendor.sim_me_lock_mode";

    /**
     * The property shows sim slot lock policy-by device.
     */
    private static final String PROPERTY_SIM_SLOT_LOCK_POLICY =
            "vendor.gsm.sim.slot.lock.policy";

    /**
     * The property shows sim slot lock service capability-by slot.
     */
    private static final String[] PROPERTY_SIM_SLOT_LOCK_SERVICE_CAPABILITY = {
            "vendor.gsm.sim.slot.lock.service.capability",
            "vendor.gsm.sim.slot.lock.service.capability.2",
            "vendor.gsm.sim.slot.lock.service.capability.3",
            "vendor.gsm.sim.slot.lock.service.capability.4",
    };

    /**
     * The property shows if sim slot card valid or not-by slot.
     */
    private static final String[] PROPERTY_SIM_SLOT_LOCK_CARD_VALID = {
            "vendor.gsm.sim.slot.lock.card.valid",
            "vendor.gsm.sim.slot.lock.card.valid.2",
            "vendor.gsm.sim.slot.lock.card.valid.3",
            "vendor.gsm.sim.slot.lock.card.valid.4",
    };

    /**
     * The property shows sim slot lock state-by device.
     */
    private static final String PROPERTY_SIM_SLOT_LOCK_STATE =
            "vendor.gsm.sim.slot.lock.state";

    private boolean mIsSmlLockMode = SystemProperties.get(PROPERTY_SML_MODE, "").equals("3");
    // SIM ME LOCK - End

    /// M: CC: ECC is in progress @{
    /**
     * Intent to notify other modules ECC is in progress.
     * @hide
     */
    public static final String ACTION_ECC_IN_PROGRESS = "android.intent.action.ECC_IN_PROGRESS";

    /**
     * Extra value to indicate the ECC state.
     * @hide
     */
    public static final String EXTRA_IN_PROGRESS = "in_progress";
    /// @}

    /**
     * Construction function for TelephonyManager
     * @param context a context
     */
    public MtkTelephonyManagerEx(Context context) {
        mContext = context;
        mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService(
                    "telephony.registry"));

    }

    /*  Construction function for TelephonyManager */
    private MtkTelephonyManagerEx() {
        mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService(
                   "telephony.registry"));
    }

    private  static MtkTelephonyManagerEx sInstance = new MtkTelephonyManagerEx();

    /**
     * Return the static instance of TelephonyManagerEx
     * @return return the static instance of TelephonyManagerEx
     * @hide
     * @deprecated
     */
    public static MtkTelephonyManagerEx getDefault() {
        return sInstance;
    }

    /**
     * Returns a constant indicating the device phone type.  This
     * indicates the type of radio used to transmit voice calls.
     *
     * @param simId Indicates which SIM(slot) to query
     * @return  a constant indicating the device phone type
     *
     * @see #PHONE_TYPE_NONE
     * @see #PHONE_TYPE_GSM
     * @see #PHONE_TYPE_CDMA
     */
    public int getPhoneType(int simId) {
        int subIds[] = SubscriptionManager.getSubId(simId);
        if (subIds == null) {
            return TelephonyManager.getDefault()
                    .getCurrentPhoneType(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        }
        Rlog.e(TAG, "Deprecated! getPhoneType with simId " + simId + ", subId " + subIds[0]);
        return TelephonyManager.getDefault().getCurrentPhoneType(subIds[0]);
    }

    private int getSubIdBySlot(int slot) {
        int [] subId = SubscriptionManager.getSubId(slot);
        Rlog.d(TAG, "getSubIdBySlot, simId " + slot +
                "subId " + ((subId != null) ? subId[0] : "invalid!"));
        return (subId != null) ? subId[0] : SubscriptionManager.getDefaultSubscriptionId();
    }

    private ITelephony getITelephony() {
        return ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
    }

    private IMtkTelephonyEx getIMtkTelephonyEx() {
        return IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
    }

    private IPhoneSubInfo getSubscriberInfo() {
        // get it each time because that process crashes a lot
        return IPhoneSubInfo.Stub.asInterface(ServiceManager.getService("iphonesubinfo"));
    }

    /**
     * It used to get whether the device is in dual standby dual connection.
     * For example, call application will be able to support dual connection
     * if the device mode is in DSDA.
     *
     * @return true if the device is in DSDA mode, false for others
     * @hide
     */
    public boolean isInDsdaMode() {
        if (SystemProperties.get("ro.vendor.mtk_switch_antenna", "0").equals("1")) {
            return false;
        }
        if (SystemProperties.getInt("ro.boot.opt_c2k_lte_mode", 0) == 1) {
            // check if contains CDMAPhone
            TelephonyManager tm = TelephonyManager.getDefault();
            int simCount = tm.getSimCount();
            for (int i = 0; i < simCount; i++) {
                int[] allSubId = SubscriptionManager.getSubId(i);
                if (allSubId == null) {
                    Rlog.d(TAG, "isInDsdaMode, allSubId is null for slot" + i);
                    continue;
                }
                int phoneType = tm.getCurrentPhoneType(allSubId[0]);
                Rlog.d(TAG, "isInDsdaMode, allSubId[0]:" + allSubId[0]
                        + ", phoneType:" + phoneType);
                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    return true;
                }
            }
        }
        return false;
    }

    /// M: [Network][C2K] Add isInHomeNetwork interface. @{
    /**
     * Return whether in home area for the specific subscription id.
     *
     * @param subId the id of the subscription to be queried.
     * @return true if in home network.
     */
    public boolean isInHomeNetwork(int subId) {
        try {
            IMtkTelephonyEx telephonyEx = getIMtkTelephonyEx();
            if (telephonyEx == null) {
                return false;
            } else {
                return telephonyEx.isInHomeNetwork(subId);
            }
        } catch (RemoteException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
    }
    /// @}

    // MTK-START: SIM
    private IMtkPhoneSubInfoEx getMtkSubscriberInfoEx() {
        // get it each time because that process crashes a lot
        return IMtkPhoneSubInfoEx.Stub.asInterface(ServiceManager.getService("iphonesubinfoEx"));
    }

    /**
     * Returns the USIM Service Table (UST) that was loaded from the USIM.
     * @param service service index on UST
     * @return the indicated service is supported or not
     * @hide
     */
    public boolean getUsimService(int service) {
        return getUsimService(SubscriptionManager.getDefaultSubscriptionId(), service);
    }

    /**
     * Returns the USIM Service Table (UST) that was loaded from the USIM.
     * @param subId subscription ID to be queried
     * @param service service index on UST
     * @return the indicated service is supported or not
     * @hide
     */
    public boolean getUsimService(int subId, int service) {
        try {
            return getMtkSubscriberInfoEx().getUsimServiceForSubscriber(subId,
                    service, getOpPackageName());
        } catch (RemoteException ex) {
            return false;
        } catch (NullPointerException ex) {
            // This could happen before phone restarts due to crashing
            return false;
        }
    }

    /**
     * Get icc app family by slot id.
     * @param slotId slot id
     * @return the family type
     */
    public int getIccAppFamily(int slotId) {
        try {
            return getIMtkTelephonyEx().getIccAppFamily(slotId);
        } catch (RemoteException ex) {
            return APP_FAM_NONE;
        } catch (NullPointerException ex) {
            return APP_FAM_NONE;
        }
    }

    /**
     * Get Icc Card Type
     * @param subId which subId to query
     * @return "SIM" for SIM card or "USIM" for USIM card.
     * @hide
     * @internal
     */
    public String getIccCardType(int subId) {
        String type = null;
        try {
            type = getIMtkTelephonyEx().getIccCardType(subId);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            // This could happen before phone restarts due to crashing
            ex.printStackTrace();
        }
        Rlog.d(TAG, "getIccCardType sub " + subId + " ,icc type " +
                ((type != null) ? type : "null"));
        return type;
    }

      private String getOpPackageName() {
        // For legacy reasons the TelephonyManager has API for getting
        // a static instance with no context set preventing us from
        // getting the op package name. As a workaround we do a best
        // effort and get the context from the current activity thread.
        if (mContext != null) {
            return mContext.getOpPackageName();
        }
        return ActivityThread.currentOpPackageName();
    }

    /**
     * Returns the response APDU for a command APDU sent through SIM_IO.
     *
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#MODIFY_PHONE_STATE MODIFY_PHONE_STATE}
     * Or the calling app has carrier privileges. @see #hasCarrierPrivileges
     *
     * @param slot
     * @param family
     * @param fileID
     * @param filePath
     * @return The APDU response
     * @hide
     */
    public byte[] loadEFTransparent(int slotId, int family, int fileID, String filePath) {
        try {
            IMtkTelephonyEx telephony = getIMtkTelephonyEx();
            if (telephony != null)
                return telephony.loadEFTransparent(slotId, family, fileID, filePath);
        } catch (RemoteException ex) {
        } catch (NullPointerException ex) {
        }
        return null;
    }

    /**
     * Returns the response APDU for a command APDU sent through SIM_IO.
     *
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#MODIFY_PHONE_STATE MODIFY_PHONE_STATE}
     * Or the calling app has carrier privileges. @see #hasCarrierPrivileges
     *
     * @param slot
     * @param family
     * @param fileID
     * @param filePath
     * @return The APDU response
     * @hide
     */
    public List<String> loadEFLinearFixedAll(int slotId, int family, int fileID,
            String filePath) {
        try {
            IMtkTelephonyEx telephony = getIMtkTelephonyEx();
            if (telephony != null)
                return telephony.loadEFLinearFixedAll(slotId, family, fileID, filePath);
        } catch (RemoteException ex) {
        } catch (NullPointerException ex) {
        }
        return null;
    }
    // MTK-END

    // [SIM-C2K] @{
    /**
     * Get uim imsi by given sub id.
     * @param subId subscriber id
     *
     * Requires Permission:
     *   {@link android.Manifest.permission#READ_PHONE_STATE}
     *
     * @return uim imsi
     */
    public String getUimSubscriberId(int subId) {
        String  uimImsi = "";

        try {
            uimImsi = getIMtkTelephonyEx().getUimSubscriberId(getOpPackageName(), subId);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }

        return uimImsi;
    }

    /**
     * Return the supported card type of the SIM card in the slot.
     *
     * @param slotId the given slot id.
     * @return supported card type array.
     */
    public String[] getSupportCardType(int slotId) {
        String property = null;
        String prop = null;
        String values[] = null;

        if (slotId < 0 || slotId >= PROPERTY_RIL_FULL_UICC_TYPE.length) {
            Rlog.e(TAG, "getSupportCardType: invalid slotId " + slotId);
            return null;
        }
        prop = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[slotId], "");
        if ((!prop.equals("")) && (prop.length() > 0)) {
            values = prop.split(",");
        }
        Rlog.d(TAG, "getSupportCardType slotId " + slotId + ", prop value= " + prop +
                ", size= " + ((values != null) ? values.length : 0));
        return values;
    }

    /**
     * Check if the specified slot is CT 3G dual mode card.
     * @param slotId slot ID
     * @return if it's CT 3G dual mode card
     */
    public boolean isCt3gDualMode(int slotId) {
        if (slotId < 0 || slotId >= PROPERTY_RIL_CT3G.length) {
            Rlog.e(TAG, "isCt3gDualMode: invalid slotId " + slotId);
            return false;
        }
        String result = SystemProperties.get(PROPERTY_RIL_CT3G[slotId], "");
        Rlog.d(TAG, "isCt3gDualMode:  " + result);
        return ("1".equals(result));
    }

    /**
     * Get CDMA card type by given slot id.
     * @param slotId slot ID
     * @return the SIM type
     */
    public MtkIccCardConstants.CardType getCdmaCardType(int slotId) {
        if (slotId < 0 || slotId >= PROPERTY_RIL_CT3G.length) {
            Rlog.e(TAG, "getCdmaCardType: invalid slotId " + slotId);
            return null;
        }
        MtkIccCardConstants.CardType mCdmaCardType = MtkIccCardConstants.CardType.UNKNOW_CARD;
        String result = SystemProperties.get(PROPERTY_RIL_CDMA_CARD_TYPE[slotId], "");
        if (!result.equals("")) {
            int cardtype = Integer.parseInt(result);
            mCdmaCardType = MtkIccCardConstants.CardType.getCardTypeFromInt(cardtype);
        }
        Rlog.d(TAG, "getCdmaCardType result: " + result + "  mCdmaCardType: " + mCdmaCardType);
        return mCdmaCardType;
    }

    /**
     * Returns the serial number for the given subscription, if applicable. Return null if it is
     * unavailable.
     * @param simId  Indicates which SIM to query.
     *               Value of simId:
     *                 0 for SIM1
     *                 1 for SIM2
     * <p>
     * @return       serial number of the SIM, if applicable. Null is returned if it is unavailable.
     *
     */
    public String getSimSerialNumber(int simId) {
        if (simId < 0 || simId >= TelephonyManager.getDefault().getSimCount()) {
            Rlog.e(TAG, "getSimSerialNumber with invalid simId " + simId);
            return null;
        }

        String iccId = null;
        try {
            iccId = getIMtkTelephonyEx().getSimSerialNumber(getOpPackageName(), simId);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }

        if (iccId != null && (iccId.equals("N/A") || iccId.equals(""))) {
            iccId = null;
        }

        return iccId;
    }
    // [SIM-C2K] @}

    /**
     * Returns the current cell location of the device.
     * <p>
     * Required Permission:
     *  android.Manifest.permission#ACCESS_COARSE_LOCATION ACCESS_COARSE_LOCATION or
     *  android.Manifest.permission#ACCESS_COARSE_LOCATION ACCESS_FINE_LOCATION.
     *
     * @param simId  Indicates which SIM to query.
     *               Value of simId:
     *                 0 for SIM1
     *                 1 for SIM2
     * @return current cell location of the device. A CellLocation object
     * returns null if the current location is not available.
     *
     */
    public CellLocation getCellLocation(int simId) {
        try {
            IMtkTelephonyEx telephony = getIMtkTelephonyEx();
            if (telephony == null) {
                Rlog.d(TAG, "getCellLocation returning null because telephony is null");
                return null;
            }
            Bundle bundle = telephony.getCellLocationUsingSlotId(simId);
            if (bundle == null) {
                Rlog.d(TAG, "getCellLocation returning null because bundle is null");
                return null;
            } else if (bundle.isEmpty()) {
                Rlog.d(TAG, "getCellLocation returning null because bundle is empty");
                return null;
            }
            int phoneType = getPhoneType(simId);
            CellLocation cl = null;
            switch (phoneType) {
                case PhoneConstants.PHONE_TYPE_CDMA:
                    cl = new CdmaCellLocation(bundle);
                    break;
                case PhoneConstants.PHONE_TYPE_GSM:
                    cl = new GsmCellLocation(bundle);
                    break;
                default:
                    cl = null;
                    break;
            }
            Rlog.d(TAG, "getCellLocation is" + cl);
            if (cl == null) {
                Rlog.d(TAG, "getCellLocation returning null because cl is null");
                return null;
            } else if (cl.isEmpty()) {
                Rlog.d(TAG, "getCellLocation returning null because CellLocation is empty");
                return null;
            }
            return cl;
        } catch (RemoteException ex) {
            Rlog.d(TAG, "getCellLocation returning null due to RemoteException " + ex);
            return null;
        } catch (NullPointerException ex) {
            Rlog.d(TAG, "getCellLocation returning null due to NullPointerException " + ex);
            return null;
        }
    }

    /**
     * Check radio technology is GSM or not for SMS over IMS.
     * @param radioTechnology indicates radio technology type.
     * @return TRUE or FALSE depend on radioTechnology is GSM or not.
     */
    public boolean isGsm(int radioTechnology) {
        return radioTechnology == TelephonyManager.NETWORK_TYPE_GPRS
                || radioTechnology == TelephonyManager.NETWORK_TYPE_EDGE
                || radioTechnology == TelephonyManager.NETWORK_TYPE_UMTS
                || radioTechnology == TelephonyManager.NETWORK_TYPE_HSDPA
                || radioTechnology == TelephonyManager.NETWORK_TYPE_HSUPA
                || radioTechnology == TelephonyManager.NETWORK_TYPE_HSPA
                || radioTechnology == TelephonyManager.NETWORK_TYPE_LTE
                || radioTechnology == TelephonyManager.NETWORK_TYPE_HSPAP
                || radioTechnology == TelephonyManager.NETWORK_TYPE_GSM
                || radioTechnology == TelephonyManager.NETWORK_TYPE_TD_SCDMA
                || radioTechnology == TelephonyManager.NETWORK_TYPE_LTE_CA;
    }

    // MTK-START: ISIM
    /**
     * Returns the IMS private user identity (IMPI) that was loaded from the ISIM.
     * @param subId subscription ID to be queried
     * @return the IMPI, or null if not present or not loaded
     * @hide
     */
    public String getIsimImpi(int subId) {
        try {
            return getMtkSubscriberInfoEx().getIsimImpiForSubscriber(subId);
        } catch (RemoteException ex) {
            return null;
        } catch (NullPointerException ex) {
            // This could happen before phone restarts due to crashing
            return null;
        }
    }

    /**
     * Returns the IMS home network domain name that was loaded from the ISIM.
     * @param subId subscription ID to be queried
     * @return the IMS domain name, or null if not present or not loaded
     * @hide
     */
    public String getIsimDomain(int subId) {
        try {
            return getMtkSubscriberInfoEx().getIsimDomainForSubscriber(subId);
        } catch (RemoteException ex) {
            return null;
        } catch (NullPointerException ex) {
            // This could happen before phone restarts due to crashing
            return null;
        }
    }

    /**
     * Returns the IMS public user identities (IMPU) that were loaded from the ISIM.
     * @param subId subscription ID to be queried
     * @return an array of IMPU strings, with one IMPU per string, or null if
     *      not present or not loaded
     * @hide
     */
    public String[] getIsimImpu(int subId) {
        try {
            return getMtkSubscriberInfoEx().getIsimImpuForSubscriber(subId);
        } catch (RemoteException ex) {
            return null;
        } catch (NullPointerException ex) {
            // This could happen before phone restarts due to crashing
            return null;
        }
    }

    /**
     * Returns the IMS Service Table (IST) that was loaded from the ISIM.
     * @param subId subscription ID to be queried
     * @return IMS Service Table or null if not present or not loaded
     * @hide
     */
    public String getIsimIst(int subId) {
        try {
            return getMtkSubscriberInfoEx().getIsimIstForSubscriber(subId);
        } catch (RemoteException ex) {
            return null;
        } catch (NullPointerException ex) {
            // This could happen before phone restarts due to crashing
            return null;
        }
    }

    /**
     * Returns the IMS Proxy Call Session Control Function(PCSCF) that were loaded from the ISIM.
     * @param subId subscription ID to be queried
     * @return an array of PCSCF strings with one PCSCF per string, or null if
     *         not present or not loaded
     * @hide
     */
    public String[] getIsimPcscf(int subId) {
        try {
            return getMtkSubscriberInfoEx().getIsimPcscfForSubscriber(subId);
        } catch (RemoteException ex) {
            return null;
        } catch (NullPointerException ex) {
            // This could happen before phone restarts due to crashing
            return null;
        }
    }
    // MTK-END

    /**
     * Get IMS registration state by given sub-id.
     * @param subId The subId for query
     * @return true if IMS is registered, or false
     * @hide
     */
    public boolean isImsRegistered(int subId) {
        try {
            return getIMtkTelephonyEx().isImsRegistered(subId);
        } catch (RemoteException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
    }

    /**
     * Get Volte registration state by given sub-id.
     * @param subId The subId for query
     * @return true if volte is registered, or false
     * @hide
     */
    public boolean isVolteEnabled(int subId) {
        try {
            return getIMtkTelephonyEx().isVolteEnabled(subId);
        } catch (RemoteException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
    }

    /**
     * Get WFC registration state by given sub-id.
     * @param subId The subId for query
     * @return true if wfc is registered, or false
     * @hide
     */
    public boolean isWifiCallingEnabled(int subId) {
        try {
            return getIMtkTelephonyEx().isWifiCallingEnabled(subId);
        } catch (RemoteException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
    }

    /**
     * Get WFC wifi PDN is active or not due to wifi may out of service.
     * @param subId The subId for query
     * @return true if wifi pdn is active, or false
     * @hide
     */
    public boolean isWifiCalllingActive(int subId) {
        try {
            // TODO: Need to change to isWifiCallingEnabled && !isWifiPdnOutOffService()
            return getIMtkTelephonyEx().isWifiCallingEnabled(subId);
        } catch (RemoteException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
    }

    // MTK-START: SIM GBA
    /**
     * Returns the GBA bootstrapping parameters (GBABP) that was loaded from the ISIM.
     * @return GBA bootstrapping parameters or null if not present or not loaded
     * @hide
     */
    public String getIsimGbabp() {
        return getIsimGbabp(SubscriptionManager.getDefaultSubscriptionId());
    }

    /**
     * Returns the GBA bootstrapping parameters (GBABP) that was loaded from the ISIM.
     * @param subId subscription ID to be queried
     * @return GBA bootstrapping parameters or null if not present or not loaded
     * @hide
     */
    public String getIsimGbabp(int subId) {
        try {
            return getMtkSubscriberInfoEx().getIsimGbabpForSubscriber(subId);
        } catch (RemoteException ex) {
            return null;
        } catch (NullPointerException ex) {
            // This could happen before phone restarts due to crashing
            return null;
        }
    }

    /**
     * Set the GBA bootstrapping parameters (GBABP) value into the ISIM.
     * @param gbabp a GBA bootstrapping parameters value in String type
     * @param onComplete
     *        onComplete.obj will be an AsyncResult
     *        ((AsyncResult)onComplete.obj).exception == null on success
     *        ((AsyncResult)onComplete.obj).exception != null on fail
     * @hide
     */
    public void setIsimGbabp(String gbabp, Message onComplete) {
        setIsimGbabp(SubscriptionManager.getDefaultSubscriptionId(), gbabp, onComplete);
    }

    /**
     * Set the GBA bootstrapping parameters (GBABP) value into the ISIM.
     * @param subId subscription ID to be queried
     * @param gbabp a GBA bootstrapping parameters value in String type
     * @param onComplete
     *        onComplete.obj will be an AsyncResult
     *        ((AsyncResult)onComplete.obj).exception == null on success
     *        ((AsyncResult)onComplete.obj).exception != null on fail
     * @hide
     */
    public void setIsimGbabp(int subId, String gbabp, Message onComplete) {
        try {
            getMtkSubscriberInfoEx().setIsimGbabpForSubscriber(subId, gbabp, onComplete);
        } catch (RemoteException ex) {
            return;
        } catch (NullPointerException ex) {
            // This could happen before phone restarts due to crashing
            return;
        }
    }
    // MTK-END

    // MTK-START: SIM GBA
    /**
     * Returns the GBA bootstrapping parameters (GBABP) that was loaded from the USIM.
     * @return GBA bootstrapping parameters or null if not present or not loaded
     * @hide
     */
    public String getUsimGbabp() {
        return getUsimGbabp(SubscriptionManager.getDefaultSubscriptionId());
    }

    /**
     * Returns the GBA bootstrapping parameters (GBABP) that was loaded from the USIM.
     * @param subId subscription ID to be queried
     * @return GBA bootstrapping parameters or null if not present or not loaded
     * @hide
     */
    public String getUsimGbabp(int subId) {
        try {
            return getMtkSubscriberInfoEx().getUsimGbabpForSubscriber(subId);
        } catch (RemoteException ex) {
            return null;
        } catch (NullPointerException ex) {
            // This could happen before phone restarts due to crashing
            return null;
        }
    }

    /**
     * Set the GBA bootstrapping parameters (GBABP) value into the USIM.
     * @param gbabp a GBA bootstrapping parameters value in String type
     * @param onComplete
     *        onComplete.obj will be an AsyncResult
     *        ((AsyncResult)onComplete.obj).exception == null on success
     *        ((AsyncResult)onComplete.obj).exception != null on fail
     * @hide
     */
    public void setUsimGbabp(String gbabp, Message onComplete) {
        setUsimGbabp(SubscriptionManager.getDefaultSubscriptionId(), gbabp, onComplete);
    }

    /**
     * Set the GBA bootstrapping parameters (GBABP) value into the USIM.
     * @param subId subscription ID to be queried
     * @param gbabp a GBA bootstrapping parameters value in String type
     * @param onComplete
     *        onComplete.obj will be an AsyncResult
     *        ((AsyncResult)onComplete.obj).exception == null on success
     *        ((AsyncResult)onComplete.obj).exception != null on fail
     * @hide
     */
    public void setUsimGbabp(int subId, String gbabp, Message onComplete) {
        try {
            getMtkSubscriberInfoEx().setUsimGbabpForSubscriber(subId, gbabp, onComplete);
        } catch (RemoteException ex) {
            return;
        } catch (NullPointerException ex) {
            // This could happen before phone restarts due to crashing
            return;
        }
    }
    // MTK-END

    /**
     * Get PrlVersion by subId.
     * @param subId subId
     * @return prl version
     * @hide
     */
    ///M: [Network][C2K] provide API to get Prl version by SubId. @{
    public String getPrlVersion(int subId) {
        int slotId = SubscriptionManager.getSlotIndex(subId);
        String prlVersion = SystemProperties.get(PRLVERSION + slotId, "");
        Rlog.d(TAG, "getPrlversion PRLVERSION subId = " + subId
                + " key = " + PRLVERSION + slotId
                + " value = " + prlVersion);
        return prlVersion;
    }
    /// @}

    /**
     * Set RF test Configuration to defautl phone
     * @param config The configuration
     *  0: signal information is not available on all Rx chains
     *  1: Rx diversity bitmask for chain 0(primary antenna)
     *  2: Rx diversity bitmask for chain 1(secondary antenna) is available
     *  3: Signal information on both Rx chains is available
     */
    public int [] setRxTestConfig(int config) {
        int defaultPhoneId =
                SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultSubscriptionId());
        try {
            return getIMtkTelephonyEx().setRxTestConfig(defaultPhoneId, config);
        } catch (RemoteException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }
    }

    /**
     * Query RF Test Result
     */
    public int [] getRxTestResult() {
        int defaultPhoneId =
                SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultSubscriptionId());
        try {
            return getIMtkTelephonyEx().getRxTestResult(defaultPhoneId);
        } catch (RemoteException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }
    }

    /// M: CC: ECC is in progress @{
    /**
     * Set ECC in progress.
     *
     * @param state ECC in pregress or not.
     */
    public void setEccInProgress(boolean state) {
        try {
            getIMtkTelephonyEx().setEccInProgress(state);
        } catch (RemoteException ex) {
            return;
        } catch (NullPointerException ex) {
            return;
        }
    }

    /**
     * Check if ECC is in progress or not.
     *
     * @return true if ECC is in progress, otherwise false.
     */
    public boolean isEccInProgress() {
        try {
            return getIMtkTelephonyEx().isEccInProgress();
        } catch (RemoteException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
    }
    /// @}

    /**
     * Request to exit emergency call back mode.
     *
     * @param subId the subscription ID
     * @return true if exist the emeregency call back mode sucessfully
     */
    public boolean exitEmergencyCallbackMode(int subId) {
        try {
            return getIMtkTelephonyEx().exitEmergencyCallbackMode(subId);
        } catch (RemoteException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public void setApcMode(int slotId, int mode, boolean reportOn,
            int reportInterval) {
        if (slotId < 0 || slotId >= TelephonyManager.getDefault().getSimCount()) {
            Rlog.e(TAG, "setApcMode error with invalid slotId " + slotId);
            return;
        }
        if (mode < 0 || mode > 2) {
            Rlog.e(TAG, "setApcMode error with invalid mode " + mode);
            return;
        }

        try {
            IMtkTelephonyEx telephony = getIMtkTelephonyEx();
            if (telephony == null) {
                Rlog.e(TAG, "setApcMode error because telephony is null");
                return;
            }
            telephony.setApcModeUsingSlotId(slotId, mode,
                    reportOn, reportInterval);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "setApcMode error due to RemoteException " + ex);
            return;
        } catch (NullPointerException ex) {
            Rlog.e(TAG, "setApcMode error due to NullPointerException " + ex);
            return;
        }
    }

    public PseudoCellInfo getApcInfo(int slotId) {
        if (slotId < 0 || slotId >= TelephonyManager.getDefault().getSimCount()) {
            Rlog.e(TAG, "getApcInfo with invalid slotId " + slotId);
            return null;
        }

        try {
            IMtkTelephonyEx telephony = getIMtkTelephonyEx();
            if (telephony == null) {
                Rlog.e(TAG, "getApcInfo return null because telephony is null");
                return null;
            }
            return telephony.getApcInfoUsingSlotId(slotId);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "getApcInfo returning null due to RemoteException " + ex);
            return null;
        } catch (NullPointerException ex) {
            Rlog.e(TAG, "getApcInfo returning null due to NullPointerException " + ex);
            return null;
        }
    }

    /**
     * Get CDMA subscription active status  by subId.
     * @param subId subId
     * @return active status. 1 is active, 0 is deactive
     */
    public int getCdmaSubscriptionActStatus(int subId) {
        int actStatus = 0;

        try {
            actStatus = getIMtkTelephonyEx().getCdmaSubscriptionActStatus(subId);
        } catch (RemoteException ex) {
            Rlog.d(TAG, "fail to getCdmaSubscriptionActStatus due to RemoteException");
        } catch (NullPointerException ex) {
            Rlog.d(TAG, "fail to getCdmaSubscriptionActStatus due to NullPointerException");
        }
        return actStatus;
    }

    /// M: CC: AT&T ECC @{
    public boolean useATTLogic() {
        boolean ret = false;

        // Via CarrierExpress or default OPTR
        String optr = SystemProperties.get("persist.vendor.operator.optr", "OM");
        if (optr.equals("OP07")) {
            ret = true;
        }
        return ret;
    }
    /// @}

    /// M: CC: Vzw ECC/hVoLTE redial @{
    public boolean useVzwLogic() {
        boolean ret = false;

        // Via CarrierExpress or default OPTR
        String optr = SystemProperties.get("persist.vendor.operator.optr", "OM");
        if (optr.equals("OP12") || optr.equals("OP20")) {
            ret = true;
        }
        return ret;
    }
    ///@}

    /**
     * Returns the result and response from RIL for oem request
     *
     * @param oemReq the data is sent to ril.
     * @param oemResp the respose data from RIL.
     * @return negative value request was not handled or get error
     *         0 request was handled succesfully, but no response data
     *         positive value success, data length of response
     * @deprecated, use invokeOemRilRequestRawBySlot for instead
     */
    public int invokeOemRilRequestRaw(byte[] oemReq, byte[] oemResp) {
        try {
            IMtkTelephonyEx telephony = getIMtkTelephonyEx();
            if (telephony != null)
                return telephony.invokeOemRilRequestRaw(oemReq, oemResp);
        } catch (RemoteException ex) {
        } catch (NullPointerException ex) {
        }
        return -1;
    }

    /**
     * Returns the result and response from RIL for oem request
     *
     * @param slotId the slot ID.
     * @param oemReq the data is sent to ril.
     * @param oemResp the respose data from RIL.
     * @return negative value request was not handled or get error
     *         0 request was handled succesfully, but no response data
     *         positive value success, data length of response
     */
    public int invokeOemRilRequestRawBySlot(int slotId, byte[] oemReq, byte[] oemResp) {
        try {
            IMtkTelephonyEx telephony = getIMtkTelephonyEx();
            if (telephony != null)
                return telephony.invokeOemRilRequestRawBySlot(slotId, oemReq, oemResp);
        } catch (RemoteException ex) {
        } catch (NullPointerException ex) {
        }
        return -1;
    }

    /**
     * Whether the phone supports Digits multiple lines.
     *
     * @return {@code true} if the device supports Digits, and {@code false} otherwise.
     */
    public boolean isDigitsSupported() {
        boolean result = SystemProperties.getInt("persist.vendor.mtk_digits_support", 0) == 1;
        return result;
    }

    /*
     * For CDMA system UI display requirement. Check whether in CS call.
     */
    public boolean isInCsCall(int phoneId) {
        try {
            IMtkTelephonyEx telephony = getIMtkTelephonyEx();
            if (telephony == null) {
                Rlog.e(TAG, "[isInCsCall] telephony = null");
                return false;
            }
            return telephony.isInCsCall(phoneId);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "[isInCsCall] RemoteException " + ex);
            return false;
        } catch (NullPointerException ex) {
            Rlog.e(TAG, "[isInCsCall] NullPointerException " + ex);
            return false;
        }
    }

    /**
     * Returns a constant indicating the state of the slot index SIM card.
     *
     * @see #SIM_STATE_UNKNOWN
     * @see #SIM_STATE_ABSENT
     * @see #SIM_STATE_CARD_IO_ERROR
     * @see #SIM_STATE_CARD_RESTRICTED
     * @see #SIM_STATE_PRESENT
     *
     * @param slotId slot index
     * @return SIM card state
     * @hide
     */
    public int getSimCardState(int slotId) {
        int simCardState = SubscriptionManager.getSimStateForSlotIndex(slotId);

        switch (simCardState) {
            case TelephonyManager.SIM_STATE_UNKNOWN:
            case TelephonyManager.SIM_STATE_ABSENT:
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
            case TelephonyManager.SIM_STATE_CARD_RESTRICTED:
                return simCardState;
            default:
                return TelephonyManager.SIM_STATE_PRESENT;
        }
    }

    /**
     * Returns a constant indicating the state of the card applications on the slot index SIM card.
     *
     * @see #SIM_STATE_UNKNOWN
     * @see #SIM_STATE_PIN_REQUIRED
     * @see #SIM_STATE_PUK_REQUIRED
     * @see #SIM_STATE_NETWORK_LOCKED
     * @see #SIM_STATE_NOT_READY
     * @see #SIM_STATE_PERM_DISABLED
     * @see #SIM_STATE_LOADED
     *
     * @param slotId slot index
     * @return SIM application state
     * @hide
     */
     public int getSimApplicationState(int slotId) {
        int simApplicationState = SubscriptionManager.getSimStateForSlotIndex(slotId);

        switch (simApplicationState) {
            case TelephonyManager.SIM_STATE_UNKNOWN:
            case TelephonyManager.SIM_STATE_ABSENT:
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
            case TelephonyManager.SIM_STATE_CARD_RESTRICTED:
                return TelephonyManager.SIM_STATE_UNKNOWN;
            case TelephonyManager.SIM_STATE_READY:
                // Ready is not a valid state anymore. The state that is broadcast goes from
                // NOT_READY to either LOCKED or LOADED.
                return TelephonyManager.SIM_STATE_NOT_READY;
            default:
                return simApplicationState;
        }
    }

    /**
     * Returns all observed cell information from the specific slot of
     * device including the primary and neighboring cells.
     *
     * <p>
     * The list can include one or more {@link android.telephony.CellInfoGsm CellInfoGsm},
     * {@link android.telephony.CellInfoCdma CellInfoCdma},
     * {@link android.telephony.CellInfoLte CellInfoLte}, and
     * {@link android.telephony.CellInfoWcdma CellInfoWcdma} objects, in any combination.
     *
     * @param slotId the id of the slot.
     * @return List of {@link android.telephony.CellInfo}; null if cell information is unavailable.
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    public List<CellInfo> getAllCellInfo(int slotId) {
        try {
            IMtkTelephonyEx telephonyEx = getIMtkTelephonyEx();
            if (telephonyEx == null) {
                return null;
            }
            return telephonyEx.getAllCellInfo(slotId, getOpPackageName());
        } catch (RemoteException ex) {
            return null;
        }
    }

    /**
     * Get current located plmn.
     * @param phoneId phoneId
     * @return located plmn
     */
    public String getLocatedPlmn(int phoneId) {
        String plmn = null;

        try {
            IMtkTelephonyEx telephonyEx = getIMtkTelephonyEx();
            if (telephonyEx != null) {
                plmn = telephonyEx.getLocatedPlmn(phoneId);
            }
        } catch (RemoteException ex) {
            Rlog.e(TAG, "fail to getLocatedPlmn due to RemoteException");
        } catch (NullPointerException ex) {
            Rlog.e(TAG, "fail to getLocatedPlmn due to NullPointerException");
        }
        return plmn;
    }

    /**
     * Check if dynamic sim switch without sim reset is supported.
     * @return {@code true} if the device supports dynamic sim switch without sim reset, and {@code false} otherwise.
     * @hide
     */
    public boolean isDssNoResetSupport() {
        if (SystemProperties.get("vendor.ril.simswitch.no_reset_support").equals("1")) {
            Rlog.d(TAG, "return true for isDssNoResetSupport");
            return true;
        }
        Rlog.d(TAG, "return false for isDssNoResetSupport");
        return false;
    }

    /**
     * Get the protocol stack id by slot id, the slot is 0-based, protocol stack id is 1-based
     * @param slot slot id
     * @return protocol stack id
     * @hide
     */
    public int getProtocolStackId(int slot) {
        // The major sim is mapped to PS1, from 93 modem which supports dynamic sim switch without
        // modem reset, the other sims are mapped to PS2~PS4 in ascending order; For the modem
        // before 93, the major sim is switched protocol stack with SIM1.
        int majorSlot = 0;
        try {
            IMtkTelephonyEx telephonyEx = getIMtkTelephonyEx();
            if (telephonyEx != null) {
                majorSlot = telephonyEx.getMainCapabilityPhoneId();
            }
        } catch (RemoteException ex) {
            Rlog.e(TAG, "fail to getMainCapabilityPhoneId due to RemoteException");
        } catch (NullPointerException ex) {
            Rlog.e(TAG, "fail to getMainCapabilityPhoneId due to NullPointerException");
        }
        if (slot == majorSlot) {
            return 1;
        }
        if (isDssNoResetSupport()) {
            if (slot < majorSlot) {
                return slot + 2;
            }
        } else if (slot == 0) {
            return majorSlot + 1;
        }
        return slot + 1;
    }

    // SIM ME LOCK - Start
    /**
     * Get SIM lock policy
     * @return SIM lock policy:
     * When SIM_ME_LOCK_MODE == 3 wil return
     *     SML_SLOT_LOCK_POLICY_UNKNOWN = -1, MD not ready
     *     SML_SLOT_LOCK_POLICY_NONE = 0
     *     SML_SLOT_LOCK_POLICY_ONLY_SLOT1 = 1
     *     SML_SLOT_LOCK_POLICY_ONLY_SLOT2 = 2
     *     SML_SLOT_LOCK_POLICY_ALL_SLOTS_INDIVIDUAL = 3
     *     SML_SLOT_LOCK_POLICY_LK_SLOT1 = 4
     *     SML_SLOT_LOCK_POLICY_LK_SLOT2 = 5
     *     SML_SLOT_LOCK_POLICY_LK_SLOTA = 6
     *     SML_SLOT_LOCK_POLICY_LK_SLOTA_RESTRICT_INVALID_CS = 7
     *     SML_SLOT_LOCK_POLICY_LK_SLOTA_RESTRICT_INVALID_ECC_FOR_VALID_NO_SERVICE = 9
     *     SML_SLOT_LOCK_POLICY_LK_SLOTA_RESTRICT_INVALID_VOICE = 10
     *     SML_SLOT_LOCK_POLICY_ALL_SLOTS_INDIVIDUAL_AND_RSU_VZW = 11
     *     SML_SLOT_LOCK_POLICY_LEGACY = 255
     * When SIM_ME_LOCK_MODE != 3 will only return
     *     SML_SLOT_LOCK_POLICY_NONE = 0
     */
    public int getSimLockPolicy() {
        if (mIsSmlLockMode) {
            int policy = SystemProperties.getInt(PROPERTY_SIM_SLOT_LOCK_POLICY, -1);
            // TODO: remove log after IT Done
            Rlog.d(TAG, "getSimLockPolicy: " + policy);
            return policy;
        } else {
            return MtkIccCardConstants.SML_SLOT_LOCK_POLICY_NONE;
        }
    }

    /**
     * Not support check by subid because difference combination,
     * difference capability exists.
     * By slot, and the service capability is the one should be, not real.
     * @param slotId:the slotId for the SIM
     * @return
     * When SIM_ME_LOCK_MODE == 3 wil return
     *     SML_SLOT_LOCK_POLICY_SERVICE_CAPABILITY_UNKNOWN = -1, MD not ready
     *     SML_SLOT_LOCK_POLICY_SERVICE_CAPABILITY_FULL = 0
     *     SML_SLOT_LOCK_POLICY_SERVICE_CAPABILITY_CS_ONLY = 1
     *     SML_SLOT_LOCK_POLICY_SERVICE_CAPABILITY_PS_ONLY = 2
     *     SML_SLOT_LOCK_POLICY_SERVICE_CAPABILITY_ECC_ONLY = 3
     *     SML_SLOT_LOCK_POLICY_SERVICE_CAPABILITY_NO_SERVICE = 4, No sim inserted
     * When SIM_ME_LOCK_MODE != 3 will only return
     *     SML_SLOT_LOCK_POLICY_SERVICE_CAPABILITY_FULL = 0
     */
    public int getShouldServiceCapability(int slotId) {
        if (mIsSmlLockMode) {
            if (slotId < 0 || slotId >= PROPERTY_SIM_SLOT_LOCK_SERVICE_CAPABILITY.length) {
                Rlog.e(TAG, "getShouldServiceCapability: invalid slotId: " + slotId);
                return MtkIccCardConstants.SML_SLOT_LOCK_POLICY_SERVICE_CAPABILITY_NO_SERVICE;
            }
            int capability = SystemProperties.getInt(
                    PROPERTY_SIM_SLOT_LOCK_SERVICE_CAPABILITY[slotId], -1);
            // TODO: remove log after IT Done
            Rlog.d(TAG, "getShouldServiceCapability: " + capability + ",slotId: " + slotId);
            return capability;
        } else {
            return MtkIccCardConstants.SML_SLOT_LOCK_POLICY_SERVICE_CAPABILITY_FULL;
        }
    }

    /**
     * By slot, and valid means it meet the lock policy.
     * @param slotId:the slotId for the SIM
     * @return
     * When SIM_ME_LOCK_MODE == 3 wil return
     *     SML_SLOT_LOCK_POLICY_VALID_CARD_UNKNOWN = -1, modem not ready
     *     SML_SLOT_LOCK_POLICY_VALID_CARD_YES = 0
     *     SML_SLOT_LOCK_POLICY_VALID_CARD_NO = 1
     *     SML_SLOT_LOCK_POLICY_VALID_CARD_ABSENT = 2
     * When SIM_ME_LOCK_MODE != 3 will only return
     *     SML_SLOT_LOCK_POLICY_VALID_CARD_YES = 0
     */
    public int checkValidCard(int slotId) {
        if (mIsSmlLockMode) {
            if (slotId < 0 || slotId >= PROPERTY_SIM_SLOT_LOCK_CARD_VALID.length) {
                Rlog.e(TAG, "checkValidCard: invalid slotId " + slotId);
                return MtkIccCardConstants.SML_SLOT_LOCK_POLICY_VALID_CARD_ABSENT;
            }
            int validCard = SystemProperties.getInt(
                    PROPERTY_SIM_SLOT_LOCK_CARD_VALID[slotId], -1);
            // TODO: remove log after IT Done
            Rlog.d(TAG, "checkValidCard: " + validCard + ",slotId: " + slotId);
            return validCard;
        } else {
            return MtkIccCardConstants.SML_SLOT_LOCK_POLICY_VALID_CARD_YES;
        }
    }

    /**
     * To get SIM lock state
     * @return
     * When SIM_ME_LOCK_MODE == 3 wil return
     *     SML_SLOT_LOCK_POLICY_LOCK_STATE_UNKNOWN = -1, MD not ready
     *     SML_SLOT_LOCK_POLICY_LOCK_STATE_YES = 0
     *     SML_SLOT_LOCK_POLICY_LOCK_STATE_NO = 1
     * When SIM_ME_LOCK_MODE != 3 will only return
     *     SML_SLOT_LOCK_POLICY_LOCK_STATE_NO = 1
     */
    public int getSimLockState() {
        if (mIsSmlLockMode) {
            int lockState = SystemProperties.getInt(PROPERTY_SIM_SLOT_LOCK_STATE, -1);
            // TODO: remove log after IT Done
            Rlog.d(TAG, "getSimLockState: " + lockState);
            return lockState;
        } else {
            return MtkIccCardConstants.SML_SLOT_LOCK_POLICY_LOCK_STATE_NO;
        }
    }
    // SIM ME LOCK - End

    /**
     * Returns the phone number string for line 1, for example, the MSISDN
     * for a GSM phone for a particular subscription. Return null if it is unavailable.
     * <p>
     * The default SMS app can also use this.
     *
     * @param subId whose phone number for line 1 is returned
     * @hide
     */
    @RequiresPermission(anyOf = {
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.READ_PHONE_NUMBERS
    })
    public String getLine1PhoneNumber(int subId) {
        String number = null;
        try {
            ITelephony telephony = getITelephony();
            if (telephony != null)
                number = telephony.getLine1NumberForDisplay(subId, mContext.getOpPackageName());
        } catch (RemoteException ex) {
        } catch (NullPointerException ex) {
        }
        if (number != null) {
            return number;
        }
        try {
            IMtkPhoneSubInfoEx info = getMtkSubscriberInfoEx();
            if (info == null)
                return null;
            return info.getLine1PhoneNumberForSubscriber(subId, getOpPackageName());
        } catch (RemoteException ex) {
            return null;
        } catch (NullPointerException ex) {
            // This could happen before phone restarts due to crashing
            return null;
        }
    }

    /**
     * Try to clean up the PDN connection of specific type
     *
     * @param phoneId the phone the request is sent to.
     * @param type the PDN type of the connection to be cleaned up.
     * @return false if exception occurs or true if the request is successfully sent.
     */
    public boolean tearDownPdnByType(int phoneId, String type) {
        try {
            IMtkTelephonyEx telephony = getIMtkTelephonyEx();
            if (telephony == null) {
                Rlog.e(TAG, "tearDownPdnByType: telephony = null");
                return false;
            }
            telephony.tearDownPdnByType(phoneId, type);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "tearDownPdnByType: RemoteException " + ex);
            return false;
        } catch (NullPointerException ex) {
            Rlog.e(TAG, "tearDownPdnByType: NullPointerException " + ex);
            return false;
        }
        return true;
    }

    /**
     * Try to establish the PDN connection of specific type
     *
     * @param phoneId the phone the request is sent to.
     * @param type the PDN type of the connection to be cleaned up.
     * @return false if exception occurs or true if the request is successfully sent.
     */
    public boolean setupPdnByType(int phoneId, String type) {
        try {
            IMtkTelephonyEx telephony = getIMtkTelephonyEx();
            if (telephony == null) {
                Rlog.e(TAG, "setupPdnByType: telephony = null");
                return false;
            }
            telephony.setupPdnByType(phoneId, type);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "setupPdnByType: RemoteException " + ex);
            return false;
        } catch (NullPointerException ex) {
            Rlog.e(TAG, "setupPdnByType: NullPointerException " + ex);
            return false;
        }
        return true;
    }
}
