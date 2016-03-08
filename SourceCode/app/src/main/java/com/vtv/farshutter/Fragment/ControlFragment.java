package com.vtv.farshutter.Fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.esotericsoftware.kryonet.Connection;
import com.vtv.farshutter.Net.Wifi.WifiAPI;
import com.vtv.farshutter.R;
import com.vtv.farshutter.Services.ControllerService;

public class ControlFragment extends Fragment {

    //region Fields

    private static final String TAG = "ControlFragment";
    private ControllerService mControllerService = null;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ControllerService.ControllerBinder binder = (ControllerService.ControllerBinder) service;

            mControllerService = binder.getService();
            onBindService(mControllerService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mControllerService = null;
        }
    };

    //endregion

    //region Properties

    //endregion

    //region Constructors

    public ControlFragment() {
        // Required empty public constructor
    }

    public static ControlFragment newInstance() {
        ControlFragment fragment = new ControlFragment();
        return fragment;
    }

    //endregion

    //region Functions

    /**
     * On Service Bound
     * @param service
     */
    private void onBindService(ControllerService service){
        mControllerService.setControllerListener(new ControllerService.OnControllerListener() {
            @Override
            public void onConnected(Connection server) {
                Log.d(TAG, "Connected to camera: " + server.getRemoteAddressTCP().getHostName());
            }

            @Override
            public void onDisconnected(Connection server) {
                Log.d(TAG, "Disconnected from camera: " + server.getRemoteAddressTCP().getHostName());
            }

            @Override
            public void onCaptureComplete(Connection server) {
                Log.d(TAG, "Capture complete from: " + server.getRemoteAddressTCP().getHostName());
            }
        });
    }

    /**
     * Connect to wifi access point
     * @param SSID
     * @param password
     */
    private void connectWifi(String SSID, String password){
        WifiAPI.Connect(this.getContext(), SSID, password);
    }

    //endregion

    //region Methods

    //endregion

    //region EventListeners

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_control, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        WifiAPI.TurnOnWifiIfOff(this.getContext()); // turn on wifi if was turn off
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();

        //Bind controller to service
        Intent controllerIntent = new Intent(this.getActivity().getApplicationContext(), ControllerService.class);
        this.getActivity().getApplicationContext().bindService(controllerIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();

        //Unbind from controller service
        this.getActivity().getApplicationContext().unbindService(mServiceConnection);
    }

    //endregion

    //region ChildClasses

    //endregion

    //region Interfaces

    //endregion
}
