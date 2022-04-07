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

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.android.i18n.phonenumbers.ShortNumberInfo;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Country;
import android.location.CountryDetector;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.SparseIntArray;

import static com.android.internal.telephony.TelephonyProperties.PROPERTY_OPERATOR_IDP_STRING;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.internal.telephony.cdma.pluscode.IPlusCodeUtils;
import com.mediatek.internal.telephony.cdma.pluscode.PlusCodeProcessor;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.telephony.MtkTelephonyManagerEx;

import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

/**
 * Various utilities for dealing with MTK phone number strings.
 */
public class MtkPhoneNumberUtils {
    static final String LOG_TAG = "MtkPhoneNumberUtils";
    private static final boolean VDBG = false;
    private static final int MAX_SIM_NUM = 4;
    private static final int MIN_MATCH = 7;
    private static final int MIN_MATCH_CTA = 11;
    private static final int MAX_ECC_NUM_TO_MD_TOTAL = 15;
    private static final int MAX_ECC_NUM_TO_MD_PER_AT = 10;
    private static final String ICCID_CN_PREFIX = "8986";
    private static int sSpecificEccCat = -1;

    private static final String[] SIM_RECORDS_PROPERTY_ECC_LIST = {
        "vendor.ril.ecclist",
        "vendor.ril.ecclist1",
        "vendor.ril.ecclist2",
        "vendor.ril.ecclist3",
    };

    private static final String[] CDMA_SIM_RECORDS_PROPERTY_ECC_LIST = {
        "vendor.ril.cdma.ecclist",
        "vendor.ril.cdma.ecclist1",
        "vendor.ril.cdma.ecclist2",
        "vendor.ril.cdma.ecclist3",
    };

    private static final String[] NETWORK_ECC_LIST = {
        "vendor.ril.ecc.service.category.list",
        "vendor.ril.ecc.service.category.list.1",
        "vendor.ril.ecc.service.category.list.2",
        "vendor.ril.ecc.service.category.list.3",
    };

    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE  = {
        "vendor.gsm.ril.fulluicctype",
        "vendor.gsm.ril.fulluicctype.2",
        "vendor.gsm.ril.fulluicctype.3",
        "vendor.gsm.ril.fulluicctype.4",
    };

    private static final String[] UICC_PLMN_PROPERTY = {
        "vendor.gsm.ril.uicc.mccmnc",
        "vendor.gsm.ril.uicc.mccmnc.1",
        "vendor.gsm.ril.uicc.mccmnc.2",
        "vendor.gsm.ril.uicc.mccmnc.3",
    };

    private static IPlusCodeUtils sPlusCodeUtils = null;

    private static boolean sIsCtaSupport = false;
    private static boolean sIsCtaSet = false;
    private static boolean sIsC2kSupport = false;
    private static boolean sIsOP09Support = false;
    private static boolean sIsCdmaLessSupport = false;

    private static EccSource sXmlEcc = null;
    private static EccSource sCtaEcc = null;
    private static EccSource sNetworkEcc = null;
    private static EccSource sSimEcc = null;
    private static EccSource sPropertyEcc = null;
    private static EccSource sTestEcc = null;
    private static ArrayList<EccSource> sAllEccSource = null;

    // AOSP adapter @{
    /*
     * Special characters
     *
     * (See "What is a phone number?" doc)
     * 'p' --- GSM pause character, same as comma
     * 'n' --- GSM wild character
     * 'w' --- GSM wait character
     */
    public static final char PAUSE = ',';
    public static final char WAIT = ';';
    public static final char WILD = 'N';

    /*
     * Calling Line Identification Restriction (CLIR)
     */
    private static final String CLIR_ON = "*31#";
    private static final String CLIR_OFF = "#31#";

    /*
     * TOA = TON + NPI
     * See TS 24.008 section 10.5.4.7 for details.
     * These are the only really useful TOA values
     */
    public static final int TOA_International = 0x91;
    public static final int TOA_Unknown = 0x81;

    /** The current locale is unknown, look for a country code or don't format */
    public static final int FORMAT_UNKNOWN = 0;
    /** NANP formatting */
    public static final int FORMAT_NANP = 1;
    /** Japanese formatting */
    public static final int FORMAT_JAPAN = 2;

    /** List of country codes for countries that use the NANP */
    private static final String[] NANP_COUNTRIES = new String[] {
        "US", // United States
        "CA", // Canada
        "AS", // American Samoa
        "AI", // Anguilla
        "AG", // Antigua and Barbuda
        "BS", // Bahamas
        "BB", // Barbados
        "BM", // Bermuda
        "VG", // British Virgin Islands
        "KY", // Cayman Islands
        "DM", // Dominica
        "DO", // Dominican Republic
        "GD", // Grenada
        "GU", // Guam
        "JM", // Jamaica
        "PR", // Puerto Rico
        "MS", // Montserrat
        "MP", // Northern Mariana Islands
        "KN", // Saint Kitts and Nevis
        "LC", // Saint Lucia
        "VC", // Saint Vincent and the Grenadines
        "TT", // Trinidad and Tobago
        "TC", // Turks and Caicos Islands
        "VI", // U.S. Virgin Islands
    };

    private static final char PLUS_SIGN_CHAR = '+';
    /**
     * @hide
     */
    private static final String PLUS_SIGN_STRING = "+";

    // Countries not use google libphonenumber ecc lib which conflict with
    // operator requirement.
    private static final String[] COUNTRIES_NOT_USE_ECC_LIB = new String[] {
        "AU", // Australia
        "BD", // Bangladesh
        "CO", // Colombia
        "FR", // France
        "NG", // Nigeria
        "SI", // Slovenia
        "TW", // Taiwan
        "US", // United States
    };

    private static final String[] PLMN_NO_C2K = {
        "206", // Belgium
        "208", // France
        "214", // Spain
        "216", // Hungary
        "219", // Croatia
        "222", // Italy
        "226", // Romania
        "228", // Switzerland
        "230", // Czech
        "231", // Slovakia
        "232", // Austria
        "234", // United Kingdom
        "238", // Denmark
        "240", // Sweden
        "242", // Norway
        "244", // Finland
        "248", // Estonia
        "260", // Poland
        "262", // Germany
        "268", // Portugal
        "272", // Ireland
        "284", // Bulgaria
        "286", // Turkey
        "293", // Slovenia
        "295", // Lichtenstein
        "44010", // Japan DoCoMo
    };

    public static byte[] numberToCalledPartyBCD(String number) {
        return PhoneNumberUtils.numberToCalledPartyBCD(number);
    }

    public static String calledPartyBCDFragmentToString(byte[] bytes, int offset, int length) {
        return PhoneNumberUtils.calledPartyBCDFragmentToString(bytes, offset, length);
    }

    public static String calledPartyBCDToString(byte[] bytes, int offset, int length) {
        return PhoneNumberUtils.calledPartyBCDToString(bytes, offset, length);
    }

    public static String stripSeparators(String phoneNumber) {
        return PhoneNumberUtils.stripSeparators(phoneNumber);
    }

    public static String extractNetworkPortion(String phoneNumber) {
        return PhoneNumberUtils.extractNetworkPortion(phoneNumber);
    }

    public static String stringFromStringAndTOA(String s, int TOA) {
        return PhoneNumberUtils.stringFromStringAndTOA(s, TOA);
    }

    public static String convertPreDial(String phoneNumber) {
        return PhoneNumberUtils.convertPreDial(phoneNumber);
    }

    public static boolean isNonSeparator(String address) {
        for (int i = 0, count = address.length(); i < count; i++) {
            if (!PhoneNumberUtils.isNonSeparator(address.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static int getFormatTypeFromCountryCode(String country) {
        // Check for the NANP countries
        int length = NANP_COUNTRIES.length;
        for (int i = 0; i < length; i++) {
            if (NANP_COUNTRIES[i].compareToIgnoreCase(country) == 0) {
                return FORMAT_NANP;
            }
        }
        if ("jp".compareToIgnoreCase(country) == 0) {
            return FORMAT_JAPAN;
        }
        return FORMAT_UNKNOWN;
    }

    private static int findDialableIndexFromPostDialStr(String postDialStr) {
        for (int index = 0;index < postDialStr.length();index++) {
             char c = postDialStr.charAt(index);
             if (PhoneNumberUtils.isReallyDialable(c)) {
                return index;
             }
        }
        return -1;
    }

    private static String appendPwCharBackToOrigDialStr(int dialableIndex,
            String origStr, String dialStr) {
        String retStr;

        // There is only 1 P/W character before the dialable characters
        if (dialableIndex == 1) {
            StringBuilder ret = new StringBuilder(origStr);
            ret = ret.append(dialStr.charAt(0));
            retStr = ret.toString();
        } else {
            // It means more than 1 P/W characters in the post dial string,
            // appends to retStr
            String nonDigitStr = dialStr.substring(0,dialableIndex);
            retStr = origStr.concat(nonDigitStr);
        }
        return retStr;
    }

    public static boolean isEmergencyNumber(String number) {
        return PhoneNumberUtils.isEmergencyNumber(number);
    }

    public static boolean isEmergencyNumber(int subId, String number) {
        return PhoneNumberUtils.isEmergencyNumber(subId, number);
    }
    // AOSP function adapter @}

    /** @hide */
    public static class EccEntry {
        public static final String ECC_LIST_PATH = "/system/vendor/etc/";
        public static final String ECC_LIST = "ecc_list.xml";
        public static final String CDMA_ECC_LIST = "cdma_ecc_list.xml";
        public static final String CDMA_SS_ECC_LIST = "cdma_ecc_list_ss.xml";
        public static final String ECC_ENTRY_TAG = "EccEntry";
        public static final String ECC_ATTR = "Ecc";
        public static final String CATEGORY_ATTR = "Category";
        public static final String CONDITION_ATTR = "Condition";
        public static final String PLMN_ATTR = "Plmn";

        public static final String PROPERTY_PREFIX = "ro.vendor.semc.ecclist.";
        public static final String PROPERTY_COUNT = PROPERTY_PREFIX + "num";
        public static final String PROPERTY_NUMBER = PROPERTY_PREFIX + "number.";
        public static final String PROPERTY_TYPE = PROPERTY_PREFIX + "type.";
        public static final String PROPERTY_PLMN = PROPERTY_PREFIX + "plmn.";
        public static final String PROPERTY_NON_ECC = PROPERTY_PREFIX + "non_ecc.";

        public static final String[] PROPERTY_TYPE_KEY =
                {"police", "ambulance", "firebrigade", "marineguard", "mountainrescue"};
        public static final Short[] PROPERTY_TYPE_VALUE = {0x0001, 0x0002, 0x0004, 0x0008, 0x0010};

        public static final String ECC_NO_SIM = "0";
        public static final String ECC_ALWAYS = "1";
        public static final String ECC_FOR_MMI = "2";

        private String mEcc;
        private String mCategory;
        private String mCondition; // ECC_NO_SIM, ECC_ALWAYS, or ECC_FOR_MMI
        private String mPlmn;
        private String mName;

        public EccEntry() {
            mEcc = new String("");
            mCategory = new String("");
            mCondition = new String("");
            mPlmn = new String("");
        }
        public EccEntry(String name, String number) {
            mName = name;
            mEcc = number;
        }
        public void setName(String name) {
            mName = name;
        }
        public String getName() {
            return mName;
        }
        public void setEcc(String strEcc) {
            mEcc = strEcc;
        }
        public void setCategory(String strCategory) {
            mCategory = strCategory;
        }
        public void setCondition(String strCondition) {
            mCondition = strCondition;
        }
        public void setPlmn(String strPlmn) {
            mPlmn = strPlmn;
        }

        public String getEcc() {
            return mEcc;
        }
        public String getCategory() {
            return mCategory;
        }
        public String getCondition() {
            return mCondition;
        }
        public String getPlmn() {
            return mPlmn;
        }

        @Override
        public String toString() {
            return ("\n" + ECC_ATTR + "=" + getEcc() + ", " + CATEGORY_ATTR + "="
                    + getCategory() + ", " + CONDITION_ATTR + "=" + getCondition()
                    + ", " + PLMN_ATTR + "=" + getPlmn()
                    + ", name=" + getName());
        }
    }

    /** @hide */
    private static class EccSource {
        private int mPhoneType = 0;
        protected ArrayList<EccEntry> mEccList = null;
        protected ArrayList<EccEntry> mCdmaEccList = null;

        public EccSource(int phoneType) {
            mPhoneType = phoneType;
            parseEccList();
        }

        public boolean isEmergencyNumber(String number, int subId, int phoneType) {
            return false;
        }

        public boolean isMatch(String strEcc, String number) {
            String numberPlus = strEcc + '+';
            // VZW CDMA Less requirement: The device shall treat the dial string *272911
            // as a 911 call. Refer to Verizon wireless E911 for LTE only or LTE multi-mode
            // VoLTE capable devices requirement for details.
            if (sIsCdmaLessSupport) {
                String vzwEcc = "*272" + strEcc;
                if (strEcc.equals(number) || numberPlus.equals(number) || vzwEcc.equals(number)) {
                    return true;
                }
            } else {
                if (strEcc.equals(number) || numberPlus.equals(number)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isMatch(String strEcc, String number, String plmn) {
            if (isMatch(strEcc, number) && isEccPlmnMatch(plmn)) {
                return true;
            }
            return false;
        }

        public synchronized int getServiceCategory(String number, int subId) {
            if (mEccList != null) {
                for (EccEntry eccEntry : mEccList) {
                    String ecc = eccEntry.getEcc();
                    if (isMatch(ecc, number)) {
                        log("[getServiceCategory] match customized, ECC: "
                                + ecc + ", Category= " + eccEntry.getCategory());
                        return Integer.parseInt(eccEntry.getCategory());
                    }
                }
            }
            return -1;
        }

        public synchronized void addToEccList(ArrayList<EccEntry> eccList) {
            if (mEccList != null && eccList != null) {
                for (EccEntry srcEntry : mEccList) {
                    // sync non PLMN specify Ecc list to modem
                    if (TextUtils.isEmpty(srcEntry.getPlmn())) {
                        boolean bFound = false;
                        int nIndex = 0;
                        for (EccEntry destEntry : eccList) {
                            if (srcEntry.getEcc().equals(destEntry.getEcc())) {
                                bFound = true;
                                break;
                            }
                            nIndex++;
                        }

                        if (bFound) {
                            // Don't overide the setting because we add the ecc
                            // according to priority
                            // eccList.set(nIndex, srcEntry);
                        } else {
                            eccList.add(srcEntry);
                        }
                    }
                }
            }
        }

        public synchronized void parseEccList() {
        }

        public synchronized boolean isSpecialEmergencyNumber(String number) {
            return isSpecialEmergencyNumber(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID, number);
        }

        public synchronized boolean isSpecialEmergencyNumber(int subId, String number) {
            if (mEccList != null) {
                // 93MD revise for ECC list control.
                // in 93MD, MD will not maintain ECC list and all ECC are decided by AP
                // To fullfill CTA requirement, we need to dial CTA number using:
                // 1. Normal call when SIM inserted with GSM phone type (return true) and SIM ready
                //    (Not SIM locked or network locked)
                // 2. ECC when No SIM or CDMA phone. (return false)
                boolean eccApCtrl = SystemProperties.get(
                        "ro.vendor.mtk_ril_mode").equals("c6m_1rild");
                boolean isGsmPhone = TelephonyManager.getDefault().getCurrentPhoneType(subId) ==
                        PhoneConstants.PHONE_TYPE_GSM;
                boolean isGsmSimInserted = isSimInsert(subId, PhoneConstants.PHONE_TYPE_GSM);
                // In CT Volte (LTE only) mode (GSM phone), should always dial using ATDE
                boolean isCt4G = isCt4GDualModeCard(subId);
                boolean isNeedCheckSpecial =
                        // 90/91/92MD for GSM (CDMA should always return false)
                        (!eccApCtrl && isGsmPhone && !isCt4G) ||
                        // After 93MD
                        (isGsmPhone && isGsmSimInserted && !isCt4G && isSimReady(subId));
                dlog("[isSpecialEmergencyNumber] subId: " + subId
                        + ", eccApCtrl: " + eccApCtrl
                        + ", isGsmPhone: " + isGsmPhone
                        + ", isGsmSimInserted: " + isGsmSimInserted
                        + ", isCt4G: " + isCt4G
                        + ", isNeedCheckSpecial: " + isNeedCheckSpecial);
                if (isNeedCheckSpecial) {
                    for (EccEntry eccEntry : mEccList) {
                        if (eccEntry.getCondition().equals(EccEntry.ECC_FOR_MMI)) {
                            if (isMatch(eccEntry.getEcc(), number, eccEntry.getPlmn())) {
                                dlog("[isSpecialEmergencyNumber] match customized ecc");
                                return true;
                            }
                        }
                    }
                }
            }
            dlog("[isSpecialEmergencyNumber] return false");
            return false;
        }

        public boolean isPhoneTypeSupport(int phoneType) {
            return (mPhoneType & phoneType) == 0 ? false : true;
        }

        public boolean isSimInsert(int phoneType) {
            return isSimInsert(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID, phoneType);
        }

        public boolean isSimInsert(int subId, int phoneType) {
            String strEfEccList = null;
            boolean bSIMInserted = false;
            String[] propertyList = (phoneType == PhoneConstants.PHONE_TYPE_CDMA) ?
                CDMA_SIM_RECORDS_PROPERTY_ECC_LIST : SIM_RECORDS_PROPERTY_ECC_LIST;

            // DEFAULT_SUBSCRIPTION_ID, check all slot
            if (SubscriptionManager.DEFAULT_SUBSCRIPTION_ID == subId) {
                for (int i = 0; i < MAX_SIM_NUM; i++) {
                    strEfEccList = SystemProperties.get(propertyList[i]);
                    if (!TextUtils.isEmpty(strEfEccList)) {
                        bSIMInserted = true;
                        break;
                    }
                }

                // double check if CDMA card is insert or not
                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    int subIdCdma = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                    int slotId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
                    TelephonyManager tm = TelephonyManager.getDefault();
                    int simCount = tm.getSimCount();
                    int tmpSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                    for (int i = 0; i < simCount; i++) {
                        int[] subIds = SubscriptionManager.getSubId(i);
                        if (subIds != null && subIds.length > 0) {
                            tmpSubId= subIds[0];
                        }
                        if (tm.getCurrentPhoneType(tmpSubId) == PhoneConstants.PHONE_TYPE_CDMA) {
                            subIdCdma = tmpSubId;
                            slotId = i;
                            break;
                        }
                    }
                    if (subIdCdma != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        bSIMInserted = tm.hasIccCard(slotId);
                    }

                    vlog("[isSimInsert] CDMA subId:" + subIdCdma + ", slotId:" + slotId
                            + ", bSIMInserted:" + bSIMInserted);
                }
            } else {
                int slotId = SubscriptionManager.getSlotIndex(subId);
                if (SubscriptionManager.isValidSlotIndex(slotId)) {
                    strEfEccList = SystemProperties.get(propertyList[slotId]);
                    if (!TextUtils.isEmpty(strEfEccList)) {
                        bSIMInserted = true;
                    }
                }
            }
            return bSIMInserted;
        }

        public static boolean isEccPlmnMatch(String strPlmn) {
            if (TextUtils.isEmpty(strPlmn)) {
                return true;
            }

            // Get PLMN in following sequence until we got valid PLMN, otherwise loop to next SIM:
            //  1. From network plmn
            //  2. From SIM PLMN (getSimOperatorNumericForPhone) available after SIM records loaded
            //  3. From UICC PLMN property report by modem (set in RILD after +EUSIM)
            for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
                // 1. check network operator PLMN
                String strOperatorPlmn =
                        TelephonyManager.getDefault().getNetworkOperatorForPhone(i);
                vlog("[isEccPlmnMatch] NW PLMN: " + strOperatorPlmn);
                if (TextUtils.isEmpty(strOperatorPlmn)) {
                    // 2. Check SIM PLMN
                    strOperatorPlmn =
                            TelephonyManager.getDefault().getSimOperatorNumericForPhone(i);
                    vlog("[isEccPlmnMatch] SIM PLMN: " + strOperatorPlmn);
                    if (TextUtils.isEmpty(strOperatorPlmn)) {
                        // 3. Check UICC PLMN directly if can't get SIM operator PLMN
                        strOperatorPlmn = SystemProperties.get(UICC_PLMN_PROPERTY[i]);
                        vlog("[isEccPlmnMatch] UICC PLMN: " + strOperatorPlmn);
                    }
                    // If SIM not ready, plmn get from SIM will be N/A
                    if (TextUtils.isEmpty(strOperatorPlmn) || "N/A".equals(strOperatorPlmn)) {
                        // 4. no PLMN detect from network/SIM, try get from Located PLMN
                        strOperatorPlmn = MtkTelephonyManagerEx.getDefault().getLocatedPlmn(i);
                    }
                    if (TextUtils.isEmpty(strOperatorPlmn)) {
                        // no PLMN detect from network/SIM, continue to next SIM
                        continue;
                    }
                }

                String strPlmnFormatted =
                        strOperatorPlmn.substring(0, 3) + " " + strOperatorPlmn.substring(3);
                vlog("[isEccPlmnMatch] PLMN ("
                        + i + "): " + strPlmnFormatted + ", strPlmn: " + strPlmn);
                if (strPlmnFormatted.equals(strPlmn)
                        || (0 == strPlmn.substring(4).compareToIgnoreCase("FFF")
                        && strPlmn.substring(0, 3).equals(
                        strPlmnFormatted.substring(0, 3)))) {
                    vlog("[isEccPlmnMatch] PLMN matched strPlmn: " + strPlmn);
                    return true;
                }
            }
            return false;
        }

        /* Check if SIM status is ready (Ex: not PIN locked...) */
        private boolean isSimReady(int subId) {
            int slotId = SubscriptionManager.getSlotIndex(subId);
            int state = TelephonyManager.getDefault().getSimState(slotId);
            dlog("[isSimReady] subId: " + subId + ", state: " + state);
            return state == TelephonyManager.SIM_STATE_READY;
        }

        private boolean isCt4GDualModeCard(int subId) {
            int slotId = SubscriptionManager.getSlotIndex(subId);
            if (SubscriptionManager.isValidSlotIndex(slotId)) {
                String cardType = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[slotId]);
                if (!TextUtils.isEmpty(cardType) &&
                        (cardType.indexOf("CSIM") >= 0) && (cardType.indexOf("USIM") >= 0)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** @hide */
    private static class XmlEccSource extends EccSource {
        //private ArrayList<EccEntry> mCdmaEccList = null;

        public XmlEccSource(int phoneType) {
            super(phoneType);
        }

        @Override
        public synchronized void parseEccList() {
            String optr = SystemProperties.get("persist.vendor.operator.optr");

            // Parse GSM ECC list
            mEccList = new ArrayList<EccEntry>();

            // By default, OM load use default ECC XML
            String xmlPath = EccEntry.ECC_LIST_PATH + EccEntry.ECC_LIST;
            // 1. Check operator ECC XML configure file
            if (!TextUtils.isEmpty(optr)) {
                xmlPath = EccEntry.ECC_LIST_PATH + "ecc_list_" + optr + ".xml";
                File opFileCheck = new File(xmlPath);
                if (!opFileCheck.exists()) {
                    log("[parseEccList] OP ECC file not exist, xmlPath: " + xmlPath);
                    // 2. No operator ECC xml, use default ECC XML
                    xmlPath = EccEntry.ECC_LIST_PATH + EccEntry.ECC_LIST;
                }
            }
            log("[parseEccList] Read ECC list from " + xmlPath);
            parseFromXml(xmlPath, mEccList);

            // Parse CDMA ECC list
            if (sIsC2kSupport) {
                mCdmaEccList = new ArrayList<EccEntry>();
                // By default, OM load use default ECC XML
                String cdmaXmlPath = EccEntry.ECC_LIST_PATH + EccEntry.CDMA_ECC_LIST;

                // CDMA_SS_ECC_LIST don't support OP12 special ECC *911, #911
                if ("ss".equals(SystemProperties.get("persist.radio.multisim.config"))
                        && !("OP12".equals(optr))) {
                    cdmaXmlPath = EccEntry.ECC_LIST_PATH + EccEntry.CDMA_SS_ECC_LIST;
                } else {
                    // Check operator ECC XML configure file
                    if (!TextUtils.isEmpty(optr)) {
                        cdmaXmlPath = EccEntry.ECC_LIST_PATH + "cdma_ecc_list_" + optr + ".xml";
                        File opFileCheck = new File(cdmaXmlPath);
                        if (!opFileCheck.exists()) {
                            // 3. No operator ECC xml, use default ECC XML
                            cdmaXmlPath = EccEntry.ECC_LIST_PATH + EccEntry.CDMA_ECC_LIST;
                        }
                    }
                }
                log("[parseEccList] Read CDMA ECC list from " + cdmaXmlPath);
                parseFromXml(cdmaXmlPath, mCdmaEccList);
            }
            dlog("[parseEccList] GSM XML ECC list: " + mEccList);
            dlog("[parseEccList] CDMA XML ECC list: " + mCdmaEccList);
        }

        @Override
        public synchronized boolean isEmergencyNumber(String number, int subId, int phoneType) {
            ArrayList<EccEntry> eccList = (phoneType == PhoneConstants.PHONE_TYPE_CDMA) ?
                    mCdmaEccList : mEccList;
            vlog("[isEmergencyNumber] eccList: " + eccList);
            if (isSimInsert(phoneType)) {
                if (eccList != null) {
                    for (EccEntry eccEntry : eccList) {
                        if (!eccEntry.getCondition().equals(EccEntry.ECC_NO_SIM)) {
                            if (isMatch(eccEntry.getEcc(), number, eccEntry.getPlmn())) {
                                log("[isEmergencyNumber] match XML ECC (w/ SIM) for phoneType: "
                                        + phoneType);
                                return true;
                            }
                        }
                    }
                }
            } else {
                if (eccList != null) {
                    for (EccEntry eccEntry : eccList) {
                        if (isMatch(eccEntry.getEcc(), number, eccEntry.getPlmn())) {
                            log("[isEmergencyNumber] match XML ECC (w/o SIM) for phoneType: "
                                    + phoneType);
                            return true;
                        }
                    }
                }
            }
            vlog("[isEmergencyNumber] no match XML ECC for phoneType: " + phoneType);
            return false;
        }

        @Override
        public synchronized int getServiceCategory(String number, int subId) {
            if (mEccList != null) {
                for (EccEntry eccEntry : mEccList) {
                    String ecc = eccEntry.getEcc();
                    if (isMatch(ecc, number, eccEntry.getPlmn())) {
                        log("[getServiceCategory] match xml customized, ECC: "
                                + ecc + ", Category= " + eccEntry.getCategory()
                                + ", plmn: " + eccEntry.getPlmn());
                        return Integer.parseInt(eccEntry.getCategory());
                    }
                }
            }
            return -1;
        }

        private synchronized void parseFromXml(String path, ArrayList<EccEntry> eccList) {
            try {
                FileReader fileReader;
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                if (parser == null) {
                    log("[parseFromXml] XmlPullParserFactory.newPullParser() return null");
                    return;
                }

                fileReader = new FileReader(path);

                parser.setInput(fileReader);
                int eventType = parser.getEventType();
                EccEntry record = null;
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            if (parser.getName().equals(EccEntry.ECC_ENTRY_TAG)) {
                                record = new EccEntry();
                                int attrNum = parser.getAttributeCount();
                                for (int i = 0; i < attrNum; ++i) {
                                    String name = parser.getAttributeName(i);
                                    String value = parser.getAttributeValue(i);
                                    if (name.equals(EccEntry.ECC_ATTR)) {
                                        record.setEcc(value);
                                    } else if (name.equals(EccEntry.CATEGORY_ATTR)) {
                                        record.setCategory(value);
                                    } else if (name.equals(EccEntry.CONDITION_ATTR)) {
                                        record.setCondition(value);
                                    } else if (name.equals(EccEntry.PLMN_ATTR)) {
                                        record.setPlmn(value);
                                    }
                                }
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            if (parser.getName().equals(EccEntry.ECC_ENTRY_TAG)
                                    && record != null) {
                                eccList.add(record);
                            }
                            break;
                        default:
                            break;
                    }
                    eventType = parser.next();
                }
                fileReader.close();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** @hide */
    private static class NetworkEccSource extends EccSource {
        public NetworkEccSource(int phoneType) {
            super(phoneType);
        }

        @Override
        public synchronized boolean isEmergencyNumber(String number, int subId, int phoneType) {
            // 3GPP spec network ECC
            if (!isPhoneTypeSupport(phoneType)) {
                return false;
            }

            String strEccCategoryList = null;
            if (subId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
                // if no SUB id, query all SIM network ECC
                for (int i = 0; i < MAX_SIM_NUM; i++) {
                    strEccCategoryList = SystemProperties.get(NETWORK_ECC_LIST[i]);
                    if (!TextUtils.isEmpty(strEccCategoryList)) {
                        dlog("[isEmergencyNumber] network list [" + i
                                + "]:" + strEccCategoryList);
                        for (String strEccCategory : strEccCategoryList.split(";")) {
                            if (!strEccCategory.isEmpty()) {
                                String[] strEccCategoryAry = strEccCategory.split(",");
                                if (2 == strEccCategoryAry.length) {
                                    if (isMatch(strEccCategoryAry[0], number)) {
                                        log("[isEmergencyNumber] match network ECC for phoneType: "
                                                + phoneType);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                int slotId = SubscriptionManager.getSlotIndex(subId);
                if (SubscriptionManager.isValidSlotIndex(slotId)) {
                    strEccCategoryList = SystemProperties.get(NETWORK_ECC_LIST[slotId]);
                    if (!TextUtils.isEmpty(strEccCategoryList)) {
                        dlog("[isEmergencyNumber]ril.ecc.service.category.list["
                                + slotId + "]" + strEccCategoryList);
                        for (String strEccCategory : strEccCategoryList.split(";")) {
                            if (!strEccCategory.isEmpty()) {
                                String[] strEccCategoryAry = strEccCategory.split(",");
                                if (2 == strEccCategoryAry.length) {
                                    if (isMatch(strEccCategoryAry[0], number)) {
                                        log("[isEmergencyNumber] match network ECC for phoneType: "
                                                + phoneType);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public synchronized int getServiceCategory(String number, int subId) {
            String strEccCategoryList;
            if (subId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
                // Query without SUB id, query all SIM network ECC service category
                for (int i = 0; i < MAX_SIM_NUM; i++) {
                    strEccCategoryList = SystemProperties.get(NETWORK_ECC_LIST[i]);
                    if (!TextUtils.isEmpty(strEccCategoryList)) {
                        log("[getServiceCategory] Network ECC List: "
                                + strEccCategoryList);
                        for (String strEccCategory : strEccCategoryList.split(";")) {
                            if (!strEccCategory.isEmpty()) {
                                String[] strEccCategoryAry = strEccCategory.split(",");
                                if (2 == strEccCategoryAry.length) {
                                    if (isMatch(strEccCategoryAry[0], number)) {
                                        log("[getServiceCategory] match network, "
                                                + "Ecc= " + number + ", Category= "
                                                + Integer.parseInt(strEccCategoryAry[1]));
                                        return Integer.parseInt(strEccCategoryAry[1]);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                int slotId = SubscriptionManager.getSlotIndex(subId);
                if (SubscriptionManager.isValidSlotIndex(slotId)) {
                    strEccCategoryList = SystemProperties.get(NETWORK_ECC_LIST[slotId]);
                    if (!TextUtils.isEmpty(strEccCategoryList)) {
                        log("[getServiceCategory] Network ECC List: "
                               + strEccCategoryList);
                        for (String strEccCategory : strEccCategoryList.split(";")) {
                            if (!strEccCategory.isEmpty()) {
                                String[] strEccCategoryAry = strEccCategory.split(",");
                                if (2 == strEccCategoryAry.length) {
                                    if (isMatch(strEccCategoryAry[0], number)) {
                                        log("[getServiceCategory] match network, "
                                                + "Ecc= " + number + ", Category= "
                                                + Integer.parseInt(strEccCategoryAry[1]));
                                        return Integer.parseInt(strEccCategoryAry[1]);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // not found
            return -1;
        }
    }

    /** @hide */
    private static class SimEccSource extends EccSource {
        public SimEccSource(int phoneType) {
            super(phoneType);
        }

        public synchronized boolean isEmergencyNumber(String number, int subId, int phoneType) {
            String strEfEccList;
            // If not specific subId, query all slot SIM ECC
            if (subId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    for (int i = 0; i < MAX_SIM_NUM; i++) {
                        String numbers = SystemProperties.get(
                                CDMA_SIM_RECORDS_PROPERTY_ECC_LIST[i]);
                        if (!TextUtils.isEmpty(numbers)) {
                            for (String emergencyNum : numbers.split(",")) {
                                if (isMatch(emergencyNum, number)) {
                                    log("[isEmergencyNumber] match CDMA SIM ECC for phoneType: "
                                            + phoneType);
                                    return true;
                                }
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < MAX_SIM_NUM; i++) {
                        strEfEccList = SystemProperties.get(SIM_RECORDS_PROPERTY_ECC_LIST[i]);
                        if (!TextUtils.isEmpty(strEfEccList)) {
                            for (String strEccCategory : strEfEccList.split(";")) {
                                if (!strEccCategory.isEmpty()) {
                                    String[] strEccCategoryAry = strEccCategory.split(",");
                                    if (2 == strEccCategoryAry.length) {
                                        if (isMatch(strEccCategoryAry[0], number)) {
                                            log("[isEmergencyNumber] match GSM SIM ECC phoneType: "
                                                    + phoneType);
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                int slotId = SubscriptionManager.getSlotIndex(subId);
                if (!SubscriptionManager.isValidSlotIndex(slotId)) {
                    return false;
                }

                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    String numbers = SystemProperties.get(
                            CDMA_SIM_RECORDS_PROPERTY_ECC_LIST[slotId]);
                    if (!TextUtils.isEmpty(numbers)) {
                        for (String emergencyNum : numbers.split(",")) {
                            if (isMatch(emergencyNum, number)) {
                                log("[isEmergencyNumber] match CDMA SIM ECC for phoneType: "
                                        + phoneType);
                                return true;
                            }
                        }
                    }
                } else {
                    strEfEccList = SystemProperties.get(SIM_RECORDS_PROPERTY_ECC_LIST[slotId]);
                    if (!TextUtils.isEmpty(strEfEccList)) {
                        for (String strEccCategory : strEfEccList.split(";")) {
                            if (!strEccCategory.isEmpty()) {
                                String[] strEccCategoryAry = strEccCategory.split(",");
                                if (2 == strEccCategoryAry.length) {
                                    if (isMatch(strEccCategoryAry[0], number)) {
                                        log("[isEmergencyNumber] match GSM SIM ECC for phoneType: "
                                                + phoneType);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return false;
        }

        @Override
        public synchronized int getServiceCategory(String number, int subId) {
            String strEccCategoryList;

            for (int i = 0; i < MAX_SIM_NUM; i++) {
                strEccCategoryList = SystemProperties.get(SIM_RECORDS_PROPERTY_ECC_LIST[i]);
                if (!TextUtils.isEmpty(strEccCategoryList)) {
                    dlog("[getServiceCategory] list[" + i + "]: " + strEccCategoryList);
                    for (String strEccCategory : strEccCategoryList.split(";")) {
                        if (!strEccCategory.isEmpty()) {
                            String[] strEccCategoryAry = strEccCategory.split(",");
                            if (2 == strEccCategoryAry.length) {
                                if (isMatch(strEccCategoryAry[0], number)) {
                                    return Integer.parseInt(strEccCategoryAry[1]);
                                }
                            }
                        }
                    }
                }
            }

            return -1;
        }
    }

    /** @hide */
    private static class CtaEccSource extends EccSource {
        private static String[] sCtaList = {"120", "122", "119", "110"};

        public CtaEccSource(int phoneType) {
            super(phoneType);
        }

        @Override
        public synchronized void parseEccList() {
            EccEntry record = null;
            mEccList = new ArrayList<EccEntry>();
            for (String emergencyNum : sCtaList) {
                record = new EccEntry();
                record.setEcc(emergencyNum);
                record.setCategory("0");
                record.setCondition(EccEntry.ECC_FOR_MMI);
                mEccList.add(record);
            }

            dlog("[parseEccList] CTA ECC list: " + mEccList);
        }

        @Override
        public synchronized boolean isEmergencyNumber(String number, int subId, int phoneType) {
            if (isPhoneTypeSupport(phoneType) && isNeedCheckCtaSet() && mEccList != null) {
                for (EccEntry eccEntry : mEccList) {
                    String ecc = eccEntry.getEcc();
                    if (isMatch(ecc, number)) {
                        log("[isEmergencyNumber] match CTA ECC for phoneType: " + phoneType);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public synchronized void addToEccList(ArrayList<EccEntry> eccList) {
            if (isNeedCheckCtaSet()) {
                super.addToEccList(eccList);
            }
        }

        @Override
        public synchronized int getServiceCategory(String number, int subId) {
            return -1;
        }

        private boolean isNeedCheckCtaSet() {
            // If no SIM insert we don't know if need to enable CTA check or not
            // return true because CTA_SET feature option is ON
            if (!(isSimInsert(PhoneConstants.PHONE_TYPE_GSM)
                    || isSimInsert(PhoneConstants.PHONE_TYPE_CDMA))) {
                vlog("[isNeedCheckCtaSet] No SIM insert, return true: ");
                return true;
            }

            TelephonyManager tm = TelephonyManager.getDefault();
            int simCount = tm.getSimCount();
            String strIccid = null;

            // Error handle permission for third party
            if (IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx")) == null) {
                log("[isNeedCheckCtaSet] No permission to get phoneEx:");
                return true;
            }

            for (int i = 0; i < simCount; i++) {
                try {
                    strIccid = MtkTelephonyManagerEx.getDefault().getSimSerialNumber(i);
                    vlog("[isNeedCheckCtaSet] strIccid[" + i + "]: " + strIccid);
                    // Only China operator SIM card need to enable CTA ECC check
                    if (!TextUtils.isEmpty(strIccid) && strIccid.startsWith(ICCID_CN_PREFIX)) {
                        return true;
                    }
                } catch (NullPointerException ex) {
                    vlog("[isNeedCheckCtaSet] NullPointerException:" + ex);
                    return true;
                } catch (Exception e) {
                    // Permission exception may happen if third party application
                    // don't have read phone state permission.
                    vlog("[isNeedCheckCtaSet] Exception: " + e);
                    return true;
                }
            }
            return false;
        }

        @Override
        public synchronized boolean isSpecialEmergencyNumber(int subId, String number) {
            if (!isNeedCheckCtaSet()) {
                return false;
            }

            return super.isSpecialEmergencyNumber(subId, number);
        }
    }

    /** @hide */
    private static class PropertyEccSource extends EccSource {
        public PropertyEccSource(int phoneType) {
            super(phoneType);
        }

        @Override
        public synchronized void parseEccList() {
            String strCount = SystemProperties.get(EccEntry.PROPERTY_COUNT);
            if (TextUtils.isEmpty(strCount)) {
                log("[parseEccList] empty property");
                return;
            }

            mEccList = new ArrayList<EccEntry>();

            int nCount = Integer.parseInt(strCount);
            for (int i = 0; i < nCount; i++) {
                String strNumber = SystemProperties.get(EccEntry.PROPERTY_NUMBER + i);
                if (!TextUtils.isEmpty(strNumber)) {
                    EccEntry entry = new EccEntry();
                    entry.setEcc(strNumber);

                    String strType = SystemProperties.get(EccEntry.PROPERTY_TYPE + i);
                    if (!TextUtils.isEmpty(strType)) {
                        short nType = 0;
                        for (String strTypeKey : strType.split(" ")) {
                            for (int index = 0; index < EccEntry.PROPERTY_TYPE_KEY.length;
                                    index++) {
                                if (strTypeKey.equals(EccEntry.PROPERTY_TYPE_KEY[index])) {
                                    nType |= EccEntry.PROPERTY_TYPE_VALUE[index];
                                }
                            }
                        }
                        entry.setCategory(Short.toString(nType));
                    } else {
                        entry.setCategory("0");
                    }

                    String strNonEcc = SystemProperties.get(EccEntry.PROPERTY_NON_ECC + i);
                    if (TextUtils.isEmpty(strNonEcc) || strNonEcc.equals("false")) {
                        entry.setCondition(EccEntry.ECC_ALWAYS);
                    } else {
                        entry.setCondition(EccEntry.ECC_NO_SIM);
                    }

                    String strPlmn = SystemProperties.get(EccEntry.PROPERTY_PLMN + i);
                    if (!TextUtils.isEmpty(strPlmn)) {
                        entry.setPlmn(strPlmn);
                    }

                    mEccList.add(entry);
                }
            }
            dlog("[parseEccList] property ECC list: " + mEccList);
        }

        @Override
        public synchronized boolean isEmergencyNumber(String number, int subId, int phoneType) {
            if (!isPhoneTypeSupport(phoneType)) {
                return false;
            }

            if (isSimInsert(phoneType)) {
                if (mEccList != null) {
                    for (EccEntry eccEntry : mEccList) {
                        if (!eccEntry.getCondition().equals(EccEntry.ECC_NO_SIM)) {
                            String ecc = eccEntry.getEcc();
                            if ((isMatch(ecc, number))
                                    && isEccPlmnMatch(eccEntry.getPlmn())) {
                                log("[isEmergencyNumber] match property ECC(w/ SIM) for phoneType:"
                                        + phoneType);
                                return true;
                            }
                        }
                    }
                }
            } else {
                if (mEccList != null) {
                    for (EccEntry eccEntry : mEccList) {
                        String ecc = eccEntry.getEcc();
                        if (isMatch(ecc, number)) {
                            log("[isEmergencyNumber] match property ECC(w/o SIM) for phoneType:"
                                    + phoneType);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    /** @hide */
    private static class TestEccSource extends EccSource {
        private static final String TEST_ECC_LIST = "persist.vendor.em.testecc";

        public TestEccSource(int phoneType) {
            super(phoneType);
        }

        @Override
        public synchronized boolean isEmergencyNumber(String number, int subId, int phoneType) {
            String strtestEccList = SystemProperties.get(TEST_ECC_LIST);
            if (TextUtils.isEmpty(strtestEccList)) {
                return false;
            }

            if (isSimInsert(PhoneConstants.PHONE_TYPE_GSM)
                    || isSimInsert(PhoneConstants.PHONE_TYPE_CDMA)) {
                dlog("[isEmergencyNumber] test ECC list: " + strtestEccList);
                for (String strEcc : strtestEccList.split(",")) {
                    if (!strEcc.isEmpty()) {
                        if (isMatch(strEcc, number)) {
                            dlog("[isEmergencyNumber] match test ECC for phoneType: "
                                    + phoneType);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    // Initialization
    static {
        initialize();
    }

    private static void initialize() {
        sIsCtaSupport = "1".equals(SystemProperties.get("ro.vendor.mtk_cta_support"));
        sIsCtaSet = "1".equals(SystemProperties.get("ro.vendor.mtk_cta_set"));
        sIsC2kSupport = (SystemProperties.get("ro.boot.opt_ps1_rat").indexOf('C') >= 0);
        sIsOP09Support = "OP09".equals(SystemProperties.get("persist.vendor.operator.optr"))
                && ("SEGDEFAULT".equals(SystemProperties.get("persist.vendor.operator.seg"))
                || "SEGC".equals(SystemProperties.get("persist.vendor.operator.seg")));
        sIsCdmaLessSupport =
                ("3".equals(SystemProperties.get("persist.vendor.vzw_device_type", "0")) ||
                "4".equals(SystemProperties.get("persist.vendor.vzw_device_type", "0")));

        log("Init: sIsCtaSupport: " + sIsCtaSupport +
                ", sIsCtaSet: " + sIsCtaSet + ", sIsC2kSupport: " + sIsC2kSupport +
                ", sIsOP09Support: " + sIsOP09Support +
                ", sIsCdmaLessSupport: " + sIsCdmaLessSupport);
        sPlusCodeUtils = PlusCodeProcessor.getPlusCodeUtils();

        initEccSource();
    }

    private static void initEccSource() {
        sAllEccSource = new ArrayList<EccSource>();

        sNetworkEcc = new NetworkEccSource(PhoneConstants.PHONE_TYPE_GSM);
        sPropertyEcc = new PropertyEccSource(PhoneConstants.PHONE_TYPE_GSM);
        if (sIsC2kSupport) {
            sXmlEcc = new XmlEccSource(
                    PhoneConstants.PHONE_TYPE_GSM | PhoneConstants.PHONE_TYPE_CDMA);
            sSimEcc = new SimEccSource(
                    PhoneConstants.PHONE_TYPE_GSM | PhoneConstants.PHONE_TYPE_CDMA);
            sTestEcc = new TestEccSource(
                    PhoneConstants.PHONE_TYPE_GSM | PhoneConstants.PHONE_TYPE_CDMA);
        } else {
            sXmlEcc = new XmlEccSource(PhoneConstants.PHONE_TYPE_GSM);
            sSimEcc = new SimEccSource(PhoneConstants.PHONE_TYPE_GSM);
            sTestEcc = new TestEccSource(PhoneConstants.PHONE_TYPE_GSM);
        }

        // Add EccSource according to priority
        // Network ECC > SIM ECC > Other ECC
        // 1. Add network ECC source
        sAllEccSource.add(sNetworkEcc);
        // 2. Add SIM ECC
        sAllEccSource.add(sSimEcc);

        // 3. Other ECC
        sAllEccSource.add(sXmlEcc);
        sAllEccSource.add(sPropertyEcc);
        sAllEccSource.add(sTestEcc);

        if (sIsCtaSet) {
            sCtaEcc = new CtaEccSource(PhoneConstants.PHONE_TYPE_GSM);
            sAllEccSource.add(sCtaEcc);
        }
    }

    /**
     * This function checks if there is a plus sign (+) in the passed-in dialing number.
     * If there is, it processes the plus sign based on the default telephone
     * numbering plan of the system when the phone is activated and the current
     * telephone numbering plan of the system that the phone is camped on.
     * Currently, we only support the case that the default and current telephone
     * numbering plans are North American Numbering Plan(NANP).
     *
     * The passed-in dialStr should only contain the valid format as described below,
     * 1) the 1st character in the dialStr should be one of the really dialable
     *    characters listed below
     *    ISO-LATIN characters 0-9, *, # , +
     * 2) the dialStr should already strip out the separator characters,
     *    every character in the dialStr should be one of the non separator characters
     *    listed below
     *    ISO-LATIN characters 0-9, *, # , +, WILD, WAIT, PAUSE
     *
     * Otherwise, this function returns the dial string passed in
     *
     * @param dialStr the original dial string
     * @return the converted dial string if the current/default countries belong to NANP,
     * and if there is the "+" in the original dial string. Otherwise, the original dial
     * string returns.
     *
     * This API is for CDMA only
     *
     */
    public static String cdmaCheckAndProcessPlusCode(String dialStr) {
        /// M: @{
        String result = preProcessPlusCode(dialStr);
        if (result != null && !result.equals(dialStr)) {
            return result;
        }
        /// @}
        if (!TextUtils.isEmpty(dialStr)) {
            if (PhoneNumberUtils.isReallyDialable(dialStr.charAt(0)) &&
                isNonSeparator(dialStr)) {
                String currIso = TelephonyManager.getDefault().getNetworkCountryIso();
                String defaultIso = TelephonyManager.getDefault().getSimCountryIso();
                if (!TextUtils.isEmpty(currIso) && !TextUtils.isEmpty(defaultIso)) {
                    return PhoneNumberUtils.cdmaCheckAndProcessPlusCodeByNumberFormat(dialStr,
                            getFormatTypeFromCountryCode(currIso),
                            getFormatTypeFromCountryCode(defaultIso));
                }
            }
        }
        return dialStr;
    }

    /**
     * Process phone number for CDMA, converting plus code using the home network number format.
     * This is used for outgoing SMS messages.
     *
     * @param dialStr the original dial string
     * @return the converted dial string
     * @hide for internal use
     */
    public static String cdmaCheckAndProcessPlusCodeForSms(String dialStr) {
        /// M: @{
        String result = preProcessPlusCodeForSms(dialStr);
        if (result != null && !result.equals(dialStr)) {
            return result;
        }
        /// @}

        if (!TextUtils.isEmpty(dialStr)) {
            if (PhoneNumberUtils.isReallyDialable(dialStr.charAt(0)) && isNonSeparator(dialStr)) {
                String defaultIso = TelephonyManager.getDefault().getSimCountryIso();
                if (!TextUtils.isEmpty(defaultIso)) {
                    int format = getFormatTypeFromCountryCode(defaultIso);
                    return PhoneNumberUtils.cdmaCheckAndProcessPlusCodeByNumberFormat(
                            dialStr, format, format);
                }
            }
        }
        return dialStr;
    }
    // Modify AOSP END

    // MTK Added Start

    /**
     * Return the extracted phone number.
     *
     * @param phoneNumber Phone number string.
     * @return Return number whiched is extracted the CLIR part.
     */
    public static String extractCLIRPortion(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        // ex. **61*<any international number>**<timer>#
        Pattern p = Pattern.compile(
                "^([*][#]|[*]{1,2}|[#]{1,2})([0-9]{2,3})([*])([+]?[0-9]+)(.*)(#)$");
        Matcher m = p.matcher(phoneNumber);
        if (m.matches()) {
            return m.group(4); // return <any international number>
        } else if (phoneNumber.startsWith("*31#") || phoneNumber.startsWith("#31#")) {
            vlog(phoneNumber + " Start with *31# or #31#, return " + phoneNumber.substring(4));
            return phoneNumber.substring(4);
        } else if (phoneNumber.indexOf(PLUS_SIGN_STRING) != -1 &&
                   phoneNumber.indexOf(PLUS_SIGN_STRING) ==
                   phoneNumber.lastIndexOf(PLUS_SIGN_STRING)) {
            p = Pattern.compile("(^[#*])(.*)([#*])(.*)(#)$");
            m = p.matcher(phoneNumber);
            if (m.matches()) {
                if ("".equals(m.group(2))) {
                    // Started with two [#*] ends with #
                    // So no dialing number and we'll just return "" a +, this handles **21#+
                    vlog(phoneNumber + " matcher pattern1, return empty string.");
                    return "";
                } else {
                    String strDialNumber = m.group(4);
                    if (strDialNumber != null && strDialNumber.length() > 1
                            && strDialNumber.charAt(0) == PLUS_SIGN_CHAR) {
                        // Starts with [#*] and ends with #
                        // Assume group 4 is a dialing number such as *21*+1234554#
                        vlog(phoneNumber + " matcher pattern1, return " + strDialNumber);
                        return strDialNumber;
                    }
                }
            } else {
                p = Pattern.compile("(^[#*])(.*)([#*])(.*)");
                m = p.matcher(phoneNumber);
                if (m.matches()) {
                    String strDialNumber = m.group(4);
                    if (strDialNumber != null && strDialNumber.length() > 1
                            && strDialNumber.charAt(0) == PLUS_SIGN_CHAR) {
                        // Starts with [#*] and only one other [#*]
                        // Assume the data after last [#*] is dialing number
                        // (i.e. group 4) such as *31#+11234567890.
                        // This also includes the odd ball *21#+
                        vlog(phoneNumber + " matcher pattern2, return " + strDialNumber);
                        return strDialNumber;
                    }
                }
            }
        }

        return phoneNumber;
    }


    /**
     * Prepend plus to the number.
     * @param number The original number.
     * @return The number with plus sign.
     * @hide
     */
    public static String prependPlusToNumber(String number) {
        // This is an "international number" and should have
        // a plus prepended to the dialing number. But there
        // can also be Gsm MMI codes as defined in TS 22.030 6.5.2
        // so we need to handle those also.
        //
        // http://web.telia.com/~u47904776/gsmkode.htm is a
        // has a nice list of some of these GSM codes.
        //
        // Examples are:
        //   **21*+886988171479#
        //   **21*8311234567#
        //   **21*+34606445635**20#
        //   **21*34606445635**20#
        //   *21#
        //   #21#
        //   *#21#
        //   *31#+11234567890
        //   #31#+18311234567
        //   #31#8311234567
        //   18311234567
        //   +18311234567#
        //   +18311234567
        // Odd ball cases that some phones handled
        // where there is no dialing number so they
        // append the "+"
        //   *21#+
        //   **21#+
        StringBuilder ret;
        String retString = number.toString();
        Pattern p = Pattern.compile(
                "^([*][#]|[*]{1,2}|[#]{1,2})([0-9]{2,3})([*])([0-9]+)(.*)(#)$");
        Matcher m = p.matcher(retString);
        if (m.matches()) {
            ret = new StringBuilder();
            ret.append(m.group(1));
            ret.append(m.group(2));
            ret.append(m.group(3));
            ret.append("+");
            ret.append(m.group(4));
            ret.append(m.group(5));
            ret.append(m.group(6));
        } else {
            p = Pattern.compile("(^[#*])(.*)([#*])(.*)(#)$");
            m = p.matcher(retString);
            if (m.matches()) {
                if ("".equals(m.group(2))) {
                    // Started with two [#*] ends with #
                    // So no dialing number and we'll just
                    // append a +, this handles **21#+
                    ret = new StringBuilder();
                    ret.append(m.group(1));
                    ret.append(m.group(3));
                    ret.append(m.group(4));
                    ret.append(m.group(5));
                    ret.append("+");
                } else {
                    // Starts with [#*] and ends with #
                    // Assume group 4 is a dialing number
                    // such as *21*+1234554#
                    ret = new StringBuilder();
                    ret.append(m.group(1));
                    ret.append(m.group(2));
                    ret.append(m.group(3));
                    ret.append("+");
                    ret.append(m.group(4));
                    ret.append(m.group(5));
                }
            } else {
                p = Pattern.compile("(^[#*])(.*)([#*])(.*)");
                m = p.matcher(retString);
                if (m.matches()) {
                    // Starts with [#*] and only one other [#*]
                    // Assume the data after last [#*] is dialing
                    // number (i.e. group 4) such as *31#+11234567890.
                    // This also includes the odd ball *21#+
                    ret = new StringBuilder();
                    ret.append(m.group(1));
                    ret.append(m.group(2));
                    ret.append(m.group(3));
                    ret.append("+");
                    ret.append(m.group(4));
                } else {
                    // Does NOT start with [#*] just prepend '+'
                    ret = new StringBuilder();
                    ret.append('+');
                    ret.append(retString);
                }
            }
        }
        return ret.toString();
    }

    private static String preProcessPlusCode(String dialStr) {
        if (!TextUtils.isEmpty(dialStr)) {
            if (PhoneNumberUtils.isReallyDialable(dialStr.charAt(0)) && isNonSeparator(dialStr)) {
                String currIso = TelephonyManager.getDefault().getNetworkCountryIso();
                String defaultIso = TelephonyManager.getDefault().getSimCountryIso();
                boolean needToFormat = true;
                if (!TextUtils.isEmpty(currIso) && !TextUtils.isEmpty(defaultIso)) {
                    int currFormat = getFormatTypeFromCountryCode(currIso);
                    int defaultFormat = getFormatTypeFromCountryCode(defaultIso);
                    needToFormat = !((currFormat == defaultFormat) && (currFormat == FORMAT_NANP));
                }
                if (needToFormat) {
                    vlog("preProcessPlusCode, before format number:" + dialStr);
                    String retStr = dialStr;
                    // Checks if the plus sign character is in the passed-in dial string
                    if (dialStr.lastIndexOf(PLUS_SIGN_STRING) != -1) {
                        String postDialStr = null;
                        String tempDialStr = dialStr;

                        // Sets the retStr to null since the conversion will be performed below.
                        retStr = null;
                        do {
                            String networkDialStr;
                            networkDialStr = PhoneNumberUtils.extractNetworkPortionAlt(
                                    tempDialStr);
                            if (networkDialStr != null &&
                                    networkDialStr.charAt(0) == PLUS_SIGN_CHAR &&
                                    networkDialStr.length() > 1) {
                                if (sPlusCodeUtils.canFormatPlusToIddNdd()) {
                                    networkDialStr = sPlusCodeUtils.replacePlusCodeWithIddNdd(
                                            networkDialStr);
                                } else {
                                    vlog("preProcessPlusCode, can't format plus code.");
                                    return dialStr;
                                }
                            }

                            vlog("preProcessPlusCode, networkDialStr:" + networkDialStr);
                            // Concatenates the string that is converted from network portion
                            if (!TextUtils.isEmpty(networkDialStr)) {
                                if (retStr == null) {
                                    retStr = networkDialStr;
                                } else {
                                    retStr = retStr.concat(networkDialStr);
                                }
                            } else {
                                Rlog.e(LOG_TAG, "preProcessPlusCode, null");
                                return dialStr;
                            }
                            postDialStr = PhoneNumberUtils.extractPostDialPortion(tempDialStr);
                            if (!TextUtils.isEmpty(postDialStr)) {
                                int dialableIndex = findDialableIndexFromPostDialStr(postDialStr);

                                // dialableIndex should always be greater than 0
                                if (dialableIndex >= 1) {
                                    retStr = appendPwCharBackToOrigDialStr(dialableIndex,
                                             retStr, postDialStr);
                                    // Skips the P/W character, extracts the dialable portion
                                    tempDialStr = postDialStr.substring(dialableIndex);
                                } else {
                                    if (dialableIndex < 0) {
                                        postDialStr = "";
                                    }
                                    Rlog.e(LOG_TAG, "preProcessPlusCode, wrong postDialStr");
                                }
                            }
                            vlog("preProcessPlusCode, postDialStr:" + postDialStr
                                    + ", tempDialStr:" + tempDialStr);
                        } while (!TextUtils.isEmpty(postDialStr)
                                && !TextUtils.isEmpty(tempDialStr));
                    }
                    dialStr = retStr;
                    vlog("preProcessPlusCode, after format number:" + dialStr);
                } else {
                    dlog("preProcessPlusCode, no need format, currIso:" + currIso
                            + ", defaultIso:" + defaultIso);
                }
            }
        }
        return dialStr;
    }

    private static String preProcessPlusCodeForSms(String dialStr) {
        dlog("preProcessPlusCodeForSms ENTER.");
        if (!TextUtils.isEmpty(dialStr) && dialStr.startsWith("+")) {
            if (PhoneNumberUtils.isReallyDialable(dialStr.charAt(0)) && isNonSeparator(dialStr)) {
                String defaultIso = TelephonyManager.getDefault().getSimCountryIso();
                if (getFormatTypeFromCountryCode(defaultIso) != FORMAT_NANP) {
                    if (sPlusCodeUtils.canFormatPlusCodeForSms()) {
                        String retAddr = sPlusCodeUtils.replacePlusCodeForSms(dialStr);
                        if (TextUtils.isEmpty(retAddr)) {
                            dlog("preProcessPlusCodeForSms," +
                                    " can't handle the plus code by PlusCodeUtils");
                        } else {
                            vlog("preProcessPlusCodeForSms, "
                                    + "new dialStr = " + retAddr);
                            dialStr = retAddr;
                        }
                    }
                }
            }
        }
        return dialStr;
    }

    // Phone Number ECC API revise START

    /**
     * Helper function for isLocalEmergencyNumber() and
     * isPotentialLocalEmergencyNumber().
     *
     * @param subId the subscription id of the SIM.
     * @param number the number to look up.
     * @param context the specific context which the number should be checked against
     * @param useExactMatch if true, consider a number to be an emergency
     *           number only if it *exactly* matches a number listed in
     *           the RIL / SIM.  If false, a number is considered to be an
     *           emergency number if it simply starts with the same digits
     *           as any of the emergency numbers listed in the RIL / SIM.
     *
     * @return true if the specified number is an emergency number for a
     *              local country, based on the CountryDetector.
     * @hide
     */
    public static boolean isLocalEmergencyNumberInternal(int subId, String number,
                                                          Context context,
                                                          boolean useExactMatch) {
        // Performance enhance for phone number
        if (TextUtils.isEmpty(number) || number.length() >= getMinMatch()) {
            dlog("[isLocalEmergencyNumberInternal] return false");
            return false;
        }

        String countryIso;
        CountryDetector detector = (CountryDetector) context.getSystemService(
                Context.COUNTRY_DETECTOR);
        Country country = null;
        if (detector != null) {
            country = detector.detectCountry();
        }
        if (country != null) {
            countryIso = country.getCountryIso();
        } else {
            Locale locale = context.getResources().getConfiguration().locale;
            countryIso = locale.getCountry();
            Rlog.w(LOG_TAG, "No CountryDetector; falling back to countryIso based on locale: "
                    + countryIso);
        }
        return isEmergencyNumberExt(subId, number, countryIso, useExactMatch);
    }

    /**
     * Helper function for isEmergencyNumber(String, String) and
     * isPotentialEmergencyNumber(String, String).
     *
     * Mediatek revise for retry ECC with Phone type (GSM or CDMA)
     *
     * @param subId the subscription id of the SIM.
     * @param number the number to look up.
     * @param defaultCountryIso the specific country which the number should be checked against
     * @param useExactMatch if true, consider a number to be an emergency
     *           number only if it *exactly* matches a number listed in
     *           the RIL / SIM.  If false, a number is considered to be an
     *           emergency number if it simply starts with the same digits
     *           as any of the emergency numbers listed in the RIL / SIM.
     *
     * @return true if the number is an emergency number for the specified country.
     * @hide
     */
    public static boolean isEmergencyNumberExt(int subId, String number,
            String defaultCountryIso, boolean useExactMatch) {
        // If the number passed in is null, just return false:
        // For in coming call case, if network assign private number,
        // it will pass "" to check ECC, so we should also handle "" case.
        if (TextUtils.isEmpty(number)) {
            return false;
        }

        // If the number passed in is a SIP address, return false, since the
        // concept of "emergency numbers" is only meaningful for calls placed
        // over the cell network.
        // (Be sure to do this check *before* calling extractNetworkPortionAlt(),
        // since the whole point of extractNetworkPortionAlt() is to filter out
        // any non-dialable characters (which would turn 'abc911def@example.com'
        // into '911', for example.))
        if (PhoneNumberUtils.isUriNumber(number)) {
            return false;
        }

        // Strip the separators from the number before comparing it
        // to the list.
        number = PhoneNumberUtils.extractNetworkPortionAlt(number);

        dlog("[isEmergencyNumberExt] subId: " + subId + ", iso: "
                + defaultCountryIso + ", useExactMatch: " + useExactMatch);

        // MTK ECC retry by Phone type (GSM/CDMA) START
        if ((subId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID)
                    || (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID)) {
            int queryPhoneType = getQueryPhoneType(subId);

            // Query if GSM ECC
            if ((queryPhoneType & PhoneConstants.PHONE_TYPE_GSM) != 0
                    && isEmergencyNumberExt(number, PhoneConstants.PHONE_TYPE_GSM, subId)) {
                return true;
            }

            // Query if CDMA ECC
            if ((queryPhoneType & PhoneConstants.PHONE_TYPE_CDMA) != 0
                    && isEmergencyNumberExt(number, PhoneConstants.PHONE_TYPE_CDMA, subId)) {
                return true;
            }
            // MTK ECC retry by Phone type END
        } else {
            // Query current phone by type
            int queryPhoneType = TelephonyManager.getDefault().getCurrentPhoneType(subId);
            boolean ret = false;
            if (queryPhoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                ret = isEmergencyNumberExt(number, PhoneConstants.PHONE_TYPE_CDMA, subId);
            } else {
                // Query GSM ECC for all other phone type except CDMA phone (IMS/SIP phone)
                ret = isEmergencyNumberExt(number, PhoneConstants.PHONE_TYPE_GSM, subId);
            }
            if (ret) {
                return true;
            }
        }

        // AOSP ECC check by country ISO (Local emergency number)
        // ECC may conflict with MMI code like (*112#) because ShortNumberUtil
        // will remove non digit chars before match ECC, so we add MMI code check
        // before match ECC by ISO
        if (defaultCountryIso != null && shouldCheckGoogleEcc(defaultCountryIso) &&
                !isMmiCode(number)) {
            ShortNumberInfo info = ShortNumberInfo.getInstance();
            boolean ret = false;
            if (useExactMatch) {
                ret = info.isEmergencyNumber(number, defaultCountryIso);
            } else {
                ret = info.connectsToEmergencyNumber(number, defaultCountryIso);
            }
            dlog("[isEmergencyNumberExt] AOSP check return: " +
                    ret + ", iso: " + defaultCountryIso + ", useExactMatch: " + useExactMatch);
            return ret;
        }

        dlog("[isEmergencyNumber] no match ");
        return false;
    }

    private static boolean shouldCheckGoogleEcc(String iso) {
        for (int i = 0; i < COUNTRIES_NOT_USE_ECC_LIB.length; i++) {
            if (iso.equals(COUNTRIES_NOT_USE_ECC_LIB[i])) {
                dlog("[shouldCheckGoogleEcc] should not check for iso: " + iso);
                return false;
            }
        }
        return true;
    }

    /**
     * Checks a given number against the list of
     * emergency numbers provided by the RIL and SIM card.
     *
     * @param number the number to look up.
     * @param phoneType CDMA or GSM for checking different ECC list.
     * @return true if the number is in the list of emergency numbers
     *         listed in the RIL / SIM, otherwise return false.
     */
    public static boolean isEmergencyNumberExt(String number, int phoneType) {
        dlog("[isEmergencyNumberExt] phoneType:" + phoneType);
        return isEmergencyNumberExt(number, phoneType,
            SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
    }

    /**
     * Checks a given number against the list of
     * emergency numbers provided by the RIL and SIM card by sub id.
     *
     * @param number the number to look up.
     * @param phoneType CDMA or GSM for checking different ECC list.
     * @param subId sub id to query.
     * @return true if the number is in the list of emergency numbers
     *         listed in the RIL / SIM, otherwise return false.
     * @hide
     */
    public static boolean isEmergencyNumberExt(String number, int phoneType, int subId) {
        vlog("[isEmergencyNumberExt], number:" + number + ", phoneType:" + phoneType);

        // Vzw special requirement
        if (isHighPriorityAccessEmergencyNumber(number)) {
            return true;
        }

        // Performance enhance for phone number
        if (number.length() >= getMinMatch()) {
            return false;
        }

        for (EccSource es : sAllEccSource) {
            if (es.isEmergencyNumber(number, subId, phoneType)) {
                return true;
            }
        }

        dlog("[isEmergencyNumberExt] no match for phoneType: " + phoneType);
        return false;
    }

    /**
     * Check if the dailing number is a special ECC
     *
     * Add for CTA requirement to check if sim insert or not to decide dial using
     * emergency call or normal call for emergency numbers.
     * CTA requirment:
     *    1. For CTA numbers (110, 119, 122,120), should always display ECC UI
     *    2. Dial using normal call when SIM insert and ECC call when No SIM.
     * Here we use SIM ecc reported by MD to decide if SIM is insert or not which
     * is the same as the logic in isEmergencyNumber().
     * @param dialString dailing number string.
     * @return true if it is a special ECC.
     */
    public static boolean isSpecialEmergencyNumber(String dialString) {
        /* Special emergency number will show ecc in MMI but sent to nw as normal call */
        return isSpecialEmergencyNumber(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID, dialString);
    }

    /**
     * Check if the dailing number is a special ECC
     *
     * Add for CTA requirement to check if sim insert or not to decide dial using
     * emergency call or normal call for emergency numbers.
     * CTA requirment:
     *    1. For CTA numbers (110, 119, 122,120), should always display ECC UI
     *    2. Dial using normal call when SIM insert and ECC call when No SIM.
     * Here we use SIM ecc reported by MD to decide if SIM is insert or not which
     * is the same as the logic in isEmergencyNumber().
     * @param dialString dailing number string.
     * @return true if it is a special ECC.
     * @hide
     */
    public static boolean isSpecialEmergencyNumber(int subId, String dialString) {
        /* Special emergency number will show ecc in MMI but sent to nw as normal call */
        if (sNetworkEcc.isEmergencyNumber(dialString, subId, PhoneConstants.PHONE_TYPE_GSM) ||
                sSimEcc.isEmergencyNumber(dialString, subId, PhoneConstants.PHONE_TYPE_GSM)) {
                // If network or SIM ecc, should not treat as special emergency number
                return false;
        }

        for (EccSource es : sAllEccSource) {
            if (es.isSpecialEmergencyNumber(subId, dialString)) {
                return true;
            }
        }

        log("[isSpecialEmergencyNumber] not special ecc");
        return false;
    }

    /**
     * Get Ecc List which will be sync to MD.
     * Will be phase out on 93MD
     *
     * @param none.
     * @return Ecc List with type ArrayList<String>.
     * @hide
     */
    public static ArrayList<String> getEccList() {
        // Currently we only sync xml/property/cta ecc list to MD
        ArrayList<EccEntry> resList = new ArrayList<EccEntry>();

        // Add the ECC list according to priority (CTA > Prop > XML)
        // Because MD can only support max 15 entries. For the entries
        // which more than 15 will be dropped.
        if (sIsCtaSet) {
            sCtaEcc.addToEccList(resList);
        }
        sPropertyEcc.addToEccList(resList);
        sXmlEcc.addToEccList(resList);
        dlog("[getEccList] ECC list: " + resList);

        // TO MD AT command format (+EECCUD)
        int numToSync = resList.size() > MAX_ECC_NUM_TO_MD_TOTAL ?
                MAX_ECC_NUM_TO_MD_TOTAL : resList.size();
        ArrayList<String> resStringList = new ArrayList<String>();
        int loop = 0;
        do {
            int count = numToSync > MAX_ECC_NUM_TO_MD_PER_AT ? MAX_ECC_NUM_TO_MD_PER_AT: numToSync;
            String syncAtString = count + "";
            for (int i = 0; i < count; i++) {
                EccEntry entry = resList.get(i + loop * MAX_ECC_NUM_TO_MD_PER_AT);
                if (entry != null) {
                    syncAtString += ",\"" + entry.getEcc() + "\""; // ECC
                    syncAtString += "," + entry.getCategory(); // Service category
                    String strCondition = entry.getCondition();
                    if (strCondition.equals(EccEntry.ECC_FOR_MMI)
                            || !TextUtils.isEmpty(entry.getPlmn())) {
                        strCondition = EccEntry.ECC_NO_SIM;
                    }
                    syncAtString += "," + strCondition; // Condition
                }
            }
            dlog("[getEccList] syncAtString: " + syncAtString);
            resStringList.add(syncAtString);
            loop++;
        } while ((numToSync -= MAX_ECC_NUM_TO_MD_PER_AT) > 0);
        return resStringList;
    }

    public static String getSpecialEccList() {
        String specialEccList = "";
        if (sXmlEcc == null) {
            return "";
        }

        for (EccEntry eccEntry : sXmlEcc.mEccList) {
            if (eccEntry.getCondition().equals(EccEntry.ECC_FOR_MMI)) {
                specialEccList += eccEntry.getEcc() + "," + eccEntry.getPlmn() + ",";
            }
        }
        dlog("[DBG]getSpecialEccList: " + specialEccList);
        return specialEccList;
    }

    /**
     * Set specific ECC category.
     *
     * @param eccCat represent a setted specific ECC category
     * @hide
     */
    public static void setSpecificEccCategory(int eccCat) {
        log("[setSpecificEccCategory] set ECC category: " + eccCat);
        sSpecificEccCat = eccCat;
    }

    /**
     * Get the service category for the given ECC number.
     * @param number The ECC number.
     * @return The service category for the given number.
     * @hide
     */
    public static int getServiceCategoryFromEcc(String number) {
        return getServiceCategoryFromEccBySubId(number,
                SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
    }

    /**
     * Get the service category for the given ECC number.
     * @param number The ECC number.
     * @param subId  The sub id to query
     * @return The service category for the given number.
     * @hide
     */
    public static int getServiceCategoryFromEccBySubId(String number, int subId) {
        /// M: support for release 12, specific ECC category from NW. @{
        if (sSpecificEccCat >= 0) {
            log("[getServiceCategoryFromEccBySubId] specific ECC category: " + sSpecificEccCat);
            int eccCat = sSpecificEccCat;
            sSpecificEccCat = -1; // reset specific ecc category
            return eccCat;
        }
        /// @}

        for (EccSource es : sAllEccSource) {
            int category = es.getServiceCategory(number, subId);
            if (category > 0) {
                return category;
            }
        }

        log("[getServiceCategoryFromEccBySubId] no matched subId: " + subId);
        return 0;
    }

    private static int getQueryPhoneType(int subId) {
        int simNum = TelephonyManager.getDefault().getPhoneCount();
        boolean needQueryGsm = false;
        boolean needQueryCdma = false;

        // Don't check CDMA ECC for:
        // 1. GSM FTA mode (CTA=1, FTA=2, IOT=3)
        // 2. Network don't support C2K
        if (SystemProperties.getInt("vendor.gsm.gcf.testmode", 0) == 2 ||
                !isNetworkSupportCdma()) {
            return PhoneConstants.PHONE_TYPE_GSM;
        }

        // Only Query GSM and CDMA for C2K Project to enhance performance
        if (sIsC2kSupport) {
            for (int i = 0; i < simNum; i++) {
                int phoneType = TelephonyManager.getDefault().getCurrentPhoneTypeForSlot(i);
                if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                    needQueryGsm = true;
                } else if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    needQueryCdma = true;
                }
            }

            // If no SIM insert in all slot, should query GSM
            // Example: SS load with CDMAPhone type only, it will query
            // CDMA only which cause the 3GPP No SIM ECC(000,08,118)
            // can't dial out as ECC even there is no SIM insert.
            if (!needQueryGsm && !isSimInsert()) {
                needQueryGsm = true;
            }
        } else {
            // For GSM only project, always query GSM phone ECC only
            // to enhance performance.
            needQueryGsm = true;
        }

        // For ECC new design for N Denali+
        // Case: Insert G+G card, then remove both SIMs, the phone type
        // will be GSM phone and we'll query GSM ECC only, but in fact
        // in this case, we may call ECC through CDMA
        if (sIsC2kSupport && simNum > 1 && !needQueryCdma) {
            boolean isRoaming = false;
            int[] iccTypes = new int[simNum];
            for (int i = 0; i < simNum; i++) {
                try {
                    iccTypes[i] = MtkTelephonyManagerEx.getDefault().getIccAppFamily(i);
                } catch (NullPointerException ex) {
                    log("getIccAppFamily, NullPointerException:" + ex);
                }
            }
            for (int i = 0; i < simNum; i++) {
                if (iccTypes[i] >= 0x02 || isCt3gDualModeCard(i)) {
                    log("[getQueryPhoneType] Slot" + i + " is roaming");
                    isRoaming = true;
                    break;
                }
            }
            if (!isRoaming) {
                for (int i = 0; i < simNum; i++) {
                    if (iccTypes[i] == 0x00) {
                        vlog("[getQueryPhoneType] Slot" + i + " no card");
                        needQueryCdma = true;
                        break;
                    }
                }
            }
        }

        int phoneTypeRet = 0;
        if (needQueryGsm) {
            phoneTypeRet |= PhoneConstants.PHONE_TYPE_GSM;
        }
        if (needQueryCdma) {
            phoneTypeRet |= PhoneConstants.PHONE_TYPE_CDMA;
        }
        vlog("[getQueryPhoneType] needQueryGsm:" + needQueryGsm
                + ", needQueryCdma:" + needQueryCdma + ", ret: " + phoneTypeRet);
        return phoneTypeRet;
    }

    private static boolean isCt3gDualModeCard(int slotId) {
        final String[] ct3gProp = {
            "vendor.gsm.ril.ct3g",
            "vendor.gsm.ril.ct3g.2",
            "vendor.gsm.ril.ct3g.3",
            "vendor.gsm.ril.ct3g.4",
        };
        if (slotId < 0 || slotId >= ct3gProp.length) {
            return false;
        }
        return "1".equals(SystemProperties.get(ct3gProp[slotId]));
    }

    /**
     * Returns phone number minimum match length.
     */
    public static int getMinMatch() {
        // getMinMatch may called before initialize so init before using
        sIsCtaSupport = "1".equals(SystemProperties.get("ro.vendor.mtk_cta_support"));
        sIsOP09Support = "OP09".equals(SystemProperties.get("persist.vendor.operator.optr"))
                && ("SEGDEFAULT".equals(SystemProperties.get("persist.vendor.operator.seg"))
                || "SEGC".equals(SystemProperties.get("persist.vendor.operator.seg")));
        if (sIsOP09Support || sIsCtaSupport) {
            vlog("[DBG] getMinMatch return 11 for CTA/OP09");
            return MIN_MATCH_CTA;
        } else {
            vlog("[DBG] getMinMatch return 7");
            return MIN_MATCH;
        }
    }

    private static boolean isMmiCode(String number) {
        // See TS 22.030 6.5.2 "Structure of the MMI"
        Pattern p = Pattern.compile(
                "((\\*|#|\\*#|\\*\\*|##)(\\d{2,3})(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*))?)?)?)?#)(.*)");
        Matcher m = p.matcher(number);
        // TS 22.230 sec 6.5.3.2
        // "Entry of any characters defined in the 3GPP TS 23/038 [8] Default Alphabet
        // (up to the maxium defined in 3GPP TS 24.080 [10]), followed by #SEND".
        if (m.matches() || number.endsWith("#")) {
            return true;
        }
        return false;
    }

    private static boolean isSimInsert() {
        int simNum = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < simNum; i++) {
            if (TelephonyManager.getDefault().hasIccCard(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns Default voice subscription Id.
     */
    public static int getDefaultVoiceSubId() {
        // To support MTK ECC retry
        return SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
    }

    public static boolean isHighPriorityAccessEmergencyNumber(String number) {
        if (!("OP12".equals(SystemProperties.get("persist.vendor.operator.optr")))) {
            return false;
        }

        Pattern p = Pattern.compile("^[*|#]272[*|#]{0,1}911$");
        Matcher m = p.matcher(number);
        if (m.matches()) {
            dlog("[isHighPriorityAccessEmergencyNumber] return true");
            return true;
        }
        return false;
    }

    private static boolean isNetworkSupportCdma() {
        if (!sIsC2kSupport) {
            return false;
        }
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            String plmn = TelephonyManager.getDefault().getNetworkOperatorForPhone(i);
            if (TextUtils.isEmpty(plmn) || plmn.length() < 3) {
                continue;
            }
            for (int j = 0; j < PLMN_NO_C2K.length; j++) {
                if (plmn.equals(PLMN_NO_C2K[j]) || plmn.substring(0, 3).equals(PLMN_NO_C2K[j])) {
                    vlog("isNetworkSupportCdma() false, plmn = " + plmn);
                    return false;
                }
            }
        }
        return true;
    }

    // Phone Number ECC API revise END

    // print log in all load (eng/user/userdebug)
    private static void log(String msg) {
        Rlog.i(LOG_TAG, msg);
    }

    // print log only in eng load
    private static void dlog(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    // need to turn on VDBG manually before print log
    private static void vlog(String msg) {
        if (VDBG) {
            dlog(msg);
        }
    }

    // MTK Added END
}
