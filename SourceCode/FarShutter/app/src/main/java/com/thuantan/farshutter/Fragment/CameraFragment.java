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
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.thuantan.farshutter.Common.ActionCodes;
import com.thuantan.farshutter.Common.Settings;
import com.thuantan.farshutter.Net.NativeSocketReceiver;
import com.thuantan.farshutter.Net.Wifi.WifiAPI;
import com.thuantan.farshutter.R;
import com.thuantan.farshutter.Services.CameraService;
import com.thuantan.farshutter.Utils.CameraPreview;

import net.glxn.qrgen.android.QRCode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import at.markushi.ui.CircleButton;

public class CameraFragment extends Fragment implements SensorEventListener, NativeSocketReceiver.NativeSocketResponse {

    //region Fields

    private static final String TAG = "CameraFragment";
    private CameraService mCameraService = null;
    private CameraPreview mPreview;
    private Camera mCamera;
    private FrameLayout mPreviewContainer;
    private ImageButton mBtnQRCode;
    private ImageButton mBtnImagePreview;
    private CircleButton mBtnTakePhoto;
    private Dialog mQRCodeDialog;
    private Dialog mConfirmBackDialog;
    //private Dialog mPreviewImageDialog;

    private Bitmap mQRCodeImage = null;
    private int deviceWidth;
    private int orientation;
    private int degrees = -1;
    private SensorManager sensorManager = null;
    private boolean isSendingStream = false;
    private Date lastSend = new Date();
    private boolean blockStream = false;
    private Camera.Size sendImageSize = null;
    private String lastImagePath = null;
    private PowerManager.WakeLock mWakeLock = null;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CameraService.CameraBinder binder = (CameraService.CameraBinder) service;

            mCameraService = binder.getService();
            if (mCameraService != null) {
                //Wait wifi connect
                while (WifiAPI.GetCurrentIP() == null) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                mCameraService.setCallback(CameraFragment.this);
                mCameraService.startServer(Settings.GetRandomPort());
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

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.autoFocus(null);
            mCamera.release(); // release the camera for other applications
            mCamera = null;

            System.out.println("Release camera");
        }
    }

    private void createCamera() {
        // Create an instance of Camera
        Camera camera = getCameraInstance();

        if (camera == null) {
            return;
        }

        mCamera = camera;

        // Setting the right parameters in the camera
        Camera.Parameters params = mCamera.getParameters();

        //params.setPictureSize(2560, 1920);
        // params.setPictureSize(1200, 1600);
        params.setPictureFormat(PixelFormat.JPEG);
        params.setJpegQuality(90);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        mCamera.setParameters(params);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(getActivity(), mCamera, mPreviewCallBack, mAutoFocusCallback);

        // Calculating the width of the preview so it is proportional.
        /*float heightFloat = (float) (deviceWidth * 4 / 3);
        int height = Math.round(heightFloat);*/

        // Resizing the LinearLayout so we can make a proportional preview. This
        // approach is not 100% perfect because on devices with a really small
        // screen the the image will still be distorted - there is place for
        // improvment.
        //LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(deviceWidth, Math.round(deviceWidth * (float) params.getPictureSize().width / params.getPictureSize().height));
        // layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        //mPreviewContainer.setLayoutParams(layoutParams);

        // Adding the camera preview after the FrameLayout and before the button
        // as a separated element.
        mPreviewContainer.addView(mPreview);
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    private static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    private RotateAnimation getRotateAnimation(float toDegrees) {
        float compensation = 0;

        if (Math.abs(degrees - toDegrees) > 180) {
            compensation = 360;
        }

        // When the device is being held on the left side (default position for
        // a camera) we need to add, not subtract from the toDegrees.
        if (toDegrees == 0) {
            compensation = -compensation;
        }

        // Creating the animation and the RELATIVE_TO_SELF means that he image
        // will rotate on it center instead of a corner.
        RotateAnimation animation = new RotateAnimation(degrees, toDegrees - compensation, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

        // Adding the time needed to rotate the image
        animation.setDuration(250);

        // Set the animation to stop after reaching the desired position. With
        // out this it would return to the original state.
        animation.setFillAfter(true);

        return animation;
    }

    private void TakePhoto() {
        if (mCamera != null) {

            //Save photo
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        camera.takePicture(null, null, new Camera.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data, Camera camera) {
                                try {
                                    lastImagePath = SavePicture(data);

                                    //Add Image to Media
                                    MediaScannerConnection.scanFile(getContext(), new String[]{lastImagePath}, null, null);

                                    //Show Image in preview
                                    Bitmap previousBitmap = ((BitmapDrawable)mBtnImagePreview.getDrawable()).getBitmap();
                                    if (previousBitmap != null) {
                                        previousBitmap.recycle();
                                    }

                                    Bitmap decodeImage = BitmapFactory.decodeByteArray(data, 0, data.length);

                                    int marginWidth = Math.round((decodeImage.getWidth() - mBtnImagePreview.getHeight() * (decodeImage.getHeight() * 1.0f / mBtnImagePreview.getWidth())) / 2);
                                    Bitmap cropImage = Bitmap.createBitmap(decodeImage, marginWidth, 0, decodeImage.getWidth() - 2 * marginWidth, decodeImage.getHeight());
                                    decodeImage.recycle();

                                    Bitmap scaleImage = Bitmap.createScaledBitmap(cropImage, mBtnImagePreview.getWidth(), mBtnImagePreview.getHeight(),false);
                                    cropImage.recycle();

                                    mBtnImagePreview.setImageBitmap(scaleImage);

                                    SafeSendCapturedImage(data);
                                } catch (IOException | RuntimeException e) {
                                    e.printStackTrace();
                                }
                                camera.startPreview();
                            }
                        });
                    }
                }
            });
        }
    }

    private String SavePicture(byte[] data) throws IOException, RuntimeException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "FARSHUTTER_" + timeStamp;
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File cameraFolder = new File(storageDir.getAbsolutePath(), "FARSHUTTER");

        if (!cameraFolder.exists()) {
            cameraFolder.mkdir();
        }

        Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);

        if (bm == null) {
            throw new RuntimeException("data in not image format");
        }

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                cameraFolder      /* directory */
        );

        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);

        FileOutputStream outputStream = new FileOutputStream(image);
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

        bm.recycle();
        rotatedBitmap.recycle();

        return image.getAbsolutePath();
    }

    private void SafeSendCode(String code) {
        if (mCameraService != null) {
            blockStream = true;

            while (isSendingStream) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            mCameraService.sendCodeData(code);

            blockStream = false;
        }
    }

    private void SafeSendCapturedImage(byte[] data) {
        if (mCameraService != null) {
            blockStream = true;

            while (isSendingStream) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            mCameraService.sendImageData(data);

            blockStream = false;
        }
    }

    //endregion

    //region Methods

    //endregion

    //region EventListeners

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WifiAPI.TurnWifi(getContext(), false);
        WifiAPI.TurnOnOffHotspot(getContext(), Settings.HOTSPOT_SSID, Settings.HOSTPOST_PASS, true);

        //Bind controller to service
        Intent controllerIntent = new Intent(this.getContext().getApplicationContext(), CameraService.class);
        this.getActivity().getApplicationContext().bindService(controllerIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        deviceWidth = display.getWidth();

        // Getting the sensor service.
        sensorManager = (SensorManager) getContext().getSystemService(getContext().SENSOR_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        mPreviewContainer = (FrameLayout) view.findViewById(R.id.camera_preview);
        mBtnQRCode = (ImageButton) view.findViewById(R.id.btnShowQRCode);
        mBtnTakePhoto = (CircleButton) view.findViewById(R.id.btnTakePhoto);
        mBtnImagePreview = (ImageButton) view.findViewById(R.id.btnReview);

        mBtnImagePreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastImagePath != null) {
                    Log.d(TAG, lastImagePath);
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse("file://" + lastImagePath), "image/*");
                    startActivity(intent);
                }
            }
        });

        mBtnQRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mQRCodeDialog == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    LayoutInflater inflater = getLayoutInflater(null);
                    builder.setView(inflater.inflate(R.layout.qr_dialog, null));
                    builder.setTitle("QR Code");
                    builder.setPositiveButton("DONE", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    mQRCodeDialog = builder.create();
                }
                mQRCodeDialog.show();

                if (mQRCodeImage != null) {
                    mQRCodeImage.recycle();
                }

                String ip = WifiAPI.GetCurrentIP();
                if (ip != null && mCameraService != null) {
                    ImageView qrImage = (ImageView) mQRCodeDialog.findViewById(R.id.qr_code_img);
                    //TODO GET SSID AND PASS OF HOTSPOT
                    int qrSize = Math.round(deviceWidth * 0.8f);
                    mQRCodeImage = QRCode.from(ip + ":" + mCameraService.CurrentPort() + ";" + Settings.HOTSPOT_SSID + ";" + Settings.HOSTPOST_PASS).withSize(qrSize, qrSize).bitmap();
                    qrImage.setImageBitmap(mQRCodeImage);
                }
            }
        });


        mBtnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TakePhoto();
            }
        });

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

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mCameraService != null) {
                    mCameraService.CloseSocket();
                }
                getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setMessage("Do you want to close ?");
        mConfirmBackDialog = builder.create();

        /*AlertDialog.Builder builder2 = new AlertDialog.Builder(getContext());
        builder2.setView(inflater.inflate(R.layout.preview_dialog, null));
        builder2.setTitle("Preview");
        builder2.setPositiveButton("DONE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        mPreviewImageDialog = builder2.create();*/

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register this class as a listener for the accelerometer sensor
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        createCamera();

        PowerManager powerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FarShutterWakeLock");
        mWakeLock.acquire();
    }

    @Override
    public void onPause() {
        super.onPause();

        releaseCamera();

        if (mPreviewContainer != null && mPreviewContainer.getChildCount() > 0) {
            mPreviewContainer.removeViewAt(0);
        }

        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        WifiAPI.TurnOnOffHotspot(getContext(), Settings.HOTSPOT_SSID, Settings.HOSTPOST_PASS, false);

        //Unbind from controller service
        this.getActivity().getApplicationContext().unbindService(mServiceConnection);
        if (mCameraService != null) {
            mCameraService.removeCallback();
            mCameraService.CloseSocket();
        }
        mCameraService = null;

        if (mQRCodeImage != null) {
            mQRCodeImage.recycle();
            mQRCodeImage = null;
        }

        mJpegCompressionBuffer.reset();

        releaseCamera();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                RotateAnimation animation = null;
                if (event.values[0] < 4 && event.values[0] > -4) {
                    if (event.values[1] > 0 && orientation != ExifInterface.ORIENTATION_ROTATE_90) {
                        // UP
                        orientation = ExifInterface.ORIENTATION_ROTATE_90;
                        animation = getRotateAnimation(270);
                        degrees = 270;
                    } else if (event.values[1] < 0 && orientation != ExifInterface.ORIENTATION_ROTATE_270) {
                        // UP SIDE DOWN
                        orientation = ExifInterface.ORIENTATION_ROTATE_270;
                        animation = getRotateAnimation(90);
                        degrees = 90;
                    }
                } else if (event.values[1] < 4 && event.values[1] > -4) {
                    if (event.values[0] > 0 && orientation != ExifInterface.ORIENTATION_NORMAL) {
                        // LEFT
                        orientation = ExifInterface.ORIENTATION_NORMAL;
                        animation = getRotateAnimation(0);
                        degrees = 0;
                    } else if (event.values[0] < 0 && orientation != ExifInterface.ORIENTATION_ROTATE_180) {
                        // RIGHT
                        orientation = ExifInterface.ORIENTATION_ROTATE_180;
                        animation = getRotateAnimation(180);
                        degrees = 180;
                    }
                }
                if (animation != null) {
                    // rotatingImage.startAnimation(animation);
                }
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            try {
                System.out.println("focused: " + success);
            } catch (Exception e) {
                System.out.println("focused exception");
            }
        }
    };

    ByteArrayOutputStream mJpegCompressionBuffer = new ByteArrayOutputStream();

    private Camera.PreviewCallback mPreviewCallBack = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (!isSendingStream && !blockStream && mCameraService != null) {
                Date current = new Date();
                if ((current.getTime() - lastSend.getTime()) >= 125) {
                    lastSend = current;
                    isSendingStream = true;

                    mJpegCompressionBuffer.reset();

                    if (sendImageSize == null) {
                        sendImageSize = mCamera.getParameters().getPreviewSize();
                    }

                    YuvImage image = new YuvImage(data, mCamera.getParameters().getPreviewFormat(), sendImageSize.width, sendImageSize.height, null);
                    image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 90, mJpegCompressionBuffer);
                    mCameraService.sendStreamData(mJpegCompressionBuffer.toByteArray());

                    isSendingStream = false;
                }
            }
        }
    };

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

    }

    @Override
    public void OnReceiveStream(byte[] data) {

    }

    @Override
    public void OnReceiveCode(String data) {
        switch (data) {
            case ActionCodes.CAPTURE:
                TakePhoto();
                break;
        }
    }

    //endregion

    //region ChildClasses

    //endregion

    //region Interfaces

    //endregion
}
