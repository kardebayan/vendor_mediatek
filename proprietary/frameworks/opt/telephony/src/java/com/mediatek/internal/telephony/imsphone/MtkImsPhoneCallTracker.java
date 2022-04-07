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

package com.mediatek.internal.telephony.imsphone;

import static com.mediatek.internal.telephony.MtkTelephonyProperties.PROPERTY_TBCW_MODE;
import static com.mediatek.internal.telephony.MtkTelephonyProperties.TBCW_DISABLED;
import static com.mediatek.internal.telephony.MtkTelephonyProperties.TBCW_OFF;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.preference.PreferenceManager;
import android.telecom.ConferenceParticipant;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.DisconnectCause;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsCallSession;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsStreamMediaProfile;
import android.telephony.ims.ImsSuppServiceNotification;

import com.android.ims.ImsCall;
import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsUtInterface;
import com.android.ims.internal.IImsCallSession;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.dataconnection.DataEnabledSettings;
import com.android.internal.telephony.imsphone.ImsExternalCallTracker;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCall;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.internal.telephony.imsphone.ImsPullCall;
import com.android.internal.telephony.nano.TelephonyProto.ImsConnectionState;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SettingsObserver;
import com.android.internal.telephony.TelephonyDevController;
import com.android.internal.telephony.TelephonyProperties;
import com.android.server.net.NetworkStatsService;

import com.mediatek.ims.MtkImsCall;
import com.mediatek.ims.MtkImsConnectionStateListener;
import com.mediatek.ims.MtkImsReasonInfo;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.MtkCallStateException;
import com.mediatek.internal.telephony.MtkHardwareConfig;

import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils;
import com.mediatek.internal.telephony.MtkIncomingCallChecker;

import mediatek.telecom.FormattedLog;
import mediatek.telephony.MtkDisconnectCause;
import mediatek.telephony.MtkCarrierConfigManager;

import com.mediatek.internal.telephony.digits.DigitsUtil;
import com.mediatek.internal.telephony.digits.DigitsUssdManager;
import com.mediatek.internal.telephony.imsphone.op.OpCommonImsPhoneCallTracker;
import com.mediatek.internal.telephony.imsphone.op.OpImsPhoneCallTracker;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;

import com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.ImsCommand;

/**
 * {@hide}
 */
public class MtkImsPhoneCallTracker extends ImsPhoneCallTracker implements ImsPullCall {
    static final String LOG_TAG = "MtkImsPhoneCallTracker";
    // Sensitive log task
    private static final String PROP_FORCE_DEBUG_KEY = "persist.vendor.log.tel_dbg";
    private static final boolean SENLOG = TextUtils.equals(Build.TYPE, "user");
    private static final boolean TELDBG = (SystemProperties.getInt(PROP_FORCE_DEBUG_KEY, 0) == 1);

    {
        mRingingCall = new MtkImsPhoneCall(this, ImsPhoneCall.CONTEXT_RINGING);
        mForegroundCall = new MtkImsPhoneCall(this, ImsPhoneCall.CONTEXT_FOREGROUND);
        mBackgroundCall = new MtkImsPhoneCall(this, ImsPhoneCall.CONTEXT_BACKGROUND);
        mHandoverCall = new MtkImsPhoneCall(this, ImsPhoneCall.CONTEXT_HANDOVER);
    }

    //private MtkImsPhoneConnection mPendingMO;

    /// M: @{
    // Redial as ECC
    private boolean mDialAsECC = false;

    // ALPS02023277 Set this flag to true when there is a resume request not finished.
    // Prevent to accept a waiting call during resuming a background call.
    private boolean mHasPendingResumeRequest = false;

    ///M: ALPS02577419. @{
    private boolean mIsOnCallResumed = false;
    /// @}

    /// M: ALPS02261962. For IMS registration state and capability information.
    private int mImsRegistrationErrorCode;

    private OpCommonImsPhoneCallTracker mOpCommonImsPhoneCallTracker;
    private OpImsPhoneCallTracker mOpImsPhoneCallTracker;
    private DigitsUtil mDigitsUtil;
    private boolean mIsImsEccSupported = false;

    private int mWifiPdnOOSState = MtkImsManager.OOS_END_WITH_RESUME;

    private SubscriptionManager mSubscriptionManager;

    private boolean mRoamingVariablesInited = false;

    protected final OnSubscriptionsChangedListener mOnSubscriptionsChangedListener =
        new OnSubscriptionsChangedListener() {

            /**
             * Callback invoked when there is any change to any SubscriptionInfo. Typically
             * this method invokes {@link SubscriptionManager#getActiveSubscriptionInfoList}
             */
            @Override
            public void onSubscriptionsChanged() {
                if (DBG) log("SubscriptionListener.onSubscriptionInfoChanged, subId=" + mPhone.getSubId());

                // Update settingObserver
                int subId = mPhone.getSubId();
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    // Only need init roaming and roaming settings value when first time sub id ready
                    // These values will be updated by each registrar later.
                    if (!mRoamingVariablesInited) {
                        mRoamingVariablesInited = true;
                        initRoamingAndRoamingSetting();
                    }

                    registerSettingsObserver();
                }
            }
        };

    private final SettingsObserver mSettingsObserver;
    private boolean mIsDataRoaming = false;
    private boolean mIsDataRoamingSettingEnabled = false;
    private boolean mIgnoreDataRoaming = false;

    /// MTK incoming call number check for reject call at EAIC phase.
    private MtkIncomingCallChecker mIncomingCallCheker = null;

    // ALPS02462990 For OP01 requirement: Only one video call allowed.
    private static final int INVALID_CALL_MODE = 0xFF;
    private static final int IMS_VOICE_CALL = 20;
    private static final int IMS_VIDEO_CALL = 21;
    private static final int IMS_VOICE_CONF = 22;
    private static final int IMS_VIDEO_CONF = 23;
    private static final int IMS_VOICE_CONF_PARTS = 24;
    private static final int IMS_VIDEO_CONF_PARTS = 25;
    /// @}

    private static final int EVENT_ROAMING_ON = 27;
    private static final int EVENT_ROAMING_OFF = 28;
    private static final int EVENT_ROAMING_SETTING_CHANGE = 29;

    // flag for identify the modify operation triggered from IMS FWK
    public static final int IMS_SESSION_MODIFY_OPERATION_FLAG = 0x8000;

    /// M: ALPS03905309 for data to register the srvcc disconnected event.
    private RegistrantList mCallsDisconnectedDuringSrvccRegistrants = new RegistrantList();

    /// M: CC: Vzw/CTVolte ECC @{
    TelephonyDevController mTelDevController = TelephonyDevController.getInstance();
    private boolean hasC2kOverImsModem() {
        if (mTelDevController != null &&
                mTelDevController.getModem(0) != null &&
                ((MtkHardwareConfig) mTelDevController.getModem(0)).hasC2kOverImsModem() == true) {
                    return true;
        }
        return false;
    }
    /// @}

    private MtkImsConnectionStateListener mImsStateListener = new MtkImsConnectionStateListener() {
        /**
         * M: Called when IMS emergency capability changed.
         */
        @Override
        public void onImsEmergencyCapabilityChanged(boolean eccSupport) {
            log("onImsEmergencyCapabilityChanged: " + eccSupport);
            // notify SST to update UI
            mPhone.onFeatureCapabilityChanged();
            mIsImsEccSupported = eccSupport;
            ((MtkImsPhone) mPhone).updateIsEmergencyOnly();
        }

        /**
         * M: Called when VoWifi wifi PDN Out Of Service state changed.
         */
        @Override
        public void onWifiPdnOOSStateChanged(int oosState) {
            log("onWifiPdnOOSStateChanged: " + oosState);
            // store the oosState in here and let UI to query
            mWifiPdnOOSState = oosState;
        }

        /**
         * The status of the feature's capabilities has changed to either available or unavailable.
         * If unavailable, the feature is not able to support the unavailable capability at this
         * time.
         *
         * @param config The new availability of the capabilities.
         */
        @Override
        public void onCapabilitiesStatusChanged(ImsFeature.Capabilities config) {
            log("onCapabilitiesStatusChanged: " + config);
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = config;
            // Remove any pending updates; they're already stale, so no need to process
            // them.
            removeMessages(EVENT_ON_FEATURE_CAPABILITY_CHANGED);
            obtainMessage(EVENT_ON_FEATURE_CAPABILITY_CHANGED, args).sendToTarget();
            //M: for RTT
            checkRttCallType();
        }

        /**
         * Called when the device is trying to connect to the IMS network with {@param imsRadioTech}.
         */
        @Override
        public void onImsConnected(int imsRadioTech) {
            if (DBG) log("onImsConnected imsRadioTech=" + imsRadioTech);
            mPhone.setServiceState(ServiceState.STATE_IN_SERVICE);
            mPhone.setImsRegistered(true);
            mMetrics.writeOnImsConnectionState(mPhone.getPhoneId(),
                    ImsConnectionState.State.CONNECTED, null);
        }

        /**
         * Called when the device is trying to connect to the IMS network with {@param imsRadioTech}.
         */
        public void onImsProgressing(int imsRadioTech) {
            if (DBG) log("onImsProgressing imsRadioTech=" + imsRadioTech);
            mPhone.setServiceState(ServiceState.STATE_OUT_OF_SERVICE);
            mPhone.setImsRegistered(false);
            mMetrics.writeOnImsConnectionState(mPhone.getPhoneId(),
                    ImsConnectionState.State.PROGRESSING, null);
        }

        /**
         * Called when the device is disconnected from the IMS network.
         */
        public void onImsDisconnected(ImsReasonInfo imsReasonInfo) {
            if (DBG) log("onImsDisconnected imsReasonInfo=" + imsReasonInfo);
            mPhone.setServiceState(ServiceState.STATE_OUT_OF_SERVICE);
            mPhone.setImsRegistered(false);
            mPhone.processDisconnectReason(imsReasonInfo);
            mMetrics.writeOnImsConnectionState(mPhone.getPhoneId(),
                    ImsConnectionState.State.DISCONNECTED, imsReasonInfo);
        }
    };

    public boolean isSupportImsEcc() {
        return mIsImsEccSupported;
    }

    public MtkImsPhoneCallTracker(ImsPhone phone) {
        super(phone);
        mOpCommonImsPhoneCallTracker = OpTelephonyCustomizationUtils.getOpCommonInstance()
                .makeOpCommonImsPhoneCallTracker();
        mOpImsPhoneCallTracker = OpTelephonyCustomizationUtils.getOpFactory(mPhone.getContext())
                .makeOpImsPhoneCallTracker();
        mDigitsUtil = OpTelephonyCustomizationUtils.getOpFactory(mPhone.getContext())
                .makeDigitsUtil();

        /// M: register the indication receiver. @{
        registerIndicationReceiver();
        /// @}

        mSettingsObserver = new SettingsObserver(mPhone.getContext(), this);
        registerSettingsObserver();

        mSubscriptionManager = SubscriptionManager.from(mPhone.getContext());
        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangedListener);

        // Register for data roaming and roaming settings
        mPhone.getDefaultPhone().getServiceStateTracker().registerForDataRoamingOn(this,
                EVENT_ROAMING_ON, null);
        mPhone.getDefaultPhone().getServiceStateTracker().registerForDataRoamingOff(this,
                EVENT_ROAMING_OFF, null, true);

        ///M: for RTT
        mOpCommonImsPhoneCallTracker.initRtt(mPhone);
    }

    @Override
    public void dispose() {
        if (DBG) log("dispose");
        mRingingCall.dispose();
        mBackgroundCall.dispose();
        mForegroundCall.dispose();
        mHandoverCall.dispose();

        clearDisconnected();
        if (mUtInterface != null) {
            mUtInterface.unregisterForSuppServiceIndication(this);
        }
        mPhone.getContext().unregisterReceiver(mReceiver);

        /// M: It needs to unregister Indication receivers here.  @{
        unregisterIndicationReceiver();

        // For WFC-DS, ImsPhone is disposed when SIM-Switch.
        mPhone.setServiceState(ServiceState.STATE_OUT_OF_SERVICE);
        mPhone.setImsRegistered(false);
        resetImsCapabilities();
        mPhone.onFeatureCapabilityChanged();
        /// @}

        mPhone.getDefaultPhone().unregisterForDataEnabledChanged(this);
        mImsManagerConnector.disconnect();

        mPhone.getDefaultPhone().getServiceStateTracker().unregisterForDataRoamingOn(this);
        mPhone.getDefaultPhone().getServiceStateTracker().unregisterForDataRoamingOff(this);

        mSubscriptionManager
                .removeOnSubscriptionsChangedListener(mOnSubscriptionsChangedListener);
        mSettingsObserver.unobserve();

        //M: RTT
        mOpCommonImsPhoneCallTracker.disposeRtt(mPhone, mForegroundCall, mSrvccState);

        if (mImsManager != null) {
            try {
                ((MtkImsManager) mImsManager).removeImsConnectionStateListener(mImsStateListener);
            } catch(ImsException e) {
                loge("dispose() : removeRegistrationListener failed: " + e);
            }
        }
    }

    @Override
    public synchronized Connection dial(String dialString, ImsPhone.ImsDialArgs dialArgs)
            throws CallStateException {
        if (mSrvccState == Call.SrvccState.STARTED || mSrvccState == Call.SrvccState.COMPLETED) {
            throw new CallStateException(
                    MtkCallStateException.ERROR_INVALID_DURING_SRVCC, "cannot dial call: SRVCC");
        }
        return super.dial(dialString, dialArgs);
    }

    @Override
    protected void dialInternal(ImsPhoneConnection conn, int clirMode, int videoState,
                              Bundle intentExtras) {

        if (conn == null) {
            return;
        }

        boolean isOneKeyConf = conn instanceof MtkImsPhoneConnection
                    && ((MtkImsPhoneConnection) conn).getConfDialStrings() != null;

        /// M:  For VoLTE enhanced conference call. @{
        if (!isOneKeyConf) {
            /// @}
            if (conn.getAddress() == null || conn.getAddress().length() == 0
                    || conn.getAddress().indexOf(PhoneNumberUtils.WILD) >= 0) {
                // Phone number is invalid
                conn.setDisconnectCause(DisconnectCause.INVALID_NUMBER);
                sendEmptyMessageDelayed(EVENT_HANGUP_PENDINGMO, TIMEOUT_HANGUP_PENDINGMO);
                return;
            }
        }

        // Always unmute when initiating a new call
        setMute(false);

        // M: CC: For 93, MD can switch phoneType when SIM not inserted,
        // TeleService won't trigger phone switch, so check both SIM's ECC
        // int serviceType = PhoneNumberUtils.isEmergencyNumber(conn.getAddress()) ?
        boolean isEmergencyNumber = isEmergencyNumber(conn.getAddress());
        /// @}

        int serviceType = isEmergencyNumber ?
                ImsCallProfile.SERVICE_TYPE_EMERGENCY : ImsCallProfile.SERVICE_TYPE_NORMAL;

        // Redial as ECC
        if (mDialAsECC) {
            serviceType = ImsCallProfile.SERVICE_TYPE_EMERGENCY;
            log("Dial as ECC: conn.getAddress(): " + conn.getAddress());
            mDialAsECC = false;
        }
        /// @}
        int callType = ImsCallProfile.getCallTypeFromVideoState(videoState);
        //TODO(vt): Is this sufficient?  At what point do we know the video state of the call?
        conn.setVideoState(videoState);

        try {
            /// M:  For VoLTE enhanced conference call. @{
            // String[] callees = new String[] { conn.getAddress() };
            String[] callees = null;
            if (isOneKeyConf) {
                ArrayList<String> dialStrings = ((MtkImsPhoneConnection) conn).getConfDialStrings();
                callees = (String[]) dialStrings.toArray(new String[dialStrings.size()]);
            } else {
                callees = new String[] { conn.getAddress() };
            }
            /// @}
            ImsCallProfile profile = mImsManager.createCallProfile(serviceType, callType);
            profile.setCallExtraInt(ImsCallProfile.EXTRA_OIR, clirMode);
            /// M:  For VoLTE enhanced conference call. @{
            if (isOneKeyConf) {
                profile.setCallExtraBoolean(ImsCallProfile.EXTRA_CONFERENCE, true);
            }
            /// @}

            // Translate call subject intent-extra from Telecom-specific extra key to the
            // ImsCallProfile key.
            if (intentExtras != null) {
                if (intentExtras.containsKey(android.telecom.TelecomManager.EXTRA_CALL_SUBJECT)) {
                    intentExtras.putString(ImsCallProfile.EXTRA_DISPLAY_TEXT,
                            cleanseInstantLetteringMessage(intentExtras.getString(
                                    android.telecom.TelecomManager.EXTRA_CALL_SUBJECT))
                    );
                }

                if (conn.hasRttTextStream()) {
                    profile.mMediaProfile.mRttMode = ImsStreamMediaProfile.RTT_MODE_FULL;
                }

                if (intentExtras.containsKey(ImsCallProfile.EXTRA_IS_CALL_PULL)) {
                    profile.mCallExtras.putBoolean(ImsCallProfile.EXTRA_IS_CALL_PULL,
                            intentExtras.getBoolean(ImsCallProfile.EXTRA_IS_CALL_PULL));
                    int dialogId = intentExtras.getInt(
                            ImsExternalCallTracker.EXTRA_IMS_EXTERNAL_CALL_ID);
                    conn.setIsPulledCall(true);
                    conn.setPulledDialogId(dialogId);
                }

                // Pack the OEM-specific call extras.
                profile.mCallExtras.putBundle(ImsCallProfile.EXTRA_OEM_EXTRAS, intentExtras);

                // NOTE: Extras to be sent over the network are packed into the
                // intentExtras individually, with uniquely defined keys.
                // These key-value pairs are processed by IMS Service before
                // being sent to the lower layers/to the network.

                //M: RTT
                mOpCommonImsPhoneCallTracker.setRttMode(intentExtras, profile);

                // Put dial from information to profile
                mDigitsUtil.putDialFrom(intentExtras, profile);
            }

            /// M: @{
            // ALPS03110315 When user dial a number with FDN on, will dial failed. And will not
            // receive ECPI '130' URC, and the EXTRA_OI is null. But the address number will be
            // updated as NULL due to null of EXTRA_OI.
            // Set EXTRA_OI before dial to avoid this case.
            if ((callees != null) && (callees.length == 1) &&
                    !profile.getCallExtraBoolean(ImsCallProfile.EXTRA_CONFERENCE)) {
                profile.setCallExtra(ImsCallProfile.EXTRA_OI, callees[0]);
            }
            /// @}

            ImsCall imsCall = mImsManager.makeCall(profile, callees, mMtkImsCallListener);
            conn.setImsCall(imsCall);

            mMetrics.writeOnImsCallStart(mPhone.getPhoneId(),
                    imsCall.getSession());

            setVideoCallProvider(conn, imsCall);
            conn.setAllowAddCallDuringVideoCall(mAllowAddCallDuringVideoCall);
        } catch (ImsException e) {
            loge("dialInternal : " + e);
            conn.setDisconnectCause(DisconnectCause.ERROR_UNSPECIFIED);
            sendEmptyMessageDelayed(EVENT_HANGUP_PENDINGMO, TIMEOUT_HANGUP_PENDINGMO);
            retryGetImsService();
        } catch (RemoteException e) {
        }
    }

    /**
     * Accepts a call with the specified video state.  The video state is the video state that the
     * user has agreed upon in the InCall UI.
     *
     * @param videoState The video State
     * @throws CallStateException
     */
    @Override
    public void acceptCall (int videoState) throws CallStateException {
        // Handle the action during SRVCC.
        if (mSrvccState == Call.SrvccState.STARTED || mSrvccState == Call.SrvccState.COMPLETED) {
            throw new CallStateException(
                    MtkCallStateException.ERROR_INVALID_DURING_SRVCC, "cannot accept call: SRVCC");
        }

        int videoStateAfterCheckingData = videoState;

        if (!isDataAvailableForViLTE()
            && !mRingingCall.getImsCall().isWifiCall()) {
            videoStateAfterCheckingData = VideoProfile.STATE_AUDIO_ONLY;
            log("Data is off, answer as voice call");
        }

        logDebugMessagesWithOpFormat("CC", "Answer", mRingingCall.getFirstConnection(), "");
        super.acceptCall(videoStateAfterCheckingData);
    }

    @Override
    public void rejectCall () throws CallStateException {
        /// M: ALPS02136981. Prints debug logs for ImsPhone.
        logDebugMessagesWithOpFormat("CC", "Reject", mRingingCall.getFirstConnection(), "");
        super.rejectCall();
    }

    @Override
    public void switchWaitingOrHoldingAndActive() throws CallStateException {
        // Handle the action during SRVCC.
        if (mSrvccState == Call.SrvccState.STARTED || mSrvccState == Call.SrvccState.COMPLETED) {
            throw new CallStateException(MtkCallStateException.ERROR_INVALID_DURING_SRVCC,
                    "cannot hold/unhold call: SRVCC");
        }

        // ALPS02136981. Prints debug logs for ImsPhone.
        ImsPhoneConnection conn = null;
        String msg = null;
        if (mForegroundCall.getState() == ImsPhoneCall.State.ACTIVE) {
            conn = mForegroundCall.getFirstConnection();
            if (mBackgroundCall.getState().isAlive()) {
                msg = "switch with background connection:" + mBackgroundCall.getFirstConnection();
            } else {
                msg = "hold to background";
            }
        } else {
            conn = mBackgroundCall.getFirstConnection();
            msg = "unhold to foreground";
        }
        logDebugMessagesWithOpFormat("CC", "Swap", conn, msg);

        super.switchWaitingOrHoldingAndActive();
    }

    @Override
    public void
    conference() {
        /// M: ALPS02136981. Prints debug logs for ImsPhone. @{
        logDebugMessagesWithOpFormat("CC", "Conference", mForegroundCall.getFirstConnection(),
                " merge with " + mBackgroundCall.getFirstConnection());
        /// @}

        if (mOpCommonImsPhoneCallTracker.isRttCallInvolved(
                    mForegroundCall.getImsCall(), mBackgroundCall.getImsCall())
                && !mOpCommonImsPhoneCallTracker.isAllowMergeRttCallToVoiceOnly()) {
            return;
        }
        super.conference();
    }

    @Override
    public void
    explicitCallTransfer() {
        if (DBG) {
            log("explicitCallTransfer");
        }

        ImsCall fgImsCall = mForegroundCall.getImsCall();
        if (fgImsCall == null) {
            log("explicitCallTransfer no foreground ims call");
            return;
        }

        ImsCall bgImsCall = mBackgroundCall.getImsCall();
        if (bgImsCall == null) {
            log("explicitCallTransfer no background ims call");
            return;
        }

        if (mForegroundCall.getState() != ImsPhoneCall.State.ACTIVE
                || mBackgroundCall.getState() != ImsPhoneCall.State.HOLDING) {
            log("annot transfer call");
            return;
        }

        try {
            ((MtkImsCall)fgImsCall).explicitCallTransfer();
        } catch (ImsException e) {
            log("explicitCallTransfer " + e.getMessage());
        }
    }

    /**
     * Transfers the active to specific number.
     *
     * @param number The transfer target number.
     * @param type ECT type
     */
    public void
    unattendedCallTransfer(String number, int type) {
        if (DBG) {
            log("unattendedCallTransfer number : " + sensitiveEncode(number) + ", type : " + type);
        }

        ImsCall fgImsCall = mForegroundCall.getImsCall();
        if (fgImsCall == null) {
            log("explicitCallTransfer no foreground ims call");
            return;
        }

        try {
            ((MtkImsCall)fgImsCall).unattendedCallTransfer(number, type);
        } catch (ImsException e) {
            log("explicitCallTransfer " + e.getMessage());
        }
    }

    /**
     * Switch the active call to specific device.
     *
     * @param number The switch target number.
     * @param deviceId The switch target deviceId.
     */
    public void
    deviceSwitch(String number, String deviceId) {
        if (DBG) {
            log("deviceSwitch number : " + sensitiveEncode(number) + ", deviceId : " + deviceId);
        }

        ImsCall fgImsCall = mForegroundCall.getImsCall();
        if (fgImsCall == null) {
            log("deviceSwitch no foreground ims call");
            return;
        }

        try {
            ((MtkImsCall)fgImsCall).deviceSwitch(number, deviceId);
        } catch (ImsException e) {
            log("deviceSwitch " + e.getMessage());
        }
    }

    /**
     * Cancel the device switch action
     *
     */
    public void
    cancelDeviceSwitch() {
        if (DBG) {
            log("cancelDeviceSwitch");
        }

        ImsCall fgImsCall = mForegroundCall.getImsCall();
        if (fgImsCall == null) {
            log("cancelDeviceSwitch no foreground ims call");
            return;
        }

        try {
            ((MtkImsCall)fgImsCall).cancelDeviceSwitch();
        } catch (ImsException e) {
            log("cancelDeviceSwitch " + e.getMessage());
        }
    }

    @Override
    public boolean
    canDial() {
        boolean ret;
        int serviceState = mPhone.getServiceState().getState();
        String disableCall = SystemProperties.get(
                TelephonyProperties.PROPERTY_DISABLE_CALL, "false");

        /// M: add for debug @{
        log("IMS: canDial() serviceState = " + serviceState + ", disableCall = " + disableCall
                + ", mPendingMO = " + mPendingMO + ", Is mRingingCall ringing = "
                + mRingingCall.isRinging() + ", Is mForegroundCall alive = "
                + mForegroundCall.getState().isAlive() +
                ", Is mBackgroundCall alive = " + mBackgroundCall.getState().isAlive());
        /// @}

        if ((mPhone != null) && (mPhone.getDefaultPhone() instanceof MtkGsmCdmaPhone) &&
                ((MtkGsmCdmaPhone)mPhone.getDefaultPhone()).shouldProcessSelfActivation()) {
            log("IMS: canDial(), bypass dial for self activation");
            return true;
        }

        return super.canDial();
    }

    //***** Called from ImsPhoneCall
    @Override
    public void hangup (ImsPhoneCall call) throws CallStateException {
        /// M: Handle the action during SRVCC. @{
        if (mSrvccState == Call.SrvccState.STARTED || mSrvccState == Call.SrvccState.COMPLETED) {
            throw new CallStateException(
                    MtkCallStateException.ERROR_INVALID_DURING_SRVCC, "cannot hangup call: SRVCC");
        }
        /// @}
        super.hangup(call);
    }

    public void hangup (ImsPhoneCall call, int reason) throws CallStateException {
        log("hangup call with reason: " + reason);

        /// M: Handle the action during SRVCC. @{
        if (mSrvccState == Call.SrvccState.STARTED || mSrvccState == Call.SrvccState.COMPLETED) {
            throw new CallStateException(
                    MtkCallStateException.ERROR_INVALID_DURING_SRVCC, "cannot hangup call: SRVCC");
        }
        /// @}

        if (call.getConnections().size() == 0) {
            throw new CallStateException("no connections");
        }

        ImsCall imsCall = call.getImsCall();
        boolean rejectCall = false;

        if (call == mRingingCall) {
            if (Phone.DEBUG_PHONE) log("(ringing) hangup incoming");
            rejectCall = true;
        } else if (call == mForegroundCall) {
            if (call.isDialingOrAlerting()) {
                if (Phone.DEBUG_PHONE) {
                    log("(foregnd) hangup dialing or alerting...");
                }
            } else {
                if (Phone.DEBUG_PHONE) {
                    log("(foregnd) hangup foreground");
                }
                //held call will be resumed by onCallTerminated
            }
        } else if (call == mBackgroundCall) {
            if (Phone.DEBUG_PHONE) {
                log("(backgnd) hangup waiting or background");
            }
        } else {
            throw new CallStateException ("ImsPhoneCall " + call +
                    "does not belong to ImsPhoneCallTracker " + this);
        }
        call.onHangupLocal();

        try {
            if (imsCall != null) {
                if (rejectCall) {
                    imsCall.reject(getHangupReasionInfo(reason));
                    mMetrics.writeOnImsCommand(mPhone.getPhoneId(), imsCall.getSession(),
                            ImsCommand.IMS_CMD_REJECT);
                } else {
                    imsCall.terminate(getHangupReasionInfo(reason));
                    mMetrics.writeOnImsCommand(mPhone.getPhoneId(), imsCall.getSession(),
                            ImsCommand.IMS_CMD_TERMINATE);
                }
            } else if (mPendingMO != null && call == mForegroundCall) {
                // is holding a foreground call
                mPendingMO.update(null, ImsPhoneCall.State.DISCONNECTED);
                mPendingMO.onDisconnect();
                removeConnection(mPendingMO);
                mPendingMO = null;
                updatePhoneState();
                removeMessages(EVENT_DIAL_PENDINGMO);
            }
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }

        mPhone.notifyPreciseCallStateChanged();
    }

    @Override
    protected void callEndCleanupHandOverCallIfAny() {
        if (mHandoverCall.mConnections.size() > 0) {
            if (DBG) log("callEndCleanupHandOverCallIfAny, mHandoverCall.mConnections="
                    + mHandoverCall.mConnections);
            /// M: ALPS01979162 make sure all connections of mHandoverCall are removed from
            /// mConnections to prevent leak @{
            for (Connection conn : mHandoverCall.mConnections) {
                log("SRVCC: remove connection=" + conn);
                removeConnection((ImsPhoneConnection) conn);
            }
            /// @}
            mHandoverCall.mConnections.clear();
            mConnections.clear();
            mState = PhoneConstants.State.IDLE;

            /// M: ALPS02192901. @{
            if (mPhone != null && mPhone.mDefaultPhone != null
                    && mPhone.mDefaultPhone.getState() == PhoneConstants.State.IDLE) {
                // If the call is disconnected before GSMPhone poll calls, the phone state of
                // GSMPhone keeps in idle state, so it will not notify phone state changed. In this
                // case, ImsPhone needs to notify by itself.
                log("SRVCC: notify ImsPhone state as idle.");
                mPhone.notifyPhoneStateChanged();
                /// M: ALPS03905309
                mCallsDisconnectedDuringSrvccRegistrants.notifyRegistrants(
                    getCallStateChangeAsyncResult());
            }
            /// @}
        }
    }

    @Override
    public void sendUSSD (String ussdString, Message response) {
        if (DBG) log("sendUSSD");

        try {
            if (mUssdSession != null) {
                mUssdSession.sendUssd(ussdString);
                AsyncResult.forMessage(response, null, null);
                response.sendToTarget();
                return;
            }

            if (mImsManager == null) {
                mPhone.sendErrorResponse(response, getImsManagerIsNullException());
                return;
            }

            String[] callees = new String[] { ussdString };
            ImsCallProfile profile = mImsManager.createCallProfile(ImsCallProfile
                    .SERVICE_TYPE_NORMAL, ImsCallProfile.CALL_TYPE_VOICE);
            profile.setCallExtraInt(ImsCallProfile.EXTRA_DIALSTRING,
                    ImsCallProfile.DIALSTRING_USSD);

            DigitsUssdManager digitsUssdManager = OpTelephonyCustomizationUtils
                    .getOpFactory(mPhone.getContext()).makeDigitsUssdManager();
            mDigitsUtil.putDialFrom(digitsUssdManager.getUssdExtra(), profile);

            /// M: Assign return message for UE initiated USSI @{
            mPendingUssd = response;
            /// @}
            mUssdSession = mImsManager.makeCall(profile, callees, mMtkImsUssdListener);
        } catch (ImsException e) {
            loge("sendUSSD : " + e);
            mPhone.sendErrorResponse(response, e);
            retryGetImsService();
        }
    }

    @Override
    protected synchronized void addConnection(ImsPhoneConnection conn) {
        super.addConnection(conn);

        if (conn.isEmergency()) {
            //M: RTT
            mOpCommonImsPhoneCallTracker.stopRttEmcGuardTimer();
        }
    }

    @Override
    protected void processCallStateChange(ImsCall imsCall, ImsPhoneCall.State state, int cause,
                                        boolean ignoreState) {
        super.processCallStateChange(imsCall, state, cause, ignoreState);
        /// M: ALPS02136981. Prints debug logs for ImsPhone.
        ImsPhoneConnection conn = findConnection(imsCall);
        logDebugMessagesWithDumpFormat("CC", conn, "");
        //M: for RTT, need to do it after updateMediaCapabilities(imsCall)
        checkRttCallType();
    }

    @Override
    public int getDisconnectCauseFromReasonInfo(ImsReasonInfo reasonInfo, Call.State callState) {
        int code = maybeRemapReasonCode(reasonInfo);
        switch (code) {
            /// M: @{
            case MtkImsReasonInfo.CODE_SIP_REDIRECTED_EMERGENCY:
                return MtkDisconnectCause.IMS_EMERGENCY_REREG;
            case MtkImsReasonInfo.CODE_SIP_WIFI_SIGNAL_LOST:
                return MtkDisconnectCause.WFC_WIFI_SIGNAL_LOST;
            case MtkImsReasonInfo.CODE_SIP_WFC_ISP_PROBLEM:
                return MtkDisconnectCause.WFC_ISP_PROBLEM;
            case MtkImsReasonInfo.CODE_SIP_HANDOVER_WIFI_FAIL:
                return MtkDisconnectCause.WFC_HANDOVER_WIFI_FAIL;
            case MtkImsReasonInfo.CODE_SIP_HANDOVER_LTE_FAIL:
                return MtkDisconnectCause.WFC_HANDOVER_LTE_FAIL;
            case MtkImsReasonInfo.CODE_WFC_BAD_RSSI:
                return MtkDisconnectCause.WFC_CALL_DROP_BAD_RSSI;
            case MtkImsReasonInfo.CODE_WFC_WIFI_BACKHAUL_CONGESTION:
                return MtkDisconnectCause.WFC_CALL_DROP_BACKHAUL_CONGESTION;
            case MtkImsReasonInfo.CODE_SIP_503_ECC_OVER_WIFI_UNSUPPORTED:
                return MtkDisconnectCause.ECC_OVER_WIFI_UNSUPPORTED;
            case MtkImsReasonInfo.CODE_SIP_403_WFC_UNAVAILABLE_IN_CURRENT_LOCATION:
                return MtkDisconnectCause.WFC_UNAVAILABLE_IN_CURRENT_LOCATION;
            // For telcel requirement
            case MtkImsReasonInfo.CODE_SIP_301_MOVED_PERMANENTLY:
                return MtkDisconnectCause.CAUSE_MOVED_PERMANENTLY;
            case MtkImsReasonInfo.CODE_SIP_400_BAD_REQUEST:
                return MtkDisconnectCause.CAUSE_BAD_REQUEST;
            case MtkImsReasonInfo.CODE_SIP_401_UNAUTHORIZED:
                return MtkDisconnectCause.CAUSE_UNAUTHORIZED;
            case MtkImsReasonInfo.CODE_SIP_402_PAYMENT_REQUIRED:
                return MtkDisconnectCause.CAUSE_PAYMENT_REQUIRED;
            case MtkImsReasonInfo.CODE_SIP_403_FORBIDDEN:
                return MtkDisconnectCause.CAUSE_FORBIDDEN;
            case MtkImsReasonInfo.CODE_SIP_404_NOT_FOUND:
                return MtkDisconnectCause.CAUSE_NOT_FOUND;
            case MtkImsReasonInfo.CODE_SIP_405_METHOD_NOT_ALLOWED:
                return MtkDisconnectCause.CAUSE_METHOD_NOT_ALLOWED;
            case MtkImsReasonInfo.CODE_SIP_406_NOT_ACCEPTABLE:
                return MtkDisconnectCause.CAUSE_NOT_ACCEPTABLE;
            case MtkImsReasonInfo.CODE_SIP_407_PROXY_AUTHENTICATION_REQUIRED:
                return MtkDisconnectCause.CAUSE_PROXY_AUTHENTICATION_REQUIRED;
            case MtkImsReasonInfo.CODE_SIP_408_REQUEST_TIMEOUT:
                return MtkDisconnectCause.CAUSE_REQUEST_TIMEOUT;
            case MtkImsReasonInfo.CODE_SIP_409_CONFLICT:
                return MtkDisconnectCause.CAUSE_CONFLICT;
            case MtkImsReasonInfo.CODE_SIP_410_GONE:
                return MtkDisconnectCause.CAUSE_GONE;
            case MtkImsReasonInfo.CODE_SIP_411_LENGTH_REQUIRED:
                return MtkDisconnectCause.CAUSE_LENGTH_REQUIRED;
            case MtkImsReasonInfo.CODE_SIP_413_REQUEST_ENTRY_TOO_LONG:
                return MtkDisconnectCause.CAUSE_REQUEST_ENTRY_TOO_LONG;
            case MtkImsReasonInfo.CODE_SIP_414_REQUEST_URI_TOO_LONG:
                return MtkDisconnectCause.CAUSE_REQUEST_URI_TOO_LONG;
            case MtkImsReasonInfo.CODE_SIP_415_UNSUPPORTED_MEDIA_TYPE:
                return MtkDisconnectCause.CAUSE_UNSUPPORTED_MEDIA_TYPE;
            case MtkImsReasonInfo.CODE_SIP_416_UNSUPPORTED_URI_SCHEME:
                return MtkDisconnectCause.CAUSE_UNSUPPORTED_URI_SCHEME;
            case MtkImsReasonInfo.CODE_SIP_420_BAD_EXTENSION:
                return MtkDisconnectCause.CAUSE_BAD_EXTENSION;
            case MtkImsReasonInfo.CODE_SIP_421_BAD_EXTENSION_REQUIRED:
                return MtkDisconnectCause.CAUSE_EXTENSION_REQUIRED;
            case MtkImsReasonInfo.CODE_SIP_423_INTERVAL_TOO_BRIEF:
                return MtkDisconnectCause.CAUSE_INTERVAL_TOO_BRIEF;
            case MtkImsReasonInfo.CODE_SIP_480_TEMPORARILY_UNAVAILABLE:
                return MtkDisconnectCause.CAUSE_TEMPORARILY_UNAVAILABLE;
            case MtkImsReasonInfo.CODE_SIP_481_CALL_TRANSACTION_NOT_EXIST:
                return MtkDisconnectCause.CAUSE_CALL_TRANSACTION_NOT_EXIST;
            case MtkImsReasonInfo.CODE_SIP_482_LOOP_DETECTED:
                return MtkDisconnectCause.CAUSE_LOOP_DETECTED;
            case MtkImsReasonInfo.CODE_SIP_483_TOO_MANY_HOPS:
                return MtkDisconnectCause.CAUSE_TOO_MANY_HOPS;
            case MtkImsReasonInfo.CODE_SIP_484_TOO_ADDRESS_INCOMPLETE:
                return MtkDisconnectCause.CAUSE_ADDRESS_INCOMPLETE;
            case MtkImsReasonInfo.CODE_SIP_485_AMBIGUOUS:
                return MtkDisconnectCause.CAUSE_AMBIGUOUS;
            case MtkImsReasonInfo.CODE_SIP_486_BUSY_HERE:
                return MtkDisconnectCause.CAUSE_BUSY_HERE;
            case MtkImsReasonInfo.CODE_SIP_487_REQUEST_TERMINATED:
                return MtkDisconnectCause.CAUSE_REQUEST_TERMINATED;
            case MtkImsReasonInfo.CODE_SIP_488_NOT_ACCEPTABLE_HERE:
                return MtkDisconnectCause.CAUSE_NOT_ACCEPTABLE_HERE;
            case MtkImsReasonInfo.CODE_SIP_500_SERVER_INTERNAL_ERROR:
                return MtkDisconnectCause.CAUSE_SERVER_INTERNAL_ERROR;
            case MtkImsReasonInfo.CODE_SIP_501_NOT_IMPLEMENTED:
                return MtkDisconnectCause.CAUSE_NOT_IMPLEMENTED;
            case MtkImsReasonInfo.CODE_SIP_502_BAD_GATEWAY:
                return MtkDisconnectCause.CAUSE_BAD_GATEWAY;
            case MtkImsReasonInfo.CODE_SIP_503_SERVICE_UNAVAILABLE:
                return MtkDisconnectCause.CAUSE_SERVICE_UNAVAILABLE;
            case MtkImsReasonInfo.CODE_SIP_504_GATEWAY_TIMEOUT:
                return MtkDisconnectCause.CAUSE_GATEWAY_TIMEOUT;
            case MtkImsReasonInfo.CODE_SIP_505_VERSION_NOT_SUPPORTED:
                return MtkDisconnectCause.CAUSE_VERSION_NOT_SUPPORTED;
            case MtkImsReasonInfo.CODE_SIP_513_MESSAGE_TOO_LONG:
                return MtkDisconnectCause.CAUSE_MESSAGE_TOO_LONG;
            case MtkImsReasonInfo.CODE_SIP_600_BUSY_EVERYWHERE:
                return MtkDisconnectCause.CAUSE_BUSY_EVERYWHERE;
            case MtkImsReasonInfo.CODE_SIP_603_DECLINE:
                return MtkDisconnectCause.CAUSE_DECLINE;
            case MtkImsReasonInfo.CODE_SIP_604_DOES_NOT_EXIST_ANYWHERE:
                return MtkDisconnectCause.CAUSE_DOES_NOT_EXIST_ANYWHERE;
            case MtkImsReasonInfo.CODE_SIP_606_NOT_ACCEPTABLE:
                return MtkDisconnectCause.CAUSE_SESSION_NOT_ACCEPTABLE;
            case MtkImsReasonInfo.CODE_NO_COVERAGE:
                return MtkDisconnectCause.INCOMING_REJECTED_NO_COVERAGE;
            case MtkImsReasonInfo.CODE_FORWARD:
                return MtkDisconnectCause.INCOMING_REJECTED_FORWARD;
            case MtkImsReasonInfo.CODE_LOW_BATTERY:
                return MtkDisconnectCause.INCOMING_REJECTED_LOW_BATTERY;
            case MtkImsReasonInfo.CODE_SPECIAL_HANGUP:
                return MtkDisconnectCause.INCOMING_REJECTED_SPECIAL_HANGUP;
            /// @}
            default:
                break;
        }
        return super.getDisconnectCauseFromReasonInfo(reasonInfo, callState);
    }

    /**
     * Listen to the IMS call state change
     */
    private ImsCall.Listener mMtkImsCallListener = new MtkImsCall.Listener() {
        @Override
        public void onCallProgressing(ImsCall imsCall) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onCallProgressing(imsCall);
            ImsPhoneConnection conn = findConnection(imsCall);
            if (conn != null) {
                conn.onConnectionEvent(
                        mediatek.telecom.MtkConnection.EVENT_CALL_ALERTING_NOTIFICATION, null);
            }
        }

        @Override
        public void onCallStarted(ImsCall imsCall) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onCallStarted(imsCall);
        }

        @Override
        public void onCallUpdated(ImsCall imsCall) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onCallUpdated(imsCall);
        }

        /**
         * onCallStartFailed will be invoked when:
         * case 1) Dialing fails
         * case 2) Ringing call is disconnected by local or remote user
         */
        @Override
        public void onCallStartFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onCallStartFailed(imsCall, reasonInfo);
            /// M: Auto resume background call if start failed. @{
            if (mBackgroundCall.getState() == ImsPhoneCall.State.HOLDING) {
                log("auto resume holding call");
                setPendingResumeRequest(true);
                sendEmptyMessage(EVENT_RESUME_BACKGROUND);
            }
            /// @}
        }

        @Override
        public void onCallTerminated(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onCallTerminated(imsCall, reasonInfo);
        }

        @Override
        public void onCallHeld(ImsCall imsCall) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onCallHeld(imsCall);
        }

        @Override
        public void onCallHoldFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            if (DBG) log("onCallHoldFailed reasonCode=" + reasonInfo.getCode());

            synchronized (mSyncHold) {
                ImsPhoneCall.State bgState = mBackgroundCall.getState();
                if (reasonInfo.getCode() == ImsReasonInfo.CODE_LOCAL_CALL_TERMINATED) {
                    // disconnected while processing hold
                    /// M: ALPS02147333 Although call is held failed, but we treat it like call
                    /// held succeeded if the call has been terminated @{
                    if ((mForegroundCall.getState() == ImsPhoneCall.State.HOLDING)
                            || (mRingingCall.getState() == ImsPhoneCall.State.WAITING)) {
                        if (DBG) {
                            log("onCallHoldFailed resume background");
                        }
                        sendEmptyMessage(EVENT_RESUME_BACKGROUND);
                    } else {
                        if (mPendingMO != null) {
                            dialPendingMO();
                        }
                    }
                    /// @}
                } else if (bgState == ImsPhoneCall.State.ACTIVE
                        || bgState == ImsPhoneCall.State.DISCONNECTING) {
                    mForegroundCall.switchWith(mBackgroundCall);

                    if (mPendingMO != null) {
                        mPendingMO.setDisconnectCause(DisconnectCause.ERROR_UNSPECIFIED);
                        sendEmptyMessageDelayed(EVENT_HANGUP_PENDINGMO, TIMEOUT_HANGUP_PENDINGMO);
                    }
                }
                mPhone.notifySuppServiceFailed(Phone.SuppService.HOLD);
            }
            /// M: ALPS03307614, hold call failed, resume will not process. Reset the flag. @{
            if (mSwitchingFgAndBgCalls) {
                mSwitchingFgAndBgCalls = false;
                mCallExpectedToResume = null;
            }
            /// @}
            mMetrics.writeOnImsCallHoldFailed(mPhone.getPhoneId(), imsCall.getCallSession(),
                    reasonInfo);
        }

        @Override
        public void onCallResumed(ImsCall imsCall) {
            if (DBG) log("onCallResumed");

            ///M: ALPS02577419. @{
            mIsOnCallResumed = true;
            /// @}

            // If we are the in midst of swapping FG and BG calls and the call we end up resuming
            // is not the one we expected, we likely had a resume failure and we need to swap the
            // FG and BG calls back.
            if (mSwitchingFgAndBgCalls) {
                if (imsCall != mCallExpectedToResume) {
                    // If the call which resumed isn't as expected, we need to swap back to the
                    // previous configuration; the swap has failed.
                    if (DBG) {
                        log("onCallResumed : switching " + mForegroundCall + " with "
                                + mBackgroundCall);
                    }
                    mForegroundCall.switchWith(mBackgroundCall);
                } else {
                    // The call which resumed is the one we expected to resume, so we can clear out
                    // the mSwitchingFgAndBgCalls flag.
                    if (DBG) {
                        log("onCallResumed : expected call resumed.");
                    }
                }
                mSwitchingFgAndBgCalls = false;
                mCallExpectedToResume = null;
            }
            /// M: ALPS02023277. @{
            /// Reset this flag since call is resumed.
            /// This flag is used to prevent to accept a waiting call,
            /// during resuming a backdground call.
            mHasPendingResumeRequest = false;
            /// @}
            processCallStateChange(imsCall, ImsPhoneCall.State.ACTIVE,
                    DisconnectCause.NOT_DISCONNECTED);
            mMetrics.writeOnImsCallResumed(mPhone.getPhoneId(), imsCall.getCallSession());

            ///M: ALPS02577419. @{
            mIsOnCallResumed = false;
            /// @}
        }

        @Override
        public void onCallResumeFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            /// M: ALPS02023277. For debug purpose. @{
            if (DBG) {
                log("onCallResumeFailed");
            }
            /// @}

            if (mSwitchingFgAndBgCalls) {
                // If we are in the midst of swapping the FG and BG calls and
                // we got a resume fail, we need to swap back the FG and BG calls.
                // Since the FG call was held, will also try to resume the same.
                if (imsCall == mCallExpectedToResume) {
                    if (DBG) {
                        log("onCallResumeFailed : switching " + mForegroundCall + " with "
                                + mBackgroundCall);
                    }
                    mForegroundCall.switchWith(mBackgroundCall);
                    if (mForegroundCall.getState() == ImsPhoneCall.State.HOLDING) {
                        sendEmptyMessage(EVENT_RESUME_BACKGROUND);
                    }
                }

                //Call swap is done, reset the relevant variables
                mCallExpectedToResume = null;
                mSwitchingFgAndBgCalls = false;
            }
            /// M: ALPS02023277. @{
            /// Reset this flag since resume operation is done.
            /// This flag is used to prevent to accept a waiting call,
            /// during resuming a backdground call.
            mHasPendingResumeRequest = false;
            /// @}
            mPhone.notifySuppServiceFailed(Phone.SuppService.RESUME);
            mMetrics.writeOnImsCallResumeFailed(mPhone.getPhoneId(), imsCall.getCallSession(),
                    reasonInfo);
        }

        @Override
        public void onCallResumeReceived(ImsCall imsCall) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onCallResumeReceived(imsCall);
        }

        @Override
        public void onCallHoldReceived(ImsCall imsCall) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onCallHoldReceived(imsCall);
        }

        @Override
        public void onCallSuppServiceReceived(ImsCall call,
                ImsSuppServiceNotification suppServiceInfo) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onCallSuppServiceReceived(call,
                    suppServiceInfo);
        }

        @Override
        public void onCallMerged(final ImsCall call, final ImsCall peerCall, boolean swapCalls) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onCallMerged(call, peerCall, swapCalls);
            /// M: ALPS02136981. For formatted log, workaround for merge case. @{
            ImsPhoneConnection hostConn = findConnection(call);
            if (hostConn != null && hostConn instanceof MtkImsPhoneConnection) {
                MtkImsPhoneConnection hostConnExt = (MtkImsPhoneConnection)hostConn;
                FormattedLog formattedLog = new FormattedLog.Builder()
                        .setCategory("CC").setServiceName("ImsPhone")
                        .setOpType(FormattedLog.OpType.DUMP)
                        .setCallNumber(hostConn.getAddress())
                        .setCallId(getConnectionCallId(hostConnExt))
                        .setStatusInfo("state", "disconnected")
                        .setStatusInfo("isConfCall", "No")
                        .setStatusInfo("isConfChildCall", "No")
                        .setStatusInfo("parent", hostConnExt.getParentCallName())
                        .buildDumpInfo();

                if (formattedLog != null) {
                    log(formattedLog.toString());
                }
            }
            /// @}
        }

        @Override
        public void onCallMergeFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onCallMergeFailed(call, reasonInfo);
        }

        /**
         * Called when the state of IMS conference participant(s) has changed.
         *
         * @param call the call object that carries out the IMS call.
         * @param participants the participant(s) and their new state information.
         */
        @Override
        public void onConferenceParticipantsStateChanged(ImsCall call,
                List<ConferenceParticipant> participants) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onConferenceParticipantsStateChanged
                    (call, participants);
        }

        @Override
        public void onCallSessionTtyModeReceived(ImsCall call, int mode) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onCallSessionTtyModeReceived(call, mode);
        }

        @Override
        public void onCallHandover(ImsCall imsCall, int srcAccessTech, int targetAccessTech,
                ImsReasonInfo reasonInfo) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onCallHandover(imsCall, srcAccessTech,
                    targetAccessTech, reasonInfo);
        }

        @Override
        public void onCallHandoverFailed(ImsCall imsCall, int srcAccessTech, int targetAccessTech,
                                         ImsReasonInfo reasonInfo) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onCallHandoverFailed(imsCall,
                    srcAccessTech, targetAccessTech, reasonInfo);
        }

        /**
         * Handles a change to the multiparty state for an {@code ImsCall}.  Notifies the associated
         * {@link ImsPhoneConnection} of the change.
         *
         * @param imsCall The IMS call.
         * @param isMultiParty {@code true} if the call became multiparty, {@code false}
         *      otherwise.
         */
        @Override
        public void onMultipartyStateChanged(ImsCall imsCall, boolean isMultiParty) {
            MtkImsPhoneCallTracker.super.mImsCallListener.onMultipartyStateChanged(imsCall,
                    isMultiParty);
        }

        /// M: @{
        @Override
        public void onCallInviteParticipantsRequestDelivered(ImsCall call) {
            if (DBG) {
                log("onCallInviteParticipantsRequestDelivered");
            }

            ImsPhoneConnection conn = findConnection(call);
            if (conn != null && conn instanceof MtkImsPhoneConnection) {
                ((MtkImsPhoneConnection) conn).notifyConferenceParticipantsInvited(true);
            }
        }

        @Override
        public void onCallInviteParticipantsRequestFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            if (DBG) {
                log("onCallInviteParticipantsRequestFailed reasonCode=" +
                        reasonInfo.getCode());
            }
            ImsPhoneConnection conn = findConnection(call);
            if (conn != null && conn instanceof MtkImsPhoneConnection) {
                ((MtkImsPhoneConnection) conn).notifyConferenceParticipantsInvited(false);
            }
        }

        @Override
        public void onCallTransferred(ImsCall imsCall) {
            if (DBG) {
                log("onCallTransferred");
            }
        }

        @Override
        public void onCallTransferFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            if (DBG) {
                log("onCallTransferFailed");
            }

            mPhone.notifySuppServiceFailed(Phone.SuppService.TRANSFER);
        }

        @Override
        public void onTextCapabilityChanged(ImsCall call, int localCapability,
                int remoteCapability, int localTextStatus, int realRemoteCapability) {
            ImsPhoneConnection conn = findConnection(call);

            mOpCommonImsPhoneCallTracker.onTextCapabilityChanged(conn, localCapability, remoteCapability,
                    localTextStatus, realRemoteCapability);

            checkRttCallType();
        }

        @Override
        public void onRttEventReceived(ImsCall call, int event) {
            ImsPhoneConnection conn = findConnection(call);
            mOpCommonImsPhoneCallTracker.onRttEventReceived(conn, event);
        }
        /// @}

        @Override
        public void onRttModifyResponseReceived(ImsCall imsCall, int status) {
            log("onRttModifyResponseReceived : " + status);
            ImsPhoneConnection conn = findConnection(imsCall);
            if (conn != null) {
                if (status ==
                        android.telecom.Connection.RttModifyStatus.SESSION_MODIFY_REQUEST_SUCCESS) {

                    conn.onRttModifyResponseReceived(status);
                    processRttModifySuccessCase(imsCall, status, conn);
                    checkRttCallType();
                    conn.startRttTextProcessing();
                } else {
                    processRttModifyFailCase(imsCall, status, conn);
                }
            }
        }

        @Override
        public void onRttMessageReceived(ImsCall imsCall, String message) {
            log("onRttMessageReceived : " + message);
            MtkImsPhoneCallTracker.super.mImsCallListener.onRttMessageReceived(imsCall, message);
        }

        @Override
        public void onRttModifyRequestReceived(ImsCall imsCall) {
            log("onRttModifyRequestReceived");
            MtkImsPhoneCallTracker.super.mImsCallListener.onRttModifyRequestReceived(imsCall);
        }

        @Override
        public void onCallDeviceSwitched(ImsCall call) {
            if (DBG) {
                log("onCallDeviceSwitched");
            }
            ImsPhoneConnection conn = findConnection(call);
            if (conn != null && conn instanceof MtkImsPhoneConnection) {
                ((MtkImsPhoneConnection) conn).notifyDeviceSwitched(true);
            }
        }

        @Override
        public void onCallDeviceSwitchFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            if (DBG) {
                log("onCallDeviceSwitchFailed");
            }
            ImsPhoneConnection conn = findConnection(call);
            if (conn != null && conn instanceof MtkImsPhoneConnection) {
                ((MtkImsPhoneConnection) conn).notifyDeviceSwitched(false);
            }
        }

        @Override
        public void onCallRedialEcc(ImsCall call) {
            if (DBG) {
                log("onCallRedialEcc");
            }
            ImsPhoneConnection conn = findConnection(call);
            if (conn != null && conn instanceof MtkImsPhoneConnection) {
                ((MtkImsPhoneConnection) conn).notifyRedialEcc();
            }
        }
    };

    /**
     * Listen to the IMS call state change
     */
    private ImsCall.Listener mMtkImsUssdListener = new ImsCall.Listener() {
        @Override
        public void onCallStarted(ImsCall imsCall) {
            if (DBG) log("mMtkImsUssdListener onCallStarted");

            if (imsCall == mUssdSession) {
                if (mPendingUssd != null) {
                    AsyncResult.forMessage(mPendingUssd);
                    mPendingUssd.sendToTarget();
                    mPendingUssd = null;
                }
            }
        }

        @Override
        public void onCallStartFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            if (DBG) log("mMtkImsUssdListener onCallStartFailed reasonCode=" + reasonInfo.getCode());

            onCallTerminated(imsCall, reasonInfo);
        }

        @Override
        public void onCallTerminated(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            if (DBG) log("mMtkImsUssdListener onCallTerminated reasonCode=" + reasonInfo.getCode());
            removeMessages(EVENT_CHECK_FOR_WIFI_HANDOVER);

            if (imsCall == mUssdSession) {
                mUssdSession = null;
                if (mPendingUssd != null) {
                    CommandException ex =
                            new CommandException(CommandException.Error.GENERIC_FAILURE);
                    AsyncResult.forMessage(mPendingUssd, null, ex);
                    mPendingUssd.sendToTarget();
                    mPendingUssd = null;
                }
            }
            imsCall.close();
        }

        @Override
        public void onCallUssdMessageReceived(ImsCall call,
                                              int mode, String ussdMessage) {
            if (DBG) log("mMtkImsUssdListener onCallUssdMessageReceived mode=" + mode);

            // [ALPS04423740] When USSD mode is -2, no need to CSFB. So the original ussdMode
            // should not be overridden here.
            // int ussdMode = -1;
            int ussdMode = mode;

            switch(mode) {
                case ImsCall.USSD_MODE_REQUEST:
                    ussdMode = CommandsInterface.USSD_MODE_REQUEST;
                    break;

                case ImsCall.USSD_MODE_NOTIFY:
                    ussdMode = CommandsInterface.USSD_MODE_NOTIFY;
                    /// M: After USSI notify, close this USSI session. @{
                    if (call == mUssdSession) {
                        mUssdSession = null;
                        call.close();
                    }
                    /// @}
                    break;
                /// M: Clear USSI session when any error from MD @{
                default:
                    if (call == mUssdSession) {
                        log("invalid mode: " + mode + ", clear ussi session");
                        mUssdSession = null;
                        call.close();
                    }
                    break;
                ///@}
            }

            ((MtkImsPhone) mPhone).onIncomingUSSD(ussdMode, ussdMessage);
        }
    };

    @Override
    protected void notifySrvccState(Call.SrvccState state) {

        //M: check SRVCC and CSFB for RTT
        if (state == Call.SrvccState.COMPLETED) {
            mOpCommonImsPhoneCallTracker.sendRttSrvccOrCsfbEvent(mForegroundCall);
            mOpCommonImsPhoneCallTracker.sendRttSrvccOrCsfbEvent(mBackgroundCall);
            mOpCommonImsPhoneCallTracker.sendRttSrvccOrCsfbEvent(mRingingCall);
        }
        super.notifySrvccState(state);
        //M: for RTT
        if (mSrvccState == Call.SrvccState.COMPLETED) {
            checkRttCallType();
        }
    }

    @Override
    protected void releasePendingMOIfRequired() {
        if (mPendingMO == null) {
            return;
        }
        mPendingMO.setDisconnectCause(DisconnectCause.ERROR_UNSPECIFIED);
        sendEmptyMessageDelayed(EVENT_HANGUP_PENDINGMO, TIMEOUT_HANGUP_PENDINGMO);
    }

    protected void transferHandoverConnections(ImsPhoneCall call) {
        //M: reset the hold tone. @{
        if (DBG) log("transferHandoverConnections mSrvccState:" + mSrvccState);
        if (mSrvccState == Call.SrvccState.COMPLETED) {
            if (call.mConnections != null) {
                for (Connection conn : call.mConnections) {
                    if (mOnHoldToneStarted && conn != null
                            && mOnHoldToneId == System.identityHashCode(conn)) {
                        if (DBG) log("transferHandoverConnections reset the hold tone.");
                        mPhone.stopOnHoldTone(conn);
                        mOnHoldToneStarted = false;
                        mOnHoldToneId = -1;
                    }
                }
            }
        }
        super.transferHandoverConnections(call);
        //@}
    }

    //****** Overridden from Handler

    @Override
    public void
    handleMessage (Message msg) {
        AsyncResult ar;
        if (DBG) log("handleMessage what=" + msg.what);

        switch (msg.what) {
             case EVENT_DIAL_PENDINGMO:
                /// M: ALPS02675166 @{
                /// Check if pendingMO have been already dialed, should not dial twice.
                // dialInternal(mPendingMO, mClirMode,
                //         mPendingCallVideoState, mPendingIntentExtras);
                // mPendingIntentExtras = null;
                if (mPendingMO != null && mPendingMO.getImsCall() == null) {
                    dialInternal(mPendingMO, mClirMode,
                            mPendingCallVideoState, mPendingIntentExtras);
                    mPendingIntentExtras = null;
                }
                /// @}
                break;

             case EVENT_ROAMING_ON:
                 onDataRoamingOn();
                 break;

             case EVENT_ROAMING_OFF:
                 onDataRoamingOff();
                 break;

             case EVENT_ROAMING_SETTING_CHANGE:
                 onRoamingSettingsChanged();
                 break;

            default:
                super.handleMessage(msg);
                break;
        }
    }

    /// M: MTK added functions @{
    /// For ACTION_IMS_INCOMING_CALL_INDICATION, mIndicationReceiver is
    /// responsible to handle it

    private IncomingCallEventRecevier mIndicationReceiver = new IncomingCallEventRecevier();
    public class IncomingCallEventRecevier extends BroadcastReceiver implements MtkIncomingCallChecker.OnCheckCompleteListener {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MtkImsManager.ACTION_IMS_INCOMING_CALL_INDICATION)) {
                if (DBG) {
                    log("onReceive() indication call intent");
                }

                if (mImsManager == null) {
                    if (DBG) {
                        log("onReceive() no ims manager");
                    }
                    return;
                }

                boolean isAllow = true; /// default value is always allowed to take call
                int phoneId = intent.getIntExtra(MtkImsManager.EXTRA_PHONE_ID, -1);
                int subId = mPhone.getSubId();
                int rejectCause = MtkDisconnectCause.INCOMING_MISSED;
                String number = intent.getStringExtra(MtkImsManager.EXTRA_DIAL_STRING);

                if (DBG) {
                    log("onReceive() : subId = " + subId + ", number =" + number + ", phoneId = "+ phoneId);
                }

                if (phoneId != mPhone.getPhoneId()) {
                    return;
                }

                // check unregister number & number in black list
                mIncomingCallCheker = new MtkIncomingCallChecker("ims_call_pre_check", intent);

                boolean bCheckStart = mIncomingCallCheker.startIncomingCallNumberCheck(
                                                        mPhone.getContext(),
                                                        subId,
                                                        number,
                                                        this);

                if (bCheckStart) {
                    log("onReceive() startIncomingCallNumberCheck true. start check ");
                    return;

                } else {
                    log("onReceive() startIncomingCallNumberCheck false, and flow continues");
                }

                if (DBG) {
                    log("setCallIndication : intent = " + intent + ", isAllow = " + isAllow + ", cause = " + rejectCause);
                }

                try {
                    if (mImsManager instanceof MtkImsManager) {
                        ((MtkImsManager) mImsManager).setCallIndication(phoneId, intent, isAllow, rejectCause);
                    }
                } catch (ImsException e) {
                    loge("setCallIndication ImsException " + e);
                }
            }
        }

        @Override
        public void onCheckComplete(boolean result, Object obj) {

            int rejectCause = MtkDisconnectCause.INCOMING_MISSED;
            boolean isAllow = true;
            Intent intent = (Intent) obj;
            int phoneId = -1;

            if (intent != null) {
                phoneId = intent.getIntExtra(MtkImsManager.EXTRA_PHONE_ID, -1);
            }

            if (result) {
                rejectCause = MtkDisconnectCause.INCOMING_REJECTED;
                isAllow = false;
            }

            if (DBG) {
                log("onCheckComplete(): intent = " + intent + ", isAllow = " + isAllow + ", cause = " + rejectCause);
            }

            try {
                if (mImsManager instanceof MtkImsManager) {
                    ((MtkImsManager) mImsManager).setCallIndication(phoneId, intent, isAllow, rejectCause);
                }
            } catch (ImsException e) {
                loge("onCheckComplete() ImsException " + e);
            }
        }
    };

    private void registerIndicationReceiver() {
        if (DBG) {
            log("registerIndicationReceiver");
        }
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction(MtkImsManager.ACTION_IMS_INCOMING_CALL_INDICATION);
        mPhone.getContext().registerReceiver(mIndicationReceiver, intentfilter);

    }
    private void unregisterIndicationReceiver() {
        if (DBG) {
            log("unregisterIndicationReceiver");
        }
        mPhone.getContext().unregisterReceiver(mIndicationReceiver);
    }

    // For VoLTE enhanced conference call.
    /* package */
    Connection dial(List<String> numbers, int videoState) throws CallStateException {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mPhone.getContext());
        int oirMode = sp.getInt(Phone.CLIR_KEY + mPhone.getPhoneId(),
                CommandsInterface.CLIR_DEFAULT);
        return dial(numbers, oirMode, videoState);
    }

    // For VoLTE enhanced conference call.
    /* package */
    synchronized Connection
    dial(List<String> numbers, int clirMode, int videoState) throws CallStateException {
        if (DBG) {
            log("dial clirMode=" + clirMode);
        }

        // note that this triggers call state changed notif
        clearDisconnected();

        if (mImsManager == null) {
            throw new CallStateException("service not available");
        }

        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
        }

        boolean holdBeforeDial = false;

        // The new call must be assigned to the foreground call.
        // That call must be idle, so place anything that's
        // there on hold
        if (mForegroundCall.getState() == ImsPhoneCall.State.ACTIVE) {
            if (mBackgroundCall.getState() != ImsPhoneCall.State.IDLE) {
                //we should have failed in !canDial() above before we get here
                throw new CallStateException("cannot dial in current state");
            }
            // foreground call is empty for the newly dialed connection
            holdBeforeDial = true;
            switchWaitingOrHoldingAndActive();
        }

        ImsPhoneCall.State fgState = ImsPhoneCall.State.IDLE;
        ImsPhoneCall.State bgState = ImsPhoneCall.State.IDLE;

        mClirMode = clirMode;
        ImsPhoneConnection conn;
        synchronized (mSyncHold) {
            if (holdBeforeDial) {
                fgState = mForegroundCall.getState();
                bgState = mBackgroundCall.getState();

                //holding foreground call failed
                if (fgState == ImsPhoneCall.State.ACTIVE) {
                    throw new CallStateException("cannot dial in current state");
                }

                //holding foreground call succeeded
                if (bgState == ImsPhoneCall.State.HOLDING) {
                    holdBeforeDial = false;
                }
            }

            // Create IMS conference host connection
            // M: keep the host connection or mPendingMO might be set to null
            // if the callStartFailed immediately.
            conn = new MtkImsPhoneConnection(mPhone, "",
                    this, mForegroundCall, false);

            mPendingMO = conn;

            ArrayList<String> dialStrings = new ArrayList<String>();
            for (String str : numbers) {
                if (!PhoneNumberUtils.isUriNumber(str)) {
                    dialStrings.add(PhoneNumberUtils.extractNetworkPortionAlt(str));
                } else {
                    dialStrings.add(str);
                }
            }
            ((MtkImsPhoneConnection) mPendingMO).setConfDialStrings(dialStrings);
        }
        addConnection(mPendingMO);

        // ALPS02136981. Prints debug logs for ImsPhone.
        StringBuilder sb = new StringBuilder();
        for (String number : numbers) {
            sb.append(number);
            sb.append(", ");
        }
        logDebugMessagesWithOpFormat("CC", "DialConf", mPendingMO, " numbers=" + sb.toString());
        logDebugMessagesWithDumpFormat("CC", mPendingMO, "");

        if (!holdBeforeDial) {
            dialInternal(mPendingMO, clirMode, videoState, null);
        }

        updatePhoneState();
        mPhone.notifyPreciseCallStateChanged();

        return conn;
    }

    /* package */
    void hangupAll() throws CallStateException {
        if (DBG) {
            log("hangupAll");
        }

        if (mImsManager == null || !(mImsManager instanceof MtkImsManager)) {
            throw new CallStateException("No MtkImsManager Instance");
        }

        try {
            ((MtkImsManager)mImsManager).hangupAllCall(mPhone.getPhoneId());
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }

        if (!mRingingCall.isIdle()) {
            setCallTerminationFlag(mRingingCall);
            mRingingCall.onHangupLocal();
        }
        if (!mForegroundCall.isIdle()) {
            setCallTerminationFlag(mForegroundCall);
            mForegroundCall.onHangupLocal();
        }
        if (!mBackgroundCall.isIdle()) {
            setCallTerminationFlag(mBackgroundCall);
            mBackgroundCall.onHangupLocal();
        }
    }

    // ALPS04018607. Hang up all during merge will update call state failed without set this flag.
    private void setCallTerminationFlag(ImsPhoneCall imsPhoneCall) {
        if (DBG) {
            log("setCallTerminationFlag");
        }

        ImsCall imsCall = imsPhoneCall.getImsCall();
        if (imsCall == null) {
            log("setCallTerminationFlag " + imsPhoneCall + " no ims call");
            return;
        }

        ((MtkImsCall)imsCall).setTerminationRequestFlag(true);
    }

    // ALPS02136981. Prints debug logs for ImsPhone.
    /**
     * Logs unified debug log messages for "OP".
     * Format: [category][Module][OP][Action][call-number][local-call-ID] Msg. String.
     * P.S. uses the RIL call ID as the local call ID.
     *
     * @param category currently we only have 'CC' category.
     * @param action the action name. (e.q. Dial, Hold, etc.)
     * @param conn the connection instance.
     * @param msg the optional messages
     * @hide
     */
    @Override
    protected void logDebugMessagesWithOpFormat(
            String category, String action, ImsPhoneConnection conn, String msg) {
        if (category == null || action == null || conn == null) {
            // return if no mandatory tags.
            return;
        }
        MtkImsPhoneConnection connExt = (MtkImsPhoneConnection)conn;
        FormattedLog formattedLog = new FormattedLog.Builder()
                .setCategory(category)
                .setServiceName("ImsPhone")
                .setOpType(FormattedLog.OpType.OPERATION)
                .setActionName(action)
                .setCallNumber(getCallNumber(conn))
                .setCallId(getConnectionCallId(connExt))
                .setExtraMessage(msg)
                .buildDebugMsg();

        if (formattedLog != null) {
            if (!SENLOG || TELDBG) {
                log(formattedLog.toString());
            }
        }
    }

    /**
     * Logs unified debug log messages, for "Dump".
     * format: [CC][Module][Dump][call-number][local-call-ID]-[name:value],[name:value]-Msg. String
     * P.S. uses the RIL call ID as the local call ID.
     *
     * @param category currently we only have 'CC' category.
     * @param conn the ImsPhoneConnection to be dumped.
     * @param msg the optional messages
     * @hide
     */
    @Override
    protected void logDebugMessagesWithDumpFormat(String category, ImsPhoneConnection conn,
            String msg) {
        if (category == null || conn == null || !(conn instanceof MtkImsPhoneConnection)) {
            // return if no mandatory tags.
            return;
        }
        MtkImsPhoneConnection connExt = (MtkImsPhoneConnection)conn;
        FormattedLog formattedLog = new FormattedLog.Builder()
                .setCategory("CC")
                .setServiceName("ImsPhone")
                .setOpType(FormattedLog.OpType.DUMP)
                .setCallNumber(getCallNumber(conn))
                .setCallId(getConnectionCallId(connExt))
                .setExtraMessage(msg)
                .setStatusInfo("state", conn.getState().toString())
                .setStatusInfo("isConfCall", conn.isMultiparty() ? "Yes" : "No")
                .setStatusInfo("isConfChildCall", "No")
                .setStatusInfo("parent", connExt.getParentCallName())
                .buildDumpInfo();

        if (formattedLog != null) {
            if (!SENLOG || TELDBG) {
                log(formattedLog.toString());
            }
        }
    }

    /**
     * get call ID of the imsPhoneConnection. (the same as RIL code ID)
     *
     * @param conn imsPhoneConnection
     * @return call ID string.
     * @hide
     */
    private String getConnectionCallId(MtkImsPhoneConnection conn) {
        if (conn == null) {
            return "";
        }

        int callId = conn.getCallId();
        if (callId == -1) {
            callId = conn.getCallIdBeforeDisconnected();
            if (callId == -1) {
                return "";
            }
        }
        return String.valueOf(callId);
    }

    /**
     * get call number of the imsPhoneConnection.
     *
     * @param conn imsPhoneConnection
     * @return call ID number.
     * @hide
     */
    private String getCallNumber(ImsPhoneConnection conn) {
        if (conn == null) {
            return null;
        }

        if (conn.isMultiparty()) {
            return "conferenceCall";
        } else {
            return conn.getAddress();
        }
    }

    // ALPS02495477: API for unhold specific connection and call.
    /*package*/ void
    unhold (ImsPhoneConnection conn) throws CallStateException {
        if (DBG) {
            log("unhold connection, is in switching? : "  + mSwitchingFgAndBgCalls);
        }

        if (conn.getOwner() != this) {
            throw new CallStateException ("ImsPhoneConnection " + conn
                    + "does not belong to MtkImsPhoneCallTracker " + this);
        }

        if (mSwitchingFgAndBgCalls == false) {
            unhold(conn.getCall());
        }
    }

    private void unhold (ImsPhoneCall call) throws CallStateException {
        if (DBG) {
            log("unhold call, is in switching? : " + mSwitchingFgAndBgCalls);
        }

        if (call.getConnections().size() == 0) {
            throw new CallStateException("no connections");
        }
        ///M: ALPS02577419. @{
        if (mIsOnCallResumed) {
            if (DBG) {
                log("unhold call: drop unhold, an call is processing onCallResumed");
            }
            return;
        }
        /// @}

        try {
            // if call is background call, switch to foreground
            if (call == mBackgroundCall) {
                if (DBG) {
                    log("unhold call: it is bg call, swap fg and bg");
                }
                mSwitchingFgAndBgCalls = true;
                mCallExpectedToResume = mBackgroundCall.getImsCall();
                mForegroundCall.switchWith(mBackgroundCall);
            } else if (call != mForegroundCall) {
                if (DBG) {
                    log("unhold call which is neither background nor foreground call");
                }
                return;
            }
            if (mForegroundCall.getState().isAlive()) {
                if (DBG) {
                    log("unhold call: foreground call is alive; try to resume it");
                }
                ImsCall imsCall = mForegroundCall.getImsCall();
                if (imsCall != null) {
                    imsCall.resume();
                }
            }
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }
    }

    // ALPS02501206. For operator custumization error causes.
    private boolean isVendorDisconnectCauseNeeded(ImsReasonInfo reasonInfo) {
        if (reasonInfo == null) {
            return false;
        }

        int errorCode = reasonInfo.getCode();
        String errorMsg = reasonInfo.getExtraMessage();

        if (errorMsg == null) {
            log("isVendorDisconnectCauseNeeded = no due to empty errorMsg");
            return false;
        }

        log("isVendorDisconnectCauseNeeded = no, no matched case");
        return false;
    }

    @Override
    public ImsUtInterface getUtInterface() throws ImsException {
        if (mImsManager == null) {
            throw getImsManagerIsNullException();
        }

        ImsUtInterface ut = ((MtkImsManager)mImsManager).getSupplementaryServiceConfiguration();
        return ut;
    }

    @Override
    protected ImsPhoneConnection makeImsPhoneConnectionForMO(String dialString, boolean
            isEmergencyNumber) {
        return new MtkImsPhoneConnection(mPhone, checkForTestEmergencyNumber(dialString), this,
                mForegroundCall, isEmergencyNumber);
    }

    @Override
    protected ImsPhoneConnection makeImsPhoneConnectionForMT(ImsCall imsCall, boolean isUnknown) {
        return new MtkImsPhoneConnection(mPhone, imsCall, this, (isUnknown ? mForegroundCall :
                mRingingCall), isUnknown);
    }

    @Override
    protected ImsCall takeCall(IImsCallSession c, Bundle extras) throws ImsException {
        if (extras.getBoolean(ImsManager.EXTRA_USSD, false)) {
            return mImsManager.takeCall(c, extras, mMtkImsUssdListener);
        } else {
            return mImsManager.takeCall(c, extras, mMtkImsCallListener);
        }
    }

    @Override
    protected void mtkNotifyRemoteHeld(ImsPhoneConnection conn, boolean held) {
        if (conn != null && conn instanceof MtkImsPhoneConnection) {
            ((MtkImsPhoneConnection) conn).notifyRemoteHeld(held);
        }
    }

    @Override
    protected boolean isEmergencyNumber(String dialString) {
        /*
        // CC: For 93, MD can switch phoneType when SIM not inserted,
        // TeleService won't trigger phone switch, so check both SIM's ECC
        if (hasC2kOverImsModem() &&
                (!TelephonyManager.getDefault().hasIccCard(mPhone.getPhoneId())
                || mPhone.getServiceState().getState() != ServiceState.STATE_IN_SERVICE)) {
            return PhoneNumberUtils.isEmergencyNumber(dialString);
        } else {
            // ALPS02399162
            // ECC number is different for CDMA and GSM. Should carry phone sub id to query if the
            // number is ECC or not
            return PhoneNumberUtils.isEmergencyNumber(mPhone.getSubId(), dialString);
        }
        */
        return MtkLocalPhoneNumberUtils.getIsEmergencyNumber();
    }

    @Override
    protected void checkforCsfb() throws CallStateException {
        // ALPS02015368, it should use GSMPhone to dial during SRVCC.
        // When SRVCC, GSMPhone will continue the handover calls until disconnected.
        // It should use GSMPhone to dial to prevent PS/CS call conflict problem.
        // Throw CS_FALLBACK exception here will let GSMPhone to dial.
        if (mHandoverCall.mConnections.size() > 0) {
            log("SRVCC: there are connections during handover, trigger CSFB!");
            throw new CallStateException(ImsPhone.CS_FALLBACK);
        }

        // ALPS02015368 and ALPS02298554.
        /*
         * In ALPS02015368, we should use GSMPhone to dial after SRVCC happened, until the handover
         * calls end. In ALPS02298554, even the IMS over ePDG is connected, but there is a CS MT
         * call happened so we still need to use GSMPhone to dial.
         */
        if (mPhone != null && mPhone.getDefaultPhone() != null) {
            // default phone is GSMPhone or CDMAPhone, which owns the ImsPhone.
            Phone defaultPhone = mPhone.getDefaultPhone();
            if (defaultPhone.getState() != PhoneConstants.State.IDLE
                    && getState() == PhoneConstants.State.IDLE) {
                log("There are CS connections, trigger CSFB!");
                throw new CallStateException(ImsPhone.CS_FALLBACK);
            }
        }
    }

    @Override
    protected void resetFlagWhenSwitchFailed() {
        mSwitchingFgAndBgCalls = false;
        mCallExpectedToResume = null;
    }

    @Override
    protected void setPendingResumeRequest(boolean pendingResumeRequest) {
        if (pendingResumeRequest) {
            log("turn on the resuem pending request lock!");
        }
        mHasPendingResumeRequest = pendingResumeRequest;
    }

    @Override
    protected boolean hasPendingResumeRequest() {
        if (mHasPendingResumeRequest) {
            log("there is a pending resume background request, ignore accept()!");
        }
        return mHasPendingResumeRequest;
    }

    @Override
    protected boolean canDailOnCallTerminated() {
        return mPendingMO != null && !hasMessages(EVENT_HANGUP_PENDINGMO);
    }

    @Override
    protected void setRedialAsEcc(int cause) {
        if (cause == MtkDisconnectCause.IMS_EMERGENCY_REREG) {
            mDialAsECC = true;
        }
    }

    @Override
    protected void setVendorDisconnectCause(ImsPhoneConnection conn, ImsReasonInfo reasonInfo) {
        // ALPS02501206. For OP07 requirement.
        if (conn instanceof MtkImsPhoneConnection) {
            ((MtkImsPhoneConnection) conn).setVendorDisconnectCause(
                    reasonInfo.getExtraMessage());
        }
    }

    @Override
    protected int updateDisconnectCause(int cause, ImsPhoneConnection conn ) {
        if (cause == DisconnectCause.ERROR_UNSPECIFIED && conn != null
                && conn.getImsCall().isMerged()) {
            // Call was terminated while it is merged instead of a remote disconnect.
            return DisconnectCause.IMS_MERGED_SUCCESSFULLY;
        }
        return cause;
    }

    @Override
    protected void setMultiPartyState(Connection c) {
        // M: for conference SRVCC.
        if (c instanceof MtkImsPhoneConnection) {
            ((MtkImsPhoneConnection)c).mWasMultiparty = c.isMultiparty();
            ((MtkImsPhoneConnection)c).mWasPreMultipartyHost = c.isConferenceHost();
            log("SRVCC: Connection isMultiparty is " +
                    ((MtkImsPhoneConnection)c).mWasMultiparty + "and isConfHost is " +
                    ((MtkImsPhoneConnection)c).mWasPreMultipartyHost + " before handover");
        }
    }

    @Override
    protected void resetRingBackTone(ImsPhoneCall call) {
        /// M: ALPS02589783 @{
        // If ringback tone flag is set for foreground call, after SRVCC it has no chance to reset.
        // We reset manually when handover happened.
        if (call instanceof MtkImsPhoneCall) {
            ((MtkImsPhoneCall)call).resetRingbackTone();
        }
        /// @}
    }

    @Override
    protected void updateForSrvccCompleted() {
        // ALPS02015368 mPendingMO should be cleared when fake SRVCC/bSRVCC happens,
        // or dial function will fail
        if (mPendingMO != null) {
            log("SRVCC: reset mPendingMO");
            removeConnection(mPendingMO);
            mPendingMO = null;
        }
        // ALPS02530061, after SRVCC complete, update phone state to listener.
        updatePhoneState();

        // reset the SRVCC state for SRVCC pending action handling.
        mSrvccState = Call.SrvccState.NONE;
    }

    /// M: MTK SS start @{
    // Assign return message for terminating USSI.
    public void cancelUSSD(Message response) {
        if (mUssdSession == null) return;
        mPendingUssd = response;
        try {
            mUssdSession.terminate(ImsReasonInfo.CODE_USER_TERMINATED);
        } catch (ImsException e) {
        }
    }
    /// @}

    /// M: MTK update voice call state with SRVCC state
    protected AsyncResult getCallStateChangeAsyncResult() {
        return new AsyncResult(null, mSrvccState, null);
    }
    ///

    @Override
    protected void checkIncomingCallInRttEmcGuardTime(ImsPhoneConnection conn) {
        mOpCommonImsPhoneCallTracker.checkIncomingCallInRttEmcGuardTime(conn);
    }


    protected void processRttModifyFailCase(ImsCall imsCall, int status,
        ImsPhoneConnection conn){
        mOpCommonImsPhoneCallTracker.processRttModifyFailCase(imsCall, status, conn);
    }
    @Override
    protected void checkRttCallType() {
        mOpCommonImsPhoneCallTracker.checkRttCallType(mPhone, mForegroundCall, mSrvccState);
    }

    protected void processRttModifySuccessCase(ImsCall imsCall, int status,
        ImsPhoneConnection conn){
        mOpCommonImsPhoneCallTracker.processRttModifySuccessCase(imsCall, status, conn);
    }

    @Override
    protected void startRttEmcGuardTimer() {
        mOpCommonImsPhoneCallTracker.startRttEmcGuardTimer(mPhone);
    }

    @Override
    protected void startListeningForCalls() throws ImsException {
        super.startListeningForCalls();

        try {
            ((MtkImsManager) mImsManager).addImsConnectionStateListener(mImsStateListener);
            log("startListeningForCalls() : register ims succeed, " + mImsStateListener);
        } catch (ImsException e) {
            // Could not get the ImsService.
            log("startListeningForCalls() : register ims fail!");
        }
    }

    protected void modifyVideoCall(ImsCall imsCall, int newVideoState) {

        ImsPhoneConnection conn = findConnection(imsCall);

        newVideoState = newVideoState | IMS_SESSION_MODIFY_OPERATION_FLAG;

        if (conn != null) {
            int oldVideoState = conn.getVideoState();
            if (conn.getVideoProvider() != null) {
                conn.getVideoProvider().onSendSessionModifyRequest(
                        new VideoProfile(oldVideoState), new VideoProfile(newVideoState));
            }
        }
    }

    @Override
    protected void switchWfcModeIfRequired(ImsManager imsManager, boolean isWfcEnabled, boolean
            isEmergencyNumber) {
        mOpImsPhoneCallTracker.switchWfcModeIfRequired(imsManager, isWfcEnabled, isEmergencyNumber);
    }

    @Override
    protected String getVtInterface() {
        TelephonyManager telephony =
            (TelephonyManager) mPhone.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String subscriberId = telephony.getSubscriberId(mPhone.getSubId());
        String vtIf =
                new String(NetworkStatsService.VT_INTERFACE + subscriberId);
        log("[SubId=" + mPhone.getSubId() + "] " + "getVtInterface(): " + vtIf);
        return vtIf;
    }

    private String sensitiveEncode(String input) {
        if (!SENLOG || TELDBG) {
            return input;
        }
        return "[hidden]";
    }

    @Override
    protected boolean isCarrierPauseAllowed(ImsCall imsCall) {
        /// M: ALPS03728528 @{
        // If mobile data is disabled during call initiation stage the call will not have
        // downgrade capability. So the video call gets paused instead. Instead of pausing
        // the call terminate the call.
        if (imsCall != null && (imsCall.getState() == ImsCallSession.State.ESTABLISHING)) {
            return false;
        } else {
            return true;
        }
        /// @}
    }

    public boolean isWifiPdnOutOfService() {
        return mWifiPdnOOSState == MtkImsManager.OOS_START ||
               mWifiPdnOOSState == MtkImsManager.OOS_END_WITH_DISCONN;
    }

    /// M: ALPS03905309 API for data to register the event if disconnection occurs in srvcc. @{
    public void registerForCallsDisconnectedDuringSrvcc(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mCallsDisconnectedDuringSrvccRegistrants.add(r);
    }

    public void unregisterForCallsDisconnectedDuringSrvcc(Handler h) {
        mCallsDisconnectedDuringSrvccRegistrants.remove(h);
    }
    /// @}

    public void registerSettingsObserver() {

        mSettingsObserver.unobserve();
        String simSuffix = "";
        if (TelephonyManager.getDefault().getSimCount() > 1) {
            simSuffix = Integer.toString(mPhone.getDefaultPhone().getSubId());
        }

        mSettingsObserver.observe(
                Settings.Global.getUriFor(Settings.Global.DATA_ROAMING + simSuffix),
                EVENT_ROAMING_SETTING_CHANGE);
    }

    public void initRoamingAndRoamingSetting() {
        mIsDataRoaming = mPhone.getDefaultPhone().getServiceState().getDataRoaming();
        mIsDataRoamingSettingEnabled = mPhone.getDefaultPhone().mDcTracker.getDataRoamingEnabled();

        MtkImsManager imsMgr = (MtkImsManager)ImsManager.getInstance(mPhone.getContext(), mPhone.getPhoneId());
        imsMgr.setDataRoaming(mIsDataRoaming);
        imsMgr.setDataRoamingSettingsEnabled(mIsDataRoamingSettingEnabled);

        log("initRoamingAndRoamingSetting, mIsDataRoaming = " + mIsDataRoaming +
            ", mIsDataRoamingSettingEnabled = " + mIsDataRoamingSettingEnabled);
    }

    // Data setting & Roaming & Roaming setting
    // |-------------------------+---+---+---+---+---+---+---+---+
    // | Current Roaming         | 0 | 0 | 0 | 0 | 1 | 1 | 1 | 1 |
    // | Mobile data on/off      | 0 | 0 | 1 | 1 | 0 | 0 | 1 | 1 |
    // | Roaming setting on/off  | 0 | 1 | 0 | 1 | 0 | 1 | 0 | 1 |
    // |-------------------------+---+---+---+---+---+---+---+---+
    // | ViLTE enable/disable    | 0 | 0 | 1 | 1 | 0 | op| 0 | 1 |
    // |-------------------------+---+---+---+---+---+---+---+---+
    //
    // This method is called
    // 1. When the data roaming status changes from non-roaming to roaming.
    protected void onDataRoamingOn() {

        log("onDataRoamingOn");

        if (mIsDataRoaming) {
            if (DBG) log("onDataRoamingOn: device already in roaming. ignored the update.");
            return;
        }

        mIsDataRoaming = mPhone.getDefaultPhone().getServiceState().getDataRoaming();
        MtkImsManager imsMgr =
            (MtkImsManager)ImsManager.getInstance(mPhone.getContext(), mPhone.getPhoneId());
        imsMgr.setDataRoaming(mIsDataRoaming);

        if (mIsDataRoamingSettingEnabled) {
            log("onDataRoamingOn: setup data on roaming");
            onDataRoamingEnabledChanged(true);

        } else {
            log("onDataRoamingOn: Tear down data connection on roaming.");
            onDataRoamingEnabledChanged(false);
        }
    }

    protected void onDataRoamingOff() {

        log("onDataRoamingOff");

        if (!mIsDataRoaming) {
            if (DBG) log("onDataRoamingOff: device already not roaming. ignored the update.");
            return;
        }

        mIsDataRoaming = mPhone.getDefaultPhone().getServiceState().getDataRoaming();
        MtkImsManager imsMgr =
            (MtkImsManager)ImsManager.getInstance(mPhone.getContext(), mPhone.getPhoneId());
        imsMgr.setDataRoaming(mIsDataRoaming);

        if (!mIsDataRoamingSettingEnabled) {
            onDataRoamingEnabledChanged(true);
        }
    }

    protected void onRoamingSettingsChanged() {

        log("onRoamingSettingsChanged");

        mIsDataRoamingSettingEnabled = mPhone.getDefaultPhone().mDcTracker.getDataRoamingEnabled();
        MtkImsManager imsMgr =
            (MtkImsManager)ImsManager.getInstance(mPhone.getContext(), mPhone.getPhoneId());
        imsMgr.setDataRoamingSettingsEnabled(mIsDataRoamingSettingEnabled);

        log("onRoamingSettingsChanged: mIsDataRoaming = " + mIsDataRoaming +
            ", mIsDataRoamingSettingEnabled = " + mIsDataRoamingSettingEnabled);

        // Check if the device is actually data roaming
        if (!mIsDataRoaming) {
            if (DBG) log("onRoamingSettingsChanged: device is not roaming. ignored the request.");
            return;
        }

        if (mIsDataRoamingSettingEnabled) {
            log("onRoamingSettingsChanged: setup data on roaming");
            onDataRoamingEnabledChanged(true);

        } else {
            log("onRoamingSettingsChanged: Tear down data connection on roaming.");
            onDataRoamingEnabledChanged(false);
        }
    }

    // Same as onDataEnabledChanged
    private void onDataRoamingEnabledChanged(boolean enabled) {

        log("onDataRoamingEnabledChanged: enabled=" + enabled);

        if (!mIsViLteDataMetered) {
            log("onDataRoamingEnabledChanged: Ignore data " + ((enabled) ? "enabled" : "disabled")
                    + " - carrier policy indicates that data is not metered for ViLTE calls.");
            return;
        }

        if (mIgnoreDataRoaming) {
            log("onDataRoaming: Ignore data " + ((enabled) ? "enabled" : "disabled")
                    + " - carrier policy indicates that ignore data roaming");
            return;
        }

        if (enabled && !mIsDataEnabled) {
            log("onDataRoamingEnabledChanged: Ignore on when data off");
            return;
        }

        // Inform connections that data has been disabled to ensure we turn off video capability
        // if this is an LTE call.
        for (ImsPhoneConnection conn : mConnections) {
            ImsCall imsCall = conn.getImsCall();
            boolean isLocalVideoCapable = enabled || (imsCall != null && imsCall.isWifiCall());
            conn.handleDataEnabledChange(isLocalVideoCapable);
        }

        int reason = DataEnabledSettings.REASON_USER_DATA_ENABLED;
        int reasonCode = ImsReasonInfo.CODE_DATA_DISABLED;

        // Potentially send connection events so the InCall UI knows that video calls are being
        // downgraded due to data being enabled/disabled.
        maybeNotifyDataDisabled(enabled, reasonCode);
        // Handle video state changes required as a result of data being enabled/disabled.
        handleDataEnabledChange(enabled, reasonCode);

        // We do not want to update the ImsConfig for REASON_REGISTERED, since it can happen before
        // the carrier config has loaded and will deregister IMS.
        if (!mShouldUpdateImsConfigOnDisconnect
                && reason != DataEnabledSettings.REASON_REGISTERED) {
            // This will call into updateVideoCallFeatureValue and eventually all clients will be
            // asynchronously notified that the availability of VT over LTE has changed.
            ImsManager.updateImsServiceConfig(mPhone.getContext(), mPhone.getPhoneId(), true);
        }
    }

    @Override
    protected boolean isRoamingOnAndRoamingSettingOff() {
        return (mIsDataRoaming && !mIsDataRoamingSettingEnabled && !mIgnoreDataRoaming);
    }

    @Override
    protected void cacheCarrierConfiguration(int subId) {
        super.cacheCarrierConfiguration(subId);

        log("cacheCarrierConfiguration: mSupportDowngradeVtToAudio: " + mSupportDowngradeVtToAudio);

        CarrierConfigManager carrierConfigManager = (CarrierConfigManager)
                mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (carrierConfigManager == null) {
            loge("cacheCarrierConfiguration: No carrier config service found.");
            return;
        }

        PersistableBundle carrierConfig = carrierConfigManager.getConfigForSubId(subId);
        if (carrierConfig == null) {
            loge("cacheCarrierConfiguration: Empty carrier config.");
            return;
        }

        mIgnoreDataRoaming = carrierConfig.getBoolean(
            MtkCarrierConfigManager.MTK_KEY_IGNORE_DATA_ROAMING_FOR_VIDEO_CALLS);
    }

    @Override
    public boolean isCarrierDowngradeOfVtCallSupported() {
        log("isCarrierDowngradeOfVtCallSupported: " + mSupportDowngradeVtToAudio);
        return super.isCarrierDowngradeOfVtCallSupported();
    }

    protected boolean isDataAvailableForViLTE() {
        return (!mIsViLteDataMetered || (mIsDataEnabled && !isRoamingOnAndRoamingSettingOff()));
    }

    protected boolean checkNoActiveCall() {
        return mForegroundCall.getState() != ImsPhoneCall.State.ACTIVE &&
                mBackgroundCall.getState() != ImsPhoneCall.State.ACTIVE;
    }

    private int getHangupReasionInfo(int disconnectCause) {

        if (disconnectCause == MtkDisconnectCause.INCOMING_REJECTED_NO_COVERAGE){
            return MtkImsReasonInfo.CODE_NO_COVERAGE;
        } else if (disconnectCause == MtkDisconnectCause.INCOMING_REJECTED_LOW_BATTERY){
            return MtkImsReasonInfo.CODE_LOW_BATTERY;
        } else if (disconnectCause == MtkDisconnectCause.INCOMING_REJECTED_FORWARD){
            return MtkImsReasonInfo.CODE_FORWARD;
        } else if (disconnectCause == MtkDisconnectCause.INCOMING_REJECTED_SPECIAL_HANGUP){
            return MtkImsReasonInfo.CODE_SPECIAL_HANGUP;
        }else {
            return ImsReasonInfo.CODE_USER_DECLINE;
        }
    }
}
