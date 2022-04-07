package com.mediatek.internal.telephony.datasub;

import android.content.Context;
import android.content.Intent;

import android.os.Build;
import android.os.SystemProperties;

import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SubscriptionController;

import com.mediatek.internal.telephony.MtkIccCardConstants;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;

import com.mediatek.internal.telephony.datasub.DataSubConstants;

import com.mediatek.telephony.MtkTelephonyManagerEx;

import java.util.ArrayList;
import java.util.Arrays;

public class DataSubSelectorOpExt implements IDataSubSelectorOPExt {
    private static final boolean USER_BUILD = TextUtils.equals(Build.TYPE, "user");

    private static boolean DBG = true;

    private static String LOG_TAG = "DSSExt";

    private static Context mContext = null;

    private static DataSubSelectorOpExt mInstance = null;

    private static DataSubSelector mDataSubSelector = null;

    private static ISimSwitchForDSSExt mSimSwitchForDSS = null;

    private static CapabilitySwitch mCapabilitySwitch = null;

    private Intent mIntent = null;

    // marker for retry cause
    private static final int DSS_RET_INVALID_PHONE_INDEX = -1;
    private static final int DSS_RET_CANNOT_GET_SIM_INFO = -2;

    // check result of SIM ME LOCK
    // already set default data by policy
    private static final int SML_CHECK_SWITCH_DONE = 1;
    // waiting for vail card info update
    private static final int SML_CHECK_WAIT_VAILD_CARD_INFO = 2;
    // follow OM flow after check(need set capability)
    private static final int SML_CHECK_FOLLOW_OM = 3;
    // follow OM flow but do notthing after check
    private static final int SML_CHECK_FOLLOW_OM_DO_NOTHING = 4;

    public DataSubSelectorOpExt(Context context) {
        mContext = context;
    }

    public void init(DataSubSelector dataSubSelector, ISimSwitchForDSSExt simSwitchForDSS) {
        mDataSubSelector = dataSubSelector;
        mCapabilitySwitch = CapabilitySwitch.getInstance(mContext, dataSubSelector);
        mSimSwitchForDSS = simSwitchForDSS;
    }

    @Override
    public void handleSimStateChanged(Intent intent) {
        int simStatus = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE,
                        TelephonyManager.SIM_STATE_UNKNOWN);
        int slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY, PhoneConstants.SIM_ID_1);
        if (simStatus == TelephonyManager.SIM_STATE_LOADED) {
            mCapabilitySwitch.handleSimImsiStatus(intent);

            handleNeedWaitImsi(intent);
            handleNeedWaitUnlock(intent);
        } else if (simStatus == TelephonyManager.SIM_STATE_NOT_READY) {
            mCapabilitySwitch.handleSimImsiStatus(intent);
        }
    }

    @Override
    public void handleSubinfoRecordUpdated(Intent intent) {
        int detectedType = intent.getIntExtra(MtkSubscriptionManager.INTENT_KEY_DETECT_STATUS,
                    MtkSubscriptionManager.EXTRA_VALUE_NOCHANGE);

        // sync main capability to default data sim
        if (isSubIdReady(intent) == true) {
            subSelector(intent);
        }
    }

    private void handleNeedWaitImsi(Intent intent) {
        if (CapabilitySwitch.isNeedWaitImsi()) {
            CapabilitySwitch.setNeedWaitImsi(Boolean.toString(false));
            subSelector(intent);
        }
        if (CapabilitySwitch.isNeedWaitImsiRoaming() == true) {
            CapabilitySwitch.setNeedWaitImsiRoaming(Boolean.toString(false));
        }
    }

    private void handleNeedWaitUnlock(Intent intent) {
        if (CapabilitySwitch.isNeedWaitUnlock()) {
            CapabilitySwitch.setNeedWaitUnlock("false");
            subSelector(intent);
        }
        if (CapabilitySwitch.isNeedWaitUnlockRoaming()) {
            CapabilitySwitch.setNeedWaitUnlockRoaming("false");
        }
    }

    private boolean isSubIdReady(Intent intent) {
        for (int i = 0; i < mDataSubSelector.getPhoneNum(); i++) {
            int[] subIdList = SubscriptionManager.getSubId(i);
            if (subIdList == null || subIdList.length < 1) {
                log("isSubIdReady, subIdList is null : phone" + i);
                return false;
            }
        }
        return true;
    }

    private int getHighCapabilityPhoneIdBySimType() {
        int phoneId = DSS_RET_INVALID_PHONE_INDEX;
        int[] simOpInfo = new int[mDataSubSelector.getPhoneNum()];
        int[] simType = new int[mDataSubSelector.getPhoneNum()];
        int insertedState = 0;
        int insertedSimCount = 0;
        int tSimCount = 0;
        int wSimCount = 0;
        int cSimCount = 0;
        String[] currIccId = new String[mDataSubSelector.getPhoneNum()];

        if (RadioCapabilitySwitchUtil.isPS2SupportLTE() && mDataSubSelector.getPhoneNum() == 2) {
            // check sim cards number
            for (int i = 0; i < mDataSubSelector.getPhoneNum(); i++) {
                currIccId[i] = DataSubSelectorUtil.getIccidFromProp(i);
                if (currIccId[i] == null || "".equals(currIccId[i])) {
                    log("sim not ready, can not get high capability phone id");
                    return DSS_RET_INVALID_PHONE_INDEX;
                }
                if (!DataSubConstants.NO_SIM_VALUE.equals(currIccId[i])) {
                    ++insertedSimCount;
                    insertedState = insertedState | (1 << i);
                }
            }

            // no sim card
            if (insertedSimCount == 0) {
                log("no sim card, don't switch");
                return DSS_RET_INVALID_PHONE_INDEX;
            }

            // check sim info
            if (RadioCapabilitySwitchUtil.getSimInfo(simOpInfo, simType, insertedState) == false) {
                log("cannot get sim operator info, don't switch");
                return DSS_RET_CANNOT_GET_SIM_INFO;
            }

            for (int i = 0; i < mDataSubSelector.getPhoneNum(); i++) {
                if (RadioCapabilitySwitchUtil.SIM_OP_INFO_OP01 == simOpInfo[i]) {
                    tSimCount++;
                } else if (RadioCapabilitySwitchUtil.isCdmaCard(i, simOpInfo[i], mContext)) {
                    cSimCount++;
                    simOpInfo[i] = RadioCapabilitySwitchUtil.SIM_OP_INFO_OP09;
                } else if (RadioCapabilitySwitchUtil.SIM_OP_INFO_UNKNOWN!= simOpInfo[i]){
                    wSimCount++;
                    simOpInfo[i] = RadioCapabilitySwitchUtil.SIM_OP_INFO_OP02;
                }
            }
            log("getHighCapabilityPhoneIdBySimType : Inserted SIM count: " + insertedSimCount
                + ", insertedStatus: " + insertedState + ", tSimCount: " + tSimCount
                + ", wSimCount: " + wSimCount + ", cSimCount: " + cSimCount
                + Arrays.toString(simOpInfo));

            // t + w --> if support real T+W, always on t card
            if (RadioCapabilitySwitchUtil.isSupportSimSwitchEnhancement(
                    RadioCapabilitySwitchUtil.ENHANCEMENT_T_PLUS_W)
                    && RadioCapabilitySwitchUtil.isTPlusWSupport()) {
                if (simOpInfo[0] == RadioCapabilitySwitchUtil.SIM_OP_INFO_OP01 &&
                    simOpInfo[1] == RadioCapabilitySwitchUtil.SIM_OP_INFO_OP02) {
                    phoneId = 0;
                } else if (simOpInfo[0] == RadioCapabilitySwitchUtil.SIM_OP_INFO_OP02 &&
                    simOpInfo[1] == RadioCapabilitySwitchUtil.SIM_OP_INFO_OP01) {
                    phoneId = 1;
                }
            }

            // t + c --> always on c card
            if (RadioCapabilitySwitchUtil.isSupportSimSwitchEnhancement(
                    RadioCapabilitySwitchUtil.ENHANCEMENT_T_PLUS_C)) {
                if (simOpInfo[0] == RadioCapabilitySwitchUtil.SIM_OP_INFO_OP01 &&
                    RadioCapabilitySwitchUtil.isCdmaCard(1, simOpInfo[1], mContext)) {
                    phoneId = 1;
                } else if (RadioCapabilitySwitchUtil.isCdmaCard(0, simOpInfo[0], mContext) &&
                    simOpInfo[1] == RadioCapabilitySwitchUtil.SIM_OP_INFO_OP01) {
                    phoneId = 0;
                }
            }

            // w + c--> always on c card
            if (RadioCapabilitySwitchUtil.isSupportSimSwitchEnhancement(
                    RadioCapabilitySwitchUtil.ENHANCEMENT_W_PLUS_C)) {
                if (RadioCapabilitySwitchUtil.isCdmaCard(0, simOpInfo[0], mContext) &&
                    simOpInfo[1] == RadioCapabilitySwitchUtil.SIM_OP_INFO_OP02) {
                    phoneId = 0;
                } else if (simOpInfo[0] == RadioCapabilitySwitchUtil.SIM_OP_INFO_OP02 &&
                    RadioCapabilitySwitchUtil.isCdmaCard(1, simOpInfo[1], mContext)) {
                    phoneId = 1;
                }
            }
        }
        log("getHighCapabilityPhoneIdBySimType : " + phoneId);
        return phoneId;
    }

    @Override
    public void subSelector(Intent intent) {
        // only handle 3/4G switching
        int phoneId = DSS_RET_INVALID_PHONE_INDEX;
        String[] currIccId = new String[mDataSubSelector.getPhoneNum()];

        // Sim ME lock
        int checkResult = SML_CHECK_FOLLOW_OM;
        if (MtkTelephonyManagerEx.getDefault().getSimLockPolicy() !=
                MtkIccCardConstants.SML_SLOT_LOCK_POLICY_NONE) {

            checkResult = preCheckForSimMeLock(intent);
            log("preCheckForSimMeLock result=" + checkResult);

            if ((checkResult == SML_CHECK_SWITCH_DONE) ||
                    (checkResult == SML_CHECK_FOLLOW_OM_DO_NOTHING) ||
                    (checkResult == SML_CHECK_WAIT_VAILD_CARD_INFO)) {
                return;
            }
        }

        phoneId = getHighCapabilityPhoneIdBySimType();

        if (phoneId == DSS_RET_CANNOT_GET_SIM_INFO) {
            CapabilitySwitch.setNeedWaitImsi(Boolean.toString(true));
        } else if (phoneId == DSS_RET_INVALID_PHONE_INDEX) {
            //Get previous default data
            //Modify the way for get defaultIccid,
            //because the SystemProperties may not update on time
            String defaultIccid = "";
            int defDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
            int defDataPhoneId = SubscriptionManager.getPhoneId(defDataSubId);
            if (defDataPhoneId >= 0) {
                if (defDataPhoneId >= DataSubSelectorUtil.getIccidNum()) {
                   log("phoneId out of boundary :" + defDataPhoneId);
                } else {
                   defaultIccid = DataSubSelectorUtil.getIccidFromProp(defDataPhoneId);
                }
            }
            if (("N/A".equals(defaultIccid)) || ("".equals(defaultIccid))) {
                return;
            }
            for (int i = 0; i < mDataSubSelector.getPhoneNum(); i++) {
                currIccId[i] = DataSubSelectorUtil.getIccidFromProp(i);
                if (currIccId[i] == null || "".equals(currIccId[i])) {
                    log("error: iccid not found, wait for next sub ready");
                    return;
                }
                if (defaultIccid.equals(currIccId[i])) {
                    phoneId = i;
                    break;
                }
            }
        }

        // check pin lock
        if (mCapabilitySwitch.isSimUnLocked() == false) {
            log("DataSubSelector for OM: do not switch because of sim locking");
            CapabilitySwitch.setNeedWaitUnlock("true");
            mIntent = intent;
            CapabilitySwitch.setSimStatus(intent);
            CapabilitySwitch.setNewSimSlot(intent);
            return;
        } else {
            log("DataSubSelector for OM: no pin lock");
            CapabilitySwitch.setNeedWaitUnlock("false");
        }

        log("Default data phoneid = " + phoneId);
        if (phoneId >= 0) {
            // always set capability to this phone
            mCapabilitySwitch.setCapabilityIfNeeded(phoneId);
        }

        // clean system property
        CapabilitySwitch.resetSimStatus();
        CapabilitySwitch.resetNewSimSlot();
    }

    @Override
    public void handleAirPlaneModeOff(Intent intent) {
        if (isSubIdReady(intent) == true) {
            subSelector(intent);
        }
    }

    public void handlePlmnChanged(Intent intent) {}

    public void handleDefaultDataChanged(Intent intent) {}

    public void handleSimMeLock(Intent intent) {
        subSelector(intent);
    }

    private int preCheckForSimMeLock(Intent intent) {
        int simLockPolicy = MtkTelephonyManagerEx.getDefault().getSimLockPolicy();
        int[] simSlotvaildInfo = new int[mDataSubSelector.getPhoneNum()];

        int phoneId = SubscriptionManager.INVALID_PHONE_INDEX;
        int slotId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        int vaildState = MtkIccCardConstants.SML_SLOT_LOCK_POLICY_VALID_CARD_UNKNOWN;

        int tempDefaultDataPhone = DSS_RET_INVALID_PHONE_INDEX;

        int simCount = 0;
        int unlockedSimCount = 0;
        int simValidCount = 0;

        if ((simLockPolicy == MtkIccCardConstants.SML_SLOT_LOCK_POLICY_ONLY_SLOT1) ||
                (simLockPolicy == MtkIccCardConstants.SML_SLOT_LOCK_POLICY_ONLY_SLOT2) ||
                (simLockPolicy == MtkIccCardConstants.SML_SLOT_LOCK_POLICY_ALL_SLOTS_INDIVIDUAL) ||
                (simLockPolicy == MtkIccCardConstants.SML_SLOT_LOCK_POLICY_LK_SLOTA_RESTRICT_INVALID_CS)) {
            phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY,
                    SubscriptionManager.INVALID_PHONE_INDEX);
            slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY,
                    SubscriptionManager.INVALID_SIM_SLOT_INDEX);
            subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            vaildState = intent.getIntExtra(MtkIccCardConstants.INTENT_KEY_SML_SLOT_SIM_VALID,
                    MtkIccCardConstants.SML_SLOT_LOCK_POLICY_VALID_CARD_UNKNOWN);

            log("preCheckForSimMeLock() phoneId=" + phoneId + " slotId="+ slotId +
                    " subId= " + subId + " vaildState=" + vaildState);

            for (int i = 0; i < mDataSubSelector.getPhoneNum(); i++) {
                simSlotvaildInfo[i] = MtkTelephonyManagerEx.getDefault().checkValidCard(i);
                log("preCheckForSimMeLock() simSlotvaildInfo[" + i + "]=" + simSlotvaildInfo[i]);

                if (simSlotvaildInfo[i] == MtkIccCardConstants.SML_SLOT_LOCK_POLICY_VALID_CARD_UNKNOWN) {
                    // wait for sim slot vaild state update
                    log("preCheckForSimMeLock() wait for sim vaild state update");
                    return SML_CHECK_WAIT_VAILD_CARD_INFO;
                } else if (simSlotvaildInfo[i] != MtkIccCardConstants.SML_SLOT_LOCK_POLICY_VALID_CARD_ABSENT) {
                    simCount = simCount + 1;
                }
            }

            if (simCount == 1) {
                // check if any unlocked SIM
                for (int i = 0; i < mDataSubSelector.getPhoneNum(); i++) {
                    if (simSlotvaildInfo[i] == MtkIccCardConstants.SML_SLOT_LOCK_POLICY_VALID_CARD_YES) {
                        log("preCheckForSimMeLock() only one unlocked sim in slot" + i);
                        return SML_CHECK_FOLLOW_OM;
                    }
                }
                log("preCheckForSimMeLock() only one locked sim in slot");
                return SML_CHECK_FOLLOW_OM_DO_NOTHING;
            } else {
                // find unlock sim
                for (int i = 0; i < mDataSubSelector.getPhoneNum(); i++) {
                    if (simSlotvaildInfo[i] == MtkIccCardConstants.SML_SLOT_LOCK_POLICY_VALID_CARD_YES) {
                        tempDefaultDataPhone = i;
                        unlockedSimCount = unlockedSimCount + 1;
                    }
                }

                if (unlockedSimCount == 1) {
                    // set default data to unlock SIM slot
                    // case 5: one locked SIM + one Unlocked SIM
                    log("preCheckForSimMeLock() one unlocked SIM + n Unlocked SIM");
                    mCapabilitySwitch.setCapabilityIfNeeded(tempDefaultDataPhone);
                    mDataSubSelector.setDefaultData(tempDefaultDataPhone);
                    return SML_CHECK_SWITCH_DONE;
                } else if (unlockedSimCount > 1) {
                    // default data sub select by user for case 3
                    // follow OM: check card typ and set main capability
                    // case 3: two unlocked SIMs
                    log("preCheckForSimMeLock() two unlocked SIMs");
                    return SML_CHECK_FOLLOW_OM;
                } else {
                    log("preCheckForSimMeLock() two locked SIMs");
                    return SML_CHECK_FOLLOW_OM_DO_NOTHING;

                }
            }
        } else if ((simLockPolicy == MtkIccCardConstants.SML_SLOT_LOCK_POLICY_LK_SLOT1) ||
                (simLockPolicy == MtkIccCardConstants.SML_SLOT_LOCK_POLICY_LK_SLOT2) ||
                (simLockPolicy == MtkIccCardConstants.SML_SLOT_LOCK_POLICY_LK_SLOTA)) {
            for (int i = 0; i < mDataSubSelector.getPhoneNum(); i++) {
                simSlotvaildInfo[i] = MtkTelephonyManagerEx.getDefault().checkValidCard(i);
                log("preCheckForSimMeLock() simSlotvaildInfo[" + i + "]=" + simSlotvaildInfo[i]);
                if (simSlotvaildInfo[i] ==
                        MtkIccCardConstants.SML_SLOT_LOCK_POLICY_VALID_CARD_UNKNOWN) {
                    // wait for sim slot vaild state update
                    log("preCheckForSimMeLock() wait for sim vaild state update");
                    return SML_CHECK_WAIT_VAILD_CARD_INFO;
                } else if (simSlotvaildInfo[i] ==
                        MtkIccCardConstants.SML_SLOT_LOCK_POLICY_VALID_CARD_YES) {
                    simValidCount = simValidCount + 1;
                }
            }
            log("preCheckForSimMeLock() simValidCount=" + simValidCount);
            if (simValidCount >= 1) {
                return SML_CHECK_FOLLOW_OM;
            } else {
                return SML_CHECK_FOLLOW_OM_DO_NOTHING;
            }
        } else if (simLockPolicy == MtkIccCardConstants.SML_SLOT_LOCK_POLICY_LEGACY) {
            log("Follow OM for Legacy, simLockPolicy=" + simLockPolicy);
            return SML_CHECK_FOLLOW_OM;
        } else {
            log("not handled simLockPolicy=" + simLockPolicy);
            return SML_CHECK_FOLLOW_OM_DO_NOTHING;
        }
    }

    private void log(String txt) {
        if (DBG) {
            Rlog.d(LOG_TAG, txt);
        }
    }

    private void loge(String txt) {
        if (DBG) {
            Rlog.e(LOG_TAG, txt);
        }
    }
}
