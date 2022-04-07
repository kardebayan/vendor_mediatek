package com.mediatek.ims.ril;

import static com.android.internal.telephony.RILConstants.RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_CALL_INFO_INDICATION;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_ECONF_RESULT_INDICATION;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_GET_PROVISION_DONE;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_BEARER_ACTIVATION;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_BEARER_DEACTIVATION;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_BEARER_INIT;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_DISABLE_DONE;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_DISABLE_START;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_DEREG_DONE;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_ENABLE_DONE;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_ENABLE_START;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_REGISTRATION_INFO;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_RTP_INFO;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_SIP_CALL_PROGRESS_INDICATOR;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_EVENT_PACKAGE_INDICATION;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_VIDEO_CAPABILITY_INDICATOR;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_INCOMING_CALL_INDICATION;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_VOLTE_SETTING;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_ECT_INDICATION;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_ON_USSI;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_ON_XUI;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_ON_VOLTE_SUBSCRIPTION;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_MULTIIMS_COUNT;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_CALLMOD_CHANGE_INDICATOR;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_CONFERENCE_INFO_INDICATION;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_LTE_MESSAGE_WAITING_INDICATION;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_CONFIG_DYNAMIC_IMS_SWITCH_COMPLETE;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_CONFIG_CONFIG_CHANGED;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_CONFIG_FEATURE_CHANGED;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_CONFIG_CONFIG_LOADED;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_IMS_DATA_INFO_NOTIFY;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT_EX;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_RESPONSE_NEW_SMS_EX;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_RESPONSE_CDMA_NEW_SMS_EX;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_NO_EMERGENCY_CALLBACK_MODE;
import static com.mediatek.internal.telephony.MtkRILConstants.RIL_UNSOL_INCOMING_CALL_ADDITIONAL_INFO;

import java.util.ArrayList;

import vendor.mediatek.hardware.radio.V3_0.IncomingCallNotification;
import vendor.mediatek.hardware.radio.V3_0.ImsConfParticipant;
import vendor.mediatek.hardware.radio.V3_0.Dialog;

import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.os.AsyncResult;
import android.os.Build;
import android.os.RemoteException;
import android.telephony.Rlog;
import android.telephony.SmsMessage;

import com.android.internal.telephony.cdma.SmsMessageConverter;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.RIL;
import com.mediatek.ims.ImsCallSessionProxy.User;
import com.mediatek.ims.ril.ImsCommandsInterface.RadioState;

public class ImsRadioIndication extends ImsRadioIndicationBase {

    ImsRadioIndication(ImsRILAdapter ril, int phoneId) {
        mRil= ril;
        mPhoneId = phoneId;
    }

    // IMS RIL Instance
    private ImsRILAdapter mRil;
    // Phone Id
    private int mPhoneId;

    // IMS Constants
    private static final int INVALID_CALL_MODE = 0xFF;

    /**
     * Indicates of enter emergency callback mode
     * URC: RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE
     * @param indicationType RadioIndicationType
     * @param radioState android.hardware.radio.V1_0.RadioState
     */
    @Override
    public void enterEmergencyCallbackMode(int indicationType) {

        mRil.processIndication(indicationType);
        if (ImsRILAdapter.IMS_RILA_LOGD)
            mRil.unsljLog(RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE);

        if (mRil.mEnterECBMRegistrants != null) {
            mRil.mEnterECBMRegistrants.notifyRegistrants();
        }
    }

    /**
     * Indicates of exit emergency callback mode
     * URC: RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE
     * @param indicationType RadioIndicationType
     * @param radioState android.hardware.radio.V1_0.RadioState
     */
    @Override
    public void exitEmergencyCallbackMode(int indicationType) {

        mRil.processIndication(indicationType);
        if (ImsRILAdapter.IMS_RILA_LOGD)
            mRil.unsljLog(RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE);

        if (mRil.mExitECBMRegistrants != null) {
            mRil.mExitECBMRegistrants.notifyRegistrants();
        }
    }

    /**
     * Indicates of no emergency callback mode
     * URC: RIL_UNSOL_NO_EMERGENCY_CALLBACK_MODE
     * @param indicationType RadioIndicationType
     * @param radioState android.hardware.radio.V1_0.RadioState
     */
    @Override
    public void noEmergencyCallbackMode(int indicationType) {

        mRil.processIndication(indicationType);
        if (ImsRILAdapter.IMS_RILA_LOGD)
            mRil.unsljLog(RIL_UNSOL_NO_EMERGENCY_CALLBACK_MODE);

        if (mRil.mNoECBMRegistrants != null) {
            mRil.mNoECBMRegistrants.notifyRegistrants();
        }
    }

    /**
     * Indicates of Video Capabilities
     * URC: RIL_UNSOL_VIDEO_CAPABILITY_INDICATOR
     * @param type
     * @param callId
     * @param localVideoCap
     * @param remoteVideoCap
     */
    @Override
    public void videoCapabilityIndicator(int type, String callId,
                                         String localVideoCap,
                                         String remoteVideoCap) {

        mRil.processIndication(type);
        String [] ret = new String[] { callId, localVideoCap, remoteVideoCap };

        if (ImsRILAdapter.IMS_RILA_LOGD)
            mRil.unsljLogRet(RIL_UNSOL_VIDEO_CAPABILITY_INDICATOR, ret);

        if (mRil.mVideoCapabilityIndicatorRegistrants != null) {
            mRil.mVideoCapabilityIndicatorRegistrants
                    .notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * Indicates of Call Mode Change
     * URC: RIL_UNSOL_CALLMOD_CHANGE_INDICATOR
     * @param type Indication type
     * @param callId Call id
     * @param callMode Call mode
     * @param videoState Video state
     * @param audioDirection auto direction
     * @param pau PAU
     */
    @Override
    public void callmodChangeIndicator(int type, String callId, String callMode,
                                       String videoState, String audioDirection,
                                       String pau) {

        mRil.processIndication(type);
        String [] ret = new String[] { callId, callMode, videoState,
                                       audioDirection, pau };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            if (ImsRILAdapter.IMSRIL_SDBG) {
                mRil.unsljLogRet(RIL_UNSOL_CALLMOD_CHANGE_INDICATOR, ret);
            } else {
                mRil.unsljLogRet(RIL_UNSOL_CALLMOD_CHANGE_INDICATOR, "[hidden]");
            }
        }

        if (mRil.mCallModeChangeIndicatorRegistrants != null) {
            mRil.mCallModeChangeIndicatorRegistrants
                    .notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * Indication for VoLTE Setting
     * URC: RIL_UNSOL_VOLTE_SETTING
     * @param type Indication type
     * @param isEnable is VoLTE enable
     */
    @Override
    public void volteSetting(int type, boolean isEnable) {

        mRil.processIndication(type);
        int [] ret = new int[] { isEnable ? 1:0, mPhoneId };

        if (ImsRILAdapter.IMS_RILA_LOGD)
            mRil.unsljLogRet(RIL_UNSOL_VOLTE_SETTING, ret);

        // Store it for sticky notification
        mRil.mVolteSettingValue = ret;
        if (mRil.mVolteSettingRegistrants != null) {
            mRil.mVolteSettingRegistrants
                    .notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * Indication for XUI
     * URC: RIL_UNSOL_ON_XUI
     * @param type Type
     * @param accountId Account Id
     * @param broadcastFlag Broadcast flag
     * @param xuiInfo XUI Information
     */
    @Override
    public void onXui(int type, String accountId, String broadcastFlag,
                      String xuiInfo) {

        mRil.processIndication(type);
        String [] ret = new String[] { accountId, broadcastFlag,
                                       xuiInfo, Integer.toString(mPhoneId) };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_ON_XUI,
                Rlog.pii(ImsRILAdapter.IMSRIL_LOG_TAG, ret));
        }

        if (mRil.mXuiRegistrants != null) {
            mRil.mXuiRegistrants.notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * Indication for XUI
     * URC: RIL_UNSOL_ON_VOLTE_SUBSCRIPTION
     * @param type Type
     * @param status VoLTE Subscription status (VolTE Card, non VoLTE card, Unknown)
     */
    @Override
    public void onVolteSubscription(int type, int status) {

        mRil.processIndication(type);

        int[] ret = new int[] { status, mPhoneId };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_ON_VOLTE_SUBSCRIPTION, ret);
        }

        if (mRil.mVolteSubscriptionRegistrants != null) {
            mRil.mVolteSubscriptionRegistrants.notifyRegistrants(
                    new AsyncResult(null, ret, null));
        }
    }

    /**
     * Indication for ECT
     * URC: RIL_UNSOL_ECT_INDICATION
     * @param type Type
     * @param callId Call id
     * @param ectResult ECT result
     * @param cause Cause
     */
    @Override
    public void ectIndication(int type, int callId, int ectResult,
                              int cause) {

        mRil.processIndication(type);
        int [] ret = new int[] { callId, ectResult, cause };

        if (ImsRILAdapter.IMS_RILA_LOGD)
            mRil.unsljLogRet(RIL_UNSOL_ECT_INDICATION, ret);

        if (mRil.mEctResultRegistrants != null) {
            mRil.mEctResultRegistrants.notifyRegistrants(
                                             new AsyncResult(null, ret, null));
        }
    }

    /**
     * Indication for USSI
     * URC: RIL_UNSOL_ON_USSI
     * @param type Type
     * @param clazz Class
     * @param status Status
     * @param str String
     * @param lang Cause
     * @param errorCode Cause
     * @param alertingPattern Cause
     * @param sipCause Cause
     */
    @Override
    public void onUssi(int type, String clazz, String status, String str,
                       String lang, String errorCode, String alertingPattern,
                       String sipCause) {

        mRil.processIndication(type);
        String [] ret = new String[] { clazz, status, str, lang, errorCode,
                                       alertingPattern, sipCause,
                                       Integer.toString(mPhoneId) };

        if (ImsRILAdapter.IMS_RILA_LOGD)
            mRil.unsljLogRet(RIL_UNSOL_ON_USSI, ret);

        if (mRil.mUSSIRegistrants != null) {
            mRil.mUSSIRegistrants.notifyRegistrants(
                                            new AsyncResult(null, ret, null));

            mRil.riljLog("mRil.mUSSIRegistrants.size() = " + mRil.mUSSIRegistrants.size());
            // If there is no instance in mUSSIRegistrants, it means the current USSI URC
            // is initiated by network, not user
            if (mRil.mUSSIRegistrants.size() == 0) {
                mRil.mNetworkInitUSSIRegistrants.notifyRegistrants(
                        new AsyncResult(null, ret, null));
            }
        }
    }

    /**
     * Indicates for SIP call progress indicator
     * URC: RIL_UNSOL_SIP_CALL_PROGRESS_INDICATOR
     * @param type Indication type
     * @param callId Call Id
     * @param dir Directory
     * @param sipMsgType SIP message type
     * @param method Method
     * @param responseCode Reason code
     * @param reasonText Reason text
     */
    @Override
    public void sipCallProgressIndicator(int type, String callId,
                                         String dir, String sipMsgType,
                                         String method, String responseCode,
                                         String reasonText) {

        mRil.processIndication(type);
        String [] ret = {callId, dir, sipMsgType, method, responseCode, reasonText};

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_SIP_CALL_PROGRESS_INDICATOR, ret);
        }

        if (mRil.mCallProgressIndicatorRegistrants != null) {
            mRil.mCallProgressIndicatorRegistrants
                    .notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * Indicates for ECONF result indication
     * URC: RIL_UNSOL_ECONF_RESULT_INDICATION
     * @param type Indication type
     * @param confCallId Conference call id
     * @param op Operator
     * @param num Number
     * @param result Result
     * @param cause Cause
     * @param joinedCallId Joined call id
     */
    @Override
    public void econfResultIndication(int type, String confCallId,
                                      String op, String num,
                                      String result, String cause,
                                      String joinedCallId) {

        mRil.processIndication(type);
        String [] ret = {confCallId, op, num, result, cause, joinedCallId};

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            if (ImsRILAdapter.IMSRIL_SDBG) {
                mRil.unsljLogRet(RIL_UNSOL_ECONF_RESULT_INDICATION, ret);
            } else {
                mRil.unsljLogRet(RIL_UNSOL_ECONF_RESULT_INDICATION, "[hidden]");
            }
        }

        if (mRil.mEconfResultRegistrants != null) {
            mRil.riljLog("ECONF result = " + ret[3]);
            mRil.mEconfResultRegistrants.notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * Indicates when radio state changes
     * URC: RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED
     * @param indicationType RadioIndicationType
     * @param radioState android.hardware.radio.V1_0.RadioState
     */
    @Override
    public void radioStateChanged(int type, int radioState)
    {

        mRil.processIndication(type);
        RadioState newState = getRadioStateFromInt(radioState);

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogMore(RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED,
                    "radioStateChanged: " + newState);
        }

        mRil.setRadioState(newState);
        mRil.notifyRadioStateChanged(newState);
    }

    /**
     * Call Information Indication
     * URC: RIL_UNSOL_CALL_INFO_INDICATION
     * @param type Indication Type
     * @param result Call indication data
     */
    @Override
    public void callInfoIndication(int indicationType, ArrayList<String> result)
    {

        mRil.processIndication(indicationType);
        String [] callInfo = null;
        if(result == null || result.size() == 0) {
            return;
        }
        else {
            callInfo = result.toArray(new String[result.size()]);
        }

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            if (ImsRILAdapter.IMSRIL_SDBG) {
                mRil.unsljLogRet(RIL_UNSOL_CALL_INFO_INDICATION, callInfo);
            } else {
                mRil.unsljLogRet(RIL_UNSOL_CALL_INFO_INDICATION, "[hidden]");
            }
        }

        if (mRil.mCallInfoRegistrants != null) {
            mRil.mCallInfoRegistrants
                    .notifyRegistrants(new AsyncResult(null, callInfo, null));
        }
    }

    /**
     * Incoming Call Indication
     * URC: RIL_UNSOL_INCOMING_CALL_INDICATION
     * @param type Indication Type
     * @param inCallNotify Call Notification object
     */
    @Override
    public void incomingCallIndication(int type,
            IncomingCallNotification inCallNotify) {

        mRil.processIndication(type);
        String[] ret = new String[7];
        ret[0] = inCallNotify.callId;
        ret[1] = inCallNotify.number;
        ret[2] = inCallNotify.type;
        ret[3] = inCallNotify.callMode;
        ret[4] = inCallNotify.seqNo;
        ret[5] = inCallNotify.redirectNumber;
        ret[6] = inCallNotify.toNumber;
        if (ImsRILAdapter.IMS_RILA_LOGD) {
            if (ImsRILAdapter.IMSRIL_SDBG) {
                mRil.unsljLogRet(RIL_UNSOL_INCOMING_CALL_INDICATION, ret);
            } else {
                mRil.unsljLogRet(RIL_UNSOL_INCOMING_CALL_INDICATION, "[hidden]");
            }
        }

        if (mRil.mIncomingCallIndicationRegistrants != null) {
            mRil.mIncomingCallIndicationRegistrants
                    .notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * Get Provision Down
     * URC: RIL_UNSOL_GET_PROVISION_DONE
     * @param type Indication Type
     * @param result1 Provision Data 1
     * @param result2 Provision Data 2
     */
    @Override
    public void getProvisionDone(int type, String result1, String result2)
    {

        mRil.processIndication(type);
        String[] ret = new String[] {result1, result2};
        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_GET_PROVISION_DONE, ret);
        }

        if (mRil.mImsGetProvisionDoneRegistrants != null) {
            mRil.mImsGetProvisionDoneRegistrants.
                    notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * IMS RTP Information
     * URC: RIL_UNSOL_IMS_RTP_INFO
     * @param type Indication Type
     * @param pdnId PDN Id
     * @param networkId Network Id
     * @param timer Timer
     * @param sendPktLost Send packet lost
     * @param recvPktLost Receive package lost
     * @param jitter Jitter in ms
     * @param delay Delay in ms
     */
    @Override
    public void imsRtpInfoReport(int type, String pdnId, String networkId, String timer,
                           String sendPktLost, String recvPktLost, String jitter, String delay)
   {

        mRil.processIndication(type);
        String[] ret = new String[] {pdnId, networkId, timer, sendPktLost, recvPktLost,
                jitter, delay};
        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_RTP_INFO, ret);
        }

        if (mRil.mRTPInfoRegistrants != null) {
            mRil.mRTPInfoRegistrants.
                    notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

   /**
     * IMS RTP Information
     * URC: RIL_UNSOL_IMS_EVENT_PACKAGE_INDICATION
     * @param type Indication Type
     * @param callid Call Id
     * @param pType P Type
     * @param urcIdx URC Index
     * @param totalUrcCount Total URC count
     * @param rawData Raw Data
     */
    @Override
    public void imsEventPackageIndication(int type,
                                          String callId, String pType, String urcIdx,
                                          String totalUrcCount, String rawData) {
        mRil.processIndication(type);
        String [] ret = new String[] {callId, pType, urcIdx, totalUrcCount,
                                      rawData, Integer.toString(mPhoneId) };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            if (ImsRILAdapter.IMSRIL_SDBG) {
                mRil.unsljLogRet(RIL_UNSOL_IMS_EVENT_PACKAGE_INDICATION, ret);
            } else {
                mRil.unsljLogRet(RIL_UNSOL_IMS_EVENT_PACKAGE_INDICATION, "[hidden]");
            }
        }

        if (mRil.mImsEvtPkgRegistrants != null) {
            mRil.mImsEvtPkgRegistrants.
                    notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * IMS Registeration Information Updated
     * URC: RIL_UNSOL_IMS_REGISTRATION_INFO
     * @param type Indication Type
     * @param status IMS registeration status
     * @param capability IMS capabilities
     */
    @Override
    public void imsRegistrationInfo(int type, int status, int capability)
    {

        mRil.processIndication(type);
        int [] ret = new int[] {status, capability, mPhoneId};
        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_REGISTRATION_INFO, ret);
        }

        if (mRil.mImsRegistrationInfoRegistrants != null) {
            mRil.mImsRegistrationInfoRegistrants.
                    notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * IMS Enabled
     * URC: RIL_UNSOL_IMS_ENABLE_DONE
     * @param type Indication Type
     */
    @Override
    public void imsEnableDone(int type)
    {

        mRil.processIndication(type);
        int [] ret = new int[] { mPhoneId };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_ENABLE_DONE, ret);
        }

        if (mRil.mImsEnableDoneRegistrants != null) {
            mRil.mImsEnableDoneRegistrants.notifyRegistrants(new AsyncResult(null, ret,
                    null));
        }
    }

    /**
     * IMS Disabled
     * URC: RIL_UNSOL_IMS_DISABLE_DONE
     * @param type Indication Type
     */
    @Override
    public void imsDisableDone(int type)
    {

        mRil.processIndication(type);
        int [] ret = new int[] { mPhoneId };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_DISABLE_DONE, ret);
        }

        if (mRil.mImsDisableDoneRegistrants != null) {
            mRil.mImsDisableDoneRegistrants.notifyRegistrants(new AsyncResult(null, ret,
                    null));
        }
    }

    /**
     * Start IMS Enabling
     * URC: RIL_UNSOL_IMS_ENABLE_START
     * @param type Indication Type
     */
    @Override
    public void imsEnableStart(int type)
    {

        mRil.processIndication(type);
        int [] ret = new int[] { mPhoneId };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_ENABLE_START, ret);
        }

        if (mRil.mImsEnableStartRegistrants != null) {
            mRil.mImsEnableStartRegistrants.notifyRegistrants(new AsyncResult(null, ret,
                    null));
        }
    }

    /**
     * Start IMS Disabling
     * URC: RIL_UNSOL_IMS_DISABLE_START
     * @param type Indication Type
     */
    @Override
    public void imsDisableStart(int type)
    {

        mRil.processIndication(type);
        int [] ret = new int[] { mPhoneId };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_DISABLE_START, ret);
        }

        if (mRil.mImsDisableStartRegistrants != null) {
            mRil.mImsDisableStartRegistrants.notifyRegistrants(new AsyncResult(null, ret,
                    null));
        }
    }

    /**
     * IMS Bearer Activation
     * URC: RIL_UNSOL_IMS_BEARER_ACTIVATION
     * @param type Indication Type
     * @param aid AID
     * @param capability Capability
     */
    @Override
    public void imsBearerActivation(int type, int aid, String capability)
    {

        mRil.processIndication(type);

        String phoneId = String.valueOf(mPhoneId);
        String strAid = String.valueOf(aid);
        String [] ret = new String[] { phoneId, strAid, capability };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_BEARER_ACTIVATION, ret);
        }

        if (mRil.mActivateBearerRegistrants != null) {
            mRil.mActivateBearerRegistrants.notifyRegistrants(new AsyncResult(null, ret,
                    null));
        }
    }

    /**
     * Indication for IMS Bearer Deactivation
     * URC: RIL_UNSOL_IMS_BEARER_DEACTIVATION
     * @param type Indication Type
     * @param aid AID
     * @param capability Capability
     */
    @Override
    public void imsBearerDeactivation(int type, int aid, String capability)
    {

        mRil.processIndication(type);

        String phoneId = String.valueOf(mPhoneId);
        String strAid = String.valueOf(aid);
        String [] ret = new String[] { phoneId, strAid, capability };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_BEARER_DEACTIVATION, ret);
        }

        if (mRil.mDeactivateBearerRegistrants != null) {
            mRil.mDeactivateBearerRegistrants.notifyRegistrants(new AsyncResult(null, ret,
                    null));
        }
    }

    /**
     * Indication for IMS Bearer Deactivation
     * URC: RIL_UNSOL_IMS_BEARER_INIT
     * @param type Indication Type
     * @param aid AID
     * @param capability Capability
     */
    @Override
    public void imsBearerInit(int type)
    {

        mRil.processIndication(type);

        int ret [] = new int[] { mPhoneId };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_BEARER_INIT, ret);
        }

        if (mRil.mBearerInitRegistrants != null) {
            mRil.mBearerInitRegistrants.notifyRegistrants(new AsyncResult(null, ret,
                    null));
        }
    }

    @Override
    public void imsDataInfoNotify(int type, String capability,
              String event, String extra) {

        mRil.processIndication(type);

        String phoneId = String.valueOf(mPhoneId);
        String [] ret = new String[] { phoneId, capability, event, extra };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_DATA_INFO_NOTIFY, ret);
        }

        if (mRil.mImsDataInfoNotifyRegistrants != null) {
            mRil.mImsDataInfoNotifyRegistrants.
                    notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    @Override
    public void imsDeregDone(int type) {

        mRil.processIndication(type);

        int ret [] = new int[] { mPhoneId };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_DEREG_DONE, ret);
        }

        if (mRil.mImsDeregistrationDoneRegistrants != null) {
            mRil.mImsDeregistrationDoneRegistrants.notifyRegistrants(
                   new AsyncResult(null, ret, null));
        }
    }

    @Override
    public void multiImsCount(int type, int count) {

        mRil.processIndication(type);

        int [] ret = new int[] {count, mPhoneId};

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_MULTIIMS_COUNT, ret);
        }

        if (mRil.mMultiImsCountRegistrants != null) {
            mRil.mMultiImsCountRegistrants.
                    notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    @Override
    public void imsSupportEcc(int type, int supportLteEcc) {

        mRil.processIndication(type);

        int[] ret = new int[] { supportLteEcc, mPhoneId };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.riljLog(" RIL_UNSOL_IMS_ECC_SUPPORT, " + supportLteEcc +
                    " phoneId = " + mPhoneId);
        }

        if (mRil.mImsEccSupportRegistrants != null &&
                mRil.mImsEccSupportRegistrants.size() != 0) {
            mRil.mImsEccSupportRegistrants.notifyRegistrants(
                    new AsyncResult(null, ret, null));
        } else {
            mRil.riljLog("Cache supportLteEcc, " + supportLteEcc +
                    " phoneId = " + mPhoneId);
            mRil.mSupportLteEcc = supportLteEcc;
        }
    }

    @Override
    public void imsRadioInfoChange(int type, String iid, String info) {

    }

    @Override
    public void speechCodecInfoIndication(int type, int info) {
        mRil.processIndication(type);
        int[] ret = new int[] { info };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.riljLog(" RIL_UNSOL_SPEECH_CODEC_INFO, " + info +
                    " phoneId = " + mPhoneId);
        }

        if (mRil.mSpeechCodecInfoRegistrant != null) {
            mRil.mSpeechCodecInfoRegistrant.notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * Indication for IMS Conference participants info
     * URC: RIL_UNSOL_IMS_CONFERENCE_INFO_INDICATION
     * @param type Indication type
     * @param arrays of IMS conference participant info
     */
    @Override
    public void imsConferenceInfoIndication(int type,
            ArrayList<ImsConfParticipant> participants) {

        mRil.processIndication(type);
        ArrayList<User> ret = new ArrayList<User>();
        for (int i = 0; i < participants.size(); i++) {
            User user = new User();
            user.mUserAddr = participants.get(i).user_addr;
            user.mEndPoint = participants.get(i).end_point;
            user.mEntity   = participants.get(i).entity;
            user.mDisplayText = participants.get(i).display_text;
            user.mStatus = participants.get(i).status;
            ret.add(user);
        }

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            if (ImsRILAdapter.IMSRIL_SDBG) {
                mRil.unsljLogRet(RIL_UNSOL_IMS_CONFERENCE_INFO_INDICATION, ret);
            } else {
                mRil.unsljLogRet(RIL_UNSOL_IMS_CONFERENCE_INFO_INDICATION, "[hidden]");
            }
        }

        if (mRil.mImsConfInfoRegistrants != null) {
            mRil.mImsConfInfoRegistrants.
            notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * LTE Message Waiting Information
     * URC: RIL_UNSOL_LTE_MESSAGE_WAITING_INDICATION
     * @param type Indication Type
     * @param callid Call Id
     * @param pType P Type
     * @param urcIdx URC Index
     * @param totalUrcCount Total URC count
     * @param rawData Raw Data
     */
    @Override
    public void lteMessageWaitingIndication(int type,
                                          String callId, String pType, String urcIdx,
                                          String totalUrcCount, String rawData) {

        mRil.processIndication(type);
        String [] ret = new String[] {callId, pType, urcIdx, totalUrcCount,
                rawData, Integer.toString(mPhoneId) };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            if (ImsRILAdapter.IMSRIL_SDBG) {
                mRil.unsljLogRet(RIL_UNSOL_LTE_MESSAGE_WAITING_INDICATION, ret);
            } else {
                mRil.unsljLogRet(RIL_UNSOL_LTE_MESSAGE_WAITING_INDICATION, "[hidden]");
            }
        }

        if (mRil.mLteMsgWaitingRegistrants != null) {
            mRil.mLteMsgWaitingRegistrants.
            notifyRegistrants(new AsyncResult(null, ret, null));
        }
    }

    /**
     * [IMS] IMS Dialog Event Package Indiciation
     * @param type Type of radio indication
     * @param dialogList the dialog info list
     */
    @Override
    public void imsDialogIndication(int type, ArrayList<Dialog> dialogList) {
        mRil.processIndication(type);

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.riljLog("RIL_UNSOL_IMS_DIALOG_INDICATION");
            for (Dialog d : dialogList) {
                mRil.riljLog("RIL_UNSOL_IMS_DIALOG_INDICATION" + "dialogId = " + d.dialogId
                        + ",address:" + d.address);
            }
        }
        if (mRil.mImsDialogRegistrant != null) {
            mRil.mImsDialogRegistrant.notifyRegistrants(new AsyncResult(null, dialogList, null));
        }
    }

    @Override
    public void imsCfgDynamicImsSwitchComplete(int type) {
        mRil.processIndication(type);

        int ret [] = new int[] { mPhoneId };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_CONFIG_DYNAMIC_IMS_SWITCH_COMPLETE, ret);
        }

        if (mRil.mImsCfgDynamicImsSwitchCompleteRegistrants != null) {
            mRil.mImsCfgDynamicImsSwitchCompleteRegistrants.notifyRegistrants(
                    new AsyncResult(null, ret, null));
        }
    }

    @Override
    public void imsCfgFeatureChanged(int type, int phoneId, int featureId, int value) {
        mRil.processIndication(type);

        int ret [] = new int[] { mPhoneId, featureId, value };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_CONFIG_FEATURE_CHANGED, ret);
        }

        if (mRil.mImsCfgFeatureChangedRegistrants != null) {
            mRil.mImsCfgFeatureChangedRegistrants.notifyRegistrants(
                    new AsyncResult(null, ret, null));
        }
    }

    @Override
    public void imsCfgConfigChanged(int type, int phoneId, String configId, String value) {
        mRil.processIndication(type);

        String ret [] = new String[] { Integer.toString(mPhoneId), configId, value };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_CONFIG_CONFIG_CHANGED, ret);
        }

        if (mRil.mImsCfgConfigChangedRegistrants != null) {
            mRil.mImsCfgConfigChangedRegistrants.notifyRegistrants(
                    new AsyncResult(null, ret, null));
        }
    }

    @Override
    public void imsCfgConfigLoaded(int type) {
        mRil.processIndication(type);

        String ret [] = new String[] { Integer.toString(mPhoneId)};

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_IMS_CONFIG_CONFIG_LOADED, ret);
        }

        if (mRil.mImsCfgConfigLoadedRegistrants != null) {
            mRil.mImsCfgConfigLoadedRegistrants.notifyRegistrants(
                    new AsyncResult(null, ret, null));
        }
    }

    @Override
    public void newSmsStatusReportEx(int indicationType, ArrayList<Byte> pdu) {
        mRil.processIndication(indicationType);

        String ret [] = new String[] { Integer.toString(mPhoneId)};

        byte[] pduArray = RIL.arrayListToPrimitiveArray(pdu);
        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT_EX, ret);
        }

        if (mRil.mSmsStatusRegistrant != null) {
            mRil.mSmsStatusRegistrant.notifyRegistrant(new AsyncResult(null, pduArray, null));
        }
    }

    @Override
    public void newSmsEx(int indicationType, ArrayList<Byte> pdu) {
        mRil.processIndication(indicationType);

        String ret [] = new String[] { Integer.toString(mPhoneId)};

        byte[] pduArray = RIL.arrayListToPrimitiveArray(pdu);
        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_RESPONSE_NEW_SMS_EX, ret);
        }

        if (mRil.mNewSmsRegistrant != null) {
            mRil.mNewSmsRegistrant.notifyRegistrant(new AsyncResult(null, pduArray, null));
        }
    }

    @Override
    public void cdmaNewSmsEx(int indicationType, CdmaSmsMessage msg) {
        mRil.processIndication(indicationType);

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.unsljLog(RIL_UNSOL_RESPONSE_CDMA_NEW_SMS_EX);
        }

        SmsMessage sms = SmsMessageConverter.newSmsMessageFromCdmaSmsMessage(msg);
        if (mRil.mCdmaSmsRegistrant != null) {
            mRil.mCdmaSmsRegistrant.notifyRegistrant(new AsyncResult(null, sms, null));
        }
    }

    public void imsRedialEmergencyIndication(int type, String callId) {

        mRil.processIndication(type);

        String [] ret = new String[] {callId, Integer.toString(mPhoneId) };

        if (ImsRILAdapter.IMS_RILA_LOGD) {
            mRil.riljLog(" RIL_UNSOL_REDIAL_EMERGENCY_INDICATION, " + callId +
                    " phoneId = " + mPhoneId);
        }

        if (mRil.mImsRedialEccIndRegistrants != null) {
            mRil.mImsRedialEccIndRegistrants.notifyRegistrants(
                    new AsyncResult(null, ret, null));
        }
    }

    @Override
    public void incomingCallAdditionalInfoInd(int indicationType, ArrayList<String> info) {
        mRil.processIndication(indicationType);

        String [] notification = info.toArray(new String[info.size()]);

        if (RIL.RILJ_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_INCOMING_CALL_ADDITIONAL_INFO, notification);
        }


        if (mRil.mIncomingCallAdditionalInfoRegistrant !=  null) {
            mRil.mIncomingCallAdditionalInfoRegistrant
                    .notifyRegistrants(new AsyncResult(null, notification, null));
        }
    }

    @Override
    protected void riljLoge(String msg) {
        mRil.riljLoge(msg);
    }

    /**
     * Get Radio State from Int
     * AOSP Code
     * @param stateInt
     * @return
     */
    private RadioState getRadioStateFromInt(int stateInt) {
        RadioState state;
        /* RIL_RadioState ril.h */
        switch(stateInt) {
            case 0: state = RadioState.RADIO_OFF; break;
            case 1: state = RadioState.RADIO_UNAVAILABLE; break;
            case 10: state = RadioState.RADIO_ON; break;
            default:
                throw new RuntimeException(
                            "Unrecognized IMS_RIL_RadioState: " + stateInt);
        }
        return state;
    }
}
