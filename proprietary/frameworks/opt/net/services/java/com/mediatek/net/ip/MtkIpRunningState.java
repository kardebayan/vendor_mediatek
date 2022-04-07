/*
 * Copyright (C) 2014 MediaTek Inc.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
*/


package com.mediatek.net.ip;

import android.content.Context;
import android.net.StaticIpConfiguration;
import android.net.dhcp.DhcpClient;
import android.net.ip.IpClient;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.util.State;
import com.android.server.net.NetlinkTracker;

import com.mediatek.net.dhcp.MtkDhcp6Client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * MtkIpRunningState
 *
 * This class provides the mechanism to trigger DHCPv6 procedure.
 *
 * @hide
 */
public class MtkIpRunningState extends State {
    private static final String TAG = "MtkIpRunningState";
    private static final String WLAN_INTERFACE = "wlan0";
    private static final boolean DBG = false;
    private static final boolean VDBG = false;

    // Refer from IpClient.java
    private static final int EVENT_PRE_DHCP_ACTION_COMPLETE = 4;

    private final String mIfaceName;
    private final Context   mContext;
    private final IpClient mIpClient;
    private final NetlinkTracker mNetlinkTracker;
    private StaticIpConfiguration mStaticIpConfig;
    private MtkDhcp6Client mDhcp6Client;
    private ArrayList<InetAddress> mDnsV6Servers;
    private State mIpRunningState;

    private static final boolean sMtkDhcpv6cWifi =
        SystemProperties.get("ro.vendor.mtk_dhcpv6c_wifi").equals("1");

    public MtkIpRunningState(Context ctx, IpClient ipClient, String ifName,
        NetlinkTracker netlinkTracker, State sst) {
        super();

        Log.d(TAG, "Initialize MtkIpRunningState");
        mContext = ctx;
        mIpClient = ipClient;
        mIfaceName = ifName;
        mNetlinkTracker = netlinkTracker;
        mIpRunningState = sst;
    }

    @Override
    public void enter() {
        mIpRunningState.enter();

        mStaticIpConfig = getIpConfiguration();
        if (isDhcp6Support() && mStaticIpConfig == null) {
            mDhcp6Client = MtkDhcp6Client.makeDhcp6Client(
                                mContext, mIpClient, mIfaceName);
            mDhcp6Client.registerForPreDhcpNotification();
            mDhcp6Client.sendMessage(MtkDhcp6Client.CMD_START_DHCP);
        }
        Log.d(TAG, "enter");
    }

    @Override
    public void exit() {
        mIpRunningState.exit();
        if (isDhcp6Support(mDhcp6Client)) {
            mDhcp6Client.sendMessage(MtkDhcp6Client.CMD_STOP_DHCP);
            mDhcp6Client.doQuit();
        }
        Log.d(TAG, "exit");
    }

    @Override
    public boolean processMessage(Message msg) {
        switch (msg.what) {
            case EVENT_PRE_DHCP_ACTION_COMPLETE:
                if (isDhcp6Support(mDhcp6Client)) {
                    mDhcp6Client.sendMessage(
                            MtkDhcp6Client.CMD_PRE_DHCP_ACTION_COMPLETE);
                }
                break;
            case MtkDhcp6Client.CMD_CONFIGURE_DNSV6:
                mDnsV6Servers = (ArrayList<InetAddress>) msg.obj;
                String[] dnsV6Servers = new String[mDnsV6Servers.size()];
                for (int i = 0; i < mDnsV6Servers.size(); i++) {
                    dnsV6Servers[i] = mDnsV6Servers.get(i).getHostAddress();
                }
                mNetlinkTracker.interfaceDnsServerInfo(mIfaceName, 3600, dnsV6Servers);
                try {
                    Method method = mIpClient.getClass().getDeclaredMethod(
                            "handleLinkPropertiesUpdate", boolean.class);
                    method.setAccessible(true);
                    method.invoke(mIpClient, true);
                } catch (ReflectiveOperationException e) {
                    e.printStackTrace();
                }
                return HANDLED;
            case MtkDhcp6Client.CMD_ON_QUIT:
                // DHCPv6 quit early for some reason.
                Log.e(TAG, "Unexpected v6 CMD_ON_QUIT.");
                mDhcp6Client = null;
                mDnsV6Servers = null;
                return HANDLED;
            case DhcpClient.CMD_CLEAR_LINKADDRESS:
                if (mStaticIpConfig != null) {
                    Log.d(TAG, "static Ip is configured, ignore clearIPv4Address");
                    return HANDLED;
                }
                break;
            default:
                break;
        }
        return mIpRunningState.processMessage(msg);
    }

    private boolean isDhcp6Support() {
        if (sMtkDhcpv6cWifi) {
            return WLAN_INTERFACE.equals(mIfaceName);
        }
        return false;
    }

    private boolean isDhcp6Support(MtkDhcp6Client client) {
        if (sMtkDhcpv6cWifi && client != null) {
            return WLAN_INTERFACE.equals(mIfaceName);
        }
        return false;
    }

    private StaticIpConfiguration getIpConfiguration() {
        StaticIpConfiguration staticIpConfig = null;
        try {
            Field field = mIpClient.getClass().getDeclaredField("mConfiguration");
            field.setAccessible(true);
            Object configuration = field.get(mIpClient);
            Field field2 = configuration.getClass().getDeclaredField("mStaticIpConfig");
            field2.setAccessible(true);
            staticIpConfig = (StaticIpConfiguration) field2.get(configuration);
            Log.d(TAG, "getIpConfiguration:" + staticIpConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return staticIpConfig;
    }
}
