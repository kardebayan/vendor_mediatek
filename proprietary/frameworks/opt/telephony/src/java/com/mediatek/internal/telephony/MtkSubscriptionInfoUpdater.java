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

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.CarrierAppUtils;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IntentBroadcaster;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.SubscriptionInfoUpdater;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;

import com.mediatek.internal.telephony.MtkDefaultSmsSimSettings;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.internal.telephony.uicc.MtkSpnOverride;
import com.mediatek.telephony.MtkTelephonyManagerEx;

import java.util.List;


/**
 *@hide
 */
public class MtkSubscriptionInfoUpdater extends SubscriptionInfoUpdater {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "MtkSubscriptionInfoUpdater";

    private static final int EVENT_RADIO_AVAILABLE = 101;
    private static final int EVENT_RADIO_UNAVAILABLE = 102;
    // For the feature SIM Hot Swap with Common Slot
    private static final int EVENT_SIM_NO_CHANGED = 103;
    private static final int EVENT_TRAY_PLUG_IN = 104;
    private static final int EVENT_SIM_PLUG_OUT = 105;
    // For the feature SIM ME LOCK - SML SBP
    private static final int EVENT_SIM_MOUNT_CHANGED = 106;

    private static final String ICCID_STRING_FOR_NO_SIM = "N/A";

    private final Object mLock = new Object();
    // SIM ME LOCK - Start
    private final Object mLockUpdateNew = new Object();
    private final Object mLockUpdateOld = new Object();
    // lock info: {detected type, sub count, SIM1 valid, SIM2 valid}
    private int[] newSmlInfo = {4, 0, -1, -1};
    private int[] oldSmlInfo = {4, 0, -1, -1};
    private boolean mSimMountChangeState = false;
    private static MtkSubscriptionInfoUpdater sInstance = null;
    // The property shows SIM ME LOCK mode
    private static final String PROPERTY_SML_MODE = "ro.vendor.sim_me_lock_mode";
    private boolean mIsSmlLockMode = SystemProperties.get(PROPERTY_SML_MODE, "").equals("3");
    // SIM ME LOCK - End


    private CommandsInterface[] mCis = null;

    private int[] mIsUpdateAvailable = new int[PROJECT_SIM_NUM];
    private int mReadIccIdCount = 0;

    // For the feature SIM Hot Swap with Common Slot
    private static final String COMMON_SLOT_PROPERTY = "ro.vendor.mtk_sim_hot_swap_common_slot";
    private boolean mCommonSlotResetDone = false;

    private static final boolean MTK_FLIGHTMODE_POWEROFF_MD_SUPPORT
            = "1".equals(SystemProperties.get("ro.vendor.mtk_flight_mode_power_off_md"));
    private static final int sReadICCID_retry_time = 1000;
    private static final String[] PROPERTY_ICCID_SIM = {
        "vendor.ril.iccid.sim1",
        "vendor.ril.iccid.sim2",
        "vendor.ril.iccid.sim3",
        "vendor.ril.iccid.sim4",
    };

    // A big rule, please add you code in the rear of API and file.
    public MtkSubscriptionInfoUpdater(Looper looper, Context context, Phone[] phone,
            CommandsInterface[] ci) {
        super(looper, context, phone, ci);
        logd("MtkSubscriptionInfoUpdater created");

        mCis = ci;
        // register MTK self receiver for funtion extension
        IntentFilter intentFilter = new IntentFilter(
                TelephonyIntents.ACTION_COMMON_SLOT_NO_CHANGED);
        if ("OP09".equals(SystemProperties.get("persist.vendor.operator.optr"))) {
            intentFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
        }
        mContext.registerReceiver(mMtkReceiver, intentFilter);

        for (int i = 0; i < mCis.length; i++) {
            Integer index = new Integer(i);
            mCis[i].registerForNotAvailable(this, EVENT_RADIO_UNAVAILABLE, index);
            mCis[i].registerForAvailable(this, EVENT_RADIO_AVAILABLE, index);
            if (SystemProperties.get(COMMON_SLOT_PROPERTY).equals("1")) {
                ((MtkRIL)mCis[i]).registerForSimTrayPlugIn(this, EVENT_TRAY_PLUG_IN, index);
                ((MtkRIL)mCis[i]).registerForSimPlugOut(this, EVENT_SIM_PLUG_OUT, index);
            }
        }
    }

    @Override
    protected boolean isAllIccIdQueryDone() {
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mIccId[i] == null || mIccId[i].equals("")) {
                logd("Wait for SIM" + (i + 1) + " IccId");
                return false;
            }
        }
        logd("All IccIds query complete");

        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        Integer index = getCiIndex(msg);

        switch (msg.what) {
            case EVENT_SIM_READY: {
                if (checkAllIccIdReady()) {
                    updateSubscriptionInfoIfNeed();
                }
                super.handleMessage(msg);
                break;
            }

            case EVENT_RADIO_UNAVAILABLE:
                logd("handleMessage : <EVENT_RADIO_UNAVAILABLE> SIM" + (index + 1));
                mIsUpdateAvailable[index] = 0;
                if (SystemProperties.get(COMMON_SLOT_PROPERTY).equals("1")) {
                    logd("[Common slot] reset mCommonSlotResetDone in EVENT_RADIO_UNAVAILABLE");
                    mCommonSlotResetDone = false;
                }
                break;

            case EVENT_RADIO_AVAILABLE:
                logd("handleMessage : <EVENT_RADIO_AVAILABLE> SIM" + (index + 1));
                mIsUpdateAvailable[index] = 1;

                if (checkIsAvailable()) {
                    mReadIccIdCount = 0;
                    if (!checkAllIccIdReady()) {
                        postDelayed(mReadIccIdPropertyRunnable, sReadICCID_retry_time);
                    } else {
                        updateSubscriptionInfoIfNeed();
                    }
                }
                break;

            case EVENT_SIM_NO_CHANGED: {
                if (checkAllIccIdReady()) {
                    updateSubscriptionInfoIfNeed();
                } else {
                    int slotId = msg.arg1;
                    mIccId[slotId] = ICCID_STRING_FOR_NO_SIM;
                    logd("case SIM_NO_CHANGED: set N/A for slot" + slotId);
                    mReadIccIdCount = 0;
                    postDelayed(mReadIccIdPropertyRunnable, sReadICCID_retry_time);
                }
                break;
            }

            case EVENT_TRAY_PLUG_IN: {
                logd("[Common Slot] handle EVENT_TRAY_PLUG_IN " + mCommonSlotResetDone);
                if (!mCommonSlotResetDone) {
                    mCommonSlotResetDone = true;
                    int vsimEnabledBitMask = 0;
                    for (int i = 0; i < PROJECT_SIM_NUM; i++) {
                        String vsimEnabled = TelephonyManager.getDefault().getTelephonyProperty(
                                i, MtkTelephonyProperties.PROPERTY_EXTERNAL_SIM_ENABLED, "0");

                        vsimEnabled = ((vsimEnabled.length() == 0) ? "0" : vsimEnabled);
                        logd("vsimEnabled[" + i + "]: (" + vsimEnabled + ")");

                        try {
                            if ("0".equals(vsimEnabled)) {
                                logd("[Common Slot] reset mIccId[" + i + "] to empty.");
                                mIccId[i] = "";
                            } else {
                                vsimEnabledBitMask = vsimEnabledBitMask | (0x01 << i);
                            }
                        } catch (NumberFormatException e) {
                            logd("[Common Slot] NumberFormatException, reset mIccId[" + i +
                                    "] to empty.");
                            mIccId[i] = "";
                        }
                    }
                    if (vsimEnabledBitMask == 0) {
                        mReadIccIdCount = 0;
                        if (!checkAllIccIdReady()) {
                            postDelayed(mReadIccIdPropertyRunnable, sReadICCID_retry_time);
                        } else {
                            updateSubscriptionInfoIfNeed();
                        }
                    }
                }
                break;
            }
            case EVENT_SIM_PLUG_OUT: {
                logd("[Common Slot] handle EVENT_SIM_PLUG_OUT " + mCommonSlotResetDone);
                if (SystemProperties.get(COMMON_SLOT_PROPERTY).equals("1")) {
                    mReadIccIdCount = 0;
                    postDelayed(mReadIccIdPropertyRunnable, sReadICCID_retry_time);
                }
                mCommonSlotResetDone = false;
                break;
            }

            case EVENT_SIM_MOUNT_CHANGED: {
                updateNewSmlInfo(newSmlInfo[0], newSmlInfo[1]);
                resetSimMountChangeState();
                break;
            }

            default:
                super.handleMessage(msg);
        }
    }

    @Override
    protected void handleSimLocked(int slotId, String reason) {
        if (mIccId[slotId] != null && mIccId[slotId].equals(ICCID_STRING_FOR_NO_SIM)) {
            logd("SIM" + (slotId + 1) + " hot plug in");
            mIccId[slotId] = null;
        }

        String iccId = mIccId[slotId];
        if (iccId == null) {
            IccCard iccCard = mPhone[slotId].getIccCard();
            if (iccCard == null) {
                logd("handleSimLocked: IccCard null");
                return;
            }
            IccRecords records = iccCard.getIccRecords();
            if (records == null) {
                logd("handleSimLocked: IccRecords null");
                return;
            }

            /* if (IccUtils.stripTrailingFs(records.getFullIccId()) == null) {
                logd("handleSimLocked: IccID null");
                return;
            }
            mIccId[slotId] = IccUtils.stripTrailingFs(records.getFullIccId()); */
        } else {
            logd("NOT Querying IccId its already set sIccid[" + slotId + "]="
                    + SubscriptionInfo.givePrintableIccid(iccId));

            String tempIccid = SystemProperties.get(PROPERTY_ICCID_SIM[slotId], "");
            if (MTK_FLIGHTMODE_POWEROFF_MD_SUPPORT && !checkAllIccIdReady()
                    && (!tempIccid.equals(mIccId[slotId]))) {
                logd("All iccids are not ready and iccid changed");
                mIccId[slotId] = null;
                mSubscriptionManager.clearSubscriptionInfo();
            }
        }

        if (checkAllIccIdReady()) {
            updateSubscriptionInfoIfNeed();
        }

        updateCarrierServices(slotId, IccCardConstants.INTENT_VALUE_ICC_LOCKED);
        broadcastSimStateChanged(slotId, IccCardConstants.INTENT_VALUE_ICC_LOCKED, reason);
        broadcastSimCardStateChanged(slotId, TelephonyManager.SIM_STATE_PRESENT);
        broadcastSimApplicationStateChanged(slotId, getSimStateFromLockedReason(reason));
    }

    @Override
    protected void handleSimLoaded(int slotId) {
        logd("handleSimLoaded: slotId: " + slotId);

        // The SIM should be loaded at this state, but it is possible in cases such as SIM being
        // removed or a refresh RESET that the IccRecords could be null. The right behavior is to
        // not broadcast the SIM loaded.
        int loadedSlotId = slotId;
        IccCard iccCard = mPhone[slotId].getIccCard();
        if (iccCard == null) {  // Possibly a race condition.
            logd("handleSimLoaded: IccCard null");
            return;
        }
        IccRecords records = iccCard.getIccRecords();
        if (records == null) {  // Possibly a race condition.
            logd("handleSimLoaded: IccRecords null");
            return;
        }
        if (IccUtils.stripTrailingFs(records.getFullIccId()) == null) {
            logd("handleSimLoaded: IccID null");
            return;
        }

        // Check iccid in updateSubscriptionInfoIfNeed().
        // mIccId[slotId] = IccUtils.stripTrailingFs(records.getFullIccId());

        if (checkAllIccIdReady()) {
            updateSubscriptionInfoIfNeed();
            int[] subIds = mSubscriptionManager.getActiveSubscriptionIdList();
            for (int subId : subIds) {
                //Just update loaded slotId's sub infomation.
                slotId = MtkSubscriptionController.getMtkInstance().getPhoneId(subId);
                if (loadedSlotId != slotId) {
                    continue;
                }

                // Shoudn't use getDefault, it will suffer permission issue and can't get
                // operator and line1 number correctly.
                //TelephonyManager tm = TelephonyManager.getDefault();
                TelephonyManager tm = TelephonyManager.from(mContext);
                String operator = tm.getSimOperatorNumeric(subId);

                if (!TextUtils.isEmpty(operator)) {
                    if (subId == MtkSubscriptionController.getMtkInstance().getDefaultSubId()) {
                        MccTable.updateMccMncConfiguration(mContext, operator, false);
                    }
                    MtkSubscriptionController.getMtkInstance().setMccMnc(operator, subId);
                } else {
                    logd("EVENT_RECORDS_LOADED Operator name is null");
                }

                String msisdn = tm.getLine1Number(subId);
                ContentResolver contentResolver = mContext.getContentResolver();

                if (msisdn != null) {
                    MtkSubscriptionController.getMtkInstance().setDisplayNumber(msisdn, subId);
                }

                SubscriptionInfo subInfo = mSubscriptionManager.getActiveSubscriptionInfo(subId);
                String nameToSet;
                String simCarrierName = tm.getSimOperatorName(subId);

                if (subInfo != null && subInfo.getNameSource() !=
                        SubscriptionManager.NAME_SOURCE_USER_INPUT) {
                    String simNumeric = tm.getSimOperatorNumeric(subId);
                    String simMvnoName = MtkSpnOverride.getInstance()
                            .lookupOperatorNameForDisplayName(subId, simNumeric, true, mContext);
                    logd("[handleSimLoaded]- simNumeric: " + simNumeric + ", simMvnoName: "
                            + simMvnoName + ", simCarrierName: " + simCarrierName);

                    if (!TextUtils.isEmpty(simMvnoName)) {
                        nameToSet = simMvnoName;
                    } else if (!TextUtils.isEmpty(simCarrierName)) {
                        nameToSet = simCarrierName;
                    } else {
                        nameToSet = "CARD " + Integer.toString(slotId + 1);
                    }
                    logd("sim name = " + nameToSet);
                    MtkSubscriptionController.getMtkInstance().setDisplayName(nameToSet, subId);
                }

                /* Update preferred network type and network selection mode on SIM change.
                 * Storing last subId in SharedPreference for now to detect SIM change. */
                SharedPreferences sp =
                        PreferenceManager.getDefaultSharedPreferences(mContext);
                int storedSubId = sp.getInt(CURR_SUBID + slotId, -1);

                if (storedSubId != subId) {
                    int networkType = Settings.Global.getInt(
                            mPhone[slotId].getContext().getContentResolver(),
                            Settings.Global.PREFERRED_NETWORK_MODE + subId,
                            -1 /* invalid network mode */);

                    if (networkType == -1) {
                        networkType = RILConstants.PREFERRED_NETWORK_MODE;
                        try {
                            networkType = TelephonyManager.getIntAtIndex(
                                    mContext.getContentResolver(),
                                    Settings.Global.PREFERRED_NETWORK_MODE, slotId);
                        } catch (SettingNotFoundException retrySnfe) {
                            Rlog.e(LOG_TAG, "Settings Exception Reading Value At Index for "
                                    + "Settings.Global.PREFERRED_NETWORK_MODE");
                        }
                    }
                    // Dynamic check if modem chip cannot access 4G END
                    Settings.Global.putInt(mPhone[slotId].getContext().getContentResolver(),
                            Global.PREFERRED_NETWORK_MODE + subId, networkType);

                    // Set the modem network mode
                    // mPhone[slotId].setPreferredNetworkType(networkType, null);

                    // Only support automatic selection mode on SIM change.
                    mPhone[slotId].getNetworkSelectionMode(
                            obtainMessage(EVENT_GET_NETWORK_SELECTION_MODE_DONE,
                            new Integer(slotId)));

                    // Update stored subId
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putInt(CURR_SUBID + slotId, subId);
                    editor.apply();
                }
            }

            // Update set of enabled carrier apps now that the privilege rules may have changed.
            CarrierAppUtils.disableCarrierAppsUntilPrivileged(mContext.getOpPackageName(),
                    mPackageManager, TelephonyManager.getDefault(),
                    mContext.getContentResolver(), mCurrentlyActiveUserId);

            broadcastSimStateChanged(loadedSlotId, IccCardConstants.INTENT_VALUE_ICC_LOADED, null);
            broadcastSimCardStateChanged(loadedSlotId, TelephonyManager.SIM_STATE_PRESENT);
            broadcastSimApplicationStateChanged(loadedSlotId, TelephonyManager.SIM_STATE_LOADED);
            updateCarrierServices(loadedSlotId, IccCardConstants.INTENT_VALUE_ICC_LOADED);
        } else {
            logd("[handleSimLoaded] checkAllIccIdReady is false, retry it in slot[" + slotId + "]");
            sendMessageDelayed(obtainMessage(EVENT_SIM_LOADED, slotId, -1), 100);
        }
    }

    @Override
    protected void handleSimAbsent(int slotId) {
        if (mIccId[slotId] != null && !mIccId[slotId].equals(ICCID_STRING_FOR_NO_SIM)) {
            logd("SIM" + (slotId + 1) + " hot plug out");
        }

        if (checkAllIccIdReady()) {
            updateSubscriptionInfoIfNeed();
        }

        updateCarrierServices(slotId, IccCardConstants.INTENT_VALUE_ICC_ABSENT);
        broadcastSimStateChanged(slotId, IccCardConstants.INTENT_VALUE_ICC_ABSENT, null);
        broadcastSimCardStateChanged(slotId, TelephonyManager.SIM_STATE_ABSENT);
        broadcastSimApplicationStateChanged(slotId, TelephonyManager.SIM_STATE_NOT_READY);
    }

    @Override
    protected void handleSimError(int slotId) {
        if (mIccId[slotId] != null && !mIccId[slotId].equals(ICCID_STRING_FOR_NO_SIM)) {
            logd("SIM" + (slotId + 1) + " Error ");
        }
        mIccId[slotId] = ICCID_STRING_FOR_NO_SIM;
        if (isAllIccIdQueryDone()) {
            updateSubscriptionInfoByIccId();
        }
        updateCarrierServices(slotId, IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR);
        broadcastSimStateChanged(slotId, IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR,
                IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR);
        broadcastSimCardStateChanged(slotId, TelephonyManager.SIM_STATE_CARD_IO_ERROR);
        broadcastSimApplicationStateChanged(slotId, TelephonyManager.SIM_STATE_NOT_READY);
    }

    /**
     * TODO: Simplify more, as no one is interested in what happened
     * only what the current list contains.
     */
    @Override
    synchronized protected void updateSubscriptionInfoByIccId() {
        logd("updateSubscriptionInfoByIccId:+ Start");

        // ALPS01933839 timing issue, JE after receiving IPO shutdown
        // do this update
        if (!isAllIccIdQueryDone()) {
            return;
        }
        // Reset the flag because all sIccId are ready.
        mCommonSlotResetDone = false;
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            mInsertSimState[i] = SIM_NOT_CHANGE;
        }

        int insertedSimCount = PROJECT_SIM_NUM;
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (ICCID_STRING_FOR_NO_SIM.equals(mIccId[i])) {
                insertedSimCount--;
                mInsertSimState[i] = SIM_NOT_INSERT;
            }
        }
        logd("insertedSimCount = " + insertedSimCount);

        // We only clear the slot-to-sub map when one/some SIM was removed. Note this is a
        // workaround for some race conditions that the empty map was accessed while we are
        // rebuilding the map.
        /* if (SubscriptionController.getInstance().getActiveSubIdList().length >
                   insertedSimCount) {
            SubscriptionController.getInstance().clearSubInfo();
        } */

        int index = 0;
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mInsertSimState[i] == SIM_NOT_INSERT) {
                continue;
            }
            index = 2;
            for (int j = i + 1; j < PROJECT_SIM_NUM; j++) {
                if (mInsertSimState[j] == SIM_NOT_CHANGE && mIccId[i].equals(mIccId[j])) {
                    mInsertSimState[i] = 1;
                    mInsertSimState[j] = index;
                    index++;
                }
            }
        }

        ContentResolver contentResolver = mContext.getContentResolver();
        String[] oldIccId = new String[PROJECT_SIM_NUM];
        String[] decIccId = new String[PROJECT_SIM_NUM];
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            oldIccId[i] = null;
            List<SubscriptionInfo> oldSubInfo =
                    MtkSubscriptionController.getMtkInstance()
                    .getSubInfoUsingSlotIndexPrivileged(i, false);
            decIccId[i] = IccUtils.getDecimalSubstring(mIccId[i]);
            if (oldSubInfo != null && oldSubInfo.size() > 0) {
                oldIccId[i] = oldSubInfo.get(0).getIccId();
                logd("updateSubscriptionInfoByIccId: oldSubId = "
                        + oldSubInfo.get(0).getSubscriptionId());
                if (mInsertSimState[i] == SIM_NOT_CHANGE && !(mIccId[i].equals(oldIccId[i])
                        || (decIccId[i] != null && decIccId[i].equals(oldIccId[i]))
                        || (mIccId[i].toLowerCase().equals(oldIccId[i])))) {
                    mInsertSimState[i] = SIM_CHANGED;
                }
                if (mInsertSimState[i] != SIM_NOT_CHANGE) {
                    MtkSubscriptionController.getMtkInstance().clearSubInfoUsingPhoneId(i);
                    logd("updateSubscriptionInfoByIccId: clearSubInfoUsingPhoneId phoneId = "
                            + i);
                    try {
                        ContentValues value = new ContentValues(1);
                        value.put(SubscriptionManager.SIM_SLOT_INDEX,
                                SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                        contentResolver.update(SubscriptionManager.CONTENT_URI, value,
                                SubscriptionManager.UNIQUE_KEY_SUBSCRIPTION_ID + "="
                                + Integer.toString(oldSubInfo.get(0).getSubscriptionId()),
                                null);
                    } catch (IllegalArgumentException e) {
                        throw e;
                    }

                    // refresh Cached Active Subscription Info List
                    SubscriptionController.getInstance()
                            .refreshCachedActiveSubscriptionInfoList();
                }
            } else {
                if (mInsertSimState[i] == SIM_NOT_CHANGE) {
                    // no SIM inserted last time, but there is one SIM inserted now
                    mInsertSimState[i] = SIM_CHANGED;
                }
                MtkSubscriptionController.getMtkInstance().clearSubInfoUsingPhoneId(i);
                logd("updateSubscriptionInfoByIccId: clearSubInfoUsingPhoneId phoneId = " + i);
                oldIccId[i] = ICCID_STRING_FOR_NO_SIM;
                logd("updateSubscriptionInfoByIccId: No SIM in slot " + i + " last time");
            }
        }

        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            logd("updateSubscriptionInfoByIccId: oldIccId[" + i + "] = " +
                    SubscriptionInfo.givePrintableIccid(oldIccId[i]) +
                    ", sIccId[" + i + "] = " + SubscriptionInfo.givePrintableIccid(mIccId[i]));
        }

        //check if the inserted SIM is new SIM
        int nNewCardCount = 0;
        int nNewSimStatus = 0;
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mInsertSimState[i] == SIM_NOT_INSERT) {
                logd("updateSubscriptionInfoByIccId: No SIM inserted in slot " + i
                        + " this time");
            } else {
                if (mInsertSimState[i] > 0) {
                    //some special SIMs may have the same IccIds, add suffix to distinguish them
                    //FIXME: addSubInfoRecord can return an error.
                    mSubscriptionManager.addSubscriptionInfoRecord(mIccId[i]
                            + Integer.toString(mInsertSimState[i]), i);
                    logd("SUB" + (i + 1) + " has invalid IccId");
                } else /*if (sInsertSimState[i] != SIM_NOT_INSERT)*/ {
                    mSubscriptionManager.addSubscriptionInfoRecord(mIccId[i], i);
                }
                if (isNewSim(mIccId[i], decIccId[i], oldIccId)) {
                    nNewCardCount++;
                    switch (i) {
                        case PhoneConstants.SUB1:
                            nNewSimStatus |= STATUS_SIM1_INSERTED;
                            break;
                        case PhoneConstants.SUB2:
                            nNewSimStatus |= STATUS_SIM2_INSERTED;
                            break;
                        case PhoneConstants.SUB3:
                            nNewSimStatus |= STATUS_SIM3_INSERTED;
                            break;
                        //case PhoneConstants.SUB3:
                        //    nNewSimStatus |= STATUS_SIM4_INSERTED;
                        //    break;
                        default:
                            break;
                    }

                    mInsertSimState[i] = SIM_NEW;
                }
            }
        }

        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mInsertSimState[i] == SIM_CHANGED) {
                mInsertSimState[i] = SIM_REPOSITION;
            }
            logd("updateSubscriptionInfoByIccId: sInsertSimState[" + i + "] = "
                    + mInsertSimState[i]);
        }

        List<SubscriptionInfo> subInfos = mSubscriptionManager.getActiveSubscriptionInfoList();
        int nSubCount = (subInfos == null) ? 0 : subInfos.size();
        logd("updateSubscriptionInfoByIccId: nSubCount = " + nSubCount);
        for (int i = 0; i < nSubCount; i++) {
            SubscriptionInfo temp = subInfos.get(i);

            // Shoudn't use getDefault, it will suffer permission issue and can't get
            // line1 number correctly.
            String msisdn = TelephonyManager.from(mContext).getLine1Number(
                    temp.getSubscriptionId());

            if (msisdn != null) {
                //ContentValues value = new ContentValues(1);
                //value.put(SubscriptionManager.NUMBER, msisdn);
                //contentResolver.update(SubscriptionManager.CONTENT_URI, value,
                //        SubscriptionManager.UNIQUE_KEY_SUBSCRIPTION_ID + "="
                //        + Integer.toString(temp.getSubscriptionId()), null);
                MtkSubscriptionController.getMtkInstance()
                        .setDisplayNumber(msisdn, temp.getSubscriptionId());

                // refresh Cached Active Subscription Info List
                SubscriptionController.getInstance()
                        .refreshCachedActiveSubscriptionInfoList();
            }
        }

        MtkDefaultSmsSimSettings.setSmsTalkDefaultSim(subInfos, mContext);

        // true if any slot has no SIM this time, but has SIM last time
        boolean hasSimRemoved = false;
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mIccId[i] != null && mIccId[i].equals(ICCID_STRING_FOR_NO_SIM)
                    && !oldIccId[i].equals(ICCID_STRING_FOR_NO_SIM)) {
                hasSimRemoved = true;
                break;
            }
        }

        Intent intent = null;
        if (nNewCardCount == 0) {
            int i;
            if (hasSimRemoved) {
                // no new SIM, at least one SIM is removed, check if any SIM is repositioned
                for (i = 0; i < PROJECT_SIM_NUM; i++) {
                    if (mInsertSimState[i] == SIM_REPOSITION) {
                        logd("No new SIM detected and SIM repositioned");
                        intent = setUpdatedData(
                                MtkSubscriptionManager.EXTRA_VALUE_REPOSITION_SIM,
                                nSubCount, nNewSimStatus);
                        break;
                    }
                }
                if (i == PROJECT_SIM_NUM) {
                    // no new SIM, no SIM is repositioned => at least one SIM is removed
                    logd("No new SIM detected and SIM removed");
                    intent = setUpdatedData(MtkSubscriptionManager.EXTRA_VALUE_REMOVE_SIM,
                            nSubCount, nNewSimStatus);
                }
            } else {
                // no SIM is removed, no new SIM, just check if any SIM is repositioned
                for (i = 0; i < PROJECT_SIM_NUM; i++) {
                    if (mInsertSimState[i] == SIM_REPOSITION) {
                        logd("No new SIM detected and SIM repositioned");
                        intent = setUpdatedData(
                                MtkSubscriptionManager.EXTRA_VALUE_REPOSITION_SIM,
                                nSubCount, nNewSimStatus);
                        break;
                    }
                }
                if (i == PROJECT_SIM_NUM) {
                    // all status remain unchanged
                    logd("[updateSimInfoByIccId] All SIM inserted into the same slot");
                    intent = setUpdatedData(MtkSubscriptionManager.EXTRA_VALUE_NOCHANGE,
                            nSubCount, nNewSimStatus);
                }
            }
        } else {
            logd("New SIM detected");
            intent = setUpdatedData(MtkSubscriptionManager.EXTRA_VALUE_NEW_SIM, nSubCount,
                    nNewSimStatus);
        }
        // Single project has update in add
        if (PROJECT_SIM_NUM > 1) {
            // We only set default data subId here due to capability switch cause RADIO_UNAVAILABLE
            // on our platform. Radio module will do capability switch later.
            MtkSubscriptionController.getMtkInstance()
                    .setDefaultDataSubIdWithoutCapabilitySwitch(
                    mSubscriptionManager.getDefaultDataSubscriptionId());
        }

        // External SIM [Start]
        if (SystemProperties.getInt("ro.vendor.mtk_external_sim_support", 0) == 1 &&
                SystemProperties.getInt("ro.vendor.mtk_non_dsda_rsim_support", 0) == 1) {

            int rsimPhoneId = SystemProperties.getInt(
                    MtkTelephonyProperties.PROPERTY_PREFERED_REMOTE_SIM, -1);
            int rsimSubId[] = MtkSubscriptionController.getMtkInstance().getSubId(rsimPhoneId);
            if (rsimPhoneId >= 0 && rsimPhoneId < PROJECT_SIM_NUM
                    && rsimSubId != null && rsimSubId.length != 0) {
                MtkSubscriptionController.getMtkInstance().setDefaultDataSubId(
                        rsimSubId[0]);
            }
        }
        // External SIM [End]

        // No need to check return value here as we notify for the above changes anyway.
        updateEmbeddedSubscriptions();

        MtkSubscriptionController.getMtkInstance().notifySubscriptionInfoChanged(intent);
        logd("updateSubscriptionInfoByIccId:- SubscriptionInfo update complete");
    }

    @Override
    protected boolean isNewSim(String iccId, String decIccId, String[] oldIccId) {
        boolean newSim = true;
        for(int i = 0; i < PROJECT_SIM_NUM; i++) {
            if ((iccId != null) && (oldIccId[i] != null) && (oldIccId[i].indexOf(iccId) == 0)) {
                newSim = false;
                break;
            } else if (decIccId != null && decIccId.equals(oldIccId[i])) {
                newSim = false;
                break;
            }
        }
        logd("newSim = " + newSim);

        return newSim;
    }

    public void dispose() {
        logd("[dispose]");
        mContext.unregisterReceiver(mMtkReceiver);
    }

    // Please add override APIs before this API and add MTK APIs after this API.
    // Notes, please place the MTK code by functions, inner class and member order
    private Intent setUpdatedData(int detectedType, int subCount, int newSimStatus) {

        Intent intent = new Intent(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);

        logd("[setUpdatedData]+ ");

        if (detectedType == MtkSubscriptionManager.EXTRA_VALUE_NEW_SIM) {
            intent.putExtra(MtkSubscriptionManager.INTENT_KEY_DETECT_STATUS,
                    MtkSubscriptionManager.EXTRA_VALUE_NEW_SIM);
            intent.putExtra(MtkSubscriptionManager.INTENT_KEY_SIM_COUNT, subCount);
            intent.putExtra(MtkSubscriptionManager.INTENT_KEY_NEW_SIM_SLOT, newSimStatus);
        } else if (detectedType == MtkSubscriptionManager.EXTRA_VALUE_REPOSITION_SIM) {
            intent.putExtra(MtkSubscriptionManager.INTENT_KEY_DETECT_STATUS,
                    MtkSubscriptionManager.EXTRA_VALUE_REPOSITION_SIM);
            intent.putExtra(MtkSubscriptionManager.INTENT_KEY_SIM_COUNT, subCount);
        } else if (detectedType == MtkSubscriptionManager.EXTRA_VALUE_REMOVE_SIM) {
            intent.putExtra(MtkSubscriptionManager.INTENT_KEY_DETECT_STATUS,
                    MtkSubscriptionManager.EXTRA_VALUE_REMOVE_SIM);
            intent.putExtra(MtkSubscriptionManager.INTENT_KEY_SIM_COUNT, subCount);
        } else if (detectedType == MtkSubscriptionManager.EXTRA_VALUE_NOCHANGE) {
            intent.putExtra(MtkSubscriptionManager.INTENT_KEY_DETECT_STATUS,
                    MtkSubscriptionManager.EXTRA_VALUE_NOCHANGE);
        }

        intent.putExtra(MtkSubscriptionManager.INTENT_KEY_PROP_KEY, "");

        logd("[setUpdatedData]- [" + detectedType + ", " + subCount + ", " + newSimStatus + "]");
        // SIM ME LOCK - Start
        if (mIsSmlLockMode) {
            updateNewSmlInfo(detectedType, subCount);
            triggerUpdateInternalSimMountState(-1);
        }
        // SIM ME LOCK - End
        return intent;
    }

    private boolean checkAllIccIdReady() {
        String iccId = "";
        logd("checkAllIccIdReady +, retry_count = " + mReadIccIdCount);
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            iccId = SystemProperties.get(PROPERTY_ICCID_SIM[i], "");
            if (iccId.length() == 3) {
                logd("No SIM insert :" + i);
            }
            if (iccId.equals("")) {
                return false;
            }
            logd("iccId[" + i + "] = " + SubscriptionInfo.givePrintableIccid(iccId));
        }

        return true;
    }

    private void updateSubscriptionInfoIfNeed() {
        boolean needUpdate = false;
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mIccId[i] == null ||
                    !mIccId[i].equals(SystemProperties.get(PROPERTY_ICCID_SIM[i], ""))) {
                mIccId[i] = SystemProperties.get(PROPERTY_ICCID_SIM[i], "");
                needUpdate = true;
                logd("[updateSubscriptionInfoIfNeed] icc id change, slot[" + i + "]"
                        + " needUpdate: " + needUpdate);
            }
        }

        if (isAllIccIdQueryDone() && needUpdate) {
            updateSubscriptionInfoByIccId();
        }
    }

    private Integer getCiIndex(Message msg) {
        AsyncResult ar;
        Integer index = new Integer(PhoneConstants.DEFAULT_CARD_INDEX);

        /*
         * The events can be come in two ways. By explicitly sending it using
         * sendMessage, in this case the user object passed is msg.obj and from
         * the CommandsInterface, in this case the user object is msg.obj.userObj
         */
        if (msg != null) {
            if (msg.obj != null && msg.obj instanceof Integer) {
                index = (Integer) msg.obj;
            } else if (msg.obj != null && msg.obj instanceof AsyncResult) {
                ar = (AsyncResult) msg.obj;
                if (ar.userObj != null && ar.userObj instanceof Integer) {
                    index = (Integer) ar.userObj;
                }
            }
        }
        return index;
    }

    private boolean checkIsAvailable() {
        boolean result = true;
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mIsUpdateAvailable[i] <= 0) {
                logd("mIsUpdateAvailable[" + i + "] = " + mIsUpdateAvailable[i]);
                result = false;
                break;
            }
        }
        logd("checkIsAvailable result = " + result);
        return result;
    }

    private final BroadcastReceiver mMtkReceiver = new  BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logd("onReceive, Action: " + action);

            if (!action.equals(TelephonyIntents.ACTION_COMMON_SLOT_NO_CHANGED)
                    && !action.equals(Intent.ACTION_LOCALE_CHANGED)) {
                return;
            }

            if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
                int[] subIdList = mSubscriptionManager.getActiveSubscriptionIdList();
                for (int subId : subIdList) {
                    updateSubName(subId);
                }
            } else if (action.equals(TelephonyIntents.ACTION_COMMON_SLOT_NO_CHANGED)) {
                int slotIndex = intent.getIntExtra(PhoneConstants.PHONE_KEY,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                logd("[Common Slot] NO_CHANTED, slotId: " + slotIndex);
                sendMessage(obtainMessage(EVENT_SIM_NO_CHANGED, slotIndex, -1));
            }
        }
    };

    private Runnable mReadIccIdPropertyRunnable = new Runnable() {
        public void run() {
            ++mReadIccIdCount;
            if (mReadIccIdCount <= 10) {
                if (!checkAllIccIdReady()) {
                    postDelayed(mReadIccIdPropertyRunnable, sReadICCID_retry_time);
                } else {
                    updateSubscriptionInfoIfNeed();
                }
            }
        }
    };

    private void updateSubName(int subId) {
        SubscriptionInfo subInfo =
                MtkSubscriptionManager.getSubInfo(null, subId);
        if (subInfo != null
                && subInfo.getNameSource() != SubscriptionManager.NAME_SOURCE_USER_INPUT) {
            MtkSpnOverride spnOverride = MtkSpnOverride.getInstance();
            String nameToSet;
            String carrierName = TelephonyManager.getDefault().getSimOperator(subId);
            int slotId = SubscriptionManager.getSlotIndex(subId);
            logd("updateSubName, carrierName = " + carrierName + ", subId = " + subId);
            if (SubscriptionManager.isValidSlotIndex(slotId)) {
                if (spnOverride.containsCarrierEx(carrierName)) {
                    nameToSet = spnOverride.lookupOperatorName(subId, carrierName,
                        true, mContext);
                    logd("SPN found, name = " + nameToSet);
                } else {
                    nameToSet = "CARD " + Integer.toString(slotId + 1);
                    logd("SPN not found, set name to " + nameToSet);
                }
                mSubscriptionManager.setDisplayName(nameToSet, subId);
            }
        }
    }
    // SIM ME LOCK - Start
    public void triggerUpdateInternalSimMountState(int slotId) {
        logd("triggerUpdateInternalSimMountState slotId " + slotId);
        sendMessage(obtainMessage(EVENT_SIM_MOUNT_CHANGED, slotId));
    }

    private void resetSimMountChangeState() {
        boolean needReport = false;
        for (int i = 0; i< 4; i++) {
            if (newSmlInfo[i] != oldSmlInfo[i]) {
                needReport = true;
                break;
            }
        }
        if (needReport) {
            int newDetectedType = newSmlInfo[0];
            int newSimCount = newSmlInfo[1];
            int newValid1 = newSmlInfo[2];
            int newValid2 = newSmlInfo[3];
            Intent intent = new Intent(TelephonyIntents.ACTION_SIM_SLOT_SIM_MOUNT_CHANGE);
            intent.putExtra(MtkIccCardConstants.INTENT_KEY_SML_SLOT_DETECTED_TYPE, newDetectedType);
            intent.putExtra(MtkIccCardConstants.INTENT_KEY_SML_SLOT_SIM_COUNT, newSimCount);
            intent.putExtra(MtkIccCardConstants.INTENT_KEY_SML_SLOT_SIM1_VALID, newValid1);
            intent.putExtra(MtkIccCardConstants.INTENT_KEY_SML_SLOT_SIM2_VALID, newValid2);
            //TODO: remove log after IT done
            logd("Broadcasting ACTION_SIM_SLOT_SIM_MOUNT_CHANGE,  detected type: "
                    + newDetectedType + ", newSubCount: " + newSimCount + ", SIM 1 valid"
                    + newValid1 +  ", SIM 2 valid" + newValid2);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            updateOldSmlInfo(newDetectedType, newSimCount,newValid1, newValid2);
         } else {
             logd("resetSimMountChangeState no  need report ");
         }
     }

    public void updateNewSmlInfo(int detectedType, int simCount) {
        synchronized (mLockUpdateNew) {
            newSmlInfo[0] = detectedType;
            newSmlInfo[1] = simCount;
            newSmlInfo[2] = MtkTelephonyManagerEx.getDefault().checkValidCard(0);
            newSmlInfo[3] = MtkTelephonyManagerEx.getDefault().checkValidCard(1);
            logd("[updateNewSmlInfo]- [" + newSmlInfo[0] + ", " + newSmlInfo[1] + ", "
                    + newSmlInfo[2] + ", " + newSmlInfo[3] + "]");
        }
   }

    public void updateOldSmlInfo(int detectedType, int simCount, int valid1, int valid2) {
        synchronized (mLockUpdateOld) {
            oldSmlInfo[0] = detectedType;
            oldSmlInfo[1] = simCount;
            oldSmlInfo[2] = valid1;
            oldSmlInfo[3] = valid2;
        }
   }
   // SIM ME LOCK - End

    private void logd(String message) {
        Rlog.d(LOG_TAG, message);
    }
}
