package com.chienpm.zecorder.ui.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.ui.utils.MyUtils;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {{}@link LiveStreamFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LiveStreamFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LiveStreamFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "chienpm_log";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    private static final String EMAIL = "email";
    private static final String PUBLISH_VIDEO = "publish_video";
//    private static final String USER_POSTS = "user_posts";
    private static final String AUTH_TYPE = "rerequest";

    private View mViewRoot;
    private CallbackManager mCallbackManager;
    private LoginButton mLoginButton;
    String mStreamId ="", mStreamURL ="", mSecureStreamUrl ="";

    public LiveStreamFragment() {
        // Required empty public constructor
    }

    public static LiveStreamFragment newInstance(String param1, String param2) {
        LiveStreamFragment fragment = new LiveStreamFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mViewRoot = inflater.inflate(R.layout.fragment_live_stream, container, false);
        return mViewRoot;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initViews();

    }

    @Override
    public void onStart() {
        super.onStart();
        if(isSignedIn() && !isRequestedStreamURL()){
            requestStreamURL(AccessToken.getCurrentAccessToken());
        }

    }

    private boolean isRequestedStreamURL() {
        return !TextUtils.isEmpty(mStreamId) && !TextUtils.isEmpty(mStreamURL) && !TextUtils.isEmpty(mSecureStreamUrl);
    }

    private boolean isSignedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null && !accessToken.isExpired();
    }

    private void initViews() {
        mCallbackManager = CallbackManager.Factory.create();

        mLoginButton = (LoginButton) mViewRoot.findViewById(R.id.login_button);

        mLoginButton.setReadPermissions(Arrays.asList(EMAIL, PUBLISH_VIDEO));

        mLoginButton.setAuthType(AUTH_TYPE);

        mLoginButton.setFragment(this);

        // If you are using in a fragment, call mLoginButton.setFragment(this);

        // Callback registration

        mLoginButton.registerCallback(mCallbackManager, mLoginResultCallback);

    }

    private void requestStreamURL(AccessToken token) {
        GraphRequest request = null;
        try {
            request = GraphRequest.newPostRequest(
                    token,
                    "/"+token.getUserId()+"/live_videos",
                    new JSONObject("{\"Test streaming\":\"Today's Live Video\",\"description\":\"This is the live video for today.\"}"),
                    new GraphRequest.Callback() {
                        @Override
                        public void onCompleted(GraphResponse response) {
                            // Insert your code here
//                                    Log.d(TAG, "onGraphResponeCompleted: "+response.getRawResponse());
                            try {
                                mStreamId = response.getJSONObject().getString("id");
                                mStreamURL = response.getJSONObject().getString("stream_url");
                                mSecureStreamUrl = response.getJSONObject().getString("secure_stream_url");

                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.d(TAG, "PARSE JSON Response FAILED");
                            }

                            Log.d(TAG, "PARSE JSON Stream_Id:"+ mStreamId);
                            Log.d(TAG, "PARSE JSON Stream_Url:"+ mStreamURL);
                            Log.d(TAG, "PARSE JSON Secure_Stream_Id:"+ mSecureStreamUrl);
                        }
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
        request.executeAsync();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        mListener = null;
    }

    final FacebookCallback<LoginResult> mLoginResultCallback = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            // App code
            AccessToken token = AccessToken.getCurrentAccessToken();
            MyUtils.showSnackBarNotification(mViewRoot, "Signed-in account: " + token.toString(), Snackbar.LENGTH_INDEFINITE);

            requestStreamURL(token);
        }

        @Override
        public void onCancel() {
            // App code
            MyUtils.showSnackBarNotification(mViewRoot, "Signed in Canceled!", Snackbar.LENGTH_INDEFINITE);
        }

        @Override
        public void onError(FacebookException exception) {
            // App code
            MyUtils.showSnackBarNotification(mViewRoot, "Signed in ERROR: " + exception.getMessage(), Snackbar.LENGTH_LONG);
        }
    };
}
