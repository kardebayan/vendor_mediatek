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

import com.android.internal.telephony.RILConstants;

public interface MtkRILConstants extends RILConstants {

    /// M: [Network][C2K]Add the MTK new network type. @{
    int NETWORK_MODE_LTE_GSM        = 30; /*LTE/GSM */
    int NETWORK_MODE_LTE_TDD_ONLY   = 31; /* LTE TDD Only mode. */
    int NETWORK_MODE_CDMA_GSM                         = 32; /* CDMA,GSM(2G Global) */
    int NETWORK_MODE_CDMA_EVDO_GSM                    = 33; /* CDMA,EVDO,GSM */
    int NETWORK_MODE_LTE_CDMA_EVDO_GSM                = 34; /* LTE,CDMA,EVDO,GSM(4G Global, 4M) */
    /// @}

    // MTK-START, SIM power
    public static final int SIM_POWER_OFF   = 0;
    public static final int SIM_POWER_ON    = 1;
    public static final int SIM_POWER_RESET = 2;
    // MTK-END, SIM power

    // PHB START
    /* PHB Storage type, PHB_XDN*/
    int PHB_ADN = 0;
    int PHB_FDN = 1;
    int PHB_MSISDN = 2;
    int PHB_ECC = 3;

    /* Max PHB entryies to be read at once,
        Refer to RIL_MAX_PHB_ENTRY defined in the ril_phb.h */
    int PHB_MAX_ENTRY = 10;
    // PHB END

    // Carrier Express
    int CXP_CONFIG_TYPE_OPTR  = 0;
    int CXP_CONFIG_TYPE_SPEC  = 1;
    int CXP_CONFIG_TYPE_SEG   = 2;
    int CXP_CONFIG_TYPE_SBP   = 3;
    int CXP_CONFIG_TYPE_SUBID = 4;

    // M: VDF MMS over ePDG @{
    public static final int DATA_PROFILE_MMS = DATA_PROFILE_OEM_BASE + 1;
    public static final int DATA_PROFILE_SUPL = DATA_PROFILE_OEM_BASE + 2;
    public static final int DATA_PROFILE_HIPRI = DATA_PROFILE_OEM_BASE + 3;
    public static final int DATA_PROFILE_WAP = DATA_PROFILE_OEM_BASE + 4;
    public static final int DATA_PROFILE_EMERGENCY = DATA_PROFILE_OEM_BASE + 5;
    public static final int DATA_PROFILE_XCAP = DATA_PROFILE_OEM_BASE + 6;
    public static final int DATA_PROFILE_RCS = DATA_PROFILE_OEM_BASE + 7;
    /// @}
    public static final int DATA_PROFILE_BIP = DATA_PROFILE_OEM_BASE + 8;
    public static final int DATA_PROFILE_VSIM = DATA_PROFILE_OEM_BASE + 9;
    public static final int DATA_PROFILE_ALL = DATA_PROFILE_OEM_BASE + 10;

    /**
     * M: Deactivate data call reasons
     * some of the reasons are passed to modem via EAPNACT, and therefore
     * please add new causes incrementally and do not modify or remove existing causes
     */
    public static final int DEACTIVATE_REASON_BASE = 2000;
    public static final int DEACTIVATE_REASON_NORMAL = DEACTIVATE_REASON_BASE + 1;
    public static final int DEACTIVATE_REASON_RA_INITIAL_FAIL = DEACTIVATE_REASON_BASE + 2;
    public static final int DEACTIVATE_REASON_NO_PCSCF = DEACTIVATE_REASON_BASE + 3;
    public static final int DEACTIVATE_REASON_RA_REFRESH_FAIL = DEACTIVATE_REASON_BASE + 4;
    public static final int DEACTIVATE_REASON_APN_CHANGED = DEACTIVATE_REASON_BASE + 5;

    /* key of SIM mode setting*/
    public static final String MSIM_MODE_SETTING = "msim_mode_setting";

    /*********************************************************************************/
    /*  Vendor request                                                               */
    /*********************************************************************************/
    int RIL_REQUEST_VENDOR_BASE = 2000;

    int RIL_REQUEST_RESUME_REGISTRATION = (RIL_REQUEST_VENDOR_BASE + 0);

    // MTK-START: SIM
    int RIL_REQUEST_SIM_GET_ATR = (RIL_REQUEST_VENDOR_BASE + 1);
    int RIL_REQUEST_SET_SIM_POWER = (RIL_REQUEST_VENDOR_BASE + 2);

    // modem power
    int RIL_REQUEST_MODEM_POWERON = (RIL_REQUEST_VENDOR_BASE + 3);
    int RIL_REQUEST_MODEM_POWEROFF = (RIL_REQUEST_VENDOR_BASE + 4);

    // MTK-END
    int RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL_WITH_ACT = (RIL_REQUEST_VENDOR_BASE + 5);
    int RIL_REQUEST_QUERY_AVAILABLE_NETWORKS_WITH_ACT = (RIL_REQUEST_VENDOR_BASE + 6);
    int RIL_REQUEST_ABORT_QUERY_AVAILABLE_NETWORKS = (RIL_REQUEST_VENDOR_BASE + 7);

    // ATCI
    int RIL_REQUEST_OEM_HOOK_ATCI_INTERNAL = (RIL_REQUEST_VENDOR_BASE + 8);

    int RIL_REQUEST_GSM_SET_BROADCAST_LANGUAGE = (RIL_REQUEST_VENDOR_BASE + 9);
    int RIL_REQUEST_GSM_GET_BROADCAST_LANGUAGE = (RIL_REQUEST_VENDOR_BASE + 10);
    int RIL_REQUEST_GET_SMS_SIM_MEM_STATUS = (RIL_REQUEST_VENDOR_BASE + 11);
    int RIL_REQUEST_GET_SMS_PARAMS = (RIL_REQUEST_VENDOR_BASE + 12);
    int RIL_REQUEST_SET_SMS_PARAMS = (RIL_REQUEST_VENDOR_BASE + 13);
    int RIL_REQUEST_SET_ETWS = (RIL_REQUEST_VENDOR_BASE + 14);
    int RIL_REQUEST_REMOVE_CB_MESSAGE = (RIL_REQUEST_VENDOR_BASE + 15);

    /// M: CC: Proprietary incoming call handling
    int RIL_REQUEST_SET_CALL_INDICATION = (RIL_REQUEST_VENDOR_BASE + 16);
    /// M: CC: Proprietary ECC enhancement @{
    int RIL_REQUEST_EMERGENCY_DIAL = RIL_REQUEST_VENDOR_BASE + 17;
    int RIL_REQUEST_SET_ECC_SERVICE_CATEGORY = RIL_REQUEST_VENDOR_BASE + 18;
    /// @}
    /// M: CC: HangupAll for FTA 31.4.4.2
    /**
     * "RIL_REQUEST_HANGUP_ALL"
     * Hang up all (like ATH, but use AT+CHLD=6 to prevent channel limitation)
     * For ATH, the channel usd to setup call and release must be the same.
     * AT+CHLD=6 has no such limitation
     */
    int RIL_REQUEST_HANGUP_ALL = RIL_REQUEST_VENDOR_BASE + 19;

    int RIL_REQUEST_SET_PS_REGISTRATION = (RIL_REQUEST_VENDOR_BASE + 20);

    /// M: APC. @{
    int RIL_REQUEST_SET_PSEUDO_CELL_MODE = (RIL_REQUEST_VENDOR_BASE + 21);
    int RIL_REQUEST_GET_PSEUDO_CELL_INFO = (RIL_REQUEST_VENDOR_BASE + 22);
    /// @}

    int RIL_REQUEST_SWITCH_MODE_FOR_ECC = (RIL_REQUEST_VENDOR_BASE + 23);
    int RIL_REQUEST_GET_SMS_RUIM_MEM_STATUS = RIL_REQUEST_VENDOR_BASE + 24;

    // Fast Dormancy
    int RIL_REQUEST_SET_FD_MODE = RIL_REQUEST_VENDOR_BASE + 25;

    // World Phone
    int RIL_REQUEST_RELOAD_MODEM_TYPE = (RIL_REQUEST_VENDOR_BASE + 26);
    int RIL_REQUEST_STORE_MODEM_TYPE = (RIL_REQUEST_VENDOR_BASE + 27);
    int RIL_REQUEST_SET_TRM = RIL_REQUEST_VENDOR_BASE + 28;
    // STK
    int RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM_WITH_RESULT_CODE
            = (RIL_REQUEST_VENDOR_BASE + 29);

    // Set ECC list to MD
    int RIL_REQUEST_SET_ECC_LIST = RIL_REQUEST_VENDOR_BASE + 30;

    int RIL_REQUEST_FORCE_RELEASE_CALL = (RIL_REQUEST_VENDOR_BASE + 34);

    /// M: CC: Vzw/CTVolte ECC @{
    int RIL_REQUEST_CURRENT_STATUS = (RIL_REQUEST_VENDOR_BASE + 35);
    /// @}

    // PHB Start (2036-2054)
    int RIL_REQUEST_QUERY_PHB_STORAGE_INFO = (RIL_REQUEST_VENDOR_BASE + 36);
    int RIL_REQUEST_WRITE_PHB_ENTRY = (RIL_REQUEST_VENDOR_BASE + 37);
    int RIL_REQUEST_READ_PHB_ENTRY = (RIL_REQUEST_VENDOR_BASE + 38);
    int RIL_REQUEST_QUERY_UPB_CAPABILITY = (RIL_REQUEST_VENDOR_BASE + 39);
    int RIL_REQUEST_EDIT_UPB_ENTRY = (RIL_REQUEST_VENDOR_BASE + 40);
    int RIL_REQUEST_DELETE_UPB_ENTRY = (RIL_REQUEST_VENDOR_BASE + 41);
    int RIL_REQUEST_READ_UPB_GAS_LIST = (RIL_REQUEST_VENDOR_BASE + 42);
    int RIL_REQUEST_READ_UPB_GRP = (RIL_REQUEST_VENDOR_BASE + 43);
    int RIL_REQUEST_WRITE_UPB_GRP = (RIL_REQUEST_VENDOR_BASE + 44);
    int RIL_REQUEST_GET_PHB_STRING_LENGTH = (RIL_REQUEST_VENDOR_BASE + 45);
    int RIL_REQUEST_GET_PHB_MEM_STORAGE = (RIL_REQUEST_VENDOR_BASE + 46);
    int RIL_REQUEST_SET_PHB_MEM_STORAGE = (RIL_REQUEST_VENDOR_BASE + 47);
    int RIL_REQUEST_READ_PHB_ENTRY_EXT = (RIL_REQUEST_VENDOR_BASE + 48);
    int RIL_REQUEST_WRITE_PHB_ENTRY_EXT = (RIL_REQUEST_VENDOR_BASE + 49);
    int RIL_REQUEST_QUERY_UPB_AVAILABLE = (RIL_REQUEST_VENDOR_BASE + 50);
    int RIL_REQUEST_READ_EMAIL_ENTRY = (RIL_REQUEST_VENDOR_BASE + 51);
    int RIL_REQUEST_READ_SNE_ENTRY = (RIL_REQUEST_VENDOR_BASE + 52);
    int RIL_REQUEST_READ_ANR_ENTRY = (RIL_REQUEST_VENDOR_BASE + 53);
    int RIL_REQUEST_READ_UPB_AAS_LIST = (RIL_REQUEST_VENDOR_BASE + 54);
    // PHB End

    // Femtocell (CSG)
    int RIL_REQUEST_GET_FEMTOCELL_LIST  = (RIL_REQUEST_VENDOR_BASE + 55);
    int RIL_REQUEST_ABORT_FEMTOCELL_LIST = (RIL_REQUEST_VENDOR_BASE + 56);
    int RIL_REQUEST_SELECT_FEMTOCELL = (RIL_REQUEST_VENDOR_BASE + 57);
    int RIL_REQUEST_QUERY_FEMTOCELL_SYSTEM_SELECTION_MODE = (RIL_REQUEST_VENDOR_BASE + 58);
    int RIL_REQUEST_SET_FEMTOCELL_SYSTEM_SELECTION_MODE = (RIL_REQUEST_VENDOR_BASE + 59);

    /// M: eMBMS feature
    int RIL_REQUEST_EMBMS_AT_CMD = (RIL_REQUEST_VENDOR_BASE + 60);
    /// M: eMBMS end

    int RIL_REQUEST_SYNC_APN_TABLE = (RIL_REQUEST_VENDOR_BASE + 61);

    // M: Data Framework - common part enhancement
    int RIL_REQUEST_SYNC_DATA_SETTINGS_TO_MD = (RIL_REQUEST_VENDOR_BASE + 62);
    // M: Data Framework - Data Retry enhancement
    int RIL_REQUEST_RESET_MD_DATA_RETRY_COUNT = (RIL_REQUEST_VENDOR_BASE + 63);

    // MTK-START: SIM GBA / AUTH
    int RIL_REQUEST_GENERAL_SIM_AUTH = (RIL_REQUEST_VENDOR_BASE + 64);
    // MTK-END
    // M: [LTE][Low Power][UL traffic shaping] @{
    int RIL_REQUEST_SET_LTE_ACCESS_STRATUM_REPORT = RIL_REQUEST_VENDOR_BASE + 65;
    int RIL_REQUEST_SET_LTE_UPLINK_DATA_TRANSFER = RIL_REQUEST_VENDOR_BASE + 66;
    // M: [LTE][Low Power][UL traffic shaping] @}

    // MTK-START: SIM ME LOCK
    int RIL_REQUEST_QUERY_SIM_NETWORK_LOCK = (RIL_REQUEST_VENDOR_BASE + 67);
    int RIL_REQUEST_SET_SIM_NETWORK_LOCK = (RIL_REQUEST_VENDOR_BASE + 68);
    // MTK-END

    /// [IMS] IMS RIL_REQUEST ==============================================================
    int RIL_REQUEST_SET_IMS_ENABLE = RIL_REQUEST_VENDOR_BASE + 69;
    int RIL_REQUEST_SET_VOLTE_ENABLE = RIL_REQUEST_VENDOR_BASE + 70;
    int RIL_REQUEST_SET_WFC_ENABLE = RIL_REQUEST_VENDOR_BASE + 71;
    int RIL_REQUEST_SET_VILTE_ENABLE = RIL_REQUEST_VENDOR_BASE + 72;
    int RIL_REQUEST_SET_VIWIFI_ENABLE = RIL_REQUEST_VENDOR_BASE + 73;
    int RIL_REQUEST_SET_IMS_VOICE_ENABLE = RIL_REQUEST_VENDOR_BASE + 74;
    int RIL_REQUEST_SET_IMS_VIDEO_ENABLE = RIL_REQUEST_VENDOR_BASE + 75;
    int RIL_REQUEST_VIDEO_CALL_ACCEPT = RIL_REQUEST_VENDOR_BASE + 76;
    int RIL_REQUEST_SET_IMSCFG = RIL_REQUEST_VENDOR_BASE + 77;
    /// [IMS] IMS Provision Configs
    int RIL_REQUEST_GET_PROVISION_VALUE = RIL_REQUEST_VENDOR_BASE + 78;
    int RIL_REQUEST_SET_PROVISION_VALUE = RIL_REQUEST_VENDOR_BASE + 79;
    /// [IMS] IMS Bearer Activate/Deactivate
    int RIL_REQUEST_IMS_BEARER_ACTIVATION_DONE = RIL_REQUEST_VENDOR_BASE + 80;
    int RIL_REQUEST_IMS_BEARER_DEACTIVATION_DONE = RIL_REQUEST_VENDOR_BASE + 81;
    int RIL_REQUEST_IMS_DEREG_NOTIFICATION = RIL_REQUEST_VENDOR_BASE + 82;
    // IMS blind/assured ECT
    int RIL_REQUEST_IMS_ECT = RIL_REQUEST_VENDOR_BASE + 83;

    // [IMS] IMS Call
    int RIL_REQUEST_HOLD_CALL = RIL_REQUEST_VENDOR_BASE + 84;
    int RIL_REQUEST_RESUME_CALL = RIL_REQUEST_VENDOR_BASE + 85;
    int RIL_REQUEST_DIAL_WITH_SIP_URI = RIL_REQUEST_VENDOR_BASE + 86;
    // [IMS] Emergency Dial
    int RIL_REQUEST_IMS_EMERGENCY_DIAL = RIL_REQUEST_VENDOR_BASE + 87;

    int RIL_REQUEST_SET_IMS_RTP_REPORT = RIL_REQUEST_VENDOR_BASE + 88;

    // [IMS] IMS Conference Call
    int RIL_REQUEST_CONFERENCE_DIAL = RIL_REQUEST_VENDOR_BASE + 89;
    int RIL_REQUEST_ADD_IMS_CONFERENCE_CALL_MEMBER = RIL_REQUEST_VENDOR_BASE + 90;
    int RIL_REQUEST_REMOVE_IMS_CONFERENCE_CALL_MEMBER = RIL_REQUEST_VENDOR_BASE + 91;
    // [IMS] IMS Video Call
    int RIL_REQUEST_VT_DIAL_WITH_SIP_URI = RIL_REQUEST_VENDOR_BASE + 92;
    // [IMS] USSI
    int RIL_REQUEST_SEND_USSI = RIL_REQUEST_VENDOR_BASE + 93;
    int RIL_REQUEST_CANCEL_USSI = RIL_REQUEST_VENDOR_BASE + 94;
    // [IMS] WFC
    int RIL_REQUEST_SET_WFC_PROFILE = RIL_REQUEST_VENDOR_BASE + 95;
    // [IMS] Pull Call
    int RIL_REQUEST_PULL_CALL = RIL_REQUEST_VENDOR_BASE + 96;
    // [IMS] Registration Report
    int RIL_REQUEST_SET_IMS_REGISTRATION_REPORT = RIL_REQUEST_VENDOR_BASE + 97;
    // [IMS] Dial
    int RIL_REQUEST_IMS_DIAL = RIL_REQUEST_VENDOR_BASE + 98;
    // [IMS] VT Dial
    int RIL_REQUEST_IMS_VT_DIAL = RIL_REQUEST_VENDOR_BASE + 99;

    /// [IMS] IMS RIL_REQUEST ==============================================================

    // M: Data Framework - CC 33
    int RIL_REQUEST_SET_REMOVE_RESTRICT_EUTRAN_MODE = RIL_REQUEST_VENDOR_BASE + 100;

    // MTK_TC1_FEATURE for Antenna Testing start
    int RIL_REQUEST_VSS_ANTENNA_CONF = (RIL_REQUEST_VENDOR_BASE + 101);
    int RIL_REQUEST_VSS_ANTENNA_INFO = (RIL_REQUEST_VENDOR_BASE + 102);
    // MTK_TC1_FEATURE for Antenna Testing end

    // SS Start
    int RIL_REQUEST_SET_CLIP  = (RIL_REQUEST_VENDOR_BASE + 103);
    int RIL_REQUEST_GET_COLP  = (RIL_REQUEST_VENDOR_BASE + 104);
    int RIL_REQUEST_GET_COLR  = (RIL_REQUEST_VENDOR_BASE + 105);
    int RIL_REQUEST_SEND_CNAP = (RIL_REQUEST_VENDOR_BASE + 106);
    // SS End

    int RIL_REQUEST_GET_POL_CAPABILITY = (RIL_REQUEST_VENDOR_BASE + 107);
    int RIL_REQUEST_GET_POL_LIST = (RIL_REQUEST_VENDOR_BASE + 108);
    int RIL_REQUEST_SET_POL_ENTRY = (RIL_REQUEST_VENDOR_BASE + 109);

    /// M: CC: Vzw/CTVolte ECC @{
    int RIL_REQUEST_ECC_PREFERRED_RAT = (RIL_REQUEST_VENDOR_BASE + 110);
    /// @}

    /// M: [Network][C2K] Sprint roaming control @{
    int RIL_REQUEST_SET_ROAMING_ENABLE = (RIL_REQUEST_VENDOR_BASE + 111);
    int RIL_REQUEST_GET_ROAMING_ENABLE = (RIL_REQUEST_VENDOR_BASE + 112);
    /// @}

    // External SIM [START]
    int RIL_REQUEST_VSIM_NOTIFICATION = (RIL_REQUEST_VENDOR_BASE + 113);
    int RIL_REQUEST_VSIM_OPERATION = (RIL_REQUEST_VENDOR_BASE + 114);
    // External SIM [END]

    int RIL_REQUEST_GET_GSM_SMS_BROADCAST_ACTIVATION = (RIL_REQUEST_VENDOR_BASE + 115);

    /// M: Mwis - Mobile Wifi Interation Service @{
    int RIL_REQUEST_SET_WIFI_ENABLED = (RIL_REQUEST_VENDOR_BASE + 116);
    int RIL_REQUEST_SET_WIFI_ASSOCIATED = (RIL_REQUEST_VENDOR_BASE + 117);
    int RIL_REQUEST_SET_WIFI_SIGNAL_LEVEL = (RIL_REQUEST_VENDOR_BASE + 118);
    int RIL_REQUEST_SET_WIFI_IP_ADDRESS = (RIL_REQUEST_VENDOR_BASE + 119);
    int RIL_REQUEST_SET_GEO_LOCATION = (RIL_REQUEST_VENDOR_BASE + 120);
    int RIL_REQUEST_SET_EMERGENCY_ADDRESS_ID = (RIL_REQUEST_VENDOR_BASE + 121);
    /// @}

    // VzW CC
    int RIL_REQUEST_SET_VOICE_DOMAIN_PREFERENCE = (RIL_REQUEST_VENDOR_BASE + 122);

    /// M: IMS SS @{
    int RIL_REQUEST_SET_COLP = (RIL_REQUEST_VENDOR_BASE + 123);
    int RIL_REQUEST_SET_COLR = (RIL_REQUEST_VENDOR_BASE + 124);
    int RIL_REQUEST_QUERY_CALL_FORWARD_IN_TIME_SLOT = (RIL_REQUEST_VENDOR_BASE + 125);
    int RIL_REQUEST_SET_CALL_FORWARD_IN_TIME_SLOT = (RIL_REQUEST_VENDOR_BASE + 126);
    int RIL_REQUEST_RUN_GBA = (RIL_REQUEST_VENDOR_BASE + 127);
    /// @}

    int RIL_REQUEST_SET_MD_IMSCFG = (RIL_REQUEST_VENDOR_BASE + 128);
    /// M: E911 ECBM. @{
    int RIL_REQUEST_SET_E911_STATE = (RIL_REQUEST_VENDOR_BASE + 129);
    // @}

    int RIL_REQUEST_SET_SERVICE_STATE = (RIL_REQUEST_VENDOR_BASE + 130);

    /// M: Mwis - WiFi Keepalive @{
    int RIL_REQUEST_SET_NATT_KEEPALIVE_STATUS = (RIL_REQUEST_VENDOR_BASE + 131);
    /// @}

    /// M:  WfoService - Backhaul strength.
    int RIL_REQUEST_SET_WIFI_PING_RESULT = (RIL_REQUEST_VENDOR_BASE + 132);
    /// @}

    int RIL_REQUEST_IMS_SEND_SMS_EX = (RIL_REQUEST_VENDOR_BASE + 133);

    int RIL_REQUEST_SET_SMS_FWK_READY = (RIL_REQUEST_VENDOR_BASE + 134);

    /// M: IMS DATA @{
    int RIL_REQUEST_SET_IMS_BEARER_NOTIFICATION = (RIL_REQUEST_VENDOR_BASE + 135);
    ///@}

    /// M: Telephonyware IMS Config Request @{
    int RIL_REQUEST_IMS_CONFIG_SET_FEATURE = (RIL_REQUEST_VENDOR_BASE + 136);

    int RIL_REQUEST_IMS_CONFIG_GET_FEATURE = (RIL_REQUEST_VENDOR_BASE + 137);

    int RIL_REQUEST_IMS_CONFIG_SET_PROVISION = (RIL_REQUEST_VENDOR_BASE + 138);

    int RIL_REQUEST_IMS_CONFIG_GET_PROVISION = (RIL_REQUEST_VENDOR_BASE + 139);

    int RIL_REQUEST_IMS_CONFIG_SET_RESOURCE_CAP = (RIL_REQUEST_VENDOR_BASE + 140);

    int RIL_REQUEST_IMS_CONFIG_GET_RESOURCE_CAP = (RIL_REQUEST_VENDOR_BASE + 141);
    /// @}

    int RIL_REQUEST_SIM_GET_ICCID = (RIL_REQUEST_VENDOR_BASE + 142);

    int RIL_REQUEST_ENTER_DEPERSONALIZATION = (RIL_REQUEST_VENDOR_BASE + 143);

    /// M: Data attach/detach, reset all data connections. @{
    int RIL_REQUEST_DATA_CONNECTION_ATTACH = (RIL_REQUEST_VENDOR_BASE + 144);

    int RIL_REQUEST_DATA_CONNECTION_DETACH = (RIL_REQUEST_VENDOR_BASE + 145);

    int RIL_REQUEST_RESET_ALL_CONNECTIONS = (RIL_REQUEST_VENDOR_BASE + 146);
    /// @}

    int RIL_REQUEST_SET_VOICE_PREFER_STATUS = RIL_REQUEST_VENDOR_BASE + 147;
    int RIL_REQUEST_SET_ECC_NUM = RIL_REQUEST_VENDOR_BASE + 148;
    int RIL_REQUEST_GET_ECC_NUM = RIL_REQUEST_VENDOR_BASE + 149;
    int RIL_REQUEST_RESTART_RILD = (RIL_REQUEST_VENDOR_BASE + 150);
    int RIL_REQUEST_SET_LTE_RELEASE_VERSION = (RIL_REQUEST_VENDOR_BASE + 151);
    int RIL_REQUEST_GET_LTE_RELEASE_VERSION = (RIL_REQUEST_VENDOR_BASE + 152);
    int RIL_REQUEST_SIGNAL_STRENGTH_WITH_WCDMA_ECIO = (RIL_REQUEST_VENDOR_BASE + 153);
    int RIL_REQUEST_REPORT_AIRPLANE_MODE = (RIL_REQUEST_VENDOR_BASE + 154);
    int RIL_REQUEST_REPORT_SIM_MODE = (RIL_REQUEST_VENDOR_BASE + 155);
    int RIL_REQUEST_SET_SILENT_REBOOT = (RIL_REQUEST_VENDOR_BASE + 156);
    int RIL_REQUEST_SET_PHONEBOOK_READY = (RIL_REQUEST_VENDOR_BASE + 157);
    int RIL_REQUEST_SET_TX_POWER_STATUS = (RIL_REQUEST_VENDOR_BASE + 158);
    int RIL_REQUEST_SETPROP_IMS_HANDOVER = (RIL_REQUEST_VENDOR_BASE + 159);
    int RIL_REQUEST_SET_PDN_REUSE = (RIL_REQUEST_VENDOR_BASE + 160);
    int RIL_REQUEST_SET_OVERRIDE_APN = (RIL_REQUEST_VENDOR_BASE + 161);
    int RIL_REQUEST_SET_PDN_NAME_REUSE = (RIL_REQUEST_VENDOR_BASE + 162);
    int RIL_REQUEST_GET_XCAP_STATUS = (RIL_REQUEST_VENDOR_BASE + 163);
    int RIL_REQUEST_RESET_SUPP_SERV = (RIL_REQUEST_VENDOR_BASE + 164);
    int RIL_REQUEST_SET_OPERATOR_CONFIGURATION = (RIL_REQUEST_VENDOR_BASE + 165);

    int RIL_REQUEST_SET_RCS_UA_ENABLE = RIL_REQUEST_VENDOR_BASE + 166;
    int RIL_REQUEST_SETUP_XCAP_USER_AGENT_STRING = RIL_REQUEST_VENDOR_BASE + 167;
    int RIL_REQUEST_SET_SS_PROPERTY = RIL_REQUEST_VENDOR_BASE + 168;
    int RIL_REQUEST_GET_SS_PROPERTY = RIL_REQUEST_VENDOR_BASE + 169;

    int RIL_REQUEST_SMS_ACKNOWLEDGE_EX = (RIL_REQUEST_VENDOR_BASE + 170);
    // SIM ME LOCK
    int RIL_REQUEST_ENTER_DEVICE_NETWORK_DEPERSONALIZATION = (RIL_REQUEST_VENDOR_BASE + 171);
    int RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE_EX = (RIL_REQUEST_VENDOR_BASE + 172);

    /// M: CC: Proprietary incoming call handling
    int RIL_REQUEST_SET_CALL_INDICATION_WITH_CAUSE = (RIL_REQUEST_VENDOR_BASE + 174);
    /// M: CC: Proprietary ringing/waiting call hangup with cause
    int RIL_REQUEST_HANGUP_WITH_CAUSE = (RIL_REQUEST_VENDOR_BASE + 175);

    int RIL_REQUEST_HANGUP_WITH_REASON = RIL_REQUEST_VENDOR_BASE + 176;

    /*********************************************************************************/
    /*  Vendor unsol                                                                 */
    /*********************************************************************************/
    int RIL_UNSOL_VENDOR_BASE = 3000;

    int RIL_UNSOL_RESPONSE_PLMN_CHANGED = RIL_UNSOL_VENDOR_BASE + 0;
    int RIL_UNSOL_RESPONSE_REGISTRATION_SUSPENDED = RIL_UNSOL_VENDOR_BASE + 1;

    int RIL_UNSOL_RESPONSE_PS_NETWORK_CHANGED = RIL_UNSOL_VENDOR_BASE + 2;

    /// M: [C2K 6M][NW] add for Iwlan @{
    int RIL_UNSOL_GMSS_RAT_CHANGED = RIL_UNSOL_VENDOR_BASE + 3;
    int RIL_UNSOL_CDMA_PLMN_CHANGED = RIL_UNSOL_VENDOR_BASE + 4;

    // MTK-START: SIM
    int RIL_UNSOL_VIRTUAL_SIM_ON = (RIL_UNSOL_VENDOR_BASE + 5);
    int RIL_UNSOL_VIRTUAL_SIM_OFF = (RIL_UNSOL_VENDOR_BASE + 6);
    int RIL_UNSOL_IMEI_LOCK = (RIL_UNSOL_VENDOR_BASE + 7);
    int RIL_UNSOL_IMSI_REFRESH_DONE = (RIL_UNSOL_VENDOR_BASE + 8);
    // MTK-END

    // ATCI
    int RIL_UNSOL_ATCI_RESPONSE = (RIL_UNSOL_VENDOR_BASE + 9);

    int RIL_UNSOL_RESPONSE_ETWS_NOTIFICATION = (RIL_UNSOL_VENDOR_BASE + 10);
    int RIL_UNSOL_ME_SMS_STORAGE_FULL = (RIL_UNSOL_VENDOR_BASE + 11);
    int RIL_UNSOL_SMS_READY_NOTIFICATION = (RIL_UNSOL_VENDOR_BASE + 12);

    int RIL_UNSOL_RESPONSE_CS_NETWORK_STATE_CHANGED = (RIL_UNSOL_VENDOR_BASE + 13);

    int RIL_UNSOL_DATA_ALLOWED = (RIL_UNSOL_VENDOR_BASE + 14);
    /// M: CC: Proprietary incoming call handling
    int RIL_UNSOL_INCOMING_CALL_INDICATION = (RIL_UNSOL_VENDOR_BASE + 15);
    int RIL_UNSOL_INVALID_SIM = RIL_UNSOL_VENDOR_BASE + 16;
    // APC
    int RIL_UNSOL_PSEUDO_CELL_INFO = (RIL_UNSOL_VENDOR_BASE + 17);
    int RIL_UNSOL_NETWORK_EVENT = (RIL_UNSOL_VENDOR_BASE + 18);
    int RIL_UNSOL_MODULATION_INFO = (RIL_UNSOL_VENDOR_BASE + 19);
    int RIL_UNSOL_RESET_ATTACH_APN = (RIL_UNSOL_VENDOR_BASE + 20);
    int RIL_UNSOL_DATA_ATTACH_APN_CHANGED = (RIL_UNSOL_VENDOR_BASE + 21);
    int RIL_UNSOL_WORLD_MODE_CHANGED = (RIL_UNSOL_VENDOR_BASE + 22);
    int RIL_UNSOL_CDMA_CARD_INITIAL_ESN_OR_MEID = (RIL_UNSOL_VENDOR_BASE + 23);

    /// M: CC: GSM 02.07 B.1.26 Ciphering Indicator support
    int RIL_UNSOL_CIPHER_INDICATION = (RIL_UNSOL_VENDOR_BASE + 24);

    /// M: CC: Proprietary CRSS handling
    int RIL_UNSOL_CRSS_NOTIFICATION = (RIL_UNSOL_VENDOR_BASE + 25);

    /// M: CC: GSA HD Voice for 2/3G network support
    int RIL_UNSOL_SPEECH_CODEC_INFO = RIL_UNSOL_VENDOR_BASE + 27;
    // PHB Start (3028)
    int RIL_UNSOL_PHB_READY_NOTIFICATION = (RIL_UNSOL_VENDOR_BASE + 28);
    // PHB End

    // Femtocell (CSG)
    int RIL_UNSOL_FEMTOCELL_INFO = (RIL_UNSOL_VENDOR_BASE + 29);

    int RIL_UNSOL_NETWORK_INFO = (RIL_UNSOL_VENDOR_BASE + 30);

    /// [IMS] IMS RIL_UNSOL INDICATION =====================================================
    int RIL_UNSOL_CALL_INFO_INDICATION = RIL_UNSOL_VENDOR_BASE + 31;
    int RIL_UNSOL_ECONF_RESULT_INDICATION = RIL_UNSOL_VENDOR_BASE + 32;
    int RIL_UNSOL_SIP_CALL_PROGRESS_INDICATOR = RIL_UNSOL_VENDOR_BASE + 33;
    int RIL_UNSOL_CALLMOD_CHANGE_INDICATOR = RIL_UNSOL_VENDOR_BASE + 34;
    int RIL_UNSOL_VIDEO_CAPABILITY_INDICATOR = RIL_UNSOL_VENDOR_BASE + 35;
    int RIL_UNSOL_ON_USSI = RIL_UNSOL_VENDOR_BASE + 36;
    int RIL_UNSOL_GET_PROVISION_DONE = RIL_UNSOL_VENDOR_BASE + 37;
    int RIL_UNSOL_IMS_RTP_INFO = RIL_UNSOL_VENDOR_BASE + 38;
    int RIL_UNSOL_ON_XUI = RIL_UNSOL_VENDOR_BASE + 39;
    int RIL_UNSOL_IMS_EVENT_PACKAGE_INDICATION = RIL_UNSOL_VENDOR_BASE + 40;
    int RIL_UNSOL_IMS_REGISTRATION_INFO = RIL_UNSOL_VENDOR_BASE + 41;
    int RIL_UNSOL_IMS_ENABLE_DONE = RIL_UNSOL_VENDOR_BASE + 42;
    int RIL_UNSOL_IMS_DISABLE_DONE = RIL_UNSOL_VENDOR_BASE + 43;
    int RIL_UNSOL_IMS_ENABLE_START = RIL_UNSOL_VENDOR_BASE + 44;
    int RIL_UNSOL_IMS_DISABLE_START = RIL_UNSOL_VENDOR_BASE + 45;
    int RIL_UNSOL_ECT_INDICATION = RIL_UNSOL_VENDOR_BASE + 46;
    int RIL_UNSOL_VOLTE_SETTING = RIL_UNSOL_VENDOR_BASE + 47;
    int RIL_UNSOL_GTT_CAPABILITY_INDICATION = RIL_UNSOL_VENDOR_BASE + 48;
    int RIL_UNSOL_IMS_BEARER_ACTIVATION = RIL_UNSOL_VENDOR_BASE + 49;
    int RIL_UNSOL_IMS_BEARER_DEACTIVATION = RIL_UNSOL_VENDOR_BASE + 50;
    int RIL_UNSOL_IMS_BEARER_INIT = RIL_UNSOL_VENDOR_BASE + 51;
    int RIL_UNSOL_IMS_DEREG_DONE = RIL_UNSOL_VENDOR_BASE + 52;
    /// [IMS] IMS RIL_UNSOL INDICATION ======================================================

    // M: [VzW] Data Framework
    int RIL_UNSOL_PCO_DATA_AFTER_ATTACHED = (RIL_UNSOL_VENDOR_BASE + 53);

    /// M: eMBMS feature
    int RIL_UNSOL_EMBMS_SESSION_STATUS = (RIL_UNSOL_VENDOR_BASE + 54);
    int RIL_UNSOL_EMBMS_AT_INFO = (RIL_UNSOL_VENDOR_BASE + 55);
    /// M: eMBMS end

    // MTK-START: SIM TMO RSU
    int RIL_UNSOL_MELOCK_NOTIFICATION = (RIL_UNSOL_VENDOR_BASE + 56);
    // MTK-END

    // / M: BIP {
    int RIL_UNSOL_STK_BIP_PROACTIVE_COMMAND = (RIL_UNSOL_VENDOR_BASE + 57);
    // / M: BIP }
    // / M: OTASP {
    int RIL_UNSOL_TRIGGER_OTASP = (RIL_UNSOL_VENDOR_BASE + 58);
    // / M: OTASP }

    // M: Data Framework - Data Retry enhancement
    int RIL_UNSOL_MD_DATA_RETRY_COUNT_RESET = (RIL_UNSOL_VENDOR_BASE + 59);
    // M: Data Framework - CC 33
    int RIL_UNSOL_REMOVE_RESTRICT_EUTRAN = (RIL_UNSOL_VENDOR_BASE + 60);

    int RIL_UNSOL_PCO_STATUS = (RIL_UNSOL_VENDOR_BASE + 61);

    // M: [LTE][Low Power][UL traffic shaping] @{
    int RIL_UNSOL_LTE_ACCESS_STRATUM_STATE_CHANGE = RIL_UNSOL_VENDOR_BASE + 62;
    // M: [LTE][Low Power][UL traffic shaping] @}


    // MTK-START: SIM HOT SWAP / RECOVERY
    int RIL_UNSOL_SIM_PLUG_IN = (RIL_UNSOL_VENDOR_BASE + 63);
    int RIL_UNSOL_SIM_PLUG_OUT = (RIL_UNSOL_VENDOR_BASE + 64);
    int RIL_UNSOL_SIM_MISSING = (RIL_UNSOL_VENDOR_BASE + 65);
    int RIL_UNSOL_SIM_RECOVERY = (RIL_UNSOL_VENDOR_BASE + 66);
    // MTK-END
    // MTK-START: SIM COMMON SLOT
    int RIL_UNSOL_TRAY_PLUG_IN = (RIL_UNSOL_VENDOR_BASE + 67);
    int RIL_UNSOL_SIM_COMMON_SLOT_NO_CHANGED = (RIL_UNSOL_VENDOR_BASE + 68);
    // MTK-END

    /// M: CC: CDMA call accepted
    int RIL_UNSOL_CDMA_CALL_ACCEPTED = (RIL_UNSOL_VENDOR_BASE + 69);
    // M: SS
    int RIL_UNSOL_CALL_FORWARDING = (RIL_UNSOL_VENDOR_BASE + 70);
    /// M:STK {
    int RIL_UNSOL_STK_SETUP_MENU_RESET = (RIL_UNSOL_VENDOR_BASE + 71);
    /// M:STK }
    int RIL_UNSOL_ECONF_SRVCC_INDICATION = (RIL_UNSOL_VENDOR_BASE + 72);

    // M: [VzW] Data Framework
    int RIL_UNSOL_VOLTE_LTE_CONNECTION_STATUS = (RIL_UNSOL_VENDOR_BASE + 73);

    // External SIM [START]
    int RIL_UNSOL_VSIM_OPERATION_INDICATION = (RIL_UNSOL_VENDOR_BASE + 74);
    // External SIM [END]

    /// M: Mwis - Mobile Wifi Interation Service @{
    int RIL_UNSOL_MOBILE_WIFI_ROVEOUT = (RIL_UNSOL_VENDOR_BASE + 75);
    int RIL_UNSOL_MOBILE_WIFI_HANDOVER = (RIL_UNSOL_VENDOR_BASE + 76);
    int RIL_UNSOL_ACTIVE_WIFI_PDN_COUNT = (RIL_UNSOL_VENDOR_BASE + 77);
    int RIL_UNSOL_WIFI_RSSI_MONITORING_CONFIG = (RIL_UNSOL_VENDOR_BASE + 78);
    int RIL_UNSOL_WIFI_PDN_ERROR = (RIL_UNSOL_VENDOR_BASE + 79);
    int RIL_UNSOL_REQUEST_GEO_LOCATION = (RIL_UNSOL_VENDOR_BASE + 80);
    int RIL_UNSOL_WFC_PDN_STATE = (RIL_UNSOL_VENDOR_BASE + 81);
    /// @}

    /// Ims Data Framework {@
    int RIL_UNSOL_DEDICATE_BEARER_ACTIVATED = (RIL_UNSOL_VENDOR_BASE + 82);
    int RIL_UNSOL_DEDICATE_BEARER_MODIFIED = (RIL_UNSOL_VENDOR_BASE + 83);
    int RIL_UNSOL_DEDICATE_BEARER_DEACTIVATED = (RIL_UNSOL_VENDOR_BASE + 84);
    ///@}

    /// M: Modem Multi-IMS support count
    int RIL_UNSOL_IMS_MULTIIMS_COUNT = (RIL_UNSOL_VENDOR_BASE + 85);

    /// M: Mwis - Wifi Keep Alive
    int RIL_UNSOL_NATT_KEEP_ALIVE_CHANGED = (RIL_UNSOL_VENDOR_BASE + 86);

    /// M: Mwis - Wifi Ping Request
    int RIL_UNSOL_WIFI_PING_REQUEST = (RIL_UNSOL_VENDOR_BASE + 87);

    /// M: Mwis - Wifi PDN Out Of Service
    int RIL_UNSOL_WIFI_PDN_OOS = (RIL_UNSOL_VENDOR_BASE + 88);

    /// M: IMS Event Indication 1.IMS conference info update 2.MWI info update @{
    int RIL_UNSOL_IMS_CONFERENCE_INFO_INDICATION = (RIL_UNSOL_VENDOR_BASE + 89);
    int RIL_UNSOL_LTE_MESSAGE_WAITING_INDICATION = (RIL_UNSOL_VENDOR_BASE + 90);
    /// @}

    /// Telephonyware IMS config @{
    int RIL_UNSOL_IMS_CONFIG_DYNAMIC_IMS_SWITCH_COMPLETE = (RIL_UNSOL_VENDOR_BASE + 91);
    int RIL_UNSOL_IMS_CONFIG_FEATURE_CHANGED = (RIL_UNSOL_VENDOR_BASE + 92);
    int RIL_UNSOL_IMS_CONFIG_CONFIG_CHANGED = (RIL_UNSOL_VENDOR_BASE + 93);
    int RIL_UNSOL_IMS_CONFIG_CONFIG_LOADED = (RIL_UNSOL_VENDOR_BASE + 94);
    /// @}

    int RIL_UNSOL_ECC_NUM = RIL_UNSOL_VENDOR_BASE + 95;

    int RIL_UNSOL_MCCMNC_CHANGED = RIL_UNSOL_VENDOR_BASE + 96;
    int RIL_UNSOL_SIGNAL_STRENGTH_WITH_WCDMA_ECIO = (RIL_UNSOL_VENDOR_BASE + 97);
    int RIL_UNSOL_TX_POWER_STATUS = (RIL_UNSOL_VENDOR_BASE + 107);
    int RIL_UNSOL_NETWORK_REJECT_CAUSE = (RIL_UNSOL_VENDOR_BASE + 109);
    int RIL_UNSOL_ON_VOLTE_SUBSCRIPTION = (RIL_UNSOL_VENDOR_BASE + 110);
    int RIL_UNSOL_IMS_DATA_INFO_NOTIFY = (RIL_UNSOL_VENDOR_BASE + 111);
    int RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT_EX = (RIL_UNSOL_VENDOR_BASE + 112);
    int RIL_UNSOL_RESPONSE_NEW_SMS_EX = (RIL_UNSOL_VENDOR_BASE + 113);
    int RIL_UNSOL_DSBP_STATE_CHANGED = (RIL_UNSOL_VENDOR_BASE + 114);
    /// M: SIM ME LOCK
    int RIL_UNSOL_SIM_SLOT_LOCK_POLICY_NOTIFY = (RIL_UNSOL_VENDOR_BASE + 115);
    int RIL_UNSOL_RESPONSE_CDMA_NEW_SMS_EX = (RIL_UNSOL_VENDOR_BASE + 116);
    int RIL_UNSOL_NO_EMERGENCY_CALLBACK_MODE = (RIL_UNSOL_VENDOR_BASE + 117);
    int RIL_UNSOL_INCOMING_CALL_ADDITIONAL_INFO = (RIL_UNSOL_VENDOR_BASE + 118);
}
