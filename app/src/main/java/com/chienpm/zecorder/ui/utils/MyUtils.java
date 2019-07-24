package com.chienpm.zecorder.ui.utils;

import android.content.Context;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;
import android.widget.Toast;

public class MyUtils {
    public static final int RESULT_CODE_FAILED = -999999;
    public static final String SCREEN_CAPTURE_INTENT_RESULT_CODE = "SCREEN_CAPTURE_INTENT_RESULT_CODE";
    public static final String ACTION_OPEN_SETTING_ACTIVITY = "ACTION_OPEN_SETTING_ACTIVITY";
    public static final String ACTION_OPEN_LIVE_ACTIVITY = "ACTION_OPEN_LIVE_ACTIVITY";
    public static final String ACTION_OPEN_VIDEO_MANAGER_ACTIVITY = "ACTION_OPEN_VIDOE_MANAGER_ACTIVITY";
    public static final String ACTION_UPDATE_SETTING = "ACTION_UPDATE_SETTING";
    public static final int SELECTED_MODE_EMPTY = 0;
    public static final int SELECTED_MODE_ALL = 1;
    public static final int SELECTED_MODE_MULTIPLE = 2;
    public static final int SELECTED_MODE_SINGLE = 3;


    public static void showSnackBarNotification(View view, String msg, int length) {
        Snackbar.make(view, msg, length).show();
    }

    public static void toast(Context mContext, String msg, int length) {
        Toast.makeText(mContext, msg, length).show();
    }

    public static boolean isValidFilenameSynctax(String filename) {
        for(int i = 0; i< filename.length(); i++){
            char c = filename.charAt(i);
            if(c == '/' || c =='\\' || c=='"' || c == ':' || c=='*'||c=='<'|| c =='>' || c == '|')
                return true;
        }
        return false;
    }

}
