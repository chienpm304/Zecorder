package com.takusemba.rtmppublisher.helper;

import java.io.Serializable;

public class StreamProfile implements Serializable {
    private String mId, mStreamUrl, mSecureStreamUrl, mTitle, mDescription, mHost, mApp, mPlayPath;
    private int mPort;

    public StreamProfile(String id, String streamURL, String secureStreamUrl) {
        mId = id;
        setStreamUrl(streamURL);
        mSecureStreamUrl = secureStreamUrl;
    }

    public String getId() {
        return mId;
    }

    public void setId(String mStreamId) {
        this.mId = mStreamId;
    }

    public String getStreamUrl() {
        return mStreamUrl;
    }

    public void setStreamUrl(String mStreamUrl) {
        this.mStreamUrl = mStreamUrl;
        updateHostPortPlayPath();
    }

    private void updateHostPortPlayPath() {
        //rtmp://ingest-seo.mixer.com:1935/beam/93296292-g7csbf82z04gxpf0jbtuy9u5x6n2zrta
//        rtmp://live-api-s.facebook.com:80/rtmp/888586108172475?s_bl=1&s_sml=3&s_sw=0&s_vt=api-s&a=AbyhpmGw0kkfAUv4
//        host: rtmp-api.facebook.com
//        port: 80
//        app: rtmp
//        playpath: 10656346080699666?ds=1&s_l=1&a=xTxRpuj_8A2xtHy-
        mHost = "live-api-s.facebook.com";
        mPort = 80;
        mApp = "rtmp";
        mPlayPath = mStreamUrl.substring(mStreamUrl.lastIndexOf('/')+1);
    }

    public String getSecureStreamUrl() {
        return mSecureStreamUrl;
    }

    public void setSecureStreamUrl(String mSecureStreamUrl) {
        this.mSecureStreamUrl = mSecureStreamUrl;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getHost() {
        return mHost;
    }

    public void setHost(String host) {
        mHost = host;
    }

    public String getApp() {
        return mApp;
    }

    public void setApp(String app) {
        mApp = app;
    }

    public String getPlayPath() {
        return mPlayPath;
    }

    public void setPlayPath(String playPath) {
        mPlayPath = playPath;
    }

    public int getPort() {
        return mPort;
    }

    public void setPort(int port) {
        mPort = port;
    }

    @Override
    public String toString() {
        return "StreamProfile{" +
                "mId='" + mId + '\'' +
                ", mStreamUrl='" + mStreamUrl + '\'' +
                ", mSecureStreamUrl='" + mSecureStreamUrl + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mHost='" + mHost + '\'' +
                ", mApp='" + mApp + '\'' +
                ", mPlayPath='" + mPlayPath + '\'' +
                ", mPort=" + mPort +
                '}';
    }
}
