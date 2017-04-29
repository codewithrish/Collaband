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
import android.widget.SeekBar;
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
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity   {

    private Button mRecordBtn;
    private TextView mRecordLable;

    private Button sendFileButton, discardFileButton;
    private static String mFileName = null;
    private static final String LOG_TAG = "Record_Log";

    private MediaRecorder mRecorder = null;
    private MediaPlayer   mPlayer[] = new MediaPlayer[1000];
    private MediaPlayer   mP1= null;

    private boolean started = false;

    private int fileIndex = 0;

    private StorageReference mStorage;
    private DatabaseReference mDatabase;

    private ProgressDialog mProgressDialog;

    private File localFile = null;

    private int val = 100;

    static SeekBar sb;
    private long iTime, fTime;

    boolean flag = true;

    Thread updateSeekBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressDialog = new ProgressDialog(this);

        mStorage = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mRecordBtn = (Button) findViewById(R.id.recordBtn);
        sendFileButton = (Button) findViewById(R.id.sendFileButton);
        discardFileButton = (Button) findViewById(R.id.discardFileButton);
        mRecordLable = (TextView) findViewById(R.id.recordLabel);

        //sb = (SeekBar)findViewById(R.id.seekBar1);

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName+="/recorded_audio.3gp";

        mRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(started == false)
                {
                    started = true;
                    mRecordLable.setText("Stop Recording");
                    mRecordBtn.setBackgroundResource(R.drawable.stop_recording);
                    startRecording();
                    mRecordLable.setText("Recording Started .... ");
                }
                else
                {
                    started = false;
                    mRecordLable.setText("Start Recording");
                    mRecordBtn.setBackgroundResource(R.drawable.start_recording);
                    stopRecording();
                    mRecordLable.setText("Recording Stopped.");
                }
            }
        });

        StorageReference delete = mStorage.child("Audio").child("new_audio.3gp");
        delete.delete();
        updateDatabase();

        if(flag)
        {
            mProgressDialog.setMessage("Preparing Studio ... ");
            mProgressDialog.show();
            flag = false;
        }
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

        if(fileIndex != 0) {
            mRecordLable.setText("Wait till the loop starts again...");
            while(mPlayer[0].getCurrentPosition() != mPlayer[0].getDuration());
            mRecordLable.setText("Recording Started, please press button to stop.");
            mRecorder.start();
            iTime= System.currentTimeMillis();

        }
        else
            mRecorder.start();
    }

    private void stopRecording() {
        mP1 = new MediaPlayer();

        if(fileIndex != 0 && mPlayer[0] != null) {
            mRecordLable.setText("Wait till the loop starts again...");
            //while(mPlayer[0].getCurrentPosition() != val); // Calibration Value 290 motorola && 190 for Nexus
            //while(mPlayer[0].getCurrentPosition() != 190); // Calibration Value 290 motorola && 190 for Nexus
            mRecordLable.setText("Recording Stopped, please press button to start again.");
            fTime= System.currentTimeMillis();
            while((fTime - iTime - 300) % mPlayer[0].getDuration() != 0)
            {
                fTime= System.currentTimeMillis();
            }
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;

            try {
                mP1.setDataSource((new File(mFileName)).toString());
                mP1.prepare();
                val = mPlayer[0].getDuration() - mP1.getDuration();
                if(val < 0)
                {
                    val = mPlayer[0].getDuration() + val;
                }
                else if(val > mPlayer[0].getDuration())
                {
                    val = val - mPlayer[0].getDuration();
                }
                Log.i("val", ""+val);
                mP1.start();
                mP1.setLooping(true);
                mP1.start();
                mP1.setLooping(true);
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
            }

            sendFileButton.setVisibility(View.VISIBLE);
            discardFileButton.setVisibility(View.VISIBLE);

            sendFileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mP1.release();
                    mP1 = null;
                    uploadAudio();
                    sendFileButton.setVisibility(View.GONE);
                    discardFileButton.setVisibility(View.GONE);
                }
            });

            discardFileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mP1.release();
                    mP1 = null;
                    sendFileButton.setVisibility(View.GONE);
                    discardFileButton.setVisibility(View.GONE);
                }
            });
        }
        else{
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;

            try {
                mP1.setDataSource((new File(mFileName)).toString());
                mP1.prepare();
                mP1.start();
                mP1.setLooping(true);
                mP1.start();
                mP1.setLooping(true);
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
            }

            sendFileButton.setVisibility(View.VISIBLE);
            discardFileButton.setVisibility(View.VISIBLE);

            sendFileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    mP1.release();
                    mP1 = null;
                    uploadAudio();
                    sendFileButton.setVisibility(View.GONE);
                    discardFileButton.setVisibility(View.GONE);
                }
            });

            discardFileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    mP1.release();
                    mP1 = null;
                    sendFileButton.setVisibility(View.GONE);
                    discardFileButton.setVisibility(View.GONE);
                }
            });
            mDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

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

        if(!flag) {
            mProgressDialog.setMessage("Please wait ... ");
            mProgressDialog.show();
        }
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

                startPlaying(localFile);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Ready To Use! Create Music :)", Toast.LENGTH_SHORT).show();
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

    private void startPlaying(File mMusic) {
        mPlayer[fileIndex] = new MediaPlayer();
        try {
            mPlayer[fileIndex].setDataSource(mMusic.toString());
            mPlayer[fileIndex].prepare();
            Log.i("mPlayer " + fileIndex, ((String.valueOf(mPlayer[fileIndex].getDuration()))));
            if (fileIndex == 0) {
                mPlayer[fileIndex].start();
                mPlayer[fileIndex].setLooping(true);



                /*sb.setMax( mPlayer[0].getDuration());

                sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mPlayer[0].seekTo(seekBar.getProgress());
                    }
                });*/

                updateSeekBar = new Thread(){
                    @Override
                    public void run() {
                        int totalDuration = mPlayer[0].getDuration();
                        int currentPosition = 0;
                        while(currentPosition<totalDuration){
                            try{
                                sleep(50);
                                currentPosition = mPlayer[0].getCurrentPosition();
                                sb.setProgress(currentPosition);
                                double timeRemaining = totalDuration - currentPosition;

                                String min1="",sec1="";
                                int min =  (int)TimeUnit.MILLISECONDS.toMinutes((long) currentPosition);
                                int sec = (int)(TimeUnit.MILLISECONDS.toSeconds((long) currentPosition)- TimeUnit.MILLISECONDS.toMinutes((long) currentPosition)*60);
                                if(min<=9){
                                    min1 = "0"+min;
                                }
                                else{
                                    min1 = min+"";
                                }
                                if(sec<=9){
                                    sec1 = "0"+sec;
                                }
                                else{
                                    sec1 = sec+"";
                                };
                            }
                            catch (Exception ex){
                                ex.printStackTrace();
                            }
                        }

                    }
                };
                updateSeekBar.start();
            }
            else
            {
                while(mPlayer[0].getCurrentPosition() != mPlayer[0].getDuration());
                mPlayer[fileIndex].start();/*
                mPlayer[0].setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Toast.makeText(MainActivity.this, "Completed", Toast.LENGTH_SHORT).show();

                        for (int loopI = 1; loopI < fileIndex; loopI++){
                            mPlayer[loopI].start();
                        }
                    }
                });*/
                mPlayer[fileIndex].setLooping(true);
            }
            fileIndex++;
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer[fileIndex].release();
        mPlayer[fileIndex] = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < fileIndex; i++)
        {
            mPlayer[i].stop();
        }
    }
}
