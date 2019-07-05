package com.chienpm.zecorder.ui.utils;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

public class UiUtils {
    public static final int PERMISSION_SCREEN_SHARE_DENIED = 9876;


    public static void showSnackBarNotification(View view, String msg, int length) {
        Snackbar.make(view, msg, length).show();
    }

    public static void toast(Context mContext, String msg, int length) {
        Toast.makeText(mContext, msg, length).show();
    }
}
