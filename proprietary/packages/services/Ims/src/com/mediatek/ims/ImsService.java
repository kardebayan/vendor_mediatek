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

package com.mediatek.ims;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.IBinder;
import android.os.Messenger;
import android.os.Message;
import android.os.PatternMatcher;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.PersistableBundle;
import android.provider.Settings;

import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsCallSessionListener;
import android.telephony.ims.stub.ImsCallSessionImplBase;
import android.telephony.ims.stub.ImsEcbmImplBase;
import android.telephony.ims.stub.ImsUtImplBase;
import android.telephony.ims.stub.ImsMultiEndpointImplBase;
import android.telephony.ims.aidl.IImsCallSessionListener;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import android.text.TextUtils;

import com.android.ims.ImsConfig;
import com.android.ims.ImsManager;
import com.android.ims.ImsServiceBase;
import com.android.ims.ImsServiceClass;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsUt;
import com.android.ims.internal.IImsConfig;
import com.android.ims.internal.IImsService;

import com.android.internal.telephony.CommandsInterface;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.gba.NafSessionKey;
import com.mediatek.ims.config.internal.ImsConfigUtils;
import com.mediatek.ims.ext.DigitsUtil;
import com.mediatek.ims.ext.IImsServiceExt;
import com.mediatek.ims.ext.OpImsServiceCustomizationUtils;
import com.mediatek.ims.internal.ImsMultiEndpointProxy;
import com.mediatek.ims.internal.ImsDataTracker;
import com.mediatek.ims.internal.IMtkImsCallSession;
import com.mediatek.ims.internal.IMtkImsUt;
import com.mediatek.ims.internal.ImsXuiManager;
import com.android.ims.internal.IImsRegistrationListener;
import com.mediatek.ims.ImsAdapter;
import com.mediatek.ims.ImsConstants;
import com.mediatek.ims.ImsServiceCallTracker;
import com.mediatek.ims.ril.ImsCommandsInterface;
import com.mediatek.ims.ril.ImsCommandsInterface.RadioState;
import com.mediatek.ims.ril.ImsRILAdapter;
import com.mediatek.ims.ImsEventPackageAdapter;
import com.mediatek.ims.MtkSuppServExt;
import com.mediatek.ims.WfcReasonInfo;
import com.mediatek.ims.MtkImsConstants;
import com.mediatek.ims.plugin.ExtensionFactory;
import com.mediatek.ims.plugin.ImsManagerOemPlugin;
import com.mediatek.internal.telephony.MtkPhoneConstants;
import com.mediatek.wfo.DisconnectCause;
import com.mediatek.wfo.IWifiOffloadService;
import com.mediatek.wfo.WifiOffloadManager;
import com.mediatek.wfo.IMwiService;
import com.mediatek.wfo.MwisConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.mediatek.ims.ril.OpImsCommandsInterface;
import com.mediatek.ims.feature.MtkImsUtImplBase;
import com.mediatek.ims.internal.op.OpImsServiceFactoryBase;
import com.mediatek.ims.internal.op.OpImsService;
import com.mediatek.ims.plugin.ExtensionFactory;
import com.mediatek.ims.plugin.LegacyComponentFactory;

// SMS-START
import android.provider.Telephony.Sms.Intents;
import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.stub.ImsSmsImplBase;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import com.android.internal.telephony.SmsResponse;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsConstants;
// SMS-END

// EAIC handling
import static com.mediatek.internal.telephony.MtkTelephonyProperties.PROPERTY_TBCW_MODE;
import static com.mediatek.internal.telephony.MtkTelephonyProperties.TBCW_DISABLED;
import static com.mediatek.internal.telephony.MtkTelephonyProperties.TBCW_OFF;

import android.telephony.CarrierConfigManager;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import com.mediatek.ims.MtkImsRegistrationImpl;
import android.util.SparseArray;
import com.mediatek.ims.plugin.OemPluginFactory;
import com.mediatek.ims.plugin.ImsRegistrationOemPlugin;

public class ImsService extends ImsServiceBase {
    private static final String LOG_TAG = "ImsService";
    private static final boolean DBG = true;
    private static final boolean VDBG = false; // STOPSHIP if true
    private static final boolean ENGLOAD = "eng".equals(Build.TYPE);
    private static final boolean SENLOG = TextUtils.equals(Build.TYPE, "user");

    // ImsService Name
    private static final String IMS_SERVICE = "ims";

    private ImsAdapter mImsAdapter = null;
    private ImsCommandsInterface [] mImsRILAdapters = null;
    private ImsCallSessionProxy mPendingMT = null;
    private MtkImsCallSessionProxy mMtkPendingMT = null;

    // For synchronization of private variables
    private Object mLockObj = new Object();
    private Object mLockUri = new Object();
    private Context mContext;

    private static IWifiOffloadService sWifiOffloadService = null;
    private IWifiOffloadServiceDeathRecipient mDeathRecipient =
            new IWifiOffloadServiceDeathRecipient();
    private IWifiOffloadListenerProxy mProxy = null;

    private static HashMap<Integer, MtkImsRegistrationImpl> sMtkImsRegImpl =
            new HashMap<Integer, MtkImsRegistrationImpl>();

    private static HashMap<Integer, MtkSuppServExt> sMtkSSExt =
            new HashMap<Integer, MtkSuppServExt>();

    private int mNumOfPhones = 0;
    private final Handler [] mHandler;

    // Add via onAddRegistrationListener, each phone may have multiple listeners
    // due to different process access ImsManager will have different instance
    private ArrayList<HashSet<IImsRegistrationListener>> mListener =
        new ArrayList<HashSet<IImsRegistrationListener>>();
    private int[] mImsRegInfo;
    private int[] mImsExtInfo;
    private int[] mServiceId;
    private int[] mImsState;
    private int[] mExpectedImsState;
    private int[] mRegErrorCode;
    private int[] mRAN;
    private int[] mIsImsEccSupported;

    /// M: ECBM @{
    private ImsEcbmProxy[] mImsEcbm;
    /// @}

    /// N: IMS Configuration Manager
    private ImsConfigManager mImsConfigManager = null;

    private String mPendingMTCallId;

    private ImsManagerOemPlugin mImsManagerOemPlugin = null;

    //***** Event Constants
    private static final int EVENT_IMS_REGISTRATION_INFO = 1;
    protected static final int EVENT_RADIO_NOT_AVAILABLE    = 2;
    protected static final int EVENT_SET_IMS_ENABLED_DONE   = 3;
    protected static final int EVENT_SET_IMS_DISABLE_DONE   = 4;
    protected static final int EVENT_IMS_DISABLED_URC   = 5;
    private static final int EVENT_VIRTUAL_SIM_ON = 6;
    protected static final int EVENT_INCOMING_CALL_INDICATION = 7;
    protected static final int EVENT_CALL_INFO_INDICATION = 8;
    // protected static final int EVENT_CALL_RING = 9;
    protected static final int EVENT_IMS_ENABLING_URC   = 10;
    protected static final int EVENT_IMS_ENABLED_URC   = 11;
    protected static final int EVENT_IMS_DISABLING_URC   = 12;
    ///M : WFC @{
    protected static final int EVENT_SIP_CODE_INDICATION = 13;
    protected static final int EVENT_SIP_CODE_INDICATION_DEREG = 14;
    /// @}
    /// M: Event for network initiated USSI @{
    protected static final int EVENT_ON_NETWORK_INIT_USSI = 15;
    /// @}
    /// M: Event for IMS deregistration @{
    protected static final int EVENT_IMS_DEREG_DONE = 16;
    protected static final int EVENT_IMS_DEREG_URC = 17;
    /// @}
    /// M: Event for radio off @{
    protected static final int EVENT_RADIO_OFF = 18;
    /// @}
    /// M: Event for radio on @{
    protected static final int EVENT_RADIO_ON = 19;
    /// @}
    /// M: Event for IMS RTP Report @{
    protected static final int EVENT_IMS_RTP_INFO_URC = 20;
    /// @}

    /// M: Event for IMS registration report @{
    protected static final int EVENT_SET_IMS_REGISTRATION_REPORT_DONE = 21;
    /// @}

    /// M: Sync volte setting value. @{
    protected static final int EVENT_IMS_VOLTE_SETTING_URC = 22;
    /// @}

    /// M: Event fori 93 IMS SS @{
    protected static final int EVENT_RUN_GBA = 23;
    /// @}

    /// M: Event for XUI update @{
    protected static final int EVENT_SELF_IDENTIFY_UPDATE = 24;
    /// @}

    /// M: Event for IMS ECC support message @{
    protected static final int EVENT_IMS_SUPPORT_ECC_URC = 25;
    /// @}

    /// M: Event for start gba service @{
    protected static final int EVENT_START_GBA_SERVICE = 26;
    /// @}
    protected static final int EVENT_INIT_CALL_SESSION_PROXY = 27;
    private boolean mIsPendingMTTerminated = false;
    private ImsCallProfile mImsCallProfile;

    // SMS-START
    protected static final int EVENT_SEND_SMS_DONE = 28;
    // SMS-END

    protected static final int EVENT_UT_CAPABILITY_CHANGE = 29;

    protected static final int EVENT_IMS_SMS_STATUS_REPORT = 30;
    protected static final int EVENT_IMS_SMS_NEW_SMS = 31;
    protected static final int EVENT_IMS_SMS_NEW_CDMA_SMS = 32;
    /// M: Event for IMS STK call @{
    protected static final int EVENT_INCOMING_CALL_ADDITIONAL_INFO_INDICATION = 33;
    /// @}

    /// M: 93 IMS SS. @{
    private static final int IMS_SS_TIMEOUT_ERROR = 1;
    private static final int IMS_SS_INTERRUPT_ERROR = 2;
    private static final int IMS_SS_CMD_ERROR = 3;
    private static final int IMS_SS_CMD_SUCCESS = 4;

    private class NafSessionKeyResult {
        NafSessionKey nafSessionKey = null;
        int cmdResult = IMS_SS_TIMEOUT_ERROR;
        Object lockObj = new Object();
    }
    /// @}

    private static final int IMS_ALLOW_INCOMING_CALL_INDICATION = 0;
    private static final int IMS_DISALLOW_INCOMING_CALL_INDICATION = 1;

    private static final int MT_CALL_DIAL_IMS_STK = 100;

    //***** IMS Feature Support
    private static final int IMS_VOICE_OVER_LTE = 1;
    private static final int IMS_RCS_OVER_LTE = 2;
    private static final int IMS_SMS_OVER_LTE = 4;
    private static final int IMS_VIDEO_OVER_LTE = 8;
    private static final int IMS_VOICE_OVER_WIFI = 16;

    //***** IMS REG URI type
    private static final int IMS_REG_SIP_URI_TYPE_MSISDN = 0;
    private static final int IMS_REG_SIP_URI_TYPE_IMSI = 1;

    //Refer to ImsConfig FeatureConstants
    private static final int IMS_MAX_FEATURE_SUPPORT_SIZE = 6;

    /// M: Sync volte setting value. @{
    private static final String PROPERTY_IMSCONFIG_FORCE_NOTIFY =
                                    "vendor.ril.imsconfig.force.notify";
    /// @}

    private boolean[] mTempDisableWFC;

    /// M: ims radio registraion extension type info @{
    private static final String PROPERTY_IMS_REG_EXTINFO = "ril.ims.extinfo";

    private ImsDataTracker mImsDataTracker;

    private ImsEventPackageAdapter[] mImsEvtPkgAdapter;

    private Map<Integer, ImsMultiEndpointProxy> mMultiEndpointMap =
            new HashMap<Integer, ImsMultiEndpointProxy>();

    ImsCallSessionProxy mNwInitUssiSession = null;

    // For operator add-on
    private OpImsService mOpExt = null;

    // SMS-START
    private ArrayList<HashSet<IImsSmsListener>> mImsSmsListener =
            new ArrayList<HashSet<IImsSmsListener>>();
    // SMS-END

    private static ImsService sInstance = null;
    protected static final Object mLock = new Object();

    /// M: Sync volte setting value. @{
    private final static String CONFIG_EXTRA_PHONE_ID = "phone_id";
    private boolean mRegisterSubInfoChange = false;
    private boolean[] mWaitSubInfoChange;
    private boolean[] mVolteEnable;
    private int mWaitFeatureChange = 0;
    /// @ }

    // this variables is used to keep mtk call session proxy which is not mampping to aosp call session proxy
    // it may happen when upper layer create aosp proxy individually
    // and we need upper layer to complete the mapping in onCreateMtkCallSessionProxy()
    private static Map<Object, Object> mPendingMtkImsCallSessionProxy = new HashMap<>();


    public static ImsService getInstance(Context context) {
        synchronized (mLock) {
            if (sInstance == null && context != null) {
                sInstance = new ImsService(context);
                sInstance.log("ImsService is created!");
            }
            return sInstance;
        }
    }

    /// M: Simulate IMS Registration @{
    private boolean mImsRegistry = false;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            log("[onReceive] action=" + intent.getAction());
            ///M : WFC @{
            if ("ACTION_IMS_SIMULATE".equals(intent.getAction())){
            /// @}
                mImsRegistry = intent.getBooleanExtra("registry", false);
                logw("Simulate IMS Registration: " + mImsRegistry);
                int phoneId = ImsCommonUtil.getMainCapabilityPhoneId();
                int[] result = new int[] {
                    (mImsRegistry ? 1 : 0),
                    15,
                    phoneId};
                AsyncResult ar = new AsyncResult(null, result, null);
                mHandler[phoneId].sendMessage(
                  mHandler[phoneId].obtainMessage(EVENT_IMS_REGISTRATION_INFO, ar));
            } else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                if (ImsCommonUtil.supportMdAutoSetupIms() == false) {
                    bindAndRegisterWifiOffloadService();
                } else {
                    bindAndRegisterMWIService();
                }
                // send IMS_SERVICE_UP intent again in case previous is before BOOT_COMPLETE
                for(int i = 0; i < mNumOfPhones; i++) {
                    if (mImsState[i] == MtkPhoneConstants.IMS_STATE_ENABLE) {
                        Intent newIntent = new Intent(ImsManager.ACTION_IMS_SERVICE_UP);
                        newIntent.putExtra(ImsManager.EXTRA_PHONE_ID, i);
                        mContext.sendBroadcast(newIntent);
                        log("broadcast IMS_SERVICE_UP for phone=" + i);
                    }
                }
            } else if (ImsConstants.SELF_IDENTIFY_UPDATE.equals(intent.getAction())) {
                int extraPhoneId = intent.getIntExtra(ImsManager.EXTRA_PHONE_ID,
                        SubscriptionManager.INVALID_PHONE_INDEX);
                log("SELF_IDENTIFY_UPDATE: extraPhoneId=" + extraPhoneId);
                if (extraPhoneId != SubscriptionManager.INVALID_PHONE_INDEX) {
                    mHandler[extraPhoneId].sendMessage(
                            mHandler[extraPhoneId].obtainMessage(EVENT_SELF_IDENTIFY_UPDATE));
                }
            } else if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(intent.getAction())) {
                String simStatus = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simStatus)) {
                    int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY,
                            SubscriptionManager.INVALID_PHONE_INDEX);
                    if (SubscriptionManager.isValidPhoneId(phoneId)) {
                        resetXuiAndNotify(phoneId);
                    }
                }
            }
            log("[onReceive] finished action=" + intent.getAction());
        }
    };
    /// @}

    public ImsService(Context context) {
        logi("init");
        mContext = context;
        /// Get Number of Phones
        mNumOfPhones = TelephonyManager.getDefault().getPhoneCount();
        /// M: keep old logic for 92gen and before
        if (ImsCommonUtil.supportMdAutoSetupIms() == false) {
            mImsAdapter = new ImsAdapter(context);
        }

        mHandler = new MyHandler[mNumOfPhones];
        mImsRILAdapters = new ImsCommandsInterface[mNumOfPhones];
        for(int i = 0; i < mNumOfPhones; i++) {
            mHandler[i] = new MyHandler(i);
            ImsRILAdapter ril = new ImsRILAdapter(context, i);

            /// register for radio state changed
            ril.registerForNotAvailable(mHandler[i], EVENT_RADIO_NOT_AVAILABLE, null);
            ril.registerForOff(mHandler[i], EVENT_RADIO_OFF, null);
            ril.registerForOn(mHandler[i], EVENT_RADIO_ON, null);

            ril.registerForImsRegistrationInfo(mHandler[i], EVENT_IMS_REGISTRATION_INFO, null);
            ril.registerForImsEnableStart(mHandler[i], EVENT_IMS_ENABLING_URC, null);
            ril.registerForImsEnableComplete(mHandler[i], EVENT_IMS_ENABLED_URC, null);
            ril.registerForImsDisableStart(mHandler[i], EVENT_IMS_DISABLING_URC, null);
            ril.registerForImsDisableComplete(mHandler[i], EVENT_IMS_DISABLED_URC, null);
            ril.setOnIncomingCallIndication(mHandler[i], EVENT_INCOMING_CALL_INDICATION, null);
            ril.registerForCallProgressIndicator(mHandler[i], EVENT_SIP_CODE_INDICATION, null);
            ril.registerForImsDeregisterComplete(mHandler[i], EVENT_IMS_DEREG_URC, null);
            /// M: register for IMS ECC support event @{
            ril.registerForImsEccSupport(mHandler[i], EVENT_IMS_SUPPORT_ECC_URC, null);
            /// @}

            /// M: Listen for network initiated USSI @{
            ril.setOnNetworkInitUSSI(mHandler[i], EVENT_ON_NETWORK_INIT_USSI, null);
            /// @}
            /// M: register for IMS RTP report event @{
            ril.registerForImsRTPInfo(mHandler[i], EVENT_IMS_RTP_INFO_URC, null);
            /// @}
            /// M: Sync volte setting value. @{
            ril.registerForVolteSettingChanged(mHandler[i], EVENT_IMS_VOLTE_SETTING_URC, null);
            /// @}
            /// M: XUI URC will be handled by ImsService after 93gen
            if (ImsCommonUtil.supportMdAutoSetupIms()) {
                ril.registerForXuiInfo(mHandler[i], EVENT_SELF_IDENTIFY_UPDATE, null);
            }
            // Register SMS over IMS status report and New Ims SMS indication
            ril.setOnSmsStatus(mHandler[i], EVENT_IMS_SMS_STATUS_REPORT, null);
            ril.setOnNewSms(mHandler[i], EVENT_IMS_SMS_NEW_SMS, null);
            ril.setOnNewCdmaSms(mHandler[i], EVENT_IMS_SMS_NEW_CDMA_SMS, null);
            /// M: register for STK IMS call event @{
            ril.setOnIncomingCallAdditionalInfo(mHandler[i], EVENT_INCOMING_CALL_ADDITIONAL_INFO_INDICATION, null);
            /// @}

            mImsRILAdapters[i] = ril;
        }

        /// M: create for 93MD events handle.
        if (ImsCommonUtil.supportMdAutoSetupIms()) {
            log("Initializing");
            mImsDataTracker = new ImsDataTracker(context, mImsRILAdapters);
        }

        /// M: Simulate IMS Registration @{
        final IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_IMS_SIMULATE");
        /// @}
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(ImsConstants.SELF_IDENTIFY_UPDATE);
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);

        context.registerReceiver(mBroadcastReceiver, filter);

        mImsRegInfo = new int[mNumOfPhones];
        mImsExtInfo = new int[mNumOfPhones];
        mServiceId = new int[mNumOfPhones];
        mImsState = new int[mNumOfPhones];
        mExpectedImsState = new int[mNumOfPhones];
        mRegErrorCode = new int[mNumOfPhones];
        mRAN = new int[mNumOfPhones];
        mImsEcbm = new ImsEcbmProxy[mNumOfPhones];
        mImsEvtPkgAdapter = new ImsEventPackageAdapter[mNumOfPhones];
        mImsConfigManager = new ImsConfigManager(context, mImsRILAdapters);
        mIsImsEccSupported = new int[mNumOfPhones];
        /// M: Sync volte setting value. @{
        mWaitSubInfoChange = new boolean[mNumOfPhones];
        mVolteEnable = new boolean[mNumOfPhones];
        /// @ }

        HandlerThread ssHandlerThread = new HandlerThread("MtkSSExt");
        ssHandlerThread.start();
        Looper sslooper = ssHandlerThread.getLooper();

        for (int i = 0; i < mNumOfPhones; i++) {
            mListener.add(new HashSet<IImsRegistrationListener>());
            mImsRegInfo[i] = ServiceState.STATE_POWER_OFF;
            mImsExtInfo[i] = 0;
            mServiceId[i] = i+1;
            mImsState[i] = MtkPhoneConstants.IMS_STATE_DISABLED;
            mExpectedImsState[i] = MtkPhoneConstants.IMS_STATE_DISABLED;
            mRegErrorCode[i] = ImsReasonInfo.CODE_UNSPECIFIED;
            mRAN[i] = WifiOffloadManager.RAN_TYPE_MOBILE_3GPP;
            mImsEcbm[i] = new ImsEcbmProxy(mContext, mImsRILAdapters[i], i);
            mImsConfigManager.init(i);
            mIsImsEccSupported[i] = 0;
            if (ImsCommonUtil.supportMdAutoSetupIms()) {
                sMtkSSExt.put(i, new MtkSuppServExt(mContext, i, this, sslooper));
            }

            mImsEvtPkgAdapter[i] =
                new ImsEventPackageAdapter(mContext, mHandler[i], mImsRILAdapters[i], i);
            /// M: Sync volte setting value. @{
            mWaitSubInfoChange[i] = false;
            mVolteEnable[i] = false;
            /// @ }
        }

        // init flow, initialze ims capability at constructor. for multiple ims
        // do this for all phones
        if (ImsCommonUtil.supportMims() == false) {
            final int mainPhoneId = ImsCommonUtil.getMainCapabilityPhoneId();
            log("getMainCapabilityPhoneId: mainPhoneId = " + mainPhoneId);

            // Error handling in case ImsService process died
            mImsRILAdapters[mainPhoneId].setImsRegistrationReport(
                    mHandler[mainPhoneId].obtainMessage(EVENT_SET_IMS_REGISTRATION_REPORT_DONE));
            if (ImsCommonUtil.supportMdAutoSetupIms() == false) {
                if (SubscriptionManager.isValidPhoneId(mainPhoneId)) {
                    initImsAvailability(mainPhoneId, 0, EVENT_SET_IMS_ENABLED_DONE,
                            EVENT_SET_IMS_DISABLE_DONE);
                }
            }
        } else {
            for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {

                // Error handling in case ImsService process died
                mImsRILAdapters[i].setImsRegistrationReport(
                        mHandler[i].obtainMessage(EVENT_SET_IMS_REGISTRATION_REPORT_DONE));
                if (ImsCommonUtil.supportMdAutoSetupIms() == false) {
                    initImsAvailability(i, i, EVENT_SET_IMS_ENABLED_DONE,
                            EVENT_SET_IMS_DISABLE_DONE);
                }
            }
        }
        /// @}

        // For operator add-on
        mOpExt = OpImsServiceFactoryBase.getInstance().makeOpImsService();

        // Load Extension Libraries
        ExtensionFactory.makeOemPluginFactory();
        ExtensionFactory.makeExtensionPluginFactory();

        startWfoService();
    }

    // ImsService operator plugin instance
    private IImsServiceExt getOpImsService() {
        return OpImsServiceCustomizationUtils.getOpFactory(mContext).makeImsServiceExt(mContext);
    }

    private void enableImsAdapter(int phoneId) {
        mImsAdapter.enableImsAdapter(phoneId);
    }

    private void disableImsAdapter(int phoneId, boolean isNormalDisable) {
        mImsAdapter.disableImsAdapter(phoneId, isNormalDisable);
    }

    private void startWfoService() {
        mTempDisableWFC = new boolean[TelephonyManager.getDefault().getPhoneCount()];

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Intent wfoIntent = new Intent("com.mediatek.START_WFO");
                Intent wfoIntent = new Intent();
                // wfoIntent.setPackage("com.mediatek.wfo.impl");
                ComponentName componentName = new ComponentName(
                        "com.mediatek.wfo.impl", "com.mediatek.wfo.impl.WfoService");
                wfoIntent.setComponent(componentName);
                mContext.bindService(wfoIntent, mConnection, Context.BIND_AUTO_CREATE);
            }
        }).start();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            log("WfoService onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            log("WfoService onServiceFailed");
        }
    };

    private void startGbaService() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                log("start gba service");
                int phoneId = 0;
                Intent gbaIntent = new Intent("com.mediatek.START_GBA");
                gbaIntent.setPackage("com.mediatek.gba");

                if (!mContext.bindService(gbaIntent, mGbaConnection, Context.BIND_AUTO_CREATE)) {
                    mHandler[phoneId].sendMessageDelayed(mHandler[phoneId].obtainMessage(EVENT_START_GBA_SERVICE),
                            5000);
                }
            }
        }).start();
    }

    private ServiceConnection mGbaConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            log("GbaService onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            log("GbaService onServiceFailed");
        }
    };

    @Override
    protected int onOpen(int phoneId, int serviceClass, PendingIntent incomingCallIntent,
            IImsRegistrationListener listener) {
        log("onOpen: phoneId=" + phoneId + " serviceClass=" + serviceClass +
                " listener=" + listener);
        int serviceId = mapPhoneIdToServiceId(phoneId);
        englog("onOpen: serviceId=" + serviceId);
        return serviceId;
    }

    @Override
    protected void onClose(int serviceId) {

        synchronized(mLockObj) {
            int phoneId = serviceId;

            try {
                if (mImsEcbm[phoneId] != null) {
                    mImsEcbm[phoneId].getImsEcbm().setListener(null);
                }
            } catch (RemoteException e) {
                // error handling. Currently no-op
            }

        }
    }

    /**
     * Checks if the IMS service has successfully registered to the IMS network
     * with the specified service & call type.
     *
     * TODO check each serviceType and callType
     */
    @Override
    protected boolean onIsConnected(int serviceId, int serviceType, int callType) {
        log("onIsConnected: serviceId=" + serviceId + ", serviceType=" + serviceType
                + ", callType=" + callType);
        int phoneId = serviceId;
        return (mImsRegInfo[phoneId] == ServiceState.STATE_IN_SERVICE);
    }

    /**
     * Checks if the specified IMS service is opend.
     *
     * @return true if the specified service id is opened; false otherwise
     */
    @Override
    protected boolean onIsOpened(int serviceId) {
        log("onIsOpened: serviceId=" + serviceId);
        int phoneId = serviceId;
        HashSet<IImsRegistrationListener> listeners = mListener.get(phoneId);
        /* onOpen will add RegistrationListener into mListener set*/
        return (listeners.size() > 0);
    }

    /**
     * We assume no one will use this API and hence no implementation
     */
    @Override
    protected void onSetRegistrationListener(int serviceId, IImsRegistrationListener listener) {
        log("onSetRegistrationListener: serviceId=" + serviceId + ", listener=" + listener);
    }

    @Override
    protected void onAddRegistrationListener(
            int phoneId, int serviceType, IImsRegistrationListener listener) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("onAddRegistrationListener() error phoneId:" + phoneId);
            return;
        }
        log("onAddRegistrationListener: phoneId=" + phoneId + " serviceType=" + serviceType +
                " listener=" + listener);
        HashSet<IImsRegistrationListener> listeners = mListener.get(phoneId);
        synchronized (listeners) {
            if (listeners.contains(listener)) {
                log("listener already exist");
            } else {
                listeners.add(listener);
                log("listener set size=" + listeners.size());
            }
        }
        if (mImsRegInfo[phoneId] != ServiceState.STATE_POWER_OFF) {
            notifyRegistrationStateChange(phoneId, mImsRegInfo[phoneId], true);
        }

        if ((mImsRegInfo[phoneId] == ServiceState.STATE_IN_SERVICE)) {
            notifyRegistrationCapabilityChange(phoneId, mImsExtInfo[phoneId], true);

            ImsXuiManager xuiManager = ImsXuiManager.getInstance();
            notifyRegistrationAssociatedUriChange(xuiManager, phoneId);
        }
        int resultEvent = 0;
        log("onAddRegistrationListener mIsImsEccSupported=" + mIsImsEccSupported[phoneId]);
        if (listener != null) {
            if (mIsImsEccSupported[phoneId] > 0) {
                resultEvent = MtkImsConstants
                        .SERVICE_REG_CAPABILITY_EVENT_ECC_SUPPORT;
            } else {
                resultEvent = MtkImsConstants
                        .SERVICE_REG_CAPABILITY_EVENT_ECC_NOT_SUPPORT;
            }
            try {
                listener.registrationServiceCapabilityChanged(
                            ImsServiceClass.MMTEL, resultEvent);
            } catch (RemoteException e) {
                // error handling. Currently no-op
              loge("onAddRegistrationListener RemoteException " + e);
            }
        }
    }

    @Override
    public ImsCallProfile onCreateCallProfile(int serviceId, int serviceType, int callType) {
        return new ImsCallProfile(serviceType, callType);
    }

    public IImsCallSession onCreateCallSession(int serviceId, ImsCallProfile profile,
            IImsCallSessionListener listener) {
        // This API is for outgoing call to create IImsCallSession
        return onCreateCallSessionProxy(serviceId, profile, listener).getServiceImpl();
    }

    public ImsCallSessionProxy onCreateCallSessionProxy(int serviceId, ImsCallProfile profile,
            IImsCallSessionListener listener) {

        log("onCreateCallSessionProxy: serviceId =" + serviceId + " profile =" + profile + " listener =" + listener);

        ImsCallSessionListener sessionListener = null;
        if (listener != null) {
            sessionListener = new ImsCallSessionListener(listener);
        }
        // This API is for outgoing call to create ImsCallSessionProxy
        int phoneId = serviceId;
        ImsCallSessionProxy cs = new ImsCallSessionProxy(
                                    mContext, profile, sessionListener, this, mHandler[phoneId],
                                    mImsRILAdapters[phoneId], phoneId);
        MtkImsCallSessionProxy mtk_cs = new MtkImsCallSessionProxy(
                                        mContext, profile, sessionListener, this, mHandler[phoneId],
                                        mImsRILAdapters[phoneId], phoneId);
        mtk_cs.setAospCallSessionProxy(cs);
        cs.setMtkCallSessionProxy(mtk_cs);

        log("onCreateCallSessionProxy: cs.getServiceImpl() = " + cs.getServiceImpl());

        mPendingMtkImsCallSessionProxy.put(cs.getServiceImpl(), mtk_cs);

        return cs;
    }

    // Relay from MtkImsService
    public IMtkImsCallSession onCreateMtkCallSession(int phoneId, ImsCallProfile profile,
                                                  IImsCallSessionListener listener, IImsCallSession aospCallSessionImpl) {

        // This API is for outgoing call to create IMtkImsCallSession
        return onCreateMtkCallSessionProxy(phoneId, profile, listener, aospCallSessionImpl).getServiceImpl();
    }

    public MtkImsCallSessionProxy onCreateMtkCallSessionProxy(int phoneId, ImsCallProfile profile,
                                                  IImsCallSessionListener listener, IImsCallSession aospCallSessionImpl) {

        // This API is for outgoing call to create MtkImsCallSessionProxy
        log("onCreateMtkCallSessionProxy: aospCallSessionImpl = " + aospCallSessionImpl);
        log("onCreateMtkCallSessionProxy: containsKey = " + mPendingMtkImsCallSessionProxy.containsKey(aospCallSessionImpl));

        MtkImsCallSessionProxy mtk_cs = null;

        if (mPendingMtkImsCallSessionProxy.containsKey(aospCallSessionImpl)) {

            mtk_cs = (MtkImsCallSessionProxy) mPendingMtkImsCallSessionProxy.get(aospCallSessionImpl);
            mPendingMtkImsCallSessionProxy.remove(aospCallSessionImpl);
        }

        return mtk_cs;
    }

    // Relay from MtkImsService
    /**
     * Used to trigger GBA in native SS solution.
     */
    protected NafSessionKey onRunGbaAuthentication(String nafFqdn,
            byte[] nafSecureProtocolId, boolean forceRun, int netId, int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("onRunGbaAuthentication() error phoneId:" + phoneId + ", use phone 0");
            phoneId = 0;
        }
        NafSessionKeyResult result = new NafSessionKeyResult();
        Message msg = mHandler[phoneId].obtainMessage(EVENT_RUN_GBA, result);

        synchronized(result.lockObj) {
            mImsRILAdapters[phoneId].runGbaAuthentication(nafFqdn,
                   ImsCommonUtil.bytesToHex(nafSecureProtocolId), forceRun, netId, msg);
            try {
                result.lockObj.wait(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                result.cmdResult = IMS_SS_INTERRUPT_ERROR;
            }
        }
        log("onRunGbaAuthentication complete, nafSessionKey:" + result.nafSessionKey +
                ", cmdResult:" + result.cmdResult);

        return result.nafSessionKey;
    }

    @Override
    protected IImsCallSession onGetPendingCallSession(int serviceId, String callId) {

        int phoneId = serviceId;

        log("onGetPendingCallSession() : serviceId = " + serviceId + ", callId = " + callId);

        // This API is for incoming call to create IImsCallSession
        if (mPendingMT == null) {
            return null;
        }

        IImsCallSession pendingMTsession = mPendingMT.getServiceImpl();

        try {
            if (pendingMTsession.getCallId().equals(callId)) {
                mPendingMT = null;
                return pendingMTsession;
            }
        } catch (RemoteException e) {
            // error handling. Currently no-op
        }

        return null;
    }

    /**
     * Ut interface for the supplementary service configuration.
     */
    @Override
    protected IImsUt onGetUtInterface(int phoneId) {
        IImsUt inst = null;
        if (ImsCommonUtil.supportMdAutoSetupIms()) {
            inst = ImsUtImpl.getInstance(mContext, phoneId, this).getInterface();
        } else {
            LegacyComponentFactory factory = ExtensionFactory.makeLegacyComponentFactory();
            ImsUtImplBase utImpl = factory.makeImsUt(mContext, phoneId, this);
            if (utImpl != null) {
                inst = utImpl.getInterface();
            }
        }

        return inst;
    }

    // Relay from MtkImsService
    protected IMtkImsUt onGetMtkUtInterface(int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("onGetMtkUtInterface() error phoneId:" + phoneId + ", use phone 0");
            phoneId = 0;
        }
        IMtkImsUt inst = null;
        if (ImsCommonUtil.supportMdAutoSetupIms()) {
            inst = MtkImsUtImpl.getInstance(mContext, phoneId, this).getInterface();
        } else {
            LegacyComponentFactory factory = ExtensionFactory.makeLegacyComponentFactory();
            MtkImsUtImplBase utImpl = factory.makeMtkImsUt(mContext, phoneId, this);
            if (utImpl != null) {
                inst = utImpl.getInterface();
            }
        }

        return inst;
    }

    /**
     * Config interface to get/set IMS service/capability parameters.
     */
    @Override
    public IImsConfig onGetConfigInterface(int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("onGetConfigInterface() error phoneId:" + phoneId + ", use phone 0");
            phoneId = 0;
        }
        if (ImsCommonUtil.supportMdAutoSetupIms() == false) {
            bindAndRegisterWifiOffloadService();
        } else {
            bindAndRegisterMWIService();
        }

        return mImsConfigManager.get(phoneId);
    }


    /**
     * Used for turning on IMS when its in OFF state.
     */

    public void enableIms(int phoneId) {
        log("turnOnIms phoneId = " + phoneId);
        onTurnOnIms(phoneId);
    }

    @Override
    protected void onTurnOnIms(int phoneId) {
        log("turnOnIms phoneId = " + phoneId);
    }

    /**
     * Used for turning off IMS when its in ON state.
     * When IMS is OFF, device will behave as CSFB'ed.
     */
    public void disableIms(int phoneId) {
        log("turnOffIms, phoneId = " + phoneId);
        onTurnOffIms(phoneId);
    }

    @Override
    protected void onTurnOffIms(int phoneId) {
        log("turnOffIms, phoneId = " + phoneId);
    }

    /**
     * ECBM interface for Emergency Callback mode mechanism.
     */
    @Override
    protected IImsEcbm onGetEcbmInterface(int serviceId) {
        int phoneId = serviceId;

        if (mImsEcbm[phoneId] == null) {
            mImsEcbm[phoneId] = new ImsEcbmProxy(mContext, mImsRILAdapters[phoneId], phoneId);
        }
        return mImsEcbm[phoneId].getImsEcbm();
    }

    /**
     * ECBM interface for Emergency Callback mode mechanism.
     */
    public ImsEcbmImplBase onGetEcbmProxy(int serviceId) {
        int phoneId = serviceId;

        if (mImsEcbm[phoneId] == null) {
            mImsEcbm[phoneId] = new ImsEcbmProxy(mContext, mImsRILAdapters[phoneId], phoneId);
        }
        return mImsEcbm[phoneId];
    }

    /**
      * Used to set current TTY Mode.
      */
    @Override
    protected void onSetUiTTYMode(int serviceId, int uiTtyMode, Message onComplete) {
        log("onSetUiTTYMode: " + uiTtyMode);
        int phoneId = serviceId;

        return;
    }

    /**
      * MultiEndpoint interface for DEP.
      */
    @Override
    protected IImsMultiEndpoint onGetMultiEndpointInterface(int serviceId) {
        int phoneId = serviceId;

        Rlog.d(LOG_TAG, "onGetMultiEndpointInterface serviceId is " + serviceId);
        if (mMultiEndpointMap.containsKey(serviceId)) {
            return mMultiEndpointMap.get(serviceId).getIImsMultiEndpoint();
        }
        ImsMultiEndpointProxy instance = new ImsMultiEndpointProxy(mContext);
        Rlog.d(LOG_TAG, "onGetMultiEndpointInterface instance is " + instance);
        mMultiEndpointMap.put(serviceId, instance);
        return instance.getIImsMultiEndpoint();
    }

    public ImsMultiEndpointImplBase onGetMultiEndpointProxy(int serviceId) {
        int phoneId = serviceId;

        Rlog.d(LOG_TAG, "onGetMultiEndpointProxy serviceId is " + serviceId);
        if (mMultiEndpointMap.containsKey(serviceId)) {
            return mMultiEndpointMap.get(serviceId);
        }
        ImsMultiEndpointProxy instance = new ImsMultiEndpointProxy(mContext);
        Rlog.d(LOG_TAG, "onGetMultiEndpointProxy instance is " + instance);
        mMultiEndpointMap.put(serviceId, instance);
        return instance;
    }

    /**
     * Query ims enable/disable status.
     *@return ims status
     *@hide
     */
    protected int getImsState(int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("getImsState() error phoneId:" + phoneId + ", use phone 0");
            phoneId = 0;
        }
        return mImsState[phoneId];
    }

    /**
     * Query ims reg ext info.
     *@return reg ext info
     *@hide
     */
    protected int getImsRegUriType(int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("getImsRegUriType() error phoneId:" + phoneId + ", use phone 0");
            phoneId = 0;
        }
        int uri_type = IMS_REG_SIP_URI_TYPE_IMSI;
        String key = PROPERTY_IMS_REG_EXTINFO + phoneId;

        if ((mImsRegInfo[phoneId] == ServiceState.STATE_IN_SERVICE)) {
            uri_type = SystemProperties.getInt(key, IMS_REG_SIP_URI_TYPE_IMSI);
        }
        log("getImsRegUriType, phoneId = " + phoneId + "uri_type =" + uri_type);

        return uri_type;
    }

    /**
     * Use to hang up all calls .
     *@hide
     */
    protected void onHangupAllCall(int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("onHangupAllCall() error phoneId:" + phoneId);
            return;
        }
        mImsRILAdapters[phoneId].hangupAllCall(null);
    }

    /**
     * Used to deregister IMS.
     */
    protected void deregisterIms(int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("deregisterIms() error phoneId:" + phoneId);
            return;
        }
        log("deregisterIms, phoneId = " + phoneId);
        if (ImsCommonUtil.supportMims() == false) {
            phoneId = ImsCommonUtil.getMainCapabilityPhoneId();
            log("deregisterIms, MainCapabilityPhoneId = " + phoneId);
        }
        mImsRILAdapters[phoneId].deregisterIms(
                mHandler[phoneId].obtainMessage(EVENT_IMS_DEREG_DONE));
    }

    /**
     * Used to deregister IMS for Volte SS.
     */
    public void deregisterImsWithCause(int phoneId, int cause) {
        log("deregisterImsWithCause, phoneId = " + phoneId + " cause = " + cause);

        if (ImsCommonUtil.supportMims() == false) {
            phoneId = ImsCommonUtil.getMainCapabilityPhoneId();
            log("deregisterImsWithCause, MainCapabilityPhoneId = " + phoneId);
        }
        mImsRILAdapters[phoneId].deregisterImsWithCause(cause,
                mHandler[phoneId].obtainMessage(EVENT_IMS_DEREG_DONE));
    }

    /**
     * Used to update radio state.
     *
     * @param radioState int value cast from RadioState
     * @param phoneId  to indicate which phone.
     */
    public void updateRadioState(int radioState, int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("updateRadioState() error phoneId:" + phoneId);
            return;
        }
        log("updateRadioState, phoneId = " + phoneId + " radioState = " + radioState);
        if (ImsCommonUtil.supportMdAutoSetupIms()) {
            if (RadioState.RADIO_UNAVAILABLE.ordinal() != radioState) {
                if (mImsManagerOemPlugin == null) {
                    mImsManagerOemPlugin = ExtensionFactory.makeOemPluginFactory()
                            .makeImsManagerPlugin(mContext);
                }

                mImsManagerOemPlugin.updateImsServiceConfig(mContext, phoneId, true);
            }
        } else {
            bindAndRegisterWifiOffloadService();

            if (sWifiOffloadService != null) {
                try {
                    sWifiOffloadService.updateRadioState(phoneId, radioState);
                } catch (RemoteException e) {
                    loge("can't update radio state");
                }
            } else {
                loge("can't get WifiOffloadService");
            }
        }
        //englog("updateRadioState done");
    }

    /**
     * Use to map phoneId to serviceId.
     * @param phoneId Phone ID
     * @return serviceId
     *@hide
     */
    public int mapPhoneIdToServiceId(int phoneId) {
        return (phoneId + 1);
    }

    /**
     * Use to query ims service state .
     *@return mImsRegInfo for service state information.
     *@hide
     */
    public int getImsServiceState(int phoneId) {
        if (ImsCommonUtil.supportMims() == false) {
            phoneId = ImsCommonUtil.getMainCapabilityPhoneId();
        }

        return mImsRegInfo[phoneId];
    }

    /**
     * Use to query multiple ims support count.
     *@return modem multi ims count
     */
    public int getModemMultiImsCount() {
        log("getModemMultiImsCount");
        int mdMultiImsCount =
            SystemProperties.getInt(ImsConstants.PROPERTY_MD_MULTI_IMS_SUPPORT, -1);
        log("mdMultiImsCount=" + mdMultiImsCount);
        if (mdMultiImsCount == -1) {
            logw("MD Multi IMS Count not initialized");
        }
        return mdMultiImsCount;
    }

    /**
     *create wifiOffloadListnerProxy.
     *@return return wifiOffloadLisetnerProxy
     *@hide
     */
    private IWifiOffloadListenerProxy createWifiOffloadListenerProxy() {
        if (mProxy == null) {
            log("create WifiOffloadListenerProxy");
            mProxy = new IWifiOffloadListenerProxy();
        }
        return mProxy;
    }

    /**
     *transfer sip error cause to wfc specified error cause
     *@param sipErrorCode sip error code.
     *@param sipMethod sip operration. (0:REG, 9:SUBSCRIBE)
     *@param sipReasonText sip reason text. ("BLOCK_WIFI_REG09")
     *@return return wfcRegErrorCode which is used in AP side.
     *@hide
     */
    private int mapToWfcRegErrorCause(int sipErrorCode, int sipMethod, String sipReasonText) {

        int wfcRegErrorCode = WfcReasonInfo.CODE_UNSPECIFIED;

        switch (sipErrorCode) {
            case 403:
                if (sipMethod == 9 && sipReasonText.equals("SHOW_WIFI_REG09")) {
                    /* REG09: Missing 911 Address */
                    wfcRegErrorCode = WfcReasonInfo.CODE_WFC_911_MISSING;
                } else if (sipMethod == 0 && sipReasonText.equals(
                    "WiFi Calling Not Allowed from this Region")) {

                    wfcRegErrorCode = WfcReasonInfo.CODE_WFC_403_NOT_ALLOWED_FROM_THIS_REGION;

                } else if (sipMethod == 0) {
                    /* REG90: Unable to Connect */
                    wfcRegErrorCode = WfcReasonInfo.CODE_WFC_403_FORBIDDEN;
                } else {
                    /* Error403: Mismatch identities */
                    wfcRegErrorCode = WfcReasonInfo.CODE_WFC_403_MISMATCH_IDENTITIES;
                }
                break;
            case 40301:
                wfcRegErrorCode = WfcReasonInfo.CODE_WFC_403_UNKNOWN_USER;
                break;
            case 40302:
                wfcRegErrorCode = WfcReasonInfo.CODE_WFC_403_ROAMING_NOT_ALLOWED;
                break;
            case 40303:
                /* Error403: Mismatch identities */
                wfcRegErrorCode = WfcReasonInfo.CODE_WFC_403_MISMATCH_IDENTITIES;
                break;
            case 40304:
                wfcRegErrorCode = WfcReasonInfo.CODE_WFC_403_AUTH_SCHEME_UNSUPPORTED;
                break;
            case 40305:
                wfcRegErrorCode = WfcReasonInfo.CODE_WFC_403_HANDSET_BLACKLISTED;
                break;
            case 500:
                wfcRegErrorCode = WfcReasonInfo.CODE_WFC_INTERNAL_SERVER_ERROR;
                break;
            case 503:
                if (sipMethod == 0 && sipReasonText.equals(
                    "Emergency Calls over Wi-Fi is not allowed in this location")) {

                    /* REG09: Missing 911 Address */
                    wfcRegErrorCode = WfcReasonInfo.CODE_WFC_503_ECC_OVER_WIFI_NOT_ALLOWED;
                }
                break;
            case 606:
                if (sipMethod == 0 && sipReasonText.equals("Not Acceptable")) {
                    wfcRegErrorCode = WfcReasonInfo.CODE_WFC_606_WIFI_CALLING_IP_NOT_ACCEPTABLE;
                }
                break;
            default:
                break;
        }
        log("mapToWfcRegErrorCause(), sipErrorCode:" + sipErrorCode + " sipMethod:" + sipMethod +
            " sipReasonText: " + sipReasonText + " wfcRegErrorCode:" + wfcRegErrorCode);
        return wfcRegErrorCode;
    }

    private void handleWifiPdnOOS(int simIdx, int oosState) {
        log("handleWifiPdnOOS oosState= " + oosState);
        switch(oosState) {
            case 0:    // disconnect
                mTempDisableWFC[simIdx] = false;

                // set to default rat.
                mRAN[simIdx] = WifiOffloadManager.RAN_TYPE_MOBILE_3GPP;
                mImsExtInfo[simIdx] = mImsExtInfo[simIdx] & ~IMS_VOICE_OVER_WIFI;
                break;
            case 1:    // start
                mTempDisableWFC[simIdx] = true;
                break;
            case 2:    // resume
                mTempDisableWFC[simIdx] = false;
                break;
            default:
                break;
        }
        notifyRegistrationStateChange(simIdx, mImsRegInfo[simIdx], false);
        notifyRegistrationCapabilityChange(simIdx, mImsExtInfo[simIdx], false);
    }

    /**
     * Adapter class for {@link IWifiOffloadListener}.
     */
    private class IWifiOffloadListenerProxy extends WifiOffloadManager.Listener {

        @Override
        public void onHandover(int simIdx, int stage, int ratType) {
            log("onHandover simIdx=" + simIdx + ", stage=" + stage + ", ratType=" + ratType);

            if ((stage == WifiOffloadManager.HANDOVER_END &&
                    mImsRegInfo[simIdx] == ServiceState.STATE_IN_SERVICE)) {
                // only update rat type after a successful handover
                mRAN[simIdx] = ratType;
                notifyRegistrationStateChange(simIdx, mImsRegInfo[simIdx], false);
                notifyRegistrationCapabilityChange(simIdx, mImsExtInfo[simIdx], false);
            }
        }

        @Override
        public void onRequestImsSwitch(int simIdx, boolean isImsOn) {
            if (ImsCommonUtil.supportMdAutoSetupIms()) {
                return;
            }
            int mainCapabilityPhoneId = ImsCommonUtil.getMainCapabilityPhoneId();
            log("onRequestImsSwitch simIdx=" + simIdx +
                    " isImsOn=" + isImsOn + " mainCapabilityPhoneId=" + mainCapabilityPhoneId);

            if (simIdx >= mNumOfPhones) {
                loge("onRequestImsSwitch can't enable/disable ims due to wrong sim id");
            }

            if (ImsCommonUtil.supportMims() == false) {
                if (simIdx != mainCapabilityPhoneId) {
                    logw("onRequestImsSwitch, ignore not MainCapabilityPhoneId request");
                    return;
                }
            }

            if (isImsOn) {
                if (mImsState[simIdx] != MtkPhoneConstants.IMS_STATE_ENABLE
                    || mExpectedImsState[simIdx] == MtkPhoneConstants.IMS_STATE_DISABLED) {
                    mImsRILAdapters[simIdx].turnOnIms(
                            mHandler[simIdx].obtainMessage(EVENT_SET_IMS_ENABLED_DONE));
                    mExpectedImsState[simIdx] = MtkPhoneConstants.IMS_STATE_ENABLE;
                    mImsState[simIdx] = MtkPhoneConstants.IMS_STATE_ENABLING;
                } else {
                    log("Ims already enable and ignore to send AT command.");
                }
            } else {
                if (mImsState[simIdx] != MtkPhoneConstants.IMS_STATE_DISABLED
                    || mExpectedImsState[simIdx] == MtkPhoneConstants.IMS_STATE_ENABLE) {
                    mImsRILAdapters[simIdx].turnOffIms(
                            mHandler[simIdx].obtainMessage(EVENT_SET_IMS_DISABLE_DONE));
                    mExpectedImsState[simIdx] = MtkPhoneConstants.IMS_STATE_DISABLED;
                    mImsState[simIdx] = MtkPhoneConstants.IMS_STATE_DISABLING;
                } else {
                    log("Ims already disabled and ignore to send AT command.");
                }
            }
        }

        @Override
        public void onWifiPdnOOSStateChanged(int simId, int oosState) {
            log("onWifiPdnOOSStateChanged simIdx=" + simId + ", oosState=" + oosState);
            notifyRegistrationOOSStateChanged(simId, oosState);
        }
    }

    public ImsCommandsInterface getImsRILAdapter(int phoneId) {
        if (mImsRILAdapters[phoneId] == null) {
            logw("getImsRILAdapter phoneId=" + phoneId + ", mImsRILAdapter is null ");
        }
        return mImsRILAdapters[phoneId];
    }

    public ImsConfigManager getImsConfigManager() {
        return mImsConfigManager;
    }

    /**
     * Binds the WifiOffload service.
     */
    private void checkAndBindWifiOffloadService() {
        IBinder b = ServiceManager.getService(WifiOffloadManager.WFO_SERVICE);

        if (b != null) {
            try {
                b.linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
            }
            sWifiOffloadService = IWifiOffloadService.Stub.asInterface(b);
        } else {
            loge("can't get WifiOffloadService");
            b = ServiceManager.getService(MwisConstants.MWI_SERVICE);
            try {
                if (b != null) {
                    b.linkToDeath(mDeathRecipient, 0);
                    sWifiOffloadService = IMwiService.Stub.asInterface(b).getWfcHandlerInterface();
                } else {
                    log("No MwiService exist");
                }
            } catch (RemoteException e) {
                Rlog.e(LOG_TAG, "can't get MwiService", e);
            }
        }

        log("checkAndBindWifiOffloadService: sWifiOffloadService = " +
                sWifiOffloadService);
    }

    /**
     * Death recipient class for monitoring WifiOffload service.
     */
    private class IWifiOffloadServiceDeathRecipient implements IBinder.DeathRecipient {
        @Override
        public void binderDied() {
            sWifiOffloadService = null;
        }
    }

    /**
     * Try to bind WifiOffload service and register for handover event.
     */
    void bindAndRegisterWifiOffloadService() {
        if (sWifiOffloadService == null) {
            //first use wifioffloadservice and new the object.
            checkAndBindWifiOffloadService();
            if (sWifiOffloadService != null) {
                try {
                    sWifiOffloadService.registerForHandoverEvent(
                            createWifiOffloadListenerProxy());
                } catch (RemoteException e) {
                    loge("can't register handover event");
                }
            } else {
                if(SystemProperties.getInt("persist.vendor.mtk_wfc_support", 0) == 1){
                    loge("can't get WifiOffloadService");
                }
            }
        }
    }

    /**
     * Try to bind MWI service and register for handover event.
     * MWI service and WifiOffload service use the same service name.
     * This is just a wrapper to reduce confusion.
     */
    private void bindAndRegisterMWIService() {
        bindAndRegisterWifiOffloadService();
    }

    private int getRadioTech(int phoneId) throws RemoteException {
        int radioTech = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;

        log("getRadioTech mRAN = " + mRAN[phoneId]);

        if (ImsCommonUtil.supportMdAutoSetupIms() == false) {
            bindAndRegisterWifiOffloadService();

            if (sWifiOffloadService != null) {
                mRAN[phoneId] = sWifiOffloadService.getRatType(phoneId);
            }
        } else {
            // MWI service does not support getRatType()
            // rat type will be updated via +CIREGU
            bindAndRegisterMWIService();
        }

        switch (mRAN[phoneId]) {
            case WifiOffloadManager.RAN_TYPE_WIFI:
                radioTech = ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN;
                break;
            case WifiOffloadManager.RAN_TYPE_MOBILE_3GPP:
            case WifiOffloadManager.RAN_TYPE_MOBILE_3GPP2:
            default:
                radioTech = ServiceState.RIL_RADIO_TECHNOLOGY_LTE;
                break;
        }
        log("getRadioTech mRAN=" + mRAN[phoneId] + ", radioTech=" + radioTech);
        return radioTech;
    }

    private ImsReasonInfo createImsReasonInfo(int phoneId) {
        ImsReasonInfo imsReasonInfo = null;
        imsReasonInfo = new ImsReasonInfo(ImsReasonInfo.CODE_REGISTRATION_ERROR,
                mRegErrorCode[phoneId], Integer.toString(mRegErrorCode[phoneId]));
        return imsReasonInfo;
    }

    /**
     * Used to Update Ims state change.
     */
    protected void onUpdateImsSate(int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("onUpdateImsSate() error phoneId:" + phoneId);
            return;
        }
        log("request onUpdateImsSate for ImsManager add local registrant");
        if (mImsRegInfo[phoneId] != ServiceState.STATE_POWER_OFF) {
            notifyRegistrationStateChange(phoneId, mImsRegInfo[phoneId], false);
        }

        if ((mImsRegInfo[phoneId] == ServiceState.STATE_IN_SERVICE)) {
            ImsXuiManager xuiManager = ImsXuiManager.getInstance();
            notifyRegistrationCapabilityChange(phoneId, mImsExtInfo[phoneId], false);
            notifyRegistrationAssociatedUriChange(xuiManager, phoneId);
        }
    }

    /**
     * Notifies notifyRegistrationAssociatedUriChange
     *
     * @param phoneId   the specific phone index
     *@hide
     */
    private void notifyRegistrationAssociatedUriChange(ImsXuiManager xuiManager, int phoneId) {
        Uri [] uris = xuiManager.getSelfIdentifyUri(phoneId);
        log("notifyRegistrationAssociatedUriChange" + " phoneId=" + phoneId);
        englog("uris=" + Rlog.pii(LOG_TAG, uris));
        HashSet<IImsRegistrationListener> listeners = mListener.get(phoneId);
        if (listeners != null && uris != null) {
            synchronized (listeners) {
                listeners.forEach(l -> {
                    try {
                        l.registrationAssociatedUriChanged(uris);
                    } catch (RemoteException e) {
                        loge("handle self identify update failed!!");
                    }
                });
            }
        }
        updateAssociatedUriChanged(phoneId, uris);
    }

    private void updateAssociatedUriChanged(int slotId, Uri[] uris) {
        synchronized(mLockUri) {
            MtkImsRegistrationImpl imsReg = sMtkImsRegImpl.get(slotId);
            if (imsReg != null) {
                log("[" + slotId + "] updateAssociatedUriChanged");
                englog("uris=" + uris);
                imsReg.onSubscriberAssociatedUriChanged(uris);
            } else {
                loge("There is not ImsRegistrationImpl for slot " + slotId);
            }
        }
    }

    /**
     * Notifies the application when the device is connected/disconnected to the IMS network.
     *
     * @param phoneId   the specific phone index
     * @param imsRegInfo   the registration inforamtion
     *@hide
     */
    private void notifyRegistrationStateChange(int phoneId, int imsRegInfo,
            boolean staticReg) {
        synchronized(mLockObj) {
            log("notifyRegistrationStateChange imsRegInfo= " + imsRegInfo
                    + ", phoneId=" + phoneId + ", staticReg=" + staticReg + ", mRAN[phoneId]="
                    + mRAN[phoneId]);

            HashSet<IImsRegistrationListener> listeners = mListener.get(phoneId);
            if (imsRegInfo == ServiceState.STATE_IN_SERVICE) {
                try {
                    final int radioTech = getRadioTech(phoneId);

                    if (!staticReg) {
                        updateImsRegstration(phoneId,
                                MtkImsRegistrationImpl.REGISTRATION_STATE_REGISTERED,
                                convertImsRegistrationTech(radioTech), null);
                    }

                    if (listeners != null) {
                        synchronized (listeners) {
                            listeners.forEach(l -> {
                                try {
                                    l.registrationConnectedWithRadioTech(
                                            convertImsRegistrationTech(radioTech));
                                } catch (RemoteException e) {
                                    // error handling. Currently no-op
                                    loge("IMS: l.registrationConnectedWithRadioTech failed");
                                }
                            });
                        }
                    }

                    IImsServiceExt opImsService = getOpImsService();
                    if (opImsService != null) {
                        opImsService.notifyRegistrationStateChange(
                            mRAN[phoneId], mHandler[phoneId], (Object) mImsRILAdapters[phoneId]);
                    }
                    mRegErrorCode[phoneId] = ImsReasonInfo.CODE_UNSPECIFIED;
                } catch (RemoteException e) {
                    loge("IMS: notifyStateChange fail on access WifiOffloadService");
                }
            } else {
                ImsReasonInfo imsReasonInfo = createImsReasonInfo(phoneId);

                updateImsRegstration(phoneId,
                            MtkImsRegistrationImpl.REGISTRATION_STATE_DEREGISTERED,
                            ImsRegistrationImplBase.REGISTRATION_TECH_NONE, imsReasonInfo);

                if (listeners != null) {
                    synchronized (listeners) {
                        listeners.forEach(l -> {
                            try {
                                l.registrationDisconnected(imsReasonInfo);
                            } catch (RemoteException e) {
                                // error handling. Currently no-op
                            }
                        });
                    }
                }
            }
        }
    }

    private void updateCapabilityChange(int phoneId, int imsExtInfo,
            int[] enabledFeatures, int[] disabledFeatures) {
        for (int i = 0; i < IMS_MAX_FEATURE_SUPPORT_SIZE; i++) {
            enabledFeatures[i] = -1;
            disabledFeatures[i] = -1;
        }

        if (mRAN[phoneId] != WifiOffloadManager.RAN_TYPE_WIFI &&
                (imsExtInfo & IMS_VOICE_OVER_LTE) == IMS_VOICE_OVER_LTE) {
            enabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE] =
                    ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE;
            enabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_LTE] =
                    ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_LTE;
        } else {
            disabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE] =
                    ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE;
            disabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_LTE] =
                    ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_LTE;
        }

        if (mRAN[phoneId] != WifiOffloadManager.RAN_TYPE_WIFI &&
                (imsExtInfo & IMS_VIDEO_OVER_LTE) == IMS_VIDEO_OVER_LTE) {
            enabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_LTE] =
                    ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_LTE;
        } else {
            disabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_LTE] =
                    ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_LTE;
        }
        /// WFC @{
        if (ImsCommonUtil.supportMdAutoSetupIms()) {
            if (mRAN[phoneId] == WifiOffloadManager.RAN_TYPE_WIFI &&
                    (imsExtInfo & IMS_VOICE_OVER_LTE) == IMS_VOICE_OVER_LTE &&
                    !mTempDisableWFC[phoneId]) {
                enabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI] =
                        ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI;
                enabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_WIFI] =
                        ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_WIFI;
                log("[WFC]IMS_VOICE_OVER_WIFI");
            } else {
                disabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI] =
                        ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI;
                disabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_WIFI] =
                        ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_WIFI;
            }
        } else {
            if (mRAN[phoneId] == WifiOffloadManager.RAN_TYPE_WIFI &&
                    (imsExtInfo & IMS_VOICE_OVER_LTE) == IMS_VOICE_OVER_LTE) {
                enabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI] =
                        ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI;
                enabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_WIFI] =
                        ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_WIFI;
                log("[WFC]IMS_VOICE_OVER_WIFI");
            } else {
                disabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI] =
                        ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI;
                disabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_WIFI] =
                        ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_WIFI;
            }
        }

        if (mRAN[phoneId] == WifiOffloadManager.RAN_TYPE_WIFI &&
                (imsExtInfo & IMS_VIDEO_OVER_LTE) == IMS_VIDEO_OVER_LTE) {
            enabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_WIFI] =
                    ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_WIFI;
            log("[WFC]IMS_VIDEO_OVER_WIFI");
        } else {
            disabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_WIFI] =
                    ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_WIFI;
        }
        /// @}
    }

    /**
     * Notifies the application when features on a particular service enabled or
     * disabled successfully based on user preferences.
     *
     * @param phoneId   the specific phone index
     * @param imsExtInfo   the ims feature capability inforamtion.
     *@hide
     */
    private void notifyRegistrationCapabilityChange(int phoneId, int imsExtInfo,
            boolean staticReg) {
        log("notifyRegistrationCapabilityChange imsExtInfo= " + imsExtInfo
                + ", phoneId=" + phoneId + ", staticReg=" + staticReg);

        int[] enabledFeatures = new int[IMS_MAX_FEATURE_SUPPORT_SIZE];
        int[] disabledFeatures = new int[IMS_MAX_FEATURE_SUPPORT_SIZE];
        updateCapabilityChange(phoneId, imsExtInfo, enabledFeatures, disabledFeatures);

        // Add  Ut capability
        updateUtCapabilityChange(phoneId, enabledFeatures, disabledFeatures);

        // Use new MtkMmTelFeature to notify register who interesting in capability changed
        MmTelFeature.MmTelCapabilities capabilities = convertCapabilities(enabledFeatures);
        if ((imsExtInfo & IMS_SMS_OVER_LTE) == IMS_SMS_OVER_LTE) {
            capabilities.addCapabilities(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_SMS);
        }
        if (!staticReg) {
            notifyCapabilityChanged(phoneId, capabilities);
        }

        HashSet<IImsRegistrationListener> listeners = mListener.get(phoneId);

        if (listeners != null){
            synchronized (listeners) {
                listeners.forEach(l -> {
                    try {
                        l.registrationFeatureCapabilityChanged(
                            ImsServiceClass.MMTEL, enabledFeatures, disabledFeatures);
                    } catch (RemoteException e) {
                        // error handling. Currently no-op
                    }
                });
            }
        }
    }

    public void notifyUtCapabilityChange(int phoneId) {
        log("notifyUtCapabilityChange, phoneId = " + phoneId);
        mHandler[phoneId].sendMessage(mHandler[phoneId].obtainMessage(EVENT_UT_CAPABILITY_CHANGE,
                phoneId, 0));
    }

    private void updateUtCapabilityChange(int phoneId, int[] enabledFeatures,
            int[] disabledFeatures) {
        if (sMtkSSExt.containsKey(phoneId)) {
            int utCap = sMtkSSExt.get(phoneId).getUtCapabilityFromSettings();
            log("updateUtCapabilityChange, add Ut capability, utCap = " + utCap +
                    ", phoneId = " + phoneId);
            if (utCap == 1) {
                enabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_LTE] =
                        ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_LTE;
            }
        }
    }

    private String eventToString(int eventId) {
        switch (eventId) {
            case EVENT_IMS_REGISTRATION_INFO:
                return "EVENT_IMS_REGISTRATION_INFO";
            case EVENT_RADIO_NOT_AVAILABLE:
                return "EVENT_RADIO_NOT_AVAILABLE";
            case EVENT_SET_IMS_ENABLED_DONE:
                return "EVENT_SET_IMS_ENABLED_DONE";
            case EVENT_SET_IMS_DISABLE_DONE:
                return "EVENT_SET_IMS_DISABLE_DONE";
            case EVENT_IMS_DISABLED_URC:
                return "EVENT_IMS_DISABLED_URC";
            case EVENT_VIRTUAL_SIM_ON:
                return "EVENT_VIRTUAL_SIM_ON";
            case EVENT_INCOMING_CALL_INDICATION:
                return "EVENT_INCOMING_CALL_INDICATION";
            case EVENT_CALL_INFO_INDICATION:
                return "EVENT_CALL_INFO_INDICATION";
            case EVENT_IMS_ENABLING_URC:
                return "EVENT_IMS_ENABLING_URC";
            case EVENT_IMS_ENABLED_URC:
                return "EVENT_IMS_ENABLED_URC";
            case EVENT_IMS_DISABLING_URC:
                return "EVENT_IMS_DISABLING_URC";
            case EVENT_SIP_CODE_INDICATION:
                return "EVENT_SIP_CODE_INDICATION";
            case EVENT_SIP_CODE_INDICATION_DEREG:
                return "EVENT_SIP_CODE_INDICATION_DEREG";
            case EVENT_ON_NETWORK_INIT_USSI:
                return "EVENT_ON_NETWORK_INIT_USSI";
            case EVENT_IMS_DEREG_DONE:
                return "EVENT_IMS_DEREG_DONE";
            case EVENT_IMS_DEREG_URC:
                return "EVENT_IMS_DEREG_URC";
            case EVENT_RADIO_OFF:
                return "EVENT_RADIO_OFF";
            case EVENT_RADIO_ON:
                return "EVENT_RADIO_ON";
            case EVENT_IMS_RTP_INFO_URC:
                return "EVENT_IMS_RTP_INFO_URC";
            case EVENT_SET_IMS_REGISTRATION_REPORT_DONE:
                return "EVENT_SET_IMS_REGISTRATION_REPORT_DONE";
            case EVENT_IMS_VOLTE_SETTING_URC:
                return "EVENT_IMS_VOLTE_SETTING_URC";
            case EVENT_RUN_GBA:
                return "EVENT_RUN_GBA";
            case EVENT_SELF_IDENTIFY_UPDATE:
                return "EVENT_SELF_IDENTIFY_UPDATE";
            case EVENT_IMS_SUPPORT_ECC_URC:
                return "EVENT_IMS_SUPPORT_ECC_URC";
            case EVENT_START_GBA_SERVICE:
                return "EVENT_START_GBA_SERVICE";
            case EVENT_INIT_CALL_SESSION_PROXY:
                return "EVENT_INIT_CALL_SESSION_PROXY";
            case EVENT_SEND_SMS_DONE:
                return "EVENT_SEND_SMS_DONE";
            case EVENT_IMS_SMS_STATUS_REPORT:
                return "EVENT_IMS_SMS_STATUS_REPORT";
            case EVENT_IMS_SMS_NEW_SMS:
                return "EVENT_IMS_SMS_NEW_SMS";
            case EVENT_IMS_SMS_NEW_CDMA_SMS:
                return "EVENT_IMS_SMS_NEW_CDMA_SMS";
            case EVENT_INCOMING_CALL_ADDITIONAL_INFO_INDICATION:
                return "EVENT_INCOMING_CALL_ADDITIONAL_INFO_INDICATION";
            default:
                return "UNKNOWN EVENT: " + eventId;
        }
    }

    /**
     *Ims service Message hanadler.
     *@hide
     */
    private class MyHandler extends Handler {

        int mSocketId;

        /**
         * Constructor associates this handler with the socket Id
         * @param socketId The socket id, must not be less than 0.
         */
        public MyHandler(int socketId) {
            super(null, false);
            mSocketId = socketId;
        }

        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            Intent intent;

            switch (msg.what) {
                case EVENT_IMS_REGISTRATION_INFO:
                case EVENT_SIP_CODE_INDICATION_DEREG:
                    if (hasMessages(EVENT_SIP_CODE_INDICATION_DEREG)) {
                        // Remove deregister enevt sent from ESIPCPI
                        removeMessages(EVENT_SIP_CODE_INDICATION_DEREG);
                    }
                    /**
                     * According to 3GPP TS 27.007 +CIREGU format
                     *
                     * AsyncResult.result is an Object[]
                     * ((Object[])AsyncResult.result)[0] is integer type to indicate the IMS regiration status.
                     *                                    0: not registered
                     *                                    1: registered
                     * ((Object[])AsyncResult.result)[1] is numeric value in hexadecimal format to indicate the IMS capability.
                     *                                    1: RTP-based transfer of voice according to MMTEL (see 3GPP TS 24.173 [87])
                     *                                    2: RTP-based transfer of text according to MMTEL (see 3GPP TS 24.173 [87])
                     *                                    4: SMS using IMS functionality (see 3GPP TS 24.341[101])
                     *                                    8: RTP-based transfer of video according to MMTEL (see 3GPP TS 24.183 [87])
                     *
                     */
                    ar = (AsyncResult) msg.obj;

                    int newImsRegInfo = ServiceState.STATE_POWER_OFF;
                    if (((int[]) ar.result)[0] == 1) {
                        newImsRegInfo = ServiceState.STATE_IN_SERVICE;
                    } else {
                        newImsRegInfo = ServiceState.STATE_OUT_OF_SERVICE;
                    }
                    /// M: Simulate IMS Registration @{
                    if (SystemProperties.getInt("persist.vendor.ims.simulate", 0) == 1) {
                        newImsRegInfo = (mImsRegistry ?
                                ServiceState.STATE_IN_SERVICE : ServiceState.STATE_OUT_OF_SERVICE);
                        log("handleMessage() : Override EVENT_IMS_REGISTRATION_INFO: newImsRegInfo=" +
                                newImsRegInfo);
                    }
                    /// @}
                    int newImsExtInfo = ((int[]) ar.result)[1];
                    // 93MD IMS framework can NOT get ims RAN type, transfer EWFC state
                    // by ext_info.
                    if (ImsCommonUtil.supportMdAutoSetupIms()) {
                        if ((newImsExtInfo & IMS_VOICE_OVER_WIFI) == IMS_VOICE_OVER_WIFI) {
                            mRAN[mSocketId] = WifiOffloadManager.RAN_TYPE_WIFI;
                        } else {
                            // switch to default value
                            mRAN[mSocketId] = WifiOffloadManager.RAN_TYPE_MOBILE_3GPP;
                        }
                    }

                    /* notify upper application the IMS registration status is chagned */
                    log("handleMessage() : newReg:" + newImsRegInfo + " oldReg:" + mImsRegInfo[mSocketId]);

                    mImsRegInfo[mSocketId] = newImsRegInfo;
                    notifyRegistrationStateChange(mSocketId, mImsRegInfo[mSocketId], false);

                    /* notify upper application the IMS capability is chagned when IMS is registered */
                    log("handleMessage() : newRegExt:" + newImsExtInfo + "oldRegExt:" +  mImsExtInfo[mSocketId]);

                    if ((mImsRegInfo[mSocketId] == ServiceState.STATE_IN_SERVICE)) {
                        mImsExtInfo[mSocketId] = newImsExtInfo;
                    } else {
                        mImsExtInfo[mSocketId] = 0;
                    }
                    notifyRegistrationCapabilityChange(mSocketId, mImsExtInfo[mSocketId], false);

                    boolean isVoWiFi = false;
                    if (mRAN[mSocketId] == WifiOffloadManager.RAN_TYPE_WIFI &&
                            (mImsExtInfo[mSocketId] & IMS_VOICE_OVER_LTE) == IMS_VOICE_OVER_LTE) {
                        isVoWiFi = true;
                    }

                    OemPluginFactory oemPlugin = ExtensionFactory.makeOemPluginFactory();
                    ImsRegistrationOemPlugin imsRegOemPlugin =
                            (oemPlugin != null)?
                            oemPlugin.makeImsRegistrationPlugin(
                                    mContext) : null;
                    if (imsRegOemPlugin != null) {
                        imsRegOemPlugin.broadcastImsRegistration(
                                mSocketId, mImsRegInfo[mSocketId], isVoWiFi);
                    }
                    break;
                case EVENT_IMS_ENABLING_URC:
                    //+EIMS: 1
                    log("handleMessage() : [Info]receive EVENT_IMS_ENABLING_URC, socketId = " + mSocketId + " ExpImsState = " + mExpectedImsState[mSocketId] +
                        " mImsState = " + mImsState[mSocketId]);
                    // notify AP Ims Service is up only when state changed to ENABLE
                    if (mImsState[mSocketId] != MtkPhoneConstants.IMS_STATE_ENABLE) {
                        intent = new Intent(ImsManager.ACTION_IMS_SERVICE_UP);
                        intent.putExtra(ImsManager.EXTRA_PHONE_ID, mSocketId);
                        mContext.sendBroadcast(intent);
                        log("handleMessage() : broadcast IMS_SERVICE_UP");
                    }
                    if (ImsCommonUtil.supportMdAutoSetupIms() == false) {
                        // enable ImsAdapter
                        enableImsAdapter(mSocketId);
                    }
                    mImsState[mSocketId] = MtkPhoneConstants.IMS_STATE_ENABLE;
                    //mExpectedImsState[mSocketId] = MtkPhoneConstants.IMS_STATE_ENABLE;
                    break;
                case EVENT_IMS_ENABLED_URC:
                    //+EIMCFLAG: 1
                    break;
                case EVENT_IMS_DISABLING_URC:
                    //+EIMS: 0
                    break;
                case EVENT_IMS_DISABLED_URC:
                    //+EIMCFLAG: 0
                    log("handleMessage() : [Info]EVENT_IMS_DISABLED_URC: socketId = " + mSocketId + " ExpImsState = " + mExpectedImsState[mSocketId] +
                        " mImsState = " + mImsState[mSocketId]);
                        disableIms(mSocketId, true);
                    break;
                case EVENT_SET_IMS_ENABLED_DONE:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        logw("handleMessage() : turnOnIms failed, return to disabled state!");
                        disableIms(mSocketId, false);
                    }
                    break;
                case EVENT_INCOMING_CALL_INDICATION:
                    ar = (AsyncResult) msg.obj;
                    sendIncomingCallIndication(mSocketId, ar);

                    ///M: RTT send EIMSRTT 2 before AT + EAIC
                    mOpExt.setRttModeForIncomingCall(mImsRILAdapters[mSocketId]);

                    break;
                case EVENT_INCOMING_CALL_ADDITIONAL_INFO_INDICATION:
                    ar = (AsyncResult) msg.obj;
                    String[] incomingCallInfo = (String[]) ar.result;
                    int type = Integer.parseInt(incomingCallInfo[0]);
                    if (type == MT_CALL_DIAL_IMS_STK) {
                        handleImsStkCall(mSocketId, incomingCallInfo);
                    }
                    break;
                case EVENT_SIP_CODE_INDICATION:
                    ar = (AsyncResult) msg.obj;
                    String[] sipMessage = (String[]) ar.result;
                    /* ESIPCPI:
                     * <call_id>,<dir>,<SIP_msg_type>,<method>,<response_code>,
                     * <reason_text>,<socket Id>
                    */
                    if (sipMessage != null) {
                        log("handleMessage() : Method =" + sipMessage[3] + " response_code =" + sipMessage[4] +
                            " reason_text =" + sipMessage[5]);

                        int sipMethod = Integer.parseInt(sipMessage[3]);
                        int sipResponseCode = Integer.parseInt(sipMessage[4]);
                        String sipReasonText = sipMessage[5];
                        if (sipMethod == 0 || sipMethod == 9) {
                            IImsServiceExt opImsService = getOpImsService();
                            if (mRAN[mSocketId] == WifiOffloadManager.RAN_TYPE_WIFI
                                    || (opImsService != null
                                    && opImsService.isWfcRegErrorCauseSupported())) {
                                mRegErrorCode[mSocketId] = mapToWfcRegErrorCause(sipResponseCode,
                                        sipMethod, sipReasonText);
                                if (mRegErrorCode[mSocketId] == WfcReasonInfo.CODE_WFC_403_FORBIDDEN
                                        && sipMethod == 0) {
                                    log("handleMessage() : L-ePDG-5025 8-13. Received SIP REG 403 response, perform ImsDiscommect flow.");
                                    int[] result = new int[] {0, IMS_VOICE_OVER_WIFI};
                                    AsyncResult arCip = new AsyncResult(null, result, null);
                                    sendMessageDelayed(obtainMessage(EVENT_SIP_CODE_INDICATION_DEREG, arCip), 1000);
                                }
                            } else {
                                mRegErrorCode[mSocketId] = sipResponseCode;
                            }
                        }
                    }
                    break;
                /// M: Event for network initiated USSI @{
                case EVENT_ON_NETWORK_INIT_USSI:
                    ar = (AsyncResult) msg.obj;
                    // +EIUSD: <m>,<n>,<str>,<lang>
                    String[] eiusd = (String[]) ar.result;

                    log("EVENT_ON_NETWORK_INIT_USSI, m = " + eiusd[0]
                            + ", n = " + eiusd[1]
                            + ", str = " + eiusd[2]);
                    ImsCallProfile imsCallProfile = onCreateCallProfile(1,
                            ImsCallProfile.SERVICE_TYPE_NORMAL, ImsCallProfile.CALL_TYPE_VOICE);
                    imsCallProfile.setCallExtraInt(ImsCallProfile.EXTRA_DIALSTRING,
                            ImsCallProfile.DIALSTRING_USSD);
                    imsCallProfile.setCallExtra("m", eiusd[0]);
                    imsCallProfile.setCallExtra("n", eiusd[1]);
                    imsCallProfile.setCallExtra("str", eiusd[2]);

                    mNwInitUssiSession = new ImsCallSessionProxy(
                            mContext,
                            imsCallProfile,
                            null,
                            ImsService.this,
                            mHandler[mSocketId],
                            mImsRILAdapters[mSocketId],
                            "1",
                            mSocketId,
                            true);

                    Bundle ussiExtras = new Bundle();
                    ussiExtras.putBoolean(ImsManager.EXTRA_USSD, true);
                    ussiExtras.putString(ImsManager.EXTRA_CALL_ID, "1");
                    ussiExtras.putInt(ImsManager.EXTRA_SERVICE_ID, mapPhoneIdToServiceId(mSocketId));

                    notifyIncomingCall(mSocketId, mNwInitUssiSession, ussiExtras);
                    break;
                /// @}
                case EVENT_IMS_DEREG_DONE:
                    // Only log for tracking ims deregister command response
                    break;
                case EVENT_IMS_DEREG_URC:
                    //+EIMSDEREG
                    intent = new Intent(MtkImsConstants.ACTION_IMS_SERVICE_DEREGISTERED);
                    intent.putExtra(ImsManager.EXTRA_PHONE_ID, mSocketId);
                    mContext.sendBroadcast(intent);
                    break;
                case EVENT_SET_IMS_REGISTRATION_REPORT_DONE:
                    // Only log for tracking
                    break;
                case EVENT_RADIO_NOT_AVAILABLE:
                    disableIms(mSocketId, false);
                    break;
                case EVENT_RADIO_OFF:
                    updateRadioState(RadioState.RADIO_OFF.ordinal(), mSocketId);
                    break;
                case EVENT_RADIO_ON:
                    updateRadioState(RadioState.RADIO_ON.ordinal(), mSocketId);
                    break;
                /// M: Sync volte setting value. @{
                case EVENT_IMS_VOLTE_SETTING_URC:
                    ar = (AsyncResult) msg.obj;
                    boolean enable = ((int[]) ar.result)[0] == 1;
                    int simState = SubscriptionManager.getSimStateForSlotIndex(mSocketId);
                    if (simState == TelephonyManager.SIM_STATE_ABSENT
                            || (getSubIdUsingPhoneId(mSocketId)
                                    <= SubscriptionManager.INVALID_SUBSCRIPTION_ID)) {
                        if (mRegisterSubInfoChange == false) {
                            final IntentFilter filter = new IntentFilter();
                            filter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
                            mContext.registerReceiver(mSubInfoReceiver, filter);
                            mRegisterSubInfoChange = true;
                        }
                        mWaitSubInfoChange[mSocketId] = true;
                    } else {
                        mWaitSubInfoChange[mSocketId] = false;
                    }
                    mVolteEnable[mSocketId] = enable;
                    if (mWaitFeatureChange == 0) {
                        SystemProperties.set(PROPERTY_IMSCONFIG_FORCE_NOTIFY, "1");
                        IntentFilter filter = new IntentFilter();
                        filter.addAction(ImsConfig.ACTION_IMS_FEATURE_CHANGED);
                        mContext.registerReceiver(mFeatureValueReceiver, filter);
                    }
                    mWaitFeatureChange = mWaitFeatureChange | (1 << mSocketId);
                    setEnhanced4gLteModeSetting(mSocketId, enable);
                    log("handleMessage() : Volte_Setting_Enable=" + enable +
                        ", register:" + mWaitSubInfoChange[mSocketId] +
                        ", mWaitFeatureChange:" + mWaitFeatureChange);
                    break;
                /// @}
                case EVENT_RUN_GBA:
                    log("handleMessage() : receive EVENT_RUN_GBA: Enter messege");

                    NafSessionKeyResult result;
                    String[] nafInfoTemp;

                    ar = (AsyncResult) msg.obj;
                    nafInfoTemp = (String[]) ar.result;
                    result = (NafSessionKeyResult) ar.userObj;

                    synchronized(result.lockObj) {
                        if (ar.exception != null) {
                            result.cmdResult = IMS_SS_CMD_ERROR;
                            log("handleMessage() : receive EVENT_RUN_GBA: IMS_SS_CMD_ERROR");
                        } else {
                            if (!SENLOG) {
                                log("handleMessage() : receive EVENT_RUN_GBA: hexkey:" + nafInfoTemp[0] +
                                     ", btid:" + nafInfoTemp[2] + ", keylifetime:" + nafInfoTemp[3]);
                            }
                            NafSessionKey nafKey = new NafSessionKey(
                                    nafInfoTemp[2], ImsCommonUtil.hexToBytes(nafInfoTemp[0]), nafInfoTemp[3]);

                            result.nafSessionKey = nafKey;
                            result.cmdResult = IMS_SS_CMD_SUCCESS;
                            log("handleMessage() : receive EVENT_RUN_GBA: IMS_SS_CMD_SUCCESS");
                        }
                        result.lockObj.notify();
                        log("handleMessage() : receive EVENT_RUN_GBA: notify result");
                    }
                    break;
                case EVENT_SELF_IDENTIFY_UPDATE:
                    ImsXuiManager xuiManager = ImsXuiManager.getInstance();
                    if (ImsCommonUtil.supportMdAutoSetupIms()) {
                        ar = (AsyncResult) msg.obj;
                        // +EIMSXUI: <account_id>, <broadcast_flag>, <xui_info>, <socketId>
                        String[] exui = (String[]) ar.result;
                        log("handleMessage() : XUI_INFO=" + ((!SENLOG) ? exui[2] : "[hidden]"));
                        xuiManager.setXui(mSocketId, exui[2]);
                    }
                    notifyRegistrationAssociatedUriChange(xuiManager, mSocketId);
                    break;
                    /// M: handle the IMS ECC support message @{
                case EVENT_IMS_SUPPORT_ECC_URC:
                    ar = (AsyncResult) msg.obj;
                    int eccSupport = ((int[]) ar.result)[0];
                    log("receive EVENT_IMS_SUPPORT_ECC_URC, enable = " + eccSupport
                            + " phoneId = " + mSocketId);
                    // eccSupport may be from +CNEMS1: and +EIMSESS:
                    // from +EIMSESS: will be 0 or 1. (limited service)
                    // from +CNEMS1:  will be 2 or 3. (normal service)
                    // we trasform it to bitwise value to cache the state here.
                    if (eccSupport == 0) {
                        mIsImsEccSupported[mSocketId] &= ~0x01;
                    } else if (eccSupport == 1) {
                        mIsImsEccSupported[mSocketId] |= 0x01;
                    } else if (eccSupport == 2) {
                        mIsImsEccSupported[mSocketId] &= ~0x10;
                    } else if (eccSupport == 3) {
                        mIsImsEccSupported[mSocketId] |= 0x10;
                    }
                    HashSet<IImsRegistrationListener> ecclisteners = mListener.get(mSocketId);
                    int resultEvent = 0;
                    if (ecclisteners != null) {
                        if (mIsImsEccSupported[mSocketId] > 0) {
                            resultEvent = MtkImsConstants
                                    .SERVICE_REG_CAPABILITY_EVENT_ECC_SUPPORT;
                        } else {
                            resultEvent = MtkImsConstants
                                    .SERVICE_REG_CAPABILITY_EVENT_ECC_NOT_SUPPORT;
                        }
                        synchronized (ecclisteners) {
                            try {
                                for (IImsRegistrationListener l : ecclisteners) {
                                        l.registrationServiceCapabilityChanged(
                                                ImsServiceClass.MMTEL, resultEvent);
                                }
                            } catch (RemoteException e) {
                                // error handling. Currently no-op
                            }
                        }
                    }
                    break;
                    /// @}
                case EVENT_START_GBA_SERVICE:
                    startGbaService();
                    break;
                case EVENT_CALL_INFO_INDICATION:
                    /// ALPS03375897. @{
                    ar = (AsyncResult) msg.obj;
                    String[] callInfo = (String[]) ar.result;
                    int msgType = 0;
                    msgType = Integer.parseInt(callInfo[1]);
                    if((msgType == 133) && mPendingMTCallId != null
                            && mPendingMTCallId.equals(callInfo[0])) {
                        mIsPendingMTTerminated = true;
                    }
                    /// @}
                    break;
                case EVENT_INIT_CALL_SESSION_PROXY:

                    log("handleMessage() : Start init call session proxy");

                    Bundle b = msg.getData();
                    String callId = b.getString("callId");
                    int phoneId = b.getInt("phoneId");
                    int seqNum = b.getInt("seqNum");

                    mMtkPendingMT = new MtkImsCallSessionProxy(
                            mContext, mImsCallProfile, null, ImsService.this,
                            mHandler[phoneId], mImsRILAdapters[phoneId], callId, phoneId);

                    ImsCallSessionProxy imsCallSessionProxy = new ImsCallSessionProxy(
                            mContext, mImsCallProfile, null, ImsService.this,
                            mHandler[phoneId], mImsRILAdapters[phoneId], callId, phoneId);

                    mMtkPendingMT.setAospCallSessionProxy(imsCallSessionProxy);
                    imsCallSessionProxy.setMtkCallSessionProxy(mMtkPendingMT);

                    mImsRILAdapters[phoneId].unregisterForCallInfo(mHandler[phoneId]);
                    mImsRILAdapters[phoneId].setCallIndication(
                            IMS_ALLOW_INCOMING_CALL_INDICATION,
                            Integer.parseInt(callId),
                            seqNum,
                            0);

                    if (mIsPendingMTTerminated) {

                        log("handleMessage() : Start deal with pending 133");

                        ((MtkImsCallSessionProxy)mMtkPendingMT).callTerminated();
                        mMtkPendingMT.setServiceImpl(null);
                        mMtkPendingMT.setServiceImpl(null);
                        mMtkPendingMT = null;
                        mIsPendingMTTerminated = false;
                    }
                    break;
                case EVENT_SEND_SMS_DONE: {
                    int phone_id = msg.arg1;
                    int token = msg.arg2;
                    // Default value is 0 in SmsTracker
                    int messageRef = 0;
                    ar = (AsyncResult) msg.obj;

                    if (ar.result != null) {
                        messageRef = ((SmsResponse)ar.result).mMessageRef;
                    } else {
                        log("handleMessage() : SmsResponse was null");
                    }

                    if (ar.exception == null) {
                        log("handleMessage() : SMS send complete, messageRef: " + messageRef);
                        if (mMmTelFeatureCallback.get(phone_id) != null) {
                            mMmTelFeatureCallback.get(phone_id).sendSmsRsp(token, messageRef,
                                    ImsSmsImplBase.SEND_STATUS_OK,
                                    SmsManager.RESULT_ERROR_NONE);
                        }
                    } else {
                        log("handleMessage() : SMS send failed");
                        int status = ImsSmsImplBase.SEND_STATUS_ERROR;
                        int reason = SmsManager.RESULT_ERROR_GENERIC_FAILURE;
                        if ((((CommandException)(ar.exception)).getCommandError()
                                == CommandException.Error.SMS_FAIL_RETRY)) {
                            /*
                             * Generally, modem will do the SMS retry.
                             * If we got retry here, it means to retry in CS domain.
                             */
                            status = ImsSmsImplBase.SEND_STATUS_ERROR_FALLBACK;
                        } else if ((((CommandException)(ar.exception)).getCommandError()
                                == CommandException.Error.FDN_CHECK_FAILURE)) {
                            reason = SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE;
                        }
                        if (mMmTelFeatureCallback.get(phone_id) != null) {
                            mMmTelFeatureCallback.get(phone_id).sendSmsRsp(token,
                                    messageRef, status, reason);
                        }
                    }
                }
                    break;
                case EVENT_IMS_SMS_STATUS_REPORT: {
                    ar = (AsyncResult) msg.obj;
                    if (ar != null) {
                        byte[] pdu = (byte[]) ar.result;
                        log("EVENT_IMS_SMS_STATUS_REPORT, mSocketId = " + mSocketId);
                        if (mMmTelFeatureCallback.get(mSocketId) != null) {
                            mMmTelFeatureCallback.get(mSocketId).newStatusReportInd(pdu,
                                SmsConstants.FORMAT_3GPP);
                        }
                    }
                }
                    break;
                case EVENT_IMS_SMS_NEW_SMS: {
                    ar = (AsyncResult) msg.obj;
                    if (ar != null) {
                        byte[] pdu = (byte[]) ar.result;
                        log("EVENT_IMS_SMS_NEW_SMS, mSocketId = " + mSocketId);
                        if (mMmTelFeatureCallback.get(mSocketId) != null) {
                            mMmTelFeatureCallback.get(mSocketId).newImsSmsInd(pdu,
                                SmsConstants.FORMAT_3GPP);
                        }
                    }
                }
                    break;

                case EVENT_IMS_SMS_NEW_CDMA_SMS: {
                    if (!handleNewCdmaSms((AsyncResult) msg.obj, mSocketId)) {
                        acknowledgeLastIncomingCdmaSms(mSocketId, false,
                                Intents.RESULT_SMS_GENERIC_ERROR);
                    }
                    break;
                }

                case EVENT_UT_CAPABILITY_CHANGE: {
                    log("receive EVENT_UT_CAPABILITY_CHANGE, phoneId = " + msg.arg1);
                    notifyRegistrationCapabilityChange(msg.arg1, mImsExtInfo[msg.arg1], false);
                }
                default:
                    break;
            }

            IImsServiceExt opImsService = getOpImsService();
            if (opImsService != null) {
                opImsService.notifyImsServiceEvent(mSocketId, mContext, msg);
            }
        }
    }

    private void handleImsStkCall(int phoneId, String[] incomingCallInfo) {
        /* +ECPI: <call_id>, <msg_type>, <is_ibt>,
            <is_tch>, <dir>, <call_mode>, <number>, <type>, "<pau>", [<cause>] */
        String callId = incomingCallInfo[1];
        String callNum = incomingCallInfo[7];

        ImsCallProfile imsCallProfile = new ImsCallProfile();
        if ((callNum != null) && (!callNum.equals(""))) {
            loge("setCallIndication new call profile: " + callNum);
            imsCallProfile.setCallExtra(ImsCallProfile.EXTRA_OI, callNum);
            imsCallProfile.setCallExtraInt(ImsCallProfile.EXTRA_OIR,
                    ImsCallProfile.OIR_PRESENTATION_NOT_RESTRICTED);
        }
        mMtkPendingMT = new MtkImsCallSessionProxy(
                mContext, imsCallProfile, null, ImsService.this,
                mHandler[phoneId], mImsRILAdapters[phoneId], callId, phoneId);

        ImsCallSessionProxy imsCallSessionProxy = new ImsCallSessionProxy(
                mContext, imsCallProfile, null, ImsService.this,
                mHandler[phoneId], mImsRILAdapters[phoneId], callId, phoneId);

        mMtkPendingMT.setAospCallSessionProxy(imsCallSessionProxy);
        imsCallSessionProxy.setMtkCallSessionProxy(mMtkPendingMT);

        mPendingMtkImsCallSessionProxy.put(
                imsCallSessionProxy.getServiceImpl(),
                mMtkPendingMT);

        // Need to put this call into call list of ImsServiceCallTracker.
        String[] callInfo = Arrays.copyOfRange(incomingCallInfo, 1, incomingCallInfo.length);
        ImsServiceCallTracker imsCallTracker = ImsServiceCallTracker.getInstance(phoneId);
        imsCallTracker.processCallInfoIndication(callInfo, imsCallSessionProxy,
                imsCallSessionProxy.getCallProfile());

        Bundle extras = new Bundle();
        extras.putString(ImsManager.EXTRA_CALL_ID, callId);
        extras.putString(MtkImsConstants.EXTRA_DIAL_STRING, incomingCallInfo[6]);
        extras.putInt(ImsManager.EXTRA_SERVICE_ID, phoneId);
        extras.putBoolean(ImsManager.EXTRA_IS_UNKNOWN_CALL, true);
        notifyIncomingCallSession(phoneId, imsCallSessionProxy.getServiceImpl(), extras);
    }

    /**
     * Notify AP IMS Service is disabled and disable ImsAdapter.
     *@param isNormalDisable is IMS service disabled normally or abnormally
     *@hide
     */
    private void disableIms(int phoneId, boolean isNormalDisable) {
        if (ImsCommonUtil.supportMdAutoSetupIms() == false) {
            disableImsAdapter(phoneId,isNormalDisable);
        }
        mImsState[phoneId] = MtkPhoneConstants.IMS_STATE_DISABLED;
        //mExpectedImsState[phoneId] = MtkPhoneConstants.IMS_STATE_DISABLED;
    }

    /**
     * initialize ims avalability, turn on/off ims for specified phone id
     * @param phoneId
     * @param capabilityOffset
     */
    private void initImsAvailability(int phoneId, int capabilityOffset,
            int enableMessageId, int disableMessageId) {
        int volteCapability = SystemProperties.getInt("persist.vendor.mtk.volte.enable", 0);
        int wfcCapability = SystemProperties.getInt("persist.vendor.mtk.wfc.enable", 0);
        if ((volteCapability & (1 << capabilityOffset)) > 0
                || (wfcCapability & (1 << capabilityOffset)) > 0) {
            log("initImsAvailability turnOnIms : " + phoneId);
            mImsRILAdapters[phoneId].turnOnIms(mHandler[phoneId].obtainMessage(enableMessageId));
            mImsState[phoneId] = MtkPhoneConstants.IMS_STATE_ENABLING;
        } else {
            log("initImsAvailability turnOffIms : " + phoneId);
            mImsRILAdapters[phoneId].turnOffIms(mHandler[phoneId].obtainMessage(disableMessageId));
            mImsState[phoneId] = MtkPhoneConstants.IMS_STATE_DISABLING;
        }

        // IMS Ctrl flow error handling
        // report RADIO_UNAVAILABLE here in case of ImsService died
        updateRadioState(RadioState.RADIO_UNAVAILABLE.ordinal(), phoneId);
    }

    public int getRatType(int phoneId) {
        return mRAN[phoneId];
    }

    private void log(String s) {
        if (DBG) {
            Rlog.d(LOG_TAG, s);
        }
    }

    private void englog(String s) {
        if (ENGLOAD) {
            log(s);
        }
    }

    private void logw(String s) {
        Rlog.w(LOG_TAG, s);
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, s);
    }

    private void logi(String s) {
        Rlog.i(LOG_TAG, s);
    }

    /**
     *call interface for allowing/refusing the incoming call indication send to App.
     *@hide
     */
    protected void onSetCallIndication(int phoneId, String callId, String callNum, int seqNum,
                                       String toNumber, boolean isAllow, int cause) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("onSetCallIndication() error phoneId:" + phoneId);
            return;
        }

        /* leave blank */
        if (isAllow) {
            mImsCallProfile = new ImsCallProfile();
            if ((callNum != null) && (!callNum.equals(""))) {
                log("setCallIndication new call profile: " + callNum);
                mImsCallProfile.setCallExtra(ImsCallProfile.EXTRA_OI, callNum);
                mImsCallProfile.setCallExtraInt(ImsCallProfile.EXTRA_OIR,
                        ImsCallProfile.OIR_PRESENTATION_NOT_RESTRICTED);
            }

            // put MT to number in call profile
            DigitsUtil digitsUtil =
                OpImsServiceCustomizationUtils.getOpFactory(mContext).makeDigitsUtil();
            digitsUtil.putMtToNumber(toNumber, mImsCallProfile);

            /// ALPS03375897. @{
            // For MT, call is terminated before receiving ECPI 0, it will cause
            // the pending MT dosen't close. To avoid timing issue, we put the
            // creation process in the main thread.
            Message msg = mHandler[phoneId].obtainMessage(EVENT_INIT_CALL_SESSION_PROXY);
            Bundle b = new Bundle();
            b.putString("callId", callId);
            b.putInt("phoneId", phoneId);
            b.putInt("seqNum", seqNum);
            msg.setData(b);
            mHandler[phoneId].sendMessage(msg);
            /// @}

        } else {
            mImsRILAdapters[phoneId].setCallIndication(
                    IMS_DISALLOW_INCOMING_CALL_INDICATION,
                    Integer.parseInt(callId),
                    seqNum,
                    cause);
        }
    }

    IMtkImsCallSession onGetPendingMtkCallSession(String callId) {
        // This API is for incoming call to create IImsCallSession
        if (mMtkPendingMT == null) {
            return null;
        }

        IMtkImsCallSession pendingMTsession = mMtkPendingMT.getServiceImpl();

        try {
            if (pendingMTsession.getCallId().equals(callId)) {
                mMtkPendingMT = null;
                return pendingMTsession;
            }
        } catch (RemoteException e) {
            // error handling. Currently no-op
        }

        return null;
    }

    private void sendIncomingCallIndication(int phoneId, AsyncResult ar) {
        // +EAIC:<call_id>,<number>,<type>,<call_mode>,<seq_no>[,<redirect_num>[,<digit_line_num>]]
        mIsPendingMTTerminated = false;

        mImsRILAdapters[phoneId].registerForCallInfo(
                mHandler[phoneId], EVENT_CALL_INFO_INDICATION, null);

        String callId = ((String[]) ar.result)[0];
        mPendingMTCallId = callId;

        String dialString = ((String[]) ar.result)[1];
        String callMode = ((String[]) ar.result)[3];
        String seqNum = ((String[]) ar.result)[4];
        String toLineNum = ((String[]) ar.result)[6];

        log("sendIncomingCallIndication() : call_id = " + callId +
                " dialString = " + Rlog.pii(LOG_TAG, dialString) + " seqNum = " + seqNum +
                " phoneId = " + phoneId);

        ImsServiceCallTracker imsCallTracker = ImsServiceCallTracker.getInstance(phoneId);

        boolean isAllow = true; /// default value is always allowed to take call

        // ALPS02037830. Needs to use Phone-Id to query call waiting setting.
        String callWaitingSetting = TelephonyManager.getTelephonyProperty(
                phoneId, PROPERTY_TBCW_MODE, TBCW_DISABLED);

        /// M: for ALPS03153860 Need check call state. @{
        if (callWaitingSetting.equals(TBCW_OFF) == true && imsCallTracker.isInCall()) {

            log("sendIncomingCallIndication() : PROPERTY_TBCW_MODE = TBCW_OFF. Reject the call as UDUB ");

            isAllow = false;
        }

        // Now we handle ECC/MT conflict issue.
        if (imsCallTracker.isEccExist()) {
            log("sendIncomingCallIndication() : there is an ECC call, dis-allow this incoming call!");
            isAllow = false;
        }

        // need to check if CMCC/CU/CT case
        if (OperatorUtils.isMatched(OperatorUtils.OPID.OP01, phoneId)
                || OperatorUtils.isMatched(OperatorUtils.OPID.OP02, phoneId)
                || OperatorUtils.isMatched(OperatorUtils.OPID.OP09, phoneId)) {

            log("sendIncomingCallIndication() : OP01 or OP09 case");

            // there is video call
            if (imsCallTracker.isVideoCallExist()) {
                log("sendIncomingCallIndication() : there is video calls, dis-allow this incoming call!");
                isAllow = false;

            // or MT call is video call
            } else if (imsCallTracker.isVideoCall(Integer.parseInt(callMode)) && imsCallTracker.isInCall())  {
                log("sendIncomingCallIndication() : MT is video calls during call, dis-allow this incoming call!");
                isAllow = false;
            }
        }

        // need to check if KDDI case
        if (OperatorUtils.isMatched(OperatorUtils.OPID.OP129, phoneId)) {

            log("sendIncomingCallIndication() : OP129 case");

            // there is conference call
            if (imsCallTracker.isConferenceHostCallExist()) {
                log("sendIncomingCallIndication() : there is conference call, dis-allow this incoming call!");
                isAllow = false;
            }
        }

        // from P, we use imsServiceCallTracker to make decision in ims service
        // but we still need to notify imsCalltracker if FWK allow this call and upper layer may check in addition
        log("sendIncomingCallIndication() : isAllow = " + isAllow);
        if (!isAllow) {
            onSetCallIndication(phoneId, callId, dialString, Integer.parseInt(seqNum), toLineNum, isAllow, -1);

        } else {

            CarrierConfigManager configManager = (CarrierConfigManager) mContext.getSystemService(
                    Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle carrierConfig = configManager.getConfigForSubId(getSubIdUsingPhoneId(phoneId));
            if (carrierConfig == null) {
                carrierConfig = configManager.getDefaultConfig();
            }
            boolean needCheckEnhanceCallBlacking = carrierConfig.getBoolean(
                MtkImsConstants.MTK_KEY_SUPPORT_ENHANCED_CALL_BLOCKING_BOOL);

            log("sendIncomingCallIndication() : needCheckEnhanceCallBlacking = " + needCheckEnhanceCallBlacking);

            if (needCheckEnhanceCallBlacking) {
                Intent intent = new Intent(MtkImsConstants.ACTION_IMS_INCOMING_CALL_INDICATION);
                intent.setPackage("com.android.phone");
                intent.putExtra(ImsManager.EXTRA_CALL_ID, callId);
                intent.putExtra(MtkImsConstants.EXTRA_DIAL_STRING, dialString);
                intent.putExtra(MtkImsConstants.EXTRA_CALL_MODE, Integer.parseInt(callMode));
                intent.putExtra(MtkImsConstants.EXTRA_SEQ_NUM, Integer.parseInt(seqNum));
                intent.putExtra(MtkImsConstants.EXTRA_PHONE_ID, phoneId);
                intent.putExtra(MtkImsConstants.EXTRA_MT_TO_NUMBER, toLineNum);
                mContext.sendBroadcast(intent);
            } else {
                onSetCallIndication(phoneId, callId, dialString, Integer.parseInt(seqNum), toLineNum, isAllow, -1);
            }
        }
    }

    int getCurrentCallCount(int phoneId) {
        if (phoneId < 0 || phoneId > mNumOfPhones - 1) {
            log("IMS: getCurrentCallCount() phoneId: " + phoneId);
            return 0;
        }

        return ImsServiceCallTracker.getInstance(phoneId).getCurrentCallCount();
    }

    public boolean isImsEccSupported(int phoneId) {
        return mIsImsEccSupported[phoneId] > 0;
    }

    public boolean isImsEccSupportedWhenNormalService(int phoneId) {
        return (mIsImsEccSupported[phoneId] & 0x10) > 0;
    }

    private void resetXuiAndNotify(int phoneId) {
        log("resetXuiAndNotify() phoneId: " + phoneId);
        ImsXuiManager.getInstance().setXui(phoneId, null);
        Uri [] uris = { Uri.parse("") };
        HashSet<IImsRegistrationListener> listeners = mListener.get(phoneId);
        if (listeners != null) {
            synchronized (listeners) {
                listeners.forEach(l -> {
                    try {
                        l.registrationAssociatedUriChanged(uris);
                    } catch (RemoteException e) {
                        loge("clear self identify failed!!");
                    }
                });
            }
        }
    }
    public int[] getImsNetworkState(int capability) {
        return mImsDataTracker.getImsNetworkState(capability);
    }

    // SMS-START
    protected void onAddImsSmsListener(int phoneId, IImsSmsListener listener) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("onAddImsSmsListener() error phoneId:" + phoneId);
            return;
        }
        log("onAddImsSmsListener: phoneId=" + phoneId + " listener=" + listener);
        HashSet<IImsSmsListener> listeners = mImsSmsListener.get(phoneId);
        synchronized (listeners) {
            if (!listeners.isEmpty()) {
                // There is only a IImsSmsListener for a phoneId at the same time
                listeners.clear();
            }
            listeners.add(listener);
            log("IMS SMS listener set size=" + listeners.size());
        }
    }

    /**
     * Send Sms
     *@hide
     */
    public void sendSms(int phoneId, int token, int messageRef, String format, String smsc,
            boolean isRetry, byte[] pdu) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("sendSms() error phoneId:" + phoneId);
            return;
        }
        log("sendSms, token " + token + ", messageRef " + messageRef);
        Message response = mHandler[phoneId].obtainMessage(EVENT_SEND_SMS_DONE, phoneId, token);
        mImsRILAdapters[phoneId].sendSms(token, messageRef, format, smsc, isRetry, pdu, response);
    }

    /**
     * Ack for new sms or sms status report
     *@hide
     */
    public void acknowledgeLastIncomingGsmSms(int phoneId, boolean success,  int cause) {
        log("acknowledgeLastIncomingGsmSms, success " + success + ", cause " + cause);
        mImsRILAdapters[phoneId].acknowledgeLastIncomingGsmSms(success, cause, null);
    }

    public void acknowledgeLastIncomingCdmaSms(int phoneId, boolean success, int cause) {
        log("acknowledgeLastIncomingCdmaSms, success " + success + ", cause " + cause);
        mImsRILAdapters[phoneId].acknowledgeLastIncomingCdmaSmsEx(success, cause, null);
    }

    private boolean handleNewCdmaSms(AsyncResult ar, int socketId) {
        if (ar.exception != null) {
            loge("Exception processing incoming SMS: " + ar.exception);
            return false;
        }
        SmsMessage sms = (SmsMessage) ar.result;
        if (sms == null) {
            loge("SmsMessage is null");
            return false;
        }
        SmsMessageBase smsb = sms.mWrappedSmsMessage;
        if (smsb == null) {
            loge("SmsMessageBase is null");
            return false;
        }
        if (mMmTelFeatureCallback == null) {
            loge("mMmTelFeatureCallback is null");
            return false;
        }
        boolean statusReport = false;
        com.android.internal.telephony.cdma.SmsMessage cdmaSms =
            (com.android.internal.telephony.cdma.SmsMessage) smsb;
        if (SmsEnvelope.MESSAGE_TYPE_POINT_TO_POINT == cdmaSms.getMessageType()) {
            try {

                cdmaSms.parseSms();
                if (cdmaSms.isStatusReportMessage()) {
                    statusReport = true;
                }
            } catch (RuntimeException ex) {
                loge("Exception dispatching message: " + ex);
                return false;
            }
        }
        if (statusReport) {
            log("EVENT_IMS_SMS_STATUS_REPORT, socketId = " + socketId);
            mMmTelFeatureCallback.get(socketId).newStatusReportInd(
                    smsb.getPdu(),
                    SmsConstants.FORMAT_3GPP2);
        } else {
            log("EVENT_IMS_SMS_NEW_SMS, socketId = " + socketId);
            mMmTelFeatureCallback.get(socketId).newImsSmsInd(
                    smsb.getPdu(),
                    SmsConstants.FORMAT_3GPP2);
        }
        return true;
    }
    // SMS-END

    public void explicitCallTransfer(int phoneId, Message result, Messenger target) {

        log("explicitCallTransfer: phoneId " + phoneId);

        ImsServiceCallTracker imsCallTracker = ImsServiceCallTracker.getInstance(phoneId);
        ImsCallSessionProxy fgCallSession = imsCallTracker.getFgCall();

        if (fgCallSession != null) {
            fgCallSession.explicitCallTransferWithCallback(result, target);

        } else {
            result.arg1 = 0;

            try {
                target.send(result);
            } catch (RemoteException e) {
                log(e.toString());
            }
        }
    }

    private void notifyRegistrationOOSStateChanged(int simId, int oosState) {
        handleWifiPdnOOS(simId, oosState);

        HashSet<IImsRegistrationListener> listeners = mListener.get(simId);
        if (listeners == null) {
            log("notifyRegistrationOOSStateChanged: listeners is null");
            return;
        }
        synchronized (listeners){
            int resultEvent = MtkImsConstants.SERVICE_REG_EVENT_WIFI_PDN_OOS_START;
            switch (oosState) {
                case 0:
                    resultEvent = MtkImsConstants.
                        SERVICE_REG_EVENT_WIFI_PDN_OOS_END_WITH_DISCONN;
                    break;
                case 1:
                    resultEvent = MtkImsConstants.
                        SERVICE_REG_EVENT_WIFI_PDN_OOS_START;
                    break;
                case 2:
                    resultEvent = MtkImsConstants.
                        SERVICE_REG_EVENT_WIFI_PDN_OOS_END_WITH_RESUME;
                    break;
            }
            try {
                log("notifyRegistrationOOSStateChanged listener size: " + listeners.size());
                for (IImsRegistrationListener l : listeners) {
                    log("call registrationServiceCapabilityChanged with event: " + resultEvent);
                    l.registrationServiceCapabilityChanged(
                            ImsServiceClass.MMTEL, resultEvent);
                }
            } catch (RemoteException e) {
                log(e.toString());
            }
        }
    }

    public interface IMtkMmTelFeatureCallback {
        void notifyContextChanged(Context context);

        void sendSmsRsp(int token, int messageRef,
            @ImsSmsImplBase.SendStatusResult int status,
            int reason);
        void newStatusReportInd(byte[] pdu, String format);
        void newImsSmsInd(byte[] pdu, String format);

        void notifyCapabilitiesChanged(MmTelFeature.MmTelCapabilities c);

        void notifyIncomingCall(ImsCallSessionImplBase c, Bundle extras);
        void notifyIncomingCallSession(IImsCallSession c, Bundle extras);
    }

    private final SparseArray<IMtkMmTelFeatureCallback> mMmTelFeatureCallback =
            new SparseArray<>();

    public void setMmTelFeatureCallback(int phoneId, IMtkMmTelFeatureCallback c) {
        mMmTelFeatureCallback.delete(phoneId);
        if (c != null && SubscriptionManager.isValidPhoneId(phoneId)) {
            mMmTelFeatureCallback.put(phoneId, c);
            c.notifyContextChanged(mContext);
        }
    }

    private MmTelFeature.MmTelCapabilities convertCapabilities(int[] enabledFeatures) {
        boolean[] featuresEnabled = new boolean[enabledFeatures.length];
        for (int i = ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE;
                i <= ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_WIFI
                && i < enabledFeatures.length; i++) {
            if (enabledFeatures[i] == i) {
                featuresEnabled[i] = true;
            } else if (enabledFeatures[i] == ImsConfig.FeatureConstants.FEATURE_TYPE_UNKNOWN) {
                // FEATURE_TYPE_UNKNOWN indicates that a feature is disabled.
                featuresEnabled[i] = false;
            }
        }
        MmTelFeature.MmTelCapabilities capabilities = new MmTelFeature.MmTelCapabilities();
        if (featuresEnabled[ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE]
                || featuresEnabled[ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI]) {
            // voice is enabled
            capabilities.addCapabilities(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE);
        }
        if (featuresEnabled[ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_LTE]
                || featuresEnabled[ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_WIFI]) {
            // video is enabled
            capabilities.addCapabilities(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO);
        }
        if (featuresEnabled[ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_LTE]
                || featuresEnabled[ImsConfig.FeatureConstants.FEATURE_TYPE_UT_OVER_WIFI]) {
            // ut is enabled
            capabilities.addCapabilities(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_UT);
        }
        log("convertCapabilities - capabilities: " + capabilities);
        return capabilities;
    }

    private void notifyCapabilityChanged(int phoneId, MmTelFeature.MmTelCapabilities c) {
        if (mMmTelFeatureCallback.get(phoneId) != null) {
            mMmTelFeatureCallback.get(phoneId).notifyCapabilitiesChanged(c);
        }
    }

    private void notifyIncomingCall(int phoneId, ImsCallSessionImplBase c, Bundle extras) {
        if (mMmTelFeatureCallback.get(phoneId) != null) {
            mMmTelFeatureCallback.get(phoneId).notifyIncomingCall(c, extras);
        }
    }

    public void notifyIncomingCallSession(int phoneId, IImsCallSession c, Bundle extras) {
        if (mMmTelFeatureCallback.get(phoneId) != null) {
            try {
                mMmTelFeatureCallback.get(phoneId).notifyIncomingCallSession(c, extras);
            } catch (RuntimeException e) {
                loge("Fail to notifyIncomingCallSession " + e);
            }
        }
    }

    public void setImsRegistration(int slotId, MtkImsRegistrationImpl imsRegImpl) {
        sMtkImsRegImpl.remove(slotId);
        if (imsRegImpl != null) {
            sMtkImsRegImpl.put(slotId, imsRegImpl);
            if (mImsRegInfo[slotId] != ServiceState.STATE_POWER_OFF) {
                if (mImsRegInfo[slotId] == ServiceState.STATE_IN_SERVICE) {
                    try {
                        final int radioTech = getRadioTech(slotId);
                        updateImsRegstration(slotId,
                            MtkImsRegistrationImpl.REGISTRATION_STATE_REGISTERED,
                            convertImsRegistrationTech(radioTech), null);
                    } catch (RemoteException e) {
                        loge("Fail to get radio tech " + e);
                    }
                } else if (mImsRegInfo[slotId] == ServiceState.STATE_OUT_OF_SERVICE) {
                    ImsReasonInfo imsReasonInfo = createImsReasonInfo(slotId);
                    updateImsRegstration(slotId,
                            MtkImsRegistrationImpl.REGISTRATION_STATE_DEREGISTERED,
                            ImsRegistrationImplBase.REGISTRATION_TECH_NONE, imsReasonInfo);
                }
            }
        }
    }

    private void updateImsRegstration(int slotId,
            @MtkImsRegistrationImpl.ImsRegistrationState int state,
            @ImsRegistrationImplBase.ImsRegistrationTech int imsRadioTech,
            ImsReasonInfo reason) {
        MtkImsRegistrationImpl imsReg = sMtkImsRegImpl.get(slotId);
        if (imsReg != null) {
            try {
                logi("[" + slotId + "] state " + state + " updateImsRegstration, tech "
                        + imsRadioTech + ", reason " + reason);
                switch (state) {
                    case MtkImsRegistrationImpl.REGISTRATION_STATE_REGISTERING:
                        imsReg.onRegistering(imsRadioTech);
                        break;
                    case MtkImsRegistrationImpl.REGISTRATION_STATE_REGISTERED:
                        imsReg.onRegistered(imsRadioTech);
                        break;
                    case MtkImsRegistrationImpl.REGISTRATION_STATE_DEREGISTERED:
                        imsReg.onDeregistered(reason);
                }
            } catch (IllegalStateException e) {
                loge("Failed to updateImsRegstration " + e);
            }
        } else {
            loge("There is not ImsRegistrationImpl for slot " + slotId);
        }
    }

    private @ImsRegistrationImplBase.ImsRegistrationTech int convertImsRegistrationTech(
            int tech) {
        switch (tech) {
            case ServiceState.RIL_RADIO_TECHNOLOGY_LTE:
                return ImsRegistrationImplBase.REGISTRATION_TECH_LTE;
            case ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN:
                return ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN;
            default:
                return ImsRegistrationImplBase.REGISTRATION_TECH_NONE;
        }
    }

    /// M: Sync CT volte setting value. @{
    public int getSubIdUsingPhoneId(int phoneId) {
        int[] subIds = SubscriptionManager.getSubId(phoneId);
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (subIds != null && subIds.length >= 1) {
            subId = subIds[0];
        }
        log("[getSubIdUsingPhoneId] volte_setting subId: " + subId);
        return subId;
    }

    private final BroadcastReceiver mSubInfoReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            log("volte_setting mSubInfoReceiver action: " + intent.getAction());
            if (TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED.equals(intent.getAction())) {
                boolean needDereg = true;
                for (int phoneId = 0; phoneId < mNumOfPhones; phoneId++) {
                    if (mWaitSubInfoChange[phoneId]
                            && (getSubIdUsingPhoneId(phoneId) >
                                    SubscriptionManager.INVALID_SUBSCRIPTION_ID)) {
                        setEnhanced4gLteModeSetting(phoneId, mVolteEnable[phoneId]);
                        mWaitSubInfoChange[phoneId] = false;
                    }
                    if (mWaitSubInfoChange[phoneId]) {
                        needDereg = false;
                    }
                }
                if (needDereg) {
                    mContext.unregisterReceiver(mSubInfoReceiver);
                    mRegisterSubInfoChange = false;
                }
            }
            log("volte_setting mSubInfoReceiver finished");
        }
    };

    private BroadcastReceiver mFeatureValueReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int feature = intent.getIntExtra(ImsConfig.EXTRA_CHANGED_ITEM, -1);
            int phoneId = intent.getIntExtra(CONFIG_EXTRA_PHONE_ID, -1);
            log("volte_setting mFeatureValueReceiver action: " + intent.getAction()
                + ", phoneId: " + phoneId + ", feature: " + feature
                + ", mWaitFeatureChange" + mWaitFeatureChange);
            if (feature == ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE) {
                if ((mWaitFeatureChange & 1 << phoneId) != 0) {
                    mWaitFeatureChange = mWaitFeatureChange & (~(1 << phoneId));
                }
                if (mWaitFeatureChange == 0) {
                    mContext.unregisterReceiver(mFeatureValueReceiver);
                    SystemProperties.set(PROPERTY_IMSCONFIG_FORCE_NOTIFY, "0");
                }
                log("volte_setting mFeatureValueReceiver finished mWaitFeatureChange:"
                    + mWaitFeatureChange);
            }
        }
    };

    private void setEnhanced4gLteModeSetting(int phoneId, boolean enabled) {
        ImsManager imsMgr = ImsManager.getInstance(mContext, phoneId);

        if (imsMgr != null) {
            imsMgr.setEnhanced4gLteModeSetting(enabled);
            if (!imsMgr.isServiceReady()) {
                try {
                    mImsConfigManager.get(phoneId).setFeatureValue(
                        ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE,
                        TelephonyManager.NETWORK_TYPE_LTE,
                        enabled ? ImsConfig.FeatureValueConstants.ON
                                : ImsConfig.FeatureValueConstants.OFF,
                        null);
                    log("volte_setting setEnhanced4gLteModeSetting with service not ready yet.");
                } catch (RemoteException e) {
                    log("volte_setting setEnhanced4gLteModeSetting with exception.");
                }
            }
        } else {
            loge("[" + phoneId + "] Fail to setEnhanced4gLteModeSetting because imsMgr is null");
        }
    }
    /// @}

    public boolean isSupportCFT(int phoneId) {
        synchronized (sMtkSSExt) {
            boolean isSupport = false;
            if (sMtkSSExt.containsKey(phoneId)) {
                isSupport = sMtkSSExt.get(phoneId).isSupportCFT();
            }
            log("isSupportCFT: " + isSupport);
            return isSupport;
        }
    }
}
