package com.amsavarthan.posizione.ui.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.receivers.ManageServiceReceiver;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mukesh.OtpView;

public class ParentLockActivity extends AppCompatActivity {

    String pass;
    ImageView image;
    TextView textView;
    OtpView pass_code;
    FloatingActionButton fab;
    private boolean fromNotification;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        supportFinishAfterTransition();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parental_lock);

        fromNotification=getIntent().getBooleanExtra("fromNotification",false);

        getSupportActionBar().setTitle("Parental lock");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        pass_code=findViewById(R.id.pass);
        image=findViewById(R.id.pic);
        textView=findViewById(R.id.text);
        fab=findViewById(R.id.fab);
        pass=getSharedPreferences("lock",MODE_PRIVATE).getString("password","0");

        if(pass.equalsIgnoreCase("0")){

            image.setImageDrawable(getResources().getDrawable(R.drawable.ic_lock_open_black_24dp));
            textView.setText("Set up a pin");
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String input=pass_code.getText().toString().replace(" ","");
                    if(TextUtils.isEmpty(input) || input.length()<5){
                        Toast.makeText(ParentLockActivity.this, "Invalid pin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    getSharedPreferences("lock",MODE_PRIVATE).edit().putString("password",input).apply();
                    Toast.makeText(ParentLockActivity.this, "Parental lock enabled", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });


        }else{

            image.setImageDrawable(getResources().getDrawable(R.drawable.ic_lock_outline_black_24dp));
            textView.setText("Enter pin to continue");
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    String input=pass_code.getText().toString().replace(" ","");
                    if(TextUtils.isEmpty(input) || input.length()<5){
                        Toast.makeText(ParentLockActivity.this, "Invalid pin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(input.equalsIgnoreCase(pass)) {
                        getSharedPreferences("lock",MODE_PRIVATE).edit().putString("password","0").apply();

                        if(!fromNotification) {
                            Toast.makeText(ParentLockActivity.this, "Parental lock disabled", Toast.LENGTH_SHORT).show();
                        }else{

                            Intent broadcastIntent = new Intent();
                            broadcastIntent.setAction("com.amsavarthan.posizione.STOP");
                            broadcastIntent.setClass(getApplicationContext(), ManageServiceReceiver.class);
                            sendBroadcast(broadcastIntent);

                        }

                        finish();
                    }else{
                        Toast.makeText(ParentLockActivity.this, "Invalid pin", Toast.LENGTH_SHORT).show();
                    }

                }
            });

        }

    }
}
