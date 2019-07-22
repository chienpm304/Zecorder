package com.chienpm.zecorder.ui.adapters;
import android.app.Dialog;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.chienpm.zecorder.R;
import com.chienpm.zecorder.controllers.settings.VideoSetting;
import com.chienpm.zecorder.data.database.VideoDatabase;
import com.chienpm.zecorder.data.entities.Video;
import com.chienpm.zecorder.ui.utils.MyUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.serenegiant.utils.UIThreadHelper.runOnUiThread;

public class VideoAdapter extends ArrayAdapter<Video> {

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
        final ArrayList<Video> videos = new ArrayList<>();

        for(int i: mSelectedPositions){
            Video video = mVideos.get(i);
            videos.add(video);
            //delete from storage
        };

        Video[] list = new Video[videos.size()];
        videos.toArray(list);

        deleteVideosFromDatabase(list);
        deleteFilesFromStorage(list);

    }

    public void deleteVideosFromDatabase(final Video ... videos) {
        if(videos.length > 0) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            // and deleting
                            synchronized (mSync) {
                                VideoDatabase.getInstance(getContext()).getVideoDao().deleteVideos(videos);
                                for(Video v: videos){
                                    mVideos.remove(v);
                                }
                                mSelectedPositions.clear();
                            }
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    showAllCheckboxes(false);

                                }
                            });
                        }
                    });
                }
            });
        }
    }

    private void deleteFilesFromStorage(Video[] videos) {
        for(Video v: videos){
            File file = new File(v.getLocalPath());
            if(file.exists()){
                file.delete();
            }
        }
//        MyUtils.toast(getContext(), "Deleted video from database", Toast.LENGTH_LONG);
    }

    public void clearSelected() {
        mSelectedPositions.clear();
        notifyDataSetChanged();;
    }

    public void verifyData() {

        for(Video v: mVideos){
            File file = new File(v.getLocalPath());

            if(!file.exists()){
                deleteVideosFromDatabase(v);
            }
        }
        notifyDataSetChanged();
    }

    public void showDetailDialog() {
        // custom dialog
        if(getSelectedMode() == MyUtils.SELECTED_MODE_SINGLE){
            Video video = getItem(mSelectedPositions.get(0));// get selected video
            final Dialog dialog = new Dialog(getContext());
            dialog.setContentView(R.layout.layout_video_detail);
            dialog.setTitle("Properties");

            ((TextView)dialog.findViewById(R.id.detail_title)).setText(video.getTitle());
            ((TextView)dialog.findViewById(R.id.detail_size)).setText(VideoSetting.getFormattedSize(video.getSize()) + "\n"+video.getSize()+" bytes");
            ((TextView)dialog.findViewById(R.id.detail_date)).setText(video.getFormattedDate("dd/MM/yyyy hh:mm aa"));
            ((TextView)dialog.findViewById(R.id.detail_path)).setText(video.getLocalPath());
            ((TextView)dialog.findViewById(R.id.detail_resolution)).setText(video.getResolution());
            ((TextView)dialog.findViewById(R.id.detail_duration)).setText(VideoSetting.getFormattedDuration(video.getDuration()));
            ((TextView)dialog.findViewById(R.id.detail_bitrate)).setText(VideoSetting.getFormattedBitrate(video.getBitrate()));
            ((TextView)dialog.findViewById(R.id.detail_fps)).setText(video.getFps()+"");
            ((TextView)dialog.findViewById(R.id.detail_sync)).setText(video.getSynced()?"Synced":"Local only");

            Button dialogButton = (Button) dialog.findViewById(R.id.detail_btn_ok);
            // if button is clicked, close the custom dialog
            dialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();
        }

    }

    public void showRenameDialog() {
        if(getSelectedMode() == MyUtils.SELECTED_MODE_SINGLE){
            final Video video = getItem(mSelectedPositions.get(0));// get selected video

            final Dialog dialog = new Dialog(getContext());
            dialog.setContentView(R.layout.layout_rename);
            dialog.setTitle("Properties");
            final TextInputLayout tilEditext = (TextInputLayout) dialog.findViewById(R.id.tilRename);

            final boolean isValid = true;

            final EditText editText = dialog.findViewById(R.id.edRename);
            editText.setText(video.getNameOnly());
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if(isValidFilenameSynctax(s.toString())) {
                        tilEditext.setError("A filename cannot contain any of the following charactor: \\/\":*<>| is not n");
                    }
                    else {
                        tilEditext.setError("");
                    }
                }
            });
            Button btnOk = (Button) dialog.findViewById(R.id.rename_btn_ok);
            // if button is clicked, close the custom dialog
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newTitle = editText.getText().toString() + ".mp4";
                    if(!TextUtils.equals(video.getTitle(), newTitle)){
//                        MyUtils.toast(getContext(), "I will rename later: "+newTitle, Toast.LENGTH_LONG);
                        try {
                            tryToRenameFile(video, newTitle);
                            dialog.dismiss();
                        } catch (Exception e) {
                            e.printStackTrace();
                            tilEditext.setError(e.getMessage());
                        }
                    }else
                        dialog.dismiss();

                }
            });

            Button btnCancel = (Button) dialog.findViewById(R.id.rename_btn_cancel);
            // if button is clicked, close the custom dialog
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }

    private void tryToRenameFile(final Video video, final String newTitle) throws Exception {
        if(isValidFilenameSynctax(newTitle))
            throw new Exception("A filename cannot contain any of the following charactor: \\/\":*<>| is not n");

        File file = new File(video.getLocalPath());

        final File fileWithNewName = new File(file.getParent(), newTitle);
        if (fileWithNewName.exists()) {
            throw new IOException("This filename is exists. Please choose another name");
        }

        // Rename file (or directory)
        boolean success = file.renameTo(fileWithNewName);

        if (!success) {
            // File was not successfully renamed
            throw new Exception("Cannot rename this file");
        }
        else {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    // and deleting
                    synchronized (mSync) {
                        VideoDatabase.getInstance(getContext()).getVideoDao().updateVideo(video);
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if(mSelectedPositions.size()>0)
                                mVideos.get(mSelectedPositions.indexOf(0)).updateTitle(newTitle, fileWithNewName.getAbsolutePath());
                            showAllCheckboxes(false);
                        }
                    });
                }
            });
        }
    }

    private boolean isValidFilenameSynctax(String filename) {
        for(int i = 0; i< filename.length(); i++){
            char c = filename.charAt(i);
            if(c == '/' || c =='\\' || c=='"' || c == ':' || c=='*'||c=='<'|| c =='>' || c == '|')
                return true;
        }
        return false;
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
