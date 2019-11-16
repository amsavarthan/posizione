package com.amsavarthan.posizione.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import androidx.appcompat.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.models.User;
import com.amsavarthan.posizione.room.friends.FriendDatabase;
import com.amsavarthan.posizione.room.friends.FriendEntity;
import com.amsavarthan.posizione.room.user.UserDatabase;
import com.amsavarthan.posizione.room.user.UserEntity;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.amsavarthan.posizione.utils.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.mukesh.OtpView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class NewFriendActivity extends AppCompatActivity {

    private ProgressDialog mDialog;
    private UserDatabase userDatabase;
    private FriendDatabase friendsDatabase;
    private CircleImageView pic;
    private TextView name;
    private OtpView unique_id;
    private User user;
    private List<FriendEntity> existingFriends;
    private FloatingActionButton remove,save;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_friend);

        getSupportActionBar().setTitle("Add Person");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        remove=findViewById(R.id.remove);
        save=findViewById(R.id.save);
        pic=findViewById(R.id.pic);
        unique_id=findViewById(R.id.unique_id);
        name=findViewById(R.id.text);

        mDialog=new ProgressDialog(this);
        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setIndeterminate(true);
        mDialog.setMessage("Please wait...");

        userDatabase=UserDatabase.getInstance(this);
        friendsDatabase=FriendDatabase.getInstance(this);

        remove.hide();

        existingFriends=new ArrayList<>();
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                existingFriends.addAll(friendsDatabase.friendDao().getFriendsList());
            }
        });



    }

    private void getUserDetails(final String code) {

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final String current_user_unique_id=userDatabase.userDao().getUserById(1).getUnique_id();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(code.equals(current_user_unique_id)){
                            mDialog.dismiss();
                            Toast.makeText(NewFriendActivity.this, "You can't add yourself", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        FirebaseDatabase.getInstance().getReference()
                                .child("users")
                                .orderByChild("unique_id")
                                .equalTo(code)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        mDialog.dismiss();
                                        if(dataSnapshot.exists()){

                                            for(DataSnapshot details:dataSnapshot.getChildren()) {
                                                user = details.getValue(User.class);

                                                save.setImageResource(R.drawable.ic_done_black_24dp);
                                                remove.show();

                                                name.setAlpha(0.0f);
                                                name.setText(user.getName());
                                                name.animate().alpha(1.0f).setDuration(150).start();

                                                unique_id.setEnabled(false);
                                                unique_id.setTextColor(getResources().getColor(R.color.white));
                                                unique_id.setLineColor(getResources().getColor(R.color.colorAccent));

                                                pic.setAlpha(0.0f);
                                                pic.animate().alpha(1.0f).setDuration(450)
                                                        .setListener(new AnimatorListenerAdapter() {
                                                            @Override
                                                            public void onAnimationEnd(Animator animation) {
                                                                super.onAnimationEnd(animation);
                                                                pic.setVisibility(View.VISIBLE);
                                                            }
                                                        }).start();

                                                Glide.with(getApplicationContext())
                                                        .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_invert))
                                                        .asBitmap()
                                                        .load(user.getImage())
                                                        .into(pic);
                                            }

                                        }else{
                                            Toast.makeText(NewFriendActivity.this, "Entered ID doesn't exist", Toast.LENGTH_SHORT).show();
                                        }

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                    }
                });
            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        supportFinishAfterTransition();
    }


    public void onDoneClicked(View view) {

        if(user!=null)
        {

            if(!Utils.isOnline(this)){
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                return;
            }

            mDialog.show();
            FirebaseDatabase.getInstance().getReference().child("users")
                    .orderByChild("unique_id")
                    .equalTo(user.getUnique_id())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            if(!dataSnapshot.exists()) {
                                mDialog.dismiss();
                                Toast.makeText(NewFriendActivity.this, "Account has been deleted", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            for(DataSnapshot details:dataSnapshot.getChildren()) {

                                mDialog.dismiss();
                                if(!details.exists()){
                                    Toast.makeText(NewFriendActivity.this, "Account has been deleted", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                user = details.getValue(User.class);

                                switch (Integer.parseInt(user.getWho_can_track())){

                                    case 1:
                                        addUser(true);
                                        break;
                                    case 2:
                                        new MaterialDialog.Builder(NewFriendActivity.this)
                                                .title("Privacy enabled")
                                                .content(String.format("Are you sure do you want to send track request to %s?",user.getName()))
                                                .positiveText("Yes")
                                                .negativeText("No")
                                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                    @Override
                                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                        addUser(false);
                                                    }
                                                })
                                                .show();
                                        break;
                                    case 3:
                                        new MaterialDialog.Builder(NewFriendActivity.this)
                                                .title("Privacy enabled")
                                                .content(String.format("%s has set their privacy settings in such way that no one track them",user.getName()))
                                                .positiveText("Ok")
                                                .show();

                                }

                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

        }
        else {

            if(TextUtils.isEmpty(unique_id.getText().toString().replace(" ","")) || unique_id.getText().toString().replace(" ","").length()<6){
                Toast.makeText(NewFriendActivity.this, "Invalid ID", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean alreadyExist=false;
            for(FriendEntity friendEntity:existingFriends){
                if(friendEntity.getUnique_id().equals(unique_id.getText().toString())){
                    alreadyExist=true;
                    break;
                }
            }

            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(unique_id.getWindowToken(), 0);

            if(!alreadyExist) {
                mDialog.show();
                getUserDetails(unique_id.getText().toString());
            }else Toast.makeText(NewFriendActivity.this, "User already exist", Toast.LENGTH_SHORT).show();

        }

    }

    private void addUser(final boolean value) {

        mDialog.show();

        FirebaseDatabase.getInstance().getReference().child("users")
                .orderByChild("unique_id")
                .equalTo(user.getUnique_id())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                        if(!dataSnapshot.exists()){
                            Toast.makeText(NewFriendActivity.this, "Account was deleted by "+user.getName(), Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        for(final DataSnapshot userDetails:dataSnapshot.getChildren()) {

                            if (!userDetails.exists()) {
                                Toast.makeText(NewFriendActivity.this, "Account was deleted by " + user.getName(), Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }

                            final String userUid=userDetails.getKey();

                            FirebaseDatabase.getInstance().getReference().child("users")
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .child("friends")
                                    .child(user.getUnique_id())
                                    .setValue(value)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {

                                            if(value) {
                                                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        FriendEntity friendEntity = new FriendEntity();
                                                        friendEntity.setName(user.getName());
                                                        friendEntity.setUnique_id(user.getUnique_id());
                                                        friendEntity.setImage(user.getImage());
                                                        friendEntity.setLocation(user.getLocation());
                                                        friendEntity.setDevice(user.getDevice());
                                                        friendEntity.setPhone(user.getPhone());
                                                        friendEntity.setWho_can_track(user.getWho_can_track());
                                                        friendsDatabase.friendDao().addUser(friendEntity);

                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {

                                                                Map<String,Object> map=new HashMap<>();
                                                                map.put("timestamp",ServerValue.TIMESTAMP);

                                                                FirebaseDatabase.getInstance().getReference().child("users")
                                                                        .child(userUid)
                                                                        .child("trackers")
                                                                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                                        .setValue(map, new DatabaseReference.CompletionListener() {
                                                                            @Override
                                                                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                                                                mDialog.dismiss();
                                                                                Toast.makeText(NewFriendActivity.this, "User added", Toast.LENGTH_SHORT).show();
                                                                                finish();
                                                                            }
                                                                        });

                                                            }
                                                        });

                                                    }
                                                });
                                                return;
                                            }

                                            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    final UserEntity user=userDatabase.userDao().getUserById(1);

                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {

                                                            String uid=FirebaseAuth.getInstance().getCurrentUser().getUid();

                                                            Map<String,Object> map=new HashMap<>();
                                                            map.put("data", uid +"/"+user.getName()+"/"+user.getImage());
                                                            map.put("timestamp", ServerValue.TIMESTAMP);

                                                            FirebaseDatabase.getInstance().getReference().child("users")
                                                                    .child(userUid)
                                                                    .child("requests")
                                                                    .child(uid)
                                                                    .updateChildren(map, new DatabaseReference.CompletionListener() {
                                                                        @Override
                                                                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                                                            if(databaseError!=null){
                                                                                databaseError.toException().printStackTrace();
                                                                                Toast.makeText(NewFriendActivity.this, "Error: "+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                return;
                                                                            }

                                                                            mDialog.dismiss();
                                                                            Toast.makeText(NewFriendActivity.this, "Track request sent", Toast.LENGTH_SHORT).show();
                                                                            finish();
                                                                        }
                                                                    });

                                                        }
                                                    });

                                                }
                                            });

                                        }})
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            e.printStackTrace();
                                            mDialog.dismiss();
                                            Toast.makeText(NewFriendActivity.this, "Error: "+e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });


                        }

                        }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    public void removeUser(View view) {

        user=null;
        remove.hide();
        save.setImageResource(R.drawable.ic_arrow_forward_black_24dp);

        pic.animate().alpha(0.0f).setDuration(250).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                pic.setVisibility(View.INVISIBLE);
            }
        }).start();


        name.setAlpha(0.0f);
        name.setText("Enter unique id");
        name.animate().alpha(1.0f).setDuration(150).start();

        unique_id.setEnabled(true);
        unique_id.setTextColor(getResources().getColor(R.color.colorAccent));
        unique_id.setLineColor(getResources().getColor(R.color.white));
        unique_id.setText("");

    }
}
