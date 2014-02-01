package com.bleatware.authmodule;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * AuthModule
 * User: vasuman
 * Date: 2/1/14
 * Time: 3:40 AM
 */
public class SpikeDetect {
    public static final int PLAT_SIZE = 128;
    private static final float LPF_ALPHA = 0.8f;
    private static final byte SPIKE_VALUE = 127;


    public int recordDetect(int secs) {
        final int BUF_SIZE = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        final AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUF_SIZE);
        final int[] spikes = {0};
        final boolean[] isRecording = {true};
        Runnable process = new Runnable() {
            @Override
            public void run() {
                short[] buffer = new short[BUF_SIZE];
                record.read(buffer, 0, BUF_SIZE);
                float avg = toInt(buffer[0]);
                while (isRecording[0]) {
                    float dAvg = 0;
                    for(int i = 0; i < BUF_SIZE; i++) {
                        avg = (LPF_ALPHA * avg + (1 - LPF_ALPHA) * toInt(buffer[i]));
                        dAvg += buffer[i];
                    }
                    dAvg /= BUF_SIZE;
                    if((dAvg - avg) > SPIKE_VALUE) {
                        spikes[0]++;
                    }
                    record.read(buffer, 0, BUF_SIZE);
                }
            }

        };
        record.startRecording();
        try {
            new Thread(process).run();
            Thread.sleep(secs * 1000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        record.stop();
        isRecording[0] = false;
        record.release();
        return spikes[0];
    }

    private int toInt(short b) {
        return b & 0xFFFF;
    }
}
