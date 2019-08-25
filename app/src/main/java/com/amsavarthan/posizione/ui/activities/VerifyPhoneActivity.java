package com.amsavarthan.posizione.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.room.user.UserDatabase;
import com.amsavarthan.posizione.room.user.UserEntity;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mukesh.OtpView;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class VerifyPhoneActivity extends AppCompatActivity {

    String phone;
    TextView otp;
    FirebaseAuth mAuth;
    OtpView code_text;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    MaterialButton retry;
    ProgressBar mBar;
    private UserDatabase userDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_phone);

        mAuth=FirebaseAuth.getInstance();
        userDatabase=UserDatabase.getInstance(this);
        phone=getIntent().getStringExtra("phone");

        otp=findViewById(R.id.otp);
        code_text=findViewById(R.id.code);
        retry=findViewById(R.id.resend);
        mBar=findViewById(R.id.pbar);

        otp.setText(String.format(getResources().getString(R.string.otp_sent),phone));
        mBar.setVisibility(VISIBLE);

        sendVerificationCode(phone);

    }

    private void sendVerificationCode(String mobile) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                mobile,
                30,
                TimeUnit.SECONDS,
                TaskExecutors.MAIN_THREAD,
                mCallbacks);
    }


    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
            //Getting the code sent by SMS
            String code = phoneAuthCredential.getSmsCode();

            //sometime the code is not detected automatically
            //in this case the code will be null
            //so user has to manually enter the code
            if (code != null) {
                code_text.setText(code);
                //verifying the code
                verifyVerificationCode(code);
            }
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            mBar.setVisibility(GONE);
            retry.setEnabled(true);
            Toast.makeText(VerifyPhoneActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCodeAutoRetrievalTimeOut(String s) {
            super.onCodeAutoRetrievalTimeOut(s);
            mBar.setVisibility(GONE);
            retry.setEnabled(true);
            otp.setText(String.format(getResources().getString(R.string.otp_failed),phone));
        }

        @Override
        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            otp.setText(String.format(getResources().getString(R.string.otp_sent),phone));
            mVerificationId = s;
            mResendToken = forceResendingToken;
        }
    };

    private void verifyVerificationCode(String otp) {
        //creating the credential
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, otp);
        //signing the user
        if(mAuth.getCurrentUser()!=null){

            final ProgressDialog progressDialog=new ProgressDialog(VerifyPhoneActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setMessage("Please wait..");
            progressDialog.setIndeterminate(true);
            progressDialog.show();

            if(mBar.getVisibility()==GONE){
                mBar.setVisibility(VISIBLE);
            }

            mAuth.getCurrentUser().updatePhoneNumber(credential)
                    .addOnCompleteListener(VerifyPhoneActivity.this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {

                                Map<String,Object> map=new HashMap<>();
                                map.put("phone",phone);

                                FirebaseDatabase.getInstance().getReference().child("users")
                                        .child(mAuth.getCurrentUser().getUid())
                                        .updateChildren(map, new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                                mBar.setVisibility(GONE);
                                                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        UserEntity userEntity=userDatabase.userDao().getUserById(1);
                                                        userEntity.setPhone(phone);
                                                        userDatabase.userDao().updateUser(userEntity);
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                progressDialog.dismiss();
                                                                Intent intent = new Intent(VerifyPhoneActivity.this, MainActivity.class);
                                                                startActivity(intent);
                                                                Toast.makeText(VerifyPhoneActivity.this, "Phone number changed", Toast.LENGTH_SHORT).show();
                                                                finish();
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                        });


                            } else {

                                if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(VerifyPhoneActivity.this, "Invalid code", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                Toast.makeText(VerifyPhoneActivity.this, "Something went wrong...try later", Toast.LENGTH_SHORT).show();

                            }
                        }
                    });

        }else {
            signInWithPhoneAuthCredential(credential);
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {

        if(mBar.getVisibility()==GONE){
            mBar.setVisibility(VISIBLE);
        }

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(VerifyPhoneActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        mBar.setVisibility(GONE);
                        if (task.isSuccessful()) {

                            Intent intent = new Intent(VerifyPhoneActivity.this, SetProfileActivity.class);
                            intent.putExtra("phone",phone);
                            startActivity(intent);
                            finish();

                        } else {

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(VerifyPhoneActivity.this, "Invalid code", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Toast.makeText(VerifyPhoneActivity.this, "Something went wrong...try later", Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }

    public void verify(View view) {

        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(code_text.getWindowToken(), 0);
        String code = code_text.getText().toString().trim();
        if (code.isEmpty() || code.length() < 6) {
            Toast.makeText(this, "Invalid code", Toast.LENGTH_SHORT).show();
            return;

        }
        verifyVerificationCode(code);
    }

    public void resend(View view) {
        mBar.setVisibility(VISIBLE);
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phone,
                20,
                TimeUnit.SECONDS,
                TaskExecutors.MAIN_THREAD,
                mCallbacks,
                mResendToken);
    }

    public void changeNumber(View view) {
        startActivity(new Intent(this,PhoneAuthActivity.class));
        finish();
    }
}

