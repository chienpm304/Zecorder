package com.chienpm.zecorder.ui.activities;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.ui.adapters.ViewPaperAdapter;
import com.chienpm.zecorder.ui.fragments.LiveStreamFragment;
import com.chienpm.zecorder.ui.fragments.SettingFragment;
import com.chienpm.zecorder.ui.fragments.VideoManagerFragment;
import com.chienpm.zecorder.ui.services.RecordingControllerService;
import com.chienpm.zecorder.ui.utils.MyUtils;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "chienpm";
    private static final int PERMISSION_REQUEST_CODE = 3004;
    private static final int PERMISSION_DRAW_OVER_WINDOW = 3005;
    private static final int PERMISSION_RECORD_DISPLAY = 3006;
    private static String[] mPermission = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private Intent mScreenCaptureIntent = null;

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private ViewPaperAdapter mAdapter;

    private int [] tabIcons = {
            R.drawable.ic_video,
            R.drawable.ic_live,
            R.drawable.ic_setting
    };

    private int mScreenCaptureResultCode = MyUtils.RESULT_CODE_FAILED;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();

        Intent intent = getIntent();
        if(intent!=null)
            handleIncomingRequest(intent);
    }

    private void handleIncomingRequest(Intent intent) {
        if(intent != null) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case MyUtils.ACTION_OPEN_SETTING_ACTIVITY:
                    mTabLayout.getTabAt(2).select();
                    break;
                case MyUtils.ACTION_OPEN_LIVE_ACTIVITY:
                    mTabLayout.getTabAt(1).select();
                    break;
                case MyUtils.ACTION_OPEN_VIDEO_MANAGER_ACTIVITY:
                    mTabLayout.getTabAt(0).select();
                    break;
            }
        }
    }

    private void requestScreenCaptureIntent() {
        if(mScreenCaptureIntent == null){
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), PERMISSION_RECORD_DISPLAY);
        }
    }

    ImageView mImgRec;

    private void initViews() {

        mViewPager = findViewById(R.id.viewpaper);
        setupViewPaper();

        mTabLayout = findViewById(R.id.tabLayout);
        mTabLayout.setupWithViewPager(mViewPager);
        setupTabIcon();

        /*
         * View initization
         */
        mImgRec =  findViewById(R.id.fab_rec);
        mImgRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isMyServiceRunning(RecordingControllerService.class)){
                    MyUtils.showSnackBarNotification(mImgRec,"Recording service is running!", Snackbar.LENGTH_LONG);
                    return;
                }
                if(mScreenCaptureIntent == null || mScreenCaptureResultCode == MyUtils.RESULT_CODE_FAILED)
                    requestScreenCaptureIntent();
                if(hasPermission()) {
                    startRecordingControllerService();
                }
                else{
                    requestPermissions();
                    requestScreenCaptureIntent();
                }
            }
        });


    }

    private void setupTabIcon() {
        mTabLayout.getTabAt(0).setIcon(tabIcons[0]);
        mTabLayout.getTabAt(1).setIcon(tabIcons[1]);
        mTabLayout.getTabAt(2).setIcon(tabIcons[2]);
        mTabLayout.getTabAt(1).select();
    }

    private void setupViewPaper() {
        mAdapter = new ViewPaperAdapter(getSupportFragmentManager());
        mAdapter.addFragment(new VideoManagerFragment(), "Video");
        mAdapter.addFragment(new LiveStreamFragment(), "Live");
        mAdapter.addFragment(new SettingFragment(), "Setting");
        mViewPager.setAdapter(mAdapter);
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // PERMISSION DRAW OVER
            if(!Settings.canDrawOverlays(this)){
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, PERMISSION_DRAW_OVER_WINDOW);
            }
            ActivityCompat.requestPermissions(this, mPermission, PERMISSION_REQUEST_CODE);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    int granted = PackageManager.PERMISSION_GRANTED;
                    for(int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != granted) {
                            MyUtils.showSnackBarNotification(mImgRec,"Please grant all permissions to record screen.", Snackbar.LENGTH_LONG);
                            return;
                        }
                    }

                    shouldStartRecordingControllerSerivce();
                }
                break;
            }


        }
    }

    private void shouldStartRecordingControllerSerivce() {
        if (hasPermission() && !isMyServiceRunning(RecordingControllerService.class)) {
            MyUtils.showSnackBarNotification(mImgRec, "Permissions Granted!", Snackbar.LENGTH_SHORT);
            startRecordingControllerService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PERMISSION_DRAW_OVER_WINDOW) {

            //Check if the permission is granted or not.
            if (resultCode != RESULT_OK) { //Permission is not available
                MyUtils.showSnackBarNotification(mImgRec, "Draw over other app permission not available.",Snackbar.LENGTH_SHORT);
            }
        }
        else if( requestCode == PERMISSION_RECORD_DISPLAY) {
            if(resultCode != RESULT_OK){
                MyUtils.showSnackBarNotification(mImgRec, "Recording display permission not available.",Snackbar.LENGTH_SHORT);
                mScreenCaptureIntent = null;
            }
            else{
                mScreenCaptureIntent = data;
                mScreenCaptureIntent.putExtra(MyUtils.SCREEN_CAPTURE_INTENT_RESULT_CODE, resultCode);
                mScreenCaptureResultCode = resultCode;

                shouldStartRecordingControllerSerivce();
            }
        }
        else{
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startRecordingControllerService() {

        Intent recordingControllerService = new Intent(MainActivity.this, RecordingControllerService.class);

        if(checkCameraHardware(this)){
            recordingControllerService.setAction("Camera_Available");
        }

        recordingControllerService.putExtra(Intent.EXTRA_INTENT, mScreenCaptureIntent);

        startService(recordingControllerService);

        finish();
    }

     /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean hasPermission(){
        int granted = PackageManager.PERMISSION_GRANTED;

        return ContextCompat.checkSelfPermission(this, mPermission[0]) == granted
                && ContextCompat.checkSelfPermission(this, mPermission[1]) == granted
                    && ContextCompat.checkSelfPermission(this, mPermission[2]) == granted
                        && Settings.canDrawOverlays(this)
                            && mScreenCaptureIntent != null
                                && mScreenCaptureResultCode != MyUtils.RESULT_CODE_FAILED;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

