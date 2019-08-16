package com.chienpm.zecorder.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.chienpm.zecorder.ui.utils.DriveServiceHelper;
import com.chienpm.zecorder.ui.utils.GoogleDriveFileHolder;
import com.chienpm.zecorder.ui.utils.MyUtils;
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

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.chienpm.zecorder.ui.utils.DriveServiceHelper.getGoogleDriveService;


public class SyncActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;
    private DriveServiceHelper mDriveServiceHelper;
    private static final String TAG = "SyncActivity";
    private Object mSync = new Object();
    private TextView mTvEmpty;
    private ProgressBar mProgressBar;
    private ListView mListViewVideos;
    private SyncVideoAdapter mSyncAdapter;

    private Button mBtnTryAgain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Set<Scope> requiredScopes = new HashSet<>(2);
        requiredScopes.add(new Scope(DriveScopes.DRIVE_FILE));
        requiredScopes.add(new Scope(DriveScopes.DRIVE_APPDATA));

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

        if (account != null && account.getGrantedScopes().containsAll(requiredScopes)) {
            MyUtils.showSnackBarNotification(mTvEmpty,"Signed in as " + account.getEmail(), Snackbar.LENGTH_INDEFINITE);
            mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getApplicationContext(), account, getResources().getString(R.string.app_name)));
            requestData();
        }
        else{
            signIn();
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
                    MyUtils.showSnackBarNotification(mTvEmpty, "You must grant all permission to sync data. Please try again!", Snackbar.LENGTH_INDEFINITE);
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
                        MyUtils.showSnackBarNotification(mTvEmpty,"Signed in as " + googleSignInAccount.getEmail(), Snackbar.LENGTH_INDEFINITE);

                        mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getApplicationContext(), googleSignInAccount, "appName"));

                        requestData();

                        Log.d(TAG, "handleSignInResult: " + mDriveServiceHelper);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Unable to sign in.", e);
                    }
                });
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

    public void uploadVideo(Video video, SyncVideoAdapter.ViewHolder holder){
//        Log.d(TAG, "onClick: uploading: "+video.toString());
//        mSyncAdapter.getView(0, null, null).findViewById(R.id.sync_progressBar).setVisibility(View.GONE);

        String folderId = getMasterFolderId();
        if(TextUtils.isEmpty(folderId)){
            MyUtils.showSnackBarNotification(mTvEmpty, "Folder Id is empty/ try again", Snackbar.LENGTH_LONG);
            return;
        }
        MyUtils.showSnackBarNotification(mTvEmpty, "Uploading "+video.getTitle()+"...", Snackbar.LENGTH_INDEFINITE);
        holder.progress.setVisibility(View.VISIBLE);
        mDriveServiceHelper.uploadFile(new File(video.getLocalPath()), "video/mpeg", getMasterFolderId())
                        .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                            @Override
                            public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                                Gson gson = new Gson();
                                Log.d(TAG, "onUpload: onSuccess: " + gson.toJson(googleDriveFileHolder));
                                MyUtils.showSnackBarNotification(mTvEmpty, "Uploaded video "+video.getTitle(), Snackbar.LENGTH_LONG);
                                holder.progress.setIndeterminate(false);
                                holder.progress.setProgress(100);
                                holder.progress.postInvalidate();
                                holder.sync.setImageDrawable(getDrawable(R.drawable.ic_check));
                                mSyncAdapter.addToUploaded(video);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "onUpload: onFailure: " + e.getMessage());
                                e.printStackTrace();
                                MyUtils.showSnackBarNotification(mTvEmpty, "Failed to upload video "+video.getTitle(), Snackbar.LENGTH_LONG);
                                holder.progress.setIndeterminate(false);
                                holder.progress.setProgress(0);
                                holder.progress.postInvalidate();
                                holder.sync.setImageDrawable(getDrawable(R.drawable.ic_error));
                            }
                        });

    }

    public void downloadVideo(Video video, SyncVideoAdapter.ViewHolder holder){
        String folderId = getMasterFolderId();

        if(TextUtils.isEmpty(folderId)){
            MyUtils.showSnackBarNotification(mTvEmpty, "Folder Id is empty/ try again", Snackbar.LENGTH_LONG);
            return;
        }
        MyUtils.showSnackBarNotification(mTvEmpty, "Downloading "+video.getTitle()+"...", Snackbar.LENGTH_INDEFINITE);
        holder.progress.setVisibility(View.VISIBLE);
        File fileSave = new java.io.File(MyUtils.getBaseStorageDirectory(), video.getTitle());
        mDriveServiceHelper.downloadFile(fileSave, video.getCloudPath())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                holder.progress.setIndeterminate(false);
                                holder.progress.setProgress(100);
                                holder.progress.postInvalidate();
                                holder.sync.setImageDrawable(getDrawable(R.drawable.ic_check));
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
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    MyUtils.showSnackBarNotification(mTvEmpty, "Downloaded video "+video.getTitle(), Snackbar.LENGTH_LONG);
                                                    //remove video in adapter when downloaded
                                                    mSyncAdapter.addToDownloaded(mVideo);
                                                }
                                            });
                                        }
                                    }
                                }).start();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                MyUtils.showSnackBarNotification(mTvEmpty, "Failed to upload video "+video.getTitle(), Snackbar.LENGTH_LONG);
                                holder.progress.setIndeterminate(false);
                                holder.progress.setProgress(0);
                                holder.progress.postInvalidate();
                                holder.sync.setImageDrawable(getDrawable(R.drawable.ic_error));
                            }
                        });
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
                if(mSyncAdapter.isSyncCompleted()) {
                    mSyncAdapter.removeAll();
                    updateUI();
                }
                srl.setRefreshing(false);
            }

        });
    }

    private void fetchVideosFromDrive() {
        String folderId = getMasterFolderId();
        if(TextUtils.isEmpty(folderId)){
            MyUtils.showSnackBarNotification(mTvEmpty, "Folder Id is empty/ try again", Snackbar.LENGTH_LONG);
            return;
        }

          mDriveServiceHelper.queryFiles(folderId)
                .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
                    @Override
                    public void onSuccess(List<GoogleDriveFileHolder> files) {
                        Gson gson = new Gson();
                        Log.d(TAG, "OnViewFolder: onSuccess: " + gson.toJson(files));

                        ArrayList<Video> driveVideos = Video.createTempVideoFromGoogleDriveData(files);

                        mSyncAdapter.setDriveVideos(driveVideos);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "OnViewFolder: onFailure: "+e.getMessage());
                    }
                });

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

//searchFile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDriveServiceHelper == null) {
//                    return;
//                }
//                mDriveServiceHelper.searchFile("textfilename.txt", "text/plain")
//                        .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
//                            @Override
//                            public void onSuccess(List<GoogleDriveFileHolder> googleDriveFileHolders) {
//
//                                Gson gson = new Gson();
//                                Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolders));
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.d(TAG, "onFailure: " + e.getMessage());
//                            }
//                        });
//
//            }
//        });
//
//        searchFolder.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDriveServiceHelper == null) {
//                    Log.d(TAG, "onSearchFolder: mDriveServiceHelper NULL");
//                    return;
//                }
//
//                mDriveServiceHelper.searchFolder("Zecorder")
//                        .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
//                            @Override
//                            public void onSuccess(List<GoogleDriveFileHolder> googleDriveFileHolders) {
//                                Gson gson = new Gson();
//                                Log.d(TAG, "SeachFolder: onSuccess: " + gson.toJson(googleDriveFileHolders));
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.d(TAG, "SeachFolder: onFailure: " + e.getMessage());
//                            }
//                        });
//            }
//        });
//
//        createTextFile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDriveServiceHelper == null) {
//                    Log.d(TAG, "oncreateTextFile: mDriveServiceHelper NULL");
//                    return;
//                }
//                // you can provide  folder id in case you want to save this file inside some folder.
//                // if folder id is null, it will save file to the root
//                mDriveServiceHelper.createTextFile("textfilename.txt", "some text", null)
//                        .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
//                            @Override
//                            public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
//                                Gson gson = new Gson();
//                                Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.d(TAG, "onFailure: " + e.getMessage());
//                            }
//                        });
//            }
//        });
//
//        createFolder.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDriveServiceHelper == null) {
//                    Log.d(TAG, "onCreateFolder: mDriveServiceHelper NULL");
//                    MyUtils.toast(getApplicationContext(), "mDriveServiceHelper NULL!", Toast.LENGTH_LONG);
//                    return;
//                }
//                // you can provide  folder id in case you want to save this file inside some folder.
//                // if folder id is null, it will save file to the root
//                mDriveServiceHelper.createFolderIfNotExist(getString(R.string.app_name), null)
//                        .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
//                            @Override
//                            public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
//                                Gson gson = new Gson();
//                                Log.d(TAG, "onCreateFolder: onSuccess: " + gson.toJson(googleDriveFileHolder));
//                                MyUtils.toast(getApplicationContext(), "Created folder!", Toast.LENGTH_LONG);
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.d(TAG, "onCreateFolder: onFailure: " + e.getMessage());
//                                MyUtils.toast(getApplicationContext(), "Failed to Created folder!", Toast.LENGTH_LONG);
//                            }
//                        });
//            }
//        });
//
//        uploadFile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                AsyncTask.execute(new Runnable() {
//                    @Override
//                    public void run() {
//
//
//                        List<Video> videos = VideoDatabase.getInstance(getApplicationContext()).getVideoDao().getAllVideo();
//
//                        if (mDriveServiceHelper == null || videos.isEmpty()) {
//                            Log.d(TAG, "onUpload: mDriveService NULL or videos is empty ");
//                            return;
//                        }
//
//                        Video video = videos.get(0);
//
//                        Log.d(TAG, "onUpload video: "+video.toString());
//
//                        mDriveServiceHelper.uploadFile(new File(video.getLocalPath()), "video/mpeg", "1P5rarGXLVDs2ITI8ain2h1pqtIKY1u7N")
//                                .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
//                                    @Override
//                                    public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
//                                        Gson gson = new Gson();
//                                        Log.d(TAG, "onUpload: onSuccess: " + gson.toJson(googleDriveFileHolder));
//                                    }
//                                })
//                                .addOnFailureListener(new OnFailureListener() {
//                                    @Override
//                                    public void onFailure(@NonNull Exception e) {
//                                        Log.d(TAG, "onUpload: onFailure: " + e.getMessage());
//                                        e.printStackTrace();
//                                    }
//                                });
//                    }
//                });
//            }
//        });
//
//        downloadFile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDriveServiceHelper == null) {
//                    return;
//                }
//                mDriveServiceHelper.downloadFile(new java.io.File(getApplicationContext().getFilesDir(), "filename.txt"), "google_drive_file_id_here")
//                        .addOnSuccessListener(new OnSuccessListener<Void>() {
//                            @Override
//                            public void onSuccess(Void aVoid) {
//
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//
//                            }
//                        });
//            }
//        });
//
//        viewFileFolder.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDriveServiceHelper == null) {
//                    Log.d(TAG, "OnViewFolder: mDriveServiceHelper NULL");
//                    return;
//                }
//
//                mDriveServiceHelper.queryFiles("1P5rarGXLVDs2ITI8ain2h1pqtIKY1u7N")
//                        .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
//                            @Override
//                            public void onSuccess(List<GoogleDriveFileHolder> googleDriveFileHolders) {
//                                Gson gson = new Gson();
//                                Log.d(TAG, "OnViewFolder: onSuccess: " + gson.toJson(googleDriveFileHolders));
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.d(TAG, "OnViewFolder: onFailure: "+e.getMessage());
//                            }
//                        });
//
//
//            }
//        });

