package com.collaband.collaband;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

public class Equiliser extends AppCompatActivity {

    MediaPlayer mMediaPlayer;

    Equalizer mEqualizer;

    Visualizer mVisualizer;

    LinearLayout mLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equiliser);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mMediaPlayer = MediaPlayer.create(this, R.raw.burn);
        mMediaPlayer.start();

        mLinearLayout = (LinearLayout) findViewById(R.id.linearLayoutEqual);
        mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());

        mEqualizer = new Equalizer(0,mMediaPlayer.getAudioSessionId());
        mEqualizer.setEnabled(true);

        setupVisualizerFxAndUi();
        //setupEqualizerFxAndUi();

        mVisualizer.setEnabled(true);

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mVisualizer.setEnabled(false);
            }
        });
    }

    private void setupVisualizerFxAndUi() {


        TextView equalizerHeading = new TextView(this);
        equalizerHeading.setText("Equialiser");
        equalizerHeading.setTextSize(20);
        equalizerHeading.setGravity(Gravity.CENTER_HORIZONTAL);
        mLinearLayout.addView(equalizerHeading);

        short numberFrequencyBands = mEqualizer.getNumberOfBands();

        final short lowerEqualizerBandLevel = mEqualizer.getBandLevelRange()[0];
        final short upperEqualizerBandLevel = mEqualizer.getBandLevelRange()[1];

        for (short i = 0 ; i< numberFrequencyBands; i++) {
            final short equiliserBandIndex = i;

            TextView frequencyHeaderTextView = new TextView(this);
            frequencyHeaderTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            frequencyHeaderTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            frequencyHeaderTextView.setText((mEqualizer.getCenterFreq(equiliserBandIndex)/1000)+"Hz");
            mLinearLayout.addView(frequencyHeaderTextView);


            LinearLayout seekBarRowLayout = new LinearLayout(this);
            seekBarRowLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView lowerEqualizerBandLevelTextView = new TextView(this);
            lowerEqualizerBandLevelTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            lowerEqualizerBandLevelTextView.setText((lowerEqualizerBandLevel)/100 + "dB");

            TextView upperEqualizerBandLevelTextView = new TextView(this);
            upperEqualizerBandLevelTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            upperEqualizerBandLevelTextView.setText((lowerEqualizerBandLevel)/100 + "dB");

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT

            );

            layoutParams.weight = 1;

            SeekBar seekbar = new SeekBar(this);

            seekbar.setId(R.id.seek_bar);

            seekbar.setLayoutParams(layoutParams);

            seekbar.setMax(upperEqualizerBandLevel - lowerEqualizerBandLevel);

            seekbar.setProgress(mEqualizer.getBandLevel(equiliserBandIndex));

            seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mEqualizer.setBandLevel(equiliserBandIndex, (short)(progress  + lowerEqualizerBandLevel));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            seekBarRowLayout.addView(lowerEqualizerBandLevelTextView);
            seekBarRowLayout.addView(seekbar);
            seekBarRowLayout.addView(upperEqualizerBandLevelTextView);
            mLinearLayout.addView(seekBarRowLayout);

            equalisedSound();
        }

    }

    private void equalisedSound() {
        ArrayList<String> equilizerPresentNames = new ArrayList<>();
        ArrayAdapter<String> equilizerPresentSpinnerAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item,
                equilizerPresentNames
        );
        equilizerPresentSpinnerAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner equalizerPresntSpinner = (Spinner)findViewById(R.id.spinner);


        for (short i = 0; i< mEqualizer.getNumberOfPresets(); i++) {
            equilizerPresentNames.add(mEqualizer.getPresetName(i));
        }

        equalizerPresntSpinner.setAdapter(equilizerPresentSpinnerAdapter);

        equalizerPresntSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mEqualizer.usePreset(((short)position));

                short numberFrequenyBands = mEqualizer.getNumberOfBands();
                final short lowerEqualizerBandLevel = mEqualizer.getBandLevelRange()[0];

                for(short i = 0; i < numberFrequenyBands; i++)
                {
                    short equalizerBandIndex = i;
                    SeekBar seekBar = (SeekBar) findViewById(R.id.seek_bar);

                    seekBar.setProgress(mEqualizer.getBandLevel(equalizerBandIndex) -  lowerEqualizerBandLevel);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing() && mMediaPlayer != null)
        {
            mVisualizer.release();;
            mEqualizer.release();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}
