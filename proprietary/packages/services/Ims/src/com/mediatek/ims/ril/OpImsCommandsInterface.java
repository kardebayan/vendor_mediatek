/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.ims.ril;

import android.os.Handler;
import android.os.Message;

public interface OpImsCommandsInterface {

    // Actions Interfaces below

    /**
     * Set RTT Mode
     * RIL_REQUEST_SET_RTT_MODE
     * @param mode Mode
     * @param response Response object
     */
    void setRttMode(int mode, Message response);

    /**
     * Send RTT Modify Request
     * RIL_REQUEST_SEND_RTT_MODIFY_REQUEST
     * @param callId Call id
     * @param newMode New mode
     * @param response Response object
     */
    void sendRttModifyRequest(int callId, int newMode, Message response);

    /**
     * Send RTT Text
     * RIL_REQUEST_SEND_RTT_TEXT
     * @param callId Call Id
     * @param text Text
     * @param response Response object
     */
    void sendRttText(int callId, String text, int length, Message response);

    /**
     * Request RTT Modify Response
     * RIL_REQUEST_RTT_MODIFY_REQUEST_RESPONSE
     * @param callId Call id
     * @param result Result
     * @param response Response object
     */
    void setRttModifyRequestResponse(int callId, int result, Message response);


    // URC Listener / Registrant Interfaces below

    /**
     * Registers the handler for GTT capability changed event.
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     *
     */
    void registerForGttCapabilityIndicator(Handler h, int what, Object obj);


    /**
     * Unregisters the handler for GTT capability changed event.
     *
     * @param h Handler for notification message.
     *
     */
    void unregisterForGttCapabilityIndicator(Handler h);

    /**
     * Registers the handler for Rtt Modify Response event.
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     *
     */
    void registerForRttModifyResponse(Handler h, int what, Object obj);

    /**
     * Unregisters the handler for Rtt Modify Response event.
     *
     * @param h Handler for notification message.
     *
     */
    void unregisterForRttModifyResponse(Handler h);

    /**
     * Registers the handler for Rtt Text Receive event.
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     *
     */
    void registerForRttTextReceive(Handler h, int what, Object obj);

    /**
     * Unregisters the handler for Rtt Text Receive event.
     *
     * @param h Handler for notification message.
     *
     */
    void unregisterForRttTextReceive(Handler h);

    /**
     * Registers the handler for Rtt Modify Request Receive event.
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     *
     */
    void registerForRttModifyRequestReceive(Handler h, int what, Object obj);

    /**
     * Unregisters the handler for Rtt Modify Request Receive event.
     *
     * @param h Handler for notification message.
     *
     */
    void unregisterForRttModifyRequestReceive(Handler h);

    /**
     * Dial a call from specific address
     * @param address
     * @param fromAddress
     * @param clirMode
     * @param isVideoCall
     * @param result
     */
    void dialFrom(String address, String fromAddress, int clirMode, boolean isVideoCall, Message
            result);

    /**
     * Send USSI from specific line
     * @param from
     * @param action
     * @param ussi
     * @param response
     */
    void sendUssiFrom(String from, int action, String ussi, Message response);

    /**
     * Cancel USSI from specific line
     * @param from
     * @param response
     */
    void cancelUssiFrom(String from, Message response);

    /**
     * Set Emergecny call config
     * @param category
     * @param isForceEcc
     * @param response
     */
    void setEmergencyCallConfig(int category, boolean isForceEcc, Message response);

    /**
     * Device switch
     * @param number
     * @param deviceId
     * @param response
     */
    void deviceSwitch(String number, String deviceId, Message response);

    /**
     * Cancel device switch
     * @param response
     */
    void cancelDeviceSwitch(Message response);

    /**
     * Device switch
     * @param callId call id
     * @param enable enable RTT audio indication
     * @param response
     */
    void enableRttAudioIndication(int callId, int enable, Message response);

    /**
     * Registers the handler for Audio Indication.
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     *
     */
    void registerForAudioIndication(Handler h, int what, Object obj);

    /**
     * Unregisters the handler for Audio Indication.
     *
     * @param h Handler for notification message.
     *
     */
    void unregisterForAudioIndication(Handler h);
}


