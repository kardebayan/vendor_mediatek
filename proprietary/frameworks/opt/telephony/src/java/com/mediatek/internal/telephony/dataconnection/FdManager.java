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
* have been modified by MediaTek Inc. All revisions are subject to any receiver\'s
* applicable license agreements with MediaTek Inc.
*/

package com.mediatek.internal.telephony.dataconnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.os.AsyncResult;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.util.SparseArray;
import android.view.Display;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.internal.telephony.MtkRIL;

import java.util.ArrayList;

/**
 * FdManager is requested by 3GPP spec for power saving.
 * Register and receive events to send Fast dormancy configuration to modem.
 *
 * FastDormancy has 3 categories of configuration,
 * 1. Enable/disable FastDormancy
 * 2. Sync screen status to modem.
 * 3. Sync timer value to modem.
 *
 * The first configuration (enable/disable Fast dormancy) includes 2 parts,
 * 1. Allow/DisAllow to set the configuration.
 *    Allow when the slot has 3G4G radio capability, the slot is default
 *    data slot and satisfies some system properties.
 * 2. Enable/Disable Fast dormancy to modem.
 *    Enable when not charging, not tethering, and other conditions.
 */

public class FdManager extends Handler {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "FdManager";

    private static final String PROPERTY_FD_ON_CHARGE = "persist.vendor.fd.on.charge";
    private static final String PROPERTY_RIL_FD_MODE = "vendor.ril.fd.mode";
    private static final String PROPERTY_FD_SCREEN_OFF_ONLY = "vendor.fd.screen.off.only";

    // Timer Key
    private static final String STR_PROPERTY_FD_SCREEN_ON_TIMER =
            "persist.vendor.radio.fd.counter";
    private static final String STR_PROPERTY_FD_SCREEN_ON_R8_TIMER =
            "persist.vendor.radio.fd.r8.counter";
    private static final String STR_PROPERTY_FD_SCREEN_OFF_TIMER =
            "persist.vendor.radio.fd.off.counter";
    private static final String STR_PROPERTY_FD_SCREEN_OFF_R8_TIMER =
            "persist.vendor.radio.fd.off.r8.counter";

    // Current support fast dormancy mode
    private static final int FD_MODE = 1;

    // Timer Default Value
    private static final String DEFAULT_FD_SCREEN_ON_TIMER = "150";
    private static final String DEFAULT_FD_SCREEN_ON_R8_TIMER = "150";
    private static final String DEFAULT_FD_SCREEN_OFF_TIMER = "50";
    private static final String DEFAULT_FD_SCREEN_OFF_R8_TIMER = "50";

    // Charging state
    private static final boolean IN_CHARGING = true;
    private static final boolean NOT_IN_CHARGING = false;

    private static final int EVENT_BASE = 0;
    private static final int EVENT_FD_MODE_SET = EVENT_BASE + 0;
    private static final int EVENT_RADIO_ON = EVENT_BASE + 1;

    private static final boolean MTK_FD_SUPPORT = (Integer.parseInt(SystemProperties
            .get("ro.vendor.mtk_fd_support", "0")) == 1 ? true : false);

    private static final SparseArray<FdManager> sFdManagers = new SparseArray<FdManager>();
    private Phone mPhone;
    private DisplayManager mDisplayManager;

    private boolean mIsCharging = false;
    private boolean mIsTetheredMode = false;
    private int mEnableFdOnCharing = Integer.parseInt(SystemProperties.get(
            PROPERTY_FD_ON_CHARGE, "0"));
    private boolean mIsScreenOn = true;

    // Time Unit:0.1 sec => {5sec, 15sec, 5sec, 15sec}
    private static String sTimerValue[] = {"50", "150", "50", "150"};

    private enum FdModeType {
        DISABLE_MD_FD,
        ENABLE_MD_FD,
        SET_FD_INACTIVITY_TIMER,
        INFO_MD_SCREEN_STATUS
    }

    private enum FdTimerType {
        SCREEN_OFF_LEGACY_FD,
        SCREEN_ON_LEGACY_FD,
        SCREEN_OFF_R8_FD,
        SCREEN_ON_R8_FD,
        SUPPORT_TIMER_TYPES
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
            case BatteryManager.ACTION_CHARGING:
                logd("mIntentReceiver: Received ACTION_CHARGING");
                onChargingModeSwitched(IN_CHARGING);
                break;
            case BatteryManager.ACTION_DISCHARGING:
                logd("mIntentReceiver: Received ACTION_DISCHARGING");
                onChargingModeSwitched(NOT_IN_CHARGING);
                break;
            case ConnectivityManager.ACTION_TETHER_STATE_CHANGED:
                ArrayList<String> active = intent
                        .getStringArrayListExtra(ConnectivityManager.EXTRA_ACTIVE_TETHER);
                mIsTetheredMode = ((active != null) && (active.size() > 0));
                logd("mIntentReceiver: Received ACTION_TETHER_STATE_CHANGED"
                        + " mIsTetheredMode = " + mIsTetheredMode);
                onTetheringSwitched();
                break;
            case TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED:
                logd("mIntentReceiver: Received ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
                onDefaultDataSwitched();
                break;
            default:
                logw("mIntentReceiver: weird, should never be here!");
                break;
            }
        }
    };

    private final DisplayManager.DisplayListener mDisplayListener =
                                        new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            onScreenSwitched(isScreenOn());
        }
    };

    public static FdManager getInstance(Phone phone) {
        if (MTK_FD_SUPPORT) {
            if (phone != null) {
                int phoneId = getPhoneId(phone);
                if (!SubscriptionManager.isValidPhoneId(phoneId)) {
                    Rlog.e(LOG_TAG, "phoneId " + phoneId + " is invalid!");
                    return null;
                }
                FdManager fdMgr = sFdManagers.get(phoneId);
                if (fdMgr == null) {
                    Rlog.d(LOG_TAG, "FdManager " + phoneId + " doesn't exist, create one");
                    fdMgr = new FdManager(phone);
                    sFdManagers.put(phoneId, fdMgr);
                }
                return fdMgr;
            }
        }

        Rlog.e(LOG_TAG, "Fast dormancy feature is not enabled or FdManager initialize fail");
        return null;
    }

    private FdManager(Phone p) {
        mPhone = p;

        mIsCharging = isDeviceCharging();
        logd("Initial FdManager: mIsCharging = " + mIsCharging);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BatteryManager.ACTION_CHARGING);
        filter.addAction(BatteryManager.ACTION_DISCHARGING);
        filter.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);

        mPhone.getContext().registerReceiver(mIntentReceiver, filter, null, mPhone);

        /** Register radio on is to send fast dormancy configuration to modem.
         * Like in the situations of modem off, rild will ignore fast dormancy
         * request and modem will keep in old configuration, so need resend
         * states to modem to keep modem in newest configuration.
         */
        mPhone.mCi.registerForOn(this, EVENT_RADIO_ON, null);

        mDisplayManager = (DisplayManager) mPhone.getContext()
                .getSystemService(Context.DISPLAY_SERVICE);
        mDisplayManager.registerDisplayListener(mDisplayListener, null);

        initFdTimer();
    }

    public void dispose() {
        logd("Dispose FdManager");

        if (MTK_FD_SUPPORT) {
            mPhone.getContext().unregisterReceiver(this.mIntentReceiver);
            mPhone.mCi.unregisterForOn(this);
            mDisplayManager.unregisterDisplayListener(mDisplayListener);
            sFdManagers.remove(getPhoneId(mPhone));
        }
    }

    private void initFdTimer() {
        String timerStr[] = new String[4];
        timerStr[0] = SystemProperties.get(STR_PROPERTY_FD_SCREEN_OFF_TIMER,
                DEFAULT_FD_SCREEN_OFF_TIMER);
        sTimerValue[FdTimerType.SCREEN_OFF_LEGACY_FD.ordinal()] = Integer
                .toString((int) (Double.parseDouble(timerStr[0])));
        timerStr[1] = SystemProperties.get(STR_PROPERTY_FD_SCREEN_ON_TIMER,
                DEFAULT_FD_SCREEN_ON_TIMER);
        sTimerValue[FdTimerType.SCREEN_ON_LEGACY_FD.ordinal()] = Integer
                .toString((int) (Double.parseDouble(timerStr[1])));
        timerStr[2] = SystemProperties.get(STR_PROPERTY_FD_SCREEN_OFF_R8_TIMER,
                DEFAULT_FD_SCREEN_OFF_R8_TIMER);
        sTimerValue[FdTimerType.SCREEN_OFF_R8_FD.ordinal()] = Integer
                .toString((int) (Double.parseDouble(timerStr[2])));
        timerStr[3] = SystemProperties.get(STR_PROPERTY_FD_SCREEN_ON_R8_TIMER,
                DEFAULT_FD_SCREEN_ON_R8_TIMER);
        sTimerValue[FdTimerType.SCREEN_ON_R8_FD.ordinal()] = Integer
                .toString((int) (Double.parseDouble(timerStr[3])));
        logd("initFdTimer: timers = " + sTimerValue[0] + ", " + sTimerValue[1]
                + ", " + sTimerValue[2] + ", " + sTimerValue[3]);
    }

    public int getNumberOfSupportedTypes() {
        return FdTimerType.SUPPORT_TIMER_TYPES.ordinal();
    }

    public int setFdTimerValue(String newTimerValue[], Message onComplete) {
        int fdMode = Integer.parseInt(SystemProperties.get(PROPERTY_RIL_FD_MODE, "0"));
        if (MTK_FD_SUPPORT && fdMode == FD_MODE && isFdAllowed()) {
            for (int i = 0; i < newTimerValue.length; i++) {
                sTimerValue[i] = newTimerValue[i];
            }

            ((MtkRIL)(mPhone.mCi)).setFdMode(
                    FdModeType.SET_FD_INACTIVITY_TIMER.ordinal(),
                    FdTimerType.SCREEN_OFF_LEGACY_FD.ordinal(),
                    Integer.parseInt(sTimerValue[FdTimerType.SCREEN_OFF_LEGACY_FD.ordinal()]),
                    null);
            ((MtkRIL)(mPhone.mCi)).setFdMode(
                    FdModeType.SET_FD_INACTIVITY_TIMER.ordinal(),
                    FdTimerType.SCREEN_ON_LEGACY_FD.ordinal(),
                    Integer.parseInt(sTimerValue[FdTimerType.SCREEN_ON_LEGACY_FD.ordinal()]),
                    null);
            ((MtkRIL)(mPhone.mCi)).setFdMode(
                    FdModeType.SET_FD_INACTIVITY_TIMER.ordinal(),
                    FdTimerType.SCREEN_OFF_R8_FD.ordinal(),
                    Integer.parseInt(sTimerValue[FdTimerType.SCREEN_OFF_R8_FD.ordinal()]),
                    null);
            ((MtkRIL)(mPhone.mCi)).setFdMode(
                    FdModeType.SET_FD_INACTIVITY_TIMER.ordinal(),
                    FdTimerType.SCREEN_ON_R8_FD.ordinal(),
                    Integer.parseInt(sTimerValue[FdTimerType.SCREEN_ON_R8_FD.ordinal()]),
                    onComplete);

            logd("setFdTimerValue: sTimerValue = " + sTimerValue[0] + ", "
                    + sTimerValue[1] + ", " + sTimerValue[2] + ", "
                    + sTimerValue[3]);
        }

        return 0;
    }

    public int setFdTimerValue(String newTimerValue[], Message onComplete,
            Phone phone) {
        FdManager fdMgr = getInstance(phone);
        if (fdMgr != null) {
            fdMgr.setFdTimerValue(newTimerValue, onComplete);
        } else {
            logw("setFdTimerValue: fail!");
        }

        return 0;
    }

    public static String[] getFdTimerValue() {
        return sTimerValue;
    }

    @Override
    public void handleMessage(Message msg) {
        logd("handleMessage: msg.what = " + msg.what);

        switch (msg.what) {
        case EVENT_FD_MODE_SET:
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                loge("handleMessage: RIL_REQUEST_SET_FD_MODE error!");
            }
            break;
        case EVENT_RADIO_ON:
            onRadioOn();
            break;
        default:
            logw("handleMessage: weird, should never be here!");
            break;
        }
    }

    /**
     * @return True if one of the device's screen (e.g. main screen, wifi display,
     *         HDMI display, or Android auto, etc...) is on.
     */
    private boolean isScreenOn() {
        /** Note that we don't listen to Intent.SCREEN_ON and Intent.SCREEN_OFF
         * because they are no longer adequate for monitoring the screen state
         * since they are not sent in cases where the screen is turned off transiently
         * such as due to the proximity sensor.
         */
        final DisplayManager dm = (DisplayManager) mPhone.getContext()
                .getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = dm.getDisplays();
        if (displays != null) {
            for (Display display : displays) {
                // Anything other than STATE_ON is treated as screen off, such
                // as STATE_DOZE, STATE_DOZE_SUSPEND, etc...
                if (display.getState() == Display.STATE_ON) {
                    logd("isScreenOn: Screen "
                            + Display.typeToString(display.getType()) + " on");
                    return true;
                }
            }
            logd("isScreenOn: Screens all off");
            return false;
        }
        logd("isScreenOn: No displays found");
        return false;
    }

    private boolean isDeviceCharging() {
        final BatteryManager bm = (BatteryManager) mPhone.getContext().getSystemService(
                Context.BATTERY_SERVICE);
        return bm.isCharging();
    }

    private boolean isDefaultDataSubId(int subId) {
        int dataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
        logd("isDefaultDataSubId: subId = " + subId + " dataSubId = " + dataSubId);

        return (SubscriptionManager.isUsableSubIdValue(subId) && (subId == dataSubId));
    }

    private boolean isRadioCapabilityValid(Phone phone) {
        return ((phone.getRadioAccessFamily() & RadioAccessFamily.RAF_LTE)
                == RadioAccessFamily.RAF_LTE ||
                (phone.getRadioAccessFamily() & RadioAccessFamily.RAF_UMTS)
                == RadioAccessFamily.RAF_UMTS);
    }

    private boolean isFdAllowed() {
        // Allowed when current SIM is default data SIM and has 3G/4G radio capability.
        return (isRadioCapabilityValid(mPhone) && isDefaultDataSubId(mPhone.getSubId()));
    }

    private boolean shouldEnableFd() {
        if (!(isFdEnabledOnlyWhenScreenOff() && mIsScreenOn)
                && !(mIsCharging && (mEnableFdOnCharing == 0))
                && !mIsTetheredMode) {
            return true;
        }
        return false;
    }

    private void updateFdModeIfNeeded() {
        int fdMode = Integer.parseInt(SystemProperties.get(PROPERTY_RIL_FD_MODE, "0"));
        if (fdMode == FD_MODE && isFdAllowed()) {
            boolean enable = shouldEnableFd();
            if (enable) {
                ((MtkRIL)(mPhone.mCi)).setFdMode(FdModeType.ENABLE_MD_FD.ordinal(), -1, -1,
                        obtainMessage(EVENT_FD_MODE_SET));
            } else {
                ((MtkRIL)(mPhone.mCi)).setFdMode(FdModeType.DISABLE_MD_FD.ordinal(), -1, -1,
                        obtainMessage(EVENT_FD_MODE_SET));
            }
        }
    }

    private void updateScreenStatusIfNeeded() {
        if (isFdAllowed()) {
            int screenState = (mIsScreenOn ? 1 : 0);
            ((MtkRIL)(mPhone.mCi)).setFdMode(
                    FdModeType.INFO_MD_SCREEN_STATUS.ordinal(), screenState, -1,
                    obtainMessage(EVENT_FD_MODE_SET));
        }
    }

    private void onRadioOn() {
        logd("onRadioOn: update fd status when radio on");
        updateScreenStatusIfNeeded();
        updateFdModeIfNeeded();
    }

    private void onScreenSwitched(boolean isScreenOn) {
        logd("onScreenSwitched: screenOn = " + isScreenOn);
        mIsScreenOn = isScreenOn;
        updateScreenStatusIfNeeded();

        if (isFdEnabledOnlyWhenScreenOff()) {
            updateFdModeIfNeeded();
        }
    }

    private void onChargingModeSwitched(boolean isCharging) {
        boolean preCharging = mIsCharging;
        mIsCharging = isCharging;
        int preEnableFdonCharging = mEnableFdOnCharing;
        mEnableFdOnCharing = Integer.parseInt(SystemProperties.get(
                PROPERTY_FD_ON_CHARGE, "0"));

        logd("onChargingModeSwitched: preCharging = " + preCharging
                + " mIsCharging = " + mIsCharging
                + " preEnableFdonCharging = " + preEnableFdonCharging
                + " mEnableFdOnCharing = " + mEnableFdOnCharing);

        if ((preCharging != mIsCharging)
                || (preEnableFdonCharging != mEnableFdOnCharing)) {
            updateFdModeIfNeeded();
        }
    }

    private void onTetheringSwitched() {
        updateFdModeIfNeeded();
    }

    private void onDefaultDataSwitched() {
        updateFdModeIfNeeded();
    }

    private static boolean isFdEnabledOnlyWhenScreenOff() {
        return (SystemProperties.getInt(PROPERTY_FD_SCREEN_OFF_ONLY, 0) == 1);
    }

    private static int getPhoneId(Phone phone) {
        return phone.getPhoneId();
    }

    private void logd(String s) {
        Rlog.d(LOG_TAG, "[phoneId" + getPhoneId(mPhone) + "]" + s);
    }

    private void logw(String s) {
        Rlog.w(LOG_TAG, "[phoneId" + getPhoneId(mPhone) + "]" + s);
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, "[phoneId" + getPhoneId(mPhone) + "]" + s);
    }
}
