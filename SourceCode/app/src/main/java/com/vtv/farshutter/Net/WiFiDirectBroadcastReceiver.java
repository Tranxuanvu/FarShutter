package com.vtv.farshutter.Net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;

/**
 * Created by choxu on 026/26/2/2016.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiDirectListener mListener;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
    }

    @Override
    public void onReceive(Context context, final Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            if (mListener != null) {
                mListener.onStateChanged(intent);
                mListener.onWifiStateChange(intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1) == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (mManager != null) {
                mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        if (mListener != null) {
                            mListener.onPeerChanged(intent, peers);
                        }
                    }
                });
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (mListener != null) {
                mListener.onConnectChanged(intent);
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            if (mListener != null) {
                mListener.onDeviceChanged(intent);
            }
        }
    }

    public void setWifiListener(WifiDirectListener listener) {
        this.mListener = listener;
    }

    interface WifiDirectListener {
        void onStateChanged(Intent intent);

        void onPeerChanged(Intent intent, WifiP2pDeviceList peers);

        void onConnectChanged(Intent intent);

        void onDeviceChanged(Intent intent);

        void onWifiStateChange(boolean available);
    }
}
