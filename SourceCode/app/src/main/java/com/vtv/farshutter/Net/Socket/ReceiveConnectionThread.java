package com.vtv.farshutter.Net.Socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by choxu on 027/27/2/2016.
 */
public class ReceiveConnectionThread extends Thread {
    private ServerSocket mServerSocket;
    private List<ReceiveDataThread> mReceiveDataThreads;
    private OnClientEventListener mListener;
    private ReceiveDataThread.OnCloseListener mReceiveThreadCloseListener;

    public ReceiveConnectionThread(ServerSocket serverSocket) {
        this.mServerSocket = serverSocket;
        this.mReceiveDataThreads = new ArrayList<>();
        this.mReceiveThreadCloseListener = new ReceiveDataThread.OnCloseListener() {
            @Override
            public void onClose(ReceiveDataThread thread) {
                mReceiveDataThreads.remove(thread);
            }
        };
    }

    @Override
    public void run() {
        while (!mServerSocket.isClosed()) {
            try {
                Socket socket = mServerSocket.accept(); // On new connect

                ReceiveDataThread thread = new ReceiveDataThread(new CSocket(socket), mListener, mReceiveThreadCloseListener); // new receive data thread
                mReceiveDataThreads.add(thread); // add thread to array receive data thread
                thread.start(); //start receive data thread

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Disconnect all client
        for (ReceiveDataThread thread : mReceiveDataThreads){
            thread.getClient().disconnect();
        }

        try {
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setOnClientEvent(OnClientEventListener listener){
        this.mListener = listener;

        for (ReceiveDataThread thread : mReceiveDataThreads){
            thread.setOnClientEventListener(mListener);
        }
    }

    public void emit(String data){
        for (ReceiveDataThread thread : mReceiveDataThreads){
            thread.getClient().emit(data);
        }
    }

    public interface OnClientEventListener{
        void onConnect(CSocket client);
        void onDisconnect(CSocket client);
        void onData(CSocket client, SocketData data);
    }
}
