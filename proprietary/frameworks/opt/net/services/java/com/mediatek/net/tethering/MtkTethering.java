/*
 * Copyright (C) 2017 MediaTek Inc.
 *
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 */
/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.net.tethering;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.NetworkState;
import android.net.RouteInfo;
import android.net.util.InterfaceSet;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.messages.nano.SystemMessageProto.SystemMessage;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.connectivity.Tethering;

import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import mediatek.net.wifi.HotspotClient;
import mediatek.net.wifi.WifiHotspotManager;
import vendor.mediatek.hardware.netdagent.V1_0.INetdagent;


/**
 *
 * @hide
 */
public class MtkTethering {
    private static final boolean DBG = false;
    private static final String TAG = "MtkTethering";


    static final int EVENT_TETHER_STATUS = 0;
    static final int EVENT_HOTSPOT_STATUS = 1;
    static final int EVENT_LOCALE_CHANGED = 2;
    static final int EVENT_BOOTUP = 3;

    private final Context mContext;
    private final BroadcastReceiver mStateReceiver;
    private static Tethering sTethering;

    protected final HandlerThread mHandlerThread;
    final private InternalHandler mHandler;

    private String[] mWifiRegexs;
    private Notification.Builder mTetheredNotificationBuilder;
    private int mLastNotificationId;

    public MtkTethering(Context context, Tethering tethering) {
        mContext = context;
        sTethering = tethering;

        mHandlerThread = new HandlerThread("TetheringInternalHandler");
        mHandlerThread.start();
        mHandler = new InternalHandler(mHandlerThread.getLooper());

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        filter.addAction(WifiHotspotManager.WIFI_HOTSPOT_CLIENTS_CHANGED_ACTION);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        mStateReceiver = new StateReceiver();
        mContext.registerReceiver(mStateReceiver, filter, null, mHandler);

        mHandler.sendEmptyMessage(EVENT_BOOTUP);
    }

    // must be stateless - things change under us.
    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_TETHER_STATUS:
                    showTetheredNotification((boolean) msg.obj);
                    break;
                case EVENT_HOTSPOT_STATUS:
                    updateTetheredNotification();
                    break;
                case EVENT_LOCALE_CHANGED:
                    updateAospNotificatin();
                    updateTetheredNotification();
                    break;
               case EVENT_BOOTUP:
                    checkEmSetting();
                    break;
            }
        }
    }

    private void checkEmSetting() {
        boolean isBgdataDisabled =
                SystemProperties.getBoolean("persist.vendor.radio.bgdata.disabled", false);
        if (isBgdataDisabled) {
            try {
                INetdagent netagent = INetdagent.getService();
                if (netagent == null) {
                    Log.e(TAG, "netagent is null");
                    return;
                }
                Log.d(TAG, "setIotFirewall");
                netagent.dispatchNetdagentCmd("netdagent firewall set_nsiot_firewall");
            } catch (Exception e) {
                Log.d(TAG, "Exception:" + e);
            }
        }
    }

    private void updateAospNotificatin() {
        try {
            Method method = sTethering.getClass().getDeclaredMethod(
                    "clearTetheredNotification", (Class[]) null);
            method.setAccessible(true);
            method.invoke(sTethering);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        try {
            Method method = sTethering.getClass().getDeclaredMethod(
                    "sendTetherStateChangedBroadcast", (Class[]) null);
            method.setAccessible(true);
            method.invoke(sTethering);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    private class StateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent) {
            final String action = intent.getAction();
            if (action == null) return;
            Log.i(TAG, "Intent:" + intent);
            if (action.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED)) {
                handleTetherStatus(intent);
            } else if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
                mHandler.sendEmptyMessage(EVENT_LOCALE_CHANGED);
            }  else if (action.equals(WifiHotspotManager.WIFI_HOTSPOT_CLIENTS_CHANGED_ACTION)) {
                mHandler.sendEmptyMessage(EVENT_HOTSPOT_STATUS);
            }
        }
    }

    private void handleTetherStatus(Intent intent) {
        boolean wifiTethered = false;

        String[] wifiRegexs = sTethering.getTetherableWifiRegexs();
        ArrayList<String> activeTetherIfaces = intent.getStringArrayListExtra(
                ConnectivityManager.EXTRA_ACTIVE_TETHER);
        if (activeTetherIfaces != null) {
            for (String s : activeTetherIfaces) {
                for (String regex : wifiRegexs) {
                    if (s.matches(regex)) wifiTethered = true;
                }
            }
        }
        Message msg = Message.obtain(
                mHandler, EVENT_TETHER_STATUS, 0, 0, wifiTethered);
        msg.sendToTarget();
    }

    private void showTetheredNotification(boolean isShowed) {
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        Log.i(TAG, "showTetheredNotification:" + isShowed + ":" + mLastNotificationId);

        if (!isShowed && mLastNotificationId != 0) {
            notificationManager.cancelAsUser(null, mLastNotificationId,
                    UserHandle.ALL);
            mLastNotificationId = 0;
            Log.i(TAG, "Disable notification");
            return;
        } else if (!isShowed) {
            Log.i(TAG, "Ignore");
            return;
        }

        int icon = com.android.internal.R.drawable.stat_sys_tether_general;
        if (mLastNotificationId != 0) {
            if (mLastNotificationId == icon) {
                return;
            }
            notificationManager.cancelAsUser(null, mLastNotificationId,
                    UserHandle.ALL);
            mLastNotificationId = 0;
        }

        Intent intent = new Intent();
        intent.setClassName("com.android.settings",
                "com.android.settings.TetherSettings");
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        PendingIntent pi = PendingIntent.getActivityAsUser(mContext, 0, intent, 0,
                null, UserHandle.CURRENT);

        Resources r = Resources.getSystem();
        CharSequence title = r.getText(
                com.android.internal.R.string.tethered_notification_title);
        String message = getHotspotClientInfo(r);

        if (mTetheredNotificationBuilder == null) {
            mTetheredNotificationBuilder =
                    new Notification.Builder(mContext, SystemNotificationChannels.NETWORK_STATUS);
            mTetheredNotificationBuilder.setWhen(0)
                    .setOngoing(true)
                    .setColor(mContext.getColor(
                            com.android.internal.R.color.system_notification_accent_color))
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setCategory(Notification.CATEGORY_STATUS);
        }
        mTetheredNotificationBuilder.setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pi);
        mLastNotificationId = SystemMessage.NOTE_TETHER_GENERAL;

        notificationManager.notifyAsUser(null, mLastNotificationId,
                mTetheredNotificationBuilder.build(), UserHandle.ALL);
    }

    private void updateTetheredNotification() {
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null || mTetheredNotificationBuilder == null
            || mLastNotificationId == 0) {
            return;
        }

        Resources r = Resources.getSystem();
        CharSequence title = r.getText(
                com.android.internal.R.string.tethered_notification_title);
        String message = getHotspotClientInfo(r);

        mTetheredNotificationBuilder.setContentTitle(title).setContentText(message);
        notificationManager.notifyAsUser(null, mLastNotificationId,
                mTetheredNotificationBuilder.build(), UserHandle.ALL);
    }

    private String getHotspotClientInfo(Resources r) {
        String message = "";
        int connected = 0;
        int blocked = 0;

        try {
            message = r.getString(
                        com.mediatek.internal.R.string.tethered_notification_message_for_hotspot,
                        connected, blocked);

            WifiManager mgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            if (mgr == null) {
                return message;
            }
            WifiHotspotManager hotspotMgr = mgr.getWifiHotspotManager();
            if (mgr == null) {
                return message;
            }
            List<HotspotClient> clients = hotspotMgr.getHotspotClients();
            if (clients != null) {
                for (HotspotClient client : clients) {
                    if (client.isBlocked) {
                        blocked++;
                    }
                }
                connected = clients.size() - blocked;
            }
            Log.i(TAG, "getHotspotClientInfo:" + connected + ":" + blocked);
            message = r.getString(
                    com.mediatek.internal.R.string.tethered_notification_message_for_hotspot,
                    connected, blocked);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return message;
    }

    /// M: [ALPS04058632] allow IPv6 ULA for auto testing
    // The following three methods are copied from TetheringInterfaceUtils.
    /**
     * Get upstream interfaces for tethering based on default routes for IPv4/IPv6.
     * @return null if there is no usable interface, or a set of at least one interface otherwise.
     */
    public InterfaceSet getTetheringInterfaces(NetworkState ns) {
        if (ns == null) {
            return null;
        }

        final LinkProperties lp = ns.linkProperties;
        final String if4 = getInterfaceForDestination(lp, Inet4Address.ANY);
        final String if6 = getIPv6Interface(ns);
        Log.d(TAG, "getTetheringInterfaces if4: " + if4 + " if6: " + if6);

        return (if4 == null && if6 == null) ? null : new InterfaceSet(if4, if6);
    }

    /**
     * Get the upstream interface for IPv6 tethering.
     * @return null if there is no usable interface, or the interface name otherwise.
     */
    public String getIPv6Interface(NetworkState ns) {
        // Broadly speaking:
        //
        //     [1] does the upstream have an IPv6 default route?
        //
        // and
        //
        //     [2] does the upstream have one or more global IPv6 /64s
        //         dedicated to this device?
        //
        // In lieu of Prefix Delegation and other evaluation of whether a
        // prefix may or may not be dedicated to this device, for now just
        // check whether the upstream is TRANSPORT_CELLULAR. This works
        // because "[t]he 3GPP network allocates each default bearer a unique
        // /64 prefix", per RFC 6459, Section 5.2.
        final boolean canTether =
                (ns != null) && (ns.network != null) &&
                (ns.linkProperties != null) && (ns.networkCapabilities != null) &&
                /// M: [ALPS04061619] do not check DNS for auto test
                // At least one upstream DNS server:
                // ns.linkProperties.hasIPv6DnsServer() &&
                // Minimal amount of IPv6 provisioning:
                hasIPv6GlobalAddress(ns.linkProperties) &&
                // Temporary approximation of "dedicated prefix":
                ns.networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

        return canTether
                ? getInterfaceForDestination(ns.linkProperties, Inet6Address.ANY)
                : null;
    }

    private String getInterfaceForDestination(LinkProperties lp, InetAddress dst) {
        final RouteInfo ri = (lp != null)
                ? RouteInfo.selectBestRoute(lp.getAllRoutes(), dst)
                : null;
        return (ri != null) ? ri.getInterface() : null;
    }

    private boolean hasIPv6GlobalAddress(LinkProperties lp) {
        for (InetAddress address : lp.getAllAddresses()) {
            if (address instanceof Inet6Address &&
                !address.isAnyLocalAddress() &&
                !address.isLoopbackAddress() &&
                !address.isLinkLocalAddress() &&
                !address.isSiteLocalAddress() &&
                !address.isMulticastAddress()) {
                return true;
            }
        }
        return false;
    }
}
