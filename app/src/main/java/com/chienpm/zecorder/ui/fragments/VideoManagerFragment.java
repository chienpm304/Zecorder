package com.chienpm.zecorder.ui.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.data.database.VideoDatabase;
import com.chienpm.zecorder.data.entities.Video;

import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VideoManagerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VideoManagerFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    public VideoManagerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment VideoManagerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static VideoManagerFragment newInstance(String param1, String param2) {
        VideoManagerFragment fragment = new VideoManagerFragment();
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

        //Test get Data from db
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Video> mList = VideoDatabase.getInstance(getContext()).getVideoDao().getAllVideo();
                Log.d("chienpm_log", "getAllVideo: ");
                for (Video v: mList
                ) {
                    Log.d("chienpm_log+", "video: "+v.toString());
                }
            }
        }).start();



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_video_manager, container, false);
    }

}
