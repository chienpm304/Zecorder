package com.chienpm.zecorder.ui.fragments;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.data.database.VideoDatabase;
import com.chienpm.zecorder.data.entities.Video;
import com.chienpm.zecorder.ui.adapters.VideoAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VideoManagerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VideoManagerFragment extends Fragment implements LoaderManager.LoaderCallbacks<ArrayList<Video>> {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "chienpm_log";
    private View mViewRoot;
    ListView mListviewVideos;
    TextView mTvEmpty;
    private VideoAdapter mAdapter;

    // TODO: Rename and change types of parameters

    public VideoManagerFragment() {
        // Required empty public constructor
    }

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

        //Test get Data from db
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                List<Video> mList = VideoDatabase.getInstance(getContext()).getVideoDao().getAllVideo();
//                Log.d("chienpm_log", "getAllVideo: ");
//                for (Video v: mList
//                ) {
//                    Log.d("chienpm_log+", "video: "+v.toString());
//                }
//            }
//        }).start();



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mViewRoot = inflater.inflate(R.layout.fragment_video_manager, container, false);
        return mViewRoot;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initViews();
    }

    private void initViews() {
         mListviewVideos = (ListView) mViewRoot.findViewById(R.id.list_videos);
         mTvEmpty = mViewRoot.findViewById(R.id.tvEmpty);

         // Create textview to set empty state of listview
        mListviewVideos.setEmptyView(mTvEmpty);

        // Create a new {@link ArrayAdapter} of earthquakes: gắn cái datalist vào layout
        mAdapter = new VideoAdapter(
                getActivity(), new ArrayList<Video>());

        // Set the mAdapter on the {@link ListView}
        // so the list can be populated in the user interface
        mListviewVideos.setAdapter(mAdapter);

        mListviewVideos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Video video = mAdapter.getItem(position);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getLocalPath()));
                intent.setDataAndType(Uri.parse(video.getLocalPath()), "video/mp4");
                startActivity(intent);
            }
        });

        Log.d(TAG, "initLoader (activity create) called");
        getLoaderManager().initLoader(0, null, this);

    }

    @NonNull
    @Override
    public Loader<ArrayList<Video>> onCreateLoader(int i, @Nullable Bundle bundle) {
        return new VideoAsyncTaskLoader(getContext());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<Video>> loader, ArrayList<Video> videos) {
        Log.d(TAG, "onLoadFinished: called");
        //clear the adapter of previous earthquake data
        mAdapter.clear();

        // If there is a valid list of {@link Earthquake}s, then add them to the adapter's
        // data set. This will trigger the ListView to notifySettingChanged.
        if(videos != null && !videos.isEmpty()) {
            mAdapter.addAll(videos);
        }
        else{
            mTvEmpty.setText("SUck, no data");
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<Video>> loader) {
        Log.d(TAG, "onLoaderReset: called");
        mAdapter.clear();
    }

    static class VideoAsyncTaskLoader extends AsyncTaskLoader<ArrayList<Video>>{
        WeakReference<Context> mWeakReference;
        public VideoAsyncTaskLoader(@NonNull Context context) {
            super(context);
            mWeakReference = new WeakReference<>(context);
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Nullable
        @Override
        public ArrayList<Video> loadInBackground() {
            try {
                return (ArrayList<Video>) VideoDatabase.getInstance(mWeakReference.get()).getVideoDao().getAllVideo();
            }catch (Exception e){
                e.printStackTrace();
                Log.d(TAG, "loadInBackground: "+e.getMessage());
                return null;
            }
        }

        @Override
        protected void onReset() {
            super.onReset();
        }
    }

}
