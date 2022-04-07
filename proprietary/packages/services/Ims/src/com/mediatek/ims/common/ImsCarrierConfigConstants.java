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

package com.mediatek.ims.common;

/******************************************************************
  * Constants of IMS CarrierConfigManager's key
  * Copy from MtkImsCarrierConfigManager
  * You need to add/update keys in both
  * ImsCarrierConfigConstants.java & MtkCarrierConfigManager.java
  *****************************************************************/
public interface ImsCarrierConfigConstants {

    /**
     * Support nourth America high priority CLIR such as *82
     * Default value is false
     */
    String MTK_KEY_CARRIER_NOURTH_AMERICA_HIGH_PRIORITY_CLIR_PREFIX_SUPPORTED =
           "mtk_carrier_nouth_america_high_priority_clir_prefix_supported";

    /**
     * Need to swap conference to foreground before merge
     * Default value is false
     */
    String MTK_KEY_CARRIER_SWAP_CONFERENCE_TO_FOREGROUND_BEFORE_MERGE =
           "mtk_carrier_swap_conference_to_foreground_before_merge";

    /**
     * Need to update dialing address from PAU
     * Default value is false
     */
    String MTK_KEY_CARRIER_UPDATE_DIALING_ADDRESS_FROM_PAU =
           "mtk_carrier_update_dialing_address_from_pau";


    /**
     * WFC bad Wifi quality disconnect cause
     * Default value is false
     */
    String MTK_KEY_CARRIER_NOTIFY_BAD_WIFI_QUALITY_DISCONNECT_CAUSE =
           "mtk_carrier_notify_bad_wifi_quality_disconnect_cause";

    /**
     * Restore participants address for IMS conference.
     */
    String MTK_KEY_RESTORE_ADDRESS_FOR_IMS_CONFERENCE_PARTICIPANTS =
           "mtk_key_restore_address_for_ims_conference_participants";

    /**
     * Operate IMS conference participants by user untity from CEP.
     * @hide
     */
    public static final String MTK_KEY_OPERATE_IMS_CONFERENCE_PARTICIPANTS_BY_USER_ENTITY =
            "mtk_key_operate_ims_conference_participants_by_user_entity";

    /**
     * Determine whether to remove WFC preference mode or not.
     */
    String MTK_KEY_WFC_REMOVE_PREFERENCE_MODE_BOOL =
           "mtk_wfc_remove_preference_mode_bool";

    /**
     * @M: Operator ID
     * AP requires this information to know which operator SIM card is inserted.
     * Default value is 0(OM or no SIM inserted).
     */
    String KEY_OPERATOR_ID_INT = "operator_id";

    /**
     * IMS ECBM supported
     * Default value is false
     */
    String MTK_KEY_CARRIER_IMS_ECBM_SUPPORTED =
           "mtk_carrier_ims_ecbm_supported";

    /**
     * Added to check the case if Operator server supports Conference management or not.
     * @hide
     */
    String MTK_KEY_CONFERENCE_MANAGEMENT_SUPPORTED =
            "mtk_key_conference_management_supported";

    /**
     * Need to swap conference to foreground before merge
     * Default value is false
     * @hide
     */
    public static final String MTK_KEY_CARRIER_SWAP_VT_CONFERENCE_TO_FOREGROUND_BEFORE_MERGE =
            "mtk_carrier_swap_vt_conference_to_foreground_before_merge";

    /**
     * Added to check the case if Operator server supports first participant as host.
     * @hide
     */
    String MTK_KEY_CONF_FIRST_PARTICIPANT_AS_HOST_SUPPORTED =
            "mtk_key_conference_first_participant_as_host_supported";
}
