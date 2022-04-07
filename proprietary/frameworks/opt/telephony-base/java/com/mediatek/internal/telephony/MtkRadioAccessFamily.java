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
package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.telephony.RILConstants;
import com.mediatek.internal.telephony.MtkRILConstants;


/**
 * Object to indicate the phone radio type and access technology.
 *
 * @hide
 */
public class MtkRadioAccessFamily {

    // Radio Access Family
    // 2G
    public static final int RAF_UNKNOWN = (1 <<  ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN);
    public static final int RAF_GSM = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_GSM);
    public static final int RAF_GPRS = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_GPRS);
    public static final int RAF_EDGE = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_EDGE);
    public static final int RAF_IS95A = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_IS95A);
    public static final int RAF_IS95B = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_IS95B);
    public static final int RAF_1xRTT = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_1xRTT);
    // 3G
    public static final int RAF_EVDO_0 = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_0);
    public static final int RAF_EVDO_A = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_A);
    public static final int RAF_EVDO_B = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_B);
    public static final int RAF_EHRPD = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD);
    public static final int RAF_HSUPA = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_HSUPA);
    public static final int RAF_HSDPA = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_HSDPA);
    public static final int RAF_HSPA = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_HSPA);
    public static final int RAF_HSPAP = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_HSPAP);
    public static final int RAF_UMTS = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_UMTS);
    public static final int RAF_TD_SCDMA = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_TD_SCDMA);
    // 4G
    public static final int RAF_LTE = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_LTE);
    public static final int RAF_LTE_CA = (1 << ServiceState.RIL_RADIO_TECHNOLOGY_LTE_CA);

    // Grouping of RAFs
    // 2G
    private static final int GSM = RAF_GSM | RAF_GPRS | RAF_EDGE;
    private static final int CDMA = RAF_IS95A | RAF_IS95B | RAF_1xRTT;
    // 3G
    private static final int EVDO = RAF_EVDO_0 | RAF_EVDO_A | RAF_EVDO_B | RAF_EHRPD;
    private static final int HS = RAF_HSUPA | RAF_HSDPA | RAF_HSPA | RAF_HSPAP;
    private static final int WCDMA = HS | RAF_UMTS | RAF_TD_SCDMA;
    // 4G
    private static final int LTE = RAF_LTE | RAF_LTE_CA;

    public static int getRafFromNetworkType(int type) {
        int raf;

        switch (type) {
            case RILConstants.NETWORK_MODE_WCDMA_PREF:
                raf = GSM | WCDMA;
                break;
            case RILConstants.NETWORK_MODE_GSM_ONLY:
                raf = GSM;
                break;
            case RILConstants.NETWORK_MODE_WCDMA_ONLY:
                raf = WCDMA;
                break;
            case RILConstants.NETWORK_MODE_GSM_UMTS:
                raf = GSM | WCDMA;
                break;
            case RILConstants.NETWORK_MODE_CDMA:
                raf = CDMA | EVDO;
                break;
            case RILConstants.NETWORK_MODE_LTE_CDMA_EVDO:
                raf = LTE | CDMA | EVDO;
                break;
            case RILConstants.NETWORK_MODE_LTE_GSM_WCDMA:
                raf = LTE | GSM | WCDMA;
                break;
            case RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                raf = LTE | CDMA | EVDO | GSM | WCDMA;
                break;
            case RILConstants.NETWORK_MODE_LTE_ONLY:
                raf = LTE;
                break;
            case RILConstants.NETWORK_MODE_LTE_WCDMA:
                raf = LTE | WCDMA;
                break;
            case RILConstants.NETWORK_MODE_CDMA_NO_EVDO:
                raf = CDMA;
                break;
            case RILConstants.NETWORK_MODE_EVDO_NO_CDMA:
                raf = EVDO;
                break;
            case RILConstants.NETWORK_MODE_GLOBAL:
                raf = GSM | WCDMA | CDMA | EVDO;
                break;
            case RILConstants.NETWORK_MODE_TDSCDMA_ONLY:
                raf = RAF_TD_SCDMA;
                break;
            case RILConstants.NETWORK_MODE_TDSCDMA_WCDMA:
                raf = RAF_TD_SCDMA | WCDMA;
                break;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA:
                raf = LTE | RAF_TD_SCDMA;
                break;
            case RILConstants.NETWORK_MODE_TDSCDMA_GSM:
                raf = RAF_TD_SCDMA | GSM;
                break;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM:
                raf = LTE | RAF_TD_SCDMA | GSM;
                break;
            case RILConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
                raf = RAF_TD_SCDMA | GSM | WCDMA;
                break;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
                raf = LTE | RAF_TD_SCDMA | WCDMA;
                break;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
                raf = LTE | RAF_TD_SCDMA | GSM | WCDMA;
                break;
            case RILConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                raf = RAF_TD_SCDMA | CDMA | EVDO | GSM | WCDMA;
                break;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                raf = LTE | RAF_TD_SCDMA | CDMA | EVDO | GSM | WCDMA;
                break;
            case MtkRILConstants.NETWORK_MODE_LTE_GSM:
                raf = LTE | GSM;
                break;
            case MtkRILConstants.NETWORK_MODE_LTE_TDD_ONLY:
                raf = LTE;
                break;
            case MtkRILConstants.NETWORK_MODE_CDMA_GSM:
                raf = CDMA | GSM;
                break;
            case MtkRILConstants.NETWORK_MODE_CDMA_EVDO_GSM:
                raf = CDMA | EVDO | GSM;
                break;
            case MtkRILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM:
                raf = LTE | CDMA | EVDO | GSM;
                break;
            default:
                raf = RAF_UNKNOWN;
                break;
        }

        return raf;
    }

    /**
     * if the raf includes ANY bit set for a group
     * adjust it to contain ALL the bits for that group
     */
    private static int getAdjustedRaf(int raf) {
        raf = ((GSM & raf) > 0) ? (GSM | raf) : raf;
        raf = ((WCDMA & raf) > 0) ? (WCDMA | raf) : raf;
        raf = ((CDMA & raf) > 0) ? (CDMA | raf) : raf;
        raf = ((EVDO & raf) > 0) ? (EVDO | raf) : raf;
        raf = ((LTE & raf) > 0) ? (LTE | raf) : raf;

        return raf;
    }

    public static int getNetworkTypeFromRaf(int raf) {
        int type;

        raf = getAdjustedRaf(raf);

        switch (raf) {
            case (GSM | WCDMA):
                type = RILConstants.NETWORK_MODE_WCDMA_PREF;
                break;
            case GSM:
                type = RILConstants.NETWORK_MODE_GSM_ONLY;
                break;
            case WCDMA:
                type = RILConstants.NETWORK_MODE_WCDMA_ONLY;
                break;
            case (CDMA | EVDO):
                type = RILConstants.NETWORK_MODE_CDMA;
                break;
            case (LTE | CDMA | EVDO):
                type = RILConstants.NETWORK_MODE_LTE_CDMA_EVDO;
                break;
            case (LTE | GSM | WCDMA):
                type = RILConstants.NETWORK_MODE_LTE_GSM_WCDMA;
                break;
            case (LTE | CDMA | EVDO | GSM | WCDMA):
                type = RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
                break;
            case LTE:
                type = RILConstants.NETWORK_MODE_LTE_ONLY;
                break;
            case (LTE | WCDMA):
                type = RILConstants.NETWORK_MODE_LTE_WCDMA;
                break;
            case CDMA:
                type = RILConstants.NETWORK_MODE_CDMA_NO_EVDO;
                break;
            case EVDO:
                type = RILConstants.NETWORK_MODE_EVDO_NO_CDMA;
                break;
            case (GSM | WCDMA | CDMA | EVDO):
                type = RILConstants.NETWORK_MODE_GLOBAL;
                break;
            case RAF_TD_SCDMA:
                type = RILConstants.NETWORK_MODE_TDSCDMA_ONLY;
                break;
            case (LTE | RAF_TD_SCDMA):
                type = RILConstants.NETWORK_MODE_LTE_TDSCDMA;
                break;
            case (RAF_TD_SCDMA | GSM):
                type = RILConstants.NETWORK_MODE_TDSCDMA_GSM;
                break;
            case (LTE | RAF_TD_SCDMA | GSM):
                type = RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM;
                break;
            case (LTE | GSM):
                type = MtkRILConstants.NETWORK_MODE_LTE_GSM;
                break;
            case (CDMA | GSM):
                type = MtkRILConstants.NETWORK_MODE_CDMA_GSM;
                break;
            case (CDMA | EVDO | GSM):
                type = MtkRILConstants.NETWORK_MODE_CDMA_EVDO_GSM;
                break;
            case (LTE | CDMA | EVDO | GSM):
                type = MtkRILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM;
                break;
            default:
                type = RILConstants.PREFERRED_NETWORK_MODE;
                break;
        }

        return type;
    }
}
