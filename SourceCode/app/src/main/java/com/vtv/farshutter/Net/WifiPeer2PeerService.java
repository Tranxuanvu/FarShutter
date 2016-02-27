package com.vtv.farshutter.Net;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by choxu on 026/26/2/2016.
 */
public class WifiPeer2PeerService extends Service {
    private  IBinder mBinder = new WifiPeer2PeerBinder();



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class WifiPeer2PeerBinder extends Binder {
        WifiPeer2PeerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return WifiPeer2PeerService.this;
        }
    }
}
