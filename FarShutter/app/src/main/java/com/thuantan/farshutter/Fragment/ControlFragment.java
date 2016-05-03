package com.thuantan.farshutter.Fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.thuantan.farshutter.Common.ActionCodes;
import com.thuantan.farshutter.Net.NativeSocketReceiver;
import com.thuantan.farshutter.Net.Wifi.WifiAPI;
import com.thuantan.farshutter.R;
import com.thuantan.farshutter.Services.CameraService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.markushi.ui.CircleButton;
import eu.livotov.labs.android.camview.ScannerLiveView;

public class ControlFragment extends Fragment implements NativeSocketReceiver.NativeSocketResponse {
    //region Fields

    private enum ReceiveState{
        RECEIVE_CAPTURED_IMAGE, RECEIVE_STEAM_IMAGE
    }

    private static final String TAG = "ControlFragment";
    private CameraService mCameraService = null;
    private ScannerLiveView mScanner = null;
    private Dialog mPreviewImageDialog = null;
    private Dialog mConfirmBackDialog = null;
    private Dialog mServerDisconnectDialog = null;
    private boolean mScanning = false;

    private CircleButton mBtnTakePhoto;
    private ImageButton mBtnConnectCamera;
    private ImageView mIvCameraPreview;
    private Dialog mQRScannerDialog;
    private Bitmap bmp1;
    private Bitmap bmp2;
    private Bitmap result;
    private Bitmap previewBitmap;
    private boolean bmpChoose = true; /* true is 1, false is 2 */
    private ReceiveState mReceiveState = ReceiveState.RECEIVE_STEAM_IMAGE;

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

    private void ShowQRScanner(){
        if (mQRScannerDialog != null){
            mQRScannerDialog.show();

            mScanner = (ScannerLiveView)mQRScannerDialog.findViewById(R.id.scanner);
            if (mScanner != null){
                mScanner.setScannerViewEventListener(new ScannerLiveView.ScannerViewEventListener() {
                    @Override
                    public void onScannerStarted(ScannerLiveView scanner) {

                    }

                    @Override
                    public void onScannerStopped(ScannerLiveView scanner) {

                    }

                    @Override
                    public void onScannerError(Throwable err) {

                    }

                    @Override
                    public void onCodeScanned(String data) {
                        mQRScannerDialog.dismiss();

                        String[] parts = data.split(";");

                        if (parts.length >= 3) {
                            //Connect WIFI
                            WifiAPI.Connect(getContext(), parts[0], parts[1]);

                            //Connect Server
                            Pattern p = Pattern.compile("^[0-9]\\.[0-9]\\.[0-9]\\.[0-9]$");
                            Matcher m = p.matcher(parts[2]);
                            if (m.find()) {
                                //Wait for wifi connected
                                while (WifiAPI.GetCurrentIP() == null) {
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                ConnectServer(data);
                            }
                        }else{
                            Toast.makeText(getContext(), "Scan Fail", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            mQRScannerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    StopScan();
                }
            });
        }
    }

    private void StartScan(){
        if (mScanner != null) {
            mScanning = true;
            mScanner.startScanner();
        }
    }

    private void StopScan(){
        if (mScanner != null) {
            mScanner.stopScanner();
            mScanning = false;
        }
    }

    private void ConnectServer(String ip){
        if (mCameraService != null){
            mCameraService.initClient(ip);
        }
    }

    private void ShowCapturedImage(byte[] data) {
        mPreviewImageDialog.show();

        ImageView imageView = (ImageView) mPreviewImageDialog.findViewById(R.id.preview_image);
        imageView.setImageBitmap(previewBitmap = BitmapFactory.decodeByteArray(data, 0 , data.length));
    }

    private void RenderStreamImage(final byte[] data) {
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

        //Bind Service
        Intent controllerIntent = new Intent(this.getActivity().getApplicationContext(), CameraService.class);
        this.getActivity().getApplicationContext().bindService(controllerIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, container, false);

        mIvCameraPreview = (ImageView) view.findViewById(R.id.ivCameraPreview);

        mBtnConnectCamera = (ImageButton) view.findViewById(R.id.btnConnectCamera);
        mBtnConnectCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowQRScanner();
                StartScan();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(inflater.inflate(R.layout.qr_scanner_dialog, null));
        builder.setTitle("Scanning QR Code...");
        builder.setPositiveButton("DONE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        mQRScannerDialog = builder.create();

        AlertDialog.Builder builder2 = new AlertDialog.Builder(getContext());
        builder2.setView(inflater.inflate(R.layout.preview_dialog, null));
        builder2.setTitle("Preview");
        builder2.setPositiveButton("DONE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        mPreviewImageDialog = builder2.create();
        mPreviewImageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                previewBitmap.recycle();
            }
        });

        AlertDialog.Builder builder3 = new AlertDialog.Builder(getContext());
        builder3.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mCameraService != null) {
                    mCameraService.closeSocket();
                }
                getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });
        builder3.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder3.setMessage("Do you want to close ?");
        mConfirmBackDialog = builder3.create();

        AlertDialog.Builder builder4 = new AlertDialog.Builder(getContext());
        builder4.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder4.setMessage("Server disconnected!");
        mServerDisconnectDialog = builder4.create();

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    mConfirmBackDialog.show();
                    return true;
                } else {
                    return false;
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

        if (mScanning){
            StartScan();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mCameraService != null){
            mCameraService.removeCallback();
            mCameraService = null;
        }

        if(mScanner != null){
            mScanner.stopScanner();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Unbind from controller service
        this.getActivity().getApplicationContext().unbindService(mServiceConnection);
    }

    @Override
    public void OnCreateServer() {

    }

    @Override
    public void OnCreateServerError() {

    }

    @Override
    public void OnConnectError() {
        mServerDisconnectDialog.show();
    }

    @Override
    public void OnConnect() {

    }

    @Override
    public void OnReceiveImage(byte[] data) {
        switch (mReceiveState){
            case RECEIVE_STEAM_IMAGE:
                RenderStreamImage(data);
                break;
            case RECEIVE_CAPTURED_IMAGE:
                ShowCapturedImage(data);
                break;
        }
}

    @Override
    public void OnReceiveCode(String data) {
        switch (data){
            case ActionCodes.START_SEND_IMAGE:
                mReceiveState = ReceiveState.RECEIVE_CAPTURED_IMAGE;
                break;
            case ActionCodes.END_SEND_IMAGE:
                mReceiveState = ReceiveState.RECEIVE_STEAM_IMAGE;
                break;
        }
    }

    //endregion

    //region ChildClasses

    //endregion

    //region Interfaces

    //endregion
}
