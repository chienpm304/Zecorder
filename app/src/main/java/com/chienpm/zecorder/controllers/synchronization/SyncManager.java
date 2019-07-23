package com.chienpm.zecorder.controllers.synchronization;


/*
  - Recorded:
    + store file in local
    + store video in database: id, localpath, isSync = false, cloudPath (drive's file id) = <null>


  - Upload:
    + request POST method to api and store FILE_ID

    + store file id in appFolderData on Drive: used to download all file uploaded back to local storage when app database was dropped

    + store file id (of drive) to local database: used to check if the video is uploaded or not? Can verify by checking file id in case user delete video on drive
        set db: isSync = true, cloudPath = file_id



  - Download:
    + if has file id uploaded in databse: try to download file with file id
    + if do not has file id -> read data file in appFolderData on Drive to get file id then download file with file id



  - Delete from drive:


  - Delete from local:

  - Sync press:
    + Download: if has local database

    Client ID
        596902902776-1sbvu4rl9gumull21vqj732i4dp8k13h.apps.googleusercontent.com

    Client Secret
        UDYHWh2pvxmJgsRo29l5Fotg

 */

import android.content.Context;
import androidx.annotation.NonNull;

public class SyncManager{

    private final Object mSync = new Object();

    private Context mContext;

    private static SyncManager mInstance = null;

    private SyncManager(@NonNull Context context){
        mContext = context;
    }

    public static SyncManager getInstance(@NonNull Context context){
        if(mInstance == null){
            synchronized (SyncManager.class){
                mInstance = new SyncManager(context);
            }
        }
        return mInstance;
    }

    public void signIn(){

//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestEmail()
//                .build();
//
//
//        if (!GoogleSignIn.hasPermissions(
//                GoogleSignIn.getLastSignedInAccount(mContext),
//                Drive.SCOPE_APPFOLDER)) {
//
//            GoogleSignIn.requestPermissions(
//                    MyExampleActivity.this,
//                    RC_REQUEST_PERMISSION_SUCCESS_CONTINUE_FILE_CREATION,
//                    GoogleSignIn.getLastSignedInAccount(getActivity()),
//                    Drive.SCOPE_APPFOLDER);
//        } else {
//            saveToDriveAppFolder();
//        }
    }

}
