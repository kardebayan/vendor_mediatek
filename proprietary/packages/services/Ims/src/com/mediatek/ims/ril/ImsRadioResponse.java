package com.mediatek.ims.ril;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.LastCallFailCause;

import vendor.mediatek.hardware.radio.V3_0.CallForwardInfoEx;

import android.hardware.radio.V1_0.LastCallFailCauseInfo;
import android.hardware.radio.V1_0.RadioError;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.os.AsyncResult;
import android.os.Message;
import android.os.RemoteException;

import com.mediatek.internal.telephony.MtkCallForwardInfo;

// SMS-START
import android.hardware.radio.V1_0.SendSmsResult;
import com.android.internal.telephony.SmsResponse;
// SMS-END

public class ImsRadioResponse extends ImsRadioResponseBase {

    ImsRadioResponse(ImsRILAdapter ril, int phoneId) {
        mRil= ril;
        mPhoneId = phoneId;
        mRil.riljLogv("ImsRadioResponse, phone = " + mPhoneId);
    }

    // IMS RIL Instance
    private ImsRILAdapter mRil;
    // Phone Id
    private int mPhoneId;

    /**
     * Helper function to send response msg
     * @param msg Response message to be sent
     * @param ret Return object to be included in the response message
     */
    static void sendMessageResponse(Message msg, Object ret) {
        if (msg != null) {
            AsyncResult.forMessage(msg, ret, null);
            msg.sendToTarget();
        }
    }

    /**
     * Response for request 'getLastCallFailCause'
     * @param responseInfo Response info containing response type, serial no. and error
     */
    @Override
    public void getLastCallFailCauseResponse(RadioResponseInfo responseInfo,
                LastCallFailCauseInfo failCauseInfo) {

        responseFailCause(responseInfo, failCauseInfo);
    }

    /**
     * Response for request 'pullCall'
     * @param responseInfo Response info containing response type, serial no. and error
     */
    @Override
    public void pullCallResponse(RadioResponseInfo responseInfo) {
       responseVoid(responseInfo);
    }

    /**
     * Response for request 'acceptCall'
     * @param responseInfo Response info containing response type, serial no. and error
     */
    @Override
    public void acceptCallResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * Response for request 'conference'
     * @param info Radio Response Info
     */
    @Override
    public void conferenceResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'dial'
     * @param responseInfo Response info containing response type, serial no. and error
     */
    @Override
    public void dialResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * Response for request 'exitEmergencyCallbackModeResponse'
     * @param responseInfo Response info containing response type, serial no. and error
     */
    @Override
    public void exitEmergencyCallbackModeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * Response for request 'explicitCallTransfer'
     * @param responseInfo Response info containing response type, serial no. and error
     */
    @Override
    public void explicitCallTransferResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * Response for request 'hangupConnection'
     * AOSP code
     * @param responseInfo Response info containing response type, serial no. and error
     */
    @Override
    public void hangupConnectionResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * Response for request 'sendDtmf'
     * AOSP code
     * @param responseInfo Response info containing response type, serial no. and error
     */
    @Override
    public void sendDtmfResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * Response for request 'setMute'
     * @param responseInfo Response info containing response type, serial no. and error
     */
    @Override
    public void setMuteResponse(RadioResponseInfo response) {
        responseVoid(response);
    }

    /**
     * Response for request 'startDtmf'
     * AOSP code
     * @param responseInfo Response info containing response type, serial no. and error
     */
    @Override
    public void startDtmfResponse(RadioResponseInfo responseInfo) {
        mRil.handleDtmfQueueNext(responseInfo.serial);
        responseVoid(responseInfo);
    }

    /**
     * Response for request 'stopDtmf'
     * AOSP code
     * @param responseInfo Response info containing response type, serial no. and error
     */
    @Override
    public void stopDtmfResponse(RadioResponseInfo info) {
        mRil.handleDtmfQueueNext(info.serial);
        responseVoid(info);
    }

    /**
     * Response for request 'switchWaitingOrHoldingAndActiveResponse'
     * @param responseInfo Response info containing response type, serial no. and error
     */
    @Override
    public void switchWaitingOrHoldingAndActiveResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /// MTK Proprietary Interfaces are as below =========================================

    /**
     * Response as StringArrayList
     * @param ril
     * @param responseInfo
     * @param strings
     */
    static void responseStringArrayList(ImsRILAdapter ril,
                                        RadioResponseInfo responseInfo,
                                        ArrayList<String> strings) {

        RILRequest rr = ril.processResponse(responseInfo);
        if (rr != null) {
            String[] ret = null;
            if (responseInfo.error == RadioError.NONE) {
                ret = new String[strings.size()];
                for (int i = 0; i < strings.size(); i++) {
                    ret[i] = strings.get(i);
                }
                sendMessageResponse(rr.mResult, ret);
            }
            ril.processResponseDone(rr, responseInfo, ret);
        }
    }

    /// IRadio Extension APIs Below =====================================================

    /**
     * Response for request 'videoCallAccept'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void videoCallAcceptResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * Response for request 'imsEctCommand'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void imsEctCommandResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * Response for request 'holdCall'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void holdCallResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'resumeCall'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void resumeCallResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'setCallIndication'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void setCallIndicationResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'imsDeregNotification'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void imsDeregNotificationResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'setImsEnable'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void setImsEnableResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'setVolteEnable'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void setVolteEnableResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'setWfcEnable'
     * @param info Radio Response Info
     */
    @Override
    public void setWfcEnableResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'setVilteEnable'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void setVilteEnableResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'setViWifiEnable'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void setViWifiEnableResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'setRcsUaEnable'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void setRcsUaEnableResponse(RadioResponseInfo info) {
        responseVoid(info);
    }


    /**
     * Response for request 'setImsVoiceEnable'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void setImsVoiceEnableResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'setImsVideoEnable'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void setImsVideoEnableResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'setImscfg'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void setImscfgResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'setModemImsCfg'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void setModemImsCfgResponse(RadioResponseInfo info, String results) {
        responseString(info, results);
    }

    /**
     * Response for request 'getProvisionValue'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void getProvisionValueResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'setProvisionValue'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void setProvisionValueResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    @Override
    public void setImsCfgFeatureValueResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    @Override
    public void getImsCfgFeatureValueResponse(RadioResponseInfo info, int value) {
        responseInts(info, value);
    }

    @Override
    public void setImsCfgProvisionValueResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    @Override
    public void getImsCfgProvisionValueResponse(RadioResponseInfo info, String value) {
        responseString(info, value);
    }

    @Override
    public void setImsCfgResourceCapValueResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    @Override
    public void getImsCfgResourceCapValueResponse(RadioResponseInfo info, int value) {
        responseInts(info, value);
    }

    /**
     * Response for request 'addImsConferenceCallMember'
     * @param info Response info containing response type, serial no. and error
     * @param participant Participant
     */
    @Override
    public void addImsConferenceCallMemberResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'removeImsConferenceCallMemberResponse'
     * @param info Response info containing response type, serial no. and error
     * @param participant Participant
     */
    @Override
    public void removeImsConferenceCallMemberResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'hangupAll'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void hangupAllResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'setWfcProfile'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void setWfcProfileResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'emergencyDial'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void emergencyDialResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'conferenceDial'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void conferenceDialResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'setEccServiceCategory'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void setEccServiceCategoryResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'vtDial'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void vtDialResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'vtDialWithSipUri'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void vtDialWithSipUriResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'dialWithSipUri'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void dialWithSipUriResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'sendUssi'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void sendUssiResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'cancelUssi'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void cancelUssiResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'getXcapStatusResponse'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void getXcapStatusResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'resetSuppServResponse'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void resetSuppServResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'setupXcapUserAgentStringResponse'
     * @param info Response info containing response type, serial no. and error
     */
    @Override
    public void setupXcapUserAgentStringResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'forceReleaseCall'
     * @param info Radio Response Info
     */
    @Override
    public void forceReleaseCallResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'imsBearerActivationDoneResponse'
     * @param info Radio Response Info
     */
    @Override
    public void imsBearerActivationDoneResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'imsBearerDeactivationDoneResponse'
     * @param info Radio Response Info
     */
    @Override
    public void imsBearerDeactivationDoneResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'imsBearerDeactivationDoneResponse'
     * @param info Radio Response Info
     */
    @Override
    public void setImsBearerNotificationResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * Response for request 'imsRtpReport'
     * @param info Radio Response Info
     */
    @Override
    public void setImsRtpReportResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    @Override
    public void setImsRegistrationReportResponse(RadioResponseInfo info) {
       responseVoid(info);
    }

    /* MTK SS Feature : Start */

    /**
     *
     * @param info Response info struct containing response type, serial no. and error
     * @param response 0 is the TS 27.007 service class bit vector of
     *        services for which the specified barring facility
     *        is active. "0" means "disabled for all"
     */
    @Override
    public void getFacilityLockForAppResponse(RadioResponseInfo info, int resp) {
        responseInts(info, resp);
    }

    /**
     *
     * @param info Response info struct containing response type, serial no. and error
     * @param retry 0 is the number of retries remaining, or -1 if unknown
     */
    @Override
    public void setFacilityLockForAppResponse(RadioResponseInfo info, int retry) {
        responseInts(info, retry);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     */
    @Override
    public void setCallForwardResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param callForwardInfos points to a vector of CallForwardInfo, one for
     *        each distinct registered phone number.
     */
    @Override
    public void getCallForwardStatusResponse(RadioResponseInfo info,
            ArrayList<android.hardware.radio.V1_0.CallForwardInfo> callForwardInfos) {
        responseCallForwardInfo(info, callForwardInfos);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param enable If current call waiting state is disabled, enable = false else true
     * @param serviceClass If enable, then callWaitingResp[1]
     *        must follow, with the TS 27.007 service class bit vector of services
     *        for which call waiting is enabled.
     *        For example, if callWaitingResp[0] is 1 and
     *        callWaitingResp[1] is 3, then call waiting is enabled for data
     *        and voice and disabled for everything else.
     */
    @Override
    public void getCallWaitingResponse(RadioResponseInfo info, boolean enable, int serviceClass) {
        responseInts(info, enable ? 1 : 0, serviceClass);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     */
    @Override
    public void setCallWaitingResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param n is "n" parameter from TS 27.007 7.7
     * @param m is "m" parameter from TS 27.007 7.7
     */
    @Override
    public void getClirResponse(RadioResponseInfo info, int n, int m) {
        responseInts(info, n, m);
    }

    @Override
    public void setVoiceDomainPreferenceResponse(RadioResponseInfo info) {

       responseVoid(info);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     */
    @Override
    public void setClirResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     *
     * @param info Response info struct containing response type, serial no. and error
     * @param status indicates CLIP status
     */
    @Override
    public void getClipResponse(RadioResponseInfo info, int status) {
        responseInts(info, status);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     */
    @Override
    public void setClipResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param status indicates COLR status. "0" means not provisioned, "1" means provisioned,
     *        "2" means unknown
     */
    @Override
    public void getColrResponse(RadioResponseInfo info, int status) {
        responseInts(info, status);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     */
    @Override
    public void setColrResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param n Colp status in network, "0" means disabled, "1" means enabled
     * @param m Service status, "0" means not provisioned, "1" means provisioned in permanent mode
     */
    @Override
    public void getColpResponse(RadioResponseInfo info, int n, int m) {
        responseInts(info, n, m);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     */
    @Override
    public void setColpResponse(RadioResponseInfo info) {
        responseVoid(info);
    }

    @Override
    public void queryCallForwardInTimeSlotStatusResponse(RadioResponseInfo responseInfo,
                                                         ArrayList<CallForwardInfoEx>
                                                         callForwardInfoExs) {
        responseCallForwardInfoEx(responseInfo, callForwardInfoExs);
    }

    @Override
    public void setCallForwardInTimeSlotResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    @Override
    public void runGbaAuthenticationResponse(RadioResponseInfo responseInfo,
                                             ArrayList<String> resList) {

        responseStringArrayList(mRil, responseInfo, resList);
    }
    /* MTK SS Feature : End */

    /// Protected Methods ===============================================================

    @Override
    protected void riljLoge(String msg) {
        mRil.riljLoge(msg);
    }

    /// Private Methods =================================================================

    private void responseCallForwardInfo(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.CallForwardInfo> callForwardInfos) {

        RILRequest rr = mRil.processResponse(responseInfo);
        if (rr != null) {
            CallForwardInfo[] ret = new CallForwardInfo[callForwardInfos.size()];
            for (int i = 0; i < callForwardInfos.size(); i++) {
                ret[i] = new CallForwardInfo();
                ret[i].status = callForwardInfos.get(i).status;
                ret[i].reason = callForwardInfos.get(i).reason;
                ret[i].serviceClass = callForwardInfos.get(i).serviceClass;
                ret[i].toa = callForwardInfos.get(i).toa;
                ret[i].number = callForwardInfos.get(i).number;
                ret[i].timeSeconds = callForwardInfos.get(i).timeSeconds;
            }
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCallForwardInfoEx(RadioResponseInfo responseInfo,
                                           ArrayList<CallForwardInfoEx> callForwardInfoExs) {

        long[] timeSlot;
        String[] timeSlotStr;
        // process response in Ims RIL
        RILRequest rr = mRil.processResponse(responseInfo);
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
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }


    /**
     * Send a void response message
     * @param responseInfo
     */
    private void responseVoid(RadioResponseInfo responseInfo) {

        RILRequest rr = mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = null;
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * Send a string response message
     * @param responseInfo
     * @param str
     */
    private void responseString(RadioResponseInfo responseInfo, String str) {

        RILRequest rr = mRil.processResponse(responseInfo);
        if (rr != null) {
            String ret = null;
            if (responseInfo.error == RadioError.NONE) {
                ret = str;
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * Send a ints response message
     * @param responseInfo
     * @param ...var
     */
    public void responseInts(RadioResponseInfo responseInfo, int ...var) {
        final ArrayList<Integer> ints = new ArrayList<>();
        for (int i = 0; i < var.length; i++) {
            ints.add(var[i]);
        }
        responseIntArrayList(responseInfo, ints);
    }

    public void responseIntArrayList(RadioResponseInfo responseInfo, ArrayList<Integer> var) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            int[] ret = new int[var.size()];
            for (int i = 0; i < var.size(); i++) {
                ret[i] = var.get(i);
            }
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * Send a last call fail cause response
     * @param responseInfo
     * @param info
     */
    private void responseFailCause(RadioResponseInfo responseInfo,
                                   LastCallFailCauseInfo info) {

        RILRequest rr = mRil.processResponse(responseInfo);
        if (rr != null) {
            LastCallFailCause failCause = null;
            if (responseInfo.error == RadioError.NONE) {
                failCause = new LastCallFailCause();
                failCause.causeCode = info.causeCode;
                failCause.vendorCause = info.vendorCause;
                sendMessageResponse(rr.mResult, failCause);
            }
            mRil.processResponseDone(rr, responseInfo, failCause);
        }
    }

    // SMS-START
    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to sms sent as defined by SendSmsResult in types.hal
     */
    public void sendImsSmsExResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            SmsResponse ret = new SmsResponse(sms.messageRef, sms.ackPDU, sms.errorCode);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acknowledgeLastIncomingGsmSmsExResponse (
            RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void acknowledgeLastIncomingCdmaSmsExResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }
    // SMS-END
}
