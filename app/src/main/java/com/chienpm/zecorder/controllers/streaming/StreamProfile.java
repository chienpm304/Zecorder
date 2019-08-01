package com.chienpm.zecorder.controllers.streaming;

public class StreamProfile {
    private String mStreamId, mStreamUrl, mSecureStreamUrl;

    public StreamProfile(String streamId, String streamURL, String secureStreamUrl) {
        mStreamId = streamId;
        mStreamUrl = streamURL;
        mSecureStreamUrl = secureStreamUrl;
    }

    public String getStreamId() {
        return mStreamId;
    }

    public void setStreamId(String mStreamId) {
        this.mStreamId = mStreamId;
    }

    public String getStreamUrl() {
        return mStreamUrl;
    }

    public void setStreamUrl(String mStreamUrl) {
        this.mStreamUrl = mStreamUrl;
    }

    public String getSecureStreamUrl() {
        return mSecureStreamUrl;
    }

    public void setSecureStreamUrl(String mSecureStreamUrl) {
        this.mSecureStreamUrl = mSecureStreamUrl;
    }
}
