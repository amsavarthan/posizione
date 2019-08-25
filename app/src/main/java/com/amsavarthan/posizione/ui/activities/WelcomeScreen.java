package com.amsavarthan.posizione.ui.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.amsavarthan.posizione.R;

public class WelcomeScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);
    }

    public void getStarted(View view) {

        startActivity(new Intent(this,PhoneAuthActivity.class));
        finish();

    }
}
