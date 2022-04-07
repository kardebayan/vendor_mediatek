package com.mediatek.internal.telephony.datasub;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.NetworkRequest;
import android.net.NetworkCapabilities;
import android.net.Network;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseCallState;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;

import com.android.ims.ImsException;

import com.android.internal.telephony.GsmCdmaCall;
import com.android.internal.telephony.GsmCdmaCallTracker;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker;
import com.android.internal.telephony.imsphone.ImsPhoneCall;

import com.android.internal.telephony.IOnSubscriptionsChangedListener;
import com.android.internal.telephony.ITelephonyRegistry;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SettingsObserver;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.ims.internal.MtkImsManagerEx;

import com.mediatek.wfo.IWifiOffloadService;
import com.mediatek.wfo.WifiOffloadManager;
import com.mediatek.wfo.IMwiService;
import com.mediatek.wfo.MwisConstants;

import com.mediatek.internal.telephony.OpTelephonyCustomizationFactoryBase;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkSubscriptionManager;

import com.mediatek.provider.MtkSettingsExt;

import static com.mediatek.internal.telephony.datasub.DataSubConstants.*;

public class SmartDataSwitchAssistant extends Handler {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "SmartDataSwitch";

    public enum SwitchState {
        IDLE(0), INCALL_SWITCH(1), INCALL_NOT_SWITCH(2);

        SwitchState(int code) {
            this.code = code;
        }

        private int code;

        public int getCode() {
            return code;
        }

        public boolean isInCall() {
            return (this == INCALL_SWITCH || this == INCALL_NOT_SWITCH);
        }
    }

    // call type
    public static final int CALL_TYPE_UNKNOW = -1;
    public static final int CALL_TYPE_CS_2G = 0;        // 2G call
    public static final int CALL_TYPE_CS_3G = 1;        // 3G call
    public static final int CALL_TYPE_CS_CDMA = 2;        // CDMA call
    public static final int CALL_TYPE_PS_CELLULAR = 3;  // VoLTE/ViLTE call
    public static final int CALL_TYPE_PS_WIFI = 4;      // VoWiFi call

    // Disallow cause for data switch
    public static final int DISALLOW_CAUSE_NONE = 0;  // allow to switch
    public static final int DISALLOW_CAUSE_TEMP_DATA_SETTING_OFF = 1;
    public static final int DISALLOW_CAUSE_DATA_SIM_INCALL = 2;
    public static final int DISALLOW_CAUSE_OPERATOR_CHOICE = 4;
    public static final int DISALLOW_CAUSE_INVAILD_CALL_TYPE = 8;
    public static final int DISALLOW_CAUSE_DEFAULTE_DATA_UNSET = 16;

    private static SmartDataSwitchAssistant sSmartDataSwitchAssistant = null;
    private ISmartDataSwitchAssistantOpExt mSmartDataOpExt  = null;
    protected ContentResolver mResolver;
    protected Phone[] mPhones;
    protected int mPhoneNum;
    protected int mDefaultDataPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
    protected boolean isTemporaryDataServiceSettingOn = false;


    protected SwitchState mState = SwitchState.IDLE;
    // needs to reset when mState changed to IDLE -- start
    protected int mInCallPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
    protected int mCallType = CALL_TYPE_UNKNOW;
    // needs to reset when mState changed to IDLE -- end

    // Reset settings value of temporary data service when sim hot plug and data switch
    protected boolean isResetTdsSettingsByFwk = true;

    private static String mOperatorSpec;
    private Context mContext = null;

    // system service
    private OpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory = null;
    private TelephonyManager mTelephonyManager = null;
    private ConnectivityManager mConnectivityManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private IWifiOffloadService mWfoService;

    private HandoverStateListener mHandoverStateListener;

    // event id must be multiple of EVENT_ID_INTVL
    private static final int EVENT_ID_INTVL = 10;
    private static final int EVENT_CALL_STARTED = EVENT_ID_INTVL * 1;
    private static final int EVENT_CALL_ENDED = EVENT_ID_INTVL * 2;
    private static final int EVENT_SRVCC_STATE_CHANGED = EVENT_ID_INTVL * 3;
    private static final int EVENT_TEMPORARY_DATA_SERVICE_SETTINGS = EVENT_ID_INTVL * 4;
    private static final int EVENT_SERVICE_STATE_CHANGED = EVENT_ID_INTVL * 5;

    // Settings URI of tempoary data service enable
    private static final String TEMP_DATA_SERVICE = MtkSettingsExt.Global.DATA_SERVICE_ENABLED;
    private static final String PROPERTY_DEFAULT_DATA_SELECTED
            = "persist.vendor.radio.default.data.selected";

    protected final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Message msg = null;
            if (action == null) {
                return;
            }
            if (action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                // update mDefaultDataPhoneId
                int defaultDataSubId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                logd("onReceive: DEFAULT_DATA_SUBSCRIPTION_CHANGED defaultDataSubId=" +
                        defaultDataSubId);
                updateDefaultDataPhoneId(defaultDataSubId, "DataSubChanged");
            } else if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                int status = intent.getIntExtra(MtkSubscriptionManager.INTENT_KEY_DETECT_STATUS,
                        MtkSubscriptionManager.EXTRA_VALUE_NOCHANGE);
                // check if reset settings is handle by framework when sim plug out.
                if (status == MtkSubscriptionManager.EXTRA_VALUE_REMOVE_SIM ||
                            status == MtkSubscriptionManager.EXTRA_VALUE_NEW_SIM) {
                    logd("onSubInfoRecordUpdated: Detecct Status:" + status);
                    resetTdsSettingsByFwk();
                }
            } else if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                logd("onPhoneStateChanged: PhoneState:" + phoneState);
                if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    msg = obtainMessage(EVENT_CALL_ENDED);
                    msg.sendToTarget();
                }
                else if (phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING) ||
                        phoneState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    int activeCallPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
                    activeCallPhoneId = getActiveCallPhoneId();
                    if (activeCallPhoneId != SubscriptionManager.INVALID_PHONE_INDEX) {
                        logd("onPhoneStateChanged: phoneId=" + activeCallPhoneId);
                        msg = obtainMessage(EVENT_CALL_STARTED + activeCallPhoneId);
                        msg.sendToTarget();
                    }
                }
            } else {
                // unknown action
            }
        }
    };

    private class HandoverStateListener extends WifiOffloadManager.Listener {
        @Override
        public void onHandover(int simIdx, int stage, int ratType) {
            logd("onHandover() simIdx:" + simIdx + " stage:" + stage + " ratType:" + ratType);
            Message msg = null;
            if (stage == WifiOffloadManager.HANDOVER_END &&
                    mInCallPhoneId != SubscriptionManager.INVALID_PHONE_INDEX) {
                if (ratType == WifiOffloadManager.RAN_TYPE_WIFI) {
                    onHandoverToWifi();
                } else if (ratType == WifiOffloadManager.RAN_TYPE_MOBILE_3GPP ||
                    ratType == WifiOffloadManager.RAN_TYPE_MOBILE_3GPP2){
                    onHandoverToCellular();
                }
            }
        }
    }

    private final OnSubscriptionsChangedListener mSubscriptionsChangedListener =
            new OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            // update default dat sub id
            int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
            updateDefaultDataPhoneId(defaultDataSubId, "SubscriptionsChanged");
        }
    };

    protected final SettingsObserver mSettingsObserver;
    protected void regSettingsObserver() {
        mSettingsObserver.unobserve();
        mSettingsObserver.observe(
                Settings.Global.getUriFor(TEMP_DATA_SERVICE),
                EVENT_TEMPORARY_DATA_SERVICE_SETTINGS);
    }

    public static SmartDataSwitchAssistant makeSmartDataSwitchAssistant(Context context,
            Phone[] phones) {
        if (context == null || phones == null) {
            throw new RuntimeException("param is null");
        }

        if (sSmartDataSwitchAssistant == null) {
            sSmartDataSwitchAssistant = new SmartDataSwitchAssistant(context, phones);
        }
        logd("makeSDSA: X sSDSA =" + sSmartDataSwitchAssistant);
        return sSmartDataSwitchAssistant;
    }

    public static SmartDataSwitchAssistant getInstance() {
        if (sSmartDataSwitchAssistant == null) {
            throw new RuntimeException("Should not be called before sSmartDataSwitchAssistant");
        }
        return sSmartDataSwitchAssistant;
    }

    private SmartDataSwitchAssistant(Context context, Phone[] phones) {
        logd(" is created");
        mPhones = phones;
        mPhoneNum = phones.length;
        mContext = context;
        mResolver = mContext.getContentResolver();
        mOperatorSpec = SystemProperties.get(PROPERTY_OPERATOR_OPTR, OPERATOR_OM);
        int tempDataSettings = 0;
        mSettingsObserver = new SettingsObserver(mContext, this);

        if (isSmartDataSwitchSupport()) {
            registerEvents();
        }

        initOpSmartDataSwitchAssistant(context);
        if (mSmartDataOpExt == null) {
            mSmartDataOpExt = new SmartDataSwitchAssistantOpExt(context);
        }
        mSmartDataOpExt.init(this);

        tempDataSettings = Settings.Global.getInt(mResolver, TEMP_DATA_SERVICE, 0);
        if (tempDataSettings != 0) {
            isTemporaryDataServiceSettingOn = true;
        } else {
            isTemporaryDataServiceSettingOn = false;
        }
        logd("init isTemporaryDataServiceSettingOn=" + isTemporaryDataServiceSettingOn);
    }

    private void initOpSmartDataSwitchAssistant(Context context) {
       try {
           mTelephonyCustomizationFactory =
                   OpTelephonyCustomizationUtils.getOpFactory(context);
           mSmartDataOpExt =
                   mTelephonyCustomizationFactory.makeSmartDataSwitchAssistantOpExt(context);
       } catch (Exception e) {
           if (DBG) loge("mSmartDataOpExt init fail");
           e.printStackTrace();
       }
    }

    public void dispose() {
        logd("SmartDataSwitchAssistant.dispose");
        if (isSmartDataSwitchSupport()) {
            unregisterEvents();
        }
    }

    private void registerEvents() {
        logd("registerEvents");

        regSettingsObserver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        filter.addAction(TelephonyManager.ACTION_PRECISE_CALL_STATE_CHANGED);
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        if (isResetTdsSettingsByFwk) {
            filter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        }
        mContext.registerReceiver(mBroadcastReceiver, filter);

        SubscriptionManager.from(mContext).addOnSubscriptionsChangedListener(
                mSubscriptionsChangedListener);
    }

    private void unregisterEvents() {
        logd("unregisterEvents");
        TelephonyManager tm = getTelephonyManager();

        SubscriptionManager.from(mContext).removeOnSubscriptionsChangedListener(
                mSubscriptionsChangedListener);
    }

    @Override
    public void handleMessage(Message msg) {
        // msg_id = n * EVENT_ID_INTVL + phone_id, use mod operator to get phone_id
        // event_id must be multiple of EVENT_ID_INTVL => n * EVENT_ID_INTVL
        int phoneId = msg.what % EVENT_ID_INTVL;
        int eventId = msg.what - phoneId;
        AsyncResult ar;
        switch (eventId) {
            case EVENT_CALL_STARTED:
                if (mState == SwitchState.IDLE) {
                    logd("Call Started, phoneId=" + phoneId);
                    updateCallType(phoneId);
                    onCallStarted(phoneId);
                }
                break;
            case EVENT_CALL_ENDED:
                logd("Call Ended, phoneId=" + mInCallPhoneId);
                onCallEnded();
                break;
            case EVENT_SRVCC_STATE_CHANGED:
                logd("SRVCC, phoneId=" + phoneId);
                onSrvccStateChanged();
                break;
            case EVENT_TEMPORARY_DATA_SERVICE_SETTINGS:
                boolean oldSettings = isTemporaryDataServiceSettingOn;
                boolean newSettings = false;
                int tdsSettings = Settings.Global.getInt(mResolver, TEMP_DATA_SERVICE, 0);
                if (tdsSettings != 0){
                    newSettings = true;
                } else {
                    newSettings = false;
                }

                if (oldSettings != newSettings) {
                    isTemporaryDataServiceSettingOn = newSettings;
                    logd("TemporaryDataSetting changed newSettings=" + newSettings);
                    onTemporaryDataSettingsChanged();
                }
                break;
            case EVENT_SERVICE_STATE_CHANGED:
                onServiceStateChanged(phoneId);
                break;
            default:
                logd("Unhandled message with number: " + msg.what);
                break;
        }
    }

    public void regSrvccEvent() {
        for (int i = 0; i < mPhoneNum; i++) {
            // register SRVCC event
            mPhones[i].registerForHandoverStateChanged(this, EVENT_SRVCC_STATE_CHANGED + i, null);
        }
    }

    public void unregSrvccEvent() {
        for (int i = 0; i < mPhoneNum; i++) {
            // unregister SRVCC event
            mPhones[i].unregisterForHandoverStateChanged(this);
        }
    }

    public void regServiceStateChangedEvent() {
        for (int i = 0; i < mPhoneNum; i++) {
            // register service state changed event
            mPhones[i].registerForServiceStateChanged(this, EVENT_SERVICE_STATE_CHANGED + i, null);
        }
    }

    public void unregServiceStateChangedEvent() {
        for (int i = 0; i < mPhoneNum; i++) {
            // unregister service state changed event
            mPhones[i].unregisterForServiceStateChanged(this);
        }
    }

    public void regImsHandoverEvent() {
        // register handover event. Ex: VoLTE handover to VoWifi.
        if (mWfoService == null) {
            mWfoService = getWifiOffLoadService();
        }
        if (mWfoService != null) {
            try {
                if (mHandoverStateListener == null) {
                    mHandoverStateListener = new HandoverStateListener();
                }
                mWfoService.registerForHandoverEvent(mHandoverStateListener);
            } catch (Exception e) {
                loge("regImsHandoverEvent(): RemoteException mWfoService()");
            }
        }
    }

    public void unregImsHandoverEvent() {
        try {
            // unregister handover event
            mWfoService.unregisterForHandoverEvent(mHandoverStateListener);
        } catch (Exception e) {
            loge("unregImsHandoverEvent: RemoteException mWfoService()");
        }
    }

    private boolean preCheckByCallState(Intent intent) {
        boolean result = false;
        int ringingCallState = intent.getIntExtra(
                TelephonyManager.EXTRA_RINGING_CALL_STATE,
                PreciseCallState.PRECISE_CALL_STATE_NOT_VALID);
        int foregroundCallState = intent.getIntExtra(
                TelephonyManager.EXTRA_FOREGROUND_CALL_STATE,
                PreciseCallState.PRECISE_CALL_STATE_NOT_VALID);
        logd("preCheckByCallState: ringingCallState:" + ringingCallState +
                " foregroundCallState:" + foregroundCallState);

        if ((foregroundCallState == PreciseCallState.PRECISE_CALL_STATE_DIALING) ||
                (ringingCallState == PreciseCallState.PRECISE_CALL_STATE_INCOMING)) {
            result = true;
        }

        // check if need customize for OP
        mSmartDataOpExt.preCheckByCallStateExt(intent, result);
        logd("preCheckByCallState: result:" + result);
        return result;
    }

    public int getActiveCallPhoneId () {
        int inCallPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;

        // check for PS call
        inCallPhoneId = getImsCallPhoneId();
        if (inCallPhoneId == SubscriptionManager.INVALID_PHONE_INDEX) {
            // check for CS call
            inCallPhoneId = getCsCallPhoneId();
        }

        return inCallPhoneId;
    }

    public void updateCallType(int phoneId){
        if (phoneId == SubscriptionManager.INVALID_PHONE_INDEX) {
            loge("updateCallType() invalid Phone Id!");
            return;
        }

        if (((MtkGsmCdmaPhone)mPhones[phoneId]).isDuringImsCall()) {
            updateCallTypeForPsCall(phoneId);
        } else {
            updateCallTypeForCsCall(phoneId);
        }
    }

    private int getImsCallPhoneId(){
        int activeImsPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
        int count = 0;
        try {
            for (int i = 0; i < mPhoneNum; i++) {
                ImsPhone mImsPhone = (ImsPhone)mPhones[i].getImsPhone();
                if (mImsPhone != null) {
                    ImsPhoneCallTracker callTracker = (ImsPhoneCallTracker)mImsPhone.getCallTracker();
                    if (callTracker != null) {
                        count = ((ImsPhoneCall)(callTracker.mRingingCall)).getConnections().size();
                        logd("ImsRingCall count=" + count);
                        count += ((ImsPhoneCall)(callTracker.mForegroundCall)).getConnections().size();
                        logd("ImsRingCall count + ImsForegroundCall count=" + count);
                        if (count > 0) {
                            activeImsPhoneId = i;
                            break;
                        } else {
                            count = 0;
                        }
                    }
                } else {
                    logd("can't get instance of ImsPhone");
                }
            }
        } catch(Exception e) {
           loge("getImsCallPhoneId: " + e);
        }
        logd("get ImsCallPhoneId = " + activeImsPhoneId);
        return activeImsPhoneId;
    }

    private int getCsCallPhoneId(){
        int activeCsPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
        for (int i = 0; i < mPhoneNum; i++) {
            GsmCdmaCallTracker callTracker = (GsmCdmaCallTracker)mPhones[i].getCallTracker();
            boolean mIsRinging = ((GsmCdmaCall)(callTracker.mRingingCall)).isRinging();
            boolean mForegroundCallIsIdle = (((GsmCdmaCall)(callTracker.mForegroundCall)).isIdle());
            logd("[" + i + "]: mIsRinging=" + mIsRinging
                        + ", mForegroundCallIsIdle=" + mForegroundCallIsIdle);
            if (mIsRinging || !mForegroundCallIsIdle) {
                activeCsPhoneId = i;
                break;
            }
        }

        logd("get getCsCallPhoneId = " + activeCsPhoneId);
        return activeCsPhoneId;
    }

    private void updateCallTypeForPsCall(int phoneId) {
        if (isWifcCalling(phoneId)) {
            setCallType(CALL_TYPE_PS_WIFI);
        } else {
            setCallType(CALL_TYPE_PS_CELLULAR);
        }
        logd("updateCallTypeForPsCall() calltype=" + getCallType());
    }

    private boolean isWifcCalling(int phoneId) {
        return mPhones[phoneId].isWifiCallingEnabled();
    }

    private void updateCallTypeForCsCall(int phoneId) {
        if (isCampOnCdma(phoneId)) {
            setCallType(CALL_TYPE_CS_CDMA);
        } else if (isCampOn3G(phoneId)) {
            setCallType(CALL_TYPE_CS_3G);
        } else {
            setCallType(CALL_TYPE_CS_2G);
        }
        logd("updateCallTypeForCsCall() calltype=" + getCallType());
    }

    public boolean isCampOnCdma(int phoneId) {
        boolean result = false;
        int nwType = getVoiceNetworkType(phoneId);
        if (ServiceState.isCdma(nwType)) {
            result = true;
        }
        logd("isCampOnCdma() getVoiceNetworkType=" + nwType + " return " + result);
        return result;
    }

    public boolean isCampOn3G(int phoneId) {
        boolean result = false;
        int nwType = getVoiceNetworkType(phoneId);
        if (!ServiceState.isLte(nwType)) {
            if (!is2gNetworkType(nwType)) {
                result = true;
            }
        }
        logd("isCampOn3G() getVoiceNetworkType=" + nwType + " return " + result);
        return result;
    }

    public boolean is2gNetworkType(int networkType){
        boolean result = false;
        if (networkType == ServiceState.RIL_RADIO_TECHNOLOGY_GSM) {
            result = true;
        }
        logd("is2gNetworkType() networkType=" + networkType + " return " + result);
        return result;
    }

    public int getVoiceNetworkType(int phoneId) {
        return mPhones[phoneId].getServiceStateTracker().mSS.getRilVoiceRadioTechnology();
    }

    public void updateDefaultDataPhoneId(int currDataSubId, String reason) {
        int newDefaultDataPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;

        if (SubscriptionManager.isValidSubscriptionId(currDataSubId)) {
            newDefaultDataPhoneId = SubscriptionManager.getPhoneId(currDataSubId);
            if (mDefaultDataPhoneId == SubscriptionManager.INVALID_PHONE_INDEX) {
                // First time receive event the mDefaultDataPhoneId is invalid
                mDefaultDataPhoneId = newDefaultDataPhoneId;
                logd("first time to update mDefaultDataPhoneId=" + mDefaultDataPhoneId
                        + " reason:" + reason);
                // check if reset settings is handle by framework when default sub changed.
                if (!isDefaultDataSelectedBeforeReboot()) {
                    resetTdsSettingsByFwk();
                }
            } else {
                // Default Data phone Id has chaged after bootup
                if (newDefaultDataPhoneId != mDefaultDataPhoneId) {
                    mDefaultDataPhoneId = newDefaultDataPhoneId;
                    logd("updateDefaultDataPhoneId() mDefaultDataPhoneId=" +
                            mDefaultDataPhoneId + " reason:" + reason);
                    // check if reset settings is handle by framework when default sub changed.
                    resetTdsSettingsByFwk();
                }
            }
            setDefaultDataSelectedProperty(1);
        } else {
            setDefaultDataSelectedProperty(0);
        }
    }


    public void onCallStarted(int phoneId) {
        if (!mState.isInCall()) {
            logd("onCallStarted: phoneId=" + phoneId);

            // register Handover event
            regImsHandoverEvent();

            setInCallPhoneId(phoneId);
            mSmartDataOpExt.onCallStarted(phoneId);
            mSmartDataOpExt.transitionTo(SwitchState.INCALL_SWITCH);
        }
    }

    public void onCallEnded(){
        if (mState.isInCall()) {
            logd("onCallEnded()");

            // register Handover event
            unregImsHandoverEvent();

            mSmartDataOpExt.onCallEnded();
            mSmartDataOpExt.transitionTo(SwitchState.IDLE);
        }
    }

    public void onSrvccStateChanged() {
        logd("onSrvccStateChanged()");
        mSmartDataOpExt.onSrvccStateChanged();
    }

    public void onServiceStateChanged(int phoneId) {
        mSmartDataOpExt.onServiceStateChanged(phoneId);
    }

    public void onHandoverToWifi() {
        logd("onHandoverToWifi()");
        mSmartDataOpExt.onHandoverToWifi();
        mSmartDataOpExt.transitionTo(SwitchState.INCALL_NOT_SWITCH);

    }

    public void onHandoverToCellular() {
        logd("onHandoverToCellular()");
        mSmartDataOpExt.onHandoverToCellular();
        mSmartDataOpExt.transitionTo(SwitchState.INCALL_SWITCH);
    }

    public void onTemporaryDataSettingsChanged() {
        logd("onTemporaryDataSettingsChanged() newSettings=" + isTemporaryDataServiceSettingOn);
        if (mState.isInCall()) {
            if (isTemporaryDataServiceSettingOn) {
                if (mState == SwitchState.INCALL_NOT_SWITCH) {
                    mSmartDataOpExt.transitionTo(SwitchState.INCALL_SWITCH);
                }
            } else {
                if (mState == SwitchState.INCALL_SWITCH) {
                    mSmartDataOpExt.transitionTo(SwitchState.INCALL_NOT_SWITCH);
                }
            }
        }
    }

    public void newNetworkRequest(int phoneId) {
        int tempDataSubId = mPhones[phoneId].getSubId();
        ConnectivityManager connectivityManager = getConnectivityManager();
        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                logd("mNetworkCallback.onAvailable");
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                logd("mNetworkCallback.onLost");
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                logd("mNetworkCallback.onUnavailable");
            }
        };

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_PREEMPT);
        builder.setNetworkSpecifier(String.valueOf(tempDataSubId));
        NetworkRequest networkRequest = builder.build();
        logd("networkRequest:" + networkRequest);
        connectivityManager.registerNetworkCallback(networkRequest, mNetworkCallback);
        connectivityManager.requestNetwork(networkRequest, mNetworkCallback);
    }

    public void releaseNetworkRequest() {
        if (mNetworkCallback != null) {
            logd("releaseNetworkRequest()");
            ConnectivityManager connectivityManager = getConnectivityManager();
            connectivityManager.unregisterNetworkCallback(mNetworkCallback);
            mNetworkCallback = null;
        } else {
            //logd("releaseNetworkRequest: mNetworkCallback is null");
        }
    }

    public void resetTdsSettingsByFwk() {
        if (isResetTdsSettingsByFwk) {
            // reset settings value of Tempoary Data Service
            Settings.Global.putInt(mResolver, TEMP_DATA_SERVICE, 0);
            logd("reset settings of Tempoary Data Service!");
        }
    }

    public boolean switchDataService(int phoneId){
        int disallowCauses = checkSwitchConditions();
        if (disallowCauses == DISALLOW_CAUSE_NONE) {
            newNetworkRequest(phoneId);
            return true;
        } else {
            logd("switchDataService: can't pass switch condictions");
            return false;
        }
    }

    private int checkSwitchConditions() {
        int disallowCauses = DISALLOW_CAUSE_NONE;

        // check switch conditions
        if (!isTemporaryDataServiceSettingOn) {
            disallowCauses |= DISALLOW_CAUSE_TEMP_DATA_SETTING_OFF;
        }

        if (!isDefaultDataPhoneIdValid()) {
            disallowCauses |= DISALLOW_CAUSE_DEFAULTE_DATA_UNSET;
        } else if (mDefaultDataPhoneId == mInCallPhoneId) {
            disallowCauses |= DISALLOW_CAUSE_DATA_SIM_INCALL;
        }

        if (!mSmartDataOpExt.isSmartDataSwtichAllowed()) {
            disallowCauses |= DISALLOW_CAUSE_OPERATOR_CHOICE;
        }

        if (!mSmartDataOpExt.isNeedSwitchCallType(getCallType())) {
            disallowCauses |= DISALLOW_CAUSE_INVAILD_CALL_TYPE;
        }

        logd("checkSwitchConditions: disallowCauses=" + disallowCauses);
        return disallowCauses;
    }

    private boolean isDefaultDataPhoneIdValid(){
        // In AOSP, getPhoneId would returns DEFAULT_PHONE_INDEX if
        // subId can not found phoneId in sub module.
        if(mDefaultDataPhoneId == SubscriptionManager.INVALID_PHONE_INDEX ||
           mDefaultDataPhoneId == SubscriptionManager.DEFAULT_PHONE_INDEX) {
            return false;
        } else {
            return true;
        }
    }

    public void setCallType(int type) {
        mCallType = type;
    }

    public int getCallType() {
        return mCallType;
    }

    public void setInCallPhoneId(int phoneId) {
        mInCallPhoneId = phoneId;
    }

    public int getInCallPhoneId() {
        return mInCallPhoneId;
    }

    public void setSwitchState(SwitchState state) {
        if (state == SwitchState.IDLE) {
            mInCallPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
            mCallType = CALL_TYPE_UNKNOW;
        }
        mState = state;
    }

    public SwitchState getSwitchState() {
        return mState;
    }

    private ConnectivityManager getConnectivityManager() {
        if (mConnectivityManager == null) {
            mConnectivityManager = (ConnectivityManager) mContext.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
        }
        return mConnectivityManager;
    }

    private TelephonyManager getTelephonyManager() {
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager) mContext.getSystemService(
                    Context.TELEPHONY_SERVICE);
        }
        return mTelephonyManager;
    }

    private IWifiOffloadService getWifiOffLoadService() {
        if (mWfoService == null) {
            IBinder binder = ServiceManager.getService(WifiOffloadManager.WFO_SERVICE);
            if (binder != null) {
                mWfoService = IWifiOffloadService.Stub.asInterface(binder);
            } else {
                binder = ServiceManager.getService(MwisConstants.MWI_SERVICE);
                try {
                    if (binder != null) {
                        mWfoService = IMwiService.Stub.asInterface(binder).getWfcHandlerInterface();
                    } else {
                        loge("getWifiOffLoadService: No MwiService exist");
                    }
                } catch (Exception e) {
                    loge("getWifiOffLoadService: can't get MwiService error:"+ e);
                }
            }

        }
        return mWfoService;
    }

    private void setDefaultDataSelectedProperty(int selected) {
        String defaultDataSelected = SystemProperties.get(PROPERTY_DEFAULT_DATA_SELECTED);
        if (!defaultDataSelected.equals(String.valueOf(selected))) {
            SystemProperties.set(PROPERTY_DEFAULT_DATA_SELECTED, String.valueOf(selected));
            logd("setDefaultDataSelectedProperty() selected=" + String.valueOf(selected));
        }
    }

    private boolean isDefaultDataSelectedBeforeReboot() {
        String defaultDataSelected = SystemProperties.get(PROPERTY_DEFAULT_DATA_SELECTED);
        logd("isDefaultDataSelectedBeforeReboot() property=" + defaultDataSelected);
        return defaultDataSelected.equals("1");
    }

    private boolean isSmartDataSwitchSupport() {
        return SystemProperties.get("persist.vendor.radio.smart.data.switch").equals("1");
    }

    protected static void logv(String s) {
        if (DBG) {
            Rlog.v(LOG_TAG, s);
        }
    }

    protected static void logd(String s) {
        if (DBG) {
            Rlog.d(LOG_TAG, s);
        }
    }

    protected static void loge(String s) {
        if (DBG) {
            Rlog.e(LOG_TAG, s);
        }
    }

    protected static void logi(String s) {
        if (DBG) {
            Rlog.i(LOG_TAG, s);
        }
    }

}
