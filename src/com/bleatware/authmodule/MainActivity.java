package com.bleatware.authmodule;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.view.View;
import android.widget.Button;

/**
 * AuthModule
 * User: vasuman
 * Date: 1/29/14
 * Time: 12:51 PM
 */
public class MainActivity extends Activity {

    private Messenger messenger;
    private boolean running = false;
    private boolean bound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messenger = new Messenger(service);
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        Intent serviceIntent = new Intent(this, BackgroundDaemon.class);
        serviceIntent.putExtras(getIntent().getExtras());
        bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);
    }

    public void toggleService(View view) {
        running = !running;
        updateUI();
        Message message = Message.obtain();
        message.what = running ? BackgroundDaemon.START_SERVICE : BackgroundDaemon.STOP_SERVICE;
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void updateUI() {
        Button b = (Button) findViewById(R.id.start_button);
        if(running) {
            b.setText("Stop");
        } else {
            b.setText("Start");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Message message = Message.obtain();
        message.what = BackgroundDaemon.STOP_SERVICE;
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        unbindService(mConnection);
    }
}