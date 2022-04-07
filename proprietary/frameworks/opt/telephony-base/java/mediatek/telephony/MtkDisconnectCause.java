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

import android.telephony.DisconnectCause;

public class MtkDisconnectCause extends DisconnectCause {
    public static final int IMS_EMERGENCY_REREG = 380;

    public static final int WFC_WIFI_SIGNAL_LOST = 400;
    public static final int WFC_ISP_PROBLEM = 401;
    public static final int WFC_HANDOVER_WIFI_FAIL = 402;
    public static final int WFC_HANDOVER_LTE_FAIL = 403;

    /// M: CC: Extend Call Fail Cause @{
    /// [ALPS00093395]
    /**
     * @hide
     */
    public static final int MTK_DISCONNECTED_CAUSE_BASE = 1000;
    /** no route to destination
     * @hide
     */
    public static final int NO_ROUTE_TO_DESTINATION        = MTK_DISCONNECTED_CAUSE_BASE + 1;
    /** no user responding
     * @hide
     */
    public static final int NO_USER_RESPONDING             = MTK_DISCONNECTED_CAUSE_BASE + 2;
    /** user alerting, no answer
     * @hide
     */
    public static final int USER_ALERTING_NO_ANSWER        = MTK_DISCONNECTED_CAUSE_BASE + 3;
    /** call rejected
     * @hide
     */
    public static final int CALL_REJECTED                  = MTK_DISCONNECTED_CAUSE_BASE + 4;
    /** invalid number format
     * @hide
     */
    public static final int INVALID_NUMBER_FORMAT          = MTK_DISCONNECTED_CAUSE_BASE + 5;
    /** facility rejected
     * @hide
     */
    public static final int FACILITY_REJECTED              = MTK_DISCONNECTED_CAUSE_BASE + 6;
    /** normal, unspecified
     * @hide
     */
    public static final int NORMAL_UNSPECIFIED             = MTK_DISCONNECTED_CAUSE_BASE + 7;
    /** no circuit/channel available
     * @hide
     */
    public static final int NO_CIRCUIT_AVAIL               = MTK_DISCONNECTED_CAUSE_BASE + 8;
    /** switching equipment congestion
     * @hide
     */
    public static final int SWITCHING_CONGESTION           = MTK_DISCONNECTED_CAUSE_BASE + 9;
    /** resource unavailable, unspecified
     * @hide
     */
    public static final int RESOURCE_UNAVAILABLE           = MTK_DISCONNECTED_CAUSE_BASE + 10;
    /** bearer capability not authorized
     * @hide
     */
    public static final int BEARER_NOT_AUTHORIZED          = MTK_DISCONNECTED_CAUSE_BASE + 11;
    /** bearer capability not presently available
     * @hide
     */
    public static final int BEARER_NOT_AVAIL               = MTK_DISCONNECTED_CAUSE_BASE + 12;
    /** service or option not available, unspecified
     * @hide
     */
    public static final int SERVICE_NOT_AVAILABLE          = MTK_DISCONNECTED_CAUSE_BASE + 13;
    /** bearer service not implemented
     * @hide
     */
    public static final int BEARER_NOT_IMPLEMENT           = MTK_DISCONNECTED_CAUSE_BASE + 14;
    /** Requested facility not implemented
     * @hide
     */
    public static final int FACILITY_NOT_IMPLEMENT         = MTK_DISCONNECTED_CAUSE_BASE + 15;
    /** only restricted digital information bearer capability is available
     * @hide
     */
    public static final int RESTRICTED_BEARER_AVAILABLE    = MTK_DISCONNECTED_CAUSE_BASE + 16;
    /** service or option not implemented, unspecified
     * @hide
     */
    public static final int OPTION_NOT_AVAILABLE           = MTK_DISCONNECTED_CAUSE_BASE + 17;
    /** incompatible destination
     * @hide
     */
    public static final int INCOMPATIBLE_DESTINATION       = MTK_DISCONNECTED_CAUSE_BASE + 18;
    /** RR connection release
     * @hide
     */
    public static final int CHANNEL_UNACCEPTABLE           = MTK_DISCONNECTED_CAUSE_BASE + 19;
    /**
     * @hide
     */
    public static final int OPERATOR_DETERMINED_BARRING    = MTK_DISCONNECTED_CAUSE_BASE + 20;
    /**
     * @hide
     */
    public static final int PRE_EMPTION                    = MTK_DISCONNECTED_CAUSE_BASE + 21;
    /**
     * @hide
     */
    public static final int NON_SELECTED_USER_CLEARING     = MTK_DISCONNECTED_CAUSE_BASE + 22;
    /**
     * @hide
     */
    public static final int DESTINATION_OUT_OF_ORDER       = MTK_DISCONNECTED_CAUSE_BASE + 23;
    /**
     * @hide
     */
    public static final int ACCESS_INFORMATION_DISCARDED   = MTK_DISCONNECTED_CAUSE_BASE + 24;
    /**
     * @hide
     */
    public static final int REQUESTED_FACILITY_NOT_SUBSCRIBED = MTK_DISCONNECTED_CAUSE_BASE + 25;
    /**
     * @hide
     */
    public static final int INCOMING_CALL_BARRED_WITHIN_CUG   = MTK_DISCONNECTED_CAUSE_BASE + 26;
    /**
     * @hide
     */
    public static final int INVALID_TRANSACTION_ID_VALUE   = MTK_DISCONNECTED_CAUSE_BASE + 27;
    /**
     * @hide
     */
    public static final int USER_NOT_MEMBER_OF_CUG         = MTK_DISCONNECTED_CAUSE_BASE + 28;
    /**
     * @hide
     */
    public static final int INVALID_TRANSIT_NETWORK_SELECTION = MTK_DISCONNECTED_CAUSE_BASE + 29;
    /**
     * @hide
     */
    public static final int SEMANTICALLY_INCORRECT_MESSAGE = MTK_DISCONNECTED_CAUSE_BASE + 30;
    /**
     * @hide
     */
    public static final int INVALID_MANDATORY_INFORMATION  = MTK_DISCONNECTED_CAUSE_BASE + 31;
    /**
     * @hide
     */
    public static final int MESSAGE_TYPE_NON_EXISTENT      = MTK_DISCONNECTED_CAUSE_BASE + 32;
    /**
     * @hide
     */
    public static final int MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROT_STATE =
            MTK_DISCONNECTED_CAUSE_BASE + 33;
    /**
     * @hide
     */
    public static final int IE_NON_EXISTENT_OR_NOT_IMPLEMENTED = MTK_DISCONNECTED_CAUSE_BASE + 34;
    /**
     * @hide
     */
    public static final int CONDITIONAL_IE_ERROR           = MTK_DISCONNECTED_CAUSE_BASE + 35;
    /**
     * @hide
     */
    public static final int MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE = MTK_DISCONNECTED_CAUSE_BASE
            + 36;
    /**
     * @hide
     */
    public static final int RECOVERY_ON_TIMER_EXPIRY       = MTK_DISCONNECTED_CAUSE_BASE + 37;
    /**
     * @hide
     */
    public static final int PROTOCOL_ERROR_UNSPECIFIED     = MTK_DISCONNECTED_CAUSE_BASE + 38;
    /**
     * @hide
     */
    public static final int INTERWORKING_UNSPECIFIED       = MTK_DISCONNECTED_CAUSE_BASE + 39;
    /**
     * @hide
     */
    public static final int CM_MM_RR_CONNECTION_RELEASE    = MTK_DISCONNECTED_CAUSE_BASE + 40;
    /// @}
    /// M: CC: Error message due to CellConnMgr checking @{
    /**
     * @hide
     */
    public static final int OUTGOING_CANCELED_BY_SERVICE   = MTK_DISCONNECTED_CAUSE_BASE + 41;
    /// @}

    /// M: ALPS02501206. For OP07 requirement. @{
    public static final int ECC_OVER_WIFI_UNSUPPORTED = MTK_DISCONNECTED_CAUSE_BASE + 42;
    public static final int WFC_UNAVAILABLE_IN_CURRENT_LOCATION = MTK_DISCONNECTED_CAUSE_BASE + 43;
    /// @}

    /// M: SS: Error message due to VoLTE SS checking @{
    /**
     * Reject MMI for setting SS under VoLTE without data setting enabled, since XCAP is thru HTTP
     * It shares same error string as modifying SS setting under same scenario.
     * see {@link com.android.services.telephony.DisconnectCauseUtil#toTelecomDisconnectCauseLabel}
     * @hide
     */
    public static final int VOLTE_SS_DATA_OFF              = MTK_DISCONNECTED_CAUSE_BASE + 44;
    /// @}

    public static final int WFC_CALL_DROP_BAD_RSSI = MTK_DISCONNECTED_CAUSE_BASE + 45;
    public static final int WFC_CALL_DROP_BACKHAUL_CONGESTION = MTK_DISCONNECTED_CAUSE_BASE + 46;

    /// M: Telcel requirement. @{
    /**
     * @hide
     */
    public static final int CAUSE_MOVED_PERMANENTLY = MTK_DISCONNECTED_CAUSE_BASE + 500;
    /**
     * @hide
     */
    public static final int CAUSE_BAD_REQUEST = MTK_DISCONNECTED_CAUSE_BASE + 501;
    /**
     * @hide
     */
    public static final int CAUSE_UNAUTHORIZED = MTK_DISCONNECTED_CAUSE_BASE + 502;
    /**
     * @hide
     */
    public static final int CAUSE_PAYMENT_REQUIRED = MTK_DISCONNECTED_CAUSE_BASE + 503;
    /**
     * @hide
     */
    public static final int CAUSE_FORBIDDEN = MTK_DISCONNECTED_CAUSE_BASE + 504;
    /**
     * @hide
     */
    public static final int CAUSE_NOT_FOUND = MTK_DISCONNECTED_CAUSE_BASE + 505;
    /**
     * @hide
     */
    public static final int CAUSE_METHOD_NOT_ALLOWED = MTK_DISCONNECTED_CAUSE_BASE + 506;
    /**
     * @hide
     */
    public static final int CAUSE_NOT_ACCEPTABLE = MTK_DISCONNECTED_CAUSE_BASE + 507;
    /**
     * @hide
     */
    public static final int CAUSE_PROXY_AUTHENTICATION_REQUIRED = MTK_DISCONNECTED_CAUSE_BASE + 508;
    /**
     * @hide
     */
    public static final int CAUSE_REQUEST_TIMEOUT = MTK_DISCONNECTED_CAUSE_BASE + 509;
    /**
     * @hide
     */
    public static final int CAUSE_CONFLICT = MTK_DISCONNECTED_CAUSE_BASE + 510;
    /**
     * @hide
     */
    public static final int CAUSE_GONE = MTK_DISCONNECTED_CAUSE_BASE + 511;
    /**
     * @hide
     */
    public static final int CAUSE_LENGTH_REQUIRED = MTK_DISCONNECTED_CAUSE_BASE + 512;
    /**
     * @hide
     */
    public static final int CAUSE_REQUEST_ENTRY_TOO_LONG = MTK_DISCONNECTED_CAUSE_BASE + 513;
    /**
     * @hide
     */
    public static final int CAUSE_REQUEST_URI_TOO_LONG = MTK_DISCONNECTED_CAUSE_BASE + 514;
    /**
     * @hide
     */
    public static final int CAUSE_UNSUPPORTED_MEDIA_TYPE = MTK_DISCONNECTED_CAUSE_BASE + 515;
    /**
     * @hide
     */
    public static final int CAUSE_UNSUPPORTED_URI_SCHEME = MTK_DISCONNECTED_CAUSE_BASE + 516;
    /**
     * @hide
     */
    public static final int CAUSE_BAD_EXTENSION  = MTK_DISCONNECTED_CAUSE_BASE + 517;
    /**
     * @hide
     */
    public static final int CAUSE_EXTENSION_REQUIRED = MTK_DISCONNECTED_CAUSE_BASE + 518;
    /**
     * @hide
     */
    public static final int CAUSE_INTERVAL_TOO_BRIEF = MTK_DISCONNECTED_CAUSE_BASE + 519;
    /**
     * @hide
     */
    public static final int CAUSE_TEMPORARILY_UNAVAILABLE = MTK_DISCONNECTED_CAUSE_BASE + 520;
    /**
     * @hide
     */
    public static final int CAUSE_CALL_TRANSACTION_NOT_EXIST = MTK_DISCONNECTED_CAUSE_BASE + 521;
    /**
     * @hide
     */
    public static final int CAUSE_LOOP_DETECTED = MTK_DISCONNECTED_CAUSE_BASE + 522;
    /**
     * @hide
     */
    public static final int CAUSE_TOO_MANY_HOPS = MTK_DISCONNECTED_CAUSE_BASE + 523;
    /**
     * @hide
     */
    public static final int CAUSE_ADDRESS_INCOMPLETE = MTK_DISCONNECTED_CAUSE_BASE + 524;
    /**
     * @hide
     */
    public static final int CAUSE_AMBIGUOUS = MTK_DISCONNECTED_CAUSE_BASE + 525;
    /**
     * @hide
     */
    public static final int CAUSE_BUSY_HERE = MTK_DISCONNECTED_CAUSE_BASE + 526;
    /**
     * @hide
     */
    public static final int CAUSE_REQUEST_TERMINATED = MTK_DISCONNECTED_CAUSE_BASE + 527;
    /**
     * @hide
     */
    public static final int CAUSE_NOT_ACCEPTABLE_HERE = MTK_DISCONNECTED_CAUSE_BASE + 528;
    /**
     * @hide
     */
    public static final int CAUSE_SERVER_INTERNAL_ERROR = MTK_DISCONNECTED_CAUSE_BASE + 529;
    /**
     * @hide
     */
    public static final int CAUSE_NOT_IMPLEMENTED = MTK_DISCONNECTED_CAUSE_BASE + 530;
    /**
     * @hide
     */
    public static final int CAUSE_BAD_GATEWAY = MTK_DISCONNECTED_CAUSE_BASE + 531;
    /**
     * @hide
     */
    public static final int CAUSE_SERVICE_UNAVAILABLE = MTK_DISCONNECTED_CAUSE_BASE + 532;
    /**
     * @hide
     */
    public static final int CAUSE_GATEWAY_TIMEOUT = MTK_DISCONNECTED_CAUSE_BASE + 533;
    /**
     * @hide
     */
    public static final int CAUSE_VERSION_NOT_SUPPORTED = MTK_DISCONNECTED_CAUSE_BASE + 534;
    /**
     * @hide
     */
    public static final int CAUSE_MESSAGE_TOO_LONG = MTK_DISCONNECTED_CAUSE_BASE + 535;
    /**
     * @hide
     */
    public static final int CAUSE_BUSY_EVERYWHERE = MTK_DISCONNECTED_CAUSE_BASE + 536;
    /**
     * @hide
     */
    public static final int CAUSE_DECLINE = MTK_DISCONNECTED_CAUSE_BASE + 537;
    /**
     * @hide
     */
    public static final int CAUSE_DOES_NOT_EXIST_ANYWHERE = MTK_DISCONNECTED_CAUSE_BASE + 538;
    /**
     * @hide
     */
    public static final int CAUSE_SESSION_NOT_ACCEPTABLE = MTK_DISCONNECTED_CAUSE_BASE + 539;
    /// @}

    /// M: CC: incoming call reject with cause handling @{
    /**
     * M: CC: An incoming call that was rejected with cause
     *
     *     INCOMING_REJECTED_NO_FORWARD: reject the incoming call and caller not forwarded.
     *     INCOMING_REJECTED_FORWARD: reject the incoming call and the caller get forwarded if
     *                                any call forwarding setting rule matched.
     * @hide
     */
    public static final int INCOMING_REJECTED_NO_FORWARD = MTK_DISCONNECTED_CAUSE_BASE + 540;
    public static final int INCOMING_REJECTED_FORWARD = MTK_DISCONNECTED_CAUSE_BASE + 541;
    public static final int INCOMING_REJECTED_NO_COVERAGE = MTK_DISCONNECTED_CAUSE_BASE + 542;
    public static final int INCOMING_REJECTED_LOW_BATTERY = MTK_DISCONNECTED_CAUSE_BASE + 543;
    public static final int INCOMING_REJECTED_SPECIAL_HANGUP = MTK_DISCONNECTED_CAUSE_BASE + 544;
    /// @}

    /** Returns descriptive string for the specified disconnect cause. */
    public static String toString(int cause) {
        switch (cause) {
        /// M: CC: Extend Call Fail Cause @{
        /// [ALPS00093395]
        case NO_ROUTE_TO_DESTINATION:
            return "NO_ROUTE_TO_DESTINATION";
        case NO_USER_RESPONDING:
            return "NO_USER_RESPONDING";
        case USER_ALERTING_NO_ANSWER:
            return "USER_ALERTING_NO_ANSWER";
        case CALL_REJECTED:
            return "CALL_REJECTED";
        case INVALID_NUMBER_FORMAT:
            return "INVALID_NUMBER_FORMAT";
        case FACILITY_REJECTED:
            return "FACILITY_REJECTED";
        case NORMAL_UNSPECIFIED:
            return "NORMAL_UNSPECIFIED";
        case NO_CIRCUIT_AVAIL:
            return "NO_CIRCUIT_AVAIL";
        case SWITCHING_CONGESTION:
            return "SWITCHING_CONGESTION";
        case RESOURCE_UNAVAILABLE:
            return "RESOURCE_UNAVAILABLE";
        case BEARER_NOT_AUTHORIZED:
            return "BEARER_NOT_AUTHORIZED";
        case BEARER_NOT_AVAIL:
            return "BEARER_NOT_AVAIL";
        case SERVICE_NOT_AVAILABLE:
            return "SERVICE_NOT_AVAILABLE";
        case BEARER_NOT_IMPLEMENT:
            return "BEARER_NOT_IMPLEMENT";
        case FACILITY_NOT_IMPLEMENT:
            return "FACILITY_NOT_IMPLEMENT";
        case RESTRICTED_BEARER_AVAILABLE:
            return "RESTRICTED_BEARER_AVAILABLE";
        case OPTION_NOT_AVAILABLE:
            return "OPTION_NOT_AVAILABLE";
        case INCOMPATIBLE_DESTINATION:
            return "INCOMPATIBLE_DESTINATION";
        case CHANNEL_UNACCEPTABLE:
            return "CHANNEL_UNACCEPTABLE";
        case OPERATOR_DETERMINED_BARRING:
            return "OPERATOR_DETERMINED_BARRING";
        case PRE_EMPTION:
            return "PRE_EMPTION";
        case NON_SELECTED_USER_CLEARING:
            return "NON_SELECTED_USER_CLEARING";
        case DESTINATION_OUT_OF_ORDER:
            return "DESTINATION_OUT_OF_ORDER";
        case ACCESS_INFORMATION_DISCARDED:
            return "ACCESS_INFORMATION_DISCARDED";
        case REQUESTED_FACILITY_NOT_SUBSCRIBED:
            return "REQUESTED_FACILITY_NOT_SUBSCRIBED";
        case INCOMING_CALL_BARRED_WITHIN_CUG:
            return "INCOMING_CALL_BARRED_WITHIN_CUG";
        case INVALID_TRANSACTION_ID_VALUE:
            return "INVALID_TRANSACTION_ID_VALUE";
        case USER_NOT_MEMBER_OF_CUG:
            return "USER_NOT_MEMBER_OF_CUG";
        case INVALID_TRANSIT_NETWORK_SELECTION:
            return "INVALID_TRANSIT_NETWORK_SELECTION";
        case SEMANTICALLY_INCORRECT_MESSAGE:
            return "SEMANTICALLY_INCORRECT_MESSAGE";
        case INVALID_MANDATORY_INFORMATION:
            return "INVALID_MANDATORY_INFORMATION";
        case MESSAGE_TYPE_NON_EXISTENT:
            return "MESSAGE_TYPE_NON_EXISTENT";
        case MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROT_STATE:
            return "MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROT_STATE";
        case IE_NON_EXISTENT_OR_NOT_IMPLEMENTED:
            return "IE_NON_EXISTENT_OR_NOT_IMPLEMENTED";
        case CONDITIONAL_IE_ERROR:
            return "CONDITIONAL_IE_ERROR";
        case MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE:
            return "MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE";
        case RECOVERY_ON_TIMER_EXPIRY:
            return "RECOVERY_ON_TIMER_EXPIRY";
        case PROTOCOL_ERROR_UNSPECIFIED:
            return "PROTOCOL_ERROR_UNSPECIFIED";
        case INTERWORKING_UNSPECIFIED:
            return "INTERWORKING_UNSPECIFIED";
        case CM_MM_RR_CONNECTION_RELEASE:
            return "CM_MM_RR_CONNECTION_RELEASE";
        /// @}
        /// M: CC: Error message due to CellConnMgr checking @{
        case OUTGOING_CANCELED_BY_SERVICE:
            return "OUTGOING_CANCELED_BY_SERVICE";
        /// @}
        /// M: CC: incoming call reject with cause handling @{
        case INCOMING_REJECTED_NO_FORWARD:
            return "INCOMING_REJECTED_NO_FORWARD";
        case INCOMING_REJECTED_FORWARD:
            return "INCOMING_REJECTED_FORWARD";
        case INCOMING_REJECTED_NO_COVERAGE:
            return "INCOMING_REJECTED_NO_COVERAGE";
        case INCOMING_REJECTED_LOW_BATTERY:
            return "INCOMING_REJECTED_LOW_BATTERY";
        case INCOMING_REJECTED_SPECIAL_HANGUP:
            return "INCOMING_REJECTED_SPECIAL_HANGUP";
        /// @}
        case ECC_OVER_WIFI_UNSUPPORTED:
            return "ECC_OVER_WIFI_UNSUPPORTED";
        case WFC_UNAVAILABLE_IN_CURRENT_LOCATION:
            return "WFC_UNAVAILABLE_IN_CURRENT_LOCATION";
        case WFC_CALL_DROP_BACKHAUL_CONGESTION:
            return "WFC_CALL_DROP_BACKHAUL_CONGESTION";
        case WFC_CALL_DROP_BAD_RSSI:
            return "WFC_CALL_DROP_BAD_RSSI";
        /// M: SS: Error message due to VoLTE SS checking @{
        case VOLTE_SS_DATA_OFF:
            return "VOLTE_SS_DATA_OFF";
        /// @}
        /// M: Telcel requirement. @{
        case CAUSE_MOVED_PERMANENTLY:
            return "CAUSE_MOVED_PERMANENTLY";
        case CAUSE_BAD_REQUEST:
            return "CAUSE_BAD_REQUEST";
        case CAUSE_UNAUTHORIZED:
            return "CAUSE_UNAUTHORIZED";
        case CAUSE_PAYMENT_REQUIRED:
            return "CAUSE_PAYMENT_REQUIRED";
        case CAUSE_FORBIDDEN:
            return "CAUSE_FORBIDDEN";
        case CAUSE_NOT_FOUND:
            return "CAUSE_NOT_FOUND";
        case CAUSE_METHOD_NOT_ALLOWED:
            return "CAUSE_METHOD_NOT_ALLOWED";
        case CAUSE_NOT_ACCEPTABLE:
            return "CAUSE_NOT_ACCEPTABLE";
        case CAUSE_PROXY_AUTHENTICATION_REQUIRED:
            return "CAUSE_PROXY_AUTHENTICATION_REQUIRED";
        case CAUSE_REQUEST_TIMEOUT:
            return "CAUSE_REQUEST_TIMEOUT";
        case CAUSE_CONFLICT:
            return "CAUSE_CONFLICT";
        case CAUSE_GONE:
            return "CAUSE_GONE";
        case CAUSE_LENGTH_REQUIRED:
            return "CAUSE_LENGTH_REQUIRED";
        case CAUSE_REQUEST_ENTRY_TOO_LONG:
            return "CAUSE_REQUEST_ENTRY_TOO_LONG";
        case CAUSE_REQUEST_URI_TOO_LONG:
            return "CAUSE_REQUEST_URI_TOO_LONG";
        case CAUSE_UNSUPPORTED_MEDIA_TYPE:
            return "CAUSE_UNSUPPORTED_MEDIA_TYPE";
        case CAUSE_UNSUPPORTED_URI_SCHEME:
            return "CAUSE_UNSUPPORTED_URI_SCHEME";
        case CAUSE_BAD_EXTENSION:
            return "CAUSE_BAD_EXTENSION";
        case CAUSE_EXTENSION_REQUIRED:
            return "CAUSE_EXTENSION_REQUIRED";
        case CAUSE_INTERVAL_TOO_BRIEF:
            return "CAUSE_INTERVAL_TOO_BRIEF";
        case CAUSE_TEMPORARILY_UNAVAILABLE:
            return "CAUSE_TEMPORARILY_UNAVAILABLE";
        case CAUSE_CALL_TRANSACTION_NOT_EXIST:
            return "CAUSE_CALL_TRANSACTION_NOT_EXIST";
        case CAUSE_LOOP_DETECTED:
            return "CAUSE_LOOP_DETECTED";
        case CAUSE_TOO_MANY_HOPS:
            return "CAUSE_TOO_MANY_HOPS";
        case CAUSE_ADDRESS_INCOMPLETE:
            return "CAUSE_ADDRESS_INCOMPLETE";
        case CAUSE_AMBIGUOUS:
            return "CAUSE_AMBIGUOUS";
        case CAUSE_BUSY_HERE:
            return "CAUSE_BUSY_HERE";
        case CAUSE_REQUEST_TERMINATED:
            return "CAUSE_REQUEST_TERMINATED";
        case CAUSE_NOT_ACCEPTABLE_HERE:
            return "CAUSE_NOT_ACCEPTABLE_HERE";
        case CAUSE_SERVER_INTERNAL_ERROR:
            return "CAUSE_SERVER_INTERNAL_ERROR";
        case CAUSE_NOT_IMPLEMENTED:
            return "CAUSE_NOT_IMPLEMENTED";
        case CAUSE_BAD_GATEWAY:
            return "CAUSE_BAD_GATEWAY";
        case CAUSE_SERVICE_UNAVAILABLE:
            return "CAUSE_SERVICE_UNAVAILABLE";
        case CAUSE_GATEWAY_TIMEOUT:
            return "CAUSE_GATEWAY_TIMEOUT";
        case CAUSE_VERSION_NOT_SUPPORTED:
            return "CAUSE_VERSION_NOT_SUPPORTED";
        case CAUSE_MESSAGE_TOO_LONG:
            return "CAUSE_MESSAGE_TOO_LONG";
        case CAUSE_BUSY_EVERYWHERE:
            return "CAUSE_BUSY_EVERYWHERE";
        case CAUSE_DECLINE:
            return "CAUSE_DECLINE";
        case CAUSE_DOES_NOT_EXIST_ANYWHERE:
            return "CAUSE_DOES_NOT_EXIST_ANYWHERE";
        case CAUSE_SESSION_NOT_ACCEPTABLE:
            return "CAUSE_SESSION_NOT_ACCEPTABLE";
        /// @}
        default:
            return DisconnectCause.toString(cause);
        }
    }

}
