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
/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mediatek.telephony;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemProperties;

import android.app.ActivityThread;

import android.content.Context;

import android.text.TextUtils;
import android.telephony.Rlog;
import android.telephony.SignalStrength;

import com.mediatek.internal.telephony.MtkOpTelephonyCustomizationUtils;
import com.mediatek.internal.telephony.MtkOpTelephonyCustomizationFactoryBase;

/**
 * Contains phone signal strength related information.
 */
public class MtkSignalStrength extends SignalStrength {

    private static final String LOG_TAG = "MtkSignalStrength";
    private static final boolean DBG = true;

    private MtkOpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory = null;
    private static ISignalStrengthExt mSignalStrengthExt = null;

    protected int mPhoneId;

    public static final String PROPERTY_OPERATOR_OPTR = "persist.vendor.operator.optr";
    private static String mOpId = null;
    /**
     * Empty constructor
     */
    public MtkSignalStrength(int phoneId) {
        super();
        mPhoneId = phoneId;
    }

    /**
     * Copy constructors
     *
     * @param phoneId provide phoneId to get phone
     * @param s Source MtkSignalStrength
     */
    public MtkSignalStrength(int phoneId, SignalStrength s) {
        super(s);
        mPhoneId = phoneId;
    }

    /**
     * Construct a SignalStrength object from the given parcel.
     *
     * @hide
     */
    public MtkSignalStrength(Parcel in) {
        super(in);
        mPhoneId = in.readInt();
    }

    /**
     * {@link Parcelable#writeToParcel}
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(mPhoneId);
    }

    private ISignalStrengthExt getOpInstance() {
        String op_id;
        op_id = SystemProperties.get(PROPERTY_OPERATOR_OPTR);
        if (TextUtils.isEmpty(op_id)) op_id = "om";
        if (mOpId == null || !mOpId.equals(op_id)) {
            Context context = ActivityThread.currentApplication().getApplicationContext();
            try {
                mTelephonyCustomizationFactory =
                        MtkOpTelephonyCustomizationUtils.getOpFactory(context);
                mSignalStrengthExt =
                        mTelephonyCustomizationFactory.makeSignalStrengthExt();
                mOpId = op_id;
                if (DBG) log("mSignalStrengthExt init success " + op_id);
            } catch (Exception e) {
                if (DBG) log("mSignalStrengthExt init fail");
                e.printStackTrace();
            }
        }
        return mSignalStrengthExt;
    }

    /**
     * log
     */
    private static void log(String s) {
        Rlog.w(LOG_TAG, s);
    }

    @Override
    public int getLevel() {
        return super.getLevel();
    }

    /**
     * Get LTE as level 0..4.
     * @return int the level for lte singal strength
     * @hide
     */
    @Override
    public int getLteLevel() {
        int rsrpIconLevel = -1;

        ISignalStrengthExt ssExt = getOpInstance();
        if (ssExt != null) {
            rsrpIconLevel = ssExt.mapLteSignalLevel(mLteRsrp, mLteRssnr, mLteSignalStrength);
            return rsrpIconLevel;
        } else {
            log("[getLteLevel] null op customization instance");
        }

        return super.getLteLevel();
    }
}
