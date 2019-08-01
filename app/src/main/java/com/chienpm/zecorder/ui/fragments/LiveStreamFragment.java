package com.chienpm.zecorder.ui.fragments;

import android.app.MediaRouteButton;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.controllers.streaming.StreamProfile;
import com.chienpm.zecorder.ui.utils.MyUtils;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.login.widget.ProfilePictureView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

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
    private static final String PUBLIC_PROFILE = "public_profile";
    private static final String AUTH_TYPE = "rerequest";

    private View mViewRoot;
    ProfilePictureView mProfilePicture;
    TextView mProfileName;
    Button mBtnRequestStream;
    private CallbackManager mCallbackManager;
    private LoginButton mLoginButton;
    String mStreamId ="", mStreamURL ="", mSecureStreamUrl ="";
    private EditText mEdTitle, mEdDescription;
    private FrameLayout mBlockUiFrame;


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
            updateProfileUI(Profile.getCurrentProfile());
        }
        else{
            signIn();
        }

    }

    private void signIn() {
        mLoginButton.performClick();
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

//        mLoginButton.setReadPermissions(Arrays.asList(EMAIL, PUBLISH_VIDEO));
        mLoginButton.setPermissions(Arrays.asList(EMAIL, PUBLIC_PROFILE, PUBLISH_VIDEO));

        mLoginButton.setAuthType(AUTH_TYPE);

        mLoginButton.setFragment(this);

        // If you are using in a fragment, call mLoginButton.setFragment(this);

        // Callback registration

        mLoginButton.registerCallback(mCallbackManager, mLoginResultCallback);

        mProfilePicture = mViewRoot.findViewById(R.id.img_profile_avatar);
        mProfileName = mViewRoot.findViewById(R.id.tv_profile_name);
        mBtnRequestStream = mViewRoot.findViewById(R.id.btn_request_stream);
        mBtnRequestStream.setOnClickListener(mRequestStreamClickListener);
        mEdTitle = mViewRoot.findViewById(R.id.ed_title);
        mEdDescription = mViewRoot.findViewById(R.id.ed_description);
        mBlockUiFrame = mViewRoot.findViewById(R.id.frameBlockUI);

        final TextInputLayout tilTitle = mViewRoot.findViewById(R.id.til_title);

        mEdTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s.toString())) {
                    tilTitle.setError("Please enter a steaming video name!");
                } else {
                    tilTitle.setError("");
                }
            }
        });

        final TextInputLayout tilDescription = mViewRoot.findViewById(R.id.til_description);

        mEdTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s.toString())) {
                    tilDescription.setError("Please enter a steaming video description!");
                } else {
                    tilDescription.setError("");
                }
            }
        });
    }

    private void requestStreamURL(AccessToken token, String title, String description) {
        GraphRequest request = null;
        try {
            request = GraphRequest.newPostRequest(
                    token,
                    "/"+token.getUserId()+"/live_videos",
                    new JSONObject("{\"title\":\""+title+"\",\"description\":\""+description+"\"}"),
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

                            StreamProfile mStreamProfile = new StreamProfile(mStreamId, mStreamURL, mSecureStreamUrl);
                            unlockUI();
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


            toggleAccountProfileInfo(true);
            updateProfileUI(Profile.getCurrentProfile());
//            requestStreamURL(token);
        }

        @Override
        public void onCancel() {
            // App code

            MyUtils.showSnackBarNotification(mViewRoot, "Signed in Canceled!", Snackbar.LENGTH_INDEFINITE);
            toggleAccountProfileInfo(false);

        }

        @Override
        public void onError(FacebookException exception) {
            // App code
            MyUtils.showSnackBarNotification(mViewRoot, "Signed in ERROR: " + exception.getMessage(), Snackbar.LENGTH_LONG);
            toggleAccountProfileInfo(false);
        }
    };

    private void toggleAccountProfileInfo(boolean visible) {
        if(visible) {
            mViewRoot.findViewById(R.id.account_container).setVisibility(View.VISIBLE);
            mLoginButton.setVisibility(View.GONE);
        }
        else{
            mViewRoot.findViewById(R.id.account_container).setVisibility(View.GONE);
            mLoginButton.setVisibility(View.VISIBLE);
        }
    }

    private void updateProfileUI(Profile profile) {
        mProfilePicture.setProfileId(profile.getId());

        mProfileName.setText(profile.getName());
    }

    private final View.OnClickListener mRequestStreamClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(isSignedIn() && !isRequestedStreamURL()){

                if(isValidSettingInput()) {

                    String title = mEdTitle.getText().toString();
                    String description = mEdDescription.getText().toString();

                    blockUI();
                    requestStreamURL(AccessToken.getCurrentAccessToken(), title, description);
                }
            }
            else{
                signIn();
            }
        }
    };

    private void blockUI() {
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        mBlockUiFrame.setVisibility(View.VISIBLE);
    }

    private void unlockUI(){
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        mBlockUiFrame.setVisibility(View.GONE);
    }

    private boolean isValidSettingInput() {
        if(TextUtils.isEmpty(mEdTitle.getText())){
            MyUtils.showSnackBarNotification(mViewRoot, "Please enter a video stream title", Snackbar.LENGTH_LONG);
            mEdTitle.requestFocus();
            return false;
        }
        if(TextUtils.isEmpty(mEdDescription.getText())){
            MyUtils.showSnackBarNotification(mViewRoot, "Please enter a video stream description", Snackbar.LENGTH_LONG);
            mEdDescription.requestFocus();
            return false;
        }
        return true;
    }
}
