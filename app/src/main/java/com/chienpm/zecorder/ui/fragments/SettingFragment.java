package com.chienpm.zecorder.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import com.chienpm.zecorder.R;

public class SettingFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

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
            String summary = sharedPreferences.getString(key,"");
            if(key.equals(getString(R.string.setting_common_countdown)))
                summary += "s";
            else if(key.equals(getString(R.string.setting_video_bitrate)))
                summary += " Kbps";
            preference.setSummary(summary);
        }
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
