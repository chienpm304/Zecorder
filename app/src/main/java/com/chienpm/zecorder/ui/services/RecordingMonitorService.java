package com.chienpm.zecorder.ui.services;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.ui.activities.MainActivity;

public class RecordingMonitorService extends Service {
    private View mViewRoot;
    private WindowManager mWindowManager;

    final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
    );

    private ImageView mImgClose, mImgRec, mImgStarStop, mImgPauseResume, mImgCapture, mImgLive, mImgSetting;

    public RecordingMonitorService() {
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
        mViewRoot = LayoutInflater.from(this).inflate(R.layout.layout_recording, null);



        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mViewRoot, params);


        initializeViews();

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
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);

                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (Xdiff < 20 && Ydiff < 20) {
                            if (isViewCollapsed()) {
                                //When user clicks on the image view of the collapsed layout,
                                //visibility of the collapsed layout will be changed to "View.GONE"
                                //and expanded view will become visible.
                                togleNavigationButton(View.VISIBLE);
                            }
                            else {
                                togleNavigationButton(View.GONE);
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
    }

    private void initializeViews() {
        mImgRec = mViewRoot.findViewById(R.id.imgRec);
        mImgCapture = mViewRoot.findViewById(R.id.imgCapture);
        mImgClose = mViewRoot.findViewById(R.id.imgClose);
        mImgLive = mViewRoot.findViewById(R.id.imgLive);
        mImgPauseResume = mViewRoot.findViewById(R.id.imgPauseResume);
        mImgStarStop = mViewRoot.findViewById(R.id.imgStartStop);
        mImgSetting = mViewRoot.findViewById(R.id.imgSetting);

        togleNavigationButton(View.GONE);

//        mImgRec.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(isViewCollapsed())
//                    togleNavigationButton(View.VISIBLE);
//                else
//                    togleNavigationButton(View.GONE);
//            }
//        });

        mImgCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Capture clicked", Toast.LENGTH_SHORT).show();
            }
        });

        mImgPauseResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Pause/Resume recording!", Toast.LENGTH_SHORT).show();
            }
        });


        mImgSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Setting clicked", Toast.LENGTH_SHORT).show();
            }
        });


        mImgStarStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Start/Stop clicked", Toast.LENGTH_SHORT).show();
            }
        });


        mImgLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Live clicked", Toast.LENGTH_SHORT).show();
            }
        });

        mImgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                stopSelf();
            }
        });



    }

    private boolean isViewCollapsed() {
        return mViewRoot == null || mViewRoot.findViewById(R.id.imgSetting).getVisibility() == View.GONE;
    }

    void togleNavigationButton(int viewMode){
        mImgStarStop.setVisibility(viewMode);
        mImgSetting.setVisibility(viewMode);
        mImgPauseResume.setVisibility(viewMode);
        mImgCapture.setVisibility(viewMode);
        mImgLive.setVisibility(viewMode);
        mImgClose.setVisibility(viewMode);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mViewRoot!=null){
            mWindowManager.removeViewImmediate(mViewRoot);
        }
    }
}
