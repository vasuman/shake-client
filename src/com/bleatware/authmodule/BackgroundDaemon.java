package com.bleatware.authmodule;

import android.app.Service;
import android.content.Intent;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.*;
import android.widget.Toast;
import com.squareup.seismic.ShakeDetector;

import java.util.ArrayList;
import java.util.List;

/**
 * AuthModule
 * User: vasuman
 * Date: 1/29/14
 * Time: 9:37 PM
 */
public class BackgroundDaemon extends Service implements ShakeDetector.Listener {
    private static final int MIN_LEVEL = -10;
    private static final long POLL_INTERVAL = 5 * 1000;
    public static final int STOP_SERVICE = 0;
    public static final int START_SERVICE = 1;
    public static final int LOOP_SERVICE = 2;
    private static final long WAIT_TIME = 4 * 1000;
    private static final long VIBRATE_TIME = 1500;
    private static final long DONE_INTERVAL = 60 * 1000;
    private WifiManager mWifi;
    private Vibrator mVibrator;
    private NetworkClient client;
    private PowerManager.WakeLock wake;
    private HandlerThread thread;
    private SensorManager mSensor;
    private ShakeDetector shakeDetector;
    private final String PATTERN = "PAYPAL_DEMO_";
    private ServiceHandler mHandler;
    private Bundle data;
    private PowerManager mPower;
    private SoundPool pool;
    private String token;
    private int[] soundIds = new int[11];
    private boolean shake;

    public BackgroundDaemon() {
    }

    @Override
    public void hearShake() {
        shake = true;
    }


    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.what == STOP_SERVICE) {
                stop();
            } else {
                if(msg.what == START_SERVICE) {
                    run();
                }
                boolean flag = handleIntent();
                if(flag) {
                    Message msgD = obtainMessage();
                    msgD.what = LOOP_SERVICE;
                    sendMessageDelayed(msgD, POLL_INTERVAL);
                }
            }
        }
    }

    public boolean isConnectedToCorrectNetwork() {
        WifiInfo info = mWifi.getConnectionInfo();
        if(info.getSSID() != null) {
            return info.getSSID().startsWith(PATTERN);
        }
        return false;
    }

    private static enum State {
        Scanning, Connected, Stopped, Authenticated
    }

    private State state;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void stop() {
        state = State.Stopped;
    }

    private void run() {
        state = State.Scanning;
    }

    protected boolean handleIntent() {
        if(state == State.Stopped) {
            return false;
        }
        if(state == State.Scanning) {
            if(isConnectedToCorrectNetwork()) {
                state = State.Connected;
                return true;
            }
            scanNetworks();
        } else {
            if(!isConnectedToCorrectNetwork()) {
                state = State.Scanning;
                scanNetworks();
                return true;
            }
            if(state == State.Connected) {
                if(tryAuth()) {
                    state = State.Authenticated;
                }
                return true;
            }
            handleResponse(client.poll(token));
        }
        return true;
    }

    private boolean tryAuth() {
        token = client.auth(data.getString("username"), data.getString("password"));
        if(token != null) {
            System.out.println(token);
            return true;
        }
        return false;
    }

    private void handleResponse(NetworkClient.PollResult response) {
        if(response == null) {
            //Some ERROR!!
            Toast.makeText(this, "Error making request", Toast.LENGTH_LONG).show();
            state = State.Connected;
        } else if(response.id == 100) {
            Toast.makeText(this, "Invalid token", Toast.LENGTH_LONG).show();
            state = State.Connected;
        } else if(response.id == 101) {
            Toast.makeText(this, "Name bump OK", Toast.LENGTH_LONG).show();
        } else if(response.id == 103) {
            //PAY somebody
            Toast.makeText(this, "Payment Amt." + response.amount, Toast.LENGTH_LONG).show();
            mVibrator.vibrate(VIBRATE_TIME);
            try {
                Thread.sleep(VIBRATE_TIME);
                //TODO: Read out amount
                readOut((int) response.amount);
                boolean confirm = listenToTaps();
                client.confirmPayment(token, confirm);
                Thread.sleep(DONE_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private void readOut(int amount) {
        try {
            List<Byte> digits = new ArrayList<Byte>();
            while(amount > 0) {
                digits.add((byte) (amount % 10));
                amount /= 10;
            }
            while(digits.size() != 0) {
                int i = digits.remove(digits.size() - 1);
                pool.play(soundIds[i], 1, 1, 1, 0, 1);
                Thread.sleep(1100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean listenToTaps() {
        shake = false;
        shakeDetector.start(mSensor);
        try {
            Thread.sleep(WAIT_TIME);
        } catch (Exception e) {
            e.printStackTrace();
        }
        shakeDetector.stop();
        return shake;
    }


    private void scanNetworks() {
        if(!mWifi.isWifiEnabled()) {
            Toast.makeText(this, "Wifi not enabled", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Scanning networks...", Toast.LENGTH_SHORT).show();
        List<ScanResult> scanResults = mWifi.getScanResults();
        for(ScanResult result: scanResults) {
            if(result.SSID.startsWith(PATTERN)) {
                if(tryConnect(result.SSID)) {
                    state = State.Connected;
                }
            }
        }
    }


    private boolean tryConnect(String SSID) {
        Toast.makeText(this, "Connecting to " + SSID, Toast.LENGTH_SHORT).show();
        for(WifiConfiguration config: mWifi.getConfiguredNetworks()) {
            if(config.SSID.equals(SSID)) {
                return mWifi.enableNetwork(config.networkId, true);
            }
        }
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + SSID + "\"";
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        int id = mWifi.addNetwork(config);
        return mWifi.enableNetwork(id, true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        state = State.Stopped;
        thread = new HandlerThread("SSID_SERVICE");
        thread.start();
        mHandler = new ServiceHandler(thread.getLooper());
        mWifi = (WifiManager) getSystemService(WIFI_SERVICE);
        mSensor = (SensorManager) getSystemService(SENSOR_SERVICE);
        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        mPower = (PowerManager) getSystemService(POWER_SERVICE);
        shakeDetector = new ShakeDetector(this);
        pool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        loadSounds();
        wake = mPower.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AuthModule");
        wake.acquire();
        client = new NetworkClient();
    }

    private void loadSounds() {
        soundIds[0] = pool.load(this, R.raw.s0, 1);
        soundIds[1] = pool.load(this, R.raw.s1, 1);
        soundIds[2] = pool.load(this, R.raw.s2, 1);
        soundIds[3] = pool.load(this, R.raw.s3, 1);
        soundIds[4] = pool.load(this, R.raw.s4, 1);
        soundIds[5] = pool.load(this, R.raw.s5, 1);
        soundIds[6] = pool.load(this, R.raw.s6, 1);
        soundIds[7] = pool.load(this, R.raw.s7, 1);
        soundIds[8] = pool.load(this, R.raw.s8, 1);
        soundIds[9] = pool.load(this, R.raw.s9, 1);
        soundIds[10] = pool.load(this, R.raw.say, 1);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wake.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        data = intent.getExtras();
        return new Messenger(mHandler).getBinder();
    }
}
