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

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MtkDupSmsFilter implements IMtkDupSmsFilter {

    private static String TAG = "MtkDupSmsFilter";
    private static boolean DBG = SystemProperties.get("ro.build.type").equals("eng");

    protected static final String KEY_DUP_SMS_KEEP_PERIOD = "dev.dup_sms_keep_period";
    protected static final long DEFAULT_DUP_SMS_KEEP_PERIOD = 5 * 60 * 1000;
    protected static final int EVENT_CLEAR_SMS_LIST = 0x01;

    protected Context mContext = null;
    protected int mPhoneId = -1;

    protected HashMap<Long, byte[]> mSmsMap = null;

    public MtkDupSmsFilter(Context context) {
        Log.d("@M_" + TAG, "call constructor");
        if (context == null) {
            Log.d("@M_" + TAG, "FAIL! context is null");
            return;
        }

        mContext = context;
        mSmsMap = new HashMap<Long, byte[]>();
    }

    public void setPhoneId(int phoneId) {
        mPhoneId = phoneId;
    }

    public boolean containDupSms(byte[] pdu) {
        Log.d("@M_" + TAG, "call containDupSms");

        /* Test SIM card should not use the duplicate mechanism */
        if (isTestIccCard()) {
            return false;
        }

        removeExpiredItem();
        Iterator<Map.Entry<Long, byte[]>> iter = mSmsMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, byte[]> entry = (Map.Entry<Long, byte[]>) iter.next();
            if (isDupSms(pdu, entry.getValue())) {
                Log.d("@M_" + TAG, "find a duplicated sms");
                return true;
            }
        }
        synchronized (mSmsMap) {
            mSmsMap.put(System.currentTimeMillis(), pdu);
        }
        Log.d("@M_" + TAG, "Not duplicated sms");
        return false;
    }

    protected boolean isDupSms(byte[] newPdu, byte[] oldPdu) {
        if (newPdu == null || oldPdu == null) {
            Log.d("@M_" + TAG, "Return false directly because either newPdu or oldPdu is null.");
            return false;
        }
        // Copy the pdu first in order not to modify the original pdu
        byte[] newClonePdu = newPdu.clone();
        byte[] oldClonePdu = oldPdu.clone();

        /**
         * In China network, network will re-send the SMS but changing the
         * TP-MMS value. After confirming with modem owner, we can ignore the
         * difference of TP-MMS and apply it in all over the world.
         * The reason is because the TP-MMS is not related with message content.
         */
        // Mark the TP-MMS as 1 by default
        int newFirstBytePos = getFirstBytePosition(newClonePdu);
        int oldFirstBytePos = getFirstBytePosition(oldClonePdu);
        if (DBG) {
            Log.d("@M_" + TAG, "newClonePdu " + Arrays.toString(newClonePdu) +
                    ", newFirstBytePos " + newFirstBytePos + ", oldClonePdu " +
                    Arrays.toString(oldClonePdu) + ", oldFirstBytePos " + oldFirstBytePos);
        }
        byte tpMms = 0x4;
        newClonePdu[newFirstBytePos] |= tpMms;
        oldClonePdu[oldFirstBytePos] |= tpMms;
        if (DBG) {
            Log.d("@M_" + TAG, "Mark TP-MMS as 1, " +
                    "newPdu first byte: " + newPdu[newFirstBytePos] + "->" +
                    newClonePdu[newFirstBytePos] +
                    ", oldPdu first byte: " + oldPdu[oldFirstBytePos] + "->" +
                    oldClonePdu[oldFirstBytePos]);
        }

        if (Arrays.equals(newClonePdu, oldClonePdu)) {
            return true;
        }

        return false;
    }

    synchronized private void removeExpiredItem() {
        Log.d("@M_" + TAG, "call removeExpiredItem");
        long delayedPeriod = SystemProperties.getLong(
                KEY_DUP_SMS_KEEP_PERIOD,
                DEFAULT_DUP_SMS_KEEP_PERIOD);
        Iterator<Map.Entry<Long, byte[]>> iter = mSmsMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, byte[]> entry = (Map.Entry<Long, byte[]>) iter.next();
            if (entry.getKey() < System.currentTimeMillis() - delayedPeriod) {
                iter.remove();
            }
        }
        Log.d("@M_" + TAG, "mSmsMap has " + mSmsMap.size() + " items after removeExpiredItem");
    }

    private boolean isTestIccCard() {
        int ret = -1;
        // FIXME: it should use the sub id key and whole files should put back to telephony-common
        if (mPhoneId == PhoneConstants.SIM_ID_1) {
            ret = SystemProperties.getInt("vendor.gsm.sim.ril.testsim", -1);
        } else if (mPhoneId == PhoneConstants.SIM_ID_2) {
            ret = SystemProperties.getInt("vendor.gsm.sim.ril.testsim.2", -1);
        } else if (mPhoneId == PhoneConstants.SIM_ID_3) {
            ret = SystemProperties.getInt("vendor.gsm.sim.ril.testsim.3", -1);
        } else if (mPhoneId == PhoneConstants.SIM_ID_4) {
            ret = SystemProperties.getInt("vendor.gsm.sim.ril.testsim.4", -1);
        }
        Log.d("@M_" + TAG, "Phone id: " + mPhoneId + "isTestIccCard: " + ret);
        return (ret == 1);
    }

    private int getFirstBytePosition(byte[] pdu) {
        // refer to getSCAddress()@gsm/SmsMessage.java
        int len;
        int pos = 0;

        // length of SC Address
        len = pdu[0] & 0xff;

        if (len == 0) {
            // no SC address
            pos = 1;  // ignore the first byte
        } else {
            // SC address
            pos = 1 + len;
        }

        return pos;
    }
}