package com.vtv.farshutter.Net;

import java.io.UnsupportedEncodingException;

/**
 * Created by choxu on 022/22/4/2016.
 */
public class NativeSocketReceiver {

    public interface NativeSocketResponse {

        void OnCreateServer();

        void OnCreateServerError();

        void OnConnectError();

        void OnConnect();

        void OnReceiveImage(byte[] data);

        void OnReceiveCode(String data);

    }

    private static NativeSocketResponse Callback;

    public static void setCallback(NativeSocketResponse response) {
        Callback = response;
    }

    public static void removeCallback() {
        Callback = null;
    }

    public static void OnCreateServer() {
        if (Callback != null) Callback.OnCreateServer();
    }

    public static void OnCreateServerError() {
        if (Callback != null) Callback.OnCreateServerError();
    }

    public static void OnConnect() {
        if (Callback != null) Callback.OnConnect();
    }

    public static void OnConnectError() {
        if (Callback != null) Callback.OnConnectError();
    }

    public static void OnReceiveImage(byte[] data) {
        if (Callback != null) Callback.OnReceiveImage(data);
    }

    public static void OnReceiveCode(byte[] data) {
        if (Callback != null) Callback.OnReceiveCode(decodeResponseData(data));
    }

    public static String decodeResponseData(byte[] data) {
        try {
            return new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }
}
