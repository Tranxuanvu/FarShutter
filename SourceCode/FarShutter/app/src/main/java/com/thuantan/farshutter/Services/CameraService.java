package com.thuantan.farshutter.Services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.thuantan.farshutter.Net.NativeSocketReceiver;

import java.io.UnsupportedEncodingException;

/**
 * Created by choxu on 007/7/3/2016.
 */
public class CameraService extends Service {

    //region Fields
    static {
        System.loadLibrary("SharedLibrarySocket");
    }

    private static final String TAG = "CameraService";
    private IBinder mBinder = new CameraBinder();
    private int mPort = 0;

    //endregion

    //region Properties

    //endregion

    //region Constructors

    //endregion

    //region Functions

    public void setCallback(NativeSocketReceiver.NativeSocketResponse response) {
        NativeSocketReceiver.setCallback(response);
    }

    public void removeCallback() {
        NativeSocketReceiver.removeCallback();
    }

    //endregion

    //region Methods

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void sendCodeData(String data) {
        try {
            byte[] bytes = data.getBytes("UTF-8");
            sendCodeData(bytes);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public native String sendImageData(byte[] data);

    public native String sendStreamData(byte[] data);

    private native String createServer(int port);

    public native String initClient(String ip, int port);

    public native String sendCodeData(byte[] data);

    private native String closeSocket();

    public void startServer(int port){
        mPort = port;
        new Thread(new Runnable() {
            @Override
            public void run() {
                createServer(mPort);
            }
        }).start();
    }

    public void CloseSocket(){
        closeSocket();
        mPort = 0;
    }

    public int CurrentPort(){
        return mPort;
    }

    //endregion

    //region EventListeners

    @Override
    public void onCreate() {
        super.onCreate();
    }


    //endregion

    //region ChildClasses

    /**
     * CameraService Binder
     */
    public class CameraBinder extends Binder {
        public CameraService getService() {
            // Return this instance of LocalService so clients can call public methods
            return CameraService.this;
        }
    }
    //endregion

    //region Interfaces

    //endregion
}
