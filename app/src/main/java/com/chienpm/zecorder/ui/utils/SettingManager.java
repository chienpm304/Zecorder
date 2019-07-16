package com.chienpm.zecorder.ui.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.chienpm.zecorder.R;

public class SettingManager {

    public static int getSettingCountdownValue(Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = getStringRes(context, R.string.setting_common_countdown);
        String defValue = getStringRes(context, R.string.default_setting_countdown);
        String res = preferences.getString(key, defValue);
        return Integer.parseInt(res);
    }



    private static int getVideoSettingBitrateValue(Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = getStringRes(context, R.string.setting_video_bitrate);
        String defValue = getStringRes(context, R.string.default_setting_bitrate);
        String res = preferences.getString(key, defValue);
        return Integer.parseInt(res);
    }

    private static String getVideoSettingResolutionValue(Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = getStringRes(context, R.string.setting_video_resolution);
        String defValue = getStringRes(context, R.string.default_setting_resolution);

        return preferences.getString(key, defValue);
    }

    private static int getVideoSettingFPSValue(Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = getStringRes(context, R.string.setting_video_fps);
        String defValue = getStringRes(context, R.string.default_setting_fps);
        String res = preferences.getString(key, defValue);
        return Integer.parseInt(res);
    }

    @NonNull
    private static String getStringRes(Context context, int resId) {
        return context.getResources().getString(resId);
    }

    public static VideoProfile getVideoProfile(Context context) {
        VideoProfile videoProfile = null;

        String resolution = getVideoSettingResolutionValue(context);

        switch (resolution){
            case "SD": videoProfile = VideoProfile.VIDEO_PROFILE_SD; break;
            case "HD": videoProfile = VideoProfile.VIDEO_PROFILE_HD; break;
            case "FHD": videoProfile =  VideoProfile.VIDEO_PROFILE_FHD; break;
        }

        int fps = getVideoSettingFPSValue(context);

        int bitrate = getVideoSettingBitrateValue(context);

        if(bitrate != -1) // not auto
            videoProfile.setBitrate(bitrate);

        videoProfile.setFPS(fps);
        Log.d("chienpm", "getVideoProfile: "+videoProfile.toString());
        return videoProfile;
    }
}
