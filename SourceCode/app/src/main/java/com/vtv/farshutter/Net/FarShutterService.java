package com.vtv.farshutter.Net;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;
import com.vtv.farshutter.Net.Actions.Action;
import com.vtv.farshutter.Utils.Point;

import java.io.IOException;

/**
 * Created by choxu on 028/28/2/2016.
 */
public class FarShutterService extends Service{

    private Server mServer;
    private Client mClient;

    private OnWifiChangedListener mWifiChangedListener;
    private OnCameraListener mCameraListener;
    private OnControllerListener mControllerListener;

    public Server startCameraService(int port, OnCameraListener cameraListener) {
        mCameraListener = cameraListener;
        mServer.start();
        try {
            mServer.bind(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mServer;
    }

    public Client startControllerService(OnControllerListener controllerListener){
        mControllerListener = controllerListener;
        mClient.start();

        return mClient;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mServer = new Server();
        mClient = new Client();

        //register classes
        Kryo kryoServer = mServer.getKryo();
        kryoServer.register(Action.class);

        Kryo kryoClient = mClient.getKryo();
        kryoClient.register(Action.class);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    /**
     * WIFI broadcast receiver
     */
    private BroadcastReceiver mWifiBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(action)){

            }else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)){

            }
        }
    };

    /**
     * WIFI P2P broadcast receiver
     */
    /*private BroadcastReceiver mWifiP2pBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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
                                mListener.onPeerChanged(peers);
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
    };*/


    /**
     * On wifi change interface
     */
    public interface OnWifiChangedListener{
        void onConnect(WifiManager manager, NetworkInfo info);
        void onDisonnect(WifiManager manager);
        void onWifiEnable(WifiManager manager);
        void onWifiDisable(WifiManager manager);
    }

    //Camera Listener
    public interface OnCameraListener{
        void onCapture(Point point);
    }

    //Controller Listener
    public interface OnControllerListener{
        void onCaptureComplete();
    }
}
