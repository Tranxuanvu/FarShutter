package com.vtv.farshutter.Net.Socket;

import android.util.Log;

import com.vtv.farshutter.Net.Socket.Listener.OnReceiveDataListener;
import com.vtv.farshutter.Net.Socket.Listener.OnSocketChangedListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by choxu on 027/27/2/2016.
 */
public class Server {
    private int mPort;
    private Map<String, OnReceiveDataListener> mEmitListenerMap;
    private ServerSocket mServerSocket;
    private ReceiveConnectionThread mConnectThread;
    private OnSocketChangedListener mOnSocketChangedListener;

    public Server(int port){
        this.mPort = port;
        mEmitListenerMap = new HashMap<>();
    }

    public boolean start(){
        try {
            mServerSocket = new ServerSocket(mPort);
            mConnectThread = new ReceiveConnectionThread(mServerSocket);

            mConnectThread.setOnClientEvent(new ReceiveConnectionThread.OnClientEventListener() {
                @Override
                public void onConnect(CSocket client) {
                    if (mOnSocketChangedListener != null) {
                        mOnSocketChangedListener.onConnect(client);
                    }
                }

                @Override
                public void onDisconnect(CSocket client) {
                    if (mOnSocketChangedListener != null) {
                        mOnSocketChangedListener.onDisconnect(client);
                    }
                }

                @Override
                public void onData(CSocket client, SocketData data) {
                    if (mEmitListenerMap.containsKey(data.tag)) {
                        mEmitListenerMap.get(data.tag).onData(client, data.data);
                    }
                }
            });

            mConnectThread.start();

            Log.d("SERVER SOCKET", "server listen on port " + mServerSocket.getLocalPort());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void stop(){
        try {
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isClose(){
        return mServerSocket == null || mServerSocket.isClosed();
    }

    public void emit(String data){
        mConnectThread.emit(data);
    }

    public void setOnSocketChangedListener(OnSocketChangedListener socketChangedListener){
        this.mOnSocketChangedListener = socketChangedListener;
    }

    public void on(String tag, OnReceiveDataListener receiveDataListener){
        mEmitListenerMap.put(tag,receiveDataListener);
    }
}
