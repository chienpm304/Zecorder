package com.chienpm.zecorder.ui.utils;

public class MyCameraProfile {
    public static String CAMERA_MODE_FRONT = "Front";
    public static String CAMERA_MODE_BACK = "Back";
    public static String CAMERA_MODE_OFF = "Off";

    public static String POSITION_TOP_LEFT = "Top Left";
    public static String POSITION_TOP_RIGHT = "Top Right";
    public static String POSITION_BOTTOM_LEFT = "Bottom Left";
    public static String POSITION_BOTTOM_RIGHT = "Bottom Right";
    public static String POSITION_CENTER = "Center";

    String mMode; //Front, Back, Off
    String mPosition; //
    String mSize; //Big, Medium, Small

    public MyCameraProfile(String mode, String pos, String size) {
        mMode = mode; mPosition = pos; mSize = size;
    }


    public String getMode() {
        return mMode;
    }

    public String getPosition() {
        return mPosition;
    }

    public String getSize() {
        return mSize;
    }
}
