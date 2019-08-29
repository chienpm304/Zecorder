package com.chienpm.zecorder.ui.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.chienpm.zecorder.controllers.settings.VideoSetting;
import com.chienpm.zecorder.data.entities.Video;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyUtils {
    public static final boolean DEBUG = true;	// TODO set false on release

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
    public static final String DRIVE_MASTER_FOLDER = "Zecorder";
    public static final String STREAM_PROFILE = "Stream_Profile";
    public static final String ACTION_NOTIFY_FROM_STREAM_SERVICE = "ACTION_NOTIFY_FROM_STREAM_SERVICE";
    public static final String KEY_CAMERA_AVAILABLE = "KEY_CAMERA_AVAILABLE";
    public static final String KEY_CONTROLlER_MODE = "KEY_CONTROLLER_MODE";
    public static final String ACTION_INIT_CONTROLLER = "ACTION INIT CONTROLLER";
    public static final String SAMPLE_RMPT_URL = "rtmp://10.199.220.239/live/test";
    public static final String KEY_STREAM_URL = "rtmp stream";
    public static final String KEY_STREAM_LOG = "Stream log";
    public static final String KEY_STREAM_IS_TESTED = "KEY_STREAM_IS_TESTED";
    public static final String ACTION_UPDATE_STREAM_PROFILE = "ACTION_UPDATE_STREAM_PROFILE";

    private static final String TAG = "chienpm_utils";
    public static final int MODE_STREAMING = 101;
    public static final int MODE_RECORDING = 102;

    @NonNull
    public static String createFileName(@NonNull String ext) {
        return "Zecorder-" +
                Long.toHexString(System.currentTimeMillis()) + ext;
    }

    @NonNull
    public static String getBaseStorageDirectory() {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES) + "/Zecorder";
    }

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

    public static void saveCanvas(Canvas canvas) {

    }

    public static void logBytes(String msg, byte[] bytes) {
        StringBuilder str = new StringBuilder(msg + ": "+bytes.length+"\n");

        if(bytes == null || bytes.length < 1)
            str.append("bytes is null or length < 1");
        else{

            String base64Encoded = Base64.encodeToString(bytes, Base64.DEFAULT);
            str.append("\nbase64: ").append(base64Encoded);//.append("\nbytes: ");
//            for(int i = 0; i < bytes.length; i++){
////                str.append( bytes[i]).append(" ");
//                str.append(String.format("%02x ", bytes[i]));
//            }
//            str.append(getHex(bytes));

        }
        Log.i(TAG, str.toString());
    }

    static final String HEXES = "0123456789ABCDEF";
    public static String getHex( byte [] raw ) {
        if ( raw == null ) {
            return null;
        }
        final StringBuilder hex = new StringBuilder( 2 * raw.length );
        for ( final byte b : raw ) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4))
                    .append(HEXES.charAt((b & 0x0F))).append(' ');
        }
        return hex.toString();
    }

    public static void shootPicture(ByteBuffer buf, int mWidth, int mHeight) {

        Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;

        File captureFile = new File(MyUtils.getBaseStorageDirectory(), MyUtils.createFileName(".jpg"));

        if (!captureFile.getParentFile().exists()) {
            captureFile.getParentFile().mkdirs();
        }

        if (captureFile.toString().endsWith(".jpg")) {
            compressFormat = Bitmap.CompressFormat.JPEG;
        }
        BufferedOutputStream os = null;
        try {
            try {
                os = new BufferedOutputStream(new FileOutputStream(captureFile));
                final Bitmap bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                buf.clear();
                bmp.copyPixelsFromBuffer(buf);
                bmp.compress(compressFormat, 100, os);

                //Log
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream .toByteArray();

                String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
                Log.i(TAG, "shootPicture: "+encoded);
                //endlog

                bmp.recycle();
                os.flush();
            } finally {
                if (os != null) os.close();
            }
        } catch (final FileNotFoundException e) {
            Log.w(TAG, "failed to save file", e);
        } catch (final IOException e) {
            Log.w(TAG, "failed to save file", e);
        }
    }

    public static final String IP_ADDRESS_PATTERN =
            "^rtmp://"+
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])" +
            "/\\S" +
            "/\\S$";
    static String DOMAIN_PATTERN = "^rtmp://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]/[a-zA-Z0-9_.]*[a-zA-Z0-9_.]/[a-zA-Z0-9_.]*[a-zA-Z0-9_.]";

    public static final Pattern ipPattern = Pattern.compile(IP_ADDRESS_PATTERN);
    public static final Pattern domainPattern = Pattern.compile(DOMAIN_PATTERN);

    public static boolean isValidStreamUrlFormat(String url) {
        Matcher matcher = domainPattern.matcher(url);
        return matcher.find();
    }

    public static Video tryToExtractVideoInfoFile(Context context, VideoSetting videoSetting) {
        Video mVideo = null;
        try {

            File file = new File(Uri.parse(videoSetting.getOutputPath()).toString());
            Log.i(TAG, "tryToExtractVideoInfoFile: "+file);
            long size = file.length();
            String title = file.getName();

            String localPath = videoSetting.getOutputPath();
            int bitrate;
            int width = videoSetting.getWidth();
            int height = videoSetting.getHeight();
            int fps = videoSetting.getFPS();
            long duration;

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            //use one of overloaded setDataSource() functions to set your data source
            retriever.setDataSource(file.getAbsolutePath());
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String sBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
//            String sWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
//            String sHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
//            String sFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);

            try {
                bitrate = Integer.parseInt(sBitrate);
            }catch (Exception e){
                bitrate = videoSetting.getBirate();
            }

            try {
                duration = Long.parseLong(time)/1000;
            } catch (Exception e){
                duration = 0;
            }


            mVideo = new Video(title, duration, bitrate, fps, width, height, size, localPath, 0, "", "");

            retriever.release();

            Log.i(TAG, "tryToExtractVideoInfoFile: "+mVideo.toString());

        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "tryToExtractVideoInfoFile: error-"+ e.getMessage());
        }
        return mVideo;
    }

    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
