/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.ims.internal;

// for Data usage
import android.telephony.TelephonyManager;
import android.content.Context;
import android.net.IConnectivityManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkCapabilities;
import android.net.NetworkStats;
import android.os.INetworkManagementService;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.os.PersistableBundle;
import java.util.Objects;
import static android.net.NetworkStats.UID_ALL;
import static android.net.NetworkStats.INTERFACES_ALL;

import com.mediatek.ims.common.SubscriptionManagerHelper;

// for judge if need count usage
import android.telephony.CarrierConfigManager;
import android.os.SystemProperties;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;

import android.os.RegistrantList;

import android.content.Context;

import android.util.Log;

import com.mediatek.ims.internal.ImsVTProviderUtil;

public class ImsVTUsageManager {

    public static class ImsVTUsage {

        private long                                mLTEUsage;
        private long                                mWifiUsage;

        public ImsVTUsage() {
            mLTEUsage = 0;
            mWifiUsage = 0;
        }

        public long getLteUsage() {
            return mLTEUsage;
        }

        public long getWifiUsage() {
            return mWifiUsage;
        }

        public void setLteUsage(long usage) {
            mLTEUsage = usage;
        }

        public void setWifiUsage(long usage) {
            mWifiUsage = usage;
        }
    }

    static final String                         TAG = "ImsVT Usage";

    public int                                  mId;
    protected int                               mSimId;

    private Context                             mContext;

    private ImsVTUsage                          mInitialUsage;
    private ImsVTUsage                          mUsage;
    public ImsVTProviderUtil                    mVTProviderUtil = ImsVTProviderUtil.getInstance();

    private boolean                             mNeedReportDataUsage = true;
    private RegistrantList mDataUsageUpdateRegistrants = new RegistrantList();

    public ImsVTUsageManager() {
        mUsage = new ImsVTUsage();
    }

    public void setId(int id) {
        mId = id;
    }

    public void setSimId(int simId) {
        mSimId = simId;
    }

    public void setInitUsage(ImsVTUsage initUsage) {
        mInitialUsage = initUsage;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public ImsVTUsage requestCallDataUsage() {

        Log.d(TAG, "[ID=" + mId + "] [onRequestCallDataUsage] Start");

        if (!canRequestDataUsage()) {
            return null;
        }

        // ====================================================================
        // get IMS/WIFI Interface and data from CONNECTIVITY SERVICE
        //
        String mActiveImsIface = "";
        String mActiveWifiIface = "";

        String subIdStr = "" + SubscriptionManagerHelper.getSubIdUsingPhoneId(mSimId);
        ConnectivityManager sConnMgr = (ConnectivityManager) mVTProviderUtil.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network [] nets = sConnMgr.getAllNetworks();
        NetworkInfo nwInfo;

        if (null != nets) {

            for (Network net : nets) {

                nwInfo = sConnMgr.getNetworkInfo(net);

                if (nwInfo != null && nwInfo.isConnected() ) {

                    NetworkCapabilities netCap = sConnMgr.getNetworkCapabilities(net);

                    if (null == netCap) {
                        Log.d(TAG, "[onRequestCallDataUsage] netCap = null");
                        continue;
                    }

                    Log.d(TAG, "[onRequestCallDataUsage] nwInfo:" + nwInfo.toString() +
                            ", checking net=" + net + " cap=" + netCap);

                    if (null != sConnMgr.getLinkProperties(net)) {
                        if (netCap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            mActiveWifiIface = sConnMgr.getLinkProperties(net).getInterfaceName();
                            Log.d(TAG, "[onRequestCallDataUsage] mActiveWifiIface=" + mActiveWifiIface);

                        } else if (netCap.hasCapability(NetworkCapabilities.NET_CAPABILITY_IMS)) {
                            String networkSpecifier = "" + netCap.getNetworkSpecifier();
                            if (!subIdStr.equals(networkSpecifier)) {
                                Log.d(TAG, "[onRequestCallDataUsage] Get Ims interface with different sub, " +
                                        "net=" + net + " specifier=" + networkSpecifier + " sub=" + subIdStr +
                                        "specifier.length=" + networkSpecifier.length() + " sub.length=" +
                                        subIdStr.length());
                                continue;
                            }
                            mActiveImsIface = sConnMgr.getLinkProperties(net).getInterfaceName();
                            Log.d(TAG, "[onRequestCallDataUsage] mActiveImsIface=" + mActiveImsIface);

                        } else {
                            Log.d(TAG, "[onRequestCallDataUsage] netCap neither contain WIF nor LTE.");
                        }
                    } else {
                        Log.e(TAG, "[onRequestCallDataUsage] sConnMgr.getLinkProperties(net) = NULL");
                    }
                }
            }

            if ("" == mActiveImsIface) {
                Log.e(TAG, "[onRequestCallDataUsage] mActiveImsIface is empty");
                return null;
            }

        } else {
            Log.d(TAG, "[onRequestCallDataUsage] getAllNetworks returns null.");
            return null;
        }

        // ====================================================================
        // Calculate the usage of ViLTE IMS
        //
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        INetworkManagementService mNetworkManager = INetworkManagementService.Stub.asInterface(b);

        try {
            // should use UID_ALL = -1
            NetworkStats uidSnapshot = mNetworkManager.getNetworkStatsUidDetail(UID_ALL, INTERFACES_ALL);

            int VILTE_UID = 1000;
            long usage_ImsTaginImsInterface = getTaggedSnapshot(uidSnapshot, VILTE_UID, mActiveImsIface, ImsVTProviderUtil.TAG_VILTE_MOBILE + mId);
            //long usage_WifiTaginImsInterface = getTaggedSnapshot(uidSnapshot, VILTE_UID, mActiveImsIface, ImsVTProviderUtil.TAG_VILTE_WIFI + mId);
            long usage_WifiTaginWifiInterface = getTaggedSnapshot(uidSnapshot, VILTE_UID, mActiveWifiIface, ImsVTProviderUtil.TAG_VILTE_WIFI + mId);

            mUsage.setLteUsage(usage_ImsTaginImsInterface- mInitialUsage.getLteUsage());
            mUsage.setWifiUsage(usage_WifiTaginWifiInterface- mInitialUsage.getWifiUsage());

            mVTProviderUtil.usageSet(mId, usage_ImsTaginImsInterface, ImsVTProviderUtil.TAG_VILTE_MOBILE);
            mVTProviderUtil.usageSet(mId, usage_WifiTaginWifiInterface, ImsVTProviderUtil.TAG_VILTE_WIFI);

            Log.d(TAG, "[ID=" + mId + "] [onRequestCallDataUsage] Finish (VIWIFI usage:" +
                    usage_WifiTaginWifiInterface + ")");

            return mUsage;

        } catch (RemoteException e) {
            Log.d(TAG, "Exception:" + e);
            return null;
        }
    }

    private boolean canRequestDataUsage() {
        Log.d(TAG, "[canRequestDataUsage]");

        boolean forceRequest = SystemProperties.get("persist.vendor.vt.data_simulate").equals("1");
        if (forceRequest) {
            return true;
        }

        int subId = SubscriptionManagerHelper.getSubIdUsingPhoneId(mSimId);

        boolean ignoreDataEnabledChanged = getBooleanCarrierConfig(
                mVTProviderUtil.mContext,
                CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS,
                subId);

        if (mNeedReportDataUsage && ignoreDataEnabledChanged) {

            Log.d(TAG, "[canRequestDataUsage] set dataUsage as false");
            mNeedReportDataUsage = false;
        }

        return mNeedReportDataUsage;
    }

    private boolean getBooleanCarrierConfig(Context context, String key, int subId) {

        CarrierConfigManager configManager = (CarrierConfigManager) context.getSystemService(Context.CARRIER_CONFIG_SERVICE);

        PersistableBundle carrierConfig = null;

        if (configManager != null) {
            carrierConfig = configManager.getConfigForSubId(subId);
        }

        if (carrierConfig != null) {
            return carrierConfig.getBoolean(key);
        } else {
            // Return static default defined in CarrierConfigManager.
            return CarrierConfigManager.getDefaultConfig().getBoolean(key);
        }
    }

    // Example of network info file:
    //
    //     idx              2
    //     iface            ccmni1
    //     acct_tag_hex     0x0
    //     uid_tag_int      0
    //     cnt_set          0           // foreground or background
    //     rx_bytes         3085
    //     rx_packets       15
    //     tx_bytes         827
    //     tx_packets       15
    //     rx_tcp_bytes     366
    //     rx_tcp_packets   6
    //     rx_udp_bytes     2719
    //     rx_udp_packets   9
    //     rx_other_bytes   0
    //     rx_other_packets 0
    //     tx_tcp_bytes     252
    //     tx_tcp_packets   6
    //     tx_udp_bytes     575
    //     tx_udp_packets   9
    //     tx_other_bytes   0
    //     tx_other_packets 0
    private long getTaggedSnapshot(NetworkStats uidSnapshot, int match_uid, String iface, int tag) {

        Log.i(TAG, "getTaggedSnapshot match_uid:" + match_uid + ", iface:" + iface + ", tag:" + NetworkStats.tagToString(tag));

        long TotalBytes = 0;
        NetworkStats.Entry entry = null;

        for (int j = 0; j < uidSnapshot.size(); j++) {

            entry = uidSnapshot.getValues(j, entry);

            if (Objects.equals(entry.iface, iface) && entry.uid == match_uid && entry.tag == tag) {

                Log.i(TAG, "getTaggedSnapshot entry:" + entry.toString());

                TotalBytes += entry.rxBytes;
                TotalBytes += entry.txBytes;
                Log.i(TAG, "getTaggedSnapshot entry.rxBytes:" + Long.toString(entry.rxBytes) +
                        ", entry.txBytes:" + Long.toString(entry.txBytes));
            }
        }
        Log.i(TAG, "TotalBytes:" + Long.toString(TotalBytes));
        return TotalBytes;
    }
    //
}

