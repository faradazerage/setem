package com.mamotip.setem;

import android.app.Activity;
import android.content.Context;
import android.media.MediaRecorder;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by faradazerage on 5/2/16.
 */
class RecordButton extends Button {
    boolean mStartRecording = true;
    MediaRecorder mRecorder = null;
    public String mFileName;


    private Activity act;
    public long down = 0L;
    public long up = 0L;
    public boolean dirty = false;

    public void startRecording() {
        if (mFileName == null) {
            return;
        }

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e("NFC", "prepare() failed");
        }

        mRecorder.start();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                stopRecording();
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setBackgroundResource(R.drawable.green_button);
                        setText("Tag Found");
                    }
                });
                mStartRecording = !mStartRecording;
            }
        }, 5000);
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    public void setFileName(String fName) {
        mFileName = fName;
    }

    public void SetActivity(Activity activity) {
        act = activity;
    }

    public RecordButton(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        setText("Record");
    }
}
