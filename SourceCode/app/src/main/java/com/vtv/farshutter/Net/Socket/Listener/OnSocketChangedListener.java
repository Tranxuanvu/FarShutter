package com.vtv.farshutter.Net.Socket.Listener;

import com.vtv.farshutter.Net.Socket.CSocket;

/**
 * Created by choxu on 028/28/2/2016.
 */
public interface OnSocketChangedListener {
    void onConnect(CSocket client);
    void onDisconnect(CSocket client);
}
