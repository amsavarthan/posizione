package com.amsavarthan.posizione.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.models.User;
import com.amsavarthan.posizione.room.user.UserDatabase;
import com.amsavarthan.posizione.room.user.UserEntity;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.amsavarthan.posizione.utils.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.abara.library.batterystats.BatteryStats;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class SetProfileActivity extends AppCompatActivity {

    private Uri imageUri=null;
    private CircleImageView pic;
    private TextInputEditText name;
    private FirebaseAuth mAuth;
    private ProgressDialog mDialog;
    private User oldUser=null;
    private String phoneNumber;

    @Override
    protected void onResume() {
        super.onResume();
        if(Utils.isOnline(this)) {
            mDialog.show();
        }
        FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(mAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        mDialog.dismiss();

                        if(dataSnapshot.exists()) {

                            oldUser = dataSnapshot.getValue(User.class);
                            name.setText(oldUser.getName());
                            Glide.with(getApplicationContext())
                                    .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art))
                                    .load(oldUser.getImage())
                                    .into(pic);

                        }else{
                            oldUser=new User();
                            oldUser.setImage("");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    private void updateDeviceInfo(User user){

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
        String status=(batteryStats.isCharging())?"Charging":"Discharging";

        user.setDevice(device_name+"/"+android_version+"/"+batteryStats.getLevel()+"/"+status+"/"+timestamp);
        Map<String,Object> map=new HashMap<>();
        map.put("device",device_name+"/"+android_version+"/"+batteryStats.getLevel()+"/"+status+"/"+timestamp);

        FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(mAuth.getCurrentUser().getUid())
                .updateChildren(map, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        Log.i("firebase","updated device details");
                    }
                });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_profile);

        phoneNumber=getIntent().getStringExtra("phone");

        mAuth=FirebaseAuth.getInstance();
        pic=findViewById(R.id.pic);
        name=findViewById(R.id.name);

        mDialog=new ProgressDialog(this);
        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setIndeterminate(true);
        mDialog.setMessage("Please wait...");
        if(Utils.isOnline(this)) {
            mDialog.show();
        }
        FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(mAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        mDialog.dismiss();

                        if(dataSnapshot.exists()) {

                            oldUser = dataSnapshot.getValue(User.class);
                            name.setText(oldUser.getName());
                            Glide.with(getApplicationContext())
                                    .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art))
                                    .load(oldUser.getImage())
                                    .into(pic);

                        }else{
                            oldUser=new User();
                            oldUser.setImage("");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    public void proceed(View view) {

        if(!Utils.isOnline(this)){
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(name.getWindowToken(), 0);
        if(TextUtils.isEmpty(name.getText().toString().replace(" ",""))){
            Toast.makeText(this, "Invalid name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(oldUser.getImage()) && imageUri == null) {
            Toast.makeText(this, "Profile picture recommended", Toast.LENGTH_SHORT).show();
            return;
        }

        if(imageUri!=null && TextUtils.isEmpty(oldUser.getImage())){

        mDialog.show();

            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                @Override
                public void onSuccess(final InstanceIdResult instanceIdResult) {

                    final User user=new User();
                    user.setName(name.getText().toString());
                    user.setPhone(phoneNumber);
                    user.setWho_can_track("2");
                    if(TextUtils.isEmpty(oldUser.getName())) {
                        //if no old user found
                        user.setUnique_id(Utils.locationUserId(6));
                        user.setToken(instanceIdResult.getToken());
                        user.setLocation("");
                        //user.setWho_can_track("2");
                    }else{
                        //user.setWho_can_track(oldUser.getWho_can_track());
                        user.setUnique_id(oldUser.getUnique_id());
                        user.setLocation(oldUser.getLocation());
                        if(!TextUtils.isEmpty(oldUser.getToken())){

                            mDialog.dismiss();
                            AlertDialog.Builder builder = new AlertDialog.Builder(SetProfileActivity.this);
                            builder.setTitle("Override")
                                    .setMessage("App installed with same account on another device, logout from that device or click override to continue")
                                    .setIcon(R.drawable.ic_devices_24dp)
                                    .setCancelable(true)
                                    .setPositiveButton("Override", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            mDialog.show();
                                            user.setToken(instanceIdResult.getToken());
                                            uploadUser(user);
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            return;
                        }else {
                            user.setToken(instanceIdResult.getToken());
                            uploadUser(user);
                        }

                    }

                    uploadUser(user);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    mDialog.dismiss();
                    e.printStackTrace();
                    Toast.makeText(SetProfileActivity.this, "Error : "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        }else{

            if(!TextUtils.isEmpty(oldUser.getToken())){

                mDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(SetProfileActivity.this);
                builder.setTitle("Override")
                        .setMessage("App installed with same account on another device, logout from that device or click override to continue")
                        .setIcon(R.drawable.ic_devices_24dp)
                        .setCancelable(true)
                        .setPositiveButton("Override", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mDialog.show();
                                uploadOldUser();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
                return;

            }

            uploadOldUser();

        }

    }

    private void uploadUser(final User user) {

        final StorageReference storageReference= FirebaseStorage.getInstance().getReference().child("profile_pictures").child(mAuth.getCurrentUser().getUid()+".webp");
        storageReference.putFile(imageUri)
                .addOnSuccessListener(this,new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        storageReference.getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess( Uri uri) {

                                        user.setImage(uri.toString());

                                        Map<String,Object> map=new HashMap<>();
                                        map.put("name",user.getName());
                                        map.put("unique_id",user.getUnique_id());
                                        map.put("image",user.getImage());
                                        map.put("token",user.getToken());
                                        map.put("location",user.getLocation());
                                        map.put("phone",user.getPhone());
                                        map.put("who_can_track",user.getWho_can_track());

                                        getSharedPreferences("privacy",MODE_PRIVATE).edit().putString("who_can_track",user.getWho_can_track()).apply();

                                        FirebaseDatabase.getInstance().getReference().child("users")
                                                .child(mAuth.getCurrentUser().getUid())
                                                .updateChildren(map)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {

                                                        updateDeviceInfo(user);
                                                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                UserDatabase userDatabase=UserDatabase.getInstance(getApplicationContext());
                                                                UserEntity userEntity=new UserEntity(1,user.getName(),user.getImage(),user.getUnique_id(), user.getLocation(), user.getToken(), user.getDevice(), user.getPhone(), user.getWho_can_track());
                                                                userDatabase.userDao().addUser(userEntity);
                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        mDialog.dismiss();
                                                                        startActivity(new Intent(getApplicationContext(),MainActivity.class));
                                                                        finish();
                                                                    }
                                                                });
                                                            }
                                                        });

                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        mDialog.dismiss();
                                                        e.printStackTrace();
                                                        Toast.makeText(SetProfileActivity.this, "Error : "+e.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                });

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        mDialog.dismiss();
                                        e.printStackTrace();
                                        Toast.makeText(SetProfileActivity.this, "Error : "+e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });

                    }
                })
                .addOnFailureListener(this,new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mDialog.dismiss();
                        e.printStackTrace();
                        Toast.makeText(SetProfileActivity.this, "Error : "+e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

    }

    private void uploadOldUser() {

        mDialog.show();
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                final User user=new User();
                user.setName(name.getText().toString());
                user.setImage(oldUser.getImage());
                user.setUnique_id(oldUser.getUnique_id());
                user.setToken(instanceIdResult.getToken());
                user.setLocation(oldUser.getLocation());
                user.setWho_can_track("2");
                //user.setWho_can_track(oldUser.getWho_can_track());
                user.setPhone(phoneNumber);

                Map<String,Object> map=new HashMap<>();
                map.put("name",user.getName());
                map.put("unique_id",user.getUnique_id());
                map.put("image",user.getImage());
                map.put("token",user.getToken());
                map.put("location",user.getLocation());
                map.put("phone",user.getPhone());
                map.put("who_can_track",user.getWho_can_track());
                getSharedPreferences("privacy",MODE_PRIVATE).edit().putString("who_can_track",user.getWho_can_track()).apply();

                FirebaseDatabase.getInstance().getReference().child("users")
                        .child(mAuth.getCurrentUser().getUid())
                        .updateChildren(map)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                updateDeviceInfo(user);
                                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        UserDatabase userDatabase=UserDatabase.getInstance(getApplicationContext());
                                        UserEntity userEntity=new UserEntity(1,user.getName(),user.getImage(),user.getUnique_id(), user.getLocation(), user.getToken(), user.getDevice(), user.getPhone(), user.getWho_can_track());
                                        userDatabase.userDao().addUser(userEntity);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mDialog.dismiss();
                                                startActivity(new Intent(getApplicationContext(),MainActivity.class));
                                                finish();
                                            }
                                        });
                                    }
                                });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mDialog.dismiss();
                                e.printStackTrace();
                                Toast.makeText(SetProfileActivity.this, "Error : "+e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                mDialog.dismiss();
                e.printStackTrace();
                Toast.makeText(SetProfileActivity.this, "Error : "+e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){

            CropImage.ActivityResult result=CropImage.getActivityResult(data);
            if(resultCode==RESULT_OK){

                if(!TextUtils.isEmpty(oldUser.getImage())){
                    oldUser.setImage("");
                }

                try {
                    File compressedFile= new Compressor(this)
                            .setCompressFormat(Bitmap.CompressFormat.WEBP)
                            .setQuality(75)
                            .compressToFile(new File(result.getUri().getPath()));
                    imageUri=Uri.fromFile(compressedFile);
                } catch (IOException e) {
                    imageUri=result.getUri();
                    e.printStackTrace();
                }

                Glide.with(this)
                        .load(imageUri)
                        .into(pic);

            }else if(resultCode==CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                Exception error=result.getError();
                error.printStackTrace();
                Toast.makeText(this, "Some error occurred", Toast.LENGTH_SHORT).show();
            }


        }

    }

    public void choosePic(View view) {

        if(!Utils.isOnline(this)){
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON_TOUCH)
                .setAspectRatio(1,1)
                .setActivityTitle("Crop Picture")
                .setAutoZoomEnabled(true)
                .setCropShape(CropImageView.CropShape.OVAL)
                .start(this);

    }

 
}
