package com.vtv.farshutter.Net.Wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

/**
 * Created by choxu on 007/7/3/2016.
 */
public class WifiAPI {
    private static final String TAG = "WifiAPI";

    public static boolean IsWifiOn(Context context){
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    public static void TurnOnWifi(Context context){
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        wifiManager.setWifiEnabled(true);

        while (!wifiManager.isWifiEnabled()){
            Log.d(TAG, "Wait Turn On Wifi");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void TurnOffWifi(Context context){
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        wifiManager.setWifiEnabled(false);

        while (wifiManager.isWifiEnabled()){
            Log.d(TAG, "Wait Turn Off Wifi");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void Connect(Context context, String SSID, String password){
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);

            while (!wifiManager.isWifiEnabled()){
                Log.d(TAG, "Wait Enable Wifi");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        boolean isConnect = false;

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals(SSID)) {
                try {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();
                    isConnect = true;
                    break;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (!isConnect){
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + SSID + "\"";
            conf.preSharedKey = "\""+ password +"\"";

            wifiManager.disconnect();
            wifiManager.enableNetwork(wifiManager.addNetwork(conf), true);
            wifiManager.reconnect();
        }
    }

    public static void TurnOnOffHotspot(Context context, String SSID, String password, boolean isTurnToOn) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiApControl apControl = WifiApControl.getApControl(wifiManager);
        if (apControl != null) {

            WifiConfiguration wifiConfiguration = new WifiConfiguration();
            wifiConfiguration.SSID = SSID;
            wifiConfiguration.preSharedKey = password;
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

            apControl.setWifiApEnabled(wifiConfiguration, isTurnToOn);
        }
    }
}
