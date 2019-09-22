package com.amsavarthan.posizione.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.receivers.ManageServiceReceiver;
import com.amsavarthan.posizione.ui.activities.MainActivity;
import com.amsavarthan.posizione.ui.activities.SplashScreen;

public class AccidentDetectionService extends Service implements ShakeListener.OnShakeListener {
    private ShakeListener mShaker;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    public void onCreate() {

        super.onCreate();
        this.mSensorManager = ((SensorManager)getSystemService(Context.SENSOR_SERVICE));
        this.mAccelerometer = this.mSensorManager.getDefaultSensor(1);
        mShaker = new ShakeListener(this);
        mShaker.setOnShakeListener(this);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "accident");
        Intent ii = new Intent(this, SplashScreen.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, ii, 0);

        Intent broadcastIntent = new Intent(this, ManageServiceReceiver.class);
        broadcastIntent.setAction("com.amsavarthan.posizione.accident_detector.STOP");
        PendingIntent turnOffIntent=PendingIntent.getBroadcast(this,1,broadcastIntent,0);

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.drawable.ic_notification);
        mBuilder.setColor(getResources().getColor(R.color.colorAccent));
        mBuilder.setContentTitle("Accident detector Active");
        mBuilder.setContentText("Posizione is running in background");
        mBuilder.setAutoCancel(false);
        mBuilder.setOngoing(true);
        mBuilder.addAction(R.drawable.ic_remove_circle_outline_black_24dp,"Turn off",turnOffIntent);

        startForeground(2,mBuilder.build());

    }

    @Override
    public void onShake() {
        Toast.makeText(AccidentDetectionService.this, "Accident detected!", Toast.LENGTH_LONG).show();
        final Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vib != null) {
            vib.vibrate(500);
        }
        /*Intent i = new Intent();
        i.setClass(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);*/
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
}