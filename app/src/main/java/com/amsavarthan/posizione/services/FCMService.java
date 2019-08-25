package com.amsavarthan.posizione.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.ui.activities.MainActivity;
import com.amsavarthan.posizione.ui.activities.TrackRequests;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.amsavarthan.posizione.room.user.UserDatabase;
import com.amsavarthan.posizione.room.user.UserEntity;
import com.amsavarthan.posizione.utils.Utils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

import static com.amsavarthan.posizione.utils.Utils.getBitmapFromURL;
import static com.amsavarthan.posizione.utils.Utils.getCircularBitmap;

public class FCMService extends FirebaseMessagingService {

    private UserDatabase userDatabase;

    @Override
    public void onNewToken(final String s) {
        super.onNewToken(s);

        if(FirebaseAuth.getInstance().getCurrentUser()!=null) {

            userDatabase=UserDatabase.getInstance(getApplicationContext());
            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    UserEntity user=userDatabase.userDao().getUserById(1);
                    user.setToken(s);
                    userDatabase.userDao().updateUser(user);
                }
            });

            if (Utils.isOnline(this)) {
                Map<String, Object> map = new HashMap<>();
                map.put("token", s);

                FirebaseDatabase.getInstance().getReference()
                        .child("users")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .updateChildren(map)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.i("Token", "Token updated");
                            }
                        });
            }

        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title=remoteMessage.getData().get("title");
        String body=remoteMessage.getData().get("body");
        String action=remoteMessage.getData().get("action");
        String image=remoteMessage.getData().get("image");
        String timestamp=remoteMessage.getData().get("timestamp");

        Intent intent;
        NotificationCompat.Builder mBuilder;
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());

        int id=(int)System.currentTimeMillis();


        if(title.equals("New Request")){
            mBuilder = new NotificationCompat.Builder(getApplicationContext(), "request");
        }else{
            mBuilder = new NotificationCompat.Builder(getApplicationContext(), "other");
        }

        if (action.equals("com.amsavarthan.posizione.NEW_REQUEST")){
            intent=new Intent(getApplicationContext(), TrackRequests.class);
        }else{
            intent=new Intent(getApplicationContext(), MainActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        getApplicationContext(),
                        id,
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT
                );

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(title);
        bigTextStyle.bigText(body);

        Notification notification;

        notification = mBuilder
                .setAutoCancel(true)
                .setContentTitle(title)
                .setTicker(body)
                .setContentIntent(resultPendingIntent)
                .setColorized(true)
                .setColor(getResources().getColor(R.color.colorAccent))
                .setStyle(bigTextStyle)
                .setLargeIcon(getCircularBitmap(getBitmapFromURL(image)))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentText(body)
                .build();


        notificationManagerCompat.notify(id, notification);

    }
}
