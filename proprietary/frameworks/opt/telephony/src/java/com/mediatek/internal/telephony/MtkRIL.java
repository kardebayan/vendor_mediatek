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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.HardwareConfig;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.RILRequest;
import com.android.internal.telephony.UUSInfo;
import com.mediatek.internal.telephony.MtkRILConstants;

/// CC: M: call control part @{
import android.hardware.radio.V1_0.Dial;
import android.hardware.radio.V1_0.UusInfo;

/// M: CC: DTMF request special handling
import java.util.Arrays;
import java.util.Vector;
/// @}

// MTK-START, SMS part
import android.hardware.radio.V1_0.CdmaSmsWriteArgs;
import vendor.mediatek.hardware.radio.V3_0.SmsParams;
import mediatek.telephony.MtkSmsParameters;
import com.mediatek.internal.telephony.MtkIccSmsStorageStatus;
// MTK-END, SMS part
import android.hardware.radio.V1_0.HardwareConfigModem;

/// M: eMBMS feature
import android.content.Intent;
import com.android.internal.telephony.PhoneConstants;
/// M: eMBMS end


// NW START
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.mediatek.internal.telephony.MtkTelephonyProperties;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.internal.telephony.uicc.MtkSIMRecords;
import com.mediatek.internal.telephony.uicc.MtkSpnOverride;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccController;
import java.io.UnsupportedEncodingException;
// NW END

import android.os.Message;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import vendor.mediatek.hardware.radio.V3_0.IRadio;

import android.os.RemoteException;
import com.android.internal.telephony.CommandException;
import static com.android.internal.telephony.RILConstants.*;
import static com.mediatek.internal.telephony.MtkRILConstants.*;

// MTK-START: SIM GBA
import vendor.mediatek.hardware.radio.V3_0.SimAuthStructure;
// MTK-END

// M: Set ECC ist to MD
import com.mediatek.internal.telephony.MtkPhoneNumberUtils;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils.EccEntry;

import java.util.ArrayList;

/// M: CC: Proprietary CRSS handling
import com.mediatek.internal.telephony.gsm.MtkSuppCrssNotification;

// PHB START
import com.mediatek.internal.telephony.phb.PBEntry;
import com.mediatek.internal.telephony.phb.PhbEntry;

import vendor.mediatek.hardware.radio.V3_0.PhbEntryExt;
import vendor.mediatek.hardware.radio.V3_0.PhbEntryStructure;
// PHB END

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// Ims Data Framework {@
import com.mediatek.internal.telephony.ims.MtkDedicateDataCallResponse;
import com.mediatek.internal.telephony.ims.MtkQosStatus;
import com.mediatek.internal.telephony.ims.MtkTftStatus;
import com.mediatek.internal.telephony.ims.MtkTftParameter;
import com.mediatek.internal.telephony.ims.MtkPacketFilterInfo;
import vendor.mediatek.hardware.radio.V3_0.DedicateDataCall;
import vendor.mediatek.hardware.radio.V3_0.Tft;
import vendor.mediatek.hardware.radio.V3_0.TftParameter;
import vendor.mediatek.hardware.radio.V3_0.PktFilter;
import vendor.mediatek.hardware.radio.V3_0.Qos;
/// @}

/**
 * MtkRIL implementation of the CommandsInterface.
 *
 * {@hide}
 */
public class MtkRIL extends RIL {
    static final String RILJ_LOG_TAG = "MtkRILJ";

    volatile vendor.mediatek.hardware.radio.V3_0.IRadio mRadioProxyMtk = null;
    MtkRadioResponse mRadioResponseMtk;
    MtkRadioIndication mRadioIndicationMtk;
    boolean mMtkRilJIntiDone = false;

    public Integer mInstanceId;

    protected Context mMtkContext;

    /// M: CC: Proprietary incoming call handling
    protected Registrant mIncomingCallIndicationRegistrant;
    /// M: CC: CDMA call accepted
    protected RegistrantList mCdmaCallAcceptedRegistrant = new RegistrantList();

    /// M: CC: GSM 02.07 B.1.26 Ciphering Indicator support
    protected RegistrantList mCipherIndicationRegistrant = new RegistrantList();

    // Femtocell (CSG) feature
    protected RegistrantList mFemtoCellInfoRegistrants = new RegistrantList();

    /// M: eMBMS feature
    protected RegistrantList mEmbmsSessionStatusNotificationRegistrant = new RegistrantList();
    protected RegistrantList mEmbmsAtInfoNotificationRegistrant = new RegistrantList();
    /// M: eMBMS end

    /// M: CC: GSA HD Voice for 2/3G network support
    protected Registrant mSpeechCodecInfoRegistrant;

    /// M: CC: Proprietary CRSS handling
    protected Registrant mCallRelatedSuppSvcRegistrant;

    // PHB
    public RegistrantList mPhbReadyRegistrants = new RegistrantList();

    /* MTK SS */
    protected RegistrantList mCallForwardingInfoRegistrants = new RegistrantList();

    protected RegistrantList mTxPowerRegistrant = new RegistrantList();

    protected RegistrantList mTxPowerStatusRegistrant = new RegistrantList();

    Object mCfuReturnValue = null;

    protected Registrant mUnsolOemHookRegistrant;

    /* SS part : [mtk04070][111118][ALPS00093395] */
    /* Add for Line2 */
    public static final int SERVICE_CLASS_LINE2 = (1 << 8);
    /* SERVICE_CLASS_VIDEO Service Supplementary Information
     * codes for Video Telephony support.
     */
    public static final int SERVICE_CLASS_VIDEO = (1 << 9);
    public static final int SERVICE_CLASS_MTK_MAX = (1 << 9); // Max SERVICE_CLASS value for MTK
    public static final int CF_REASON_NOT_REGISTERED = 6;
    public static final String CB_FACILITY_BA_ACR = "ACR";

    private IMtkRilOp mMtkRilOp = null;

    /// M: CC: DTMF request special handling @{
    /* DTMF request will be ignored when duplicated sending */
    class DtmfQueueHandler {
        public class DtmfQueueRR {
            public RILRequest rr;
            public Object[] params;

            public DtmfQueueRR(RILRequest rr, Object[] params) {
                this.rr = rr;
                this.params = params;
            }
        }

        public DtmfQueueHandler() {
            mDtmfStatus = DTMF_STATUS_STOP;
        }

        public void start() {
            mDtmfStatus = DTMF_STATUS_START;
        }

        public void stop() {
            mDtmfStatus = DTMF_STATUS_STOP;
        }

        public boolean isStart() {
            return (mDtmfStatus == DTMF_STATUS_START);
        }

        public void add(DtmfQueueRR o) {
            mDtmfQueue.addElement(o);
        }

        public void remove(DtmfQueueRR o) {
            mDtmfQueue.remove(o);
        }

        public void remove(int idx) {
            mDtmfQueue.removeElementAt(idx);
        }

        public DtmfQueueRR get() {
            return (DtmfQueueRR) mDtmfQueue.get(0);
        }

        public int size() {
            return mDtmfQueue.size();
        }

        public void setPendingRequest(DtmfQueueRR r) {
            mPendingCHLDRequest = r;
        }

        public DtmfQueueRR getPendingRequest() {
            return mPendingCHLDRequest;
        }

        public void setSendChldRequest() {
            mIsSendChldRequest = true;
        }

        public void resetSendChldRequest() {
            mIsSendChldRequest = false;
        }

        public boolean hasSendChldRequest() {
            mtkRiljLog("mIsSendChldRequest = " + mIsSendChldRequest);
            return mIsSendChldRequest;
        }

        public final int MAXIMUM_DTMF_REQUEST = 32;
        private final boolean DTMF_STATUS_START = true;
        private final boolean DTMF_STATUS_STOP = false;

        private boolean mDtmfStatus = DTMF_STATUS_STOP;
        private Vector mDtmfQueue = new Vector(MAXIMUM_DTMF_REQUEST);

        private DtmfQueueRR mPendingCHLDRequest = null;
        private boolean mIsSendChldRequest = false;

        public DtmfQueueHandler.DtmfQueueRR buildDtmfQueueRR(RILRequest rr, Object[] param) {
            if (rr == null) {
                return null;
            }
            if (RILJ_LOGD) {
                mtkRiljLog("DtmfQueueHandler.buildDtmfQueueRR build ([" + rr.mSerial + "] reqId="
                        + rr.mRequest + ")");
            }
            return new DtmfQueueHandler.DtmfQueueRR(rr, param);
        }
    }

    DtmfQueueHandler mDtmfReqQueue = new DtmfQueueHandler();
    /// @}

    // world phone {
    protected RegistrantList mPlmnChangeNotificationRegistrant = new RegistrantList();
    protected Registrant mRegistrationSuspendedRegistrant;
    protected Object mEmsrReturnValue = null;
    protected Object mEcopsReturnValue = null;
    protected Object mWPMonitor = new Object();
    protected RegistrantList mGmssRatChangedRegistrant = new RegistrantList();
    // world phone }
    protected RegistrantList mResetAttachApnRegistrants = new RegistrantList();
    protected RegistrantList mAttachApnChangedRegistrants = new RegistrantList();
    // M: [VzW] Data Framework
    protected RegistrantList mPcoDataAfterAttachedRegistrants = new RegistrantList();
    // M: Data Framework - CC 33
    protected RegistrantList mRemoveRestrictEutranRegistrants = new RegistrantList();
    // M: Data Framework - Data Retry enhancement
    protected RegistrantList mMdDataRetryCountResetRegistrants = new RegistrantList();

    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        private static final int MODE_CDMA_ASSERT = 31;
        private static final int MODE_CDMA_RESET = 32;

        private static final int MODE_PHONE_PROCESS_JE = 100;
        private static final int MODE_GSM_RILD_NE = 101;
        private static final int MODE_CDMA_RILD_NE = 103;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.mtk.TEST_TRM")) {
                int mode = intent.getIntExtra("mode", 2);
                Rlog.d(RILJ_LOG_TAG, "RIL received com.mtk.TEST_TRM, mode = "
                        + mode + ", mInstanceIds = " + mInstanceId);
                if (mode == MODE_PHONE_PROCESS_JE) {
                    throw new RuntimeException("UserTriggerPhoneJE");
                } else {
                    setTrm(mode, null);
                }
            } else {
                Rlog.w(RILJ_LOG_TAG, "RIL received unexpected Intent: " + intent.getAction());
            }
        }/* end of onReceive */
    };
    //M: NW
    private ArrayList<String> hide_plmns = new ArrayList<String>();
    //***** Constructors
    @VisibleForTesting
    public MtkRIL() {
    }

    public MtkRIL(Context context, int preferredNetworkType,
            int cdmaSubscription, Integer instanceId) {
        super(context, preferredNetworkType, cdmaSubscription, instanceId);
        Rlog.d(RILJ_LOG_TAG, "constructor: sub = " + instanceId);
        mMtkContext = context;
        mInstanceId = instanceId;
        mRadioResponseMtk = new MtkRadioResponse(this);
        mRadioIndicationMtk = new MtkRadioIndication(this);

        if (instanceId == 0) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.mtk.TEST_TRM");
            context.registerReceiver(mIntentReceiver, filter);
        }
        mMtkRilJIntiDone = true;
        getRadioProxy(null);
        getRilOp();
    }

    public vendor.mediatek.hardware.radio.V3_0.IRadio getRadioProxy(Message result) {
        if(!mMtkRilJIntiDone) {
            return null;
        }
        if (!mIsMobileNetworkSupported) {
            if (RILJ_LOGV) mtkRiljLog("getRadioProxy: Not calling getService(): wifi-only");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(RADIO_NOT_AVAILABLE));
                result.sendToTarget();
            }
            return null;
        }

        if (mRadioProxyMtk != null) {
            return mRadioProxyMtk;
        }

        try {
            mRadioProxyMtk =
                    vendor.mediatek.hardware.radio.V3_0.IRadio.getService(
                    HIDL_SERVICE_NAME[mPhoneId == null ? 0 : mPhoneId], false);
            if (mRadioProxyMtk != null) {
                mRadioProxyMtk.linkToDeath(mRadioProxyDeathRecipient,
                        mRadioProxyCookie.incrementAndGet());
                mRadioProxyMtk.setResponseFunctionsMtk(mRadioResponseMtk, mRadioIndicationMtk);
                mRadioProxyMtk.setResponseFunctions(mRadioResponse, mRadioIndication);

                /// M: CC: DTMF request special handling @{
                if (mDtmfReqQueue != null) {
                    synchronized (mDtmfReqQueue) {
                        int i;

                        if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "queue size  " + mDtmfReqQueue.size());

                        for (i = mDtmfReqQueue.size() - 1; i >= 0; i--) {
                            mDtmfReqQueue.remove(i);
                        }

                        if (mDtmfReqQueue.getPendingRequest() != null) {
                            Rlog.d(RILJ_LOG_TAG, "reset pending switch request");
                            /// M: for ALPS02418573. @{
                            // need check if there is pending request before calling
                            // setPendingRequest. if there is pending request and exist message,
                            // we must send it to target.
                            DtmfQueueHandler.DtmfQueueRR pendingDqrr = mDtmfReqQueue
                                    .getPendingRequest();
                            RILRequest pendingRequest = pendingDqrr.rr;
                            if (pendingRequest.mResult != null) {
                                AsyncResult.forMessage(pendingRequest.mResult, null, null);
                                pendingRequest.mResult.sendToTarget();
                            }
                            /// @}
                            mDtmfReqQueue.resetSendChldRequest();
                            mDtmfReqQueue.setPendingRequest(null);
                        }
                    }
                }
                /// @}

            } else {
                Rlog.e(RILJ_LOG_TAG, "getRadioProxy: mRadioProxy == null");
            }
        } catch (RemoteException | RuntimeException e) {
            mRadioProxyMtk = null;
            Rlog.e(RILJ_LOG_TAG, "RadioProxy getService/setResponseFunctions: " + e);
        }

        if (mRadioProxyMtk == null) {
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(RADIO_NOT_AVAILABLE));
                result.sendToTarget();
            }

            // if service is not up, treat it like death notification to try to get service again
            mRilHandler.sendMessageDelayed(
                    mRilHandler.obtainMessage(EVENT_RADIO_PROXY_DEAD, mRadioProxyCookie.get()),
                    IRADIO_GET_SERVICE_DELAY_MILLIS);
        }

        return mRadioProxyMtk;
    }

    public IMtkRilOp getRilOp() {
        Rlog.d(RILJ_LOG_TAG, "getRilOp");
        if (mMtkRilOp != null) {
            return mMtkRilOp;
        } else {
            String optr = SystemProperties.get("persist.vendor.operator.optr", "0");
            if ("0".equals(optr)) {
                Rlog.d(RILJ_LOG_TAG, "mMtkRilOp init fail, because OM load");
                return null;
            }
            String className = "com.mediatek.opcommon.telephony.MtkRilOp";
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
                Rlog.d(RILJ_LOG_TAG, "class = " + clazz);
                Constructor clazzConstructfunc = clazz.getConstructor(
                        new Class[]{Context.class, int.class, int.class, Integer.class});
                Rlog.d(RILJ_LOG_TAG, "constructor function = " + clazzConstructfunc);
                mMtkRilOp = (IMtkRilOp) clazzConstructfunc.newInstance(
                        mContext, mPreferredNetworkType, mCdmaSubscription, mPhoneId);
            } catch (Exception e) {
                Rlog.d(RILJ_LOG_TAG, "mMtkRilOp init fail");
                e.printStackTrace();
                return null;
            }
            return mMtkRilOp;
        }
    }

    protected void resetProxyAndRequestList() {
        mRadioProxyMtk = null;
        super.resetProxyAndRequestList();
    }

    public static String requestToStringEx(Integer request) {
        String msg;
        switch(request) {
            case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL_WITH_ACT:
                msg = "SET_NETWORK_SELECTION_MANUAL_WITH_ACT";
                break;
            case RIL_REQUEST_QUERY_AVAILABLE_NETWORKS_WITH_ACT:
                msg = "QUERY_AVAILABLE_NETWORKS_WITH_ACT";
                break;
            case RIL_REQUEST_SIGNAL_STRENGTH_WITH_WCDMA_ECIO:
                msg = "RIL_REQUEST_SIGNAL_STRENGTH_WITH_WCDMA_ECIO";
                break;
            case RIL_REQUEST_ABORT_QUERY_AVAILABLE_NETWORKS:
                msg = "ABORT_QUERY_AVAILABLE_NETWORKS";
                break;
            case MtkRILConstants.RIL_REQUEST_MODEM_POWERON:
                msg = "RIL_REQUEST_MODEM_POWERON";
                break;
            case MtkRILConstants.RIL_REQUEST_MODEM_POWEROFF:
                msg = "RIL_REQUEST_MODEM_POWEROFF";
                break;
            /// M: CC: Proprietary incoming call handling
            case RIL_REQUEST_SET_CALL_INDICATION:
                msg = "SET_CALL_INDICATION";
                break;
            case RIL_REQUEST_SET_CALL_INDICATION_WITH_CAUSE:
                msg = "SET_CALL_INDICATION_WITH_CAUSE";
                break;
            case RIL_REQUEST_HANGUP_WITH_CAUSE:
                msg = "HANGUP_WITH_CAUSE";
                break;
            /// M: eMBMS feature
            case RIL_REQUEST_EMBMS_AT_CMD:
                msg = "RIL_REQUEST_EMBMS_AT_CMD";
                break;
            /// M: eMBMS end
            /// M: CC: Proprietary ECC enhancement @{
            case RIL_REQUEST_EMERGENCY_DIAL:
                msg = "EMERGENCY_DIAL";
                break;
            case RIL_REQUEST_SET_ECC_SERVICE_CATEGORY:
                msg = "SET_ECC_SERVICE_CATEGORY";
                break;
            case RIL_REQUEST_CURRENT_STATUS:
                msg = "CURRENT_STATUS";
                break;
            case RIL_REQUEST_ECC_PREFERRED_RAT:
                msg = "ECC_PREFERRED_RAT";
                break;
            case RIL_REQUEST_SET_ECC_LIST:
                msg = "RIL_REQUEST_SET_ECC_LIST";
                break;
            /// @}
            /// M: CC: HangupAll for FTA 31.4.4.2
            case RIL_REQUEST_HANGUP_ALL:
                msg = "HANGUP_ALL";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_PSEUDO_CELL_MODE:
                msg = "RIL_REQUEST_SET_PSEUDO_CELL_MODE";
                break;
            case MtkRILConstants.RIL_REQUEST_GET_PSEUDO_CELL_INFO:
                msg = "RIL_REQUEST_GET_PSEUDO_CELL_INFO";
                break;
            case RIL_REQUEST_SWITCH_MODE_FOR_ECC:
                msg = "RIL_REQUEST_SWITCH_MODE_FOR_ECC";
                break;
            case RIL_REQUEST_GET_SMS_RUIM_MEM_STATUS:
                msg = "RIL_REQUEST_GET_SMS_RUIM_MEM_STATUS";
                break;
            case RIL_REQUEST_SET_FD_MODE:
                msg = "RIL_REQUEST_SET_FD_MODE";
                break;
            case RIL_REQUEST_RESUME_REGISTRATION:
                msg = "RIL_REQUEST_RESUME_REGISTRATION";
                break;
            case RIL_REQUEST_RELOAD_MODEM_TYPE:
                msg = "RIL_REQUEST_RELOAD_MODEM_TYPE";
                break;
            case RIL_REQUEST_STORE_MODEM_TYPE:
                msg = "RIL_REQUEST_STORE_MODEM_TYPE";
                break;
            case RIL_REQUEST_SET_TRM:
                msg = "RIL_REQUEST_SET_TRM";
                break;
            case RIL_REQUEST_RESTART_RILD:
                msg = "RIL_REQUEST_RESTART_RILD";
                break;
            //Femtocell (CSG) feature START
            case RIL_REQUEST_GET_FEMTOCELL_LIST:
                msg = "REQUEST_GET_FEMTOCELL_LIST";
                break;
            case RIL_REQUEST_ABORT_FEMTOCELL_LIST:
                msg = "REQUEST_ABORT_FEMTOCELL_LIST";
                break;
            case RIL_REQUEST_SELECT_FEMTOCELL:
                msg = "REQUEST_SELECT_FEMTOCELL";
                break;
            case RIL_REQUEST_QUERY_FEMTOCELL_SYSTEM_SELECTION_MODE:
                msg = "REQUEST_QUERY_FEMTOCELL_SYSTEM_SELECTION_MODE";
                break;
            case RIL_REQUEST_SET_FEMTOCELL_SYSTEM_SELECTION_MODE:
                msg = "REQUEST_SET_FEMTOCELL_SYSTEM_SELECTION_MODE";
                break;
            //Femtocell (CSG) feature END
            // PHB Start
            case RIL_REQUEST_QUERY_PHB_STORAGE_INFO: msg = "RIL_REQUEST_QUERY_PHB_STORAGE_INFO";
                break;
            case RIL_REQUEST_WRITE_PHB_ENTRY: msg = "RIL_REQUEST_WRITE_PHB_ENTRY";
                break;
            case RIL_REQUEST_READ_PHB_ENTRY: msg = "RIL_REQUEST_READ_PHB_ENTRY";
                break;
            case RIL_REQUEST_QUERY_UPB_CAPABILITY: msg = "RIL_REQUEST_QUERY_UPB_CAPABILITY";
                break;
            case RIL_REQUEST_EDIT_UPB_ENTRY: msg = "RIL_REQUEST_EDIT_UPB_ENTRY";
                break;
            case RIL_REQUEST_DELETE_UPB_ENTRY: msg = "RIL_REQUEST_DELETE_UPB_ENTRY";
                break;
            case RIL_REQUEST_READ_UPB_GAS_LIST: msg = "RIL_REQUEST_READ_UPB_GAS_LIST";
                break;
            case RIL_REQUEST_READ_UPB_GRP: msg = "RIL_REQUEST_READ_UPB_GRP";
                break;
            case RIL_REQUEST_WRITE_UPB_GRP: msg = "RIL_REQUEST_WRITE_UPB_GRP";
                break;
            case RIL_REQUEST_GET_PHB_STRING_LENGTH: msg = "RIL_REQUEST_GET_PHB_STRING_LENGTH";
                break;
            case RIL_REQUEST_GET_PHB_MEM_STORAGE: msg = "RIL_REQUEST_GET_PHB_MEM_STORAGE";
                break;
            case RIL_REQUEST_SET_PHB_MEM_STORAGE: msg = "RIL_REQUEST_SET_PHB_MEM_STORAGE";
                break;
            case RIL_REQUEST_READ_PHB_ENTRY_EXT: msg = "RIL_REQUEST_READ_PHB_ENTRY_EXT";
                break;
            case RIL_REQUEST_WRITE_PHB_ENTRY_EXT: msg = "RIL_REQUEST_WRITE_PHB_ENTRY_EXT";
                break;
            case RIL_REQUEST_QUERY_UPB_AVAILABLE: msg = "RIL_REQUEST_QUERY_UPB_AVAILABLE";
                break;
            case RIL_REQUEST_READ_EMAIL_ENTRY: msg = "RIL_REQUEST_READ_EMAIL_ENTRY";
                break;
            case RIL_REQUEST_READ_SNE_ENTRY: msg = "RIL_REQUEST_READ_SNE_ENTRY";
                break;
            case RIL_REQUEST_READ_ANR_ENTRY: msg = "RIL_REQUEST_READ_ANR_ENTRY";
                break;
            case RIL_REQUEST_READ_UPB_AAS_LIST: msg = "RIL_REQUEST_READ_UPB_AAS_LIST";
                break;
            // PHB End
            // MTK_TC1_FEATURE for Antenna Testing start
            case RIL_REQUEST_VSS_ANTENNA_CONF:
                msg = "RIL_REQUEST_VSS_ANTENNA_CONF";
                break;
            case RIL_REQUEST_VSS_ANTENNA_INFO:
                msg = "RIL_REQUEST_VSS_ANTENNA_INFO";
                break;
            // MTK_TC1_FEATURE for Antenna Testing end
            case RIL_REQUEST_GET_POL_CAPABILITY:
                msg = "RIL_REQUEST_GET_POL_CAPABILITY";
                break;
            case RIL_REQUEST_GET_POL_LIST:
                msg = "RIL_REQUEST_GET_POL_LIST";
                break;
            case RIL_REQUEST_SET_POL_ENTRY:
                msg = "RIL_REQUEST_SET_POL_ENTRY";
                break;
            // M: Data Framework - common part enhancement @}
            case RIL_REQUEST_SYNC_DATA_SETTINGS_TO_MD:
                msg = "RIL_REQUEST_SYNC_DATA_SETTINGS_TO_MD";
                break;
            // M: Data Framework - common part enhancement @}
            // M: Data Framework - Data Retry enhancement
            case RIL_REQUEST_RESET_MD_DATA_RETRY_COUNT:
                msg = "RIL_REQUEST_RESET_MD_DATA_RETRY_COUNT";
                break;
                // M: Data Framework - CC 33
            case RIL_REQUEST_SET_REMOVE_RESTRICT_EUTRAN_MODE:
                msg = "RIL_REQUEST_SET_REMOVE_RESTRICT_EUTRAN_MODE";
                break;
            // M: [LTE][Low Power][UL traffic shaping] @{
            case RIL_REQUEST_SET_LTE_ACCESS_STRATUM_REPORT:
                msg = "RIL_REQUEST_SET_LTE_ACCESS_STRATUM_REPORT";
                break;
            case RIL_REQUEST_SET_LTE_UPLINK_DATA_TRANSFER:
                msg = "RIL_REQUEST_SET_LTE_UPLINK_DATA_TRANSFER";
                break;
            // M: [LTE][Low Power][UL traffic shaping] @}
            // MTK-START: SIM
            case MtkRILConstants.RIL_REQUEST_QUERY_SIM_NETWORK_LOCK:
                msg = "RIL_REQUEST_QUERY_SIM_NETWORK_LOCK";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_SIM_NETWORK_LOCK:
                msg = "RIL_REQUEST_SET_SIM_NETWORK_LOCK";
                break;
            /// M: [Network][C2K] Sprint roaming control @{
            case RIL_REQUEST_SET_ROAMING_ENABLE:
                msg = "SET_ROAMING_ENABLE";
                break;
            case RIL_REQUEST_GET_ROAMING_ENABLE:
                msg = "GET_ROAMING_ENABLE";
                break;
            /// @}
            // External SIM [Start]
            case MtkRILConstants.RIL_REQUEST_VSIM_NOTIFICATION:
                msg = "RIL_REQUEST_VSIM_NOTIFICATION";
                break;
            case MtkRILConstants.RIL_REQUEST_VSIM_OPERATION:
                msg = "RIL_REQUEST_VSIM_OPERATION";
                break;
            // External SIM [End]
            // WFC [Start]
            case MtkRILConstants.RIL_REQUEST_SET_WIFI_ENABLED:
                msg = "RIL_REQUEST_SET_WIFI_ENABLED";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_WIFI_ASSOCIATED:
                msg = "RIL_REQUEST_SET_WIFI_ASSOCIATED";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_WIFI_SIGNAL_LEVEL:
                msg = "RIL_REQUEST_SET_WIFI_SIGNAL_LEVEL";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_WIFI_IP_ADDRESS:
                msg = "RIL_REQUEST_SET_WIFI_IP_ADDRESS";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_GEO_LOCATION:
                msg = "RIL_REQUEST_SET_GEO_LOCATION";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_EMERGENCY_ADDRESS_ID:
                msg = "RIL_REQUEST_SET_EMERGENCY_ADDRESS_ID";
                break;
            // WFC [End]
            case MtkRILConstants.RIL_REQUEST_SET_E911_STATE:
                msg = "RIL_REQUEST_SET_E911_STATE";
                break;
            // Network [Start]
            case MtkRILConstants.RIL_REQUEST_SET_SERVICE_STATE:
                msg = "RIL_REQUEST_SET_SERVICE_STATE";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_LTE_RELEASE_VERSION:
                msg = "RIL_REQUEST_SET_LTE_RELEASE_VERSION";
                break;
            case MtkRILConstants.RIL_REQUEST_GET_LTE_RELEASE_VERSION:
                msg = "RIL_REQUEST_GET_LTE_RELEASE_VERSION";
                break;
            // Network [End]
            // SS [Start]
            case MtkRILConstants.RIL_REQUEST_SET_CLIP:
                msg = "RIL_REQUEST_SET_CLIP";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_COLP:
                msg = "RIL_REQUEST_SET_COLP";
                break;
            case MtkRILConstants.RIL_REQUEST_GET_COLP:
                msg = "RIL_REQUEST_GET_COLP";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_COLR:
                msg = "RIL_REQUEST_SET_COLR";
                break;
            case MtkRILConstants.RIL_REQUEST_GET_COLR:
                msg = "RIL_REQUEST_GET_COLR";
                break;
            case MtkRILConstants.RIL_REQUEST_SEND_CNAP:
                msg = "RIL_REQUEST_SEND_CNAP";
                break;
            case MtkRILConstants.RIL_REQUEST_QUERY_CALL_FORWARD_IN_TIME_SLOT:
                msg = "RIL_REQUEST_QUERY_CALL_FORWARD_IN_TIME_SLOT";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_CALL_FORWARD_IN_TIME_SLOT:
                msg = "RIL_REQUEST_SET_CALL_FORWARD_IN_TIME_SLOT";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_SS_PROPERTY:
                msg = "RIL_REQUEST_SET_SS_PROPERTY";
                break;
            case MtkRILConstants.RIL_REQUEST_GET_SS_PROPERTY:
                msg = "RIL_REQUEST_GET_SS_PROPERTY";
                break;
            // SS [End]
            case RIL_REQUEST_OEM_HOOK_RAW:
                msg =  "OEM_HOOK_RAW";
                break;
            case RIL_REQUEST_OEM_HOOK_STRINGS:
                msg =  "OEM_HOOK_STRINGS";
                break;
            // SMS-START
            case MtkRILConstants.RIL_REQUEST_GSM_SET_BROADCAST_LANGUAGE:
                msg = "RIL_REQUEST_GSM_SET_BROADCAST_LANGUAGE";
                break;
            case MtkRILConstants.RIL_REQUEST_GSM_GET_BROADCAST_LANGUAGE:
                msg = "RIL_REQUEST_GSM_GET_BROADCAST_LANGUAGE";
                break;
            case MtkRILConstants.RIL_REQUEST_GET_SMS_SIM_MEM_STATUS:
                msg = "RIL_REQUEST_GET_SMS_SIM_MEM_STATUS";
                break;
            case MtkRILConstants.RIL_REQUEST_GET_SMS_PARAMS:
                msg = "RIL_REQUEST_GET_SMS_PARAMS";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_SMS_PARAMS:
                msg = "RIL_REQUEST_SET_SMS_PARAMS";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_ETWS:
                msg = "RIL_REQUEST_SET_ETWS";
                break;
            case MtkRILConstants.RIL_REQUEST_REMOVE_CB_MESSAGE:
                msg = "RIL_REQUEST_REMOVE_CB_MESSAGE";
                break;
            case MtkRILConstants.RIL_REQUEST_GET_GSM_SMS_BROADCAST_ACTIVATION:
                msg = "RIL_REQUEST_GET_GSM_SMS_BROADCAST_ACTIVATION";
                break;
            case MtkRILConstants.RIL_REQUEST_IMS_SEND_SMS_EX:
                msg = "RIL_REQUEST_IMS_SEND_SMS_EX";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_SMS_FWK_READY:
                msg = "RIL_REQUEST_SET_SMS_FWK_READY";
                break;
            // SMS-END
            case RIL_REQUEST_DATA_CONNECTION_ATTACH:
                msg = "RIL_REQUEST_DATA_CONNECTION_ATTACH";
                break;
            case RIL_REQUEST_DATA_CONNECTION_DETACH:
                msg = "RIL_REQUEST_DATA_CONNECTION_DETACH";
                break;
            case RIL_REQUEST_RESET_ALL_CONNECTIONS:
                msg = "RIL_REQUEST_RESET_ALL_CONNECTIONS";
                break;
            case RIL_REQUEST_SET_VOICE_PREFER_STATUS:
                msg = "RIL_REQUEST_SET_VOICE_PREFER_STATUS";
                break;
            case RIL_REQUEST_SET_ECC_NUM:
                msg = "RIL_REQUEST_SET_ECC_NUM";
                break;
            case RIL_REQUEST_GET_ECC_NUM:
                msg = "RIL_REQUEST_GET_ECC_NUM";
                break;
            case MtkRILConstants.RIL_REQUEST_REPORT_AIRPLANE_MODE:
                msg = "RIL_REQUEST_REPORT_AIRPLANE_MODE";
                break;
            case MtkRILConstants.RIL_REQUEST_REPORT_SIM_MODE:
                msg = "RIL_REQUEST_REPORT_SIM_MODE";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_SILENT_REBOOT:
                msg = "RIL_REQUEST_SET_SILENT_REBOOT";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_PHONEBOOK_READY:
                msg = "RIL_REQUEST_SET_PHONEBOOK_READY";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_TX_POWER_STATUS:
                msg = "RIL_REQUEST_SET_TX_POWER_STATUS";
                break;
            case RIL_REQUEST_SETPROP_IMS_HANDOVER:
                msg = "RIL_REQUEST_SETPROP_IMS_HANDOVER";
                break;
            case MtkRILConstants.RIL_REQUEST_SET_OPERATOR_CONFIGURATION:
                msg = "RIL_REQUEST_SET_OPERATOR_CONFIGURATION";
                break;
            case RIL_REQUEST_SET_PDN_REUSE:
                msg = "RIL_REQUEST_SET_PDN_REUSE";
                break;
            case RIL_REQUEST_SET_OVERRIDE_APN:
                msg = "RIL_REQUEST_SET_OVERRIDE_APN";
                break;
            case RIL_REQUEST_SET_PDN_NAME_REUSE:
                msg = "RIL_REQUEST_SET_PDN_NAME_REUSE";
                break;
            // SIM ME LOCK
            case MtkRILConstants.RIL_REQUEST_ENTER_DEVICE_NETWORK_DEPERSONALIZATION:
                msg = "RIL_REQUEST_ENTER_DEVICE_NETWORK_DEPERSONALIZATION";
                break;
            default:
                msg = "<unknown request> " + request;
                break;
        }
        return "MTK: " + msg;
    }

    public static String responseToStringEx(Integer request) {
        String msg;
        switch(request) {
            case RIL_UNSOL_DATA_ALLOWED:
                msg = "RIL_UNSOL_DATA_ALLOWED";
                break;
            /// M: CC: Proprietary incoming call handling
            case RIL_UNSOL_INCOMING_CALL_INDICATION:
                msg = "UNSOL_INCOMING_CALL_INDICATION";
                break;
            /// M: CC: GSM 02.07 B.1.26 Ciphering Indicator support
            case RIL_UNSOL_CIPHER_INDICATION:
                msg = "UNSOL_CIPHER_INDICATION";
                break;
            /// M: CC: Proprietary CRSS handling
            case RIL_UNSOL_CRSS_NOTIFICATION:
                msg = "UNSOL_CRSS_NOTIFICATION";
                break;
            /// M: CC: GSA HD Voice for 2/3G network support
            case RIL_UNSOL_SPEECH_CODEC_INFO:
                msg = "UNSOL_SPEECH_CODEC_INFO";
                break;
            /// M: CC: CDMA call accepted. @{
            case RIL_UNSOL_CDMA_CALL_ACCEPTED:
                msg = "UNSOL_CDMA_CALL_ACCEPTED";
                break;
            /// @}
            case RIL_UNSOL_INVALID_SIM:
                msg = "RIL_UNSOL_INVALID_SIM";
                break;
            case RIL_UNSOL_NETWORK_EVENT:
                msg = "RIL_UNSOL_NETWORK_EVENT";
                break;
            case RIL_UNSOL_MODULATION_INFO:
                msg = "RIL_UNSOL_MODULATION_INFO";
                break;
            case RIL_UNSOL_PSEUDO_CELL_INFO:
                msg = "RIL_UNSOL_PSEUDO_CELL_INFO";
                break;
            /// M: eMBMS feature
            case RIL_UNSOL_EMBMS_SESSION_STATUS:
                msg = "RIL_UNSOL_EMBMS_SESSION_STATUS";
                break;
            case RIL_UNSOL_EMBMS_AT_INFO:
                msg = "RIL_UNSOL_EMBMS_AT_INFO";
                break;
            /// M: eMBMS end
            case RIL_UNSOL_WORLD_MODE_CHANGED:
                msg = "RIL_UNSOL_WORLD_MODE_CHANGED";
                break;
            case RIL_UNSOL_GMSS_RAT_CHANGED:
                msg = "RIL_UNSOL_GMSS_RAT_CHANGED";
                break;
            case RIL_UNSOL_RESPONSE_REGISTRATION_SUSPENDED:
                msg = "RIL_UNSOL_RESPONSE_REGISTRATION_SUSPENDED";
                break;
            case RIL_UNSOL_RESPONSE_PLMN_CHANGED:
                msg = "RIL_UNSOL_RESPONSE_PLMN_CHANGED";
                break;
            case RIL_UNSOL_CDMA_CARD_INITIAL_ESN_OR_MEID:
                msg = "RIL_UNSOL_CDMA_CARD_INITIAL_ESN_OR_MEID";
                break;
            case RIL_UNSOL_RESET_ATTACH_APN:
                msg = "RIL_UNSOL_RESET_ATTACH_APN";
                break;
            case RIL_UNSOL_DATA_ATTACH_APN_CHANGED:
                msg = "RIL_UNSOL_DATA_ATTACH_APN_CHANGED";
                break;
            case RIL_UNSOL_FEMTOCELL_INFO:
                msg = "UNSOL_FEMTOCELL_INFO";
                break;
            // M: Data Framework - Data Retry enhancement
            case RIL_UNSOL_MD_DATA_RETRY_COUNT_RESET:
                msg = "RIL_UNSOL_MD_DATA_RETRY_COUNT_RESET";
                break;
            // M: Data Framework - CC 33
            case RIL_UNSOL_REMOVE_RESTRICT_EUTRAN:
                msg = "RIL_UNSOL_REMOVE_RESTRICT_EUTRAN";
                break;
            // PHB START
            case RIL_UNSOL_PHB_READY_NOTIFICATION:
                msg = "UNSOL_PHB_READY_NOTIFICATION";
                break;
            // PHB END
            case RIL_UNSOL_NETWORK_INFO:
                msg = "UNSOL_NETWORK_INFO";
                break;
            case RIL_UNSOL_SIGNAL_STRENGTH_WITH_WCDMA_ECIO:
                msg = "UNSOL_SIGNAL_STRENGTH_WITH_WCDMA_ECIO";
                break;
            case RIL_UNSOL_CALL_FORWARDING:
                msg = "UNSOL_CALL_FORWARDING";
                break;
            // IMS conference SRVCC
            case RIL_UNSOL_ECONF_SRVCC_INDICATION:
                msg = "RIL_UNSOL_ECONF_SRVCC_INDICATION";
                break;
            // M: [LTE][Low Power][UL traffic shaping] @{
            case RIL_UNSOL_LTE_ACCESS_STRATUM_STATE_CHANGE:
                msg = "RIL_UNSOL_LTE_ACCESS_STRATUM_STATE_CHANGE";
                break;
            // M: [LTE][Low Power][UL traffic shaping] @}
            // External SIM [Start]
            case RIL_UNSOL_VSIM_OPERATION_INDICATION:
                msg = "RIL_UNSOL_VSIM_OPERATION_INDICATION";
                break;
            // External SIM [End]
            /// Ims Data Framework @{
            case RIL_UNSOL_DEDICATE_BEARER_ACTIVATED:
                return "RIL_UNSOL_DEDICATE_BEARER_ACTIVATED";
            case RIL_UNSOL_DEDICATE_BEARER_MODIFIED:
                return "RIL_UNSOL_DEDICATE_BEARER_MODIFIED";
            case RIL_UNSOL_DEDICATE_BEARER_DEACTIVATED:
                return "RIL_UNSOL_DEDICATE_BEARER_DEACTIVATED";
            /// @}
            case RIL_UNSOL_MOBILE_WIFI_ROVEOUT:
                msg = "RIL_UNSOL_MOBILE_WIFI_ROVEOUT";
                break;
            case RIL_UNSOL_MOBILE_WIFI_HANDOVER:
                msg = "RIL_UNSOL_MOBILE_WIFI_HANDOVER";
                break;
            case RIL_UNSOL_ACTIVE_WIFI_PDN_COUNT:
                msg = "RIL_UNSOL_ACTIVE_WIFI_PDN_COUNT";
                break;
            case RIL_UNSOL_WIFI_RSSI_MONITORING_CONFIG:
                msg = "RIL_UNSOL_WIFI_RSSI_MONITORING_CONFIG";
                break;
            case RIL_UNSOL_WIFI_PDN_ERROR:
                msg = "RIL_UNSOL_WIFI_PDN_ERROR";
                break;
            case RIL_UNSOL_REQUEST_GEO_LOCATION:
                msg = "RIL_UNSOL_REQUEST_GEO_LOCATION";
                break;
            case RIL_UNSOL_WFC_PDN_STATE:
                msg = "RIL_UNSOL_WFC_PDN_STATE";
                break;
            case RIL_UNSOL_NATT_KEEP_ALIVE_CHANGED:
                msg = "RIL_UNSOL_NATT_KEEP_ALIVE_CHANGED";
                break;
            case RIL_UNSOL_PCO_DATA_AFTER_ATTACHED:
                msg = "RIL_UNSOL_PCO_DATA_AFTER_ATTACHED";
                break;
            case RIL_UNSOL_OEM_HOOK_RAW:
                msg = "UNSOL_OEM_HOOK_RAW";
                break;
            case RIL_UNSOL_WIFI_PDN_OOS:
                msg = "RIL_UNSOL_WIFI_PDN_OOS";
                break;
            case RIL_UNSOL_ECC_NUM:
                msg = "RIL_UNSOL_ECC_NUM";
                break;
            case RIL_UNSOL_MCCMNC_CHANGED:
                msg = "RIL_UNSOL_MCCMNC_CHANGED";
                break;
            case RIL_UNSOL_NETWORK_REJECT_CAUSE:
                msg = "RIL_UNSOL_NETWORK_REJECT_CAUSE";
                break;
            case RIL_UNSOL_DSBP_STATE_CHANGED:
                msg = "RIL_UNSOL_DSBP_STATE_CHANGED";
                break;
            // SIM ME LOCK
            case RIL_UNSOL_SIM_SLOT_LOCK_POLICY_NOTIFY:
                msg = "RIL_UNSOL_SIM_SLOT_LOCK_POLICY_NOTIFY";
                break;
            default:
                msg = "<unknown response>";
                break;
        }
        return "MTK: " + msg;
    }

    /* M: network part start */
    protected RegistrantList mCsNetworkStateRegistrants = new RegistrantList();
    /* M: network part end */

    public void registerForCsNetworkStateChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);

        mCsNetworkStateRegistrants.add(r);
    }

    public void unregisterForCsNetworkStateChanged(Handler h) {
        mCsNetworkStateRegistrants.remove(h);
    }

    /* M: network part start,Add for RIL_UNSOL_SIGNAL_STRENGTH_WITH_WCDMA_ECIO CHANGED */
    protected RegistrantList mSignalStrengthWithWcdmaEcioRegistrants = new RegistrantList();

    /**
     * Registers the handler for when signal strength changed events
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForSignalStrengthWithWcdmaEcioChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mSignalStrengthWithWcdmaEcioRegistrants.add(r);
    }

    public void unregisterForignalStrengthWithWcdmaEcioChanged(Handler h) {
        mSignalStrengthWithWcdmaEcioRegistrants.remove(h);
    }

    protected static String retToString(int req, Object ret) {
        return RIL.retToString(req, ret);
    }

    public void setTrm(int mode, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_TRM, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.setTrm(rr.mSerial, mode);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setTrm", e);
            }
        }
    }

    // MTK-START: SIM
    /**
     * {@inheritDoc}
     */
    public void getATR(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_SIM_GET_ATR, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getATR(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getATR", e);
            }
        }
    }

    /**
     * Get SIM card's Iccid.
     * @param result messge object.
     */
    public void getIccid(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_SIM_GET_ICCID, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.getIccid(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getIccid", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setSimPower(int mode, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_SET_SIM_POWER, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.setSimPower(rr.mSerial, mode);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setSimPower", e);
            }
        }
    }

    protected RegistrantList mVirtualSimOn = new RegistrantList();
    protected RegistrantList mVirtualSimOff = new RegistrantList();
    protected RegistrantList mImeiLockRegistrant = new RegistrantList();
    protected RegistrantList mImsiRefreshDoneRegistrant = new RegistrantList();

    public void registerForVirtualSimOn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mVirtualSimOn.add(r);
    }

    public void unregisterForVirtualSimOn(Handler h) {
        mVirtualSimOn.remove(h);
    }

    public void registerForVirtualSimOff(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mVirtualSimOff.add(r);
    }

    public void unregisterForVirtualSimOff(Handler h) {
        mVirtualSimOff.remove(h);
    }

    public void registerForIMEILock(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mImeiLockRegistrant.add(r);
    }

    public void unregisterForIMEILock(Handler h) {
        mImeiLockRegistrant.remove(h);
    }

    public void registerForImsiRefreshDone(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mImsiRefreshDoneRegistrant.add(r);
    }

    public void unregisterForImsiRefreshDone(Handler h) {
        mImsiRefreshDoneRegistrant.remove(h);
    }

    // MTK-END

    // MTK-START: SIM GBA
    /**
     * Convert to SimAuthStructure defined in types.hal
     * @param sessionId sessionId
     * @param mode Auth mode
     * @param tag Used for GBA mode
     * @param param1
     * @param param2
     * @return A converted SimAuthStructure for hal
     */
    private SimAuthStructure convertToHalSimAuthStructure(int sessionId, int mode,
            int tag, String param1, String param2) {
        SimAuthStructure simAuth = new SimAuthStructure();
        simAuth.sessionId = sessionId;
        simAuth.mode = mode;
        if (param1 != null && param1.length() > 0) {
            String length = Integer.toHexString(param1.length() / 2);
            length = (((length.length() % 2 == 1) ? "0" : "") + length);
            // Session id is equal to 0, for backward compability, we use old AT command
            // old AT command no need to include param's length
            simAuth.param1 = convertNullToEmptyString(((sessionId == 0) ?
                    param1 : (length + param1)));
        } else {
            simAuth.param1 = convertNullToEmptyString(param1);
        }

        // Calcuate param2 length in byte length
        if (param2 != null && param2.length() > 0) {
            String length = Integer.toHexString(param2.length() / 2);
            length = (((length.length() % 2 == 1) ? "0" : "") + length);
            // Session id is equal to 0, for backward compability, we use old AT command
            // old AT command no need to include param's length
            simAuth.param2 = convertNullToEmptyString(((sessionId == 0) ?
                    param2 : (length + param2)));
        } else {
            simAuth.param2 = convertNullToEmptyString(param2);
        }
        if (mode == 1) {
            simAuth.tag = tag;
        }
        return simAuth;
    }

    /**
     * {@inheritDoc}
     */
    public void doGeneralSimAuthentication(int sessionId, int mode, int tag,
            String param1, String param2, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_GENERAL_SIM_AUTH, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            SimAuthStructure simAuth = convertToHalSimAuthStructure(sessionId, mode, tag,
                    param1, param2);
            try {
                radioProxy.doGeneralSimAuthentication(rr.mSerial, simAuth);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "doGeneralSimAuthentication", e);
            }
        }
    }
    // MTK-END

    // MTK-START: NW
    /* M: Network part start */
    public String lookupOperatorName(int subId, String numeric,
            boolean desireLongName, int nLac) {
        String operatorName = null;

        /*
         * Operator name from SIM (EONS/CPHS) has highest priority to
         * display. To get operator name from OPL/PNN/CPHS, we need
         * lac info.
         */
        UiccController uiccController = UiccController.getInstance();
        MtkSIMRecords simRecord = (MtkSIMRecords) uiccController
                .getIccRecords(mInstanceId, UiccController.APP_FAM_3GPP);
        String sEons = null;
        Rlog.d(RILJ_LOG_TAG, "subId=" + subId + " numeric=" + numeric
                + " desireLongName=" + desireLongName + " nLac=" + nLac);

        if (mPhoneType == RILConstants.GSM_PHONE) {
            if ((nLac != 0xfffe) && (nLac != -1)) {
                try {
                    sEons = (simRecord != null) ?
                            simRecord.getEonsIfExist(numeric, nLac, desireLongName) : null;
                } catch (RuntimeException ex) {
                    Rlog.e(RILJ_LOG_TAG, "Exception while getEonsIfExist. " + ex);
                }

                if (sEons != null && !sEons.equals("")) {
                    Rlog.d(RILJ_LOG_TAG, "plmn name update to Eons: " + sEons);
                    return sEons;
                }
            } else {
                Rlog.d(RILJ_LOG_TAG, "invalid lac ignored");
            }

            // CPHS operator name shall
            // only be used for HPLMN name dispaly
            String mSimOperatorNumeric = (simRecord != null) ?
                    simRecord.getOperatorNumeric() : null;
            if ((mSimOperatorNumeric != null) &&
                    (mSimOperatorNumeric.equals(numeric))) {
                String sCphsOns = null;
                sCphsOns = (simRecord != null) ? simRecord.getSIMCPHSOns() : null;
                /// M: check the network operator names' validation
                if (!TextUtils.isEmpty(sCphsOns)) {
                    Rlog.d(RILJ_LOG_TAG, "plmn name update to CPHS Ons: "
                            + sCphsOns);
                    return sCphsOns;
                }
            }
        }

        /* Operator name from network MM information */
        int phoneId = SubscriptionManager.getPhoneId(subId);
        String nitzOperatorNumeric = null;
        String nitzOperatorName = null;

        nitzOperatorNumeric = TelephonyManager.getTelephonyProperty(phoneId,
                MtkTelephonyProperties.PROPERTY_NITZ_OPER_CODE, "");
        if ((numeric != null) && (numeric.equals(nitzOperatorNumeric))) {
            if (desireLongName == true) {
                nitzOperatorName = TelephonyManager.getTelephonyProperty(phoneId,
                        MtkTelephonyProperties.PROPERTY_NITZ_OPER_LNAME, "");
            } else {
                nitzOperatorName = TelephonyManager.getTelephonyProperty(phoneId,
                        MtkTelephonyProperties.PROPERTY_NITZ_OPER_SNAME, "");
            }

            /* handle UCS2 format name : prefix + hex string ex: "uCs2806F767C79D1" */
            if ((nitzOperatorName != null) && (nitzOperatorName.startsWith("uCs2") == true))
            {
                Rlog.d(RILJ_LOG_TAG, "lookupOperatorName() handling UCS2 format name");
                try {
                    nitzOperatorName = new String(
                            IccUtils.hexStringToBytes(nitzOperatorName.substring(4)), "UTF-16");
                } catch (UnsupportedEncodingException ex) {
                    Rlog.d(RILJ_LOG_TAG, "lookupOperatorName() UnsupportedEncodingException");
                }
            }

            Rlog.d(RILJ_LOG_TAG, "plmn name update to Nitz: "
                    + nitzOperatorName);
            /// M: check the network operator names' validation
            if (!TextUtils.isEmpty(nitzOperatorName)) {
                return nitzOperatorName;
            }
        }

        /* Default display manufacturer maintained operator name table */
        if (numeric != null) {
            operatorName = MtkServiceStateTracker.lookupOperatorName(
                    mMtkContext, subId, numeric, desireLongName);
            Rlog.d(RILJ_LOG_TAG, "plmn name update to MVNO: " + operatorName);
            return operatorName;
        }

        return null;
    }

    /**
     * Select a network with act.
     * @param operatorNumeric is mcc/mnc of network.
     * @param act is technology  of network.
     * @param mode selection mode.
     * @param result messge object.
     */
    public void
    setNetworkSelectionModeManualWithAct(String operatorNumeric,
            String act, int mode, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL_WITH_ACT, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " operatorNumeric = " + operatorNumeric);
            }

            try {
                radioProxy.setNetworkSelectionModeManualWithAct(rr.mSerial,
                        convertNullToEmptyString(operatorNumeric),
                        convertNullToEmptyString(act), Integer.toString(mode));
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rr, "setNetworkSelectionModeManual", e);
            }
        }
    }

    /**
     * Check whether to hide the plmn.
     * @param mccmcn PLMN
     * @return true if we need hide this plmn
     */
    public boolean hidePLMN(String mccmnc) {
        for (String plmn: hide_plmns) {
            if (plmn.equals(mccmnc)) return true;
        }
        return false;
    }

    /**
     * Queries the currently available networks with ACT.
     * @param result messge object.
     */
    public void
    getAvailableNetworksWithAct(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_AVAILABLE_NETWORKS_WITH_ACT, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.getAvailableNetworksWithAct(rr.mSerial);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rr, "getAvailableNetworks", e);
            }
        }
    }

    /**
     * Cancel queries the currently available networks with ACT.
     * @param result messge object.
     */
    public void
    cancelAvailableNetworks(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ABORT_QUERY_AVAILABLE_NETWORKS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.cancelAvailableNetworks(rr.mSerial);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rr, "getAvailableNetworks", e);
            }
        }
    }

    // Femtocell (CSG) feature START
    /**
     * Get Femtocell list.
     * @param result messge object.
     */
    public void getFemtoCellList(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_FEMTOCELL_LIST, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }
            try {
                radioProxy.getFemtocellList(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getFemtoCellList", e);
            }
        }
    }

    /**
     * Cancel Femtocell list.
     * @param result messge object.
     */
    public void abortFemtoCellList(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ABORT_FEMTOCELL_LIST, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }
            try {
                radioProxy.abortFemtocellList(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "abortFemtoCellList", e);
            }
        }
    }

    /**
     * Select Femtocells.
     * @param femtocell information.
     * @param result messge object.
     */
    public void selectFemtoCell(FemtoCellInfo femtocell, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SELECT_FEMTOCELL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            int act = femtocell.getCsgRat();
            if (act == ServiceState.RIL_RADIO_TECHNOLOGY_LTE) {
                act = 7;
            } else if (act == ServiceState.RIL_RADIO_TECHNOLOGY_UMTS) {
                act = 2;
            } else {
                act = 0;
            }

            mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " csgId="
                    + femtocell.getCsgId() + " plmn=" + femtocell.getOperatorNumeric() + " rat="
                    + femtocell.getCsgRat() + " act=" + act);
            try {
                radioProxy.selectFemtocell(rr.mSerial,
                        convertNullToEmptyString(femtocell.getOperatorNumeric()),
                        convertNullToEmptyString(Integer.toString(act)),
                        convertNullToEmptyString(Integer.toString(femtocell.getCsgId())));
            } catch (Exception e) {
                handleRadioProxyExceptionForRR(rr, "selectFemtoCell", e);
            }
        }
    }

    /**
     * Query Femtocell system selection mode.
     * @param result messge object.
     */
    public void queryFemtoCellSystemSelectionMode(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_FEMTOCELL_SYSTEM_SELECTION_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.queryFemtoCellSystemSelectionMode(rr.mSerial);
            } catch (Exception e) {
                handleRadioProxyExceptionForRR(rr, "queryFemtoCellSystemSelectionMode", e);
            }
        }
    }

    /**
     * Set Femtocell system selection mode.
     * @param mode system selection mode.
     * @param result messge object.
     */
    public void setFemtoCellSystemSelectionMode(int mode, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_FEMTOCELL_SYSTEM_SELECTION_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " mode=" + mode);
            }

            try {
                radioProxy.setFemtoCellSystemSelectionMode(rr.mSerial, mode);
            } catch (Exception e) {
                handleRadioProxyExceptionForRR(rr, "setFemtoCellSystemSelectionMode", e);
            }
        }
    }

    /**
     * getSignalStrengthWithWcdmaEcio get signal strength with wcdma_ecio.
     * @param result messge object.
     */
    public void getSignalStrengthWithWcdmaEcio(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIGNAL_STRENGTH_WITH_WCDMA_ECIO, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getSignalStrengthWithWcdmaEcio(rr.mSerial);
            } catch (Exception e) {
                handleRadioProxyExceptionForRR(rr, "getSignalStrength", e);
            }
        }
    }

    // Femtocell (CSG) feature END
    // MTK-END: NW

    public void setModemPower(boolean isOn, Message result) {

        if (RILJ_LOGD) mtkRiljLog("Set Modem power as: " + isOn);
        RILRequest rr;
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            if (isOn) {
                rr = obtainRequest(MtkRILConstants.RIL_REQUEST_MODEM_POWERON, result,
                        mRILDefaultWorkSource);
            } else {
                rr = obtainRequest(MtkRILConstants.RIL_REQUEST_MODEM_POWEROFF, result,
                        mRILDefaultWorkSource);
            }

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " " + isOn);
            }

            try {
                radioProxy.setModemPower(rr.mSerial, isOn);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setModemPower", e);
            }
        }
    }

    protected RegistrantList mInvalidSimInfoRegistrant = new RegistrantList();

    public void setInvalidSimInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mInvalidSimInfoRegistrant.add(r);
    }

    public void unSetInvalidSimInfo(Handler h) {
        mInvalidSimInfoRegistrant.remove(h);
    }

    protected RegistrantList mNetworkEventRegistrants = new RegistrantList();

    public void registerForNetworkEvent(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mNetworkEventRegistrants.add(r);
    }

    public void unregisterForNetworkEvent(Handler h) {
        mNetworkEventRegistrants.remove(h);
    }

    protected RegistrantList mNetworkRejectRegistrants = new RegistrantList();

    public void registerForNetworkReject(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mNetworkRejectRegistrants.add(r);
    }

    public void unregisterForNetworkReject(Handler h) {
        mNetworkRejectRegistrants.remove(h);
    }

    protected RegistrantList mModulationRegistrants = new RegistrantList();

    public void registerForModulation(Handler h, int what, Object obj) {
        // for op only
        IMtkRilOp rilOp = getRilOp();
        if (rilOp != null) {
            rilOp.registerForModulation(h, what, obj);
            return;
        }
    }

    public void unregisterForModulation(Handler h) {
        // for op only
        IMtkRilOp rilOp = getRilOp();
        if (rilOp != null) {
            rilOp.unregisterForModulation(h);
            return;
        }
    }

    /**
     * Register for femto cell information.
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForFemtoCellInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mFemtoCellInfoRegistrants.add(r);
    }

    /**
     * Unregister for femto cell information.
     * @param h Handler for notification message.
     */
    public void unregisterForFemtoCellInfo(Handler h) {
        mFemtoCellInfoRegistrants.remove(h);
    }

    // SMS-START
    // In order to cache the event from modem at boot-up sequence
    public boolean mIsSmsReady = false;
    protected RegistrantList mSmsReadyRegistrants = new RegistrantList();
    protected Registrant mMeSmsFullRegistrant;
    protected Registrant mEtwsNotificationRegistrant;
    protected Registrant mCDMACardEsnMeidRegistrant;
    protected Object mEspOrMeid = null;

    public void registerForSmsReady(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mSmsReadyRegistrants.add(r);

        if (mIsSmsReady == true) {
            // Only notify the new registrant
            r.notifyRegistrant();
        }
    }

    public void unregisterForSmsReady(Handler h) {
        mSmsReadyRegistrants.remove(h);
    }

    public void setOnMeSmsFull(Handler h, int what, Object obj) {
        mMeSmsFullRegistrant = new Registrant(h, what, obj);
    }

    public void unSetOnMeSmsFull(Handler h) {
        mMeSmsFullRegistrant.clear();
    }

    public void setOnEtwsNotification(Handler h, int what, Object obj) {
        mEtwsNotificationRegistrant = new Registrant(h, what, obj);
    }

    public void unSetOnEtwsNotification(Handler h) {
        mEtwsNotificationRegistrant.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void getSmsParameters(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_GET_SMS_PARAMS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getSmsParameters(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getSmsParameters", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setSmsParameters(MtkSmsParameters params, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_SET_SMS_PARAMS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            SmsParams smsp = new SmsParams();
            smsp.dcs = params.dcs;
            smsp.format = params.format;
            smsp.pid = params.pid;
            smsp.vp = params.vp;
            try {
                radioProxy.setSmsParameters(rr.mSerial, smsp);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setSmsParameters", e);
            }
        }
    }
    /**
     * {@inheritDoc}
     */
    public void setEtws(int mode, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_SET_ETWS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.setEtws(rr.mSerial, mode);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setEtws", e);
            }
        }
    }

    public void removeCellBroadcastMsg(int channelId, int serialId, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_REMOVE_CB_MESSAGE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " +
                    channelId + ", " + serialId);

            try {
                radioProxy.removeCbMsg(rr.mSerial, channelId, serialId);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "removeCellBroadcastMsg", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void getSmsSimMemoryStatus(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_GET_SMS_SIM_MEM_STATUS,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getSmsMemStatus(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getSmsSimMemoryStatus", e);
            }
        }
    }

    public void setGsmBroadcastLangs(String lang, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_GSM_SET_BROADCAST_LANGUAGE,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) +
                    ", lang:" + lang);

            try {
                radioProxy.setGsmBroadcastLangs(rr.mSerial, lang);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setGsmBroadcastLangs", e);
            }
        }
    }

    public void getGsmBroadcastLangs(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_GSM_GET_BROADCAST_LANGUAGE,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getGsmBroadcastLangs(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getGsmBroadcastLangs", e);
            }
        }
    }

    public void getGsmBroadcastActivation(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(
                    MtkRILConstants.RIL_REQUEST_GET_GSM_SMS_BROADCAST_ACTIVATION,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getGsmBroadcastActivation(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getGsmBroadcastActivation", e);
            }
        }
    }

    /**
     * Register ESN/MEID change report.
     *
     * @param h the handler to handle the message
     * @param what the message ID
     * @param obj the user data of the message reciever
     */
    public void setCDMACardInitalEsnMeid(Handler h, int what, Object obj) {
        mCDMACardEsnMeidRegistrant = new Registrant(h, what, obj);
        if (mEspOrMeid != null) {
            mCDMACardEsnMeidRegistrant.notifyRegistrant(new AsyncResult(null, mEspOrMeid, null));
        }
    }

    @Override
    public void writeSmsToRuim(int status, String pdu, Message result) {
        status = translateStatus(status);
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGV) {
                mtkRiljLog(rr.serialString() + "> "
                        + requestToString(rr.mRequest)
                        + " status = " + status);
            }

            CdmaSmsWriteArgs args = new CdmaSmsWriteArgs();
            args.status = status;
            constructCdmaSendSmsRilRequest(args.message, IccUtils.hexStringToBytes(pdu));

            try {
                radioProxy.writeSmsToRuim(rr.mSerial, args);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "writeSmsToRuim", e);
            }
        }
    }

    public void setSmsFwkReady(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(
                    MtkRILConstants.RIL_REQUEST_SET_SMS_FWK_READY,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.setSmsFwkReady(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setSmsFwkReady", e);
            }
        }
    }
    // SMS-END

    protected RegistrantList mPsNetworkStateRegistrants = new RegistrantList();
    public void registerForPsNetworkStateChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mPsNetworkStateRegistrants.add(r);
    }

    public void unregisterForPsNetworkStateChanged(Handler h) {
        mPsNetworkStateRegistrants.remove(h);
    }

    protected RegistrantList mNetworkInfoRegistrant = new RegistrantList();
    public void registerForNetworkInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mNetworkInfoRegistrant.add(r);
    }

    public void unregisterForNetworkInfo(Handler h) {
        mNetworkInfoRegistrant.remove(h);
    }

    /* MTK SS Feature : Start */
    /**
     * Change Cal Barring Password (checked by Network)
     */
    public void changeBarringPassword(String facility, String oldPwd, String newPwd,
                                      String newCfm, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CHANGE_BARRING_PASSWORD, result,
                    mRILDefaultWorkSource);

            // Do not log all function args for privacy
            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + "facility = " + facility);
            }

            try {
                radioProxy.setBarringPasswordCheckedByNW(rr.mSerial,
                        convertNullToEmptyString(facility),
                        convertNullToEmptyString(oldPwd),
                        convertNullToEmptyString(newPwd),
                        convertNullToEmptyString(newCfm));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "changeBarringPasswordCheckedByNW", e);
            }
        }
    }

    /**
     * Set CLIP (Calling Line Identification Presentation)
     */
    public void setCLIP(int clipEnable, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_SET_CLIP, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " clipEnable = " + clipEnable);
            }

            try {
                radioProxy.setClip(rr.mSerial, clipEnable);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setCLIP", e);
            }
        }
    }

    /**
     * Query COLP (Connected Line Identification Presentation)
     */
    public void getCOLP(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_GET_COLP, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.getColp(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getCOLP", e);
            }
        }
    }

    /**
     * Query COLR (Connected Line Identification Restriction)
     */
    public void getCOLR(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_GET_COLR, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.getColr(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getCOLR", e);
            }
        }
    }

    /**
     * Query CNAP (Calling Name Presentation)
     */
    public void sendCNAP(String cnapssMessage, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_SEND_CNAP, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) +
                        "CNAP string = " + cnapssMessage);
            }

            try {
                radioProxy.sendCnap(rr.mSerial, cnapssMessage);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "sendCNAP", e);
            }
        }
    }

    /**
     * MTK Method
     * Set colr
     */
    public void setCOLR(int colrEnable, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_SET_COLR, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " colrEnable = " + colrEnable);
            }

            try {
                radioProxy.setColr(rr.mSerial, colrEnable);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setCOLR", e);
            }
        }
    }

    /**
     * Set colp
     */
    public void setCOLP(int colpEnable, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_SET_COLP, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " colpEnable = " + colpEnable);
            }

            try {
                radioProxy.setColp(rr.mSerial, colpEnable);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setCOLP", e);
            }
        }
    }

    /**
     * Query Call Forward in Time Slot
     */
    public void queryCallForwardInTimeSlotStatus(int cfReason, int serviceClass, Message result) {
        String number = "";
        String timeSlotBegin = "";
        String timeSlotEnd = "";

        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(
                    MtkRILConstants.RIL_REQUEST_QUERY_CALL_FORWARD_IN_TIME_SLOT,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " cfreason = " + cfReason + " serviceClass = " + serviceClass);
            }

            vendor.mediatek.hardware.radio.V3_0.CallForwardInfoEx cfInfoEx =
                    new vendor.mediatek.hardware.radio.V3_0.CallForwardInfoEx();
            cfInfoEx.reason = cfReason;
            cfInfoEx.serviceClass = serviceClass;
            cfInfoEx.toa = PhoneNumberUtils.toaFromString(number); // not in used
            cfInfoEx.number = convertNullToEmptyString(number);
            cfInfoEx.timeSeconds = 0;
            cfInfoEx.timeSlotBegin = convertNullToEmptyString(timeSlotBegin);
            cfInfoEx.timeSlotEnd = convertNullToEmptyString(timeSlotEnd);

            try {
                radioProxy.queryCallForwardInTimeSlotStatus(rr.mSerial, cfInfoEx);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "queryCallForwardInTimeSlotStatus", e);
            }
        }
    }

    /**
     * Set Call Forward in Time Slot
     */
    public void setCallForwardInTimeSlot(int action, int cfReason, int serviceClass,
                   String number, int timeSeconds, long[] timeSlot, Message result) {
        String timeSlotBegin = "";
        String timeSlotEnd = "";

        // convertToSeverTime
        if (timeSlot != null && timeSlot.length == 2) {
            for (int i = 0; i < timeSlot.length; i++) {
                Date date = new Date(timeSlot[i]);
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));

                if (i == 0) {
                    timeSlotBegin = dateFormat.format(date);
                } else {
                    timeSlotEnd = dateFormat.format(date);
                }
            }
        }

        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_SET_CALL_FORWARD_IN_TIME_SLOT,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " action = " + action + " cfReason = " + cfReason + " serviceClass = "
                        + serviceClass + " timeSeconds = " + timeSeconds
                        + "timeSlot = [" + timeSlotBegin + ":" + timeSlotEnd + "]");
            }

            vendor.mediatek.hardware.radio.V3_0.CallForwardInfoEx cfInfoEx =
                    new vendor.mediatek.hardware.radio.V3_0.CallForwardInfoEx();
            cfInfoEx.status = action;
            cfInfoEx.reason = cfReason;
            cfInfoEx.serviceClass = serviceClass;
            cfInfoEx.toa = PhoneNumberUtils.toaFromString(number);
            cfInfoEx.number = convertNullToEmptyString(number);
            cfInfoEx.timeSeconds = timeSeconds;
            cfInfoEx.timeSlotBegin = convertNullToEmptyString(timeSlotBegin);
            cfInfoEx.timeSlotEnd = convertNullToEmptyString(timeSlotEnd);

            try {
                radioProxy.setCallForwardInTimeSlot(rr.mSerial, cfInfoEx);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setCallForwardInTimeSlot", e);

            }
        }
    }

    /**
     * run Gba Authentication
     * @param nafFqdn NAF FQDN String
     * @param nafSecureProtocolId nafSecureProtocolId Value
     * @param forceRun boolean
     * @param netId Integer
     * @param phoneId Integer
     * @param result Response Data Parcel
     */
    public void runGbaAuthentication(String nafFqdn, String nafSecureProtocolId,
            boolean forceRun, int netId, int phoneId, Message result) {

        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_RUN_GBA,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString()
                        + ">  " + requestToString(rr.mRequest)
                        + " nafFqdn = " + nafFqdn
                        + " nafSecureProtocolId = " + nafSecureProtocolId
                        + " forceRun = " + forceRun
                        + " netId = " + netId);
            }

            try {
                radioProxy.runGbaAuthentication(rr.mSerial, nafFqdn, nafSecureProtocolId,
                        forceRun, netId);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "runGbaAuthentication", e);
            }
        }
    }
    /* MTK SS Feature : End */

    // DATA
    protected RegistrantList mDataAllowedRegistrants = new RegistrantList();
    public void registerForDataAllowed(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDataAllowedRegistrants.add(r);
    }

    /// M: eMBMS feature
    /**
     * Send eMBMS Command String.
     *
     * @param data command string
     * @param result messge object.
     */
    public void sendEmbmsAtCommand(String data, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_EMBMS_AT_CMD, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " data: "
                        + data);
            }

            try {
                radioProxy.sendEmbmsAtCommand(rr.mSerial, data);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rr, "sendEmbmsAtCommand", e);
            }
        }
    }

    /**
     * Register for eMBMS.
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void setEmbmsSessionStatusNotification(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mEmbmsSessionStatusNotificationRegistrant.add(r);
    }

    /**
     * Unregister for eMBMS.
     * @param h Handler for notification message.
     */
    public void unSetEmbmsSessionStatusNotification(Handler h) {
        mEmbmsSessionStatusNotificationRegistrant.remove(h);
    }
    /// M: eMBMS end

    /**
     * Register for plmn.
     * When AP receive plmn Urc to decide to target is in home or roaming.
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void setAtInfoNotification(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mEmbmsAtInfoNotificationRegistrant.add(r);
    }


    public void unSetAtInfoNotification(Handler h) {
        mEmbmsAtInfoNotificationRegistrant.remove(h);
    }

    public void unregisterForDataAllowed(Handler h) {
        mDataAllowedRegistrants.remove(h);
    }

    /* TODO: Need to add mCfuReturnValue to airplane mode listener and SIM plugout event */
    /* MTK SS: CFU Notification */
    public void registerForCallForwardingInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        Rlog.d(RILJ_LOG_TAG, "call registerForCallForwardingInfo, Handler : " + h);
        mCallForwardingInfoRegistrants.add(r);

        /* If someone register this event, notify the handler immediately to update */
        if (mCfuReturnValue != null) {
            r.notifyRegistrant(new AsyncResult(null, mCfuReturnValue, null));
        }
    }

    public void unregisterForCallForwardingInfo(Handler h) {
        mCallForwardingInfoRegistrants.remove(h);
    }

    /// M: CC: Proprietary incoming call handling @{
    public void setOnIncomingCallIndication(Handler h, int what, Object obj) {
        mIncomingCallIndicationRegistrant = new Registrant(h, what, obj);
    }

    public void unsetOnIncomingCallIndication(Handler h) {
        mIncomingCallIndicationRegistrant.clear();
    }
    /// @}

    /// M: CC: Proprietary CRSS handling @{
    public void setOnCallRelatedSuppSvc(Handler h, int what, Object obj) {
        mCallRelatedSuppSvcRegistrant = new Registrant(h, what, obj);
    }

    public void unSetOnCallRelatedSuppSvc(Handler h) {
        mCallRelatedSuppSvcRegistrant.clear();
    }
    /// @}

    /// M: CC: GSM 02.07 B.1.26 Ciphering Indicator support @{
    /**
     * Registers the handler when network reports cipher indication info for the voice call.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForCipherIndication(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mCipherIndicationRegistrant.add(r);
    }

    /**
     * Unregister for notifications when network reports cipher indication info for the voice call.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForCipherIndication(Handler h) {
        mCipherIndicationRegistrant.remove(h);
    }
    /// @}

    /// M: CC: CDMA call accepted @{
    /**
     * Register the handler for cdma call accepted.
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForCdmaCallAccepted(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mCdmaCallAcceptedRegistrant.add(r);
    }

    /**
     * Unregister the handler for cdma call accepted.
     * @param h Handler for notification message.
     */
    public void unregisterForCdmaCallAccepted(Handler h) {
        mCdmaCallAcceptedRegistrant.remove(h);
    }
    /// @}

    /// M: CC: DTMF request special handling @{
    /*
     * to protect modem status we need to avoid two case :
     * 1. DTMF start -> CHLD request -> DTMF stop
     * 2. CHLD request -> DTMF request
     */
    private void handleChldRelatedRequest(RILRequest rr, Object[] params) {
        synchronized (mDtmfReqQueue) {
            int queueSize = mDtmfReqQueue.size();
            int i, j;
            if (queueSize > 0) {
                DtmfQueueHandler.DtmfQueueRR dqrr2 = mDtmfReqQueue.get();
                RILRequest rr2 = dqrr2.rr;
                if (rr2.mRequest == RIL_REQUEST_DTMF_START) {
                    // need to send the STOP command
                    if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "DTMF queue isn't 0, first request is START, "
                            + "send stop dtmf and pending switch");
                    if (queueSize > 1) {
                        j = 2;
                    } else {
                        // need to create a new STOP command
                        j = 1;
                    }
                    if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "queue size  " + mDtmfReqQueue.size());

                    for (i = queueSize - 1; i >= j; i--) {
                        mDtmfReqQueue.remove(i);
                    }
                    if (mDtmfReqQueue.size() == 1) { // only start command
                                                     // , we need to add stop command
                        if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "add dummy stop dtmf request");

                        RILRequest rr3 = obtainRequest(RIL_REQUEST_DTMF_STOP, null,
                                mRILDefaultWorkSource);
                        Class[] myClz = { int.class };
                        Object[] myParam = { rr3.mSerial };
                        DtmfQueueHandler.DtmfQueueRR dqrr3 = mDtmfReqQueue.buildDtmfQueueRR(
                                rr3, myParam);

                        mDtmfReqQueue.stop();
                        mDtmfReqQueue.add(dqrr3);
                    }
                }
                else {
                    // first request is STOP, just remove it and send switch
                    if (RILJ_LOGD)
                        Rlog.d(RILJ_LOG_TAG, "DTMF queue isn't 0, first request is STOP, penging switch");
                    j = 1;
                    for (i = queueSize - 1; i >= j; i--) {
                        mDtmfReqQueue.remove(i);
                    }
                }

                /// M: for ALPS02418573. @{
                // need check if there is pending request before calling setPendingRequest.
                // if there is pending request and exist message. we must send it to target.
                if (mDtmfReqQueue.getPendingRequest() != null){
                    DtmfQueueHandler.DtmfQueueRR pendingDqrr = mDtmfReqQueue.getPendingRequest();
                    RILRequest pendingRequest = pendingDqrr.rr;
                    if (pendingRequest.mResult != null) {
                        AsyncResult.forMessage(pendingRequest.mResult, null, null);
                        pendingRequest.mResult.sendToTarget();
                    }
                }
                /// @}

                DtmfQueueHandler.DtmfQueueRR dqrr = mDtmfReqQueue.buildDtmfQueueRR(rr, params);
                mDtmfReqQueue.setPendingRequest(dqrr);
            } else {
                if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "DTMF queue is 0, send switch Immediately");
                mDtmfReqQueue.setSendChldRequest();

                DtmfQueueHandler.DtmfQueueRR dqrr = mDtmfReqQueue.buildDtmfQueueRR(rr, params);
                sendDtmfQueueRR(dqrr);
            }
        }
    }
    public void sendDtmfQueueRR(DtmfQueueHandler.DtmfQueueRR dqrr) {
        RILRequest rr = dqrr.rr;
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(rr.mResult);
        if (radioProxy == null) {
            mtkRiljLoge("get RadioProxy null. ([" + rr.serialString() + "] request: "
                    + requestToString(rr.mRequest) + ")");
            return;
        }
        if (RILJ_LOGD) {
            mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                    + " (by DtmfQueueRR)");
        }
        try {
            Object[] params = null;
            switch (rr.mRequest) {
            case RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE:
                radioProxy.switchWaitingOrHoldingAndActive(rr.mSerial);
                break;
            case RIL_REQUEST_CONFERENCE:
                radioProxy.conference(rr.mSerial);
                break;
            case RIL_REQUEST_SEPARATE_CONNECTION:
                params = dqrr.params;
                if (params.length != 1) {
                    mtkRiljLoge("request " + requestToString(rr.mRequest) + " params error. ("
                            + Arrays.toString(params) + ")");
                } else {
                    int gsmIndex = (int) params[0];
                    radioProxy.separateConnection(rr.mSerial, gsmIndex);
                }
                break;
            case RIL_REQUEST_EXPLICIT_CALL_TRANSFER:
                radioProxy.explicitCallTransfer(rr.mSerial);
                break;
            case RIL_REQUEST_DTMF_START:
                params = dqrr.params;
                if (params.length != 1) {
                    mtkRiljLoge("request " + requestToString(rr.mRequest) + " params error. ("
                            + params.toString() + ")");
                } else {
                    char c = (char)params[0];
                    radioProxy.startDtmf(rr.mSerial, c + "");
                }
                break;
            case RIL_REQUEST_DTMF_STOP:
                radioProxy.stopDtmf(rr.mSerial);
                break;
            default:
                mtkRiljLoge("get RadioProxy null. ([" + rr.serialString() + "] request: "
                        + requestToString(rr.mRequest) + ")");
            }
        } catch (RemoteException | RuntimeException e) {
            handleRadioProxyExceptionForRR(rr,
                    "DtmfQueueRR(" + requestToString(rr.mRequest) + ")", e);
        }
    }
    /// @}

    /// M: CC: DTMF request special handling @{
    public void handleDtmfQueueNext(int serial) {
        /* DTMF request will be ignored when the count of requests reaches 32 */
        if (RILJ_LOGD) {
            mtkRiljLog("handleDtmfQueueNext (serial = " + serial);
        }
        synchronized (mDtmfReqQueue) {
            DtmfQueueHandler.DtmfQueueRR dqrr = null;
            for (int i = 0; i< mDtmfReqQueue.mDtmfQueue.size(); i++) {
                DtmfQueueHandler.DtmfQueueRR adqrr =
                        (DtmfQueueHandler.DtmfQueueRR) mDtmfReqQueue.mDtmfQueue.get(i);
                if (adqrr != null && adqrr.rr.mSerial == serial) {
                    dqrr = adqrr;
                    break;
                }
            }
            if (dqrr == null) {
                mtkRiljLoge("cannot find serial " + serial + " from mDtmfQueue. (size = "
                        + mDtmfReqQueue.size() + ")");
            } else {
                mDtmfReqQueue.remove(dqrr);
                if (RILJ_LOGD) {
                    mtkRiljLog("remove first item in dtmf queue done. (size = " + mDtmfReqQueue.size()
                            + ")");
                }
            }
            if (mDtmfReqQueue.size() > 0) {
                DtmfQueueHandler.DtmfQueueRR dqrr2 = mDtmfReqQueue.get();
                RILRequest rr2 = dqrr2.rr;
                if (RILJ_LOGD) {
                    mtkRiljLog(rr2.serialString() + "> " + requestToString(rr2.mRequest));
                }
                sendDtmfQueueRR(dqrr2);
            } else {
                if (mDtmfReqQueue.getPendingRequest() != null) {
                    mtkRiljLog("send pending switch request");
                    DtmfQueueHandler.DtmfQueueRR pendingReq = mDtmfReqQueue.getPendingRequest();
                    sendDtmfQueueRR(pendingReq);

                    mDtmfReqQueue.setSendChldRequest();
                    mDtmfReqQueue.setPendingRequest(null);
                }
            }
        }
        /// @}
    }

    @Override
    public void switchWaitingOrHoldingAndActive(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            Object[] params = null;
            handleChldRelatedRequest(rr, params);
        }
    }

    @Override
    public void conference(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CONFERENCE, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            Object[] params = null;
            handleChldRelatedRequest(rr, params);
        }
    }

    @Override
    public void separateConnection(int gsmIndex, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SEPARATE_CONNECTION, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " gsmIndex = " + gsmIndex);
            }
            Object[] params = { gsmIndex };
            handleChldRelatedRequest(rr, params);
        }
    }

    @Override
    public void explicitCallTransfer(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_EXPLICIT_CALL_TRANSFER, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            Object[] params = null;
            handleChldRelatedRequest(rr, params);
        }
    }

    @Override
    public void startDtmf(char c, Message result) {
        /// M: CC: DTMF request special handling @{
        /* DTMF request will be ignored when the count of requests reaches 32 */
        synchronized (mDtmfReqQueue) {
            if (!mDtmfReqQueue.hasSendChldRequest()
                    && mDtmfReqQueue.size() < mDtmfReqQueue.MAXIMUM_DTMF_REQUEST) {
                if (!mDtmfReqQueue.isStart()) {
                    vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
                    if (radioProxy != null) {
                        RILRequest rr = obtainRequest(RIL_REQUEST_DTMF_START, result,
                                mRILDefaultWorkSource);

                        mDtmfReqQueue.start();
                        Object[] param = { c };
                        DtmfQueueHandler.DtmfQueueRR dqrr = mDtmfReqQueue.buildDtmfQueueRR(rr,
                                param);
                        mDtmfReqQueue.add(dqrr);

                        if (mDtmfReqQueue.size() == 1) {
                            mtkRiljLog("send start dtmf");
                            // Do not log function arg for privacy
                            if (RILJ_LOGD)
                                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
                            sendDtmfQueueRR(dqrr);
                        }
                    }
                } else {
                    mtkRiljLog("DTMF status conflict, want to start DTMF when status is "
                            + mDtmfReqQueue.isStart());
                }
            }
        }
        /// @}
    }

    @Override
    public void stopDtmf(Message result) {
        /// M: CC: DTMF request special handling @{
        /* DTMF request will be ignored when the count of requests reaches 32 */
        synchronized (mDtmfReqQueue) {
            if (!mDtmfReqQueue.hasSendChldRequest()
                    && mDtmfReqQueue.size() < mDtmfReqQueue.MAXIMUM_DTMF_REQUEST) {
                if (mDtmfReqQueue.isStart()) {
                    vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
                    if (radioProxy != null) {
                        RILRequest rr = obtainRequest(RIL_REQUEST_DTMF_STOP, result,
                                mRILDefaultWorkSource);
                        mDtmfReqQueue.stop();
                        Object[] param = null;
                        DtmfQueueHandler.DtmfQueueRR dqrr = mDtmfReqQueue.buildDtmfQueueRR(rr,
                                param);
                        mDtmfReqQueue.add(dqrr);
                        if (mDtmfReqQueue.size() == 1) {
                            mtkRiljLog("send stop dtmf");
                            if (RILJ_LOGD)
                                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
                            sendDtmfQueueRR(dqrr);
                        }
                    }
                } else {
                    mtkRiljLog("DTMF status conflict, want to start DTMF when status is "
                            + mDtmfReqQueue.isStart());
                }
            }
        }
        /// @}
    }

    /// M: CC: HangupAll for FTA 31.4.4.2 @{
    public void
    hangupAll(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_HANGUP_ALL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.hangupAll(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "hangupAll", e);
            }
        }
    }
    /// @}

    /// M: CC: Proprietary incoming call handling
    public void setCallIndication(int mode, int callId, int seqNumber, Message result) {

        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_CALL_INDICATION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToStringEx(rr.mRequest)
                + " " + mode + ", " + callId + ", " + seqNumber);
            }

            try {
                radioProxy.setCallIndication(rr.mSerial, mode, callId, seqNumber);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setCallIndication", e);
            }
        }
    }

    public void setCallIndicationWithCause(int mode, int callId, int seqNumber, Message result,
            int cause) {

        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            vendor.mediatek.hardware.radio.V3_10.IRadio radioProxy310 = vendor.mediatek.hardware
                    .radio.V3_10.IRadio.castFrom(radioProxy);

            RILRequest rr = null;
            if (radioProxy310 != null) {
                rr = obtainRequest(RIL_REQUEST_SET_CALL_INDICATION_WITH_CAUSE, result,
                        mRILDefaultWorkSource);
                if (RILJ_LOGD) {
                    mtkRiljLog(rr.serialString() + "> " + requestToStringEx(rr.mRequest)
                    + " " + mode + ", " + callId + ", " + seqNumber + ", " + cause);
                }
            } else {
                rr = obtainRequest(RIL_REQUEST_SET_CALL_INDICATION, result,
                        mRILDefaultWorkSource);
                if (RILJ_LOGD) {
                    mtkRiljLog(rr.serialString() + "> " + requestToStringEx(rr.mRequest)
                    + " " + mode + ", " + callId + ", " + seqNumber);
                }
            }


            try {
                if (radioProxy310 != null) {
                    radioProxy310.setCallIndicationWithCause(rr.mSerial, mode, callId, seqNumber,
                            cause);
                } else {
                    radioProxy.setCallIndication(rr.mSerial, mode, callId, seqNumber);
                }
            } catch (RemoteException | RuntimeException e) {
                if (radioProxy310 != null) {
                    handleRadioProxyExceptionForRR(rr, "setCallIndicationWithCause", e);
                } else {
                    handleRadioProxyExceptionForRR(rr, "setCallIndication", e);
                }
            }
        }
    }

    /**
     * Hang up one individual connection with cause to modem.
     * This is an extension to hangupConnection(int gsmIndex, Message result)
     * function, and use to carry cause to RIL for replying to modem.
     */
    public void hangupConnectionWithCause (int gsmIndex, int cause, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            vendor.mediatek.hardware.radio.V3_10.IRadio radioProxy310 = vendor.mediatek.hardware
                    .radio.V3_10.IRadio.castFrom(radioProxy);

            RILRequest rr = null;
            if (radioProxy310 != null) {
                rr = obtainRequest(RIL_REQUEST_HANGUP_WITH_CAUSE, result,
                        mRILDefaultWorkSource);
                if (RILJ_LOGD) {
                    riljLog(rr.serialString() + "> " + requestToStringEx(rr.mRequest)
                            + " gsmIndex = " + gsmIndex + " , cause = " + cause);
                }
            } else {
                rr = obtainRequest(RIL_REQUEST_HANGUP, result,
                        mRILDefaultWorkSource);
                if (RILJ_LOGD) {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " gsmIndex = "
                            + gsmIndex);
                }
            }


            try {
                if (radioProxy310 != null) {
                    radioProxy310.hangupWithCause(rr.mSerial, gsmIndex, cause);
                } else {
                    radioProxy.hangup(rr.mSerial, gsmIndex);
                }
            } catch (RemoteException | RuntimeException e) {
                if (radioProxy310 != null) {
                    handleRadioProxyExceptionForRR(rr, "hangupConnectionWithCause", e);
                } else {
                    handleRadioProxyExceptionForRR(rr, "hangupConnection", e);
                }
            }
        }
    }

    /// M: CC: Proprietary ECC enhancement @{
    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     *
     * CLIR_DEFAULT     == on "use subscription default value"
     * CLIR_SUPPRESSION == on "CLIR suppression" (allow CLI presentation)
     * CLIR_INVOCATION  == on "CLIR invocation" (restrict CLI presentation)
     */
    public void emergencyDial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_EMERGENCY_DIAL, result,
                    mRILDefaultWorkSource);

            Dial dialInfo = new Dial();
            dialInfo.address = convertNullToEmptyString(address);
            dialInfo.clir = clirMode;
            if (uusInfo != null) {
                UusInfo info = new UusInfo();
                info.uusType = uusInfo.getType();
                info.uusDcs = uusInfo.getDcs();
                info.uusData = new String(uusInfo.getUserData());
                dialInfo.uusInfo.add(info);
            }

            if (RILJ_LOGD) {
                // Do not log function arg for privacy
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.emergencyDial(rr.mSerial, dialInfo);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "emergencyDial", e);
            }
        }
    }

    public void setEccServiceCategory(int serviceCategory, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_ECC_SERVICE_CATEGORY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " serviceCategory=" + serviceCategory);
            }

            try {
                radioProxy.setEccServiceCategory(rr.mSerial, serviceCategory);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setEccServiceCategory", e);
            }
        }
    }

    /// M: CC: Vzw/CTVolte ECC @{
    /**
     * Let modem know start of E911 and deliver some information.
     *
     * @param airplaneMode
     *          0 : off
     *          1 : on
     *
     * @param imsReg
     *          0 : ims deregistered
     *          1 : ims registered
     *
     * @hide
     */
    public void setCurrentStatus(int airplaneMode, int imsReg, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CURRENT_STATUS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " airplaneMode=" + airplaneMode
                        + " imsReg=" + imsReg);
            }

            try {
                radioProxy.currentStatus(rr.mSerial, airplaneMode, imsReg);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setCurrentStatus", e);
            }
        }
    }
    /// @}

    /**
     * Set ECC preferred Rat
     *
     */
    public void setEccPreferredRat(int phoneType, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ECC_PREFERRED_RAT, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " phoneType=" + phoneType);
            }

            try {
                radioProxy.eccPreferredRat(rr.mSerial, phoneType);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setEccPreferredRat", e);
            }
        }
    }

    /**
     * Set emergency number list to modem.
     *
     */
    public void setEccList() {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(null);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_ECC_LIST, null,
                    mRILDefaultWorkSource);

            ArrayList<String> eccList = MtkPhoneNumberUtils.getEccList();
            String[] eccListString = {"", ""};
            int i = 0;
            for (String list : eccList) {
                if (i >= 2) {
                    // MD support Max 15 customized ECC so we will at most
                    // sync 2 ecc lists to MD (10 entries at once)
                    break;
                }
                eccListString[i++] = list;
            }

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " ecc1: " + eccListString[0] + ", ecc2: " + eccListString[1]);
            }

            try {
                radioProxy.setEccList(rr.mSerial, eccListString[0], eccListString[1]);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setEccList", e);
            }
        }
    }
    /// @}

    /// M: CC: GSA HD Voice for 2/3G network support @{
    /**
     * Sets the handler for notifying Speech Codec Type for recognizing HD voice capability.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void setOnSpeechCodecInfo(Handler h, int what, Object obj) {
        mSpeechCodecInfoRegistrant = new Registrant(h, what, obj);
    }

    /**
     * Unsets the handler for notifying Speech Codec Type for recognizing HD voice capability.
     *
     * @param h Handler for notification message.
     */
    public void unSetOnSpeechCodecInfo(Handler h) {
        if (mSpeechCodecInfoRegistrant != null && mSpeechCodecInfoRegistrant.getHandler() == h) {
            mSpeechCodecInfoRegistrant.clear();
            mSpeechCodecInfoRegistrant = null;
        }
    }
    /// @}

    /**
     * Set voice prefer status.
     *
     * @param status Voice prefer status
     */
    public void setVoicePreferStatus(int status) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(null);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_VOICE_PREFER_STATUS, null,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " status: " + status);
            }

            try {
                radioProxy.setVoicePreferStatus(rr.mSerial, status);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setVoicePreferStatus", e);
            }
        }
    }

    /**
     * Set ECC numbers.
     *
     * @param eccListWithCard ECC numbers when card inserted
     * @param eccListNoCard ECC numbers when card not inserted
     */
    public void setEccNum(String eccListWithCard, String eccListNoCard) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(null);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_ECC_NUM, null,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " eccListWithCard: " + eccListWithCard
                        + ", eccListNoCard: " + eccListNoCard);
            }

            try {
                radioProxy.setEccNum(rr.mSerial, eccListWithCard, eccListNoCard);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setEccNum", e);
            }
        }
    }

    /**
     * Get ECC numbers.
     */
    public void getEccNum() {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(null);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_ECC_NUM, null,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.getEccNum(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getEccNum", e);
            }
        }
    }

    // APC
    protected RegistrantList mPseudoCellInfoRegistrants = new RegistrantList();
    public void registerForPseudoCellInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mPseudoCellInfoRegistrants.add(r);
    }

    public void unregisterForPseudoCellInfo(Handler h) {
        mPseudoCellInfoRegistrants.remove(h);
    }

    public void setApcMode(int apcMode, boolean reportOn, int reportInterval,
                                            Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_PSEUDO_CELL_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                    + " " + apcMode + ", " + reportOn + ", " + reportInterval);
            }

            try {
                int reportMode = (reportOn == true ? 1 : 0);
                radioProxy.setApcMode(rr.mSerial, apcMode, reportMode, reportInterval);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setApcMode", e);
            }
        }
    }

    public void getApcInfo(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_PSEUDO_CELL_INFO, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.getApcInfo(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getApcInfo", e);
            }
        }
    }

    /**
     * If want to call ECC number by CDMA/GSM network when there is no card in phone,
     * should guarantee that: the phone which user wanted to use is CDMA/GSM Phone.
     * If the phone type is not correct, should change the phone type throngh using this
     * mechanism to switch mode to CDMA/CSFB.
     *
     * @param mode 4: CARD_TYPE_CSIM; 1: CARD_TYPE_SIM
     * @param result messge object.
     */
    public void triggerModeSwitchByEcc(int mode, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SWITCH_MODE_FOR_ECC, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.triggerModeSwitchByEcc(rr.mSerial, mode);
                Message msg = mRilHandler.obtainMessage(EVENT_BLOCKING_RESPONSE_TIMEOUT);
                msg.obj = null;
                msg.arg1 = rr.mSerial;
                mRilHandler.sendMessageDelayed(msg, DEFAULT_BLOCKING_MESSAGE_RESPONSE_TIMEOUT_MS);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rr, "triggerModeSwitchByEcc", e);
            }
        }
   }

    /**
     * Get the RUIM SMS memory Status.
     *
     * @param result the response message
     */
    public void getSmsRuimMemoryStatus(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_SMS_RUIM_MEM_STATUS, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }
            try {
                radioProxy.getSmsRuimMemoryStatus(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getSmsRuimMemoryStatus", e);
            }
        }
    }

    // FastDormancy
    public void setFdMode(int mode, int para1, int para2, Message response) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(response);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_FD_MODE, response,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.setFdMode(rr.mSerial, mode, para1, para2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setFdMode", e);
            }
        }
    }

    protected ArrayList<HardwareConfig> convertHalHwConfigList(
            ArrayList<android.hardware.radio.V1_0.HardwareConfig> hwListRil,
            RIL ril) {
        int num;
        ArrayList<HardwareConfig> response;
        HardwareConfig hw;

        num = hwListRil.size();
        response = new ArrayList<HardwareConfig>(num);

        if (RILJ_LOGV) {
            mtkRiljLog("convertHalHwConfigList: num=" + num);
        }
        for (android.hardware.radio.V1_0.HardwareConfig hwRil : hwListRil) {
            int type = hwRil.type;
            switch(type) {
                case HardwareConfig.DEV_HARDWARE_TYPE_MODEM: {
                    hw = new MtkHardwareConfig(type);
                    HardwareConfigModem hwModem = hwRil.modem.get(0);
                    hw.assignModem(hwRil.uuid, hwRil.state, hwModem.rilModel, hwModem.rat,
                            hwModem.maxVoice, hwModem.maxData, hwModem.maxStandby);
                    break;
                }
                case HardwareConfig.DEV_HARDWARE_TYPE_SIM: {
                    hw = new MtkHardwareConfig(type);
                    hw.assignSim(hwRil.uuid, hwRil.state, hwRil.sim.get(0).modemUuid);
                    break;
                }
                default: {
                    throw new RuntimeException(
                            "RIL_REQUEST_GET_HARDWARE_CONFIG invalid hardward type:" + type);
                }
            }
            response.add(hw);
        }

        return response;
    }


    /**
     * Register for plmn.
     * When AP receive plmn Urc to decide to target is in home or roaming.
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void setOnPlmnChangeNotification(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
         synchronized (mWPMonitor) {
            mPlmnChangeNotificationRegistrant.add(r);

            if (mEcopsReturnValue != null) {
               // Only notify the new registrant
               r.notifyRegistrant(new AsyncResult(null, mEcopsReturnValue, null));
               mEcopsReturnValue = null;
            }
        }
    }


    public void unSetOnPlmnChangeNotification(Handler h) {
        synchronized (mWPMonitor) {
            mPlmnChangeNotificationRegistrant.remove(h);
        }
    }

    /**
     * Register for EMSR.
     * When AP receive EMSR Urc to decide to resume camping network.
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void setOnRegistrationSuspended(Handler h, int what, Object obj) {
        synchronized (mWPMonitor) {
            mRegistrationSuspendedRegistrant = new Registrant(h, what, obj);

            if (mEmsrReturnValue != null) {
                // Only notify the new registrant
                mRegistrationSuspendedRegistrant.notifyRegistrant(
                    new AsyncResult(null, mEmsrReturnValue, null));
                mEmsrReturnValue = null;
            }
        }
    }

    public void unSetOnRegistrationSuspended(Handler h) {
        synchronized (mWPMonitor) {
            mRegistrationSuspendedRegistrant.clear();
        }
    }

    /**
     * Register for GMSS RAT.
     * When boot the phone,AP can use this informaiton decide PS' type(LTE or C2K).
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForGmssRatChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mGmssRatChangedRegistrant.add(r);
    }

    /**
     * Request GSM modem to resume network registration.
     * @param sessionId the session index.
     * @param result the responding message.
     */
    public void setResumeRegistration(int sessionId, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_RESUME_REGISTRATION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " sessionId = " + sessionId);
            }

            try {
                radioProxy.setResumeRegistration(rr.mSerial, sessionId);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setResumeRegistration", e);
            }
        }
    }

    /**
     * Request to set IMS handover property in RILD.
     * @param result the responding message.
     */
    public void setPropImsHandover(String value, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SETPROP_IMS_HANDOVER, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " value = " + value);
            }

            try {
                radioProxy.setPropImsHandover(rr.mSerial, value);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setPropImsHandover", e);
            }
        }
    }

    /**
     * Request GSM modem to store new modem type.
     * @param modemType worldmodeid.
     * @param result the responding message.
     */
    public void storeModemType(int modemType, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_STORE_MODEM_TYPE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " modemType = " + modemType);
            }

            try {
                radioProxy.storeModemType(rr.mSerial, modemType);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "storeModemType", e);
            }
        }
    }

    /**
     * Request GSM modem to reload new modem type.
     * @param modemType worldmodeid.
     * @param result the responding message.
     */
    public void reloadModemType(int modemType, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_RELOAD_MODEM_TYPE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " modemType = " + modemType);
            }

            try {
                radioProxy.reloadModemType(rr.mSerial, modemType);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "reloadModemType", e);
            }
        }
    }

    public void handleStkCallSetupRequestFromSimWithResCode(boolean accept, int resCode,
            Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(
                    RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM_WITH_RESULT_CODE,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
               mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }
            int[] param = new int[1];
            if (resCode == 0x21 || resCode == 0x20) {
                param[0] = resCode;
            } else {
                param[0] = accept ? 1 : 0;
            }

            try {
               radioProxy.handleStkCallSetupRequestFromSimWithResCode(rr.mSerial, param[0]);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr,
                        "handleStkCallSetupRequestFromSimWithResCode", e);
            }
        }
    }

    public void setPdnReuse(String pdnReuse, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_PDN_REUSE,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
               mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
               radioProxy.setPdnReuse(rr.mSerial, pdnReuse);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setPdnReuse", e);
            }
        }
    }

    public void setOverrideApn(String overrideApn, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_OVERRIDE_APN,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
               mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
               radioProxy.setOverrideApn(rr.mSerial, overrideApn);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setOverrideApn", e);
            }
        }
    }

    public void setPdnNameReuse(String apnName, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_PDN_NAME_REUSE,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
               mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
               radioProxy.setPdnNameReuse(rr.mSerial, apnName);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setPdnNameReuse", e);
            }
        }
    }


    // MTK-START: SIM COMMON SLOT
    protected RegistrantList mSimTrayPlugIn = new RegistrantList();
    protected RegistrantList mSimCommonSlotNoChanged = new RegistrantList();

    public void registerForSimTrayPlugIn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mSimTrayPlugIn.add(r);
    }

    public void unregisterForSimTrayPlugIn(Handler h) {
        mSimTrayPlugIn.remove(h);
    }

    public void registerForCommonSlotNoChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mSimCommonSlotNoChanged.add(r);
    }

    public void unregisterForCommonSlotNoChanged(Handler h) {
        mSimCommonSlotNoChanged.remove(h);
    }
    // MTK-END

    @Override
    public void resetRadio(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_RESET_RADIO, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.resetRadio(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "resetRadio", e);
            }
        }
    }

    public void restartRILD(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_RESTART_RILD, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.restartRILD(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "restartRILD", e);
            }
        }
    }

    // / M: BIP {
    protected RegistrantList mBipProCmdRegistrant = new RegistrantList();

    public void setOnBipProactiveCmd(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mBipProCmdRegistrant.add(r);
    }

    public void unSetOnBipProactiveCmd(Handler h) {
        mBipProCmdRegistrant.remove(h);
    }

    // / M: BIP }

    // / M: STK {
    protected RegistrantList mStkSetupMenuResetRegistrant = new RegistrantList();

    public void setOnStkSetupMenuReset(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mStkSetupMenuResetRegistrant.add(r);
    }

    public void unSetOnStkSetupMenuReset(Handler h) {
        mStkSetupMenuResetRegistrant.remove(h);
    }
    // / M: STK }

    // MTK-START: SIM ME LOCK
    public void queryNetworkLock(int category, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_QUERY_SIM_NETWORK_LOCK,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.queryNetworkLock(rr.mSerial, category);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "queryNetworkLock", e);
            }
        }
    }

    public void setNetworkLock(int category, int lockop, String password,
                        String data_imsi, String gid1, String gid2, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_SET_SIM_NETWORK_LOCK, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            password = (password == null) ? "" : password;
            data_imsi = (data_imsi == null) ? "" : data_imsi;
            gid1 = (gid1 == null) ? "" : gid1;
            gid2 = (gid2 == null) ? "" : gid2;
            try {
                radioProxy.setNetworkLock(rr.mSerial, category, lockop, password, data_imsi,
                        gid1, gid2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setNetworkLock", e);
            }
        }
    }

    /**
     * Supply to unlock SIM ME LOCK with fixed type.
     * @param netpin SIM ME LOCK password.
     * @param type Lock category.
     * @param result messge object.
     */
    public void supplyDepersonalization(String netpin, int type, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_ENTER_DEPERSONALIZATION,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " netpin = "
                        + netpin + " type = " + type);
            }

            try {
                radioProxy.supplyDepersonalization(rr.mSerial, convertNullToEmptyString(netpin),
                        type);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "supplyNetworkDepersonalization", e);
            }
        }
    }
    // MTK-END

    public void registerForResetAttachApn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mResetAttachApnRegistrants.add(r);
    }

    public void unregisterForResetAttachApn(Handler h) {
        mResetAttachApnRegistrants.remove(h);
    }

    public void registerForAttachApnChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);

        mAttachApnChangedRegistrants.add(r);
    }

    public void unregisterForAttachApnChanged(Handler h) {
        mAttachApnChangedRegistrants.remove(h);
    }

    // M: [LTE][Low Power][UL traffic shaping] @{
    protected RegistrantList mLteAccessStratumStateRegistrants = new RegistrantList();

    public void registerForLteAccessStratumState(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mLteAccessStratumStateRegistrants.add(r);
    }

    public void unregisterForLteAccessStratumState(Handler h) {
        mLteAccessStratumStateRegistrants.remove(h);
    }

    public void setLteAccessStratumReport(boolean enable, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_LTE_ACCESS_STRATUM_REPORT, result,
                    mRILDefaultWorkSource);
            int type = enable ? 1 : 0;

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + type);
            }

            try {

                radioProxy.setLteAccessStratumReport(rr.mSerial, type);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setLteAccessStratumReport", e);
            }
        }
    }

    public void setLteUplinkDataTransfer(int state, int interfaceId, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_LTE_UPLINK_DATA_TRANSFER, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " state = " + state
                        + ", interfaceId = " + interfaceId);
            }

            try {
                radioProxy.setLteUplinkDataTransfer(rr.mSerial, state, interfaceId);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setLteUplinkDataTransfer", e);
            }
        }
    }
    // M: [LTE][Low Power][UL traffic shaping] @}

    // MTK-START: SIM HOT SWAP / SIM RECOVERY
    protected RegistrantList mSimPlugIn = new RegistrantList();
    protected RegistrantList mSimPlugOut = new RegistrantList();
    protected RegistrantList mSimMissing = new RegistrantList();
    protected RegistrantList mSimRecovery = new RegistrantList();

    public void registerForSimPlugIn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mSimPlugIn.add(r);
    }

    public void unregisterForSimPlugIn(Handler h) {
        mSimPlugIn.remove(h);
    }

    public void registerForSimPlugOut(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mSimPlugOut.add(r);
    }

    public void unregisterForSimPlugOut(Handler h) {
        mSimPlugOut.remove(h);
    }

    public void registerForSimMissing(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mSimMissing.add(r);
    }

    public void unregisterForSimMissing(Handler h) {
        mSimMissing.remove(h);
    }

    public void registerForSimRecovery(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mSimRecovery.add(r);
    }

    public void unregisterForSimRecovery(Handler h) {
        mSimRecovery.remove(h);
    }
    // MTK-END
    // MTK-START: SIM ME LOCK
    protected RegistrantList mSmlSlotLockInfoChanged = new RegistrantList();
    Object mSmlSlotLockInfo = null;

    public void registerForSmlSlotLockInfoChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mSmlSlotLockInfoChanged.add(r);
        if (mSmlSlotLockInfo != null) {
            r.notifyRegistrant(new AsyncResult(null, mSmlSlotLockInfo, null));
        }
    }

    public void unregisterForSmlSlotLockInfoChanged(Handler h) {
        mSmlSlotLockInfoChanged.remove(h);
    }

    public void supplyDeviceNetworkDepersonalization(String pwd, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(
                    MtkRILConstants.RIL_REQUEST_ENTER_DEVICE_NETWORK_DEPERSONALIZATION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }
            try {
                radioProxy.supplyDeviceNetworkDepersonalization(rr.mSerial,
                        convertNullToEmptyString(pwd));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "supplyDeviceNetworkDepersonalization", e);
            }
        }
    }
    // MTK-END
    // PHB Start
    /**
     * Sets the handler for PHB ready notification.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForPhbReady(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        Rlog.d(RILJ_LOG_TAG, "call registerForPhbReady Handler : " + h);
        mPhbReadyRegistrants.add(r);
    }

    /**
     * Unregister the handler for PHB ready notification.
     *
     * @param h Handler for notification message.
     */
    public void unregisterForPhbReady(Handler h) {
        mPhbReadyRegistrants.remove(h);
    }

    /**
     * Request the information of the given storage type
     *
     * @param type
     *          the type of the storage, refer to PHB_XDN defined in the RilConstants
     * @param result
     *          Callback message
     *          response.obj.result is an int[4]
     *          response.obj.result[0] is number of current used entries
     *          response.obj.result[1] is number of total entries in the storage
     *          response.obj.result[2] is maximum supported length of the number
     *          response.obj.result[3] is maximum supported length of the alphaId
     */
    public void queryPhbStorageInfo(int type, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_PHB_STORAGE_INFO, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": "
                + type);

            try {
                radioProxy.queryPhbStorageInfo(rr.mSerial, type);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "queryPhbStorageInfo", e);
            }
        }
    }

    /**
     * Convert to PhbEntryStructure defined in types.hal.
     * @param pe PHB entry
     * @return A converted PHB entry for hal
     */
    private PhbEntryStructure convertToHalPhbEntryStructure(PhbEntry pe) {
        PhbEntryStructure pes = new PhbEntryStructure();

        pes.type = pe.type;
        pes.index = pe.index;
        pes.number = convertNullToEmptyString(pe.number);
        pes.ton = pe.ton;
        pes.alphaId = convertNullToEmptyString(pe.alphaId);

        return pes;
    }

    /**
     * Request update a PHB entry using the given. {@link PhbEntry}
     *
     * @param entry a PHB entry strucutre {@link PhbEntry}
     *          when one of the following occurs, it means delete the entry.
     *          1. entry.number is NULL
     *          2. entry.number is empty and entry.ton = 0x91
     *          3. entry.alphaId is NULL
     *          4. both entry.number and entry.alphaId are empty.
     * @param result
     *          Callback message containing if the action is success or not.
     */
    public void writePhbEntry(PhbEntry entry, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_WRITE_PHB_ENTRY, result,
                    mRILDefaultWorkSource);

            // Convert to HAL PhbEntry Structure
            PhbEntryStructure pes = convertToHalPhbEntryStructure(entry);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": "
                + entry);

            try {
                radioProxy.writePhbEntry(rr.mSerial, pes);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "writePhbEntry", e);
            }
        }
    }

    /**
     * Request read PHB entries from the given storage.
     * @param type
     *          the type of the storage, refer to PHB_* defined in the RilConstants
     * @param bIndex
     *          the begin index of the entries to be read
     * @param eIndex
     *          the end index of the entries to be read, note that the (eIndex - bIndex +1)
     *          should not exceed the value RilConstants.PHB_MAX_ENTRY
     *
     * @param result
     *          Callback message containing an array of {@link PhbEntry} structure.
     */
    public void readPhbEntry(int type, int bIndex, int eIndex, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_READ_PHB_ENTRY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": "
                + type + " begin: " + bIndex + " end: " + eIndex);

            try {
                radioProxy.readPhbEntry(rr.mSerial, type, bIndex, eIndex);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "readPhbEntry", e);
            }
        }
    }

    /**
     * Query capability of USIM PHB.
     *
     * @param result callback message
     * ((AsyncResult)response.obj).result is
     *  <N_ANR>,<N_EMAIL>,<N_SNE>,<N_AAS>,<L_AAS>,<N_GAS>,<L_GAS>,<N_GRP>
     */
    public void queryUPBCapability(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_UPB_CAPABILITY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.queryUPBCapability(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "queryUPBCapability", e);
            }
        }
    }


    /**
     * Update a USIM PHB field's entry.
     * This is a new API mainly for update EF_ANR.
     *
     * @param entryType must be 0(ANR), 1(EMAIL), 2(SNE), 3(AAS), or 4(GAS)
     * @param adnIndex ADN index
     * @param entryIndex the i-th EF_(EMAIL/ANR/SNE)
     * @param strVal is the value string to be updated
     * @param tonForNum TON for ANR
     * @param aasAnrIndex AAS index of the ANR
     * @param result callback message
     */
    public void editUPBEntry(int entryType, int adnIndex, int entryIndex,
            String strVal, String tonForNum, String aasAnrIndex, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_EDIT_UPB_ENTRY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            ArrayList<String> arrList = new ArrayList<>();
            arrList.add(Integer.toString(entryType));
            arrList.add(Integer.toString(adnIndex));
            arrList.add(Integer.toString(entryIndex));
            arrList.add(strVal);
            if (entryType == 0) {
                arrList.add(tonForNum);
                arrList.add(aasAnrIndex);
            }

            try {
                radioProxy.editUPBEntry(rr.mSerial, arrList);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "editUPBEntry", e);
            }
        }
    }

    /**
     * Update a USIM PHB field's entry.
     *
     * @param entryType must be 0(ANR), 1(EMAIL), 2(SNE), 3(AAS), or 4(GAS)
     * @param adnIndex ADN index
     * @param entryIndex the i-th EF_(EMAIL/ANR/SNE)
     * @param strVal is the value string to be updated
     * @param tonForNum TON for ANR
     * @param result callback message
     */
    public void editUPBEntry(int entryType, int adnIndex, int entryIndex,
            String strVal, String tonForNum, Message result) {
        editUPBEntry(entryType, adnIndex, entryIndex, strVal, tonForNum, null, result);
    }

    /**
     * Delete a USIM PHB field's entry.
     *
     * @param entryType must be 0(ANR), 1(EMAIL), 2(SNE), 3(AAS), or 4(GAS)
     * @param adnIndex ADN index
     * @param entryIndex the i-th EF_(EMAIL/ANR/SNE)
     * @param result callback message
     */
    public void deleteUPBEntry(int entryType, int adnIndex, int entryIndex, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DELETE_UPB_ENTRY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": "
                + entryType + " adnIndex: " + adnIndex + " entryIndex: " + entryIndex);

            try {
                radioProxy.deleteUPBEntry(rr.mSerial, entryType, adnIndex, entryIndex);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "deleteUPBEntry", e);
            }
        }
    }

    /**
     * Read GAS entry by giving range.
     *
     * @param startIndex GAS index start to read
     * @param endIndex GAS index end to read
     * @param result callback message
     * ((AsyncResult)response.obj).result is a GAS string list
     */
    public void readUPBGasList(int startIndex, int endIndex, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_READ_UPB_GAS_LIST, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": "
                + " startIndex: " + startIndex + " endIndex: " + endIndex);

            try {
                radioProxy.readUPBGasList(rr.mSerial, startIndex, endIndex);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "readUPBGasList", e);
            }
        }
    }

    /**
     * Read a GRP entry by ADN index.
     *
     * @param adnIndex ADN index
     * @param result callback message
     * ((AsyncResult)response.obj).result is a Group id list of the ADN
     */
    public void readUPBGrpEntry(int adnIndex, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_READ_UPB_GRP, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": "
                + " adnIndex: " + adnIndex);

            try {
                radioProxy.readUPBGrpEntry(rr.mSerial, adnIndex);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "readUPBGrpEntry", e);
            }
        }
    }

    /**
     * Update a GRP entry by ADN index.
     *
     * @param adnIndex ADN index
     * @param grpIds Group id list to be updated
     * @param result callback message
     */
    public void writeUPBGrpEntry(int adnIndex, int[] grpIds, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_WRITE_UPB_GRP, result,
                    mRILDefaultWorkSource);
            int nLen = grpIds.length;
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": "
                + " adnIndex: " + adnIndex + " nLen: " + nLen);

            ArrayList<Integer> intList = new ArrayList<Integer>(grpIds.length);
            for (int i = 0; i < grpIds.length; i++) {
                intList.add(grpIds[i]);
            }
            try {
                radioProxy.writeUPBGrpEntry(rr.mSerial, adnIndex, intList);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "writeUPBGrpEntry", e);
            }
        }
    }

    /**
     * at+cpbr=?
     * @return  <nlength><tlength><glength><slength><elength>
     */
    public void getPhoneBookStringsLength(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_PHB_STRING_LENGTH, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> :::" + requestToString(rr.mRequest));

            try {
                radioProxy.getPhoneBookStringsLength(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getPhoneBookStringsLength", e);
            }
        }
    }

    /**
     * at+cpbs?
     * @return  PBMemStorage :: +cpbs:<storage>,<used>,<total>
     */
    public void getPhoneBookMemStorage(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_PHB_MEM_STORAGE, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> :::" + requestToString(rr.mRequest));

            try {
                radioProxy.getPhoneBookMemStorage(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getPhoneBookMemStorage", e);
            }
        }
    }

    /**
     * at+epin2=<p2>; at+cpbs=<storage>
     * @return
     */
    public void setPhoneBookMemStorage(String storage, String password, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_PHB_MEM_STORAGE, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> :::" + requestToString(rr.mRequest));

            try {
                radioProxy.setPhoneBookMemStorage(rr.mSerial,
                        convertNullToEmptyString(storage),
                        convertNullToEmptyString(password));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "writeUPBGrpEntry", e);
            }
        }
    }

    /**
     * M at+cpbr=<index1>,<index2> +CPBR:<indexn>,<number>,<type>,<text>,
     * <hidden>,<group>,<adnumber>,<adtype>,<secondtext>,<email>
     */
    public void readPhoneBookEntryExt(int index1, int index2, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_READ_PHB_ENTRY_EXT, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> :::" + requestToString(rr.mRequest));

            try {
                radioProxy.readPhoneBookEntryExt(rr.mSerial, index1, index2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "readPhoneBookEntryExt", e);
            }
        }
    }

    /**
     * Convert to PhbEntryExt defined in types.hal.
     * @param pbe PHB entry ext
     * @return A converted PHB entry ext for hal
     */
    private static PhbEntryExt convertToHalPhbEntryExt(PBEntry pbe) {
        PhbEntryExt pee = new PhbEntryExt();

        pee.index = pbe.getIndex1();
        pee.number = pbe.getNumber();
        pee.type = pbe.getType();
        pee.text = pbe.getText();
        pee.hidden = pbe.getHidden();
        pee.group = pbe.getGroup();
        pee.adnumber = pbe.getAdnumber();
        pee.adtype = pbe.getAdtype();
        pee.secondtext = pbe.getSecondtext();
        pee.email = pbe.getEmail();

        return pee;
    }

    /**
     * M AT+CPBW=<index>,<number>,<type>,<text>,<hidden>,<group>,<adnumber>,
     * <adtype>,<secondtext>,<email>
     */
    public void writePhoneBookEntryExt(PBEntry entry, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_WRITE_PHB_ENTRY_EXT, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> :::" + requestToString(rr.mRequest));

            PhbEntryExt pee = convertToHalPhbEntryExt(entry);

            try {
                radioProxy.writePhoneBookEntryExt(rr.mSerial, pee);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "writePhoneBookEntryExt", e);
            }
        }
    }

    /**
     * Query info of the EF_EMAIL/EF_ANR/EF_Sne.
     *
     * @param eftype 0:EF_ANR, 1:EF_EMAIL, 2: EF_SNE
     * @param fileIndex the i-th EF_EMAIL/EF_ANR/EF_SNE (1-based)
     * @param result callback message
     * ((AsyncResult)response.obj).result[0] is <M_NUM>, Max number of entries
     * ((AsyncResult)response.obj).result[1] is <A_NUM>, Available number of entries
     * ((AsyncResult)response.obj).result[2] is <L_XXX>, Max support length
     */
    public void queryUPBAvailable(int eftype, int fileIndex, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_UPB_AVAILABLE, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
            + " eftype: " + eftype + " fileIndex: " + fileIndex);

            try {
                radioProxy.queryUPBAvailable(rr.mSerial, eftype, fileIndex);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "queryUPBAvailable", e);
            }
        }
    }

    /**
     * Read a Email entry by ADN index.
     *
     * @param adnIndex ADN index
     * @param fileIndex the i-th EF_EMAIL (1-based)
     * @param result callback message
     * ((AsyncResult)response.obj).result is a Email string
     */
    public void readUPBEmailEntry(int adnIndex, int fileIndex, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_READ_EMAIL_ENTRY, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
            + " adnIndex: " + adnIndex + " fileIndex: " + fileIndex);

            try {
                radioProxy.readUPBEmailEntry(rr.mSerial, adnIndex, fileIndex);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "readUPBEmailEntry", e);
            }
        }
    }

    /**
     * Read a SNE entry by ADN index.
     *
     * @param adnIndex ADN index
     * @param fileIndex the i-th EF_SNE (1-based)
     * @param result callback message
     * ((AsyncResult)response.obj).result is a SNE string (need to be decoded)
     */
    public void readUPBSneEntry(int adnIndex, int fileIndex, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_READ_SNE_ENTRY, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
            + " adnIndex: " + adnIndex + " fileIndex: " + fileIndex);

            try {
                radioProxy.readUPBSneEntry(rr.mSerial, adnIndex, fileIndex);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "readUPBSneEntry", e);
            }
        }
    }

    /**
     * Read a ANR entry by ADN index.
     *
     * @param adnIndex ADN index
     * @param fileIndex the i-th EF_ANR (1-based)
     * @param result callback message
     * ((AsyncResult)response.obj).result is a ANR contains in PhbEntry
     */
    public void readUPBAnrEntry(int adnIndex, int fileIndex, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_READ_ANR_ENTRY, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
            + " adnIndex: " + adnIndex + " fileIndex: " + fileIndex);

            try {
                radioProxy.readUPBAnrEntry(rr.mSerial, adnIndex, fileIndex);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "readUPBAnrEntry", e);
            }
        }
    }

    /**
     * Read AAS entry by giving range.
     *
     * @param startIndex AAS index start to read
     * @param endIndex AAS index end to read
     * @param result callback message
     * ((AsyncResult)response.obj).result is a AAS string list
     */
    public void readUPBAasList(int startIndex, int endIndex, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_READ_UPB_AAS_LIST, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
            + " startIndex: " + startIndex + " endIndex: " + endIndex);

            try {
                radioProxy.readUPBAasList(rr.mSerial, startIndex, endIndex);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "readUPBAasList", e);
            }
        }
    }

    /**
     * set SIM phonebook ready state.
     *
     * @param ready SIM phonebook ready state
     */
    public void setPhonebookReady(int ready, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_PHONEBOOK_READY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " ready = " + ready);
            }

            try {
                radioProxy.setPhonebookReady(rr.mSerial, ready);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setPhonebookReady", e);
            }
        }
    }

    // PHB End

    // MTK_TC1_FEATURE for Antenna Testing start
    public void setRxTestConfig (int AntType, Message result) {
        // for op only
        IMtkRilOp rilOp = getRilOp();
        if (rilOp != null) {
            rilOp.setRxTestConfig(AntType, result);
            return;
        }
    }

    public void getRxTestResult(Message result) {
        // for op only
        IMtkRilOp rilOp = getRilOp();
        if (rilOp != null) {
            rilOp.getRxTestResult(result);
            return;
        }
    }

    public void getPOLCapability(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_POL_CAPABILITY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.getPOLCapability(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getPOLCapability", e);
            }
        }
    }

    public void getCurrentPOLList(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_POL_LIST, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.getCurrentPOLList(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getCurrentPOLList", e);
            }
        }
    }

    public void setPOLEntry(int index, String numeric, int nAct, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_POL_ENTRY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.setPOLEntry(rr.mSerial, index, numeric, nAct);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setPOLEntry", e);
            }
        }
    }

    // M: [VzW] Data Framework @{
    /**
     * Register for the pco status after data attached.
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForPcoDataAfterAttached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mPcoDataAfterAttachedRegistrants.add(r);
    }

    /**
     * Unregister for the pco status after data attached.
     * @param h Handler for notification message.
     */
    public void unregisterForPcoDataAfterAttached(Handler h) {
        mPcoDataAfterAttachedRegistrants.remove(h);
    }
    // M: [VzW] Data Framework @}

    // M: Data Framework - common part enhancement @{
    /**
     * Sync data related settings to MD
     *
     * @param dataSetting[] data related setting
     *              dataSetting[0]: data setting on/off
     *              dataSetting[1]: data roaming setting on/off
     *              dataSetting[2]: default data sim
     * @param result for result
     */
    public void syncDataSettingsToMd(int[] dataSetting, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            // AT+ECNCFG=<mobile_data>,<data_roaming>,[<volte>,<ims_test_mode>]
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_SYNC_DATA_SETTINGS_TO_MD,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGV) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                    + ", " + dataSetting[0] + ", " + dataSetting[1] + ", " + dataSetting[2]);
            }

            ArrayList<Integer> settingList = new ArrayList<Integer>(dataSetting.length);
            for (int i = 0; i < dataSetting.length; i++) {
                settingList.add(dataSetting[i]);
            }

            try {
                radioProxy.syncDataSettingsToMd(rr.mSerial, settingList);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "syncDataSettingsToMd", e);
            }
        }
    }
    // M: Data Framework - common part enhancement @}

    // M: Data Framework - Data Retry enhancement @{
    // Reset the data retry count in modem
    public void resetMdDataRetryCount(String apnName, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            //AT+EDRETRY=1,<apn name>
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_RESET_MD_DATA_RETRY_COUNT,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> "
                                    + requestToString(rr.mRequest) + ": " + apnName);

            try {
                radioProxy.resetMdDataRetryCount(rr.mSerial, apnName);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "resetMdDataRetryCount", e);
            }
        }
    }
    public void registerForMdDataRetryCountReset(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mMdDataRetryCountResetRegistrants.add(r);
    }
    public void unregisterForMdDataRetryCountReset(Handler h) {
        mMdDataRetryCountResetRegistrants.remove(h);
    }
    // M: Data Framework - Data Retry enhancement @}

    // M: Data Framework - CC 33 @{
    public void setRemoveRestrictEutranMode(boolean enable, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            //AT+ECODE33 = <on/off>
            RILRequest rr = obtainRequest(
                    MtkRILConstants.RIL_REQUEST_SET_REMOVE_RESTRICT_EUTRAN_MODE,
                    result, mRILDefaultWorkSource);
            int type = enable ? 1 : 0;
            if (RILJ_LOGD) mtkRiljLog(rr.serialString() + "> "
                                    + requestToString(rr.mRequest) + ": " + type);

            try {
                radioProxy.setRemoveRestrictEutranMode(rr.mSerial, type);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setRemoveRestrictEutranMode", e);
            }
        }
    }
    public void registerForRemoveRestrictEutran(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mRemoveRestrictEutranRegistrants.add(r);
    }
    public void unregisterForRemoveRestrictEutran(Handler h) {
        mRemoveRestrictEutranRegistrants.remove(h);
    }
    // M: Data Framework - CC 33 @}

    // For IMS conference
    /* Register for updating call ids for conference call after SRVCC is done. */
    protected RegistrantList mEconfSrvccRegistrants = new RegistrantList();

    /* Register for updating call ids for conference call after SRVCC is done. */
    public void registerForEconfSrvcc(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mEconfSrvccRegistrants.add(r);
    }

    public void unregisterForEconfSrvcc(Handler h) {
        mEconfSrvccRegistrants.remove(h);
    }

    /// M: [Network][C2K] Sprint roaming control @{
    public void setRoamingEnable(int[] config, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_ROAMING_ENABLE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            ArrayList<Integer> intList = new ArrayList<Integer>(config.length);
            for (int i = 0; i < config.length; i++) {
                intList.add(config[i]);
            }

            try {
                radioProxy.setRoamingEnable(rr.mSerial, intList);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setRoamingEnable", e);
            }
        }
    }

    public void getRoamingEnable(int phoneId, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_ROAMING_ENABLE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.getRoamingEnable(rr.mSerial, phoneId);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getRoamingEnable", e);
            }
        }
    }
    /// @}

    /**
     * Set lte release version.
     *
     * @param mode setting mode of lte release version.
     * @param result response callback message.
     */
    public void setLteReleaseVersion(int mode , Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_LTE_RELEASE_VERSION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " mode = " + mode);
            }

            try {
                radioProxy.setLteReleaseVersion(rr.mSerial, mode);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setLteReleaseVersion", e);
            }
        }
    }

    /**
     * Get lte release version.
     *
     * @param result response callback message.
     */
    public void getLteReleaseVersion(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_LTE_RELEASE_VERSION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.getLteReleaseVersion(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getLteReleaseVersion", e);
            }
        }
    }

    // Add for RIL_UNSOL_MCCMNC_CHANGED
    protected RegistrantList mMccMncRegistrants = new RegistrantList();

    /**
     * Registers the handler for when MCC MNC changed events
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForMccMncChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mMccMncRegistrants.add(r);
    }

    public void unregisterForMccMncChanged(Handler h) {
        mMccMncRegistrants.remove(h);
    }

    // External SIM [Start]
    protected RegistrantList mVsimIndicationRegistrants = new RegistrantList();
    public void registerForVsimIndication(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        if (RILJ_LOGD)  mtkRiljLog("registerForVsimIndication called...");
        mVsimIndicationRegistrants.add(r);
    }

    public void unregisterForVsimIndication(Handler h) {
        if (RILJ_LOGD)  mtkRiljLog("unregisterForVsimIndication called...");
        mVsimIndicationRegistrants.remove(h);
    }

    public boolean sendVsimNotification(
            int transactionId, int eventId, int simType, Message message) {
        boolean result = true;

        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_VSIM_NOTIFICATION, message,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + ", eventId: " +  eventId
                        + ", simTpye: " + simType);
            }

            try {
                radioProxy.sendVsimNotification(rr.mSerial, transactionId, eventId, simType);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rr, "sendVsimNotification", e);
                result = false;
            }
        } else {
            result = false;
        }

        return result;
    }

    public boolean sendVsimOperation(int transactionId, int eventId, int message,
            int dataLength, byte[] data, Message response) {
        boolean result = true;

        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(response);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_VSIM_OPERATION, response,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            if (RILJ_LOGV) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + ", eventId: " + eventId
                        + ", length: " + dataLength);
            }

            ArrayList<Byte> arrList = new ArrayList<>();
            for (int i = 0; i < data.length; i++) {
                arrList.add(data[i]);
            }

            try {
                radioProxy.sendVsimOperation(rr.mSerial, transactionId, eventId,
                        message, dataLength, arrList);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rr, "sendVsimOperation", e);
                result = false;
            }
        } else {
            result = false;
        }

        return result;
    }
    // External SIM [End]

    /// Ims Data Framework {@
    protected RegistrantList mDedicatedBearerActivedRegistrants = new RegistrantList();
    public void registerForDedicatedBearerActivated(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDedicatedBearerActivedRegistrants.add(r);
    }

    public void unregisterForDedicatedBearerActivated(Handler h) {
        mDedicatedBearerActivedRegistrants.remove(h);
    }

    protected RegistrantList mDedicatedBearerModifiedRegistrants = new RegistrantList();
    public void registerForDedicatedBearerModified(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDedicatedBearerModifiedRegistrants.add(r);
    }

    public void unregisterForDedicatedBearerModified(Handler h) {
        mDedicatedBearerModifiedRegistrants.remove(h);
    }

    protected RegistrantList mDedicatedBearerDeactivatedRegistrants = new RegistrantList();
    public void registerForDedicatedBearerDeactivationed(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDedicatedBearerDeactivatedRegistrants.add(r);
    }

    public void unregisterForDedicatedBearerDeactivationed(Handler h) {
        mDedicatedBearerDeactivatedRegistrants.remove(h);
    }

    public MtkDedicateDataCallResponse convertDedicatedDataCallResult(DedicateDataCall ddcResult) {

        int ddcId = ddcResult.ddcId;
        int interfaceId = ddcResult.interfaceId;
        int primaryCid = ddcResult.primaryCid;
        int cid = ddcResult.cid;
        int active = ddcResult.active;
        int signalingFlag = ddcResult.signalingFlag;
        int bearerId = ddcResult.bearerId;
        int failCause = ddcResult.failCause;

        MtkQosStatus mtkQosStatus = null;
        mtkRiljLog("ddcResult.hasQos: " + ddcResult.hasQos);
        if (ddcResult.hasQos != 0) {
            int qci = ddcResult.qos.qci;
            int dlGbr = ddcResult.qos.dlGbr;
            int ulGbr = ddcResult.qos.ulGbr;
            int dlMbr = ddcResult.qos.dlMbr;
            int ulMbr = ddcResult.qos.ulMbr;
            mtkQosStatus = new MtkQosStatus(qci, dlGbr, ulGbr, dlMbr, ulMbr);
        }

        MtkTftStatus mtkTftStatus = null;
        mtkRiljLog("ddcResult.hasTft: " + ddcResult.hasTft);
        if (ddcResult.hasTft != 0) {
            int operation = ddcResult.tft.operation;
            ArrayList<MtkPacketFilterInfo> mtkPacketFilterInfo
                    = new ArrayList<MtkPacketFilterInfo>();
            for(PktFilter info : ddcResult.tft.pfList){
                MtkPacketFilterInfo pfInfo
                        = new MtkPacketFilterInfo(info.id, info.precedence, info.direction,
                                                  info.networkPfIdentifier, info.bitmap,
                                                  info.address, info.mask,
                                                  info.protocolNextHeader, info.localPortLow,
                                                  info.localPortHigh, info.remotePortLow,
                                                  info.remotePortHigh, info.spi, info.tos,
                                                  info.tosMask, info.flowLabel);
                mtkPacketFilterInfo.add(pfInfo);
            }

            ArrayList<Integer> pfList = (ArrayList<Integer>)ddcResult.tft.tftParameter.linkedPfList;
            MtkTftParameter mtkTftParameter = new MtkTftParameter(pfList);
            mtkTftStatus = new MtkTftStatus(operation, mtkPacketFilterInfo, mtkTftParameter);
        }

        String pcscfAddress = ddcResult.pcscf;
        return new MtkDedicateDataCallResponse(interfaceId, primaryCid, cid, active,
                                               signalingFlag, bearerId, failCause,
                                               mtkQosStatus, mtkTftStatus, pcscfAddress);
    }
    /// @}

    /**
     * Request modem to exit ECBM(MD1)
     *
     * @param result callback message
     * @hide
     */
    public void setE911State(int state, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(
                    MtkRILConstants.RIL_REQUEST_SET_E911_STATE,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) +
                        ", state: " + state);
            }

// FIXME
/*
            try {
                radioProxy.setE911State(rr.mSerial, state);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setE911State", e);
            }
*/
        }
    }

    public void setServiceStateToModem(int voiceRegState, int dataRegState, int voiceRoamingType,
            int dataRoamingType, int rilVoiceRegState, int rilDataRegState, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(MtkRILConstants.RIL_REQUEST_SET_SERVICE_STATE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " voiceRegState: " + voiceRegState
                + " dataRegState: " + dataRegState
                + " voiceRoamingType: " + voiceRoamingType
                + " dataRoamingType: " + dataRoamingType
                + " rilVoiceRegState: " + rilVoiceRegState
                + " rilDataRegState:" + rilDataRegState);
            }

            try {
                radioProxy.setServiceStateToModem(rr.mSerial,
                        voiceRegState, dataRegState, voiceRoamingType, dataRoamingType,
                        rilVoiceRegState, rilDataRegState);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setServiceStateToModem", e);
            }
        }
    }

    public void dataConnectionAttach(int type, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DATA_CONNECTION_ATTACH,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.dataConnectionAttach(rr.mSerial, type);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "dataConnectionAttach", e);
            }
        }
    }

    public void dataConnectionDetach(int type, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DATA_CONNECTION_DETACH,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.dataConnectionDetach(rr.mSerial, type);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "dataConnectionDetach", e);
            }
        }
    }

    public void resetAllConnections(Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_RESET_ALL_CONNECTIONS,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.resetAllConnections(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "resetAllConnections", e);
            }
        }
    }

    public void setOnUnsolOemHookRaw(Handler h, int what, Object obj) {
            mUnsolOemHookRegistrant = new Registrant (h, what, obj);
        }

    public void unSetOnUnsolOemHookRaw(Handler h) {
        if (mUnsolOemHookRegistrant != null && mUnsolOemHookRegistrant.getHandler() == h) {
            mUnsolOemHookRegistrant.clear();
            mUnsolOemHookRegistrant = null;
        }
    }

    public void invokeOemRilRequestRaw(byte[] data, Message response) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(response);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_OEM_HOOK_RAW, response,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + "[" + IccUtils.bytesToHexString(data) + "]");
            }

            try {
                radioProxy.sendRequestRaw(rr.mSerial, primitiveArrayToArrayList(data));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "invokeOemRilRequestStrings", e);
            }
        }
    }

    public void invokeOemRilRequestStrings(String[] strings, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_OEM_HOOK_STRINGS, result,
                    mRILDefaultWorkSource);

            String logStr = "";
            for (int i = 0; i < strings.length; i++) {
                logStr = logStr + strings[i] + " ";
            }
            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " strings = "
                        + logStr);
            }

            try {
                radioProxy.sendRequestStrings(rr.mSerial,
                        new ArrayList<String>(Arrays.asList(strings)));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "invokeOemRilRequestStrings", e);
            }
        }
    }

    public void setDisable2G(boolean mode, Message result){
        // for op only
        IMtkRilOp rilOp = getRilOp();
        if (rilOp != null) {
            rilOp.setDisable2G(mode, result);
            return;
        }
    }

    public void getDisable2G(Message result){
        // for op only
        IMtkRilOp rilOp = getRilOp();
        if (rilOp != null) {
            rilOp.getDisable2G(result);
            return;
        }
    }

    /**
    * Register for TX power change
    */
    public void registerForTxPower(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mTxPowerRegistrant.add(r);
    }

    /**
    * unregister for TX power
    */
    public void unregisterForTxPower(Handler h) {
        mTxPowerRegistrant.remove(h);
    }

     /**
     * Register for TX power status change, for Tx power reduction
     */
     public void registerForTxPowerStatus(Handler h, int what, Object obj) {
         Registrant r = new Registrant(h, what, obj);
         mTxPowerStatusRegistrant.add(r);
     }

     /**
        * unregister for TX power status, for Tx power reduction
        */
    public void unregisterForTxPowerStatus(Handler h) {
        mTxPowerStatusRegistrant.remove(h);
    }

    public void setTxPowerStatus(int enable, Message result) {
      vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
      if (radioProxy != null) {
          RILRequest rr = obtainRequest(RIL_REQUEST_SET_TX_POWER_STATUS, result,
                  mRILDefaultWorkSource);

          if (RILJ_LOGD) {
              mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
          }

          try {
              radioProxy.setTxPowerStatus(rr.mSerial, enable);
              Message msg = mRilHandler.obtainMessage(EVENT_BLOCKING_RESPONSE_TIMEOUT);
              msg.obj = null;
              msg.arg1 = rr.mSerial;
              mRilHandler.sendMessageDelayed(msg, DEFAULT_BLOCKING_MESSAGE_RESPONSE_TIMEOUT_MS);
          } catch (RemoteException e) {
              handleRadioProxyExceptionForRR(rr, "setTxPowerStatus", e);
          }
      }
    }

    public void reportAirplaneMode(int on, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_REPORT_AIRPLANE_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.reportAirplaneMode(rr.mSerial, on);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "reportAirplaneMode", e);
            }
        }
    }

    public void reportSimMode(int simMode, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_REPORT_SIM_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.reportSimMode(rr.mSerial, simMode);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "reportSimMode", e);
            }
        }
    }

    public void setSilentReboot(int on, Message result) {
        vendor.mediatek.hardware.radio.V3_0.IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_SILENT_REBOOT, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.setSilentReboot(rr.mSerial, on);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setSilentReboot", e);
            }
        }
    }


    // M: Data Framework - common part enhancement @{
    /**
     * Carrier Express: sync operator configuration to vendor layer
     *
     * @param type operator configuration type
     * @param data operator configuration data
     * @param result for result
     */
    public void setOperatorConfiguration(int type, String data, Message result) {
        IRadio radioProxy = getRadioProxy(result);

        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_OPERATOR_CONFIGURATION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToStringEx(rr.mRequest)
                        + " type=" + type + ", data=" + data);
            }

            try {
                radioProxy.setOperatorConfiguration(rr.mSerial, type, data);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setOperatorConfiguration", e);
            }
        }
    }

    // M: Supplementary Service - IOT easy config property setting @{
    /**
     * @param name property name
     * @param data property value
     * @param result for result
     */
    public void setSuppServProperty(String name, String value, Message result) {
        IRadio radioProxy = getRadioProxy(result);

        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_SS_PROPERTY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToStringEx(rr.mRequest)
                        + " name=" + name + ", value=" + value + ", result=" + result);
            }

            try {
                radioProxy.setSuppServProperty(rr.mSerial, name, value);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setSuppServProperty", e);
            }
        }
    }

    public void getSuppServProperty(String name, Message result) {
        IRadio radioProxy = getRadioProxy(result);

        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_SS_PROPERTY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                mtkRiljLog(rr.serialString() + "> " + requestToStringEx(rr.mRequest)
                        + " name=" + name);
            }

            try {
                radioProxy.getSuppServProperty(rr.mSerial, name);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getSuppServProperty", e);
            }
        }
    }

    protected RegistrantList mDsbpStateRegistrant = new RegistrantList();

    public void registerForDsbpStateChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDsbpStateRegistrant.add(r);
    }

    public void unregisterForDsbpStateChanged(Handler h) {
        mDsbpStateRegistrant.remove(h);
    }

    public void mtkRiljLog(String msg) {
        Rlog.d(RILJ_LOG_TAG, msg
                + (mPhoneId != null ? (" [SUB" + mPhoneId + "]") : ""));
    }

    public void mtkRiljLoge(String msg) {
        Rlog.e(RILJ_LOG_TAG, msg
                + (mPhoneId != null ? (" [SUB" + mPhoneId + "]") : ""));
    }

}
