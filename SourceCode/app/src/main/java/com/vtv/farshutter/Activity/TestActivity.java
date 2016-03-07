package com.vtv.farshutter.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.vtv.farshutter.Net.Wifi.WifiAPI;
import com.vtv.farshutter.Net.WifiPeer2PeerService;
import com.vtv.farshutter.R;

import java.util.Collection;

public class TestActivity extends AppCompatActivity {
    private Button mBtnDiscoverPeer;
    private Button mBtnConnectFirst;
    private Button mBtnStartCameraService;
    private Button mBtnConnect;

    private WifiPeer2PeerService mService;
    private boolean mBound = false;

    private Collection<WifiP2pDevice> mDeviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        mBtnDiscoverPeer = (Button) findViewById(R.id.btnDiscoverPeer);
        mBtnDiscoverPeer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound) {
                    mService.discoverPeer();
                }
            }
        });

        mBtnConnectFirst = (Button) findViewById(R.id.btnConnectFirst);
        mBtnConnectFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDeviceList != null && mDeviceList.size() > 0) {
                    mService.connectPeer(mDeviceList.iterator().next());
                }
            }
        });

        mBtnStartCameraService = (Button) findViewById(R.id.btnStartCameraService);
        mBtnStartCameraService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.startCameraServer();
            }
        });

        mBtnConnect = (Button) findViewById(R.id.btnConnectWifi);
        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiAPI.connect(TestActivity.this, "VTV", "qwertyuiop");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, WifiPeer2PeerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, final IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            WifiPeer2PeerService.WifiPeer2PeerBinder binder = (WifiPeer2PeerService.WifiPeer2PeerBinder) service;
            mService = binder.getService();
            mBound = true;

            mService.setWifiDirectListener(new WifiPeer2PeerService.WifiDirectListener() {
                @Override
                public void onStateChanged(Intent intent) {
                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (info != null && info.isConnected()){
                        mService.discoverPeer();

                        WifiManager wifiManager = (WifiManager)TestActivity.this.getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        Log.d("TEST ACTIVITY", wifiInfo.getSSID());
                    }
                }

                @Override
                public void onPeerChanged(Intent intent, WifiP2pDeviceList peers) {
                    mDeviceList = peers.getDeviceList();
                }

                @Override
                public void onConnectChanged(Intent intent) {

                }

                @Override
                public void onDeviceChanged(Intent intent) {

                }

                @Override
                public void onWifiStateChange(boolean enable) {

                }

                @Override
                public void onConnect(WifiP2pDevice device, boolean success, int reasonCode) {
                    Toast.makeText(TestActivity.this ,"Connect " + device.deviceName + (success? " success": " fail, reason: " + reasonCode),Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
