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

import static android.provider.Telephony.ServiceStateTable.getContentValuesForServiceState;
import static android.provider.Telephony.ServiceStateTable.getUriForSubscriptionId;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.hardware.radio.V1_0.CellInfoType;
import android.os.AsyncResult;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.WorkSource;
import android.provider.Settings;
import android.telephony.AccessNetworkConstants;
import android.telephony.Rlog;
import android.telephony.CarrierConfigManager;
import android.telephony.CellInfo;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityTdscdma;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellLocation;
import android.telephony.DataSpecificRegistrationStates;
import android.telephony.NetworkRegistrationState;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.TelephonyManager;
import android.telephony.VoiceSpecificRegistrationStates;
import android.telephony.SignalStrength;
import android.telephony.cdma.CdmaCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.StatsLog;
import android.util.TimeUtils;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.HbpcdUtils;
import com.android.internal.telephony.RestrictedState;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.util.NotificationChannelController;
import com.mediatek.internal.telephony.uicc.MtkIccRefreshResponse;
import com.mediatek.internal.telephony.uicc.MtkIccConstants;
import com.mediatek.internal.telephony.uicc.MtkSIMRecords;

import mediatek.telephony.MtkServiceState;
import mediatek.telephony.MtkCarrierConfigManager;

import com.mediatek.internal.telephony.MtkRIL;
import com.mediatek.internal.telephony.MtkRILConstants;
///M: [Network][C2K] Add CDMA plus code to parse special MCC/MNC. @{
import com.mediatek.internal.telephony.cdma.pluscode.IPlusCodeUtils;
import com.mediatek.internal.telephony.cdma.pluscode.PlusCodeProcessor;
import com.mediatek.internal.telephony.uicc.MtkSpnOverride;
///  @}
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.OpTelephonyCustomizationFactoryBase;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
import com.mediatek.internal.telephony.dataconnection.MtkDcTracker;
import android.telephony.RadioAccessFamily;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Calendar;

import android.app.Notification;
import android.app.NotificationManager;

import com.mediatek.common.carrierexpress.CarrierExpressManager;

public class MtkServiceStateTracker extends ServiceStateTracker {
    private static final String LOG_TAG = "MTKSST";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;  // STOPSHIP if true

    /// M: [Network][C2K] for EriTriggeredPollState @{
    // Always broadcast service state to trigger possible roaming state change
    private boolean mEriTriggeredPollState = false;
    /// @}
    private boolean mEnableERI = false;
    private OpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory = null;
    private IServiceStateTrackerExt mServiceStateTrackerExt = null;

    private RegistrantList mDataRoamingTypeChangedRegistrants = new RegistrantList();

    protected static final String PROP_IWLAN_STATE = "persist.vendor.radio.wfc_state";
    protected static final String PROP_MTK_DATA_TYPE = "persist.vendor.radio.mtk_data_type";

    /* M: MTK added events begin */
    protected static final int EVENT_CS_NETWORK_STATE_CHANGED = 100;
    protected static final int EVENT_INVALID_SIM_INFO = 101; //ALPS00248788
    protected static final int EVENT_FEMTO_CELL_INFO = 102;
    protected static final int EVENT_PS_NETWORK_STATE_CHANGED = 103;
    protected static final int EVENT_NETWORK_EVENT = 104;
    protected static final int EVENT_MODULATION_INFO = 105;
    protected static final int EVENT_ICC_REFRESH = 106;
    protected static final int EVENT_IMEI_LOCK = 107; /* ALPS00296298 */
    protected static final int EVENT_SIM_OPL_LOADED = 119;
    /* MTK added events end */

    /* M: MTK handler */
    private static final int EVENT_MTK_GET_CELL_INFO_LIST = 1;
    private Handler mtkHandler;
    private HandlerThread mtkHandlerThread;
    private Object mLastCellInfoListLock = new Object();
    private static final long MTK_LAST_CELL_INFO_LIST_MAX_AGE_MS = 1000;
    /* M: MTK handler end*/

    private Notification.Builder mNotificationBuilder;
    private Notification mNotification;
    public static final int REJECT_NOTIFICATION = 890;

    private boolean mIsImeiLock = false;
    private String mLocatedPlmn = null;
    private int mPsRegState = ServiceState.STATE_OUT_OF_SERVICE;
    private int mPsRegStateRaw = NetworkRegistrationState.REG_STATE_NOT_REG_NOT_SEARCHING;
    private String mHhbName = null;
    private String mCsgId = null;
    private int mFemtocellDomain = 0;
    private int mIsFemtocell = 0;
    private String mFemtoPlmn = null;
    private String mFemtoAct = null;

    public boolean hasPendingPollState = false;

    private String mLastRegisteredPLMN = null;
    private String mLastPSRegisteredPLMN = null;
    private boolean mEverIVSR = false;  /* ALPS00324111: at least one chance to do IVSR  */
    private boolean isCsInvalidCard = false;
    private boolean mMtkVoiceCapable = mPhone.getContext().getResources().getBoolean(
            com.android.internal.R.bool.config_voice_capable);
    private int mLastPhoneGetNitz = SubscriptionManager.INVALID_PHONE_INDEX;

    // Mapping table from iso to time zone id of capital city.
    private String[][] mTimeZoneIdOfCapitalCity = {
        {"au", "Australia/Sydney"},
        {"br", "America/Sao_Paulo"},
        {"ca", "America/Toronto"},
        {"cl", "America/Santiago"},
        {"es", "Europe/Madrid"},
        {"fm", "Pacific/Ponape"},
        {"gl", "America/Godthab"},
        {"id", "Asia/Jakarta"},
        {"kz", "Asia/Almaty"},
        {"mn", "Asia/Ulaanbaatar"},
        {"mx", "America/Mexico_City"},
        {"pf", "Pacific/Tahiti"},
        {"pt", "Europe/Lisbon"},
        {"ru", "Europe/Moscow"},
        {"us", "America/New_York"},
        {"ec", "America/Guayaquil"},
        {"cn", "Asia/Shanghai"}
    };
    private String mSavedGuessTimeZone = null;
    ///M: [Network][C2K] Add CDMA plus code to parse special MCC/MNC. @{
    private IPlusCodeUtils mPlusCodeUtils = PlusCodeProcessor.getPlusCodeUtils();
    /// @}
    ///M: [Network][C2K] Record network existence state for calculate emergency call only state. @{
    private boolean mNetworkExsit = false;
    ///M @}
    ///M: Fix the operator info not update issue.
    private  boolean mForceBroadcastServiceState = false;

    private BroadcastReceiver mMtkIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED)) {
                onCarrierConfigChanged();
                return;
            }

            if (intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED)) {
                // update emergency string whenever locale changed
                refreshSpn(mSS, mCellLoc, false);
                updateSpnDisplay();

                /// M: pollState() again to broadcast and update property if needed. @{
                if (mForceBroadcastServiceState) {
                    pollState();
                }
                /// @}
            } else if (intent.getAction().equals(ACTION_RADIO_OFF)) {
                mAlarmSwitch = false;
                DcTracker dcTracker = mPhone.mDcTracker;
                powerOffRadioSafely(dcTracker);
            } else if (intent.getAction().equals(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED)) {
                int simState = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE,
                        TelephonyManager.SIM_STATE_UNKNOWN);
                int slotId = intent.getIntExtra(PhoneConstants.PHONE_KEY,
                    SubscriptionManager.INVALID_PHONE_INDEX);
                if (slotId == mPhone.getPhoneId()) {
                    log("SIM state change, slotId: " + slotId + " simState[" + simState + "]");
                    if (!mPhone.isPhoneTypeGsm()) {
                        /// M:[Network][C2K] When Sim status changed, should update mMdn to null @{
                        if (TelephonyManager.SIM_STATE_ABSENT == simState) {
                             mMdn = null;
                        }
                        return;
                        /// @}
                    } else {
                        if (simState == TelephonyManager.SIM_STATE_ABSENT) {
                            // M: IVSR
                            mLastRegisteredPLMN = null;
                            mLastPSRegisteredPLMN = null;
                        }
                    }
                }
            } else if (intent.getAction().equals(
                        TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED)) {
                int slotId = intent.getIntExtra(PhoneConstants.PHONE_KEY,
                                SubscriptionManager.INVALID_PHONE_INDEX);
                int simState = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE,
                               TelephonyManager.SIM_STATE_UNKNOWN);
                log("ACTION_SIM_APPLICATION_STATE_CHANGED, slotId: " + slotId
                        + " simState[" + simState + "]");
                if (slotId == mPhone.getPhoneId()
                        && simState == TelephonyManager.SIM_STATE_LOADED) {
                    setDeviceRatMode(mPhone.getPhoneId());
                }
            } else if (intent.getAction().equals(
                    CarrierExpressManager.ACTION_OPERATOR_CONFIG_CHANGED)) {
                // reload op package
                try {
                    mTelephonyCustomizationFactory =
                            OpTelephonyCustomizationUtils.getOpFactory(mPhone.getContext());
                    mServiceStateTrackerExt =
                            mTelephonyCustomizationFactory.makeServiceStateTrackerExt(mPhone.getContext());
                    if (DBG) log("mServiceStateTrackerExt reload success");
                } catch (Exception e) {
                    if (DBG) log("mServiceStateTrackerExt init fail");
                    e.printStackTrace();
                }
            }
        }
    };

    private class MtkCellInfoResult {
        List<CellInfo> list;
        Object lockObj = new Object();
    }

    private class MtkHandler extends Handler {
        public MtkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg){
            AsyncResult ar;

            switch(msg.what) {
            case EVENT_MTK_GET_CELL_INFO_LIST:{
                ar = (AsyncResult) msg.obj;
                MtkCellInfoResult result = (MtkCellInfoResult) ar.userObj;
                synchronized(result.lockObj) {
                    if (ar.exception != null) {
                        log("EVENT_MTK_GET_CELL_INFO_LIST: error ret null, e=" + ar.exception);
                        result.list = null;
                    } else {
                        result.list = (List<CellInfo>) ar.result;

                        if (VDBG) {
                            log("EVENT_MTK_GET_CELL_INFO_LIST: size=" + result.list.size()
                                    + " list=" + result.list);
                        }
                    }
                    synchronized(mLastCellInfoListLock) {
                        mLastCellInfoListTime = SystemClock.elapsedRealtime();
                        mLastCellInfoList = result.list;
                    }
                    result.lockObj.notify();
                }
                break;
            }
            default:
                loge("Should not be here msg.what="+msg.what);
            }
        }
    }

    public MtkServiceStateTracker(GsmCdmaPhone phone, CommandsInterface ci) {
        super(phone, ci);

        mtkHandlerThread = new HandlerThread("MtkHandlerThread");
        mtkHandlerThread.start();

        mtkHandler = new MtkHandler(mtkHandlerThread.getLooper());

        /// M: unregister AOSP broadcast receiver. @{
        Context context = mPhone.getContext();
        context.unregisterReceiver(mIntentReceiver);  // it's not working
        /// @}

        // Add MTK interested intents
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        context.registerReceiver(mMtkIntentReceiver, filter);
        filter = new IntentFilter();
        filter.addAction(ACTION_RADIO_OFF);
        context.registerReceiver(mMtkIntentReceiver, filter);
        filter = new IntentFilter();
        filter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        context.registerReceiver(mMtkIntentReceiver, filter);
        filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
        context.registerReceiver(mMtkIntentReceiver, filter);
        /// M: [Network][C2K] add for CDMA update MDN when SIM changed. @{
        filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED);
        context.registerReceiver(mMtkIntentReceiver, filter);
        /// @}
        // For op pachage dynamic change.
        filter = new IntentFilter();
        filter.addAction(CarrierExpressManager.ACTION_OPERATOR_CONFIG_CHANGED);
        context.registerReceiver(mMtkIntentReceiver, filter);
        //
        try {
            mTelephonyCustomizationFactory =
                    OpTelephonyCustomizationUtils.getOpFactory(mPhone.getContext());
            mServiceStateTrackerExt =
                    mTelephonyCustomizationFactory.makeServiceStateTrackerExt(mPhone.getContext());
        } catch (Exception e) {
            if (DBG) log("mServiceStateTrackerExt init fail");
            e.printStackTrace();
        }
        ((MtkRIL)mCi).registerForCsNetworkStateChanged(this, EVENT_CS_NETWORK_STATE_CHANGED, null);
        ((MtkRIL)mCi).registerForPsNetworkStateChanged(this, EVENT_PS_NETWORK_STATE_CHANGED, null);
    }

    @Override
    public void updatePhoneType() {
        // If we are previously voice roaming, we need to notify that roaming status changed before
        // we change back to non-roaming.
        if (mSS != null && mSS.getVoiceRoaming()) {
            mVoiceRoamingOffRegistrants.notifyRegistrants();
        }

        // If we are previously data roaming, we need to notify that roaming status changed before
        // we change back to non-roaming.
        if (mSS != null && mSS.getDataRoaming()) {
            mDataRoamingOffRegistrants.notifyRegistrants();
        }

        // If we are previously in service, we need to notify that we are out of service now.
        if (mSS != null && mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) {
            mNetworkDetachedRegistrants.notifyRegistrants();
        }

        // If we are previously in service, we need to notify that we are out of service now.
        if (mSS != null && mSS.getDataRegState() == ServiceState.STATE_IN_SERVICE) {
            mDetachedRegistrants.notifyRegistrants();
        }

        mSS = new MtkServiceState();
        mNewSS = new MtkServiceState();
        mLastCellInfoListTime = 0;
        mLastCellInfoList = null;
        mSignalStrength = new SignalStrength();
        mRestrictedState = new RestrictedState();
        mStartedGprsRegCheck = false;
        mReportedGprsNoReg = false;
        mMdn = null;
        mMin = null;
        mPrlVersion = null;
        mIsMinInfoReady = false;
        mNitzState.handleNetworkUnavailable();

        //cancel any pending pollstate request on voice tech switching
        cancelPollState();
        // M: MTK added  ALPS02974868
        if (mPhone.isPhoneTypeGsm()) {
            //clear CDMA registrations first
            if (mCdmaSSM != null) {
                mCdmaSSM.dispose(this);
            }

            mCi.unregisterForCdmaPrlChanged(this);
            mPhone.unregisterForEriFileLoaded(this);
            mCi.unregisterForCdmaOtaProvision(this);
            mPhone.unregisterForSimRecordsLoaded(this);

            mCellLoc = new GsmCellLocation();
            mNewCellLoc = new GsmCellLocation();

            // M: MTK added
            ((MtkRIL)mCi).setInvalidSimInfo(this, EVENT_INVALID_SIM_INFO, null);
            ((MtkRIL)mCi).registerForIccRefresh(this, EVENT_ICC_REFRESH, null);
            ((MtkRIL)mCi).registerForNetworkEvent(this, EVENT_NETWORK_EVENT, null);
            ((MtkRIL)mCi).registerForModulation(this, EVENT_MODULATION_INFO, null);

            if (SystemProperties.get("ro.vendor.mtk_femto_cell_support").equals("1")) {
                ((MtkRIL) mCi).registerForFemtoCellInfo(this, EVENT_FEMTO_CELL_INFO, null);
            }

            try {
                if (mServiceStateTrackerExt != null &&
                        mServiceStateTrackerExt.isImeiLocked())
                   ((MtkRIL)mCi).registerForIMEILock(this, EVENT_IMEI_LOCK, null);
            } catch (RuntimeException e) {
               /* BSP must exception here but Turnkey should not exception here */
               loge("No isImeiLocked");
            }

        } else {
            //clear GSM regsitrations first
            mCi.unregisterForAvailable(this);
            mCi.unSetOnRestrictedStateChanged(this);
            ///M: [Network][C2K] Reset Restricted State. @{
            mPsRestrictDisabledRegistrants.notifyRegistrants();
            /// @}
            /// M: [Network][C2K] Clean CDMA event listener  @{
            mCi.unregisterForCdmaPrlChanged(this);
            mPhone.unregisterForEriFileLoaded(this);
            mCi.unregisterForCdmaOtaProvision(this);
            mPhone.unregisterForSimRecordsLoaded(this);
            /// @}

            ((MtkRIL)mCi).unregisterForIccRefresh(this);
            ((MtkRIL)mCi).unSetInvalidSimInfo(this);
            ((MtkRIL)mCi).unregisterForNetworkEvent(this);
            ((MtkRIL)mCi).unregisterForModulation(this);

            try {
                if (mServiceStateTrackerExt != null &&
                        mServiceStateTrackerExt.isImeiLocked())
                    ((MtkRIL)mCi).unregisterForIMEILock(this);
            } catch (RuntimeException e) {
                /* BSP must exception here but Turnkey should not exception here */
                loge("No isImeiLocked");
            }

            if (mPhone.isPhoneTypeCdmaLte()) {
                mPhone.registerForSimRecordsLoaded(this, EVENT_SIM_RECORDS_LOADED, null);
            }
            mCellLoc = new CdmaCellLocation();
            mNewCellLoc = new CdmaCellLocation();
            mCdmaSSM = CdmaSubscriptionSourceManager.getInstance(mPhone.getContext(), mCi, this,
                    EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED, null);
            mIsSubscriptionFromRuim = (mCdmaSSM.getCdmaSubscriptionSource() ==
                    CdmaSubscriptionSourceManager.SUBSCRIPTION_FROM_RUIM);

            mCi.registerForCdmaPrlChanged(this, EVENT_CDMA_PRL_VERSION_CHANGED, null);
            mPhone.registerForEriFileLoaded(this, EVENT_ERI_FILE_LOADED, null);
            mCi.registerForCdmaOtaProvision(this, EVENT_OTA_PROVISION_STATUS_CHANGE, null);

            mHbpcdUtils = new HbpcdUtils(mPhone.getContext());
            // update OTASP state in case previously set by another service
            updateOtaspState();
        }

        // This should be done after the technology specific initializations above since it relies
        // on fields like mIsSubscriptionFromRuim (which is updated above)
        onUpdateIccAvailability();

        mPhone.setSystemProperty(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE,
                ServiceState.rilRadioTechnologyToString(ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN));
        // Query signal strength from the modem after service tracker is created (i.e. boot up,
        // switching between GSM and CDMA phone), because the unsolicited signal strength
        // information might come late or even never come. This will get the accurate signal
        // strength information displayed on the UI.
        mCi.getSignalStrength(obtainMessage(EVENT_GET_SIGNAL_STRENGTH));
        sendMessage(obtainMessage(EVENT_PHONE_TYPE_SWITCHED));

        logPhoneTypeChange();

        // Tell everybody that the registration state and RAT have changed.
        notifyDataRegStateRilRadioTechnologyChanged();
    }

    /**
     * Registration point for roaming type changed of mobile data
     * notify when data roaming is true and roaming type differs the previous
     *
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForDataRoamingTypeChange(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDataRoamingTypeChangedRegistrants.add(r);
    }

    public void unregisterForDataRoamingTypeChange(Handler h) {
        mDataRoamingTypeChangedRegistrants.remove(h);
    }

    @Override
    public void dispose() {
        super.dispose();
        // M:
        ((MtkRIL)mCi).unregisterForCsNetworkStateChanged(this);
        ((MtkRIL)mCi).unregisterForPsNetworkStateChanged(this);
        mtkHandlerThread.quit();
        if (mPhone.isPhoneTypeGsm()) {
            ((MtkRIL)mCi).unregisterForIccRefresh(this);
            ((MtkRIL)mCi).unSetInvalidSimInfo(this);
            ((MtkRIL)mCi).unregisterForNetworkEvent(this);
            ((MtkRIL)mCi).unregisterForModulation(this);

            try {
                if (mServiceStateTrackerExt.isImeiLocked())
                    ((MtkRIL)mCi).unregisterForIMEILock(this);
            } catch (RuntimeException e) {
                /* BSP must exception here but Turnkey should not exception here */
                loge("No isImeiLocked");
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;
        int[] ints;
        Message message;

        if (VDBG) logv("received event " + msg.what);
        switch (msg.what) {
            case EVENT_SIM_RECORDS_LOADED:
                if (mPhone.isPhoneTypeGsm()) {
                    refreshSpn(mSS, mCellLoc, false);
                }
                super.handleMessage(msg);

                /// M: pollState() again to broadcast and update property if needed. @{
                if (mForceBroadcastServiceState) {
                    pollState();
                }
                /// @}
                break;

            case EVENT_SIM_NOT_INSERTED:
                if (DBG) log("EVENT_SIM_NOT_INSERTED, ignored.");
                break;

            case EVENT_PS_NETWORK_STATE_CHANGED:
                ar = (AsyncResult) msg.obj;
                onPsNetworkStateChangeResult(ar);
                break;

            case EVENT_ERI_FILE_LOADED:
                /// M: [Network][C2K] for EriTriggeredPollState @{
                mEriTriggeredPollState = true;
                /// @}
                super.handleMessage(msg);
                break;
             case EVENT_RUIM_READY:
                /// M: [Network][C2K] replace LTE_ON_CDMA mode since google not support
                /// CDMA only case. @{
                if (mPhone.isPhoneTypeCdmaLte()) {
                // @}
                    // Subscription will be read from SIM I/O
                    if (DBG) log("Receive EVENT_RUIM_READY");
                    pollState();
                } else {
                    if (DBG) log("Receive EVENT_RUIM_READY and Send Request getCDMASubscription.");
                    getSubscriptionInfoAndStartPollingThreads();
                }
                // Only support automatic selection mode in CDMA.
                mCi.getNetworkSelectionMode(obtainMessage(EVENT_POLL_STATE_NETWORK_SELECTION_MODE));
                break;
                /// @}
            case EVENT_CS_NETWORK_STATE_CHANGED:
                ar = (AsyncResult) msg.obj;
                onNetworkStateChangeResult(ar);
                break;
            case EVENT_NETWORK_EVENT:
                if (mPhone.isPhoneTypeGsm()) {
                    ar = (AsyncResult) msg.obj;
                    onNetworkEventReceived(ar);
                }
                break;
            case EVENT_MODULATION_INFO:
                if (mPhone.isPhoneTypeGsm()) {
                    ar = (AsyncResult) msg.obj;
                    onModulationInfoReceived(ar);
                }
                break;
            case EVENT_IMEI_LOCK: //ALPS00296298
                if (mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_IMEI_LOCK GSM");
                    mIsImeiLock = true;
                }
                break;
            case EVENT_ICC_REFRESH:
                if (mPhone.isPhoneTypeGsm()) {
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        IccRefreshResponse res = ((IccRefreshResponse) ar.result);
                        if (res == null) {
                            log("IccRefreshResponse is null");
                            break;
                        }
                        switch (res.refreshResult) {
                            case MtkIccRefreshResponse.REFRESH_INIT_FULL_FILE_UPDATED:
                            case MtkIccRefreshResponse.REFRESH_SESSION_RESET:
                                // NAA session Reset only applicable for a 3G platform
                                 /* ALPS00949490 */
                                 mLastRegisteredPLMN = null;
                                 mLastPSRegisteredPLMN = null;
                                 log("Reset mLastRegisteredPLMN/mLastPSRegisteredPLMN"
                                         + "for ICC refresh");
                                 break;
                            case IccRefreshResponse.REFRESH_RESULT_FILE_UPDATE:
                            case MtkIccRefreshResponse.REFRESH_INIT_FILE_UPDATED:
                                 if (res.efId == MtkIccConstants.EF_IMSI) {
                                     mLastRegisteredPLMN = null;
                                     mLastPSRegisteredPLMN = null;
                                     log("Reset flag of IVSR for IMSI update");
                                     break;
                                 }
                                 break;
                            default:
                                 log("GSST EVENT_ICC_REFRESH IccRefreshResponse =" + res);
                                 break;
                        }
                    }
                }
                break;
            case EVENT_INVALID_SIM_INFO: //ALPS00248788
                if (mPhone.isPhoneTypeGsm()) {
                    ar = (AsyncResult) msg.obj;
                    onInvalidSimInfoReceived(ar);
                }
                break;

            case EVENT_FEMTO_CELL_INFO:
                ar = (AsyncResult) msg.obj;
                onFemtoCellInfoResult(ar);
                break;

            case EVENT_RADIO_STATE_CHANGED:
            case EVENT_PHONE_TYPE_SWITCHED:
                log("handle EVENT_RADIO_STATE_CHANGED");
                if(!mPhone.isPhoneTypeGsm() &&
                        mCi.getRadioState() == CommandsInterface.RadioState.RADIO_ON) {
                    handleCdmaSubscriptionSource(mCdmaSSM.getCdmaSubscriptionSource());

                    // Signal strength polling stops when radio is off.
                    queueNextSignalStrengthPoll();
                }

                // This will do nothing in the 'radio not available' case
                // setPowerStateToDesired();
                RadioManager.getInstance().setRadioPower(mDesiredPowerState, mPhone.getPhoneId());
                // These events are modem triggered, so pollState() needs to be forced
                modemTriggeredPollState();
                break;
            case EVENT_NITZ_TIME:
                // M: only last phone which get NITZ can update time
                mLastPhoneGetNitz = mPhone.getPhoneId();

                ar = (AsyncResult) msg.obj;

                final String nitzString = (String)((Object[])ar.result)[0];
                final long nitzReceiveTime = ((Long)((Object[])ar.result)[1]).longValue();

                new Thread(new Runnable() {
                    public void run() {
                        setTimeFromNITZString(nitzString, nitzReceiveTime);
                    }
                }).start();
                break;
            // M @{
            case EVENT_ALL_DATA_DISCONNECTED:
                if (SubscriptionManager.getDefaultDataSubscriptionId()
                        == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    int[] subIds = SubscriptionManager.getSubId(
                            RadioCapabilitySwitchUtil.getMainCapabilityPhoneId());
                    if (subIds != null && subIds.length > 0) {
                        ProxyController.getInstance().unregisterForAllDataDisconnected(
                                subIds[0], this);
                    }
                }
                mPhone.unregisterForAllDataDisconnected(this);
                super.handleMessage(msg);
                break;
            // GSM only event.
            case EVENT_SIM_OPL_LOADED:
                ar = (AsyncResult)msg.obj;
                if ((ar != null) && (ar.result != null)) {
                    Integer id = (Integer)ar.result;
                    if (id.intValue() == MtkSIMRecords.EVENT_OPL) {
                        if (mPhone.isPhoneTypeGsm()) {
                            log("EVENT_SIM_OPL_LOADED: EVENT_OPL");
                            refreshSpn(mSS, mCellLoc, false);
                            if (mForceBroadcastServiceState) {
                                pollState();
                            }
                        } else {
                            loge("EVENT_SIM_OPL_LOADED should not be here");
                        }
                    }
                } else {
                    loge("EVENT_SIM_OPL_LOADED obj is null");
                }
                break;
            // M @}
            case EVENT_UNSOL_CELL_INFO_LIST: {
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    log("EVENT_UNSOL_CELL_INFO_LIST: error ignoring, e=" + ar.exception);
                } else {
                    List<CellInfo> list = (List<CellInfo>) ar.result;
                    if (VDBG) {
                        log("EVENT_UNSOL_CELL_INFO_LIST: size=" + list.size() + " list=" + list);
                    }
                    synchronized(mLastCellInfoListLock) {
                        mLastCellInfoListTime = SystemClock.elapsedRealtime();
                        mLastCellInfoList = list;
                    }
                    mPhone.notifyCellInfo(list);
                }
                break;
            }
            case EVENT_IMS_SERVICE_STATE_CHANGED:
                if (DBG) log("EVENT_IMS_SERVICE_STATE_CHANGED");
                // IMS state will only affect the merged service state if the service state of
                // GsmCdma phone is not STATE_IN_SERVICE.
                if (mSS.getState() != ServiceState.STATE_IN_SERVICE) {
                    /// M: [Network] IMS service state is reliable only when data in service. @{
                    if (mSS.getDataRegState() == ServiceState.STATE_IN_SERVICE) {
                        mPhone.notifyServiceStateChanged(mPhone.getServiceState());
                    }
                    /// @}
                }
                break;
            default:
                // log("Unhandled message with number: " + msg.what);
                super.handleMessage(msg);
                break;
        }
    }

    @Override
    protected void handlePollStateResultMessage(int what, AsyncResult ar) {
        int ints[];
        switch (what) {
            case EVENT_POLL_STATE_REGISTRATION: {
                NetworkRegistrationState networkRegState;
                if (ar.result instanceof android.hardware.radio.V1_2.VoiceRegStateResult) {
                    networkRegState = createRegistrationStateFromVoiceRegState(ar.result);
                } else {
                    networkRegState = (NetworkRegistrationState) ar.result;
                }

                VoiceSpecificRegistrationStates voiceSpecificStates =
                        networkRegState.getVoiceSpecificStates();

                int registrationState = networkRegState.getRegState();
                int cssIndicator = voiceSpecificStates.cssSupported ? 1 : 0;
                int newVoiceRat = ServiceState.networkTypeToRilRadioTechnology(
                        networkRegState.getAccessNetworkTechnology());

                mNewSS.setVoiceRegState(regCodeToServiceState(registrationState));
                mNewSS.setCssIndicator(cssIndicator);
                mNewSS.setRilVoiceRadioTechnology(newVoiceRat);
                mNewSS.addNetworkRegistrationState(networkRegState);
                setPhyCellInfoFromCellIdentity(mNewSS, networkRegState.getCellIdentity());

                /// M: [Network] add ril voice state for world phone. @{
                ((MtkServiceState)mNewSS).setRilVoiceRegState(registrationState);
                /// @}

                //Denial reason if registrationState = 3
                int reasonForDenial = networkRegState.getReasonForDenial();
                if (mPhone.isPhoneTypeGsm()) {

                    mGsmRoaming = regCodeIsRoaming(registrationState);

                    boolean isVoiceCapable = mPhone.getContext().getResources()
                            .getBoolean(com.android.internal.R.bool.config_voice_capable);
                    mEmergencyOnly = networkRegState.isEmergencyEnabled();
                } else {
                    int roamingIndicator = voiceSpecificStates.roamingIndicator;

                    //Indicates if current system is in PR
                    int systemIsInPrl = voiceSpecificStates.systemIsInPrl;

                    //Is default roaming indicator from PRL
                    int defaultRoamingIndicator = voiceSpecificStates.defaultRoamingIndicator;

                    mRegistrationState = registrationState;
                    // When registration state is roaming and TSB58
                    // roaming indicator is not in the carrier-specified
                    // list of ERIs for home system, mCdmaRoaming is true.
                    boolean cdmaRoaming =
                            regCodeIsRoaming(registrationState)
                                    && !isRoamIndForHomeSystem(
                                            Integer.toString(roamingIndicator));
                    mNewSS.setVoiceRoaming(cdmaRoaming);
                    /// M: [Network][C2K]  add ril voice state for world phone. @{
                    if (cdmaRoaming) {
                        ((MtkServiceState)mNewSS).setRilVoiceRegState(
                                    NetworkRegistrationState.REG_STATE_ROAMING);
                    }
                    /// @}
                    mRoamingIndicator = roamingIndicator;
                    mIsInPrl = (systemIsInPrl == 0) ? false : true;
                    mDefaultRoamingIndicator = defaultRoamingIndicator;

                    int systemId = 0;
                    int networkId = 0;
                    CellIdentity cellIdentity = networkRegState.getCellIdentity();
                    if (cellIdentity != null && cellIdentity.getType() == CellInfoType.CDMA) {
                        systemId = ((CellIdentityCdma) cellIdentity).getSystemId();
                        networkId = ((CellIdentityCdma) cellIdentity).getNetworkId();
                    }
                    mNewSS.setCdmaSystemAndNetworkId(systemId, networkId);

                    if (reasonForDenial == 0) {
                        mRegistrationDeniedReason = ServiceStateTracker.REGISTRATION_DENIED_GEN;
                    } else if (reasonForDenial == 1) {
                        mRegistrationDeniedReason = ServiceStateTracker.REGISTRATION_DENIED_AUTH;
                    } else {
                        mRegistrationDeniedReason = "";
                    }

                    if (mRegistrationState == 3) {
                        if (DBG) log("Registration denied, " + mRegistrationDeniedReason);
                    }
                }

                processCellLocationInfo(mNewCellLoc, networkRegState.getCellIdentity());

                if (DBG) {
                    //log("handlPollVoiceRegResultMessage: regState=" + registrationState
                    //        + " radioTechnology=" + voiceRegStateResult.rat);
                }
                break;
            }

            case EVENT_POLL_STATE_GPRS: {
                NetworkRegistrationState networkRegState;
                if (ar.result instanceof android.hardware.radio.V1_2.DataRegStateResult) {
                    networkRegState = createRegistrationStateFromDataRegState(ar.result);
                } else {
                    networkRegState = (NetworkRegistrationState) ar.result;
                }

                int reasonForDenial = networkRegState.getReasonForDenial();
                ((MtkServiceState)mNewSS).setDataRejectCause(reasonForDenial);

                DataSpecificRegistrationStates dataSpecificStates =
                        networkRegState.getDataSpecificStates();
                int registrationState = networkRegState.getRegState();
                int serviceState = regCodeToServiceState(registrationState);
                int newDataRat = ServiceState.networkTypeToRilRadioTechnology(
                        networkRegState.getAccessNetworkTechnology());
                mNewSS.setDataRegState(serviceState);
                mNewSS.setRilDataRadioTechnology(newDataRat);
                mNewSS.addNetworkRegistrationState(networkRegState);
                // When we receive OOS reset the PhyChanConfig list so that non-return-to-idle
                // implementers of PhyChanConfig unsol will not carry forward a CA report
                // (2 or more cells) to a new cell if they camp for emergency service only.
                if (serviceState == ServiceState.STATE_OUT_OF_SERVICE) {
                    mLastPhysicalChannelConfigList = null;
                }
                setPhyCellInfoFromCellIdentity(mNewSS, networkRegState.getCellIdentity());

                /// M: [Network] Add for world phone get data registration state. @{
                ((MtkServiceState) mNewSS).setRilDataRegState(registrationState);
                /// @}

                // M: Get Mtk's data type
                int mtk_data_type = 0;
                try {
                    mtk_data_type = Integer.valueOf(TelephonyManager.getTelephonyProperty(
                        mPhone.getPhoneId(), PROP_MTK_DATA_TYPE, "0"));
                } catch(Exception e) {
                    loge("INVALID PROP_MTK_DATA_TYPE");
                }
                // M: Get data type for AP to update 4G+ icon when it is DC or HSPX
                ((MtkServiceState)mNewSS).setProprietaryDataRadioTechnology(mtk_data_type);

                if (mPhone.isPhoneTypeGsm()) {

                    mNewReasonDataDenied = networkRegState.getReasonForDenial();
                    mNewMaxDataCalls = dataSpecificStates.maxDataCalls;
                    mDataRoaming = regCodeIsRoaming(registrationState);
                    // Save the data roaming state reported by modem registration before resource
                    // overlay or carrier config possibly overrides it.
                    mNewSS.setDataRoamingFromRegistration(mDataRoaming);

                    if (DBG) {
                        //log("handlPollStateResultMessage: GsmSST dataServiceState=" + serviceState
                        //        + " regState=" + registrationState
                        //        + " dataRadioTechnology=" + newDataRat);
                    }
                } else if (mPhone.isPhoneTypeCdma()) {

                    boolean isDataRoaming = regCodeIsRoaming(registrationState);
                    mNewSS.setDataRoaming(isDataRoaming);
                    // Save the data roaming state reported by modem registration before resource
                    // overlay or carrier config possibly overrides it.
                    mNewSS.setDataRoamingFromRegistration(isDataRoaming);

                    if (DBG) {
                        log("handlPollStateResultMessage: cdma dataServiceState=" + serviceState
                                + " regState=" + registrationState
                                + " dataRadioTechnology=" + newDataRat);
                    }
                } else {

                    // If the unsolicited signal strength comes just before data RAT family changes
                    // (i.e. from UNKNOWN to LTE, CDMA to LTE, LTE to CDMA), the signal bar might
                    // display the wrong information until the next unsolicited signal strength
                    // information coming from the modem, which might take a long time to come or
                    // even not come at all.  In order to provide the best user experience, we
                    // query the latest signal information so it will show up on the UI on time.
                    int oldDataRAT = mSS.getRilDataRadioTechnology();
                    if (((oldDataRAT == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN)
                            && (newDataRat != ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN))
                            || (ServiceState.isCdma(oldDataRAT) && ServiceState.isLte(newDataRat))
                            || (ServiceState.isLte(oldDataRAT)
                            && ServiceState.isCdma(newDataRat))) {
                        mCi.getSignalStrength(obtainMessage(EVENT_GET_SIGNAL_STRENGTH));
                    }
                    /// M: [Network][C2K] Add for world phone and 4G+ icon. @{
                    if (regCodeIsRoaming(registrationState)) {
                        ((MtkServiceState)mNewSS).setRilDataRegState(
                                    NetworkRegistrationState.REG_STATE_ROAMING);
                    }

                    // voice roaming state in done while handling EVENT_POLL_STATE_REGISTRATION_CDMA
                    boolean isDataRoaming = regCodeIsRoaming(registrationState);
                    mNewSS.setDataRoaming(isDataRoaming);
                    // Save the data roaming state reported by modem registration before resource
                    // overlay or carrier config possibly overrides it.
                    mNewSS.setDataRoamingFromRegistration(isDataRoaming);
                    if (DBG) {
                        log("handlPollStateResultMessage: CdmaLteSST dataServiceState="
                                + serviceState + " registrationState=" + registrationState
                                + " dataRadioTechnology=" + newDataRat);
                    }
                }

                updateServiceStateLteEarfcnBoost(mNewSS,
                        getLteEarfcn(networkRegState.getCellIdentity()));
                break;
            }

            case EVENT_POLL_STATE_OPERATOR: {
                if (mPhone.isPhoneTypeGsm()) {
                    String opNames[] = (String[]) ar.result;

                    if (opNames != null && opNames.length >= 3) {
                        // FIXME: Giving brandOverride higher precedence, is this desired?
                        String brandOverride = mUiccController.getUiccCard(getPhoneId()) != null
                                ? mUiccController.getUiccCard(getPhoneId())
                                        .getOperatorBrandOverride() : null;
                        if (brandOverride != null) {
                            log("EVENT_POLL_STATE_OPERATOR: use brandOverride=" + brandOverride);
                            mNewSS.setOperatorName(brandOverride, brandOverride, opNames[2]);
                        } else {
                            mNewSS.setOperatorName(opNames[0], opNames[1], opNames[2]);
                        }
                    }
                } else {
                    String opNames[] = (String[])ar.result;

                    if (opNames != null && opNames.length >= 3) {
                        // TODO: Do we care about overriding in this case.
                        // If the NUMERIC field isn't valid use PROPERTY_CDMA_HOME_OPERATOR_NUMERIC
                        if ((opNames[2] == null) || (opNames[2].length() < 5)
                                || ("00000".equals(opNames[2]))
                                ///M: [Network][C2K] change for invalid MCC/MNC and fix it to "". @{
                                || ("N/AN/A".equals(opNames[2]))) {
                            opNames[2] = SystemProperties.get(
                                    GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC, "");
                            ///  @}
                            if (DBG) {
                                log("RIL_REQUEST_OPERATOR.response[2], the numeric, " +
                                        " is bad. Using SystemProperties '" +
                                        GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC +
                                        "'= " + opNames[2]);
                            }
                        }
                        ///M: [Network][C2K] Use CDMA plus code to handle special MCC/MNC. @{
                        String numeric = opNames[2];
                        boolean plusCode = false;
                        if (numeric != null && numeric.startsWith("2134") &&
                                numeric.length() == 7) {
                            String tempStr = mPlusCodeUtils.checkMccBySidLtmOff(numeric);
                            if (!tempStr.equals("0")) {
                                opNames[2] = tempStr + numeric.substring(4);
                                numeric = tempStr;
                                log("EVENT_POLL_STATE_OPERATOR_CDMA: checkMccBySidLtmOff: numeric ="
                                        + numeric + ", plmn =" + opNames[2]);
                            }
                            plusCode = true;
                        }
                        /// @}
                        if (!mIsSubscriptionFromRuim) {
                            // NV device (as opposed to CSIM)
                            ///M: [Network][C2K] handle special MCC/MNC. @{
                            if (plusCode) {
                                opNames[1] = lookupOperatorName(mPhone.getContext(),
                                        mPhone.getSubId(), opNames[2], false);
                            }
                            mNewSS.setOperatorName(null, opNames[1], opNames[2]);
                            /// @}
                        } else {
                            String brandOverride = mUiccController.getUiccCard(getPhoneId()) != null
                                    ? mUiccController.getUiccCard(getPhoneId())
                                    .getOperatorBrandOverride() : null;
                            if (brandOverride != null) {
                                ///M: [Network][C2K] Add log. @{
                                log("EVENT_POLL_STATE_OPERATOR_CDMA: use brand=" + brandOverride);
                                ///  @}
                                mNewSS.setOperatorName(brandOverride, brandOverride, opNames[2]);
                            } else {
                                mNewSS.setOperatorName(opNames[0], opNames[1], opNames[2]);
                            }
                        }
                    } else {
                        if (DBG) log("EVENT_POLL_STATE_OPERATOR_CDMA: error parsing opNames");
                    }
                }
                break;
            }

            case EVENT_POLL_STATE_NETWORK_SELECTION_MODE: {
                ints = (int[])ar.result;
                mNewSS.setIsManualSelection(ints[0] == 1);
                if ((ints[0] == 1) && (mPhone.shouldForceAutoNetworkSelect())) {
                        /*
                         * modem is currently in manual selection but manual
                         * selection is not allowed in the current mode so
                         * switch to automatic registration
                         */
                    mPhone.setNetworkSelectionModeAutomatic (null);
                    log(" Forcing Automatic Network Selection, " +
                            "manual selection is not allowed");
                }
                break;
            }

            default:
                loge("handlePollStateResultMessage: Unexpected RIL response received: " + what);
        }
    }

    /**
     * Query the carrier configuration to determine if there any network overrides
     * for roaming or not roaming for the current service state.
     */
    @Override
    protected void updateRoamingState() {
        if (mPhone.isPhoneTypeGsm()) {
            /**
             * Since the roaming state of gsm service (from +CREG) and
             * data service (from +CGREG) could be different, the new SS
             * is set to roaming when either is true.
             *
             * There are exceptions for the above rule.
             * The new SS is not set as roaming while gsm service reports
             * roaming but indeed it is same operator.
             * And the operator is considered non roaming.
             *
             * The test for the operators is to handle special roaming
             * agreements and MVNO's.
             */
            // M: mDataRoaming only cares Cellular network here.
            boolean roaming = (mGsmRoaming || mDataRoaming);
            // M: consider both cs & ps
            if (roaming && !isOperatorConsideredRoaming(mNewSS)
                    && (isSameNamedOperators(mNewSS) || isOperatorConsideredNonRoaming(mNewSS))) {
                roaming = false;
            }

            CarrierConfigManager configLoader = (CarrierConfigManager)
                    mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);

            if (configLoader != null) {
                try {
                    PersistableBundle b = configLoader.getConfigForSubId(mPhone.getSubId());

                    if (alwaysOnHomeNetwork(b)) {
                        log("updateRoamingState: carrier config override always on home network");
                        roaming = false;
                    } else if (isNonRoamingInGsmNetwork(b, mNewSS.getOperatorNumeric())) {
                        log("updateRoamingState: carrier config override set non roaming:"
                                + mNewSS.getOperatorNumeric());
                        roaming = false;
                    } else if (isRoamingInGsmNetwork(b, mNewSS.getOperatorNumeric())) {
                        log("updateRoamingState: carrier config override set roaming:"
                                + mNewSS.getOperatorNumeric());
                        roaming = true;
                    }
                } catch (Exception e) {
                    loge("updateRoamingState: unable to access carrier config service");
                }
            } else {
                log("updateRoamingState: no carrier config service available");
            }

            mNewSS.setVoiceRoaming(roaming);
            mNewSS.setDataRoaming(roaming);
        } else {

            CarrierConfigManager configLoader = (CarrierConfigManager)
                    mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (configLoader != null) {
                try {
                    PersistableBundle b = configLoader.getConfigForSubId(mPhone.getSubId());
                    String systemId = Integer.toString(mNewSS.getCdmaSystemId());

                    if (alwaysOnHomeNetwork(b)) {
                        log("updateRoamingState: carrier config override always on home network");
                        setRoamingOff();
                    } else if (isNonRoamingInGsmNetwork(b, mNewSS.getOperatorNumeric())
                            || isNonRoamingInCdmaNetwork(b, systemId)) {
                        log("updateRoamingState: carrier config override set non-roaming:"
                                + mNewSS.getOperatorNumeric() + ", " + systemId);
                        setRoamingOff();
                    } else if (isRoamingInGsmNetwork(b, mNewSS.getOperatorNumeric())
                            || isRoamingInCdmaNetwork(b, systemId)) {
                        log("updateRoamingState: carrier config override set roaming:"
                                + mNewSS.getOperatorNumeric() + ", " + systemId);
                        setRoamingOn();
                    }
                } catch (Exception e) {
                    loge("updateRoamingState: unable to access carrier config service");
                }
            } else {
                log("updateRoamingState: no carrier config service available");
            }

            if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean(PROP_FORCE_ROAMING, false)) {
                mNewSS.setVoiceRoaming(true);
                mNewSS.setDataRoaming(true);
            }
        }
    }

    @Override
    protected void handlePollStateResult(int what, AsyncResult ar) {
        // Ignore stale requests from last poll
        if (ar.userObj != mPollingContext) return;

        if (ar.exception != null) {
            CommandException.Error err=null;
            if (ar.exception instanceof IllegalStateException) {
                log("handlePollStateResult exception " + ar.exception);
            }
            if (ar.exception instanceof CommandException) {
                err = ((CommandException)(ar.exception)).getCommandError();
            }

            if (err == CommandException.Error.RADIO_NOT_AVAILABLE) {
                // Radio has crashed or turned off
                cancelPollState();
                // Invoke pollState again to trigger pollStateDone() if needed
                if (hasPendingPollState) {
                    hasPendingPollState = false;
                    pollState();
                    loge("handlePollStateResult trigger pending pollState()");
                } else if (mCi.getRadioState() != CommandsInterface.RadioState.RADIO_ON) {
                    ///M: fix google issue, clean status and notify to other module @{
                    if (mCi.getRadioState() == CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
                        mNewSS.setStateOutOfService();
                    } else {
                        mNewSS.setStateOff();
                    }
                    mNewCellLoc.setStateInvalid();
                    setSignalStrengthDefaultValues();
                    mPsRegStateRaw = NetworkRegistrationState.REG_STATE_NOT_REG_NOT_SEARCHING;
                    pollStateDone();
                    if (DBG) loge("Mlog: pollStateDone to notify RADIO_NOT_AVAILABLE");
                    ///@}
                }
                return;
            }

            if (err != CommandException.Error.OP_NOT_ALLOWED_BEFORE_REG_NW) {
                loge("RIL implementation has returned an error where it must succeed" +
                        ar.exception);
            }
        } else try {
            handlePollStateResultMessage(what, ar);
        } catch (RuntimeException ex) {
            loge("Exception while polling service state. Probably malformed RIL response." + ex);
        }

        mPollingContext[0]--;

        if (mPollingContext[0] == 0) {
            // Simulate REQUEST_IWLAN_STATE
            int mIwlanState = 0;
            try {
                mIwlanState = Integer.valueOf(TelephonyManager.getTelephonyProperty(
                    mPhone.getPhoneId(), PROP_IWLAN_STATE, "0"));
            } catch(Exception e) {
                loge("INVALID PROP_IWLAN_STATE");
            }
            ((MtkServiceState) mNewSS).setIwlanRegState(regCodeToServiceState(mIwlanState));


            if (mPhone.isPhoneTypeGsm()) {
                // PollState Queue optimization
                // If there are URC between 4 pollState Request, need discard this pollStateResult
                // when Oper is null but in service, or oper is not null but out of service
                boolean in_service = ((mNewSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE)
                        || (mNewSS.getDataRegState() == ServiceState.STATE_IN_SERVICE));
                boolean radioOffwithIwlan =
                        (mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF)
                        && ((mNewSS.getRilDataRadioTechnology()
                                == ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN) ||
                             (((MtkServiceState) mNewSS).getIwlanRegState()
                                == ServiceState.STATE_IN_SERVICE));
                String oper = mNewSS.getOperatorNumeric();
                if (((in_service == false && !TextUtils.isEmpty(oper)) // no service but has oper
                        || (in_service == true && TextUtils.isEmpty(oper))) // in service & no oper
                        && (!radioOffwithIwlan) // not flight mode iwlan
                        && hasPendingPollState) { // flag false means not temp state
                    loge("Temporary service state, need restart PollState");
                    hasPendingPollState = false;
                    cancelPollState();
                    modemTriggeredPollState();
                    return;
                }
                updateRoamingState();
                // M: merge IMS ECC for ECC string display
                boolean isImsEccOnly = getImsEccOnly();
                if ((in_service == false) && (isImsEccOnly == true)) {
                    mEmergencyOnly = true;
                }
                // M: END
                mNewSS.setEmergencyOnly(mEmergencyOnly);
            } else {
                boolean namMatch = false;
                if (!isSidsAllZeros() && isHomeSid(mNewSS.getCdmaSystemId())) {
                    namMatch = true;
                }

                // Setting SS Roaming (general)
                if (mIsSubscriptionFromRuim) {
                    boolean isRoamingBetweenOperators = isRoamingBetweenOperators(
                            mNewSS.getVoiceRoaming(), mNewSS);
                    if (isRoamingBetweenOperators != mNewSS.getVoiceRoaming()) {
                        log("isRoamingBetweenOperators=" + isRoamingBetweenOperators
                                + ". Override CDMA voice roaming to " + isRoamingBetweenOperators);
                        mNewSS.setVoiceRoaming(isRoamingBetweenOperators);
                    }
                }

                /**
                * For CDMA, voice and data should have the same roaming status.
                * If voice is not in service, use TSB58 roaming indicator to set
                * data roaming status. If TSB58 roaming indicator is not in the
                * carrier-specified list of ERIs for home system then set roaming.
                */
                final int dataRat = mNewSS.getRilDataRadioTechnology();
                if (ServiceState.isCdma(dataRat)) {
                    final boolean isVoiceInService =
                            (mNewSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE);
                    if (isVoiceInService) {
                        boolean isVoiceRoaming = mNewSS.getVoiceRoaming();
                        if (mNewSS.getDataRoaming() != isVoiceRoaming) {
                            log("Data roaming != Voice roaming. Override data roaming to "
                                    + isVoiceRoaming);
                            mNewSS.setDataRoaming(isVoiceRoaming);
                        }
                    } else {
                        /**
                        * As per VoiceRegStateResult from radio types.hal the TSB58
                        * Roaming Indicator shall be sent if device is registered
                        * on a CDMA or EVDO system.
                        */
                        boolean isRoamIndForHomeSystem = isRoamIndForHomeSystem(
                                Integer.toString(mRoamingIndicator));
                        boolean dataRoamingState = mNewSS.getDataRoaming();
                        if (mNewSS.getDataRoaming() == isRoamIndForHomeSystem) {
                            log("isRoamIndForHomeSystem=" + isRoamIndForHomeSystem
                                    + ", override data roaming to " + !isRoamIndForHomeSystem);
                            mNewSS.setDataRoaming(!isRoamIndForHomeSystem);
                        }
                        /**
                        * ALPS03564542, fix google issue, if did not set carrier-specifiled ERI list,
                        * and roaming status is not roaming, should not set data roaming
                        * state to roaming, this is opposite with google requirement.
                        */
                        ///M: [Network][C2K] @{
                        String[] homeRoamIndicators = Resources.getSystem().getStringArray(
                                com.android.internal.R.array.config_cdma_home_system);
                        if (!dataRoamingState && !isRoamIndForHomeSystem
                                    && (homeRoamIndicators != null)
                                    && (homeRoamIndicators.length == 0)) {
                                log("isRoamIndForHomeSystem=" + isRoamIndForHomeSystem
                                    + ", override data roaming to false");
                                mNewSS.setDataRoaming(false);
                        }
                        /// @}
                    }
                }

                /// M: [Network][C2K] Add for show EccButton when sim out of service. @{
                mEmergencyOnly = false;
                if (mCi.getRadioState().isOn()) {
                    if ((mNewSS.getVoiceRegState() == ServiceState.STATE_OUT_OF_SERVICE)
                            && (mNewSS.getDataRegState() == ServiceState.STATE_OUT_OF_SERVICE)
                            && mNetworkExsit) {
                        mEmergencyOnly = true;
                    }
                    mEmergencyOnly = mergeEmergencyOnlyCdmaIms(mEmergencyOnly);
                }
                mNewSS.setEmergencyOnly(mEmergencyOnly);
                /// @}

                // Setting SS CdmaRoamingIndicator and CdmaDefaultRoamingIndicator
                mNewSS.setCdmaDefaultRoamingIndicator(mDefaultRoamingIndicator);
                mNewSS.setCdmaRoamingIndicator(mRoamingIndicator);
                boolean isPrlLoaded = true;
                if (TextUtils.isEmpty(mPrlVersion)) {
                    isPrlLoaded = false;
                }
                if (!isPrlLoaded || (mNewSS.getRilVoiceRadioTechnology()
                        == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN)) {
                    logv("Turn off roaming indicator if !isPrlLoaded or voice RAT is unknown");
                    mNewSS.setCdmaRoamingIndicator(EriInfo.ROAMING_INDICATOR_OFF);
                } else if (!isSidsAllZeros()) {
                    if (!namMatch && !mIsInPrl) {
                        // Use default
                        mNewSS.setCdmaRoamingIndicator(mDefaultRoamingIndicator);
                    } else if (namMatch && !mIsInPrl) {
                        // TODO this will be removed when we handle roaming on LTE
                        // on CDMA+LTE phones
                        if (ServiceState.isLte(mNewSS.getRilVoiceRadioTechnology())) {
                            log("Turn off roaming indicator as voice is LTE");
                            mNewSS.setCdmaRoamingIndicator(EriInfo.ROAMING_INDICATOR_OFF);
                        } else {
                            mNewSS.setCdmaRoamingIndicator(EriInfo.ROAMING_INDICATOR_FLASH);
                        }
                    } else if (!namMatch && mIsInPrl) {
                        // Use the one from PRL/ERI
                        mNewSS.setCdmaRoamingIndicator(mRoamingIndicator);
                    } else {
                        // It means namMatch && mIsInPrl
                        if ((mRoamingIndicator <= 2)) {
                            mNewSS.setCdmaRoamingIndicator(EriInfo.ROAMING_INDICATOR_OFF);
                        } else {
                            // Use the one from PRL/ERI
                            mNewSS.setCdmaRoamingIndicator(mRoamingIndicator);
                        }
                    }
                }

                int roamingIndicator = mNewSS.getCdmaRoamingIndicator();
                mNewSS.setCdmaEriIconIndex(mPhone.mEriManager.getCdmaEriIconIndex(roamingIndicator,
                        mDefaultRoamingIndicator));
                mNewSS.setCdmaEriIconMode(mPhone.mEriManager.getCdmaEriIconMode(roamingIndicator,
                        mDefaultRoamingIndicator));

                // NOTE: Some operator may require overriding mCdmaRoaming
                // (set by the modem), depending on the mRoamingIndicator.

                if (DBG) {
                    log("Set CDMA Roaming Indicator to: " + mNewSS.getCdmaRoamingIndicator()
                            + ". voiceRoaming = " + mNewSS.getVoiceRoaming()
                            + ". dataRoaming = " + mNewSS.getDataRoaming()
                            + ", isPrlLoaded = " + isPrlLoaded
                            + ". namMatch = " + namMatch + " , mIsInPrl = " + mIsInPrl
                            + ", mRoamingIndicator = " + mRoamingIndicator
                            + ", mDefaultRoamingIndicator= " + mDefaultRoamingIndicator
                            + ", set mEmergencyOnly=" + mEmergencyOnly
                            + ", mNetworkExsit=" + mNetworkExsit);
                }
            }
            pollStateDone();
        }

    }

    @Override
    protected void updateSpnDisplay() {
        updateOperatorNameFromEri();

        String wfcVoiceSpnFormat = null;
        String wfcDataSpnFormat = null;
        int combinedRegState = getCombinedRegState();
        if (mPhone.getImsPhone() != null && mPhone.getImsPhone().isWifiCallingEnabled()
                && (combinedRegState == ServiceState.STATE_IN_SERVICE)) {
            // In Wi-Fi Calling mode show SPN or PLMN + WiFi Calling
            //
            // 1) Show SPN + Wi-Fi Calling If SIM has SPN and SPN display condition
            //    is satisfied or SPN override is enabled for this carrier
            //
            // 2) Show PLMN + Wi-Fi Calling if there is no valid SPN in case 1

            String[] wfcSpnFormats = mPhone.getContext().getResources().getStringArray(
                    com.android.internal.R.array.wfcSpnFormats);
            int voiceIdx = 0;
            int dataIdx = 0;
            CarrierConfigManager configLoader = (CarrierConfigManager)
                    mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (configLoader != null) {
                try {
                    PersistableBundle b = configLoader.getConfigForSubId(mPhone.getSubId());
                    if (b != null) {
                        voiceIdx = b.getInt(CarrierConfigManager.KEY_WFC_SPN_FORMAT_IDX_INT);
                        dataIdx = b.getInt(
                                CarrierConfigManager.KEY_WFC_DATA_SPN_FORMAT_IDX_INT);
                    }
                } catch (Exception e) {
                    loge("updateSpnDisplay: carrier config error: " + e);
                }
            }

            wfcVoiceSpnFormat = wfcSpnFormats[voiceIdx];
            wfcDataSpnFormat = wfcSpnFormats[dataIdx];
        }

        if (mPhone.isPhoneTypeGsm()) {
            // The values of plmn/showPlmn change in different scenarios.
            // 1) No service but emergency call allowed -> expected
            //    to show "Emergency call only"
            //    EXTRA_SHOW_PLMN = true
            //    EXTRA_PLMN = "Emergency call only"

            // 2) No service at all --> expected to show "No service"
            //    EXTRA_SHOW_PLMN = true
            //    EXTRA_PLMN = "No service"

            // 3) Normal operation in either home or roaming service
            //    EXTRA_SHOW_PLMN = depending on IccRecords rule
            //    EXTRA_PLMN = plmn

            // 4) No service due to power off, aka airplane mode
            //    EXTRA_SHOW_PLMN = false
            //    EXTRA_PLMN = null

            IccRecords iccRecords = mIccRecords;
            String plmn = null;
            boolean showPlmn = false;
            int rule = (iccRecords != null) ? iccRecords.getDisplayRule(mSS) : 0;
            boolean noService = false;
            if (combinedRegState == ServiceState.STATE_OUT_OF_SERVICE
                    || combinedRegState == ServiceState.STATE_EMERGENCY_ONLY) {
                showPlmn = true;

                // Force display no service
                final boolean forceDisplayNoService = mPhone.getContext().getResources().getBoolean(
                        com.android.internal.R.bool.config_display_no_service_when_sim_unready)
                                && !mIsSimReady;
                if (mEmergencyOnly && !forceDisplayNoService) {
                    // No service but emergency call allowed
                    plmn = Resources.getSystem().
                            getText(com.android.internal.R.string.emergency_calls_only).toString();
                } else {
                    // No service at all
                    plmn = Resources.getSystem().
                            getText(com.android.internal.R.string.lockscreen_carrier_default).
                            toString();
                    noService = true;
                }
                if (DBG) log("updateSpnDisplay: radio is on but out " +
                        "of service, set plmn='" + plmn + "'");
            } else if (combinedRegState == ServiceState.STATE_IN_SERVICE) {
                // In either home or roaming service
                plmn = mSS.getOperatorAlpha();
                showPlmn = !TextUtils.isEmpty(plmn) &&
                        ((rule & SIMRecords.SPN_RULE_SHOW_PLMN)
                                == SIMRecords.SPN_RULE_SHOW_PLMN);
            } else {
                // Power off state, such as airplane mode, show plmn as "No service"
                showPlmn = true;
                plmn = Resources.getSystem().
                        getText(com.android.internal.R.string.lockscreen_carrier_default).
                        toString();
                if (DBG) log("updateSpnDisplay: radio is off w/ showPlmn="
                        + showPlmn + " plmn=" + plmn);
            }

            try {
                plmn = mServiceStateTrackerExt.onUpdateSpnDisplay(plmn,
                           (MtkServiceState)mSS, mPhone.getPhoneId());
            } catch (RuntimeException e) {
                e.printStackTrace();
            }

            if ((mSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE) &&
                    (mSS.getDataRegState() == ServiceState.STATE_IN_SERVICE)) {
                //[ALPS01650043]-Start: don't update PLMN name
                // when it is null for backward compatible
                if (plmn != null) {
                    // show PLMN + ECC for IMS when PS is in service
                    boolean eccForIms = mServiceStateTrackerExt.showEccForIms();
                    boolean imsEccOnly = getImsEccOnly();
                    log("CS OOS/PS In service, check eccForIms=" +
                            eccForIms + " imsEccOnly=" + imsEccOnly);
                    if (eccForIms && imsEccOnly) {
                        plmn = plmn + "(" + Resources.getSystem()
                            .getText(com.android.internal.R.string.emergency_calls_only)
                            .toString() + ")";
                    }
                } else {
                    log("PLMN name is null when CS not registered and PS registered");
                }
            }

            /* ALPS00296298 */
            if (mIsImeiLock) {
                plmn = Resources.getSystem().getText(com.mediatek.R.string.invalid_card).toString();
            }

            // The value of spn/showSpn are same in different scenarios.
            //    EXTRA_SHOW_SPN = depending on IccRecords rule and radio/IMS state
            //    EXTRA_SPN = spn
            //    EXTRA_DATA_SPN = dataSpn
            String spn = (iccRecords != null) ? iccRecords.getServiceProviderName() : "";
            String dataSpn = spn;
            boolean showSpn = !noService && !TextUtils.isEmpty(spn)
                    && ((rule & SIMRecords.SPN_RULE_SHOW_SPN)
                    == SIMRecords.SPN_RULE_SHOW_SPN);

            if (!TextUtils.isEmpty(spn) && !TextUtils.isEmpty(wfcVoiceSpnFormat) &&
                    !TextUtils.isEmpty(wfcDataSpnFormat)) {
                // Show SPN + Wi-Fi Calling If SIM has SPN and SPN display condition
                // is satisfied or SPN override is enabled for this carrier.

                String originalSpn = spn.trim();
                spn = String.format(wfcVoiceSpnFormat, originalSpn);
                dataSpn = String.format(wfcDataSpnFormat, originalSpn);
                showSpn = true;
                showPlmn = false;
            } else if (!TextUtils.isEmpty(plmn) && !TextUtils.isEmpty(wfcVoiceSpnFormat)) {
                // Show PLMN + Wi-Fi Calling if there is no valid SPN in the above case

                String originalPlmn = plmn.trim();
                plmn = String.format(wfcVoiceSpnFormat, originalPlmn);
            } else if (mSS.getVoiceRegState() == ServiceState.STATE_POWER_OFF
                    || (showPlmn && TextUtils.equals(spn, plmn))) {
                // airplane mode or spn equals plmn, do not show spn
                spn = null;
                showSpn = false;
            }

            //M: don't show SPN when cs/ps not in service
            if (mSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE &&
                    mSS.getDataRegState() != ServiceState.STATE_IN_SERVICE) {
                spn = null;
                showSpn = false;
            }

            try {
                if (mServiceStateTrackerExt.needSpnRuleShowPlmnOnly() &&
                        !TextUtils.isEmpty(plmn)) {
                    log("origin showSpn:" + showSpn + " showPlmn:" + showPlmn + " rule:" + rule);
                    showSpn = false;
                    showPlmn = true;
                    rule = SIMRecords.SPN_RULE_SHOW_PLMN;
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }

            int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            int[] subIds = SubscriptionManager.getSubId(mPhone.getPhoneId());
            if (subIds != null && subIds.length > 0) {
                subId = subIds[0];
            }

            /// M: [Network][C2K] for [CT case][TC-IRLAB-02009]. @{
            try {
                if (!mServiceStateTrackerExt.allowSpnDisplayed()) {
                    if (rule == (SIMRecords.SPN_RULE_SHOW_PLMN
                            | SIMRecords.SPN_RULE_SHOW_SPN)) {
                        showSpn = false;
                        spn = null;
                    }
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            /// @}

            // Update SPN_STRINGS_UPDATED_ACTION IFF any value changes
            if (mSubId != subId ||
                    showPlmn != mCurShowPlmn
                    || showSpn != mCurShowSpn
                    || !TextUtils.equals(spn, mCurSpn)
                    || !TextUtils.equals(dataSpn, mCurDataSpn)
                    || !TextUtils.equals(plmn, mCurPlmn)) {
                if (DBG) {
                    log(String.format("updateSpnDisplay: changed sending intent rule=" + rule +
                            " showPlmn='%b' plmn='%s' showSpn='%b' spn='%s' dataSpn='%s' " +
                            "subId='%d'", showPlmn, plmn, showSpn, spn, dataSpn, subId));
                }
                Intent intent = new Intent(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
                intent.putExtra(TelephonyIntents.EXTRA_SHOW_SPN, showSpn);
                intent.putExtra(TelephonyIntents.EXTRA_SPN, spn);
                intent.putExtra(TelephonyIntents.EXTRA_DATA_SPN, dataSpn);
                intent.putExtra(TelephonyIntents.EXTRA_SHOW_PLMN, showPlmn);
                intent.putExtra(TelephonyIntents.EXTRA_PLMN, plmn);
                // Femtocell (CSG) info
                intent.putExtra(TelephonyIntents.EXTRA_HNB_NAME, mHhbName);
                intent.putExtra(TelephonyIntents.EXTRA_CSG_ID, mCsgId);
                intent.putExtra(TelephonyIntents.EXTRA_DOMAIN, mFemtocellDomain);
                // isFemtocell (LTE/C2K)
                intent.putExtra(TelephonyIntents.EXTRA_FEMTO, mIsFemtocell);

                SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
                mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);

                if (!mSubscriptionController.setPlmnSpn(mPhone.getPhoneId(),
                        showPlmn, plmn, showSpn, spn)) {
                    mSpnUpdatePending = true;
                }
            }

            mSubId = subId;
            mCurShowSpn = showSpn;
            mCurShowPlmn = showPlmn;
            mCurSpn = spn;
            mCurDataSpn = dataSpn;
            mCurPlmn = plmn;
        } else {
            // mOperatorAlpha contains the ERI text
            String plmn = mSS.getOperatorAlpha();
            boolean showPlmn = false;

            showPlmn = plmn != null;
            ///M: [Network][C2K] handle "" plmn. @{
            if (plmn != null && plmn.equals("")) {
                plmn = null;
            }
            /// @}
            int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            int[] subIds = SubscriptionManager.getSubId(mPhone.getPhoneId());
            if (subIds != null && subIds.length > 0) {
                subId = subIds[0];
            }

            if (!TextUtils.isEmpty(plmn) && !TextUtils.isEmpty(wfcVoiceSpnFormat)) {
                // In Wi-Fi Calling mode show SPN+WiFi

                String originalPlmn = plmn.trim();
                plmn = String.format(wfcVoiceSpnFormat, originalPlmn);
            } else if (mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
                // todo: temporary hack; should have a better fix. This is to avoid using operator
                // name from ServiceState (populated in resetServiceStateInIwlanMode()) until
                // wifi calling is actually enabled
                log("updateSpnDisplay: overwriting plmn from " + plmn + " to null as radio " +
                        "state is off");
                plmn = null;
            }

            if (combinedRegState == ServiceState.STATE_OUT_OF_SERVICE
                    || combinedRegState == ServiceState.STATE_POWER_OFF) {
                ///M: [Network][C2K] when out of service, should show plmn as "no service". @{
                showPlmn = true;
                /// @}
                plmn = Resources.getSystem().getText(com.android.internal.R.string
                        .lockscreen_carrier_default).toString();
                if (DBG) {
                    log("updateSpnDisplay: radio is on but out of svc, set plmn='" + plmn + "'");
                }
            }

            ///M: [Network][C2K] Add for update opeartor name as "Emergency Only". @{
            if (mEmergencyOnly && mCi.getRadioState().isOn()) {
                log("[CDMA]updateSpnDisplay:phone show emergency call only, mEmergencyOnly = true");
                showPlmn = true;
                plmn = Resources.getSystem().
                        getText(com.android.internal.R.string.emergency_calls_only).toString();
            }
            /// @}
            ///M: [Network][C2K] Add for PLMN/SPN display. @{
            String spn = "";
            boolean showSpn = false;
            //for [CT case][TC-IRLAB-02009]. @{
            try {
                if (mServiceStateTrackerExt.allowSpnDisplayed()) {
                    int rule = 0;
                    IccRecords r = mPhone.getIccRecords();
                    rule = (r != null) ? r.getDisplayRule(mSS) : IccRecords.SPN_RULE_SHOW_PLMN;
                    spn = (r != null) ? r.getServiceProviderName() : "";

                    showSpn = !TextUtils.isEmpty(spn)
                            && ((rule & RuimRecords.SPN_RULE_SHOW_SPN)
                                == RuimRecords.SPN_RULE_SHOW_SPN)
                            && !(mSS.getVoiceRegState() == ServiceState.STATE_POWER_OFF)
                            && !mSS.getRoaming();

                    log("[CDMA]updateSpnDisplay: rule=" + rule + ", spn=" + spn
                            + ", showSpn=" + showSpn);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            // @}
            if (mSubId != subId ||
                showPlmn != mCurShowPlmn
                || showSpn != mCurShowSpn
                || !TextUtils.equals(spn, mCurSpn)
                || !TextUtils.equals(plmn, mCurPlmn)) {
            /// @}
                // Allow A blank plmn, "" to set showPlmn to true. Previously, we
                // would set showPlmn to true only if plmn was not empty, i.e. was not
                // null and not blank. But this would cause us to incorrectly display
                // "No Service". Now showPlmn is set to true for any non null string.
                /// M: [Network][C2K] Modify for the spn display feature. @{
                showPlmn = plmn != null;

                // Airplane mode, out_of_service, roaming state or spn is null, do not show spn
                try {
                    if (mServiceStateTrackerExt.allowSpnDisplayed()) {
                        if (mSS.getVoiceRegState() == ServiceState.STATE_POWER_OFF
                                || mSS.getVoiceRegState() == ServiceState.STATE_OUT_OF_SERVICE
                                || mSS.getRoaming()
                                || (spn == null || spn.equals(""))) {
                            showSpn = false;
                            showPlmn = true;
                        } else {
                            showSpn = true;
                            showPlmn = false;
                        }
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                if (DBG) {
                    logv(String.format("[CDMA]updateSpnDisplay: changed sending intent" +
                            " subId='%d' showPlmn='%b' plmn='%s' showSpn='%b' spn='%s'",
                            subId, showPlmn, plmn, showSpn, spn));
                }
                /// @}
                Intent intent = new Intent(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
                ///M: [Network][C2K] For multiple SIM support, share the same intent,
                /// do not replace the other one. @{
                if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                    intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                }
                /// @}
                ///M: [Network][C2K] Add for PLMN/APN display. @{
                intent.putExtra(TelephonyIntents.EXTRA_SHOW_SPN, showSpn);
                intent.putExtra(TelephonyIntents.EXTRA_SPN, spn);
                /// @}
                intent.putExtra(TelephonyIntents.EXTRA_SHOW_PLMN, showPlmn);
                intent.putExtra(TelephonyIntents.EXTRA_PLMN, plmn);
                // Femtocell (CSG) info
                intent.putExtra(TelephonyIntents.EXTRA_HNB_NAME, mHhbName);
                intent.putExtra(TelephonyIntents.EXTRA_CSG_ID, mCsgId);
                intent.putExtra(TelephonyIntents.EXTRA_DOMAIN, mFemtocellDomain);
                // isFemtocell (LTE/C2K)
                intent.putExtra(TelephonyIntents.EXTRA_FEMTO, mIsFemtocell);

                SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
                mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                ///M: [Network][C2K] add for plmn/spn display. @{
                if (!mSubscriptionController.setPlmnSpn(mPhone.getPhoneId(),
                        showPlmn, plmn, showSpn, spn)) {
                    mSpnUpdatePending = true;
                }
                log("[CDMA]updateSpnDisplay: subId=" + subId +
                        ", showPlmn=" + showPlmn +
                        ", plmn=" + plmn +
                        ", showSpn=" + showSpn +
                        ", spn=" + spn +
                        ", mSpnUpdatePending=" + mSpnUpdatePending);
            }

            mSubId = subId;
            mCurShowSpn = showSpn;
            mCurShowPlmn = showPlmn;
            mCurSpn = spn;
            mCurPlmn = plmn;
            /// @}
        }
    }

    @Override
    protected void log(String s) {
        if (mPhone.isPhoneTypeGsm()) {
            Rlog.d(LOG_TAG, "[GsmSST" + mPhone.getPhoneId() + "] " + s);
        } else if (mPhone.isPhoneTypeCdma()) {
            Rlog.d(LOG_TAG, "[CdmaSST" + mPhone.getPhoneId() + "] " + s);
        } else {
            Rlog.d(LOG_TAG, "[CdmaLteSST" + mPhone.getPhoneId() + "] " + s);
        }
    }

    @Override
    protected void loge(String s) {
        if (mPhone.isPhoneTypeGsm()) {
            Rlog.e(LOG_TAG, "[GsmSST" + mPhone.getPhoneId() + "] " + s);
        } else if (mPhone.isPhoneTypeCdma()) {
            Rlog.e(LOG_TAG, "[CdmaSST" + mPhone.getPhoneId() + "] " + s);
        } else {
            Rlog.e(LOG_TAG, "[CdmaLteSST" + mPhone.getPhoneId() + "] " + s);
        }
    }

    protected void logv(String s) {
        if (mPhone.isPhoneTypeGsm()) {
            Rlog.v(LOG_TAG, "[GsmSST" + mPhone.getPhoneId() + "] " + s);
        } else if (mPhone.isPhoneTypeCdma()) {
            Rlog.v(LOG_TAG, "[CdmaSST" + mPhone.getPhoneId() + "] " + s);
        } else {
            Rlog.v(LOG_TAG, "[CdmaLteSST" + mPhone.getPhoneId() + "] " + s);
        }
    }

    private void onNetworkStateChangeResult(AsyncResult ar) {
        String info[];
        if (ar.exception != null || ar.result == null) {
            loge("onNetworkStateChangeResult exception");
            return;
        }
        info = (String[]) ar.result;
        if (mPhone.isPhoneTypeGsm()) {
            int state = -1;
            int lac = -1;
            int cid = -1;
            int Act = -1;
            int cause = -1;

        /* Note: There might not be full +CREG URC info when screen off
                 Full URC format: +CREG: <stat>,<lac>,<cid>,<Act>,<cause> */
            if (info.length > 0) {

                state = Integer.parseInt(info[0]);

                if (info[1] != null && info[1].length() > 0) {
                   lac = Integer.parseInt(info[1], 16);
                }

                if (info[2] != null && info[2].length() > 0) {
                   //TODO: fix JE (java.lang.NumberFormatException: Invalid int: "ffffffff")
                   if (info[2].equals("FFFFFFFF") || info[2].equals("ffffffff")) {
                       log("Invalid cid:" + info[2]);
                       info[2] = "0000ffff";
                   }
                   cid = Integer.parseInt(info[2], 16);
                }

                if (info[3] != null && info[3].length() > 0) {
                   Act = Integer.parseInt(info[3]);
                }

                if (info[4] != null && info[4].length() > 0) {
                   cause = Integer.parseInt(info[4]);
                }

                try {
                // ALPS00283696 CDR-NWS-241
                    if (mServiceStateTrackerExt.needRejectCauseNotification(cause) == true) {
                        setRejectCauseNotification(cause);
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }

            } else {
                log("onNetworkStateChangeResult length zero");
            }
        } else {
            ///M: [Network][C2K] Calculate mnetworkExist by modem URC @{
            if (info.length > 5) {
                if (info[5] != null && info[5].length() > 0) {
                    mNetworkExsit = (1 == Integer.parseInt(info[5])) ? true : false;;
                    //log("onCdmaNetworkExistStateChanged C2K mNetworkExist=" + mNetworkExsit);
                }
            } else {
                log("onCdmaNetworkExistStateChanged Network existence not reported");
            }
            /// @}
        }
        return;
    }

    private void onPsNetworkStateChangeResult(AsyncResult ar) {
        // info[0] = state, info[1] = mccmnc, info[2] = rat.
        int info[];
        int newUrcState;
        String operator_plmn = null;

        if (ar.exception != null || ar.result == null) {
           loge("onPsNetworkStateChangeResult exception");
        } else {
            info = (int[]) ar.result;
            newUrcState = regCodeToServiceState(info[0]);
            if (info.length >= 4) {
                log("onPsNetworkStateChangeResult, mPsRegState:" + mPsRegState
                        + ",newUrcState:" + newUrcState
                        + ",info[0]:" + info[0]
                        + ",info[1]:" + info[1]
                        + ",info[2]:" + info[2]
                        + ",info[3]:" + info[3]);
            } else {
                log("onPsNetworkStateChangeResult, mPsRegState:" + mPsRegState
                        + ",newUrcState:" + newUrcState
                        + ",info[0]:" + info[0]
                        + ",info[1]:" + info[1]
                        + ",info[2]:" + info[2]);
            }
            //get the raw state value for roaming
            mPsRegStateRaw = info[0];
            // get operator plm (consider limited service)
            operator_plmn = String.valueOf(info[1]);
            if ((operator_plmn != null) &&
                (operator_plmn.length() >= 5)) {
                updateLocatedPlmn(operator_plmn);
            } else {
                updateLocatedPlmn(null);
            }
        }
    }

    @Override
    public void pollState(boolean modemTriggered) {
        log("pollState: modemTriggered=" + modemTriggered + ", mPollingContext="
                + (mPollingContext != null ? mPollingContext[0] : -1) + ", RadioState="
                + mCi.getRadioState());

        // [ALPS03020226]
        if ((mPollingContext != null)
                && (mCi.getRadioState() != CommandsInterface.RadioState.RADIO_UNAVAILABLE)) {
            // [ALPS03496447]
            if ((mPhone.isPhoneTypeGsm() && mPollingContext[0] == 4)
                    || (!mPhone.isPhoneTypeGsm() && mPollingContext[0] == 3)) {
                hasPendingPollState = true;
                return;
            }
        }

        mPollingContext = new int[1];
        mPollingContext[0] = 0;

        switch (mCi.getRadioState()) {
            case RADIO_UNAVAILABLE:
                mNewSS.setStateOutOfService();
                mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                mNitzState.handleNetworkUnavailable();
                //M: MTK added
                if (mPhone.isPhoneTypeGsm()) {
                    mPsRegStateRaw = NetworkRegistrationState.REG_STATE_NOT_REG_NOT_SEARCHING;
                }
                //M: MTK added end
                pollStateDone();
                break;

            case RADIO_OFF:
                mNewSS.setStateOff();
                mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                mNitzState.handleNetworkUnavailable();
                // don't poll for state when the radio is off
                // EXCEPT, if the poll was modemTrigged (they sent us new radio data)
                // or we're on IWLAN
                // we don't need to check MtkServiceState.getIwlanRegState
                // because mSS has merged state.
                if (!modemTriggered && ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                        != mSS.getRilDataRadioTechnology()) {
                    //M: MTK added
                    if (mPhone.isPhoneTypeGsm()) {
                        mPsRegStateRaw = NetworkRegistrationState.REG_STATE_NOT_REG_NOT_SEARCHING;
                    }
                    //M: MTK added end
                    pollStateDone();
                    break;
                }

            default:
                // Issue all poll-related commands at once then count down the responses, which
                // are allowed to arrive out-of-order
                // TODO: Add WLAN support.
                mPollingContext[0]++;
                mCi.getOperator(obtainMessage(EVENT_POLL_STATE_OPERATOR, mPollingContext));

                if (mRegStateManagers.get(AccessNetworkConstants.TransportType.WWAN).
                        isServiceConnected() == false) {
                    logv("old poll state");
                    mPollingContext[0]++;
                    mCi.getDataRegistrationState(obtainMessage(EVENT_POLL_STATE_GPRS,
                            mPollingContext));

                    mPollingContext[0]++;
                    mCi.getVoiceRegistrationState(obtainMessage(EVENT_POLL_STATE_REGISTRATION,
                            mPollingContext));
                } else {
                    mPollingContext[0]++;
                    mRegStateManagers.get(AccessNetworkConstants.TransportType.WWAN)
                            .getNetworkRegistrationState(NetworkRegistrationState.DOMAIN_PS,
                            obtainMessage(EVENT_POLL_STATE_GPRS, mPollingContext));

                    mPollingContext[0]++;
                    mRegStateManagers.get(AccessNetworkConstants.TransportType.WWAN)
                            .getNetworkRegistrationState(NetworkRegistrationState.DOMAIN_CS,
                            obtainMessage(EVENT_POLL_STATE_REGISTRATION, mPollingContext));

                }
                if (mPhone.isPhoneTypeGsm()) {
                    mPollingContext[0]++;
                    mCi.getNetworkSelectionMode(obtainMessage(
                            EVENT_POLL_STATE_NETWORK_SELECTION_MODE, mPollingContext));
                }
                break;
        }
    }

    protected void pollStateDone() {
        MtkServiceState mMtkSS = (MtkServiceState) mSS;
        MtkServiceState mMtkNewSS = (MtkServiceState) mNewSS;

        refreshSpn(mNewSS, mNewCellLoc, true);
        if (!mPhone.isPhoneTypeGsm()) {
            updateRoamingState();
        }

        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean(PROP_FORCE_ROAMING, false)) {
            mNewSS.setVoiceRoaming(true);
            mNewSS.setDataRoaming(true);
        }
        useDataRegStateForDataOnlyDevices();
        resetServiceStateInIwlanMode();

        if (Build.IS_DEBUGGABLE && mPhone.mTelephonyTester != null) {
// FIXME
//            mPhone.mTelephonyTester.overrideServiceState(mNewSS);
        }

        // M: used to log and hasChanged compare
        // Fix hasChanged is always true because AOSP set roaming type after check hasChange.
        MtkServiceState mFinalMtkNewSS = new MtkServiceState((MtkServiceState) mNewSS);
        setRoamingType(mFinalMtkNewSS);
        mFinalMtkNewSS.mergeIwlanServiceState();

        if (DBG) {
            log("Poll ServiceState done: "
                    // M: print the NewSS with updated roaming type and iwlan state.
                    + " oldSS=[" + mSS + "] newSS=[" + mFinalMtkNewSS + "]"
                    + " oldMaxDataCalls=" + mMaxDataCalls
                    + " mNewMaxDataCalls=" + mNewMaxDataCalls
                    + " oldReasonDataDenied=" + mReasonDataDenied
                    + " mNewReasonDataDenied=" + mNewReasonDataDenied
                    + " isImsEccOnly= " + getImsEccOnly());
        }

        boolean hasRegistered =
                mSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE
                        && mNewSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE;

        boolean hasDeregistered =
                mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE
                        && mNewSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE;

        boolean hasDataAttached =
                mSS.getDataRegState() != ServiceState.STATE_IN_SERVICE
                        // M: getDataRegState considers cellular status
                        && ((mNewSS.getDataRegState() == ServiceState.STATE_IN_SERVICE) ||
                            (mMtkNewSS.getIwlanRegState() == ServiceState.STATE_IN_SERVICE));

        boolean hasDataDetached =
                mSS.getDataRegState() == ServiceState.STATE_IN_SERVICE
                        // M: getDataRegState considers cellular status
                        && ((mNewSS.getDataRegState() != ServiceState.STATE_IN_SERVICE) &&
                            (mMtkNewSS.getIwlanRegState() != ServiceState.STATE_IN_SERVICE));

        boolean hasDataRegStateChanged =
                // M: getDataRegState considers cellular status
                (mMtkNewSS.getIwlanRegState() == ServiceState.STATE_IN_SERVICE) ?
                (mSS.getDataRegState() != mMtkNewSS.getIwlanRegState()) :
                (mSS.getDataRegState() != mNewSS.getDataRegState());

        boolean hasVoiceRegStateChanged =
                mSS.getVoiceRegState() != mNewSS.getVoiceRegState();

        boolean hasLocationChanged = !mNewCellLoc.equals(mCellLoc);

        // ratchet the new tech up through its rat family but don't drop back down
        // until cell change or device is OOS
        // M: getDataRegState considers cellular status
        boolean isDataInService =
                (mMtkNewSS.getIwlanRegState() == ServiceState.STATE_IN_SERVICE) ?
                true :
                (mNewSS.getDataRegState() == ServiceState.STATE_IN_SERVICE);
        if (isDataInService) {
            mRatRatcheter.ratchet(mSS, mNewSS, hasLocationChanged);
        }

        boolean hasRilVoiceRadioTechnologyChanged =
                mSS.getRilVoiceRadioTechnology() != mNewSS.getRilVoiceRadioTechnology();

        boolean hasRilDataRadioTechnologyChanged =
                // M: compare current and final New SS
                (mMtkSS.getRilDataRadioTechnology() != mFinalMtkNewSS.getRilDataRadioTechnology());


        ///M: Fix the operator info not update issue.
        // M: Compare the NewSS with updated roaming type and Iwlan state.
        boolean hasChanged = !mFinalMtkNewSS.equals(mSS) || mForceBroadcastServiceState;

        boolean hasVoiceRoamingOn = !mSS.getVoiceRoaming() && mNewSS.getVoiceRoaming();

        boolean hasVoiceRoamingOff = mSS.getVoiceRoaming() && !mNewSS.getVoiceRoaming();

        // M: getDataRegState considers cellular status
        boolean hasDataRoamingOn =
                (mMtkNewSS.getIwlanRegState() == ServiceState.STATE_IN_SERVICE) ?
                false : // if IWLAN is enabled, roaming can not be on.
                (!mSS.getDataRoaming() && mNewSS.getDataRoaming());

        // M: getDataRegState considers cellular status
        boolean hasDataRoamingOff =
                (mMtkNewSS.getIwlanRegState() == ServiceState.STATE_IN_SERVICE) ?
                mSS.getDataRoaming() : // if IWLAN is enabled, it's true when mSS is roaming.
                (mSS.getDataRoaming() && !mNewSS.getDataRoaming());

        boolean hasRejectCauseChanged = mRejectCode != mNewRejectCode;

        boolean hasCssIndicatorChanged = (mSS.getCssIndicator() != mNewSS.getCssIndicator());

        boolean has4gHandoff = false;
        boolean hasMultiApnSupport = false;
        boolean hasLostMultiApnSupport = false;
        if (mPhone.isPhoneTypeCdmaLte()) {
            has4gHandoff = mNewSS.getDataRegState() == ServiceState.STATE_IN_SERVICE
                    && ((ServiceState.isLte(mSS.getRilDataRadioTechnology())
                    && (mNewSS.getRilDataRadioTechnology()
                    == ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD))
                    ||
                    ((mSS.getRilDataRadioTechnology()
                            == ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD)
                            && ServiceState.isLte(mNewSS.getRilDataRadioTechnology())));

            hasMultiApnSupport = ((ServiceState.isLte(mNewSS.getRilDataRadioTechnology())
                    || (mNewSS.getRilDataRadioTechnology()
                    == ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD))
                    &&
                    (!ServiceState.isLte(mSS.getRilDataRadioTechnology())
                            && (mSS.getRilDataRadioTechnology()
                            != ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD)));

            hasLostMultiApnSupport =
                    ((mNewSS.getRilDataRadioTechnology()
                            >= ServiceState.RIL_RADIO_TECHNOLOGY_IS95A)
                            && (mNewSS.getRilDataRadioTechnology()
                            <= ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_A));
        }

        // M: used to check whether roaming type of cs/ps is changed.
        boolean hasVoiceRoamingTypeChange =
                mSS.getVoiceRoaming() && mFinalMtkNewSS.getVoiceRoaming() &&
                mSS.getVoiceRoamingType() != mFinalMtkNewSS.getVoiceRoamingType();

        boolean hasDataRoamingTypeChange =
                mSS.getDataRoaming() && mFinalMtkNewSS.getDataRoaming() &&
                mSS.getDataRoamingType() != mFinalMtkNewSS.getDataRoamingType();

        if (DBG) {
            log("pollStateDone:"
                    + " hasRegistered=" + hasRegistered
                    + " hasDeregistered=" + hasDeregistered
                    + " hasDataAttached=" + hasDataAttached
                    + " hasDataDetached=" + hasDataDetached
                    + " hasDataRegStateChanged=" + hasDataRegStateChanged
                    + " hasRilVoiceRadioTechnologyChanged= " + hasRilVoiceRadioTechnologyChanged
                    + " hasRilDataRadioTechnologyChanged=" + hasRilDataRadioTechnologyChanged
                    + " hasChanged=" + hasChanged
                    + " hasVoiceRoamingOn=" + hasVoiceRoamingOn
                    + " hasVoiceRoamingOff=" + hasVoiceRoamingOff
                    + " hasDataRoamingOn=" + hasDataRoamingOn
                    + " hasDataRoamingOff=" + hasDataRoamingOff
                    + " hasLocationChanged=" + hasLocationChanged
                    + " has4gHandoff = " + has4gHandoff
                    + " hasMultiApnSupport=" + hasMultiApnSupport
                    + " hasLostMultiApnSupport=" + hasLostMultiApnSupport
                    + " hasCssIndicatorChanged=" + hasCssIndicatorChanged
                    + " hasVoiceRoamingTypeChange=" + hasVoiceRoamingTypeChange
                    + " hasDataRoamingTypeChange=" + hasDataRoamingTypeChange);
        }

        // Add an event log when connection state changes
        if (hasVoiceRegStateChanged || hasDataRegStateChanged) {
            EventLog.writeEvent(mPhone.isPhoneTypeGsm() ? EventLogTags.GSM_SERVICE_STATE_CHANGE :
                            EventLogTags.CDMA_SERVICE_STATE_CHANGE,
                    mSS.getVoiceRegState(), mSS.getDataRegState(),
                    mNewSS.getVoiceRegState(), mNewSS.getDataRegState());
        }

        if (mPhone.isPhoneTypeGsm()) {
            // Add an event log when network type switched
            // TODO: we may add filtering to reduce the event logged,
            // i.e. check preferred network setting, only switch to 2G, etc
            if (hasRilVoiceRadioTechnologyChanged) {
                int cid = -1;
                GsmCellLocation loc = (GsmCellLocation) mNewCellLoc;
                if (loc != null) cid = loc.getCid();
                // NOTE: this code was previously located after mSS and mNewSS are swapped, so
                // existing logs were incorrectly using the new state for "network_from"
                // and STATE_OUT_OF_SERVICE for "network_to". To avoid confusion, use a new log tag
                // to record the correct states.
                EventLog.writeEvent(EventLogTags.GSM_RAT_SWITCHED_NEW, cid,
                        mSS.getRilVoiceRadioTechnology(),
                        mNewSS.getRilVoiceRadioTechnology());
                if (DBG) {
                    log("RAT switched "
                            + ServiceState.rilRadioTechnologyToString(
                            mSS.getRilVoiceRadioTechnology())
                            + " -> "
                            + ServiceState.rilRadioTechnologyToString(
                            mNewSS.getRilVoiceRadioTechnology()) + " at cell " + cid);
                }
            }

            if (hasCssIndicatorChanged) {
                mPhone.notifyDataConnection(Phone.REASON_CSS_INDICATOR_CHANGED);
            }

            mReasonDataDenied = mNewReasonDataDenied;
            mMaxDataCalls = mNewMaxDataCalls;
            mRejectCode = mNewRejectCode;
        }

        ServiceState oldMergedSS = mPhone.getServiceState();

        /// M: [Network][C2K] The lastest RilDataRadioTechnology.@{
        final int oldRilDataRadioTechnology = mSS.getRilDataRadioTechnology();
        /// @}

        // swap mSS and mNewSS to put new state in mSS
        ServiceState tss = mSS;
        mSS = mNewSS;
        mNewSS = tss;
        // clean slate for next time
        // We need to keep mSS state until compare mNewSS state.
        // Move to bottom of function avoid to put new state in mSS.
        // mNewSS.setStateOutOfService();

        // M:
        mMtkSS = (MtkServiceState) mSS;
        mMtkNewSS = (MtkServiceState) mNewSS;

        // swap mCellLoc and mNewCellLoc to put new state in mCellLoc
        CellLocation tcl = mCellLoc;
        mCellLoc = mNewCellLoc;
        mNewCellLoc = tcl;

        if (hasRilVoiceRadioTechnologyChanged) {
            updatePhoneObject();
        }

        TelephonyManager tm =
                (TelephonyManager) mPhone.getContext().getSystemService(Context.TELEPHONY_SERVICE);

        if (hasRilDataRadioTechnologyChanged) {
            // M: getDataRegState considers cellular status
            int dataRadioTechnology =
                    (mMtkSS.getIwlanRegState() == ServiceState.STATE_IN_SERVICE) ?
                    ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN :
                    mSS.getRilDataRadioTechnology();

            tm.setDataNetworkTypeForPhone(mPhone.getPhoneId(), dataRadioTechnology);
            StatsLog.write(StatsLog.MOBILE_RADIO_TECHNOLOGY_CHANGED,
                    ServiceState.rilRadioTechnologyToNetworkType(mSS.getRilDataRadioTechnology()),
                    mPhone.getPhoneId());

            // M: getDataRegState considers cellular status, so check iwlan only
            if (mMtkSS.getIwlanRegState() == ServiceState.STATE_IN_SERVICE) {
                log("pollStateDone: IWLAN enabled");
            }

            /// M: [Network][C2K] When signal strength not changed, modem will not report CSQ urc.
            /// Then the signal indicator will not update in time such as IRAT switch.
            /// When RilDataRadioTechnology changed, poll and update signal strength. @{
            if (mPhone.isPhoneTypeCdmaLte()) {
                if (oldRilDataRadioTechnology == ServiceState.RIL_RADIO_TECHNOLOGY_LTE
                      || mSS.getRilDataRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_LTE) {
                    log("[CDMALTE]pollStateDone: update signal for RAT switch between diff group");
                    sendMessage(obtainMessage(EVENT_POLL_SIGNAL_STRENGTH));
                }
            }
            /// @}
        }

        if (hasRegistered) {
            mNetworkAttachedRegistrants.notifyRegistrants();
            // M: IVSR
            mLastRegisteredPLMN = mSS.getOperatorNumeric() ;
            mNitzState.handleNetworkAvailable();
        }

        if (hasDeregistered) {
            mNetworkDetachedRegistrants.notifyRegistrants();
            mNitzState.handleNetworkUnavailable();
        }

        if (hasRejectCauseChanged) {
            setNotification(CS_REJECT_CAUSE_ENABLED);
        }

        /// M: [Network][C2K] for EriTriggeredPollState @{
        // Force broadcast service state if pollState() is triggered by EVENT_ERI_FILE_LOADED
        if (mPhone.isPhoneTypeCdmaLte()) {
            /// M: [Network][C2K] Sprint roaming control @{
            if (mEriTriggeredPollState || hasChanged) {
                hasChanged = true;
                String simMccMnc = getSIMOperatorNumeric();
                // Enable ERI only for Sprint SIM
                if (simMccMnc != null &&
                        (simMccMnc.equals("310120") ||
                         simMccMnc.equals("310009") ||
                         simMccMnc.equals("311490") ||
                         simMccMnc.equals("311870"))) {
                    mEnableERI = true;
                } else {
                    mEnableERI = false;
                }
            }
            /// @}
        }
        /// @}

        // M: Check mSignalStrenght when network type may changes.
        // M: considers Cellular state only
        if (hasRilVoiceRadioTechnologyChanged || hasRilDataRadioTechnologyChanged) {
            boolean isGsmNew = false;
            boolean isGsmOld = mSignalStrength.isGsm();
            int dataRat = mSS.getRilDataRadioTechnology();
            int voiceRat = mSS.getRilVoiceRadioTechnology();
            int voiceState = mSS.getVoiceRegState();
            int dataState = mSS.getDataRegState();

            if ((dataRat != ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                    && ServiceState.isGsm(dataRat))
                    || (voiceRat != ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                    && ServiceState.isGsm(voiceRat))) {
                isGsmNew = true;
            }
            // isGsm when type of cs/ps is unknown with GsmPhone
            if (voiceRat == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN &&
                    dataRat == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN &&
                    mPhone.isPhoneTypeGsm()) {
                isGsmNew = true;
            }

            if ((isGsmNew != isGsmOld)
                    && ((voiceState == ServiceState.STATE_IN_SERVICE)
                    || (dataState == ServiceState.STATE_IN_SERVICE))) {
                mSignalStrength.setGsm(isGsmNew);
                notifySignalStrength();
                if (DBG) log("pollStateDone: correct the mSignalStrength.isGsm " +
                        "New:" + isGsmNew + " Old:" + isGsmOld);
            }
        }
        // M: END

        if (hasChanged) {
            // M: merge IWLAN state
            ((MtkServiceState) mSS).mergeIwlanServiceState();
            updateSpnDisplay();

            ///M: Fix the operator info not update issue.
            mForceBroadcastServiceState = false;

            tm.setNetworkOperatorNameForPhone(mPhone.getPhoneId(), mSS.getOperatorAlpha());

            String prevOperatorNumeric = tm.getNetworkOperatorForPhone(mPhone.getPhoneId());
            String prevCountryIsoCode = tm.getNetworkCountryIso(mPhone.getPhoneId());
            String operatorNumeric = mSS.getOperatorNumeric();

            if (!mPhone.isPhoneTypeGsm()) {
                // try to fix the invalid Operator Numeric
                if (isInvalidOperatorNumeric(operatorNumeric)) {
                    int sid = mSS.getCdmaSystemId();
                    operatorNumeric = fixUnknownMcc(operatorNumeric, sid);
                }
            }

            tm.setNetworkOperatorNumericForPhone(mPhone.getPhoneId(), operatorNumeric);
            if (isInvalidOperatorNumeric(operatorNumeric)) {
                if (DBG) log("operatorNumeric " + operatorNumeric + " is invalid");
                // Passing empty string is important for the first update. The initial value of
                // operator numeric in locale tracker is null. The async update will allow getting
                // cell info from the modem instead of using the cached one.
                // M: only call cell info when radio off avoid cell info dead lock.
                if (!getDesiredPowerState()) {
                    getLocaleTracker().updateOperatorNumericAsync("");
                }
                mNitzState.handleNetworkUnavailable();
            } else if ((mSS.getRilDataRadioTechnology() != ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN)
                // M: Only lagecy's getRilDataRadioTechnology considers IWLAN
                && (mMtkSS.getIwlanRegState() != ServiceState.STATE_IN_SERVICE)) {
                // If the device is on IWLAN, modems manufacture a ServiceState with the MCC/MNC of
                // the SIM as if we were talking to towers. Telephony code then uses that with
                // mccTable to suggest a timezone. We shouldn't do that if the MCC/MNC is from IWLAN

                // Update IDD.
                if (!mPhone.isPhoneTypeGsm()) {
                    setOperatorIdd(operatorNumeric);
                }
                getLocaleTracker().updateOperatorNumericSync(operatorNumeric);
                String countryIsoCode = getLocaleTracker().getCurrentCountry();
                // Update Time Zone.
                boolean iccCardExists = iccCardExists();
                boolean networkIsoChanged =
                        networkCountryIsoChanged(countryIsoCode, prevCountryIsoCode);

                // Determine countryChanged: networkIso is only reliable if there's an ICC card.
                boolean countryChanged = iccCardExists && networkIsoChanged;
                if (DBG) {
                    long ctm = System.currentTimeMillis();
                    log("Before handleNetworkCountryCodeKnown:"
                            + " countryChanged=" + countryChanged
                            + " iccCardExist=" + iccCardExists
                            + " countryIsoChanged=" + networkIsoChanged
                            + " operatorNumeric=" + operatorNumeric
                            + " prevOperatorNumeric=" + prevOperatorNumeric
                            + " countryIsoCode=" + countryIsoCode
                            + " prevCountryIsoCode=" + prevCountryIsoCode
                            + " ltod=" + TimeUtils.logTimeOfDay(ctm));
                }
                mNitzState.handleNetworkCountryCodeSet(countryChanged);
            }

            tm.setNetworkRoamingForPhone(mPhone.getPhoneId(),
                    mPhone.isPhoneTypeGsm() ? mSS.getVoiceRoaming() :
                            (mSS.getVoiceRoaming() || mSS.getDataRoaming()));

            setRoamingType(mSS);

            log("Broadcasting ServiceState : " + mSS);
            // notify using PhoneStateListener and the legacy intent ACTION_SERVICE_STATE_CHANGED
            // notify service state changed only if the merged service state is changed.
            if (!oldMergedSS.equals(mPhone.getServiceState()) || mEriTriggeredPollState) {
                mEriTriggeredPollState = false;
                mPhone.notifyServiceStateChanged(mPhone.getServiceState());
            }

            // M: only non-legacy support setServiceStateToModem.
            ((MtkRIL) mCi).setServiceStateToModem(
                    mSS.getVoiceRegState(), mMtkSS.getCellularDataRegState(),
                    mSS.getVoiceRoamingType(), mMtkSS.getCellularDataRoamingType(),
                    mMtkSS.getRilVoiceRegState(),
                    mMtkSS.getRilCellularDataRegState(), null);

            // insert into ServiceStateProvider. This will trigger apps to wake through JobScheduler
            mPhone.getContext().getContentResolver()
                    .insert(getUriForSubscriptionId(mPhone.getSubId()),
                            getContentValuesForServiceState(mSS));

            TelephonyMetrics.getInstance().writeServiceStateChanged(mPhone.getPhoneId(), mSS);
        } else {
            // mSS should update RoamingType and mergeIwlanServiceState, we use final SS instead.
            mSS = mFinalMtkNewSS;
        }

        if (hasDataAttached || has4gHandoff || hasDataDetached || hasRegistered
                || hasDeregistered) {
            logAttachChange();
        }

        if (hasDataAttached || has4gHandoff) {
            mAttachedRegistrants.notifyRegistrants();
            // M: IVSR
            mLastPSRegisteredPLMN = mSS.getOperatorNumeric() ;
        }

        if (hasDataDetached) {
            mDetachedRegistrants.notifyRegistrants();
        }

        if (hasRilDataRadioTechnologyChanged || hasRilVoiceRadioTechnologyChanged) {
            logRatChange();
        }

        mPsRegState = mSS.getDataRegState();

        if (hasDataRegStateChanged || hasRilDataRadioTechnologyChanged) {
            notifyDataRegStateRilRadioTechnologyChanged();

            if (ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                    == mSS.getRilDataRadioTechnology()) {
                mPhone.notifyDataConnection(Phone.REASON_IWLAN_AVAILABLE);
            } else {
                mPhone.notifyDataConnection(null);
            }
        }

        if (hasVoiceRoamingOn || hasVoiceRoamingOff || hasDataRoamingOn || hasDataRoamingOff) {
            logRoamingChange();
        }

        if (hasVoiceRoamingOn) {
            mVoiceRoamingOnRegistrants.notifyRegistrants();
        }

        if (hasVoiceRoamingOff) {
            mVoiceRoamingOffRegistrants.notifyRegistrants();
        }

        if (hasDataRoamingOn) {
            mDataRoamingOnRegistrants.notifyRegistrants();
        }

        if (hasDataRoamingOff) {
            mDataRoamingOffRegistrants.notifyRegistrants();
        }

        // M: notify roaming type to DATA because AOSP only notify home/roaming case.
        if (hasDataRoamingTypeChange) {
            // Criteria
            // 1. International roaming -> domestic roaming -> send notification
            // 2. Domestic roaming -> international roaming -> send notification
            log("notify roaming type change.");

            mDataRoamingTypeChangedRegistrants.notifyRegistrants();
        }

        // Reset the mLocatedPlmn because pollStateDone
        if (mMtkSS.getCellularRegState() == ServiceState.STATE_IN_SERVICE) {
            if ((mLocatedPlmn == null) || // never get ps urc before
                (!mLocatedPlmn.equals(mMtkSS.getOperatorNumeric()))) {
                updateLocatedPlmn(mMtkSS.getOperatorNumeric());
            }
        }

        // For Data
        if (((MtkDcTracker)mPhone.mDcTracker).getPendingDataCallFlag()) {
            ((MtkDcTracker)mPhone.mDcTracker).processPendingSetupData(this);
        }

        if (hasLocationChanged) {
            mPhone.notifyLocationChanged();
        }

        if (mPhone.isPhoneTypeGsm()) {
            if (!isGprsConsistent(mSS.getDataRegState(), mSS.getVoiceRegState())) {
                if (!mStartedGprsRegCheck && !mReportedGprsNoReg) {
                    mStartedGprsRegCheck = true;

                    int check_period = Settings.Global.getInt(
                            mPhone.getContext().getContentResolver(),
                            Settings.Global.GPRS_REGISTER_CHECK_PERIOD_MS,
                            DEFAULT_GPRS_CHECK_PERIOD_MILLIS);
                    sendMessageDelayed(obtainMessage(EVENT_CHECK_REPORT_GPRS),
                            check_period);
                }
            } else {
                mReportedGprsNoReg = false;
            }
        }

        if (hasPendingPollState) {
            hasPendingPollState = false;
            modemTriggeredPollState();
        }
        // M: clean mNewSS state here avoid to put new state in mSS before merge.
        mNewSS.setStateOutOfService();
        // M: If it's power off, we miss to notify other module that there is no signal
        if (notifySignalStrength())
            log("PollStateDone with signal notification, level =" + mSignalStrength.getLevel());
    }

    /**
     * @return true when data's rat is IWLAN and IMS is in service
     */
    private final boolean isConcurrentVoiceAndDataAllowedForIwlan() {
        if (mSS.getDataRegState() == ServiceState.STATE_IN_SERVICE
                && mSS.getRilDataRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                && getImsServiceState() == ServiceState.STATE_IN_SERVICE) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isConcurrentVoiceAndDataAllowed() {
        if (mSS.getCssIndicator() == 1) {
            // Checking the Concurrent Service Supported flag first for all phone types.
            return true;
        } else if (mPhone.isPhoneTypeGsm()) {
            /// M: For VoLTE concurrent voice and data allow. @{
            if (isConcurrentVoiceAndDataAllowedForVolte()) {
                return true;
            }
            /// @}
            // M: Add for IWLAN
            if (isConcurrentVoiceAndDataAllowedForIwlan()) {
                return true;
            }
            // M: fix AOSP's bug, GSM doesn't support concurrent Voice and Data.
            if (mSS.getRilVoiceRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_GSM) {
                return false;
            }
            return (mSS.getRilDataRadioTechnology() >= ServiceState.RIL_RADIO_TECHNOLOGY_UMTS);
        } else if (mPhone.isPhoneTypeCdma()) {
            // Note: it needs to be confirmed which CDMA network types
            // can support voice and data calls concurrently.
            // For the time-being, the return value will be false.
            return false;
        } else {
            // Using the Conncurrent Service Supported flag for CdmaLte devices.
            /// M: [Network][C2K]For svlte concurrent voice and data allow. @{
            // here just a indicator of concurrent capability, but may be could not concurrent right
            // now, so ignore voice register state.
            if (SystemProperties.getInt("ro.boot.opt_c2k_lte_mode", 0) == 1
                    && mSS.getRilDataRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_LTE) {
                return true;
                }
            /// @}

            /// M: For VoLTE concurrent voice and data allow. @{
            if (isConcurrentVoiceAndDataAllowedForVolte()) {
                return true;
            }
            /// @}
            return mSS.getCssIndicator() == 1;
        }
    }

    private int calculateDeviceRatMode(int phoneId) {
        if (mPhone.isPhoneTypeGsm()) {
            try {
                if (mServiceStateTrackerExt.isSupportRatBalancing()) {
                    log("networkType is controlled by RAT Blancing,"
                        + " no need to set network type");
                    return -1;
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }

        int networkType = PhoneFactory.calculatePreferredNetworkType(
                mPhone.getContext(), mPhone.getSubId());
        log("calculateDeviceRatMode=" + networkType);
        return networkType;
    }

    /**
     * @param phoneId set RAT to specific phone.
     */
    public void setDeviceRatMode(int phoneId) {
        if (SubscriptionManager.isValidSubscriptionId(mPhone.getSubId())) {
            int networkType = calculateDeviceRatMode(phoneId);
            if (networkType >= Phone.NT_MODE_WCDMA_PREF) {
                mPhone.setPreferredNetworkType(networkType, null);
            }
        } else {
            log("Invalid subId, skip setDeviceRatMode!");
        }
    }


    /**
     * For Data module
     * Return true when PLMN will changed while latest broadcasted cellular state is in service.
     * @return true of false
     */
    public boolean willLocatedPlmnChange() {
        MtkServiceState mMtkSS = (MtkServiceState)mSS;
        if (mMtkSS == null || mLocatedPlmn == null) return false;
        if (mMtkSS.getCellularRegState() == ServiceState.STATE_IN_SERVICE) {
            if (!mSS.getOperatorNumeric().equals(mLocatedPlmn)) {
                // lasted broadcasted PLMN is different from the new one.
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Return the current located PLMN string (ex: "46000") or null (ex: flight mode or no signal
     * area).
     * @return mLocatedPlmn return register PLMN id.
     */
    public String getLocatedPlmn() {
        return mLocatedPlmn;
    }

    private void updateLocatedPlmn(String plmn) {

        if (((mLocatedPlmn == null) && (plmn != null)) ||
            ((mLocatedPlmn != null) && (plmn == null)) ||
            ((mLocatedPlmn != null) && (plmn != null) && !(mLocatedPlmn.equals(plmn)))) {
            log("updateLocatedPlmn(),previous plmn= " + mLocatedPlmn + " ,update to: " + plmn);
            Intent intent = new Intent(TelephonyIntents.ACTION_LOCATED_PLMN_CHANGED);
            if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            }
            intent.putExtra(TelephonyIntents.EXTRA_PLMN, plmn);

            if (plmn != null) {
                int mcc;
                try {
                    mcc = Integer.parseInt(plmn.substring(0, 3));
                    intent.putExtra(TelephonyIntents.EXTRA_ISO, MccTable.countryCodeForMcc(mcc));
                } catch (NumberFormatException ex) {
                    loge("updateLocatedPlmn: countryCodeForMcc error" + ex);
                    intent.putExtra(TelephonyIntents.EXTRA_ISO, "");
                } catch (StringIndexOutOfBoundsException ex) {
                    loge("updateLocatedPlmn: countryCodeForMcc error" + ex);
                    intent.putExtra(TelephonyIntents.EXTRA_ISO, "");
                }
                mLocatedPlmn = plmn;  //[ALPS02198932]
                setDeviceRatMode(mPhone.getPhoneId());
            } else {
                intent.putExtra(TelephonyIntents.EXTRA_ISO, "");
            }

            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
            mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        mLocatedPlmn = plmn;
    }

    /* Refresh operator name after mcc, mnc and lac are available
     * so we can get EONS correctly
     * If caller is not from pollState, we can not set the new PLMN to ss.
     * It leads wrong state during the next pollState.
     * If PLMN changes, the caller should restart the pollState.
     */
    protected void refreshSpn(ServiceState ss, CellLocation cellLoc, boolean fromPollState) {
        String strOperatorLong = "";
        String strOperatorShort = "";
        String brandOverride = mUiccController.getUiccCard(getPhoneId()) != null ?
                mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() : null;
        if (brandOverride != null) {
            log("refreshSpn: use brandOverride" + brandOverride);
            strOperatorLong = brandOverride;
            strOperatorShort = brandOverride;
        } else {
            int lac = (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) ?
                    ((GsmCellLocation)cellLoc).getLac() : -1;

            strOperatorLong = ((MtkRIL)mCi).lookupOperatorName(
                    mPhone.getSubId(),
                    ss.getOperatorNumeric(), true, lac);
            strOperatorShort = ((MtkRIL)mCi).lookupOperatorName(
                    mPhone.getSubId(),
                    ss.getOperatorNumeric(), false, lac);
        }
        // If the PLMN is different with the ss.
        if ((!TextUtils.equals(strOperatorLong, ss.getOperatorAlphaLong())) ||
            (!TextUtils.equals(strOperatorShort, ss.getOperatorAlphaShort()))) {
            mForceBroadcastServiceState = true;
            // PLMN changes and we only update it if we are doing pollState
            // Other source should start pollState if mForceBroadcastServiceState is true.
            if (fromPollState)
                ss.setOperatorName(strOperatorLong, strOperatorShort, ss.getOperatorNumeric());
        }
        log("refreshSpn: " + strOperatorLong
                + ", " + strOperatorShort
                + ", fromPollState=" + fromPollState
                + ", mForceBroadcastServiceState=" + mForceBroadcastServiceState);
    }

    @Override
    protected void setPowerStateToDesired() {
        if (DBG) {
            String tmpLog = "mDeviceShuttingDown=" + mDeviceShuttingDown +
                    ", mDesiredPowerState=" + mDesiredPowerState +
                    ", getRadioState=" + mCi.getRadioState() +
                    ", mPowerOffDelayNeed=" + mPowerOffDelayNeed +
                    ", mAlarmSwitch=" + mAlarmSwitch +
                    ", mRadioDisabledByCarrier=" + mRadioDisabledByCarrier;
            log(tmpLog);
            mRadioPowerLog.log(tmpLog);
        }

        if (mPhone.isPhoneTypeGsm() && mAlarmSwitch) {
            if(DBG) log("mAlarmSwitch == true");
            Context context = mPhone.getContext();
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.cancel(mRadioOffIntent);
            mAlarmSwitch = false;
        }

        // If we want it on and it's off, turn it on
        if (mDesiredPowerState && !mRadioDisabledByCarrier
                && mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
            // [GSM] Send preferred network type before turn on radio power to avoid using wrong rat
            if (mPhone.isPhoneTypeGsm()) {
                setDeviceRatMode(mPhone.getPhoneId());
            }
            RadioManager.getInstance().sendRequestBeforeSetRadioPower(true, mPhone.getPhoneId());
            /// MTK-END
            mCi.setRadioPower(true, null);
        } else if ((!mDesiredPowerState || mRadioDisabledByCarrier) && mCi.getRadioState().isOn()) {
            // If it's on and available and we want it off gracefully
            if (mPhone.isPhoneTypeGsm() && mPowerOffDelayNeed) {
                if (mImsRegistrationOnOff && !mAlarmSwitch) {
                    if(DBG) log("mImsRegistrationOnOff == true");
                    Context context = mPhone.getContext();
                    AlarmManager am =
                        (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                    Intent intent = new Intent(ACTION_RADIO_OFF);
                    mRadioOffIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

                    mAlarmSwitch = true;
                    if (DBG) log("Alarm setting");
                    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            SystemClock.elapsedRealtime() + 3000, mRadioOffIntent);
                } else {
                    DcTracker dcTracker = mPhone.mDcTracker;
                    powerOffRadioSafely(dcTracker);
                }
            } else {
                DcTracker dcTracker = mPhone.mDcTracker;
                powerOffRadioSafely(dcTracker);
            }
        } else if (mDeviceShuttingDown && mCi.getRadioState().isAvailable()) {
            mCi.requestShutdown(null);
        }
    }

    /**
     * Hang up all voice call and turn off radio. Implemented by derived class.
     */
    @Override
    protected void hangupAndPowerOff() {
        // hang up all active voice calls
        if (!mPhone.isPhoneTypeGsm() || mPhone.isInCall()) {
            mPhone.mCT.mRingingCall.hangupIfAlive();
            mPhone.mCT.mBackgroundCall.hangupIfAlive();
            mPhone.mCT.mForegroundCall.hangupIfAlive();
        }
        hangupAllImsCall();
        //MTK-START some actions must be took before EFUN
        /// M: hangup all active IMS calls except Wifi Call. @{
        final Phone imsPhone = mPhone.getImsPhone();
        if ((imsPhone != null && imsPhone.isWifiCallingEnabled() == false)
            || mDeviceShuttingDown) {
            imsPhone.getForegroundCall().hangupIfAlive();
            imsPhone.getBackgroundCall().hangupIfAlive();
            imsPhone.getRingingCall().hangupIfAlive();
            log("hangupAndPowerOff: hangup VoLTE call.");
        }
        /// @}
        RadioManager.getInstance().sendRequestBeforeSetRadioPower(false, mPhone.getPhoneId());
        //MTK-END
        mCi.setRadioPower(false, obtainMessage(EVENT_RADIO_POWER_OFF_DONE));
    }

    // return Femto CsgId
    public String getFemtoCsgId() {
        return mCsgId;
    }

    // return Femto Plmn
    public String getFemtoPlmn() {
        return mFemtoPlmn;
    }

    // return Femto Act
    public String getFemtoAct() {
        return mFemtoAct;
    }

    private void onFemtoCellInfoResult(AsyncResult ar) {
        String info[];
        int isCsgCell = 0;

        if (ar.exception != null || ar.result == null) {
           loge("onFemtoCellInfo exception");
        } else {
            info = (String[]) ar.result;

            if (info.length > 0) {
                if (info[0] != null && info[0].length() > 0) {
                    mFemtocellDomain = Integer.parseInt(info[0]);
                    log("onFemtoCellInfo: mFemtocellDomain set to " + mFemtocellDomain);
                }

                if (info[3] != null && info[3].length() > 0) {
                   mFemtoPlmn = info[3];
                   log("onFemtoCellInfo: mFemtoPlmn set to " + mFemtoPlmn);
                }

                if (info[4] != null && info[4].length() > 0) {
                   mFemtoAct = info[4];
                   log("onFemtoCellInfo: mFemtoAct set to " + mFemtoAct);
                }

                if (info[5] != null && info[5].length() > 0) {
                   isCsgCell = Integer.parseInt(info[5]);
                }
                mIsFemtocell = isCsgCell;
                log("onFemtoCellInfo: domain= " + mFemtocellDomain + ",isCsgCell= " + isCsgCell);

                if (isCsgCell == 1) {
                    if (info[6] != null && info[6].length() > 0) {
                        mCsgId = info[6];
                        log("onFemtoCellInfo: mCsgId set to " + mCsgId);
                    }

                    if (info[8] != null && info[8].length() > 0) {
                        mHhbName = new String(IccUtils.hexStringToBytes(info[8]));
                        log("onFemtoCellInfo: mHhbName set from " + info[8] + " to " + mHhbName);
                    } else {
                        mHhbName = null;
                        log("onFemtoCellInfo: mHhbName is not available ,set to null");
                    }
                } else {
                    mCsgId = null;
                    mHhbName = null;
                    log("onFemtoCellInfo: csgId and hnbName are cleared");
                }
                if (isCsgCell != 2 && // ignore LTE & C2K case
                    (info[1] != null && info[1].length() > 0)  &&
                    (info[9] != null && info[0].length() > 0)) {
                    int state = Integer.parseInt(info[1]);
                    int cause = Integer.parseInt(info[9]);
                    try {
                        if (mServiceStateTrackerExt.needIgnoreFemtocellUpdate(
                                state, cause) == true) {
                            log("needIgnoreFemtocellUpdate due to state= " + state + ",cause= "
                                + cause);
                            // return here to prevent update variables and broadcast for CSG
                            return;
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
                Intent intent = new Intent(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
                SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());

                if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                    intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                }

                intent.putExtra(TelephonyIntents.EXTRA_SHOW_SPN, mCurShowSpn);
                intent.putExtra(TelephonyIntents.EXTRA_SPN, mCurSpn);
                intent.putExtra(TelephonyIntents.EXTRA_SHOW_PLMN, mCurShowPlmn);
                intent.putExtra(TelephonyIntents.EXTRA_PLMN, mCurPlmn);
                // Femtocell (CSG) info
                intent.putExtra(TelephonyIntents.EXTRA_HNB_NAME, mHhbName);
                intent.putExtra(TelephonyIntents.EXTRA_CSG_ID, mCsgId);
                intent.putExtra(TelephonyIntents.EXTRA_DOMAIN, mFemtocellDomain);
                // isFemtocell (LTE/C2K)
                intent.putExtra(TelephonyIntents.EXTRA_FEMTO, mIsFemtocell);

                mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);

                int phoneId = mPhone.getPhoneId();
                String plmn = mCurPlmn;
                if ((mHhbName == null) && (mCsgId != null)) {
                    try {
                        if (mServiceStateTrackerExt.needToShowCsgId() == true) {
                            plmn += " - ";
                            plmn += mCsgId;
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                } else if (mHhbName != null) {
                    plmn += " - ";
                    plmn += mHhbName;
                }
                boolean setResult = mSubscriptionController.setPlmnSpn(phoneId,
                        mCurShowPlmn, plmn, mCurShowSpn, mCurSpn);
                if (!setResult) {
                    mSpnUpdatePending = true;
                }
            }
        }
    }

    private void onInvalidSimInfoReceived(AsyncResult ar) {
        String[] InvalidSimInfo = (String[]) ar.result;
        String plmn = InvalidSimInfo[0];
        int cs_invalid = Integer.parseInt(InvalidSimInfo[1]);
        int ps_invalid = Integer.parseInt(InvalidSimInfo[2]);
        int cause = Integer.parseInt(InvalidSimInfo[3]);
        int testMode = -1;

        // do NOT apply IVSR when in TEST mode
        testMode = SystemProperties.getInt("vendor.gsm.gcf.testmode", 0);
        // there is only one test mode in modem. actually it's not SIM dependent , so remove
        // testmode2 property here

        log("onInvalidSimInfoReceived testMode:" + testMode + " cause:" + cause + " cs_invalid:"
                + cs_invalid + " ps_invalid:" + ps_invalid + " plmn:" + plmn
                + " mEverIVSR:" + mEverIVSR);

        //Check UE is set to test mode or not   (CTA =1,FTA =2 , IOT=3 ...)
        if (testMode != 0) {
            log("InvalidSimInfo received during test mode: " + testMode);
            return;
        }

        if (mServiceStateTrackerExt.isNeedDisableIVSR()) {
            log("Disable IVSR");
            return;
        }

        //MTK-ADD Start : for CS not registered , PS regsitered (ex: LTE PS only mode or 2/3G PS
        //only SIM card or CS domain network registeration temporary failure
        if (cs_invalid == 1) {
             isCsInvalidCard = true;
        }
         //MTK-ADD END : for CS not registered , PS regsitered (ex: LTE PS only mode or 2/3G PS
         //only SIM card or CS domain network registeration temporary failure

        /* check if CS domain ever sucessfully registered to the invalid SIM PLMN */
        /* Integrate ALPS00286197 with MR2 data only device state update , not to apply CS domain
           IVSR for data only device */
        if (mMtkVoiceCapable) {
            if ((cs_invalid == 1) && (mLastRegisteredPLMN != null)
                    && (plmn.equals(mLastRegisteredPLMN))) {
                log("InvalidSimInfo reset SIM due to CS invalid");
                setEverIVSR(true);
                mLastRegisteredPLMN = null;
                mLastPSRegisteredPLMN = null;
                ((MtkRIL)mCi).setSimPower(MtkRILConstants.SIM_POWER_RESET, null);
                return;
            }
        }

        /* check if PS domain ever sucessfully registered to the invalid SIM PLMN */
        //[ALPS02261450] - start
        if ((ps_invalid == 1) && (isAllowRecoveryOnIvsr(ar)) &&
                (mLastPSRegisteredPLMN != null) && (plmn.equals(mLastPSRegisteredPLMN))){
        //if ((ps_invalid == 1) && (mLastPSRegisteredPLMN != null) &&
        //              (plmn.equals(mLastPSRegisteredPLMN)))
        //[ALPS02261450] - end
            log("InvalidSimInfo reset SIM due to PS invalid ");
            setEverIVSR(true);
            mLastRegisteredPLMN = null;
            mLastPSRegisteredPLMN = null;
            ((MtkRIL)mCi).setSimPower(MtkRILConstants.SIM_POWER_RESET, null);
            return;
        }

        /* ALPS00324111: to force trigger IVSR */
        /* ALPS00407923  : The following code is to "Force trigger IVSR even
                  when MS never register to the
                  network before"The code was intended to cover the scenario of "invalid
                  SIM NW issue happen
                  at the first network registeration during boot-up".
                  However, it might cause false alarm IVSR ex: certain sim card only register
                  CS domain network , but PS domain is invalid.
                  For such sim card, MS will receive invalid SIM at the first PS domain
                  network registeration In such case , to trigger IVSR will be a false alarm,
                  which will cause  CS domain network
                  registeration time longer (due to IVSR impact)
                  It's a tradeoff. Please think about the false alarm impact
                  before using the code below.*/
        /*
        if ((mEverIVSR == false) && (gprsState != ServiceState.STATE_IN_SERVICE)
                &&(mSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE))
        {
            log("InvalidSimInfo set TRM due to never set IVSR");
            setEverIVSR(true);
            mLastRegisteredPLMN = null;
            mLastPSRegisteredPLMN = null;
            phone.setTRM(3, null);
            return;
        }
        */
    }

    private void onNetworkEventReceived(AsyncResult ar) {
        if (ar.exception != null || ar.result == null) {
           loge("onNetworkEventReceived exception");
        } else {
            // result[0]: <Act> not used
            // result[1]: <event_type> 0: for RAU event , 1: for TAU event
            int nwEventType = ((int[]) ar.result)[1];
            log("[onNetworkEventReceived] event_type:" + nwEventType);

            Intent intent = new Intent(TelephonyIntents.ACTION_NETWORK_EVENT);
            intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            intent.putExtra(TelephonyIntents.EXTRA_EVENT_TYPE, nwEventType + 1);

            mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private void onModulationInfoReceived(AsyncResult ar) {
        if (ar.exception != null || ar.result == null) {
           loge("onModulationInfoReceived exception");
        } else {
            int info[];
            int modulation;
            info = (int[]) ar.result;
            modulation = info[0];
            log("[onModulationInfoReceived] modulation:" + modulation);

            Intent intent = new Intent(TelephonyIntents.ACTION_NOTIFY_MODULATION_INFO);
            intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            intent.putExtra(TelephonyIntents.EXTRA_MODULATION_INFO, modulation);

            mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private boolean isAllowRecoveryOnIvsr(AsyncResult ar) {
        if (mPhone.isInCall()){
            log("[isAllowRecoveryOnIvsr] isInCall()=true");
            Message msg;
            msg = obtainMessage();
            msg.what = EVENT_INVALID_SIM_INFO;
            msg.obj = ar;
            sendMessageDelayed(msg, 10 * 1000); //check 10s later
            return false;
        } else {
            log("isAllowRecoveryOnIvsr() return true");
            return true;
        }
    }

    private void setEverIVSR(boolean value) {
        log("setEverIVSR:" + value);
        mEverIVSR = value;

        /* ALPS00376525 notify IVSR start event */
        if (value == true) {
            Intent intent = new Intent(TelephonyIntents.ACTION_IVSR_NOTIFY);
            intent.putExtra(TelephonyIntents.INTENT_KEY_IVSR_ACTION, "start");
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());

            if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            }

            log("broadcast ACTION_IVSR_NOTIFY intent");

            mPhone.getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    //TODO: check each values, which need to be reset.
    private void setNullState() {
        isCsInvalidCard = false;
    }

    protected final boolean IsInternationalRoamingException(String operatorNumeric) {
        String carrierConfig =
                MtkCarrierConfigManager.KEY_CARRIER_INTERNATIONAL_ROAMING_EXCEPTION_LIST_STRINGS;

        CarrierConfigManager configManager = (CarrierConfigManager)
                mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager == null) {
            Rlog.e(LOG_TAG, "Carrier config service is not available");
            return false;
        }

        PersistableBundle b = configManager.getConfigForSubId(mPhone.getSubId());
        if (b == null) {
            Rlog.e(LOG_TAG, "Can't get the config. subId = " + mPhone.getSubId());
            return false;
        }

        String[] operatorRoamingException = b.getStringArray(carrierConfig);
        if (operatorRoamingException == null) {
            Rlog.e(LOG_TAG, carrierConfig +  " is not available. " + "subId = " +
                    mPhone.getSubId());
            return false;
        }

        HashSet<String> internationalRoamingSet = new HashSet<>(
                Arrays.asList(operatorRoamingException));
        if (DBG) {
            Rlog.d(LOG_TAG, "For subId = " + mPhone.getSubId() +
                    ", international roaming exceptions are " +
                    Arrays.toString(internationalRoamingSet.toArray()) +
                    ", operatorNumeric = " + operatorNumeric);
        }

        // If registered plmn is in list, then roaming type should be international roaming.
        if (internationalRoamingSet.contains(operatorNumeric)) {
            if (DBG) Rlog.d(LOG_TAG, operatorNumeric + " in list.");
            return true;
        }

        if (DBG) Rlog.d(LOG_TAG, operatorNumeric + " is not in list.");
        return false;
    }

    /**
     * Set both voice and data roaming type,
     * judging from the ISO country of SIM VS network.
     */
    @Override
    protected void setRoamingType(ServiceState currentServiceState) {
        final boolean isVoiceInService =
                (currentServiceState.getVoiceRegState() == ServiceState.STATE_IN_SERVICE);
        boolean isInternationalRoaming = false;
        if (isVoiceInService) {
            if (currentServiceState.getVoiceRoaming()) {
                if (mPhone.isPhoneTypeGsm()) {
                    // check roaming type by MCC
                    if (inSameCountry(currentServiceState.getVoiceOperatorNumeric())) {
                        currentServiceState.setVoiceRoamingType(
                                ServiceState.ROAMING_TYPE_DOMESTIC);
                    } else {
                        currentServiceState.setVoiceRoamingType(
                                ServiceState.ROAMING_TYPE_INTERNATIONAL);
                    }

                    // check operator specified international roaming
                    isInternationalRoaming = IsInternationalRoamingException(
                            currentServiceState.getVoiceOperatorNumeric());

                    if (isInternationalRoaming) {
                        log(currentServiceState.getVoiceOperatorNumeric()
                                + " is in operator defined international roaming list");
                        currentServiceState.setVoiceRoamingType(
                                ServiceState.ROAMING_TYPE_INTERNATIONAL);
                    }
                } else {
                    // some carrier defines international roaming by indicator
                    int[] intRoamingIndicators = mPhone.getContext().getResources().getIntArray(
                            com.android.internal.R.array
                            .config_cdma_international_roaming_indicators);
                    if ((intRoamingIndicators != null) && (intRoamingIndicators.length > 0)) {
                        // It's domestic roaming at least now
                        currentServiceState.setVoiceRoamingType(ServiceState.ROAMING_TYPE_DOMESTIC);
                        int curRoamingIndicator = currentServiceState.getCdmaRoamingIndicator();
                        for (int i = 0; i < intRoamingIndicators.length; i++) {
                            if (curRoamingIndicator == intRoamingIndicators[i]) {
                                currentServiceState.setVoiceRoamingType(
                                        ServiceState.ROAMING_TYPE_INTERNATIONAL);
                                break;
                            }
                        }
                    } else {
                        // check roaming type by MCC
                        if (inSameCountry(currentServiceState.getVoiceOperatorNumeric())) {
                            currentServiceState.setVoiceRoamingType(
                                    ServiceState.ROAMING_TYPE_DOMESTIC);
                        } else {
                            currentServiceState.setVoiceRoamingType(
                                    ServiceState.ROAMING_TYPE_INTERNATIONAL);
                        }
                    }
                }
            } else {
                currentServiceState.setVoiceRoamingType(ServiceState.ROAMING_TYPE_NOT_ROAMING);
            }
        }
        final boolean isDataInService =
                (currentServiceState.getDataRegState() == ServiceState.STATE_IN_SERVICE);
        final int dataRegType = currentServiceState.getRilDataRadioTechnology();
        if (isDataInService) {
            if (!currentServiceState.getDataRoaming()) {
                currentServiceState.setDataRoamingType(ServiceState.ROAMING_TYPE_NOT_ROAMING);
            } else {
                if (mPhone.isPhoneTypeGsm()) {
                    if (ServiceState.isGsm(dataRegType)) {
                        if (isVoiceInService) {
                            // GSM data should have the same state as voice
                            currentServiceState.setDataRoamingType(currentServiceState
                                    .getVoiceRoamingType());
                        } else {
                            // we can not decide GSM data roaming type without voice
                            //currentServiceState.setDataRoamingType(ServiceState
                            //        .ROAMING_TYPE_UNKNOWN);
                            // MTK data framework need data roaming type when voice not registered
                            // check roaming type by MCC
                            if (inSameCountry(currentServiceState.getVoiceOperatorNumeric())) {
                                currentServiceState.setDataRoamingType(
                                        ServiceState.ROAMING_TYPE_DOMESTIC);
                            } else {
                                currentServiceState.setDataRoamingType(
                                        ServiceState.ROAMING_TYPE_INTERNATIONAL);
                            }

                            // check operator specified international roaming
                            isInternationalRoaming = IsInternationalRoamingException(
                                    currentServiceState.getVoiceOperatorNumeric());

                            if (isInternationalRoaming) {
                                log(currentServiceState.getVoiceOperatorNumeric()
                                        + " is in operator defined international roaming list");
                                currentServiceState.setDataRoamingType(
                                        ServiceState.ROAMING_TYPE_INTERNATIONAL);
                            }
                        }
                    } else {
                        // we can not decide 3gpp2 roaming state here
                        currentServiceState.setDataRoamingType(ServiceState.ROAMING_TYPE_UNKNOWN);
                    }
                } else {
                    if (ServiceState.isCdma(dataRegType)) {
                        if (isVoiceInService) {
                            // CDMA data should have the same state as voice
                            currentServiceState.setDataRoamingType(currentServiceState
                                    .getVoiceRoamingType());
                        } else {
                            // we can not decide CDMA data roaming type without voice
                            // set it as same as last time
                            currentServiceState.setDataRoamingType(ServiceState
                                    .ROAMING_TYPE_UNKNOWN);
                        }
                    } else {
                        // take it as 3GPP roaming
                        if (inSameCountry(currentServiceState.getDataOperatorNumeric())) {
                            currentServiceState.setDataRoamingType(ServiceState
                                    .ROAMING_TYPE_DOMESTIC);
                        } else {
                            currentServiceState.setDataRoamingType(
                                    ServiceState.ROAMING_TYPE_INTERNATIONAL);
                        }
                    }
                }
            }
        }
    }

    /// M: Add for VOLTE @{
    private final boolean isConcurrentVoiceAndDataAllowedForVolte() {
        if (mSS.getDataRegState() == ServiceState.STATE_IN_SERVICE
                && ServiceState.isLte(mSS.getRilDataRadioTechnology())
                && getImsServiceState() == ServiceState.STATE_IN_SERVICE) {
            return true;
        } else {
            return false;
        }
    }

    private final int getImsServiceState() {
        final Phone imsPhone = mPhone.getImsPhone();
        if (imsPhone != null) {
            return imsPhone.getServiceState().getState();
        }
        return ServiceState.STATE_OUT_OF_SERVICE;
    }

    private final boolean mergeEmergencyOnlyCdmaIms(boolean baseEmergencyOnly) {
        if (baseEmergencyOnly) {
            return baseEmergencyOnly;
        }

        if ((mNewSS.getVoiceRegState() == ServiceState.STATE_OUT_OF_SERVICE)
                && (mNewSS.getDataRegState() == ServiceState.STATE_OUT_OF_SERVICE)) {
            final Phone imsPhone = mPhone.getImsPhone();
            if (imsPhone != null) {
                return imsPhone.getServiceState().isEmergencyOnly();
            }
        }

        return baseEmergencyOnly;
    }

    /// @}

    /**
     * Post a notification to NotificationManager for network reject cause
     *
     * @param cause
     */
    private void setRejectCauseNotification(int cause) {
        if (DBG) log("setRejectCauseNotification: create notification " + cause);

        Context context = mPhone.getContext();
        mNotificationBuilder = new Notification.Builder(context);
        mNotificationBuilder.setWhen(System.currentTimeMillis());
        mNotificationBuilder.setAutoCancel(true);
        mNotificationBuilder.setSmallIcon(com.android.internal.R.drawable.stat_sys_warning);
        mNotificationBuilder.setChannel(NotificationChannelController.CHANNEL_ID_ALERT);

        Intent intent = new Intent();
        mNotificationBuilder.setContentIntent(PendingIntent.
                getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

        CharSequence details = "";
        CharSequence title = context.getText(com.mediatek.R.string.RejectCauseTitle);
        int notificationId = REJECT_NOTIFICATION;

        switch (cause) {
            case 2:
                details = context.getText(com.mediatek.R.string.MMRejectCause2);;
                break;
            case 3:
                details = context.getText(com.mediatek.R.string.MMRejectCause3);;
                break;
            case 5:
                details = context.getText(com.mediatek.R.string.MMRejectCause5);;
                break;
            case 6:
                details = context.getText(com.mediatek.R.string.MMRejectCause6);;
                break;
            case 13:
                details = context.getText(com.mediatek.R.string.MMRejectCause13);
                break;
            default:
                break;
        }

        if (DBG) log("setRejectCauseNotification: put notification " + title + " / " + details);
        mNotificationBuilder.setContentTitle(title);
        mNotificationBuilder.setContentText(details);

        NotificationManager notificationManager = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotification = mNotificationBuilder.build();
        notificationManager.notify(notificationId, mNotification);
    }

    @Override
    protected boolean isOperatorConsideredNonRoaming(ServiceState s) {
        boolean result = super.isOperatorConsideredNonRoaming(s);
        if (result) {
            log("isOperatorConsideredNonRoaming true");
        }
        return result;
    }

    @Override
    protected boolean isOperatorConsideredRoaming(ServiceState s) {
        boolean result = super.isOperatorConsideredRoaming(s);
        if (result) {
            log("isOperatorConsideredRoaming true");
        }
        return result;
    }

    @Override
    protected void onUpdateIccAvailability() {
        if (mUiccController == null ) {
            return;
        }

        UiccCardApplication newUiccApplication = getUiccCardApplication();
        ///M: [Network][C2K] Add for show EccButton when PIN and PUK status. @{
        if (mPhone.isPhoneTypeCdma() || mPhone.isPhoneTypeCdmaLte()) {
            if (newUiccApplication != null) {
                AppState appState = newUiccApplication.getState();
                if ((appState == AppState.APPSTATE_PIN || appState == AppState.APPSTATE_PUK)
                        && mNetworkExsit) {
                    mEmergencyOnly = true;
                } else {
                    mEmergencyOnly = false;
                }
                mEmergencyOnly = mergeEmergencyOnlyCdmaIms(mEmergencyOnly);
                log("[CDMA]onUpdateIccAvailability, appstate=" + appState
                        + ", mNetworkExsit=" + mNetworkExsit
                        + ", mEmergencyOnly=" + mEmergencyOnly);
            }
        }
        /// @}
        if (mUiccApplcation != newUiccApplication) {
            if (mUiccApplcation != null) {
                log("Removing stale icc objects.");
                mUiccApplcation.unregisterForReady(this);
                if (mIccRecords != null) {
                    mIccRecords.unregisterForRecordsLoaded(this);
                    if (mPhone.isPhoneTypeGsm()) {
                        mIccRecords.unregisterForRecordsEvents(this);
                    }
                }
                mIccRecords = null;
                mUiccApplcation = null;
            }
            if (newUiccApplication != null) {
                logv("New card found");
                mUiccApplcation = newUiccApplication;
                mIccRecords = mUiccApplcation.getIccRecords();
                if (mPhone.isPhoneTypeGsm()) {
                    mUiccApplcation.registerForReady(this, EVENT_SIM_READY, null);
                    if (mIccRecords != null) {
                        mIccRecords.registerForRecordsLoaded(this, EVENT_SIM_RECORDS_LOADED, null);
                        mIccRecords.registerForRecordsEvents(this, EVENT_SIM_OPL_LOADED, null);
                    }
                } else if (mIsSubscriptionFromRuim) {
                    mUiccApplcation.registerForReady(this, EVENT_RUIM_READY, null);
                    if (mIccRecords != null) {
                        mIccRecords.registerForRecordsLoaded(this, EVENT_RUIM_RECORDS_LOADED, null);
                    }
                }
            }
        }
    }

    @Override
    protected void updateOperatorNameFromEri() {
        if (mPhone.isPhoneTypeCdma()) {
            if ((mCi.getRadioState().isOn()) && (!mIsSubscriptionFromRuim)) {
                String eriText;
                // Now the Phone sees the new ServiceState so it can get the new ERI text
                if (mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) {
                    eriText = mPhone.getCdmaEriText();
                } else {
                    // Note that ServiceState.STATE_OUT_OF_SERVICE is valid used for
                    // mRegistrationState 0,2,3 and 4
                    eriText = mPhone.getContext().getText(
                            com.android.internal.R.string.roamingTextSearching).toString();
                }
                mSS.setOperatorAlphaLong(eriText);
            }
        } else if (mPhone.isPhoneTypeCdmaLte()) {
            boolean hasBrandOverride = mUiccController.getUiccCard(getPhoneId()) != null &&
                    mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() != null;
            if (!hasBrandOverride && (mCi.getRadioState().isOn()) && (mPhone.isEriFileLoaded()) &&
                    (!ServiceState.isLte(mSS.getRilVoiceRadioTechnology()) ||
                            mPhone.getContext().getResources().getBoolean(com.android.internal.R.
                            bool.config_LTE_eri_for_network_name)) &&
                            (!mIsSubscriptionFromRuim) &&
                            /// M: [Network][C2K] Sprint roaming control @{
                            mEnableERI) {
                            /// @}
                // Only when CDMA is in service, ERI will take effect
                String eriText = mSS.getOperatorAlpha();
                // Now the Phone sees the new ServiceState so it can get the new ERI text
                if (mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) {
                    /// M: [Network][C2K] Sprint roaming control @{
                    // Append ERI text to the end of PLMN
                    // eriText = mPhone.getCdmaEriText();
                    if (eriText == null || eriText.length() == 0) {
                        eriText = mPhone.getCdmaEriText();
                    } else if (mPhone.getCdmaEriText() != null &&
                            !mPhone.getCdmaEriText().equals("") &&
                            (mSS.getCdmaRoamingIndicator() != 1) && //Sprint GTR-SYSSEL-00061
                            (mSS.getCdmaRoamingIndicator() != 160)) { //Sprint GTR-SYSSEL-00063
                        log("Append ERI text to PLMN String");
                        eriText = mSS.getOperatorAlphaLong() + "- " + mPhone.getCdmaEriText();
                    }
                    /// @}
                } else if (mSS.getVoiceRegState() == ServiceState.STATE_POWER_OFF) {
                    eriText = (mIccRecords != null) ? mIccRecords.getServiceProviderName() : null;
                    if (TextUtils.isEmpty(eriText)) {
                        // Sets operator alpha property by retrieving from
                        // build-time system property
                        eriText = SystemProperties.get("ro.cdma.home.operator.alpha");
                    }
                } else if (mSS.getDataRegState() != ServiceState.STATE_IN_SERVICE) {
                    // Note that ServiceState.STATE_OUT_OF_SERVICE is valid used
                    // for mRegistrationState 0,2,3 and 4
                    eriText = mPhone.getContext()
                            .getText(com.android.internal.R.string.roamingTextSearching).toString();
                }
                mSS.setOperatorAlphaLong(eriText);
            }

            if (mUiccApplcation != null && mUiccApplcation.getState() == AppState.APPSTATE_READY &&
                    mIccRecords != null && getCombinedRegState() == ServiceState.STATE_IN_SERVICE
                    && !ServiceState.isLte(mSS.getRilVoiceRadioTechnology())) {
                // SIM is found on the device. If ERI roaming is OFF, and SID/NID matches
                // one configured in SIM, use operator name from CSIM record. Note that ERI, SID,
                // and NID are CDMA only, not applicable to LTE.
                boolean showSpn =
                        ((RuimRecords) mIccRecords).getCsimSpnDisplayCondition();
                /// M: [Network][C2K] China Telecom require not show spn @{
                try {
                    showSpn = showSpn && mServiceStateTrackerExt.allowSpnDisplayed();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                /// @}
                int iconIndex = mSS.getCdmaEriIconIndex();

                if (showSpn && (iconIndex == EriInfo.ROAMING_INDICATOR_OFF)
                        && isInHomeSidNid(mSS.getCdmaSystemId(), mSS.getCdmaNetworkId())
                        && mIccRecords != null) {
                    mSS.setOperatorAlphaLong(mIccRecords.getServiceProviderName());
                }
            }
        }
    }

    @Override
    protected void setOperatorIdd(String operatorNumeric) {
        // Retrieve the current country information
        // with the MCC got from opeatorNumeric.
        /// M: Use try catch to avoid Integer pars exception @{
        String idd = "";
        try {
            idd = mHbpcdUtils.getIddByMcc(
                Integer.parseInt(operatorNumeric.substring(0,3)));
        } catch (NumberFormatException ex) {
            loge("setOperatorIdd: idd error" + ex);
        } catch (StringIndexOutOfBoundsException ex) {
            loge("setOperatorIdd: idd error" + ex);
        }
        /// @}
        if (idd != null && !idd.isEmpty()) {
            mPhone.setGlobalSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_IDP_STRING,
                    idd);
        } else {
            // use default "+", since we don't know the current IDP
            mPhone.setGlobalSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_IDP_STRING, "+");
        }
    }

    /**
     * Radio power set from carrier action. if set to false means carrier desire to turn radio off
     * and radio wont be re-enabled unless carrier explicitly turn it back on.
     * @param enable indicate if radio power is enabled or disabled from carrier action.
     */
    @Override
    public void setRadioPowerFromCarrier(boolean enable) {
        mRadioDisabledByCarrier = !enable;
        // Replace aosp logic. Using RadioManager to controller the radio logic
        // setPowerStateToDesired();
        RadioManager.getInstance().setRadioPower(enable, mPhone.getPhoneId());
    }

    /// M: [Network][C2K] Sprint roaming control @{
    private String getSIMOperatorNumeric() {
        IccRecords r = mIccRecords;
        String mccmnc;
        String imsi;

        if (r != null) {
            mccmnc = r.getOperatorNumeric();

            if (mccmnc == null) {
                imsi = r.getIMSI();
                if (imsi != null && !imsi.equals("")) {
                    mccmnc = imsi.substring(0, 5);
                    log("get MCC/MNC from IMSI = " + mccmnc);
                }
            }
            if (mPhone.isPhoneTypeGsm()) {
                if (mccmnc == null || mccmnc.equals("")) {
                    String simMccMncProp = "vendor.gsm.ril.uicc.mccmnc";
                    if (mPhone.getPhoneId() != 0) {
                        simMccMncProp += "." + mPhone.getPhoneId();
                    }
                    mccmnc = SystemProperties.get(simMccMncProp, "");
                    log("get MccMnc from property(" + simMccMncProp + "): " + mccmnc);
                }
            }
            return mccmnc;
        } else {
            return null;
        }
    }
    /// @}

    // anchor method for powerOffRadioSafely
    @Override
    protected int mtkReplaceDdsIfUnset(int dds) {
        if (dds == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            int[] subIds = SubscriptionManager.getSubId(
                    RadioCapabilitySwitchUtil.getMainCapabilityPhoneId());
            if (subIds != null && subIds.length > 0) {
                log("powerOffRadioSafely: replace dds with main protocol sub ");
                dds = subIds[0];
            }
        }
        return dds;
    }

    // anchor method for powerOffRadioSafely
    @Override
    protected void mtkRegisterAllDataDisconnected() {
        boolean isAirplaneModeOn = TextUtils.equals(SystemProperties.
                get("persist.radio.airplane_mode_on", ""), "1");
        int dds = mtkReplaceDdsIfUnset(SubscriptionManager.getDefaultDataSubscriptionId());
        boolean isDefaultDataDisconnected = ProxyController.getInstance().isDataDisconnected(dds);

        log("powerOffRadioSafely: apm:" + isAirplaneModeOn + ", dds:" + dds
                + ", mSubId:" + mPhone.getSubId() + ", shutdown:" +  isDeviceShuttingDown()
                + ", isDefaultDataDisconnected:" + isDefaultDataDisconnected);

        if ((dds == mPhone.getSubId())
                || (!isAirplaneModeOn && !isDeviceShuttingDown())
                || isDefaultDataDisconnected) {
            synchronized (this) {
                log("powerOffRadioSafely: register EVENT_ALL_DATA_DISCONNECTED for self phone");
                mPhone.registerForAllDataDisconnected(this, EVENT_ALL_DATA_DISCONNECTED, null);
                mPendingRadioPowerOffAfterDataOff = true;
            }
        }
    }

    // anchor method for powerOffRadioSafely to customized the data disconnected timer
    @Override
    protected int mtkReplaceDisconnectTimer() {
        if (isDeviceShuttingDown()) {
            log("Shutting down, reduce 30s->5s for data to disconnect, then turn off radio.");
            return 5000;
        } else {
            return 30000;
        }
    }

    /**
     * Lookup operator name by numeric for network only.
     *
     * @param context the context used to get resource.
     * @param subId subId the id of the subscription to be queried.
     * @param numeric numeric name (MCC+MNC).
     * @param desireLongName whether is desire long name.
     * @return the operator name for the specific numeric.
     */
    protected static final String lookupOperatorName(Context context, int subId, String numeric,
            boolean desireLongName) {
        final String defaultName = numeric;
        String operName = null;

        // Step 1. check if phone is available.
        Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(subId));
        if (phone == null) {
            Rlog.e(LOG_TAG, "lookupOperatorName getPhone null");
            return defaultName;
        }

        // Step 2. get spn from mvno parrtern.
        operName = MtkSpnOverride.getInstance().getSpnByPattern(subId, numeric);

        // Step 3. check if is China Telecom MVNO.
        boolean isChinaTelecomMvno = isChinaTelecomMvno(context, subId, numeric, operName);

        // Step 4. get Spn by numeric from resource and spn_conf.xml
        if (operName == null || isChinaTelecomMvno) {
            operName = MtkSpnOverride.getInstance().getSpnByNumeric(numeric, desireLongName,
                    context);
        }

        // Step5. if didn't found any spn, return default name.
        return ((operName == null) ? defaultName : operName);
    }

    /**
     * Return whether is China Telecom MVNO for the specific numeric.
     *
     * @param context the context used to get resource.
     * @param subId subId the id of the subscription to be queried.
     * @param numeric numeric name (MCC+MNC).
     * @param mvnoOperName MVNO operator name.
     * @return true if is China Telecom MVNO.
     */
    private static final boolean isChinaTelecomMvno(Context context, int subId, String numeric,
            String mvnoOperName) {
        boolean isChinaTelecomMvno = false;
        final String ctName = context.getText(com.mediatek.internal.R.string.ct_name).toString();
        final String simCarrierName = TelephonyManager.from(context).getSimOperatorName(subId);
        if (ctName != null && ctName.equals(mvnoOperName)) {
            isChinaTelecomMvno = true;
        } else if (("20404".equals(numeric) || "45403".equals(numeric))
                && (ctName != null && ctName.equals(simCarrierName))) {
            isChinaTelecomMvno = true;
        }
        return isChinaTelecomMvno;
    }

    @Override
    protected boolean onSignalStrengthResult(AsyncResult ar) {
        boolean isGsm = false;
        int dataRat = mSS.getRilDataRadioTechnology();
        int voiceRat = mSS.getRilVoiceRadioTechnology();

        String mlog = "";
        if (mSignalStrength != null) {
            mlog = "old:{level:" + mSignalStrength.getLevel()
                + ", raw:"+ mSignalStrength.toString() + "}, ";
        }

        boolean ssChanged = super.onSignalStrengthResult(ar);

        if (mSignalStrength != null) {
            mlog = mlog + "new:{level:" + mSignalStrength.getLevel()
                + ", raw:"+ mSignalStrength.toString() + "}";
        }
        log(mlog);

        return ssChanged;
    }

    @Override
    protected boolean currentMccEqualsSimMcc(ServiceState s) {
        String simNumeric = ((TelephonyManager) mPhone.getContext().
                getSystemService(Context.TELEPHONY_SERVICE)).
                getSimOperatorNumericForPhone(getPhoneId());
        String operatorNumeric = s.getOperatorNumeric();
        //M: default should be false
        boolean equalsMcc = false;

        try {
            equalsMcc = simNumeric.substring(0, 3).
                    equals(operatorNumeric.substring(0, 3));
        } catch (Exception e){
        }
        return equalsMcc;
    }

    private boolean getImsEccOnly() {
        final Phone imsPhone = mPhone.getImsPhone();
        if (imsPhone != null) {
            return imsPhone.getServiceState().isEmergencyOnly();
        }
        return false;
    }

    /* Reset Service state when IWLAN is enabled as polling in airplane mode
     * causes state to go to OUT_OF_SERVICE state instead of STATE_OFF
     */
    @Override
    protected void resetServiceStateInIwlanMode() {
        if (mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
            log("set service state as POWER_OFF");
            boolean restoreIwlanState = false;
            if (((MtkServiceState) mNewSS).getIwlanRegState()
                    == ServiceState.STATE_IN_SERVICE) {
                log("pollStateDone: restore iwlan RAT value");
                restoreIwlanState = true;
            }
            mNewSS.setStateOff();
            if (restoreIwlanState) {
                ((MtkServiceState) mNewSS).setIwlanRegState(ServiceState.STATE_IN_SERVICE);
                log("pollStateDone: mNewSS = " + mNewSS);
            }
        }
    }

    private int getRegStateFromHalRegState(int halRegState) {
        switch (halRegState) {
            case 0:
            case 10:
                return NetworkRegistrationState.REG_STATE_NOT_REG_NOT_SEARCHING;
            case 1:
                return NetworkRegistrationState.REG_STATE_HOME;
            case 2:
            case 12:
                return NetworkRegistrationState.REG_STATE_NOT_REG_SEARCHING;
            case 3:
            case 13:
                return NetworkRegistrationState.REG_STATE_DENIED;
            case 4:
            case 14:
                return NetworkRegistrationState.REG_STATE_UNKNOWN;
            case 5:
                return NetworkRegistrationState.REG_STATE_ROAMING;
            default:
                return NetworkRegistrationState.REG_STATE_NOT_REG_NOT_SEARCHING;
        }
    }

    private boolean isEmergencyOnly(int halRegState) {
        switch (halRegState) {
            case 10:
            case 12:
            case 13:
            case 14:
                return true;
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            default:
                return false;
        }
    }

    private int[] getAvailableServices(int regState, int domain, boolean emergencyOnly) {
        int[] availableServices = null;

        // In emergency only states, only SERVICE_TYPE_EMERGENCY is available.
        // Otherwise, certain services are available only if it's registered on home or roaming
        // network.
        if (emergencyOnly) {
            availableServices = new int[] {NetworkRegistrationState.SERVICE_TYPE_EMERGENCY};
        } else if (regState == NetworkRegistrationState.REG_STATE_ROAMING
                || regState == NetworkRegistrationState.REG_STATE_HOME) {
            if (domain == NetworkRegistrationState.DOMAIN_PS) {
                availableServices = new int[] {NetworkRegistrationState.SERVICE_TYPE_DATA};
            } else if (domain == NetworkRegistrationState.DOMAIN_CS) {
                availableServices = new int[] {
                        NetworkRegistrationState.SERVICE_TYPE_VOICE,
                        NetworkRegistrationState.SERVICE_TYPE_SMS,
                        NetworkRegistrationState.SERVICE_TYPE_VIDEO
                };
            }
        }
        return availableServices;
    }

    private CellIdentity convertHalCellIdentityToCellIdentity(
            android.hardware.radio.V1_2.CellIdentity cellIdentity) {
        if (cellIdentity == null) {
            return null;
        }

        CellIdentity result = null;
        switch (cellIdentity.cellInfoType) {
            case CellInfoType.GSM: {
                if (cellIdentity.cellIdentityGsm.size() == 1) {
                    android.hardware.radio.V1_2.CellIdentityGsm cellIdentityGsm =
                        cellIdentity.cellIdentityGsm.get(0);

                    result = new CellIdentityGsm(
                            cellIdentityGsm.base.lac,
                            cellIdentityGsm.base.cid,
                            cellIdentityGsm.base.arfcn,
                            cellIdentityGsm.base.bsic,
                            cellIdentityGsm.base.mcc,
                            cellIdentityGsm.base.mnc,
                            cellIdentityGsm.operatorNames.alphaLong,
                            cellIdentityGsm.operatorNames.alphaShort);
                }
                break;
            }
            case CellInfoType.WCDMA: {
                if (cellIdentity.cellIdentityWcdma.size() == 1) {
                    android.hardware.radio.V1_2.CellIdentityWcdma cellIdentityWcdma =
                            cellIdentity.cellIdentityWcdma.get(0);

                    result = new CellIdentityWcdma(
                            cellIdentityWcdma.base.lac,
                            cellIdentityWcdma.base.cid,
                            cellIdentityWcdma.base.psc,
                            cellIdentityWcdma.base.uarfcn,
                            cellIdentityWcdma.base.mcc,
                            cellIdentityWcdma.base.mnc,
                            cellIdentityWcdma.operatorNames.alphaLong,
                            cellIdentityWcdma.operatorNames.alphaShort);
                }
                break;
            }
            case CellInfoType.TD_SCDMA: {
                if (cellIdentity.cellIdentityTdscdma.size() == 1) {
                    android.hardware.radio.V1_2.CellIdentityTdscdma cellIdentityTdscdma =
                            cellIdentity.cellIdentityTdscdma.get(0);

                    result = new  CellIdentityTdscdma(
                            cellIdentityTdscdma.base.mcc,
                            cellIdentityTdscdma.base.mnc,
                            cellIdentityTdscdma.base.lac,
                            cellIdentityTdscdma.base.cid,
                            cellIdentityTdscdma.base.cpid,
                            cellIdentityTdscdma.operatorNames.alphaLong,
                            cellIdentityTdscdma.operatorNames.alphaShort);
                }
                break;
            }
            case CellInfoType.LTE: {
                if (cellIdentity.cellIdentityLte.size() == 1) {
                    android.hardware.radio.V1_2.CellIdentityLte cellIdentityLte =
                            cellIdentity.cellIdentityLte.get(0);

                    result = new CellIdentityLte(
                            cellIdentityLte.base.ci,
                            cellIdentityLte.base.pci,
                            cellIdentityLte.base.tac,
                            cellIdentityLte.base.earfcn,
                            cellIdentityLte.bandwidth,
                            cellIdentityLte.base.mcc,
                            cellIdentityLte.base.mnc,
                            cellIdentityLte.operatorNames.alphaLong,
                            cellIdentityLte.operatorNames.alphaShort);
                }
                break;
            }
            case CellInfoType.CDMA: {
                if (cellIdentity.cellIdentityCdma.size() == 1) {
                    android.hardware.radio.V1_2.CellIdentityCdma cellIdentityCdma =
                            cellIdentity.cellIdentityCdma.get(0);

                    result = new CellIdentityCdma(
                            cellIdentityCdma.base.networkId,
                            cellIdentityCdma.base.systemId,
                            cellIdentityCdma.base.baseStationId,
                            cellIdentityCdma.base.longitude,
                            cellIdentityCdma.base.latitude,
                            cellIdentityCdma.operatorNames.alphaLong,
                            cellIdentityCdma.operatorNames.alphaShort);
                }
                break;
            }
            case CellInfoType.NONE:
            default:
                break;
        }

        return result;
    }

    private NetworkRegistrationState createRegistrationStateFromVoiceRegState(Object result) {
        int transportType = AccessNetworkConstants.TransportType.WWAN;
        int domain = NetworkRegistrationState.DOMAIN_CS;

        android.hardware.radio.V1_2.VoiceRegStateResult voiceRegState =
                (android.hardware.radio.V1_2.VoiceRegStateResult) result;
        int regState = getRegStateFromHalRegState(voiceRegState.regState);
        int accessNetworkTechnology = ServiceState.
                rilRadioTechnologyToNetworkType(voiceRegState.rat);
        int reasonForDenial = voiceRegState.reasonForDenial;
        boolean emergencyOnly = isEmergencyOnly(voiceRegState.regState);
        boolean cssSupported = voiceRegState.cssSupported;
        int roamingIndicator = voiceRegState.roamingIndicator;
        int systemIsInPrl = voiceRegState.systemIsInPrl;
        int defaultRoamingIndicator = voiceRegState.defaultRoamingIndicator;
        int[] availableServices = getAvailableServices(
                regState, domain, emergencyOnly);
        CellIdentity cellIdentity =
                convertHalCellIdentityToCellIdentity(voiceRegState.cellIdentity);

        return new NetworkRegistrationState(transportType, domain, regState,
                accessNetworkTechnology, reasonForDenial, emergencyOnly, availableServices,
                cellIdentity, cssSupported, roamingIndicator, systemIsInPrl,
                defaultRoamingIndicator);
    }

    private NetworkRegistrationState createRegistrationStateFromDataRegState(Object result) {
        int transportType = AccessNetworkConstants.TransportType.WWAN;
        int domain = NetworkRegistrationState.DOMAIN_PS;

        android.hardware.radio.V1_2.DataRegStateResult dataRegState =
                (android.hardware.radio.V1_2.DataRegStateResult) result;
        int regState = getRegStateFromHalRegState(dataRegState.regState);
        int accessNetworkTechnology = ServiceState.
                rilRadioTechnologyToNetworkType(dataRegState.rat);
        int reasonForDenial = dataRegState.reasonDataDenied;
        boolean emergencyOnly = isEmergencyOnly(dataRegState.regState);
        int maxDataCalls = dataRegState.maxDataCalls;
        int[] availableServices = getAvailableServices(regState, domain, emergencyOnly);
        CellIdentity cellIdentity =
                convertHalCellIdentityToCellIdentity(dataRegState.cellIdentity);

        return new NetworkRegistrationState(transportType, domain, regState,
                accessNetworkTechnology, reasonForDenial, emergencyOnly, availableServices,
                cellIdentity, maxDataCalls);
    }

    /**
     * @return all available cell information or null if none.
     */
    @Override
    public List<CellInfo> getAllCellInfo(WorkSource workSource) {
        MtkCellInfoResult result = new MtkCellInfoResult();
        if (VDBG) log("SST.getAllCellInfo(): E");
        int ver = mCi.getRilVersion();
        if (ver >= 8) {
            if (isCallerOnDifferentThread()) {
                if ((SystemClock.elapsedRealtime() - mLastCellInfoListTime)
                        > MTK_LAST_CELL_INFO_LIST_MAX_AGE_MS) {
                    // use MtkHandler to handle the CELL_IFNO_LIST with sub thread.
                    Message msg = mtkHandler.obtainMessage(EVENT_MTK_GET_CELL_INFO_LIST, result);
                    synchronized(result.lockObj) {
                        result.list = null;
                        mCi.getCellInfoList(msg, workSource);
                        try {
                            result.lockObj.wait(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (DBG) log("SST.getAllCellInfo(): return last, back to back calls");
                    result.list = mLastCellInfoList;
                }
            } else {
                if (DBG) log("SST.getAllCellInfo(): return last, same thread can't block");
                result.list = mLastCellInfoList;
            }
        } else {
            if (DBG) log("SST.getAllCellInfo(): not implemented");
            result.list = null;
        }
        synchronized(result.lockObj) {
            if (result.list != null) {
                if (VDBG) log("SST.getAllCellInfo(): X size=" + result.list.size()
                        + " list=" + result.list);
                return result.list;
            } else {
                if (DBG) log("SST.getAllCellInfo(): X size=0 list=null");
                return null;
            }
        }
    }

    @Override
    public boolean getDesiredPowerState() {
        if (RadioManager.getInstance().isModemOff(mPhone.getPhoneId())) {
            return false;
        }
        return mDesiredPowerState;
    }

    protected void hangupAllImsCall() {
        // hangup all active IMS calls except Wifi Call.
        final Phone imsPhone = mPhone.getImsPhone();
        if (imsPhone != null && imsPhone.isWifiCallingEnabled() == false) {
            imsPhone.getForegroundCall().hangupIfAlive();
            imsPhone.getBackgroundCall().hangupIfAlive();
            imsPhone.getRingingCall().hangupIfAlive();
            log("hangupAllImsCall: hangup VoLTE call.");
        }
    }
};
