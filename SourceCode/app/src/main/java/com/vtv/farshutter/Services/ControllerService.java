package com.vtv.farshutter.Services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.vtv.farshutter.Common.ActionCodes;
import com.vtv.farshutter.Net.Actions.Action;
import com.vtv.farshutter.Utils.Point;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

/**
 * Created by choxu on 007/7/3/2016.
 */
public class ControllerService extends Service {

    //region Fields

    private static final String TAG = "ControllerService";
    private OnControllerListener mControllerListener;
    private IBinder mBinder = new ControllerBinder();
    private Client mClient = new Client();

    //endregion

    //region Properties

    //endregion

    //region Constructors

    //endregion

    //region Functions

    //endregion

    //region Methods

    /**
     * Get list camera in network
     * @param tcpPort
     * @return List InetAddress of camera server
     */
    public List<InetAddress> discoverCameras(int tcpPort) {
        return mClient.discoverHosts(tcpPort, 5000);
    }

    /**
     * Connect to camera
     * @param address
     * @param tcpPort
     * @param udpPort
     * @throws IOException
     */
    public void connect(InetAddress address, int tcpPort, int udpPort) throws IOException {
        mClient.connect(5000, address, tcpPort, udpPort);

        mClient.addListener(new Listener(){
            @Override
            public void connected(Connection server) {
                if (mControllerListener != null){
                    mControllerListener.onConnected(server);
                }
            }

            @Override
            public void disconnected(Connection server) {
                if (mControllerListener != null){
                    mControllerListener.onDisconnected(server);
                }
            }

            @Override
            public void received(Connection server, Object object) {
                if (mControllerListener != null) {
                    if (object instanceof Action) {
                        Action action = (Action) object;

                        switch (action.code) {
                            case ActionCodes.CAPTURE_COMPLETE: {
                                mControllerListener.onCaptureComplete(server);
                            }
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * Capture Image
     * @param x
     * @param y
     */
    public void capture(int x, int y){
        mClient.sendTCP(new Action(ActionCodes.CAPTURE, new Point(x, y)));
    }

    /**
     * Set Listener
     * @param listener
     */
    public void setControllerListener(OnControllerListener listener){
        mControllerListener = listener;
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
        Kryo kryoServer = mClient.getKryo();
        kryoServer.register(Action.class);
    }

    //endregion

    //region ChildClasses

    /**
     * ControllerService Binder
     */
    public class ControllerBinder extends Binder {
        public ControllerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ControllerService.this;
        }
    }
    //endregion

    //region Interfaces

    public interface OnControllerListener {
        void onConnected(Connection server);
        void onDisconnected(Connection server);
        void onCaptureComplete(Connection server);
    }

    //endregion
}
