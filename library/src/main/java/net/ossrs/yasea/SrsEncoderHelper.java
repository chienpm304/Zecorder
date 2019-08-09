package net.ossrs.yasea;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

public class SrsEncoderHelper {
    private static final SrsEncoderHelper ourInstance = new SrsEncoderHelper();

    public static SrsEncoderHelper getInstance() {
        return ourInstance;
    }

    private SrsEncoderHelper() {
    }

}
