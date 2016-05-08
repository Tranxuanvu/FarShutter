package com.thuantan.farshutter.Common;

/**
 * Created by choxu on 019/19/4/2016.
 */
public class Settings {
    public static final String HOTSPOT_SSID = "FarShutter";
    public static final String HOSTPOST_PASS = "123456789";
    public static int GetRandomPort(){
        return (int)(Math.random()*40000 + 9151);
    }
}
