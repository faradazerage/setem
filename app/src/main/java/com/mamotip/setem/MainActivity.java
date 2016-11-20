package com.mamotip.setem;

import android.nfc.FormatException;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import android.os.Environment;
import android.util.Log;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.content.Intent;
import android.app.PendingIntent;
import android.app.Activity;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.widget.TextView;

import java.io.IOException;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;


public class MainActivity extends Activity {

    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "Setem";
    private TextView mTextView;
    RecordButton mRecordButton;
    private MediaRecorder mRecorder = null;
    private String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";

    PlayButton   mPlayButton;
    private MediaPlayer   mPlayer = null;

    private NfcAdapter nfcAdapter;


    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        private NdefRecord createRecord(String text) {

            //create the message in according with the standard
            String lang = "en";
            byte[] textBytes = text.getBytes();
            byte[] langBytes = null;
            try {
                langBytes = lang.getBytes("US-ASCII");
            } catch (IOException e) {
                e.printStackTrace();
            }

            int langLength = langBytes.length;
            int textLength = textBytes.length;

            byte[] payload = new byte[1 + langLength + textLength];
            payload[0] = (byte) langLength;

            // copy langbytes and textbytes into payload
            System.arraycopy(langBytes, 0, payload, 1, langLength);
            System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

            NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
            return recordNFC;
        }

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            if (ndef.getCachedNdefMessage() == null) {
                // No Message Yet
                try {
                    ndef.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy-hhmmss.SSS");
                String text = "Sound-" + sdf.format(new Date()) + ".3gp";

                NdefRecord[] records = { createRecord(text) };
                NdefMessage message = new NdefMessage(records);

                try {
                    ndef.writeNdefMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (FormatException e) {
                    e.printStackTrace();
                }
                try {
                    ndef.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return text;
            }


            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {

            byte[] payload = record.getPayload();
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            int languageCodeLength = payload[0] & 0063;
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                mRecordButton.setFileName(path + result);
                mPlayButton.setFileName(path + result);

                mRecordButton.setText("Tag Found");
                mRecordButton.setBackgroundResource(R.drawable.green_button);

                File f = new File(path + result);
                if (f.exists()) {
                    mPlayButton.startPlaying();
                }
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mRecordButton = (RecordButton) findViewById(R.id.mRecordButton);
        mRecordButton.SetActivity(MainActivity.this);
        mRecordButton.setText("Scan a Tag");

        mRecordButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mRecordButton.mFileName == null) {
                    return false;
                }

                long diff = 0L;

                final int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        mRecordButton.down = SystemClock.elapsedRealtime();
                        break;
                    case MotionEvent.ACTION_UP:
                        mRecordButton.up = SystemClock.elapsedRealtime();
                        mRecordButton.down = SystemClock.elapsedRealtime();
                        mRecordButton.dirty = false;
                        break;
                    default:
                        break;
                }

                if ( ((SystemClock.elapsedRealtime() - mRecordButton.down) > 3000) && (!mRecordButton.dirty)) {
                    mRecordButton.dirty = true;
                    Log.e("NFC", "Running this bitch!");
                    mRecordButton.setText("Recording");
                    mRecordButton.setBackgroundResource(R.drawable.round_button);
                    mRecordButton.startRecording();

                }
                Log.e("NFC", "Touch!");
                return true;
            }
        });

        //mPlayButton = (PlayButton) findViewById(R.id.mPlayButton);//new PlayButton(this);
        mPlayButton = new PlayButton(this);
        //mTextView = (TextView) findViewById(R.id.mTextView);
        mTextView = new TextView(this);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if ((nfcAdapter != null) && (nfcAdapter.isEnabled())) {
            Toast.makeText(this, "NFC Available!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "NFC Not Available!", Toast.LENGTH_LONG).show();
        }
        handleIntent(getIntent());

        mTextView.setText("No Tags yet!");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupForegroundDispatch(this, nfcAdapter);
    }

    @Override
    public void onPause() {
        stopForegroundDispatch(this, nfcAdapter);
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    public void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            mTextView.setText(tag.getId().toString());
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
                // testing git
            }
        }

    }

    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity, activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);

        adapter.enableForegroundDispatch(activity, pendingIntent, null, null);
    }

    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

}
