package com.vtv.farshutter.Fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.vtv.farshutter.Net.NativeSocketReceiver;
import com.vtv.farshutter.R;
import com.vtv.farshutter.Services.CameraService;
import com.vtv.farshutter.Utils.CameraPreview;

import java.io.ByteArrayOutputStream;

public class CameraFragment extends Fragment implements SensorEventListener, NativeSocketReceiver.NativeSocketResponse {

    //region Fields

    private static final String TAG = "CameraFragment";
    private CameraService mCameraService = null;
    private CameraPreview mPreview;
    private Camera mCamera;
    private FrameLayout mPreviewContainer;
    private View mVRoot;

    private int deviceWidth;
    private int orientation;
    private int degrees = -1;
    private SensorManager sensorManager = null;
    private boolean isSending = false;
    private boolean ignore = false;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CameraService.CameraBinder binder = (CameraService.CameraBinder) service;

            mCameraService = binder.getService();
            if (mCameraService != null){
                mCameraService.setCallback(CameraFragment.this);
                mCameraService.startServer();
                createCamera();
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
        mCamera = getCameraInstance();

        // Setting the right parameters in the camera
        Camera.Parameters params = mCamera.getParameters();

        //params.setPictureSize(2560, 1920);
        // params.setPictureSize(1200, 1600);
        params.setPictureFormat(PixelFormat.JPEG);
        params.setJpegQuality(90);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(params);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(getActivity(), mCamera, mPreviewCallBack, mAutoFocusCallback);

        // Calculating the width of the preview so it is proportional.
        float heightFloat = (float) (deviceWidth * 4 / 3);
        int height = Math.round(heightFloat);
        // Resizing the LinearLayout so we can make a proportional preview. This
        // approach is not 100% perfect because on devices with a really small
        // screen the the image will still be distorted - there is place for
        // improvment.
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(deviceWidth, height);
        // layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mPreviewContainer.setLayoutParams(layoutParams);

        // Adding the camera preview after the FrameLayout and before the button
        // as a separated element.
        mPreviewContainer.addView(mPreview);
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
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
        View view = mVRoot = inflater.inflate(R.layout.fragment_camera, container, false);

        mPreviewContainer = (FrameLayout) view.findViewById(R.id.camera_preview);

        Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        deviceWidth = display.getWidth();

        // Getting the sensor service.
        sensorManager = (SensorManager) getActivity().getSystemService(getActivity().SENSOR_SERVICE);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register this class as a listener for the accelerometer sensor
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        //Bind controller to service
        Intent controllerIntent = new Intent(this.getActivity().getApplicationContext(), CameraService.class);
        this.getActivity().getApplicationContext().bindService(controllerIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();

        //Unbind from controller service
        this.getActivity().getApplicationContext().unbindService(mServiceConnection);

        //
        releaseCamera();
        if (mCameraService != null){
            mCameraService.removeCallback();
        }
        if (mPreviewContainer != null && mPreviewContainer.getChildCount() > 0) {
            mPreviewContainer.removeViewAt(0);
        }
        mCameraService = null;
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
            ignore = !ignore;
            if (ignore || isSending) return;
            isSending = true;

            try {
                mJpegCompressionBuffer.reset();
                // File name of the image that we just took.
                //String fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()).toString() + ".jpg";

                // Creating the directory where to save the image. Sadly in older
                // version of Android we can not get the Media catalog name
                //File mkDir = new File(sdRoot, dir);
                //mkDir.mkdirs();
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = parameters.getPreviewSize();

                YuvImage image = new YuvImage(data, parameters.getPreviewFormat(), size.width, size.height, null);
                //File file = new File(sdRoot, dir + fileName);
                //FileOutputStream filecon = new FileOutputStream(file);
                image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 90, mJpegCompressionBuffer);
                byte[] dataImage = mJpegCompressionBuffer.toByteArray();
                mCameraService.sendImageData(dataImage);
            } catch (Exception e) {
                System.out.println("preview exception");
            }
            // System.out.println("--------->" + mJpegCompressionBuffer.toByteArray().length);
            isSending = false;
            /*mCamera.setPreviewCallback(null);*/
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
    public void OnReceiveCode(String data) {

    }

    //endregion

    //region ChildClasses

    //endregion

    //region Interfaces

    //endregion
}
