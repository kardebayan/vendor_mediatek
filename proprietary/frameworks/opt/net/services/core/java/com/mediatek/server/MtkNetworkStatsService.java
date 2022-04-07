/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2016. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.mediatek.server;

import static android.net.TrafficStats.MB_IN_BYTES;
import static android.net.TrafficStats.PB_IN_BYTES;
import static android.provider.Settings.Global.NETSTATS_GLOBAL_ALERT_BYTES;

import android.app.AlarmManager;
import android.content.Context;
import android.net.NetworkIdentity;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Slog;
import java.time.Clock;

import com.android.server.net.NetworkStatsObservers;
import com.android.server.net.NetworkStatsService;
import com.android.server.net.NetworkStatsService.NetworkStatsSettings;

import java.io.File;

public class MtkNetworkStatsService extends NetworkStatsService {
    private static final String TAG = MtkNetworkStatsService.class.getSimpleName();

    static final int SUBSCRIPTION_OR_SIM_CHANGED = 0;

    private Context mContext;
    private HandlerThread mHandlerThread;
    private InternalHandler mHandler;
    private long mEmGlobalAlert = 2 * MB_IN_BYTES;

    public MtkNetworkStatsService(Context context, INetworkManagementService networkManager,
            AlarmManager alarmManager, PowerManager.WakeLock wakeLock, Clock clock,
            TelephonyManager teleManager, NetworkStatsSettings settings,
            NetworkStatsObservers statsObservers, File systemDir, File baseDir) {
        super(context, networkManager, alarmManager, wakeLock, clock, teleManager,
            settings, statsObservers, systemDir, baseDir);
        Slog.d(TAG, "MtkNetworkStatsService starting up");
        mContext = context;
        initDataUsageIntent(context);
    }

    private void initDataUsageIntent(Context context) {
        mHandlerThread = new HandlerThread("NetworkStatInternalHandler");
        mHandlerThread.start();
        mHandler = new InternalHandler(mHandlerThread.getLooper());

        SubscriptionManager.from(context)
            .addOnSubscriptionsChangedListener(mOnSubscriptionsChangedListener);
    }


    // M: Add support for Multiple ViLTE
    protected void rebuildActiveVilteIfaceMap() {
        //Do not use subId anymore.
    }

    // return false to use AOSP procedure
    protected boolean findOrCreateMultipleVilteNetworkIdentitySets(NetworkIdentity vtIdent) {
        // Also check MtkImsPhoneCallTracker for interface modification
        findOrCreateNetworkIdentitySet(mActiveIfaces,
            getVtInterface(vtIdent.getSubscriberId())).add(vtIdent);
        findOrCreateNetworkIdentitySet(mActiveUidIfaces,
            getVtInterface(vtIdent.getSubscriberId())).add(vtIdent);
        return true;
    }

    private String getVtInterface(String subscribeId) {
        return VT_INTERFACE + subscribeId;
    }

    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SUBSCRIPTION_OR_SIM_CHANGED:
                    handleSimChange();
                    break;
                default:
                    break;
            }
        }
    }

    private void handleSimChange() {
        boolean isTestSim = isTestSim();
        mEmGlobalAlert = Settings.Global.getLong(mContext.getContentResolver(),
                NETSTATS_GLOBAL_ALERT_BYTES, 0);
        if (isTestSim) {
            if (mEmGlobalAlert != 2 * PB_IN_BYTES) {
                Settings.Global.putLong(mContext.getContentResolver(),
                    NETSTATS_GLOBAL_ALERT_BYTES, 2 * PB_IN_BYTES);
                advisePersistThreshold(Long.MAX_VALUE / 1000);
                Slog.d(TAG, "Configure for test sim with 2TB");
            }
        } else {
            if (mEmGlobalAlert == 2 * PB_IN_BYTES) {
                Settings.Global.putLong(mContext.getContentResolver(),
                    NETSTATS_GLOBAL_ALERT_BYTES, 2 * MB_IN_BYTES);
                advisePersistThreshold(Long.MAX_VALUE / 1000);
                Slog.d(TAG, "Restore for test sim with 2MB");
            }
        }
    }

    private final OnSubscriptionsChangedListener mOnSubscriptionsChangedListener =
            new OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            mHandler.sendEmptyMessage(SUBSCRIPTION_OR_SIM_CHANGED);
        }
    };

    public static boolean isTestSim() {
        boolean isTestSim = false;
        isTestSim = SystemProperties.get("vendor.gsm.sim.ril.testsim").equals("1") ||
                   SystemProperties.get("vendor.gsm.sim.ril.testsim.2").equals("1") ||
                   SystemProperties.get("vendor.gsm.sim.ril.testsim.3").equals("1") ||
                   SystemProperties.get("vendor.gsm.sim.ril.testsim.4").equals("1");
        return isTestSim;
    }

}