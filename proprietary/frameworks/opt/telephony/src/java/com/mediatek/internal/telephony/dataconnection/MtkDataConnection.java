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

import static android.net.NetworkPolicyManager.OVERRIDE_CONGESTED;
import static android.net.NetworkPolicyManager.OVERRIDE_UNMETERED;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.INetworkManagementEventObserver;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkMisc;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.StringNetworkSpecifier;
import android.os.AsyncResult;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataService;
import android.telephony.data.DataServiceCallback;
import android.text.TextUtils;
import android.util.Pair;
import android.util.StatsLog;

import static android.system.OsConstants.IFA_F_TEMPORARY;
import static android.system.OsConstants.RT_SCOPE_UNIVERSE;

import com.android.internal.telephony.CarrierSignalAgent;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.RetryManager;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyDevController;
import com.android.internal.telephony.dataconnection.ApnContext;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.dataconnection.DataConnection;
import com.android.internal.telephony.dataconnection.DataServiceManager;
import com.android.internal.telephony.dataconnection.DcAsyncChannel;
import com.android.internal.telephony.dataconnection.DcController;
import com.android.internal.telephony.dataconnection.DcFailCause;
import com.android.internal.telephony.dataconnection.DcTesterFailBringUpAll;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.util.Protocol;
import com.android.server.net.BaseNetworkObserver;

import com.mediatek.common.carrierexpress.CarrierExpressManager;
import com.mediatek.internal.telephony.MtkDctConstants;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkHardwareConfig;
import com.mediatek.internal.telephony.MtkPhoneConstants;
import com.mediatek.internal.telephony.MtkRILConstants;
import com.mediatek.internal.telephony.MtkTelephonyDevController;
import com.mediatek.internal.telephony.OpTelephonyCustomizationFactoryBase;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
// M: Data Framework - Data Retry enhancement
import com.mediatek.internal.telephony.dataconnection.DcFailCauseManager;

import mediatek.telephony.MtkCarrierConfigManager;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

public class MtkDataConnection extends DataConnection {
    private static final boolean DBG = true;
    private static final boolean VDBG = SystemProperties.get("ro.build.type").equals("eng") ?
            true : false;

    private OpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory = null;
    private IDataConnectionExt mDataConnectionExt = null;

    private SubscriptionController mSubController = SubscriptionController.getInstance();

    // M: Data Framework - Data Retry enhancement
    protected DcFailCauseManager mDcFcMgr;

    private String mActionRetry;
    private AlarmManager mAlarmManager;
    private static final String INTENT_RETRY_ALARM_WHAT = "what";
    private static final String INTENT_RETRY_ALARM_TAG = "tag";
    private int mRetryCount = 0;

    private final INetworkManagementService mNetworkManager;
    private String mInterfaceName = null;
    private long mValid; // M: add for ipv6 RA failed feature
    private static final int RA_INITIAL_FAIL = -1;
    private static final int RA_REFRESH_FAIL = -2;
    private static final int RA_GET_IPV6_VALID_FAIL = -1000;
    // M: PS/CS concurrent @{
    private boolean mIsInVoiceCall = false;
    private boolean mIsSupportConcurrent = false;
    // M: PS/CS concurrent @}
    private AddressInfo mGlobalV6AddrInfo = null;
    // M: Query RA capability
    private TelephonyDevController mTelDevController = MtkTelephonyDevController.getInstance();

    // M: [OD over ePDG] @{
    private int mRat; // The real rat in which data connection activated.
    // M: [OD over ePDG] @}

    // ***** Event codes for driving the state machine, package visible for Dcc
    static final int EVENT_IPV4_ADDRESS_REMOVED = BASE + CMD_TO_STRING_COUNT;
    static final int EVENT_IPV6_ADDRESS_REMOVED = BASE + CMD_TO_STRING_COUNT + 1;
    static final int EVENT_ADDRESS_REMOVED = BASE + CMD_TO_STRING_COUNT + 2;
    static final int EVENT_VOICE_CALL = BASE + CMD_TO_STRING_COUNT + 3;
    static final int EVENT_FALLBACK_RETRY_CONNECTION = BASE + CMD_TO_STRING_COUNT + 4;
    static final int EVENT_IPV6_ADDRESS_UPDATED = BASE + CMD_TO_STRING_COUNT + 5;
    static final int EVENT_GET_DATA_CALL_LIST = BASE + CMD_TO_STRING_COUNT + 6;

    static {
        //M: extend AOSP
        sCmdToString = new String[EVENT_GET_DATA_CALL_LIST - BASE + 1];
        sCmdToString[EVENT_CONNECT - BASE] = "EVENT_CONNECT";
        sCmdToString[EVENT_SETUP_DATA_CONNECTION_DONE - BASE] =
                "EVENT_SETUP_DATA_CONNECTION_DONE";
        sCmdToString[EVENT_DEACTIVATE_DONE - BASE] = "EVENT_DEACTIVATE_DONE";
        sCmdToString[EVENT_DISCONNECT - BASE] = "EVENT_DISCONNECT";
        sCmdToString[EVENT_RIL_CONNECTED - BASE] = "EVENT_RIL_CONNECTED";
        sCmdToString[EVENT_DISCONNECT_ALL - BASE] = "EVENT_DISCONNECT_ALL";
        sCmdToString[EVENT_DATA_STATE_CHANGED - BASE] = "EVENT_DATA_STATE_CHANGED";
        sCmdToString[EVENT_TEAR_DOWN_NOW - BASE] = "EVENT_TEAR_DOWN_NOW";
        sCmdToString[EVENT_LOST_CONNECTION - BASE] = "EVENT_LOST_CONNECTION";
        sCmdToString[EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED - BASE] =
                "EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED";
        sCmdToString[EVENT_DATA_CONNECTION_ROAM_ON - BASE] = "EVENT_DATA_CONNECTION_ROAM_ON";
        sCmdToString[EVENT_DATA_CONNECTION_ROAM_OFF - BASE] = "EVENT_DATA_CONNECTION_ROAM_OFF";
        sCmdToString[EVENT_BW_REFRESH_RESPONSE - BASE] = "EVENT_BW_REFRESH_RESPONSE";
        sCmdToString[EVENT_DATA_CONNECTION_VOICE_CALL_STARTED - BASE] =
                "EVENT_DATA_CONNECTION_VOICE_CALL_STARTED";
        sCmdToString[EVENT_DATA_CONNECTION_VOICE_CALL_ENDED - BASE] =
                "EVENT_DATA_CONNECTION_VOICE_CALL_ENDED";
        sCmdToString[EVENT_DATA_CONNECTION_OVERRIDE_CHANGED - BASE] =
                "EVENT_DATA_CONNECTION_OVERRIDE_CHANGED";
        sCmdToString[EVENT_KEEPALIVE_STATUS - BASE] = "EVENT_KEEPALIVE_STATUS";
        sCmdToString[EVENT_KEEPALIVE_STARTED - BASE] = "EVENT_KEEPALIVE_STARTED";
        sCmdToString[EVENT_KEEPALIVE_STOPPED - BASE] = "EVENT_KEEPALIVE_STOPPED";
        sCmdToString[EVENT_KEEPALIVE_START_REQUEST - BASE] = "EVENT_KEEPALIVE_START_REQUEST";
        sCmdToString[EVENT_KEEPALIVE_STOP_REQUEST - BASE] = "EVENT_KEEPALIVE_STOP_REQUEST";
        sCmdToString[EVENT_LINK_CAPACITY_CHANGED - BASE] = "EVENT_LINK_CAPACITY_CHANGED";
        // M @{
        sCmdToString[EVENT_IPV4_ADDRESS_REMOVED - BASE] = "EVENT_IPV4_ADDRESS_REMOVED";
        sCmdToString[EVENT_IPV6_ADDRESS_REMOVED - BASE] = "EVENT_IPV6_ADDRESS_REMOVED";
        sCmdToString[EVENT_ADDRESS_REMOVED - BASE] = "EVENT_ADDRESS_REMOVED";
        sCmdToString[EVENT_VOICE_CALL - BASE] = "EVENT_VOICE_CALL";
        sCmdToString[EVENT_FALLBACK_RETRY_CONNECTION - BASE] = "EVENT_FALLBACK_RETRY_CONNECTION";
        sCmdToString[EVENT_IPV6_ADDRESS_UPDATED - BASE] = "EVENT_IPV6_ADDRESS_UPDATED";
        sCmdToString[EVENT_GET_DATA_CALL_LIST - BASE] = "EVENT_GET_DATA_CALL_LIST";
        // M @}
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                // Our mActionXxxx's could be null when disposed this could match an empty action.
                log("onReceive: ignore empty action='" + action + "'");
                return;
            }
            if (TextUtils.equals(action, mActionRetry)) {
                if (!intent.hasExtra(INTENT_RETRY_ALARM_WHAT)) {
                    throw new RuntimeException(mActionRetry + " has no INTENT_RETRY_ALRAM_WHAT");
                }
                if (!intent.hasExtra(INTENT_RETRY_ALARM_TAG)) {
                    throw new RuntimeException(mActionRetry + " has no INTENT_RETRY_ALRAM_TAG");
                }
                int what = intent.getIntExtra(INTENT_RETRY_ALARM_WHAT, Integer.MAX_VALUE);
                int tag = intent.getIntExtra(INTENT_RETRY_ALARM_TAG, Integer.MAX_VALUE);
                if (DBG) {
                    log("onReceive: action=" + action
                            + " sendMessage(what:" + getWhatToString(what)
                            + ", tag:" + tag + ")");
                }
                sendMessage(obtainMessage(what, tag, 0));
            } else if (TextUtils.equals(action, CarrierExpressManager
                    .ACTION_OPERATOR_CONFIG_CHANGED)) {
                mIsOp20 = "OP20".equals(SystemProperties.get("persist.vendor.operator.optr", ""));
            } else {
                if (DBG) {
                    log("onReceive: unknown action=" + action);
                }
            }
        }
    };

    @Override
    protected void checkSetMtu(ApnSetting apn, LinkProperties lp) {
        if (lp != null && apn != null) {
            if ("OP12".equals(SystemProperties.get("persist.vendor.operator.optr", ""))
                    && TextUtils.equals(apn.apn, "")
                    && TextUtils.equals(apn.types[0], PhoneConstants.APN_TYPE_EMERGENCY)) {
                lp.setMtu(1428);
                if (DBG) log("For OP12, default emergency MTU set by APN to: 1428");
                return;
            }
        }
        super.checkSetMtu(apn, lp);
    }

    //***** Constructor (NOTE: uses dcc.getHandler() as its Handler)
    public MtkDataConnection(Phone phone, String name, int id,
            DcTracker dct, DataServiceManager dataServiceManager,
            DcTesterFailBringUpAll failBringUpAll,
            DcController dcc) {
        super(phone, name, id, dct, dataServiceManager, failBringUpAll, dcc);

        // M @{
        // M: [OD over ePDG] @{
        setConnectionRat(MtkPhoneConstants.RAT_TYPE_MOBILE_3GPP, "construct instance");
        // M: [OD over ePDG] @}

        try {
            mTelephonyCustomizationFactory =
                    OpTelephonyCustomizationUtils.getOpFactory(mPhone.getContext());
            mDataConnectionExt =
                    mTelephonyCustomizationFactory.makeDataConnectionExt(mPhone.getContext());
        } catch (Exception e) {
            if (DBG) {
                log("mDataConnectionExt init fail");
            }
            e.printStackTrace();
        }

        // M: Data Framework - Data Retry enhancement
        // Initialize data connection fail cause manager
        mDcFcMgr = DcFailCauseManager.getInstance(mPhone);

        // M: IPv6 RA @{
        log("get INetworkManagementService");
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        mNetworkManager = INetworkManagementService.Stub.asInterface(b);
        // M: IPv6 RA @}
        mAlarmManager = (AlarmManager) mPhone.getContext().getSystemService(Context.ALARM_SERVICE);
        mActionRetry = getClass().getCanonicalName() + "." + getName() + ".action_retry";
        resetRetryCount();
        // M @}
    }

    @Override
    protected void tearDownData(Object o) {
        int discReason = DataService.REQUEST_REASON_NORMAL;
        ApnContext apnContext = null;
        if ((o != null) && (o instanceof DisconnectParams)) {
            DisconnectParams dp = (DisconnectParams)o;
            apnContext = dp.mApnContext;
            if (TextUtils.equals(dp.mReason, Phone.REASON_RADIO_TURNED_OFF)) {
                discReason = DataService.REQUEST_REASON_SHUTDOWN;
            // M @{
            } else if (TextUtils.equals(dp.mReason, MtkGsmCdmaPhone.REASON_RA_FAILED)) {
                if (mValid == RA_INITIAL_FAIL) {
                    discReason = MtkRILConstants.DEACTIVATE_REASON_RA_INITIAL_FAIL;
                } else if (mValid == RA_REFRESH_FAIL) {
                    discReason = MtkRILConstants.DEACTIVATE_REASON_RA_REFRESH_FAIL;
                }
            } else if (TextUtils.equals(dp.mReason, MtkGsmCdmaPhone.REASON_PCSCF_ADDRESS_FAILED)) {
                /// M: P-CSCF error handling.
                discReason = MtkRILConstants.DEACTIVATE_REASON_NO_PCSCF;
            } else if (TextUtils.equals(dp.mReason, Phone.REASON_APN_CHANGED)) {
                discReason = MtkRILConstants.DEACTIVATE_REASON_APN_CHANGED;
            }
            // M @}
        }

        String str = "tearDownData. mCid=" + mCid + ", reason=" + discReason;
        if (DBG) log(str);
        if (apnContext != null) apnContext.requestLog(str);
        mDataServiceManager.deactivateDataCall(mCid, discReason,
                obtainMessage(EVENT_DEACTIVATE_DONE, mTag, 0, o));
    }

    @Override
    protected void clearSettings() {
        super.clearSettings();
        if (DBG) log("clearSettings");

        // M
        mGlobalV6AddrInfo = null;
        // M
        resetRetryCount();
        // M: [OD over ePDG]
        setConnectionRat(MtkPhoneConstants.RAT_TYPE_MOBILE_3GPP, "clear setting");
    }

    @Override
    protected SetupResult onSetupConnectionCompleted(@DataServiceCallback.ResultCode int resultCode,
                                                   DataCallResponse response,
                                                   ConnectionParams cp) {
        SetupResult result;

        if (cp.mTag != mTag) {
            if (DBG) {
                log("onSetupConnectionCompleted stale cp.tag=" + cp.mTag + ", mtag=" + mTag);
            }
            result = SetupResult.ERROR_STALE;
        } else if (resultCode == DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE) {
            result = SetupResult.ERROR_RADIO_NOT_AVAILABLE;
            result.mFailCause = DcFailCause.RADIO_NOT_AVAILABLE;
        } else if (response.getStatus() != 0) {
            if (response.getStatus() == DcFailCause.RADIO_NOT_AVAILABLE.getErrorCode()) {
                result = SetupResult.ERROR_RADIO_NOT_AVAILABLE;
                result.mFailCause = DcFailCause.RADIO_NOT_AVAILABLE;
            } else {
                result = SetupResult.ERROR_DATA_SERVICE_SPECIFIC_ERROR;
                result.mFailCause = DcFailCause.fromInt(response.getStatus());
            }
        } else {
            if (DBG) log("onSetupConnectionCompleted received successful DataCallResponse");
            mCid = response.getCallId();

            mPcscfAddr = response.getPcscfs().toArray(new String[response.getPcscfs().size()]);

            // M: [OD over ePDG] @{
            setConnectionRat(decodeRat(response.getActive()), "data call response");
            // M: [OD over ePDG] @}

            result = updateLinkProperty(response).setupResult;

            // M @{
            mInterfaceName = response.getIfname();
            log("onSetupConnectionCompleted: ifname-" + mInterfaceName);
            // M @}
        }

        return result;
    }

    @Override
    protected NetworkCapabilities getNetworkCapabilities() {
        NetworkCapabilities result = new NetworkCapabilities();
        result.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);

        /// M: [OD over ePDG] @{
        ApnSetting apnSetting = mApnSetting;
        if (mConnectionParams != null && mConnectionParams.mApnContext != null &&
                mRat == MtkPhoneConstants.RAT_TYPE_WIFI) {
            ArrayList<ApnSetting> wifiApnList = ((MtkApnContext) mConnectionParams.mApnContext)
                    .getWifiApns();
            if (wifiApnList != null) {
                for (ApnSetting tApnSetting: wifiApnList) {
                    if (tApnSetting != null && !tApnSetting.apn.equals("")) {
                        log("makeNetworkCapabilities: apn: " + tApnSetting.apn);
                        apnSetting = tApnSetting;
                    }
                }
            }
        }
        /// @}

        if (apnSetting != null) {
            // M: [OD over ePDG] use apnSetting instead of mApnSetting
            for (String type : apnSetting.types) {
                if (!mRestrictedNetworkOverride
                        && (mConnectionParams != null && mConnectionParams.mUnmeteredUseOnly)
                        && MtkApnSetting.isMeteredApnType(type, mPhone)) {
                    log("Dropped the metered " + type + " for the unmetered data call.");
                    continue;
                }
                switch (type) {
                    case PhoneConstants.APN_TYPE_ALL: {
                        /// M: conditionally added INTERNET capability @{
                        if (isDefaultDataSubPhone(mPhone)) {
                            result.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                        }
                        /// @}
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_MMS);
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_SUPL);
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_FOTA);
                        /// M: Let the APN_TYPE_ALL skip the "IMS" & "EMERGENCY" capability @{
                        /// result.addCapability(NetworkCapabilities.NET_CAPABILITY_IMS);
                        /// @}
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_CBS);
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_IA);
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_DUN);
                        /// M @{
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_WAP);
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_XCAP);
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_RCS);
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_BIP);
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_VSIM);
                        /// @}
                        break;
                    }
                    case PhoneConstants.APN_TYPE_DEFAULT: {
                        /// M: conditionally added INTERNET capability @{
                        if (isDefaultDataSubPhone(mPhone)) {
                            result.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                        }
                        /// @}
                        break;
                    }
                    case PhoneConstants.APN_TYPE_MMS: {
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_MMS);
                        break;
                    }
                    case PhoneConstants.APN_TYPE_SUPL: {
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_SUPL);
                        break;
                    }
                    case PhoneConstants.APN_TYPE_DUN: {
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_DUN);
                        break;
                    }
                    case PhoneConstants.APN_TYPE_FOTA: {
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_FOTA);
                        break;
                    }
                    case PhoneConstants.APN_TYPE_IMS: {
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_IMS);
                        break;
                    }
                    case PhoneConstants.APN_TYPE_CBS: {
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_CBS);
                        break;
                    }
                    case PhoneConstants.APN_TYPE_IA: {
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_IA);
                        break;
                    }
                    case PhoneConstants.APN_TYPE_EMERGENCY: {
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_EIMS);
                        break;
                    }
                    /// M @{
                    case MtkPhoneConstants.APN_TYPE_WAP: {
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_WAP);
                        break;
                    }
                    case MtkPhoneConstants.APN_TYPE_XCAP: {
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_XCAP);
                        break;
                    }
                    case MtkPhoneConstants.APN_TYPE_RCS: {
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_RCS);
                        break;
                    }
                    case MtkPhoneConstants.APN_TYPE_BIP: {
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_BIP);
                        break;
                    }
                    case MtkPhoneConstants.APN_TYPE_VSIM: {
                        result.addCapability(NetworkCapabilities.NET_CAPABILITY_VSIM);
                        break;
                    }
                    case MtkPhoneConstants.APN_TYPE_PREEMPT: {
                        for (ApnContext apnContext : mApnContexts.keySet()) {
                            if (TextUtils.equals(MtkPhoneConstants.APN_TYPE_PREEMPT,
                                    apnContext.getApnType())) {
                                result.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                                break;
                            }
                        }
                        break;
                    }
                    /// @}
                    default:
                }
            }

            addInternetCapForDunOnlyType(apnSetting, result);

            // Mark NOT_METERED in the following cases,
            // 1. All APNs in APN settings are unmetered.
            // 2. The non-restricted data and is intended for unmetered use only.
            if (((mConnectionParams != null && mConnectionParams.mUnmeteredUseOnly)
                    && !mRestrictedNetworkOverride)
                    // M: [OD over ePDG] use apnSetting instead of mApnSetting
                    || !apnSetting.isMetered(mPhone)) {
                result.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
            } else {
                result.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
            }

            result.maybeMarkCapabilitiesRestricted();
        }
        if (mRestrictedNetworkOverride) {
            result.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
            // don't use dun on restriction-overriden networks.
            result.removeCapability(NetworkCapabilities.NET_CAPABILITY_DUN);
        }

        int up = 14;
        int down = 14;
        switch (mRilRat) {
            case ServiceState.RIL_RADIO_TECHNOLOGY_GPRS: up = 80; down = 80; break;
            case ServiceState.RIL_RADIO_TECHNOLOGY_EDGE: up = 59; down = 236; break;
            case ServiceState.RIL_RADIO_TECHNOLOGY_UMTS: up = 384; down = 384; break;
            case ServiceState.RIL_RADIO_TECHNOLOGY_IS95A: // fall through
            case ServiceState.RIL_RADIO_TECHNOLOGY_IS95B: up = 14; down = 14; break;
            case ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_0: up = 153; down = 2457; break;
            case ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_A: up = 1843; down = 3174; break;
            case ServiceState.RIL_RADIO_TECHNOLOGY_1xRTT: up = 100; down = 100; break;
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSDPA: up = 2048; down = 14336; break;
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSUPA: up = 5898; down = 14336; break;
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSPA: up = 5898; down = 14336; break;
            case ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_B: up = 1843; down = 5017; break;
            case ServiceState.RIL_RADIO_TECHNOLOGY_LTE: up = 51200; down = 102400; break;
            case ServiceState.RIL_RADIO_TECHNOLOGY_LTE_CA: up = 51200; down = 102400; break;
            case ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD: up = 153; down = 2516; break;
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSPAP: up = 11264; down = 43008; break;
            default:
        }
        result.setLinkUpstreamBandwidthKbps(up);
        result.setLinkDownstreamBandwidthKbps(down);

        result.setNetworkSpecifier(new StringNetworkSpecifier(Integer.toString(mPhone.getSubId())));

        result.setCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING,
                !mPhone.getServiceState().getDataRoaming());

        result.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED);

        // Override values set above when requested by policy
        if ((mSubscriptionOverride & OVERRIDE_UNMETERED) != 0) {
            result.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        }
        if ((mSubscriptionOverride & OVERRIDE_CONGESTED) != 0) {
            result.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED);
        }

        return result;
    }

    private boolean mIsOp20 = "OP20".equals(SystemProperties.get(
                "persist.vendor.operator.optr", ""))
            || ("OP20".equals(SystemProperties.get("ril.fwk.test.optr", ""))
                && "eng".equals(SystemProperties.get("ro.build.type", "")));

    private void addInternetCapForDunOnlyType(ApnSetting apn, NetworkCapabilities nc) {
        if (mIsOp20 && (apn.types.length == 1)) {
            boolean isDunOnly = TextUtils.equals(PhoneConstants.APN_TYPE_DUN,
                    apn.types[0]);
            boolean isDunApn = apn.apn.contains("pamsn");

            if (isDunOnly && isDunApn) {
                switch (mRilRat) {
                    case ServiceState.RIL_RADIO_TECHNOLOGY_IS95A:
                    case ServiceState.RIL_RADIO_TECHNOLOGY_IS95B:
                    case ServiceState.RIL_RADIO_TECHNOLOGY_1xRTT:
                    case ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_0:
                    case ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_A:
                    case ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_B:
                        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                        break;
                    default:
                       break;
                }
            }
        }
    }

    @Override
    protected void updateNetworkInfoSuspendState() {
        final NetworkInfo.DetailedState oldState = mNetworkInfo.getDetailedState();

        // this is only called when we are either connected or suspended.  Decide which.
        if (mNetworkAgent == null) {
            Rlog.e(getName(), "Setting suspend state without a NetworkAgent");
        }

        // if we are not in-service change to SUSPENDED
        final ServiceStateTracker sst = mPhone.getServiceStateTracker();
        // M: PS/CS concurrent @{
        final boolean bNwNeedSuspended = isNwNeedSuspended();
        if (DBG) {
            log("updateNetworkInfoSuspendState: oldState = " + oldState
                    + ", currentDataConnectionState = " + sst.getCurrentDataConnectionState()
                    + ", bNwNeedSuspended = " + bNwNeedSuspended);
        }
        // M: PS/CS concurrent @}
        if (sst.getCurrentDataConnectionState() != ServiceState.STATE_IN_SERVICE) {
            // M: PS/CS concurrent @{
            // CSFB will cause the PS state is not in service for a while,
            // so need to check mIsInVoiceCall, if true, ignore to suspend the network.
            if (!mIsInVoiceCall && !MtkDcHelper.isImsOrEmergencyApn(getApnType())
                    && !MtkDcHelper.hasVsimApn(getApnType())) {
                mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.SUSPENDED, null,
                        mNetworkInfo.getExtraInfo());
            }
            // M: PS/CS concurrent @}
        } else {
            // check for voice call and concurrency issues
            // M: PS/CS concurrent
            if (bNwNeedSuspended) {
                mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.SUSPENDED, null,
                        mNetworkInfo.getExtraInfo());
                return;
            }
            mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null,
                    mNetworkInfo.getExtraInfo());
        }
    }

    /**
     * The parent state for all other states.
     */
    private class MtkDcDefaultState extends DcDefaultState {
        @Override
        public void enter() {
            if (DBG) log("DcDefaultState: enter");

            // Register for DRS or RAT change
            mPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(getHandler(),
                    DataConnection.EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED, null);

            mPhone.getServiceStateTracker().registerForDataRoamingOn(getHandler(),
                    DataConnection.EVENT_DATA_CONNECTION_ROAM_ON, null);
            mPhone.getServiceStateTracker().registerForDataRoamingOff(getHandler(),
                    DataConnection.EVENT_DATA_CONNECTION_ROAM_OFF, null, true);

            // M @{
            // For RA fail handling
            if (mTelDevController != null &&
                    mTelDevController.getModem(0) != null &&
                    ((MtkHardwareConfig) mTelDevController.getModem(0))
                    .hasRaCapability() == false) {
                registerNetworkAlertObserver();
            }

            // Add ourselves to the list of data connections
            mDcController.addDc(MtkDataConnection.this);

            IntentFilter filter = new IntentFilter();
            filter.addAction(mActionRetry);
            if (DBG) {
                log("DcDefaultState: register for intent action=" + mActionRetry);
            }
            filter.addAction(CarrierExpressManager.ACTION_OPERATOR_CONFIG_CHANGED);
            mPhone.getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
            // M @}
        }
        @Override
        public void exit() {
            if (DBG) log("DcDefaultState: exit");

            // Unregister for DRS or RAT change.
            mPhone.getServiceStateTracker().unregisterForDataRegStateOrRatChanged(getHandler());

            mPhone.getServiceStateTracker().unregisterForDataRoamingOn(getHandler());
            mPhone.getServiceStateTracker().unregisterForDataRoamingOff(getHandler());

            // Remove ourselves from the DC lists
            mDcController.removeDc(MtkDataConnection.this);

            if (mAc != null) {
                mAc.disconnected();
                mAc = null;
            }
            mApnContexts = null;
            mReconnectIntent = null;
            mDct = null;
            mApnSetting = null;
            mPhone = null;
            mDataServiceManager = null;
            mLinkProperties = null;
            mLastFailCause = null;
            mUserData = null;
            mDcController = null;
            mDcTesterFailBringUpAll = null;

            // M @{
            // For RA fail handling
            if (mTelDevController != null &&
                    mTelDevController.getModem(0) != null &&
                    ((MtkHardwareConfig) mTelDevController.getModem(0))
                    .hasRaCapability() == false) {
                unregisterNetworkAlertObserver();
            }
            mPhone.getContext().unregisterReceiver(mIntentReceiver);
            // M @}
        }

        @Override
        public boolean processMessage(Message msg) {
            boolean retVal = HANDLED;

            if (VDBG) {
                log("DcDefault msg=" + getWhatToString(msg.what)
                        + " RefCount=" + mApnContexts.size());
            }
            switch (msg.what) {
                // M @{
                case MtkDcAsyncChannel.REQ_GET_APNTYPE: {
                    String[] aryApnType = getApnType();
                    if (VDBG) {
                        log("REQ_GET_APNTYPE  aryApnType=" + Arrays.toString(aryApnType));
                    }
                    mAc.replyToMessage(msg, MtkDcAsyncChannel.RSP_GET_APNTYPE, aryApnType);
                    break;
                }
                case EVENT_IPV4_ADDRESS_REMOVED:
                    if (DBG) {
                        log("DcDefaultState: ignore EVENT_IPV4_ADDRESS_REMOVED not in ActiveState");
                    }
                    break;

                case EVENT_IPV6_ADDRESS_REMOVED:
                    if (DBG) {
                        log("DcDefaultState: ignore EVENT_IPV6_ADDRESS_REMOVED not in ActiveState");
                    }
                    break;

                case EVENT_ADDRESS_REMOVED:
                    if (DBG) {
                        log("DcDefaultState: " + getWhatToString(msg.what));
                    }
                    // TODO: need to do something
                    break;
                case EVENT_IPV6_ADDRESS_UPDATED:
                    if (DBG) {
                        log("DcDefaultState: ignore EVENT_IPV6_ADDRESS_UPDATED not in ActiveState");
                    }
                    break;
                // M: PS/CS concurrent @{
                case EVENT_VOICE_CALL: {
                    mIsInVoiceCall = (msg.arg1 != 0);
                    mIsSupportConcurrent = (msg.arg2 != 0);
                    break;
                }
                case EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED:
                    if (mIsInVoiceCall) {
                        mIsSupportConcurrent = MtkDcHelper.getInstance()
                                .isDataAllowedForConcurrent(mPhone.getPhoneId());
                    }
                    retVal = super.processMessage(msg);
                    break;
                // M: PS/CS concurrent @}
                // M @}
                default:
                    retVal = super.processMessage(msg);
                    break;
            }

            return retVal;
        }
    }


    /**
     * The state machine is activating a connection.
     */
    private class MtkDcActivatingState extends DcActivatingState {
        @Override
        public void enter() {
            // M: For debug @{
            if (DBG) {
                log("DcActivatingState: enter dc=" + MtkDataConnection.this);
            }
            // M: For debug @}
            super.enter();
        }

        @Override
        public void exit() {
            // M: For debug @{
            if (DBG) {
                log("DcActivatingState: exit dc=" + MtkDataConnection.this);
            }
            // M: For debug @}
            super.exit();
        }

        @Override
        public boolean processMessage(Message msg) {
            boolean retVal;
            AsyncResult ar;
            ConnectionParams cp;

            if (DBG) log("DcActivatingState: msg=" + msgToString(msg));
            switch (msg.what) {
                case EVENT_CONNECT:
                    /** M: when a new connect request is coming after the first one,
                     *  we should update the apn list right now for judgment,
                     *  the process can be delayed to next state as original design.
                     */
                    cp = (ConnectionParams) msg.obj;
                    mApnContexts.put(cp.mApnContext, cp);
                    if (DBG) {
                        log("DcActivatingState: mApnContexts size=" + mApnContexts.size());
                    }
                    // M: go through without break.
                case EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED:
                    // Activating can't process until we're done.
                    deferMessage(msg);
                    retVal = HANDLED;
                    break;
                case EVENT_DISCONNECT:
                    DisconnectParams dp = (DisconnectParams) msg.obj;
                    /** M: if disconnect apn is not valid anymore, should pre-process,
                     *  otherwise when into DcActiveState, the judgement maybe wrong,
                     *  result to DcNetworkAgent can't be created.
                     */
                    if (!mApnContexts.containsKey(dp.mApnContext)) {
                        log("DcActivatingState ERROR no such apnContext=" + dp.mApnContext
                                + " in this dc=" + MtkDataConnection.this);
                        notifyDisconnectCompleted(dp, false);
                    } else {
                        deferMessage(msg);
                    }
                    retVal = HANDLED;
                    break;
                case EVENT_SETUP_DATA_CONNECTION_DONE:
                    cp = (ConnectionParams) msg.obj;

                    DataCallResponse dataCallResponse =
                            msg.getData().getParcelable(DataServiceManager.DATA_CALL_RESPONSE);
                    SetupResult result = onSetupConnectionCompleted(msg.arg1, dataCallResponse, cp);
                    if (result != SetupResult.ERROR_STALE) {
                        if (mConnectionParams != cp) {
                            loge("DcActivatingState: WEIRD mConnectionsParams:"+ mConnectionParams
                                    + " != cp:" + cp);
                        }
                    }
                    if (DBG) {
                        log("DcActivatingState onSetupConnectionCompleted result=" + result
                                + " dc=" + MtkDataConnection.this);
                    }
                    if (cp.mApnContext != null) {
                        cp.mApnContext.requestLog("onSetupConnectionCompleted result=" + result);
                    }
                    switch (result) {
                        case SUCCESS:
                            // All is well
                            mDcFailCause = DcFailCause.NONE;
                            // M
                            resetRetryCount();
                            transitionTo(mActiveState);
                            break;
                        case ERROR_RADIO_NOT_AVAILABLE:
                            // Vendor ril rejected the command and didn't connect.
                            // Transition to inactive but send notifications after
                            // we've entered the mInactive state.
                            mInactiveState.setEnterNotificationParams(cp, result.mFailCause);
                            transitionTo(mInactiveState);
                            break;
                        case ERROR_INVALID_ARG:
                            // The addresses given from the RIL are bad
                            tearDownData(cp);
                            transitionTo(mDisconnectingErrorCreatingConnection);
                            break;
                        case ERROR_DATA_SERVICE_SPECIFIC_ERROR:

                            // Retrieve the suggested retry delay from the modem and save it.
                            // If the modem want us to retry the current APN again, it will
                            // suggest a positive delay value (in milliseconds). Otherwise we'll get
                            // NO_SUGGESTED_RETRY_DELAY here.

                            long delay = getSuggestedRetryDelay(dataCallResponse);
                            cp.mApnContext.setModemSuggestedDelay(delay);

                            String str = "DcActivatingState: ERROR_DATA_SERVICE_SPECIFIC_ERROR "
                                    + " delay=" + delay
                                    + " result=" + result
                                    + " result.isRestartRadioFail=" +
                                    result.mFailCause.isRestartRadioFail(mPhone.getContext(),
                                            mPhone.getSubId())
                                    + " isPermanentFailure=" +
                                    mDct.isPermanentFailure(result.mFailCause);
                            if (DBG) log(str);
                            if (cp.mApnContext != null) cp.mApnContext.requestLog(str);

                            // M: Customize fallback retry @{
                            if (result.mFailCause == DcFailCause.MTK_PDP_FAIL_FALLBACK_RETRY) {
                                //At least one IPv4 or IPv6 is accepted, setup connection
                                onSetupFallbackConnection(dataCallResponse, cp);
                                // Enter active state but with FALLBACK_RETRY_CONNECTION
                                mDcFailCause = DcFailCause.MTK_PDP_FAIL_FALLBACK_RETRY;
                                deferMessage(obtainMessage(EVENT_FALLBACK_RETRY_CONNECTION, mTag));
                                transitionTo(mActiveState);
                            // M: Customize fallback retry @}
                            } else {
                                // Save the cause. DcTracker.onDataSetupComplete will check this
                                // failure cause and determine if we need to retry this APN later
                                // or not.
                                mInactiveState.setEnterNotificationParams(cp, result.mFailCause);
                                transitionTo(mInactiveState);
                            }
                            break;
                        case ERROR_STALE:
                            loge("DcActivatingState: stale EVENT_SETUP_DATA_CONNECTION_DONE"
                                    + " tag:" + cp.mTag + " != mTag:" + mTag);
                            break;
                        default:
                            throw new RuntimeException("Unknown SetupResult, should not happen");
                    }
                    retVal = HANDLED;
                    break;
                // M @{
                case EVENT_IPV4_ADDRESS_REMOVED:
                case EVENT_IPV6_ADDRESS_REMOVED:
                case EVENT_IPV6_ADDRESS_UPDATED:
                    if (DBG) {
                        log("DcActivatingState deferMsg: " + getWhatToString(msg.what)
                                + ", address info: " + (AddressInfo) msg.obj);
                    }
                    deferMessage(msg);
                    retVal = HANDLED;
                    break;
                // M @}
                default:
                    retVal = super.processMessage(msg);
                    break;
            }
            return retVal;
        }
    }

    /**
     * The state machine is connected, expecting an EVENT_DISCONNECT.
     */
    private class MtkDcActiveState extends DcActiveState {
        @Override public void enter() {
            //M: For debug, Google default log, print it later.
            // if (DBG) log("DcActiveState: enter dc=" + DataConnection.this);
            StatsLog.write(StatsLog.MOBILE_CONNECTION_STATE_CHANGED,
                    StatsLog.MOBILE_CONNECTION_STATE_CHANGED__STATE__ACTIVE,
                    mPhone.getPhoneId(), mId,
                    mApnSetting != null ? (long) mApnSetting.typesBitmap : 0L,
                    mApnSetting != null
                        ? mApnSetting.canHandleType(PhoneConstants.APN_TYPE_DEFAULT) : false);

            // M: Notify data connection in advance for 'default' apn type @{
            for (ConnectionParams cp : mApnContexts.values()) {
                ApnContext apnContext = cp.mApnContext;
                if (TextUtils.equals(apnContext.getApnType(),
                        PhoneConstants.APN_TYPE_DEFAULT)) {
                    apnContext.setReason(Phone.REASON_CONNECTED);
                    apnContext.setState(DctConstants.State.CONNECTED);
                    mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
                    log("DcActiveState: notifyDataConnection in advance for default apn type.");
                    break;
                }
            }
            // @}

            updateNetworkInfo();

            // If we were retrying there maybe more than one, otherwise they'll only be one.
            notifyAllOfConnected(Phone.REASON_CONNECTED);

            // M: PS/CS concurrent @{
            //mPhone.getCallTracker().registerForVoiceCallStarted(getHandler(),
            //        DataConnection.EVENT_DATA_CONNECTION_VOICE_CALL_STARTED, null);
            //mPhone.getCallTracker().registerForVoiceCallEnded(getHandler(),
            //        DataConnection.EVENT_DATA_CONNECTION_VOICE_CALL_ENDED, null);
            // M: PS/CS concurrent @}

            // If the EVENT_CONNECT set the current max retry restore it here
            // if it didn't then this is effectively a NOP.
            mDcController.addActiveDcByCid(MtkDataConnection.this);

            // M: PS/CS concurrent @{
            if (isNwNeedSuspended()) {
                mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.SUSPENDED,
                        mNetworkInfo.getReason(), null);
            } else {
            // M: PS/CS concurrent @}
                mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED,
                        mNetworkInfo.getReason(), null);
            }

            mNetworkInfo.setExtraInfo(mApnSetting.apn);
            updateTcpBufferSizes(mRilRat);

            final NetworkMisc misc = new NetworkMisc();
            final CarrierSignalAgent carrierSignalAgent = mPhone.getCarrierSignalAgent();
            if (carrierSignalAgent.hasRegisteredReceivers(TelephonyIntents
                    .ACTION_CARRIER_SIGNAL_REDIRECTED)) {
                // carrierSignal Receivers will place the carrier-specific provisioning notification
                misc.provisioningNotificationDisabled = true;
            }
            misc.subscriberId = mPhone.getSubscriberId();

            if (DBG) {
                logi("DcActiveState: enter dc = " + MtkDataConnection.this +
                        " mNetworkInfo = " + mNetworkInfo);
            }
            setNetworkRestriction();
            if (DBG) log("mRestrictedNetworkOverride = " + mRestrictedNetworkOverride);
            mNetworkAgent = new DcNetworkAgent(getHandler().getLooper(), mPhone.getContext(),
                    "DcNetworkAgent", mNetworkInfo, getNetworkCapabilities(), mLinkProperties,
                    50, misc);
            mPhone.mCi.registerForNattKeepaliveStatus(
                    getHandler(), DataConnection.EVENT_KEEPALIVE_STATUS, null);
            mPhone.mCi.registerForLceInfo(
                    getHandler(), DataConnection.EVENT_LINK_CAPACITY_CHANGED, null);

            /* for op01 begin */
            try {
                mDataConnectionExt.onDcActivated(
                        (mApnSetting == null) ? null : mApnSetting.types,
                        (mLinkProperties == null) ? "" : mLinkProperties.getInterfaceName());
            } catch (Exception e) {
                loge("onDcActivated fail!");
                e.printStackTrace();
            }
            /* for op01 end */

        }

        @Override
        public void exit() {
            /* for op01 begin */
            try {
                mDataConnectionExt.onDcDeactivated(
                    (mApnSetting == null) ? null : mApnSetting.types,
                    (mLinkProperties == null) ? "" : mLinkProperties.getInterfaceName());
            } catch (Exception e) {
                loge("onDcDeactivated fail!");
                e.printStackTrace();
            }
            /* for op01 end */

            super.exit();
        }

        @Override
        public boolean processMessage(Message msg) {
            boolean retVal;

            switch (msg.what) {
                case EVENT_CONNECT: {
                    ConnectionParams cp = (ConnectionParams) msg.obj;
                    // either add this new apn context to our set or
                    // update the existing cp with the latest connection generation number
                    mApnContexts.put(cp.mApnContext, cp);
                    if (DBG) {
                        log("DcActiveState: EVENT_CONNECT cp=" + cp
                                + " dc=" + MtkDataConnection.this);
                    }

                    // M: update capabilities @{
                    if (mNetworkAgent != null) {
                        NetworkCapabilities cap = getNetworkCapabilities();
                        mNetworkAgent.sendNetworkCapabilities(cap);
                        log("DcActiveState update Capabilities:" + cap);
                    }
                    // @}

                    // M: [LTE][Low Power][UL traffic shaping] @{
                    checkIfDefaultApnReferenceCountChanged();
                    // @}

                    notifyConnectCompleted(cp, DcFailCause.NONE, false);
                    retVal = HANDLED;
                    break;
                }
                case EVENT_DISCONNECT: {
                    DisconnectParams dp = (DisconnectParams) msg.obj;
                    if (DBG) {
                        log("DcActiveState: EVENT_DISCONNECT dp=" + dp
                                + " dc=" + MtkDataConnection.this);
                    }
                    if (mApnContexts.containsKey(dp.mApnContext)) {
                        if (DBG) {
                            log("DcActiveState msg.what=EVENT_DISCONNECT RefCount="
                                    + mApnContexts.size());
                        }

                        if (mApnContexts.size() == 1) {
                            // M: P-CSCF error handling.
                            if (!hasMdAutoSetupImsCapability()) {
                                handlePcscfErrorCause(dp);
                            }
                            mApnContexts.clear();
                            mDisconnectParams = dp;
                            mConnectionParams = null;
                            dp.mTag = mTag;
                            tearDownData(dp);
                            transitionTo(mDisconnectingState);
                        } else {
                            mApnContexts.remove(dp.mApnContext);

                            // M: update capabilities @{
                            if (mNetworkAgent != null) {
                                NetworkCapabilities cap = getNetworkCapabilities();
                                mNetworkAgent.sendNetworkCapabilities(cap);
                                log("DcActiveState update Capabilities:" + cap);
                            }
                            checkIfDefaultApnReferenceCountChanged();
                            // @}

                            notifyDisconnectCompleted(dp, false);
                        }
                    } else {
                        log("DcActiveState ERROR no such apnContext=" + dp.mApnContext
                                + " in this dc=" + MtkDataConnection.this);
                        notifyDisconnectCompleted(dp, false);
                    }
                    retVal = HANDLED;
                    break;
                }
                // M @{
                case EVENT_IPV4_ADDRESS_REMOVED:
                    AddressInfo addrV4Info = (AddressInfo) msg.obj;
                    log("DcActiveState: " + getWhatToString(msg.what) + ": " + addrV4Info);
                    // TODO: currently do nothing here
                    retVal = HANDLED;
                    break;

                case EVENT_IPV6_ADDRESS_REMOVED: {
                    AddressInfo addrV6Info = (AddressInfo) msg.obj;
                    log("DcActiveState: " + getWhatToString(msg.what) + ": " + addrV6Info);
                    if (mInterfaceName != null && mInterfaceName.equals(addrV6Info.mIntfName)) {
                        String strAddress = addrV6Info.mLinkAddr.getAddress().getHostAddress();
                        log("strAddress: " + strAddress);
                        if (strAddress.equalsIgnoreCase("FE80::5A:5A:5A:23")) {
                            mValid = RA_INITIAL_FAIL;
                        } else if (strAddress.equalsIgnoreCase("FE80::5A:5A:5A:22")) {
                            mValid = RA_REFRESH_FAIL;
                        } else {
                            mValid = -1000;
                        }

                        if (mValid == RA_INITIAL_FAIL ||
                                mValid == RA_REFRESH_FAIL) {
                            log("DcActiveState: RA initial or refresh fail,"
                                    + " valid:" + mValid);
                            onAddressRemoved();
                        }
                    }

                    if (mGlobalV6AddrInfo != null
                            && mGlobalV6AddrInfo.mIntfName.equals(addrV6Info.mIntfName)) {
                        mGlobalV6AddrInfo = null;
                    }
                    retVal = HANDLED;
                    break;
                }
                case EVENT_IPV6_ADDRESS_UPDATED: {
                    AddressInfo addrV6Info = (AddressInfo) msg.obj;
                    if (mInterfaceName != null && mInterfaceName.equals(addrV6Info.mIntfName)) {
                        int scope = addrV6Info.mLinkAddr.getScope();
                        int flag = addrV6Info.mLinkAddr.getFlags();
                        log("EVENT_IPV6_ADDRESS_UPDATED, scope: " + scope + ", flag: " + flag);
                        if (RT_SCOPE_UNIVERSE == scope && ((flag & 1) != IFA_F_TEMPORARY)
                                && mNetworkAgent != null) {
                            mGlobalV6AddrInfo = addrV6Info;
                            mNetworkAgent.sendLinkProperties(getLinkProperties());
                            log("EVENT_IPV6_ADDRESS_UPDATED, "
                                    +"notify global ipv6 address update");
                        } else {
                            log("EVENT_IPV6_ADDRESS_UPDATED, "
                                    +"not notify global ipv6 address update");
                        }
                    }
                    retVal = HANDLED;
                    break;
                }
                // M: Customize fallback retry @{
                case EVENT_FALLBACK_RETRY_CONNECTION:
                    if (msg.arg1 == mTag) {
                        if (mDataRegState != ServiceState.STATE_IN_SERVICE) {
                            if (DBG) {
                                log("DcActiveState: EVENT_FALLBACK_RETRY_CONNECTION"
                                    + " not in service");
                            }
                        } else {
                            if (DBG) {
                                log("DcActiveState EVENT_FALLBACK_RETRY_CONNECTION"
                                        + " mConnectionParams=" + mConnectionParams);
                            }
                            onConnect(mConnectionParams);
                        }
                    } else {
                        if (DBG) {
                            log("DcActiveState stale EVENT_FALLBACK_RETRY_CONNECTION"
                                    + " tag:" + msg.arg1 + " != mTag:" + mTag);
                        }
                    }
                    retVal = HANDLED;
                    break;
                case EVENT_SETUP_DATA_CONNECTION_DONE: {
                    ConnectionParams cp = (ConnectionParams) msg.obj;

                    DataCallResponse dataCallResponse =
                            msg.getData().getParcelable(DataServiceManager.DATA_CALL_RESPONSE);
                    SetupResult result = onSetupConnectionCompleted(msg.arg1, dataCallResponse, cp);
                    if (result != SetupResult.ERROR_STALE) {
                        if (mConnectionParams != cp) {
                            loge("DcActiveState_FALLBACK_Retry: WEIRD mConnectionsParams:"
                                    + mConnectionParams + " != cp:" + cp);
                        }
                    }
                    if (DBG) {
                        log("DcActiveState_FALLBACK_Retry onSetupConnectionCompleted result="
                                + result + " dc=" + MtkDataConnection.this);
                    }
                    switch (result) {
                        case SUCCESS:
                            // All is well
                            mDcFailCause = DcFailCause.NONE;
                            resetRetryCount();
                            break;
                        case ERROR_DATA_SERVICE_SPECIFIC_ERROR:
                            String str = "DcActiveState_FALLBACK_Retry:"
                                    + " ERROR_DATA_SERVICE_SPECIFIC_ERROR"
                                    + " result=" + result
                                    + " result.isRestartRadioFail=" +
                                    result.mFailCause.isRestartRadioFail(mPhone.getContext(),
                                            mPhone.getSubId())
                                    + " result.isPermanentFailure=" +
                                    mDct.isPermanentFailure(result.mFailCause);
                            if (DBG) log(str);

                            if (result.mFailCause == DcFailCause.MTK_PDP_FAIL_FALLBACK_RETRY) {
                                // M: [General Operator] Data Framework - WWOP requirements @{
                                if (mDcFcMgr != null && mDcFcMgr.isSpecificNetworkAndSimOperator(
                                        DcFailCauseManager.Operator.OP19)) {
                                    mRetryCount++;
                                    long retryTime = mDcFcMgr.getRetryTimeByIndex(mRetryCount,
                                            DcFailCauseManager.Operator.OP19);
                                    // The value < 0 means no value is suggested
                                    if (retryTime < 0) {
                                        if (DBG) {
                                            log("DcActiveState_FALLBACK_Retry: No retry"
                                                + " but at least one IPv4 or IPv6 is accepted");
                                        }
                                        // Not to do retry anymore
                                        mDcFailCause = DcFailCause.NONE;
                                    } else {
                                        // Enter act state but with EVENT_FALLBACK_RETRY_CONNECTION
                                        mDcFailCause = DcFailCause.MTK_PDP_FAIL_FALLBACK_RETRY;
                                        startRetryAlarm(EVENT_FALLBACK_RETRY_CONNECTION, mTag,
                                                retryTime);
                                    }
                                }
                                // M: [General Operator] Data Framework - WWOP requirements @}
                            } else {
                                if (DBG) {
                                    log("DcActiveState_FALLBACK_Retry:"
                                            + " ERROR_DATA_SERVICE_SPECIFIC_ERROR"
                                            + " Not retry anymore");
                                }
                            }
                            break;
                        case ERROR_STALE:
                            loge("DcActiveState_FALLBACK_Retry:"
                                    + " stale EVENT_SETUP_DATA_CONNECTION_DONE"
                                    + " tag:" + cp.mTag + " != mTag:" + mTag
                                    + " Not retry anymore");
                            break;
                        default:
                            if (DBG) {
                                log("DcActiveState_FALLBACK_Retry: Another error cause,"
                                            + " Not retry anymore");
                            }
                    }
                    retVal = HANDLED;
                    break;
                }
                // M: Customize fallback retry @}
                // M: PS/CS concurrent @{
                case EVENT_VOICE_CALL: {
                    mIsInVoiceCall = (msg.arg1 != 0);
                    mIsSupportConcurrent = (msg.arg2 != 0);
                    updateNetworkInfoSuspendState();
                    if (mNetworkAgent != null) {
                        // state changed
                        mNetworkAgent.sendNetworkInfo(mNetworkInfo);
                    }
                    retVal = HANDLED;
                    break;
                }
                // M: PS/CS concurrent @}
                // M @}
                default:
                    retVal = super.processMessage(msg);
                    break;
            }
            return retVal;
        }
    }

    @Override
    protected long getSuggestedRetryDelay(DataCallResponse response) {
        /** According to ril.h
         * The value < 0 means no value is suggested
         * The value 0 means retry should be done ASAP.
         * The value of Integer.MAX_VALUE(0x7fffffff) means no retry.
         */

        // The value < 0 means no value is suggested
        if (response.getSuggestedRetryTime() < 0) {
            if (DBG) log("No suggested retry delay.");
            // M: Data Framework - CC 33 @{
            long delay = RetryManager.NO_SUGGESTED_RETRY_DELAY;
            DcFailCause cause = DcFailCause.fromInt(response.getStatus());
            if (mDcFcMgr != null) {
                delay = mDcFcMgr.getSuggestedRetryDelayByOp(cause);
            }
            return delay;
            // M: Data Framework - CC 33 @}
        }
        // The value of Integer.MAX_VALUE(0x7fffffff) means no retry.
        else if (response.getSuggestedRetryTime() == Integer.MAX_VALUE) {
            if (DBG) log("Modem suggested not retrying.");
            return RetryManager.NO_RETRY;
        }

        // We need to cast it to long because the value returned from RIL is a 32-bit integer,
        // but the time values used in AlarmManager are all 64-bit long.
        return (long) response.getSuggestedRetryTime();
    }

    /*
     * MTK added methods start from here
     */
    String[] getApnType() {
        String[] aryApnType = null;
        if (DBG) {
            log("getApnType: mApnContexts.size() = " + mApnContexts.size());
        }
        if (0 == mApnContexts.size()) {
            return null;
        } else {
            aryApnType = new String[mApnContexts.values().size()];
            int i = 0;
            for (ConnectionParams cp : mApnContexts.values()) {
                ApnContext apnContext = cp.mApnContext;
                String apnType = apnContext.getApnType();
                if (DBG) {
                    log("getApnType: apnType = " + apnType);
                }
                aryApnType[i] = new String(apnType);
                i++;
            }
        }
        return aryApnType;
    }

    private void notifyDefaultApnReferenceCountChanged(int refCount, int event) {
        Message msg = mDct.obtainMessage(event);
        msg.arg1 = refCount;
        AsyncResult.forMessage(msg);
        msg.sendToTarget();
    }

    private void onSetupFallbackConnection(DataCallResponse response, ConnectionParams cp) {
        SetupResult result;

        if (cp.mTag != mTag) {
            if (DBG) {
                log("onSetupFallbackConnection stale cp.tag=" + cp.mTag + ", mtag=" + mTag);
            }
            result = SetupResult.ERROR_STALE;
        } else {
            if (DBG) log("onSetupFallbackConnection received successful DataCallResponse");
            mCid = response.getCallId();

            mPcscfAddr = response.getPcscfs().toArray(new String[response.getPcscfs().size()]);

            // M: [OD over ePDG] start
            setConnectionRat(decodeRat(response.getActive()), "setup fallback");
            // M: [OD over ePDG] end

            DataCallResponse tempResponse = new DataCallResponse(
                    DcFailCause.NONE.getErrorCode(),
                    response.getSuggestedRetryTime(),
                    response.getCallId(),
                    response.getActive(),
                    response.getType(),
                    response.getIfname(),
                    response.getAddresses(),
                    response.getDnses(),
                    response.getGateways(),
                    response.getPcscfs(),
                    response.getMtu());
            result = updateLinkProperty(tempResponse).setupResult;

            // M: IPv6 RA update
            mInterfaceName = response.getIfname();
            log("onSetupFallbackConnection: ifname-" + mInterfaceName);
        }
    }

    private boolean isAddCapabilityByDataOption() {
        boolean isUserDataEnabled = mDct.isUserDataEnabled();
        boolean isDataRoamingEnabled = mDct.getDataRoamingEnabled();
        log("addCapabilityByDataOption");

        if (isUserDataEnabled) {
            if (mPhone.getServiceState().getDataRoaming()) {
                if (isDataRoamingEnabled) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private LinkProperties getLinkProperties() {
        if (mGlobalV6AddrInfo == null) {
            return mLinkProperties;
        } else {
            LinkProperties linkProperties = new LinkProperties(mLinkProperties);
            for (LinkAddress linkAddr : linkProperties.getLinkAddresses()) {
                if (linkAddr.getAddress() instanceof Inet6Address) {
                    linkProperties.removeLinkAddress(linkAddr);
                    break;
                }
            }
            linkProperties.addLinkAddress(mGlobalV6AddrInfo.mLinkAddr);
            return linkProperties;
        }
    }

    // M: PS/CS concurrent @{
    /**
     * Determine if the network info is required to be suspended based on the status of
     * the presense of voice calls, the cs/ps concurrent support, apn types, and the enabled
     * of wifi calling.
     *
     * @return if the network info should be suspended
     */
    private boolean isNwNeedSuspended() {
        final boolean bImsOrEmergencyApn = MtkDcHelper.isImsOrEmergencyApn(getApnType());
        final boolean bWifiCallingEnabled = mIsInVoiceCall ?
                MtkDcHelper.getInstance().isWifiCallingEnabled() : false;

        if (DBG) {
            log("isNwNeedSuspended: mIsInVoiceCall = " + mIsInVoiceCall
                    + ", mIsSupportConcurrent = " + mIsSupportConcurrent
                    + ", bImsOrEmergencyApn = " + bImsOrEmergencyApn
                    + ", bWifiCallingEnabled = " + bWifiCallingEnabled);
        }

        return (mIsInVoiceCall && !mIsSupportConcurrent
                && !bImsOrEmergencyApn && !bWifiCallingEnabled);
    }
    // M: PS/CS concurrent @}

    /**
     *  M: IPv6 RA updateObserver that watches for {@link INetworkManagementService} alerts.
     */
    private int getEventByAddress(boolean bUpdated, LinkAddress linkAddr) {
            int event = -1;
            InetAddress addr = linkAddr.getAddress();
            if (bUpdated == false) {
                if (addr instanceof Inet6Address) {
                    event = EVENT_IPV6_ADDRESS_REMOVED;
                } else if (addr instanceof Inet4Address) {
                    event = EVENT_IPV4_ADDRESS_REMOVED;
                } else {
                    loge("unknown address type, linkAddr: " + linkAddr);
                }
            } else {
                if (addr instanceof Inet6Address) {
                    event = EVENT_IPV6_ADDRESS_UPDATED;
                } else {
                    loge("unknown address type, linkAddr: " + linkAddr);
                }
            }

            return event;
    }

    private void sendMessageForSM(int event, String iface, LinkAddress address) {
        if (event < 0) {
            loge("sendMessageForSM: Skip notify!!!");
            return;
        }
        AddressInfo addrInfo = new AddressInfo(iface, address);
        log("sendMessageForSM: " + cmdToString(event) + ", addressInfo: " + addrInfo);
        sendMessage(obtainMessage(event, addrInfo));
    }

    private INetworkManagementEventObserver mAlertObserver = new BaseNetworkObserver() {
        @Override
        public void addressRemoved(String iface, LinkAddress address) {
            int event = getEventByAddress(false, address);

            sendMessageForSM(event, iface, address);
        }
        @Override
        public void addressUpdated(String iface, LinkAddress address) {
            int event = getEventByAddress(true, address);

            sendMessageForSM(event, iface, address);
        }
    };

    private void onAddressRemoved() {
        if ((RILConstants.SETUP_DATA_PROTOCOL_IPV6.equals(mApnSetting.protocol)
                || RILConstants.SETUP_DATA_PROTOCOL_IPV4V6.equals(mApnSetting.protocol)) &&
                !isIpv4Connected()) {
            log("onAddressRemoved: IPv6 RA failed and didn't connect with IPv4");
            if (mApnContexts != null) {
                log("onAddressRemoved: mApnContexts size: " + mApnContexts.size());
                for (ConnectionParams cp : mApnContexts.values()) {
                    ApnContext apnContext = cp.mApnContext;
                    String apnType = apnContext.getApnType();
                    if (apnContext.getState() == DctConstants.State.CONNECTED) {
                        log("onAddressRemoved: send message EVENT_DISCONNECT_ALL");
                        final Pair<ApnContext, Integer> pair =
                            new Pair<ApnContext, Integer>(apnContext, cp.mConnectionGeneration);
                        Message msg =
                            mDct.obtainMessage(DctConstants.EVENT_DISCONNECT_DONE, pair);

                        DisconnectParams dp =
                            new DisconnectParams(apnContext, MtkGsmCdmaPhone.REASON_RA_FAILED, msg);
                        MtkDataConnection.this.sendMessage(MtkDataConnection.this.
                                obtainMessage(EVENT_DISCONNECT_ALL, dp));
                        break;
                    }
                }
            }
        } else {
            if (DBG) log("onAddressRemoved: no need to remove");
        }
    }

    /**
     * M: [LTE][Low Power][UL traffic shaping]
     */
    void checkIfDefaultApnReferenceCountChanged() {
        boolean isDefaultExisted = false;
        int sizeOfOthers = 0;
        for (ConnectionParams cp : mApnContexts.values()) {
            ApnContext apnContext = cp.mApnContext;
            if (TextUtils.equals(PhoneConstants.APN_TYPE_DEFAULT, apnContext.getApnType())
                    && DctConstants.State.CONNECTED.equals(apnContext.getState())) {
                isDefaultExisted = true;
            } else if (DctConstants.State.CONNECTED.equals(apnContext.getState())) {
                sizeOfOthers++;
            }
        }

        if (isDefaultExisted == true) {
            if (DBG) log("refCount = " + mApnContexts.size()
                    + ", non-default refCount = " + sizeOfOthers);
            notifyDefaultApnReferenceCountChanged(sizeOfOthers + 1,
                    MtkDctConstants.EVENT_DEFAULT_APN_REFERENCE_COUNT_CHANGED);
        }
    }

    private boolean isDefaultDataSubPhone(Phone phone) {
        final int defaultDataPhoneId = mSubController.getPhoneId(
                mSubController.getDefaultDataSubId());
        int curPhoneId = phone.getPhoneId();

        if (defaultDataPhoneId != curPhoneId) {
            log("Current phone is not default phone: curPhoneId = "
                    + curPhoneId + ", defaultDataPhoneId = "
                    + defaultDataPhoneId);
            return false;
        }
        return true;
    }

    private void registerNetworkAlertObserver() {
        if (mNetworkManager != null) {
            log("registerNetworkAlertObserver X");
            try {
                mNetworkManager.registerObserver(mAlertObserver);
                log("registerNetworkAlertObserver E");
            } catch (RemoteException e) {
                // ignored; service lives in system_server
                loge("registerNetworkAlertObserver failed E");
            }
        }
    }

    private void unregisterNetworkAlertObserver() {
        if (mNetworkManager != null) {
            log("unregisterNetworkAlertObserver X");
            try {
                mNetworkManager.unregisterObserver(mAlertObserver);
                log("unregisterNetworkAlertObserver E");
            } catch (RemoteException e) {
                // ignored; service lives in system_server
                loge("unregisterNetworkAlertObserver failed E");
            }
            mInterfaceName = null;
        }
    }

    private class AddressInfo {
        String mIntfName;
        LinkAddress mLinkAddr;

        public AddressInfo(String intfName, LinkAddress linkAddr) {
            mIntfName = intfName;
            mLinkAddr = linkAddr;
        }

        public String toString() {
            return "interfaceName: "  + mIntfName + "/" + mLinkAddr;
        }
    }

    /**
     * Start retry alarm with delay.
     *
     * @param what for event.
     * @param tag for current tag.
     * @param delay for dealy.
     */
    public void startRetryAlarm(int what, int tag, long delay) {
        Intent intent = new Intent(mActionRetry);
        intent.putExtra(INTENT_RETRY_ALARM_WHAT, what);
        intent.putExtra(INTENT_RETRY_ALARM_TAG, tag);

        if (DBG) {
            log("startRetryAlarm: next attempt in " + (delay / 1000) + "s" +
                    " what=" + what + " tag=" + tag);
        }

        PendingIntent retryIntent = PendingIntent.getBroadcast(mPhone.getContext(), 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay, retryIntent);
    }

    /**
     * Start retry alarm with exactly delay.
     *
     * @param what for event.
     * @param tag for current tag.
     * @param delay for exactly dealy.
     */
    public void startRetryAlarmExact(int what, int tag, long delay) {
        Intent intent = new Intent(mActionRetry);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(INTENT_RETRY_ALARM_WHAT, what);
        intent.putExtra(INTENT_RETRY_ALARM_TAG, tag);

        if (DBG) {
            log("startRetryAlarmExact: next attempt in " + (delay / 1000) + "s" +
                    " what=" + what + " tag=" + tag);
        }

        PendingIntent retryIntent = PendingIntent.getBroadcast(mPhone.getContext(), 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay, retryIntent);
    }

    /**
     * Clear the data-retry counter.
     */
    private void resetRetryCount() {
        mRetryCount = 0;
        if (DBG) {
            log("resetRetryCount: " + mRetryCount);
        }
    }

    /**
     * M: IMS data retry requirements.
     */
    public void handlePcscfErrorCause(DisconnectParams dp) {
        CarrierConfigManager configMgr = (CarrierConfigManager)
                mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        int subId = mPhone.getSubId();

        do {
            if (configMgr == null) {
                loge("handlePcscfErrorCause() null configMgr!");
                break;
            }

            PersistableBundle b = configMgr.getConfigForSubId(subId);
            if (b == null) {
                loge("handlePcscfErrorCause() null config!");
                break;
            }
            boolean syncFailCause = b.getBoolean(
                    MtkCarrierConfigManager.KEY_IMS_PDN_SYNC_FAIL_CAUSE_TO_MODEM_BOOL);

            log("handlePcscfErrorCause() syncFailCause: " + syncFailCause +
                    ", subId: " + subId);

            if (syncFailCause) {
                String apnType = dp.mApnContext.getApnType();
                if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_IMS) &&
                       (mPcscfAddr == null || mPcscfAddr.length <= 0)) {
                    dp.mReason = MtkGsmCdmaPhone.REASON_PCSCF_ADDRESS_FAILED;
                    log("Disconnect with empty P-CSCF address");
                }
            }
        } while (false);
    }

    private boolean hasMdAutoSetupImsCapability() {
        if (mTelDevController != null &&
                mTelDevController.getModem(0) != null &&
                ((MtkHardwareConfig) mTelDevController.getModem(0))
                .hasMdAutoSetupImsCapability() == true) {
            log("hasMdAutoSetupImsCapability: true");
            return true;
        }
        return false;
    }

    // M: [OD over ePDG] @{
    void setConnectionRat(int newRat, String reason) {
        if ((newRat + 1) > MtkPhoneConstants.RAT_TYPE_MAX || newRat < 0) {
            loge("setConnectionRat invalid newRat: " + newRat);
            return;
        }
        log("setConnectionRat newRat: " + newRat + " mRat: " + mRat + " reason: " + reason);
        mRat = newRat;
    }

    int decodeRat(int param) {
        if (param < 0) {
            loge("decodeRat invalid param: " + param);
            return -1;
        }
        return (param / MtkPhoneConstants.RAT_TYPE_KEY) + 1;
    }
    // M: [OD over ePDG] @}

    /**
     * Anchor method to replace states implementation in the state machine initialization procedure.
     */
    @Override
    protected void mtkReplaceStates() {
        mDefaultState = new MtkDcDefaultState();
        mActivatingState = new MtkDcActivatingState();
        mActiveState = new MtkDcActiveState();
    }

    /**
     * Anchor method of updateLinkProperty/DcDefaultState.processMessage
     */
    @Override
    protected LinkProperties mtkGetLinkProperties() {
        return getLinkProperties();
    }

    /**
     * Anchor method of notifyDisconnectCompleted
     */
    @Override
    protected void mtkSetApnContextReason(ApnContext alreadySent, String reason) {
        for (ConnectionParams cp : mApnContexts.values()) {
            ApnContext apnContext = cp.mApnContext;
            if (apnContext == alreadySent &&
                    MtkGsmCdmaPhone.REASON_RA_FAILED.equals(reason)) {
                log("set reason:" + reason);
                apnContext.setReason(reason);
            }
        }
    }

    /**
     * Anchor method of initConnection
     */
    @Override
    protected void mtkCheckDefaultApnRefCount(ApnContext apnContext) {
        if (!mApnContexts.containsKey(apnContext)) {
            checkIfDefaultApnReferenceCountChanged();
        }
    }
}
