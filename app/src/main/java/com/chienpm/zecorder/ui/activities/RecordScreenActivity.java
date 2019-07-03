package com.chienpm.zecorder.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.chienpm.zecorder.R;

import java.util.ArrayList;
import java.util.List;
public class RecordScreenActivity extends Activity {
    private static final String TAG = "MediaProjectionDemo";
    private static final int PERMISSION_CODE = 1;
    private static final List<Resolution> RESOLUTIONS = new ArrayList<Resolution>() {{
        add(new Resolution(640,360));
        add(new Resolution(960,540));
        add(new Resolution(1366,768));
        add(new Resolution(1600,900));
    }};
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private int mDisplayWidth;
    private int mDisplayHeight;
    private boolean mScreenSharing;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private Surface mSurface;
    private SurfaceView mSurfaceView;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_screen);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mSurface = mSurfaceView.getHolder().getSurface();
        mProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        ArrayAdapter<Resolution> arrayAdapter = new ArrayAdapter<Resolution>(
                this, android.R.layout.simple_list_item_1, RESOLUTIONS);
        Spinner s = (Spinner) findViewById(R.id.spinner);
        s.setAdapter(arrayAdapter);
        s.setOnItemSelectedListener(new ResolutionSelector());
        s.setSelection(0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PERMISSION_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "User denied screen sharing permission", Toast.LENGTH_SHORT).show();
            return;
        }
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mVirtualDisplay = createVirtualDisplay();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onToggleScreenShare(View view) {
        if (((ToggleButton) view).isChecked()) {
            shareScreen();
        } else {
            stopScreenSharing();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void shareScreen() {
        //Todo: start write ouput file here
        mScreenSharing = true;
        if (mSurface == null) {
            return;
        }
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), PERMISSION_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
    }

    private void stopScreenSharing() {
        //Todo: Save file here
        mScreenSharing = false;
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("ScreenSharingDemo",
                mDisplayWidth, mDisplayHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface, null /*Callbacks*/, null /*Handler*/);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void resizeVirtualDisplay() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.resize(mDisplayWidth, mDisplayHeight, mScreenDensity);
    }
    private class ResolutionSelector implements Spinner.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
            //Todo: chooose resolution here and remvoe this Listener
            Resolution r = (Resolution) parent.getItemAtPosition(pos);

            ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
            if (getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE) {
                mDisplayHeight = r.y;
                mDisplayWidth = r.x;
            } else {
                mDisplayHeight = r.x;
                mDisplayWidth = r.y;
            }
            lp.height = mDisplayHeight;
            lp.width = mDisplayWidth;
            mSurfaceView.setLayoutParams(lp);
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) { /* Ignore */ }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mMediaProjection = null;
            stopScreenSharing();
        }
    }

    private class SurfaceCallbacks implements SurfaceHolder.Callback {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mDisplayWidth = width;
            mDisplayHeight = height;
            resizeVirtualDisplay();
        }
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mSurface = holder.getSurface();
            if (mScreenSharing) {
                shareScreen();
            }
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (!mScreenSharing) {
                stopScreenSharing();
            }
        }
    }
    private static class Resolution {
        int x;
        int y;
        public Resolution(int x, int y) {
            this.x = x;
            this.y = y;
        }
        @Override
        public String toString() {
            return x + "x" + y;
        }
    }
}