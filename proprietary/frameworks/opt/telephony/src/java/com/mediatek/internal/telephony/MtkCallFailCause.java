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

import com.android.internal.telephony.CallFailCause;

public interface MtkCallFailCause extends CallFailCause {
    /// M: CC: Extend Call Fail Cause @{
    int NO_ROUTE_TO_DESTINATION = 3;
    int CHANNEL_UNACCEPTABLE = 6;
    int OPERATOR_DETERMINED_BARRING = 8;
    int NO_USER_RESPONDING = 18;
    int USER_ALERTING_NO_ANSWER = 19;
    int CALL_REJECTED = 21;
    int PRE_EMPTION = 25;
    int NON_SELECTED_USER_CLEARING = 26;
    int DESTINATION_OUT_OF_ORDER = 27;
    int INVALID_NUMBER_FORMAT = 28;
    int FACILITY_REJECTED = 29;
    int NETWORK_OUT_OF_ORDER = 38;
    int ACCESS_INFORMATION_DISCARDED = 43;
    int RESOURCE_UNAVAILABLE = 47;
    int REQUESTED_FACILITY_NOT_SUBSCRIBED = 50;
    int INCOMING_CALL_BARRED_WITHIN_CUG = 55;
    int BEARER_NOT_AUTHORIZED = 57;
    int SERVICE_NOT_AVAILABLE = 63;
    int BEARER_NOT_IMPLEMENT = 65;
    int FACILITY_NOT_IMPLEMENT = 69;
    int RESTRICTED_BEARER_AVAILABLE = 70;
    int OPTION_NOT_AVAILABLE = 79;
    int INVALID_TRANSACTION_ID_VALUE = 81;
    int USER_NOT_MEMBER_OF_CUG = 87;
    int INCOMPATIBLE_DESTINATION = 88;
    int INVALID_TRANSIT_NETWORK_SELECTION = 91;
    int SEMANTICALLY_INCORRECT_MESSAGE = 95;
    int INVALID_MANDATORY_INFORMATION = 96;
    int MESSAGE_TYPE_NON_EXISTENT = 97;
    int MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROT_STATE = 98;
    int IE_NON_EXISTENT_OR_NOT_IMPLEMENTED = 99;
    int CONDITIONAL_IE_ERROR = 100;
    int MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE = 101;
    int RECOVERY_ON_TIMER_EXPIRY = 102;
    int PROTOCOL_ERROR_UNSPECIFIED = 111;
    int INTERWORKING_UNSPECIFIED = 127;
    int IMEI_NOT_ACCEPTED = 243;
    int CM_MM_RR_CONNECTION_RELEASE = 2165;
    /// @}

    /// M: IMS feature. @{
    /* Normal call failed, need to dial as ECC */
    int IMS_EMERGENCY_REREG = 380;
    /// @}
}
