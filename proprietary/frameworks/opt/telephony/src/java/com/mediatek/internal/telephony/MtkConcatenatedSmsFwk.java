/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2017. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.internal.telephony;

import static com.mediatek.internal.telephony.IMtkConcatenatedSmsFwk.UPLOAD_FLAG_TAG;
import static com.mediatek.internal.telephony.IMtkConcatenatedSmsFwk.UPLOAD_FLAG_NEW;
import static com.mediatek.internal.telephony.IMtkConcatenatedSmsFwk.UPLOAD_FLAG_UPDATE;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.InboundSmsTracker;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.util.HexDump;

import java.util.ArrayList;


public class MtkConcatenatedSmsFwk implements IMtkConcatenatedSmsFwk {
    private static final String TAG = "MtkConSmsFwk";
    private static final boolean ENG = "eng".equals(Build.TYPE);
    private static final Uri mRawUri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, "raw");
    private static final String[] CONCATE_PROJECTION = {
        "reference_number",
        "count",
        "sequence"
    };
    private static final String[] PDU_SEQUENCE_PORT_PROJECTION = {
        "pdu",
        "sequence",
        "destination_port",
    };
    private static final String[] PDU_SEQUENCE_PORT_UPLOAD_PROJECTION = {
        "pdu",
        "sequence",
        "destination_port",
        "upload_flag"
    };

    private static final String[] OUT_OF_DATE_PROJECTION = {
        "recv_time",
        "address",
        "reference_number",
        "count",
        "sub_id",
    };

    protected static int DELAYED_TIME = 60 * 1000;
    private static final String MTK_KEY_EMS_WAITING_MISSING_SEGMENT_TIME_INT =
        "mtk_ems_waiting_missing_segment_time";

    private ArrayList<MtkTimerRecord> mMtkTimerRecords = new ArrayList<MtkTimerRecord>(5);
    protected Context mContext = null;
    private ContentResolver mResolver = null;
    protected int mPhoneId = -1;
    private InboundSmsTracker mTracker;

    public MtkConcatenatedSmsFwk(Context context) {
        if (context == null) {
            Rlog.d(TAG, "FAIL! context is null");
            return;
        }
        this.mContext = context;
        this.mResolver = mContext.getContentResolver();

        IntentFilter filter = new IntentFilter(ACTION_CLEAR_OUT_SEGMENTS);
        mContext.registerReceiver(mReceiver, filter, "android.permission.BROADCAST_SMS", null);
    }

    public synchronized void setPhoneId(int phoneId) {
        mPhoneId = phoneId;
    }

    private void registerAlarmManager() {
        return;
    }

    private synchronized void deleteOutOfDateSegments(String address, int refNum, int count,
            int phoneId) {
        try {
            int subId = getSubIdUsingPhoneId();
            String where = "address=? AND reference_number=? AND count=? AND sub_id=?";
            String[] whereArgs = {
                address,
                Integer.toString(refNum),
                Integer.toString(count),
                Integer.toString(subId),
            };
            int numOfDeleted = mResolver.delete(mRawUri, where, whereArgs);
            Rlog.d(TAG, "deleteOutOfDateSegments remove " + numOfDeleted +
                " out of date segments, ref =  " + refNum);
        } catch (SQLException e) {
            Rlog.d(TAG, "FAIL! SQLException");
        }
    }

    private synchronized void checkOutOfDateSegments(boolean is3gpp2) {

        Cursor cursor = null;
        try {
            String where = "sub_id=?";
            int subId = getSubIdUsingPhoneId();
            String[] whereArgs = {Integer.toString(subId)};
            cursor = mResolver.query(mRawUri,
                    OUT_OF_DATE_PROJECTION, where, whereArgs, null);
            if (cursor != null) {
                int columnRecvTime = cursor.getColumnIndex("recv_time");
                int columnAddress = cursor.getColumnIndex("address");
                int columnRefNum = cursor.getColumnIndex("reference_number");
                int columnCount = cursor.getColumnIndex("count");
                int columnSubId = cursor.getColumnIndex("sub_id");

                int cursorCount = cursor.getCount();
                Rlog.d(TAG, "checkOutOfDateSegments cursor size=" + cursorCount +
                        ", phoneId=" + Integer.toString(mPhoneId));
                for (int i = 0; i < cursorCount; ++i) {
                    cursor.moveToNext();
                    long recv_time = cursor.getLong(columnRecvTime);
                    long curr_time = System.currentTimeMillis();

                    MtkTimerRecord tr = (MtkTimerRecord)queryTimerRecord(
                            cursor.getString(columnAddress),cursor.getInt(columnRefNum),
                            cursor.getInt(columnCount), is3gpp2);
                    if (tr != null) {
                        deleteTimerRecord(tr);
                    }
                    if (ENG) {
                        Rlog.d(TAG, "currtime=" + curr_time + ", recv_time=" + recv_time);
                    }
                    if ((curr_time - recv_time) >= OUT_OF_DATE_TIME) {
                    // delete segments which has the same address, reference_number, count & sub_id
                    int phoneId = SubscriptionManager.getPhoneId(cursor.getInt(columnSubId));
                    deleteOutOfDateSegments(cursor.getString(columnAddress),
                            cursor.getInt(columnRefNum),
                            cursor.getInt(columnCount),
                            phoneId);
                    }
                }
            } else {
                Rlog.d(TAG, "FAIL! cursor is null");
            }
        } catch (SQLException e) {
            Rlog.d(TAG, "FAIL! SQLException");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(ACTION_CLEAR_OUT_SEGMENTS)) {
                int id = intent.getIntExtra(PhoneConstants.PHONE_KEY, -1);
                synchronized (MtkConcatenatedSmsFwk.this) {
                    if (id == mPhoneId) {
                        // clear db
                        checkOutOfDateSegments(true);
                        checkOutOfDateSegments(false);
                    }
                }
            }
        }
    };

    public synchronized boolean isFirstConcatenatedSegment(String address, int refNumber,
            boolean is3gpp2) {
        boolean result = false;

        Cursor cursor = null;
        try {
            String where = "address=? AND reference_number=? AND sub_id=?" +
                    "AND deleted=0" + (is3gpp2 ? SQL_3GPP2_SMS : SQL_3GPP_SMS);
            int subId = getSubIdUsingPhoneId();
            String[] whereArgs = new String[] {
                address,
                Integer.toString(refNumber),
                Integer.toString(subId)
            };
            cursor = mResolver.query(mRawUri,
                    CONCATE_PROJECTION, where, whereArgs, null);
            if (cursor != null) {
                int messageCount = cursor.getCount();
                if (messageCount == 0) {
                    result = true;
                }
            } else {
                Rlog.d(TAG, "FAIL! cursor is null");
            }
        } catch (SQLException e) {
            Rlog.d(TAG, "FAIL! SQLException");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Rlog.d(TAG, "isFirstConcatenatedSegment: " + address + "/" + refNumber
                + "result =" + result);
        return result;
    }

    public synchronized boolean isLastConcatenatedSegment(String address, int refNumber,
            int msgCount, boolean is3gpp2) {
        boolean result = false;

        Cursor cursor = null;
        try {
            String where = "address=? AND reference_number=? AND sub_id=?"
                 + (is3gpp2 ? SQL_3GPP2_SMS : SQL_3GPP_SMS);
            int subId = getSubIdUsingPhoneId();
            String[] whereArgs = new String[] {
                address,
                Integer.toString(refNumber),
                Integer.toString(subId)
            };
            cursor = mResolver.query(mRawUri,
                    CONCATE_PROJECTION, where, whereArgs, null);
            if (cursor != null) {
                int messageCount = cursor.getCount();
                if (messageCount == msgCount - 1) {
                    result = true;
                }
            } else {
                Rlog.d(TAG, "FAIL! cursor is null");
            }
        } catch (SQLException e) {
            Rlog.d(TAG, "FAIL! SQLException");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Rlog.d(TAG, "isLastConcatenatedSegment: " + address + "/" + refNumber
                + " result =" + result);
        return result;
    }

    public void startTimer(Handler h, Object r) {
        Rlog.d(TAG, "call startTimer");
        boolean isParamsValid = checkParamsForMessageOperation(h, r);
        if (isParamsValid == false) {
            Rlog.d(TAG, "FAIL! invalid params");
            return;
        }

        addTimerRecord((MtkTimerRecord) r);
        Message m = h.obtainMessage(EVENT_DISPATCH_CONCATE_SMS_SEGMENTS, r);
        h.sendMessageDelayed(m, getDelayedTime());
    }

    public void cancelTimer(Handler h, Object r) {
        Rlog.d(TAG, "call cancelTimer");
        boolean isParamsValid = checkParamsForMessageOperation(h, r);
        if (isParamsValid == false) {
            Rlog.d(TAG, "FAIL! invalid params");
            return;
        }

        h.removeMessages(EVENT_DISPATCH_CONCATE_SMS_SEGMENTS, r);
        deleteTimerRecord((MtkTimerRecord) r);
    }

    public void refreshTimer(Handler h, Object r) {
        Rlog.d(TAG, "call refreshTimer");
        boolean isParamsValid = checkParamsForMessageOperation(h, r);
        if (isParamsValid == false) {
            Rlog.d(TAG, "FAIL! invalid params");
            return;
        }

        h.removeMessages(EVENT_DISPATCH_CONCATE_SMS_SEGMENTS, r);
        Message m = h.obtainMessage(EVENT_DISPATCH_CONCATE_SMS_SEGMENTS, r);
        h.sendMessageDelayed(m, getDelayedTime());
    }

    public synchronized MtkTimerRecord queryTimerRecord(String address, int refNumber,
            int msgCount, boolean is3gpp2) {

        MtkTimerRecord ret = null;
        for (MtkTimerRecord record : mMtkTimerRecords) {
            if (record.address.equals(address) && record.refNumber == refNumber
                    && record.msgCount == msgCount) {
                InboundSmsTracker tracker = (InboundSmsTracker) record.mTracker;
                if (tracker != null && (tracker.is3gpp2() == is3gpp2)) {
                    ret = record;
                    break;
                }
            }
        }
        Rlog.d(TAG, "queryTimerRecord find record by [" + address + ", " + refNumber + ", "
                + msgCount + "]" + " result = " + ret);
        return ret;
    }

    private void addTimerRecord(MtkTimerRecord r) {
        Rlog.d(TAG, "call addTimerRecord");
        for (MtkTimerRecord record : mMtkTimerRecords) {
            if (record == r) {
                Rlog.d(TAG, "duplicated addTimerRecord object be found");
                return;
            }
        }

        mMtkTimerRecords.add(r);
    }

    private void deleteTimerRecord(MtkTimerRecord record) {

        if (mMtkTimerRecords == null || mMtkTimerRecords.size() == 0) {
            Rlog.d(TAG, "no record can be removed ");
            return;
        }

        int countBeforeRemove = mMtkTimerRecords.size();
        mMtkTimerRecords.remove(record);
        int countAfterRemove = mMtkTimerRecords.size();

        int countRemoved = countBeforeRemove - countAfterRemove;
        if (countRemoved > 0) {
            Rlog.d(TAG, "deleteTimerRecord" + countRemoved);
        } else {
            Rlog.d(TAG, "no record be removed");
        }
    }

    private boolean checkParamsForMessageOperation(Handler h, Object r) {
        Rlog.d(TAG, "call checkParamsForMessageOperation");
        if (h == null) {
            Rlog.d(TAG, "FAIL! handler is null");
            return false;
        }
        if (r == null) {
            Rlog.d(TAG, "FAIL! record is null");
            return false;
        }
        if (!(r instanceof MtkTimerRecord)) {
            Rlog.d(TAG, "FAIL! param r is not MtkTimerRecord object");
            return false;
        }

        return true;
    }

    public synchronized byte[][] queryExistedSegments(MtkTimerRecord record) {

        byte[][] pdus = null;
        Cursor cursor = null;
        boolean is3gpp2 = ((InboundSmsTracker) record.mTracker).is3gpp2();
        try {
            String where = "address=? AND reference_number=? AND sub_id=? AND count=?"
                    + (is3gpp2 ? SQL_3GPP2_SMS : SQL_3GPP_SMS);
            int subId = getSubIdUsingPhoneId();
            String[] whereArgs = new String[] {
                record.address,
                Integer.toString(record.refNumber),
                Integer.toString(subId),
                Integer.toString(record.msgCount)
            };
            cursor = mResolver.query(mRawUri,
                    PDU_SEQUENCE_PORT_PROJECTION, where, whereArgs, null);
            if (cursor != null) {
                byte[][] tempPdus = new byte[record.msgCount][];

                int columnSeqence = cursor.getColumnIndex("sequence");
                int columnPdu = cursor.getColumnIndex("pdu");
                int columnPort = cursor.getColumnIndex("destination_port");

                int cursorCount = cursor.getCount();
                Rlog.d(TAG, "queryExistedSegments columnSeqence =" + columnSeqence
                        + "; columnPdu = " + columnPdu + "; columnPort =" + columnPort
                        + " miss " + (record.msgCount - cursorCount) + " segment(s)");

                for (int i = 0; i < cursorCount; ++i) {
                    cursor.moveToNext();
                    int cursorSequence = cursor.getInt(columnSeqence);
                    if (ENG) {
                        Rlog.d(TAG, "queried segment " + cursorSequence + ", ref = " +
                                record.refNumber);
                    }
                    tempPdus[cursorSequence - 1] = HexDump.hexStringToByteArray(
                            cursor.getString(columnPdu));
                    if (tempPdus[cursorSequence - 1] == null) {
                        if (ENG) {
                            Rlog.d(TAG, "miss segment " + cursorSequence + ", ref = " +
                                    record.refNumber);
                        }
                    }

                    int destPort = -1;
                    if (!cursor.isNull(columnPort)) {
                        destPort = cursor.getInt(columnPort);
                        destPort = InboundSmsTracker.getRealDestPort(destPort);
                        if (destPort != -1) {
                            Rlog.d(TAG, "segment contain port " + destPort);
                            return null;
                        }
                    }
                }

                pdus = new byte[cursorCount][];
                int index = 0;
                for (int i = 0, len = tempPdus.length; i < len; ++i) {
                    if (tempPdus[i] != null) {
                        // Log.d("@M_" + TAG, "add segment " + index + " into pdus");
                        pdus[index++] = tempPdus[i];
                    }
                }
            } else {
                Rlog.d(TAG, "FAIL! cursor is null");
            }
        } catch (SQLException e) {
            Rlog.d(TAG, "FAIL! SQLException");
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return pdus;
    }

    public synchronized void deleteExistedSegments(MtkTimerRecord record) {
        boolean is3gpp2 = ((InboundSmsTracker) record.mTracker).is3gpp2();
        try {
            String where = "address=? AND reference_number=? AND sub_id=?"
                    + (is3gpp2 ? SQL_3GPP2_SMS : SQL_3GPP_SMS);
            int subId = getSubIdUsingPhoneId();
            String[] whereArgs = new String[] {
                record.address,
                Integer.toString(record.refNumber),
                Integer.toString(subId)
            };
            int numOfDeleted = mResolver.delete(mRawUri, where, whereArgs);
            Rlog.d(TAG, "deleteExistedSegments remove " + numOfDeleted + " segments, ref =  "
                    + record.refNumber);
        } catch (SQLException e) {
            Rlog.d(TAG, "FAIL! SQLException");
        }

        deleteTimerRecord(record);
    }

    public synchronized int getUploadFlag(MtkTimerRecord record) {
        boolean is3gpp2 = ((InboundSmsTracker) record.mTracker).is3gpp2();

        Cursor cursor = null;
        try {
            String where = "address=? AND reference_number=? AND sub_id=?"
                    + (is3gpp2 ? SQL_3GPP2_SMS : SQL_3GPP_SMS);
            int subId = getSubIdUsingPhoneId();
            String[] whereArgs = new String[] {
                record.address,
                Integer.toString(record.refNumber),
                Integer.toString(subId)
            };
            cursor = mResolver.query(mRawUri,
                    PDU_SEQUENCE_PORT_UPLOAD_PROJECTION, where, whereArgs, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                int columnUpload = cursor.getColumnIndex(UPLOAD_FLAG_TAG);
                    int uploadFlag = cursor.getInt(columnUpload);
                    if (uploadFlag == UPLOAD_FLAG_UPDATE) {
                        Rlog.d(TAG, "getUploadFlag find update segment");
                        return UPLOAD_FLAG_UPDATE;
                    }
                }
                Rlog.d(TAG, "getUploadFlag all segments are new");
                return UPLOAD_FLAG_NEW;
            } else {
                Rlog.d(TAG, "FAIL! cursor is null");
                return -1;
            }
        } catch (SQLException e) {
            Rlog.d(TAG, "FAIL! SQLException, fail to query upload_flag");
            return -1;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public synchronized void setUploadFlag(MtkTimerRecord record) {
        boolean is3gpp2 = ((InboundSmsTracker) record.mTracker).is3gpp2();

        try {
            String where = "address=? AND reference_number=? AND sub_id=? AND upload_flag<>?"
                    + (is3gpp2 ? SQL_3GPP2_SMS : SQL_3GPP_SMS);
            int subId = getSubIdUsingPhoneId();
            String[] whereArgs = new String[] {
                record.address,
                Integer.toString(record.refNumber),
                Integer.toString(subId),
                Integer.toString(UPLOAD_FLAG_UPDATE)
            };
            ContentValues values = new ContentValues();
            values.put(UPLOAD_FLAG_TAG, UPLOAD_FLAG_UPDATE);
            int updatedCount = mResolver.update(mRawUri, values, where, whereArgs);
            Rlog.d(TAG, "setUploadFlag update count: " + updatedCount);
        } catch (SQLException e) {
            Rlog.d(TAG, "FAIL! SQLException, fail to update upload flag");
        }
    }

    private synchronized int getDelayedTime() {
        CarrierConfigManager configMgr = (CarrierConfigManager)
                mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        int subId = getSubIdUsingPhoneId();
        PersistableBundle b = configMgr.getConfigForSubId(subId);
        int delay = DELAYED_TIME;
        if (b != null) {
            delay = b.getInt(MTK_KEY_EMS_WAITING_MISSING_SEGMENT_TIME_INT);
            delay *= 1000;
        }
        Rlog.d(TAG, "getDelayedTime " + delay);
        return delay;
    }

    private int getSubIdUsingPhoneId() {
        SubscriptionController subCon = SubscriptionController.getInstance();
        int subId = (subCon != null)?
                subCon.getSubIdUsingPhoneId(mPhoneId) : SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        if (ENG) {
            Rlog.d(TAG, "[getSubIdUsingPhoneId] subId " + subId + ", phoneId " + mPhoneId);
        }

        return subId;
    }
}
