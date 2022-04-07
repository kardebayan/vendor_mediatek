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

package com.mediatek.ims.ril;

import java.util.ArrayList;

import vendor.mediatek.hardware.radio.V3_0.IImsRadioResponse;
import vendor.mediatek.hardware.radio.V3_0.OperatorInfoWithAct;
import vendor.mediatek.hardware.radio.V3_0.PhbEntryExt;
import vendor.mediatek.hardware.radio.V3_0.PhbEntryStructure;
import vendor.mediatek.hardware.radio.V3_0.PhbMemStorageResponse;
import vendor.mediatek.hardware.radio.V3_0.SmsMemStatus;
import vendor.mediatek.hardware.radio.V3_0.SmsParams;
import vendor.mediatek.hardware.radio.V3_0.MtkSetupDataCallResult;
import vendor.mediatek.hardware.radio.V3_0.CallForwardInfoEx;

import android.hardware.radio.V1_1.KeepaliveStatus;

import android.hardware.radio.V1_0.ActivityStatsInfo;
import android.hardware.radio.V1_0.Call;
import android.hardware.radio.V1_0.CallForwardInfo;
import android.hardware.radio.V1_0.CardStatus;
import android.hardware.radio.V1_0.CarrierRestrictions;
import android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.CellInfo;
import android.hardware.radio.V1_0.DataRegStateResult;
import android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.HardwareConfig;
import android.hardware.radio.V1_0.IccIoResult;
import android.hardware.radio.V1_0.LastCallFailCauseInfo;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.LceStatusInfo;
import android.hardware.radio.V1_0.NeighboringCell;
import android.hardware.radio.V1_0.OperatorInfo;
import android.hardware.radio.V1_0.RadioCapability;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.SendSmsResult;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.SignalStrength;
import android.hardware.radio.V1_0.VoiceRegStateResult;
import android.hidl.base.V1_0.DebugInfo;

import android.os.IHwBinder;
import android.os.IHwBinder.DeathRecipient;
import android.os.RemoteException;

public class ImsRadioResponseBase extends IImsRadioResponse.Stub {

    @Override
    public void acceptCallResponse(RadioResponseInfo info) {
        riljLoge("No implementation in acceptCallResponse");
    }

    @Override
    public void acknowledgeIncomingGsmSmsWithPduResponse(RadioResponseInfo info) {
        riljLoge("No implementation in acknowledgeIncomingGsmSmsWithPduResponse");
    }

    @Override
    public void acknowledgeLastIncomingCdmaSmsResponse(RadioResponseInfo info) {
        riljLoge("No implementation in acknowledgeLastIncomingCdmaSmsResponse");
    }

    @Override
    public void acknowledgeLastIncomingGsmSmsResponse(RadioResponseInfo info) {
        riljLoge("No implementation in acknowledgeLastIncomingGsmSmsResponse");
    }

    @Override
    public void acknowledgeLastIncomingGsmSmsExResponse(RadioResponseInfo info) {
        riljLoge("No implementation in acknowledgeLastIncomingGsmSmsExResponse");
    }

    @Override
    public void acknowledgeLastIncomingCdmaSmsExResponse(RadioResponseInfo info) {
        riljLoge("No implementation in acknowledgeLastIncomingCdmaSmsExResponse");
    }

    @Override
    public void acknowledgeRequest(int info) {
        riljLoge("No implementation in acknowledgeRequest");
    }

    @Override
    public void cancelPendingUssdResponse(RadioResponseInfo info) {
        riljLoge("No implementation in cancelPendingUssdResponse");
    }

    @Override
    public void changeIccPin2ForAppResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in changeIccPin2ForAppResponse");
    }

    @Override
    public void changeIccPinForAppResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in changeIccPinForAppResponse");
    }

    @Override
    public void conferenceResponse(RadioResponseInfo info) {
        riljLoge("No implementation in conferenceResponse");
    }

    @Override
    public void deactivateDataCallResponse(RadioResponseInfo info) {
        riljLoge("No implementation in deactivateDataCallResponse");
    }

    @Override
    public void deleteSmsOnRuimResponse(RadioResponseInfo info) {
        riljLoge("No implementation in deleteSmsOnRuimResponse");
    }

    @Override
    public void deleteSmsOnSimResponse(RadioResponseInfo info) {
        riljLoge("No implementation in deleteSmsOnSimResponse");
    }

    @Override
    public void dialResponse(RadioResponseInfo info) {
        riljLoge("No implementation in dialResponse");
    }

    @Override
    public void exitEmergencyCallbackModeResponse(RadioResponseInfo info) {
        riljLoge("No implementation in exitEmergencyCallbackModeResponse");
    }

    @Override
    public void explicitCallTransferResponse(RadioResponseInfo info) {
        riljLoge("No implementation in explicitCallTransferResponse");
    }

    @Override
    public void getAllowedCarriersResponse(RadioResponseInfo info, boolean arg1,
                                           CarrierRestrictions arg2) {

        riljLoge("No implementation in getAllowedCarriersResponse");
    }

    @Override
    public void getAvailableBandModesResponse(RadioResponseInfo info,
                                              ArrayList<Integer> arg1) {

        riljLoge("No implementation in getAvailableBandModesResponse");
    }

    @Override
    public void getAvailableNetworksResponse(RadioResponseInfo info,
                                             ArrayList<OperatorInfo> arg1) {

        riljLoge("No implementation in getAvailableNetworksResponse");
    }

    @Override
    public void getBasebandVersionResponse(RadioResponseInfo info, String arg1) {
        riljLoge("No implementation in getBasebandVersionResponse");
    }

    @Override
    public void getCDMASubscriptionResponse(RadioResponseInfo info,
                                            String arg1, String arg2,
                                            String arg3, String arg4,
                                            String arg5) {

        riljLoge("No implementation in getCDMASubscriptionResponse");
    }

    @Override
    public void getCallForwardStatusResponse(RadioResponseInfo info,
                ArrayList<android.hardware.radio.V1_0.CallForwardInfo> list) {

        riljLoge("No implementation in getCallForwardStatusResponse");
    }

    @Override
    public void getCallWaitingResponse(RadioResponseInfo info,
                                       boolean arg1, int arg2) {

        riljLoge("No implementation in getCallWaitingResponse");
    }

    @Override
    public void getCdmaBroadcastConfigResponse(RadioResponseInfo info,
                ArrayList<CdmaBroadcastSmsConfigInfo> arg1) {

        riljLoge("No implementation in getCdmaBroadcastConfigResponse");
    }

    @Override
    public void getCdmaRoamingPreferenceResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in getCdmaRoamingPreferenceResponse");
    }

    @Override
    public void getCdmaSubscriptionSourceResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in getCdmaSubscriptionSourceResponse");
    }

    @Override
    public void getCellInfoListResponse(RadioResponseInfo info, ArrayList<CellInfo> arg1) {
        riljLoge("No implementation in getCellInfoListResponse");
    }

    @Override
    public void getClipResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in getClipResponse");
    }

    @Override
    public void getClirResponse(RadioResponseInfo info, int arg1, int arg2) {
        riljLoge("No implementation in getClirResponse");
    }

    @Override
    public void getCurrentCallsResponse(RadioResponseInfo info,
                                        ArrayList<Call> arg1) {

        riljLoge("No implementation in getCurrentCallsResponse");
    }

    @Override
    public void getDataCallListResponse(RadioResponseInfo info,
                                        ArrayList<SetupDataCallResult> arg1) {

        riljLoge("No implementation in getDataCallListResponse");
    }

    @Override
    public void getDataRegistrationStateResponse(RadioResponseInfo info,
                                                 DataRegStateResult arg1) {

        riljLoge("No implementation in getDataRegistrationStateResponse");
    }

    @Override
    public void getDeviceIdentityResponse(RadioResponseInfo info, String arg1,
                                          String arg2, String arg3, String arg4) {

        riljLoge("No implementation in getDeviceIdentityResponse");
    }

    @Override
    public void getFacilityLockForAppResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in getFacilityLockForAppResponse");
    }

    @Override
    public void getGsmBroadcastConfigResponse(RadioResponseInfo info,
                                              ArrayList<GsmBroadcastSmsConfigInfo> arg1) {

        riljLoge("No implementation in getGsmBroadcastConfigResponse");
    }

    @Override
    public void getHardwareConfigResponse(RadioResponseInfo info,
                                          ArrayList<HardwareConfig> arg1) {

        riljLoge("No implementation in getHardwareConfigResponse");
    }

    @Override
    public void getIMSIForAppResponse(RadioResponseInfo info, String arg1) {
        riljLoge("No implementation in getIMSIForAppResponse");
    }

    @Override
    public void getIccCardStatusResponse(RadioResponseInfo info, CardStatus arg1) {
        riljLoge("No implementation in getIccCardStatusResponse");
    }

    @Override
    public void getImsRegistrationStateResponse(RadioResponseInfo info,
                                                boolean arg1, int arg2) {

        riljLoge("No implementation in getImsRegistrationStateResponse");
    }

    @Override
    public void getLastCallFailCauseResponse(RadioResponseInfo info,
                                             LastCallFailCauseInfo arg1) {

        riljLoge("No implementation in getLastCallFailCauseResponse");
    }

    @Override
    public void getModemActivityInfoResponse(RadioResponseInfo info,
                                             ActivityStatsInfo arg1) {

        riljLoge("No implementation in getModemActivityInfoResponse");
    }

    @Override
    public void getMuteResponse(RadioResponseInfo info, boolean arg1) {

        riljLoge("No implementation in getMuteResponse");
    }

    @Override
    public void getNeighboringCidsResponse(RadioResponseInfo info,
                                           ArrayList<NeighboringCell> arg1) {

        riljLoge("No implementation in getNeighboringCidsResponse");
    }

    @Override
    public void getNetworkSelectionModeResponse(RadioResponseInfo info,
                                                boolean arg1) {

        riljLoge("No implementation in getNetworkSelectionModeResponse");
    }

    @Override
    public void getOperatorResponse(RadioResponseInfo info, String arg1,
                                    String arg2, String arg3) {

        riljLoge("No implementation in getOperatorResponse");
    }

    @Override
    public void getPreferredNetworkTypeResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in getPreferredNetworkTypeResponse");
    }

    @Override
    public void getPreferredVoicePrivacyResponse(RadioResponseInfo info,
                                                 boolean arg1) {

        riljLoge("No implementation in getPreferredVoicePrivacyResponse");
    }

    @Override
    public void getRadioCapabilityResponse(RadioResponseInfo info,
                                           RadioCapability arg1) {

        riljLoge("No implementation in getRadioCapabilityResponse");
    }

    @Override
    public void getSignalStrengthResponse(RadioResponseInfo info,
                                          SignalStrength arg1) {

        riljLoge("No implementation in getSignalStrengthResponse");
    }

    @Override
    public void getSmscAddressResponse(RadioResponseInfo info, String arg1) {
        riljLoge("No implementation in getSmscAddressResponse");
    }

    @Override
    public void getTTYModeResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in getTTYModeResponse");
    }

    @Override
    public void getVoiceRadioTechnologyResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in getVoiceRadioTechnologyResponse");
    }

    @Override
    public void getVoiceRegistrationStateResponse(RadioResponseInfo info,
                                                  VoiceRegStateResult arg1) {

        riljLoge("No implementation in getVoiceRegistrationStateResponse");
    }

    @Override
    public void handleStkCallSetupRequestFromSimResponse(RadioResponseInfo info) {
        riljLoge("No implementation in handleStkCallSetupRequestFromSimResponse");
    }

    @Override
    public void hangupConnectionResponse(RadioResponseInfo info) {
        riljLoge("No implementation in hangupConnectionResponse");
    }

    @Override
    public void hangupForegroundResumeBackgroundResponse(RadioResponseInfo info) {
        riljLoge("No implementation in hangupForegroundResumeBackgroundResponse");
    }

    @Override
    public void hangupWaitingOrBackgroundResponse(RadioResponseInfo info) {
        riljLoge("No implementation in hangupWaitingOrBackgroundResponse");
    }

    @Override
    public void iccCloseLogicalChannelResponse(RadioResponseInfo info) {
        riljLoge("No implementation in iccCloseLogicalChannelResponse");
    }

    @Override
    public void iccIOForAppResponse(RadioResponseInfo info, IccIoResult arg1) {
        riljLoge("No implementation in iccIOForAppResponse");
    }

    @Override
    public void iccOpenLogicalChannelResponse(RadioResponseInfo info, int arg1,
                                              ArrayList<Byte> arg2) {

        riljLoge("No implementation in iccOpenLogicalChannelResponse");
    }

    @Override
    public void iccTransmitApduBasicChannelResponse(RadioResponseInfo info,
                                                    IccIoResult arg1) {

        riljLoge("No implementation in iccTransmitApduBasicChannelResponse");
    }

    @Override
    public void iccTransmitApduLogicalChannelResponse(RadioResponseInfo info,
                                                      IccIoResult arg1) {

        riljLoge("No implementation in iccTransmitApduLogicalChannelResponse");
    }

    @Override
    public void nvReadItemResponse(RadioResponseInfo info, String arg1) {
        riljLoge("No implementation in nvReadItemResponse");
    }

    @Override
    public void nvResetConfigResponse(RadioResponseInfo info) {
        riljLoge("No implementation in nvResetConfigResponse");
    }

    @Override
    public void nvWriteCdmaPrlResponse(RadioResponseInfo info) {
        riljLoge("No implementation in nvWriteCdmaPrlResponse");
    }

    @Override
    public void nvWriteItemResponse(RadioResponseInfo info) {
        riljLoge("No implementation in nvWriteItemResponse");
    }

    @Override
    public void pullLceDataResponse(RadioResponseInfo info, LceDataInfo arg1) {
        riljLoge("No implementation in pullLceDataResponse");
    }

    @Override
    public void rejectCallResponse(RadioResponseInfo info) {
        riljLoge("No implementation in rejectCallResponse");
    }

    @Override
    public void reportSmsMemoryStatusResponse(RadioResponseInfo info) {
        riljLoge("No implementation in reportSmsMemoryStatusResponse");
    }

    @Override
    public void reportStkServiceIsRunningResponse(RadioResponseInfo info) {
        riljLoge("No implementation in reportStkServiceIsRunningResponse");
    }

    @Override
    public void requestIccSimAuthenticationResponse(RadioResponseInfo info,
                                                    IccIoResult arg1) {

        riljLoge("No implementation in requestIccSimAuthenticationResponse");
    }

    @Override
    public void requestIsimAuthenticationResponse(RadioResponseInfo info,
                                                  String arg1) {

        riljLoge("No implementation in requestIsimAuthenticationResponse");
    }

    @Override
    public void requestShutdownResponse(RadioResponseInfo info) {
        riljLoge("No implementation in requestShutdownResponse");
    }

    @Override
    public void sendBurstDtmfResponse(RadioResponseInfo info) {
        riljLoge("No implementation in sendBurstDtmfResponse");
    }

    @Override
    public void sendCDMAFeatureCodeResponse(RadioResponseInfo info) {
        riljLoge("No implementation in sendCDMAFeatureCodeResponse");
    }

    @Override
    public void sendCdmaSmsResponse(RadioResponseInfo info, SendSmsResult arg1) {
        riljLoge("No implementation in sendCdmaSmsResponse");
    }

    @Override
    public void sendDeviceStateResponse(RadioResponseInfo info) {
        riljLoge("No implementation in sendDeviceStateResponse");
    }

    @Override
    public void sendDtmfResponse(RadioResponseInfo info) {
        riljLoge("No implementation in sendDtmfResponse");
    }

    @Override
    public void sendEnvelopeResponse(RadioResponseInfo info, String arg1) {
        riljLoge("No implementation in sendEnvelopeResponse");
    }

    @Override
    public void sendEnvelopeWithStatusResponse(RadioResponseInfo info,
                                               IccIoResult arg1) {

        riljLoge("No implementation in sendEnvelopeWithStatusResponse");
    }

    @Override
    public void sendImsSmsResponse(RadioResponseInfo info, SendSmsResult arg1) {
        riljLoge("No implementation in sendImsSmsResponse");
    }

    @Override
    public void sendImsSmsExResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        riljLoge("No implementation in sendImsSmsExResponse");
    }

    @Override
    public void sendSMSExpectMoreResponse(RadioResponseInfo info,
                                          SendSmsResult arg1) {

        riljLoge("No implementation in sendSMSExpectMoreResponse");
    }

    @Override
    public void sendSmsResponse(RadioResponseInfo info, SendSmsResult arg1) {
        riljLoge("No implementation in sendSmsResponse");
    }

    @Override
    public void sendTerminalResponseToSimResponse(RadioResponseInfo info) {
        riljLoge("No implementation in sendTerminalResponseToSimResponse");
    }

    @Override
    public void sendUssdResponse(RadioResponseInfo info) {
        riljLoge("No implementation in sendUssdResponse");
    }

    @Override
    public void separateConnectionResponse(RadioResponseInfo info) {
        riljLoge("No implementation in separateConnectionResponse");
    }

    @Override
    public void setAllowedCarriersResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in setAllowedCarriersResponse");
    }

    @Override
    public void setBandModeResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setBandModeResponse");
    }

    @Override
    public void setBarringPasswordResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setBarringPasswordResponse");
    }

    @Override
    public void setCallForwardResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setCallForwardResponse");
    }

    @Override
    public void setCallWaitingResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setCallWaitingResponse");
    }

    @Override
    public void setCdmaBroadcastActivationResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setCdmaBroadcastActivationResponse");
    }

    @Override
    public void setCdmaBroadcastConfigResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setCdmaBroadcastConfigResponse");
    }

    @Override
    public void setCdmaRoamingPreferenceResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setCdmaRoamingPreferenceResponse");
    }

    @Override
    public void setCdmaSubscriptionSourceResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setCdmaSubscriptionSourceResponse");
    }

    @Override
    public void setCellInfoListRateResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setCellInfoListRateResponse");
    }

    @Override
    public void setClirResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setClirResponse");
    }

    @Override
    public void setDataAllowedResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setDataAllowedResponse");
    }

    @Override
    public void setDataProfileResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setDataProfileResponse");
    }

    @Override
    public void setFacilityLockForAppResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in setFacilityLockForAppResponse");
    }

    @Override
    public void setGsmBroadcastActivationResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setGsmBroadcastActivationResponse");
    }

    @Override
    public void setGsmBroadcastConfigResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setGsmBroadcastConfigResponse");
    }

    @Override
    public void setIndicationFilterResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setIndicationFilterResponse");
    }

    @Override
    public void setInitialAttachApnResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setInitialAttachApnResponse");
    }

    @Override
    public void setLocationUpdatesResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setLocationUpdatesResponse");
    }

    @Override
    public void setMuteResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setMuteResponse");
    }

    @Override
    public void setNetworkSelectionModeAutomaticResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setNetworkSelectionModeAutomaticResponse");
    }

    @Override
    public void setNetworkSelectionModeManualResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setNetworkSelectionModeManualResponse");
    }

    @Override
    public void setPreferredNetworkTypeResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setPreferredNetworkTypeResponse");
    }

    @Override
    public void setPreferredVoicePrivacyResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setPreferredVoicePrivacyResponse");
    }

    @Override
    public void setRadioCapabilityResponse(RadioResponseInfo info,
                                           RadioCapability arg1) {

        riljLoge("No implementation in setRadioCapabilityResponse");
    }

    @Override
    public void setRadioPowerResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setRadioPowerResponse");
    }

    @Override
    public void setSimCardPowerResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setSimCardPowerResponse");
    }

    @Override
    public void setSmscAddressResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setSmscAddressResponse");
    }

    @Override
    public void setSuppServiceNotificationsResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setSuppServiceNotificationsResponse");
    }

    @Override
    public void setTTYModeResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setTTYModeResponse");
    }

    @Override
    public void setUiccSubscriptionResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setUiccSubscriptionResponse");
    }

    @Override
    public void setupDataCallResponse(RadioResponseInfo info,
                                      SetupDataCallResult arg1) {

        riljLoge("No implementation in setupDataCallResponse");
    }

    @Override
    public void startDtmfResponse(RadioResponseInfo info) {
        riljLoge("No implementation in startDtmfResponse");
    }

    @Override
    public void startLceServiceResponse(RadioResponseInfo info,
                                        LceStatusInfo arg1) {

        riljLoge("No implementation in startLceServiceResponse");
    }

    @Override
    public void stopDtmfResponse(RadioResponseInfo info) {
        riljLoge("No implementation in stopDtmfResponse");
    }

    @Override
    public void stopLceServiceResponse(RadioResponseInfo info,
                                       LceStatusInfo arg1) {

        riljLoge("No implementation in stopLceServiceResponse");
    }

    @Override
    public void supplyIccPin2ForAppResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in supplyIccPin2ForAppResponse");
    }

    @Override
    public void supplyIccPinForAppResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in supplyIccPinForAppResponse");
    }

    @Override
    public void supplyIccPuk2ForAppResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in supplyIccPuk2ForAppResponse");
    }

    @Override
    public void supplyIccPukForAppResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in supplyIccPukForAppResponse");
    }

    @Override
    public void supplyNetworkDepersonalizationResponse(RadioResponseInfo info,
                                                       int arg1) {

        riljLoge("No implementation in supplyNetworkDepersonalizationResponse");
    }

    @Override
    public void switchWaitingOrHoldingAndActiveResponse(RadioResponseInfo info) {
        riljLoge("No implementation in switchWaitingOrHoldingAndActiveResponse");
    }

    @Override
    public void writeSmsToRuimResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in writeSmsToRuimResponse");
    }

    @Override
    public void writeSmsToSimResponse(RadioResponseInfo info, int arg1) {
        riljLoge("No implementation in writeSmsToSimResponse");
    }

    @Override
    public void addImsConferenceCallMemberResponse(RadioResponseInfo info) {

        riljLoge("No implementation in addImsConferenceCallMemberResponse");
    }

    @Override
    public void cancelUssiResponse(RadioResponseInfo info) {
        riljLoge("No implementation in cancelUssiResponse");
    }

    @Override
    public void getXcapStatusResponse(RadioResponseInfo info) {
        riljLoge("No implementation in getXcapStatusResponse");
    }

    @Override
    public void resetSuppServResponse(RadioResponseInfo info) {
        riljLoge("No implementation in resetSuppServResponse");
    }

    @Override
    public void setupXcapUserAgentStringResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setupXcapUserAgentStringResponse");
    }

    @Override
    public void conferenceDialResponse(RadioResponseInfo info) {
        riljLoge("No implementation in conferenceDialResponse");
    }

    @Override
    public void imsDeregNotificationResponse(RadioResponseInfo info) {
        riljLoge("No implementation in deregisterImsResponse");
    }

    @Override
    public void dialWithSipUriResponse(RadioResponseInfo info) {
        riljLoge("No implementation in dialWithSipUriResponse");
    }

    @Override
    public void emergencyDialResponse(RadioResponseInfo info) {
        riljLoge("No implementation in emergencyDialResponse");
    }

    @Override
    public void forceReleaseCallResponse(RadioResponseInfo info) {
        riljLoge("No implementation in forceReleaseCallResponse");
    }

    @Override
    public void getProvisionValueResponse(RadioResponseInfo info) {
        riljLoge("No implementation in getProvisionValueResponse");
    }

    @Override
    public void hangupAllResponse(RadioResponseInfo info) {
        riljLoge("No implementation in hangupAllResponse");
    }

    @Override
    public void holdCallResponse(RadioResponseInfo info) {
        riljLoge("No implementation in holdCallResponse");
    }

    @Override
    public void imsBearerActivationDoneResponse(RadioResponseInfo info) {
        riljLoge("No implementation in imsBearerActivationDoneResponse");
    }

    @Override
    public void imsBearerDeactivationDoneResponse(RadioResponseInfo info) {
        riljLoge("No implementation in imsBearerDeactivationDoneResponse");
    }

    @Override
    public void setImsBearerNotificationResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setImsBearerNotificationResponse");
    }

    @Override
    public void imsEctCommandResponse(RadioResponseInfo info) {
        riljLoge("No implementation in imsEctCommandResponse");
    }

    @Override
    public void removeImsConferenceCallMemberResponse(RadioResponseInfo info) {

        riljLoge("No implementation in removeImsConferenceCallMemberResponse");
    }

    @Override
    public void resumeCallResponse(RadioResponseInfo info) {
        riljLoge("No implementation in resumeCallResponse");
    }

    @Override
    public void sendUssiResponse(RadioResponseInfo info) {
        riljLoge("No implementation in sendUssiResponse");
    }

    @Override
    public void setCallIndicationResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setCallIndicationResponse");
    }

    @Override
    public void setEccServiceCategoryResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setEccServiceCategoryResponse");
    }

    @Override
    public void setImsCallStatusResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setImsCallStatusResponse");
    }

    @Override
    public void setImsEnableResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setImsEnableResponse");
    }

    @Override
    public void setImsRtpReportResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setImsRtpReportResponse");
    }

    @Override
    public void setImsVideoEnableResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setImsVideoEnableResponse");
    }

    @Override
    public void setImsVoiceEnableResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setImsVoiceEnableResponse");
    }

    @Override
    public void setImscfgResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setImscfgResponse");
    }

    @Override
    public void setModemImsCfgResponse(RadioResponseInfo info, String results) {
        riljLoge("No implementation in setModemImsCfgResponse");
    }

    @Override
    public void pullCallResponse(RadioResponseInfo info) {
        riljLoge("No implementation in pullCallResponse");
    }

    @Override
    public void setProvisionValueResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setProvisionValueResponse");
    }

    @Override
    public void setImsCfgFeatureValueResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setImsCfgFeatureValueResponse");
    }

    @Override
    public void getImsCfgFeatureValueResponse(RadioResponseInfo info, int value) {
        riljLoge("No implementation in getImsCfgFeatureValueResponse");
    }

    @Override
    public void setImsCfgProvisionValueResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setImsCfgProvisionValueResponse");
    }

    @Override
    public void getImsCfgProvisionValueResponse(RadioResponseInfo info, String value) {
        riljLoge("No implementation in getImsCfgProvisionValueResponse");
    }

    @Override
    public void setImsCfgResourceCapValueResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setImsCfgResourceCapValueResponse");
    }

    @Override
    public void getImsCfgResourceCapValueResponse(RadioResponseInfo info, int value) {
        riljLoge("No implementation in getImsCfgResourceCapValueResponse");
    }

    @Override
    public void setViWifiEnableResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setViWifiEnableResponse");
    }

    @Override
    public void setRcsUaEnableResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setRcsUaEnableResponse");
    }

    @Override
    public void setVilteEnableResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setVilteEnableResponse");
    }

    @Override
    public void setVolteEnableResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setVolteEnableResponse");
    }

    @Override
    public void setWfcEnableResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setWfcEnableResponse");
    }

    @Override
    public void setWfcProfileResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setWfcProfileResponse");
    }

    @Override
    public void updateImsRegistrationStatusResponse(RadioResponseInfo info) {
        riljLoge("No implementation in updateImsRegistrationStatusResponse");
    }

    @Override
    public void videoCallAcceptResponse(RadioResponseInfo info) {
        riljLoge("No implementation in videoCallAcceptResponse");
    }

    @Override
    public void vtDialResponse(RadioResponseInfo info) {
        riljLoge("No implementation in vtDialResponse");
    }

    @Override
    public void vtDialWithSipUriResponse(RadioResponseInfo info) {
        riljLoge("No implementation in vtDialWithSipUriResponse");
    }

    @Override
    public void setImsRegistrationReportResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setImsRegistrationReportResponse");
    }

    @Override
    public void setVoiceDomainPreferenceResponse(RadioResponseInfo info) {
       riljLoge("No implementation in setImsVoiceDomainPreferenceResponse");
    }

    /* MTK SS Feature : Start */
    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    @Override
    public void setClipResponse(RadioResponseInfo responseInfo) {
        riljLoge("No implementation in setClipResponse");
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param status indicates COLR status. "0" means not provisioned, "1" means provisioned,
     *        "2" means unknown
     */
    @Override
    public void getColrResponse(RadioResponseInfo responseInfo, int status) {
        riljLoge("No implementation in getColrResponse");
    }

    @Override
    public void setColrResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setColrResponse");
    }

    @Override
    public void getColpResponse(RadioResponseInfo responseInfo, int n, int m) {
        riljLoge("No implementation in getColpResponse");
    }

    @Override
    public void setColpResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setColpResponse");
    }

    @Override
    public void queryCallForwardInTimeSlotStatusResponse(RadioResponseInfo info,
            ArrayList<CallForwardInfoEx> callForwardInfoExs) {

        riljLoge("No implementation in queryCallForwardInTimeSlotStatusResponse");
    }

    @Override
    public void setCallForwardInTimeSlotResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setCallForwardInTimeSlotResponse");
    }

    @Override
    public void runGbaAuthenticationResponse(RadioResponseInfo info, ArrayList<String> resList) {
        riljLoge("No implementation in runGbaAuthenticationResponse");
    }
    /* MTK SS Feature : End */

    @Override
    public void startNetworkScanResponse(RadioResponseInfo info) {
        riljLoge("No implementation in startNetworkScanResponse");
    }

    @Override
    public void stopKeepaliveResponse(RadioResponseInfo info) {
        riljLoge("No implementation in stopKeepaliveResponse");
    }

    @Override
    public void setCarrierInfoForImsiEncryptionResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setCarrierInfoForImsiEncryptionResponse");
    }

    @Override
    public void stopNetworkScanResponse(RadioResponseInfo info) {
        riljLoge("No implementation in stopNetworkScanResponse");
    }

    @Override
    public void setSimCardPowerResponse_1_1(RadioResponseInfo info) {
        riljLoge("No implementation in setSimCardPowerResponse_1_1");
    }

    @Override
    public void startKeepaliveResponse(RadioResponseInfo info, KeepaliveStatus status) {
        riljLoge("No implementation in startKeepaliveResponse");
    }

    /**
     * AOSP Radio 1.2 defined API
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param SignalStrength SignalStrength
     */
    @Override
    public void getSignalStrengthResponse_1_2(RadioResponseInfo info,
                android.hardware.radio.V1_2.SignalStrength signalStrength) {
        riljLoge("No implementation in getSignalStrengthResponse_1_2");
    }

    /**
     * AOSP Radio 1.2 defined API
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param calls Calls
     */
    @Override
    public void getCurrentCallsResponse_1_2(RadioResponseInfo info,
                                            ArrayList<android.hardware.radio.V1_2.Call> calls) {
        riljLoge("No implementation in getCurrentCallsResponse_1_2");
    }

    /**
     * AOSP Radio 1.2 defined API
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    @Override
    public void setLinkCapacityReportingCriteriaResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setLinkCapacityReportingCriteriaResponse");
    }

    /**
     * AOSP Radio 1.2 defined API
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    @Override
    public void setSignalStrengthReportingCriteriaResponse(RadioResponseInfo info) {
        riljLoge("No implementation in setSignalStrengthReportingCriteriaResponse");
    }

    /**
     * AOSP Radio 1.2 defined API
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cardStatus CardStatus
     */
    @Override
    public void getIccCardStatusResponse_1_2(RadioResponseInfo info,
                android.hardware.radio.V1_2.CardStatus cardStatus) {
        riljLoge("No implementation in getIccCardStatusResponse_1_2");
    }

    /**
     * AOSP Radio 1.2 defined API
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cellInfo Cell information
     */
    @Override
    public void getCellInfoListResponse_1_2(RadioResponseInfo info,
                ArrayList<android.hardware.radio.V1_2.CellInfo> cellInfo) {
        riljLoge("No implementation in getCellInfoListResponse_1_2");
    }

    /**
     * AOSP Radio 1.2 defined API
     * @param info Response info struct containing response type, serial no. and error
     * @param dataRegResponse Current Data registration response as defined by DataRegStateResult in
     *        types.hal
     */
    @Override
    public void getDataRegistrationStateResponse_1_2(RadioResponseInfo info,
            android.hardware.radio.V1_2.DataRegStateResult dataRegResponse) {
        riljLoge("No implementation in getDataRegistrationStateResponse_1_2");
    }

    @Override
    public void getVoiceRegistrationStateResponse_1_2(RadioResponseInfo info,
            android.hardware.radio.V1_2.VoiceRegStateResult voiceRegStateResult) {
        riljLoge("No implementation in getVoiceRegistrationStateResponse_1_2");
    }

    /**
     * Log for error
     * @param msg
     */
    protected void riljLoge(String msg) {}

}
