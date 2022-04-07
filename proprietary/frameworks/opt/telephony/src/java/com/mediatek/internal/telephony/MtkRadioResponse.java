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

import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.RadioResponse;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILRequest;

import vendor.mediatek.hardware.radio.V3_0.PhbEntryExt;
import vendor.mediatek.hardware.radio.V3_0.PhbEntryStructure;
import vendor.mediatek.hardware.radio.V3_0.PhbMemStorageResponse;

import android.content.Context;
import android.hardware.radio.V1_0.ActivityStatsInfo;
import android.hardware.radio.V1_0.AppStatus;
import android.hardware.radio.V1_0.CardStatus;
import android.hardware.radio.V1_0.CarrierRestrictions;
import android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.DataRegStateResult;
import android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.LastCallFailCauseInfo;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.LceStatusInfo;
import android.hardware.radio.V1_0.NeighboringCell;
import android.hardware.radio.V1_0.RadioError;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.SendSmsResult;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.VoiceRegStateResult;
import android.os.RemoteException;

import android.os.SystemProperties;
import android.text.TextUtils;

import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

// SMS-START
import vendor.mediatek.hardware.radio.V3_0.SmsParams;
import vendor.mediatek.hardware.radio.V3_0.SmsMemStatus;
import mediatek.telephony.MtkSmsParameters;
import com.mediatek.internal.telephony.MtkIccSmsStorageStatus;
// SMS-END
// NW-START
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.ServiceStateTracker;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import java.io.UnsupportedEncodingException;
import android.telephony.SignalStrength;
import vendor.mediatek.hardware.radio.V3_0.SignalStrengthWithWcdmaEcio;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
// NW END
// PHB START
import com.mediatek.internal.telephony.phb.PBEntry;
import com.mediatek.internal.telephony.phb.PBMemStorage;
import com.mediatek.internal.telephony.phb.PhbEntry;
// PHB END

import android.telephony.PhoneNumberUtils;
import com.android.internal.telephony.uicc.IccUtils;

// External SIM [Start]
import vendor.mediatek.hardware.radio.V3_0.VsimEvent;
// External SIM [End]
import com.mediatek.internal.telephony.MtkMessageBoost;

public class MtkRadioResponse extends MtkRadioResponseBase {

    // TAG
    private static final String TAG = "MtkRadioResp";
    private static final boolean isUserLoad = SystemProperties.get("ro.build.type").equals("user");

    RadioResponse mRadioResponse;
    MtkRIL mMtkRil;
    MtkMessageBoost mMtkMessageBoost;
    public MtkRadioResponse(RIL ril) {
        super(ril);
        mRadioResponse = new RadioResponse(ril);
        mMtkRil = (MtkRIL)ril;
        mMtkMessageBoost = MtkMessageBoost.init(mMtkRil);
    }

    /**
     * Acknowledge the receipt of radio request sent to the vendor. This must be sent only for
     * radio request which take long time to respond.
     * For more details, refer https://source.android.com/devices/tech/connect/ril.html
     *
     * @param serial Serial no. of the request whose acknowledgement is sent.
     */
    public void acknowledgeRequest(int serial) {
        mRadioResponse.acknowledgeRequest(serial);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cardStatus ICC card status as defined by CardStatus in types.hal
     */
    public void getIccCardStatusResponse(RadioResponseInfo responseInfo, CardStatus cardStatus) {
        mRadioResponse.getIccCardStatusResponse(responseInfo, cardStatus);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cardStatus ICC card status as defined by CardStatus in 1.2/types.hal
     */
    public void getIccCardStatusResponse_1_2(RadioResponseInfo responseInfo,
                                             android.hardware.radio.V1_2.CardStatus cardStatus) {
        mRadioResponse.getIccCardStatusResponse_1_2(responseInfo, cardStatus);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPinForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        mRadioResponse.supplyIccPinForAppResponse(responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPukForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        mRadioResponse.supplyIccPukForAppResponse(responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPin2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        mRadioResponse.supplyIccPin2ForAppResponse(responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPuk2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        mRadioResponse.supplyIccPuk2ForAppResponse(responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void changeIccPinForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        mRadioResponse.changeIccPinForAppResponse(responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void changeIccPin2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        mRadioResponse.changeIccPin2ForAppResponse(responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param retriesRemaining Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyNetworkDepersonalizationResponse(RadioResponseInfo responseInfo,
                                                       int retriesRemaining) {
        mRadioResponse.supplyNetworkDepersonalizationResponse(responseInfo, retriesRemaining);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param retriesRemaining Number of retries remaining.
     */
    public void supplyDepersonalizationResponse(RadioResponseInfo responseInfo,
            int retriesRemaining) {
        mRadioResponse.supplyNetworkDepersonalizationResponse(responseInfo, retriesRemaining);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param calls Current call list
     */
    public void getCurrentCallsResponse(RadioResponseInfo responseInfo,
                                        ArrayList<android.hardware.radio.V1_0.Call> calls) {
        mRadioResponse.getCurrentCallsResponse(responseInfo, calls);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param calls Current call list
     */
    public void getCurrentCallsResponse_1_2(RadioResponseInfo responseInfo,
                                        ArrayList<android.hardware.radio.V1_2.Call> calls) {
        mRadioResponse.getCurrentCallsResponse_1_2(responseInfo, calls);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void dialResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.dialResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param imsi String containing the IMSI
     */
    public void getIMSIForAppResponse(RadioResponseInfo responseInfo, String imsi) {
        mRadioResponse.getIMSIForAppResponse(responseInfo, imsi);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void hangupConnectionResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.hangupConnectionResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void hangupWaitingOrBackgroundResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.hangupWaitingOrBackgroundResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void hangupForegroundResumeBackgroundResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.hangupForegroundResumeBackgroundResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void switchWaitingOrHoldingAndActiveResponse(RadioResponseInfo responseInfo) {
        mMtkRil.riljLog("clear mIsSendChldRequest");
        synchronized (mMtkRil.mDtmfReqQueue) {
            mMtkRil.mDtmfReqQueue.resetSendChldRequest();
        }
        mRadioResponse.switchWaitingOrHoldingAndActiveResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void conferenceResponse(RadioResponseInfo responseInfo) {
        mMtkRil.riljLog("clear mIsSendChldRequest");
        synchronized (mMtkRil.mDtmfReqQueue) {
            mMtkRil.mDtmfReqQueue.resetSendChldRequest();
        }
        mRadioResponse.conferenceResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void rejectCallResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.rejectCallResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param fcInfo Contains LastCallFailCause and vendor cause code. GSM failure reasons
     *        are mapped to cause codes defined in TS 24.008 Annex H where possible. CDMA
     *        failure reasons are derived from the possible call failure scenarios
     *        described in the "CDMA IS-2000 Release A (C.S0005-A v6.0)" standard.
     */
    public void getLastCallFailCauseResponse(RadioResponseInfo responseInfo,
                                             LastCallFailCauseInfo fcInfo) {
        mRadioResponse.getLastCallFailCauseResponse(responseInfo, fcInfo);
    }

    public void getSignalStrengthResponse(RadioResponseInfo responseInfo,
                                          android.hardware.radio.V1_0.SignalStrength sigStrength) {
        mRadioResponse.getSignalStrengthResponse(responseInfo, sigStrength);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param signalStrength Current signal strength of camped/connected cells
     */
    public void getSignalStrengthResponse_1_2(
            RadioResponseInfo responseInfo,
            android.hardware.radio.V1_2.SignalStrength signalStrength) {
        mRadioResponse.getSignalStrengthResponse_1_2(responseInfo, signalStrength);
    }

    /*
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param voiceRegResponse Current Voice registration response as defined by VoiceRegStateResult
     *        in types.hal
     */
    public void getVoiceRegistrationStateResponse(RadioResponseInfo responseInfo,
                                                  VoiceRegStateResult voiceRegResponse) {
        mRadioResponse.getVoiceRegistrationStateResponse(responseInfo, voiceRegResponse);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param voiceRegResponse Current Voice registration response as defined by VoiceRegStateResult
     *        in 1.2/types.hal
     */
    public void getVoiceRegistrationStateResponse_1_2(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_2.VoiceRegStateResult voiceRegResponse) {
        mRadioResponse.getVoiceRegistrationStateResponse_1_2(responseInfo, voiceRegResponse);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataRegResponse Current Data registration response as defined by DataRegStateResult in
     *        types.hal
     */
    public void getDataRegistrationStateResponse(RadioResponseInfo responseInfo,
                                                 DataRegStateResult dataRegResponse) {
        mRadioResponse.getDataRegistrationStateResponse(responseInfo, dataRegResponse);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataRegResponse Current Data registration response as defined by DataRegStateResult in
     *        1.2/types.hal
     */
    public void getDataRegistrationStateResponse_1_2(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_2.DataRegStateResult dataRegResponse) {
        mRadioResponse.getDataRegistrationStateResponse_1_2(responseInfo, dataRegResponse);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param longName is long alpha ONS or EONS or empty string if unregistered
     * @param shortName is short alpha ONS or EONS or empty string if unregistered
     * @param numeric is 5 or 6 digit numeric code (MCC + MNC) or empty string if unregistered
     */
    public void getOperatorResponse(RadioResponseInfo responseInfo,
                                    String longName,
                                    String shortName,
                                    String numeric) {
        mRadioResponse.getOperatorResponse(responseInfo, longName,
                shortName, numeric);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setRadioPowerResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setRadioPowerResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendDtmfResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.sendDtmfResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to sms sent as defined by SendSmsResult in types.hal
     */
    public void sendSmsResponse(RadioResponseInfo responseInfo,
                                SendSmsResult sms) {
        mRadioResponse.sendSmsResponse(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to sms sent as defined by SendSmsResult in types.hal
     */
    public void sendSMSExpectMoreResponse(RadioResponseInfo responseInfo,
                                          SendSmsResult sms) {
        mRadioResponse.sendSMSExpectMoreResponse(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param setupDataCallResult Response to data call setup as defined by setupDataCallResult in
     *                            types.hal
     */
    public void setupDataCallResponse(RadioResponseInfo responseInfo,
                                      SetupDataCallResult setupDataCallResult) {
        mRadioResponse.setupDataCallResponse(responseInfo, setupDataCallResult);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param iccIo ICC io operation response as defined by IccIoResult in types.hal
     */
    public void iccIOForAppResponse(RadioResponseInfo responseInfo,
                            android.hardware.radio.V1_0.IccIoResult iccIo) {
        mRadioResponse.iccIOForAppResponse(responseInfo, iccIo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendUssdResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.sendUssdResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void cancelPendingUssdResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.cancelPendingUssdResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param n is "n" parameter from TS 27.007 7.7
     * @param m is "m" parameter from TS 27.007 7.7
     */
    public void getClirResponse(RadioResponseInfo responseInfo, int n, int m) {
        mRadioResponse.getClirResponse(responseInfo, n, m);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setClirResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setClirResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param callForwardInfos points to a vector of CallForwardInfo, one for
     *        each distinct registered phone number.
     */
    public void getCallForwardStatusResponse(RadioResponseInfo responseInfo,
                                             ArrayList<android.hardware.radio.V1_0.CallForwardInfo>
                                                     callForwardInfos) {
        mRadioResponse.getCallForwardStatusResponse(responseInfo, callForwardInfos);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCallForwardResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setCallForwardResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param enable If current call waiting state is disabled, enable = false else true
     * @param serviceClass If enable, then callWaitingResp[1]
     *        must follow, with the TS 27.007 service class bit vector of services
     *        for which call waiting is enabled.
     *        For example, if callWaitingResp[0] is 1 and
     *        callWaitingResp[1] is 3, then call waiting is enabled for data
     *        and voice and disabled for everything else.
     */
    public void getCallWaitingResponse(RadioResponseInfo responseInfo,
                                   boolean enable,
                                   int serviceClass) {
        mRadioResponse.getCallWaitingResponse(responseInfo, enable, serviceClass);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCallWaitingResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setCallWaitingResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acknowledgeLastIncomingGsmSmsResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.acknowledgeLastIncomingGsmSmsResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acceptCallResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.acceptCallResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void deactivateDataCallResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.deactivateDataCallResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param response 0 is the TS 27.007 service class bit vector of
     *        services for which the specified barring facility
     *        is active. "0" means "disabled for all"
     */
    public void getFacilityLockForAppResponse(RadioResponseInfo responseInfo, int response) {
        mRadioResponse.getFacilityLockForAppResponse(responseInfo, response);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param retry 0 is the number of retries remaining, or -1 if unknown
     */
    public void setFacilityLockForAppResponse(RadioResponseInfo responseInfo, int retry) {
        mRadioResponse.setFacilityLockForAppResponse(responseInfo, retry);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setBarringPasswordResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setBarringPasswordResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param selection false for automatic selection, true for manual selection
     */
    public void getNetworkSelectionModeResponse(RadioResponseInfo responseInfo, boolean selection) {
        mRadioResponse.getNetworkSelectionModeResponse(responseInfo, selection);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setNetworkSelectionModeAutomaticResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setNetworkSelectionModeAutomaticResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setNetworkSelectionModeManualResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setNetworkSelectionModeManualResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param networkInfos List of network operator information as OperatorInfos defined in
     *                     types.hal
     */
    public void getAvailableNetworksResponse(RadioResponseInfo responseInfo,
                                             ArrayList<android.hardware.radio.V1_0.OperatorInfo>
                                                     networkInfos) {
        mRadioResponse.getAvailableNetworksResponse(responseInfo, networkInfos);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void startNetworkScanResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.startNetworkScanResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void stopNetworkScanResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.stopNetworkScanResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void startDtmfResponse(RadioResponseInfo responseInfo) {
        mMtkRil.handleDtmfQueueNext(responseInfo.serial);
        mRadioResponse.startDtmfResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void stopDtmfResponse(RadioResponseInfo responseInfo) {
        mMtkRil.handleDtmfQueueNext(responseInfo.serial);
        mRadioResponse.stopDtmfResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param version string containing version string for log reporting
     */
    public void getBasebandVersionResponse(RadioResponseInfo responseInfo, String version) {
        mRadioResponse.getBasebandVersionResponse(responseInfo, version);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void separateConnectionResponse(RadioResponseInfo responseInfo) {
        mMtkRil.riljLog("clear mIsSendChldRequest");
        synchronized (mMtkRil.mDtmfReqQueue) {
            mMtkRil.mDtmfReqQueue.resetSendChldRequest();
        }
        mRadioResponse.separateConnectionResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setMuteResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setMuteResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param enable true for "mute enabled" and false for "mute disabled"
     */
    public void getMuteResponse(RadioResponseInfo responseInfo, boolean enable) {
        mRadioResponse.getMuteResponse(responseInfo, enable);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param status indicates CLIP status
     */
    public void getClipResponse(RadioResponseInfo responseInfo, int status) {
        mRadioResponse.getClipResponse(responseInfo, status);
    }

    /* MTK SS Feature : Start */
    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setClipResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param n Colp status in network, "0" means disabled, "1" means enabled
     * @param m Service status, "0" means not provisioned, "1" means provisioned in permanent mode
     */
    public void getColpResponse(RadioResponseInfo responseInfo, int n, int m) {
        mRadioResponse.responseInts(responseInfo, n, m);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param status indicates COLR status. "0" means not provisioned, "1" means provisioned,
     *        "2" means unknown
     */
    public void getColrResponse(RadioResponseInfo responseInfo, int status) {
        mRadioResponse.responseInts(responseInfo, status);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param n CNAP status, "0" means disabled, "1" means enabled.
     * @param m Service status, "0" means not provisioned, "1" means provisioned, "2" means unknown
     */
    public void sendCnapResponse(RadioResponseInfo responseInfo, int n, int m) {
        mRadioResponse.responseInts(responseInfo, n, m);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    @Override
    public void setColpResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    @Override
    public void setColrResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param callForwardInfoExs points to a vector of CallForwardInfoEx, one for
     *        each distinct registered phone number.
     */
    @Override
    public void queryCallForwardInTimeSlotStatusResponse(RadioResponseInfo responseInfo,
            ArrayList<vendor.mediatek.hardware.radio.V3_0.CallForwardInfoEx> callForwardInfoExs) {
        responseCallForwardInfoEx(responseInfo, callForwardInfoExs);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     *
     */
    @Override
    public void setCallForwardInTimeSlotResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param resList points to a vector of String.
     */
    @Override
    public void runGbaAuthenticationResponse(RadioResponseInfo responseInfo,
                                             ArrayList<String> resList) {
        mRadioResponse.responseStringArrayList(mRadioResponse.mRil, responseInfo, resList);
    }
    /* MTK SS Feature : End */

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataCallResultList Response to get data call list as defined by setupDataCallResult in
     *                           types.hal
     */
    public void getDataCallListResponse(RadioResponseInfo responseInfo,
                                        ArrayList<SetupDataCallResult> dataCallResultList) {
        mRadioResponse.getDataCallListResponse(responseInfo, dataCallResultList);
    }

    public void sendOemRilRequestRawResponse(RadioResponseInfo responseInfo,
                                             ArrayList<Byte> var2) {}

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSuppServiceNotificationsResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setSuppServiceNotificationsResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param index record index where the message is stored
     */
    public void writeSmsToSimResponse(RadioResponseInfo responseInfo, int index) {
        mRadioResponse.writeSmsToSimResponse(responseInfo, index);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void deleteSmsOnSimResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.deleteSmsOnSimResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setBandModeResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setBandModeResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param bandModes List of RadioBandMode listing supported modes
     */
    public void getAvailableBandModesResponse(RadioResponseInfo responseInfo,
                                              ArrayList<Integer> bandModes) {
        mRadioResponse.getAvailableBandModesResponse(responseInfo, bandModes);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param commandResponse SAT/USAT response in hexadecimal format
     *        string starting with first byte of response
     */
    public void sendEnvelopeResponse(RadioResponseInfo responseInfo, String commandResponse) {
        mRadioResponse.sendEnvelopeResponse(responseInfo, commandResponse);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendTerminalResponseToSimResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.sendTerminalResponseToSimResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void handleStkCallSetupRequestFromSimResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.handleStkCallSetupRequestFromSimResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void explicitCallTransferResponse(RadioResponseInfo responseInfo) {
        mMtkRil.riljLog("clear mIsSendChldRequest");
        synchronized (mMtkRil.mDtmfReqQueue) {
            mMtkRil.mDtmfReqQueue.resetSendChldRequest();
        }
        mRadioResponse.explicitCallTransferResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setPreferredNetworkTypeResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setPreferredNetworkTypeResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param nwType RadioPreferredNetworkType defined in types.hal
     */
    public void getPreferredNetworkTypeResponse(RadioResponseInfo responseInfo, int nwType) {
        mRadioResponse.getPreferredNetworkTypeResponse(responseInfo, nwType);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cells Vector of neighboring radio cell information
     */
    public void getNeighboringCidsResponse(RadioResponseInfo responseInfo,
                                           ArrayList<NeighboringCell> cells) {
        mRadioResponse.getNeighboringCidsResponse(responseInfo, cells);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setLocationUpdatesResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setLocationUpdatesResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaSubscriptionSourceResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setCdmaSubscriptionSourceResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaRoamingPreferenceResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setCdmaRoamingPreferenceResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param type CdmaRoamingType defined in types.hal
     */
    public void getCdmaRoamingPreferenceResponse(RadioResponseInfo responseInfo, int type) {
        mRadioResponse.getCdmaRoamingPreferenceResponse(responseInfo, type);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setTTYModeResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setTTYModeResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param mode TTY mode
     */
    public void getTTYModeResponse(RadioResponseInfo responseInfo, int mode) {
        mRadioResponse.getTTYModeResponse(responseInfo, mode);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setPreferredVoicePrivacyResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setPreferredVoicePrivacyResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param enable false for Standard Privacy Mode (Public Long Code Mask)
     *        true for Enhanced Privacy Mode (Private Long Code Mask)
     */
    public void getPreferredVoicePrivacyResponse(RadioResponseInfo responseInfo,
                                                 boolean enable) {
        mRadioResponse.getPreferredVoicePrivacyResponse(responseInfo, enable);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendCDMAFeatureCodeResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.sendCDMAFeatureCodeResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendBurstDtmfResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.sendBurstDtmfResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Sms result struct as defined by SendSmsResult in types.hal
     */
    public void sendCdmaSmsResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        mRadioResponse.sendCdmaSmsResponse(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acknowledgeLastIncomingCdmaSmsResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.acknowledgeLastIncomingCdmaSmsResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param configs Vector of GSM/WCDMA Cell broadcast configs
     */
    public void getGsmBroadcastConfigResponse(RadioResponseInfo responseInfo,
                                              ArrayList<GsmBroadcastSmsConfigInfo> configs) {
        mRadioResponse.getGsmBroadcastConfigResponse(responseInfo, configs);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setGsmBroadcastConfigResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setGsmBroadcastConfigResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setGsmBroadcastActivationResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setGsmBroadcastActivationResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param configs Vector of CDMA Broadcast SMS configs.
     */
    public void getCdmaBroadcastConfigResponse(RadioResponseInfo responseInfo,
                                               ArrayList<CdmaBroadcastSmsConfigInfo> configs) {
        mRadioResponse.getCdmaBroadcastConfigResponse(responseInfo, configs);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaBroadcastConfigResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setCdmaBroadcastConfigResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaBroadcastActivationResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setCdmaBroadcastActivationResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param mdn MDN if CDMA subscription is available
     * @param hSid is a comma separated list of H_SID (Home SID) if
     *        CDMA subscription is available, in decimal format
     * @param hNid is a comma separated list of H_NID (Home NID) if
     *        CDMA subscription is available, in decimal format
     * @param min MIN (10 digits, MIN2+MIN1) if CDMA subscription is available
     * @param prl PRL version if CDMA subscription is available
     */
    public void getCDMASubscriptionResponse(RadioResponseInfo responseInfo, String mdn,
                                            String hSid, String hNid, String min, String prl) {
        mRadioResponse.getCDMASubscriptionResponse(responseInfo, mdn, hSid, hNid, min, prl);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param index record index where the cmda sms message is stored
     */
    public void writeSmsToRuimResponse(RadioResponseInfo responseInfo, int index) {
        mRadioResponse.writeSmsToRuimResponse(responseInfo, index);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void deleteSmsOnRuimResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.deleteSmsOnRuimResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param imei IMEI if GSM subscription is available
     * @param imeisv IMEISV if GSM subscription is available
     * @param esn ESN if CDMA subscription is available
     * @param meid MEID if CDMA subscription is available
     */
    public void getDeviceIdentityResponse(RadioResponseInfo responseInfo, String imei,
                                          String imeisv, String esn, String meid) {
        mRadioResponse.getDeviceIdentityResponse(responseInfo, imei, imeisv, esn, meid);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void exitEmergencyCallbackModeResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.exitEmergencyCallbackModeResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param smsc Short Message Service Center address on the device
     */
    public void getSmscAddressResponse(RadioResponseInfo responseInfo, String smsc) {
        mRadioResponse.getSmscAddressResponse(responseInfo, smsc);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSmscAddressResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setSmscAddressResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void reportSmsMemoryStatusResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.reportSmsMemoryStatusResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void reportStkServiceIsRunningResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.reportStkServiceIsRunningResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param source CDMA subscription source
     */
    public void getCdmaSubscriptionSourceResponse(RadioResponseInfo responseInfo, int source) {
        mRadioResponse.getCdmaSubscriptionSourceResponse(responseInfo, source);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param response response string of the challenge/response algo for ISIM auth in base64 format
     */
    public void requestIsimAuthenticationResponse(RadioResponseInfo responseInfo, String response) {
        mRadioResponse.requestIsimAuthenticationResponse(responseInfo, response);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acknowledgeIncomingGsmSmsWithPduResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.acknowledgeIncomingGsmSmsWithPduResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param iccIo IccIoResult as defined in types.hal corresponding to ICC IO response
     */
    public void sendEnvelopeWithStatusResponse(RadioResponseInfo responseInfo,
                                               android.hardware.radio.V1_0.IccIoResult iccIo) {
        mRadioResponse.sendEnvelopeWithStatusResponse(responseInfo, iccIo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param rat Current voice RAT
     */
    public void getVoiceRadioTechnologyResponse(RadioResponseInfo responseInfo, int rat) {
        mRadioResponse.getVoiceRadioTechnologyResponse(responseInfo, rat);
    }

    public void getCellInfoListResponse(RadioResponseInfo responseInfo,
                                        ArrayList<android.hardware.radio.V1_0.CellInfo> cellInfo) {
        mRadioResponse.getCellInfoListResponse(responseInfo, cellInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cellInfo List of current cell information known to radio
     */
    public void getCellInfoListResponse_1_2(
            RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_2.CellInfo> cellInfo) {
        mRadioResponse.responseCellInfoList_1_2(responseInfo, cellInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCellInfoListRateResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setCellInfoListRateResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setInitialAttachApnResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setInitialAttachApnResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param isRegistered false = not registered, true = registered
     * @param ratFamily RadioTechnologyFamily as defined in types.hal. This value is valid only if
     *        isRegistered is true.
     */
    public void getImsRegistrationStateResponse(RadioResponseInfo responseInfo,
                                                boolean isRegistered, int ratFamily) {
        mRadioResponse.getImsRegistrationStateResponse(responseInfo, isRegistered, ratFamily);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to sms sent as defined by SendSmsResult in types.hal
     */
    public void sendImsSmsResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        mRadioResponse.sendImsSmsResponse(responseInfo, sms);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param result IccIoResult as defined in types.hal
     */
    public void iccTransmitApduBasicChannelResponse(RadioResponseInfo responseInfo,
                                                    android.hardware.radio.V1_0.IccIoResult
                                                            result) {
        mRadioResponse.iccTransmitApduBasicChannelResponse(responseInfo, result);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param channelId session id of the logical channel.
     * @param selectResponse Contains the select response for the open channel command with one
     *        byte per integer
     */
    public void iccOpenLogicalChannelResponse(RadioResponseInfo responseInfo, int channelId,
                                              ArrayList<Byte> selectResponse) {
        mRadioResponse.iccOpenLogicalChannelResponse(responseInfo, channelId, selectResponse);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void iccCloseLogicalChannelResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.iccCloseLogicalChannelResponse(responseInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param result IccIoResult as defined in types.hal
     */
    public void iccTransmitApduLogicalChannelResponse(
            RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.IccIoResult result) {
        mRadioResponse.iccTransmitApduLogicalChannelResponse(responseInfo, result);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param result string containing the contents of the NV item
     */
    public void nvReadItemResponse(RadioResponseInfo responseInfo, String result) {
        mRadioResponse.nvReadItemResponse(responseInfo, result);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void nvWriteItemResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.nvWriteItemResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void nvWriteCdmaPrlResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.nvWriteCdmaPrlResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void nvResetConfigResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.nvResetConfigResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setUiccSubscriptionResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setUiccSubscriptionResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setDataAllowedResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setDataAllowedResponse(responseInfo);
    }

    public void getHardwareConfigResponse(
            RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.HardwareConfig> config) {
        mRadioResponse.getHardwareConfigResponse(responseInfo, config);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param result IccIoResult as defined in types.hal
     */
    public void requestIccSimAuthenticationResponse(RadioResponseInfo responseInfo,
                                                    android.hardware.radio.V1_0.IccIoResult
                                                            result) {
        mRadioResponse.requestIccSimAuthenticationResponse(responseInfo, result);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setDataProfileResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setDataProfileResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void requestShutdownResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.requestShutdownResponse(responseInfo);
    }

    public void getRadioCapabilityResponse(RadioResponseInfo responseInfo,
                                           android.hardware.radio.V1_0.RadioCapability rc) {
        mRadioResponse.getRadioCapabilityResponse(responseInfo, rc);
    }

    public void setRadioCapabilityResponse(RadioResponseInfo responseInfo,
                                           android.hardware.radio.V1_0.RadioCapability rc) {
        mRadioResponse.setRadioCapabilityResponse(responseInfo, rc);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param statusInfo LceStatusInfo indicating LCE status
     */
    public void startLceServiceResponse(RadioResponseInfo responseInfo, LceStatusInfo statusInfo) {
        mRadioResponse.startLceServiceResponse(responseInfo, statusInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param statusInfo LceStatusInfo indicating LCE status
     */
    public void stopLceServiceResponse(RadioResponseInfo responseInfo, LceStatusInfo statusInfo) {
        mRadioResponse.stopLceServiceResponse(responseInfo, statusInfo);
    }

    public void pullLceDataResponse(RadioResponseInfo responseInfo, LceDataInfo lceInfo) {
        mRadioResponse.pullLceDataResponse(responseInfo, lceInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param activityInfo modem activity information
     */
    public void getModemActivityInfoResponse(RadioResponseInfo responseInfo,
                                             ActivityStatsInfo activityInfo) {
        mRadioResponse.getModemActivityInfoResponse(responseInfo, activityInfo);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param numAllowed number of allowed carriers which have been set correctly.
     *        On success, it must match the length of list Carriers->allowedCarriers.
     *        if Length of allowed carriers list is 0, numAllowed = 0.
     */
    public void setAllowedCarriersResponse(RadioResponseInfo responseInfo, int numAllowed) {
        mRadioResponse.setAllowedCarriersResponse(responseInfo, numAllowed);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param allAllowed true only when all carriers are allowed. Ignore "carriers" struct.
     *                   If false, consider "carriers" struct
     * @param carriers Carrier restriction information.
     */
    public void getAllowedCarriersResponse(RadioResponseInfo responseInfo, boolean allAllowed,
                                           CarrierRestrictions carriers) {
        mRadioResponse.getAllowedCarriersResponse(responseInfo, allAllowed, carriers);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendDeviceStateResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.sendDeviceStateResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setIndicationFilterResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setIndicationFilterResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSimCardPowerResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setSimCardPowerResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSignalStrengthReportingCriteriaResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setLinkCapacityReportingCriteriaResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void setTrmResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                mRadioResponse.sendMessageResponse(rr.mResult, null);
            }
            mMtkRil.processResponseDone(rr, responseInfo, null);
        }
    }

    // MTK-START: SIM
    /**
     * @param info Response info struct containing response type, serial no. and error.
     * @param response Response string of getATRResponse.
     */
    public void getATRResponse(RadioResponseInfo info, String response) {
        mRadioResponse.responseString(info, response);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error.
     * @param response Response string of getIccidResponse.
     */
    public void getIccidResponse(RadioResponseInfo info, String response) {
        mRadioResponse.responseString(info, response);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error.
     */
    public void setSimPowerResponse(RadioResponseInfo info) {
        mRadioResponse.responseVoid(info);
    }
    // MTK-END

    // MTK-START: NW
    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setNetworkSelectionModeManualWithActResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.setNetworkSelectionModeManualResponse(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param networkInfos List of network operator information as OperatorInfoWithAct defined in
     *      types.hal
     */
    public void getAvailableNetworksWithActResponse(RadioResponseInfo responseInfo,
                                            ArrayList<vendor.mediatek.hardware.
                                            radio.V3_0.OperatorInfoWithAct> networkInfos) {
        responseOperatorInfosWithAct(responseInfo, networkInfos);
    }

    /**
       * @param responseInfo Response info struct containing response type, serial no. and error
       * @param customerSignalStrength list of signalstrength with wcdma ecio
       */
    public void getSignalStrengthWithWcdmaEcioResponse(RadioResponseInfo responseInfo,
            SignalStrengthWithWcdmaEcio signalStrength){
        responseGetSignalStrengthWithWcdmaEcio(responseInfo, signalStrength);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void cancelAvailableNetworksResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    private int getSubId(int phoneId) {
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        int[] subIds = SubscriptionManager.getSubId(phoneId);
        if (subIds != null && subIds.length > 0) {
            subId = subIds[0];
        }
        return subId;
    }

    private void responseOperatorInfosWithAct(RadioResponseInfo responseInfo,
                                            ArrayList<vendor.mediatek.hardware.
                                            radio.V3_0.OperatorInfoWithAct> networkInfos) {
        RILRequest rr = mRadioResponse.mRil.processResponse(responseInfo);

        if (rr != null) {
            ArrayList<OperatorInfo> ret = null;
            if (responseInfo.error == RadioError.NONE) {
                ret =  new ArrayList<OperatorInfo>();
                for (int i = 0; i < networkInfos.size(); i++) {
                    int nLac = -1;
                    mMtkRil.riljLog("responseOperatorInfosWithAct: act:" + networkInfos.get(i).act);
                    mMtkRil.riljLog("responseOperatorInfosWithAct: lac:" + networkInfos.get(i).lac);
                    if (networkInfos.get(i).lac.length() > 0) {
                        nLac = Integer.parseInt(networkInfos.get(i).lac, 16);
                    }
                    networkInfos.get(i).base.alphaLong = mMtkRil.lookupOperatorName(
                            getSubId(mMtkRil.mInstanceId),
                            networkInfos.get(i).base.operatorNumeric, true, nLac);
                    networkInfos.get(i).base.alphaShort = mMtkRil.lookupOperatorName(
                            getSubId(mMtkRil.mInstanceId),
                            networkInfos.get(i).base.operatorNumeric, false, nLac);
                    /* Show Act info(ex: "2G","3G","4G") for PLMN list result */
                    networkInfos.get(i).base.alphaLong = networkInfos.get(i).base.
                            alphaLong.concat(" " + networkInfos.get(i).act);
                    networkInfos.get(i).base.alphaShort = networkInfos.get(i).base.
                            alphaShort.concat(" " + networkInfos.get(i).act);

                    if (!mMtkRil.hidePLMN(networkInfos.get(i).base.operatorNumeric)) {
                        ret.add(new OperatorInfo(networkInfos.get(i).base.alphaLong,
                                networkInfos.get(i).base.alphaShort,
                                networkInfos.get(i).base.operatorNumeric,
                                mRadioResponse.convertOpertatorInfoToString(
                                        networkInfos.get(i).base.status)));
                    } else {
                        mMtkRil.riljLog("remove this one " +
                                networkInfos.get(i).base.operatorNumeric);
                    }
                }
                mRadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRadioResponse.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseGetSignalStrengthWithWcdmaEcio(
            RadioResponseInfo responseInfo,
            SignalStrengthWithWcdmaEcio signalStrength) {
        RILRequest rr = mRadioResponse.mRil.processResponse(responseInfo);

        if (rr != null) {
            // To Do: this SignalStrength should be customer signalStrength class or struct
            SignalStrength ret = new SignalStrength (
                    signalStrength.gsm_signalStrength,
                    signalStrength.gsm_bitErrorRate,
                    signalStrength.wcdma_rscp,
                    signalStrength.wcdma_ecio,
                    signalStrength.cdma_dbm,
                    signalStrength.cdma_ecio,
                    signalStrength.evdo_dbm,
                    signalStrength.evdo_ecio,
                    signalStrength.evdo_signalNoiseRatio,
                    signalStrength.lte_signalStrength,
                    signalStrength.lte_rsrp,
                    signalStrength.lte_rsrq,
                    signalStrength.lte_rssnr,
                    signalStrength.lte_cqi,
                    signalStrength.tdscdma_rscp);
            if (responseInfo.error == RadioError.NONE) {
                mRadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mMtkRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    // MTK-END: NW

    /*
    * @param responseInfo Response info struct containing response type, serial no. and error
    */
    public void setModemPowerResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    // SMS-START
    public void getSmsParametersResponse(
            RadioResponseInfo responseInfo, SmsParams params) {
        responseSmsParams(responseInfo, params);
    }

    private void responseSmsParams(RadioResponseInfo responseInfo, SmsParams params) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            Object ret = null;
            if (responseInfo.error == RadioError.NONE) {
                MtkSmsParameters smsp = new MtkSmsParameters(
                        params.format, params.vp, params.pid, params.dcs);
                mMtkRil.riljLog("responseSmsParams: from HIDL: " + smsp);
                ret = smsp;
                mRadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mMtkRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    public void setSmsParametersResponse(
            RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }
    public void setEtwsResponse(
            RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void removeCbMsgResponse(
            RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }


    public void getSmsMemStatusResponse(
            RadioResponseInfo responseInfo, SmsMemStatus params) {
        responseSmsMemStatus(responseInfo, params);
    }

    private void responseSmsMemStatus(RadioResponseInfo responseInfo, SmsMemStatus params) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            Object ret = null;
            if (responseInfo.error == RadioError.NONE) {
                MtkIccSmsStorageStatus status = new MtkIccSmsStorageStatus(
                        params.used, params.total);
                mMtkRil.riljLog("responseSmsMemStatus: from HIDL: " + status);
                ret = status;
                mRadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mMtkRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    public void setGsmBroadcastLangsResponse(
            RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void getGsmBroadcastLangsResponse(
            RadioResponseInfo responseInfo, String langs) {
        mRadioResponse.responseString(responseInfo, langs);
    }

    public void getGsmBroadcastActivationRsp(RadioResponseInfo responseInfo,
            int activation) {
        mRadioResponse.responseInts(responseInfo, activation);
    }

    public void setSmsFwkReadyRsp(RadioResponseInfo responseInfo) {
        mMtkRil.riljLog("setSmsFwkReadyRsp: from HIDL");
        mRadioResponse.responseVoid(responseInfo);
    }
    // SMS-END

    /// M: eMBMS feature.
    /**
     * The response of sendEmbmsAtCommand.
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param result response string
     */
    public void sendEmbmsAtCommandResponse(RadioResponseInfo responseInfo, String result) {
        mRadioResponse.responseString(responseInfo, result);
    }
    /// M: eMBMS end

    /// M: CC: call control part @{
    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void hangupAllResponse(RadioResponseInfo responseInfo)
            throws RemoteException {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCallIndicationResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void emergencyDialResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setEccServiceCategoryResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setEccListResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setVoicePreferStatusResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setEccNumResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void getEccNumResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }
    /// M: CC: @}

    /// M: CC: Vzw/CTVolte ECC
    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void currentStatusResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void eccPreferredRatResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    // APC-Start
    public void setApcModeResponse(
            RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void getApcInfoResponse(
            RadioResponseInfo responseInfo, ArrayList<Integer> cellInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            int[] response = new int[cellInfo.size()];
            if (responseInfo.error == RadioError.NONE) {
                for (int i = 0; i < cellInfo.size(); i++) {
                    response[i] = cellInfo.get(i);
                }
                mRadioResponse.sendMessageResponse(rr.mResult, response);
            }
            mMtkRil.processResponseDone(rr, responseInfo, response);
        }
    }
    // APC-End

    public void triggerModeSwitchByEccResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                mRadioResponse.sendMessageResponse(rr.mResult, null);
            }
            mMtkRil.processResponseDone(rr, responseInfo, null);
        }
    }

    @Override
    public void getSmsRuimMemoryStatusResponse(RadioResponseInfo responseInfo,
            SmsMemStatus memStatus) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            MtkIccSmsStorageStatus ret = null;
            if (responseInfo.error == RadioError.NONE) {
                ret = new MtkIccSmsStorageStatus(memStatus.used, memStatus.total);
                mRadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mMtkRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    @Override
    public void setFdModeResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setResumeRegistrationResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);
        if (rr != null) {
            mRadioResponse.sendMessageResponse(rr.mResult, null);
        }
        mMtkRil.processResponseDone(rr, responseInfo, null);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void storeModemTypeResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);
        if (rr != null) {
            mRadioResponse.sendMessageResponse(rr.mResult, null);
        }
        mMtkRil.processResponseDone(rr, responseInfo, null);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void reloadModemTypeResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);
        if (rr != null) {
            mRadioResponse.sendMessageResponse(rr.mResult, null);
        }
        mMtkRil.processResponseDone(rr, responseInfo, null);
    }

    public void handleStkCallSetupRequestFromSimWithResCodeResponse(
            RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setPdnReuseResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setOverrideApnResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setPdnNameReuseResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }


    // PHB START
    public void queryPhbStorageInfoResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> storageInfo) {
        mRadioResponse.responseIntArrayList(responseInfo, storageInfo);
    }

    public void writePhbEntryResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void readPhbEntryResponse(RadioResponseInfo responseInfo,
            ArrayList<PhbEntryStructure> phbEntry) {
        responsePhbEntries(responseInfo, phbEntry);
    }

    private void
    responsePhbEntries(RadioResponseInfo responseInfo, ArrayList<PhbEntryStructure> phbEntry) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            PhbEntry[] ret = null;
            if (responseInfo.error == RadioError.NONE) {
                ret = new PhbEntry[phbEntry.size()];
                for (int i = 0; i < phbEntry.size(); i++) {
                    ret[i] = new PhbEntry();
                    ret[i].type = phbEntry.get(i).type;
                    ret[i].index = phbEntry.get(i).index;
                    ret[i].number = phbEntry.get(i).number;
                    ret[i].ton = phbEntry.get(i).ton;
                    ret[i].alphaId = phbEntry.get(i).alphaId;
                }
                mRadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mMtkRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    public void queryUPBCapabilityResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> upbCapability) {
        mRadioResponse.responseIntArrayList(responseInfo, upbCapability);
    }

    public void editUPBEntryResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void deleteUPBEntryResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void readUPBGasListResponse(RadioResponseInfo responseInfo, ArrayList<String> gasList) {
        mRadioResponse.responseStringArrayList(mRadioResponse.mRil, responseInfo, gasList);
    }

    public void readUPBGrpEntryResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> grpEntries) {
        mRadioResponse.responseIntArrayList(responseInfo, grpEntries);
    }

    public void writeUPBGrpEntryResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void getPhoneBookStringsLengthResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> stringLength) {
        mRadioResponse.responseIntArrayList(responseInfo, stringLength);
    }

    public void getPhoneBookMemStorageResponse(RadioResponseInfo responseInfo,
            PhbMemStorageResponse phbMemStorage) {
        responseGetPhbMemStorage(responseInfo, phbMemStorage);
    }

    private void
    responseGetPhbMemStorage(RadioResponseInfo responseInfo, PhbMemStorageResponse phbMemStorage) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            PBMemStorage ret = new PBMemStorage();
            if (responseInfo.error == RadioError.NONE) {
                ret.setStorage(phbMemStorage.storage);
                ret.setUsed(phbMemStorage.used);
                ret.setTotal(phbMemStorage.total);
                mRadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mMtkRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    public void setPhoneBookMemStorageResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void readPhoneBookEntryExtResponse(RadioResponseInfo responseInfo,
            ArrayList<PhbEntryExt> phbEntryExts) {
        responseReadPhbEntryExt(responseInfo, phbEntryExts);
    }

    private void responseCallForwardInfoEx(RadioResponseInfo responseInfo,
            ArrayList<vendor.mediatek.hardware.radio.V3_0.CallForwardInfoEx> callForwardInfoExs) {
        long[] timeSlot;
        String[] timeSlotStr;
        RILRequest rr = mMtkRil.processResponse(responseInfo);
        if (rr != null) {
            MtkCallForwardInfo[] ret = new MtkCallForwardInfo[callForwardInfoExs.size()];
            for (int i = 0; i < callForwardInfoExs.size(); i++) {
                timeSlot = new long[2];
                timeSlotStr = new String[2];

                ret[i] = new MtkCallForwardInfo();
                ret[i].status = callForwardInfoExs.get(i).status;
                ret[i].reason = callForwardInfoExs.get(i).reason;
                ret[i].serviceClass = callForwardInfoExs.get(i).serviceClass;
                ret[i].toa = callForwardInfoExs.get(i).toa;
                ret[i].number = callForwardInfoExs.get(i).number;
                ret[i].timeSeconds = callForwardInfoExs.get(i).timeSeconds;
                timeSlotStr[0] = callForwardInfoExs.get(i).timeSlotBegin;
                timeSlotStr[1] = callForwardInfoExs.get(i).timeSlotEnd;

                if (timeSlotStr[0] == null || timeSlotStr[1] == null) {
                    ret[i].timeSlot = null;
                } else {
                    // convert to local time
                    for (int j = 0; j < 2; j++) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
                        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                        try {
                            Date date = dateFormat.parse(timeSlotStr[j]);
                            timeSlot[j] = date.getTime();
                        } catch (ParseException e) {
                            e.printStackTrace();
                            timeSlot = null;
                            break;
                        }
                    }
                    ret[i].timeSlot = timeSlot;
                }
            }
            if (responseInfo.error == RadioError.NONE) {
                mRadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mMtkRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void
    responseReadPhbEntryExt(RadioResponseInfo responseInfo, ArrayList<PhbEntryExt> phbEntryExts) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            PBEntry[] ret = null;
            if (responseInfo.error == RadioError.NONE) {
                ret = new PBEntry[phbEntryExts.size()];
                for (int i = 0; i < phbEntryExts.size(); i++) {
                    ret[i] = new PBEntry();
                    ret[i].setIndex1(phbEntryExts.get(i).type);
                    ret[i].setNumber(phbEntryExts.get(i).number);
                    ret[i].setType(phbEntryExts.get(i).type);
                    ret[i].setText(phbEntryExts.get(i).text);
                    ret[i].setHidden(phbEntryExts.get(i).hidden);
                    ret[i].setGroup(phbEntryExts.get(i).group);
                    ret[i].setAdnumber(phbEntryExts.get(i).adnumber);
                    ret[i].setAdtype(phbEntryExts.get(i).adtype);
                    ret[i].setSecondtext(phbEntryExts.get(i).secondtext);
                    ret[i].setEmail(phbEntryExts.get(i).email);
                }
                mRadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mMtkRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    public void writePhoneBookEntryExtResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void queryUPBAvailableResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> upbAvailable) {
        mRadioResponse.responseIntArrayList(responseInfo, upbAvailable);
    }

    public void readUPBEmailEntryResponse(RadioResponseInfo responseInfo, String email) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                mRadioResponse.sendMessageResponse(rr.mResult, email);
            }
            if ((isUserLoad == true) && (responseInfo.error == 0)) {
                String str = "xxx@email.com";
                mMtkRil.processResponseDone(rr, responseInfo, str);
            } else {
                mMtkRil.processResponseDone(rr, responseInfo, email);
            }
        }
    }

    public void readUPBSneEntryResponse(RadioResponseInfo responseInfo, String sne) {
        mRadioResponse.responseString(responseInfo, sne);
    }

    public void readUPBAnrEntryResponse(RadioResponseInfo responseInfo,
            ArrayList<PhbEntryStructure> anrs) {
        responsePhbEntries(responseInfo, anrs);
    }

    public void readUPBAasListResponse(RadioResponseInfo responseInfo, ArrayList<String> aasList) {
        mRadioResponse.responseStringArrayList(mRadioResponse.mRil, responseInfo, aasList);
    }

    public void setPhonebookReadyResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }
    // PHB END

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void resetRadioResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void restartRILDResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    //MTK-START Femtocell (CSG)
    /**
     * Femtocell list response.
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param femtoList response femtocell list.
     */
    public void getFemtocellListResponse(RadioResponseInfo responseInfo,
            ArrayList<String> femtoList) {
        responseFemtoCellInfos(responseInfo, femtoList);
    }

    /**
     * Cancel femtocell list response.
     * @param responseInfo Response info struct containing response type, serial no. and error.
     */
    public void abortFemtocellListResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * Select femtocell list response.
     * @param responseInfo Response info struct containing response type, serial no. and error.
     */
    public void selectFemtocellResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * Query femtocell system selection mode response.
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param mode response femtocell system selection mode.
     */
    public void queryFemtoCellSystemSelectionModeResponse(RadioResponseInfo responseInfo,
            int mode) {
        mRadioResponse.responseInts(responseInfo, mode);
    }

    // MTK-START: SIM ME LOCK
    /**
     * @param info Response info struct containing response type, serial no. and error.
     */
    public void queryNetworkLockResponse(RadioResponseInfo info, int catagory,
            int state, int retry_cnt, int autolock_cnt, int num_set, int total_set,
            int key_state) {
        mRadioResponse.responseInts(info, catagory, state, retry_cnt, autolock_cnt,
                num_set, total_set, key_state);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error.
     */
    public void setNetworkLockResponse(RadioResponseInfo info) {
        mRadioResponse.responseVoid(info);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyDeviceNetworkDepersonalizationResponse(RadioResponseInfo responseInfo,
            int retriesRemaining) {
        mRadioResponse.responseInts(responseInfo, retriesRemaining);
    }
    // MTK-END

    /**
     * Set femtocell system selection mode response.
     * @param responseInfo Response info struct containing response type, serial no. and error.
     */
    public void setFemtoCellSystemSelectionModeResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    private void
    responseFemtoCellInfos(RadioResponseInfo responseInfo,
            ArrayList<String> info) {
        RILRequest rr = mRadioResponse.mRil.processResponse(responseInfo);
        ArrayList<FemtoCellInfo> femtoInfos = null;
        int size_femto = 7;

        String fPlmn = null;
        String fAct = null;
        String fCsgId = null;
        Phone phone = PhoneFactory.getPhone(mMtkRil.mInstanceId);
        if (phone != null) {
            ServiceStateTracker sst = phone.getServiceStateTracker();
            ServiceState ss = sst.mSS;
            fPlmn = ((MtkServiceStateTracker) sst).getFemtoPlmn();
            fAct = ((MtkServiceStateTracker) sst).getFemtoAct();
            fCsgId = ((MtkServiceStateTracker) sst).getFemtoCsgId();
        }

        if (rr != null && responseInfo.error == RadioError.NONE) {
            String[] strings = null;

            strings = new String[info.size()];
            for (int i = 0; i < info.size(); i++) {
                strings[i] = info.get(i);
            }

            if (strings.length % size_femto != 0) {
                throw new RuntimeException(
                    "responseFemtoCellInfos: invalid response. Got "
                    + strings.length + " strings, expected multible of " + size_femto);
            }
            femtoInfos = new ArrayList<FemtoCellInfo>(strings.length / size_femto);

            /* <plmn numeric>,<act>,<plmn long alpha name>,<csgId>,,csgIconType>,<hnbName>,<sig> */
            for (int i = 0 ; i < strings.length ; i += size_femto) {
                String actStr;
                String hnbName;
                int rat;
                int sig = 0;
                boolean con = false; // plmn, act, csgId are the same.

                /* ALPS00273663 handle UCS2 format : prefix + hex string ex: "uCs2806F767C79D1" */
                if ((strings[i + 1] != null) && (strings[i + 1].startsWith("uCs2") == true))
                {
                    mMtkRil.riljLog("responseFemtoCellInfos handling UCS2 format name");

                    try {
                        strings[i + 0] = new String(
                                IccUtils.hexStringToBytes(strings[i + 1].substring(4)), "UTF-16");
                    } catch (UnsupportedEncodingException ex) {
                        mMtkRil.riljLog("responseFemtoCellInfos UnsupportedEncodingException");
                    }
                }

                if (strings[i + 1] != null
                        && (strings[i + 1].equals("") || strings[i + 1].equals(strings[i + 0]))) {
                    mMtkRil.riljLog(
                            "lookup RIL responseFemtoCellInfos() for plmn id= " + strings[i + 0]);
                    strings[i + 1] = mMtkRil.lookupOperatorName(getSubId(mMtkRil.mInstanceId),
                            strings[i + 0], true, -1);
                }

                sig = Integer.valueOf(strings[i + 6]).intValue();

                if (strings[i + 2].equals("7")) {
                    actStr = "4G";
                    rat = ServiceState.RIL_RADIO_TECHNOLOGY_LTE;
                } else if (strings[i + 2].equals("2")) {
                    actStr = "3G";
                    rat = ServiceState.RIL_RADIO_TECHNOLOGY_UMTS;
                } else {
                    actStr = "2G";
                    rat = ServiceState.RIL_RADIO_TECHNOLOGY_GPRS;
                }
                strings[i + 1] = strings[i + 1].concat(" " + actStr);

                hnbName = new String(IccUtils.hexStringToBytes(strings[i + 5]));

                if ((strings[i + 0] != null && strings[i + 0].equals(fPlmn)) &&  // Plmn
                    (strings[i + 2] != null && strings[i + 2].equals(fAct)) &&  // Act
                    (strings[i + 3] != null && strings[i + 3].equals(fCsgId))) {  // CsgId
                    con = true;
                }
                mMtkRil.riljLog("FemtoCellInfo(" + strings[i + 3] + "," + strings[i + 4] + ","
                                + strings[i + 5] + "," + strings[i + 0] + "," + strings[i + 1]
                                + "," + rat + ")" + " hnbName=" + hnbName + ",sig=" + sig
                                + ",con=" + con);
                femtoInfos.add(
                    new FemtoCellInfo(
                        Integer.parseInt(strings[i + 3]),
                        Integer.parseInt(strings[i + 4]),
                        hnbName,
                        strings[i + 0],
                        strings[i + 1],
                        rat,
                        con,
                        sig));
            }
            mRadioResponse.sendMessageResponse(rr.mResult, femtoInfos);
        }
        mMtkRil.processResponseDone(rr, responseInfo, femtoInfos);
    }
    //MTK-END Femtocell (CSG)

    // M: [LTE][Low Power][UL traffic shaping] @{
    public void setLteAccessStratumReportResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                mRadioResponse.sendMessageResponse(rr.mResult, null);
            }
            mMtkRil.processResponseDone(rr, responseInfo, null);
        }
    }

    public void setLteUplinkDataTransferResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                mRadioResponse.sendMessageResponse(rr.mResult, null);
            }
            mMtkRil.processResponseDone(rr, responseInfo, null);
        }
    }
    // M: [LTE][Low Power][UL traffic shaping] @}

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param respAntConf Ant configuration
     */
    public void setRxTestConfigResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> respAntConf) {
        mRadioResponse.responseIntArrayList(responseInfo, respAntConf);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param respAntInfo Ant Info
     */
    public void getRxTestResultResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> respAntInfo) {
        mRadioResponse.responseIntArrayList(responseInfo, respAntInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param polCapability the order number of operator in the SIM/USIM preferred operator list
     */
    public void getPOLCapabilityResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> polCapability) {
        mRadioResponse.responseIntArrayList(responseInfo, polCapability);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param polList the SIM/USIM preferred operator list
     */
    public void getCurrentPOLListResponse(RadioResponseInfo responseInfo,
            ArrayList<String> polList) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);
        ArrayList<NetworkInfoWithAcT> NetworkInfos = null;
        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                if (polList.size() % 4 != 0) {
                    mMtkRil.riljLog("RIL_REQUEST_GET_POL_LIST: invalid response. Got "
                        + polList.size() + " strings, expected multible of 4");
                } else {
                    NetworkInfos = new ArrayList<NetworkInfoWithAcT>(polList.size() / 4);
                    String strOperName = null;
                    String strOperNumeric = null;
                    int nAct = 0;
                    int nIndex = 0;
                    for (int i = 0; i < polList.size(); i+=4) {
                        strOperName = null;
                        strOperNumeric = null;
                        nAct = 0;
                        nIndex = 0;
                        if (polList.get(i) != null) nIndex = Integer.parseInt(polList.get(i));
                        if (polList.get(i+1) != null) {
                            int format = Integer.parseInt(polList.get(i+1));
                            switch (format) {
                                case 0:
                                case 1:
                                    strOperName = polList.get(i+2);
                                    break;
                                case 2:
                                    if (polList.get(i+2) != null) {
                                        strOperNumeric = polList.get(i+2);
                                        strOperName = mMtkRil.lookupOperatorName(
                                                getSubId(mMtkRil.mInstanceId),
                                                strOperNumeric, true, -1);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                        if (polList.get(i+3) != null) nAct = Integer.parseInt(polList.get(i+3));
                        if (strOperNumeric != null && !strOperNumeric.equals("?????")) {
                            NetworkInfos.add(
                                new NetworkInfoWithAcT(
                                    strOperName,
                                    strOperNumeric,
                                    nAct,
                                    nIndex));
                        }
                    }
                    mRadioResponse.sendMessageResponse(rr.mResult, NetworkInfos);
                }
            }
            mMtkRil.processResponseDone(rr, responseInfo, NetworkInfos);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setPOLEntryResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    // M: Data Framework - common part enhancement
    public void syncDataSettingsToMdResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    // M: Data Framework - Data Retry enhancement
    public void resetMdDataRetryCountResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }
    // M: Data Framework - CC 33
    public void setRemoveRestrictEutranModeResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /// M: [Network][C2K] Sprint roaming control @{
    public void setRoamingEnableResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void getRoamingEnableResponse(RadioResponseInfo responseInfo, ArrayList<Integer> data) {
        mRadioResponse.responseIntArrayList(responseInfo, data);
    }
    /// @}

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setLteReleaseVersionResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param mode setting mode of lte release version.
     */
    public void getLteReleaseVersionResponse(RadioResponseInfo responseInfo, int mode) {
        mRadioResponse.responseInts(responseInfo, mode);
    }

    // External SIM [Start]
    public void vsimNotificationResponse(RadioResponseInfo info, VsimEvent event) {
        RILRequest rr = mMtkRil.processResponse(info);

        if (rr != null) {
            Object ret = null;
            if (info.error == RadioError.NONE) {
                ret = (Object) event.transactionId;
                mRadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mMtkRil.processResponseDone(rr, info, ret);
        }
    }

    public void vsimOperationResponse(RadioResponseInfo info) {
        mRadioResponse.responseVoid(info);
    }
    // External SIM [End]

    public void setWifiEnabledResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            mRadioResponse.sendMessageResponse(rr.mResult, null);
        }
        mMtkRil.processResponseDone(rr, responseInfo, null);
    }

    public void setWifiAssociatedResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            mRadioResponse.sendMessageResponse(rr.mResult, null);
        }
        mMtkRil.processResponseDone(rr, responseInfo, null);
    }

    public void setWifiSignalLevelResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            mRadioResponse.sendMessageResponse(rr.mResult, null);
        }
        mMtkRil.processResponseDone(rr, responseInfo, null);
    }

    public void setWifiIpAddressResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            mRadioResponse.sendMessageResponse(rr.mResult, null);
        }
        mMtkRil.processResponseDone(rr, responseInfo, null);
    }

    public void setLocationInfoResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            mRadioResponse.sendMessageResponse(rr.mResult, null);
        }
        mMtkRil.processResponseDone(rr, responseInfo, null);
    }

    public void setEmergencyAddressIdResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            mRadioResponse.sendMessageResponse(rr.mResult, null);
        }
        mMtkRil.processResponseDone(rr, responseInfo, null);
    }

    public void setNattKeepAliveStatusResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            mRadioResponse.sendMessageResponse(rr.mResult, null);
        }
        mMtkRil.processResponseDone(rr, responseInfo, null);
    }

    public void setWifiPingResultResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            mRadioResponse.sendMessageResponse(rr.mResult, null);
        }
        mMtkRil.processResponseDone(rr, responseInfo, null);
    }

    public void setE911StateResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void setServiceStateToModemResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param data Data returned by oem
     */
    public void sendRequestRawResponse(RadioResponseInfo responseInfo, ArrayList<Byte> data) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);

        if (rr != null) {
            byte[] ret = null;
            if (responseInfo.error == RadioError.NONE) {
                ret = RIL.arrayListToPrimitiveArray(data);
                mRadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mMtkRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    public void setTxPowerStatusResponse(RadioResponseInfo responseInfo,
            ArrayList<Byte> data) {
        RILRequest rr = mMtkRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                mRadioResponse.sendMessageResponse(rr.mResult, null);
            }
            mMtkRil.processResponseDone(rr, responseInfo, null);
        } else {
            mMtkRil.riljLog("setTxPowerStatusResponse, rr is null");
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param data Data returned by oem
     */
    public void sendRequestStringsResponse(RadioResponseInfo responseInfo, ArrayList<String> data) {
        mRadioResponse.responseStringArrayList(mMtkRil, responseInfo, data);
    }

    public void dataConnectionAttachResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void dataConnectionDetachResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void resetAllConnectionsResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void reportAirplaneModeResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void reportSimModeResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void setSilentRebootResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void setPropImsHandoverResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void setOperatorConfigurationResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void setSuppServPropertyResponse(RadioResponseInfo responseInfo) {
        mRadioResponse.responseVoid(responseInfo);
    }

    public void getSuppServPropertyResponse(RadioResponseInfo responseInfo, String value) {
        mRadioResponse.responseString(responseInfo, value);
    }
}
