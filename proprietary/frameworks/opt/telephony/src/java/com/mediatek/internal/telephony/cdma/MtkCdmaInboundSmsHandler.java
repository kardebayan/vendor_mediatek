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
package com.mediatek.internal.telephony.cdma;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsCbMessage;

import com.android.internal.telephony.BlockChecker;
import com.android.internal.telephony.InboundSmsHandler;
import com.android.internal.telephony.InboundSmsTracker;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.cdma.CdmaInboundSmsHandler;
import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.cdma.SmsMessage;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import com.android.internal.util.HexDump;

import com.mediatek.internal.telephony.IMtkConcatenatedSmsFwk;
import com.mediatek.internal.telephony.MtkConcatenatedSmsFwk;
import com.mediatek.internal.telephony.MtkInboundSmsTracker;
import com.mediatek.internal.telephony.MtkProxyController;
import com.mediatek.internal.telephony.MtkTimerRecord;
import com.mediatek.internal.telephony.MtkWapPushOverSms;
import com.mediatek.internal.telephony.util.MtkSmsCommonUtil;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import mediatek.telephony.MtkTelephony;

/**
 * Sub class to enhance AOSP class CdmaInboundSmsHandler.
 */
public class MtkCdmaInboundSmsHandler extends CdmaInboundSmsHandler {
    private static final String TAG = "MtkCdmaMtSms";
    protected Object mRawLock = new Object();
    private static final boolean VDBG = false;
    private static final boolean ENG = "eng".equals(Build.TYPE);
    private static final int TELESERVICE_REG_SMS_CT = 0xFDED;
    private static final int WAKE_LOCK_TIMEOUT = 500;
    private MtkConcatenatedSmsFwk mConcatenatedSmsFwk = null;
    private static final int RESULT_SMS_REJECT_BY_PPL = 0;
    private static final int RESULT_SMS_ACCEPT_BY_PPL = 1;
    private static final String SMS_CONCAT_WAIT_PROPERTY = "ro.vendor.mtk_sms_concat_wait_support";

    /**
     * The constructor to contruct the Mediatek's CdmaInboundSmsHandler.
     *
     * @param context the context of the phone process
     * @param storageMonitor the object the the SmsStorageMonitor
     * @param phone the object the Phone
     * @param smsDispatcher the object of the CdmaSMSDispatcher
     */
    public MtkCdmaInboundSmsHandler(Context context, SmsStorageMonitor storageMonitor,
            Phone phone, CdmaSMSDispatcher smsDispatcher) {
        super("MtkCdmaInboundSmsHandler", context, storageMonitor, phone, smsDispatcher);

        mDefaultState = new MtkDefaultState();
        mStartupState = new MtkStartupState();
        mIdleState = new MtkIdleState();
        mDeliveringState = new MtkDeliveringState();
        mWaitingState = new MtkWaitingState();

        addState(mDefaultState);
        addState(mStartupState, mDefaultState);
        addState(mIdleState, mDefaultState);
        addState(mDeliveringState, mDefaultState);
        addState(mWaitingState, mDeliveringState);

        setInitialState(mStartupState);

        if (SystemProperties.get(SMS_CONCAT_WAIT_PROPERTY).equals("1")) {
            mConcatenatedSmsFwk = new MtkConcatenatedSmsFwk(context);
            mConcatenatedSmsFwk.setPhoneId(mPhone.getPhoneId());
        }
    }

    /**
     * This parent state throws an exception (for debug builds) or prints an error for unhandled
     * message types.
     */
    private class MtkDefaultState extends InboundSmsHandler.DefaultState {
        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                default:
                    return super.processMessage(msg);
            }
        }
    }

    /**
     * The Startup state waits for {@link SmsBroadcastUndelivered} to process the raw table and
     * notify the state machine to broadcast any complete PDUs that might not have been broadcast.
     */
    private class MtkStartupState extends InboundSmsHandler.StartupState {
        @Override
        public boolean processMessage(Message msg) {
            log("StartupState.processMessage:" + msg.what);
            switch (msg.what) {
                case IMtkConcatenatedSmsFwk.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS:
                    deferMessage(msg);
                    return HANDLED;

                default:
                    // let DefaultState handle these unexpected message types
                    return super.processMessage(msg);
            }
        }
    }

    /**
     * In the idle state the wakelock is released until a new SM arrives, then we transition
     * to Delivering mode to handle it, acquiring the wakelock on exit.
     */
    private class MtkIdleState extends InboundSmsHandler.IdleState {
        @Override
        public boolean processMessage(Message msg) {
            log("IdleState.processMessage:" + msg.what);
            if (DBG) log("Idle state processing message type " + msg.what);
            switch (msg.what) {
                case IMtkConcatenatedSmsFwk.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS:
                    deferMessage(msg);
                    transitionTo(mDeliveringState);
                    return HANDLED;

                default:
                    // let DefaultState handle these unexpected message types
                    return super.processMessage(msg);
            }
        }
    }

    /**
     * In the delivering state, the inbound SMS is processed and stored in the raw table.
     * The message is acknowledged before we exit this state. If there is a message to broadcast,
     * transition to {@link WaitingState} state to send the ordered broadcast and wait for the
     * results. When all messages have been processed, the halting state will release the wakelock.
     */
    private class MtkDeliveringState extends InboundSmsHandler.DeliveringState {
        @Override
        public boolean processMessage(Message msg) {
            log("DeliveringState.processMessage:" + msg.what);
            switch (msg.what) {
                case IMtkConcatenatedSmsFwk.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS:
                    if (dispatchConcateSmsParts((MtkTimerRecord)msg.obj)) {
                        transitionTo(mWaitingState);
                    } else {
                        //Error handle for dispatchConcateSmsParts, transition to Idle
                        loge("Unexpected result for dispatching SMS segments");
                        sendMessage(EVENT_RETURN_TO_IDLE);
                    }
                    return HANDLED;
                default:
                    // let DefaultState handle these unexpected message types
                    return super.processMessage(msg);
            }
        }
    }

    /**
     * The waiting state delegates handling of new SMS to parent {@link DeliveringState}, but
     * defers handling of the {@link #EVENT_BROADCAST_SMS} phase until after the current
     * result receiver sends {@link #EVENT_BROADCAST_COMPLETE}. Before transitioning to
     * {@link DeliveringState}, {@link #EVENT_RETURN_TO_IDLE} is sent to transition to
     * {@link IdleState} after any deferred {@link #EVENT_BROADCAST_SMS} messages are handled.
     */
    private class MtkWaitingState extends InboundSmsHandler.WaitingState {
        @Override
        public boolean processMessage(Message msg) {
            log("WaitingState.processMessage:" + msg.what);
            switch (msg.what) {
                case IMtkConcatenatedSmsFwk.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS:
                    // defer until the current broadcast completes
                    deferMessage(msg);
                    return HANDLED;
                default:
                    // parent state handles the other message types
                    return super.processMessage(msg);
            }
        }
    }



    @Override
    public void dispatchIntent(Intent intent, String permission, int appOp,
        Bundle opts, BroadcastReceiver resultReceiver, UserHandle user) {
        intent.putExtra("rTime", System.currentTimeMillis());
        super.dispatchIntent(intent, permission, appOp, opts, resultReceiver, user);
    }

    @Override
    protected int addTrackerToRawTableAndSendMessage(InboundSmsTracker tracker, boolean deDup) {
        MtkInboundSmsTracker t = (MtkInboundSmsTracker) tracker;
        t.setSubId(mPhone.getSubId());
        return super.addTrackerToRawTableAndSendMessage(tracker, deDup);
    }

    @Override
    protected void deleteFromRawTable(String deleteWhere, String[] deleteWhereArgs,
            int deleteType) {
        Uri uri = deleteType == DELETE_PERMANENTLY ? sRawUriPermanentDelete : sRawUri;
        if (deleteWhere == null && deleteWhereArgs == null) {
            //the contentresolver design is to delete all in table in this case.
            //in this case, means we don't want to delete anything, so return to avoid it
            loge("No rows need be deleted from raw table!");
            return;
        }
        super.deleteFromRawTable(deleteWhere, deleteWhereArgs, deleteType);
    }

    @Override
    protected boolean processMessagePart(InboundSmsTracker tracker) {
        int messageCount = tracker.getMessageCount();
        byte[][] pdus;
        int destPort = tracker.getDestPort();
        boolean block = false;

        if (messageCount == 1) {
            // single-part message
            pdus = new byte[][]{tracker.getPdu()};
            block = BlockChecker.isBlocked(mContext, tracker.getDisplayAddress());
        } else {
            // To lock the raw table of sms database
            synchronized (mRawLock) {
                // multi-part message
                Cursor cursor = null;
                try {
                    // used by several query selection arguments
                    String address = tracker.getAddress();
                    String refNumber = Integer.toString(tracker.getReferenceNumber());
                    String count = Integer.toString(tracker.getMessageCount());
                    // MTK-START
                    String subId = Integer.toString(mPhone.getSubId());
                    // MTK-END

                    // query for all segments and broadcast message if we have all the parts
                    // MTK-START
                    String where = MtkSmsCommonUtil.SELECT_BY_REFERENCE
                            + IMtkConcatenatedSmsFwk.SQL_3GPP2_SMS;
                    String[] whereArgs = {address, refNumber, count, subId};
                    // MTK-END
                    cursor = mResolver.query(sRawUri, PDU_SEQUENCE_PORT_PROJECTION,
                            where, whereArgs, null);


                    int cursorCount = cursor.getCount();
                    if (cursorCount < messageCount) {
                        // Wait for the other message parts to arrive.
                        // It's also possible for the last
                        // segment to arrive before processing the EVENT_BROADCAST_SMS
                        // for one of the earlier segments.
                        // In that case, the broadcast will be sent as soon as all
                        // segments are in the table,
                        // and any later EVENT_BROADCAST_SMS messages will
                        // get a row count of 0 and return.
                        // Refresh the timer if receive another new concatenated segments but not
                        // finish
                        if ((tracker.getIndexOffset() == 1 && tracker.getDestPort() == -1) &&
                                (mConcatenatedSmsFwk != null)) {
                            if (ENG) {
                                log("MtkConcatenatedSmsFwk: refresh timer, ref = " +
                                        tracker.getReferenceNumber());
                            }
                            MtkTimerRecord record =
                                    (MtkTimerRecord) mConcatenatedSmsFwk.queryTimerRecord(
                                    tracker.getAddress(), tracker.getReferenceNumber(),
                                    tracker.getMessageCount(), true);
                            if (record == null) {
                                if (ENG) {
                                    log("MtkConcatenatedSmsFwk: fail to " +
                                            "get TimerRecord to refresh timer");
                                }
                            } else {
                                mConcatenatedSmsFwk.refreshTimer(getHandler(), record);
                            }
                        }
                        return false;
                    }

                    if ((tracker.getIndexOffset() == 1 && tracker.getDestPort() == -1) &&
                            (mConcatenatedSmsFwk != null)) {
                        // cancel the timer, because all segments are in place
                        if (ENG) {
                            log("MtkConcatenatedSmsFwk: cancel timer, ref = " +
                                    tracker.getReferenceNumber());
                        }
                        MtkTimerRecord record =
                                (MtkTimerRecord) mConcatenatedSmsFwk.queryTimerRecord(
                                tracker.getAddress(), tracker.getReferenceNumber(),
                                tracker.getMessageCount(), true);
                        if (record == null) {
                            if (ENG) {
                                log("MtkConcatenatedSmsFwk: fail to " +
                                        "get TimerRecord to cancel timer");
                            }
                        } else {
                            mConcatenatedSmsFwk.cancelTimer(getHandler(), record);
                        }
                    }

                    // All the parts are in place, deal with them
                    pdus = new byte[messageCount][];
                    while (cursor.moveToNext()) {
                        // subtract offset to convert sequence to 0-based array index
                        int index = cursor.getInt(PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING
                                .get(SEQUENCE_COLUMN)) - tracker.getIndexOffset();

                        pdus[index] = HexDump.hexStringToByteArray(cursor.getString(
                                PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(PDU_COLUMN)));

                        // Read the destination port from the first segment
                        // (needed for CDMA WAP PDU).
                        // It's not a bad idea to prefer the port from
                        // the first segment in other cases.
                        if (index == 0 && !cursor.isNull(PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING
                                .get(DESTINATION_PORT_COLUMN))) {
                            int port = cursor.getInt(PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING
                                    .get(DESTINATION_PORT_COLUMN));
                            // strip format flags and convert to real port number, or -1
                            port = InboundSmsTracker.getRealDestPort(port);
                            if (port != -1) {
                                destPort = port;
                            }
                        }
                        // check if display address should be blocked or not
                        if (!block) {
                            // Depending on the nature of the gateway,
                            // the display origination address
                            // is either derived from the content of the SMS TP-OA field,
                            // or the TP-OA field contains a generic gateway address and
                            // the from address is added at the beginning in the message body.
                            // In that case only the first SMS (part of Multi-SMS) comes with
                            // the display originating address which could be used for block
                            // checking purpose.
                            block = BlockChecker.isBlocked(mContext,
                                    cursor.getString(PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING
                                            .get(DISPLAY_ADDRESS_COLUMN)));
                        }
                    }
                } catch (SQLException e) {
                    loge("Can't access multipart SMS database", e);
                    return false;
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            // MTK-START
            }
            // MTK-END
        }

        // Do not process null pdu(s). Check for that and return false in that case.
        List<byte[]> pduList = Arrays.asList(pdus);
        if (pduList.size() == 0 || pduList.contains(null)) {
            loge("processMessagePart: returning false due to " +
                    (pduList.size() == 0 ? "pduList.size() == 0" : "pduList.contains(null)"));
            return false;
        }

        SmsBroadcastReceiver resultReceiver = new SmsBroadcastReceiver(tracker);

        if (!mUserManager.isUserUnlocked()) {
            return processMessagePartWithUserLocked(tracker, pdus, destPort, resultReceiver);
        }

        if (destPort == SmsHeader.PORT_WAP_PUSH) {
            // Build up the data stream
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (byte[] pdu : pdus) {
                // 3GPP needs to extract the User Data from the PDU; 3GPP2 has already done this
                if (!tracker.is3gpp2()) {
                    mediatek.telephony.MtkSmsMessage msg =
                            mediatek.telephony.MtkSmsMessage.createFromPdu(pdu,
                            SmsConstants.FORMAT_3GPP);
                    if (msg != null) {
                        pdu = msg.getUserData();
                    } else {
                        loge("processMessagePart: SmsMessage.createFromPdu returned null");
                        return false;
                    }
                }
                output.write(pdu, 0, pdu.length);
            }
            // MTK-START
            //int result = mWapPush.dispatchWapPdu(output.toByteArray(), resultReceiver, this);
            //if (DBG) log("dispatchWapPdu() returned " + result);
            int result;
            // Put the extra information on bundle
            if (MtkSmsCommonUtil.isWapPushSupport()) {
                log("dispatch wap push pdu with addr & sc addr");
                Bundle bundle = new Bundle();
                if (!(((MtkInboundSmsTracker) tracker).is3gpp2WapPdu())) {
                    mediatek.telephony.MtkSmsMessage sms =
                            mediatek.telephony.MtkSmsMessage.createFromPdu(
                            pdus[0], tracker.getFormat());
                    if (sms != null) {
                        bundle.putString(MtkTelephony.WapPush.ADDR, sms.getOriginatingAddress());
                        String sca = sms.getServiceCenterAddress();
                        if (sca == null) {
                            /* null for app is not a item, it needs to transfer to empty string */
                            sca = "";
                        }
                        bundle.putString(MtkTelephony.WapPush.SERVICE_ADDR, sca);
                    }
                } else {
                    //for CDMA, all info has been parsed into tracker before
                    bundle.putString(MtkTelephony.WapPush.ADDR, tracker.getAddress());
                    bundle.putString(MtkTelephony.WapPush.SERVICE_ADDR, "");
                }

                result = ((MtkWapPushOverSms) mWapPush).dispatchWapPdu(output.toByteArray(),
                        resultReceiver, this, bundle);
            } else {
                //int result = mWapPush.dispatchWapPdu(output.toByteArray(), resultReceiver, this);
                log("dispatch wap push pdu");
                result = mWapPush.dispatchWapPdu(output.toByteArray(), resultReceiver, this);
            }
            if (DBG) {
                log("dispatchWapPdu() returned " + result);
            }
            // MTK-END

            // result is Activity.RESULT_OK if an ordered broadcast was sent
            if (result == Activity.RESULT_OK) {
                return true;
            } else {
                deleteFromRawTable(tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(),
                        MARK_DELETED);
                return false;
            }
        }

        if (block) {
            deleteFromRawTable(tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(),
                    DELETE_PERMANENTLY);
            return false;
        }

        if (checkPplPermission(pdus, tracker.getFormat()) != RESULT_SMS_ACCEPT_BY_PPL) {
            log("The message was blocked by Ppl!");
            deleteFromRawTable(tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(),
                    DELETE_PERMANENTLY);
            return false;
        }

        boolean filterInvoked = filterSms(
            pdus, destPort, tracker, resultReceiver, true /* userUnlocked */);

        if (!filterInvoked) {
            dispatchSmsDeliveryIntent(pdus, tracker.getFormat(), destPort, resultReceiver);
        }

        return true;
    }

    @Override
    protected Cursor onGetDupCursor(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        return mResolver.query(uri, projection, selection,
                    selectionArgs, sortOrder);
    }

    @Override
    protected int addTrackerToRawTable(InboundSmsTracker tracker, boolean deDup) {
        synchronized (mRawLock) {
            if (deDup) {
                try {
                    if (duplicateExists(tracker)) {
                        return Intents.RESULT_SMS_DUPLICATED;   // reject message
                    }
                } catch (SQLException e) {
                    loge("Can't access SMS database", e);
                    return Intents.RESULT_SMS_GENERIC_ERROR;    // reject message
                }
            } else {
                logd("Skipped message de-duping logic");
            }

            // check whether the message is the first segment of one
            // concatenated sms
            boolean isFirstSegment = false;
            // check whether the message is the first segment of one
            // concatenated sms
            if ((tracker.getReferenceNumber() != -1) &&
                    (mConcatenatedSmsFwk != null)) {
                isFirstSegment = mConcatenatedSmsFwk.isFirstConcatenatedSegment(
                        tracker.getAddress(), tracker.getReferenceNumber(), true);
            }

            String address = tracker.getAddress();
            String refNumber = Integer.toString(tracker.getReferenceNumber());
            String count = Integer.toString(tracker.getMessageCount());
            ContentValues values = tracker.getContentValues();

            if (ENG) {
                log("adding content values to raw table: " + values.toString());
            }
            Uri newUri = mResolver.insert(sRawUri, values);
            if (DBG) {
                log("URI of new row -> " + newUri);
            }

            // Start a timer when the short message is not a CDMA-wap-push && not a data SMS
            // && it is the first segment
            if (tracker.getIndexOffset() == 1 && tracker.getDestPort() == -1 &&
                    isFirstSegment == true && (mConcatenatedSmsFwk != null)) {
                if (ENG) {
                    log("MtkConcatenatedSmsFwk: start a new timer, the first segment, ref = " +
                            tracker.getReferenceNumber());
                }
                MtkTimerRecord record = new MtkTimerRecord(tracker.getAddress(),
                        tracker.getReferenceNumber(), tracker.getMessageCount(), tracker);
                mConcatenatedSmsFwk.startTimer(getHandler(), record);
            }

            try {
                long rowId = ContentUris.parseId(newUri);
                if (tracker.getMessageCount() == 1) {
                    // set the delete selection args for single-part message
                    tracker.setDeleteWhere(SELECT_BY_ID, new String[]{Long.toString(rowId)});
                } else {
                    // set the delete selection args for multi-part message
                    MtkInboundSmsTracker t = (MtkInboundSmsTracker) tracker;
                    String subId = Integer.toString(t.getSubId());
                    String[] deleteWhereArgs = {address, refNumber, count, subId};
                    tracker.setDeleteWhere(MtkSmsCommonUtil.SELECT_BY_REFERENCE
                            + IMtkConcatenatedSmsFwk.SQL_3GPP2_SMS,
                            deleteWhereArgs);
                }
                return Intents.RESULT_SMS_HANDLED;
            } catch (Exception e) {
                loge("error parsing URI for new row: " + newUri, e);
                return Intents.RESULT_SMS_GENERIC_ERROR;
            }
        }
    }

    @Override
    protected int dispatchMessageRadioSpecific(SmsMessageBase smsb) {
        SmsMessage sms = (SmsMessage) smsb;
        sms = MtkSmsMessage.newMtkSmsMessage(sms);
        int ret = super.dispatchMessageRadioSpecific(sms);
        if (ret == Intents.RESULT_SMS_UNSUPPORTED) {
            int teleService = sms.getTeleService();
            if (teleService == TELESERVICE_REG_SMS_CT && sms.getPdu() != null) {
                handleAutoRegMessage(sms.getPdu());
                return Intents.RESULT_SMS_HANDLED;
            }
        }
        return ret;
    }

    private void handleAutoRegMessage(byte[] pdu) {
        ((MtkProxyController) ProxyController.getInstance()).
                getDeviceRegisterController().handleAutoRegMessage(pdu);
    }

    protected boolean dispatchConcateSmsParts(MtkTimerRecord record) {
        boolean handled = false;

        if (record == null) {
            if (ENG) {
                log("ConcatenatedSmsFwkExt: null TimerRecord in msg");
            }
            return false;
        }
        if (ENG) {
            log("ConcatenatedSmsFwkExt: timer is expired, dispatch existed segments. refNumber = "
                    + record.refNumber);
        }

        // create null tracker for FSM flow
        MtkInboundSmsTracker smsTracker = (MtkInboundSmsTracker) record.mTracker;
        SmsBroadcastReceiver receiver = new SmsBroadcastReceiver(smsTracker);

        synchronized (mRawLock) {
            byte[][] pdus = mConcatenatedSmsFwk.queryExistedSegments(record);

            // Do not process null pdu(s). Check for that and return false in that case.
            List<byte[]> pduList = Arrays.asList(pdus);
            if (pduList.size() == 0 || pduList.contains(null)) {
                loge("dispatchConcateSmsParts: returning false due to " +
                        (pduList.size() == 0 ? "pduList.size() == 0" : "pduList.contains(null)"));
                return false;
            }

            // Check user unlock
            if (!mUserManager.isUserUnlocked()) {
                log("dispatchConcateSmsParts: device is still locked so delete segment(s), ref = "
                        + record.refNumber);
                mConcatenatedSmsFwk.deleteExistedSegments(record);
                return processMessagePartWithUserLocked(smsTracker, pdus, -1, receiver);
            }

            // Check block number
            if (BlockChecker.isBlocked(mContext, smsTracker.getAddress())) {
                log("dispatchConcateSmsParts: block phone number, number = "
                        + smsTracker.getAddress());
                mConcatenatedSmsFwk.deleteExistedSegments(record);
                deleteFromRawTable(smsTracker.getDeleteWhere(), smsTracker.getDeleteWhereArgs(),
                        DELETE_PERMANENTLY);
                return false;
            }

            if (checkPplPermission(pdus, smsTracker.getFormat()) != RESULT_SMS_ACCEPT_BY_PPL) {
                log("The message was blocked by Ppl!");
                deleteFromRawTable(smsTracker.getDeleteWhere(), smsTracker.getDeleteWhereArgs(),
                        DELETE_PERMANENTLY);
                return false;
            }

            if (pdus != null && pdus.length > 0) {
                boolean filterInvoked = filterSms(
                    pdus, smsTracker.getDestPort(), smsTracker, receiver, true);

                if (!filterInvoked) {
                    dispatchSmsDeliveryIntent(pdus, smsTracker.getFormat(),
                            smsTracker.getDestPort(),
                            receiver);
                }
                handled = true;
            } else {
                if (ENG) {
                    log("ConcatenatedSmsFwkExt: no pdus to be dispatched");
                }
            }
            if (ENG) {
                log("ConcatenatedSmsFwkExt: delete segment(s), tracker = " +
                        (InboundSmsTracker) record.mTracker);
            }
            mConcatenatedSmsFwk.deleteExistedSegments(record);
        }

        return handled;
    }

    /**
     * Phone Privacy Lock intent handler.
     * To handle the intent that send from sms finite state machine.
     */
    private int checkPplPermission(byte[][] pdus, String format) {
        int result = RESULT_SMS_ACCEPT_BY_PPL;
        if ((is3gpp2() && (format.compareTo(SmsConstants.FORMAT_3GPP2) == 0)) ||
            (!is3gpp2() && (format.compareTo(SmsConstants.FORMAT_3GPP) == 0))) {
            if (MtkSmsCommonUtil.phonePrivacyLockCheck(pdus, format, mContext, mPhone.getSubId())
                    != PackageManager.PERMISSION_GRANTED) {
                result = RESULT_SMS_REJECT_BY_PPL;
            }
        }
        return result;
    }
}

