package com.chienpm.zecorder.ui.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.ui.activities.StreamingActivity;
import com.chienpm.zecorder.ui.utils.FacebookUtils;
import com.chienpm.zecorder.ui.utils.MyUtils;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.login.widget.ProfilePictureView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.takusemba.rtmppublisher.helper.StreamProfile;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import static com.chienpm.zecorder.ui.utils.MyUtils.DEBUG;

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
    private static final String TAG = LiveStreamFragment.class.getSimpleName();

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
    Button mBtnRequestStream, mBtnSignOut;
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
        if(FacebookUtils.getInstance().isSignedIn()){
            updateProfileUI(Profile.getCurrentProfile());
        }
        else{
            toggleAccountProfileInfo(false);
        }

    }

    private boolean isRequestedStreamURL() {
        return !TextUtils.isEmpty(mStreamId) && !TextUtils.isEmpty(mStreamURL) && !TextUtils.isEmpty(mSecureStreamUrl);
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
        mBtnSignOut = mViewRoot.findViewById(R.id.signout_button);
        mBtnSignOut.setOnClickListener(mRequestSignOutListener);
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

    private void requestStreamURL(AccessToken token, final String title, final String description) {
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
                                Log.e(TAG, "PARSE JSON Response FAILED");
                            }

                            if(DEBUG) Log.i(TAG, "PARSE JSON Stream_Id:"+ mStreamId);
                            if(DEBUG) Log.i(TAG, "PARSE JSON Stream_Url:"+ mStreamURL);
                            if(DEBUG) Log.i(TAG, "PARSE JSON Secure_Stream_Id:"+ mSecureStreamUrl);

                            if(TextUtils.isEmpty(mStreamId) || TextUtils.isEmpty(mStreamURL) || TextUtils.isEmpty(mSecureStreamUrl)){
                                MyUtils.showSnackBarNotification(mViewRoot, "Request stream failed. Please try again!", Snackbar.LENGTH_INDEFINITE);
                                return;
                            }

                            StreamProfile mStreamProfile = new StreamProfile(mStreamId, mStreamURL, mSecureStreamUrl);
                            mStreamProfile.setTitle(title);
                            mStreamProfile.setDescription(description);

                            unlockUI();

                            Intent streamIntent = new Intent(getContext(), StreamingActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(MyUtils.STREAM_PROFILE, mStreamProfile);
                            streamIntent.putExtras(bundle);
                            startActivity(streamIntent);


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
        private ProfileTracker mProfileTracker;
        @Override
        public void onSuccess(LoginResult loginResult) {
            if(loginResult.getAccessToken() == null)
                toggleAccountProfileInfo(false);
            else if(Profile.getCurrentProfile() == null){
                mProfileTracker = new ProfileTracker() {
                    @Override
                    protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                        MyUtils.showSnackBarNotification(mViewRoot, "Signed-in account: " + currentProfile.getName(), Snackbar.LENGTH_LONG);
                        toggleAccountProfileInfo(true);
                        //Todo: request profile
                        updateProfileUI(currentProfile);
                        mProfileTracker.stopTracking();
                    }
                };
            }
        }

        @Override
        public void onCancel() {
            // com.chienpm.zecorder.App code

            MyUtils.showSnackBarNotification(mViewRoot, "Signed in Canceled!", Snackbar.LENGTH_LONG);
            toggleAccountProfileInfo(false);

        }

        @Override
        public void onError(FacebookException exception) {
            // com.chienpm.zecorder.App code
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
            if(FacebookUtils.getInstance().isSignedIn() && !isRequestedStreamURL()){

                if(isValidSettingInput()) {

                    String title = mEdTitle.getText().toString();
                    String description = mEdDescription.getText().toString();

                    blockUI();
                    requestStreamURL(AccessToken.getCurrentAccessToken(), title, description);
                }
            }
            else{
                MyUtils.showSnackBarNotification(mViewRoot, "Requested Stream!", Snackbar.LENGTH_LONG);
            }
        }
    };

    private final View.OnClickListener mRequestSignOutListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (AccessToken.getCurrentAccessToken() == null) {
                return; // already logged out
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder
                    .setTitle("Sign out")
                    .setMessage("Are you sure you want to sign out this account?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            blockUI();
                            new GraphRequest(AccessToken.getCurrentAccessToken(), "/me/permissions/", null, HttpMethod.DELETE,
                                    new GraphRequest
                                            .Callback() {
                                        @Override
                                        public void onCompleted(GraphResponse graphResponse) {
                                            LoginManager.getInstance().logOut();
                                            toggleAccountProfileInfo(false);
                                            unlockUI();
                                        }
                                    }).executeAsync();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
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
