package com.mediatek.internal.telephony.ims;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.RemoteException;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.stub.ImsSmsImplBase;
import android.util.Log;

import com.android.ims.ImsManager;
import com.android.ims.internal.IImsRegistrationListener;
import com.android.ims.internal.IImsCallSession;
import com.android.internal.telephony.ims.MmTelFeatureCompatAdapter;
import com.android.internal.telephony.ims.MmTelInterfaceAdapter;
import com.mediatek.ims.internal.MtkImsManager;

/**
 * Provides APIs for MTK IMS services or modified AOSP APIs.
 *
 * @hide
 */
public class MtkMmTelFeatureCompatAdapter extends MmTelFeatureCompatAdapter {
    private int mSlotId = -1;
    private MtkImsManager mImsManager = null;

    private static final String TAG = "MtkMmTelFeatureCompat";
    // Handle Incoming Call as PendingIntent, the old method
    public MtkMmTelFeatureCompatAdapter(Context context, int slotId,
                                        MmTelInterfaceAdapter compatFeature) {
        super(context, slotId, compatFeature);
        mSlotId = slotId;
        mImsManager = (MtkImsManager) ImsManager.getInstance(context, slotId);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "onReceive");
                if (intent.getAction().equals(ACTION_IMS_INCOMING_CALL)) {
                    Log.i(TAG, "onReceive : incoming call intent. mSessionId:" + mSessionId);

                    String callId = intent.getStringExtra("android:imsCallID");
                    int incomingServiceId = getImsSessionId(intent);
                    if (mSessionId != incomingServiceId) {
                        Log.w(TAG, "onReceive : Service id is mismatched the incoming call intent");
                        return;
                    }
                    try {
                        IImsCallSession session = mCompatFeature.getPendingCallSession(mSessionId,
                                callId);
                        notifyIncomingCallSession(session, intent.getExtras());
                    } catch (RemoteException e) {
                        Log.w(TAG, "onReceive: Couldn't get Incoming call session.");
                    }
                }
            }
        };
        /**
         * Override the registration listener extend from AOSP for extensions, such as ims
         * emergency capability
         */
        mListener = new IImsRegistrationListener.Stub() {
            @Override
            public void registrationConnected() throws RemoteException {
                // Implemented in the Registration Adapter
            }

            @Override
            public void registrationProgressing() throws RemoteException {
                // Implemented in the Registration Adapter
            }

            @Override
            public void registrationConnectedWithRadioTech(int imsRadioTech)
                    throws RemoteException {
                // Implemented in the Registration Adapter
            }

            @Override
            public void registrationProgressingWithRadioTech(int imsRadioTech)
                    throws RemoteException {
                // Implemented in the Registration Adapter
            }

            @Override
            public void registrationDisconnected(ImsReasonInfo imsReasonInfo)
                    throws RemoteException {
                // Implemented in the Registration Adapter
            }

            @Override
            public void registrationResumed() throws RemoteException {
                // Don't care
            }

            @Override
            public void registrationSuspended() throws RemoteException {
                // Don't care
            }

            @Override
            public void registrationServiceCapabilityChanged(int serviceClass, int event)
                    throws RemoteException {
                MtkMmTelFeatureCompatAdapter.this.mImsManager
                        .notifyRegServiceCapabilityChangedEvent(event);
            }

            @Override
            public void registrationFeatureCapabilityChanged(int serviceClass,
                    int[] enabledFeatures, int[] disabledFeatures) throws RemoteException {
                notifyCapabilitiesStatusChanged(convertCapabilities(enabledFeatures));
            }

            @Override
            public void voiceMessageCountUpdate(int count) throws RemoteException {
                notifyVoiceMessageCountUpdate(count);
            }

            @Override
            public void registrationAssociatedUriChanged(Uri[] uris) throws RemoteException {
                // Implemented in the Registration Adapter
            }

            @Override
            public void registrationChangeFailed(int targetAccessTech,
                    ImsReasonInfo imsReasonInfo) throws RemoteException {
                // Implemented in the Registration Adapter
            }
        };
    }

    /**
     * Gets the service type from the specified incoming call broadcast intent.
     * 
     * @param incomingCallIntent the incoming call broadcast intent
     * @return the service identifier or -1 if the intent does not contain it
     */
    private static int getImsSessionId(Intent incomingCallIntent) {
        if (incomingCallIntent == null) {
            return (-1);
        }

        return incomingCallIntent.getIntExtra(ImsManager.EXTRA_SERVICE_ID, -1);
    }
}
