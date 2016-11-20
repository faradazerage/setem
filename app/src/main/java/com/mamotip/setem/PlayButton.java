package com.mamotip.setem;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Timer;
import java.io.IOException;
import java.util.TimerTask;


/**
 * Created by faradazerage on 5/2/16.
 */
class PlayButton extends Button {
    boolean mStartPlaying = true;
    MediaPlayer mPlayer = null;
    private String mFileName;

    public void startPlaying() {
        if (mFileName == null) {
            return;
        }

        setBackgroundResource(R.drawable.green_button);
        setText("Tag Found");

        if (mPlayer != null) {
            stopPlaying();
        }

        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e("NFC", "prepare() failed");
        }
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                stopPlaying();
                mStartPlaying = !mStartPlaying;
            }
        }, 5000);
    }

    private void stopPlaying() {
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
            }
            mPlayer.release();
            mPlayer = null;
        }
    }
    public void setFileName(String fName) {
        mFileName = fName;
    }

    public PlayButton(Context ctx) {
        super(ctx);
        setText("Play");
    }

    public PlayButton(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        setText("Play");
    }
}
