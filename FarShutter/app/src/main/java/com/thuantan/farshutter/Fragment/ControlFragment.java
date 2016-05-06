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
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.zxing.Result;
import com.thuantan.farshutter.Common.ActionCodes;
import com.thuantan.farshutter.Net.NativeSocketReceiver;
import com.thuantan.farshutter.Net.Wifi.WifiAPI;
import com.thuantan.farshutter.R;
import com.thuantan.farshutter.Services.CameraService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.markushi.ui.CircleButton;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ControlFragment extends Fragment implements NativeSocketReceiver.NativeSocketResponse {
    //region Fields

    private enum ReceiveState {
        RECEIVE_CAPTURED_IMAGE, RECEIVE_STEAM_IMAGE
    }

    private static final String TAG = "ControlFragment";
    private CameraService mCameraService = null;
    private Dialog mPreviewImageDialog = null;
    private Dialog mConfirmBackDialog = null;
    private Dialog mServerDisconnectDialog = null;
    private boolean mScanning = false;
    private ZXingScannerView mScannerView = null;

    private CircleButton mBtnTakePhoto;
    private ImageButton mBtnConnectCamera;
    private ImageButton mBtnImagePreview;
    private ImageView mIvCameraPreview;
    private Dialog mQRScannerDialog;
    private Bitmap bmp1;
    private Bitmap bmp2;
    private Bitmap result;
    private boolean bmpChoose = true; /* true is 1, false is 2 */
    private boolean isRenderStream = false;
    private ReceiveState mReceiveState = ReceiveState.RECEIVE_STEAM_IMAGE;
    private String lastImagePath = null;
    private Bitmap lastImage = null;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CameraService.CameraBinder binder = (CameraService.CameraBinder) service;
            mCameraService = binder.getService();

            if (mCameraService != null) {
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

    private void ShowQRScanner() {
        if (mQRScannerDialog != null) {
            mQRScannerDialog.show();

            if (mScannerView == null) {
                mScannerView = new ZXingScannerView(getContext());
                mScannerView.offsetTopAndBottom(0);
                mScannerView.setResultHandler(new ZXingScannerView.ResultHandler() {
                    @Override
                    public void handleResult(Result result) {
                        mQRScannerDialog.dismiss();

                        String[] parts = result.getText().split(";");

                        if (parts.length >= 3) {
                            //Connect WIFI
                            WifiAPI.Connect(getContext(), parts[1], parts[2]);

                            //Connect Server
                            Pattern p = Pattern.compile("^([0-2]?)([0-9]?)[0-9]\\.([0-2]?)([0-9]?)[0-9]\\.([0-2]?)([0-9]?)[0-9]\\.([0-2]?)([0-9]?)[0-9]$");
                            Matcher m = p.matcher(parts[0]);
                            if (m.matches()) {
                                //Wait for wifi connected
                                while (WifiAPI.GetCurrentIP() == null) {
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                ConnectServer(parts[0]);
                            }
                        } else {
                            Toast.makeText(getContext(), "Scan Fail", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                FrameLayout scanner = (FrameLayout) mQRScannerDialog.findViewById(R.id.scanner);
                scanner.addView(mScannerView);
            }

            mQRScannerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    ReleaseScan();
                }
            });
        }
    }

    private void StartScan() {
        if (mScannerView != null) {
            mScanning = true;
            mScannerView.startCamera();
        }
    }

    private void StopScan() {
        if (mScannerView != null) {
            mScannerView.stopCamera();
        }
    }

    private void ReleaseScan() {
        if (mScannerView != null) {
            mScanning = false;
            mScannerView.stopCamera();
        }
    }

    private void ConnectServer(String ip) {
        if (mCameraService != null) {
            Log.d(TAG, ip);
            mCameraService.initClient(ip);
        }
    }

    private void ShowCapturedImage(byte[] data) {
        mReceiveState = ReceiveState.RECEIVE_STEAM_IMAGE;
        try {
            ReplacePreviewImage(data);
            ShowPreviewImage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ShowPreviewImage(){
        mPreviewImageDialog.show();

        ImageView imageView = (ImageView) mPreviewImageDialog.findViewById(R.id.preview_image);
        imageView.setImageBitmap(lastImage);
    }

    private void ReceiveCapturedImage(byte[] data){
        ShowCapturedImage(data);
    }

    private void ReplacePreviewImage(byte[] data) throws IOException {
        lastImagePath = SavePicture(data);

        //Add Image to Media
        MediaScannerConnection.scanFile(getContext(), new String[]{lastImagePath}, null, null);

        //Show Image in preview
        if (lastImage != null){
            lastImage.recycle();
            lastImage = null;
        }

        lastImage = BitmapFactory.decodeByteArray(data,0, data.length);
        mBtnImagePreview.setImageBitmap(lastImage);
    }

    private void RenderStreamImage(byte[] data) {
        if (isRenderStream) {
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
    }

    private String SavePicture(byte[] data) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "FARSHUTTER_" + timeStamp;
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File cameraFolder = new File(storageDir.getAbsolutePath(), "Camera Roll");

        if (!cameraFolder.exists()) {
            cameraFolder.mkdir();
        }

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                cameraFolder      /* directory */
        );

        Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);

        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);

        FileOutputStream outputStream = new FileOutputStream(image);
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

        return image.getAbsolutePath();
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

        mBtnTakePhoto = (CircleButton) view.findViewById(R.id.btnTakePhoto);
        mBtnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCameraService != null){
                    mCameraService.sendCodeData(ActionCodes.CAPTURE);
                }
            }
        });

        mBtnImagePreview = (ImageButton) view.findViewById(R.id.btnReview);

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

        if (mScanning) {
            StartScan();
        }

        isRenderStream = true;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mCameraService != null) {
            mCameraService.removeCallback();
            mCameraService = null;
        }

        StopScan();

        isRenderStream = false;
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
    public void OnReceiveImage(final byte[] data) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (mReceiveState) {
                    case RECEIVE_STEAM_IMAGE:
                        RenderStreamImage(data);
                        break;
                    case RECEIVE_CAPTURED_IMAGE:
                        ReceiveCapturedImage(data);
                        break;
                }
            }
        });
    }

    @Override
    public void OnReceiveCode(String data) {
        Log.d(TAG, data);

        switch (data) {
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
