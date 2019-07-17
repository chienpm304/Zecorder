package com.chienpm.zecorder.controllers.settings;

import android.view.Gravity;

public class CameraSetting {
    public static final String CAMERA_MODE_FRONT = "Front";
    public static final String CAMERA_MODE_BACK = "Back";
    public static final String CAMERA_MODE_OFF = "Off";

    public static final String POSITION_TOP_LEFT = "Top Left";
    public static final String POSITION_TOP_RIGHT = "Top Right";
    public static final String POSITION_BOTTOM_LEFT = "Bottom Left";
    public static final String POSITION_BOTTOM_RIGHT = "Bottom Right";
    public static final String POSITION_CENTER = "Center";

    public static final String SIZE_BIG = "Big";
    public static final String SIZE_MEDIUM = "Medium";
    public static final String SIZE_SMALL = "Small";

    String mMode; //Front, Back, Off
    String mPosition; //
    String mSize; //Big, Medium, Small

    public CameraSetting(String mode, String pos, String size) {
        mMode = mode; mPosition = pos; mSize = size;
    }


    public String getMode() {
        return mMode;
    }

//    public String getPosition() {
//        return mPosition;
//    }

    public String getSize() {
        return mSize;
    }

    public int getParamGravity() {
        switch (mPosition){
            case POSITION_BOTTOM_LEFT: return Gravity.BOTTOM | Gravity.START;
            case POSITION_BOTTOM_RIGHT: return Gravity.BOTTOM | Gravity.END;
            case POSITION_TOP_LEFT: return Gravity.TOP | Gravity.START;
            case POSITION_TOP_RIGHT: return Gravity.TOP | Gravity.END;
            default:return Gravity.CENTER;
        }
    }
}
