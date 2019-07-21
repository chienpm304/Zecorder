package com.chienpm.zecorder.ui.adapters;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.chienpm.zecorder.R;
import com.chienpm.zecorder.controllers.settings.VideoSetting;
import com.chienpm.zecorder.data.database.VideoDatabase;
import com.chienpm.zecorder.data.entities.Video;
import com.chienpm.zecorder.ui.utils.MyUtils;

import java.util.ArrayList;

import static com.serenegiant.utils.UIThreadHelper.runOnUiThread;

public class VideoAdapter extends ArrayAdapter<Video> {

    ArrayList<Video> mVideos;
    private ArrayList<Integer> mSelectedPositions;
    private boolean mShowAllCheckBox;

    public VideoAdapter(FragmentActivity videoManagerFragment, ArrayList<Video> videos) {
        super(videoManagerFragment, 0, videos);
        mVideos = videos;
        mShowAllCheckBox = false;
        mSelectedPositions = new ArrayList<>();
    }

    public int getSelectedMode() {
        int count = getSelectedPositionCount();
        int total = mVideos.size();
        if(total == 0)
            return MyUtils.SELECTED_MODE_EMPTY;
        if(count == 0 || count == total)
            return MyUtils.SELECTED_MODE_ALL;
        if(count == 1)
            return MyUtils.SELECTED_MODE_SINGLE;
        return MyUtils.SELECTED_MODE_MULTIPLE;
    }

    public void deleteSelectedVideo() {
        final ArrayList<Video> deleleVideos = new ArrayList<>();

        for(int i: mSelectedPositions){
            Video video = mVideos.get(i);
            deleleVideos.add(video);
            //delete from storage
        };

        final Video[] list = (Video[]) deleleVideos.toArray();

        //remove from database
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        // and deleting
                        VideoDatabase.getInstance(getContext()).getVideoDao().deleteVideos(list);

                        runOnUiThread(new Runnable() {
                            public void run() {
                                MyUtils.toast(getContext(), "Deleted from database", Toast.LENGTH_LONG);
                            }
                        });
                    }
                });
            }
        });

            //remove from storage
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
        if(viewVideoItem == null){
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

            holder.date.setText(curVideo.getCreateAt());

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
        String str = "";
        for (int i: mSelectedPositions) {
            str+=i;
        }
        return str;
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
        if(value == true) {
//            showAllCheckboxes();
            for (int i = 0; i < mVideos.size(); i++) {
                mSelectedPositions.add(i);
            }
        }
        notifyDataSetChanged();
    }
}
