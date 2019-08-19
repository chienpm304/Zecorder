package com.chienpm.zecorder.ui.utils;

import android.app.Dialog;

import com.google.android.material.textfield.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.chienpm.zecorder.R;
import com.chienpm.zecorder.controllers.settings.VideoSetting;
import com.chienpm.zecorder.data.entities.Video;
import com.chienpm.zecorder.ui.adapters.VideoAdapter;

public class DialogHelper {
    private final VideoAdapter mVideoAdapter;

    private static DialogHelper mInstance = null;

    private DialogHelper(VideoAdapter mVideoAdapter) {
        this.mVideoAdapter = mVideoAdapter;
    }

    public static DialogHelper getInstance(VideoAdapter mAdapter){
        if (mInstance == null && mAdapter != null) {
            synchronized (DialogHelper.class) {
                mInstance = new DialogHelper(mAdapter);
            }
        }
        return mInstance;
    }



    public void showDetailDialog(final Video video) {
        if (video != null) {
            final Dialog dialog = new Dialog(mVideoAdapter.getContext());
            dialog.setContentView(R.layout.layout_video_detail);
            dialog.setTitle("Properties");

            ((TextView) dialog.findViewById(R.id.detail_title)).setText(video.getTitle());
            ((TextView) dialog.findViewById(R.id.detail_size)).setText(VideoSetting.getFormattedSize(video.getSize()) + "\n" + video.getSize() + " bytes");
            ((TextView) dialog.findViewById(R.id.detail_date)).setText(video.getFormattedDate("dd/MM/yyyy hh:mm aa"));
            ((TextView) dialog.findViewById(R.id.detail_path)).setText(video.getLocalPath());
            ((TextView) dialog.findViewById(R.id.detail_resolution)).setText(video.getResolution());
            ((TextView) dialog.findViewById(R.id.detail_duration)).setText(VideoSetting.getFormattedDuration(video.getDuration()));
            ((TextView) dialog.findViewById(R.id.detail_bitrate)).setText(VideoSetting.getFormattedBitrate(video.getBitrate()));
            ((TextView) dialog.findViewById(R.id.detail_fps)).setText(video.getFps() + "");
//            ((TextView) dialog.findViewById(R.id.detail_sync)).setText(video.getSynced() ? "Synced" : "Local only");

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

    public void showRenameDialog(final Video video) {
        if (video != null) {
            final Dialog dialog = new Dialog(mVideoAdapter.getContext());
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
                    if (MyUtils.isValidFilenameSynctax(s.toString())) {
                        tilEditext.setError("A filename cannot contain any of the following charactor: \\/\":*<>| is not n");
                    } else {
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
                    if (!TextUtils.equals(video.getTitle(), newTitle)) {
                        try {
                            FileHelper.getInstance(mVideoAdapter).tryToRenameFile(video, newTitle);
                            mVideoAdapter.notifyDataSetChanged();
                            dialog.dismiss();
                        } catch (Exception e) {
                            e.printStackTrace();
                            tilEditext.setError(e.getMessage());
                        }
                    } else
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
}