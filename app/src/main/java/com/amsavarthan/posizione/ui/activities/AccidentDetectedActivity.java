package com.amsavarthan.posizione.ui.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.adapters.AcciContactsRecyclerAdapter;
import com.amsavarthan.posizione.adapters.FavouritesRecyclerAdapter;
import com.amsavarthan.posizione.receivers.ManageServiceReceiver;
import com.amsavarthan.posizione.room.fav.FavDatabase;
import com.amsavarthan.posizione.room.fav.FavEntity;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.google.firebase.auth.FirebaseAuth;
import com.ncorti.slidetoact.SlideToActView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AccidentDetectedActivity extends AppCompatActivity {

    private TextView time,info;
    private SlideToActView sliderView;
    RecyclerView recyclerView;
    List<FavEntity> favEntities=new ArrayList<>();
    AcciContactsRecyclerAdapter mAdapter;
    FavDatabase favDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accident_detected);


        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("com.amsavarthan.posizione.accident_detector.STOP");
        broadcastIntent.setClass(getApplicationContext(), ManageServiceReceiver.class);
        sendBroadcast(broadcastIntent);

        int uioptiions=getWindow().getDecorView().getSystemUiVisibility();
        uioptiions|=View.SYSTEM_UI_FLAG_FULLSCREEN;
        uioptiions|=View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            uioptiions|=View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        getWindow().getDecorView().setSystemUiVisibility(uioptiions);

        favDatabase=FavDatabase.getInstance(this);
        recyclerView=findViewById(R.id.recyclerview);

        GridLayoutManager layoutManager=new GridLayoutManager(this,2);
        layoutManager.setSmoothScrollbarEnabled(false);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);

        mAdapter=new AcciContactsRecyclerAdapter(this,favEntities);
        recyclerView.setAdapter(mAdapter);

        time=findViewById(R.id.time);
        info=findViewById(R.id.info);
        sliderView=findViewById(R.id.slider);
        sliderView.setOnSlideCompleteListener(new SlideToActView.OnSlideCompleteListener() {
            @Override
            public void onSlideComplete(SlideToActView slideToActView) {
                finish();
            }
        });

        new CountDownTimer(10000,1000){

            @Override
            public void onTick(long l) {
                int seconds=(int)l/1000;
                if(seconds==10) {
                    time.setText(seconds);
                }else{
                    time.setText(String.format(Locale.ENGLISH,"0%d", seconds));
                }
            }

            @Override
            public void onFinish() {

                info.setText("Sending alerts to....");

                time.animate()
                        .alpha(0.0f)
                        .setDuration(400)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                time.setVisibility(View.GONE);
                            }
                        })
                        .start();

                sliderView.animate()
                        .alpha(0.0f)
                        .setDuration(400)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                sliderView.setVisibility(View.GONE);
                            }
                        })
                        .start();

                recyclerView.setVisibility(View.VISIBLE);
                getFavourites();

            }
        }.start();

    }

    @Override
    protected void onStop() {
        super.onStop();
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("com.amsavarthan.posizione.accident_detector.START");
        broadcastIntent.setClass(getApplicationContext(), ManageServiceReceiver.class);
        sendBroadcast(broadcastIntent);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("com.amsavarthan.posizione.accident_detector.START");
        broadcastIntent.setClass(getApplicationContext(), ManageServiceReceiver.class);
        sendBroadcast(broadcastIntent);

    }

    private void getFavourites() {

        favEntities.clear();
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {

                favEntities.addAll(favDatabase.favDao().getFavList());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        mAdapter.notifyDataSetChanged();

                    }
                });

            }
        });

    }

}
