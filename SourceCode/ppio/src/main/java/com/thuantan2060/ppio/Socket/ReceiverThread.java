package com.thuantan2060.ppio.Socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by choxu on 014/14/3/2016.
 */
public class ReceiverThread extends Thread {
    private BufferedReader socketReader;
    private Socket socket;
    private OnMessageListener mListener;

    public ReceiverThread(Socket socket, OnMessageListener listener) throws IOException {
        this.socket = socket;
        this.socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.mListener = listener;
    }

    public void setMessageListener(OnMessageListener listener){
        mListener = listener;
    }

    @Override
    public void run() {
        String message;

        while (!socket.isClosed()) {
            try {
                message = socketReader.readLine();

                if (mListener != null) {
                    mListener.onMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mListener != null) {
            mListener.onClose();
        }
    }

    public interface OnMessageListener {
        void onMessage(String message);

        void onClose();
    }
}
