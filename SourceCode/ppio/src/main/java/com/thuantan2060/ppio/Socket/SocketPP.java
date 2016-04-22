package com.thuantan2060.ppio.Socket;

import com.google.gson.Gson;
import com.thuantan2060.ppio.Message.EmitMessage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Hashtable;

/**
 * Created by choxu on 014/14/3/2016.
 */
public abstract class SocketPP {

    //region Fields

    private ReceiverThread.OnMessageListener mOnMessageListener = new ReceiverThread.OnMessageListener() {
        @Override
        public void onMessage(String data) {
            EmitMessage message = mGson.fromJson(data, EmitMessage.class);
            OnMessageListener listener = mOnMessageListeners.get(message.tag);
            if (listener != null){
                listener.onMessage(message.data);
            }
        }

        @Override
        public void onClose() {
            //TODO on socket close
        }
    };
    private Hashtable<String, OnMessageListener> mOnMessageListeners = new Hashtable<>();
    private Socket mSocket;
    private Gson mGson;

    //endregion

    //region Constructors

    public SocketPP(Socket socket){
        this.mSocket = socket;
        this.mGson = new Gson();

        try {
            ReceiverThread receiverPrinter = new ReceiverThread(mSocket, mOnMessageListener);
            receiverPrinter.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //endregion

    //region Methods

    public boolean emit(String tag, Object data){
        if (mSocket!=null && mSocket.isConnected()){

            try {
                PrintWriter socketWriter = new PrintWriter(mSocket.getOutputStream(),true);
                socketWriter.println(mGson.toJson(new EmitMessage(tag, data)));
                socketWriter.flush();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

        }

        return false;
    }

    public void on(String tag, OnMessageListener callback){
        mOnMessageListeners.put(tag, callback);
    }

    public boolean isClose(){
        return mSocket.isClosed();
    }

    public boolean isConnected(){
        return mSocket.isConnected();
    }

    public InetAddress getInetAddress(){
        return mSocket.getInetAddress();
    }

    public InetAddress getLocalAddress(){
        return mSocket.getLocalAddress();
    }

    //endregion

    //region Functions


    //endregion

    //region ChildClasses

    public interface OnMessageListener{
        void onMessage(Object data);
    }

    //endregion
}
