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

package com.mediatek.ims.internal;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsCallSession;
import android.telephony.ims.ImsReasonInfo;
import android.util.Log;



import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsCallSessionListener;

import java.util.Objects;

import com.mediatek.ims.internal.op.OpImsCallSessionBase;
import com.mediatek.internal.telephony.MtkOpCommonTelephonyCustFactoryBase;
import com.mediatek.internal.telephony.MtkOpTelephonyCustomizationUtils;

public class MtkImsCallSession extends ImsCallSession {
    private static final String TAG = "MtkImsCallSession";
    private final IMtkImsCallSession miMtkSession;
    private OpImsCallSessionBase mOpExt;

    public MtkImsCallSession(IImsCallSession iSession, IMtkImsCallSession iMtkSession) {
        miMtkSession = iMtkSession;
        miSession = iSession;

        if (iMtkSession == null || iSession == null) {
            mClosed = true;
            return;
        }

        try {
            miMtkSession.setListener(new IMtkImsCallSessionListenerProxy());
        } catch (RemoteException e) {
        }

        try {
            miSession.setListener(new IImsCallSessionListenerProxy());
        } catch (RemoteException e) {
        }

        mOpExt = MtkOpTelephonyCustomizationUtils.getOpCommonInstance().makeOpImsCallSession();
    }

    /**
     * Closes this object. This object is not usable after being closed.
     */
    public synchronized void close() {
        if (mClosed) {
            return;
        }

        try {
            miMtkSession.close();
            mClosed = true;
        } catch (RemoteException e) {
        }
    }

    /**
     * Sends a DTMF code. According to <a href="http://tools.ietf.org/html/rfc2833">RFC 2833</a>,
     * event 0 ~ 9 maps to decimal value 0 ~ 9, '*' to 10, '#' to 11, event 'A' ~ 'D' to 12 ~ 15,
     * and event flash to 16. Currently, event flash is not supported.
     *
     * @param c the DTMF to send. '0' ~ '9', 'A' ~ 'D', '*', '#' are valid inputs.
     */
    public void sendDtmf(char c, Message result) {
        if (mClosed) {
            return;
        }

        try {
            /// M: ALPS02321477 @{
            /// Google issue. Original sendDtmf could not pass Message.target to another process,
            /// because Message.writeToParcel didn't write target. Workaround this issue by adding
            /// a new API which passes target by Messenger.
            if (result != null && result.getTarget() != null) {
                Messenger target = new Messenger(result.getTarget());
                miMtkSession.sendDtmfbyTarget(c, result, target);
            } else {
                miSession.sendDtmf(c, result);
            }
            /// @}
        } catch (RemoteException e) {
        }
    }

    // For one-key conference MT displayed as incoming conference call.
    /**
     * Determines if the incoming session is multiparty.
     *
     * @return {@code True} if the incoming session is multiparty.
     * @hide
     */
    public boolean isIncomingCallMultiparty() {
        if (mClosed) {
            return false;
        }

        try {
            return miMtkSession.isIncomingCallMultiparty();
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Transfers the active & hold call.
     */
    public void explicitCallTransfer() {
        if (mClosed) {
            return;
        }

        try {
            miMtkSession.explicitCallTransfer();
        } catch (RemoteException e) {
            Log.e(TAG, "explicitCallTransfer: RemoteException!");
        }
    }

    /**
     * Transfers the active to specific number.
     *
     * @param number The transfer target number.
     * @param type ECT type
     */
    public void unattendedCallTransfer(String number, int type) {
        if (mClosed) {
            return;
        }

        try {
            miMtkSession.unattendedCallTransfer(number, type);
        } catch (RemoteException e) {
            Log.e(TAG, "explicitCallTransfer: RemoteException!");
        }
    }

    /**
     * Switch the active call to specific device.
     *
     * @param number The switch target number.
     * @param deviceId The switch target deviceId.
     * @throws ImsException if the IMS service fails to transfer the call
     */
    public void deviceSwitch(String number, String deviceId) {
        if (mClosed) {
            return;
        }

        try {
            miMtkSession.deviceSwitch(number, deviceId);
        } catch (RemoteException e) {
            Log.e(TAG, "deviceSwitch: RemoteException!");
        }
    }

    /**
     * Cancel switch the active call.
     */
    public void cancelDeviceSwitch() {
        if (mClosed) {
            return;
        }

        try {
            miMtkSession.cancelDeviceSwitch();
        } catch (RemoteException e) {
            Log.e(TAG, "cancelDeviceSwitch: RemoteException!");
        }
    }

    /**
     * A listener type for receiving notification on IMS call session events.
     * When an event is generated for an {@link IImsCallSession},
     * the application is notified by having one of the methods called on
     * the {@link IImsCallSessionListener}.
     */
    public class IMtkImsCallSessionListenerProxy extends IMtkImsCallSessionListener.Stub {
        @Override
        public void callSessionTransferred(IMtkImsCallSession session) {
            mListener.callSessionTransferred(MtkImsCallSession.this);
        }

        @Override
        public void callSessionTransferFailed(IMtkImsCallSession session,
                ImsReasonInfo reasonInfo) {
            mListener.callSessionTransferFailed(MtkImsCallSession.this, reasonInfo);
        }

        @Override
        public void callSessionTextCapabilityChanged(IMtkImsCallSession session,
                int localCapability, int remoteCapability,
                int localTextStatus, int realRemoteCapability) {
            mOpExt.callSessionTextCapabilityChanged(mListener, MtkImsCallSession.this,
                    localCapability, remoteCapability, localTextStatus, realRemoteCapability);
        }

        @Override
        public void callSessionRttEventReceived(IMtkImsCallSession session,
            int event) {
            mOpExt.callSessionRttEventReceived(mListener, MtkImsCallSession.this, event);
        }

        @Override
        public void callSessionDeviceSwitched(IMtkImsCallSession session) {
            mListener.callSessionDeviceSwitched(MtkImsCallSession.this);
        }

        @Override
        public void callSessionDeviceSwitchFailed(
                IMtkImsCallSession session, ImsReasonInfo reasonInfo) {
            mListener.callSessionDeviceSwitchFailed(MtkImsCallSession.this, reasonInfo);
        }

        /**
         * Notifies the start of a call merge operation.
         *
         * @param session The call session.
         * @param newSession The merged call session.
         * @param profile The call profile.
         */
        @Override
        public void callSessionMergeStarted(IMtkImsCallSession session,
                                            IMtkImsCallSession newSession, ImsCallProfile profile) {
            // This callback can be used for future use to add additional
            // functionality that may be needed between conference start and complete
            Log.d(TAG, "callSessionMergeStarted");
        }

        /**
         * Notifies the successful completion of a call merge operation.
         *
         * @param newSession The call session.
         */
        @Override
        public void callSessionMergeComplete(IMtkImsCallSession newSession) {
            if (mListener != null) {
                if (newSession != null) {
                    // Check if the active session is the same session that was
                    // active before the merge request was sent.
                    ImsCallSession validActiveSession = MtkImsCallSession.this;
                    try {
                        if (!Objects.equals(miSession.getCallId(), newSession.getCallId())) {

                            IImsCallSession aospImsCallSession = newSession.getIImsCallSession();

                            // New session created after conference
                            validActiveSession = new MtkImsCallSession(aospImsCallSession, newSession);
                        }
                    } catch (RemoteException rex) {
                        Log.e(TAG, "callSessionMergeComplete: exception for getCallId!");
                    }
                    mListener.callSessionMergeComplete(validActiveSession);
                } else {
                    // Session already exists. Hence no need to pass
                    mListener.callSessionMergeComplete(null);
                }
            }
        }

        /**
         * Notifies call redial as ECC.
         *
         * @param session The call session.
         */
        @Override
        public void callSessionRedialEcc(IMtkImsCallSession session) {
            Log.d(TAG, "callSessionRedialEcc");
            mListener.callSessionRedialEcc(MtkImsCallSession.this);
        }

    }
}
