package com.vtv.farshutter.Net.Wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

/**
 * Created by choxu on 028/28/2/2016.
 */
public class WifiControl {
    public static boolean isWifiEnable(Context context){
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    public static void connect(Context context, String SSID, String password){
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);

            while (!wifiManager.isWifiEnabled()){
                Log.d("WIFI CONTROL","Wait Enable Wifi");
                try {
                    Thread.sleep(200);
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
            conf.SSID = SSID;
            conf.preSharedKey = password;
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

            wifiManager.disconnect();
            wifiManager.enableNetwork(wifiManager.addNetwork(conf), true);
            wifiManager.reconnect();
        }
    }
}
