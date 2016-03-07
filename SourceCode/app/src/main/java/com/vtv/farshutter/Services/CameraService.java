package com.vtv.farshutter.Services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.vtv.farshutter.Common.ActionCodes;
import com.vtv.farshutter.Net.Actions.Action;
import com.vtv.farshutter.Utils.Point;

import java.io.IOException;

/**
 * Created by choxu on 007/7/3/2016.
 */
public class CameraService extends Service {

    //region Fields

    private static final String TAG = "CameraService";
    private OnCameraListener mCameraLister;
    private IBinder mBinder = new CameraBinder();
    private Server mServer = new Server();

    //endregion

    //region Properties

    //endregion

    //region Constructors

    public CameraService() {

    }

    //endregion

    //region Functions

    //endregion

    //region Methods

    /**
     * Start Server
     *
     * @param tcpPort
     */
    public void StartServer(int tcpPort, int udpPort) throws IOException {
        mServer.start();
        mServer.bind(tcpPort, udpPort);

        mServer.addListener(new Listener(){
            @Override
            public void connected(Connection client) {
                if (mCameraLister != null){
                    mCameraLister.onClientConnect(client);
                }
            }

            @Override
            public void disconnected(Connection client) {
                if (mCameraLister != null){
                    mCameraLister.onClientDisconnect(client);
                }
            }

            @Override
            public void received (Connection client, Object object) {
                if (mCameraLister != null) {
                    if (object instanceof Action) {
                        Action action = (Action) object;

                        switch (action.code) {
                            case ActionCodes.CAPTURE: {
                                mCameraLister.onCapture(client, (Point)action.value);
                            }
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * Send Image After Capture Complete
     *
     * @param dir
     */
    public void CaptureComplete(String dir) {
        mServer.sendToAllTCP(new Action(ActionCodes.CAPTURE_COMPLETE, null));
    }

    /**
     * Set Listener
     * @param listener
     */
    public void setCameraListener(OnCameraListener listener){
        mCameraLister = listener;
    }

    //endregion

    //region EventListeners

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //Register Message Object Class
        Kryo kryoServer = mServer.getKryo();
        kryoServer.register(Action.class);
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

    public interface OnCameraListener {
        void onClientConnect(Connection client);
        void onClientDisconnect(Connection client);
        void onCapture(Connection client, Point point);
    }

    //endregion
}
