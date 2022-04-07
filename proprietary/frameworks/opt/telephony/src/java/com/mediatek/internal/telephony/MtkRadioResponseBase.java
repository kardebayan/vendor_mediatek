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

import com.android.internal.telephony.RIL;

import vendor.mediatek.hardware.radio.V3_0.IRadioResponse;
import vendor.mediatek.hardware.radio.V3_0.PhbEntryExt;
import vendor.mediatek.hardware.radio.V3_0.PhbEntryStructure;
import vendor.mediatek.hardware.radio.V3_0.PhbMemStorageResponse;

import android.hardware.radio.V1_0.ActivityStatsInfo;
import android.hardware.radio.V1_0.CardStatus;
import android.hardware.radio.V1_0.CarrierRestrictions;
import android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.DataRegStateResult;
import android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.LastCallFailCauseInfo;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.LceStatusInfo;
import android.hardware.radio.V1_0.NeighboringCell;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.SendSmsResult;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.VoiceRegStateResult;
import android.hardware.radio.V1_1.KeepaliveStatus;
import android.os.RemoteException;
import java.util.ArrayList;

// SMS-START
import vendor.mediatek.hardware.radio.V3_0.SmsParams;
import vendor.mediatek.hardware.radio.V3_0.SmsMemStatus;
// SMS-END

// External SIM [Start]
import vendor.mediatek.hardware.radio.V3_0.VsimEvent;
// External SIM [End]

// NW
import vendor.mediatek.hardware.radio.V3_0.SignalStrengthWithWcdmaEcio;

public class MtkRadioResponseBase extends IRadioResponse.Stub {

    public MtkRadioResponseBase(RIL ril) {
    }

    /**
     * Acknowledge the receipt of radio request sent to the vendor. This must be sent only for
     * radio request which take long time to respond.
     * For more details, refer https://source.android.com/devices/tech/connect/ril.html
     *
     * @param serial Serial no. of the request whose acknowledgement is sent.
     */
    public void acknowledgeRequest(int serial) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cardStatus ICC card status as defined by CardStatus in types.hal
     */
    public void getIccCardStatusResponse(RadioResponseInfo responseInfo, CardStatus cardStatus) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cardStatus ICC card status as defined by CardStatus in 1.2/types.hal
     */
    public void getIccCardStatusResponse_1_2(RadioResponseInfo responseInfo,
                                             android.hardware.radio.V1_2.CardStatus cardStatus) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPinForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPukForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPin2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPuk2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void changeIccPinForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void changeIccPin2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param retriesRemaining Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyNetworkDepersonalizationResponse(RadioResponseInfo responseInfo,
                                                       int retriesRemaining) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void dialResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param imsi String containing the IMSI
     */
    public void getIMSIForAppResponse(RadioResponseInfo responseInfo, String imsi) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void hangupConnectionResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void hangupWaitingOrBackgroundResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void hangupForegroundResumeBackgroundResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void switchWaitingOrHoldingAndActiveResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void conferenceResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void rejectCallResponse(RadioResponseInfo responseInfo) {
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
    }

    public void getSignalStrengthResponse(RadioResponseInfo responseInfo,
                                          android.hardware.radio.V1_0.SignalStrength sigStrength) {
    }

    public void getSignalStrengthResponse_1_2(RadioResponseInfo responseInfo,
                                       android.hardware.radio.V1_2.SignalStrength signalStrength) {
    }

    /*
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param voiceRegResponse Current Voice registration response as defined by VoiceRegStateResult
     *        in types.hal
     */
    public void getVoiceRegistrationStateResponse(RadioResponseInfo responseInfo,
                                                  VoiceRegStateResult voiceRegResponse) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param voiceRegResponse Current Voice registration response as defined by VoiceRegStateResult
     *        in 1.2/types.hal
     */
    public void getVoiceRegistrationStateResponse_1_2(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_2.VoiceRegStateResult voiceRegResponse) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataRegResponse Current Data registration response as defined by DataRegStateResult in
     *        types.hal
     */
    public void getDataRegistrationStateResponse(RadioResponseInfo responseInfo,
                                                 DataRegStateResult dataRegResponse) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataRegResponse Current Data registration response as defined by DataRegStateResult in
     *        1.2/types.hal
     */
    public void getDataRegistrationStateResponse_1_2(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_2.DataRegStateResult dataRegResponse) {
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
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setRadioPowerResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendDtmfResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to sms sent as defined by SendSmsResult in types.hal
     */
    public void sendSmsResponse(RadioResponseInfo responseInfo,
                                SendSmsResult sms) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to sms sent as defined by SendSmsResult in types.hal
     */
    public void sendSMSExpectMoreResponse(RadioResponseInfo responseInfo,
                                          SendSmsResult sms) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param setupDataCallResult Response to data call setup as defined by setupDataCallResult in
     *                            types.hal
     */
    public void setupDataCallResponse(RadioResponseInfo responseInfo,
                                      SetupDataCallResult setupDataCallResult) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param iccIo ICC io operation response as defined by IccIoResult in types.hal
     */
    public void iccIOForAppResponse(RadioResponseInfo responseInfo,
                            android.hardware.radio.V1_0.IccIoResult iccIo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendUssdResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void cancelPendingUssdResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param n is "n" parameter from TS 27.007 7.7
     * @param m is "m" parameter from TS 27.007 7.7
     */
    public void getClirResponse(RadioResponseInfo responseInfo, int n, int m) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setClirResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param callForwardInfos points to a vector of CallForwardInfo, one for
     *        each distinct registered phone number.
     */
    public void getCallForwardStatusResponse(RadioResponseInfo responseInfo,
                                             ArrayList<android.hardware.radio.V1_0.CallForwardInfo>
                                                     callForwardInfos) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCallForwardResponse(RadioResponseInfo responseInfo) {
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
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCallWaitingResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acknowledgeLastIncomingGsmSmsResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acceptCallResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void deactivateDataCallResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param response 0 is the TS 27.007 service class bit vector of
     *        services for which the specified barring facility
     *        is active. "0" means "disabled for all"
     */
    public void getFacilityLockForAppResponse(RadioResponseInfo responseInfo, int response) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param retry 0 is the number of retries remaining, or -1 if unknown
     */
    public void setFacilityLockForAppResponse(RadioResponseInfo responseInfo, int retry) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setBarringPasswordResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param selection false for automatic selection, true for manual selection
     */
    public void getNetworkSelectionModeResponse(RadioResponseInfo responseInfo, boolean selection) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setNetworkSelectionModeAutomaticResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setNetworkSelectionModeManualResponse(RadioResponseInfo responseInfo) {
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
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void startNetworkScanResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void stopNetworkScanResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void startDtmfResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void stopDtmfResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param version string containing version string for log reporting
     */
    public void getBasebandVersionResponse(RadioResponseInfo responseInfo, String version) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void separateConnectionResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setMuteResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param enable true for "mute enabled" and false for "mute disabled"
     */
    public void getMuteResponse(RadioResponseInfo responseInfo, boolean enable) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param status indicates CLIP status
     */
    public void getClipResponse(RadioResponseInfo responseInfo, int status) {
    }

    /* MTK SS Feature : Start */
    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setClipResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param n Colp status in network, "0" means disabled, "1" means enabled
     * @param m Service status, "0" means not provisioned, "1" means provisioned in permanent mode
     */
    public void getColpResponse(RadioResponseInfo responseInfo, int n, int m) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param status indicates COLR status. "0" means not provisioned, "1" means provisioned,
     *        "2" means unknown
     */
    public void getColrResponse(RadioResponseInfo responseInfo, int status) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param n CNAP status, "0" means disabled, "1" means enabled.
     * @param m Service status, "0" means not provisioned, "1" means provisioned, "2" means unknown
     */
    public void sendCnapResponse(RadioResponseInfo responseInfo, int n, int m) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setColpResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setColrResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param callForwardInfoExs points to a vector of CallForwardInfoEx, one for
     *        each distinct registered phone number.
     */
    public void queryCallForwardInTimeSlotStatusResponse(RadioResponseInfo responseInfo,
            ArrayList<vendor.mediatek.hardware.radio.V3_0.CallForwardInfoEx> callForwardInfoExs) {
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     *
     */
    public void setCallForwardInTimeSlotResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param resList points to a vector of String.
     */
    public void runGbaAuthenticationResponse(RadioResponseInfo responseInfo,
                                             ArrayList<String> resList) {
    }
    /* MTK SS Feature : End */

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataCallResultList Response to get data call list as defined by setupDataCallResult in
     *                           types.hal
     */
    public void getDataCallListResponse(RadioResponseInfo responseInfo,
                                        ArrayList<SetupDataCallResult> dataCallResultList) {
    }

    public void sendOemRilRequestRawResponse(RadioResponseInfo responseInfo,
                                             ArrayList<Byte> var2) {}

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSuppServiceNotificationsResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param index record index where the message is stored
     */
    public void writeSmsToSimResponse(RadioResponseInfo responseInfo, int index) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void deleteSmsOnSimResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setBandModeResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param bandModes List of RadioBandMode listing supported modes
     */
    public void getAvailableBandModesResponse(RadioResponseInfo responseInfo,
                                              ArrayList<Integer> bandModes) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param commandResponse SAT/USAT response in hexadecimal format
     *        string starting with first byte of response
     */
    public void sendEnvelopeResponse(RadioResponseInfo responseInfo, String commandResponse) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendTerminalResponseToSimResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void handleStkCallSetupRequestFromSimResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setPdnReuseResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setOverrideApnResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setPdnNameReuseResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void explicitCallTransferResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setPreferredNetworkTypeResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param nwType RadioPreferredNetworkType defined in types.hal
     */
    public void getPreferredNetworkTypeResponse(RadioResponseInfo responseInfo, int nwType) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cells Vector of neighboring radio cell information
     */
    public void getNeighboringCidsResponse(RadioResponseInfo responseInfo,
                                           ArrayList<NeighboringCell> cells) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setLocationUpdatesResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaSubscriptionSourceResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaRoamingPreferenceResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param type CdmaRoamingType defined in types.hal
     */
    public void getCdmaRoamingPreferenceResponse(RadioResponseInfo responseInfo, int type) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setTTYModeResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param mode TTY mode
     */
    public void getTTYModeResponse(RadioResponseInfo responseInfo, int mode) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setPreferredVoicePrivacyResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param enable false for Standard Privacy Mode (Public Long Code Mask)
     *        true for Enhanced Privacy Mode (Private Long Code Mask)
     */
    public void getPreferredVoicePrivacyResponse(RadioResponseInfo responseInfo,
                                                 boolean enable) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendCDMAFeatureCodeResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendBurstDtmfResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Sms result struct as defined by SendSmsResult in types.hal
     */
    public void sendCdmaSmsResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acknowledgeLastIncomingCdmaSmsResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param configs Vector of GSM/WCDMA Cell broadcast configs
     */
    public void getGsmBroadcastConfigResponse(RadioResponseInfo responseInfo,
                                              ArrayList<GsmBroadcastSmsConfigInfo> configs) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setGsmBroadcastConfigResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setGsmBroadcastActivationResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param configs Vector of CDMA Broadcast SMS configs.
     */
    public void getCdmaBroadcastConfigResponse(RadioResponseInfo responseInfo,
                                               ArrayList<CdmaBroadcastSmsConfigInfo> configs) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaBroadcastConfigResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaBroadcastActivationResponse(RadioResponseInfo responseInfo) {
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
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param index record index where the cmda sms message is stored
     */
    public void writeSmsToRuimResponse(RadioResponseInfo responseInfo, int index) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void deleteSmsOnRuimResponse(RadioResponseInfo responseInfo) {
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
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void exitEmergencyCallbackModeResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param smsc Short Message Service Center address on the device
     */
    public void getSmscAddressResponse(RadioResponseInfo responseInfo, String smsc) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSmscAddressResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void reportSmsMemoryStatusResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void reportStkServiceIsRunningResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param source CDMA subscription source
     */
    public void getCdmaSubscriptionSourceResponse(RadioResponseInfo responseInfo, int source) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param response response string of the challenge/response algo for ISIM auth in base64 format
     */
    public void requestIsimAuthenticationResponse(RadioResponseInfo responseInfo, String response) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acknowledgeIncomingGsmSmsWithPduResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param iccIo IccIoResult as defined in types.hal corresponding to ICC IO response
     */
    public void sendEnvelopeWithStatusResponse(RadioResponseInfo responseInfo,
                                               android.hardware.radio.V1_0.IccIoResult iccIo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param rat Current voice RAT
     */
    public void getVoiceRadioTechnologyResponse(RadioResponseInfo responseInfo, int rat) {
    }

    public void getCellInfoListResponse(RadioResponseInfo responseInfo,
                                        ArrayList<android.hardware.radio.V1_0.CellInfo> cellInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cellInfo List of current cell information known to radio
     */
    public void getCellInfoListResponse_1_2(
            RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_2.CellInfo> cellInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCellInfoListRateResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setInitialAttachApnResponse(RadioResponseInfo responseInfo) {
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
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to sms sent as defined by SendSmsResult in types.hal
     */
    public void sendImsSmsResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param result IccIoResult as defined in types.hal
     */
    public void iccTransmitApduBasicChannelResponse(RadioResponseInfo responseInfo,
                                                    android.hardware.radio.V1_0.IccIoResult
                                                            result) {
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
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void iccCloseLogicalChannelResponse(RadioResponseInfo responseInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param result IccIoResult as defined in types.hal
     */
    public void iccTransmitApduLogicalChannelResponse(
            RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.IccIoResult result) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param result string containing the contents of the NV item
     */
    public void nvReadItemResponse(RadioResponseInfo responseInfo, String result) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void nvWriteItemResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void nvWriteCdmaPrlResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void nvResetConfigResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setUiccSubscriptionResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setDataAllowedResponse(RadioResponseInfo responseInfo) {
    }

    public void getHardwareConfigResponse(
            RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.HardwareConfig> config) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param result IccIoResult as defined in types.hal
     */
    public void requestIccSimAuthenticationResponse(RadioResponseInfo responseInfo,
                                                    android.hardware.radio.V1_0.IccIoResult
                                                            result) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setDataProfileResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void requestShutdownResponse(RadioResponseInfo responseInfo) {
    }

    public void getRadioCapabilityResponse(RadioResponseInfo responseInfo,
                                           android.hardware.radio.V1_0.RadioCapability rc) {
    }

    public void setRadioCapabilityResponse(RadioResponseInfo responseInfo,
                                           android.hardware.radio.V1_0.RadioCapability rc) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param statusInfo LceStatusInfo indicating LCE status
     */
    public void startLceServiceResponse(RadioResponseInfo responseInfo, LceStatusInfo statusInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param statusInfo LceStatusInfo indicating LCE status
     */
    public void stopLceServiceResponse(RadioResponseInfo responseInfo, LceStatusInfo statusInfo) {
    }

    public void pullLceDataResponse(RadioResponseInfo responseInfo, LceDataInfo lceInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param activityInfo modem activity information
     */
    public void getModemActivityInfoResponse(RadioResponseInfo responseInfo,
                                             ActivityStatsInfo activityInfo) {
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param numAllowed number of allowed carriers which have been set correctly.
     *        On success, it must match the length of list Carriers->allowedCarriers.
     *        if Length of allowed carriers list is 0, numAllowed = 0.
     */
    public void setAllowedCarriersResponse(RadioResponseInfo responseInfo, int numAllowed) {
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
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendDeviceStateResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCarrierInfoForImsiEncryptionResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setIndicationFilterResponse(RadioResponseInfo responseInfo) {
    }


    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSimCardPowerResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSignalStrengthReportingCriteriaResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setLinkCapacityReportingCriteriaResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSimCardPowerResponse_1_1(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param keepaliveStatus status of the keepalive with a handle for the session
     */
    public void startKeepaliveResponse(RadioResponseInfo responseInfo,
            KeepaliveStatus keepaliveStatus) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void stopKeepaliveResponse(RadioResponseInfo responseInfo) {
    }

    public void setTrmResponse(RadioResponseInfo responseInfo) {
    }

    // MTK-START: SIM
    /**
     * @param info Response info struct containing response type, serial no. and error.
     * @param response Response string of getATRResponse.
     */
    public void getATRResponse(RadioResponseInfo info, String response) {
    }

    /**
     * @param info Response info struct containing response type, serial no. and error.
     * @param response Response string of getIccidResponse.
     */
    public void getIccidResponse(RadioResponseInfo info, String response) {
    }

    /**
     * @param info Response info struct containing response type, serial no. and error.
     */
    public void setSimPowerResponse(RadioResponseInfo info) {
    }
    // MTK-END

    // MTK-START: NW
    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setNetworkSelectionModeManualWithActResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param networkInfos List of network operator information as OperatorInfoWithAct defined in
     *                     types.hal
     */
    public void getAvailableNetworksWithActResponse(RadioResponseInfo responseInfo,
                                            ArrayList<vendor.mediatek.hardware.
                                            radio.V3_0.OperatorInfoWithAct> networkInfos) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param customerSignalStrength list of signalstrength with wcdma ecio
     */
    public void getSignalStrengthWithWcdmaEcioResponse(RadioResponseInfo responseInfo,
            SignalStrengthWithWcdmaEcio signalStrength) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void cancelAvailableNetworksResponse(RadioResponseInfo responseInfo) {
    }

    /*
    * @param responseInfo Response info struct containing response type, serial no. and error
    */
    public void setModemPowerResponse(RadioResponseInfo responseInfo) {
    }

    // SMS-START
    public void getSmsParametersResponse(
            RadioResponseInfo responseInfo, SmsParams params) {
    }

    public void setSmsParametersResponse(
            RadioResponseInfo responseInfo) {
    }
    public void setEtwsResponse(
            RadioResponseInfo responseInfo) {
    }

    public void removeCbMsgResponse(
            RadioResponseInfo responseInfo) {
    }


    public void getSmsMemStatusResponse(
            RadioResponseInfo responseInfo, SmsMemStatus params) {
    }

    public void setGsmBroadcastLangsResponse(
            RadioResponseInfo responseInfo) {
    }

    public void getGsmBroadcastLangsResponse(
            RadioResponseInfo responseInfo, String langs) {
    }

    public void getGsmBroadcastActivationRsp(RadioResponseInfo responseInfo,
            int activation) {
    }

    public void setSmsFwkReadyRsp(RadioResponseInfo responseInfo) {
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
    }
    /// M: eMBMS end

    /// M: CC: call control part @{
    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void hangupAllResponse(RadioResponseInfo responseInfo)
            throws RemoteException {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCallIndicationResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void emergencyDialResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setEccServiceCategoryResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setEccListResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setVoicePreferStatusResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setEccNumResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void getEccNumResponse(RadioResponseInfo responseInfo) {
    }
    /// M: CC: @}

    /// M: CC: Vzw/CTVolte ECC
    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void currentStatusResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void eccPreferredRatResponse(RadioResponseInfo responseInfo) {
    }

    // APC-Start
    public void setApcModeResponse(
            RadioResponseInfo responseInfo) {
    }

    public void getApcInfoResponse(
            RadioResponseInfo responseInfo, ArrayList<Integer> cellInfo) {
    }
    // APC-End

    public void triggerModeSwitchByEccResponse(RadioResponseInfo responseInfo) {
    }

    @Override
    public void getSmsRuimMemoryStatusResponse(RadioResponseInfo responseInfo,
            SmsMemStatus memStatus) {
    }

    @Override
    public void setFdModeResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setResumeRegistrationResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void storeModemTypeResponse(RadioResponseInfo responseInfo) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void reloadModemTypeResponse(RadioResponseInfo responseInfo) {
    }

    public void handleStkCallSetupRequestFromSimWithResCodeResponse(
            RadioResponseInfo responseInfo) {
    }

    // PHB START, interface only currently.
    public void queryPhbStorageInfoResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> storageInfo) {
    }

    public void writePhbEntryResponse(RadioResponseInfo responseInfo) {
    }

    public void readPhbEntryResponse(RadioResponseInfo responseInfo,
            ArrayList<PhbEntryStructure> phbEntry) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param calls Current call list
     */
    public void getCurrentCallsResponse(RadioResponseInfo responseInfo,
                                        ArrayList<android.hardware.radio.V1_0.Call> calls) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param calls Current call list
     */
    public void getCurrentCallsResponse_1_2(RadioResponseInfo responseInfo,
                                        ArrayList<android.hardware.radio.V1_2.Call> calls) {
    }

    public void queryUPBCapabilityResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> upbCapability) {
    }

    public void editUPBEntryResponse(RadioResponseInfo responseInfo) {
    }

    public void deleteUPBEntryResponse(RadioResponseInfo responseInfo) {
    }

    public void readUPBGasListResponse(RadioResponseInfo responseInfo, ArrayList<String> gasList) {
    }

    public void readUPBGrpEntryResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> grpEntries) {
    }

    public void writeUPBGrpEntryResponse(RadioResponseInfo responseInfo) {
    }

    public void getPhoneBookStringsLengthResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> stringLength) {
    }

    public void getPhoneBookMemStorageResponse(RadioResponseInfo responseInfo,
            PhbMemStorageResponse phbMemStorage) {
    }

    public void setPhoneBookMemStorageResponse(RadioResponseInfo responseInfo) {
    }

    public void readPhoneBookEntryExtResponse(RadioResponseInfo responseInfo,
            ArrayList<PhbEntryExt> phbEntryExts) {
    }

    public void writePhoneBookEntryExtResponse(RadioResponseInfo responseInfo) {
    }

    public void queryUPBAvailableResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> upbAvailable) {
    }

    public void readUPBEmailEntryResponse(RadioResponseInfo responseInfo, String email) {
    }

    public void readUPBSneEntryResponse(RadioResponseInfo responseInfo, String sne) {
    }

    public void readUPBAnrEntryResponse(RadioResponseInfo responseInfo,
            ArrayList<PhbEntryStructure> anrs) {
    }

    public void readUPBAasListResponse(RadioResponseInfo responseInfo, ArrayList<String> aasList) {
    }

    public void setPhonebookReadyResponse(RadioResponseInfo responseInfo) {
    }
    // PHB END
    public void resetRadioResponse(RadioResponseInfo responseInfo) {
    }

    public void restartRILDResponse(RadioResponseInfo responseInfo) {
    }

    //Femtocell (CSG)-START
    public void getFemtocellListResponse(RadioResponseInfo responseInfo,
            ArrayList<String> femtoList) {
    }

    public void abortFemtocellListResponse(RadioResponseInfo responseInfo) {
    }

    public void selectFemtocellResponse(RadioResponseInfo responseInfo) {
    }

    public void queryFemtoCellSystemSelectionModeResponse(RadioResponseInfo responseInfo,
            int mode) {
    }

    public void setFemtoCellSystemSelectionModeResponse(RadioResponseInfo responseInfo) {
    }
    //Femtocell (CSG)-END

    // M: Data Framework - common part enhancement
    public void syncDataSettingsToMdResponse(RadioResponseInfo responseInfo) {}

    // M: Data Framework - Data Retry enhancement
    public void resetMdDataRetryCountResponse(RadioResponseInfo responseInfo) {}

    // M: Data Framework - CC 33
    public void setRemoveRestrictEutranModeResponse(RadioResponseInfo responseInfo) {}

    // M: [LTE][Low Power][UL traffic shaping] @{
    public void setLteAccessStratumReportResponse(RadioResponseInfo responseInfo) {}
    public void setLteUplinkDataTransferResponse(RadioResponseInfo responseInfo) {}
    // M: [LTE][Low Power][UL traffic shaping] @}
    // MTK-START: SIM ME LOCK
    public void queryNetworkLockResponse(RadioResponseInfo info, int catagory,
            int state, int retry_cnt, int autolock_cnt, int num_set, int total_set,
            int key_state) {
    }
    public void setNetworkLockResponse(RadioResponseInfo info) {
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param retriesRemaining Number of retries remaining.
     */
    public void supplyDepersonalizationResponse(RadioResponseInfo responseInfo,
            int retriesRemaining) {
    }

    public void supplyDeviceNetworkDepersonalizationResponse(RadioResponseInfo responseInfo,
            int remainingAttempts) {
    }
    // MTK-END

    public void setRxTestConfigResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> respAntConf) {
    }

    public void getRxTestResultResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> respAntInfo) {
    }

    public void getPOLCapabilityResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> polCapability) {
    }

    public void getCurrentPOLListResponse(RadioResponseInfo responseInfo,
            ArrayList<String> polList) {
    }

    public void setPOLEntryResponse(RadioResponseInfo responseInfo) {
    }

    /// M: [Network][C2K] Sprint roaming control @{
    public void setRoamingEnableResponse(RadioResponseInfo responseInfo) {
    }

    public void getRoamingEnableResponse(RadioResponseInfo responseInfo, ArrayList<Integer> data) {
    }
    /// @}

    public void setLteReleaseVersionResponse(RadioResponseInfo responseInfo) {
    }

    public void getLteReleaseVersionResponse(RadioResponseInfo responseInfo, int mode) {
    }

    // External SIM [Start]
    public void vsimNotificationResponse(RadioResponseInfo info, VsimEvent event) {
    }

    public void vsimOperationResponse(RadioResponseInfo info) {
    }
    // External SIM [End]

    public void setWifiEnabledResponse(RadioResponseInfo responseInfo) {
    }

    public void setWifiAssociatedResponse(RadioResponseInfo responseInfo) {
    }

    public void setWifiSignalLevelResponse(RadioResponseInfo responseInfo) {
    }

    public void setWifiIpAddressResponse(RadioResponseInfo responseInfo) {
    }

    public void setLocationInfoResponse(RadioResponseInfo responseInfo) {
    }

    public void setEmergencyAddressIdResponse(RadioResponseInfo responseInfo) {
    }

    public void setNattKeepAliveStatusResponse(RadioResponseInfo responseInfo) {
    }

    public void setWifiPingResultResponse(RadioResponseInfo responseInfo) {
    }

    public void setE911StateResponse(RadioResponseInfo responseInfo) {
    }

    public void setServiceStateToModemResponse(RadioResponseInfo responseInfo) {
    }

    public void sendRequestRawResponse(RadioResponseInfo responseInfo, ArrayList<Byte> data) {
    }

    public void sendRequestStringsResponse(RadioResponseInfo responseInfo, ArrayList<String> data) {
    }

    public void dataConnectionAttachResponse(RadioResponseInfo responseInfo) {
    }

    public void dataConnectionDetachResponse(RadioResponseInfo responseInfo) {
    }

    public void resetAllConnectionsResponse(RadioResponseInfo responseInfo) {
    }

    public void reportAirplaneModeResponse(RadioResponseInfo responseInfo) {
    }

    public void reportSimModeResponse(RadioResponseInfo responseInfo) {
    }

    public void setSilentRebootResponse(RadioResponseInfo responseInfo) {
    }

    public void setTxPowerStatusResponse(RadioResponseInfo responseInfo) {
    }

    public void setPropImsHandoverResponse(RadioResponseInfo responseInfo) {
    }

    public void setOperatorConfigurationResponse(RadioResponseInfo responseInfo) {
    }

    public void setSuppServPropertyResponse(RadioResponseInfo responseInfo) {
    }

    public void getSuppServPropertyResponse(RadioResponseInfo responseInfo, String value) {
    }
}
