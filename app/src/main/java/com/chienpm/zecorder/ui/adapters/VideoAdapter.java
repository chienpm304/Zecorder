package com.chienpm.zecorder.ui.adapters;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.chienpm.zecorder.R;
import com.chienpm.zecorder.controllers.settings.VideoSetting;
import com.chienpm.zecorder.data.entities.Video;
import com.chienpm.zecorder.ui.utils.FileHelper;
import com.chienpm.zecorder.ui.utils.MyUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.util.ArrayList;

public class VideoAdapter extends ArrayAdapter<Video> {
    static final String TAG = "chienpm_log";
    private final FragmentActivity mFragment;

    ArrayList<Video> mVideos;
    private ArrayList<Integer> mSelectedPositions;
    private boolean mShowAllCheckBox;
    private Object mSync = new Object();

    public VideoAdapter(FragmentActivity videoManagerFragment, ArrayList<Video> videos) {
        super(videoManagerFragment, 0, videos);
        mFragment = videoManagerFragment;
        mVideos = videos;
        mShowAllCheckBox = false;
        mSelectedPositions = new ArrayList<>();
    }

    public int getSelectedMode() {
        int count = getSelectedPositionCount();
        int total = mVideos.size();
        if(total == 0)
            return MyUtils.SELECTED_MODE_EMPTY;
        if(count == 0)
            return MyUtils.SELECTED_MODE_ALL;
        if(count == 1)
            return MyUtils.SELECTED_MODE_SINGLE;
        return MyUtils.SELECTED_MODE_MULTIPLE;
    }


    public void deleteSelectedVideo() {
        final Video[] list = getSelectedVideo();

        FileHelper.getInstance(this).
                deleteVideoFromDatabaseCallable(list)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        FileHelper
                                .getInstance(VideoAdapter.this)
                                .deleteFilesFromStorageCallable(list)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        notifyDataSetChanged();
                                        showAllCheckboxes(false);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "delete Video from storage failed", e);
                                    }
                                });;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "delete Video from database failed", e);
                    }
                });
    }

    @NonNull
    public Video[] getSelectedVideo() {
        final ArrayList<Video> videos = new ArrayList<>();

        for(int i: mSelectedPositions){
            Video video = mVideos.get(i);
            videos.add(video);
            //delete from storage
        }
        ;

        Video[] list = new Video[videos.size()];
        videos.toArray(list);
        return list;
    }

    public void clearSelected() {
        mSelectedPositions.clear();
        notifyDataSetChanged();;
    }

    public void verifyData() {

        for(Video v: mVideos){
            File file = new File(v.getLocalPath());

            if(!file.exists()){
                FileHelper.getInstance(this).deleteVideosFromDatabase(v);
            }
        }
        notifyDataSetChanged();
    }


    public Video getSingleSelectedVideo(){

        if(getSelectedMode() == MyUtils.SELECTED_MODE_SINGLE) {
            return getItem(mSelectedPositions.get(0));// get selected video
        }
        return null;
    }

    public void removeVideo(Video v) {
        try{
            mVideos.remove(v);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    static class ViewHolder {
        protected TextView title;
        protected TextView resolution;
        protected TextView date;
        protected TextView duration;
        protected TextView size;
        protected CheckBox checkBox;
        protected ImageView thumb;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View viewVideoItem = convertView;
        final ViewHolder holder;
        if(viewVideoItem == null) {
            holder = new ViewHolder();

            viewVideoItem = LayoutInflater.from(getContext()).inflate(R.layout.video_list_item, parent, false);

            initViewHolder(viewVideoItem, holder);

            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (int) holder.checkBox.getTag();
                    boolean isChecked = holder.checkBox.isChecked();

                    if(isChecked){
                        mSelectedPositions.add(position);
                    }
                    else{
                        mSelectedPositions.remove((Integer)position);
                    }
                    notifyDataSetChanged();
                    Log.d("chienpm_log", "selected: " + logSelection());
                }
            });
        }
        else{
            holder = (ViewHolder) viewVideoItem.getTag();
        }

        holder.checkBox.setTag(position);

        final Video curVideo = getItem(position);

        if(curVideo != null) {
            String path = curVideo.getLocalPath();

            Glide.with(getContext())
                    .load(path) // or URI/path
                    .into(holder.thumb); //imageview to set thumbnail to

            holder.checkBox.setBackgroundResource(R.color.TRANSPARENT);

            holder.title.setText(curVideo.getTitle());

            String resolution = VideoSetting.getShortResolution(curVideo.getWidth(), curVideo.getHeight());
            holder.resolution.setText(resolution);

            String size = VideoSetting.getFormattedSize(curVideo.getSize());
            holder.size.setText(size);

            String duration = VideoSetting.getFormattedDuration(curVideo.getDuration());
            holder.duration.setText(duration);

            holder.date.setText(curVideo.getFormattedDate("dd/MM/yyyy"));

            if(mShowAllCheckBox){
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setChecked(isSelectedAtPosition(position));
            }
            else {
                holder.checkBox.setVisibility(View.GONE);
            }
        }
        return viewVideoItem;
    }

    private void initViewHolder(View viewVideoItem, ViewHolder holder) {
        holder.title = viewVideoItem.findViewById(R.id.tvTitle);
        holder.date = viewVideoItem.findViewById(R.id.tvDate);
        holder.duration = viewVideoItem.findViewById(R.id.tvDuration);
        holder.size = viewVideoItem.findViewById(R.id.tvSize);
        holder.resolution = viewVideoItem.findViewById(R.id.tvResolution);
        holder.thumb = viewVideoItem.findViewById(R.id.imgThumbnail);
        holder.checkBox = viewVideoItem.findViewById(R.id.cbItemSelected);

        viewVideoItem.setTag(holder);
        viewVideoItem.setTag(R.id.tvTitle, holder.title);
        viewVideoItem.setTag(R.id.tvDate, holder.date);
        viewVideoItem.setTag(R.id.tvDuration, holder.duration);
        viewVideoItem.setTag(R.id.tvSize, holder.size);
        viewVideoItem.setTag(R.id.tvResolution, holder.resolution);
        viewVideoItem.setTag(R.id.imgThumbnail, holder.thumb);
        viewVideoItem.setTag(R.id.cbItemSelected, holder.checkBox);

    }

    public String logSelection() {
        StringBuilder str = new StringBuilder();
        for (int i: mSelectedPositions) {
            str.append(i);
        }
        return str.toString();
    }


    public int getSelectedPositionCount() {
        return mSelectedPositions.size();
    }

    public void toggleSelectionAtPosition(int position) {
        boolean isChecked = isSelectedAtPosition(position);
        if(isChecked){ //uncheck
            mSelectedPositions.remove((Integer)position);
        }
        else{
            mSelectedPositions.add(position);
        }
        notifyDataSetChanged();
    }

    private boolean isSelectedAtPosition(int position) {
        return mSelectedPositions.contains(position);
    }

    public void showAllCheckboxes(boolean b) {
        mShowAllCheckBox = b;
        notifyDataSetChanged();
    }

    public void selectAll(boolean value) {
        mSelectedPositions.clear();
        showAllCheckboxes(value);
        if(value) {
            for (int i = 0; i < mVideos.size(); i++) {
                mSelectedPositions.add(i);
            }
        }
        notifyDataSetChanged();
    }
}
