package com.amsavarthan.posizione.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.room.user.UserDatabase;
import com.amsavarthan.posizione.room.user.UserEntity;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.amsavarthan.posizione.utils.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class ProfileSettings extends AppCompatActivity {

    UserDatabase userDatabase;
    EditText name,phone;
    CircleImageView pic;
    Uri imageUri;
    static ProfileSettings instance;
    FirebaseAuth mAuth;
    private ProgressDialog mDialog;

    @Override
    protected void onResume() {
        super.onResume();
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final UserEntity userEntity=userDatabase.userDao().getUserById(1);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        phone.setText(userEntity.getPhone());
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed() {

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {

                final String c_name=userDatabase.userDao().getUserById(1).getName();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if(imageUri!=null || !c_name.equals(name.getText().toString()) ) {

                            new MaterialDialog.Builder(ProfileSettings.this)
                                    .title("Save changes")
                                    .content("It seems that you made some changes to your profile, Do you want to update those?")
                                    .positiveText("Yes")
                                    .negativeText("No")
                                    .neutralText("Cancel")
                                    .neutralColor(getResources().getColor(R.color.colorPrimary))
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            saveDetails(true);
                                        }
                                    })
                                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            supportFinishAfterTransition();
                                        }
                                    })
                                    .show();

                        }else{
                            supportFinishAfterTransition();
                        }

                    }
                });

            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        instance=this;
        getSupportActionBar().setTitle("Edit Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        name=findViewById(R.id.name);
        phone=findViewById(R.id.phone);
        pic=findViewById(R.id.pic);
        mAuth=FirebaseAuth.getInstance();

        mDialog=new ProgressDialog(this);
        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setIndeterminate(true);
        mDialog.setMessage("Please wait...");

        userDatabase=UserDatabase.getInstance(this);
        imageUri=null;
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final UserEntity userEntity=userDatabase.userDao().getUserById(1);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        name.setText(userEntity.getName());
                        phone.setText(userEntity.getPhone());
                        Glide.with(getApplicationContext())
                                .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art))
                                .asBitmap()
                                .load(userEntity.getImage())
                                .into(pic);

                    }
                });
            }
        });

    }

    public void saveDetails(final boolean finishAfterUpdate) {

        if(TextUtils.isEmpty(name.getText().toString().replace(" ","")) || name.getText().toString().replace(" ","").length()<=2){
            Toast.makeText(this, "Invalid name", Toast.LENGTH_SHORT).show();
            return;
        }

        if(Utils.isOnline(this)){

            if(imageUri!=null){

                mDialog.setMessage("Uploading picture...");
                mDialog.show();
                final StorageReference storageReference= FirebaseStorage.getInstance().getReference().child("profile_pictures").child(mAuth.getCurrentUser().getUid()+".webp");
                storageReference.putFile(imageUri).addOnSuccessListener(ProfileSettings.this,new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        mDialog.setMessage("Please wait...");
                        storageReference.getDownloadUrl().addOnSuccessListener(ProfileSettings.this,new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(final Uri uri) {

                                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        final UserEntity userEntity=userDatabase.userDao().getUserById(1);
                                        userEntity.setName(name.getText().toString());
                                        userEntity.setImage(uri.toString());
                                        userDatabase.userDao().updateUser(userEntity);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {

                                                Map<String,Object> map=new HashMap<>();
                                                map.put("image",userEntity.getImage());
                                                map.put("name",userEntity.getName());

                                                FirebaseDatabase.getInstance().getReference()
                                                        .child("users")
                                                        .child(mAuth.getCurrentUser().getUid())
                                                        .updateChildren(map, new DatabaseReference.CompletionListener() {
                                                            @Override
                                                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                                                mDialog.dismiss();
                                                                imageUri=null;
                                                                Toast.makeText(ProfileSettings.this, "Profile updated", Toast.LENGTH_SHORT).show();
                                                                if(finishAfterUpdate){
                                                                    supportFinishAfterTransition();
                                                                }
                                                            }
                                                        });
                                            }
                                        });
                                    }
                                });

                            }
                        });


                    }
                }).addOnFailureListener(this,new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mDialog.dismiss();
                        e.printStackTrace();
                        Toast.makeText(ProfileSettings.this, "Error : "+e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

            }else{

                mDialog.setMessage("Please wait...");
                mDialog.show();
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        final UserEntity userEntity=userDatabase.userDao().getUserById(1);
                        userEntity.setName(name.getText().toString());
                        userDatabase.userDao().updateUser(userEntity);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                Map<String,Object> map=new HashMap<>();
                                map.put("name",userEntity.getName());

                                FirebaseDatabase.getInstance().getReference()
                                        .child("users")
                                        .child(mAuth.getCurrentUser().getUid())
                                        .updateChildren(map, new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                                mDialog.dismiss();
                                                Toast.makeText(ProfileSettings.this, "Profile updated", Toast.LENGTH_SHORT).show();
                                                if(finishAfterUpdate){
                                                    supportFinishAfterTransition();
                                                }
                                            }
                                        });
                            }
                        });
                    }
                });

            }

        }else{

            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    UserEntity userEntity=userDatabase.userDao().getUserById(1);
                    userEntity.setName(name.getText().toString());
                    userDatabase.userDao().updateUser(userEntity);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if(imageUri!=null){
                                Toast.makeText(ProfileSettings.this, "Couldn't update picture, Please check your internet connection", Toast.LENGTH_LONG).show();
                                return;
                            }

                            Toast.makeText(ProfileSettings.this, "Changes will be updated when network is available", Toast.LENGTH_SHORT).show();
                            if(finishAfterUpdate){
                                supportFinishAfterTransition();
                            }

                        }
                    });
                }
            });
        }

    }

    public void changeNumber(View view) {

        PhoneAuthActivity.startActivityforEdit(this);

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){

            CropImage.ActivityResult result=CropImage.getActivityResult(data);
            if(resultCode==RESULT_OK){

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

    public void changePicture(View view) {

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile_settings,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_save) {
            saveDetails(false);
        }
        return super.onOptionsItemSelected(item);

    }
}
