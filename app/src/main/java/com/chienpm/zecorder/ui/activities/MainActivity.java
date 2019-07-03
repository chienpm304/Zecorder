package com.chienpm.zecorder.ui.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.ui.adapters.ViewPaperAdapter;
import com.chienpm.zecorder.ui.fragments.LiveStreamFragment;
import com.chienpm.zecorder.ui.fragments.SettingFragment;
import com.chienpm.zecorder.ui.fragments.VideoManagerFragment;
import com.chienpm.zecorder.ui.services.RecordingMonitorService;

public class MainActivity extends AppCompatActivity {

    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 1312;
    private static final String TAG = "chienpm";
    private static final int MY_PERMISSIONS_REQUEST_CAMERA_PERMISSION = 1231;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO_PERMISSION = 2120;
    private boolean mCameraPermissionGranted = false;
    private boolean mRecordAudioPermissionGranted = false;

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private ViewPaperAdapter mAdapter;

    private int [] tabIcons = {
            R.drawable.ic_video,
            R.drawable.ic_live,
            R.drawable.ic_setting
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        checkPermissions();

    }

    private void initViews() {

        mViewPager = findViewById(R.id.viewpaper);
        setupViewPaper();

        mTabLayout = findViewById(R.id.tabLayout);
        mTabLayout.setupWithViewPager(mViewPager);
        setupTabIcon();

        /*
         * View initization
         */
        findViewById(R.id.fab_rec).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecordingActivity();
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

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Permission to draw Overlay
            if(!Settings.canDrawOverlays(this)){
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
            }
            //Permission to use camera
            if(checkCameraHardware(this)) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA},
                            MY_PERMISSIONS_REQUEST_CAMERA_PERMISSION);
                } else {
                    mCameraPermissionGranted = true;
                }
            }
            else {
                mCameraPermissionGranted = false;
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraPermissionGranted = true;
                } else {
                    Toast.makeText(this,
                            "User denied Camera permission", Toast.LENGTH_SHORT).show();
                    mCameraPermissionGranted = false;
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {

            //Check if the permission is granted or not.
            if (resultCode != RESULT_OK) { //Permission is not available
                Toast.makeText(this,
                        "Draw over other app permission not available. Closing the application",
                        Toast.LENGTH_SHORT).show();

                finish();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startRecordingActivity() {
        Intent serviceIntent = new Intent(MainActivity.this, RecordingMonitorService.class);

        if(mCameraPermissionGranted){
            //Todo: un-comment the line of code below
//            serviceIntent.setAction("Camera_On"); //Camera_Off
        }
        if(mRecordAudioPermissionGranted){
            //Todo: setup audio

        }

        startService(serviceIntent);
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
}
