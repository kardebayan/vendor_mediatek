/*
 * Copyright (C) 2014 MediaTek Inc.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
*/

package com.mediatek.net.ip;

import android.content.Context;
import android.net.LinkProperties;
import android.net.ip.IpClient;
import android.os.Message;
import android.util.Log;

import com.android.internal.util.State;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * MtkIpRunningState
 *
 * This class provides the mechanism to trigger DHCPv6 procedure.
 *
 * @hide
 */
public class MtkIpStoppedState extends State {
    private static final String TAG = "MtkIpStoppedState";
    private static final String WLAN_INTERFACE = "wlan0";
    private static final boolean DBG = false;
    private static final boolean VDBG = false;

    // Refer from IpClient.java
    private static final int CMD_STOP = 1;
    private static final int CMD_START = 2;
    private static final int CMD_CONFIRM = 3;

    private final String mIfaceName;
    private final Context   mContext;
    private final IpClient mIpClient;
    private State mIpStoppedState;
    private boolean mIsWifiSMStarted = false;

    public MtkIpStoppedState(Context ctx, IpClient ipClient, String ifName, State sst) {
        super();
        Log.d(TAG, "Initialize MtkIpStoppedState");
        mContext = ctx;
        mIpClient = ipClient;
        mIfaceName = ifName;
        mIpStoppedState = sst;
    }

    @Override
    public void enter() {
        Log.d(TAG, "enter");

        // Need to call StoppedState.enter function firstly
        mIpStoppedState.enter();

        if (mIsWifiSMStarted) {
            Log.d(TAG, "resetLinkProperties");
            updateLinkPropertiesChange();
        }
    }

    @Override
    public void exit() {
        Log.d(TAG, "exit");
        mIpStoppedState.exit();
    }

    @Override
    public boolean processMessage(Message msg) {
        switch (msg.what) {
            case CMD_START:
                mIsWifiSMStarted = true;
                break;
            default:
                break;
        }
        return mIpStoppedState.processMessage(msg);
    }

    private void updateLinkPropertiesChange() {
        try {
            // Get mCallback
            Field field = mIpClient.getClass().getDeclaredField("mCallback");
            field.setAccessible(true);
            Object configuration = field.get(mIpClient);
            // Get mLinkProperties
            Field field2 = mIpClient.getClass().getDeclaredField("mLinkProperties");
            field2.setAccessible(true);
            LinkProperties linkProperty = (LinkProperties) field2.get(mIpClient);

            // Call mCallback.onLinkPropertiesChange(mLinkProperties)
            Method method = configuration.getClass().getDeclaredMethod(
                    "onLinkPropertiesChange", LinkProperties.class);
            method.setAccessible(true);
            method.invoke(configuration, linkProperty);
            Log.d(TAG, "updateLinkPropertiesChange for static IP");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
