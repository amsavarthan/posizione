package com.amsavarthan.posizione.utils;

import android.app.Activity;
import android.app.ActivityManager;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amsavarthan.posizione.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static int screenWidth = 0;
    private static int screenHeight = 0;

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static int dpTopx(Context context,int dp) {
        return Math.round(dp*(context.getResources().getDisplayMetrics().xdpi/ DisplayMetrics.DENSITY_DEFAULT));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void createNotificationChannels(Context context){

        NotificationManager notificationManager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        //notificationManager.createNotificationChannelGroup(new NotificationChannelGroup("posizione","Posizione"));

        NotificationChannel request_channel=new NotificationChannel("request","Request Alerts",NotificationManager.IMPORTANCE_HIGH);
        request_channel.enableLights(true);
        request_channel.enableVibration(true);
        //request_channel.setGroup("posizione");

        NotificationChannel location_channel=new NotificationChannel("location","Live Location",NotificationManager.IMPORTANCE_MIN);
        location_channel.enableLights(true);
        location_channel.enableVibration(true);
        //location_channel.setGroup("posizione");


        NotificationChannel other_channel=new NotificationChannel("other","Other Notifications",NotificationManager.IMPORTANCE_HIGH);
        other_channel.enableLights(true);
        other_channel.enableVibration(true);

        List<NotificationChannel> notificationChannels=new ArrayList<>();
        notificationChannels.add(request_channel);
        notificationChannels.add(location_channel);
        notificationChannels.add(other_channel);

        notificationManager.createNotificationChannels(notificationChannels);

    }

    public static Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static  Bitmap getCircularBitmap(Bitmap bitmap){
        final Bitmap output=Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(),Bitmap.Config.ARGB_8888);
        final Canvas canvas=new Canvas(output);
        final int color= Color.WHITE;
        final Paint paint=new Paint();
        final Rect rect=new Rect(0,0,bitmap.getWidth(),bitmap.getHeight());
        final RectF rectF=new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0,0,0,0);
        paint.setColor(color);
        canvas.drawOval(rectF,paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap,rect,rect,paint);
        bitmap.recycle();
        return output;

    }

    public static double distance(double lat1,double lng1,double lat2,double lng2){
        double thetha=lng1-lng2;
        double dist=Math.sin(deg2rad(lat1))*Math.sin(deg2rad(lat2))
                +Math.cos(deg2rad(lat1))*Math.cos(deg2rad(lat2))
                *Math.cos(deg2rad(thetha));
        dist=Math.acos(dist);
        dist=rad2deg(dist);
        dist=dist*60*1.1515;
        return Math.round(dist);
    }

    public static  double deg2rad(double deg) {
        return (deg*Math.PI/180.0);
    }

    public static  double rad2deg(double rad) {
        return (rad*180.0/Math.PI);
    }


    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz";
    public static String locationUserId(int count){

        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();

    }

    public static boolean isMyServiceRunning(Class<?> serviceClass,Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", "true");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", "false");
        return false;
    }


    public static boolean locationServicesStatusCheck(final Context context) {
        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return true;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enable GPS")
                .setMessage("This function needs your GPS, do you want to enable it now?")
                .setIcon(R.drawable.ic_gps_fixed_black_24dp)
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();


        return false;
    }

}
