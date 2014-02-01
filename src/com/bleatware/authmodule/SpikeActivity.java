package com.bleatware.authmodule;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * AuthModule
 * User: vasuman
 * Date: 2/1/14
 * Time: 4:08 AM
 */
public class SpikeActivity extends Activity {
    private SpikeDetect spikeDetect = new SpikeDetect();
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spike_layout);
    }

    public void sample(View c) {
        TextView t = (TextView) findViewById(R.id.textView);
        t.setText("Spikes: " + spikeDetect.recordDetect(3));
    }
}