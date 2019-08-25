package com.amsavarthan.posizione.services;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.amsavarthan.posizione.models.User;
import com.amsavarthan.posizione.receivers.ManageServiceReceiver;
import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.room.user.UserDatabase;
import com.amsavarthan.posizione.room.user.UserEntity;
import com.amsavarthan.posizione.ui.activities.SplashScreen;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.amsavarthan.posizione.utils.Utils;
import com.github.abara.library.batterystats.BatteryStats;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class LocationService extends Service implements LocationListener {

    boolean isGPSEnable = false;
    double latitude,longitude;
    LocationManager locationManager;
    Location location;
    private Handler mUpdateHandler = new Handler();
    private Handler mLocationHandler = new Handler();
    long latency = 3000;
    public static String str_receiver = "location.receiver";
    Intent intent;
    String status;
    int speed;
    private boolean showNotification;
    private NotificationManager mNotificationManager;
    private UserDatabase userDatabase;
    private String bestProvider;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        showNotification = intent.getBooleanExtra("notification",false);
        showNotification();
        return START_STICKY;
    }

    public LocationService() {

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        userDatabase=UserDatabase.getInstance(this);
        intent = new Intent(str_receiver);
        fn_getlocation();
        showNotification();

    }

    private void showNotification() {

        if(showNotification){

            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    final String location=userDatabase.userDao().getUserById(1).getLocation();
                    AppExecutors.getInstance().mainThread().execute(new Runnable() {
                        @Override
                        public void run() {

                            String[] seperated=location.split("/");
                            if(location.equals("")) {
                                displayNotification(null, "");
                            }else{

                                Location loc=new Location(bestProvider);
                                if(seperated.length==5) {
                                    loc.setLatitude(Double.valueOf(seperated[0]));
                                    loc.setLongitude(Double.valueOf(seperated[1]));
                                    loc.setAltitude(Double.valueOf(seperated[2]));
                                    loc.setSpeed(Float.valueOf(seperated[3]));
                                    displayNotification(loc,seperated[4]);
                                    return;
                                }
                                displayNotification(null, "");

                            }

                        }
                    });
                }
            });


        }

    }

    @Override
    public void onLocationChanged(final Location location) {

        mLocationHandler.removeCallbacksAndMessages(null);
        mUpdateHandler.removeCallbacksAndMessages(null);
        //from mps to kmph
        speed = (int) (location.getSpeed() * (18 / 5));

        Log.i("location","latency: "+latency);

        status = getStatus(speed);
        displayNotification(location,status);

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                UserEntity userEntity=userDatabase.userDao().getUserById(1);
                userEntity.setLocation(String.format("%s/%s/%s/%s/%s/%s", location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getSpeed(), status,System.currentTimeMillis()));
                userDatabase.userDao().updateUser(userEntity);
            }
        });

        mUpdateHandler.postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {

                displayNotification(location,getStatus((int)(location.getSpeed()*(18/5))));
                fn_update(location,getStatus((int)(location.getSpeed()*(18/5))));

            }
        },3000);

        //If there is no location change for 10 sec update status as IDLE
        mLocationHandler.postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {

                Log.i("location","no movement detected");
                latency=3000;
                location.setSpeed(0.0f);
                displayNotification(location,"Idle");
                fn_update(location,"Idle");

            }
        },10000);

    }

    private String getStatus(int speed) {

        String status;

        //in kmph
        int walk_speed=getSharedPreferences("speeds",MODE_PRIVATE).getInt("walk_speed",10);
        int jog_speed=getSharedPreferences("speeds",MODE_PRIVATE).getInt("jog_speed",35);
        int run_speed=getSharedPreferences("speeds",MODE_PRIVATE).getInt("run_speed",75);

        if(speed==0){
            status="Idle";
            latency=3000;
            Log.i("location","Idle "+ speed+"kmph");
        }else if(speed<=walk_speed){
            status="Walking";
            latency=3400;
            Log.i("location","Walking "+ speed+"kmph");
        }else if (speed<=jog_speed){
            status="Jogging";
            latency=8700;
            Log.i("location","Jogging "+ speed+"kmph");
        }else  if(speed<=run_speed){
            status="Running";
            latency=11800;
            Log.i("location","Running "+ speed+"kmph");
        }else{
            status="In a vehicle";
            latency=15500;
            Log.i("location","Travelling "+ speed+"kmph");
        }
        return status;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    @SuppressLint("MissingPermission")
    private void fn_getlocation(){

        locationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        Criteria criteria=new Criteria();
        bestProvider=String.valueOf(locationManager.getBestProvider(criteria,true));
        isGPSEnable = locationManager.isProviderEnabled(bestProvider);

        if(!isGPSEnable){
            if(Utils.locationServicesStatusCheck(getApplicationContext())) fn_getlocation();
        }

        location = null;
        locationManager.requestLocationUpdates(bestProvider,latency,8,this);
        if (locationManager!=null){
            location = locationManager.getLastKnownLocation(bestProvider);
            if (location!=null){
                Log.i("location",location.getLatitude()+"/"+location.getLongitude());
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                displayNotification(location,getStatus((int)(location.getSpeed()*(18/5))));
            }
        }

    }

    private void fn_update(final Location location, final String status){

        Log.i("location","firebase update start");

        boolean update=getSharedPreferences("service",MODE_PRIVATE).getBoolean("update_device_info_using_service",false);

        Map<String,Object> map=new HashMap<>();
        map.put("location", String.format("%s/%s/%s/%s/%s/%s", location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getSpeed(), status,System.currentTimeMillis()));
        if(update) {

            //android version
            Field[]fields= Build.VERSION_CODES.class.getFields();
            String android_version="";
            for(Field field:fields){
                android_version="Android "+field.getName()+" ("+Build.VERSION.RELEASE+")";
            }

            //device info
            String device_name=Build.MANUFACTURER+" "+Build.MODEL;
            long timestamp=System.currentTimeMillis();

            //battery info
            BatteryStats batteryStats=new BatteryStats(this);
            String battery_status=(batteryStats.isCharging())?"Charging":"Discharging";

            //update firebase database here
            map.put("device",device_name+"/"+android_version+"/"+batteryStats.getLevel()+"/"+battery_status+"/"+timestamp);

        }

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                UserEntity userEntity=userDatabase.userDao().getUserById(1);
                userEntity.setLocation(String.format("%s/%s/%s/%s/%s/%s", location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getSpeed(), status,System.currentTimeMillis()));
                userDatabase.userDao().updateUser(userEntity);
            }
        });


        FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .updateChildren(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("location","firebase updated");
                    }
                });

        intent.putExtra("location",String.valueOf(location.getLatitude()));
        intent.putExtra("longitude",String.valueOf(location.getLongitude()));
        intent.putExtra("altitude",String.valueOf(location.getAltitude()));
        intent.putExtra("speed",String.valueOf(location.getSpeed()));
        intent.putExtra("status",status);
        intent.putExtra("timestamp",String.valueOf(System.currentTimeMillis()));
        sendBroadcast(intent);

    }

    protected void displayNotification(Location location,String status) {


        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(getResources().getString(R.string.location_on));

        if(location==null){
            bigTextStyle.bigText(getResources().getString(R.string.running_in_background));
        }else{
            bigTextStyle.bigText(String.format(getResources().getString(R.string.details),status,String.valueOf(location.getLatitude()),String.valueOf(location.getLongitude()),String.valueOf(location.getAltitude()),String.valueOf(Math.round(location.getSpeed()))));
        }


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "location");
        Intent ii = new Intent(this, SplashScreen.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, ii, 0);

        Intent broadcastIntent = new Intent(this, ManageServiceReceiver.class);
        broadcastIntent.setAction("com.amsavarthan.posizione.STOP");
        PendingIntent turnOffIntent=PendingIntent.getBroadcast(this,1,broadcastIntent,0);

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.drawable.ic_notification);
        mBuilder.setColor(getResources().getColor(R.color.colorAccent));
        mBuilder.setContentTitle("Live Location is on");
        if(location==null){
            mBuilder.setContentText("Posizione is running in background");
        }else{
            if(status.equals("Idle")) {
                mBuilder.setContentText(String.format(getResources().getString(R.string.short_status), status));
            }else{
                if(location.hasSpeed()) {
                    mBuilder.setContentText(String.format(getResources().getString(R.string.status_with_speed), status, String.valueOf(Math.round(location.getSpeed()))));
                }else{
                    mBuilder.setContentText(String.format(getResources().getString(R.string.short_status), status));

                }
            }
        }
        mBuilder.setAutoCancel(false);
        mBuilder.setStyle(bigTextStyle);
        mBuilder.setOngoing(true);
        mBuilder.addAction(R.drawable.ic_location_off_black_24dp,"Turn off",turnOffIntent);

        mNotificationManager =
                (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        startForeground(1,mBuilder.build());

    }


}