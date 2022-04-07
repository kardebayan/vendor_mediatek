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

package com.mediatek.ims;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;

import android.telecom.VideoProfile;

import android.telephony.CarrierConfigManager;
import android.telephony.ims.stub.ImsCallSessionImplBase;
import android.telephony.ims.aidl.IImsCallSessionListener;
import android.telephony.ims.ImsCallSessionListener;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsCallSession;
import android.telephony.ims.ImsConferenceState;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsStreamMediaProfile;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;

import android.text.TextUtils;

import com.android.ims.ImsCall;
import com.android.ims.ImsManager;
import com.android.ims.internal.IImsVideoCallProvider;
import com.android.ims.internal.IImsCallSession;

import com.mediatek.ims.internal.IMtkImsCallSession;
import com.mediatek.ims.internal.IMtkImsCallSessionListener;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.LastCallFailCause;
import com.mediatek.ims.ext.OpImsServiceCustomizationUtils;
import com.mediatek.ims.ril.ImsCommandsInterface;
import com.mediatek.ims.ImsCallInfo;

// For ViLTE feature.
import com.mediatek.ims.ImsServiceCallTracker;
import com.mediatek.ims.internal.ImsVTProvider;
import com.mediatek.ims.internal.ImsVTProviderUtil;
import com.mediatek.ims.MtkImsConstants;

import com.mediatek.internal.telephony.MtkCallFailCause;

// for External component
import com.mediatek.ims.plugin.ExtensionFactory;
import com.mediatek.ims.plugin.ExtensionPluginFactory;
import com.mediatek.ims.plugin.impl.ImsSelfActivatorBase;
import com.mediatek.ims.plugin.impl.ImsCallPluginBase;
import com.mediatek.ims.plugin.ImsSelfActivator;
import com.mediatek.ims.plugin.ImsCallPlugin;

import com.mediatek.ims.common.ImsCarrierConfigConstants;
import com.mediatek.ims.config.internal.ImsConfigUtils;

import com.mediatek.wfo.DisconnectCause;
import com.mediatek.wfo.IWifiOffloadService;
import com.mediatek.wfo.WifiOffloadManager;
import com.mediatek.wfo.IMwiService;
import com.mediatek.wfo.MwisConstants;

// For operator add-on
import com.mediatek.ims.ext.DigitsUtil;
import com.mediatek.ims.internal.op.OpImsServiceFactoryBase;
import com.mediatek.ims.internal.op.OpImsCallSessionProxy;

public class ImsCallSessionProxy extends ImsCallSessionImplBase {
    private static final String LOG_TAG = "ImsCallSessionProxy";
    private static final boolean DBG = true;
    private static final boolean VDBG = false; // STOPSHIP if true

    // Sensitive log task
    private static final String PROP_FORCE_DEBUG_KEY = "persist.vendor.log.tel_dbg";
    private static final boolean SENLOG = TextUtils.equals(Build.TYPE, "user");
    private static final boolean TELDBG = (SystemProperties.getInt(PROP_FORCE_DEBUG_KEY, 0) == 1);

    private String mCallId;
    private int mPhoneId;
    private int mState = ImsCallSession.State.IDLE;
    private Context mContext;
    private ImsService mImsService;
    private ImsCommandsInterface mImsRILAdapter;
    private ImsCallProfile mCallProfile;
    private ImsCallSessionListener mListener;
    private final Handler mHandler;
    private final Handler mServiceHandler;
    private boolean mHasPendingMo = false;
    private boolean mHasPendingDisconnect;
    private int mPendingDisconnectReason;
    private boolean mIsMerging = false;
    private boolean mIsOnTerminated = false;
    private boolean mIsAddRemoveParticipantsCommandOK = false;
    private String[] mPendingParticipantInfo ;
    private int mPendingParticipantInfoIndex = 0;
    private int mPendingParticipantStatistics = 0;
    private boolean mIsHideHoldEventDuringMerging = false;
    private String mMergeCallId = "";
    private ImsCallInfo.State mMergeCallStatus = ImsCallInfo.State.INVALID;
    private String mMergedCallId = "";
    private ImsCallInfo.State mMergedCallStatus = ImsCallInfo.State.INVALID;
    // normal call merge normal call
    private boolean mNormalCallsMerge = false;
    // at least one call is merged successfully
    private boolean mThreeWayMergeSucceeded = false;
    // count for +ECONF number in normal call merge normal call case
    private int mEconfCount = 0;
    private ImsCallSessionProxy mConfSessionProxy;
    private MtkImsCallSessionProxy mMtkConfSessionProxy;
    private boolean mHaveUpdateConferenceWithMember = false;
    private ArrayList<String> mSelfAddressList;
    private boolean mRadioUnavailable = false;

    private String mCallNumber;

    // WFC
    private IWifiOffloadService mWfoService;
    private int mRatType = WifiOffloadManager.RAN_TYPE_MOBILE_3GPP;
    private static final int WFC_GET_CAUSE_FAILED = -1;

    // For ViLTE.
    private ImsVTProvider mVTProvider;
    private ImsVTProviderUtil mVTProviderUtil = ImsVTProviderUtil.getInstance();
    private ImsCallProfile mLocalCallProfile;
    private ImsCallProfile mRemoteCallProfile;

    // Cached for SIP Error codes, update it when +ESIPCPI.
    private int mLastSipMethod;
    private int mLastSipCode;
    private String mLastSIPReasonHeader;

    /// M: ALPS02502328, cache the special terminate reason.
    private int mTerminateReason = -1;

    /// M: Enhance for force hangup.
    private int mHangupCount = 0;

    private boolean mIsOneKeyConf = false;
    private ConferenceEventListener mConfEvtListener;
    private String[] mOneKeyparticipants = null;

    private enum CallErrorState {
        IDLE, DIAL, DISCONNECT;
    };

    private CallErrorState mCallErrorState = CallErrorState.IDLE;

    private Message mDtmfMsg = null;
    private Messenger mDtmfTarget = null;

    IWifiOffloadListenerProxy mWosListener;

    // For ECT, UA will put the active call on hold first.
    // Ignore the held event (ECPI 131) to avoid 2 held calls on Fwk in ECT period.
    private boolean mIsHideHoldDuringECT = false;
    // For the case call ECT not through call session but Ims Service
    // we need to callback the result with the msg caller passed in
    private Message mEctMsg = null;
    private Messenger mEctTarget = null;

    private ImsReasonInfo mImsReasonInfo = null;
    private boolean mShouldUpdateAddressByPau = true;
    private boolean mShouldUpdateAddressBySipField = true;

    public MtkImsCallSessionProxy mMtkImsCallSessionProxy;

    // For operator add-on
    private OpImsCallSessionProxy mOpExt = null;
    private DigitsUtil mDigitsUtil = null;
    private com.mediatek.ims.ext.OpImsCallSessionProxy mOpImsCallSession = null;

    private int mBadRssiThreshould = -90;

    private int mVideoState = VIDEO_STATE_SEND_RECV;

    private static final int VIDEO_STATE_PAUSE = 0;
    private static final int VIDEO_STATE_SEND_ONLY = 1;
    private static final int VIDEO_STATE_RECV_ONLY = 2;
    private static final int VIDEO_STATE_SEND_RECV = 3;

    private static final String VT_PROVIDER_ID = "video_provider_id";
    private static final String USER_ENTITY = "user-entity";
    private static final String NA_PRIOR_CLIR_PREFIX = "*82";

    // For VzW self activation
    protected ImsSelfActivatorBase mSelfActivateHelper;
    private boolean mHasTriedSelfActivation = false;

    private boolean mSipSessionProgress = false;

    private ImsServiceCallTracker mImsServiceCT;

    private boolean mIsIncomingCall = true;
    private boolean mIsEmergencyCall = false;
    private boolean mIsConferenceHost = false;

    private AsyncResult mCachedUserInfo = null;

    private boolean mWasVideoCallForRtt = false;

    public static final int CALL_INFO_MSG_TYPE_SETUP = 0;
    public static final int CALL_INFO_MSG_TYPE_ALERT = 2;
    public static final int CALL_INFO_MSG_TYPE_CONNECTED = 6;
    public static final int CALL_INFO_MSG_TYPE_MO_CALL_ID_ASSIGN = 130;
    public static final int CALL_INFO_MSG_TYPE_HELD = 131;
    public static final int CALL_INFO_MSG_TYPE_ACTIVE = 132;
    public static final int CALL_INFO_MSG_TYPE_DISCONNECTED = 133;
    public static final int CALL_INFO_MSG_TYPE_REMOTE_HOLD = 135;
    public static final int CALL_INFO_MSG_TYPE_REMOTE_RESUME = 136;

    public static final String EXTRA_CALL_INFO_MESSAGE_TYPE = "mediatek:callInfoMessageType";
    public static final String EXTRA_CALL_TYPE = "mediatek:callType";
    public static final String EXTRA_RAT_TYPE = "mediatek:ratType";
    public static final String EXTRA_INCOMING_CALL = "mediatek:incomingCall";
    public static final String EXTRA_EMERGENCY_CALL = "mediatek:emergencyCall";

    private static final String TAG_DOUBLE_QUOTE = "<ascii_34>";

    private static final int INVALID_CALL_MODE = 0xFF;
    private static final int IMS_VOICE_CALL = 20;
    private static final int IMS_VIDEO_CALL = 21;
    private static final int IMS_VOICE_CONF = 22;
    private static final int IMS_VIDEO_CONF = 23;
    private static final int IMS_VOICE_CONF_PARTS = 24;
    private static final int IMS_VIDEO_CONF_PARTS = 25;

    // WFC
    private static final int IMS_CALL_TYPE_LTE = 1;
    private static final int IMS_CALL_TYPE_WIFI = 2;
    // For UE initial USSI request or response network USSI
    private static final int USSI_REQUEST = 1;
    private static final int USSI_RESPONSE = 2;

    //***** Events URC
    private static final int EVENT_POLL_CALLS_RESULT             = 101;
    private static final int EVENT_CALL_INFO_INDICATION          = 102;
    //private static final int EVENT_RINGBACK_TONE                 = 103;
    private static final int EVENT_ECONF_RESULT_INDICATION       = 104;
    private static final int EVENT_GET_LAST_CALL_FAIL_CAUSE      = 105;
    private static final int EVENT_CALL_MODE_CHANGE_INDICATION   = 106;
    private static final int EVENT_VIDEO_CAPABILITY_INDICATION   = 107;
    // For incoming USSI event
    private static final int EVENT_ON_USSI                       = 108;
    private static final int EVENT_ECT_RESULT_INDICATION         = 109;
    private static final int EVENT_GTT_CAPABILITY_INDICATION     = 110;
    private static final int EVENT_IMS_CONFERENCE_INDICATION     = 111;

    //***** Events Operation result
    private static final int EVENT_DIAL_RESULT                   = 201;
    private static final int EVENT_ACCEPT_RESULT                 = 202;
    private static final int EVENT_HOLD_RESULT                   = 203;
    private static final int EVENT_RESUME_RESULT                 = 204;
    private static final int EVENT_MERGE_RESULT                  = 205;
    private static final int EVENT_ADD_CONFERENCE_RESULT         = 206;
    private static final int EVENT_REMOVE_CONFERENCE_RESULT      = 207;
    private static final int EVENT_SIP_CODE_INDICATION           = 208;
    private static final int EVENT_DIAL_CONFERENCE_RESULT        = 209;
    private static final int EVENT_SWAP_BEFORE_MERGE_RESULT      = 210;
    private static final int EVENT_RETRIEVE_MERGE_FAIL_RESULT    = 211;
    private static final int EVENT_DTMF_DONE    = 212;
    // For return message of sending/cancel USSI
    private static final int EVENT_SEND_USSI_COMPLETE            = 213;
    private static final int EVENT_CANCEL_USSI_COMPLETE          = 214;
    private static final int EVENT_ECT_RESULT                    = 215;
    private static final int EVENT_PULL_CALL_RESULT              = 216;

    private static final int EVENT_RADIO_NOT_AVAILABLE           = 217;

    //for RTT
    private static final int EVENT_RTT_TEXT_RECEIVE_INDICATION   = 218;
    private static final int EVENT_RTT_MODIFY_RESPONSE           = 219;
    private static final int EVENT_RTT_MODIFY_REQUEST_RECEIVE    = 220;

    private static final int EVENT_DIAL_FROM_RESULT              = 221;
    private static final int EVENT_DEVICE_SWITCH_REPONSE         = 222;

    private static final int EVENT_SPEECH_CODEC_INFO             = 223;

    private static final int EVENT_REDIAL_ECC_INDICATION         = 224;

    private static final int EVENT_AUDIO_INDICATION              = 225;

    private static final int QCELP13K = 1;
    private static final int EVRC = 2;
    private static final int EVRC_B = 3;
    private static final int EVRC_WB = 4;
    private static final int EVRC_NW = 5;
    private static final int AMR_NB = 6;
    private static final int AMR_WB = 7;
    private static final int GSM_EFR = 8;
    private static final int GSM_FR = 9;
    private static final int GSM_HR = 10;
    private static final int EVS_NB = 23;
    private static final int EVS_WB = 24;
    private static final int EVS_SW = 25;
    private static final int EVS_FB = 32;
    private static final int EVS_AWB = 33;

    // MD define
    // typedef  enum{
    //     IMS_CC_MO_DISCONNECT_CAUSE_NONE=0,
    //     IMS_CC_MO_DISCONNECT_CAUSE_NO_COVERAGE,
    //     IMS_CC_MO_DISCONNECT_CAUSE_LOW_BATTERY,
    //     IMS_CC_MO_DISCONNECT_CAUSE_FORWARD,
    //     IMS_CC_MO_DISCONNECT_CAUSE_SPECIAL_HANGUP,
    //     IMS_CC_MO_DISCONNECT_CAUSE_END,
    // }
    private static final int HANGUP_CAUSE_NONE = 0;
    private static final int HANGUP_CAUSE_NO_COVERAGE = 1;
    private static final int HANGUP_CAUSE_LOW_BATTERY = 2;
    private static final int HANGUP_CAUSE_FORWARD = 3;
    private static final int HANGUP_CAUSE_SPECIAL_HANGUP = 4;

    private class ImsCallProfileEx {
        public static final String EXTRA_MPTY = "mpty";
        public static final String EXTRA_INCOMING_MPTY = "incoming_mpty";
        public static final String EXTRA_VERSTAT = "verstat";
    }

    private class ImsCallLogLevel {
        public static final int VERBOSE     = 1;
        public static final int DEBUG       = 2;
        public static final int INFO        = 3;
        public static final int WARNNING    = 4;
        public static final int ERROR       = 5;
    }

    private HashMap<String, Bundle> mParticipants = new HashMap<String, Bundle>();
    /**
    * This class is used to store IMS conference call user information.
    *
    */
    public static class User {
        public String mUserAddr;
        public String mEndPoint;
        public String mEntity;
        public String mDisplayText;
        public String mStatus = ImsConferenceState.STATUS_DISCONNECTED;  //Default is "disconnected"
    }

    private class VtProviderListener implements ImsVTProvider.VideoProviderStateListener {
        public void onReceivePauseState(boolean isPause) {
            if (mCallProfile == null) {
                return;
            }
            logWithCallId("onReceivePauseState() : " + isPause, ImsCallLogLevel.DEBUG);
            if (isPause) {
                mCallProfile.mMediaProfile.mVideoDirection =
                        ImsStreamMediaProfile.DIRECTION_INACTIVE;
            } else {
                mCallProfile.mMediaProfile.mVideoDirection =
                        ImsStreamMediaProfile.DIRECTION_SEND_RECEIVE;
            }

            notifyCallSessionUpdated();
        }

        public void onReceiveWiFiUsage(long usage) {
        }
    }

    private VtProviderListener mVtProviderListener = new VtProviderListener();

    //Constructor for MT call
    ImsCallSessionProxy(Context context, ImsCallProfile profile,
                        ImsCallSessionListener listener, ImsService imsService,
                        Handler handler, ImsCommandsInterface imsRILAdapter,
                        String callId, int phoneId,
                        MtkImsCallSessionProxy mtkImsCallSessionProxy) {

        this(context, profile, listener, imsService, handler, imsRILAdapter, callId, phoneId);
        mMtkImsCallSessionProxy = mtkImsCallSessionProxy;
    }

    // For netwrok initiated USSI, we don't need to setOnUSSI
    ImsCallSessionProxy(Context context, ImsCallProfile profile,
                        ImsCallSessionListener listener, ImsService imsService,
                        Handler handler, ImsCommandsInterface imsRILAdapter,
                        String callId, int phoneId, boolean nwInitUssi) {

        this(context, profile, listener, imsService, handler, imsRILAdapter, callId, phoneId);
        if (nwInitUssi) {
            logWithCallId("ImsCallSessionProxy() : unSetOnUSSI", ImsCallLogLevel.DEBUG);
            mImsRILAdapter.unSetOnUSSI(mHandler);
        }
    }

    ImsCallSessionProxy(Context context, ImsCallProfile profile,
                        ImsCallSessionListener listener, ImsService imsService,
                        Handler handler, ImsCommandsInterface imsRILAdapter,
                        String callId, int phoneId) {

        mPhoneId = phoneId;
        mImsServiceCT = ImsServiceCallTracker.getInstance(mPhoneId);
        mServiceHandler = handler;
        mHandler = new MyHandler(handler.getLooper());
        mContext = context;
        mCallProfile = profile;
        mLocalCallProfile = new ImsCallProfile(profile.mServiceType, profile.mCallType);
        mRemoteCallProfile = new ImsCallProfile(profile.mServiceType, profile.mCallType);

        if (OperatorUtils.isMatched(OperatorUtils.OPID.OP12, mPhoneId)) {
            // set default as no HD icon display
            Rlog.d(LOG_TAG, "VzW: set default as no HD icon");
            mRemoteCallProfile.mRestrictCause = ImsCallProfile.CALL_RESTRICT_CAUSE_HD;
        }

        mListener = listener;
        mImsService = imsService;
        mImsRILAdapter = imsRILAdapter;
        mCallId = callId;

        if (DBG) {
            logWithCallId("ImsCallSessionProxy() : RILAdapter:" + imsRILAdapter + "imsService:" +
                    imsService + " callID:" + callId + " phoneId: " + phoneId, ImsCallLogLevel.DEBUG);
        }

        // For operator add-on
        mOpExt = OpImsServiceFactoryBase.getInstance().makeOpImsCallSessionProxy();
        mDigitsUtil = OpImsServiceCustomizationUtils.getOpFactory(context).makeDigitsUtil();
        mOpImsCallSession = OpImsServiceCustomizationUtils.getOpFactory(context)
                .makeOpImsCallSessionProxy();

        mImsRILAdapter.registerForCallInfo(mHandler, EVENT_CALL_INFO_INDICATION, null);
        /// M: Register for updating conference call merged/added result.
        mImsRILAdapter.registerForEconfResult(mHandler, EVENT_ECONF_RESULT_INDICATION, null);
        mImsRILAdapter.registerForCallProgressIndicator(mHandler, EVENT_SIP_CODE_INDICATION, null);
        mImsRILAdapter.registerForCallModeChangeIndicator(mHandler,
                EVENT_CALL_MODE_CHANGE_INDICATION, null);
        mImsRILAdapter.registerForVideoCapabilityIndicator(mHandler,
                EVENT_VIDEO_CAPABILITY_INDICATION, null);
        mImsRILAdapter.registerForEctResult(mHandler, EVENT_ECT_RESULT_INDICATION, null);
        mImsRILAdapter.registerForImsConfInfoUpdate(mHandler, EVENT_IMS_CONFERENCE_INDICATION, null);

        mImsRILAdapter.registerForNotAvailable(mHandler, EVENT_RADIO_NOT_AVAILABLE, null);
        mImsRILAdapter.registerForSpeechCodecInfo(mHandler, EVENT_SPEECH_CODEC_INFO, null);
        mImsRILAdapter.registerForImsRedialEccInd(mHandler, EVENT_REDIAL_ECC_INDICATION, null);

        mSelfActivateHelper = getImsExtSelfActivator(
            context, handler, this, imsRILAdapter, imsService, phoneId);

        ///M: Register for RTT
        mImsRILAdapter.getOpCommandsInterface().registerForGttCapabilityIndicator(mHandler,
            EVENT_GTT_CAPABILITY_INDICATION, null);
        mImsRILAdapter.getOpCommandsInterface().registerForRttModifyRequestReceive(mHandler,
            EVENT_RTT_MODIFY_REQUEST_RECEIVE, null);
        mImsRILAdapter.getOpCommandsInterface().registerForRttTextReceive(mHandler,
            EVENT_RTT_TEXT_RECEIVE_INDICATION, null);
        mImsRILAdapter.getOpCommandsInterface().registerForRttModifyResponse(mHandler,
            EVENT_RTT_MODIFY_RESPONSE, null);

        /// M: Register for audio indication of RTT
        mImsRILAdapter.getOpCommandsInterface().registerForAudioIndication(
                mHandler, EVENT_AUDIO_INDICATION, null);

        if (SystemProperties.get("persist.vendor.vilte_support").equals("1")) {

            logWithCallId("ImsCallSessionProxy() : start new VTProvider", ImsCallLogLevel.DEBUG);

            if (mCallId != null) {
                //MT:new VT service
                mVTProvider =
                        OpImsServiceCustomizationUtils.getOpFactory(mContext).makeImsVtProvider();
                mVTProviderUtil.bind(mVTProvider, Integer.parseInt(mCallId), mPhoneId);
            } else {
                //MO:new VT service
                mVTProvider =
                        OpImsServiceCustomizationUtils.getOpFactory(mContext).makeImsVtProvider();
            }
            mVTProvider.addVideoProviderStateListener(mVtProviderListener);

            logWithCallId("ImsCallSessionProxy() : end new VTProvider", ImsCallLogLevel.DEBUG);
        }
        if (mCallId == null) {
            mIsIncomingCall = false;
        }
        /// M: Listen to incoming USSI event @{
        if (mCallProfile.getCallExtraInt(ImsCallProfile.EXTRA_DIALSTRING,
                ImsCallProfile.DIALSTRING_NORMAL) == ImsCallProfile.DIALSTRING_USSD) {
            logWithCallId("ImsCallSessionProxy() : setOnUSSI", ImsCallLogLevel.DEBUG);
            mImsRILAdapter.setOnUSSI(mHandler, EVENT_ON_USSI, null);
        }
        /// @}

        // WFC: Registers the listener to WifiOffloadService for handover event and get rat type
        // from WifiOffloadService.
        IBinder b = ServiceManager.getService(WifiOffloadManager.WFO_SERVICE);
        if (b != null) {
            mWfoService = IWifiOffloadService.Stub.asInterface(b);
        } else {
            b = ServiceManager.getService(MwisConstants.MWI_SERVICE);
            try {
                if (b != null) {
                    mWfoService = IMwiService.Stub.asInterface(b).getWfcHandlerInterface();
                } else {
                    logWithCallId("ImsCallSessionProxy() : No MwiService exist", ImsCallLogLevel.ERROR);
                }
            } catch (RemoteException e) {
                logWithCallId("ImsCallSessionProxy() : can't get MwiService" + e, ImsCallLogLevel.ERROR);
            }
        }
        if (mWfoService != null) {
            try {
                if (mWosListener == null) {
                    mWosListener = new IWifiOffloadListenerProxy();
                }
                mWfoService.registerForHandoverEvent(mWosListener);

                // Update WiFiOffloadService call status for MT or conference case.
                if (callId != null) {
                    updateCallStateForWifiOffload(ImsCallSession.State.ESTABLISHING);
                }
            } catch (RemoteException e) {
                logWithCallId("ImsCallSessionProxy() : RemoteException ImsCallSessionProxy()", ImsCallLogLevel.ERROR);
            }
        }
        updateRat(mImsService.getRatType(mPhoneId));
        logWithCallId("ImsCallSessionProxy() : [WFC]mRatType is " + mRatType, ImsCallLogLevel.DEBUG);

        mConfSessionProxy = null;
        mMtkConfSessionProxy = null;
        updateShouldUseSipField();
    }

    // Constructor for MO call
    ImsCallSessionProxy(Context context, ImsCallProfile profile,
                        ImsCallSessionListener listener, ImsService imsService,
                        Handler handler, ImsCommandsInterface imsRILAdapter, int phoneId) {
        this(context, profile, listener, imsService, handler, imsRILAdapter, null, phoneId);
        if (DBG) {
            logWithCallId("ImsCallSessionProxy() : ImsCallSessionProxy MO constructor", ImsCallLogLevel.DEBUG);
        }
        mIsIncomingCall = false;
    }

    @Override
    public void close() {
        if (DBG) {
            logWithCallId("close() : ImsCallSessionProxy is closed!!! ", ImsCallLogLevel.DEBUG);
        }

        if (mState == ImsCallSession.State.INVALID) {
            logWithCallId("close() : ImsCallSessionProxy already closed", ImsCallLogLevel.DEBUG);
            return;
        }
        mState = ImsCallSession.State.INVALID;
        mImsServiceCT.removeCallConnection(mCallId);
        mImsRILAdapter.unregisterForCallInfo(mHandler);
        mImsRILAdapter.unregisterForEconfResult(mHandler);
        mImsRILAdapter.unregisterForCallProgressIndicator(mHandler);
        mImsRILAdapter.unregisterForCallModeChangeIndicator(mHandler);
        mImsRILAdapter.unregisterForVideoCapabilityIndicator(mHandler);
        mImsRILAdapter.unregisterForEctResult(mHandler);

        mImsRILAdapter.unregisterForNotAvailable(mHandler);
        mImsRILAdapter.unregisterForSpeechCodecInfo(mHandler);
        mImsRILAdapter.unregisterForImsRedialEccInd(mHandler);

        ///M: unregister for RTT
        mImsRILAdapter.getOpCommandsInterface().unregisterForGttCapabilityIndicator(mHandler);
        mImsRILAdapter.getOpCommandsInterface().unregisterForRttModifyResponse(mHandler);
        mImsRILAdapter.getOpCommandsInterface().unregisterForRttTextReceive(mHandler);
        mImsRILAdapter.getOpCommandsInterface().unregisterForRttModifyRequestReceive(mHandler);

        /// M: unregister for audio indication of RTT
        mImsRILAdapter.getOpCommandsInterface().unregisterForAudioIndication(mHandler);

        /// M: Unregister listening incoming USSI event @{
        mImsRILAdapter.unSetOnUSSI(mHandler);
        /// @}
        mImsRILAdapter.unregisterForImsConfInfoUpdate(mHandler);
        mParticipants.clear();

        IImsVideoCallProvider vtProvider = getVideoCallProvider();

        if (vtProvider != null) {
            logWithCallId("close() : Start VtProvider setUIMode", ImsCallLogLevel.DEBUG);

            mVTProviderUtil.setUIMode(mVTProvider, ImsVTProviderUtil.UI_MODE_DESTROY);
            mVTProvider.removeVideoProviderStateListener(mVtProviderListener);
            mVTProvider = null;
        }

        ImsConferenceHandler.getInstance().closeConference(mCallId);
        mConfEvtListener = null;
        mOneKeyparticipants = null;

        // unregister the wfo listener
        if (mWfoService != null && mWosListener != null) {
            try {
                mWfoService.unregisterForHandoverEvent(mWosListener);
            } catch (RemoteException e) {
                logWithCallId("close() : RemoteException when unregisterForHandoverEvent", ImsCallLogLevel.ERROR);
            }
        }

        if (mSelfActivateHelper != null) {
            mSelfActivateHelper.close();
            mSelfActivateHelper = null;
        }

        mCallId = null;
        mListener = null;
        mCachedUserInfo = null;
    }

    @Override
    public String getCallId() {
        return mCallId;
    }

    @Override
    public ImsCallProfile getCallProfile(){
        return mCallProfile;
    }

    @Override
    public ImsCallProfile getLocalCallProfile() {
        return mLocalCallProfile;
    }

    @Override
    public ImsCallProfile getRemoteCallProfile() {
        return mRemoteCallProfile;
    }

    @Override
    public String getProperty(String name) {
        return mCallProfile.getCallExtra(name);
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public boolean isInCall() {
        return false;
    }

    @Override
    public void setListener(ImsCallSessionListener listener) {
        mListener = listener;

        if (mListener != null && mCachedUserInfo != null) {
            logWithCallId("setListener and has unhandled ImsConference CEP", ImsCallLogLevel.DEBUG);
            mHandler.obtainMessage(EVENT_IMS_CONFERENCE_INDICATION, mCachedUserInfo).sendToTarget();
            mCachedUserInfo = null;
        }
    }

    @Override
    public void setMute(boolean muted) {
        mImsRILAdapter.setMute(muted, null);
    }

    @Override
    public void start(String callee, ImsCallProfile profile) {
        if (isCallPull(profile)) {
            pullCall(callee, profile);
            mHasPendingMo = true;
            mCallNumber = callee;
            updateShouldUpdateAddressByPau();
            return;
        }

        int clirMode = profile.getCallExtraInt(ImsCallProfile.EXTRA_OIR, 0);
        mIsEmergencyCall = (profile.mServiceType == ImsCallProfile.SERVICE_TYPE_EMERGENCY);
        boolean byEmergencyPdn = mIsEmergencyCall;
        int subId = mImsService.getSubIdUsingPhoneId(mPhoneId);

        if (mIsEmergencyCall && getImsExtCallUtil().isSpecialEmergencyNumber(subId, callee)) {
            byEmergencyPdn = false;
        }

        if (!ImsCommonUtil.supportMdAutoSetupIms()) {
            // ALPS03435385, *82 is higher priority than CLIR invocation.
            boolean isNAPriorityClirSupported = getBooleanFromCarrierConfig(ImsCarrierConfigConstants.
                    MTK_KEY_CARRIER_NOURTH_AMERICA_HIGH_PRIORITY_CLIR_PREFIX_SUPPORTED);

            if ((isNAPriorityClirSupported
                    && clirMode == CommandsInterface.CLIR_INVOCATION
                    && callee.startsWith(NA_PRIOR_CLIR_PREFIX))
                    || mIsEmergencyCall) {

                logWithCallId("start() : Prior CLIR supported, *82 or ECC is higher priority than CLIR invocation.", ImsCallLogLevel.DEBUG);

                clirMode = CommandsInterface.CLIR_DEFAULT;
            }
        }

        if (mIsEmergencyCall) {
            // IMS ECC not supported, hangup all holding / active call to make it possible to CSFB
            // ALPS04680554 Should only check the value from +CNEMS1.
            if (!mImsService.isImsEccSupportedWhenNormalService(mPhoneId)) {
                logWithCallId("start() : Hangup all if IMS Ecc not supported", ImsCallLogLevel.DEBUG);
                mImsRILAdapter.hangupAllCall(null);
            }
        }

        /// M: TMO requirement, IR94_6873 and IR94_6874. @{
        if (isVideoCall(profile)) {
            if(mOpImsCallSession.isValidVtDialString(callee) == false) {
                rejectDial();
                return;
            } else {
                callee = mOpImsCallSession.normalizeVtDialString(callee);
            }
        }
        /// @}

        if (mSelfActivateHelper != null && !mHasTriedSelfActivation &&
                    mSelfActivateHelper.shouldProcessSelfActivation(mPhoneId)) {
            mSelfActivateHelper.doSelfActivationDial(callee, profile, mIsEmergencyCall);
            // only access self activation once
            mHasTriedSelfActivation = true;
            return;
        }

        /// M: For UE initiated USSI @{
        if (profile.getCallExtraInt(ImsCallProfile.EXTRA_DIALSTRING,
                ImsCallProfile.DIALSTRING_NORMAL) == ImsCallProfile.DIALSTRING_USSD) {
            Message msg = mHandler.obtainMessage(EVENT_SEND_USSI_COMPLETE, USSI_REQUEST, 0);
            mCallProfile = profile;
            if (mDigitsUtil.hasDialFrom(profile)) {
                mDigitsUtil.sendUssiFrom(mImsRILAdapter, profile, USSI_REQUEST, callee, msg);
            } else {
                mImsRILAdapter.sendUSSI(USSI_REQUEST, callee, msg);
            }
            return;
        }
        /// @}

        /// M: E911 during VoLTE off @{
        tryTurnOnVolteForE911(byEmergencyPdn);
        /// @}

        //M: for RTT, set RTT mode for Dial
        mOpExt.setRttModeForDial(mCallId, mImsRILAdapter, profile.mMediaProfile.isRttCall());
        if (mDigitsUtil.hasDialFrom(profile)) {
            Message response = mHandler.obtainMessage(EVENT_DIAL_FROM_RESULT);
            mDigitsUtil.startFrom(callee, profile, clirMode, isVideoCall(profile),
                    mImsRILAdapter, response);
        } else {
            Message response = mHandler.obtainMessage(EVENT_DIAL_RESULT);
            mImsRILAdapter.start(callee, clirMode, byEmergencyPdn, isVideoCall(profile),
                    mPhoneId, response);
        }
        mHasPendingMo = true;
        mCallNumber = callee;
        updateShouldUpdateAddressByPau();
    }

    @Override
    public void startConference(
                String[] participants, ImsCallProfile profile) {
        int clirMode = profile.getCallExtraInt(ImsCallProfile.EXTRA_OIR, 0);
        Message result = mHandler.obtainMessage(EVENT_DIAL_CONFERENCE_RESULT);

        mImsRILAdapter.startConference(participants, clirMode, isVideoCall(profile), mPhoneId,
                result);

        mHasPendingMo = true;
        mIsOneKeyConf = true;
        mOneKeyparticipants = participants;
        updateShouldUpdateAddressByPau();
    }

    @Override
    public void accept(int callType, ImsStreamMediaProfile profile) {
        logWithCallId("accept() : original call Type:" + mCallProfile.mCallType
                + "accept as:" + callType, ImsCallLogLevel.DEBUG);

        /// M: For network initiated USSI @{
        if (mCallProfile.getCallExtraInt(ImsCallProfile.EXTRA_DIALSTRING,
            ImsCallProfile.DIALSTRING_NORMAL) == ImsCallProfile.DIALSTRING_USSD) {
            String m = mCallProfile.getCallExtra("m");
            String n = mCallProfile.getCallExtra("n");
            String str = mCallProfile.getCallExtra("str");

            logWithCallId("accept() : " + m + "," + n + "," + str, ImsCallLogLevel.DEBUG);

            int ussiClass = Integer.parseInt(m);
            int ussiStatus = Integer.parseInt(n);
            int mode = -1;

            if (ussiClass == 1) {
                if (ussiStatus == 0 || ussiStatus == 2) {
                    mode = ImsCall.USSD_MODE_NOTIFY;
                }
                if (ussiStatus == 1) {
                    mode = ImsCall.USSD_MODE_REQUEST;
                }
            } else if (ussiClass == 3) {
                if (ussiStatus == 0) {
                    mode = ImsCall.USSD_MODE_NOTIFY;
                }
            }

            if (mListener != null) {
                mListener.callSessionUssdMessageReceived(mode, str);
            }
            return;
        }
        /// @}

        if (mCallProfile.mCallType == ImsCallProfile.CALL_TYPE_VOICE) {
            mImsRILAdapter.accept();
        } else {
            int videoMode;
            /* We set the videoMode base on AT+EVTA mode value.
             * AT+EVTA=<mode>,<call id>
             * Mode  =1 , accept as audio
             * Mode  =2 , accept as one way only video (Rx)
             * Mode  =3 , accept as one way only video (Tx)
             * For videoMode = 0, we will use ATA command to accept this video call.
             */
            switch (callType) {
                case ImsCallProfile.CALL_TYPE_VT:
                    videoMode = 0;
                    break;
                case ImsCallProfile.CALL_TYPE_VOICE:
                    videoMode = 1;
                    break;
                case ImsCallProfile.CALL_TYPE_VT_RX:
                    videoMode = 2;
                    break;
                case ImsCallProfile.CALL_TYPE_VT_TX:
                    videoMode = 3;
                    break;
                default:
                    videoMode = 0;
                    break;
            }
            mImsRILAdapter.acceptVideoCall(videoMode, Integer.parseInt(mCallId));
        }
    }

    @Override
    public void reject(int reason) {
        if (mCallId != null) {
            int cause = getHangupCause(reason);

            if (cause <= HANGUP_CAUSE_NONE) {
                mImsRILAdapter.reject(Integer.parseInt(mCallId));
            }else {
                mImsRILAdapter.reject(Integer.parseInt(mCallId), cause);
            }
        } else {
            logWithCallId("reject() : Reject Call fail since there is no call ID. Abnormal Case", ImsCallLogLevel.ERROR);
        }
    }

    @Override
    public void terminate(int reason) {
        if (mOpImsCallSession.handleHangup() == true) return;
        /// M: For terminated USSI @{
        if (mCallProfile.getCallExtraInt(ImsCallProfile.EXTRA_DIALSTRING,
            ImsCallProfile.DIALSTRING_NORMAL) == ImsCallProfile.DIALSTRING_USSD) {
            Message msg = mHandler.obtainMessage(EVENT_CANCEL_USSI_COMPLETE);
            if (mDigitsUtil.hasDialFrom(mCallProfile)) {
                mDigitsUtil.cancelUssiFrom(mImsRILAdapter, mCallProfile, msg);
            } else {
                mImsRILAdapter.cancelPendingUssi(msg);
            }
            return;
        }
        /// @}

        if (mCallId != null) {
            /// M: Support force hangup. @{
            int cause = getHangupCause(reason);
            if (mHangupCount > 0) {
                if (cause <= HANGUP_CAUSE_NONE) {
                    mImsRILAdapter.forceHangup(Integer.parseInt(mCallId));
                }else {
                    mImsRILAdapter.forceHangup(Integer.parseInt(mCallId), cause);
                }
            } else {
                if (cause <= HANGUP_CAUSE_NONE) {
                    mImsRILAdapter.terminate(Integer.parseInt(mCallId));
                }else {
                    mImsRILAdapter.terminate(Integer.parseInt(mCallId), cause);
                }
                if (mIsMerging) {
                    terminateConferenceSession();
                }
            }
            /// @}
            /// M: ALPS02502328, cache the terminate reason.
            mTerminateReason = reason;
            ++mHangupCount;
            mState = ImsCallSession.State.TERMINATING;
        } else {
            logWithCallId("terminate() : Terminate Call fail since there is no call ID. Abnormal Case", ImsCallLogLevel.ERROR);
            if (mHasPendingMo) {
                logWithCallId("terminate() : Pending M0, wait for assign call id", ImsCallLogLevel.ERROR);
                mHasPendingDisconnect = true;
                mPendingDisconnectReason = reason;
            }
        }
    }

    @Override
    public void hold(ImsStreamMediaProfile profile) {
        Message result = mHandler.obtainMessage(EVENT_HOLD_RESULT);
        mImsRILAdapter.hold(Integer.parseInt(mCallId), result);
    }

    @Override
    public void resume(ImsStreamMediaProfile profile) {
        Message result = mHandler.obtainMessage(EVENT_RESUME_RESULT);
        mImsRILAdapter.resume(Integer.parseInt(mCallId), result);
    }

    @Override
    public void merge() {
        Message result;
        logWithCallId("merge()", ImsCallLogLevel.DEBUG);
        ImsCallInfo myCallInfo = mImsServiceCT.getCallInfo(mCallId);
        ImsCallInfo beMergedCallInfo = null;

        boolean needSwapConfToFg = getBooleanFromCarrierConfig(
                ImsCarrierConfigConstants.MTK_KEY_CARRIER_SWAP_CONFERENCE_TO_FOREGROUND_BEFORE_MERGE);

        boolean needSwapVtConfToFg = getBooleanFromCarrierConfig(
                ImsCarrierConfigConstants.MTK_KEY_CARRIER_SWAP_VT_CONFERENCE_TO_FOREGROUND_BEFORE_MERGE);

        if (myCallInfo == null) {
            logWithCallId("merge() : can't find this call callInfo", ImsCallLogLevel.ERROR);
            mergeFailed();
            return;
        }

        if (myCallInfo.mState == ImsCallInfo.State.ACTIVE) {
            beMergedCallInfo = mImsServiceCT.getCallInfo(ImsCallInfo.State.HOLDING);
        } else if (myCallInfo.mState == ImsCallInfo.State.HOLDING) {
            beMergedCallInfo = mImsServiceCT.getCallInfo(ImsCallInfo.State.ACTIVE);
        }

        if (beMergedCallInfo == null) {
            logWithCallId("merge() : can't find another call's callInfo", ImsCallLogLevel.ERROR);
            mergeFailed();
            return;
        }

        if (mCallProfile != null && mCallProfile.isVideoCall() == true) {
            needSwapConfToFg = needSwapVtConfToFg;
        }
        logWithCallId("merge() : merge command- my call: conference type=" + myCallInfo.mIsConference +
                " call status=" + myCallInfo.mState + " beMergedCall: conference type=" +
                beMergedCallInfo.mIsConference + " call status=" + beMergedCallInfo.mState +
                " needSwapConfToFg=" + needSwapConfToFg, ImsCallLogLevel.DEBUG);

        mMergeCallId = myCallInfo.mCallId;
        mMergeCallStatus = myCallInfo.mState;
        mMergedCallId = beMergedCallInfo.mCallId;
        mMergedCallStatus = beMergedCallInfo.mState;

        mIsMerging = true;

        DefaultConferenceHandler confHdler = ImsConferenceHandler.getInstance();

        if (myCallInfo.mIsConferenceHost == false && beMergedCallInfo.mIsConferenceHost == false) {
            //Case 1: Normal call merge normal call
            result = mHandler.obtainMessage(EVENT_MERGE_RESULT);
            mImsRILAdapter.merge(result);
            mIsHideHoldEventDuringMerging = true;
            mNormalCallsMerge = true;
            // keep the conf numbers
            confHdler.firstMerge(myCallInfo.mCallNum, beMergedCallInfo.mCallNum);
        } else if (myCallInfo.mIsConferenceHost == true && beMergedCallInfo.mIsConferenceHost == true) {
            // Case 2: conference call merge conference call
            logWithCallId("merge() : conference call merge conference call", ImsCallLogLevel.DEBUG);
            result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);
            mImsRILAdapter.inviteParticipant(
                    Integer.parseInt(mCallId),
                    beMergedCallInfo.mCallNum,
                    Integer.parseInt(beMergedCallInfo.mCallId),
                    result);
            return;
        } else {
            // Keep the adding conf number
            confHdler.tryAddParticipant(
                (myCallInfo.mIsConferenceHost)? beMergedCallInfo.mCallNum : myCallInfo.mCallNum);
            if (needSwapConfToFg == false) {
                // normal case
                if (myCallInfo.mIsConferenceHost) {
                    logWithCallId("merge() : active conference call merge background normal call", ImsCallLogLevel.DEBUG);
                    result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);
                    mImsRILAdapter.inviteParticipant(
                            Integer.parseInt(mCallId),
                            beMergedCallInfo.mCallNum,
                            Integer.parseInt(beMergedCallInfo.mCallId),
                            result);
                } else {
                    logWithCallId("merge() : active normal call merge background conference call", ImsCallLogLevel.DEBUG);
                    result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);
                    mImsRILAdapter.inviteParticipant(
                            Integer.parseInt(beMergedCallInfo.mCallId),
                            myCallInfo.mCallNum,
                            Integer.parseInt(myCallInfo.mCallId),
                            result);
                }
            } else {
                // OP16 workaround
                if (myCallInfo.mIsConferenceHost && myCallInfo.mState == ImsCallInfo.State.ACTIVE) {
                    logWithCallId("merge() : active conference call merge background normal call", ImsCallLogLevel.DEBUG);
                    result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);
                    mImsRILAdapter.inviteParticipant(
                            Integer.parseInt(mCallId),
                            beMergedCallInfo.mCallNum,
                            Integer.parseInt(beMergedCallInfo.mCallId),
                            result);
                } else if (beMergedCallInfo.mIsConferenceHost &&
                       beMergedCallInfo.mState == ImsCallInfo.State.ACTIVE) {
                    // beMerged is conference call and merge background normal call
                    logWithCallId("merge() : beMergedCall in foreground merge bg normal call", ImsCallLogLevel.DEBUG);
                    result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);
                    mImsRILAdapter.inviteParticipant(
                            Integer.parseInt(beMergedCallInfo.mCallId),
                            myCallInfo.mCallNum,
                            Integer.parseInt(myCallInfo.mCallId),
                            result);
                } else {
                    logWithCallId("merge() : swapping before merge", ImsCallLogLevel.DEBUG);
                    result = mHandler.obtainMessage(EVENT_SWAP_BEFORE_MERGE_RESULT);
                    mImsRILAdapter.swap(result);
                }
            }
        }
    }

    @Override
    public void update(int callType, ImsStreamMediaProfile profile) {
        // currently MD not support for video downgrade or audio upgrade.
    }

    @Override
    public void extendToConference(String[] participants) {
        // currently MD not support to join multiple participants to join conference call.
    }

    void explicitCallTransferWithCallback(Message result, Messenger target) {
        mEctMsg = result;
        mEctTarget = target;
        explicitCallTransfer();
    }

    void explicitCallTransfer() {
        Message result = mHandler.obtainMessage(EVENT_ECT_RESULT);
        mImsRILAdapter.explicitCallTransfer(result);
        mIsHideHoldDuringECT = true;
    }

    void unattendedCallTransfer(String number, int type) {
        Message result = mHandler.obtainMessage(EVENT_ECT_RESULT);
        mImsRILAdapter.unattendedCallTransfer(number, type, result);
        mIsHideHoldDuringECT = true;
    }

    void deviceSwitch(String number, String deviceId) {
        Message result = mHandler.obtainMessage(EVENT_DEVICE_SWITCH_REPONSE);
        mOpImsCallSession.deviceSwitch(mImsRILAdapter, number, deviceId, result);
    }

    void cancelDeviceSwitch() {
        mOpImsCallSession.cancelDeviceSwitch(mImsRILAdapter);
    }

    @Override
    public void inviteParticipants(String[] participants) {
        Message result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);
        mPendingParticipantInfoIndex = 0;
        mPendingParticipantInfo = participants;
        mPendingParticipantStatistics = participants.length;
        if (mCallId != null || mPendingParticipantStatistics == 0) {
            mImsRILAdapter.inviteParticipant(Integer.parseInt(mCallId),
                    mPendingParticipantInfo[mPendingParticipantInfoIndex], -1, result);
        } else {
            logWithCallId("inviteParticipants() : fail since no call ID or participants is null" +
                    " CallID=" + mCallId + " Participant number=" + mPendingParticipantStatistics, ImsCallLogLevel.ERROR);

            if (mListener != null) {
                try {
                    mListener.callSessionInviteParticipantsRequestFailed(new ImsReasonInfo());
                } catch (RuntimeException e) {
                    logWithCallId("RuntimeException callSessionInviteParticipantsRequestFailed()", ImsCallLogLevel.ERROR);
                }
            }
        }
    }

    @Override
    public void removeParticipants(String[] participants) {
        Message result = mHandler.obtainMessage(EVENT_REMOVE_CONFERENCE_RESULT);
        mPendingParticipantInfoIndex = 0;
        mPendingParticipantInfo = participants;
        mPendingParticipantStatistics = participants.length;
        if (mCallId != null || mPendingParticipantStatistics == 0) {
            String addr = mPendingParticipantInfo[mPendingParticipantInfoIndex];
            String participantUri = getConfParticipantUri(addr);
            mImsRILAdapter.removeParticipant(Integer.parseInt(mCallId), participantUri, result);

            ImsConferenceHandler.getInstance().tryRemoveParticipant(addr);
        } else {
            logWithCallId("removeParticipant() : fail since no call ID or participants is null" +
                    " CallID=" + mCallId + " Participant number=" + mPendingParticipantStatistics, ImsCallLogLevel.ERROR);

            if (mListener != null) {
                try {
                    mListener.callSessionRemoveParticipantsRequestFailed(new ImsReasonInfo());
                } catch (RuntimeException e) {
                    logWithCallId("RuntimeException callSessionRemoveParticipantsRequestFailed()", ImsCallLogLevel.ERROR);
                }
            }
        }
    }

    @Override
    public void sendDtmf(char c, Message result) {
        mImsRILAdapter.sendDtmf(c, result);
    }

    @Override
    public void startDtmf(char c) {
        mImsRILAdapter.startDtmf(c, null);
    }

    @Override
    public void stopDtmf() {
        mImsRILAdapter.stopDtmf(null);
    }


    // Google issue. Original sendDtmf could not pass Message.target to another process,
    // because Message.writeToParcel didn't write target. Workaround this issue by adding
    // a new API which passes target by Messenger.
    void sendDtmfbyTarget(char c, Message result, Messenger target) {
        mDtmfMsg = result;
        mDtmfTarget = target;
        // Use ImsCallSessionProxy handler to send result back to original Message target.
        Message local_result = mHandler.obtainMessage(EVENT_DTMF_DONE);
        mImsRILAdapter.sendDtmf(c, local_result);
    }

    @Override
    public void sendUssd(String ussdMessage) {
        /// M: Send USSI request to RIL @{
        Message msg = mHandler.obtainMessage(EVENT_SEND_USSI_COMPLETE, USSI_RESPONSE, 0);
        if (mDigitsUtil.hasDialFrom(mCallProfile)) {
            mDigitsUtil.sendUssiFrom(mImsRILAdapter, mCallProfile, USSI_REQUEST, ussdMessage, msg);
        } else {
            mImsRILAdapter.sendUSSI(USSI_RESPONSE, ussdMessage, msg);
        }
        /// @}
    }

    @Override
    public IImsVideoCallProvider getVideoCallProvider() {
        logWithCallId("getVideoCallProvider() : mVTProvider = " + mVTProvider, ImsCallLogLevel.DEBUG);
        if (mVTProvider != null) {
            return mVTProvider.getInterface();
        } else {
            return null;
        }
    }

    @Override
    public boolean isMultiparty() {
        return mCallProfile.getCallExtraInt(ImsCallProfileEx.EXTRA_MPTY, 0) == 1;
    }

    public boolean isIncomingCallMultiparty() {
        return mCallProfile.getCallExtraInt(ImsCallProfileEx.EXTRA_INCOMING_MPTY, 0) == 1;
    }

    @Override
    public void sendRttMessage(String rttMessage) {
        mOpExt.sendRttMessage(mCallId, mImsRILAdapter, rttMessage);
    }

    @Override
    public void sendRttModifyRequest(ImsCallProfile to) {
        logWithCallId("sendRttModifyRequest() : to = " + to, ImsCallLogLevel.DEBUG);
        mOpExt.sendRttModifyRequest(mCallId, mImsRILAdapter, to);
    }

    @Override
    public void sendRttModifyResponse(boolean response) {
        mOpExt.sendRttModifyResponse(mCallId, mImsRILAdapter, response);
    }

    public void callTerminated() {

        detailLog("callTerminated() : mCallNumber = " + sensitiveEncode(mCallNumber));

        if (isMultiparty()) {
            ImsConferenceHandler.getInstance().closeConference(getCallId());
        }
        if (mState == ImsCallSession.State.INVALID ||
                mState == ImsCallSession.State.TERMINATED) {
            logWithCallId("callTerminated() : mState is INVALID, return: " + mState, ImsCallLogLevel.DEBUG);
            return;
        }
        int serviceId = mImsService.mapPhoneIdToServiceId(mPhoneId);
        mOpImsCallSession.broadcastForNotRingingMtIfRequired(
                mSipSessionProgress, mState, serviceId, mCallNumber, mContext);
        mIsOnTerminated = true;
        mState = ImsCallSession.State.TERMINATED;
        if (mHasPendingMo) {
            mHasPendingMo = false;
            mCallErrorState = CallErrorState.DIAL;
        } else {
            mCallErrorState = CallErrorState.DISCONNECT;
        }
        if (mImsReasonInfo != null) {
            logWithCallId("callTerminated() : get disconnect cause directly from +ESIPCPI", ImsCallLogLevel.DEBUG);
            notifyCallSessionTerminated(mImsReasonInfo);
        } else {
            logWithCallId("callTerminated() : get disconnect cause from AT+CEER", ImsCallLogLevel.DEBUG);
            Message result = mHandler.obtainMessage(EVENT_GET_LAST_CALL_FAIL_CAUSE);
            mImsRILAdapter.getLastCallFailCause(mPhoneId, result);
        }

        // Update WiFiOffloadService call status for call terminated case.
        updateCallStateForWifiOffload(mState);
    }

    private class MyHandler extends Handler {

        private static final String PAU_NUMBER_FIELD = "<tel:";
        private static final String PAU_NAME_FIELD = "<name:";
        private static final String PAU_SIP_NUMBER_FIELD = "<sip:";
        private static final String PAU_END_FLAG_FIELD = ">";
        private static final String PAU_VERSTAT_FIELD = "verstat=";

        public MyHandler(Looper looper) {
            super(looper, null, true);
        }

        /// M: ALPS02492264, parse display name from pau @{
        private String getDisplayNameFromPau(String pau) {
            pau = pau.replace("\\\\", "\\");
            String newPau = pau.replaceAll(TAG_DOUBLE_QUOTE, "\"");
            String value = "";
            if (TextUtils.isEmpty(newPau)) {
                logWithCallId("getDisplayNameFromPau() : pau is null !", ImsCallLogLevel.DEBUG);
                return value;
            }
            for (int index = 0; index < newPau.length(); index++) {
                char aChar = newPau.charAt(index);
                if (aChar == '<') {
                    break;
                }
                value += aChar;
            }
            return value;
        }
        /// @}

        private String getFieldValueFromPau(String pau, String field) {
            String value = "";
            if (TextUtils.isEmpty(pau) || TextUtils.isEmpty(field)) {
                return value;
            }

            if (!pau.contains(field)) {
                return value;
            }

            int startIndex = pau.indexOf(field);
            startIndex += field.length();
            int endIndex = pau.indexOf(PAU_END_FLAG_FIELD, startIndex);
            value = pau.substring(startIndex, endIndex);
            return value;
        }

        private int imsReasonInfoCodeFromRilReasonCode(int causeCode) {
            logWithCallId("imsReasonInfoCodeFromRilReasonCode() : causeCode = " + causeCode, ImsCallLogLevel.DEBUG);

            switch (causeCode) {
                case CallFailCause.USER_BUSY:
                    return ImsReasonInfo.CODE_SIP_BUSY;

                case CallFailCause.TEMPORARY_FAILURE:
                case CallFailCause.CHANNEL_NOT_AVAIL:
                    return ImsReasonInfo.CODE_SIP_TEMPRARILY_UNAVAILABLE;

                case CallFailCause.QOS_NOT_AVAIL:
                    return ImsReasonInfo.CODE_SIP_NOT_ACCEPTABLE;

                case CallFailCause.NO_CIRCUIT_AVAIL:
                case MtkCallFailCause.FACILITY_NOT_IMPLEMENT:
                case MtkCallFailCause.PROTOCOL_ERROR_UNSPECIFIED:
                    return ImsReasonInfo.CODE_SIP_SERVER_INTERNAL_ERROR;

                case CallFailCause.ACM_LIMIT_EXCEEDED:
                    return ImsReasonInfo.CODE_LOCAL_CALL_EXCEEDED;

                case CallFailCause.CALL_BARRED:
                    return ImsReasonInfo.CODE_LOCAL_ILLEGAL_STATE;
                case CallFailCause.FDN_BLOCKED:
                    return ImsReasonInfo.CODE_FDN_BLOCKED;

                case CallFailCause.BEARER_NOT_AVAIL:
                    if (mWfoService != null && mRatType == WifiOffloadManager.RAN_TYPE_WIFI) {
                        try {
                            if (!mWfoService.isWifiConnected()) {
                                logWithCallId("imsReasonInfoCodeFromRilReasonCode() : Rat is Wifi, Wifi is disconnected, ret=SIGNAL_LOST", ImsCallLogLevel.DEBUG);
                                return MtkImsReasonInfo.CODE_SIP_WIFI_SIGNAL_LOST;
                            }
                        } catch (RemoteException e) {
                            logWithCallId("imsReasonInfoCodeFromRilReasonCode() : RemoteException in isWifiConnected()", ImsCallLogLevel.ERROR);
                        }
                    }

                    // return CODE_SIP_SERVER_ERROR by default.
                    return ImsReasonInfo.CODE_SIP_SERVER_ERROR;
                case MtkCallFailCause.INTERWORKING_UNSPECIFIED:
                /* sip 510 not implemented */
                case MtkCallFailCause.FACILITY_REJECTED:
                /* sip 502 bad gateway */
                case MtkCallFailCause.ACCESS_INFORMATION_DISCARDED:
                    return ImsReasonInfo.CODE_SIP_SERVER_ERROR;

                case MtkCallFailCause.NO_USER_RESPONDING:
                    return ImsReasonInfo.CODE_TIMEOUT_NO_ANSWER;

                case MtkCallFailCause.USER_ALERTING_NO_ANSWER:
                    return ImsReasonInfo.CODE_USER_NOANSWER;

                case MtkCallFailCause.CALL_REJECTED:
                    return ImsReasonInfo.CODE_SIP_USER_REJECTED;

                case CallFailCause.NORMAL_UNSPECIFIED:
                    return ImsReasonInfo.CODE_USER_TERMINATED_BY_REMOTE;

                case CallFailCause.UNOBTAINABLE_NUMBER:
                case MtkCallFailCause.INVALID_NUMBER_FORMAT:
                    return ImsReasonInfo.CODE_SIP_BAD_ADDRESS;

                case MtkCallFailCause.RESOURCE_UNAVAILABLE:
                case CallFailCause.SWITCHING_CONGESTION:
                case MtkCallFailCause.SERVICE_NOT_AVAILABLE:
                case MtkCallFailCause.NETWORK_OUT_OF_ORDER:
                case MtkCallFailCause.INCOMPATIBLE_DESTINATION:
                    return ImsReasonInfo.CODE_SIP_SERVICE_UNAVAILABLE;

                case MtkCallFailCause.BEARER_NOT_AUTHORIZED:
                case MtkCallFailCause.INCOMING_CALL_BARRED_WITHIN_CUG:
                    return ImsReasonInfo.CODE_SIP_FORBIDDEN;

                case MtkCallFailCause.CHANNEL_UNACCEPTABLE:
                case MtkCallFailCause.BEARER_NOT_IMPLEMENT:
                    return ImsReasonInfo.CODE_SIP_NOT_ACCEPTABLE;

                case MtkCallFailCause.NO_ROUTE_TO_DESTINATION:
                    return ImsReasonInfo.CODE_SIP_NOT_FOUND;

                case CallFailCause.OPERATOR_DETERMINED_BARRING:
                    return ImsReasonInfo.CODE_SIP_REQUEST_CANCELLED;

                case MtkCallFailCause.RECOVERY_ON_TIMER_EXPIRY:
                    return ImsReasonInfo.CODE_SIP_REQUEST_TIMEOUT;

                /* SIP 481: call/transaction doesn't exist */
                case MtkCallFailCause.INVALID_TRANSACTION_ID_VALUE:
                    return ImsReasonInfo.CODE_SIP_CLIENT_ERROR;

                /* [VoLTE]Normal call failed, need to dial as ECC */
                case MtkCallFailCause.IMS_EMERGENCY_REREG:
                    return MtkImsReasonInfo.CODE_SIP_REDIRECTED_EMERGENCY;

                case CallFailCause.ERROR_UNSPECIFIED:
                case CallFailCause.NORMAL_CLEARING:
                default:
                    // WFC: Because +CEER doesn't carry fail cause for WifiCalling, we need to get
                    // fail cause from WifiOffloadService
                    int wfcReason = getWfcDisconnectCause(causeCode);
                    if (wfcReason != WFC_GET_CAUSE_FAILED) {
                        return wfcReason;
                    }

                    int serviceState = mImsService.getImsServiceState(mPhoneId);

                    logWithCallId("imsReasonInfoCodeFromRilReasonCode() : serviceState = " + serviceState, ImsCallLogLevel.DEBUG);

                    if (serviceState == ServiceState.STATE_POWER_OFF) {
                        return ImsReasonInfo.CODE_LOCAL_POWER_OFF;
                    } else if (serviceState == ServiceState.STATE_OUT_OF_SERVICE
                            && !PhoneNumberUtils.isEmergencyNumber(mCallNumber)) {
                        return ImsReasonInfo.CODE_LOCAL_IMS_SERVICE_DOWN;
                    } else if (causeCode == CallFailCause.NORMAL_CLEARING) {
                        return ImsReasonInfo.CODE_USER_TERMINATED_BY_REMOTE;
                    } else {
                        // If nothing else matches, report unknown call drop reason
                        // to app, not NORMAL call end.
                        return ImsReasonInfo.CODE_UNSPECIFIED;
                    }
                }
        }

        private void updateImsReasonInfo(AsyncResult ar) {
            /* ESIPCPI: <call_id>,<dir>,<SIP_msg_type>,<method>,<response_code>[,<reason_text>] */
            String[] sipMessage = (String[]) ar.result;
            if (sipMessage == null || sipMessage[3] == null || sipMessage[4] == null
                    || mCallId == null || sipMessage[0].equals(mCallId) == false) {
                return;
            }
            logWithCallId("updateImsReasonInfo() : receive sip method = " + sipMessage[3]
                    + " cause = " + sipMessage[4] + " reason header = " + sipMessage[5], ImsCallLogLevel.DEBUG);

            SipMessage msg = new SipMessage(sipMessage);

            updateRestrictHDForRemoteCallProfile(msg.getCode(), msg.getReasonHeader());

            /// M: Get reason info by operator. @{
            ImsReasonInfo opReasonInfo = mOpImsCallSession.getImsReasonInfo(msg);
            if (opReasonInfo != null) {
                mImsReasonInfo = opReasonInfo;
                return;
            }
            /// @}

            switch (msg.getCode()) {
                case 403:
                    /// M: ALPS02501206. For OP07 requirement. @{
                    if (msg.getMethod() == SipMessage.METHOD_INVITE
                            && msg.getReasonHeader() != null && msg.getReasonHeader().length() !=
                            0) {
                        if (msg.getReasonHeader().equals(SipMessage.CALL_MAXIMUM_ALREADY_REACHED)) {
                            mImsReasonInfo = new ImsReasonInfo(
                                    ImsReasonInfo.CODE_MAXIMUM_NUMBER_OF_CALLS_REACHED,
                                    0, msg.getReasonHeader());
                        } else if (OperatorUtils.isMatched(OperatorUtils.OPID.OP07, mPhoneId)) {
                            mImsReasonInfo = new ImsReasonInfo(
                                    MtkImsReasonInfo
                                            .CODE_SIP_403_WFC_UNAVAILABLE_IN_CURRENT_LOCATION,
                                    0, msg.getReasonHeader());
                        }
                    }
                    /// @}
                    break;
                case 503:
                    /// M: ALPS02501206. For OP07 requirement. @{
                    if (msg.getMethod() == SipMessage.METHOD_INVITE
                            && msg.getReasonHeader() != null && msg.getReasonHeader().length() !=
                            0) {
                        if (OperatorUtils.isMatched(OperatorUtils.OPID.OP07, mPhoneId)) {
                            mImsReasonInfo = new ImsReasonInfo(
                                    MtkImsReasonInfo.CODE_SIP_503_ECC_OVER_WIFI_UNSUPPORTED,
                                    0, msg.getReasonHeader());
                        }
                    }
                    /// @}
                    break;
                case 200:
                    if (msg.getReasonHeader() != null &&
                            msg.getReasonHeader().toLowerCase().equals(SipMessage
                                    .CALL_COMPLETED_ELSEWHERE_HEADER)) {
                        mImsReasonInfo = new ImsReasonInfo(
                                ImsReasonInfo.CODE_ANSWERED_ELSEWHERE,
                                0, msg.getReasonHeader());
                    }
                    break;
                case 603:
                    if (msg.getReasonHeader() != null &&
                            msg.getReasonHeader().toLowerCase().equals(SipMessage
                                    .CALL_DECLINED_HEADER)) {
                        mImsReasonInfo = new ImsReasonInfo(
                                ImsReasonInfo.CODE_REMOTE_CALL_DECLINE,
                                0, msg.getReasonHeader());
                    }
                    break;
                case SipMessage.CODE_FOR_REQUEST:
                    if ((msg.getMethod() == SipMessage.METHOD_CANCEL && msg.getReasonHeader() !=
                            null
                            && isRemoteCallDecline(msg.getReasonHeader())) ||
                            mDigitsUtil.isRejectedByOthers(msg)) {
                        mImsReasonInfo = new ImsReasonInfo(
                                ImsReasonInfo.CODE_REMOTE_CALL_DECLINE,
                                0, msg.getReasonHeader());
                    } else if (msg.getMethod() == SipMessage.METHOD_CANCEL && msg.getReasonHeader
                            () != null
                            && isAnsweredElsewhere(msg.getReasonHeader())) {
                        mImsReasonInfo = new ImsReasonInfo(
                                ImsReasonInfo.CODE_ANSWERED_ELSEWHERE,
                                0, msg.getReasonHeader());
                    } else if (msg.getMethod() == SipMessage.METHOD_BYE && msg.getReasonHeader()
                            != null
                            && msg.getReasonHeader().equals(SipMessage.PULLED_AWAY_HEADER)) {
                        mImsReasonInfo = new ImsReasonInfo(
                                ImsReasonInfo.CODE_CALL_END_CAUSE_CALL_PULL,
                                0, msg.getReasonHeader());
                    } else if (msg.getDir() == SipMessage.DIR_SEND && msg.getType() == SipMessage
                            .TYPE_REQUEST
                            && msg.getMethod() == SipMessage.METHOD_INVITE && msg.getReasonHeader
                            () != null
                            && (msg.getReasonHeader().equals(SipMessage
                            .VIDEO_CALL_NOT_AVAILABLE_HEADER)
                            || msg.getReasonHeader().equals(SipMessage
                            .VIDEO_CALL_UNAVAILABLE_HEADER))) {
                        Rlog.d(LOG_TAG, msg.getReasonHeader());
                        logWithCallId("updateImsReasonInfo() : " + msg.getReasonHeader(), ImsCallLogLevel.DEBUG);
                        mImsReasonInfo = new ImsReasonInfo(
                                MtkImsReasonInfo.CODE_SIP_NOT_ACCEPTABLE_HERE,
                                0, msg.getReasonHeader());
                    } else if (msg.getMethod() == SipMessage.METHOD_BYE && msg.getDir() ==
                            SipMessage.DIR_SEND
                            && msg.getReasonHeader() != null
                            && msg.getReasonHeader().toLowerCase().equals(SipMessage
                            .NO_RTP_TIMEOUT_HEADER)) {
                        if (shouldNotifyCallDropByBadWifiQuality()) {
                            if (getWifiRssi() < mBadRssiThreshould) {
                                mImsReasonInfo = new ImsReasonInfo(
                                        MtkImsReasonInfo.CODE_WFC_BAD_RSSI,
                                        0, msg.getReasonHeader());
                            } else {
                                mImsReasonInfo = new ImsReasonInfo(
                                        MtkImsReasonInfo.CODE_WFC_WIFI_BACKHAUL_CONGESTION,
                                        0, msg.getReasonHeader());
                            }
                        }
                    }
                    break;
                case SipMessage.CODE_SESSION_PROGRESS:
                    if (msg.getDir() == SipMessage.DIR_SEND && msg.getType() == SipMessage
                            .TYPE_RESPONSE) {
                        mSipSessionProgress = true;
                    }
                    break;
                default:
                    break;
            }
        }

        private void updateRestrictHDForRemoteCallProfile(int sipCode, String reasonHeader) {
            if (!OperatorUtils.isMatched(OperatorUtils.OPID.OP12, mPhoneId)) {
                return;
            }

            Rlog.d(LOG_TAG, "updateRestrictHDForRemoteCallProfile: check for op12");
            if (sipCode != 180 && sipCode != 200) {
                return;
            }
            if (reasonHeader != null && reasonHeader.toLowerCase().equals("<call_w_mmtel_icsi_tag>")) {
                Rlog.d(LOG_TAG, "updateRestrictHDForRemoteCallProfile");
                mRemoteCallProfile.mRestrictCause = ImsCallProfile.CALL_RESTRICT_CAUSE_NONE;
                notifyCallSessionUpdated();
            }
        }

        private int getVideoDirectionFromVideoState(int videoState) {
            switch (videoState) {
                case VIDEO_STATE_PAUSE:
                    return ImsStreamMediaProfile.DIRECTION_INACTIVE;
                case VIDEO_STATE_SEND_ONLY:
                    return ImsStreamMediaProfile.DIRECTION_SEND;
                case VIDEO_STATE_RECV_ONLY:
                    return ImsStreamMediaProfile.DIRECTION_RECEIVE;
                case VIDEO_STATE_SEND_RECV:
                    return ImsStreamMediaProfile.DIRECTION_SEND_RECEIVE;
                default:
                    return ImsStreamMediaProfile.DIRECTION_INVALID;
            }
        }

        private boolean isCallModeUpdated(int callMode, int videoState) {
            logWithCallId("isCallModeUpdated() : callMode:" + callMode + "videoState:" + videoState, ImsCallLogLevel.DEBUG);
            boolean isChanged = false;
            int oldCallMode = mCallProfile.mCallType;

            if (mVideoState != videoState) {
                mVideoState = videoState;
                mCallProfile.mMediaProfile.mVideoDirection = getVideoDirectionFromVideoState(videoState);
                isChanged = true;
            }

            updateCallType(callMode, mVideoState);

            if (mCallProfile.mCallType != oldCallMode) {
                isChanged = true;

                if (mVTProvider != null) {
                   mVTProvider.onUpdateProfile(ImsCallProfile.getVideoStateFromCallType(mCallProfile.mCallType));
                }
            }

            // Currently only RJIL and TMO support video conference.
            // Voice conference will always upgrade failed so take away the upgrade capability.
            if (callMode == IMS_VOICE_CONF || callMode == IMS_VOICE_CONF_PARTS) {
                isChanged |= removeRemoteCallVideoCapability();
            }

            if (callMode == IMS_VOICE_CALL || callMode == IMS_VOICE_CONF ||
                    callMode == IMS_VOICE_CONF_PARTS) {
                if (isChanged && (mVTProvider != null)) {
                    logWithCallId("isCallModeUpdated() : Start setUIMode old: " + oldCallMode, ImsCallLogLevel.DEBUG);
                    mVTProviderUtil.setUIMode(mVTProvider, ImsVTProviderUtil.UI_MODE_RESET);
                    logWithCallId("isCallModeUpdated() : End setUIMode new: " + mCallProfile.mCallType, ImsCallLogLevel.DEBUG);
                }
            }

            return isChanged;
        }

        private void retrieveMergeFail() {

            ImsCallInfo mergeCallInfo = null;
            ImsCallInfo mergedCallInfo = null;
            boolean     isNotifyMergeFail = false;

            ImsConferenceHandler.getInstance().modifyParticipantFailed();

            logWithCallId("retrieveMergeFail()", ImsCallLogLevel.DEBUG);
            if (mMergeCallId != null && !mMergeCallId.equals("")) {
                mergeCallInfo = mImsServiceCT.getCallInfo(mMergeCallId);
            }

            if (mMergedCallId != null && !mMergedCallId.equals("")) {
                mergedCallInfo = mImsServiceCT.getCallInfo(mMergedCallId);
            }

            if (mergeCallInfo != null && mergedCallInfo != null) {

                logWithCallId("retrieveMergeFail() : MergeCallInfo: callId=" + mergeCallInfo.mCallId
                        + " call status=" + mergeCallInfo.mState + " MergedCallInfo: callId=" +
                        mergedCallInfo.mCallId + " call status=" + mergedCallInfo.mState, ImsCallLogLevel.DEBUG);

                if (mergeCallInfo.mState == ImsCallInfo.State.ACTIVE &&
                        mergedCallInfo.mState == ImsCallInfo.State.HOLDING) {
                    //Nothing Change
                    isNotifyMergeFail = true;

                } else if (mergeCallInfo.mState == ImsCallInfo.State.ACTIVE &&
                        mergedCallInfo.mState == ImsCallInfo.State.ACTIVE) {

                    // 2 active call and hold the merged call
                    logWithCallId("retrieveMergeFail() : two active call and hold merged call", ImsCallLogLevel.DEBUG);

                    Message result = mHandler.obtainMessage(EVENT_RETRIEVE_MERGE_FAIL_RESULT);
                    mImsRILAdapter.hold(Integer.parseInt(mMergedCallId), result);

                } else if (mergeCallInfo.mState == ImsCallInfo.State.HOLDING &&
                        mergedCallInfo.mState == ImsCallInfo.State.HOLDING) {

                    // 2 hold call and resume merge call
                    logWithCallId("retrieveMergeFail() : two hold call and resume merge call", ImsCallLogLevel.DEBUG);

                    Message result = mHandler.obtainMessage(EVENT_RETRIEVE_MERGE_FAIL_RESULT);
                    mImsRILAdapter.resume(Integer.parseInt(mMergeCallId), result);
                } else {
                    /*
                     *Sincemerge call is become hold and merged call is become active,
                     *we need to swap two calls
                     */
                    logWithCallId("retrieveMergeFail() : swap two calls", ImsCallLogLevel.DEBUG);
                    Message result = mHandler.obtainMessage(EVENT_RETRIEVE_MERGE_FAIL_RESULT);
                    mImsRILAdapter.swap(result);
                }
            } else if (mergeCallInfo == null || mergedCallInfo == null) {

                //Only one call is exist and maintain the call state to original state
                if (mergeCallInfo != null) {

                    logWithCallId("retrieveMergeFail() : only merge call is left", ImsCallLogLevel.DEBUG);

                    if (mergeCallInfo.mState != ImsCallInfo.State.ACTIVE) {
                        Message result = mHandler.obtainMessage(EVENT_RETRIEVE_MERGE_FAIL_RESULT);
                        mImsRILAdapter.resume(Integer.parseInt(mMergeCallId), result);
                    } else {
                        isNotifyMergeFail = true;
                    }

                } else if (mergedCallInfo != null) {

                    logWithCallId("retrieveMergeFail() : only merged call is left", ImsCallLogLevel.DEBUG);

                    if (mergedCallInfo.mState != ImsCallInfo.State.HOLDING) {
                        Message result = mHandler.obtainMessage(EVENT_RETRIEVE_MERGE_FAIL_RESULT);
                        mImsRILAdapter.hold(Integer.parseInt(mMergedCallId), result);
                    } else {
                        isNotifyMergeFail = true;
                    }
                // There is the case merge call and merged call are disconnected just before
                // merge failed message occur. Should notify merge failed event or else InCallUI
                // will hang for a lot of time.
                } else {
                    isNotifyMergeFail = true;
                }
            }

            if (isNotifyMergeFail) {
                mergeFailed();
            }
        }

        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            int callMode = INVALID_CALL_MODE;

            detailLog("handleMessage() : receive message = " + event2String(msg.what));

            DefaultConferenceHandler confHdler = ImsConferenceHandler.getInstance();
            switch (msg.what) {
                case EVENT_CALL_INFO_INDICATION:
                    /* +ECPI:<call_id>, <msg_type>, <is_ibt>, <is_tch>, <dir>, <call_mode>[, <number>, <toa>], "",<cause>
                     *
                     * if msg_type = DISCONNECT_MSG or ALL_CALLS_DISC_MSG,
                     * +ECPI:<call_id>, <msg_type>, <is_ibt>, <is_tch>,,,"",,"",<cause>
                     *
                     * if others,
                     * +ECPI:<call_id>, <msg_type>, <is_ibt>, <is_tch>, <dir>, <call_mode>[, <number>, <toa>], ""
                     *
                     *      0  O  CSMCC_SETUP_MSG
                     *      1  X  CSMCC_DISCONNECT_MSG
                     *      2  O  CSMCC_ALERT_MSG
                     *      3  X  CSMCC_CALL_PROCESS_MSG
                     *      4  X  CSMCC_SYNC_MSG
                     *      5  X  CSMCC_PROGRESS_MSG
                     *      6  O  CSMCC_CALL_CONNECTED_MSG
                     *   129  X  CSMCC_ALL_CALLS_DISC_MSG
                     *   130  O  CSMCC_MO_CALL_ID_ASSIGN_MSG
                     *   131  O  CSMCC_STATE_CHANGE_HELD
                     *   132  O  CSMCC_STATE_CHANGE_ACTIVE
                     *   133  O  CSMCC_STATE_CHANGE_DISCONNECTED
                     *   134  X  CSMCC_STATE_CHANGE_MO_DISCONNECTING
                     *   135  O  CSMCC_STATE_CHANGE_REMOTE_HOLD
                     *   136  O  CSMCC_STATE_CHANGE_REMOTE_RESUME
                     */
                    ar = (AsyncResult) msg.obj;
                    String[] callInfo = (String[]) ar.result;
                    int msgType = 0;
                    boolean isCallProfileUpdated = false;

                    if ((callInfo[1] != null) && (!callInfo[1].equals(""))) {
                        msgType = Integer.parseInt(callInfo[1]);
                    }

                    if ((callInfo[5] != null) && (!callInfo[5].equals(""))) {
                        callMode = Integer.parseInt(callInfo[5]);
                    }

                    // Update call information in ImsServiceCT when receiving self call info event.
                    if (mCallId != null && mCallId.equals(callInfo[0])) {
                        // Keep the call info to ImsServiceCT
                        mImsServiceCT.processCallInfoIndication(callInfo, ImsCallSessionProxy.this, mCallProfile);
                    }

                    boolean isCallDisplayUpdated = false;

                    if (mIsMerging && (!callInfo[0].equals(mCallId))) {
                        switch (msgType) {
                            case 130:
                                logWithCallId("handleMessage() : conference assign call id = " + callInfo[0], ImsCallLogLevel.DEBUG);

                                ImsCallProfile imsCallProfile = new ImsCallProfile();
                                mDigitsUtil.updateCallExtras(imsCallProfile, mCallProfile);
                                // Update call type to conference call profile.
                                if ((callInfo[5] != null) && (!callInfo[5].equals(""))) {
                                    callMode = Integer.parseInt(callInfo[5]);
                                }

                               if (callMode == IMS_VIDEO_CALL || callMode == IMS_VIDEO_CONF ||
                                        callMode == IMS_VIDEO_CONF_PARTS) {
                                    imsCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT;
                                } else {
                                    imsCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VOICE;
                                }
                                // For VzW MDMI
                                sendCallEventWithRat(msgType);

                                if ((callInfo[6] != null) && (!callInfo[6].equals(""))) {
                                    // setup number as address for new conference
                                    imsCallProfile.setCallExtra(ImsCallProfile.EXTRA_OI,
                                        callInfo[6]);
                                    /*
                                    *we assume the remote uri information is same as telephone
                                    * number and update the remote ui information after getting pau.
                                    */
                                    imsCallProfile.setCallExtra(ImsCallProfile.EXTRA_REMOTE_URI,
                                            callInfo[6]);
                                    imsCallProfile.setCallExtraInt(ImsCallProfile.EXTRA_OIR,
                                            ImsCallProfile.OIR_PRESENTATION_NOT_RESTRICTED);
                                } else {
                                    imsCallProfile.setCallExtraInt(ImsCallProfile.EXTRA_OIR,
                                            ImsCallProfile.OIR_PRESENTATION_NOT_RESTRICTED);
                                }
                                createConferenceSession(imsCallProfile, callInfo[0]);

                                if (mMtkConfSessionProxy != null) {
                                    mImsServiceCT.processCallInfoIndication(callInfo, mMtkConfSessionProxy.getAospCallSessionProxy(), imsCallProfile);
                                } else if (mConfSessionProxy != null) {
                                    mImsServiceCT.processCallInfoIndication(callInfo, mConfSessionProxy, imsCallProfile);
                                } else {
                                    logWithCallId("handleMessage() : conference not create callSession", ImsCallLogLevel.DEBUG);
                                }

                                break;
                            default:
                                break;
                        }
                    } else if (mCallId != null && mCallId.equals(callInfo[0])) {
                        if (TextUtils.isEmpty(mCallNumber)) {
                            mCallNumber = callInfo[6];
                        }
                        isCallDisplayUpdated =
                            updateCallDisplayFromNumberOrPau(mCallNumber, callInfo[8]);
                        updateOIR(mCallNumber, callInfo[8]);
                        int serviceId = mImsService.mapPhoneIdToServiceId(mPhoneId);
                        switch (msgType) {
                            case 0:
                                mState = ImsCallSession.State.ESTABLISHING;
                                if ((callInfo[5] != null) && (!callInfo[5].equals(""))) {
                                    callMode = Integer.parseInt(callInfo[5]);
                                }

                                updateCallType(callMode, mVideoState);
                                // For VzW MDMI
                                sendCallEventWithRat(msgType);

                                updateMultipartyState(callMode, true);

                                detailLog("handleMessage() : mCallNumber = " + sensitiveEncode(mCallNumber));

                                Intent intent = new Intent(ImsManager.ACTION_IMS_INCOMING_CALL);
                                intent.putExtra(ImsManager.EXTRA_CALL_ID, mCallId);
                                intent.putExtra(MtkImsConstants.EXTRA_DIAL_STRING, callInfo[6]);
                                intent.putExtra(ImsManager.EXTRA_SERVICE_ID, serviceId);


                                //for RTT
                                mOpExt.checkIncomingRttCallType(intent);

                                /* From P, we dinnot send intent in MT case, instead, we callback via AOSP interface */
                                /*
                                mContext.sendBroadcast(intent);
                                */

                                Bundle extras = new Bundle();
                                extras.putString(ImsManager.EXTRA_CALL_ID, mCallId);
                                extras.putString(MtkImsConstants.EXTRA_DIAL_STRING, callInfo[6]);
                                extras.putInt(ImsManager.EXTRA_SERVICE_ID, serviceId);

                                mImsService.notifyIncomingCallSession(mPhoneId, getServiceImpl(), extras);

                                break;
                            case 2: // CSMCC_ALERT_MSG
                                int isIbt = updateIsIbt(callInfo);

                                updateMultipartyState(callMode, false);

                                logWithCallId("handleMessage() : mCallNumber " + sensitiveEncode(mCallNumber) + " isIbt = " + isIbt, ImsCallLogLevel.DEBUG);

                                /// M: video ringtone @{
                                updateVideoRingtone(callMode, isIbt);
                                /// @}

                                // For VzW MDMI
                                sendCallEventWithRat(msgType);

                                if (mState != ImsCallSession.State.TERMINATING) {
                                    if (mListener != null) {
                                        try {
                                            mListener.callSessionProgressing(mCallProfile.mMediaProfile);
                                        } catch (RuntimeException e) {
                                            logWithCallId("RuntimeException callSessionRemoveParticipantsRequestFailed()", ImsCallLogLevel.ERROR);
                                        }
                                    }
                                    mHasPendingMo = false;
                                }
                                break;
                            case 6: //CSMCC_CALL_CONNECTED_MSG
                                mState = ImsCallSession.State.ESTABLISHED;
                                mCallProfile.mMediaProfile.mAudioDirection =
                                        ImsStreamMediaProfile.DIRECTION_SEND_RECEIVE;

                                updateMultipartyState(callMode, false);

                                logWithCallId("handleMessage() : mCallNumber = " + sensitiveEncode(mCallNumber), ImsCallLogLevel.DEBUG);

                                // Case: VoWifi is registered, but the ECC call is on LTE. To show
                                // ECC call icon correctly, update the actual ECC call RAT here.
                                if (mIsEmergencyCall) {
                                    String ratType = SystemProperties.get("ril.ims.ecccallrat");
                                    if (ratType.equals("1")) {
                                        updateRat(WifiOffloadManager.RAN_TYPE_MOBILE_3GPP);
                                    } else if (ratType.equals("2")) {
                                        updateRat(WifiOffloadManager.RAN_TYPE_WIFI);
                                    }
                                }

                                if (mState != ImsCallSession.State.TERMINATING) {
                                    if (mListener != null) {
                                        /* There may not has alerting message while dial
                                         * conference call. We need to reset mHasPendingMO.
                                         */
                                        try {
                                            if (mHasPendingMo) {
                                                mListener.callSessionProgressing(mCallProfile.mMediaProfile);
                                            }
                                            mListener.callSessionInitiated(mCallProfile);
                                        } catch (RuntimeException e) {
                                            logWithCallId("RuntimeException callSessionProgressing()/callSessionInitiated()",
                                                   ImsCallLogLevel.ERROR);
                                        }
                                    }
                                    mHasPendingMo = false;
                                }

                                boolean notifyCallSessionUpdate = false;

                                logWithCallId("handleMessage() : update call type = " + callMode, ImsCallLogLevel.DEBUG);

                                int oldCallType = mCallProfile.mCallType;
                                updateCallType(callMode, mVideoState);

                                // For VzW MDMI
                                sendCallEventWithRat(msgType);

                                if (mCallProfile.mCallType != oldCallType) {
                                    notifyCallSessionUpdate = true;
                                }

                                /// M: video ringtone @{
                                //if (isNeedRemoveVideoRingtone()) {
                                //    notifyCallSessionUpdate = true;
                                //}
                                /// @}
                                if (notifyCallSessionUpdate) {
                                    notifyCallSessionUpdated();
                                }

                                // Update WiFiOffloadService call status for call started case.
                                updateCallStateForWifiOffload(mState);

                                // RTT audio
                                if (mOpExt.isRttEnabledForCallSession()) {
                                    enableRttAudioIndication();
                                }
                                break;
                            case 131: //CSMCC_STATE_CHANGE_HELD

                                logWithCallId("handleMessage() : mCallNumber = " + sensitiveEncode(mCallNumber), ImsCallLogLevel.DEBUG);
                                // For VzW MDMI
                                sendCallEventWithRat(msgType);
                                if (mListener != null && mState != ImsCallSession.State.TERMINATING) {
                                    if (mIsHideHoldEventDuringMerging == false
                                            && mIsHideHoldDuringECT == false
                                            && mOpImsCallSession.isDeviceSwitching() == false) {
                                        try {
                                            mListener.callSessionHeld(mCallProfile);
                                        } catch (RuntimeException e) {
                                            logWithCallId("RuntimeException callSessionHeld()", ImsCallLogLevel.ERROR);
                                        }

                                    } else if (isCallDisplayUpdated) {
                                        notifyCallSessionUpdated();
                                    }
                                }
                                break;
                            case 132: //CSMCC_STATE_CHANGE_ACTIVE
                                // For VzW MDMI
                                sendCallEventWithRat(msgType);

                                if (mListener != null) {
                                    if (mState == ImsCallSession.State.ESTABLISHED) {
                                        logWithCallId("handleMessage() : mCallNumber = " + sensitiveEncode(mCallNumber), ImsCallLogLevel.DEBUG);
                                        try {
                                            mListener.callSessionResumed(mCallProfile);
                                        } catch (RuntimeException e) {
                                            logWithCallId("RuntimeException callSessionResumed()", ImsCallLogLevel.ERROR);
                                        }

                                        mListener.callSessionResumed(mCallProfile);

                                        if (mVTProvider != null) {
                                            mVTProvider.onReceiveCallSessionEvent(CALL_INFO_MSG_TYPE_ACTIVE);
                                        }

                                    } else if (isCallDisplayUpdated) {
                                        notifyCallSessionUpdated();
                                    }
                                }
                                break;
                            case 133: //CSMCC_STATE_CHANGE_DISCONNECTED
                                // For VzW MDMI
                                sendCallEventWithRat(msgType);
                                /// M: VzW requirement: handle SIP 486 Busy. @{
                                boolean hasHoldCall =
                                    (mImsServiceCT.getCallInfo(ImsCallInfo.State.HOLDING) != null);
                                if (mHasPendingMo
                                        && (mOpImsCallSession.handleCallStartFailed(
                                        mMtkImsCallSessionProxy.getServiceImpl(), mImsRILAdapter, hasHoldCall) == true)) {
                                    break;
                                }
                                /// @}
                                callTerminated();
                                break;
                            case 135: //CSMCC_STATE_CHANGE_REMOTE_HOLD
                                // For VzW MDMI
                                sendCallEventWithRat(msgType);
                                updateIsIbt(callInfo);
                                notifyRemoteHeld();
                                if (isCallDisplayUpdated) {
                                    notifyCallSessionUpdated();
                                }
                                break;
                            case 136: //CSMCC_STATE_CHANGE_REMOTE_RESUME
                                // For VzW MDMI
                                sendCallEventWithRat(msgType);
                                updateIsIbt(callInfo);
                                notifyRemoteResumed();
                                if (isCallDisplayUpdated) {
                                    notifyCallSessionUpdated();
                                }
                                break;
                            case 137: //RTT ECC redial notification
                                mOpExt.handleRttECCRedialEvent(mMtkImsCallSessionProxy.getServiceImpl());
                            default:
                                break;
                        }
                    } else if (mCallId == null && msgType == 130) {

                        logWithCallId("handleMessage() : receive 130 URC, assign call_id = " + callInfo[0], ImsCallLogLevel.DEBUG);

                        mImsServiceCT.processCallInfoIndication(callInfo, ImsCallSessionProxy.this, mCallProfile);

                        isCallDisplayUpdated =
                            updateCallDisplayFromNumberOrPau(mCallNumber, callInfo[8]);
                        updateOIR(mCallNumber, callInfo[8]);
                        mState = ImsCallSession.State.ESTABLISHING;
                        mCallId = callInfo[0];
                        // For VzW MDMI
                        sendCallEventWithRat(msgType);
                        if (mVTProvider != null) {
                            mVTProviderUtil
                                    .bind(mVTProvider, Integer.parseInt(mCallId), mPhoneId);
                        }
                        if (mIsOneKeyConf) {
                            ImsConferenceHandler.getInstance().startConference(
                                    mContext, new ConferenceEventListener(), callInfo[0], mPhoneId);
                            ImsConferenceHandler.getInstance().addLocalCache(mOneKeyparticipants);
                            mOneKeyparticipants = null;
                        }
                        // Should update the first OI to fwk
                        // ALPS04418264: EXTRA_CALL_RAT_TYPE has updated to wifi, so need to noitfy,
                        // or UI won't display it's a wifi call.
                        if (isCallDisplayUpdated || mRatType == WifiOffloadManager.RAN_TYPE_WIFI) {
                            notifyCallSessionUpdated();
                        }
                        // Update WiFiOffloadService call status for MO case.
                        updateCallStateForWifiOffload(ImsCallSession.State.ESTABLISHING);
                        if (mHasPendingDisconnect) {
                            mHasPendingDisconnect = false;
                            terminate(mPendingDisconnectReason);
                        }
                    }
                    break;
                case EVENT_ECONF_RESULT_INDICATION:
                    ar = (AsyncResult) msg.obj;
                    handleEconfIndication((String[]) ar.result);
                    break;
                case EVENT_DIAL_RESULT:
                case EVENT_DIAL_CONFERENCE_RESULT:
                case EVENT_PULL_CALL_RESULT:
                case EVENT_DIAL_FROM_RESULT:
                    handleDialResult((AsyncResult) msg.obj);
                    break;
                case EVENT_HOLD_RESULT:
                    ar = (AsyncResult) msg.obj;
                    if (DBG) {
                        logEventResult((ar.exception == null),
                                "handleMessage() : receive EVENT_HOLD_RESULT");
                    }
                    if (mListener != null) {
                        if (ar.exception != null) {
                            ImsReasonInfo imsReasonInfo = new ImsReasonInfo();
                            if (ar.exception instanceof CommandException) {
                                Error err = ((CommandException)(ar.exception)).getCommandError();
                                if (err == Error.OEM_ERROR_1) {
                                    imsReasonInfo = new ImsReasonInfo(
                                            ImsReasonInfo.CODE_LOCAL_CALL_TERMINATED, 0);
                                    logWithCallId("Call hold failed by call terminated",
                                            ImsCallLogLevel.ERROR);
                                }
                            }
                            try {
                                mListener.callSessionHoldFailed(imsReasonInfo);
                            } catch (RuntimeException e) {
                                logWithCallId("RuntimeException callSessionHoldFailed()", ImsCallLogLevel.ERROR);
                            }
                        }
                    }
                    break;
                case EVENT_RESUME_RESULT:
                    ar = (AsyncResult) msg.obj;
                    if (DBG) {
                        logEventResult((ar.exception == null), "handleMessage() : receive EVENT_RESUME_RESULT");
                    }
                    if (mListener != null) {
                        if (ar.exception != null) {
                            try {
                                mListener.callSessionResumeFailed(new ImsReasonInfo());
                            } catch (RuntimeException e) {
                                logWithCallId("RuntimeException callSessionResumeFailed()", ImsCallLogLevel.ERROR);
                            }
                        }
                    }
                    break;
                case EVENT_MERGE_RESULT:
                    ar = (AsyncResult) msg.obj;
                    if (DBG) {
                        logEventResult((ar.exception == null), "handleMessage() : receive EVENT_MERGE_RESULT");
                    }
                    if (mListener != null) {
                        if (ar.exception != null) {

                            logWithCallId("handleMessage() : ConfCreated failed", ImsCallLogLevel.DEBUG);

                            retrieveMergeFail();
                        }
                    }
                    break;
                case EVENT_SWAP_BEFORE_MERGE_RESULT:
                    ar = (AsyncResult) msg.obj;
                    if (DBG) {
                        logEventResult((ar.exception == null), "handleMessage() : receive EVENT_SWAP_BEFORE_MERGE_RESULT");
                    }
                    if (ar.exception != null) {
                        retrieveMergeFail();
                    } else {
                        Message result;
                        ImsCallInfo myCallInfo = mImsServiceCT.getCallInfo(mCallId);

                        // get call info of another peer.
                        ImsCallInfo beMergedCallInfo = mImsServiceCT.getCallInfo(mMergedCallId);
                        if (beMergedCallInfo == null) {
                            retrieveMergeFail();
                            break;
                        }
                        /// M: ALPS02475154, invite participants using call id. @{
                        if (myCallInfo.mIsConference) {
                            logWithCallId("handleMessage() : myCallI is conference, merge normal call", ImsCallLogLevel.DEBUG);
                            result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);
                            mImsRILAdapter.inviteParticipant(
                                    Integer.parseInt(mCallId),
                                    beMergedCallInfo.mCallNum,
                                    Integer.parseInt(beMergedCallInfo.mCallId),
                                    result);
                        } else {
                            logWithCallId("handleMessage() : bg conference is foreground now, merge normal call", ImsCallLogLevel.DEBUG);
                            result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);
                            mImsRILAdapter.inviteParticipant(
                                    Integer.parseInt(beMergedCallInfo.mCallId),
                                    myCallInfo.mCallNum,
                                    Integer.parseInt(myCallInfo.mCallId),
                                    result);
                        }
                        /// @}
                    }
                    break;
                case EVENT_RETRIEVE_MERGE_FAIL_RESULT:

                    // Don't care the retrieve result and just notify merge fail to ImsPhone.
                    mergeFailed();
                    break;
                case EVENT_ADD_CONFERENCE_RESULT:
                    ar = (AsyncResult) msg.obj;

                    if (mIsMerging) {
                        if (ar.exception != null) {
                            retrieveMergeFail();
                        }
                    } else {
                        if (mIsOnTerminated == true) {
                            break;
                        }

                        if (ar.exception == null) {
                            mIsAddRemoveParticipantsCommandOK = true;
                        } else {
                            ImsConferenceHandler.getInstance().modifyParticipantFailed();
                        }
                        mPendingParticipantInfoIndex ++;

                        if (mPendingParticipantInfoIndex < mPendingParticipantStatistics) {
                            Message result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);

                            mImsRILAdapter.inviteParticipant(Integer.parseInt(mCallId),
                                    mPendingParticipantInfo[mPendingParticipantInfoIndex], -1, result);

                        } else {
                            if (mListener != null) {
                                try {
                                    if (mIsAddRemoveParticipantsCommandOK == false) {
                                        mListener.callSessionInviteParticipantsRequestFailed(new ImsReasonInfo());
                                    } else {
                                        mListener.callSessionInviteParticipantsRequestDelivered();
                                    }
                                } catch (RuntimeException e) {
                                    logWithCallId("RuntimeException callSessionInviteParticipantsRequest", ImsCallLogLevel.ERROR);
                                }
                            }
                            mIsAddRemoveParticipantsCommandOK = false;
                        }
                    }
                    break;
                case EVENT_REMOVE_CONFERENCE_RESULT:
                    ar = (AsyncResult) msg.obj;
                    if (DBG) {
                        logEventResult((ar.exception == null), "receive EVENT_REMOVE_CONFERENCE_RESULT");
                    }

                    if (mIsOnTerminated == true) {
                        break;
                    }

                    if (ar.exception == null) {
                        mIsAddRemoveParticipantsCommandOK = true;
                        ImsConferenceHandler.getInstance().modifyParticipantComplete();
                    } else {
                        ImsConferenceHandler.getInstance().modifyParticipantFailed();
                    }

                    mPendingParticipantInfoIndex ++;
                    if (mPendingParticipantInfoIndex < mPendingParticipantStatistics) {
                        Message result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);

                        mImsRILAdapter.removeParticipant(Integer.parseInt(mCallId),
                                mPendingParticipantInfo[mPendingParticipantInfoIndex], result);
                    } else {
                        if (mListener != null) {
                            try {
                                if (mIsAddRemoveParticipantsCommandOK == false) {
                                    mListener.callSessionRemoveParticipantsRequestFailed(new ImsReasonInfo());
                                } else {
                                    mListener.callSessionRemoveParticipantsRequestDelivered();
                                }
                            } catch (RuntimeException e) {
                                logWithCallId("RuntimeException callSessionRemoveParticipantsRequest", ImsCallLogLevel.ERROR);
                            }
                        }
                        mIsAddRemoveParticipantsCommandOK = false;
                    }
                    break;
                case EVENT_GET_LAST_CALL_FAIL_CAUSE:
                    ar = (AsyncResult) msg.obj;
                    ImsReasonInfo imsReasonInfo;
                    int sipCauseCode = ImsReasonInfo.CODE_UNSPECIFIED;

                    if (ar.exception != null) {
                        imsReasonInfo = new ImsReasonInfo();
                    /// M: ALPS02502328, use the illegal state as terminate reason. @{
                    } else if (mTerminateReason == ImsReasonInfo.CODE_LOCAL_ILLEGAL_STATE) {
                        logWithCallId("handleMessage() : Terminate conference due to mMergeHost is null", ImsCallLogLevel.DEBUG);
                        imsReasonInfo = new ImsReasonInfo(mTerminateReason, 0);
                        mTerminateReason = -1;
                    /// @}
                    } else {
                        // Check SIP code/ReasonHeader first in case +ESIPCPI come after ECPI 133.
                        if (mImsReasonInfo == null) {
                            logWithCallId("handleMessage() : get disconnect cause from +CEER", ImsCallLogLevel.DEBUG);

                            LastCallFailCause failCause = (LastCallFailCause) ar.result;
                            sipCauseCode = imsReasonInfoCodeFromRilReasonCode(failCause.causeCode);
                            /// M: Release 12, ECC redial with NW assigned category @{
                            if (sipCauseCode == MtkImsReasonInfo.CODE_SIP_REDIRECTED_EMERGENCY) {
                                try {
                                    int cat = Integer.valueOf(failCause.vendorCause);
                                    getImsExtCallUtil().setSpecificEccCategory(cat);
                                } catch (NumberFormatException e) {
                                    logWithCallId("handleMessage() : ECC redirect report is not a number: " +
                                            failCause.vendorCause, ImsCallLogLevel.ERROR);
                                }
                            }
                            /// @}
                            imsReasonInfo = new ImsReasonInfo(sipCauseCode, 0);
                        } else {
                            logWithCallId("handleMessage() : get disconnect cause directly from +ESIPCPI", ImsCallLogLevel.DEBUG);
                            imsReasonInfo = mImsReasonInfo;
                        }
                    }

                    notifyCallSessionTerminated(imsReasonInfo);
                    break;
                case EVENT_SIP_CODE_INDICATION:
                    updateImsReasonInfo((AsyncResult) msg.obj);
                    break;
                case EVENT_CALL_MODE_CHANGE_INDICATION:
                    ar = (AsyncResult) msg.obj;
                    String[] callModeInfo = (String[]) ar.result;

                    /* +EIMSCMODE: <call id>,<call mode>,<video state>,<audio direction>,<PAU>  */

                    //update call mode infor to imsServiceCT.
                    mImsServiceCT.processCallModeChangeIndication(callModeInfo);

                    if (callModeInfo != null && callModeInfo[0].equals(mCallId)) {
                        // assum video state: send_recv is default value
                        int videoState = VIDEO_STATE_SEND_RECV;
                        if ((callModeInfo[1] != null) && (!callModeInfo[1].equals(""))) {
                            callMode = Integer.parseInt(callModeInfo[1]);
                        }
                        if ((callModeInfo[2] != null) && (!callModeInfo[2].equals(""))) {
                            videoState = Integer.parseInt(callModeInfo[2]);
                        }

                        if (DBG) {
                            logWithCallId("handleMessage() : mode = " + callMode + ", video state = " + videoState, ImsCallLogLevel.DEBUG);
                        }

                        boolean isCallModeChanged = isCallModeUpdated(callMode, videoState);
                        isCallDisplayUpdated  = ((callModeInfo.length >= 5) &&
                            handlePauUpdate(callModeInfo[4]));

                        boolean shouldUpdateExtras = (callMode == IMS_VIDEO_CONF_PARTS);
                        if (shouldUpdateExtras) {
                            mCallProfile.setCallExtra(VT_PROVIDER_ID, mCallId);
                            logWithCallId("handleMessage() : setCallIDAsExtras " + VT_PROVIDER_ID +" = " + mCallId, ImsCallLogLevel.ERROR);
                        }

                        if (isCallModeChanged || isCallDisplayUpdated || shouldUpdateExtras) {
                            notifyCallSessionUpdated();
                            if (isCallModeChanged) {
                                // Update WiFiOffloadService call status for call mode change case.
                                updateCallStateForWifiOffload(mState);
                            }
                        }
                        notifyMultipartyStateChanged(callMode);
                    }
                    break;
                case EVENT_VIDEO_CAPABILITY_INDICATION:
                    ar = (AsyncResult) msg.obj;
                    String[] videoCapabilityInfo = (String[]) ar.result;
                    //+EIMSVCAP: <call ID>, <local video capability>, <remote video capability>
                    int  lVideoCapability = 0;
                    int  rVideoCapability = 0;
                    if (videoCapabilityInfo != null &&
                            videoCapabilityInfo[0].equals(mCallId)) {
                        if ((videoCapabilityInfo[1] != null) &&
                                (!videoCapabilityInfo[1].equals(""))) {
                            lVideoCapability = Integer.parseInt(videoCapabilityInfo[1]);
                            if (lVideoCapability == 1) {
                                mLocalCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT;
                            } else {
                                mLocalCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VOICE;
                            }
                        }

                        if ((videoCapabilityInfo[2] != null) &&
                                (!videoCapabilityInfo[2].equals(""))) {
                            rVideoCapability = Integer.parseInt(videoCapabilityInfo[2]);
                            if (rVideoCapability == 1) {
                                mRemoteCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT;
                            } else {
                                mRemoteCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VOICE;
                            }
                        }

                        correctRemoteVideoCapabilityForVideoConference();

                        // Voice conference will always upgrade failed
                        // so take away the upgrade capability
                        if (isMultiparty() && !isVideoCall(mCallProfile)) {
                            removeRemoteCallVideoCapability();
                        }

                        if (DBG) {
                            logWithCallId("handleMessage() : local video capability = " + lVideoCapability + ", remote video capability = " + rVideoCapability, ImsCallLogLevel.DEBUG);
                        }

                        notifyCallSessionUpdated();
                    }
                    break;
                case EVENT_DTMF_DONE:
                    // Send message to original target handler.
                    if (mDtmfTarget != null && mDtmfMsg != null) {
                        try {
                            mDtmfTarget.send(mDtmfMsg);
                        } catch (RemoteException e) {
                            logWithCallId("handleMessage() : RemoteException for DTMF", ImsCallLogLevel.ERROR);
                        }
                    }
                    mDtmfTarget = null;
                    mDtmfMsg = null;
                    break;
                /// M: Messages for USSI @{
                case EVENT_SEND_USSI_COMPLETE:
                    ar = (AsyncResult) msg.obj;
                    if (mListener != null) {
                        if (msg.arg1 == USSI_REQUEST) {
                            if (ar != null && ar.exception != null) {
                                mListener.callSessionInitiatedFailed(new ImsReasonInfo());
                            } else {
                                mListener.callSessionInitiated(mCallProfile);
                            }
                        } else {
                            if (ar != null && ar.exception != null) {
                                mListener.callSessionTerminated(new ImsReasonInfo());
                            }
                        }
                    }
                    break;
                case EVENT_CANCEL_USSI_COMPLETE:
                    if (mListener != null) {
                        mListener.callSessionTerminated(new ImsReasonInfo());
                    }
                    break;
                case EVENT_ON_USSI:
                    ar = (AsyncResult) msg.obj;
                    /*
                     * USSI response from the network, or network initiated operation.
                       +EIUSD: <class>,<status>,<str>,<lang>,<error_code>,<alertingpattern>,<sip_cause>
                         <class>:
                         1   USSD notify
                         2   SS notify
                         3   MD execute result
                         <status>:  if class=1
                         0   no further user action required
                         1   further user action required
                         2   USSD terminated by network
                         3   other local client has responded
                         4   operation not supported
                         5   network time out
                         <status>:if class=3, value is return value of MD
                         0   execute success
                         1   common error
                         2   IMS unregistered
                         3   IMS busy
                         4   NW error response
                         5   session not exist
                         6   NW not support(404)
                    */
                    String[] eiusd = (String[]) ar.result;
                    if (DBG) {
                        logWithCallId("handleMessage() : receive EVENT_ON_USSI: " + eiusd[0] + "," + eiusd[1]
                            + "," + eiusd[2] + "," + eiusd[3] + "," + eiusd[4] + "," + eiusd[5]
                            + "," + eiusd[6] + "," + eiusd[7], ImsCallLogLevel.DEBUG);
                    }
                    int ussiClass = Integer.parseInt(eiusd[0]);
                    int ussiStatus = Integer.parseInt(eiusd[1]);
                    int phoneId = Integer.parseInt(eiusd[7]);
                    int mode = -1;
                    if (phoneId != mPhoneId) {
                        break;
                    }
                    if (ussiClass == 1) {
                        if (ussiStatus == 0 || ussiStatus == 2) {
                            mode = ImsCall.USSD_MODE_NOTIFY;
                        } else if (ussiStatus == 1) {
                            mode = ImsCall.USSD_MODE_REQUEST;
                        } else if (ussiStatus == 4) {
                            /**
                            * If AP receives <error_code> != 0 from modem when <m> = 1 & <n> = 4,
                            * no need to CSFB
                            *
                            * From TS 24.390:
                            * <error-code> is an integer. The following values are defined.
                            * If the received value is not listed below, it must be treated as 1.
                            * 1    error - unspecified
                            * 2    language/alphabet not supported
                            * 3    unexpected data value
                            * 4    USSD-busy
                            */
                            if (Integer.parseInt(eiusd[4]) != 0) {
                                Rlog.w(LOG_TAG, "EVENT_ON_USSI, override the mode with -2");
                                mode = -2;  // using -2 to tell MtkImsPhone not to CSFB
                            }
                        }
                    } else if (ussiClass == 3) {
                        if (ussiStatus == 0) {
                            mode = ImsCall.USSD_MODE_NOTIFY;
                        }
                    }
                    if (mListener != null) {
                        mListener.callSessionUssdMessageReceived(mode, eiusd[2]);
                    }
                    break;
                /// @}
                case EVENT_ECT_RESULT:
                    handleEctResult((AsyncResult) msg.obj);
                    break;
                case EVENT_DEVICE_SWITCH_REPONSE:
                    mOpImsCallSession.handleDeviceSwitchResponse(
                            mMtkImsCallSessionProxy.getServiceImpl(), (AsyncResult)msg.obj);
                    break;
                case EVENT_ECT_RESULT_INDICATION:
                    /// M: TMO Device Switch: modem use the same URC AT+ECT to notify ImsService
                    /// that the device switch success or failed as explicit call transfer.
                    /// Hence ImsCallSessionProxy pass the ECT_RESULT_INDICATION to OpImsCallSession
                    /// to check if the indication should be handled as the result of device switch.
                    /// Default implementation return false. @{
                    if (mOpImsCallSession.handleDeviceSwitchResult(
                            mCallId, mMtkImsCallSessionProxy.getServiceImpl(), (AsyncResult)msg.obj)) {
                        break;
                    }
                    /// @}
                    handleEctIndication((AsyncResult) msg.obj);
                    break;
                case EVENT_RADIO_NOT_AVAILABLE:
                    mRadioUnavailable = true;
                    if (mIsOnTerminated) {
                        logWithCallId("handleMessage() : EVENT_RADIO_NOT_AVAILABLE, ignore", ImsCallLogLevel.DEBUG);
                        break;
                    }
                    if (mHasPendingMo) {
                        mHasPendingMo = false;
                        mCallErrorState = CallErrorState.DIAL;
                    } else {
                        mCallErrorState = CallErrorState.DISCONNECT;
                    }
                    notifyCallSessionTerminated(
                            new ImsReasonInfo(ImsReasonInfo.CODE_LOCAL_IMS_SERVICE_DOWN, 0));
                    break;
                case EVENT_IMS_CONFERENCE_INDICATION:
                    handleImsConferenceIndication((AsyncResult) msg.obj);
                    break;
                case EVENT_GTT_CAPABILITY_INDICATION:
                    mOpExt.handleGttCapabilityIndication((AsyncResult) msg.obj,
                        mMtkImsCallSessionProxy.getServiceImpl(), mCallId, mState);

                    boolean isRttEnabledForCallSession = mOpExt.isRttEnabledForCallSession();
                    int status = ImsStreamMediaProfile.RTT_MODE_DISABLED;
                    if (isRttEnabledForCallSession) {
                        status = ImsStreamMediaProfile.RTT_MODE_FULL;
                    }
                    mCallProfile.mMediaProfile.setRttMode(status);
                    notifyCallSessionUpdated();
                    // RTT audio
                    enableRttAudioIndication();
                    break;
                case EVENT_RTT_TEXT_RECEIVE_INDICATION:
                    mOpExt.handleRttTextReceive((AsyncResult) msg.obj,
                       mCallId , mListener);
                    break;
                case EVENT_RTT_MODIFY_RESPONSE:
                    mOpExt.handleRttModifyResponse((AsyncResult) msg.obj,
                        mCallId, mListener);
                    break;
                case EVENT_RTT_MODIFY_REQUEST_RECEIVE:
                    mOpExt.handleRttModifyRequestReceive((AsyncResult) msg.obj,
                        ImsCallSessionProxy.this.getServiceImpl(), mCallId,
                        mListener, mImsRILAdapter, mWasVideoCallForRtt);
                    break;
                case EVENT_SPEECH_CODEC_INFO:
                    handleSpeechCodecInfo((AsyncResult) msg.obj);
                    break;
                case EVENT_REDIAL_ECC_INDICATION:
                    handleRedialEccIndication((AsyncResult) msg.obj);
                    break;
                case EVENT_AUDIO_INDICATION:
                    handleAudioIndication((AsyncResult)msg.obj);
                    break;
                default:
                    logWithCallId("handleMessage() : unknown messahe, ignore", ImsCallLogLevel.DEBUG);
                    break;
            }
        }

        private void handleEconfIndication(String[] result) {
            // +ECONF:<conf_call_id>,<op>,<num>,<result>,<cause>[,<joined_call_id>]
            if (DBG) {
                logWithCallId("handleEconfIndication() : receive EVENT_ECONF_RESULT_INDICATION mCallId:" + mCallId
                        + ", conf_call_id:" + result[0] + "joined_call_id:" + result[5], ImsCallLogLevel.DEBUG);
            }

            // Prevent timing issue in ImsCall.processMergeComplete(), it will check if the
            // session is still alive, by marking this session "terminating"
            // TODO: check which parameter means original call id
            if (mCallId != null && mCallId.equals(result[5]) && result[3].equals("0")) {
                mState = ImsCallSession.State.TERMINATING;
            }

            if (mIsMerging != true) {
                return;
            }

            if (mNormalCallsMerge) {
                // normal call merge normal call
                mEconfCount++;
                if (result[3].equals("0")) {
                    mThreeWayMergeSucceeded = true;
                }
                if (mEconfCount == 2) {
                    String ret = mThreeWayMergeSucceeded ? " successful" : " failed";
                    if (DBG) {
                        logWithCallId("handleEconfIndication() : 3 way conference merge result is" + ret, ImsCallLogLevel.DEBUG);
                    }

                    if (mThreeWayMergeSucceeded &&
                             ImsConferenceHandler.getInstance().isConferenceActive()) {
                        mergeCompleted();
                    } else {
                        retrieveMergeFail();
                        /// ALPS02383993: Terminate the conference if merge failed @{
                        int confCallId = Integer.parseInt(result[0]);
                        mImsRILAdapter.terminate(confCallId);
                        /// @}
                    }
                    //reset mEconfCount in order to merge again,
                    //when one of them failed to merge.
                    mEconfCount = 0;
                    mNormalCallsMerge = false;
                    mThreeWayMergeSucceeded = false;
                }
            } else {

                // conference call merge normal call
                if (result[3].equals("0")) {

                    logWithCallId("handleEconfIndication() : ConfCreated successed", ImsCallLogLevel.DEBUG);
                    mergeCompleted();

                } else {

                    logWithCallId("handleEconfIndication() : ConfCreated failed", ImsCallLogLevel.DEBUG);
                    retrieveMergeFail();
                }
            }
        }

        private void handleEctResult(AsyncResult ar) {
            if (ar == null || mListener == null) {
                if (DBG) {
                    logWithCallId("handleEctResult() : ar or mListener is null", ImsCallLogLevel.DEBUG);
                }
                return;
            }

            if (ar.exception != null) {
                if (DBG) {
                    logWithCallId("handleEctResult() : explicit call transfer failed!!", ImsCallLogLevel.DEBUG);
                }
                mIsHideHoldDuringECT = false;
                if (mMtkImsCallSessionProxy == null) {
                    return;
                }
                mMtkImsCallSessionProxy.notifyTransferFailed(new ImsReasonInfo());
            } else {
                if (DBG) {
                    logWithCallId("handleEctResult() : explicit call transfer succeeded!!", ImsCallLogLevel.DEBUG);
                }
            }
        }

        private void handleEctIndication(AsyncResult ar) {
            // +ECT: <call_id>, <ect_result>, <cause>
            // <ect_result>: 0  Fail
            //               1  Success
            logWithCallId("handleEctIndication()", ImsCallLogLevel.DEBUG);
            mIsHideHoldDuringECT = false;
            if (ar == null || mMtkImsCallSessionProxy == null) {
                if (DBG) {
                    logWithCallId("handleEctIndication() : ar or mMtkImsCallSessionProxy is null", ImsCallLogLevel.DEBUG);
                }
                return;
            }

            int[] result = (int[]) ar.result;
            if (mCallId == null || result[0] != Integer.parseInt(mCallId)) {
                return;
            }

            if (result[1] == 0) {
                mMtkImsCallSessionProxy.notifyTransferFailed(new ImsReasonInfo());
            } else if (result[1] == 1) {
                mMtkImsCallSessionProxy.notifyTransferred();
            }

            // if ECT operation caller is called from Ims Service
            // with explicitCallTransferWithCallback() not call session directly
            // we need to rsp the result via message instead of listener
            if (mDtmfTarget != null && mEctMsg != null) {

                mEctMsg.arg1 = result[1];

                try {
                    mEctTarget.send(mEctMsg);
                } catch (RemoteException e) {
                    logWithCallId("handleMessage() : RemoteException for ECT", ImsCallLogLevel.ERROR);
                }
            }
            mEctTarget = null;
            mEctMsg = null;
        }

        private void handleDialResult(AsyncResult ar) {
            if (ar == null || mListener == null) {
                if (DBG) {
                    logWithCallId("handleDialResult() : ar or mListener is null", ImsCallLogLevel.DEBUG);
                }
                return;
            }

            if (DBG) {
                logEventResult((ar.exception == null), "handleDialResult()");
            }
            if (ar.exception != null) {
                Message result = mHandler.obtainMessage(EVENT_GET_LAST_CALL_FAIL_CAUSE);
                mCallErrorState = CallErrorState.DIAL;
                mImsRILAdapter.getLastCallFailCause(mPhoneId, result);
                mHasPendingMo = false;
            }
        }

        private void handleImsConferenceIndication(AsyncResult ar) {
            if (!mIsConferenceHost) {
                return;
            }
            if (ar == null) {
                if (DBG) {
                    logWithCallId("handleImsConferenceIndication() : ar is null",
                            ImsCallLogLevel.DEBUG);
                }
                return;
            } else if (mListener == null ) {
                if (DBG) {
                    logWithCallId("handleImsConferenceIndication() : mListener is null, cache info",
                            ImsCallLogLevel.DEBUG);
                }
                mCachedUserInfo = ar;
                return;
            }
            ArrayList<User> result = (ArrayList<User>) ar.result;
            // Count active participants number.
            int activeParticipantsNum = 0;
            for (User user : result) {
                if (!isSelfAddress(user.mUserAddr)) {
                    if (!ImsConferenceState.STATUS_DISCONNECTED.equals(user.mStatus)) {
                        activeParticipantsNum++;
                    }
                }
            }
            boolean shouldAutoTerminateConference = shouldAutoTerminateEmptyConf();
            boolean shouldUpdateConference = false;
            logWithCallId("handleImsConferenceIndication()" +
                    " activeParticipantsNum:" + activeParticipantsNum +
                    " mHaveUpdateConferenceWithMember:" + mHaveUpdateConferenceWithMember +
                    " shouldAutoTerminateConference:" + shouldAutoTerminateConference,
                    ImsCallLogLevel.DEBUG);

            if (!shouldAutoTerminateConference) {
                shouldUpdateConference = true;
            } else if (activeParticipantsNum > 0) {
                shouldUpdateConference = true;
                // One key conference report CEP with only self in the beginning,
                // We should auto terminate it only after once other participants
                // existed in the conference.
                if (!mHaveUpdateConferenceWithMember) {
                    mHaveUpdateConferenceWithMember = true;
                }
            } else if (!mHaveUpdateConferenceWithMember) {
                shouldUpdateConference = true;
            }

            if (shouldUpdateConference) {
                ImsConferenceState confState = convertToImsConferenceState(result);
                try {
                    mListener.callSessionConferenceStateUpdated(confState);
                } catch (RuntimeException e) {
                    logWithCallId("RuntimeException callSessionConferenceStateUpdated()", ImsCallLogLevel.ERROR);
                }
            } else {
                // auto terminate
                logWithCallId("handleImsConferenceIndication() : auto terminate",
                        ImsCallLogLevel.DEBUG);
                terminate(ImsReasonInfo.CODE_UNSPECIFIED);
            }
        }

        private ImsConferenceState convertToImsConferenceState(ArrayList<User> users) {
            ImsConferenceState confState = new ImsConferenceState();
            int index = 0;
            for (int i = 0; i < users.size(); i++) {
                Bundle userInfo = new Bundle();
                String userAddr = users.get(i).mUserAddr;
                userInfo.putString(ImsConferenceState.USER, userAddr);
                userInfo.putString(ImsConferenceState.DISPLAY_TEXT, users.get(i).mDisplayText);
                userInfo.putString(ImsConferenceState.ENDPOINT, users.get(i).mEndPoint);
                userInfo.putString(ImsConferenceState.STATUS, users.get(i).mStatus);
                userInfo.putString(USER_ENTITY, users.get(i).mEntity);
                if (userAddr == null || userAddr.trim().isEmpty()) {
                    confState.mParticipants.put(Integer.toString(index), userInfo);
                    index++;
                } else {
                    confState.mParticipants.put(userAddr, userInfo);
                }
            }
            mParticipants = confState.mParticipants;
            return confState;
        }

        // Always return true currently, no specific request need to return false.
        private boolean shouldAutoTerminateEmptyConf() {
            boolean shouldTerminate = true;
            logWithCallId("shouldTerminate:" + shouldTerminate, ImsCallLogLevel.DEBUG);
            return shouldTerminate;
        }

        private boolean isSelfAddress(String addr) {
            if (mSelfAddressList == null) {
                mSelfAddressList = mImsServiceCT.getSelfAddressList();
            }
            if (mSelfAddressList != null) {
                for (String selfAddress : mSelfAddressList) {
                    logWithCallId("isSelfAddress() selfId: " + sensitiveEncode(selfAddress)
                    + " addr: " + sensitiveEncode(addr), ImsCallLogLevel.DEBUG);
                    if (PhoneNumberUtils.compareLoosely(addr, selfAddress)) {
                        return true;
                    }
                }
            }
            return false;
        }
        private boolean updateMultipartyState(int callMode, boolean isIncoming) {
            boolean isMultipartyMode = (callMode == IMS_VOICE_CONF || callMode == IMS_VIDEO_CONF
                    || callMode == IMS_VOICE_CONF_PARTS || callMode == IMS_VIDEO_CONF_PARTS);

            if (callMode == IMS_VOICE_CONF || callMode == IMS_VIDEO_CONF) {
                mIsConferenceHost = true;
            } else {
                mIsConferenceHost = false;
            }

            if (OperatorUtils.isMatched(OperatorUtils.OPID.OP12, mPhoneId) && mIsConferenceHost) {
                // set default as no HD icon display
                Rlog.d(LOG_TAG, "VzW: set conference no constrain for HD icon");
                mRemoteCallProfile.mRestrictCause = ImsCallProfile.CALL_RESTRICT_CAUSE_NONE;
            }

            boolean mptyState = isMultiparty();
            String mptyExtra = ImsCallProfileEx.EXTRA_MPTY;

            if (isIncoming) {
                mptyState = isIncomingCallMultiparty();
                mptyExtra = ImsCallProfileEx.EXTRA_INCOMING_MPTY;
            }

            if (mptyState == isMultipartyMode) {
                return false;
            }
            mCallProfile.setCallExtraInt(mptyExtra, (isMultipartyMode)? 1: 0);
            return true;
        }

        private void notifyMultipartyStateChanged(int callMode) {
            boolean stateChanged = updateMultipartyState(callMode, false);
            if (stateChanged == false) {
                return;
            }
            if (DBG) {
                logWithCallId("notifyMultipartyStateChanged() : isMultiparty(): " + isMultiparty(), ImsCallLogLevel.DEBUG);
            }

            if (mListener != null) {
                try {
                    mListener.callSessionMultipartyStateChanged(isMultiparty());
                } catch (RuntimeException e) {
                    logWithCallId("RuntimeException callSessionMultipartyStateChanged()",
                            ImsCallLogLevel.ERROR);
                }
            }
        }

        private void updateOIR(String num, String pau) {
            if (isMultiparty()) {
                logWithCallId("updateOIR() : ignore update OIR for mpty call: ", ImsCallLogLevel.DEBUG);
                return;
            }

            int oir = ImsCallProfile.OIR_PRESENTATION_NOT_RESTRICTED;

            if (TextUtils.isEmpty(num) && TextUtils.isEmpty(pau)) {
                oir = ImsCallProfile.OIR_PRESENTATION_RESTRICTED;
            } else if (!TextUtils.isEmpty(pau) && pau.toLowerCase().contains("anonymous")) {
                oir = ImsCallProfile.OIR_PRESENTATION_RESTRICTED;
            }

            mCallProfile.setCallExtraInt(ImsCallProfile.EXTRA_OIR, oir);
        }

        private boolean updateAddressFromPau(String pau) {
            if (!mShouldUpdateAddressByPau) {
                logWithCallId("updateAddressFromPau() : MO call, should not update addr by PAU", ImsCallLogLevel.DEBUG);
                return false;
            }

            boolean changed = false;

            String sipField = getFieldValueFromPau(pau, PAU_SIP_NUMBER_FIELD);
            String telField = getFieldValueFromPau(pau, PAU_NUMBER_FIELD);

            logWithCallId("updateAddressFromPau() : updatePau()... telNumber: "
                    + sensitiveEncode(telField) + " sipNumber: " + sensitiveEncode(sipField), ImsCallLogLevel.DEBUG);

            // use the sip field first, then use the tel field
            String addr = (!TextUtils.isEmpty(sipField) && mShouldUpdateAddressBySipField) ?
                    sipField : telField;
            String[] split = addr.split("[;@,]+");
            addr = split[0];

            String currentOI = mCallProfile.getCallExtra(ImsCallProfile.EXTRA_OI);
            if (!TextUtils.isEmpty(addr)) {
                if (!PhoneNumberUtils.compareLoosely(currentOI, addr)) {
                    changed = true;
                    mCallProfile.setCallExtra(ImsCallProfile.EXTRA_OI, addr);
                    logWithCallId("updateAddressFromPau() : updatePau()... addr: " + sensitiveEncode(addr), ImsCallLogLevel.DEBUG);
                }
            }
            return changed;
        }

        private boolean updateDisplayNameFromPau(String pau) {
            boolean changed = false;
            String displayName = getDisplayNameFromPau(pau);
            String currentDisplayName = mCallProfile.getCallExtra(ImsCallProfile.EXTRA_CNA);
            if (!TextUtils.isEmpty(displayName)) {
                mCallProfile.setCallExtraInt(ImsCallProfile.EXTRA_CNAP,
                        ImsCallProfile.OIR_PRESENTATION_NOT_RESTRICTED);
                if (!PhoneNumberUtils.compareLoosely(currentDisplayName, displayName)) {
                    mCallProfile.setCallExtra(ImsCallProfile.EXTRA_CNA, displayName);
                    mCallProfile.setCallExtraInt(ImsCallProfile.EXTRA_CNAP,
                                            ImsCallProfile.OIR_PRESENTATION_NOT_RESTRICTED);

                    logWithCallId("updateDisplayNameFromPau() : diaplayName: " + displayName, ImsCallLogLevel.DEBUG);

                    changed = true;
                }
            }
            return changed;
        }

        private boolean handlePauUpdate(String pau) {
            if (TextUtils.isEmpty(pau)) {
                return false;
            }

            String sipNumber = getFieldValueFromPau(pau, PAU_SIP_NUMBER_FIELD);
            mCallProfile.setCallExtra(ImsCallProfile.EXTRA_REMOTE_URI, sipNumber);
            boolean ret = updateAddressFromPau(pau);
            ret |= updateDisplayNameFromPau(pau);
            ret |= updateVerstatFromPau(pau);
            return ret;
        }

        private boolean updateVerstatFromPau(String pau) {
            boolean changed = false;
            String verstatField = getFieldValueFromPau(pau, PAU_VERSTAT_FIELD);

            if (!TextUtils.isEmpty(verstatField)) {
                logWithCallId("updateVerstatFromPau() :" + verstatField, ImsCallLogLevel.DEBUG);

                int currentVerstat = mCallProfile.getCallExtraInt(ImsCallProfileEx.EXTRA_VERSTAT);
                String[] split = verstatField.split("[;@,]+");
                String value = split[0];
                if (value.contains("TN-Validation-Passed") && currentVerstat != 1) {
                    mCallProfile.setCallExtraInt(ImsCallProfileEx.EXTRA_VERSTAT, 1);
                    changed = true;
                } else if (value.contains("TN-Validation-Failed") && currentVerstat != 0) {
                    mCallProfile.setCallExtraInt(ImsCallProfileEx.EXTRA_VERSTAT, 0);
                    changed = true;
                }
            }
            return changed;
        }

        private boolean updateCallDisplayFromNumberOrPau(String number, String pau) {
            boolean changed = false;
            // if pau is empty and number is not empty
            if (TextUtils.isEmpty(pau) && !TextUtils.isEmpty(number)) {
                String curOI = mCallProfile.getCallExtra(ImsCallProfile.EXTRA_OI);

                if (!curOI.equals(number)) {

                    logWithCallId("updateCallDisplayFromNumberOrPau() : number: " + sensitiveEncode(number), ImsCallLogLevel.DEBUG);

                    mCallProfile.setCallExtra(ImsCallProfile.EXTRA_OI, number);
                    mCallProfile.setCallExtra(ImsCallProfile.EXTRA_REMOTE_URI, number);
                    changed = true;
                }
                return changed;
            }
            // use pau
            return handlePauUpdate(pau);
        }

        private int getWfcDisconnectCause(int causeCode) {
            logWithCallId("[WFC] getWfcDisconnectCause mRatType = " + mRatType, ImsCallLogLevel.DEBUG);
            if (mWfoService == null || mRatType != WifiOffloadManager.RAN_TYPE_WIFI
                    || causeCode == CallFailCause.NORMAL_CLEARING) {
                return WFC_GET_CAUSE_FAILED;
            }

            DisconnectCause disconnectCause = null;
            try {
                disconnectCause = mWfoService.getDisconnectCause(mPhoneId);
            } catch (RemoteException e) {
                logWithCallId("getWfcDisconnectCause() : RemoteException in getWfcDisconnectCause()", ImsCallLogLevel.ERROR);
            }
            if (disconnectCause == null) {
                return WFC_GET_CAUSE_FAILED;
            }
            int wfcErrorCause = disconnectCause.getErrorCause();
            logWithCallId("[WFC] wfcErrorCause = " + wfcErrorCause, ImsCallLogLevel.DEBUG);
            if (wfcErrorCause == WfcReasonInfo.CODE_WFC_WIFI_SIGNAL_LOST) {
                return MtkImsReasonInfo.CODE_SIP_WIFI_SIGNAL_LOST;
            } else if ((wfcErrorCause == WfcReasonInfo.CODE_WFC_UNABLE_TO_COMPLETE_CALL)
                    || (wfcErrorCause == WfcReasonInfo.CODE_WFC_UNABLE_TO_COMPLETE_CALL_CD)) {
                return MtkImsReasonInfo.CODE_SIP_HANDOVER_WIFI_FAIL;
            } else if (wfcErrorCause ==
                    WfcReasonInfo.CODE_WFC_NO_AVAILABLE_QUALIFIED_MOBILE_NETWORK) {
                return MtkImsReasonInfo.CODE_SIP_HANDOVER_LTE_FAIL;
            } else {
                return WFC_GET_CAUSE_FAILED;
            }
        }
    }

    private void updateVideoRingtone(int ringCallMode, int isIbt) {
        if (SystemProperties.get("persist.vendor.vilte_support").equals("1")) {

            int callType = mCallProfile.mCallType;

            logWithCallId("updateVideoRingtone() : callType = " + callType, ImsCallLogLevel.DEBUG);

            if (callType == ImsCallProfile.CALL_TYPE_VT ||
                    callType == ImsCallProfile.CALL_TYPE_VT_RX) {
                // isIbt is 2 for video ringtone only
                if (ringCallMode == IMS_VIDEO_CALL && isIbt == 2) {
                    mRemoteCallProfile.mMediaProfile.mVideoDirection =
                            ImsStreamMediaProfile.DIRECTION_RECEIVE;
                } else {
                    mRemoteCallProfile.mMediaProfile.mVideoDirection =
                            ImsStreamMediaProfile.DIRECTION_INVALID;
                }
            } else {
                mRemoteCallProfile.mMediaProfile.mVideoDirection =
                        ImsStreamMediaProfile.DIRECTION_INVALID;
            }
        }
    }

    private boolean isNeedRemoveVideoRingtone() {
        boolean result = false;
        if (mRemoteCallProfile.mMediaProfile.mVideoDirection ==
            ImsStreamMediaProfile.DIRECTION_RECEIVE) {
          mRemoteCallProfile.mMediaProfile.mVideoDirection =
            ImsStreamMediaProfile.DIRECTION_INVALID;
          result = true;
          logWithCallId("isNeedRemoveVideoRingtone()", ImsCallLogLevel.DEBUG);
        }
        return result;
    }

    private void mergeCompleted() {
        notifyCallSessionMergeComplete();
        mIsMerging = false;
        mIsHideHoldEventDuringMerging = false;
        ImsConferenceHandler.getInstance().modifyParticipantComplete();

        ImsCallSessionProxy hostCallSession = mImsServiceCT.getConferenceHostCall();
        if(hostCallSession != null) {
            hostCallSession.onAddParticipantComplete();
        }

        //clear mConfSessionProxy
        //When host failed to merge, and merge it again as host.
        //It will change to normal call merge conference call,
        //this should be cleared.
        mConfSessionProxy = null;
        mMtkConfSessionProxy = null;
    }

    private void mergeFailed() {
        if (mListener != null) {
            try {
                mListener.callSessionMergeFailed(new ImsReasonInfo());
            } catch (RuntimeException e) {
                logWithCallId("RuntimeException callSessionMergeFailed()", ImsCallLogLevel.ERROR);
            }
        }
        mMergeCallId = "";
        mMergeCallStatus = ImsCallInfo.State.INVALID;
        mMergedCallId = "";
        mMergedCallStatus = ImsCallInfo.State.INVALID;

        mIsMerging = false;
        mIsHideHoldEventDuringMerging = false;
        closeConferenceSession();
    }

    /**
     * A listener type for receiving notification on WFC handover events.
     */
    private class IWifiOffloadListenerProxy extends WifiOffloadManager.Listener {
        @Override
        public void onHandover(int simIdx, int stage, int ratType) {
            if(simIdx != mPhoneId) {
                return;
            }

            if (ratType == mRatType || stage != WifiOffloadManager.HANDOVER_END) {
                return;
            }

            updateRat(ratType);
            if (mListener != null) {
                logWithCallId("onHandover()", ImsCallLogLevel.DEBUG);
                try {
                    mListener.callSessionUpdated(mCallProfile);
                    mListener.callSessionHandover(mRatType, ratType, new ImsReasonInfo());
                } catch (RuntimeException e) {
                    logWithCallId("RuntimeException callSessionHandover()", ImsCallLogLevel.ERROR);
                }
            }
        }

        @Override
        public void onRequestImsSwitch(int simIdx, boolean isImsOn) {

            // no-op
        }
    }

    private void updateCallStateForWifiOffload(int callState) {
        if (mWfoService == null) {
            logWithCallId("updateCallStateForWifiOffload() : skip, no WOS!", ImsCallLogLevel.DEBUG);
            return;
        }

        if (mCallId == null) {
            logWithCallId("updateCallStateForWifiOffload() : skip, no call ID!", ImsCallLogLevel.DEBUG);
            return;
        }

        int callId = Integer.parseInt(mCallId);
        int callType = WifiOffloadManager.CALL_TYPE_VOICE;

        if (mCallProfile.mCallType == ImsCallProfile.CALL_TYPE_VOICE ||
                mCallProfile.mCallType == ImsCallProfile.CALL_TYPE_VOICE_N_VIDEO) {
            callType = WifiOffloadManager.CALL_TYPE_VOICE;
        } else {
            callType =  WifiOffloadManager.CALL_TYPE_VIDEO;
        }

        int wosCallState;
        switch (callState) {
            case ImsCallSession.State.ESTABLISHING:
            case ImsCallSession.State.INITIATED:
            case ImsCallSession.State.NEGOTIATING:
            case ImsCallSession.State.REESTABLISHING:
            case ImsCallSession.State.RENEGOTIATING:
                wosCallState = WifiOffloadManager.CALL_STATE_ESTABLISHING;
                break;
            case ImsCallSession.State.ESTABLISHED:
                wosCallState = WifiOffloadManager.CALL_STATE_ACTIVE;
                break;
            case ImsCallSession.State.TERMINATED:
            case ImsCallSession.State.TERMINATING:
            case ImsCallSession.State.IDLE:
                wosCallState = WifiOffloadManager.CALL_STATE_END;
                break;
            default:
                logWithCallId("updateCallStateForWifiOffload() : skip, unexpected state: "
                        + callState, ImsCallLogLevel.DEBUG);
                return;
        }

        try {
            mWfoService.updateCallState(mPhoneId, callId, callType, wosCallState);
        } catch (RemoteException e) {
            logWithCallId("updateCallStateForWifiOffload() : RemoteException in Wos.updateCallState()", ImsCallLogLevel.ERROR);
        }
    }

    private void notifyCallSessionTerminated(ImsReasonInfo info) {
        if (mListener == null) {
            logWithCallId("notifyCallSessionTerminated() : mListener = NULL", ImsCallLogLevel.DEBUG);
            close();
            return;
        }

        if (mIsMerging && (mTerminateReason == ImsReasonInfo.CODE_USER_TERMINATED || mRadioUnavailable)) {
            logWithCallId("notifyCallSessionTerminated() : close while merging", ImsCallLogLevel.DEBUG);
            mergeFailed();
        }

        switch (mCallErrorState) {
            case DIAL :
                if (mListener != null) {
                    // ALPS04082860 ImsCallSessionListener will catch RemoteException then throw RuntimeException.
                    try {
                        mListener.callSessionInitiatedFailed(info);
                    } catch (RuntimeException e) {
                        logWithCallId("RuntimeException callSessionInitiatedFailed()", ImsCallLogLevel.ERROR);
                        // RuntimeException happened, no one will close this CallSessionProxy,
                        // close it self.
                        close();
                    }
                }
                break;
            case DISCONNECT :
                if (mListener != null) {
                    // ALPS04024149 ImsCallSessionListener will catch RemoteException then throw RuntimeException.
                    // We catch the RuntimeException here to prevent ImsService restart.
                    try {
                        mListener.callSessionTerminated(info);
                    } catch (RuntimeException e) {
                        logWithCallId("RuntimeException callSessionTerminated()", ImsCallLogLevel.ERROR);
                        // RuntimeException happened, no one will close this CallSessionProxy,
                        // close it self.
                        close();
                    }

                }
                break;
            default:
                break;
        }
    }

    private boolean updateRat(int ratType) {
        String radioTech;
        if (ratType == WifiOffloadManager.RAN_TYPE_MOBILE_3GPP) {
            radioTech = String.valueOf(ServiceState.RIL_RADIO_TECHNOLOGY_LTE);
        } else if (ratType == WifiOffloadManager.RAN_TYPE_WIFI) {
            radioTech = String.valueOf(ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN);
        } else {
            radioTech = String.valueOf(ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN);
        }
        if (mRatType == ratType
                && mCallProfile.getCallExtra(ImsCallProfile.EXTRA_CALL_RAT_TYPE).equals(radioTech)) {
            return false;
        }
        mRatType = ratType;
        mCallProfile.setCallExtra(ImsCallProfile.EXTRA_CALL_RAT_TYPE, radioTech);
        return true;
    }

    private void notifyRemoteHeld() {
        if (mListener != null && mState != ImsCallSession.State.TERMINATING) {
            try {
                mListener.callSessionHoldReceived(mCallProfile);
            } catch (RuntimeException e) {
                logWithCallId("RuntimeException callSessionHoldReceived()", ImsCallLogLevel.ERROR);
            }
        }
    }

    private void notifyRemoteResumed() {
        if (mListener != null && mState != ImsCallSession.State.TERMINATING) {
            try {
                mListener.callSessionResumeReceived(mCallProfile);
            } catch (RuntimeException e) {
                logWithCallId("RuntimeException callSessionResumeReceived()", ImsCallLogLevel.ERROR);
            }
        }
    }

    private void notifyCallSessionUpdated() {
        if (mListener == null) {
            return;
        }
        try {
            mListener.callSessionUpdated(mCallProfile);
        } catch (RuntimeException e) {
            logWithCallId("RuntimeException callSessionUpdated()", ImsCallLogLevel.ERROR);
        }
    }

    ConferenceEventListener getConfEvtListener() {
        if (mConfEvtListener == null) {
            mConfEvtListener = new ConferenceEventListener();
        }
        return mConfEvtListener;
    }

    class ConferenceEventListener extends ImsConferenceHandler.Listener {

        @Override
        public void onParticipantsUpdate(ImsConferenceState confState) {
            logWithCallId("onParticipantsUpdate()", ImsCallLogLevel.DEBUG);
            if (mListener == null) {
                return;
            }
            try {
                mListener.callSessionConferenceStateUpdated(confState);
            } catch (RuntimeException e) {
                logWithCallId("RuntimeException callSessionConferenceStateUpdated()", ImsCallLogLevel.ERROR);
            }
        };

        @Override
        public void onAutoTerminate() {
            logWithCallId("onAutoTerminate()", ImsCallLogLevel.DEBUG);
            terminate(ImsReasonInfo.CODE_UNSPECIFIED);
        };
    }

    public void onAddParticipantComplete() {
        Rlog.d(LOG_TAG, "onAddParticipantComplete(): " + mCallId);
        if (mVTProvider != null) {
            mVTProviderUtil.resetWrapper(mVTProvider);
        }
    }

    private boolean isCallPull(ImsCallProfile profile) {
        if (profile == null || profile.mCallExtras == null) {
            return false;
        }
        Bundle extras = profile.mCallExtras.getBundle(ImsCallProfile.EXTRA_OEM_EXTRAS);
        if (extras == null) {
            return false;
        }
        boolean isCallPull = extras.getBoolean(ImsCallProfile.EXTRA_IS_CALL_PULL, false);
        return isCallPull;
    }

    private void pullCall(String target, ImsCallProfile profile) {
        Message result = mHandler.obtainMessage(EVENT_PULL_CALL_RESULT);
        mImsRILAdapter.pullCall(target, isVideoCall(profile), result);
    }

    private boolean isVideoCall(ImsCallProfile profile) {
        if (profile == null) {
            return false;
        }
        if (ImsCallProfile.getVideoStateFromImsCallProfile(profile) !=
                VideoProfile.STATE_AUDIO_ONLY) {
            return true;
        }
        return false;
    }

    private void updateShouldUpdateAddressByPau() {
        // Do not use PAU to update address for JIO SIM
        mShouldUpdateAddressByPau = getBooleanFromCarrierConfig(
                ImsCarrierConfigConstants.MTK_KEY_CARRIER_UPDATE_DIALING_ADDRESS_FROM_PAU);
    }

    private void updateShouldUseSipField() {
        mShouldUpdateAddressBySipField = !(
                OperatorUtils.isMatched(OperatorUtils.OPID.OP236, mPhoneId)
                );
        logWithCallId("updateShouldUseSipField() : mShouldUpdateAddressBySipField = " +
                mShouldUpdateAddressBySipField, ImsCallLogLevel.DEBUG);
    }

    private int updateIsIbt(String[] callInfo) {
        int isIbt = 1;

        if (callInfo[2] != null) {
            isIbt = Integer.parseInt(callInfo[2]);
        }
        logWithCallId("updateIsIbt() : isIbt= " + isIbt, ImsCallLogLevel.DEBUG);

        if (isIbt == 0) {
            mCallProfile.mMediaProfile.mAudioDirection =
                    ImsStreamMediaProfile.DIRECTION_INACTIVE;
        } else {
            mCallProfile.mMediaProfile.mAudioDirection =
                    ImsStreamMediaProfile.DIRECTION_RECEIVE;
        }
        return isIbt;
    }

    private void createConferenceSession(ImsCallProfile imsCallProfile, String callId) {
        if (mMtkImsCallSessionProxy != null) {
            // server for MTK framework
            createMtkConferenceSession(imsCallProfile, callId);
        } else {
            // server for AOSP framework
            createAospConferenceSession(imsCallProfile, callId);
        }
    }

    private void createMtkConferenceSession(ImsCallProfile imsCallProfile, String callId) {

        mMtkConfSessionProxy = new MtkImsCallSessionProxy(
                mContext, imsCallProfile, null, mImsService,
                mServiceHandler, mImsRILAdapter, callId, mPhoneId);
        ImsCallSessionProxy imsCallSessionProxy = new ImsCallSessionProxy(
                mContext, imsCallProfile, null, mImsService,
                mServiceHandler, mImsRILAdapter, callId, mPhoneId);

        mMtkConfSessionProxy.setAospCallSessionProxy(imsCallSessionProxy);
        imsCallSessionProxy.setMtkCallSessionProxy(mMtkConfSessionProxy);

        ConferenceEventListener confEvtListener =
                mMtkConfSessionProxy.getConfEvtListener();
        ImsConferenceHandler.getInstance().startConference(
                mContext, confEvtListener, callId, mPhoneId);
        mMtkImsCallSessionProxy.notifyCallSessionMergeStarted(mMtkConfSessionProxy.getServiceImpl(), mCallProfile);
    }

    private void createAospConferenceSession(ImsCallProfile imsCallProfile, String callId) {

        mConfSessionProxy = new ImsCallSessionProxy(
                mContext, imsCallProfile, null, mImsService,
                mServiceHandler, mImsRILAdapter, callId, mPhoneId);

        ConferenceEventListener confEvtListener = mConfSessionProxy.getConfEvtListener();
        ImsConferenceHandler.getInstance().startConference(
                mContext, confEvtListener, callId, mPhoneId);
        try {
            mListener.callSessionMergeStarted(mConfSessionProxy.getServiceImpl(), mCallProfile);
        } catch (RuntimeException e) {
            logWithCallId("RuntimeException callSessionMergeStarted()", ImsCallLogLevel.ERROR);
        }
    }

    private void terminateConferenceSession() {
        if (mMtkConfSessionProxy != null) {
            logWithCallId("terminateConferenceSession() : Hangup Conference: Hangup host while merging (mtk)", ImsCallLogLevel.DEBUG);
            MtkImsCallSessionProxy confSession = mMtkConfSessionProxy;
            confSession.terminate(ImsReasonInfo.CODE_LOCAL_ILLEGAL_STATE);
            ImsConferenceHandler.getInstance().closeConference(confSession.getCallId());
            mParticipants.clear();
        } else if (mConfSessionProxy != null) {
            logWithCallId("terminateConferenceSession() : Hangup Conference: Hangup host while merging (aosp)", ImsCallLogLevel.DEBUG);
            ImsCallSessionProxy confSession = mConfSessionProxy;
            confSession.terminate(ImsReasonInfo.CODE_LOCAL_ILLEGAL_STATE);
            ImsConferenceHandler.getInstance().closeConference(confSession.getCallId());
            mParticipants.clear();
        }
    }

    private void closeConferenceSession() {
        if (mMtkConfSessionProxy != null) {
            mMtkConfSessionProxy.close();
            mMtkConfSessionProxy = null;
        } else if (mConfSessionProxy != null) {
            mConfSessionProxy.close();
            // ALPS02588163, mConfSessionProxy should set to null, or it might be
            // closed twice.
            mConfSessionProxy = null;
        }
    }

    private void notifyCallSessionMergeComplete() {
        if (mMtkImsCallSessionProxy != null) {
            if (mMtkConfSessionProxy != null) {
                mMtkImsCallSessionProxy.notifyCallSessionMergeComplete(mMtkConfSessionProxy.getServiceImpl());
            } else {
                mMtkImsCallSessionProxy.notifyCallSessionMergeComplete(null);
            }
        } else if (mListener != null) {
            try {
                if (mConfSessionProxy != null) {
                    mListener.callSessionMergeComplete(mConfSessionProxy.getServiceImpl());
                } else {
                    // for ambiguous isssue that
                    // callSessionMergeComplete() has two interface
                    IImsCallSession newSession = null;

                    mListener.callSessionMergeComplete(newSession);
                }
            } catch (RuntimeException e) {
                logWithCallId("RuntimeException callSessionMergeComplete()", ImsCallLogLevel.ERROR);
            }
        }
    }

    private boolean getBooleanFromCarrierConfig(String key) {
        int subId = mImsService.getSubIdUsingPhoneId(mPhoneId);
        CarrierConfigManager configMgr = (CarrierConfigManager) mContext.
                                                getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle carrierConfig =
                configMgr.getConfigForSubId(subId);
        boolean result = carrierConfig.getBoolean(key);
        logWithCallId("getBooleanFromCarrierConfig() : key = " + key + " result = " + result, ImsCallLogLevel.DEBUG);
        return result;
    }

    private boolean isUserPerfromedHangup() {
        if (mHangupCount > 0) {
            return true;
        }
        return false;
    }

    private boolean shouldNotifyCallDropByBadWifiQuality() {
        boolean notifyWifiQualityDisconnectCause = getBooleanFromCarrierConfig(
                ImsCarrierConfigConstants.MTK_KEY_CARRIER_NOTIFY_BAD_WIFI_QUALITY_DISCONNECT_CAUSE);
        logWithCallId("shouldNotifyCallDropByBadWifiQuality() : "
                + " carrier =  " + notifyWifiQualityDisconnectCause
                + " isUserPerfromedHangup = " + isUserPerfromedHangup()
                + " mRatType = " + mRatType, ImsCallLogLevel.DEBUG);
        if (notifyWifiQualityDisconnectCause
                && isUserPerfromedHangup() == false
                && mRatType == WifiOffloadManager.RAN_TYPE_WIFI) {
            return true;
        }
        return false;
    }

    private int getWifiRssi() {
        WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        int rssi = info.getRssi();
        logWithCallId("getWifiRssi()" + rssi, ImsCallLogLevel.DEBUG);
        return rssi;
    }

    private void correctRemoteVideoCapabilityForVideoConference() {
        if (isMultiparty() && isVideoCall(mCallProfile)) {
            logWithCallId("correctRemoteVideoCapabilityForVideoConference() : Video conference, force set remote as Video Call", ImsCallLogLevel.DEBUG);
            mRemoteCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT;
        }
    }

    private boolean removeRemoteCallVideoCapability() {
        logWithCallId("removeRemoteCallVideoCapability()", ImsCallLogLevel.DEBUG);

        if (mRemoteCallProfile.mCallType != ImsCallProfile.CALL_TYPE_VOICE) {
            mRemoteCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VOICE;
            return true;

        } else {
            return false;
        }
    }

    private void updateCallType(int callMode, int videoState) {
        if (callMode == IMS_VIDEO_CALL || callMode == IMS_VIDEO_CONF ||
                callMode == IMS_VIDEO_CONF_PARTS) {
            switch(videoState) {
                case VIDEO_STATE_PAUSE:  //pause
                    // mCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT_NODIR;
                    // logWithCallId("updateCallType() : mCallType = CALL_TYPE_VT_NODIR", ImsCallLogLevel.DEBUG);
                    break;
                case VIDEO_STATE_SEND_ONLY:  //send only
                    mCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT_TX;
                    logWithCallId("updateCallType() : mCallType = CALL_TYPE_VT_TX", ImsCallLogLevel.DEBUG);
                    break;
                case VIDEO_STATE_RECV_ONLY:  // recv only
                    mCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT_RX;
                    logWithCallId("updateCallType() : mCallType = CALL_TYPE_VT_RX", ImsCallLogLevel.DEBUG);
                    break;
                case VIDEO_STATE_SEND_RECV:  // send and recv
                    mCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT;
                    logWithCallId("updateCallType() : mCallType = CALL_TYPE_VT", ImsCallLogLevel.DEBUG);
                    break;
                default:
                    mCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT;
                    logWithCallId("updateCallType() : mCallType = CALL_TYPE_VT", ImsCallLogLevel.DEBUG);
                    break;
            }
            mWasVideoCallForRtt = true;
        } else if (callMode == IMS_VOICE_CALL || callMode == IMS_VOICE_CONF ||
                callMode == IMS_VOICE_CONF_PARTS) {
            mCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VOICE;
            logWithCallId("updateCallType() : mCallType = CALL_TYPE_VOICE", ImsCallLogLevel.DEBUG);
        }
        logWithCallId("updateCallType() : " + mCallProfile.mCallType, ImsCallLogLevel.DEBUG);
    }

    private boolean isAnsweredElsewhere(String header) {
        if (header == null) {
            return false;
        }

        if (header.equalsIgnoreCase(SipMessage.COMPETION_ELSEWHERE_HEADER) ||
                header.toLowerCase().contains(SipMessage.CALL_COMPLETED_ELSEWHERE_HEADER)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isRemoteCallDecline(String header) {
        if (header == null) {
            return false;
        }

        if (header.equalsIgnoreCase(SipMessage.REMOTE_DECLINE_HEADER) ||
                header.toLowerCase().contains(SipMessage.CALL_DECLINED_HEADER)) {
            return true;
        } else {
            return false;
        }
    }

    private void rejectDial() {
        logWithCallId("rejectDial()", ImsCallLogLevel.DEBUG);
        mCallErrorState = CallErrorState.DIAL;
        notifyCallSessionTerminated(new ImsReasonInfo());
    }

    private String getConfParticipantUri(String addr) {
        String participantUri;
        if (ImsCommonUtil.supportMdAutoSetupIms()) {
            participantUri = addr;
        } else {
            participantUri = ImsConferenceHandler.getInstance().getConfParticipantUri(addr);
        }

        return participantUri;
    }

    // For VzW MDMI
    private void sendCallEventWithRat(int msgType) {
        Bundle extras = new Bundle();
        extras.putInt(EXTRA_CALL_INFO_MESSAGE_TYPE, msgType);
        extras.putInt(EXTRA_CALL_TYPE, mCallProfile.mCallType);
        extras.putInt(EXTRA_RAT_TYPE, mRatType);
        extras.putBoolean(EXTRA_INCOMING_CALL, mIsIncomingCall);
        extras.putBoolean(EXTRA_EMERGENCY_CALL, mIsEmergencyCall);
        mOpImsCallSession.sendCallEventWithRat(extras);
    }

    private void detailLog(String msg) {

        if (TELDBG == false)
            return;

        logWithCallId(msg, ImsCallLogLevel.DEBUG);
    }

    private void logWithCallId(String msg, int lvl) {

        if (!DBG) return;

        if (ImsCallLogLevel.VERBOSE == lvl && TELDBG) {
            Rlog.v(LOG_TAG, "[callId = " + mCallId + "] " + msg);
        } else if (ImsCallLogLevel.DEBUG == lvl) {
            Rlog.d(LOG_TAG, "[callId = " + mCallId + "] " + msg);
        } else if (ImsCallLogLevel.INFO== lvl) {
            Rlog.i(LOG_TAG, "[callId = " + mCallId + "] " + msg);
        } else if (ImsCallLogLevel.WARNNING == lvl) {
            Rlog.w(LOG_TAG, "[callId = " + mCallId + "] " + msg);
        } else if (ImsCallLogLevel.ERROR == lvl) {
            Rlog.e(LOG_TAG, "[callId = " + mCallId + "] " + msg);
        } else {
            Rlog.d(LOG_TAG, "[callId = " + mCallId + "] " + msg);
        }
    }

    private void logEventResult(boolean isSuccess, String msg) {
        logWithCallId(msg + ((isSuccess)? " success": " failed"), ImsCallLogLevel.DEBUG);
    }

    private String sensitiveEncode(String msg) {
        if (!SENLOG || TELDBG) {
            return msg;
        } else {
            return "[hidden]";
        }
    }

    private int getHangupCause(int reasionInfo) {

        logWithCallId("getHangupCause() : " + reasionInfo, ImsCallLogLevel.DEBUG);

        if (reasionInfo == ImsReasonInfo.CODE_USER_DECLINE) {
            return -1; // no carry cause to MD
        } else if (reasionInfo == MtkImsReasonInfo.CODE_NO_COVERAGE){
            return HANGUP_CAUSE_NO_COVERAGE;
        } else if (reasionInfo == ImsReasonInfo.CODE_LOW_BATTERY){
            return HANGUP_CAUSE_LOW_BATTERY;
        } else if (reasionInfo == MtkImsReasonInfo.CODE_FORWARD){
            return HANGUP_CAUSE_FORWARD;
        } else if (reasionInfo == MtkImsReasonInfo.CODE_SPECIAL_HANGUP){
            return HANGUP_CAUSE_SPECIAL_HANGUP;
        }else {
            return HANGUP_CAUSE_NONE;
        }
    }

    private void handleSpeechCodecInfo(AsyncResult ar) {
        int codec = ((int[]) ar.result)[0];

        logWithCallId("handleSpeechCodecInfo() : " + codec, ImsCallLogLevel.DEBUG);

        switch (codec) {
            case QCELP13K:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_QCELP13K;
                break;
            case EVRC:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_EVRC;
                break;
            case EVRC_B:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_EVRC_B;
                break;
            case EVRC_WB:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_EVRC_WB;
                break;
            case EVRC_NW:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_EVRC_NW;
                break;
            case AMR_NB:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_AMR;
                break;
            case AMR_WB:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_AMR_WB;
                break;
            case GSM_EFR:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_GSM_EFR;
                break;
            case GSM_FR:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_GSM_FR;
                break;
            case GSM_HR:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_GSM_HR;
                break;
            case EVS_NB:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_EVS_NB;
                break;
            case EVS_WB:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_EVS_WB;
                break;
            case EVS_SW:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_EVS_SWB;
                break;
            case EVS_FB:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_EVS_FB;
                break;
            case EVS_AWB:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        MtkImsConstants.AUDIO_QUALITY_EVS_AWB;
                break;
            default:
                mLocalCallProfile.mMediaProfile.mAudioQuality =
                        ImsStreamMediaProfile.AUDIO_QUALITY_NONE;
                break;
        }
        notifyCallSessionUpdated();
    }

    private void turnOffAirplaneMode() {
        Rlog.d(LOG_TAG, "turnOffAirplaneMode()");

        if (Settings.Global.getInt(mContext.getContentResolver(),
            Settings.Global.AIRPLANE_MODE_ON, 0) > 0) {
            Rlog.d(LOG_TAG, "turnOffAirplaneMode() : Turning off airplane mode.");

            // Change the system setting
            Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0);

            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", false);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private void handleRedialEccIndication(AsyncResult ar) {
        // +ERDECCIND: <call_id>
        Rlog.d(LOG_TAG, "handleRedialEccIndication()");

        if (ar == null || mMtkImsCallSessionProxy == null) {
            if (DBG) {
                Rlog.d(LOG_TAG, "handleRedialEccIndication() : ar or mMtkImsCallSessionProxy is null");
            }
            return;
        }

        // No need to handle when call established
        if (mState == ImsCallSession.State.ESTABLISHED) {
            Rlog.d(LOG_TAG, "handleRedialEccIndication() : Call established, ignore indication");
            return;
        }

        // No need to notify ecc when call id not much (Ex: id = 0)
        String[] result = (String[]) ar.result;
        if ((result != null && result[0].equals("0"))) {
            turnOffAirplaneMode();

        } else if ((result != null && result[0].equals(mCallId))) {
            mMtkImsCallSessionProxy.notifyRedialEcc();
        }
    }

    private String event2String(int event) {

        if (EVENT_POLL_CALLS_RESULT == event) {
            return "EVENT_POLL_CALLS_RESULT";
        } else if (EVENT_CALL_INFO_INDICATION == event) {
            return "EVENT_CALL_INFO_INDICATION";
        } else if (EVENT_ECONF_RESULT_INDICATION == event) {
            return "EVENT_ECONF_RESULT_INDICATION";
        } else if (EVENT_GET_LAST_CALL_FAIL_CAUSE == event) {
            return "EVENT_GET_LAST_CALL_FAIL_CAUSE";
        } else if (EVENT_CALL_MODE_CHANGE_INDICATION == event) {
            return "EVENT_CALL_MODE_CHANGE_INDICATION";
        } else if (EVENT_VIDEO_CAPABILITY_INDICATION == event) {
            return "EVENT_VIDEO_CAPABILITY_INDICATION";
        } else if (EVENT_ON_USSI == event) {
            return "EVENT_ON_USSI";
        } else if (EVENT_ECT_RESULT_INDICATION == event) {
            return "EVENT_ECT_RESULT_INDICATION";
        } else if (EVENT_GTT_CAPABILITY_INDICATION == event) {
            return "EVENT_GTT_CAPABILITY_INDICATION";
        } else if (EVENT_IMS_CONFERENCE_INDICATION == event) {
            return "EVENT_IMS_CONFERENCE_INDICATION";
        } else if (EVENT_DIAL_RESULT == event) {
            return "EVENT_DIAL_RESULT";
        } else if (EVENT_ACCEPT_RESULT == event) {
            return "EVENT_ACCEPT_RESULT";
        } else if (EVENT_HOLD_RESULT == event) {
            return "EVENT_HOLD_RESULT";
        } else if (EVENT_RESUME_RESULT == event) {
            return "EVENT_RESUME_RESULT";
        } else if (EVENT_MERGE_RESULT == event) {
            return "EVENT_MERGE_RESULT";
        } else if (EVENT_ADD_CONFERENCE_RESULT == event) {
            return "EVENT_ADD_CONFERENCE_RESULT";
        } else if (EVENT_REMOVE_CONFERENCE_RESULT == event) {
            return "EVENT_REMOVE_CONFERENCE_RESULT";
        } else if (EVENT_SIP_CODE_INDICATION == event) {
            return "EVENT_SIP_CODE_INDICATION";
        } else if (EVENT_DIAL_CONFERENCE_RESULT == event) {
            return "EVENT_DIAL_CONFERENCE_RESULT";
        } else if (EVENT_SWAP_BEFORE_MERGE_RESULT == event) {
            return "EVENT_SWAP_BEFORE_MERGE_RESULT";
        } else if (EVENT_RETRIEVE_MERGE_FAIL_RESULT == event) {
            return "EVENT_RETRIEVE_MERGE_FAIL_RESULT";
        } else if (EVENT_DTMF_DONE == event) {
            return "EVENT_DTMF_DONE";
        } else if (EVENT_SEND_USSI_COMPLETE == event) {
            return "EVENT_SEND_USSI_COMPLETE";
        } else if (EVENT_CANCEL_USSI_COMPLETE == event) {
            return "EVENT_CANCEL_USSI_COMPLETE";
        } else if (EVENT_ECT_RESULT == event) {
            return "EVENT_ECT_RESULT";
        } else if (EVENT_PULL_CALL_RESULT == event) {
            return "EVENT_PULL_CALL_RESULT";
        } else if (EVENT_RADIO_NOT_AVAILABLE == event) {
            return "EVENT_RADIO_NOT_AVAILABLE";
        } else if (EVENT_RTT_TEXT_RECEIVE_INDICATION == event) {
            return "EVENT_RTT_TEXT_RECEIVE_INDICATION";
        } else if (EVENT_RTT_MODIFY_RESPONSE == event) {
            return "EVENT_RTT_MODIFY_RESPONSE";
        } else if (EVENT_RTT_MODIFY_REQUEST_RECEIVE == event) {
            return "EVENT_RTT_MODIFY_REQUEST_RECEIVE";
        } else if (EVENT_DIAL_FROM_RESULT == event) {
            return "EVENT_DIAL_FROM_RESULT";
        } else if (EVENT_DEVICE_SWITCH_REPONSE == event) {
            return "EVENT_DEVICE_SWITCH_REPONSE";
        } else if (EVENT_SPEECH_CODEC_INFO == event) {
            return "EVENT_SPEECH_CODEC_INFO";
        } else {
            return "unknown msg" + event;
        }
    }

    public MtkImsCallSessionProxy getMtkCallSessionProxy() {
        return mMtkImsCallSessionProxy;
    }

    public void setMtkCallSessionProxy(MtkImsCallSessionProxy callSessionProxy) {
        mMtkImsCallSessionProxy = callSessionProxy;
    }

    public ImsCallPluginBase getImsExtCallUtil() {

        ExtensionPluginFactory facotry = ExtensionFactory.makeExtensionPluginFactory();
        return facotry.makeImsCallPlugin(mContext);
    }

    public ImsSelfActivatorBase getImsExtSelfActivator(
        Context context, Handler handler, ImsCallSessionProxy callSessionProxy,
        ImsCommandsInterface imsRILAdapter, ImsService imsService, int phoneId) {

        ExtensionPluginFactory facotry = ExtensionFactory.makeExtensionPluginFactory();
        return facotry.makeImsSelfActivator(context, handler, this, imsRILAdapter, imsService, phoneId);
    }

    /// M: E911 During VoLTE off @{
    private void tryTurnOnVolteForE911(boolean isEmergencyNumber) {
        if (isEmergencyNumber) {
            ImsManager imsManager = ImsManager.getInstance(mContext, mPhoneId);
            boolean volteEnabledByPlatform = imsManager.isVolteEnabledByPlatform();
            boolean volteEnabledByUser = imsManager.isEnhanced4gLteModeSettingEnabledByUser();
            logWithCallId("tryTurnOnVolteForE911() : isEmergencNumber " + isEmergencyNumber +
                    ", volteEnabledByPlatform " + volteEnabledByPlatform +
                    ", volteEnabledByUser " + volteEnabledByUser, ImsCallLogLevel.INFO);
            if (!volteEnabledByPlatform) {
                ImsConfigUtils.triggerSendCfgForVolte(mContext, mImsRILAdapter, mPhoneId, 1);
                mImsServiceCT.setEnableVolteForImsEcc(true);
            }
        }
    }
    /// @}

    private void enableRttAudioIndication() {
        if (mState != ImsCallSession.State.ESTABLISHED) {
            logWithCallId("enableRttAudioInication() : call not established", ImsCallLogLevel.DEBUG);
            return;
        }

        mOpExt.enableRttAudioIndication(mCallId, mImsRILAdapter);
    }

    private void handleAudioIndication(AsyncResult ar) {
        // AT<+EIMSAUDIOSID: <call id>, <silence_indication>
        Rlog.d(LOG_TAG, "handleAudioIndication()");

        if (ar == null || mMtkImsCallSessionProxy == null) {
            if (DBG) {
                Rlog.d(LOG_TAG, "handleAudioIndication() : ar or mMtkImsCallSessionProxy is null");
            }
            return;
        }

        int[] result = (int[]) ar.result;
        int speech = result[1];

        // RTT audio indication
        mOpExt.handleRttSpeechEvent(mMtkImsCallSessionProxy.getServiceImpl(), speech);
    }
}
