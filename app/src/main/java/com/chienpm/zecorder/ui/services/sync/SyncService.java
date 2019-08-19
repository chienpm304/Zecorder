package com.chienpm.zecorder.ui.services.sync;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.data.database.VideoDatabase;
import com.chienpm.zecorder.data.entities.Video;
import com.chienpm.zecorder.ui.utils.DriveServiceHelper;
import com.chienpm.zecorder.ui.utils.GoogleDriveFileHolder;
import com.chienpm.zecorder.ui.utils.MyUtils;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.File;

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
    public static final String PARAM_GOOGLE_SIGNIN_ACCOUNT = "parame_google_signin_account";

    private DriveServiceHelper mDriveServiceHelper;

    private static final String TAG = "chienpm_controller";

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

                //perform upload
                uploadVideo(video);

                break;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RecordingControllerService: onCreate");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    public void uploadVideo(Video video){
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



    public void downloadVideo(Video video){
        File fileSave = new File(MyUtils.getBaseStorageDirectory(), video.getTitle());

        mDriveServiceHelper.downloadFile(fileSave, video.getCloudPath())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        video.setLocalPath(fileSave.getAbsolutePath());
                        saveVideoToDatabase(video);
                    }

                    private void saveVideoToDatabase(Video mVideo) {
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
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(PARAM_RESULT_VIDEO, video);
        sendBroadcast(intent);
    }



    private String getMasterFolderId() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return prefs.getString(getResources().getString(R.string.pref_drive_folderId), "");
    }

}
