package com.chienpm.zecorder.ui.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.data.database.VideoDatabase;
import com.chienpm.zecorder.data.entities.Video;
import com.chienpm.zecorder.ui.adapters.SyncVideoAdapter;
import com.chienpm.zecorder.ui.services.sync.SyncService;
import com.chienpm.zecorder.ui.utils.DriveServiceHelper;
import com.chienpm.zecorder.ui.utils.GoogleDriveFileHolder;
import com.chienpm.zecorder.ui.utils.MyUtils;
import com.chienpm.zecorder.ui.utils.NetworkUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.services.drive.DriveScopes;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.chienpm.zecorder.ui.utils.DriveServiceHelper.getGoogleDriveService;
import static com.chienpm.zecorder.ui.utils.MyUtils.isMyServiceRunning;


public class SyncActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;
    private DriveServiceHelper mDriveServiceHelper;
    private static final String TAG = SyncActivity.class.getSimpleName()+"_chienpm";
    private Object mSync = new Object();
    private TextView mTvEmpty;
    private ProgressBar mProgressBar;
    private ListView mListViewVideos;
    private SyncVideoAdapter mSyncAdapter;
    private Button mBtnTryAgain;

    private SyncServiceReceiver mSyncServiceReceiver;
    private ArrayList<Video> mSyncingVideos;
    private NetworkChangeReceiver mNetworkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sync);

        initView();

        handleIntentFromNotification(getIntent());
    }

    private void handleIntentFromNotification(Intent intent) {
        if(intent!=null){
            String action = intent.getAction();
            if(!TextUtils.isEmpty(action)){
                if(action.equals(SyncService.ACTION_FROM_NOTIFICATION)){
                    mSyncingVideos = intent.getParcelableArrayListExtra(SyncService.PARAM_SYNCING_VIDEOS);
                }
                else
                    mSyncingVideos = null;
            }
        }
    }

    private void registerNetworkChangeReceiver() {
        mNetworkChangeReceiver = new NetworkChangeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkChangeReceiver, filter);
    }

    private void registerSyncServiceReceiver() {
        mSyncServiceReceiver = new SyncServiceReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SyncService.ACTION_DOWNLOAD_DONE);
        intentFilter.addAction(SyncService.ACTION_UPLOAD_DONE);
        intentFilter.addAction(SyncService.ACTION_DOWNLOAD_FAILED);
        intentFilter.addAction(SyncService.ACTION_UPLOAD_FAILED);
        intentFilter.addAction(SyncService.ACTION_FATAL_ERROR);
        registerReceiver(mSyncServiceReceiver, intentFilter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!NetworkUtils.isConnected(this)){
            mProgressBar.setVisibility(View.GONE);
            mTvEmpty.setText("Found Network problem. Please check!");
            return;
        }

        checkAuthentication();
    }


    private void checkAuthentication() {
        Set<Scope> requiredScopes = new HashSet<>(2);
        requiredScopes.add(new Scope(DriveScopes.DRIVE_FILE));
        requiredScopes.add(new Scope(DriveScopes.DRIVE_APPDATA));

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

        if (account != null && account.getGrantedScopes().containsAll(requiredScopes)) {
            MyUtils.showSnackBarNotification(mTvEmpty,"Signed in as " + account.getEmail(), Snackbar.LENGTH_LONG);
            mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getApplicationContext(), account, getResources().getString(R.string.app_name)));
            shouldStartSyncService(account);
            requestData();
        }
        else{
            signIn();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mSyncServiceReceiver == null)
            registerSyncServiceReceiver();
        if(mNetworkChangeReceiver == null)
            registerNetworkChangeReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mSyncServiceReceiver!=null) {
            unregisterReceiver(mSyncServiceReceiver);
            mSyncServiceReceiver = null;
        }
        if(mNetworkChangeReceiver != null){
            unregisterReceiver(mNetworkChangeReceiver);
            mNetworkChangeReceiver = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mSyncServiceReceiver!=null) {
            unregisterReceiver(mSyncServiceReceiver);
            mSyncServiceReceiver = null;
        }
        if(mNetworkChangeReceiver != null){
            unregisterReceiver(mNetworkChangeReceiver);
        }
    }

    private void signIn() {
        mGoogleSignInClient = buildGoogleSignInClient();
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestProfile()
                        .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA), new Scope(DriveScopes.DRIVE_FILE))
                        .build();
        return GoogleSignIn.getClient(getApplicationContext(), signInOptions);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                Log.d(TAG, "onActivityResult: "+resultCode);
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    handleSignInResult(resultData);
                }
                else{
                    MyUtils.showSnackBarNotification(mTvEmpty, "You must grant all permission to sync data. Please try again!", Snackbar.LENGTH_LONG);
                    mProgressBar.setVisibility(View.GONE);
                    mBtnTryAgain.setVisibility(View.VISIBLE);
                }
                break;


        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                        MyUtils.showSnackBarNotification(mTvEmpty,"Signed in as " + googleSignInAccount.getEmail(), Snackbar.LENGTH_LONG);

                        mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getApplicationContext(), googleSignInAccount, getString(R.string.app_name)));

                        shouldStartSyncService(googleSignInAccount);

                        requestData();

                        Log.d(TAG, "handleSignInResult: " + mDriveServiceHelper);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Unable to sign in. Try again", e);

                    }
                });
    }

    private void shouldStartSyncService(GoogleSignInAccount googleSignInAccount) {
        if(!isMyServiceRunning(getApplicationContext(), SyncService.class)){
            startSynchronizeService(googleSignInAccount);
        }else{
            MyUtils.showSnackBarNotification(mTvEmpty, "Synchronize Services is running!", Snackbar.LENGTH_SHORT);
        }
    }

    private void startSynchronizeService(GoogleSignInAccount googleSignInAccount){
        Intent syncService = new Intent(SyncActivity.this, SyncService.class);

        syncService.setAction(SyncService.ACTION_START_SERVICE);

        syncService.putExtra(SyncService.PARAM_GOOGLE_SIGNIN_ACCOUNT, googleSignInAccount);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(syncService);
        }
        else{
            startService(syncService);
        }

    }

    private void requestData() {
        String folderId = getMasterFolderId();

        if (TextUtils.isEmpty(folderId)){
            mDriveServiceHelper.createFolderIfNotExist(MyUtils.DRIVE_MASTER_FOLDER, null)
                    .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                        @Override
                        public void onSuccess(GoogleDriveFileHolder folder) {
                            Gson gson = new Gson();
                            Log.d(TAG, "onCreateFolder: onSuccess: " + gson.toJson(folder));
                            MyUtils.toast(getApplicationContext(), "Created folder!", Toast.LENGTH_LONG);
                            writeMasterFolderIdToPref(folder.getId());
                            bindAdapterAndLoadData();

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "onCreateFolder: onFailure: " + e.getMessage());
                            MyUtils.toast(getApplicationContext(), "Failed to Created folder!", Toast.LENGTH_LONG);
                            finish();
                        }
                    });
        }
        else{
            bindAdapterAndLoadData();
        }
    }

    private void bindAdapterAndLoadData() {
        mSyncAdapter = new SyncVideoAdapter(this, new ArrayList<Video>());

        mListViewVideos.setAdapter(mSyncAdapter);

        getSupportLoaderManager().initLoader(0, null, mLoadVideosCallback);
    }

    public void uploadVideo(Video video){
//        Log.d(TAG, "onClick: uploading: "+video.toString());
//        mSyncAdapter.getView(0, null, null).findViewById(R.id.sync_progressBar).setVisibility(View.GONE);

        String folderId = getMasterFolderId();

        if(TextUtils.isEmpty(folderId)){
            MyUtils.showSnackBarNotification(mTvEmpty, "Folder Id is empty/ try again", Snackbar.LENGTH_LONG);
            return;
        }


        MyUtils.showSnackBarNotification(mTvEmpty, "Uploading "+video.getTitle()+"...", Snackbar.LENGTH_SHORT);

        //Start request upload video
        Intent uploader = new Intent(SyncActivity.this, SyncService.class);
        uploader.setAction(SyncService.ACTION_REQUEST_UPLOAD);
        uploader.putExtra(SyncService.PARAM_UPLOAD_VIDEO, video);
        startService(uploader);
        //End request
    }

    public void downloadVideo(Video video){
        String folderId = getMasterFolderId();

        if(TextUtils.isEmpty(folderId)){
            MyUtils.showSnackBarNotification(mTvEmpty, "Folder Id is empty/ try again", Snackbar.LENGTH_LONG);
            return;
        }
        MyUtils.showSnackBarNotification(mTvEmpty, "Downloading "+video.getTitle()+"...", Snackbar.LENGTH_SHORT);

        //Start request download video
        Intent download = new Intent(SyncActivity.this, SyncService.class);
        download.setAction(SyncService.ACTION_REQUEST_DOWNLOAD);
        download.putExtra(SyncService.PARAM_DOWNLOAD_VIDEO, video);
        startService(download);
        //End request

//        mDriveServiceHelper.downloadFile(fileSave, video.getCloudPath())
//                        .addOnSuccessListener(new OnSuccessListener<Void>() {
//                            @Override
//                            public void onSuccess(Void aVoid) {
//                                video.setLocalPath(fileSave.getAbsolutePath());
//                                saveVideoToDatabase(video);
//                            }
//
//                            private void saveVideoToDatabase(Video mVideo) {
//                                new Thread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        if(mVideo !=null){
//                                            Log.d(TAG, "onSaveVideo: "+mVideo.toString());
//                                            synchronized (mVideo) {
//                                                VideoDatabase.getInstance(getApplicationContext()).getVideoDao().insertVideo(mVideo);
//                                            }
//                                            runOnUiThread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    MyUtils.showSnackBarNotification(mTvEmpty, "Downloaded video "+video.getTitle(), Snackbar.LENGTH_SHORT);
//                                                    //remove video in adapter when downloaded
//                                                    mSyncAdapter.addSyncedVideos(mVideo);
//                                                    //todo: update notification
//
//                                                }
//                                            });
//                                        }
//                                    }
//                                }).start();
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                MyUtils.showSnackBarNotification(mTvEmpty, "Failed to download video "+video.getTitle(), Snackbar.LENGTH_LONG);
//                                mSyncAdapter.addFailedVideos(video);
//                            }
//                        });
        Log.d(TAG, "onClick: downloading: "+video.toString());
    }

    public void updateUI(){
        mProgressBar.setVisibility(View.GONE);
        if(mSyncAdapter.getCount()==0){
            mTvEmpty.setText("All videos have already synchronized");
        }
    }

    private String getMasterFolderId() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return prefs.getString(getResources().getString(R.string.pref_drive_folderId), "");
    }

    private void writeMasterFolderIdToPref(String folderId){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getResources().getString(R.string.pref_drive_folderId), folderId);
        editor.apply();
    }



    private void initView() {

        //Views
        mTvEmpty = findViewById(R.id.sync_tvEmpty);
        mListViewVideos = findViewById(R.id.sync_list_videos);
        mListViewVideos.setEmptyView(mTvEmpty);
        mProgressBar = findViewById(R.id.progress_fetching_data);
        mBtnTryAgain = findViewById(R.id.btnTryAgain);

        mBtnTryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    signIn();
                    mBtnTryAgain.setVisibility(View.GONE);
            }
        });

        final SwipeRefreshLayout srl = (SwipeRefreshLayout)findViewById(R.id.sync_swipe_layout);
        srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //todo: handle refresh
                if(mSyncAdapter.isSyncCompleted()) {
                    mSyncAdapter.removedSyncedVideos();
                    updateUI();
                }
                srl.setRefreshing(false);
            }

        });
    }

    private void fetchVideosFromDrive() {
        String folderId = getMasterFolderId();
        if(TextUtils.isEmpty(folderId)){
            MyUtils.showSnackBarNotification(mTvEmpty, "Folder Id is empty, try again", Snackbar.LENGTH_INDEFINITE);
            return;
        }

        mDriveServiceHelper.queryFiles(folderId)
                .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
                    @Override
                    public void onSuccess(List<GoogleDriveFileHolder> files) {
                        Gson gson = new Gson();
                        Log.d(TAG, "OnQueryFileFromDrive: onSuccess: " + gson.toJson(files));

                        ArrayList<Video> driveVideos = Video.createTempVideoFromGoogleDriveData(files);

                        mSyncAdapter.setDriveVideos(driveVideos);

                        if(mSyncingVideos!=null && !mSyncingVideos.isEmpty()) {
                            mSyncAdapter.setSyncingVideos(mSyncingVideos);
                            mSyncingVideos.clear();
                        }
                        updateUI();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "OnQueryFileFromDrive: onFailure: "+e.getMessage());
                        updateUI();
                        MyUtils.showSnackBarNotification(mTvEmpty, "Cannot sync video to drive. Please try later!", Snackbar.LENGTH_INDEFINITE);
                    }
                });

    }

    //Receiver
    private class SyncServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final Video video;
            if(!TextUtils.isEmpty(action)){
                switch (action){
                    case SyncService.ACTION_FATAL_ERROR:
                        String msg = intent.getStringExtra(SyncService.PARAM_ERROR_MSG);
                        if(!TextUtils.isEmpty(msg))
                            MyUtils.showSnackBarNotification(mTvEmpty, msg, Snackbar.LENGTH_INDEFINITE);
                        else
                            MyUtils.showSnackBarNotification(mTvEmpty, "Failed to connect to Google Drive. Try later", Snackbar.LENGTH_INDEFINITE);
                        break;
                    case SyncService.ACTION_UPLOAD_DONE:
                        video = intent.getParcelableExtra(SyncService.PARAM_RESULT_VIDEO);
                        MyUtils.showSnackBarNotification(mTvEmpty, "Uploaded video "+video.getTitle(), Snackbar.LENGTH_LONG);
                        mSyncAdapter.addSyncedVideos(video);
                        break;
                    case SyncService.ACTION_UPLOAD_FAILED:
                        video = intent.getParcelableExtra(SyncService.PARAM_RESULT_VIDEO);
                        MyUtils.showSnackBarNotification(mTvEmpty, "Failed to upload video "+video.getTitle(), Snackbar.LENGTH_LONG);
                        mSyncAdapter.addFailedVideos(video);
                        break;


                    case SyncService.ACTION_DOWNLOAD_DONE:
                        video = intent.getParcelableExtra(SyncService.PARAM_RESULT_VIDEO);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MyUtils.showSnackBarNotification(mTvEmpty, "Downloaded video "+video.getTitle(), Snackbar.LENGTH_LONG);
                                //remove video in adapter when downloaded
                                mSyncAdapter.addSyncedVideos(video);
                            }
                        });
                        break;

                    case SyncService.ACTION_DOWNLOAD_FAILED:
                        video = intent.getParcelableExtra(SyncService.PARAM_RESULT_VIDEO);
                        MyUtils.showSnackBarNotification(mTvEmpty, "Failed to download video "+video.getTitle(), Snackbar.LENGTH_LONG);
                        mSyncAdapter.addFailedVideos(video);
                        break;


                }
            }
        }
    }

    public class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {

            int status = NetworkUtils.getConnectivityStatusString(context);

            Log.e("NetworkReceiver", "Sulod sa network reciever");

            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
                if (status == NetworkUtils.NETWORK_STATUS_NOT_CONNECTED) {
                    MyUtils.showSnackBarNotification(mTvEmpty, "Disconnected from Network. Please check and try again!", Snackbar.LENGTH_INDEFINITE);

                } else{
                    if(mSyncAdapter != null && mSyncAdapter.isEmpty())
                        checkAuthentication();
//                    new ResumeForceExitPause(context).execute();
                }
            }
        }
    }

    //    #Load data to list view
    private LoaderManager.LoaderCallbacks<ArrayList<Video>> mLoadVideosCallback = new LoaderManager.LoaderCallbacks<ArrayList<Video>>() {
        @NonNull
        @Override
        public Loader<ArrayList<Video>> onCreateLoader(int i, @Nullable Bundle bundle) {
            return new VideoAsyncTaskLoader(getApplicationContext());
        }

        @Override
        public void onLoadFinished(@NonNull Loader<ArrayList<Video>> loader, ArrayList<Video> videos) {
            Log.d(TAG, "onLoadFinished: called");
            mSyncAdapter.setLocalVideos(videos);
            fetchVideosFromDrive();
        }

        @Override
        public void onLoaderReset(@NonNull Loader<ArrayList<Video>> loader) {
            Log.d(TAG, "onLoaderReset: called");
            mSyncAdapter.clear();
        }
    };

    static class VideoAsyncTaskLoader extends AsyncTaskLoader<ArrayList<Video>> {

        WeakReference<Context> mWeakReference;

        public VideoAsyncTaskLoader(@NonNull Context context) {
            super(context);
            mWeakReference = new WeakReference<>(context);
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Nullable
        @Override
        public ArrayList<Video> loadInBackground() {
            try {
                ArrayList<Video> localVideos;
                synchronized (this){
                    localVideos = (ArrayList<Video>) VideoDatabase.getInstance(mWeakReference.get()).getVideoDao().getAllVideo();
                }
                return localVideos;
            }catch (Exception e){
                e.printStackTrace();
                Log.d(TAG, "loadInBackground: "+e.getMessage());
                return null;
            }
        }

        @Override
        protected void onReset() {
            super.onReset();
        }
    }
}
