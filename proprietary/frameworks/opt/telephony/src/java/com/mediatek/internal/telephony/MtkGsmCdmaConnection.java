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
import android.os.AsyncResult;
import android.os.Message;
import android.os.Registrant;
import android.os.SystemClock;
import android.telephony.DisconnectCause;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.text.TextUtils;

import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.GsmCdmaCall;
import com.android.internal.telephony.GsmCdmaCallTracker;
import com.android.internal.telephony.GsmCdmaConnection;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyDevController;
import com.android.internal.telephony.Connection.PostDialState;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.UiccCardApplication;

import com.mediatek.internal.telephony.MtkPhoneNumberUtils;

import mediatek.telephony.MtkDisconnectCause;

/// M: CC: update isEmergency according to TeleService
import com.mediatek.telephony.MtkTelephonyManagerEx;

public class MtkGsmCdmaConnection extends GsmCdmaConnection {

    private static final String PROP_LOG_TAG = "GsmCdmaConn";
    /// M: Log optimization @{
    //private static final boolean VDBG = false;
    private static final boolean VDBG =
            (android.os.SystemProperties.getInt("persist.vendor.log.tel_dbg", 0) == 1);
    /// @}

    /// M: CC: Forwarding number via EAIC
    String mForwardingAddress;
    /// @}

    /// M: CC: Proprietary CRSS handling @{
    /// M: CC: Redirecting number via COLP
    String mRedirectingAddress;
    /// @}

    /// M: CC: incoming call reject cause for hangup
    int mRejectCauseToRIL = -1;

    /// M: CC: CDMA call accepted @{
    private static final int MO_CALL_VIBRATE_TIME = 200;  // msec
    private boolean mIsRealConnected; // Indicate if the MO call has been accepted by remote side
    private boolean mReceivedAccepted; // Indicate if we receive call accepted event
    /// @}

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

    /** This is an MO call, created when dialing */
    public MtkGsmCdmaConnection(GsmCdmaPhone phone, String dialString, GsmCdmaCallTracker ct,
            GsmCdmaCall parent, boolean isEmergencyCall) {
        super(phone, dialString, ct, parent, isEmergencyCall);

        /// M: CC: CDMA call accepted @{
        mIsRealConnected = false;
        mReceivedAccepted = false;
        /// @}
    }

    /** This is probably an MT call that we first saw in a CLCC response or a hand over. */
    public MtkGsmCdmaConnection(GsmCdmaPhone phone, DriverCall dc, GsmCdmaCallTracker ct,
            int index) {
        super(phone, dc, ct, index);

        /// M: CC: Reconstruct MT address by certain format @{
        String origAddress = ((MtkGsmCdmaCallTracker) ct).mMtkGsmCdmaCallTrackerExt
                .convertAddress(mAddress);
        if (origAddress != null) {
            setConnectionExtras(((MtkGsmCdmaCallTracker) ct).mMtkGsmCdmaCallTrackerExt
                    .getAddressExtras(mAddress));
            // same as setConverted(origAddress), except not to set mDialString
            mNumberConverted = true;
            mConvertedNumber = mAddress;
            mAddress = origAddress;
        }
        /// @}

        // M: CC: For 93, MD can switch phoneType when SIM not inserted,
        // TeleService won't trigger phone switch, so check both SIM's ECC @{
        //mIsEmergencyCall = PhoneNumberUtils.isLocalEmergencyNumber(phone.getContext(), mAddress);
        if (hasC2kOverImsModem() &&
                (!android.telephony.TelephonyManager.getDefault().hasIccCard(phone.getPhoneId())
                || phone.getServiceState().getState() != ServiceState.STATE_IN_SERVICE)) {
            mIsEmergencyCall = PhoneNumberUtils.isLocalEmergencyNumber(
                    phone.getContext(), mAddress);
        } else {
            mIsEmergencyCall = PhoneNumberUtils.isLocalEmergencyNumber(
                    phone.getContext(), phone.getSubId(), mAddress);
        }
        /// @}
    }

    //CDMA
    /** This is a Call waiting call*/
    public MtkGsmCdmaConnection(Context context, CdmaCallWaitingNotification cw,
            GsmCdmaCallTracker ct, GsmCdmaCall parent) {
        super(context, cw, ct, parent);
    }

    /*package*/ public boolean
    compareTo(DriverCall c) {
        // On mobile originated (MO) calls, the phone number may have changed
        // due to a SIM Toolkit call control modification.
        //
        // We assume we know when MO calls are created (since we created them)
        // and therefore don't need to compare the phone number anyway.
        if (! (mIsIncoming || c.isMT)) return true;

        // A new call appearing by SRVCC may have invalid number
        //  if IMS service is not tightly coupled with cellular modem stack.
        // Thus we prefer the preexisting handover connection instance.
        if (isPhoneTypeGsm() && mOrigConnection != null) return true;

        // ... but we can compare phone numbers on MT calls, and we have
        // no control over when they begin, so we might as well

        String cAddress = PhoneNumberUtils.stringFromStringAndTOA(c.number, c.TOA);

        /// M: CC: Digits checks mNumberConverted and ignore mAddress comparision @{
        //return mIsIncoming == c.isMT && equalsHandlesNulls(mAddress, cAddress);

        boolean addrChanged2 = ((MtkGsmCdmaCallTracker) mOwner).mMtkGsmCdmaCallTrackerExt
                .isAddressChanged(mNumberConverted, mAddress, cAddress);

        return mIsIncoming == c.isMT && !addrChanged2;
        /// @}
    }

    /// M: CC: Forwarding number via EAIC @{
    /**
     * Gets forwarding address (e.g. phone number) associated with connection.
     * A makes call to B and B redirects(Forwards) this call to C, the forwarding address is B.
     * @return address or null if unavailable
    */
    public String getForwardingAddress() {
       return mForwardingAddress;
    }

    /**
     * Sets forwarding address (e.g. phone number) associated with connection.
     * A makes call to B and B redirects(Forwards) this call to C, the forwarding address is B.
    */
    public void setForwardingAddress(String address) {
       mForwardingAddress = address;
    }
    /// @}

    /// M: CC: Proprietary CRSS handling @{
    /// M: CC: Redirecting number via COLP
    /**
     * Gets redirecting address (e.g. phone number) associated with connection.
     *
     * @return address or null if unavailable
    */
    public String getRedirectingAddress() {
       return mRedirectingAddress;
    }

    /**
     * Sets redirecting address (e.g. phone number) associated with connection.
     *
    */
    public void setRedirectingAddress(String address) {
        mRedirectingAddress = address;
    }

  /// M: CC: GSA HD Voice for 2/3G network support @{
    private static final String PROPERTY_HD_VOICE_STATUS = "vendor.audiohal.ril.hd.voice.status";

    protected int getAudioQualityFromDC(int audioQuality) {
        String op = null;
        String hdStat = null;
        op = android.os.SystemProperties.get("persist.vendor.operator.optr", "OM");
        log("isHighDefAudio, optr:" + op);
        op = op.concat("=");
        hdStat = android.os.SystemProperties.get(PROPERTY_HD_VOICE_STATUS, "");
        if (hdStat != null && !hdStat.equals("")) {
            log("HD voice status: " + hdStat);
            boolean findOp = hdStat.indexOf(op) != -1;
            boolean findOm = hdStat.indexOf("OM=") != -1;
            int start = 0;
            if (findOp && !op.equals("OM=")) {
                start = hdStat.indexOf(op) + op.length(); //OPXX=Y
            } else if (findOm) {
                start = hdStat.indexOf("OM=") + 3; //OM=Y
            }
            // Ex: ril.hd.voice.status="OM=Y;OP07=N;OP12=Y;"
            String isHd = hdStat.length() > (start + 1) ? hdStat.substring(start, start + 1) : "";
            if (isHd.equals("Y")) {
                return Connection.AUDIO_QUALITY_HIGH_DEFINITION;
            } else {
                return Connection.AUDIO_QUALITY_STANDARD;
            }
        }

        switch (audioQuality) {
        case DriverCall.AUDIO_QUALITY_AMR_WB:
        case DriverCall.AUDIO_QUALITY_EVRC_WB: // M: CC: HD should be using wide band.
            return Connection.AUDIO_QUALITY_HIGH_DEFINITION;
        default:
            return Connection.AUDIO_QUALITY_STANDARD;
        }
    }

    /// M: CC: number presentation via CLIP
    public void setNumberPresentation(int num) {
        mNumberPresentation = num;
    }
    /// @}

    /// M: CC: Proprietary incoming call handling @{
    /// Reject MT when another MT already exists via EAIC disapproval
    public void onReplaceDisconnect(int cause) {
        this.mCause = cause;

        if (!mDisconnected) {
            mIndex = -1;

            mDisconnectTime = System.currentTimeMillis();
            mDuration = SystemClock.elapsedRealtime() - mConnectTimeReal;
            mDisconnected = true;

            log("onReplaceDisconnect: cause=" + cause);

            if (mParent != null) {
                mParent.connectionDisconnected(this);
            }
        }
        releaseWakeLock();
    }
    /// @}

    protected int disconnectCauseFromCode(int causeCode) {
        switch (causeCode) {
            /// M: CC: Extend Call Fail Cause @{
            case MtkCallFailCause.NO_ROUTE_TO_DESTINATION:
                return MtkDisconnectCause.NO_ROUTE_TO_DESTINATION;

            case MtkCallFailCause.NO_USER_RESPONDING:
                return MtkDisconnectCause.NO_USER_RESPONDING;

            /**
             * Google default behavior:
             * Return DisconnectCause.ERROR_UNSPECIFIED to avoid UNKNOWN cause in inCallUI,
             * which will add 5s delay, instead of 2s delay for ERROR cause
             * USER_ALERTING_NO_ANSWER(+CEER: 19)
             */
            //case CallFailCause.USER_ALERTING_NO_ANSWER:
            //    return DisconnectCause.USER_ALERTING_NO_ANSWER;

            /**
             * Google default behavior:
             * Return DisconnectCause.ERROR_UNSPECIFIED to play TONE_CALL_ENDED for
             * CALL_REJECTED(+CEER: 21) and NORMAL_UNSPECIFIED(+CEER: 31)
             */
            //case CallFailCause.CALL_REJECTED:
            //    return DisconnectCause.CALL_REJECTED;

            //case CallFailCause.NORMAL_UNSPECIFIED:
            //    return DisconnectCause.NORMAL_UNSPECIFIED;

            case MtkCallFailCause.INVALID_NUMBER_FORMAT:
                return MtkDisconnectCause.INVALID_NUMBER_FORMAT;

            case MtkCallFailCause.FACILITY_REJECTED:
                return MtkDisconnectCause.FACILITY_REJECTED;

            case MtkCallFailCause.RESOURCE_UNAVAILABLE:
                return MtkDisconnectCause.RESOURCE_UNAVAILABLE;

            case MtkCallFailCause.BEARER_NOT_AUTHORIZED:
                return MtkDisconnectCause.BEARER_NOT_AUTHORIZED;

            case MtkCallFailCause.SERVICE_NOT_AVAILABLE:

            case MtkCallFailCause.NETWORK_OUT_OF_ORDER:
                return MtkDisconnectCause.SERVICE_NOT_AVAILABLE;

            case MtkCallFailCause.BEARER_NOT_IMPLEMENT:
                return MtkDisconnectCause.BEARER_NOT_IMPLEMENT;

            case MtkCallFailCause.FACILITY_NOT_IMPLEMENT:
                return MtkDisconnectCause.FACILITY_NOT_IMPLEMENT;

            case MtkCallFailCause.RESTRICTED_BEARER_AVAILABLE:
                return MtkDisconnectCause.RESTRICTED_BEARER_AVAILABLE;

            /**
             * In China network, ECC server ends call with error cause CM_SER_OPT_UNIMPL(+CEER: 79)
             * Return DisconnectCause.NORMAL to not trigger ECC retry
             */
            //case CallFailCause.OPTION_NOT_AVAILABLE:
            //    return DisconnectCause.OPTION_NOT_AVAILABLE;

            case MtkCallFailCause.INCOMPATIBLE_DESTINATION:
                return MtkDisconnectCause.INCOMPATIBLE_DESTINATION;

            case MtkCallFailCause.CM_MM_RR_CONNECTION_RELEASE:
                return MtkDisconnectCause.CM_MM_RR_CONNECTION_RELEASE;

            case MtkCallFailCause.CHANNEL_UNACCEPTABLE:
                return MtkDisconnectCause.CHANNEL_UNACCEPTABLE;

            case CallFailCause.OPERATOR_DETERMINED_BARRING:
                return MtkDisconnectCause.OPERATOR_DETERMINED_BARRING;

            case MtkCallFailCause.PRE_EMPTION:
                return MtkDisconnectCause.PRE_EMPTION;

            case MtkCallFailCause.NON_SELECTED_USER_CLEARING:
                return MtkDisconnectCause.NON_SELECTED_USER_CLEARING;

            case MtkCallFailCause.DESTINATION_OUT_OF_ORDER:
                return MtkDisconnectCause.DESTINATION_OUT_OF_ORDER;

            case MtkCallFailCause.ACCESS_INFORMATION_DISCARDED:
                return MtkDisconnectCause.ACCESS_INFORMATION_DISCARDED;

            case MtkCallFailCause.REQUESTED_FACILITY_NOT_SUBSCRIBED:
                return MtkDisconnectCause.REQUESTED_FACILITY_NOT_SUBSCRIBED;

            case MtkCallFailCause.INCOMING_CALL_BARRED_WITHIN_CUG:
                return MtkDisconnectCause.INCOMING_CALL_BARRED_WITHIN_CUG;

            case MtkCallFailCause.INVALID_TRANSACTION_ID_VALUE:
                return MtkDisconnectCause.INVALID_TRANSACTION_ID_VALUE;

            case MtkCallFailCause.USER_NOT_MEMBER_OF_CUG:
                return MtkDisconnectCause.USER_NOT_MEMBER_OF_CUG;

            case MtkCallFailCause.INVALID_TRANSIT_NETWORK_SELECTION:
                return MtkDisconnectCause.INVALID_TRANSIT_NETWORK_SELECTION;

            case MtkCallFailCause.SEMANTICALLY_INCORRECT_MESSAGE:
                return MtkDisconnectCause.SEMANTICALLY_INCORRECT_MESSAGE;

            case MtkCallFailCause.INVALID_MANDATORY_INFORMATION:
                return MtkDisconnectCause.INVALID_MANDATORY_INFORMATION;

            case MtkCallFailCause.MESSAGE_TYPE_NON_EXISTENT:
                return MtkDisconnectCause.MESSAGE_TYPE_NON_EXISTENT;

            case MtkCallFailCause.MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROT_STATE:
                return MtkDisconnectCause.MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROT_STATE;

            case MtkCallFailCause.IE_NON_EXISTENT_OR_NOT_IMPLEMENTED:
                return MtkDisconnectCause.IE_NON_EXISTENT_OR_NOT_IMPLEMENTED;

            case MtkCallFailCause.CONDITIONAL_IE_ERROR:
                return MtkDisconnectCause.CONDITIONAL_IE_ERROR;

            case MtkCallFailCause.MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE:
                return MtkDisconnectCause.MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE;

            case MtkCallFailCause.RECOVERY_ON_TIMER_EXPIRY:
                return MtkDisconnectCause.RECOVERY_ON_TIMER_EXPIRY;

            case MtkCallFailCause.PROTOCOL_ERROR_UNSPECIFIED:
                return MtkDisconnectCause.PROTOCOL_ERROR_UNSPECIFIED;

            /**
             * Google default behavior:
             * Return DisconnectCause.ERROR_UNSPECIFIED to avoid UNKNOWN cause in inCallUI,
             * which will add 5s delay, instead of 2s delay for ERROR cause
             * INTERWORKING_UNSPECIFIED(+CEER: 127)
             */
            //case CallFailCause.INTERWORKING_UNSPECIFIED:
            //    return DisconnectCause.INTERWORKING_UNSPECIFIED;

            /// M: CC: ECC disconnection special handling @{
            // Report DisconnectCause.NORMAL for IMEI_NOT_ACCEPTED
            // For GCF test, ECC might be rejected and not trigger ECC retry in this case.
            case CallFailCause.IMEI_NOT_ACCEPTED:
                if (MtkPhoneNumberUtils.isEmergencyNumber(getAddress())) {
                    return DisconnectCause.NORMAL;
                }
            /// @}

            case CallFailCause.ERROR_UNSPECIFIED:
            case CallFailCause.NORMAL_CLEARING:
            default:
                GsmCdmaPhone phone = mOwner.getPhone();
                int serviceState = phone.getServiceState().getState();
                UiccCardApplication cardApp = phone.getUiccCardApplication();
                AppState uiccAppState = (cardApp != null) ? cardApp.getState() :
                    AppState.APPSTATE_UNKNOWN;

                /// M: @{
                proprietaryLog("disconnectCauseFromCode, causeCode:" + causeCode
                        + ", cardApp:" + cardApp
                        + ", serviceState:" + serviceState
                        + ", uiccAppState:" + uiccAppState);
                /// @}

                /// M: CC: update isEmergency according to TeleService @{
                // Number might become non-ECC after phone type switch or vice versa
                // align mIsEmergencyCall according to TeleService state
                if (hasC2kOverImsModem() && !mIsEmergencyCall) {
                    MtkTelephonyManagerEx telEx = MtkTelephonyManagerEx.getDefault();
                    mIsEmergencyCall = telEx.isEccInProgress();
                }
                /// @}

                /// M: CC: when network disconnect the call without error cause, don't retry ECC. @{
                if (causeCode == 0) {
                    if (mIsEmergencyCall) {
                        return DisconnectCause.NORMAL;
                    } else {
                        causeCode = CallFailCause.ERROR_UNSPECIFIED;
                    }
                }
                /// @}

                /// M: CC: ECC disconnection special handling @{
                // Report DisconnectCause.NORMAL for NORMAL_UNSPECIFIED
                /**
                 * Some network play in band information when ECC in DIALING state.
                 * if ECC release from network, don't set to ERROR_UNSPECIFIED
                 * to avoid Telecom retry dialing.
                 */
                if (mIsEmergencyCall) {
                    if (causeCode == CallFailCause.NORMAL_UNSPECIFIED ||
                            causeCode == MtkCallFailCause.OPTION_NOT_AVAILABLE) {
                        return DisconnectCause.NORMAL;
                    }
                }
                /// @}

                return super.disconnectCauseFromCode(causeCode);
        }
    }

    // Returns true if state has changed, false if nothing changed
    public boolean
    update (DriverCall dc) {
        GsmCdmaCall newParent;
        boolean changed = false;
        boolean wasConnectingInOrOut = isConnectingInOrOut();
        boolean wasHolding = (getState() == GsmCdmaCall.State.HOLDING);

        newParent = parentFromDCState(dc.state);

        if (Phone.DEBUG_PHONE) log("parent= " +mParent +", newParent= " + newParent);

        //Ignore dc.number and dc.name in case of a handover connection
        if (isPhoneTypeGsm() && mOrigConnection != null) {
            if (Phone.DEBUG_PHONE) log("update: mOrigConnection is not null");
        } else if (isIncoming()) {
            log(" mNumberConverted " + mNumberConverted);

            /// M: CC: Digits checks mAddress & mNumberConverted only, ignore mConvertedNumber @{
            //if (!equalsHandlesNulls(mAddress, dc.number) && (!mNumberConverted
            //        || !equalsHandlesNulls(mConvertedNumber, dc.number))) {

            boolean addrChanged = ((MtkGsmCdmaCallTracker) mOwner).mMtkGsmCdmaCallTrackerExt
                    .isAddressChanged(mNumberConverted, dc.number, mAddress, mConvertedNumber);

            if (addrChanged) {
                if (Phone.DEBUG_PHONE) log("update: phone # changed!");
                mAddress = dc.number;
                changed = true;
            }
            /// @}
        }

        int newAudioQuality = getAudioQualityFromDC(dc.audioQuality);
        if (getAudioQuality() != newAudioQuality) {
            if (Phone.DEBUG_PHONE) {
                log("update: audioQuality # changed!:  "
                        + (newAudioQuality == Connection.AUDIO_QUALITY_HIGH_DEFINITION
                        ? "high" : "standard"));
            }
            setAudioQuality(newAudioQuality);
            changed = true;
        }

        // A null cnapName should be the same as ""
        if (TextUtils.isEmpty(dc.name)) {
            /// M: CC: CLCC without name information handling @{
            /* Name information is not updated by +CLCC, dc.name will be empty always,
               so ignore the following statements */
            //if (!TextUtils.isEmpty(mCnapName)) {
            //    changed = true;
            //    mCnapName = "";
            //}
            /// @}
        } else if (!dc.name.equals(mCnapName)) {
            changed = true;
            mCnapName = dc.name;
        }

        if (Phone.DEBUG_PHONE) log("--dssds----"+mCnapName);
        mCnapNamePresentation = dc.namePresentation;
        mNumberPresentation = dc.numberPresentation;

        if (newParent != mParent) {
            if (mParent != null) {
                mParent.detach(this);
            }
            newParent.attach(this, dc);
            mParent = newParent;
            changed = true;
        } else {
            boolean parentStateChange;
            parentStateChange = mParent.update (this, dc);
            changed = changed || parentStateChange;
        }

        /** Some state-transition events */

        if (Phone.DEBUG_PHONE) log(
                "update: parent=" + mParent +
                ", hasNewParent=" + (newParent != mParent) +
                ", wasConnectingInOrOut=" + wasConnectingInOrOut +
                ", wasHolding=" + wasHolding +
                ", isConnectingInOrOut=" + isConnectingInOrOut() +
                ", changed=" + changed);


        if (wasConnectingInOrOut && !isConnectingInOrOut()) {
            onConnectedInOrOut();
        }

        if (changed && !wasHolding && (getState() == GsmCdmaCall.State.HOLDING)) {
            // We've transitioned into HOLDING
            onStartedHolding();
        }

        /// M: CC: CDMA call accepted @{
        if (!isPhoneTypeGsm()) {
            log("state=" + getState() + ", mReceivedAccepted=" + mReceivedAccepted);
            if (getState() == GsmCdmaCall.State.ACTIVE && mReceivedAccepted) {
                onCdmaCallAccepted();
                mReceivedAccepted = false;
            }
        }
        /// @}

        return changed;
    }

    @Override
    protected void
    processNextPostDialChar() {
        char c = 0;
        Registrant postDialHandler;

        if (mPostDialState == PostDialState.CANCELLED) {
            releaseWakeLock();
            return;
        }

        if (mPostDialString == null ||
                mPostDialString.length() <= mNextPostDialChar ||
                /// M: CC: DTMF request special handling @{
                // Stop processNextPostDialChar when conn is disconnected
                mDisconnected == true) {
                /// @}

            setPostDialState(PostDialState.COMPLETE);

            // We were holding a wake lock until pause-dial was complete, so give it up now
            releaseWakeLock();

            // notifyMessage.arg1 is 0 on complete
            c = 0;
        } else {
            boolean isValid;

            setPostDialState(PostDialState.STARTED);

            c = mPostDialString.charAt(mNextPostDialChar++);

            isValid = processPostDialChar(c);

            if (!isValid) {
                // Will call processNextPostDialChar
                mHandler.obtainMessage(EVENT_NEXT_POST_DIAL).sendToTarget();
                // Don't notify application
                Rlog.e(LOG_TAG, "processNextPostDialChar: c=" + c + " isn't valid!");
                return;
            }
        }

        notifyPostDialListenersNextChar(c);

        // TODO: remove the following code since the handler no longer executes anything.
        postDialHandler = mOwner.getPhone().getPostDialHandler();

        Message notifyMessage;

        if (postDialHandler != null
                && (notifyMessage = postDialHandler.messageForRegistrant()) != null) {
            // The AsyncResult.result is the Connection object
            PostDialState state = mPostDialState;
            AsyncResult ar = AsyncResult.forMessage(notifyMessage);
            ar.result = this;
            ar.userObj = state;

            // arg1 is the character that was/is being processed
            notifyMessage.arg1 = c;

            // Rlog.v("GsmCdma", "##### processNextPostDialChar: send msg to
            // postDialHandler, arg1=" + c);
            notifyMessage.sendToTarget();
        }
    }

    /// M: CC: CDMA call accepted @{
    /**
     * Check if this connection is really connected.
     * @return true if this connection is really connected, or return false.
     * @hide
     */
    public boolean isRealConnected() {
        return mIsRealConnected;
    }

    boolean onCdmaCallAccepted() {
        log("onCdmaCallAccepted, mIsRealConnected=" + mIsRealConnected
                + ", state=" + getState());
        if (getState() != GsmCdmaCall.State.ACTIVE) {
            mReceivedAccepted = true;
            return false;
        }
        mConnectTimeReal = SystemClock.elapsedRealtime();
        mDuration = 0;
        mConnectTime = System.currentTimeMillis();
        if (!mIsRealConnected) {
            mIsRealConnected = true;
            // send DTMF when the CDMA call is really accepted.
            processNextPostDialChar();
            vibrateForAccepted();
        }
        return true;
    }

    private boolean isInChina() {
        String numeric = android.telephony.TelephonyManager.getDefault()
                .getNetworkOperatorForPhone(mParent.getPhone().getPhoneId());
        log("isInChina, numeric=" + numeric);
        return numeric.indexOf("460") == 0;
    }

    private void vibrateForAccepted() {
        String prop = android.os.SystemProperties.get("persist.vendor.radio.telecom.vibrate", "1");
        if ("0".equals(prop)) {
            log("vibrateForAccepted, disabled by Engineer Mode");
            return;
        }

        //if CDMA phone accepted, start a Vibrator
        android.os.Vibrator vibrator
                = (android.os.Vibrator) mParent.getPhone().getContext().getSystemService(
                        Context.VIBRATOR_SERVICE);
        vibrator.vibrate(MO_CALL_VIBRATE_TIME);
    }
    /// @}

    @Override
    public void onConnectedInOrOut() {
        mConnectTime = System.currentTimeMillis();
        mConnectTimeReal = SystemClock.elapsedRealtime();
        mDuration = 0;

        // bug #678474: incoming call interpreted as missed call, even though
        // it sounds like the user has picked up the call.
        if (Phone.DEBUG_PHONE) {
            log("onConnectedInOrOut: connectTime=" + mConnectTime);
        }

        if (!mIsIncoming) {
            // outgoing calls only
            /// M: CC: CDMA call accepted @{
            if (isPhoneTypeGsm()) {
                processNextPostDialChar();
            } else {
                // send DTMF when the CDMA call is really accepted.
                int count = mParent.mConnections.size();
                log("mParent.mConnections.size()=" + count);
                if (!isInChina() && !mIsRealConnected && count == 1) {
                    mIsRealConnected = true;
                    processNextPostDialChar();
                    vibrateForAccepted();
                }
                if (count > 1) {
                    mIsRealConnected = true;
                    processNextPostDialChar();
                }
            }
            /// @}
        } else {
            // Only release wake lock for incoming calls, for outgoing calls the wake lock
            // will be released after any pause-dial is completed
            releaseWakeLock();
        }
    }

    void proprietaryLog(String s) {
        Rlog.d(PROP_LOG_TAG, s);
    }

    /// M: CC: Proprietary CRSS handling @{
    /**
     * Called when this Connection is fail to enter backgroundCall,
     * because we switch fail.
     * (We think we're going to end up HOLDING in the backgroundCall when dial is initiated)
     */
    void
    resumeHoldAfterDialFailed() {
        if (mParent != null) {
            mParent.detach(this);
        }

        mParent = mOwner.mForegroundCall;
        mParent.attachFake(this, GsmCdmaCall.State.ACTIVE);
    }
    /// @}

    // IMS Conference SRVCC
    void updateConferenceParticipantAddress(String address) {
        mAddress = address;
    }

    @Override
    public boolean isMultiparty() {
        // For IMS SRVCC. mOrigConnection is used when SRVCC, but its isMultiparty() should not be
        // believed
        // if (mOrigConnection != null) {
        //    return mOrigConnection.isMultiparty();
        // }
        if (mParent != null) {
            return mParent.isMultiparty();
        }
        /// @}

        return false;
    }

    public void setRejectWithCause(int telephonyDisconnectCode) {
        if (mParent != null && mOwner != null) {
            GsmCdmaPhone phone = mOwner.getPhone();
            if (MtkIncomingCallChecker.isMtkEnhancedCallBlockingEnabled(phone.getContext(),
                    phone.getSubId())) {
                proprietaryLog("setRejectWithCause set (" + mRejectCauseToRIL + " to "
                        + telephonyDisconnectCode + ")");
                mRejectCauseToRIL = telephonyDisconnectCode;
            }
        } else {
            proprietaryLog("setRejectWithCause fail. mParent("+mParent+"), mOwner("+mOwner+")");
        }
    }

    public int getRejectWithCause() {
        return mRejectCauseToRIL;
    }

    public void clearRejectWithCause() {
        if (mRejectCauseToRIL != -1) {
            proprietaryLog("clearRejectWithCause (" + mRejectCauseToRIL + " to -1)");
            mRejectCauseToRIL = -1;
        }
    }

    public void onHangupLocal() {
        clearRejectWithCause();
        super.onHangupLocal();
    }
}
