package com.chienpm.zecorder.ui.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.controllers.settings.CameraSetting;
import com.chienpm.zecorder.controllers.settings.SettingManager;
import com.chienpm.zecorder.controllers.streaming.StreamProfile;
import com.chienpm.zecorder.ui.activities.MainActivity;
import com.chienpm.zecorder.ui.services.recording.RecordingService;
import com.chienpm.zecorder.ui.services.recording.RecordingService.RecordingBinder;
import com.chienpm.zecorder.ui.services.streaming.StreamingService;
import com.chienpm.zecorder.ui.services.streaming.StreamingService.StreamingBinder;
import com.chienpm.zecorder.ui.utils.CameraPreview;
import com.chienpm.zecorder.ui.utils.MyUtils;

import static com.chienpm.zecorder.ui.services.streaming.StreamingService.*;


public class ControllerService extends Service{
    private static final String TAG = "STControllerSV_chienpm";

    private BaseService mService;
    private Boolean mRecordingServiceBound = false;
    private View mViewRoot;
    private View mCameraLayout;
    private WindowManager mWindowManager;

    WindowManager.LayoutParams paramViewRoot;

    WindowManager.LayoutParams paramCam;

    WindowManager.LayoutParams paramCountdown;


    private Intent mScreenCaptureIntent = null;

    private ImageView mImgClose, mImgRec, mImgStart, mImgStop, mImgPause, mImgResume, mImgCapture, mImgLive, mImgSetting;
    private Boolean mRecordingStarted = false;
    private Boolean mRecordingPaused = false;
    private Camera mCamera;
    private LinearLayout cameraPreview;
    private int mScreenWidth, mScreenHeight;
    private TextView mTvCountdown;
    private View mCountdownLayout;
    private int mCameraWidth = 160, mCameraHeight = 90;
    private StreamProfile mStreamProfile;
    private int mMode;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent==null)
            return START_NOT_STICKY;

        String action = intent.getAction();
        if(action!=null) {
            handleIncomeAction(intent);

            Log.i(TAG, "StreamingControllerService: onStartCommand()");

            return START_NOT_STICKY;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void handleIncomeAction(Intent intent) {
        String action = intent.getAction();
        if(TextUtils.isEmpty(action))
            return;

        switch (action){
            case MyUtils.ACTION_INIT_CONTROLLER:
                mMode = intent.getIntExtra(MyUtils.KEY_CONTROLlER_MODE, MyUtils.MODE_RECORDING);
                mScreenCaptureIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if(mMode == MyUtils.MODE_STREAMING)
                    mStreamProfile = (StreamProfile) intent.getSerializableExtra(MyUtils.STREAM_PROFILE);
                boolean isCamera = intent.getBooleanExtra(MyUtils.KEY_CAMERA_AVAILABLE, false);

                if(isCamera)
                    initCameraView();

                if(mScreenCaptureIntent == null){
                    Log.d(TAG, "mScreenCaptureIntent is NULL");
                    stopSelf();
                }
                else{
                    Log.d(TAG, "StreamingControllerService: before run bindStreamService()");
                    bindStreamingService();
                }
                break;

            case MyUtils.ACTION_UPDATE_SETTING:
                handleUpdateSetting(intent);
                break;

            case MyUtils.ACTION_NOTIFY_FROM_STREAM_SERVICE:
                handleNotifyFromStreamService(intent);
                break;


        }
    }

    private void handleNotifyFromStreamService(Intent intent) {
        String notify_msg = intent.getStringExtra(StreamingService.KEY_NOTIFY_MSG);
        if(TextUtils.isEmpty(notify_msg))
            return;
        switch (notify_msg){
            case NOTIFY_MSG_CONNECTION_STARTED:
                MyUtils.toast(getApplicationContext(), "Stream started", Toast.LENGTH_SHORT);
                break;

            case NOTIFY_MSG_CONNECTION_FAILED:
                MyUtils.toast(getApplicationContext(), "Connection to server failed. Please try later", Toast.LENGTH_LONG);
                mImgStop.performClick();
                break;

            case NOTIFY_MSG_CONNECTION_DISCONNECTED:
                MyUtils.toast(getApplicationContext(), "Connection disconnected", Toast.LENGTH_SHORT);
                break;

            case NOTIFY_MSG_STREAM_STOPPED:
                MyUtils.toast(getApplicationContext(), "Stream Stopped", Toast.LENGTH_LONG);
                break;

            case NOTIFY_MSG_ERROR:
                MyUtils.toast(getApplicationContext(), "Sorry, Error occurs!", Toast.LENGTH_LONG);
                break;
        }
    }

    private void handleUpdateSetting(Intent intent) {
        int key = intent.getIntExtra(MyUtils.ACTION_UPDATE_SETTING, -1);
        switch (key){
            case R.string.setting_camera_size:
                updateCameraSize();
                break;
            case R.string.setting_camera_position:
                updateCameraPosition();
                break;
            case R.string.setting_camera_mode:
                updateCameraMode();
                break;
        }
    }

    private void updateCameraMode() {
        CameraSetting profile = SettingManager.getCameraProfile(getApplicationContext());
        if(profile.getMode().equals(CameraSetting.CAMERA_MODE_OFF))
            toggleView(mCameraLayout, View.GONE);
        else{
            if(mCameraLayout!=null){
                mWindowManager.removeViewImmediate(mCameraLayout);
                releaseCamera();
                initCameraView();
            }
        }
    }

    private void updateCameraPosition() {
        Log.d(TAG, "updateCameraPosition: ");
        CameraSetting profile = SettingManager.getCameraProfile(getApplicationContext());
        paramCam.gravity = profile.getParamGravity();
        paramCam.x = 0;
        paramCam.y = 0;
        mWindowManager.updateViewLayout(mCameraLayout,paramCam);
    }

    private void updateCameraSize() {
        CameraSetting profile = SettingManager.getCameraProfile(getApplicationContext());
        calculateCameraSize(profile);
        onConfigurationChanged(getResources().getConfiguration());
    }

    public ControllerService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "StreamingControllerService: onCreate");
        updateScreenSize();
        initParam();
        initializeViews();
    }

    private void initParam() {
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        paramViewRoot = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        paramCam = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        paramCountdown = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
    }

    private void updateScreenSize() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
    }

    private void initCameraView() {
        Log.d(TAG, "StreamingControllerService: initializeCamera()");
        CameraSetting cameraProfile = SettingManager.getCameraProfile(getApplication());

        mCameraLayout = LayoutInflater.from(this).inflate(R.layout.layout_camera_view, null);

        if(cameraProfile.getMode().equals(CameraSetting.CAMERA_MODE_BACK))
            mCamera =  Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        else
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);

        cameraPreview = mCameraLayout.findViewById(R.id.camera_preview);

        calculateCameraSize(cameraProfile);

        onConfigurationChanged(getResources().getConfiguration());

        paramCam.gravity = cameraProfile.getParamGravity();
        paramCam.x = 0;
        paramCam.y = 0;

        CameraPreview mPreview = new CameraPreview(this, mCamera);

        cameraPreview.addView(mPreview);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mCameraLayout, paramCam);
        mCamera.startPreview();

        //re-inflate controller
        mWindowManager.removeViewImmediate(mViewRoot);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mViewRoot, paramViewRoot);

        if(cameraProfile.getMode().equals(CameraSetting.CAMERA_MODE_OFF))
            toggleView(cameraPreview, View.GONE);
    }

    private void calculateCameraSize(CameraSetting cameraProfile) {
        int factor;
        switch (cameraProfile.getSize()){
            case CameraSetting.SIZE_BIG:
                factor = 3;
                break;
            case CameraSetting.SIZE_MEDIUM:
                factor = 4;
                break;
            default: //small
                factor = 5;
                break;
        }
        if(mScreenWidth > mScreenHeight) {//landscape
            mCameraWidth = mScreenWidth / factor;
            mCameraHeight = mScreenHeight / factor;
        }
        else{
            mCameraWidth = mScreenHeight/factor;
            mCameraHeight = mScreenWidth/factor;
        }
        Log.d(TAG, "calculateCameraSize: "+mScreenWidth+"x"+mScreenHeight);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged: DETECTED" + newConfig.orientation);

        if(cameraPreview!=null) {
            int width = mCameraWidth, height = mCameraHeight;

            ViewGroup.LayoutParams params = cameraPreview.getLayoutParams();
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                params.height = width;
                params.width = height;

            } else {
                params.height = height;
                params.width = width;
            }
            cameraPreview.setLayoutParams(params);
        }
    }

    private void initializeViews() {
        Log.d(TAG, "StreamingControllerService: initializeViews()");
        mViewRoot = LayoutInflater.from(this).inflate(R.layout.layout_recording, null);
        View mViewCountdown = LayoutInflater.from(this).inflate(R.layout.layout_countdown, null);

        paramViewRoot.gravity = Gravity.TOP | Gravity.START;
        paramViewRoot.x = 0;
        paramViewRoot.y = 100;

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mViewCountdown, paramCountdown);
        mWindowManager.addView(mViewRoot, paramViewRoot);

        mCountdownLayout = mViewCountdown.findViewById(R.id.countdown_container);
        mTvCountdown = mViewCountdown.findViewById(R.id.tvCountDown);

        toggleView(mCountdownLayout, View.GONE);

        mImgRec = mViewRoot.findViewById(R.id.imgRec);
        mImgCapture = mViewRoot.findViewById(R.id.imgCapture);
        mImgClose = mViewRoot.findViewById(R.id.imgClose);
        mImgLive = mViewRoot.findViewById(R.id.imgLive);
        mImgPause = mViewRoot.findViewById(R.id.imgPause);
        mImgStart = mViewRoot.findViewById(R.id.imgStart);
        mImgSetting = mViewRoot.findViewById(R.id.imgSetting);
        mImgStop = mViewRoot.findViewById(R.id.imgStop);
        mImgResume = mViewRoot.findViewById(R.id.imgResume);

        toggleView(mImgResume, View.GONE);
        toggleView(mImgStop, View.GONE);
        toggleNavigationButton(View.GONE);

        mImgCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyUtils.toast(getApplicationContext(), "Capture clicked", Toast.LENGTH_SHORT);
                toggleNavigationButton(View.GONE);
                if(mCameraLayout.getVisibility() == View.GONE){
                    toggleView(mCameraLayout, View.VISIBLE);
                }
                else{
                    toggleView(mCameraLayout, View.GONE);
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
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(MyUtils.ACTION_OPEN_SETTING_ACTIVITY);
                startActivity(intent);

            }
        });

        mImgStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavigationButton(View.GONE);

                if(mRecordingServiceBound){

                    toggleView(mCountdownLayout, View.VISIBLE);

                    int countdown = (SettingManager.getCountdown(getApplication())+1) * 1000;

                    new CountDownTimer(countdown, 1000) {

                        public void onTick(long millisUntilFinished) {
                            toggleView(mViewRoot, View.GONE);
                            mTvCountdown.setText(""+(millisUntilFinished / 1000));
                        }

                        public void onFinish() {
                            toggleView(mCountdownLayout, View.GONE);
                            toggleView(mViewRoot, View.VISIBLE);
                            mRecordingStarted = true;
                            mService.startPerformService();
                            MyUtils.toast(getApplicationContext(), "Recording Started", Toast.LENGTH_LONG);
                        }
                    }.start();

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
                toggleNavigationButton(View.GONE);

                if(mRecordingServiceBound){
                    //Todo: stop and save recording
                    mRecordingStarted = false;

                    mService.stopPerformService();

                    if(mMode==MyUtils.MODE_RECORDING){
                        ((RecordingService)mService).saveVideoToDatabase();
                        MyUtils.toast(getApplicationContext(), "Recording Service Closed", Toast.LENGTH_LONG);
                        return;
                    }

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
                if(mRecordingStarted){
                    mImgStop.performClick();
                }

                stopSelf();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(MyUtils.ACTION_OPEN_LIVE_ACTIVITY);
                startActivity(intent);
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
                        initialX = paramViewRoot.x;
                        initialY = paramViewRoot.y;

                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        if(event.getRawX() < mScreenWidth/2) {
                            paramViewRoot.x = 0;
                        }
                        else {
                            paramViewRoot.x = mScreenWidth;
                        }
                        paramViewRoot.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mViewRoot, paramViewRoot);


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
                        paramViewRoot.x = initialX + (int) (event.getRawX() - initialTouchX);
                        paramViewRoot.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mViewRoot, paramViewRoot);
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


    private void toggleView(View view, int visible) {
        view.setVisibility(visible);
    }

    private void bindStreamingService() {
        Log.d(TAG, "Controller: bindService()");

        Intent service;

        if(mMode == MyUtils.MODE_STREAMING) {
            if(mStreamProfile == null)
                throw new RuntimeException("Streaming proflie is null");

            service = new Intent(getApplicationContext(), StreamingService.class);
            Bundle bundle = new Bundle();

            bundle.putSerializable(MyUtils.STREAM_PROFILE, mStreamProfile);

            service.putExtras(bundle);

        }
        else {
            service = new Intent(getApplicationContext(), RecordingService.class);
        }

        service.putExtra(Intent.EXTRA_INTENT, mScreenCaptureIntent);

        bindService(service, mStreamingServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection mStreamingServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IBinder binder;
            if(mMode == MyUtils.MODE_STREAMING) {
                binder = (StreamingBinder) service;
                mService = ((StreamingBinder) binder).getService();
            }
            else{
                binder = (RecordingBinder)service;
                mService = ((RecordingBinder) binder).getService();
            }
            mRecordingServiceBound = true;

            MyUtils.toast(getApplicationContext(), "Streaming service connected", Toast.LENGTH_SHORT);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRecordingServiceBound = false;
            MyUtils.toast(getApplicationContext(), "Streaming service disconnected", Toast.LENGTH_SHORT);
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
            mWindowManager.removeView(mCameraLayout);
            releaseCamera();
        }

        if(mService !=null && mRecordingServiceBound) {
            unbindService(mStreamingServiceConnection);
            mService.stopSelf();
            mRecordingServiceBound = false;
        }
    }
}
