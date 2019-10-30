package com.chienpm.zecorder;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.chienpm.zecorder.ui.activities.MainActivity;
import com.chienpm.zecorder.ui.utils.MyUtils;

import java.util.Arrays;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 25) {
            createShortcut();
            if(Build.VERSION.SDK_INT >=26){
                pinShortcut();
            }

        }else{
            removeShortcuts();
        }
    }



    @TargetApi(25)
    private void createShortcut() {
        ShortcutManager sM = getSystemService(ShortcutManager.class);

        Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
        intent1.setAction(MyUtils.ACTION_START_CAPTURE_NOW);

        ShortcutInfo shortcut1 = new ShortcutInfo.Builder(this, "shortcut_capture_now")
                .setIntent(intent1)
                .setShortLabel("Start Now")
                .setLongLabel("Start Service Now")
                .setDisabledMessage("Permission Denied, open app to resolve it")
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .build();

        sM.setDynamicShortcuts(Arrays.asList(shortcut1));

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void pinShortcut() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager.isRequestPinShortcutSupported()) {

            ShortcutInfo pinShortcutInfo = new ShortcutInfo
                    .Builder(getApplicationContext(), "shortcut_capture_now")
                    .build();

            Intent pinnedShortcutCallbackIntent =
                    shortcutManager.createShortcutResultIntent(pinShortcutInfo);

//Get notified when a shortcut is pinned successfully//

            PendingIntent successCallback = PendingIntent.getBroadcast(getApplicationContext(), 0,
                    pinnedShortcutCallbackIntent, 0);
            shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.getIntentSender());
        }
    }

    @TargetApi(25)
    private void removeShortcuts() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        shortcutManager.disableShortcuts(Arrays.asList("shortcut_capture_now"));
        shortcutManager.removeAllDynamicShortcuts();
    }
}
