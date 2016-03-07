package com.vtv.farshutter.Net.Socket;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vtv.farshutter.Net.Socket.Listener.OnReceiveDataListener;
import com.vtv.farshutter.Net.Socket.Listener.OnSocketChangedListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by choxu on 028/28/2/2016.
 */
public class Client {
    private Map<String, OnReceiveDataListener> mEmitListenerMap;
    private OnSocketChangedListener mSocketChangedlistener;
    private CSocket mSocket;
    private Gson mGson;
    private InetAddress mAddress;
    private int mPort;

    public Client(InetAddress address, int port){
        this.mAddress = address;
        this.mPort = port;
        this.mGson = new Gson();
        this.mEmitListenerMap = new HashMap<>();
    }

    public void setSocketChangedlistener(OnSocketChangedListener socketChangedlistener){
        this.mSocketChangedlistener = socketChangedlistener;
    }

    public void on(String tag, OnReceiveDataListener receiveDataListener){
        mEmitListenerMap.put(tag, receiveDataListener);
    }

    public void connect(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(mAddress, mPort);
                    mSocket = new CSocket(socket);

                    //Callback connected event
                    if (mSocketChangedlistener != null){
                        mSocketChangedlistener.onConnect(mSocket);
                    }

                    int dataNullCount = 0;

                    while (!socket.isClosed() && dataNullCount < 50){
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String stringData = reader.readLine();
                            if (stringData != null){
                                if (stringData.length() > 0){
                                    try {
                                        SocketData data = mGson.fromJson(stringData, SocketData.class);

                                        if(data != null && mEmitListenerMap.containsKey(data.tag)){
                                            mEmitListenerMap.get(data.tag).onData(mSocket, data.data.replace("\\x45@#35", "\n"));
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
