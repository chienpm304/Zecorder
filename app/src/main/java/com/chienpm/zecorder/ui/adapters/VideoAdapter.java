package com.chienpm.zecorder.ui.adapters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.controllers.settings.VideoSetting;
import com.chienpm.zecorder.data.entities.Video;
import com.chienpm.zecorder.ui.fragments.VideoManagerFragment;

import java.util.ArrayList;

public class VideoAdapter extends ArrayAdapter<Video> {

    ArrayList<Video> mVideos;

    public VideoAdapter(FragmentActivity videoManagerFragment, ArrayList<Video> videos) {
        super(videoManagerFragment, 0, videos);
        mVideos = videos;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View viewVideoItem = convertView;

        if(viewVideoItem == null){
            viewVideoItem = LayoutInflater.from(getContext()).inflate(R.layout.video_list_item, parent, false);
        }

        Video curVideo = getItem(position);
        if(curVideo !=null) {
            ((TextView) viewVideoItem.findViewById(R.id.tvTitle)).setText(curVideo.getTitle());

            String resolution = VideoSetting.getShortResolution(curVideo.getWidth(), curVideo.getHeight());
            ((TextView) viewVideoItem.findViewById(R.id.tvResolution)).setText(resolution);

            String size = VideoSetting.getFormattedSize(curVideo.getSize());
            ((TextView) viewVideoItem.findViewById(R.id.tvSize)).setText(size);

            String duration = VideoSetting.getFormattedDuration(curVideo.getDuration());
            ((TextView) viewVideoItem.findViewById(R.id.tvDuration)).setText(duration);

            ((TextView) viewVideoItem.findViewById(R.id.tvDate)).setText(curVideo.getCreateAt());
        }
        return viewVideoItem;
    }
}
