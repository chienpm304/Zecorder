package com.chienpm.zecorder.ui.utils;

import com.facebook.AccessToken;

public class FacebookUtils {
    private static final FacebookUtils ourInstance = new FacebookUtils();

    public static FacebookUtils getInstance() {
        return ourInstance;
    }

    private FacebookUtils() {
    }

    public boolean isSignedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null && !accessToken.isExpired();
    }
}
