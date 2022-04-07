package com.mediatek.ims;

import com.android.ims.ImsConnectionStateListener;
import android.telephony.ims.feature.MmTelFeature;

public class MtkImsConnectionStateListener extends ImsConnectionStateListener {

    /**
     * M: Called when IMS emergency capability changed.
     */
    public void onImsEmergencyCapabilityChanged(boolean eccSupport) {
        // no-op
    }

    /**
     * M: Called when VoWifi wifi PDN Out Of Service state changed.
     */
    public void onWifiPdnOOSStateChanged(int oosState) {
        // no-op
    }

    /**
         * The status of the feature's capabilities has changed to either available or unavailable.
         * If unavailable, the feature is not able to support the unavailable capability at this
         * time.
         *
         * @param config The new availability of the capabilities.
         */
        public void onCapabilitiesStatusChanged(MmTelFeature.Capabilities config) {
        }
}
