package com.chienpm.zecorder.ui.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.ui.activities.MainActivity;
import com.chienpm.zecorder.ui.services.RecordingService.RecordingBinder;
import com.chienpm.zecorder.ui.utils.CameraPreview;
import com.chienpm.zecorder.ui.utils.MyUtils;


public class RecordingControllerService extends Service {
    private static final String TAG = "chienpm";

    private RecordingService mRecordingService;
    private Boolean mRecordingServiceBound = false;

    private View mViewRoot;
    private View mCameraLayout;
    private WindowManager mWindowManager;

    final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
    );

    final WindowManager.LayoutParams paramCam = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
    );

    private Intent mScreenCaptureIntent = null;

    private ImageView mImgClose, mImgRec, mImgStart, mImgStop, mImgPause, mImgResume, mImgCapture, mImgLive, mImgSetting;
    private Boolean mRecordingStarted = false;
    private Boolean mRecordingPaused = false;
    private Camera mCamera;
    private LinearLayout cameraPreview;
    private CameraPreview mPreview;
    private int mScreenWidth, mScreenHeight;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.d(TAG, "RecordingControllerService: onStartCommand()");
        if(action != null){
            if(TextUtils.equals(action, "Camera_Available")){
                initCameraView();
            }

        }
        mScreenCaptureIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);

        if(mScreenCaptureIntent == null){
            Log.d(TAG, "mScreenCaptureIntent is NULL");
            stopSelf();
        }
        else{
            Log.d(TAG, "RecordingControllerService: before run bindRecordingService()");
            bindRecordingService();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public RecordingControllerService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
//        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RecordingControllerService: onCreate");
        initializeViews();
        updateScreenSize();

    }

    private void updateScreenSize() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
    }

    private void initCameraView() {
        Log.d(TAG, "RecordingControllerService: initializeCamera()");
        mCameraLayout = LayoutInflater.from(this).inflate(R.layout.layout_camera_view, null);

        mCamera =  Camera.open();
        mCamera.setDisplayOrientation(90);
        cameraPreview = (LinearLayout) mCameraLayout.findViewById(R.id.camera_preview);
        mPreview = new CameraPreview(this, mCamera);

        paramCam.gravity = Gravity.BOTTOM | Gravity.END;
        paramCam.x = 50;
        paramCam.y = 50;

        cameraPreview.addView(mPreview);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mCameraLayout, paramCam);
        mCamera.startPreview();

        //re-inflate controller
        mWindowManager.removeViewImmediate(mViewRoot);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mViewRoot, params);
    }


    private void initializeViews() {
        Log.d(TAG, "RecordingControllerService: initializeViews()");
        mViewRoot = LayoutInflater.from(this).inflate(R.layout.layout_recording, null);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mViewRoot, params);

        mImgRec = mViewRoot.findViewById(R.id.imgRec);
        mImgCapture = mViewRoot.findViewById(R.id.imgCapture);
        mImgClose = mViewRoot.findViewById(R.id.imgClose);
        mImgLive = mViewRoot.findViewById(R.id.imgLive);
        mImgPause = mViewRoot.findViewById(R.id.imgPause);
        mImgStart = mViewRoot.findViewById(R.id.imgStart);
        mImgSetting = mViewRoot.findViewById(R.id.imgSetting);
        mImgStop = mViewRoot.findViewById(R.id.imgStop);
        mImgResume = mViewRoot.findViewById(R.id.imgResume);

        mImgResume.setVisibility(View.GONE);
        mImgStop.setVisibility(View.GONE);


        toggleNavigationButton(View.GONE);

        mImgCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyUtils.toast(getApplicationContext(), "Capture clicked", Toast.LENGTH_SHORT);
                toggleNavigationButton(View.GONE);
                if(cameraPreview.getVisibility() == View.GONE){
                    cameraPreview.setVisibility(View.VISIBLE);
                }
                else{
                    cameraPreview.setVisibility(View.GONE);
                }
            }
        });

        mImgPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyUtils.toast(getApplicationContext(), "Pause recording!", Toast.LENGTH_SHORT);
                toggleNavigationButton(View.GONE);

                mRecordingPaused = true;
            }
        });

        mImgResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyUtils.toast(getApplicationContext(), "Resume recording!", Toast.LENGTH_SHORT);
                toggleNavigationButton(View.GONE);
                mRecordingPaused = false;
            }
        });

        mImgSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyUtils.toast(getApplicationContext(), "Setting clicked", Toast.LENGTH_SHORT);
                toggleNavigationButton(View.GONE);
            }
        });

        mImgStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavigationButton(View.GONE);

                if(mRecordingServiceBound){
                    //Todo: start recording
                    mRecordingStarted = true;
                    mRecordingService.startRecording();
                    MyUtils.toast(getApplicationContext(), "Recording Started", Toast.LENGTH_LONG);
                }
                else{
                    mRecordingStarted = false;
                    MyUtils.toast(getApplicationContext(), "Recording Service connection has not been established", Toast.LENGTH_LONG);
                }
            }
        });

        mImgStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                MyUtils.toast(getApplicationContext(), "Stop recording!", Toast.LENGTH_SHORT);
                toggleNavigationButton(View.GONE);

                if(mRecordingServiceBound){
                    //Todo: stop and save recording
                    mRecordingStarted = false;
                    mRecordingService.stopRecording();
                    MyUtils.toast(getApplicationContext(), "Recording Stopped", Toast.LENGTH_LONG);
                }
                else{
                    mRecordingStarted = true;
                    MyUtils.toast(getApplicationContext(), "Recording Service connection has not been established", Toast.LENGTH_LONG);
                }
            }
        });


        mImgLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Live clicked", Toast.LENGTH_SHORT).show();
                toggleNavigationButton(View.GONE);
            }
        });

        mImgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                stopSelf();
            }
        });

        mViewRoot.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;

                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        if(event.getRawX() < mScreenWidth/2) {
                            params.x = 0;
                        }
                        else {
                            params.x = mScreenWidth;
                        }
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mViewRoot, params);


                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);

                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (Xdiff < 20 && Ydiff < 20) {
                            if (isViewCollapsed()) {
                                //When user clicks on the image view of the collapsed layout,
                                //visibility of the collapsed layout will be changed to "View.GONE"
                                //and expanded view will become visible.
                                toggleNavigationButton(View.VISIBLE);
                            }
                            else {
                                toggleNavigationButton(View.GONE);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mViewRoot, params);
                        return true;
                }

                return false;
            }
        });
        mViewRoot.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus)
                    toggleNavigationButton(View.GONE);
            }
        });
    }

    private void bindRecordingService() {
        Log.d(TAG, "RecordingControllerService: bindRecordingService()");
        Intent mRecordingServiceIntent = new Intent(getApplicationContext(), RecordingService.class);
        mRecordingServiceIntent.putExtra(Intent.EXTRA_INTENT, mScreenCaptureIntent);
        bindService(mRecordingServiceIntent, mRecordingServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mRecordingServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RecordingBinder binder = (RecordingBinder) service;
            mRecordingService = binder.getService();
            mRecordingServiceBound = true;

            MyUtils.toast(getApplicationContext(), "Recording service connected", Toast.LENGTH_SHORT);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRecordingServiceBound = false;
            MyUtils.toast(getApplicationContext(), "Recording service disconnected", Toast.LENGTH_SHORT);
        }
    };

    private boolean isViewCollapsed() {
        return mViewRoot == null || mViewRoot.findViewById(R.id.imgSetting).getVisibility() == View.GONE;
    }

    void toggleNavigationButton(int viewMode){
        //Todo: make animation here

        mImgStart.setVisibility(viewMode);
        mImgSetting.setVisibility(viewMode);
        mImgPause.setVisibility(viewMode);
        mImgCapture.setVisibility(viewMode);
        mImgLive.setVisibility(viewMode);
        mImgClose.setVisibility(viewMode);
        mImgStop.setVisibility(viewMode);
        mImgResume.setVisibility(viewMode);

        if(viewMode == View.GONE){
            mViewRoot.setPadding(32,32, 32, 32);
        }else{
            if(mRecordingStarted){
                mImgStart.setVisibility(View.GONE);
            }
            else{
                mImgStop.setVisibility(View.GONE);
            }

            if(mRecordingPaused){
                mImgPause.setVisibility(View.GONE);
            }
            else{
                mImgResume.setVisibility(View.GONE);
            }
            mViewRoot.setPadding(32,48, 32, 48);
        }
    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mViewRoot!=null){
            mWindowManager.removeViewImmediate(mViewRoot);
        }
        if(mCameraLayout!=null){
            mWindowManager.removeViewImmediate(mCameraLayout);
            releaseCamera();
        }
        if(mRecordingService!=null && mRecordingServiceBound) {
            unbindService(mRecordingServiceConnection);
            mRecordingServiceBound = false;

        }
    }
}
