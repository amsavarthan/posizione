package com.amsavarthan.posizione.ui.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.room.user.UserDatabase;
import com.amsavarthan.posizione.room.user.UserEntity;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.amsavarthan.posizione.utils.Utils;
import com.google.android.material.button.MaterialButton;

import me.zheteng.countrycodeselector.PhoneInputView;

import static com.amsavarthan.posizione.ui.activities.ProfileSettings.instance;

public class PhoneAuthActivity extends AppCompatActivity {

    PhoneInputView phoneInputView;
    TextView title;
    MaterialButton button;
    boolean forEdit;
    private UserDatabase userDatabase;

    public static void startActivityforEdit(Context context){
        context.startActivity(new Intent(context,PhoneAuthActivity.class).putExtra("forEdit",true));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_auth);

        phoneInputView=findViewById(R.id.phone);
        title=findViewById(R.id.title);
        button=findViewById(R.id.verify);

        forEdit=getIntent().getBooleanExtra("forEdit",false);
        if(forEdit){

            userDatabase=UserDatabase.getInstance(this);
            title.setText(getResources().getString(R.string.edit_phone_title));
            if(!Utils.isOnline(PhoneAuthActivity.this)) {
                button.setText(getResources().getString(R.string.save));
            }

        }


    }

    public void verify(View view) {

        if(!Utils.isOnline(this)){
            if(forEdit){
                if(!TextUtils.isEmpty(phoneInputView.getPhoneNumber().replace(" ","")) && phoneInputView.getPhoneNumber().length()==10) {
                    final String phone = "+" + phoneInputView.getCountryCode() + phoneInputView.getPhoneNumber();

                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            final String existingPhone=userDatabase.userDao().getUserById(1).getPhone();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(existingPhone.equals(phone)){
                                        Toast.makeText(PhoneAuthActivity.this, "Cannot change to this number", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    new MaterialDialog.Builder(PhoneAuthActivity.this)
                                            .title("Verify "+phone)
                                            .content("Are you sure do you want to change to this number ?")
                                            .positiveText("Yes")
                                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                @Override
                                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                                                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            UserEntity userEntity=userDatabase.userDao().getUserById(1);
                                                            userEntity.setPhone(phone);
                                                            userDatabase.userDao().updateUser(userEntity);
                                                            runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    Toast.makeText(PhoneAuthActivity.this, "Changes will be updated when network is available", Toast.LENGTH_SHORT).show();
                                                                    finish();
                                                                }
                                                            });
                                                        }
                                                    });

                                                }
                                            })
                                            .negativeText("No")
                                            .show();
                                }
                            });

                        }
                    });

                }else{
                    Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
                }

            }else {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if(!TextUtils.isEmpty(phoneInputView.getPhoneNumber().replace(" ","")) && phoneInputView.getPhoneNumber().length()==10) {

            final String phone="+"+phoneInputView.getCountryCode() + phoneInputView.getPhoneNumber();

            if(forEdit){

                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        final String existingPhone=userDatabase.userDao().getUserById(1).getPhone();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(existingPhone.equals(phone)){
                                    Toast.makeText(PhoneAuthActivity.this, "Cannot change to this number", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                new MaterialDialog.Builder(PhoneAuthActivity.this)
                                        .title("Verify "+phone)
                                        .content("Are you sure do you want to change to this number ?")
                                        .positiveText("Yes")
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                startActivity(new Intent(getApplicationContext(), VerifyPhoneActivity.class).putExtra("phone", phone));
                                                instance.finish() ;
                                                finish();
                                            }
                                        })
                                        .negativeText("No")
                                        .show();
                            }
                        });
                    }
                });

            }else{

                new MaterialDialog.Builder(PhoneAuthActivity.this)
                        .title("Verify "+phone)
                        .content("Are you sure do you want to continue ?")
                        .positiveText("Yes")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                startActivity(new Intent(getApplicationContext(), VerifyPhoneActivity.class).putExtra("phone", phone));
                                finish();
                            }
                        })
                        .negativeText("No")
                        .show();

            }
        }else{
            Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
        }
    }
}
