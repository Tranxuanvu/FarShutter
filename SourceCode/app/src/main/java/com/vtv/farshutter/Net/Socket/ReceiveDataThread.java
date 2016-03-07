package com.vtv.farshutter.Net.Socket;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by choxu on 027/27/2/2016.
 */
public class ReceiveDataThread extends Thread{
    private CSocket mClient;
    private ReceiveConnectionThread.OnClientEventListener mListener;
    private OnCloseListener mOnCloseListener;

    public ReceiveDataThread(CSocket client, ReceiveConnectionThread.OnClientEventListener listener, OnCloseListener onCloseListener){
        this.mClient = client;
        this.mListener = listener;
        this.mOnCloseListener = onCloseListener;
    }

    public void setOnClientEventListener(ReceiveConnectionThread.OnClientEventListener listener){
        this.mListener = listener;
    }

    public CSocket getClient(){
        return mClient;
    }

    @Override
    public void run() {
        Gson gson = new Gson();

        //callback on connect
        if (mListener != null) {
            mListener.onConnect(mClient);
        }

        Socket socket = mClient.getSocket();

        int dataNullCount = 0;

        //pool receive data
        while (!socket.isClosed() && dataNullCount < 50){
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String stringData = reader.readLine();
                if (stringData != null){
                    if (stringData.length() > 0){
                        try {
                            SocketData data = gson.fromJson(stringData, SocketData.class);

                            if(data != null && mListener != null){
                                data.data = data.data.replace("\\x45@#35", "\n");
                                mListener.onData(mClient, data);
                            }
                        } catch (JsonSyntaxException e){
                            e.printStackTrace();
                        }
                    }
                }else{
                    dataNullCount++;
                }

            } catch (NullPointerException | IOException e) {
                e.printStackTrace();
                dataNullCount++;
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e1) {
                    e.printStackTrace();
                }
            }
        }

        //Close socket
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //callback on disconnect
        if (mListener != null){
            mListener.onDisconnect(mClient);
        }

        //callback on close
        if (mOnCloseListener != null){
            mOnCloseListener.onClose(this);
        }
    }

    public interface OnCloseListener{
        void onClose(ReceiveDataThread thread);
    }
}
