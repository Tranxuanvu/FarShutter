package com.vtv.farshutter.Net;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.vtv.farshutter.Common.Constant;
import com.vtv.farshutter.Net.Socket.CSocket;
import com.vtv.farshutter.Net.Socket.Client;
import com.vtv.farshutter.Net.Socket.Listener.OnReceiveDataListener;
import com.vtv.farshutter.Net.Socket.Listener.OnSocketChangedListener;
import com.vtv.farshutter.Net.Socket.Server;
import com.vtv.farshutter.Net.Wifi.WifiAPI;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by choxu on 026/26/2/2016.
 */
public class WifiPeer2PeerService extends Service {
    private IBinder mBinder = new WifiPeer2PeerBinder();
    private List<WifiP2pDevice> mConnectedDevices = Collections.synchronizedList(new ArrayList<WifiP2pDevice>());
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WiFiDirectBroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;
    private WifiDirectListener mWifiDirectListener;
    private Gson mGson = new Gson();
    private boolean mWifiEnable = false;

    public WifiPeer2PeerService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel);

        mReceiver.setWifiListener(new WiFiDirectBroadcastReceiver.WifiDirectListener() {
            @Override
            public void onStateChanged(Intent intent) {
                Log.d("WIFI DIRECT SERVICE", "State changed");
                if (mWifiDirectListener != null) {
                    mWifiDirectListener.onStateChanged(intent);
                }

                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                System.out.println("INFO " + info);
                if (info != null && info.isConnected()){

                    WifiManager wifiManager = (WifiManager)WifiPeer2PeerService.this.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    Log.d("TEST ACTIVITY", wifiInfo.getSSID());
                }
            }

            @Override
            public void onPeerChanged(Intent intent, WifiP2pDeviceList peers) {
                Log.d("WIFI DIRECT SERVICE", "Peer changed: " + mGson.toJson(peers));
                if (mWifiDirectListener != null) {
                    mWifiDirectListener.onPeerChanged(intent, peers);
                }

                //Refresh connected device list
                synchronized (mConnectedDevices) {
                    mConnectedDevices.clear();
                    for (WifiP2pDevice device : peers.getDeviceList()) {
                        if (device.status == WifiP2pDevice.CONNECTED) {
                            mConnectedDevices.add(device);
                        }
                    }
                }
            }

            @Override
            public void onConnectChanged(Intent intent) {
                Log.d("WIFI DIRECT SERVICE", "Connect changed");
                if (mWifiDirectListener != null) {
                    mWifiDirectListener.onConnectChanged(intent);
                }
            }

            @Override
            public void onDeviceChanged(Intent intent) {
                Log.d("WIFI DIRECT SERVICE", "Device changed");
                if (mWifiDirectListener != null) {
                    mWifiDirectListener.onDeviceChanged(intent);
                }
            }

            @Override
            public void onWifiStateChange(boolean enable) {
                Log.d("WIFI DIRECT SERVICE", "Wifi state changed");
                if (mWifiDirectListener != null) {
                    mWifiDirectListener.onWifiStateChange(enable);
                }
                mWifiEnable = enable;
            }
        });

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        Log.d("WIFI DIRECT SERVICE", "Service started");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("WIFI DIRECT SERVICE", "Service UnBind");

        unregisterReceiver(mReceiver);
        return super.onUnbind(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("WIFI DIRECT SERVICE", "Service Bound");

        registerReceiver(mReceiver, mIntentFilter);
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d("WIFI DIRECT SERVICE", "Service ReBind");

        super.onRebind(intent);
        registerReceiver(mReceiver, mIntentFilter);
    }

    //WIFI P2P

    public boolean isWifiEnable() {
        return mWifiEnable;
    }

    public void discoverPeer() {
        if (mWifiEnable) {
            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d("WIFI DIRECT SERVICE", "Call discover success");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d("WIFI DIRECT SERVICE", "Call discover fail, reason: " + reason);
                }
            });
        }
    }

    public void connectPeer(final WifiP2pDevice device) { // Controller Method
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("WIFI DIRECT SERVICE", "Connect " + device.deviceName + " success");
                if (mWifiDirectListener != null) {
                    mWifiDirectListener.onConnect(device, true, 0);
                }
            }

            @Override
            public void onFailure(int reason) {
                Log.d("WIFI DIRECT SERVICE", "Connect " + device.deviceName + " Fail");
                if (mWifiDirectListener != null) {
                    mWifiDirectListener.onConnect(device, false, reason);
                }
            }
        });
    }

    public interface WifiDirectListener {
        void onStateChanged(Intent intent);

        void onPeerChanged(Intent intent, WifiP2pDeviceList peers);

        void onConnectChanged(Intent intent);

        void onDeviceChanged(Intent intent);

        void onWifiStateChange(boolean enable);

        void onConnect(WifiP2pDevice device, boolean success, int reason);
    }

    public void setWifiDirectListener(WifiDirectListener listener) {
        this.mWifiDirectListener = listener;
    }

    public void removeWifiDirectListener() {
        this.mWifiDirectListener = null;
    }

    public class WifiPeer2PeerBinder extends Binder {
        public WifiPeer2PeerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return WifiPeer2PeerService.this;
        }
    }

    //Controller

    private Client mSocketClient;
    private ControllerListener mControllerListener;

    private boolean connectCamera(final WifiP2pDevice device){
        if (device.status == WifiP2pDevice.CONNECTED){
            try {
                InetAddress address = Inet6Address.getByName(device.deviceAddress);
                mSocketClient = new Client(address, Constant.RECEIVE_REQUEST_PORT);

                //Register connect/disconnect event
                mSocketClient.setSocketChangedlistener(new OnSocketChangedListener() {
                    @Override
                    public void onConnect(CSocket client) {
                        if (mControllerListener != null){
                            mControllerListener.onConnected(device);
                        }
                    }

                    @Override
                    public void onDisconnect(CSocket client) {
                        if (mControllerListener != null){
                            mControllerListener.onDisconnected(device);
                        }
                    }
                });

                //Register controller event
                mSocketClient.on(Tag.CAPTURE_COMPLETE, new OnReceiveDataListener() {
                    @Override
                    public void onData(CSocket client, String data) {
                        if (mControllerListener != null){
                            mControllerListener.onCaptureComplete(device);
                        }
                    }
                });

                return true;
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void setControllerListener(ControllerListener listener) {
        this.mControllerListener = listener;
    }

    public void removeControllerListener() {
        this.mControllerListener = null;
    }

    public interface ControllerListener {
        void onConnected(WifiP2pDevice device);
        void onDisconnected(WifiP2pDevice device);
        void onCaptureComplete(WifiP2pDevice device);
    }

    public interface ConnectCameraCallBack{
        void onComplete(WifiP2pDevice device, boolean success);
    }

    //Camera

    private Server mSocketServer = new Server(Constant.RECEIVE_REQUEST_PORT); // create Socket Server
    private CameraListener mCameraListener;

    public boolean startCameraServer() { // Camera Method
        if (mSocketServer.isClose()) {
            if (!mWifiEnable) {
                WifiAPI.turnOnOffHotspot(this, true);
            }

            if (mSocketServer.start()) {
                mSocketServer.setOnSocketChangedListener(new OnSocketChangedListener() {
                    @Override
                    public void onConnect(CSocket client) {
                        if (mCameraListener != null) {
                            WifiP2pDevice currentDevice = null;

                            synchronized (mConnectedDevices){
                                for (WifiP2pDevice device : mConnectedDevices){
                                    if (device.deviceAddress == client.getSocket().getInetAddress().getHostName()){
                                        currentDevice = device;
                                        break;
                                    }
                                }
                            }

                            mCameraListener.onConnected(currentDevice);
                        }
                    }

                    @Override
                    public void onDisconnect(CSocket client) {
                        if (mCameraListener != null) {
                            WifiP2pDevice currentDevice = null;

                            synchronized (mConnectedDevices){
                                for (WifiP2pDevice device : mConnectedDevices){
                                    if (device.deviceAddress == client.getSocket().getInetAddress().getHostName()){
                                        currentDevice = device;
                                        break;
                                    }
                                }
                            }

                            mCameraListener.onDisconnected(currentDevice);
                        }
                    }
                });

                mSocketServer.on(Tag.CAPTURE, new OnReceiveDataListener() {
                    @Override
                    public void onData(CSocket client, String data) {
                        if (mCameraListener != null) {
                            WifiP2pDevice currentDevice = null;

                            synchronized (mConnectedDevices) {
                                for (WifiP2pDevice device : mConnectedDevices) {
                                    if (device.deviceAddress == client.getSocket().getInetAddress().getHostName()) {
                                        currentDevice = device;
                                        break;
                                    }
                                }
                            }

                            mCameraListener.onCapture(currentDevice);
                        }
                    }
                });

                Log.d("WIFI P2P SERVICE", "Camera service started");
                return true;
            }
        }

        Log.d("WIFI P2P SERVICE", "Camera service start fail");
        return false;
    }

    public void setCameraListener(CameraListener listener) {
        this.mCameraListener = listener;
    }

    public void removeCameraListener() {
        this.mCameraListener = null;
    }

    public interface CameraListener {
        void onConnected(WifiP2pDevice device);
        void onDisconnected(WifiP2pDevice device);
        void onCapture(WifiP2pDevice device);
    }
}
