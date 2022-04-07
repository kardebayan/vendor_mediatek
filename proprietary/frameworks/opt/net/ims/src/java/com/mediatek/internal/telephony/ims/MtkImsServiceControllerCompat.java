package com.mediatek.internal.telephony.ims;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.util.Log;

import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.ims.internal.IImsService;
import com.android.internal.telephony.ims.ImsConfigCompatAdapter;
import com.android.internal.telephony.ims.ImsRegistrationCompatAdapter;
import com.android.internal.telephony.ims.ImsServiceController;
import com.android.internal.telephony.ims.ImsServiceController.ImsServiceConnection;
import com.android.internal.telephony.ims.ImsServiceControllerStaticCompat;
import com.android.internal.telephony.ims.MmTelInterfaceAdapter;

import com.mediatek.ims.internal.IMtkImsService;

/**
 * Mtk Ims Service controller compat implementation.
 */
public class MtkImsServiceControllerCompat extends ImsServiceControllerStaticCompat {
    private static final String TAG = "MtkImsSCStaticCompat";

    private static final String MTK_IMS_SERVICE_NAME = "mtkIms";

    private IMtkImsService mMtkImsServiceCompat = null;
    private IBinder mMtkImsServiceBinder = null;

    private IBinder.DeathRecipient mMtkImsDeathRecipient = new IBinder.DeathRecipient() {
        public void binderDied() {
            Log.e(TAG, "ImsService(MtkImsServiceControllerCompat) died.");
        };
    };

    public MtkImsServiceControllerCompat(Context context, ComponentName componentName,
            ImsServiceController.ImsServiceControllerCallbacks callbacks) {
        super(context, componentName, callbacks);
    }

    @Override
    public boolean startBindToService(Intent intent, ImsServiceConnection connection, int flags) {
        Log.i(TAG, "startBindToService vendor");
        // go through the Add-on logic to bind "mtkims"
        IBinder binder = ServiceManager.checkService(MTK_IMS_SERVICE_NAME);

        if (binder == null) {
            Log.i(TAG, "get binder null");
            return false;
        }

        try {
            mMtkImsServiceBinder = binder;
            binder.linkToDeath(mMtkImsDeathRecipient, 0);
            mMtkImsServiceCompat = IMtkImsService.Stub.asInterface(binder);
        } catch (RemoteException e) {
            Log.e(TAG, "ImsService(MtkImsServiceControllerCompat) RemoteException:"
                    + e.getMessage());
            mMtkImsDeathRecipient.binderDied();
        }
        Log.i(TAG, "startBindToService default");
        // go through the AOSP logic to bind "ims"
        boolean ret = super.startBindToService(intent, connection, flags);
        return ret;
    }

    @Override
    protected void setServiceController(IBinder serviceController) {
        super.setServiceController(serviceController);
        if (serviceController == null) {
            // if super class set null, it's in a cleaning process
            mMtkImsServiceCompat = null;
        }
    }

    @Override
    public void unbind() throws RemoteException {
        Log.i(TAG, "unbind");
        if (mMtkImsServiceBinder != null) {
            mMtkImsServiceBinder.unlinkToDeath(mMtkImsDeathRecipient, 0);
            mMtkImsServiceBinder = null;
        }
        super.unbind();
    }

    @Override
    protected boolean isServiceControllerAvailable() {
        Log.d(TAG, "isServiceControllerAvailable-mImsServiceCompat: " + mImsServiceCompat
                + ", mMtkImsServiceCompat:" + mMtkImsServiceCompat);
        return (mImsServiceCompat != null && mMtkImsServiceCompat != null);
    }

    /**
     * Create mtk MMTelCompat
     */
    @Override
    protected IImsMmTelFeature createMMTelCompat(int slotId, IImsFeatureStatusCallback c)
            throws RemoteException {
        MmTelInterfaceAdapter interfaceAdapter = getInterface(slotId, c);
        MtkMmTelFeatureCompatAdapter mmTelAdapter = new MtkMmTelFeatureCompatAdapter(mContext,
                slotId, interfaceAdapter);
        mMmTelCompatAdapters.put(slotId, mmTelAdapter);
        ImsRegistrationCompatAdapter regAdapter = new ImsRegistrationCompatAdapter();
        mmTelAdapter.addRegistrationAdapter(regAdapter);
        mRegCompatAdapters.put(slotId, regAdapter);
        mConfigCompatAdapters.put(slotId,
                new ImsConfigCompatAdapter(mmTelAdapter.getOldConfigInterface()));
        return mmTelAdapter.getBinder();

    }

    /*
    Un-used in case of dynamic binding supported. Mark to avoid build error.
    @Override
    protected MmTelInterfaceAdapter getInterface(int slotId, IImsFeatureStatusCallback c)
            throws RemoteException {
        if (mImsServiceCompat == null) {
            Log.w(TAG, "getInterface: IImsService returned null.");
            return null;
        }
        if (mMtkImsServiceCompat == null) {
            Log.w(TAG, "getInterface: IMtkImsService returned null.");
            return null;
        }
        Log.d(TAG, "mImsServiceCompat: " + mImsServiceCompat + ", mMtkImsServiceCompat: " + mMtkImsServiceCompat);
        return new MtkImsServiceInterfaceAdapter(slotId, mImsServiceCompat.asBinder(),
                mMtkImsServiceCompat.asBinder());
    }*/
}
