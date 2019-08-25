package com.chienpm.zecorder.ui.fragments;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import android.util.Log;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.ui.services.ControllerService;
import com.chienpm.zecorder.ui.services.recording.RecordingControllerService;
import com.chienpm.zecorder.ui.utils.MyUtils;

import java.util.Objects;

public class SettingFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener{
    static final String TAG = "chienpm_log";
    private SharedPreferences mSharedPreferences;
    private PreferenceScreen mPreferenceScreen;

    public SettingFragment() {
        // Required empty public constructor
    }

    public static SettingFragment newInstance(String param1, String param2) {
        SettingFragment fragment = new SettingFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        addPreferencesFromResource(R.xml.setting_preferences);

        mPreferenceScreen = getPreferenceScreen();
        mSharedPreferences = mPreferenceScreen.getSharedPreferences();

        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_camera_mode));
        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_camera_size));
        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_camera_position));

        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_common_countdown));
        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_common_orientation));

        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_video_bitrate));
        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_video_fps));
        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_video_resolution));

        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_audio_source));

    }

    @Override
    public void onResume() {
        super.onResume();
        //unregister the preferenceChange listener
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(sharedPreferences.getString(key, ""));
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            try {
                String summary = sharedPreferences.getString(key, "");
                if (key.equals(getString(R.string.setting_common_countdown)))
                    summary += "s";

                preference.setSummary(summary);
            }
            catch (Exception e){
//                e.printStackTrace();
                Log.e(TAG, "onSharedPreferenceChanged: "+e.getMessage(), e);
            }
        }

        if(isMyServiceRunning(RecordingControllerService.class)) {
            int settingKey = getResources().getIdentifier(key, "string", getActivity().getPackageName());
            if (isCanUpdateSettingImmediately(settingKey)) {
                Log.d(TAG, "onSharedPreferenceChanged: "+key);
                requestUpdateSetting(settingKey);
            }
        }
    }

    private void requestUpdateSetting(int key) {
        Intent intent = new Intent(getActivity(), ControllerService.class);
        intent.setAction(MyUtils.ACTION_UPDATE_SETTING);
        intent.putExtra(MyUtils.ACTION_UPDATE_SETTING, key);
        getActivity().startService(intent);
    }

    private final int[] mList = {
            R.string.setting_camera_mode,
            R.string.setting_camera_size,
            R.string.setting_camera_position
    };


    private boolean isCanUpdateSettingImmediately(int key) {
        for (int s:mList) {
            if(s==key)
                return true;
        }
        return false;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        try {
            ActivityManager manager = (ActivityManager) Objects.requireNonNull(getActivity()).getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        catch (Exception e){
            return false;
        }

        return false;
    }
    @Override
    public void onPause() {
        super.onPause();
        //unregister the preference change listener
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
