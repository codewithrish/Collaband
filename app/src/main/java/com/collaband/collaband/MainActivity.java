package com.collaband.collaband;

import android.app.ProgressDialog;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Button mRecordBtn;
    private TextView mRecordLable;

    private static String mFileName = null;
    private static final String LOG_TAG = "Record_Log";

    private MediaRecorder mRecorder = null;
    private MediaPlayer   mPlayer = null;

    private StorageReference mStorage;
    private DatabaseReference mDatabase;

    private ProgressDialog mProgressDialog;

    private File localFile = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressDialog = new ProgressDialog(this);

        mStorage = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mRecordBtn = (Button) findViewById(R.id.recordBtn);
        mRecordLable = (TextView) findViewById(R.id.recordLabel);

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName+="/recorded_audio.3gp";

        mRecordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    startRecording();
                    mRecordLable.setText("Recording Started .... ");
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    stopRecording();
                    mRecordLable.setText("Recording Stopped.");
                }
                return false;
            }
        });
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        
        uploadAudio();
    }

    private void uploadAudio() {

        mProgressDialog.setMessage("Uploading Audio .... ");
        mProgressDialog.show();
        StorageReference filepath = mStorage.child("Audio").child("new_audio.3gp");
        Uri uri = Uri.fromFile(new File(mFileName));
        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                mProgressDialog.dismiss();
                mRecordLable.setText("Uploading Finished");
                updateDatabase();
            }
        });
    }

    private void downloadAudio() {
        mProgressDialog.setMessage("Downloading Audio .... ");
        mProgressDialog.show();

        StorageReference filepath = mStorage.child("Audio").child("new_audio.3gp");

        try {
            localFile = File.createTempFile("audio", "3gp");
        } catch (IOException e) {
            e.printStackTrace();
        }

        filepath.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(MainActivity.this, "File Downloaded", Toast.LENGTH_SHORT).show();
                mProgressDialog.dismiss();


                startPlaying();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "File can't be Downloaded", Toast.LENGTH_SHORT).show();
                mProgressDialog.dismiss();
            }
        });
    }

    private void updateDatabase() {
        int randomNum = 0 + (int)(Math.random() * 200);
        mDatabase.child("hey").setValue(randomNum+"");

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                downloadAudio();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        //
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(localFile.toString());
            mPlayer.prepare();
            mPlayer.start();
            mPlayer.setLooping(true);
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }
}
