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


package com.mediatek.internal.telephony.gsm;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Message;
import android.os.SystemProperties;
import android.os.PowerManager;
import android.os.UserManager;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.provider.Telephony.Sms.Intents;
import android.provider.Telephony;
import android.service.carrier.ICarrierMessagingService;
import android.service.carrier.CarrierMessagingService;

import android.telephony.SubscriptionManager;
import android.telephony.SmsManager;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.InboundSmsHandler;
import com.android.internal.telephony.InboundSmsTracker;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.BlockChecker;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UsimServiceTable;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import com.android.internal.telephony.gsm.SmsMessage;

import com.mediatek.internal.telephony.MtkInboundSmsTracker;
import com.mediatek.internal.telephony.MtkProxyController;
import com.mediatek.internal.telephony.MtkWapPushOverSms;
import com.mediatek.internal.telephony.IMtkDupSmsFilter;
import com.mediatek.internal.telephony.IMtkConcatenatedSmsFwk;
import com.mediatek.internal.telephony.MtkTimerRecord;
import com.mediatek.internal.telephony.MtkSmsDispatchersController;
import com.mediatek.internal.telephony.util.MtkSmsCommonUtil;
import com.mediatek.internal.telephony.OpTelephonyCustomizationFactoryBase;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
import com.mediatek.internal.telephony.ppl.PplSmsFilterExtension;

import mediatek.telephony.MtkTelephony;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.android.internal.util.HexDump;


/**
 * This class broadcasts incoming SMS messages to interested apps after storing them in
 * the SmsProvider "raw" table and ACKing them to the SMSC. After each message has been
 */
public class MtkGsmInboundSmsHandler extends GsmInboundSmsHandler {
    private OpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory = null;
    /** Check if any duplicated SMS */
    private IMtkDupSmsFilter mDupSmsFilter = null;
    /** Concatenated SMS handler. A timer to show concatenated SMS or CT special requirement */
    private IMtkConcatenatedSmsFwk mConcatenatedSmsFwk = null;

    private PplSmsFilterExtension mPplSmsFilter = null;

    private static final int RESULT_SMS_REJECT_BY_PPL = 0;
    private static final int RESULT_SMS_ACCEPT_BY_PPL = 1;

    /** sms database raw table locker */
    protected Object mRawLock = new Object();

    private static final boolean ENG = "eng".equals(Build.TYPE);

    private static final String SMS_CONCAT_WAIT_PROPERTY = "ro.vendor.mtk_sms_concat_wait_support";

    /**
     * Create a new GSM inbound SMS handler.
     */
    public MtkGsmInboundSmsHandler(Context context, SmsStorageMonitor storageMonitor,
            Phone phone) {
        super("MtkGsmInboundSmsHandler-" + phone.getPhoneId(), context, storageMonitor, phone);

        // Create MTK proprietary GsmInboundSmsHandler State
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
        if (DBG) log("created InboundSmsHandler from MtkGsmInboundSmsHandler");
        // Create plug-in
        mTelephonyCustomizationFactory = OpTelephonyCustomizationUtils.getOpFactory(context);
        if (mTelephonyCustomizationFactory != null) {
            // Create duplicate sms plug-in
            mDupSmsFilter = mTelephonyCustomizationFactory.makeMtkDupSmsFilter(context);
            if (SystemProperties.get(SMS_CONCAT_WAIT_PROPERTY).equals("1")) {
                mConcatenatedSmsFwk =
                        mTelephonyCustomizationFactory.makeMtkConcatenatedSmsFwk(context);
            }
        }

        if (mDupSmsFilter != null) {
            mDupSmsFilter.setPhoneId(mPhone.getPhoneId());
            String actualClassName = mDupSmsFilter.getClass().getName();
            log("initial IMtkDupSmsFilter done, actual class name is " + actualClassName);
        } else {
            log("FAIL! intial IMtkDupSmsFilter");
        }

        if (mConcatenatedSmsFwk != null) {
            mConcatenatedSmsFwk.setPhoneId(mPhone.getPhoneId());
            String actualClassName = mConcatenatedSmsFwk.getClass().getName();
            log("initial IMtkConcatenatedSmsFwk done, actual class name is " + actualClassName);
        } else {
            log("FAIL! intial IMtkConcatenatedSmsFwk");
        }
    }

    /**
     * Wait for state machine to enter startup state. We can't send any messages until then.
     */
    public static MtkGsmInboundSmsHandler makeInboundSmsHandler(Context context,
            SmsStorageMonitor storageMonitor, Phone phone) {
        MtkGsmInboundSmsHandler handler = new MtkGsmInboundSmsHandler(
                context, storageMonitor, phone);
        handler.start();
        return handler;
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
            switch (msg.what) {
                case IMtkConcatenatedSmsFwk.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS:
                    log("MtkStartupState.processMessage:" + msg.what);
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
            switch (msg.what) {
                case IMtkConcatenatedSmsFwk.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS:
                    log("MtkIdleState.processMessage:" + msg.what);
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
            switch (msg.what) {
                case IMtkConcatenatedSmsFwk.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS:
                    log("MtkDeliveringState.processMessage:" + msg.what);
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
            switch (msg.what) {
                case IMtkConcatenatedSmsFwk.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS:
                    log("MtkWaitingState.processMessage:" + msg.what);
                    // defer until the current broadcast completes
                    deferMessage(msg);
                    return HANDLED;
                default:
                    // parent state handles the other message types
                    return super.processMessage(msg);
            }
        }
    }

    /**
     * Dispatch a normal incoming SMS. This is called from {@link #dispatchMessageRadioSpecific}
     * if no format-specific handling was required. Saves the PDU to the SMS provider raw table,
     * creates an {@link InboundSmsTracker}, then sends it to the state machine as an
     * {@link #EVENT_BROADCAST_SMS}. Returns {@link Intents#RESULT_SMS_HANDLED} or an error value.
     *
     * @param sms the message to dispatch
     * @return {@link Intents#RESULT_SMS_HANDLED} if the message was accepted, or an error status
     */
    @Override
    protected int dispatchNormalMessage(SmsMessageBase sms) {
        SmsHeader smsHeader = sms.getUserDataHeader();
        MtkInboundSmsTracker tracker;

        if ((smsHeader == null) || (smsHeader.concatRef == null)
                || (smsHeader.concatRef.msgCount == 1)) {
            // Message is not concatenated.
            int destPort = -1;
            if (smsHeader != null && smsHeader.portAddrs != null) {
                // The message was sent to a port.
                destPort = smsHeader.portAddrs.destPort;
                if (DBG) log("destination port: " + destPort);
            }

            tracker = (MtkInboundSmsTracker)(
                    TelephonyComponentFactory.getInstance().makeInboundSmsTracker(sms.getPdu(),
                    sms.getTimestampMillis(), destPort, is3gpp2(), false,
                    sms.getOriginatingAddress(), sms.getDisplayOriginatingAddress(),
                    sms.getMessageBody()));
            tracker.setSubId(mPhone.getSubId());
        } else {
            // Create a tracker for this message segment.
            SmsHeader.ConcatRef concatRef = smsHeader.concatRef;
            SmsHeader.PortAddrs portAddrs = smsHeader.portAddrs;
            int destPort = (portAddrs != null ? portAddrs.destPort : -1);

            tracker = (MtkInboundSmsTracker)(
                    TelephonyComponentFactory.getInstance().makeInboundSmsTracker(sms.getPdu(),
                    sms.getTimestampMillis(), destPort, is3gpp2(), sms.getOriginatingAddress(),
                    sms.getDisplayOriginatingAddress(), concatRef.refNumber, concatRef.seqNumber,
                    concatRef.msgCount, false, sms.getMessageBody()));
            tracker.setSubId(mPhone.getSubId());
        }

        if (ENG) log("created tracker: " + tracker);

        // de-duping is done only for text messages
        // destPort = -1 indicates text messages, otherwise it's a data sms
        return addTrackerToRawTableAndSendMessage(tracker,
                tracker.getDestPort() == -1 /* de-dup if text message */);
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
        MtkInboundSmsTracker smsTracker = (MtkInboundSmsTracker)record.mTracker;
        SmsBroadcastReceiver receiver = new SmsBroadcastReceiver(smsTracker);

        synchronized (mRawLock) {
            byte[][] pdus = mConcatenatedSmsFwk.queryExistedSegments(record);

            // Do not process null pdu(s). Check for that and return false in that case.
            if (pdus != null) {
                List<byte[]> pduList = Arrays.asList(pdus);
                if (pduList.size() == 0 || pduList.contains(null)) {
                    loge("dispatchConcateSmsParts: returning false due to " +
                            (pduList.size() == 0 ?
                            "pduList.size() == 0" : "pduList.contains(null)"));
                    return false;
                }
            } else {
                loge("dispatchConcateSmsParts: there is at least one segment with dest port");
                return false;
            }

            if (checkPplPermission(pdus, smsTracker.getFormat()) != RESULT_SMS_ACCEPT_BY_PPL) {
                log("The message was blocked by Ppl! don't prompt to user");
                deleteFromRawTable(smsTracker.getDeleteWhere(), smsTracker.getDeleteWhereArgs(),
                        DELETE_PERMANENTLY);
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

            if (pdus != null && pdus.length > 0) {
                boolean filterInvoked = filterSms(
                    pdus, smsTracker.getDestPort(), smsTracker, receiver, true);

                if (!filterInvoked) {
                    dispatchSmsDeliveryIntent(pdus, smsTracker.getFormat(),
                            smsTracker.getDestPort(), receiver);
                }
                handled = true;
            } else {
                if (ENG) {
                    log("ConcatenatedSmsFwkExt: no pdus to be dispatched");
                }
            }
            if (ENG) {
                log("ConcatenatedSmsFwkExt: delete segment(s), tracker = " +
                        (InboundSmsTracker)record.mTracker);
            }
            mConcatenatedSmsFwk.deleteExistedSegments(record);
        }

        return handled;
    }

    /**
     * Process the inbound SMS segment. If the message is complete, send it as an ordered
     * broadcast to interested receivers and return true. If the message is a segment of an
     * incomplete multi-part SMS, return false.
     * @param tracker the tracker containing the message segment to process
     * @return true if an ordered broadcast was sent; false if waiting for more message segments
     */
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
                            + IMtkConcatenatedSmsFwk.SQL_3GPP_SMS;
                    String[] whereArgs = {address, refNumber, count, subId};
                    // MTK-END
                    cursor = mResolver.query(sRawUri, PDU_SEQUENCE_PORT_PROJECTION,
                            where, whereArgs, null);


                    int cursorCount = cursor.getCount();
                    if (cursorCount < messageCount) {
                        // Wait for the other message parts to arrive.
                        // It's also possible for the last segment to arrive before processing
                        // the EVENT_BROADCAST_SMS for one of the earlier segments.
                        // In that case, the broadcast will be sent as soon as all
                        // segments are in the table, and any later EVENT_BROADCAST_SMS messages
                        // will get a row count of 0 and return.
                        // Refresh the timer if receive another new concatenated segments but not
                        // finish
                        if (tracker.getIndexOffset() == 1 && tracker.getDestPort() == -1 &&
                                mConcatenatedSmsFwk != null) {
                            if (ENG) {
                                log("MtkConcatenatedSmsFwk: refresh timer, ref = " +
                                        tracker.getReferenceNumber());
                            }
                            MtkTimerRecord record =
                                    (MtkTimerRecord)mConcatenatedSmsFwk.queryTimerRecord(
                                    tracker.getAddress(), tracker.getReferenceNumber(),
                                    tracker.getMessageCount(), false);
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

                    if (tracker.getIndexOffset() == 1 && tracker.getDestPort() == -1 &&
                            mConcatenatedSmsFwk != null) {
                        // cancel the timer, because all segments are in place
                        if (ENG) {
                            log("MtkConcatenatedSmsFwk: cancel timer, ref = " +
                                    tracker.getReferenceNumber());
                        }
                        MtkTimerRecord record =
                                (MtkTimerRecord)mConcatenatedSmsFwk.queryTimerRecord(
                                tracker.getAddress(), tracker.getReferenceNumber(),
                                tracker.getMessageCount(), false);
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

                        // Read the destination port from the first segment (needed for CDMA
                        // WAP PDU). It's not a bad idea to prefer the port from
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
                            // Depending on the nature of the gateway, the display
                            // origination address is either derived from the content of
                            // the SMS TP-OA field, or the TP-OA field contains a generic gateway
                            // address and the from address is added at the beginning
                            // in the message body. In that case onlythe first SMS
                            // (part of Multi-SMS) comes with the display originating address
                            // which could be used for block checking purpose.
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

        if (checkPplPermission(pdus, tracker.getFormat()) != RESULT_SMS_ACCEPT_BY_PPL) {
            log("The message was blocked by Ppl! don't prompt to user");
            deleteFromRawTable(tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(),
                    DELETE_PERMANENTLY);
            return false;
        }

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
                if (!(((MtkInboundSmsTracker)tracker).is3gpp2WapPdu())) {
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

                result = ((MtkWapPushOverSms)mWapPush).dispatchWapPdu(output.toByteArray(),
                        resultReceiver, this, bundle);
            } else {
                //int result = mWapPush.dispatchWapPdu(output.toByteArray(), resultReceiver, this);
                log("dispatch wap push pdu");
                result = mWapPush.dispatchWapPdu(output.toByteArray(), resultReceiver, this);
            }
            if (DBG) log("dispatchWapPdu() returned " + result);
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

        boolean filterInvoked = filterSms(
            pdus, destPort, tracker, resultReceiver, true /* userUnlocked */);

        if (!filterInvoked) {
            dispatchSmsDeliveryIntent(pdus, tracker.getFormat(), destPort, resultReceiver);
        }

        return true;
    }

    /**
     * Handle type zero, SMS-PP data download, and 3GPP/CPHS MWI type SMS. Normal SMS messages
     * are handled by {@link #dispatchNormalMessage} in parent class.
     *
     * @param smsb the SmsMessageBase object from the RIL
     * @return a result code from {@link android.provider.Telephony.Sms.Intents},
     *  or {@link Activity#RESULT_OK} for delayed acknowledgment to SMSC
     */
    @Override
    protected int dispatchMessageRadioSpecific(SmsMessageBase smsb) {
        SmsMessage sms = (SmsMessage) smsb;

        if (sms.getDisplayOriginatingAddress().equals("10659401")) {
            log("handleAutoRegMessage.");
            handleAutoRegMessage(sms.getPdu());
            return Intents.RESULT_SMS_HANDLED;
        }

        return super.dispatchMessageRadioSpecific(smsb);
    }

    private void handleAutoRegMessage(byte[] pdu) {
        ((MtkProxyController) ProxyController.getInstance()).getDeviceRegisterController().
                handleAutoRegMessage(mPhone.getSubId(), SmsConstants.FORMAT_3GPP, pdu);
    }

    /**
     * Dispose of the WAP push object and release the wakelock.
     */
    @Override
    protected void onQuitting() {
        super.onQuitting();
    }

    /**
     * Dispatch the intent with the specified permission, appOp, and result receiver, using
     * this state machine's handler thread to run the result receiver.
     *
     * @param intent the intent to broadcast
     * @param permission receivers are required to have this permission
     * @param appOp app op that is being performed when dispatching to a receiver
     * @param user user to deliver the intent to
     */
    @Override
    public void dispatchIntent(Intent intent, String permission, int appOp,
            Bundle opts, BroadcastReceiver resultReceiver, UserHandle user) {
        intent.putExtra("rTime", System.currentTimeMillis());
        super.dispatchIntent(intent, permission, appOp, opts, resultReceiver, user);
    }

    /**
     * Helper for {@link SmsBroadcastUndelivered} to delete an old message in the raw table.
     */
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

        synchronized (mRawLock) {
            int rows = mResolver.delete(uri, deleteWhere, deleteWhereArgs);
            if (rows == 0) {
                loge("No rows were deleted from raw table!");
            } else if (DBG) {
                log("Deleted " + rows + " rows from raw table.");
            }
        }
    }

    /**
     * Function to check if message should be dropped because same message has already been
     * received. In certain cases it checks for similar messages instead of exact same (cases where
     * keeping both messages in db can cause ambiguity)
     * @return true if duplicate exists, false otherwise
     */
    // MTK-START
    // Modification for sub class
    protected boolean duplicateExists(InboundSmsTracker tracker) throws SQLException {
    // MT-END
        String address = tracker.getAddress();
        // convert to strings for query
        String refNumber = Integer.toString(tracker.getReferenceNumber());
        String count = Integer.toString(tracker.getMessageCount());
        String subId = Integer.toString(mPhone.getSubId());
        // sequence numbers are 1-based except for CDMA WAP, which is 0-based
        int sequence = tracker.getSequenceNumber();
        String seqNumber = Integer.toString(sequence);
        String date = Long.toString(tracker.getTimestamp());
        String messageBody = tracker.getMessageBody();
        String where;
        if (tracker.getMessageCount() == 1) {
            where = "address=? AND reference_number=? AND count=? AND sequence=? AND " +
                    "date=? AND message_body=? AND sub_id=?";
        } else {
            // for multi-part messages, deduping should also be done against undeleted
            // segments that can cause ambiguity when contacenating the segments, that is,
            // segments with same address, reference_number, count and sequence
            where = "address=? AND reference_number=? AND count=? AND sequence=? AND " +
                    "((date=? AND message_body=?) OR deleted=0) AND sub_id=?";
        }

        Cursor cursor = null;
        try {
            // Check for duplicate message segments
            cursor = mResolver.query(sRawUri, PDU_PROJECTION, where,
                    new String[]{address, refNumber, count, seqNumber, date, messageBody, subId},
                    null);

            // moveToNext() returns false if no duplicates were found
            if (cursor != null && cursor.moveToNext()) {
                loge("Discarding duplicate message segment, refNumber=" + refNumber
                        + " seqNumber=" + seqNumber + " count=" + count);
                if (ENG) {
                    loge("address=" + address + " date=" + date + " messageBody=" +
                            messageBody);
                }
                String oldPduString = cursor.getString(PDU_COLUMN);
                byte[] pdu = tracker.getPdu();
                byte[] oldPdu = HexDump.hexStringToByteArray(oldPduString);
                if (!Arrays.equals(oldPdu, tracker.getPdu())) {
                    loge("Warning: dup message segment PDU of length " + pdu.length
                            + " is different from existing PDU of length " + oldPdu.length);
                }
                return true;   // reject message
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return false;
    }

    /**
     * Insert a message PDU into the raw table so we can acknowledge it immediately.
     * If the device crashes before the broadcast to listeners completes, it will be delivered
     * from the raw table on the next device boot. For single-part messages, the deleteWhere
     * and deleteWhereArgs fields of the tracker will be set to delete the correct row after
     * the ordered broadcast completes.
     *
     * @param tracker the tracker to add to the raw table
     * @return true on success; false on failure to write to database
     */
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
            if (tracker.getReferenceNumber() != -1 && mConcatenatedSmsFwk != null) {
                isFirstSegment = mConcatenatedSmsFwk.isFirstConcatenatedSegment(
                        tracker.getAddress(), tracker.getReferenceNumber(), false);
            }

            String address = tracker.getAddress();
            String refNumber = Integer.toString(tracker.getReferenceNumber());
            String count = Integer.toString(tracker.getMessageCount());
            ContentValues values = tracker.getContentValues();

            if (ENG) log("adding content values to raw table: " + values.toString());
            Uri newUri = mResolver.insert(sRawUri, values);
            if (DBG) log("URI of new row -> " + newUri);

            // Start a timer when the short message is not a CDMA-wap-push && not a data SMS
            // && it is the first segment
            if (tracker.getIndexOffset() == 1 && tracker.getDestPort() == -1 &&
                    isFirstSegment == true && mConcatenatedSmsFwk != null) {
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
                    MtkInboundSmsTracker t = (MtkInboundSmsTracker)tracker;
                    String subId = Integer.toString(t.getSubId());
                    String[] deleteWhereArgs = {address, refNumber, count, subId};
                    tracker.setDeleteWhere(MtkSmsCommonUtil.SELECT_BY_REFERENCE
                            + IMtkConcatenatedSmsFwk.SQL_3GPP_SMS,
                            deleteWhereArgs);
                }
                return Intents.RESULT_SMS_HANDLED;
            } catch (Exception e) {
                loge("error parsing URI for new row: " + newUri, e);
                return Intents.RESULT_SMS_GENERIC_ERROR;
            }
        }
    }


    /**
     * Phone Privacy Lock intent handler.
     * To handle the intent that send from sms finite state machine.
     */
    private int checkPplPermission(byte[][] pdus, String format) {
        int result = RESULT_SMS_ACCEPT_BY_PPL;
        if ((is3gpp2() && (format.compareTo(SmsConstants.FORMAT_3GPP2) == 0)) ||
            (!is3gpp2() && (format.compareTo(SmsConstants.FORMAT_3GPP) == 0))) {
            if (phonePrivacyLockCheck(pdus, format) != PackageManager.PERMISSION_GRANTED) {
                result = RESULT_SMS_REJECT_BY_PPL;
            }
        }
        return result;
    }

    /**
     * Phone Privacy Lock check if this MT sms has permission to dispatch
     */
    protected int phonePrivacyLockCheck(byte[][] pdus, String format) {
        int checkResult = PackageManager.PERMISSION_GRANTED;

        if (MtkSmsCommonUtil.isPrivacyLockSupport()) {
            /* CTA-level3 for phone privacy lock */
            if (checkResult == PackageManager.PERMISSION_GRANTED) {
                if (mPplSmsFilter == null) {
                    mPplSmsFilter = new PplSmsFilterExtension(mContext);
                }
                Bundle pplData = new Bundle();

                pplData.putSerializable(mPplSmsFilter.KEY_PDUS, pdus);
                pplData.putString(mPplSmsFilter.KEY_FORMAT, format);
                pplData.putInt(mPplSmsFilter.KEY_SUB_ID, mPhone.getSubId());
                pplData.putInt(mPplSmsFilter.KEY_SMS_TYPE, 0);

                boolean pplResult = false;
                pplResult = mPplSmsFilter.pplFilter(pplData);
                if (ENG) {
                    log("[Ppl] Phone privacy check end, Need to filter(result) = "
                            + pplResult);
                }
                if (pplResult == true) {
                    checkResult = PackageManager.PERMISSION_DENIED;
                }
            }
        }

        return checkResult;
    }
}
