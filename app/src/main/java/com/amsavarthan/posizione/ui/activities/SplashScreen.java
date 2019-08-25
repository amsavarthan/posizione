package com.amsavarthan.posizione.ui.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.multidex.MultiDex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.room.user.UserDatabase;
import com.amsavarthan.posizione.room.user.UserEntity;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.google.firebase.auth.FirebaseAuth;

public class SplashScreen extends AppCompatActivity {

    FirebaseAuth mAuth;
    private UserDatabase userDatabase;
    private UserEntity user;

    @Override
    protected void attachBaseContext(Context newBase) {
        MultiDex.install(this);
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        mAuth=FirebaseAuth.getInstance();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mAuth.getCurrentUser()!=null) {

                    userDatabase=UserDatabase.getInstance(getApplicationContext());
                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            user = userDatabase.userDao().getUserById(1);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(user!=null){
                                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                        finish();
                                    }else{
                                        startActivity(new Intent(getApplicationContext(), SetProfileActivity.class));
                                        finish();
                                    }
                                }
                            });
                        }
                    });


                }else{
                    startActivity(new Intent(getApplicationContext(), WelcomeScreen.class));
                    finish();
                }
            }
        },1200);
    }
}
