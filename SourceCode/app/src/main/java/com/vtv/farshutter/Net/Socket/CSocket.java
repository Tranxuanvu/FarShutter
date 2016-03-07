package com.vtv.farshutter.Net.Socket;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by choxu on 027/27/2/2016.
 */
public class CSocket {
    private Socket mSocket;

    public CSocket(Socket socket){
        this.mSocket = socket;
    }

    public boolean emit(String data){
        try {
            PrintWriter writer = new PrintWriter(mSocket.getOutputStream(), false);
            writer.println(data);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Socket getSocket(){
        return mSocket;
    }

    public void disconnect(){
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
