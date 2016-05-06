package com.thuantan.farshutter.Net.Wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by choxu on 007/7/3/2016.
 */
public class WifiAPI {
    private static final String TAG = "WifiAPI";

    public static boolean IsWifiOn(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    public static void TurnWifi(Context context, boolean isTurnOn) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        wifiManager.setWifiEnabled(isTurnOn);

        while ((isTurnOn && !wifiManager.isWifiEnabled()) || (!isTurnOn && wifiManager.isWifiEnabled())) {
            Log.d(TAG, "Wait Turn " + (isTurnOn ? "On" : "Off") + " Wifi");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void Connect(Context context, String ssid, String password) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);

            while (!wifiManager.isWifiEnabled()) {
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
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                try {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();
                    isConnect = true;
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (!isConnect) {
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + ssid + "\"";
            conf.preSharedKey = "\"" + password + "\"";

            wifiManager.disconnect();
            wifiManager.enableNetwork(wifiManager.addNetwork(conf), true);
            wifiManager.reconnect();
        }
    }

    public static void TurnOnOffHotspot(Context context, String ssid, String password, boolean isTurnToOn) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiApControl apControl = WifiApControl.getApControl(wifiManager);
        if (apControl != null) {

            WifiConfiguration wifiConfiguration = new WifiConfiguration();
            wifiConfiguration.SSID = ssid;
            wifiConfiguration.preSharedKey = password;
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

            apControl.setWifiApEnabled(wifiConfiguration, isTurnToOn);
        }
    }

    public static String GetCurrentIP(){
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                if (intf.getName().contains("wlan")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()
                                && (inetAddress.getAddress().length == 4)) {
                            Log.d(TAG, inetAddress.getHostAddress());
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }
        return null;
    }
}
