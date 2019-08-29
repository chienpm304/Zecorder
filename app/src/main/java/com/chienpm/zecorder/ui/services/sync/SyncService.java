package com.chienpm.zecorder.ui.services.sync;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.data.database.VideoDatabase;
import com.chienpm.zecorder.data.entities.Video;
import com.chienpm.zecorder.ui.activities.MainActivity;
import com.chienpm.zecorder.ui.utils.DriveServiceHelper;
import com.chienpm.zecorder.ui.utils.GoogleDriveFileHolder;
import com.chienpm.zecorder.ui.utils.MyUtils;
import com.chienpm.zecorder.ui.utils.NotificationHelper;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;

import static com.chienpm.zecorder.ui.utils.DriveServiceHelper.getGoogleDriveService;


public class SyncService extends Service {
    public static final String ACTION_DOWNLOAD_DONE = "sync_action_download_done";
    public static final String ACTION_DOWNLOAD_FAILED = "sync_action_download_failed";
    public static final String ACTION_UPLOAD_DONE = "sync_action_upload_done";
    public static final String ACTION_UPLOAD_FAILED = "sync_action_upload_failed";
    public static final String ACTION_START_SERVICE = "sync_action_start_service";

    public static final String ACTION_REQUEST_DOWNLOAD = "sync_action_request_download";
    public static final String ACTION_REQUEST_UPLOAD = "sync_action_request_upload";
    public static final String PARAM_DRIVER_HELPER = "param_driver_helper";
    public static final String PARAM_UPLOAD_VIDEO = "param_upload_video";
    public static final String PARAM_DOWNLOAD_VIDEO = "param_download_video";
    public static final String PARAM_RESULT_VIDEO = "param_result_video";
    public static final String PARAM_GOOGLE_SIGNIN_ACCOUNT = "param_google_signin_account";
    public static final String PARAM_SYNCING_VIDEOS = "param_syncing_videos";
    public static final String ACTION_FROM_NOTIFICATION = "sync_action_from_notification";

    private DriveServiceHelper mDriveServiceHelper;

    private static final String TAG = "chienpm_controller";

    private ArrayList<Video> mSyncingVideos = new ArrayList<>();


    NotificationCompat.Builder mNotiBuilder;

    PendingIntent mPendingIntent;
    private NotificationManager mNotifyManager;
    private int mId = 1;
    public static boolean startedNotification = false;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public SyncService() {
//        super("SyncService IntentServices");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent==null)
            return START_NOT_STICKY;
        String action = intent.getAction();

        if(!TextUtils.isEmpty(action)){
            onHandleIntent(intent);
            return START_NOT_STICKY;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void checkDriveHelper() {
        if(mDriveServiceHelper == null)
            throw new RuntimeException("DriverServiceHelper passed is NULL");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
//        throw new UnsupportedOperationException("Not yet implemented");
    }

    protected void onHandleIntent(@Nullable Intent intent) {
        assert intent != null;
        String action = intent.getAction();
        if(TextUtils.isEmpty(action)) {
            throw new RuntimeException("onHandle a null action");
        }
        Video video;
        assert action != null;
        switch (action){
            case ACTION_START_SERVICE:
                Log.i(TAG, "onHandleIntent: started service, creating drive helper");
                GoogleSignInAccount googleSignInAccount = intent.getParcelableExtra(PARAM_GOOGLE_SIGNIN_ACCOUNT);
                mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getApplicationContext(), googleSignInAccount, getString(R.string.app_name)));
                checkDriveHelper();
                break;

            case ACTION_REQUEST_DOWNLOAD:
                Log.i(TAG, "onHandleIntent: request download");
                checkDriveHelper();
                //get download link
                video = intent.getParcelableExtra(PARAM_DOWNLOAD_VIDEO);
                if(video==null)
                    throw new RuntimeException("handle upload a null video");
                //add syncing video
                addSyncingVideos(video);
                //perform download
                downloadVideo(video);
                break;

            case ACTION_REQUEST_UPLOAD:
                Log.i(TAG, "onHandleIntent: request download");
                checkDriveHelper();
                //get video path to upload
                video = intent.getParcelableExtra(PARAM_UPLOAD_VIDEO);
                if(video==null)
                    throw new RuntimeException("handle upload a null video");
                //add syncing video
                addSyncingVideos(video);
                //perform upload
                uploadVideo(video);

                break;
        }
    }

    private void addSyncingVideos(Video video) {
        mSyncingVideos.add(video);
        updateNotifyIntent();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //setup notification action
        // Create an explicit intent for an Activity in your app
       updateNotifyIntent();

        //create notification channel
        NotificationHelper.getInstance().createNotificationChannel(this);
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //create notification builder
        mNotiBuilder = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle("Synchronize Videos")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(mPendingIntent)
                .setAutoCancel(false);

        //must be call in 5s when onCreate run

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(mId, mNotiBuilder.build());
        }
        //create notification list Ids
        Log.d(TAG, "RecordingControllerService: onCreate");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void notifySyncStarted() {
        mNotiBuilder
                .setContentText("Synchronizing in progress...")
                .setProgress(100, 0, true)
                .setOngoing(true);

        mNotifyManager.notify(mId, mNotiBuilder.build());
        startedNotification = true;
    }

    public void notifySyncCompleted(){

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(MyUtils.ACTION_OPEN_VIDEO_MANAGER_ACTIVITY);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(intent);

        mPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


        mNotiBuilder
                .setContentText("Synchronizing Completed")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setContentIntent(mPendingIntent);

        mNotifyManager.notify(mId, mNotiBuilder.build());
        startedNotification = false;

    }

    private void updateNotifyIntent() {
        Log.i(TAG, "updateNotifyIntent size: "+mSyncingVideos.size());
        //Todo: fixed callback to SyncActivity
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.setAction(MyUtils.ACTION_OPEN_VIDEO_MANAGER_ACTIVITY);
//        resultIntent.setAction(ACTION_FROM_NOTIFICATION);
        resultIntent.putParcelableArrayListExtra(PARAM_SYNCING_VIDEOS, mSyncingVideos);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);

        mPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    public void uploadVideo(final Video video){
        if(!startedNotification) {
            notifySyncStarted();
        }

        mDriveServiceHelper.uploadFile(new File(video.getLocalPath()), "video/mpeg", getMasterFolderId())
                .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                    @Override
                    public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                        Gson gson = new Gson();
                        Log.i(TAG, "onUpload: onSuccess: " + gson.toJson(googleDriveFileHolder));
                        sendBroadcastCallback(ACTION_UPLOAD_DONE, video);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onUpload: onFailure: " + e.getMessage());
                        e.printStackTrace();
                        sendBroadcastCallback(ACTION_UPLOAD_FAILED, video);
                    }
                });

    }



    public void downloadVideo(final Video video){
        if(!startedNotification) {
            notifySyncStarted();
        }

        final File fileSave = new File(MyUtils.getBaseStorageDirectory(), video.getTitle());

        mDriveServiceHelper.downloadFile(fileSave, video.getCloudPath())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        video.setLocalPath(fileSave.getAbsolutePath());
                        saveVideoToDatabase(video);
                    }

                    private void saveVideoToDatabase(final Video mVideo) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if(mVideo !=null){
                                    Log.d(TAG, "onSaveVideo: "+mVideo.toString());
                                    synchronized (mVideo) {
                                        VideoDatabase.getInstance(getApplicationContext()).getVideoDao().insertVideo(mVideo);
                                    }
                                    sendBroadcastCallback(ACTION_DOWNLOAD_DONE, video);
                                }
                            }
                        }).start();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        sendBroadcastCallback(ACTION_DOWNLOAD_FAILED, video);
                    }
                });
        Log.i(TAG, "onClick: downloading: "+video.toString());
    }

    private void sendBroadcastCallback(String action, Video video) {
        try {
            mSyncingVideos.remove(video);
        }catch (Exception e){
            e.printStackTrace();
        }


        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(PARAM_RESULT_VIDEO, video);
        sendBroadcast(intent);
        if(mSyncingVideos.isEmpty()) {
            notifySyncCompleted();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true);
            } else {
                stopSelf();
            }
        }
        else{
            updateNotifyIntent();
        }

    }




    private String getMasterFolderId() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return prefs.getString(getResources().getString(R.string.pref_drive_folderId), "");
    }

}
