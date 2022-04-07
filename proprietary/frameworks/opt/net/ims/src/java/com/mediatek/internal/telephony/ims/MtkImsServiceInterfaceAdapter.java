package com.mediatek.internal.telephony.ims;

import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;

import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.stub.ImsSmsImplBase;
import android.util.Log;

import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsRegistrationListener;
import com.android.ims.internal.IImsUt;
import com.android.internal.telephony.ims.ImsServiceInterfaceAdapter;
import com.mediatek.ims.internal.IMtkImsService;

/**
 * Mtk Ims service interface adapter.
 */
public class MtkImsServiceInterfaceAdapter extends ImsServiceInterfaceAdapter {
    private static final String LOG_TAG = "MtkImsSrvAdapter";
    private int mServiceId = -1;

    private IBinder mBinderMtk;

    public MtkImsServiceInterfaceAdapter(int slotId, IBinder binderAosp, IBinder binderMtk) {
        super(slotId, binderAosp);
        mBinderMtk = binderMtk;
    }

    public int startSession(android.app.PendingIntent incomingCallIntent,
            IImsRegistrationListener listener) throws RemoteException {
        mServiceId = super.startSession(incomingCallIntent, listener);
        return mServiceId;
    };

    @Override
    public void endSession(int sessionId) throws RemoteException {
        super.endSession(sessionId);
        mServiceId = -1;
    }

    @Override
    public boolean isConnected(int callSessionType, int callType) throws RemoteException {
        return getInterface().isConnected(mServiceId, callSessionType, callType);
    }

    @Override
    public boolean isOpened() throws RemoteException {
        return getInterface().isOpened(mServiceId);
    }

    @Override
    public IImsUt getUtInterface() throws RemoteException {
        return getInterface().getUtInterface(mServiceId);
    }

    @Override
    public IImsEcbm getEcbmInterface() throws RemoteException {
        return getInterface().getEcbmInterface(mServiceId);
    }

    @Override
    public void setUiTTYMode(int uiTtyMode, Message onComplete) throws RemoteException {
        getInterface().setUiTTYMode(mServiceId, uiTtyMode, onComplete);
    }

    @Override
    public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
        return getInterface().getMultiEndpointInterface(mServiceId);
    }

    public void setSmsListener(IImsSmsListener listener) {
        try {
            ((IMtkImsService) getMtkInterface()).addImsSmsListener(mSlotId, listener);
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "Fail to setSmsListener " + ex);
        }
    }

    public void sendSms(int token, int messageRef, String format, String smsc, boolean isRetry,
            byte[] pdu) {
        try {
            ((IMtkImsService) getMtkInterface()).sendSms(mSlotId, token, messageRef, format, smsc,
                    isRetry, pdu);
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "Fail to send SMS over IMS " + ex);
        }
    }

    public void acknowledgeSms(int token, int messageRef,
            @ImsSmsImplBase.DeliverStatusResult int result) {
    }

    public void acknowledgeSmsReport(int token, int messageRef,
            @ImsSmsImplBase.StatusReportResult int result) {
    }

    public void onSmsReady() {
    }

    public String getSmsFormat() {
        return "";
    }

    private IMtkImsService getMtkInterface() throws RemoteException {
        IMtkImsService feature = IMtkImsService.Stub.asInterface(mBinderMtk);
        if (feature == null) {
            throw new RemoteException("Binder not Available");
        }
        return feature;
    }
}
