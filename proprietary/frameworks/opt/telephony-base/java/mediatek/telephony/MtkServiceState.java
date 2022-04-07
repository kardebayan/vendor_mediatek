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

package mediatek.telephony;

import android.os.Bundle;
import android.os.Parcel;
import android.os.SystemProperties;
import android.telephony.NetworkRegistrationState;
import android.telephony.ServiceState;
import android.telephony.Rlog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Contains MTK proprietary phone state and service related information.
 *
 * The following phone information is included in returned ServiceState:
 *
 * <ul>
 *   <li>Service state: IN_SERVICE, OUT_OF_SERVICE, EMERGENCY_ONLY, POWER_OFF
 *   <li>Roaming indicator
 *   <li>Operator name, short name and numeric id
 *   <li>Network selection mode
 * </ul>
 */
public class MtkServiceState extends ServiceState {

    static final String LOG_TAG = "MTKSS";
    static final boolean DBG = false;

    // For HSPAP detail radio technology START
    /** @hide */
    public static final int RIL_RADIO_TECHNOLOGY_MTK = 128;
    /** @hide */
    public static final int RIL_RADIO_TECHNOLOGY_HSDPAP = RIL_RADIO_TECHNOLOGY_MTK + 1;
    /** @hide */
    public static final int RIL_RADIO_TECHNOLOGY_HSDPAP_UPA = RIL_RADIO_TECHNOLOGY_MTK + 2;
    /** @hide */
    public static final int RIL_RADIO_TECHNOLOGY_HSUPAP = RIL_RADIO_TECHNOLOGY_MTK + 3;
    /** @hide */
    public static final int RIL_RADIO_TECHNOLOGY_HSUPAP_DPA = RIL_RADIO_TECHNOLOGY_MTK + 4;
    /** @hide */
    public static final int RIL_RADIO_TECHNOLOGY_DC_DPA = RIL_RADIO_TECHNOLOGY_MTK + 5;
    /** @hide */
    public static final int RIL_RADIO_TECHNOLOGY_DC_UPA = RIL_RADIO_TECHNOLOGY_MTK + 6;
    /** @hide */
    public static final int RIL_RADIO_TECHNOLOGY_DC_HSDPAP = RIL_RADIO_TECHNOLOGY_MTK + 7;
    /** @hide */
    public static final int RIL_RADIO_TECHNOLOGY_DC_HSDPAP_UPA = RIL_RADIO_TECHNOLOGY_MTK + 8;
    /** @hide */
    public static final int RIL_RADIO_TECHNOLOGY_DC_HSDPAP_DPA = RIL_RADIO_TECHNOLOGY_MTK + 9;
    /** @hide */
    public static final int RIL_RADIO_TECHNOLOGY_DC_HSPAP = RIL_RADIO_TECHNOLOGY_MTK + 10;
    // For HSPAP detail radio technology END

    /**
     * MTK proprietary registration states for GSM, UMTS and CDMA.
     */
    /** @hide */
    public static final int
            REGISTRATION_STATE_NOT_REGISTERED_AND_NOT_SEARCHING_EMERGENCY_CALL_ENABLED = 10;
    /** @hide */
    public static final int REGISTRATION_STATE_NOT_REGISTERED_AND_SEARCHING_EMERGENCY_CALL_ENABLED
            = 12;
    /** @hide */
    public static final int REGISTRATION_STATE_REGISTRATION_DENIED_EMERGENCY_CALL_ENABLED = 13;
    /** @hide */
    public static final int REGISTRATION_STATE_UNKNOWN_EMERGENCY_CALL_ENABLED = 14;

    //MTK-START
    private int mRilVoiceRegState = NetworkRegistrationState.REG_STATE_NOT_REG_NOT_SEARCHING;
    private int mRilDataRegState  = NetworkRegistrationState.REG_STATE_NOT_REG_NOT_SEARCHING;
    //[ALPS01675318] -START
    private int mProprietaryDataRadioTechnology;
    //[ALPS01675318] -END
    private int mVoiceRejectCause = -1;
    private int mDataRejectCause = -1;
    //MTK-END

    // MtkServiceState keep five state: AOSP's voice/data, MTK's Iwlan/Cellular cs/ps.
    private int mIwlanRegState = STATE_OUT_OF_SERVICE;

    private int mCellularVoiceRegState = STATE_OUT_OF_SERVICE;
    private int mCellularDataRegState = STATE_OUT_OF_SERVICE;
    private int mRilCellularDataRegState =
            NetworkRegistrationState.REG_STATE_NOT_REG_NOT_SEARCHING;
    private int mCellularDataRoamingType = ROAMING_TYPE_NOT_ROAMING;
    private int mRilCellularDataRadioTechnology = 0;
    private boolean mIsUsingCellularCarrierAggregation = false;

    /**
     * Create a new ServiceState from a intent notifier Bundle
     *
     * This method is used by PhoneStateIntentReceiver and maybe by
     * external applications.
     *
     * @param m Bundle from intent notifier
     * @return newly created ServiceState
     * @hide
     */
    public static ServiceState newFromBundle(Bundle m) {
        MtkServiceState ret;
        ret = new MtkServiceState();
        ret.setFromNotifierBundle(m);
        return ret;
    }

    /**
     * Empty constructor
     */
    public MtkServiceState() {
        // initialize all mtk's variable.
        // follow AOSP, use OOS as default.
        setStateOutOfService();
    }

    /**
     * Copy constructors
     *
     * @param s Source service state
     */
    public MtkServiceState(MtkServiceState s) {
        copyFrom(s);
    }


    /**
     * Copy constructors
     *
     * This function is for create SS instance in makeServiceState() of TelephonyRegistry.
     *
     * @param s Source service state
     */
    public MtkServiceState(ServiceState s) {
        copyFrom((MtkServiceState)s);
    }

    protected void copyFrom(MtkServiceState s) {
        mVoiceRegState = s.mVoiceRegState;
        mDataRegState = s.mDataRegState;
        mVoiceRoamingType = s.mVoiceRoamingType;
        mDataRoamingType = s.mDataRoamingType;
        mVoiceOperatorAlphaLong = s.mVoiceOperatorAlphaLong;
        mVoiceOperatorAlphaShort = s.mVoiceOperatorAlphaShort;
        mVoiceOperatorNumeric = s.mVoiceOperatorNumeric;
        mDataOperatorAlphaLong = s.mDataOperatorAlphaLong;
        mDataOperatorAlphaShort = s.mDataOperatorAlphaShort;
        mDataOperatorNumeric = s.mDataOperatorNumeric;
        mIsManualNetworkSelection = s.mIsManualNetworkSelection;
        mRilVoiceRadioTechnology = s.mRilVoiceRadioTechnology;
        mRilDataRadioTechnology = s.mRilDataRadioTechnology;
        mCssIndicator = s.mCssIndicator;
        mNetworkId = s.mNetworkId;
        mSystemId = s.mSystemId;
        mCdmaRoamingIndicator = s.mCdmaRoamingIndicator;
        mCdmaDefaultRoamingIndicator = s.mCdmaDefaultRoamingIndicator;
        mCdmaEriIconIndex = s.mCdmaEriIconIndex;
        mCdmaEriIconMode = s.mCdmaEriIconMode;
        mIsEmergencyOnly = s.mIsEmergencyOnly;
        mIsDataRoamingFromRegistration = s.mIsDataRoamingFromRegistration;
        mIsUsingCarrierAggregation = s.mIsUsingCarrierAggregation;
        mChannelNumber = s.mChannelNumber;
        mCellBandwidths = Arrays.copyOf(s.mCellBandwidths, s.mCellBandwidths.length);
        mLteEarfcnRsrpBoost = s.mLteEarfcnRsrpBoost;
        mNetworkRegistrationStates = new ArrayList<>(s.mNetworkRegistrationStates);
        //MTK-START
        mRilVoiceRegState = s.mRilVoiceRegState;
        mRilDataRegState = s.mRilDataRegState;
        mProprietaryDataRadioTechnology = s.mProprietaryDataRadioTechnology;
        mVoiceRejectCause = s.mVoiceRejectCause;
        mDataRejectCause = s.mDataRejectCause;
        mIwlanRegState = s.mIwlanRegState;
        mCellularVoiceRegState = s.mCellularVoiceRegState;
        mCellularDataRegState = s.mCellularDataRegState;
        mRilCellularDataRegState = s.mRilCellularDataRegState;
        mCellularDataRoamingType = s.mCellularDataRoamingType;
        mRilCellularDataRadioTechnology = s.mRilCellularDataRadioTechnology;
        mIsUsingCellularCarrierAggregation = s.mIsUsingCellularCarrierAggregation;
        //MTK-END
    }

    /**
     * Construct a MtkServiceState object from the given parcel.
     */
    public MtkServiceState(Parcel in) {
        mVoiceRegState = in.readInt();
        mDataRegState = in.readInt();
        mVoiceRoamingType = in.readInt();
        mDataRoamingType = in.readInt();
        mVoiceOperatorAlphaLong = in.readString();
        mVoiceOperatorAlphaShort = in.readString();
        mVoiceOperatorNumeric = in.readString();
        mDataOperatorAlphaLong = in.readString();
        mDataOperatorAlphaShort = in.readString();
        mDataOperatorNumeric = in.readString();
        mIsManualNetworkSelection = in.readInt() != 0;
        mRilVoiceRadioTechnology = in.readInt();
        mRilDataRadioTechnology = in.readInt();
        mCssIndicator = (in.readInt() != 0);
        mNetworkId = in.readInt();
        mSystemId = in.readInt();
        mCdmaRoamingIndicator = in.readInt();
        mCdmaDefaultRoamingIndicator = in.readInt();
        mCdmaEriIconIndex = in.readInt();
        mCdmaEriIconMode = in.readInt();
        mIsEmergencyOnly = in.readInt() != 0;
        mIsDataRoamingFromRegistration = in.readInt() != 0;
        mIsUsingCarrierAggregation = in.readInt() != 0;
        mLteEarfcnRsrpBoost = in.readInt();
        mNetworkRegistrationStates = new ArrayList<>();
        in.readList(mNetworkRegistrationStates, NetworkRegistrationState.class.getClassLoader());
        mChannelNumber = in.readInt();
        mCellBandwidths = in.createIntArray();
        // MTK START
        mRilVoiceRegState = in.readInt();
        mRilDataRegState = in.readInt();
        mProprietaryDataRadioTechnology = in.readInt();
        mVoiceRejectCause = in.readInt();
        mDataRejectCause = in.readInt();
        mIwlanRegState = in.readInt();
        mCellularVoiceRegState = in.readInt();
        mCellularDataRegState = in.readInt();
        mRilCellularDataRegState = in.readInt();
        mCellularDataRoamingType = in.readInt();
        mRilCellularDataRadioTechnology = in.readInt();
        mIsUsingCellularCarrierAggregation = in.readInt() != 0;
        // MTK END
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mVoiceRegState);
        out.writeInt(mDataRegState);
        out.writeInt(mVoiceRoamingType);
        out.writeInt(mDataRoamingType);
        out.writeString(mVoiceOperatorAlphaLong);
        out.writeString(mVoiceOperatorAlphaShort);
        out.writeString(mVoiceOperatorNumeric);
        out.writeString(mDataOperatorAlphaLong);
        out.writeString(mDataOperatorAlphaShort);
        out.writeString(mDataOperatorNumeric);
        out.writeInt(mIsManualNetworkSelection ? 1 : 0);
        out.writeInt(mRilVoiceRadioTechnology);
        out.writeInt(mRilDataRadioTechnology);
        out.writeInt(mCssIndicator ? 1 : 0);
        out.writeInt(mNetworkId);
        out.writeInt(mSystemId);
        out.writeInt(mCdmaRoamingIndicator);
        out.writeInt(mCdmaDefaultRoamingIndicator);
        out.writeInt(mCdmaEriIconIndex);
        out.writeInt(mCdmaEriIconMode);
        out.writeInt(mIsEmergencyOnly ? 1 : 0);
        out.writeInt(mIsDataRoamingFromRegistration ? 1 : 0);
        out.writeInt(mIsUsingCarrierAggregation ? 1 : 0);
        out.writeInt(mLteEarfcnRsrpBoost);
        out.writeList(mNetworkRegistrationStates);
        out.writeInt(mChannelNumber);
        out.writeIntArray(mCellBandwidths);
        // MTK START
        out.writeInt(mRilVoiceRegState);
        out.writeInt(mRilDataRegState);
        out.writeInt(mProprietaryDataRadioTechnology);
        out.writeInt(mVoiceRejectCause);
        out.writeInt(mDataRejectCause);
        out.writeInt(mIwlanRegState);
        out.writeInt(mCellularVoiceRegState);
        out.writeInt(mCellularDataRegState);
        out.writeInt(mRilCellularDataRegState);
        out.writeInt(mCellularDataRoamingType);
        out.writeInt(mRilCellularDataRadioTechnology);
        out.writeInt(mIsUsingCellularCarrierAggregation ? 1 : 0);
        // MTK END
    }

    @Override
    public boolean equals (Object o) {
        MtkServiceState s;

        try {
            s = (MtkServiceState) o;
        } catch (ClassCastException ex) {
            return false;
        }

        if (o == null) {
            return false;
        }

        return (mVoiceRegState == s.mVoiceRegState
                && mDataRegState == s.mDataRegState
                && mIsManualNetworkSelection == s.mIsManualNetworkSelection
                && mVoiceRoamingType == s.mVoiceRoamingType
                && mDataRoamingType == s.mDataRoamingType
                && mChannelNumber == s.mChannelNumber
                && Arrays.equals(mCellBandwidths, s.mCellBandwidths)
                && equalsHandlesNulls(mVoiceOperatorAlphaLong, s.mVoiceOperatorAlphaLong)
                && equalsHandlesNulls(mVoiceOperatorAlphaShort, s.mVoiceOperatorAlphaShort)
                && equalsHandlesNulls(mVoiceOperatorNumeric, s.mVoiceOperatorNumeric)
                && equalsHandlesNulls(mDataOperatorAlphaLong, s.mDataOperatorAlphaLong)
                && equalsHandlesNulls(mDataOperatorAlphaShort, s.mDataOperatorAlphaShort)
                && equalsHandlesNulls(mDataOperatorNumeric, s.mDataOperatorNumeric)
                && equalsHandlesNulls(mRilVoiceRadioTechnology, s.mRilVoiceRadioTechnology)
                && equalsHandlesNulls(mRilDataRadioTechnology, s.mRilDataRadioTechnology)
                && equalsHandlesNulls(mCssIndicator, s.mCssIndicator)
                && equalsHandlesNulls(mNetworkId, s.mNetworkId)
                && equalsHandlesNulls(mSystemId, s.mSystemId)
                && equalsHandlesNulls(mCdmaRoamingIndicator, s.mCdmaRoamingIndicator)
                && equalsHandlesNulls(mCdmaDefaultRoamingIndicator,
                        s.mCdmaDefaultRoamingIndicator)
                && mIsEmergencyOnly == s.mIsEmergencyOnly
                && mIsDataRoamingFromRegistration == s.mIsDataRoamingFromRegistration
                && mIsUsingCarrierAggregation == s.mIsUsingCarrierAggregation
                && mNetworkRegistrationStates.containsAll(s.mNetworkRegistrationStates)
                //MTK START
                && mRilVoiceRegState == s.mRilVoiceRegState
                && mRilDataRegState == s.mRilDataRegState
                && equalsHandlesNulls(mProprietaryDataRadioTechnology,
                        s.mProprietaryDataRadioTechnology)
                && mVoiceRejectCause == s.mVoiceRejectCause
                && mDataRejectCause == s.mDataRejectCause
                && mIwlanRegState == s.mIwlanRegState
                && mCellularVoiceRegState == mCellularVoiceRegState
                && mCellularDataRegState == s.mCellularDataRegState
                && mRilCellularDataRegState == s.mRilCellularDataRegState
                && mCellularDataRoamingType == s.mCellularDataRoamingType
                && mRilCellularDataRadioTechnology == s.mRilCellularDataRadioTechnology
                && mIsUsingCellularCarrierAggregation == s.mIsUsingCellularCarrierAggregation);
                //MTK END
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{mVoiceRegState=").append(mVoiceRegState)
            .append("(" + rilServiceStateToString(mVoiceRegState) + ")")
            .append(", mDataRegState=").append(mDataRegState)
            .append("(" + rilServiceStateToString(mDataRegState) + ")")
            .append(", mChannelNumber=").append(mChannelNumber)
            .append(", duplexMode()=").append(getDuplexMode())
            .append(", mCellBandwidths=").append(Arrays.toString(mCellBandwidths))
            .append(", mVoiceRoamingType=").append(getRoamingLogString(mVoiceRoamingType))
            .append(", mDataRoamingType=").append(getRoamingLogString(mDataRoamingType))
            .append(", mVoiceOperatorAlphaLong=").append(mVoiceOperatorAlphaLong)
            .append(", mVoiceOperatorAlphaShort=").append(mVoiceOperatorAlphaShort)
            .append(", mDataOperatorAlphaLong=").append(mDataOperatorAlphaLong)
            .append(", mDataOperatorAlphaShort=").append(mDataOperatorAlphaShort)
            .append(", isManualNetworkSelection=").append(mIsManualNetworkSelection)
            .append(mIsManualNetworkSelection ? "(manual)" : "(automatic)")
            .append(", mRilVoiceRadioTechnology=").append(mRilVoiceRadioTechnology)
            .append("(" + rilRadioTechnologyToString(mRilVoiceRadioTechnology) + ")")
            .append(", mRilDataRadioTechnology=").append(mRilDataRadioTechnology)
            .append("(" + rilRadioTechnologyToString(mRilDataRadioTechnology) + ")")
            .append(", mCssIndicator=").append(mCssIndicator ? "supported" : "unsupported")
            .append(", mNetworkId=").append(mNetworkId)
            .append(", mSystemId=").append(mSystemId)
            .append(", mCdmaRoamingIndicator=").append(mCdmaRoamingIndicator)
            .append(", mCdmaDefaultRoamingIndicator=").append(mCdmaDefaultRoamingIndicator)
            .append(", mIsEmergencyOnly=").append(mIsEmergencyOnly)
            .append(", mIsDataRoamingFromRegistration=").append(mIsDataRoamingFromRegistration)
            .append(", mIsUsingCarrierAggregation=").append(mIsUsingCarrierAggregation)
            .append(", mLteEarfcnRsrpBoost=").append(mLteEarfcnRsrpBoost)
            .append(", mNetworkRegistrationStates=").append(mNetworkRegistrationStates)
            //MTK START
            .append(", Ril Voice Regist state=").append(mRilVoiceRegState)
            .append(", Ril Data Regist state=").append(mRilDataRegState)
            .append(", mProprietaryDataRadioTechnology=").append(mProprietaryDataRadioTechnology)
            .append(", VoiceRejectCause=").append(mVoiceRejectCause)
            .append(", DataRejectCause=").append(mDataRejectCause)
            .append(", IwlanRegState=").append(mIwlanRegState)
            .append(", CellularVoiceRegState=").append(mCellularVoiceRegState)
            .append(", CellularDataRegState=").append(mCellularDataRegState)
            .append(", RilCellularDataRegState=").append(mRilCellularDataRegState)
            .append(", CellularDataRoamingType=").append(mCellularDataRoamingType)
            .append(", RilCellularDataRadioTechnology=").append(mRilCellularDataRadioTechnology)
            .append(", IsUsingCellularCarrierAggregation=")
                    .append(mIsUsingCellularCarrierAggregation)
            //MTK END
            .append("}").toString();
    }

    @Override
    protected void setNullState(int state) {
        if (DBG) Rlog.d(LOG_TAG, "[MtkServiceState] setNullState=" + state);
        mVoiceRegState = state;
        mDataRegState = state;
        mVoiceRoamingType = ROAMING_TYPE_NOT_ROAMING;
        mDataRoamingType = ROAMING_TYPE_NOT_ROAMING;
        mChannelNumber = -1;
        mCellBandwidths = new int[0];
        mVoiceOperatorAlphaLong = null;
        mVoiceOperatorAlphaShort = null;
        mVoiceOperatorNumeric = null;
        mDataOperatorAlphaLong = null;
        mDataOperatorAlphaShort = null;
        mDataOperatorNumeric = null;
        mIsManualNetworkSelection = false;
        mRilVoiceRadioTechnology = 0;
        mRilDataRadioTechnology = 0;
        mCssIndicator = false;
        mNetworkId = -1;
        mSystemId = -1;
        mCdmaRoamingIndicator = -1;
        mCdmaDefaultRoamingIndicator = -1;
        mCdmaEriIconIndex = -1;
        mCdmaEriIconMode = -1;
        mIsEmergencyOnly = false;
        mIsDataRoamingFromRegistration = false;
        mIsUsingCarrierAggregation = false;
        mLteEarfcnRsrpBoost = 0;
        mNetworkRegistrationStates = new ArrayList<>();
        //MTK-START
        mRilVoiceRegState = NetworkRegistrationState.REG_STATE_NOT_REG_NOT_SEARCHING;
        mRilDataRegState  = NetworkRegistrationState.REG_STATE_NOT_REG_NOT_SEARCHING;
        mProprietaryDataRadioTechnology = 0;
        mVoiceRejectCause = -1;
        mDataRejectCause = -1;
        mIwlanRegState = STATE_OUT_OF_SERVICE;
        mCellularVoiceRegState = state;
        mCellularDataRegState = state;
        mRilCellularDataRegState = NetworkRegistrationState.REG_STATE_NOT_REG_NOT_SEARCHING;
        mCellularDataRoamingType = ROAMING_TYPE_NOT_ROAMING;
        mRilCellularDataRadioTechnology = 0;
        mIsUsingCellularCarrierAggregation = false;
        //MTK-END
    }

    /**
     * Set MtkServiceState based on intent notifier map.
     *
     * @param m intent notifier map
     * @hide
     */
    @Override
    protected void setFromNotifierBundle(Bundle m) {
        mVoiceRegState = m.getInt("voiceRegState");
        mDataRegState = m.getInt("dataRegState");
        mVoiceRoamingType = m.getInt("voiceRoamingType");
        mDataRoamingType = m.getInt("dataRoamingType");
        mVoiceOperatorAlphaLong = m.getString("operator-alpha-long");
        mVoiceOperatorAlphaShort = m.getString("operator-alpha-short");
        mVoiceOperatorNumeric = m.getString("operator-numeric");
        mDataOperatorAlphaLong = m.getString("data-operator-alpha-long");
        mDataOperatorAlphaShort = m.getString("data-operator-alpha-short");
        mDataOperatorNumeric = m.getString("data-operator-numeric");
        mIsManualNetworkSelection = m.getBoolean("manual");
        mRilVoiceRadioTechnology = m.getInt("radioTechnology");
        mRilDataRadioTechnology = m.getInt("dataRadioTechnology");
        mCssIndicator = m.getBoolean("cssIndicator");
        mNetworkId = m.getInt("networkId");
        mSystemId = m.getInt("systemId");
        mCdmaRoamingIndicator = m.getInt("cdmaRoamingIndicator");
        mCdmaDefaultRoamingIndicator = m.getInt("cdmaDefaultRoamingIndicator");
        mIsEmergencyOnly = m.getBoolean("emergencyOnly");
        mIsDataRoamingFromRegistration = m.getBoolean("isDataRoamingFromRegistration");
        mIsUsingCarrierAggregation = m.getBoolean("isUsingCarrierAggregation");
        mLteEarfcnRsrpBoost = m.getInt("LteEarfcnRsrpBoost");
        mChannelNumber = m.getInt("ChannelNumber");
        mCellBandwidths = m.getIntArray("CellBandwidths");
        // MTK START
        mRilVoiceRegState = m.getInt("RilVoiceRegState");
        mRilDataRegState = m.getInt("RilDataRegState");
        mProprietaryDataRadioTechnology = m.getInt("proprietaryDataRadioTechnology");
        mVoiceRejectCause= m.getInt("VoiceRejectCause");
        mDataRejectCause= m.getInt("DataRejectCause");
        mIwlanRegState = m.getInt("IwlanRegState");
        mCellularVoiceRegState = m.getInt("CellularVoiceRegState");
        mCellularDataRegState = m.getInt("CellularDataRegState");
        mRilCellularDataRegState = m.getInt("RilCellularDataRegState");
        mCellularDataRoamingType = m.getInt("CellularDataRoamingType");
        mRilCellularDataRadioTechnology = m.getInt("RilCellularDataRadioTechnology");
        mIsUsingCellularCarrierAggregation = m.getBoolean("IsUsingCellularCarrierAggregation");
        // MTK END
    }

    /**
     * Set intent notifier Bundle based on service state.
     *
     * @param m intent notifier Bundle
     * @hide
     */
    @Override
    public void fillInNotifierBundle(Bundle m) {
        m.putInt("voiceRegState", mVoiceRegState);
        m.putInt("dataRegState", mDataRegState);
        m.putInt("voiceRoamingType", mVoiceRoamingType);
        m.putInt("dataRoamingType", mDataRoamingType);
        m.putString("operator-alpha-long", mVoiceOperatorAlphaLong);
        m.putString("operator-alpha-short", mVoiceOperatorAlphaShort);
        m.putString("operator-numeric", mVoiceOperatorNumeric);
        m.putString("data-operator-alpha-long", mDataOperatorAlphaLong);
        m.putString("data-operator-alpha-short", mDataOperatorAlphaShort);
        m.putString("data-operator-numeric", mDataOperatorNumeric);
        m.putBoolean("manual", mIsManualNetworkSelection);
        m.putInt("radioTechnology", mRilVoiceRadioTechnology);
        m.putInt("dataRadioTechnology", mRilDataRadioTechnology);
        m.putBoolean("cssIndicator", mCssIndicator);
        m.putInt("networkId", mNetworkId);
        m.putInt("systemId", mSystemId);
        m.putInt("cdmaRoamingIndicator", mCdmaRoamingIndicator);
        m.putInt("cdmaDefaultRoamingIndicator", mCdmaDefaultRoamingIndicator);
        m.putBoolean("emergencyOnly", mIsEmergencyOnly);
        m.putBoolean("isDataRoamingFromRegistration", mIsDataRoamingFromRegistration);
        m.putBoolean("isUsingCarrierAggregation", mIsUsingCarrierAggregation);
        m.putInt("LteEarfcnRsrpBoost", mLteEarfcnRsrpBoost);
        m.putInt("ChannelNumber", mChannelNumber);
        m.putIntArray("CellBandwidths", mCellBandwidths);
        //MTK-START
        m.putInt("RilVoiceRegState", mRilVoiceRegState);
        m.putInt("RilDataRegState", mRilDataRegState);
        m.putInt("proprietaryDataRadioTechnology", mProprietaryDataRadioTechnology);
        m.putInt("VoiceRejectCause", mVoiceRejectCause);
        m.putInt("DataRejectCause", mDataRejectCause);
        m.putInt("IwlanRegState", mIwlanRegState);
        m.putInt("CellularVoiceRegState", mCellularVoiceRegState);
        m.putInt("CellularDataRegState", mCellularDataRegState);
        m.putInt("RilCellularDataRegState", mRilCellularDataRegState);
        m.putInt("CellularDataRoamingType", mCellularDataRoamingType);
        m.putInt("RilCellularDataRadioTechnology", mRilCellularDataRadioTechnology);
        m.putBoolean("IsUsingCellularCarrierAggregation", mIsUsingCellularCarrierAggregation);
        //MTK-END
    }

    /**
     * Returns a merged ServiceState consisting of the base SS with voice settings from the
     * voice SS. The voice SS is only used if it is IN_SERVICE (otherwise the base SS is returned).
     * @hide
     * */
    public static MtkServiceState mergeMtkServiceStates(MtkServiceState baseSs,
            MtkServiceState voiceSs) {
        if (voiceSs.mVoiceRegState != STATE_IN_SERVICE) {
            return baseSs;
        }

        MtkServiceState newSs = new MtkServiceState(baseSs);

        // voice overrides
        newSs.mVoiceRegState = voiceSs.mVoiceRegState;
        newSs.mIsEmergencyOnly = false; // only get here if voice is IN_SERVICE

        return newSs;
    }

    // M: MTK Added methods START

    /**
     * Get current voice network registration reject cause.
     * See 3GPP TS 24.008,section 10.5.3.6 and Annex G.
     * @return registration reject cause or INVALID value (-1)
     * @hide
     */
    public int getVoiceRejectCause() {
        return mVoiceRejectCause;
    }

    /**
     * Get current data network registration reject cause.
     * See 3GPP TS 24.008 Annex G.6 "Additional cause codes for GMM".
     * @return registration reject cause or INVALID value (-1)
     * @hide
     */
    public int getDataRejectCause() {
        return mDataRejectCause;
    }

    /** @hide */
    public void setVoiceRejectCause(int cause) {
        mVoiceRejectCause = cause;
    }

    /** @hide */
    public void setDataRejectCause(int cause) {
        mDataRejectCause = cause;
    }

    // [ALPS01675318] -START
    /** @hide */
    public int getProprietaryDataRadioTechnology() {
        return this.mProprietaryDataRadioTechnology;
    }

    /** @hide */
    public void setProprietaryDataRadioTechnology(int rt) {
        if (DBG) Rlog.d(LOG_TAG, "[MtkServiceState] setProprietaryDataRadioTechnology = " + rt);
        mProprietaryDataRadioTechnology = rt;
    }
    //[ALPS01675318] -END

    /**
     * @hide
     */
    public int rilRadioTechnologyToNetworkTypeEx(int rt) {
        return rilRadioTechnologyToNetworkType(rt);
    }

    /** @hide */
    public int getRilVoiceRegState() {
        return mRilVoiceRegState;
    }

    /** @hide */
    public int getRilDataRegState() {
        return mRilDataRegState;
    }

    /**
     * @hide
     */
    public void setRilVoiceRegState(int nRegState) {
        mRilVoiceRegState = nRegState;
    }

    /**
     * @hide
     */
    public void setRilDataRegState(int nDataRegState) {
        mRilDataRegState = nDataRegState;
    }

    /**
     * @hide
     */
    public boolean isVoiceRadioTechnologyHigher(int nRadioTechnology) {
        return compareTwoRadioTechnology(mRilVoiceRadioTechnology, nRadioTechnology);
    }

    /**
     * @hide
     */
    public boolean isDataRadioTechnologyHigher(int nRadioTechnology) {
        return compareTwoRadioTechnology(mRilDataRadioTechnology, nRadioTechnology);
    }

    /**
     * @hide
     */
    public boolean compareTwoRadioTechnology(int nRadioTechnology1, int nRadioTechnology2) {
        if (nRadioTechnology1 == nRadioTechnology2) {
            return false;
        } else if (nRadioTechnology1 == RIL_RADIO_TECHNOLOGY_LTE) {
            return true;
        } else if (nRadioTechnology2 == RIL_RADIO_TECHNOLOGY_LTE) {
            return false;
        } else if (nRadioTechnology1 == RIL_RADIO_TECHNOLOGY_GSM) {
            // ALPS02230032-START
            if (nRadioTechnology2 == RIL_RADIO_TECHNOLOGY_UNKNOWN) {
                return true;
            }
            // ALPS00230032-END
            return false;
        } else if (nRadioTechnology2 == RIL_RADIO_TECHNOLOGY_GSM) {
            // ALPS02230032-START
            if (nRadioTechnology1 == RIL_RADIO_TECHNOLOGY_UNKNOWN) {
                return false;
            }
            // ALPS00230032-END
            return true;
        } else if (nRadioTechnology1 > nRadioTechnology2) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get current Cellular data network roaming type.
     * @return roaming type
     * @hide
     */
    public boolean getCellularDataRoaming() {
        return mCellularDataRoamingType != ROAMING_TYPE_NOT_ROAMING;
    }

    /**
     * Get cellur data network type.
     * @return Cellur data network type
     * @hide
     */
    public int getCellularDataNetworkType() {
        return rilRadioTechnologyToNetworkType(mRilCellularDataRadioTechnology);
    }

    /**
     * Get current Cellular service state.
     *
     * @see #STATE_IN_SERVICE
     * @see #STATE_OUT_OF_SERVICE
     * @see #STATE_EMERGENCY_ONLY
     * @see #STATE_POWER_OFF
     * @return if voice and data is in service, return in service.
     * @hide
     */
    public int getCellularRegState() {
        return (mCellularDataRegState == STATE_IN_SERVICE) ?
                mCellularDataRegState :
                mCellularVoiceRegState;
    }

    /**
     * Get current Cellular voice service state.
     *
     * @see #STATE_IN_SERVICE
     * @see #STATE_OUT_OF_SERVICE
     * @see #STATE_EMERGENCY_ONLY
     * @see #STATE_POWER_OFF
     * @return cellular voice reg state
     * @hide
     */
    public int getCellularVoiceRegState() {
        return mCellularVoiceRegState;
    }


    /**
     * Get current Cellular data service state.
     *
     * @see #STATE_IN_SERVICE
     * @see #STATE_OUT_OF_SERVICE
     * @see #STATE_EMERGENCY_ONLY
     * @see #STATE_POWER_OFF
     * @return cellular data reg state
     * @hide
     */
    public int getCellularDataRegState() {
        return mCellularDataRegState;
    }

    public int getRilCellularDataRegState() {
        return mRilCellularDataRegState;
    }

    /**
     * Get current Cellular data network roaming type.
     * @return roaming type
     * @hide
     */
    public int getCellularDataRoamingType() {
        return mCellularDataRoamingType;
    }

    /**
     * @return check cellular data CA state
     * @hide
     */
    public boolean isUsingCellularCarrierAggregation() {
        return mIsUsingCellularCarrierAggregation;
    }

    /**
     * @param state should be one of these
     * @see #STATE_IN_SERVICE
     * @see #STATE_OUT_OF_SERVICE
     * @see #STATE_EMERGENCY_ONLY
     * @see #STATE_POWER_OFF
     * @hide
     */
    public void setIwlanRegState(int state) {
        mIwlanRegState = state;
    }

    /**
     * @see #STATE_IN_SERVICE
     * @see #STATE_OUT_OF_SERVICE
     * @see #STATE_EMERGENCY_ONLY
     * @see #STATE_POWER_OFF
     * @return iwlan state
     * @hide
     */
    public int getIwlanRegState() {
        return mIwlanRegState;
    }

    /**
     * Currently Android P keeps two state
     * 1. Cellular voice
     * 2. Cellular data & iwlan
     * But MTK solution keeps three state
     * 1. Cellular voice
     * 2. Cellular data
     * 3. IWLAN
     * For upper layer, we need to work as AOSP
     * @hide
     */
    public void mergeIwlanServiceState() {
        // backup Cellular service state
        mCellularVoiceRegState = mVoiceRegState;
        mCellularDataRegState = mDataRegState;
        // Following value will be different when IWLAN is enabled.
        mRilCellularDataRegState = mRilDataRegState;
        mCellularDataRoamingType = mDataRoamingType;
        mRilCellularDataRadioTechnology = mRilDataRadioTechnology;
        mIsUsingCellularCarrierAggregation = mIsUsingCarrierAggregation;

        boolean is_93_md =
            "c6m_1rild".equals(SystemProperties.get("ro.vendor.mtk_ril_mode")) ? true : false;
        // For newer platform, if we have IWLAN, we override the MD's data state.
        // For legacy platform, we use IWLAN only when MD's data is out of service.
        if ((is_93_md && (getIwlanRegState() == STATE_IN_SERVICE)) ||
            (!is_93_md && (getIwlanRegState() == STATE_IN_SERVICE)
                    && (getDataRegState() != STATE_IN_SERVICE))) {
            mDataRegState = STATE_IN_SERVICE;
            mRilDataRegState = NetworkRegistrationState.REG_STATE_HOME;
            mDataRoamingType = ROAMING_TYPE_NOT_ROAMING;
            mRilDataRadioTechnology = RIL_RADIO_TECHNOLOGY_IWLAN;
            mIsUsingCarrierAggregation = false;
        }
    }
    // M: MTK Added methods END
}
