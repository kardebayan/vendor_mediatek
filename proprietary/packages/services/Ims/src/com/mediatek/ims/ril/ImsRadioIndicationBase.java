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

import vendor.mediatek.hardware.radio.V3_0.EtwsNotification;
import vendor.mediatek.hardware.radio.V3_13.IImsRadioIndication;
import vendor.mediatek.hardware.radio.V3_0.IncomingCallNotification;
import vendor.mediatek.hardware.radio.V3_0.ImsConfParticipant;

import android.hardware.radio.V1_1.NetworkScanResult;
import android.hardware.radio.V1_1.KeepaliveStatus;

import android.hardware.radio.V1_0.CdmaCallWaiting;
import android.hardware.radio.V1_0.CdmaInformationRecords;
import android.hardware.radio.V1_0.CdmaSignalInfoRecord;
import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.hardware.radio.V1_0.CellInfo;
import android.hardware.radio.V1_0.HardwareConfig;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.PcoDataInfo;
import android.hardware.radio.V1_0.RadioCapability;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.SignalStrength;
import android.hardware.radio.V1_0.SimRefreshResult;
import android.hardware.radio.V1_0.StkCcUnsolSsResult;
import android.hardware.radio.V1_0.SuppSvcNotification;
import android.hidl.base.V1_0.DebugInfo;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwBinder.DeathRecipient;
import android.os.RemoteException;

import vendor.mediatek.hardware.radio.V3_0.Dialog;

public class ImsRadioIndicationBase extends IImsRadioIndication.Stub {


    @Override
    public void ectIndication(int arg0, int arg1, int arg2, int arg3)
            {

        riljLoge("No implementation in ectIndication");
    }

    @Override
    public void volteSetting(int arg0, boolean arg1) {
        riljLoge("No implementation in volteSetting");
    }

    @Override
    public void callInfoIndication(int type, ArrayList<String> data) {
        riljLoge("No implementation in callInfoIndication");
    }

    @Override
    public void callmodChangeIndicator(int type, String callId, String callMode,
                                       String videoState, String audioDirection,
                                       String pau) {

        riljLoge("No implementation in callmodChangeIndicator");
    }

    @Override
    public void econfResultIndication(int type, String confCallId,
                                      String op, String num, String result,
                                      String cause, String joinedCallId) {

        riljLoge("No implementation in econfResultIndication");
    }

    @Override
    public void getProvisionDone(int type, String data, String arg2) {
        riljLoge("No implementation in getProvisionDone");
    }

    @Override
    public void imsBearerActivation(int type, int data, String arg2) {
        riljLoge("No implementation in imsBearerActivation");
    }

    @Override
    public void imsBearerDeactivation(int type, int data, String arg2) {
        riljLoge("No implementation in imsBearerDeactivation");
    }

    @Override
    public void imsBearerInit(int type) {
        riljLoge("No implementation in imsBearerInit");
    }

    @Override
    public void imsDataInfoNotify(int type, String arg1, String arg2, String arg3) {
        riljLoge("No implementation in imsDataInfoNotify");
    }

    @Override
    public void imsDisableDone(int type) {
        riljLoge("No implementation in imsDisableDone");
    }

    @Override
    public void imsDisableStart(int type) {
        riljLoge("No implementation in imsDisableStart");
    }

    @Override
    public void imsEnableDone(int type) {
        riljLoge("No implementation in imsEnableDone");
    }

    @Override
    public void imsEnableStart(int type) {
        riljLoge("No implementation in imsEnableStart");
    }

    @Override
    public void imsRegistrationInfo(int type, int data, int arg2) {

        riljLoge("No implementation in imsRegistrationInfo");
    }

    @Override
    public void imsRtpInfo(int type, String pdnId, String networkId, String timer,
                           String sendPktLost, String recvPktLost) {

        riljLoge("No implementation in imsRtpInfo");
    }

    @Override
    public void imsRtpInfoReport(int type, String pdnId, String networkId, String timer,
                           String sendPktLost, String recvPktLost, String jitter, String delay) {

        riljLoge("No implementation in imsRtpInfoReport");
    }

    @Override
    public void incomingCallIndication(int type, IncomingCallNotification data) {
        riljLoge("No implementation in incomingCallIndication");
    }

    @Override
    public void onUssi(int type, String clazz, String status, String str,
                       String lang, String errorcode, String alertingPattern,
                       String sipCause) {

        riljLoge("No implementation in onUssi");
    }

    @Override
    public void onXui(int type, String accountId, String broadcastFlag,
                      String xuiInfo) {

        riljLoge("No implementation in onXui");
    }

    @Override
    public void onVolteSubscription(int type, int status) {

        riljLoge("No implementation in onVolteSubscription");
    }

    @Override
    public void sipCallProgressIndicator(int type, String callId, String dir,
                                         String sipMsgType, String method,
                                         String responseCode, String reasonText) {

        riljLoge("No implementation in sipCallProgressIndicator");
    }

    @Override
    public void videoCapabilityIndicator(int type, String callId,
                                         String localVideoCap, String remoteVideoCap) {

        riljLoge("No implementation in videoCapabilityIndicator");
    }

    @Override
    public void imsConferenceInfoIndication(int type,
            ArrayList<ImsConfParticipant> participants) {
        riljLoge("No implementation in imsConferenceInfoIndication");
    }

    @Override
    public void lteMessageWaitingIndication(int type,
                                          String callId, String pType, String urcIdx,
                                          String totalUrcCount, String rawData) {
        riljLoge("No implementation in lteMessageWaitingIndication");
    }

    /// ==== AOSP APIs below ============================================================

    @Override
    public void callRing(int type, boolean data, CdmaSignalInfoRecord arg2) {
        riljLoge("No implementation in callRing");
    }

    @Override
    public void callStateChanged(int type) {
        riljLoge("No implementation in callStateChanged");
    }

    @Override
    public void cdmaCallWaiting(int type, CdmaCallWaiting data) {
        riljLoge("No implementation in cdmaCallWaiting");
    }

    @Override
    public void cdmaInfoRec(int type, CdmaInformationRecords data) {
        riljLoge("No implementation in cdmaInfoRec");
    }

    @Override
    public void cdmaNewSms(int type, CdmaSmsMessage data) {
        riljLoge("No implementation in cdmaNewSms");
    }

    @Override
    public void cdmaOtaProvisionStatus(int type, int data) {
        riljLoge("No implementation in cdmaOtaProvisionStatus");
    }

    @Override
    public void cdmaPrlChanged(int type, int data) {
        riljLoge("No implementation in cdmaPrlChanged");
    }

    @Override
    public void cdmaRuimSmsStorageFull(int type) {
        riljLoge("No implementation in cdmaRuimSmsStorageFull");
    }

    @Override
    public void cdmaSubscriptionSourceChanged(int type, int data) {
        riljLoge("No implementation in cdmaSubscriptionSourceChanged");
    }

    @Override
    public void cellInfoList(int type, ArrayList<CellInfo> data) {
        riljLoge("No implementation in cellInfoList");
    }

    @Override
    public void currentSignalStrength(int type, SignalStrength data) {
        riljLoge("No implementation in currentSignalStrength");
    }

    @Override
    public void dataCallListChanged(int type,
            ArrayList<SetupDataCallResult> data) {

        riljLoge("No implementation in dataCallListChanged");
    }

    @Override
    public void enterEmergencyCallbackMode(int type) {
        riljLoge("No implementation in enterEmergencyCallbackMode");
    }

    @Override
    public void exitEmergencyCallbackMode(int type) {
        riljLoge("No implementation in exitEmergencyCallbackMode");
    }

    @Override
    public void hardwareConfigChanged(int type, ArrayList<HardwareConfig> data) {
        riljLoge("No implementation in hardwareConfigChanged");
    }

    @Override
    public void imsNetworkStateChanged(int type) {
        riljLoge("No implementation in imsNetworkStateChanged");
    }

    @Override
    public void indicateRingbackTone(int type, boolean data) {
        riljLoge("No implementation in indicateRingbackTone");
    }

    @Override
    public void lceData(int type, LceDataInfo data) {
        riljLoge("No implementation in lceData");
    }

    @Override
    public void modemReset(int type, String data) {
        riljLoge("No implementation in modemReset");
    }

    @Override
    public void networkStateChanged(int type) {
        riljLoge("No implementation in networkStateChanged");
    }

    @Override
    public void newBroadcastSms(int type, ArrayList<Byte> data) {
        riljLoge("No implementation in newBroadcastSms");
    }

    @Override
    public void newSms(int type, ArrayList<Byte> data) {
        riljLoge("No implementation in newSms");
    }

    @Override
    public void newSmsOnSim(int type, int data) {
        riljLoge("No implementation in newSmsOnSim");
    }

    @Override
    public void newSmsStatusReport(int type, ArrayList<Byte> data) {
        riljLoge("No implementation in newSmsStatusReport");
    }

    @Override
    public void nitzTimeReceived(int type, String data, long arg2) {
        riljLoge("No implementation in nitzTimeReceived");
    }

    @Override
    public void onSupplementaryServiceIndication(int type,
            StkCcUnsolSsResult data) {

        riljLoge("No implementation in onSupplementaryServiceIndication");
    }

    @Override
    public void onUssd(int type, int data, String arg2) {
        riljLoge("No implementation in onUssd");
    }

    @Override
    public void pcoData(int type, PcoDataInfo data) {
        riljLoge("No implementation in pcoData");
    }

    @Override
    public void radioCapabilityIndication(int type, RadioCapability data) {
        riljLoge("No implementation in radioCapabilityIndication");
    }

    @Override
    public void radioStateChanged(int type, int data) {
        riljLoge("No implementation in radioStateChanged");
    }

    @Override
    public void resendIncallMute(int type) {
        riljLoge("No implementation in resendIncallMute");
    }

    @Override
    public void restrictedStateChanged(int type, int data) {
        riljLoge("No implementation in restrictedStateChanged");
    }

    @Override
    public void rilConnected(int type) {
        riljLoge("No implementation in rilConnected");
    }

    @Override
    public void simRefresh(int type, SimRefreshResult data) {
        riljLoge("No implementation in simRefresh");
    }

    @Override
    public void simSmsStorageFull(int type) {
        riljLoge("No implementation in simSmsStorageFull");
    }

    @Override
    public void simStatusChanged(int type) {
        riljLoge("No implementation in simStatusChanged");
    }

    @Override
    public void srvccStateNotify(int type, int data) {
        riljLoge("No implementation in srvccStateNotify");
    }

    @Override
    public void stkCallControlAlphaNotify(int type, String data) {
        riljLoge("No implementation in stkCallControlAlphaNotify");
    }

    @Override
    public void stkCallSetup(int type, long data) {
        riljLoge("No implementation in stkCallSetup");
    }

    @Override
    public void stkEventNotify(int type, String data) {
        riljLoge("No implementation in stkEventNotify");
    }

    @Override
    public void stkProactiveCommand(int type, String data) {
        riljLoge("No implementation in stkProactiveCommand");
    }

    @Override
    public void stkSessionEnd(int type) {
        riljLoge("No implementation in stkSessionEnd");
    }

    @Override
    public void subscriptionStatusChanged(int type, boolean data) {
        riljLoge("No implementation in subscriptionStatusChanged");
    }

    @Override
    public void suppSvcNotify(int type, SuppSvcNotification data) {
        riljLoge("No implementation in suppSvcNotify");
    }

    @Override
    public void voiceRadioTechChanged(int type, int data) {
        riljLoge("No implementation in voiceRadioTechChanged");
    }

    @Override
    public void imsEventPackageIndication(int type,
                                          String callId, String ptype, String urcIdx,
                                          String totalUrcCount, String rawData) {
        riljLoge("No implementation in imsEventPackageIndication");
    }

    @Override
    public void imsDeregDone(int type) {
        riljLoge("No implementation in imsDeregDone");
    }

    @Override
    public void multiImsCount(int type, int count) {
        riljLoge("No implementation in multiImsCount");
    }

    @Override
    public void imsSupportEcc(int type, int supportLteEcc) {
        riljLoge("No implementation in isSupportLteEcc");
    }

    @Override
    public void imsRedialEmergencyIndication(int type, String callId) {
        riljLoge("No implementation in imsRedialEmergencyIndication");
    }

    @Override
    public void keepaliveStatus(int type, KeepaliveStatus status) {
        riljLoge("No implementation in keepaliveStatus");
    }

    @Override
    public void carrierInfoForImsiEncryption(int type) {
        riljLoge("No implementation in carrierInfoForImsiEncryption");
    }

    @Override
    public void networkScanResult(int type, NetworkScanResult result) {
        riljLoge("No implementation in networkScanResult");
    }

    @Override
    public void imsRadioInfoChange(int type, String iid, String info) {
        riljLoge("No implementation in imsRadioInfoChange");
    }

    /**
    * Reports speech codec information
    *
    * @param type Type of radio indication
    * @param info integer type speech codec info
    */
    @Override
    public void speechCodecInfoIndication(int type, int info) {
        riljLoge("No implementation in speechCodecInfoIndication");
    }

    /**
     * Indicates current signal strength of the radio.
     * AOSP Radio 1.2 Interfaces
     *
     * @param type Type of radio indication
     * @param signalStrength SignalStrength information
     */
    @Override
    public void currentSignalStrength_1_2(int type,
                android.hardware.radio.V1_2.SignalStrength signalStrength) {
        riljLoge("No implementation in currentSignalStrength_1_2");
    }

    /**
     * AOSP Radio 1.2 Interfaces
     *
     * @param type Type of radio indication
     * @param signalStrength SignalStrength information
     */
    @Override
    public void currentPhysicalChannelConfigs(int type,
                ArrayList<android.hardware.radio.V1_2.PhysicalChannelConfig> configs) {
        riljLoge("No implementation in currentPhysicalChannelConfigs");
    }

    /**
     * AOSP Radio 1.2 Interfaces
     *
     * @param type Type of radio indication
     * @param lce LinkCapacityEstimate information
     */
    @Override
    public void currentLinkCapacityEstimate(int type,
                android.hardware.radio.V1_2.LinkCapacityEstimate lce) {
        riljLoge("No implementation in currentLinkCapacityEstimate");
    }

    /**
     * AOSP Radio 1.2 Interfaces
     *
     * @param type Type of radio indication
     * @param lce LinkCapacityEstimate information
     */
    @Override
    public void cellInfoList_1_2(int type,
                                 ArrayList<android.hardware.radio.V1_2.CellInfo> records) {
        riljLoge("No implementation in cellInfoList_1_2");
    }

    /**
     * AOSP Radio 1.2 Interfaces
     *
     * @param type Type of radio indication
     * @param result NetworkScanResult information
     */
    @Override
    public void networkScanResult_1_2(int type,
                                      android.hardware.radio.V1_2.NetworkScanResult result) {
        riljLoge("No implementation in networkScanResult_1_2");
    }

    /**
     * [IMS] IMS Dialog Event Package Indiciation
     * @param type Type of radio indication
     * @param dialogList the dialog info list
     */
    @Override
    public void imsDialogIndication(int type, ArrayList<Dialog> dialogList) {
        riljLoge("No implementation in imsDialogIndication");
    }

    @Override
    public void imsCfgDynamicImsSwitchComplete(int type) {
        riljLoge("No implementation in imsCfgDynamicImsSwitchComplete");
    }

    @Override
    public void imsCfgFeatureChanged(int type, int phoneId, int featureId, int value) {
        riljLoge("No implementation in imsCfgFeatureChanged");
    }

    @Override
    public void imsCfgConfigChanged(int type, int phoneId, String configId, String value) {
        riljLoge("No implementation in imsCfgConfigChanged");
    }

    @Override
    public void imsCfgConfigLoaded(int type) {
        riljLoge("No implementation in imsCfgConfigLoaded");
    }

    @Override
    public void newSmsStatusReportEx(int indicationType, ArrayList<Byte> pdu) {
        riljLoge("No implementation in newSmsStatusReportEx");
    }

    @Override
    public void newSmsEx(int indicationType, ArrayList<Byte> pdu) {
        riljLoge("No implementation in newSmsEx");
    }

    @Override
    public void cdmaNewSmsEx(int indicationType, CdmaSmsMessage msg) {
        riljLoge("No implementation in cdmaNewSmsEx");
    }

    @Override
    public void noEmergencyCallbackMode(int indicationType) {
        riljLoge("No implementation in noEmergencyCallbackMode");
    }

    @Override
    public void incomingCallAdditionalInfoInd(int indicationType, ArrayList<String> info) {
        riljLoge("No implementation in incomingCallAdditionalInfoInd");
    }
    /**
     * Log for error
     * @param msg
     */
    protected void riljLoge(String msg) {}
}
