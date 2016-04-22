package com.vtv.farshutter.Fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.vtv.farshutter.Net.NativeSocketReceiver;
import com.vtv.farshutter.Net.Wifi.WifiAPI;
import com.vtv.farshutter.R;
import com.vtv.farshutter.Services.CameraService;

public class ControlFragment extends Fragment implements NativeSocketReceiver.NativeSocketResponse {

    //region Fields

    private static final String TAG = "ControlFragment";
    private CameraService mCameraService = null;

    private EditText mEtIP;
    private Button mBtnConnect;
    private ImageView mIvCameraPreview;
    private Bitmap bmp1;
    private Bitmap bmp2;
    private Bitmap result;
    private boolean bmpChoose = true; /* true is 1, false is 2 */

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CameraService.CameraBinder binder = (CameraService.CameraBinder) service;
            mCameraService = binder.getService();

            if (mCameraService != null){
                mCameraService.setCallback(ControlFragment.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

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
     * Connect to wifi access point
     * @param SSID
     * @param password
     */
    private void connectWifi(String SSID, String password){
        WifiAPI.Connect(this.getContext(), SSID, password);
    }

    private void renderImage(final byte []data) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bmpChoose) {
                    if (bmp2 != null) bmp2.recycle();
                    result = bmp2 = BitmapFactory.decodeByteArray(data, 0, data.length);
                } else {
                    if (bmp1 != null) bmp1.recycle();
                    result = bmp1 = BitmapFactory.decodeByteArray(data, 0, data.length);
                }
                if (result != null) {
                    mIvCameraPreview.setImageBitmap(result);
                    bmpChoose = !bmpChoose;
                }
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
        View view = inflater.inflate(R.layout.fragment_control, container, false);

        mEtIP = (EditText) view.findViewById(R.id.etIP);

        mIvCameraPreview = (ImageView) view.findViewById(R.id.ivCameraPreview);
        mIvCameraPreview.setRotation(90);

        mBtnConnect = (Button) view.findViewById(R.id.btnConnect);
        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCameraService != null){
                    mCameraService.initClient(mEtIP.getText().toString());
                }
            }
        });

        return view;
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
    public void onResume() {
        super.onResume();
        // WifiAPI.TurnOnWifi(this.getContext()); // turn on wifi if was turn off

        //Bind controller to service
        Intent controllerIntent = new Intent(this.getActivity().getApplicationContext(), CameraService.class);
        this.getActivity().getApplicationContext().bindService(controllerIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();

        //Unbind from controller service
        this.getActivity().getApplicationContext().unbindService(mServiceConnection);

        if (mCameraService != null){
            mCameraService.removeCallback();
            mCameraService = null;
        }
    }

    @Override
    public void OnCreateServer() {

    }

    @Override
    public void OnCreateServerError() {

    }

    @Override
    public void OnConnectError() {

    }

    @Override
    public void OnConnect() {

    }

    @Override
    public void OnReceiveImage(byte[] data) {
        renderImage(data);
        Log.d(TAG, "Receive data");
    }

    @Override
    public void OnReceiveCode(String data) {

    }

    //endregion

    //region ChildClasses

    //endregion

    //region Interfaces

    //endregion
}
