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
import android.content.Intent;
import com.android.internal.telephony.RadioIndication;
import com.android.internal.telephony.RIL;

import android.os.RemoteException;

import android.hardware.radio.V1_0.CdmaCallWaiting;
import android.hardware.radio.V1_0.CdmaInformationRecord;
import android.hardware.radio.V1_0.CdmaLineControlInfoRecord;
import android.hardware.radio.V1_0.CdmaNumberInfoRecord;
import android.hardware.radio.V1_0.CdmaRedirectingNumberInfoRecord;
import android.hardware.radio.V1_0.CdmaSignalInfoRecord;
import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.hardware.radio.V1_0.CdmaT53AudioControlInfoRecord;
import android.hardware.radio.V1_0.CellInfoType;
import android.hardware.radio.V1_0.CfData;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.PcoDataInfo;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.SimRefreshResult;
import android.hardware.radio.V1_0.SsInfoData;
import android.hardware.radio.V1_0.StkCcUnsolSsResult;
import android.hardware.radio.V1_0.SuppSvcNotification;

import android.os.AsyncResult;
import android.os.Build;
import android.os.SystemProperties;
import android.os.UserHandle;

import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;

import com.android.internal.telephony.cdma.CdmaInformationRecords;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.internal.telephony.gsm.MtkSuppCrssNotification;
import com.mediatek.internal.telephony.worldphone.WorldMode;
import static com.android.internal.telephony.RILConstants.*;
import static com.mediatek.internal.telephony.MtkRILConstants.*;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.android.internal.telephony.CommandsInterface;

import android.os.Build;

// MTK-START, SMS part
import vendor.mediatek.hardware.radio.V3_0.EtwsNotification;
import com.mediatek.internal.telephony.MtkEtwsNotification;
// MTK-END, SMS part

/// CC: M: call control part @{
import vendor.mediatek.hardware.radio.V3_0.IncomingCallNotification;
import vendor.mediatek.hardware.radio.V3_0.CipherNotification;
import vendor.mediatek.hardware.radio.V3_0.CrssNotification;
/// @}

// SS
import vendor.mediatek.hardware.radio.V3_0.CfuStatusNotification;

// M: [VzW] Data Framework @{
import vendor.mediatek.hardware.radio.V3_0.PcoDataAttachedInfo;
import com.mediatek.internal.telephony.dataconnection.PcoDataAfterAttached;
// M: [VzW] Data Framework @}

// External SIM [Start]
import com.android.internal.telephony.uicc.IccUtils;
import vendor.mediatek.hardware.radio.V3_0.VsimOperationEvent;
import com.mediatek.telephony.internal.telephony.vsim.ExternalSimManager.VsimEvent;
// External SIM [End]

/// Ims Data Framework @{
import com.mediatek.internal.telephony.ims.MtkDedicateDataCallResponse;
import vendor.mediatek.hardware.radio.V3_0.DedicateDataCall;
/// @}

/// [NW] @{
import android.telephony.SignalStrength;
import vendor.mediatek.hardware.radio.V3_0.SignalStrengthWithWcdmaEcio;
import android.telephony.SubscriptionManager;
/// @}

// DSBP enhancement
import vendor.mediatek.hardware.radio.V3_0.DsbpState;

public class MtkRadioIndication extends MtkRadioIndicationBase {
    private static final boolean ENG = "eng".equals(Build.TYPE);

    // TAG
    private static final String TAG = "MtkRadioInd";

    RadioIndication mRadioIndication;
    MtkRadioIndication(RIL ril) {
        super(ril);
        mRadioIndication = new RadioIndication(ril);
        mMtkRil = (MtkRIL) ril;
    }

    private MtkRIL mMtkRil;

    /**
     * Indicates when radio state changes.
     * @param indicationType RadioIndicationType
     * @param radioState android.hardware.radio.V1_0.RadioState
     */
    public void radioStateChanged(int indicationType, int radioState) {
        CommandsInterface.RadioState oldState = mMtkRil.getRadioState();
        mRadioIndication.radioStateChanged(indicationType, radioState);
        CommandsInterface.RadioState newState = mMtkRil.getRadioState();
        if (newState != oldState) {
            Intent intent = new Intent(TelephonyIntents.ACTION_RADIO_STATE_CHANGED);
            intent.putExtra("radioState", radioState);
            intent.putExtra("subId", MtkSubscriptionManager
                        .getSubIdUsingPhoneId(mMtkRil.mInstanceId));
            mMtkRil.mMtkContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            if (MtkRIL.RILJ_LOGD) {
                mMtkRil.riljLog("Broadcast for RadioStateChanged: state=" + radioState);
            }
        }
    }

    public void callStateChanged(int indicationType) {
        mRadioIndication.callStateChanged(indicationType);
    }

    /**
     * Indicates when either voice or data network state changed
     * @param indicationType RadioIndicationType
     */
    public void networkStateChanged(int indicationType) {
        mRadioIndication.networkStateChanged(indicationType);
    }

    public void newSms(int indicationType, ArrayList<Byte> pdu) {
        mRadioIndication.newSms(indicationType, pdu);
    }

    public void newSmsStatusReport(int indicationType, ArrayList<Byte> pdu) {
        mRadioIndication.newSmsStatusReport(indicationType, pdu);
    }

    public void newSmsOnSim(int indicationType, int recordNumber) {
        mRadioIndication.newSmsOnSim(indicationType, recordNumber);
    }

    public void onUssd(int indicationType, int ussdModeType, String msg) {
        mRadioIndication.onUssd(indicationType, ussdModeType, msg);
    }

    public void nitzTimeReceived(int indicationType, String nitzTime, long receivedTime) {
        mRadioIndication.nitzTimeReceived(indicationType, nitzTime, receivedTime);
    }

    public void currentSignalStrength(int indicationType,
                                      android.hardware.radio.V1_0.SignalStrength signalStrength) {
        mRadioIndication.currentSignalStrength(indicationType, signalStrength);
    }

    public void currentLinkCapacityEstimate(int indicationType,
                                            android.hardware.radio.V1_2.LinkCapacityEstimate lce) {

        mRadioIndication.currentLinkCapacityEstimate(indicationType, lce);
    }

    public void currentSignalStrength_1_2(int indicationType,
                                      android.hardware.radio.V1_2.SignalStrength signalStrength) {
        mRadioIndication.currentSignalStrength_1_2(indicationType, signalStrength);
    }

    public void currentPhysicalChannelConfigs(int indicationType,
            ArrayList<android.hardware.radio.V1_2.PhysicalChannelConfig> configs) {
        mRadioIndication.currentPhysicalChannelConfigs(indicationType, configs);
    }

    public void dataCallListChanged(int indicationType, ArrayList<SetupDataCallResult> dcList) {
        mRadioIndication.dataCallListChanged(indicationType, dcList);
    }

    public void suppSvcNotify(int indicationType, SuppSvcNotification suppSvcNotification) {
        mRadioIndication.suppSvcNotify(indicationType, suppSvcNotification);
    }

    /* MTK SS */
    public void cfuStatusNotify(int indicationType, CfuStatusNotification cfuStatus) {
        mMtkRil.processIndication(indicationType);

        int[] notification = new int[2];
        notification[0] = cfuStatus.status;
        notification[1] = cfuStatus.lineId;

        if (RIL.RILJ_LOGD) mMtkRil.unsljLogRet(RIL_UNSOL_CALL_FORWARDING, notification);

        if (notification[1] == 1) {
            mMtkRil.mCfuReturnValue = notification;
        }

        /* ONLY notify for Line 1 */
        if (mMtkRil.mCallForwardingInfoRegistrants.size() != 0 && notification[1] == 1) {
            /* Update mCfuReturnValue first */
            mMtkRil.mCallForwardingInfoRegistrants
                    .notifyRegistrants(new AsyncResult(null, notification, null));
        }
    }

    /// M: CC: call control related @{
    /// M: CC: incoming call notification handling
    public void incomingCallIndication(int indicationType, IncomingCallNotification inCallNotify) {
        mMtkRil.processIndication(indicationType);

        String[] notification = new String[7];
        notification[0] = inCallNotify.callId;
        notification[1] = inCallNotify.number;
        notification[2] = inCallNotify.type;
        notification[3] = inCallNotify.callMode;
        notification[4] = inCallNotify.seqNo;
        notification[5] = inCallNotify.redirectNumber;

        if (RIL.RILJ_LOGD) mMtkRil.unsljLogRet(RIL_UNSOL_INCOMING_CALL_INDICATION, notification);

        if (mMtkRil.mIncomingCallIndicationRegistrant != null) {
            mMtkRil.mIncomingCallIndicationRegistrant
                    .notifyRegistrant(new AsyncResult(null, notification, null));
        }

    }

    /// M: CC: ciphering support notification
    public void cipherIndication(int indicationType, CipherNotification cipherNotify) {
        mMtkRil.processIndication(indicationType);

        String[] notification = new String[4];
        notification[0] = cipherNotify.simCipherStatus;
        notification[1] = cipherNotify.sessionStatus;
        notification[2] = cipherNotify.csStatus;
        notification[3] = cipherNotify.psStatus;

        if (RIL.RILJ_LOGD) mMtkRil.unsljLogRet(RIL_UNSOL_CIPHER_INDICATION, notification);

        if (mMtkRil.mCipherIndicationRegistrant != null) {
            mMtkRil.mCipherIndicationRegistrant
                    .notifyRegistrants(new AsyncResult(null, notification, null));
        }

    }

    /// M: CC: CRSS notification handling
    public void crssIndication(int indicationType, CrssNotification crssNotification) {
        mMtkRil.processIndication(indicationType);

        MtkSuppCrssNotification notification = new MtkSuppCrssNotification();
        notification.code = crssNotification.code;
        notification.type = crssNotification.type;
        notification.alphaid = crssNotification.alphaid;
        notification.number = crssNotification.number;
        notification.cli_validity = crssNotification.cli_validity;

        if (RIL.RILJ_LOGD) mMtkRil.unsljLogRet(RIL_UNSOL_CRSS_NOTIFICATION, notification);

        if (mMtkRil.mCallRelatedSuppSvcRegistrant != null) {
            mMtkRil.mCallRelatedSuppSvcRegistrant
                    .notifyRegistrant(new AsyncResult(null, notification, null));
        }
    }

    /// M: CC: GSA HD Voice for 2/3G network support
    public void speechCodecInfoIndication(int indicationType, int info) {
        mMtkRil.processIndication(indicationType);

        if (RIL.RILJ_LOGD) mMtkRil.unsljLogvRet(RIL_UNSOL_SPEECH_CODEC_INFO, info);

        if (mMtkRil.mSpeechCodecInfoRegistrant != null) {
            // TODO: check client type (int or int*)
            mMtkRil.mSpeechCodecInfoRegistrant.notifyRegistrant(new AsyncResult(null, info, null));
        }

    }

    /// M: CC: @}

    /// M: CC: CDMA call accepted notification handling @{
    public void cdmaCallAccepted(int indicationType) {
        mMtkRil.processIndication(indicationType);

        if (RIL.RILJ_LOGD) mMtkRil.unsljLogRet(RIL_UNSOL_CDMA_CALL_ACCEPTED, indicationType);

        if (mMtkRil.mCdmaCallAcceptedRegistrant != null) {
            mMtkRil.mCdmaCallAcceptedRegistrant.notifyRegistrants(
                new AsyncResult (null, null, null));
        }
    }
    /// @}

    public void eccNumIndication(int indicationType, String eccListWithCard, String eccListNoCard) {
    }

    public void stkSessionEnd(int indicationType) {
        mRadioIndication.stkSessionEnd(indicationType);
    }

    public void stkProactiveCommand(int indicationType, String cmd) {
        mRadioIndication.stkProactiveCommand(indicationType, cmd);
    }

    public void stkEventNotify(int indicationType, String cmd) {
        mRadioIndication.stkEventNotify(indicationType, cmd);
    }

    public void stkCallSetup(int indicationType, long timeout) {
        mRadioIndication.stkCallSetup(indicationType, timeout);
    }

    public void simSmsStorageFull(int indicationType) {
        mRadioIndication.simSmsStorageFull(indicationType);
    }

    public void simRefresh(int indicationType, SimRefreshResult refreshResult) {
        mRadioIndication.simRefresh(indicationType, refreshResult);
    }

    public void callRing(int indicationType, boolean isGsm, CdmaSignalInfoRecord record) {
        mRadioIndication.callRing(indicationType, isGsm, record);
    }

    public void simStatusChanged(int indicationType) {
        mRadioIndication.simStatusChanged(indicationType);
    }

    public void cdmaNewSms(int indicationType, CdmaSmsMessage msg) {
        mRadioIndication.cdmaNewSms(indicationType, msg);
    }

    public void newBroadcastSms(int indicationType, ArrayList<Byte> data) {
        mRadioIndication.newBroadcastSms(indicationType, data);
    }

    public void cdmaRuimSmsStorageFull(int indicationType) {
        mRadioIndication.cdmaRuimSmsStorageFull(indicationType);
    }

    public void restrictedStateChanged(int indicationType, int state) {
        mRadioIndication.restrictedStateChanged(indicationType, state);
    }

    public void enterEmergencyCallbackMode(int indicationType) {
        mRadioIndication.enterEmergencyCallbackMode(indicationType);
    }

    public void cdmaCallWaiting(int indicationType, CdmaCallWaiting callWaitingRecord) {
        mRadioIndication.cdmaCallWaiting(indicationType, callWaitingRecord);
    }

    public void cdmaOtaProvisionStatus(int indicationType, int status) {
        mRadioIndication.cdmaOtaProvisionStatus(indicationType, status);
    }

    public void cdmaInfoRec(int indicationType,
                            android.hardware.radio.V1_0.CdmaInformationRecords records) {
        mRadioIndication.cdmaInfoRec(indicationType, records);
    }

    public void indicateRingbackTone(int indicationType, boolean start) {
        mRadioIndication.indicateRingbackTone(indicationType, start);
    }

    public void resendIncallMute(int indicationType) {
        mRadioIndication.resendIncallMute(indicationType);
    }

    public void cdmaSubscriptionSourceChanged(int indicationType, int cdmaSource) {
        mRadioIndication.cdmaSubscriptionSourceChanged(indicationType, cdmaSource);
    }

    public void cdmaPrlChanged(int indicationType, int version) {
        mRadioIndication.cdmaPrlChanged(indicationType, version);
    }

    public void exitEmergencyCallbackMode(int indicationType) {
        mRadioIndication.exitEmergencyCallbackMode(indicationType);
    }

    public void rilConnected(int indicationType) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (MtkRIL.RILJ_LOGD) mMtkRil.unsljLog(RIL_UNSOL_RIL_CONNECTED);

        // Initial conditions
        //mRadioIndication.mRil.setRadioPower(false, null);
        mRadioIndication.mRil.setCdmaSubscriptionSource(
                mRadioIndication.mRil.mCdmaSubscription, null);
        mRadioIndication.mRil.setCellInfoListRate();
        // todo: this should not require a version number now. Setting it to latest RIL version for
        // now.
        mRadioIndication.mRil.notifyRegistrantsRilConnectionChanged(15);
    }

    public void voiceRadioTechChanged(int indicationType, int rat) {
        mRadioIndication.voiceRadioTechChanged(indicationType, rat);
    }

    public void cellInfoList(int indicationType,
                             ArrayList<android.hardware.radio.V1_0.CellInfo> records) {
        mRadioIndication.cellInfoList(indicationType, records);
    }

    public void cellInfoList_1_2(int indicationType,
                             ArrayList<android.hardware.radio.V1_2.CellInfo> records) {
        mRadioIndication.cellInfoList_1_2(indicationType, records);
    }

    private int getSubId(int phoneId) {
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        int[] subIds = SubscriptionManager.getSubId(phoneId);
        if (subIds != null && subIds.length > 0) {
            subId = subIds[0];
        }
        return subId;
    }

    public void networkScanResult(int indicationType,
                                  android.hardware.radio.V1_1.NetworkScanResult result) {
        mRadioIndication.networkScanResult(indicationType, result);
    }

    public void networkScanResult_1_2(int indicationType,
                                      android.hardware.radio.V1_2.NetworkScanResult result) {
        final boolean showRat = true;
        String mccmnc;
        Iterator<android.hardware.radio.V1_2.CellInfo> it = result.networkInfos.iterator();
        // Process the operator name because RILD only consider TS.25 and NITZ
        // for (int i = 0; i < result.networkInfos.size(); i++) {
        while (it.hasNext()) {
            mccmnc = null;
            android.hardware.radio.V1_2.CellInfo record = it.next();
            switch (record.cellInfoType) {
                case CellInfoType.GSM: {
                    android.hardware.radio.V1_2.CellInfoGsm cellInfoGsm = record.gsm.get(0);
                    mccmnc = cellInfoGsm.cellIdentityGsm.base.mcc +
                            cellInfoGsm.cellIdentityGsm.base.mnc;
                    int nLac = cellInfoGsm.cellIdentityGsm.base.lac;
                    cellInfoGsm.cellIdentityGsm.operatorNames.alphaLong =
                            mMtkRil.lookupOperatorName(
                                    getSubId(mMtkRil.mInstanceId), mccmnc, true, nLac);
                    cellInfoGsm.cellIdentityGsm.operatorNames.alphaShort =
                            mMtkRil.lookupOperatorName(
                                    getSubId(mMtkRil.mInstanceId), mccmnc, false, nLac);
                    if (showRat) {
                        cellInfoGsm.cellIdentityGsm.operatorNames.alphaLong =
                                cellInfoGsm.cellIdentityGsm.operatorNames.alphaLong.concat(" 2G");
                        cellInfoGsm.cellIdentityGsm.operatorNames.alphaShort =
                                cellInfoGsm.cellIdentityGsm.operatorNames.alphaShort.concat(" 2G");
                    }
                    mMtkRil.riljLog("mccmnc=" + mccmnc + ", lac=" + nLac + ", longName=" +
                            cellInfoGsm.cellIdentityGsm.operatorNames.alphaLong + " shortName=" +
                            cellInfoGsm.cellIdentityGsm.operatorNames.alphaShort);
                    break;
                }

                case CellInfoType.CDMA: {
                    // android.hardware.radio.V1_2.CellInfoCdma cellInfoCdma = record.cdma.get(0);
                    break;
                }

                case CellInfoType.LTE: {
                    android.hardware.radio.V1_2.CellInfoLte cellInfoLte = record.lte.get(0);
                    mccmnc = cellInfoLte.cellIdentityLte.base.mcc +
                            cellInfoLte.cellIdentityLte.base.mnc;
                    int nLac = cellInfoLte.cellIdentityLte.base.tac;
                    cellInfoLte.cellIdentityLte.operatorNames.alphaLong =
                            mMtkRil.lookupOperatorName(
                                    getSubId(mMtkRil.mInstanceId), mccmnc, true, nLac);
                    cellInfoLte.cellIdentityLte.operatorNames.alphaShort =
                            mMtkRil.lookupOperatorName(
                                    getSubId(mMtkRil.mInstanceId), mccmnc, false, nLac);
                    if (showRat) {
                        cellInfoLte.cellIdentityLte.operatorNames.alphaLong =
                                cellInfoLte.cellIdentityLte.operatorNames.alphaLong.concat(" 4G");
                        cellInfoLte.cellIdentityLte.operatorNames.alphaShort =
                                cellInfoLte.cellIdentityLte.operatorNames.alphaShort.concat(" 4G");
                    }
                    mMtkRil.riljLog("mccmnc=" + mccmnc + ", lac=" + nLac + ", longName=" +
                            cellInfoLte.cellIdentityLte.operatorNames.alphaLong + " shortName=" +
                            cellInfoLte.cellIdentityLte.operatorNames.alphaShort);
                    break;
                }

                case CellInfoType.WCDMA: {
                    android.hardware.radio.V1_2.CellInfoWcdma cellInfoWcdma = record.wcdma.get(0);
                    mccmnc = cellInfoWcdma.cellIdentityWcdma.base.mcc +
                            cellInfoWcdma.cellIdentityWcdma.base.mnc;
                    int nLac = cellInfoWcdma.cellIdentityWcdma.base.lac;
                    cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaLong =
                            mMtkRil.lookupOperatorName(
                                    getSubId(mMtkRil.mInstanceId), mccmnc, true, nLac);
                    cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaShort =
                            mMtkRil.lookupOperatorName(
                                    getSubId(mMtkRil.mInstanceId), mccmnc, false, nLac);
                    if (showRat) {
                        cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaLong =
                            cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaLong.concat(" 3G");
                        cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaShort =
                            cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaShort.concat(" 3G");
                    }
                    mMtkRil.riljLog("mccmnc=" + mccmnc + ", lac=" + nLac + ", longName=" +
                            cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaLong +
                            " shortName=" +
                            cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaShort);
                    break;
                }

                default:
                    throw new RuntimeException("unexpected cellinfotype: " + record.cellInfoType);
            }
            if (mMtkRil.hidePLMN(mccmnc)) {
                it.remove();
                mMtkRil.riljLog("remove this one " + mccmnc);
            }
        }
        mRadioIndication.networkScanResult_1_2(indicationType, result);
    }

    public void imsNetworkStateChanged(int indicationType) {
        mRadioIndication.imsNetworkStateChanged(indicationType);
    }

    public void subscriptionStatusChanged(int indicationType, boolean activate) {
        mRadioIndication.subscriptionStatusChanged(indicationType, activate);
    }

    public void srvccStateNotify(int indicationType, int state) {
        mRadioIndication.srvccStateNotify(indicationType, state);
    }

    public void hardwareConfigChanged(
            int indicationType,
            ArrayList<android.hardware.radio.V1_0.HardwareConfig> configs) {
        mRadioIndication.hardwareConfigChanged(indicationType, configs);
    }

    public void radioCapabilityIndication(int indicationType,
                                          android.hardware.radio.V1_0.RadioCapability rc) {
        mRadioIndication.radioCapabilityIndication(indicationType, rc);
    }

    public void onSupplementaryServiceIndication(int indicationType, StkCcUnsolSsResult ss) {
        mRadioIndication.onSupplementaryServiceIndication(indicationType, ss);
    }

    public void stkCallControlAlphaNotify(int indicationType, String alpha) {
        mRadioIndication.stkCallControlAlphaNotify(indicationType, alpha);
    }

    public void lceData(int indicationType, LceDataInfo lce) {
        mRadioIndication.lceData(indicationType, lce);
    }

    public void pcoData(int indicationType, PcoDataInfo pco) {
        mRadioIndication.pcoData(indicationType, pco);
    }

    public void modemReset(int indicationType, String reason) {
        mRadioIndication.modemReset(indicationType, reason);
    }

   // NW-START
    public void responseCsNetworkStateChangeInd(int indicationType,
            ArrayList<String> state) {
        mMtkRil.processIndication(indicationType);

        mMtkRil.riljLog("[UNSL]< " + "UNSOL_RESPONSE_CS_NETWORK_STATE_CHANGED");

        if (mMtkRil.mCsNetworkStateRegistrants.size() != 0) {
            mMtkRil.mCsNetworkStateRegistrants.notifyRegistrants(
                    new AsyncResult(null, state.toArray(new String[state.size()]), null));
        }
    }

    public void responsePsNetworkStateChangeInd(int indicationType,
            ArrayList<Integer> state) {
        mMtkRil.processIndication(indicationType);

        mMtkRil.riljLog("[UNSL]< " + "UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED");

        Object ret = null;
        int[] response = new int[state.size()];
        for (int i = 0; i < state.size(); i++) {
            response[i] = state.get(i);
        }
        ret = response;
        if (mMtkRil.mPsNetworkStateRegistrants.size() != 0) {
            mMtkRil.mPsNetworkStateRegistrants.notifyRegistrants(
                    new AsyncResult(null, ret, null));
        }
    }

    public void responseNetworkEventInd(int indicationType,
            ArrayList<Integer> event) {
        mRadioIndication.mRil.processIndication(indicationType);

        Object ret = null;
        int[] response= new int[event.size()];

        for (int i = 0; i < event.size(); i++) {
             response[i] = event.get(i);
        }
        ret = response;

        mMtkRil.unsljLogRet(RIL_UNSOL_NETWORK_EVENT, ret);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);

        if (ril.mNetworkEventRegistrants.size()  !=  0) {
            ril.mNetworkEventRegistrants.notifyRegistrants(new AsyncResult(null, ret, null));
        }

    }

    public void networkRejectCauseInd(int indicationType, ArrayList<Integer> event) {
        mRadioIndication.mRil.processIndication(indicationType);

        Object ret = null;
        int[] response= new int[event.size()];

        for (int i = 0; i < event.size(); i++) {
             response[i] = event.get(i);
        }
        ret = response;

        mMtkRil.unsljLogRet(RIL_UNSOL_NETWORK_REJECT_CAUSE, ret);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);

        if (ril.mNetworkRejectRegistrants.size() != 0) {
            ril.mNetworkRejectRegistrants.notifyRegistrants(new AsyncResult(null, ret, null));
        }

    }

    public void responseModulationInfoInd(int indicationType,
            ArrayList<Integer> data) {
        mRadioIndication.mRil.processIndication(indicationType);

        Object ret = null;
        int[] response= new int[data.size()];

        for (int i = 0; i < data.size(); i++) {
             response[i] = data.get(i);
        }
        ret = response;

        mMtkRil.unsljLogRet(RIL_UNSOL_MODULATION_INFO, ret);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);

        if (ril.mModulationRegistrants.size()  !=  0) {
            ril.mModulationRegistrants.notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    public void responseInvalidSimInd(int indicationType,
            ArrayList<String> state) {
        mRadioIndication.mRil.processIndication(indicationType);
        String [] ret = state.toArray(new String[state.size()]);
        mMtkRil.unsljLogRet(RIL_UNSOL_INVALID_SIM, ret);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);

        if (ril.mInvalidSimInfoRegistrant.size()  !=  0) {
            ril.mInvalidSimInfoRegistrant.notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * Indicates when radio state changes.
     * @param indicationType RadioIndicationType
     * @param info femtocell information
     */
    public void responseFemtocellInfo(int indicationType, ArrayList<String> info) {
        mMtkRil.processIndication(indicationType);

        Object ret = null;
        String[] response = info.toArray(new String[info.size()]);
        ret = response;

        if (MtkRIL.RILJ_LOGD) {
            mMtkRil.unsljLogRet(RIL_UNSOL_FEMTOCELL_INFO, ret);
        }

        if (mMtkRil.mFemtoCellInfoRegistrants.size() !=  0) {
            mMtkRil.mFemtoCellInfoRegistrants.notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * Indicates the current signal strength of the camped or primary serving cell.
     */
    public void currentSignalStrengthWithWcdmaEcioInd(int indicationType,
            SignalStrengthWithWcdmaEcio signalStrength) {
        mMtkRil.processIndication(indicationType);
        // Todo: here should change to the signalStrength with Wcdma Ecio
        SignalStrength ss = new SignalStrength (
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
        if (MtkRIL.RILJ_LOGD) {
            mMtkRil.unsljLogRet(RIL_UNSOL_SIGNAL_STRENGTH_WITH_WCDMA_ECIO, ss);
        }
        if (MtkRIL.RILJ_LOGD) {
            mMtkRil.riljLog("currentSignalStrengthWithWcdmaEcioInd SignalStrength=" + ss);
            if (mMtkRil.mSignalStrengthWithWcdmaEcioRegistrants.size() !=  0) {
                mMtkRil.mSignalStrengthWithWcdmaEcioRegistrants.notifyRegistrants(
                            new AsyncResult(null, ss, null));
            }
        }
    }

    public void responseLteNetworkInfo(int indicationType, int info) {
        mMtkRil.riljLog("[UNSL]< " + "RIL_UNSOL_LTE_NETWORK_INFO " + info);
    }

    /**
     * Indicates when current resident network mccmnc changes.
     * @param indicationType RadioIndicationType
     * @param mccmnc current resident network mcc & mnc.
     */
    public void onMccMncChanged(int indicationType, String mccmnc) {
        mMtkRil.processIndication(indicationType);

        if (MtkRIL.RILJ_LOGD) {
            mMtkRil.unsljLogRet(RIL_UNSOL_MCCMNC_CHANGED, mccmnc);
        }

        if (mMtkRil.mMccMncRegistrants.size() != 0) {
            mMtkRil.mMccMncRegistrants.notifyRegistrants(new AsyncResult(null, mccmnc, null));
        }
    }
    // NW-END

    // MTK-START: SIM
    public void onVirtualSimOn(int indicationType, int simInserted) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_VIRTUAL_SIM_ON);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mVirtualSimOn != null) {
            ril.mVirtualSimOn.notifyRegistrants(
                                new AsyncResult(null, simInserted, null));
        }
    }

    public void onVirtualSimOff(int indicationType, int simInserted) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_VIRTUAL_SIM_OFF);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mVirtualSimOff != null) {
            ril.mVirtualSimOn.notifyRegistrants(
                                new AsyncResult(null, simInserted, null));
        }
    }

    public void onImeiLock(int indicationType) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_IMEI_LOCK);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mImeiLockRegistrant != null) {
            ril.mImeiLockRegistrant.notifyRegistrants(
                                new AsyncResult(null, null, null));
        }
    }

    public void onImsiRefreshDone(int indicationType) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_IMSI_REFRESH_DONE);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mImsiRefreshDoneRegistrant != null) {
            ril.mImsiRefreshDoneRegistrant.notifyRegistrants(
                                new AsyncResult(null, null, null));
        }
    }

    public void onCardDetectedInd(int indicationType) {
    }
    // MTK-END:

    // SMS-START
    public void newEtwsInd(int indicationType, EtwsNotification etws) {
        mRadioIndication.mRil.processIndication(indicationType);

        MtkEtwsNotification response = new MtkEtwsNotification();
        response.messageId = etws.messageId;
        response.serialNumber = etws.serialNumber;
        response.warningType = etws.warningType;
        response.plmnId = etws.plmnId;
        response.securityInfo = etws.securityInfo;

        if (ENG) mRadioIndication.mRil.unsljLogRet(
                MtkRILConstants.RIL_UNSOL_RESPONSE_ETWS_NOTIFICATION, response);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mEtwsNotificationRegistrant != null) {
            ril.mEtwsNotificationRegistrant.notifyRegistrant(new AsyncResult(null, response, null));
        }
    }

    public void meSmsStorageFullInd(int indicationType) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_ME_SMS_STORAGE_FULL);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mMeSmsFullRegistrant != null) {
            ril.mMeSmsFullRegistrant.notifyRegistrant();
        }
    }

    public void smsReadyInd(int indicationType) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_SMS_READY_NOTIFICATION);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);

        if (ril.mSmsReadyRegistrants.size() != 0) {
            ril.mSmsReadyRegistrants.notifyRegistrants();
        } else {
            // Phone process is not ready and cache it then wait register to notify
            if (ENG) mRadioIndication.mRil.riljLog("Cache sms ready event");
            ril.mIsSmsReady = true;
        }
    }
    // SMS-END

    // DATA
    public void dataAllowedNotification(int indicationType, int isAllowed) {
        mRadioIndication.mRil.processIndication(indicationType);

        int response[] = new int[1];
        response[0] = isAllowed;

        if (RIL.RILJ_LOGD) {
            mMtkRil.unsljLogMore(RIL_UNSOL_DATA_ALLOWED, (isAllowed == 1) ? "true" : "false");
        }

        if (mMtkRil.mDataAllowedRegistrants != null) {
            mMtkRil.mDataAllowedRegistrants.notifyRegistrants(
                    new AsyncResult(null, response, null));
        }
    }

    // APC URC
    public void onPseudoCellInfoInd(int indicationType, ArrayList<Integer> info) {
        mMtkRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_PSEUDO_CELL_INFO);

        int[] response = new int[info.size()];
        for (int i = 0; i < info.size(); i++) {
            response[i] = info.get(i);
        }

        PseudoCellInfo cellInfo;
        String property = String.format("persist.vendor.radio.apc.mode%d", mMtkRil.mInstanceId);
        String propStr = SystemProperties.get(property, "0");
        int index = propStr.indexOf("=");
        if (index != -1){
            String subStr = propStr.substring(index + 1);
            String[] settings = subStr.split(",");
            int mode = Integer.parseInt(settings[0]);
            int report = Integer.parseInt(settings[1]);
            boolean enable = (report == 1) ? true : false;
            int interval = Integer.parseInt(settings[2]);
            cellInfo = new PseudoCellInfo(mode, enable, interval, response);
        } else {
            cellInfo = new PseudoCellInfo(0, false, 0, response);
        }

        if (mMtkRil.mPseudoCellInfoRegistrants != null) {
            mMtkRil.mPseudoCellInfoRegistrants.notifyRegistrants(
                    new AsyncResult(null, cellInfo, null));
        }
        //sendBroadcast apc infos
        Intent intent = new Intent(TelephonyIntents.ACTION_APC_INFO_NOTIFY);
        intent.putExtra(TelephonyIntents.EXTRA_APC_PHONE, mMtkRil.mInstanceId);
        intent.putExtra(TelephonyIntents.EXTRA_APC_INFO, cellInfo);
        mMtkRil.mMtkContext.sendBroadcast(intent);
        if (MtkRIL.RILJ_LOGD) {
            mMtkRil.riljLog("Broadcast for APC info:cellInfo=" + cellInfo.toString());
        }
    }
    // M: eMBMS feature
    /*
     * Indicates of eMBMS session activate status
     *
     * @param indicationType RadioIndicationType
     * @param status Activated session:1, else 0
     */
    public void eMBMSSessionStatusIndication(int indicationType, int status) {
        mMtkRil.processIndication(indicationType);
        Object ret = null;
        int response[] = new int[1];
        response[0] = status;
        if (MtkRIL.RILJ_LOGD) {
            mMtkRil.unsljLogRet(MtkRILConstants.RIL_UNSOL_EMBMS_SESSION_STATUS, ret);
        }

        if (mMtkRil.mEmbmsSessionStatusNotificationRegistrant.size() > 0) {
            if (MtkRIL.RILJ_LOGD) {
                mMtkRil.riljLog("Notify mEmbmsSessionStatusNotificationRegistrant");
            }
            mMtkRil.mEmbmsSessionStatusNotificationRegistrant.notifyRegistrants(
                new AsyncResult(null, response, null));
        } else {
            if (MtkRIL.RILJ_LOGD) {
                mMtkRil.riljLog("No mEmbmsSessionStatusNotificationRegistrant exist");
            }
        }

        Intent intent = new Intent(
            TelephonyIntents.ACTION_EMBMS_SESSION_STATUS_CHANGED);
        intent.putExtra(TelephonyIntents.EXTRA_IS_ACTIVE, status);
        mMtkRil.mMtkContext.sendBroadcast(intent);
    }

    // MTK-START: SIM HOT SWAP / SIM RECOVERY
    public void onSimPlugIn(int indicationType) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_SIM_PLUG_IN);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mSimPlugIn != null) {
            ril.mSimPlugIn.notifyRegistrants(
                                new AsyncResult(null, null, null));
        }
    }

    public void onSimPlugOut(int indicationType) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_SIM_PLUG_OUT);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mSimPlugOut != null) {
            ril.mSimPlugOut.notifyRegistrants(
                                new AsyncResult(null, null, null));
        }
    }

    public void onSimMissing(int indicationType, int simInserted) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_SIM_MISSING);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mSimMissing != null) {
            ril.mSimMissing.notifyRegistrants(
                                new AsyncResult(null, simInserted, null));
        }
    }

    public void onSimRecovery(int indicationType, int simInserted) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_SIM_RECOVERY);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mSimRecovery != null) {
            ril.mSimRecovery.notifyRegistrants(
                                new AsyncResult(null, simInserted, null));
        }
    }
    // MTK-END
    // MTK-START: SIM ME LOCK
    public void smlSlotLockInfoChangedInd(int indicationType, ArrayList<Integer> info) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) {
            mRadioIndication.mRil.unsljLog(
                    MtkRILConstants.RIL_UNSOL_SIM_SLOT_LOCK_POLICY_NOTIFY);
        }

        Object ret = null;
        int[] response = new int[info.size()];
        for (int i = 0; i < info.size(); i++) {
            response[i] = info.get(i);
        }
        ret = response;
        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        ril.mSmlSlotLockInfo = ret;
        if (ril.mSmlSlotLockInfoChanged.size() != 0) {
            ril.mSmlSlotLockInfoChanged.notifyRegistrants(
                    new AsyncResult(null, ret, null));
        }
    }
    // MTK-END

    /*
     * Indicates of eMBMS AT command event
     *
     * @param indicationType RadioIndicationType
     * @param info Information AT command string
     */
    public void eMBMSAtInfoIndication(int indicationType, String info) {
        mMtkRil.processIndication(indicationType);
        Object ret = null;
        String response = new String(info);
        ret = response;
        if (MtkRIL.RILJ_LOGD) {
            mMtkRil.unsljLogRet(MtkRILConstants.RIL_UNSOL_EMBMS_AT_INFO, ret);
        }

        if (mMtkRil.mEmbmsAtInfoNotificationRegistrant.size() > 0) {
            if (MtkRIL.RILJ_LOGD) {
                mMtkRil.riljLog("Notify mEmbmsAtInfoNotificationRegistrant");
            }
            mMtkRil.mEmbmsAtInfoNotificationRegistrant.notifyRegistrants(
                new AsyncResult(null, ret, null));
        } else {
            if (MtkRIL.RILJ_LOGD) {
                mMtkRil.riljLog("No mEmbmsAtInfoNotificationRegistrant exist");
            }
        }
    }
    /// M: eMBMS end

    // MTK-START: SIM COMMON SLOT
    public void onSimTrayPlugIn(int indicationType) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_TRAY_PLUG_IN);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mSimTrayPlugIn != null) {
            ril.mSimTrayPlugIn.notifyRegistrants(
                                new AsyncResult(null, null, null));
        }
    }

    public void onSimCommonSlotNoChanged(int indicationType) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.
                RIL_UNSOL_SIM_COMMON_SLOT_NO_CHANGED);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mSimCommonSlotNoChanged != null) {
            ril.mSimCommonSlotNoChanged.notifyRegistrants(
                                new AsyncResult(null, null, null));
        }
    }
    // MTK-END

    /**
     * Indicates when PLMN Changed.
     * @param indicationType RadioIndicationType
     * @param plmns ArrayList<String>
     */
    public void plmnChangedIndication(int indicationType, ArrayList<String> plmns) {
        mMtkRil.processIndication(indicationType);
        Object ret = null;
        String[] response = new String[plmns.size()];
        for (int i = 0; i < plmns.size(); i++) {
            response[i] = plmns.get(i);
        }
        ret = response;
        if (MtkRIL.RILJ_LOGD) {
            mMtkRil.unsljLogRet(RIL_UNSOL_RESPONSE_PLMN_CHANGED, ret);
        }
        synchronized (mMtkRil.mWPMonitor) {
            if (mMtkRil.mPlmnChangeNotificationRegistrant.size() > 0) {
                if (MtkRIL.RILJ_LOGD) {
                    mMtkRil.riljLog("ECOPS,notify mPlmnChangeNotificationRegistrant");
                }
                mMtkRil.mPlmnChangeNotificationRegistrant.notifyRegistrants(
                    new AsyncResult(null, ret, null));
            } else {
                mMtkRil.mEcopsReturnValue = ret;
            }
        }
    }

    /**
     * Indicates when need to registrtion.
     * @param indicationType RadioIndicationType
     * @param sessionIds ArrayList<Integer>
     */
    public void registrationSuspendedIndication(int indicationType, ArrayList<Integer> sessionIds) {
        mMtkRil.processIndication(indicationType);
        Object ret = null;
        int[] response = new int[sessionIds.size()];
        for (int i = 0; i < sessionIds.size(); i++) {
            response[i] = sessionIds.get(i);
        }
        ret = response;
        if (MtkRIL.RILJ_LOGD) {
            mMtkRil.unsljLogRet(RIL_UNSOL_RESPONSE_REGISTRATION_SUSPENDED, ret);
        }
        synchronized (mMtkRil.mWPMonitor) {
            if (mMtkRil.mRegistrationSuspendedRegistrant != null) {
                if (MtkRIL.RILJ_LOGD) {
                    mMtkRil.riljLog("EMSR, notify mRegistrationSuspendedRegistrant");
                }
                mMtkRil.mRegistrationSuspendedRegistrant.notifyRegistrant(
                    new AsyncResult(null, ret, null));
            } else {
                mMtkRil.mEmsrReturnValue = ret;
            }
        }
    }

    /**
     * Indicates when GMSS Rat changed.
     * @param indicationType RadioIndicationType
     * @param gmsss ArrayList<Integer>
     */
    public void gmssRatChangedIndication(int indicationType, ArrayList<Integer> gmsss) {
        mMtkRil.processIndication(indicationType);
        Object ret = null;
        int[] response = new int[gmsss.size()];
        for (int i = 0; i < gmsss.size(); i++) {
            response[i] = gmsss.get(i);
        }
        ret = response;
        if (MtkRIL.RILJ_LOGD) {
            mMtkRil.unsljLogRet(RIL_UNSOL_GMSS_RAT_CHANGED, ret);
        }
        int[] rat = (int[]) ret;
        if (mMtkRil.mGmssRatChangedRegistrant != null) {
            mMtkRil.mGmssRatChangedRegistrant.notifyRegistrants(new AsyncResult(null, rat, null));
        }
    }

    /**
     * Indicates when modem trigger world mode.
     * @param indicationType RadioIndicationType
     * @param modes ArrayList<Integer>
     */
    public void worldModeChangedIndication(int indicationType, ArrayList<Integer> modes) {
        mMtkRil.processIndication(indicationType);
        Object ret = null;
        int[] response = new int[modes.size()];
        for (int i = 0; i < modes.size(); i++) {
            response[i] = modes.get(i);
        }
        ret = response;
        if (MtkRIL.RILJ_LOGD) {
            mMtkRil.unsljLogRet(RIL_UNSOL_WORLD_MODE_CHANGED, ret);
        }
        int state = 1;
        boolean retvalue = false;
        if (ret != null) {
            state = ((int[]) ret)[0];
            //update switching state
            if (state == 2) { //rild init send end urc
                retvalue = WorldMode.resetSwitchingState(state);
                state = 1;
            } else if (state == 0) {
                retvalue = WorldMode.updateSwitchingState(true);
            } else {
                retvalue = WorldMode.updateSwitchingState(false);
            }
            if (false == retvalue) {
                return;
            }
            //sendBroadcast with state
            Intent intent = new Intent(WorldMode.ACTION_WORLD_MODE_CHANGED);
            intent.putExtra(WorldMode.EXTRA_WORLD_MODE_CHANGE_STATE, (Integer) state);
            mMtkRil.mMtkContext.sendBroadcast(intent);
            if (MtkRIL.RILJ_LOGD) {
                mMtkRil.riljLog("Broadcast for WorldModeChanged: state=" + state);
            }
        }
    }

    /**
     * Indicates when reset attach APN
     * @param indicationType RadioIndicationType
     */
    public void resetAttachApnInd(int indicationType) {
        mMtkRil.processIndication(indicationType);


        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_RESET_ATTACH_APN);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mResetAttachApnRegistrants != null) {
            ril.mResetAttachApnRegistrants.notifyRegistrants(
                                new AsyncResult(null, null, null));
        }
    }

    /**
     * Indicates when modem changes attach APN
     * @param indicationType RadioIndicationType
     * @param apnClassType class type for APN
     */
    public void mdChangedApnInd(int indicationType, int apnClassType) {
        mMtkRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_DATA_ATTACH_APN_CHANGED);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mAttachApnChangedRegistrants != null) {
            ril.mAttachApnChangedRegistrants.notifyRegistrants(
                                new AsyncResult(null, apnClassType, null));
        }
    }

    @Override
    public void esnMeidChangeInd(int indicationType, String esnMeid) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) {
            mRadioIndication.mRil.unsljLog(
                    MtkRILConstants.RIL_UNSOL_CDMA_CARD_INITIAL_ESN_OR_MEID);
        }

        MtkRIL ril = (MtkRIL) (mRadioIndication.mRil);

        if (ril.mCDMACardEsnMeidRegistrant != null) {
            ril.mCDMACardEsnMeidRegistrant.notifyRegistrant(new AsyncResult(null, esnMeid, null));
        } else {
            if (ENG) {
                mRadioIndication.mRil.riljLog("Cache esnMeidChangeInd");
            }
            ril.mEspOrMeid = (Object) esnMeid;
        }
    }

    public void phbReadyNotification(int indicationType, int isPhbReady) {
        mRadioIndication.mRil.processIndication(indicationType);

        int response[] = new int[1];
        response[0] = isPhbReady;

        if (RIL.RILJ_LOGD) {
            mRadioIndication.mRil.unsljLogMore(RIL_UNSOL_PHB_READY_NOTIFICATION,
                    "phbReadyNotification: " + isPhbReady);
        }

        if (((MtkRIL) mRadioIndication.mRil).mPhbReadyRegistrants != null) {
            ((MtkRIL) mRadioIndication.mRil).mPhbReadyRegistrants.notifyRegistrants(
                    new AsyncResult(null, response, null));
        }
    }

    // / M: BIP {
    public void bipProactiveCommand(int indicationType, String cmd) {
        mMtkRil.processIndication(indicationType);

        if (ENG) {
            mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_STK_BIP_PROACTIVE_COMMAND);
        }

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mBipProCmdRegistrant != null) {
            ril.mBipProCmdRegistrant.notifyRegistrants(new AsyncResult (null, cmd, null));
        }
    }
    // / M: BIP }
    // / M: OTASP {
    public void triggerOtaSP(int indicationType) {
        String[] testTriggerOtasp = new String[3];
        testTriggerOtasp[0] = "AT+CDV=*22899";
        testTriggerOtasp[1] = "";
        testTriggerOtasp[2] = "DESTRILD:C2K";
        mMtkRil.invokeOemRilRequestStrings(testTriggerOtasp, null);
    }
    // M: OTASP }

    // M: [VzW] Data Framework @{
    public void pcoDataAfterAttached(int indicationType, PcoDataAttachedInfo pco) {
        mMtkRil.processIndication(indicationType);

        PcoDataAfterAttached response = new PcoDataAfterAttached(pco.cid,
                pco.apnName,
                pco.bearerProto,
                pco.pcoId,
                RIL.arrayListToPrimitiveArray(pco.contents));

        if (RIL.RILJ_LOGD) mMtkRil.unsljLogRet(RIL_UNSOL_PCO_DATA_AFTER_ATTACHED, response);

        mMtkRil.mPcoDataAfterAttachedRegistrants.notifyRegistrants(
                new AsyncResult(null, response, null));
    }
    // M: [VzW] Data Framework @}

    // / M: STK {
    public void onStkMenuReset(int indicationType) {
        mMtkRil.processIndication(indicationType);

        if (ENG) {
            mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_STK_SETUP_MENU_RESET);
        }

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mStkSetupMenuResetRegistrant != null) {
            ril.mStkSetupMenuResetRegistrant.notifyRegistrants(new AsyncResult (null, null, null));
        }
    }
    // / M: STK }

    // M: [LTE][Low Power][UL traffic shaping] @{
    public void onLteAccessStratumStateChanged(int indicationType, ArrayList<Integer> state) {
        mMtkRil.processIndication(indicationType);

        int[] response = new int[state.size()];
        for (int i = 0; i < state.size(); i++) {
            response[i] = state.get(i);
        }

        if (RIL.RILJ_LOGD) mMtkRil.unsljLogRet(RIL_UNSOL_LTE_ACCESS_STRATUM_STATE_CHANGE, response);

        if (mMtkRil.mLteAccessStratumStateRegistrants != null) {
            mMtkRil.mLteAccessStratumStateRegistrants.notifyRegistrants(
                    new AsyncResult (null, response, null));
        }
    }
    // M: [LTE][Low Power][UL traffic shaping] @}

    public void networkInfoInd(int indicationType, ArrayList<String> networkinfo) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (RIL.RILJ_LOGD) {
            mRadioIndication.mRil.unsljLogMore(RIL_UNSOL_NETWORK_INFO, "networkInfo: " +
                    networkinfo);
        }

        String [] ret = networkinfo.toArray(new String[networkinfo.size()]);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mNetworkInfoRegistrant.size() !=  0) {
            ril.mNetworkInfoRegistrant.notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    // M: Data Framework - Data Retry enhancement
    public void onMdDataRetryCountReset(int indicationType) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) {
            mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_MD_DATA_RETRY_COUNT_RESET);
        }

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mMdDataRetryCountResetRegistrants != null) {
            ril.mMdDataRetryCountResetRegistrants.notifyRegistrants(
                                new AsyncResult(null, null, null));
        }
    }

    // M: Data Framework - CC 33
    public void onRemoveRestrictEutran(int indicationType) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) {
            mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_REMOVE_RESTRICT_EUTRAN);
        }

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (ril.mRemoveRestrictEutranRegistrants!= null) {
            ril.mRemoveRestrictEutranRegistrants.notifyRegistrants(
                                new AsyncResult(null, null, null));
        }
    }

    @Override
    public void confSRVCC(int indicationType, ArrayList<Integer> callIds) {
        mMtkRil.processIndication(indicationType);

        if (RIL.RILJ_LOGD) mMtkRil.unsljLog(RIL_UNSOL_ECONF_SRVCC_INDICATION);

        int[] response = new int[callIds.size()];
        for (int i = 0; i < callIds.size(); i++) {
            response[i] = callIds.get(i);
        }
        mMtkRil.mEconfSrvccRegistrants.notifyRegistrants(new AsyncResult(null, response, null));
    }

    // M: [VzW] Data Framework
    public void volteLteConnectionStatus(int indicationType, ArrayList<Integer> data) {
        mMtkRil.processIndication(indicationType);

        mMtkRil.unsljLog(MtkRILConstants.RIL_UNSOL_VOLTE_LTE_CONNECTION_STATUS);

        int[] connStatus = new int[data.size()];
        for (int i = 0; i < data.size(); i++) {
            connStatus[i] = data.get(i);
        }

        if (connStatus != null) {
            if (connStatus.length > 2) {
                if (MtkRIL.RILJ_LOGD) {
                    mMtkRil.riljLog("LTE_CONNECTION_STATUS - status: " +
                            connStatus[0] + ", reason: " + connStatus[1]);
                }
                if ((connStatus[0] > 10 && connStatus[0] < 20) ||
                    (connStatus[0] > 30 && connStatus[0] < 40)) {
                    Intent updateLteStatus =
                        new Intent("com.lge.ims.action.LTE_CONNECTION_STATUS");
                    updateLteStatus.putExtra("status", connStatus);
                    updateLteStatus.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    mMtkRil.mMtkContext.sendBroadcastAsUser(updateLteStatus, UserHandle.ALL);
                }
            }
        }
    }

    // External SIM [Start]
    public void onVsimEventIndication(int indicationType, VsimOperationEvent event) {
        mMtkRil.processIndication(indicationType);

        if (MtkRIL.RILJ_LOGV) {
            mRadioIndication.mRil.unsljLogRet(
                MtkRILConstants.RIL_UNSOL_VSIM_OPERATION_INDICATION,
                "len=" + new Integer(event.dataLength));
        }

        int length = ((event.dataLength > 0) ? (event.dataLength / 2 + 4) : 0);

        VsimEvent indicationEvent = new VsimEvent(
                event.transactionId, event.eventId, length,
                1 << mMtkRil.mInstanceId);
        if (length > 0) {
            indicationEvent.putInt(event.dataLength / 2);
            indicationEvent.putBytes(IccUtils.hexStringToBytes(event.data));
        }

        if (ENG) mRadioIndication.mRil.unsljLogRet(
                MtkRILConstants.RIL_UNSOL_VSIM_OPERATION_INDICATION, indicationEvent.toString());

        if (mMtkRil.mVsimIndicationRegistrants != null) {
            mMtkRil.mVsimIndicationRegistrants.notifyRegistrants(
                    new AsyncResult(null, indicationEvent, null));
        }
    }
    // External SIM [End]

    /// Ims Data Framework {@
    public void dedicatedBearerActivationInd(int indicationType, DedicateDataCall ddcResult) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (RIL.RILJ_LOGD) {
            mMtkRil.unsljLog(RIL_UNSOL_DEDICATE_BEARER_ACTIVATED);
        }

        MtkDedicateDataCallResponse ret = mMtkRil.convertDedicatedDataCallResult(ddcResult);
        if (RIL.RILJ_LOGD) {
            mMtkRil.riljLog(ret.toString());
        }

        if (mMtkRil.mDedicatedBearerActivedRegistrants != null) {
            mMtkRil.mDedicatedBearerActivedRegistrants.notifyRegistrants(
                    new AsyncResult(null, ret, null));
        }
    }

    public void dedicatedBearerModificationInd(int indicationType, DedicateDataCall ddcResult) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (RIL.RILJ_LOGD) {
            mMtkRil.unsljLog(RIL_UNSOL_DEDICATE_BEARER_MODIFIED);
        }

        MtkDedicateDataCallResponse ret = mMtkRil.convertDedicatedDataCallResult(ddcResult);
        if (RIL.RILJ_LOGD) {
            mMtkRil.riljLog(ret.toString());
        }

        if (mMtkRil.mDedicatedBearerModifiedRegistrants != null) {
            mMtkRil.mDedicatedBearerModifiedRegistrants.notifyRegistrants(
                    new AsyncResult(null, ret, null));
        }
    }

    public void dedicatedBearerDeactivationInd(int indicationType, int cid) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (RIL.RILJ_LOGD) {
            mMtkRil.unsljLog(RIL_UNSOL_DEDICATE_BEARER_DEACTIVATED);
        }

        if (RIL.RILJ_LOGD) {
            mMtkRil.riljLog("dedicatedBearerDeactivationInd, cid: " + cid);
        }

        if (mMtkRil.mDedicatedBearerDeactivatedRegistrants != null) {
            mMtkRil.mDedicatedBearerDeactivatedRegistrants.notifyRegistrants(
                    new AsyncResult(null, cid, null));
        }
    }
    /// @}

    /**
     * @param indicationType RadioIndicationType
     * @param data Data sent by oem
     */
    public void oemHookRaw(int indicationType, ArrayList<Byte> data) {
        mRadioIndication.mRil.processIndication(indicationType);

        byte[] response = RIL.arrayListToPrimitiveArray(data);
        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        if (RIL.RILJ_LOGD) {
            ril.unsljLogvRet(RIL_UNSOL_OEM_HOOK_RAW,
                    com.android.internal.telephony.uicc.IccUtils.bytesToHexString(response));
        }

        if (ril.mUnsolOemHookRegistrant != null) {
            ril.mUnsolOemHookRegistrant.notifyRegistrant(new AsyncResult(null, response, null));
        }
    }

    /**
     * Indicates TX power
     * @param indicationType RadioIndicationType
     * @param txPower ArrayList<Integer>
     */
    public void onTxPowerIndication(int indicationType, ArrayList<Integer> txPower) {
        mMtkRil.processIndication(indicationType);
        int[] response = new int[txPower.size()];
        for (int i = 0; i < txPower.size(); i++) {
            response[i] = txPower.get(i);
        }

        if (((MtkRIL) mRadioIndication.mRil).mTxPowerRegistrant != null) {
            ((MtkRIL) mRadioIndication.mRil).mTxPowerRegistrant.notifyRegistrants(
                    new AsyncResult(null, response, null));
        }
    }

    /**
     * URC for Tx power reduction
     * @param indicationType RadioIndicationType
     * @param txPower ArrayList<Integer>
     */
    public void onTxPowerStatusIndication(int indicationType, ArrayList<Integer> txPower) {
        mMtkRil.processIndication(indicationType);
        int[] response = new int[txPower.size()];
        for (int i = 0; i < txPower.size(); i++) {
            response[i] = txPower.get(i);
        }

        if (((MtkRIL) mRadioIndication.mRil).mTxPowerStatusRegistrant != null) {
            ((MtkRIL) mRadioIndication.mRil).mTxPowerStatusRegistrant.notifyRegistrants(
                    new AsyncResult(null, response, null));
        }
    }

    /**
     * Indicates when dsbp changes.
     * @param indicationType RadioIndicationType
     * @param DsbpState android.hardware.radio.V3_0.DsbpState
     */
    public void dsbpStateChanged(int indicationType, int dsbpState) {
        mRadioIndication.mRil.processIndication(indicationType);

        if (ENG) mRadioIndication.mRil.unsljLog(MtkRILConstants.RIL_UNSOL_DSBP_STATE_CHANGED);

        MtkRIL ril = (MtkRIL)(mRadioIndication.mRil);
        mMtkRil.riljLog("dsbpStateChanged state: " + dsbpState);
        if (ril.mDsbpStateRegistrant != null) {
            ril.mDsbpStateRegistrant.notifyRegistrants(
                    new AsyncResult(null, dsbpState, null));
        }
    }
}
