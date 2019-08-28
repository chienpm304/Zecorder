package com.chienpm.zecorder.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.ui.activities.MainActivity;
import com.chienpm.zecorder.ui.services.ControllerService;
import com.chienpm.zecorder.ui.services.recording.RecordingService;
import com.chienpm.zecorder.ui.services.streaming.StreamingService;
import com.chienpm.zecorder.ui.utils.MyUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.takusemba.rtmppublisher.Muxer;
import com.takusemba.rtmppublisher.helper.StreamProfile;

import static com.chienpm.zecorder.ui.services.streaming.StreamingService.NOTIFY_MSG_CONNECTION_DISCONNECTED;
import static com.chienpm.zecorder.ui.services.streaming.StreamingService.NOTIFY_MSG_CONNECTION_FAILED;
import static com.chienpm.zecorder.ui.services.streaming.StreamingService.NOTIFY_MSG_CONNECTION_STARTED;
import static com.chienpm.zecorder.ui.services.streaming.StreamingService.NOTIFY_MSG_ERROR;
import static com.chienpm.zecorder.ui.services.streaming.StreamingService.NOTIFY_MSG_STREAM_STOPPED;
import static com.chienpm.zecorder.ui.utils.MyUtils.DEBUG;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LocalStreamFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LocalStreamFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

    /*
    Spannable spannable = yourText.getText();
    spannable .setSpan(new BackgroundColorSpan(Color.argb(a, r, g, b)), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
    */

public class LocalStreamFragment extends Fragment {
    private static final String TAG = LocalStreamFragment.class.getSimpleName();

    private MainActivity mActivity=null;
    String mUrl;
    private View mViewRoot;
    private EditText mEdUrl, mEdLog;
    private Button mBtnConnect;
    private boolean isTested = false;
    private String mLog;
    private StreamingReceiver mStreamReceiver = null;

    public LocalStreamFragment(){
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(MyUtils.KEY_STREAM_URL, mUrl);
        outState.putString(MyUtils.KEY_STREAM_LOG, mEdLog.getText().toString());
        outState.putBoolean(MyUtils.KEY_STREAM_IS_TESTED, isTested);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState!=null){
            String tmpUrl = savedInstanceState.getString(MyUtils.KEY_STREAM_URL);
            String tmpLog = savedInstanceState.getString(MyUtils.KEY_STREAM_LOG);
            this.isTested = savedInstanceState.getBoolean(MyUtils.KEY_STREAM_IS_TESTED);
            if(isTested){
                mBtnConnect.setText("Connected");
                mBtnConnect.setEnabled(false);
                mEdUrl.setEnabled(false);
            }
            else{
                mBtnConnect.setText("Test");
                mBtnConnect.setEnabled(true);
                mEdUrl.setEnabled(true);
            }
            if(!TextUtils.isEmpty(tmpUrl))
            {
                mEdUrl.setText(tmpUrl);
                mUrl = tmpUrl;
            }
            if(!TextUtils.isEmpty(tmpLog))
            {
                mEdLog.setText(tmpLog);
                mLog = tmpLog;
            }

        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(mStreamReceiver ==null){
            registerSyncServiceReceiver();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mViewRoot = inflater.inflate(R.layout.fragment_local_stream, container, false);
        return mViewRoot;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initViews();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mStreamReceiver ==null){
            registerSyncServiceReceiver();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mStreamReceiver!=null && getActivity()!=null){
            getActivity().unregisterReceiver(mStreamReceiver);
            mStreamReceiver = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mStreamReceiver!=null && getActivity()!=null){
            getActivity().unregisterReceiver(mStreamReceiver);
            mStreamReceiver = null;
        }
    }

    private void initViews() {
        final TextInputLayout tilUrl = mViewRoot.findViewById(R.id.til_url);
        final TextInputLayout tilLog = mViewRoot.findViewById(R.id.til_log);

        mBtnConnect = mViewRoot.findViewById(R.id.btn_connect);
        mBtnConnect.setEnabled(true);
        mEdUrl = mViewRoot.findViewById(R.id.ed_url);
        mEdLog = mViewRoot.findViewById(R.id.ed_log);
        mEdLog.setEnabled(false);
        mEdUrl.setText(MyUtils.SAMPLE_RMPT_URL);

        mBtnConnect.setOnClickListener(mConnectStreamServiceListener);

        mEdUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String tmp = s.toString();
                if (TextUtils.isEmpty(tmp)) {
                    tilUrl.setError("Please enter a Steaming URL!");
                    mBtnConnect.setEnabled(false);
                } else {
                    if(!MyUtils.isValidStreamUrlFormat(tmp)) {
                        tilUrl.setError("Wrong Url format (ex: rtmp://127.0.0.1/live/key)");
                        mBtnConnect.setEnabled(false);
                    }
                    else{
                        tilUrl.setError("");
                        mBtnConnect.setEnabled(true);
                        if(!tmp.equals(mUrl)){
                            isTested = false;
                            mBtnConnect.setText("Test");
                            mBtnConnect.setEnabled(true);
                            mUrl = mEdUrl.getText().toString();
                        }

                    }

                }
            }
        });

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void setContext(MainActivity mainActivity) {
        this.mActivity = mainActivity;
    }

    private void registerSyncServiceReceiver() {
        if(DEBUG) Log.i(TAG, "registerSyncServiceReceiver: registered");
        mStreamReceiver = new StreamingReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyUtils.ACTION_NOTIFY_FROM_STREAM_SERVICE);
        getActivity().registerReceiver(mStreamReceiver, intentFilter);

    }


    //Receiver
    private class StreamingReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if(DEBUG) Log.i(TAG, "onReceive: "+action);
            if(!TextUtils.isEmpty(action) &&
                    MyUtils.ACTION_NOTIFY_FROM_STREAM_SERVICE.equals(action)) {

                String notify_msg = intent.getStringExtra(StreamingService.KEY_NOTIFY_MSG);
                if(TextUtils.isEmpty(notify_msg))
                    return;
//                appendLog(notify_msg);
                switch (notify_msg){
                    case NOTIFY_MSG_CONNECTION_STARTED:
//                            MyUtils.toast(getContext(), "Stream started", Toast.LENGTH_SHORT);
//                            mLog.concat("Streaming started: "+mUrl+"\n");
                        appendLog("Streaming started");
                        appendLog("Streaming ...");

                        break;

                    case NOTIFY_MSG_CONNECTION_FAILED:
//                            MyUtils.toast(getContext(), "Connection to server failed. Please try later", Toast.LENGTH_LONG);
                        appendLog("Connection to server failed");
                        break;

                    case NOTIFY_MSG_CONNECTION_DISCONNECTED:
//                            MyUtils.toast(getContext(), "Connection disconnected", Toast.LENGTH_SHORT);
                        appendLog("Connection disconnected");
                        break;

                    case NOTIFY_MSG_STREAM_STOPPED:
//                            MyUtils.toast(getContext(), "Stream Stopped", Toast.LENGTH_LONG);
                        appendLog("Streaming stopped");

                        isTested = false;
                        mEdUrl.setEnabled(true);
//                        mBtnConnect.setEnabled(true);
//                        mBtnConnect.setText("Test");
//                        m

                        break;

                    case NOTIFY_MSG_ERROR:
//                            MyUtils.toast(getContext(), "Sorry, Error occurs!", Toast.LENGTH_LONG);
                        appendLog("Sorry, an error occurs!");
                        break;
                    default:
                        appendLog(notify_msg);
                }
            }
        }
    }

    private View.OnClickListener mConnectStreamServiceListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mActivity ==null){
                MyUtils.showSnackBarNotification(mViewRoot, "Streaming is detached form application. Try later", Snackbar.LENGTH_LONG);
                Log.e(TAG, "onBtnConnect clicked", new RuntimeException("Activity is null") );
                return;
            }
            if(v.getId() != R.id.btn_connect)
                return;
            mActivity.mMode = MyUtils.MODE_STREAMING;
            mUrl = mEdUrl.getText().toString();
            if(!MyUtils.isValidStreamUrlFormat(mUrl)) {
                MyUtils.showSnackBarNotification(mViewRoot, "Wrong stream url format (ex: rtmp://127.192.123.1/live/stream)", Snackbar.LENGTH_INDEFINITE);
                mEdUrl.requestFocus();
            }
            else{
                if(mActivity.isMyServiceRunning(RecordingService.class))
                {
                    MyUtils.showSnackBarNotification(mViewRoot, "You are in RECORDING Mode. Please close Recording controller", Snackbar.LENGTH_INDEFINITE);
                    return;
                }


                if(isTested){
                    mActivity.mMode = MyUtils.MODE_STREAMING;
                    StreamProfile mStreamProfile = new StreamProfile("", mEdUrl.getText().toString(), "");
                    mActivity.setStreamProfile(mStreamProfile);

                    if(mActivity.isMyServiceRunning(ControllerService.class)){
                        MyUtils.showSnackBarNotification(mViewRoot,"Streaming service is running!", Snackbar.LENGTH_LONG);
                        mActivity.notifyUpdateStreamProfile();
//                        return;
                    }
                    else
                        mActivity.shouldStartControllerService();

                    mBtnConnect.setText("Connected");
                    mBtnConnect.setEnabled(false);
                    mEdUrl.setEnabled(false);
                    appendLog("Stream connected");
                }

                else{
                    mBtnConnect.setText("Testing");
                    appendLog("Test stream "+mUrl);
                    if(testStreamUrlConnection(mUrl)){
                        isTested = true;
                        mBtnConnect.setText("Connect");
                        appendLog("Tested stream: SUCCESS");
                        MyUtils.showSnackBarNotification(mViewRoot, "Tested: URL SUCCEED", Snackbar.LENGTH_LONG);

                    }
                    else{
                        isTested = false;
                        mBtnConnect.setText("Test");
                        appendLog("Tested stream: FAILED");
                        MyUtils.showSnackBarNotification(mViewRoot, "Tested: URL FAILED", Snackbar.LENGTH_LONG);
                    }
                }
            }
        }
    };

    private void appendLog(String msg) {
        mEdLog.append("\n"+msg);
    }

    private boolean testStreamUrlConnection(String url) {
        int t = 0;
        Muxer muxer = new Muxer();
        muxer.open(url, 1280, 720);
        while (!muxer.isConnected()){
            try {
                t+=100;
                Thread.sleep(100);
                if(t>5000)
                    break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(muxer.isConnected()) {
            if(DEBUG) Log.i(TAG, "test Streaming: connected");
            muxer.close();
            return true;
        }
        else{
            if(DEBUG) Log.i(TAG, "test Streaming: failed coz muxer is not connected");
            muxer.close();
            return false;
        }
    }

}
