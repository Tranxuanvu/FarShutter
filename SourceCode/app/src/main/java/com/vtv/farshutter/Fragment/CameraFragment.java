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
import com.vtv.farshutter.Common.Constant;
import com.vtv.farshutter.Net.Wifi.WifiAPI;
import com.vtv.farshutter.R;
import com.vtv.farshutter.Services.CameraService;
import com.vtv.farshutter.Utils.Point;

public class CameraFragment extends Fragment {

    //region Fields

    private static final String TAG = "CameraFragment";
    private CameraService mCameraService = null;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CameraService.CameraBinder binder = (CameraService.CameraBinder) service;

            mCameraService = binder.getService();
            onBindService(mCameraService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCameraService = null;
        }
    };

    //endregion

    //region Properties

    //endregion

    //region Constructors

    public CameraFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CameraFragment.
     */
    public static CameraFragment newInstance() {
        CameraFragment fragment = new CameraFragment();
        return fragment;
    }

    //endregion

    //region Functions

    /**
     * On Service Bound
     * @param service
     */
    private void onBindService(CameraService service){
        mCameraService.setCameraListener(new CameraService.OnCameraListener() {
            @Override
            public void onClientConnect(Connection client) {
                Log.d(TAG, "Controller connected: " + client.getRemoteAddressTCP().getHostName());
            }

            @Override
            public void onClientDisconnect(Connection client) {
                Log.d(TAG, "Controller disconnected: " + client.getRemoteAddressTCP().getHostName());
            }

            @Override
            public void onCapture(Connection client, Point point) {
                Log.d(TAG, "Receive capture request from: " + client.getRemoteAddressTCP().getHostName() + " at (" + point.x + ", " + point.y + ")");
            }
        });
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
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (WifiAPI.TurnOnWifiIfOff(this.getContext()) /*|| //TODO check is connected to wifi*/) {
            WifiAPI.TurnOnOffHotspot(this.getContext(), Constant.HOSTPOT_SSID, Constant.HOSTPOT_PASS, true); // start hotspot
        }

        //Bind service to application
        Intent cameraIntent = new Intent(this.getActivity().getApplicationContext(),CameraService.class);
        this.getActivity().getApplicationContext().bindService(cameraIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();

        WifiAPI.TurnOnOffHotspot(this.getContext(), Constant.HOSTPOT_SSID, Constant.HOSTPOT_PASS, false); // stop hotspot

        //Unbind service from application
        this.getActivity().getApplicationContext().unbindService(mServiceConnection);
    }

    //endregion

    //region ChildClasses

    //endregion

    //region Interfaces

    //endregion
}
