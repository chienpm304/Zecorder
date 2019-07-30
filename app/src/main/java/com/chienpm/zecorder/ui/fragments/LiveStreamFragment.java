package com.chienpm.zecorder.ui.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.ui.utils.MyUtils;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.material.snackbar.Snackbar;

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

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    private static final String EMAIL = "email";
//    private static final String USER_POSTS = "user_posts";
    private static final String AUTH_TYPE = "rerequest";

    private View mViewRoot;
    private CallbackManager mCallbackManager;
    private LoginButton mLoginButton;

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

    private void initViews() {
        mCallbackManager = CallbackManager.Factory.create();

        mLoginButton = (LoginButton) mViewRoot.findViewById(R.id.login_button);

        mLoginButton.setReadPermissions(Arrays.asList(EMAIL));

        mLoginButton.setAuthType(AUTH_TYPE);

        mLoginButton.setFragment(this);

        // If you are using in a fragment, call mLoginButton.setFragment(this);

        // Callback registration
        mLoginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
//                AccessToken.getCurrentAccessToken()
                MyUtils.showSnackBarNotification(mViewRoot, "Signed-in account: ", Snackbar.LENGTH_INDEFINITE);
            }

            @Override
            public void onCancel() {
                // App code
                MyUtils.showSnackBarNotification(mViewRoot, "Signed in Canceled!", Snackbar.LENGTH_INDEFINITE);
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
                MyUtils.showSnackBarNotification(mViewRoot, "Signed in ERROR: "+exception.getMessage(), Snackbar.LENGTH_LONG);
            }
        });

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

}
