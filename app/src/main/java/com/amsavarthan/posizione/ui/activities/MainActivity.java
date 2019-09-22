package com.amsavarthan.posizione.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amsavarthan.posizione.BuildConfig;
import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.models.User;
import com.amsavarthan.posizione.room.friends.FriendDatabase;
import com.amsavarthan.posizione.room.friends.FriendEntity;
import com.amsavarthan.posizione.receivers.ManageServiceReceiver;
import com.amsavarthan.posizione.services.LocationService;
import com.amsavarthan.posizione.adapters.FriendsRecyclerAdapter;
import com.amsavarthan.posizione.room.user.UserDatabase;
import com.amsavarthan.posizione.room.user.UserEntity;
import com.amsavarthan.posizione.ui.miscellaneous.HidingScrollListener;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.amsavarthan.posizione.utils.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.abara.library.batterystats.BatteryStats;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
import static com.amsavarthan.posizione.utils.Utils.isMyServiceRunning;

public class MainActivity extends AppCompatActivity implements ChildEventListener{

    TextView location_id;
    FirebaseAuth mAuth;
    public Switch location_switch;
    static MainActivity instance;
    UserDatabase userDatabase;
    UserEntity user;
    RecyclerView mRecyclerView;
    FriendsRecyclerAdapter mAdapter;
    List<FriendEntity> friendEntities;
    ConstraintLayout mainLayout;
    FriendDatabase friendDatabase;
    FloatingActionButton add;
    HidingScrollListener listener;
    Location location;
    String who_can_track;
    ImageView icon;
    TextView badge;
    View actionView;
    MenuItem requests_item,sync_item;
    SwipeRefreshLayout refreshLayout;
    String count;
    Chip lock;
    String pass;
    MenuItem trackers_item;

    public static MainActivity getInstance(){
        return instance;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(user!=null) {
            validateUserExistense();
        }
        checkPermission();
        if(getSharedPreferences("service",MODE_PRIVATE).getBoolean("update_device_info_using_service",true))
            updateDeviceInfo();

        count=getSharedPreferences("Request",MODE_PRIVATE).getString("count","0");

        pass=getSharedPreferences("lock",MODE_PRIVATE).getString("password","0");

        if(pass.equalsIgnoreCase("0")){
            lock.setText("Enable parental lock");
        }else{
            lock.setText("Disable parental lock");
        }

        if(isMyServiceRunning(LocationService.class,this)){
            if(pass.equalsIgnoreCase("0"))
            {
                location_switch.setEnabled(true);
            }else{
                location_switch.setEnabled(false);
            }
            location_switch.setChecked(true);
        }else{
            location_switch.setEnabled(true);
            location_switch.setChecked(false);
        }

        try{

            if(!count.equals("0")) {
                if (icon != null) {
                    icon.setImageResource(R.drawable.ic_notifications_24dp);
                }
                if (badge != null) {
                    badge.setVisibility(View.VISIBLE);
                }
            }else{
                if (icon != null) {
                    icon.setImageResource(R.drawable.ic_notifications_none_24dp);
                }
                if (badge != null) {
                    badge.setVisibility(View.INVISIBLE);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        getFriends();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance=this;
        mAuth = FirebaseAuth.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Utils.createNotificationChannels(this);
        }

        location_id = findViewById(R.id.location_id);
        add=findViewById(R.id.add);
        lock=findViewById(R.id.lock);
        refreshLayout=findViewById(R.id.refreshLayout);
        location_switch = findViewById(R.id.location_switch);
        mRecyclerView=findViewById(R.id.recycler_view);
        mainLayout=findViewById(R.id.mainLayout);

        friendEntities=new ArrayList<>();
        mAdapter=new FriendsRecyclerAdapter(this,friendEntities);

        userDatabase=UserDatabase.getInstance(getApplicationContext());
        friendDatabase= FriendDatabase.getInstance(getApplicationContext());
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {

                final String value=userDatabase.userDao().getUserById(1).getWho_can_track();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        who_can_track=getSharedPreferences("privacy",MODE_PRIVATE).getString("who_can_track",value);
                    }
                });
            }
        });

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                user = userDatabase.userDao().getUserById(1);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        location_id.setText(user.getUnique_id());
                    }
                });
            }
        });

        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        layoutManager.setSmoothScrollbarEnabled(true);

        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);


        final CardView cardView=findViewById(R.id.card);
        listener=new HidingScrollListener(1) {
            @Override
            protected void onHide() {
                cardView.animate().translationY(cardView.getHeight()).setInterpolator(new AccelerateInterpolator(1)).setDuration(210);
                add.animate().translationY(cardView.getHeight()-130).setInterpolator(new AccelerateInterpolator(1)).setDuration(240);
            }

            @Override
            protected void onShow() {
                listener.resetScrollDistance();
                cardView.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1)).setDuration(210);
                add.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1)).setDuration(240);

            }
        };
        mRecyclerView.addOnScrollListener(listener);

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ActivityOptionsCompat optionsCompat=ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this,add,"fab");
                startActivity(new Intent(getApplicationContext(),NewFriendActivity.class),optionsCompat.toBundle());

            }
        });

        location_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, final boolean b) {
                if (b) {
                    if (Utils.locationServicesStatusCheck(getInstance())){
                        startService();
                        Toast.makeText(getInstance(), "Live location turned on", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    stopService();
                    Toast.makeText(getInstance(), "Live location turned off", Toast.LENGTH_SHORT).show();
                }
            }
        });

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getFriends();
            }
        });

        lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityOptionsCompat optionsCompat=ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this,add,"fab");
                startActivity(new Intent(getApplicationContext(),ParentLockActivity.class),optionsCompat.toBundle());
            }
        });

    }

    private void validateUserExistense() {

        FirebaseDatabase.getInstance().getReference().child("users")
                .child(mAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final User data=dataSnapshot.getValue(User.class);

                        if(!user.getPhone().equals(data.getPhone())){

                            new MaterialDialog.Builder(instance)
                                    .title("Phone number changed")
                                    .content("It seems that the phone number linked with your account has been changed. Please verify to continue")
                                    .positiveText("Verify")
                                    .negativeText("Cancel")
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            startActivity(new Intent(instance, VerifyPhoneActivity.class).putExtra("phone", user.getPhone()));
                                                            finish();
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    })
                                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            finish();
                                        }
                                    })
                                    .canceledOnTouchOutside(false)
                                    .cancelable(false)
                                    .show();

                            return;

                        }

                        if(!user.getToken().equals(data.getToken())){
                            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                @Override
                                public void run() {
                                    userDatabase.userDao().deleteUser(userDatabase.userDao().getUserById(1));
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mAuth.signOut();
                                            Toast.makeText(MainActivity.this, "Authentication revoked", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(instance,WelcomeScreen.class));
                                            finish();
                                        }
                                    });
                                }
                            });

                        }

                        if(!user.getUnique_id().equals(data.getUnique_id())){
                            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                @Override
                                public void run() {
                                    UserEntity userEntity=userDatabase.userDao().getUserById(1);
                                    userEntity.setUnique_id(data.getUnique_id());
                                    userDatabase.userDao().updateUser(userEntity);
                                }
                            });
                        }

                        if(!user.getName().equals(data.getName())){
                            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                @Override
                                public void run() {
                                    UserEntity userEntity=userDatabase.userDao().getUserById(1);
                                    userEntity.setName(data.getName());
                                    userDatabase.userDao().updateUser(userEntity);
                                }
                            });
                        }

                        if(!user.getImage().equals(data.getImage())){
                            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                @Override
                                public void run() {
                                    UserEntity userEntity=userDatabase.userDao().getUserById(1);
                                    userEntity.setImage(data.getImage());
                                    userDatabase.userDao().updateUser(userEntity);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    private void startService() {
        Animation animation=AnimationUtils.loadAnimation(this,R.anim.slide_up);
        animation.setDuration(200);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                lock.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        lock.startAnimation(animation);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("com.amsavarthan.posizione.START");
        broadcastIntent.setClass(getApplicationContext(), ManageServiceReceiver.class);
        sendBroadcast(broadcastIntent);
    }

    private void stopService() {
        Animation animation=AnimationUtils.loadAnimation(this,R.anim.slide_down);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                lock.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        lock.startAnimation(animation);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("com.amsavarthan.posizione.STOP");
        broadcastIntent.setClass(getApplicationContext(), ManageServiceReceiver.class);
        sendBroadcast(broadcastIntent);
    }

    private void getFriends(){
        refreshLayout.setRefreshing(true);
        friendEntities.clear();

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final List<FriendEntity> friendEntityList=friendDatabase.friendDao().getFriendsList();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //When user is offline load data from ROOM
                        friendEntities.addAll(friendEntityList);

                        if(!Utils.isOnline(getApplicationContext())){

                            refreshLayout.setRefreshing(false);
                            Toast.makeText(MainActivity.this, "Couldn't sync with Posizione server", Toast.LENGTH_SHORT).show();

                            //Check for data in ROOM
                            if(friendEntities.isEmpty()) {

                                findViewById(R.id.emptyLayout).setVisibility(View.INVISIBLE);
                                findViewById(R.id.emptyLayout).setAlpha(0.0f);
                                findViewById(R.id.emptyLayout).animate()
                                        .alpha(1.0f)
                                        .setDuration(500)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                findViewById(R.id.emptyLayout).setVisibility(View.VISIBLE);
                                            }
                                        })
                                        .start();
                            }

                        }else{

                            //Check user from ROOM and update the ROOM
                            for(final FriendEntity friendEntity:friendEntities){

                                FirebaseDatabase.getInstance()
                                        .getReference()
                                        .child("users")
                                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .child("friends")
                                        .child(friendEntity.getUnique_id())
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if(!dataSnapshot.exists()){
                                                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            friendDatabase.friendDao().deleteUser(friendEntity);
                                                            friendEntities.remove(friendEntity);
                                                        }
                                                    });
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

                            }

                            //When user is online get data from FIREBASE
                            friendEntities.clear();

                            FirebaseDatabase.getInstance()
                                    .getReference()
                                    .child("users")
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .child("friends")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            if(!dataSnapshot.exists()){
                                                refreshLayout.setRefreshing(false);
                                            }

                                            for(final DataSnapshot unique_id:dataSnapshot.getChildren()){

                                                if(!unique_id.exists()){
                                                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            friendDatabase.friendDao().deleteUser(friendDatabase.friendDao().getFriendByUniqueId(unique_id.getKey()));
                                                            mAdapter.notifyDataSetChanged();
                                                        }
                                                    });
                                                    continue;
                                                }

                                                if(!unique_id.getValue(Boolean.class)){
                                                    continue;
                                                }

                                                FirebaseDatabase.getInstance().getReference()
                                                        .child("users")
                                                        .orderByChild("unique_id")
                                                        .equalTo(unique_id.getKey())
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                                if(dataSnapshot.exists()){
                                                                    for(DataSnapshot details:dataSnapshot.getChildren()){
                                                                        final User user=details.getValue(User.class);

                                                                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                                            @Override
                                                                            public void run() {

                                                                                final FriendEntity friendEntity;

                                                                                if(!user.getWho_can_track().equals("3")) {

                                                                                    //if tracking is enabled by user
                                                                                    if (friendDatabase.friendDao().getFriendByUniqueId(user.getUnique_id()) != null) {

                                                                                        //if user already in ROOM then update
                                                                                        friendEntity=friendDatabase.friendDao().getFriendByUniqueId(user.getUnique_id());
                                                                                        friendEntity.setName(user.getName());
                                                                                        friendEntity.setLocation(user.getLocation());
                                                                                        friendEntity.setPic(user.getImage());
                                                                                        friendEntity.setDevice(user.getDevice());
                                                                                        friendEntity.setUnique_id(user.getUnique_id());
                                                                                        friendEntity.setPhone(user.getPhone());
                                                                                        friendEntity.setWho_can_track(user.getWho_can_track());

                                                                                        friendDatabase.friendDao().updateUser(friendEntity);
                                                                                    } else {

                                                                                        //if user not in ROOM then add
                                                                                        friendEntity=new FriendEntity();
                                                                                        friendEntity.setName(user.getName());
                                                                                        friendEntity.setLocation(user.getLocation());
                                                                                        friendEntity.setPic(user.getImage());
                                                                                        friendEntity.setDevice(user.getDevice());
                                                                                        friendEntity.setUnique_id(user.getUnique_id());
                                                                                        friendEntity.setPhone(user.getPhone());
                                                                                        friendEntity.setWho_can_track(user.getWho_can_track());

                                                                                        friendDatabase.friendDao().addUser(friendEntity);
                                                                                    }
                                                                                }else{

                                                                                    //if tracking disabled by user then remove from ROOM and from firebase also
                                                                                   friendEntity=null;
                                                                                    FirebaseDatabase.getInstance()
                                                                                            .getReference()
                                                                                            .child("users")
                                                                                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                                                            .child("friends")
                                                                                            .child(user.getUnique_id())
                                                                                            .removeValue(new DatabaseReference.CompletionListener() {
                                                                                                @Override
                                                                                                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                                                                                    if(databaseError!=null){
                                                                                                        databaseError.toException().printStackTrace();
                                                                                                        return;
                                                                                                    }

                                                                                                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                                                                        @Override
                                                                                                        public void run() {
                                                                                                            if (friendDatabase.friendDao().getFriendByUniqueId(user.getUnique_id()) != null) {
                                                                                                                friendDatabase.friendDao().deleteUser(friendDatabase.friendDao().getFriendByUniqueId(user.getUnique_id()));
                                                                                                            }
                                                                                                        }
                                                                                                    });

                                                                                                }
                                                                                            });

                                                                                }

                                                                                runOnUiThread(new Runnable() {
                                                                                    @Override
                                                                                    public void run() {
                                                                                        if(!user.getWho_can_track().equals("3")) {
                                                                                            friendEntities.add(friendEntity);

                                                                                            findViewById(R.id.emptyLayout).animate()
                                                                                                    .alpha(0.0f)
                                                                                                    .setDuration(300)
                                                                                                    .setListener(new AnimatorListenerAdapter() {
                                                                                                        @Override
                                                                                                        public void onAnimationEnd(Animator animation) {
                                                                                                            super.onAnimationEnd(animation);
                                                                                                            findViewById(R.id.emptyLayout).setVisibility(View.GONE);
                                                                                                        }
                                                                                                    })
                                                                                                    .start();
                                                                                        }

                                                                                        mAdapter.notifyDataSetChanged();

                                                                                    }
                                                                                });

                                                                            }
                                                                        });

                                                                    }

                                                                }else{

                                                                    FirebaseDatabase.getInstance().getReference().child("users")
                                                                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                                            .child("friends")
                                                                            .child(unique_id.getKey())
                                                                            .removeValue();

                                                                }

                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                                            }
                                                        });

                                            }
                                            refreshLayout.setRefreshing(false);
                                            if(friendEntities.isEmpty()){
                                                findViewById(R.id.emptyLayout).setVisibility(View.INVISIBLE);
                                                findViewById(R.id.emptyLayout).setAlpha(0.0f);
                                                findViewById(R.id.emptyLayout).animate()
                                                        .alpha(1.0f)
                                                        .setDuration(300)
                                                        .setListener(new AnimatorListenerAdapter() {
                                                            @Override
                                                            public void onAnimationEnd(Animator animation) {
                                                                super.onAnimationEnd(animation);
                                                                findViewById(R.id.emptyLayout).setVisibility(View.VISIBLE);
                                                            }
                                                        })
                                                        .start();
                                            }

                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                        }

                    }
                });
            }
        });
    }

    private void checkPermission() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        location_switch.setEnabled(true);
                        location_switch.setChecked(isMyServiceRunning(LocationService.class, MainActivity.this));

                        getUserLocation();

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        location_switch.setEnabled(false);
                        AlertDialog.Builder builder = new AlertDialog.Builder(getInstance());
                        builder.setTitle("Permission required")
                                .setMessage("Location permission has been denied permanently, please enable to use this feature")
                                .setIcon(R.drawable.ic_location_off_black_24dp)
                                .setCancelable(true)
                                .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Uri uri= Uri.fromParts("package",getPackageName(), null);
                                        startActivityForResult(new Intent(ACTION_APPLICATION_DETAILS_SETTINGS).setData(uri),101);
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .check();


    }

    private void getUserLocation() {

        if(!Utils.locationServicesStatusCheck(this)){
            return;
        }

        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                checkPermission();
                return;
            }
        }

        locationManager.requestSingleUpdate(bestProvider, new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {

                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            UserEntity userEntity = userDatabase.userDao().getUserById(1);
                            userEntity.setLocation(location.getLatitude() + "/" + location.getLongitude() + "/" + location.getAltitude()+"/0.0/Idle/"+System.currentTimeMillis());
                            userDatabase.userDao().updateUser(userEntity);
                        }catch (NullPointerException e){
                            e.printStackTrace();
                        }
                    }
                });

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }

        }, getMainLooper());

        location = locationManager.getLastKnownLocation(bestProvider);
        if (location != null) {

            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    UserEntity userEntity=userDatabase.userDao().getUserById(1);
                    userEntity.setLocation(location.getLatitude() + "/" + location.getLongitude() + "/" + location.getAltitude()+"/0.0/Idle/"+System.currentTimeMillis());
                    userDatabase.userDao().updateUser(userEntity);
                }
            });

            Map<String,Object>map=new HashMap<>();
            map.put("location",location.getLatitude() + "/" + location.getLongitude() + "/" + location.getAltitude()+"/0.0/Idle/"+System.currentTimeMillis());

            FirebaseDatabase.getInstance().getReference().child("users")
                    .child(mAuth.getCurrentUser().getUid())
                    .updateChildren(map, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            Log.i("firebase","updated");
                        }
                    });

        }

    }

    public void shareText(View view) {

        final ProgressDialog progressDialog=new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        if(Utils.isOnline(getApplicationContext())) {
            progressDialog.show();

            Glide.with(getApplicationContext())
                    .asBitmap()
                    .load(user.getImage())
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull final Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                            progressDialog.dismiss();
                            final View custom_view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.share_image_id, null);
                            ConstraintLayout layout = custom_view.findViewById(R.id.mainLayout);
                            TextView textView = custom_view.findViewById(R.id.text);
                            TextView id = custom_view.findViewById(R.id.unique_id);

                            textView.setText("This is my unique id");
                            id.setText(user.getUnique_id());

                            shareImage(getSharableBitmapFromView(layout, resource));

                        }
                    });
        }else{

            final View custom_view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.share_image_id, null);
            ConstraintLayout layout = custom_view.findViewById(R.id.mainLayout);
            TextView textView = custom_view.findViewById(R.id.text);
            TextView id = custom_view.findViewById(R.id.unique_id);

            textView.setText("This is my unique id");
            id.setText(user.getUnique_id());

            shareImage(getSharableBitmapFromView(layout, null));

        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.menu_main,menu);

        trackers_item=menu.findItem(R.id.action_manage);
        requests_item=menu.findItem(R.id.action_requests);
        sync_item=menu.findItem(R.id.action_sync);
        actionView=requests_item.getActionView();

        icon=actionView.findViewById(R.id.notification_icon);
        badge=actionView.findViewById(R.id.badge_icon);
        icon.setImageResource(R.drawable.ic_notifications_none_24dp);
        badge.setVisibility(View.INVISIBLE);

        trackers_item.setVisible(true);
        if(who_can_track.equals("2")){
            requests_item.setVisible(true);
            sync_item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            FirebaseDatabase.getInstance().getReference().child("users")
                    .child(mAuth.getCurrentUser().getUid())
                    .child("requests")
                    .addChildEventListener(this);

            actionView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onOptionsItemSelected(requests_item);
                }
            });

        }else{
            if(who_can_track.equals("3")){
                trackers_item.setVisible(false);
            }
            requests_item.setVisible(false);
            sync_item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {

        switch (item.getItemId()){

            case R.id.action_sync:
                if(Utils.isOnline(this)) {

                    Toast.makeText(instance, "Syncing...", Toast.LENGTH_SHORT).show();
                    item.setEnabled(false);
                    Handler handler=new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            item.setEnabled(true);
                        }
                    },15000);

                    if (user != null) {
                        validateUserExistense();
                    }
                    updateDeviceInfo();
                    friendEntities.clear();
                    getFriends();
                }else{
                    Toast.makeText(instance, "Couldn't sync", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_requests:
                startActivity(new Intent(instance, TrackRequests.class));
                break;
            case R.id.action_fav:
                startActivity(new Intent(instance, FavouritesActivity.class));
                break;
            case R.id.action_manage:
                startActivity(new Intent(instance, ManageTrackers.class));
                break;
            case R.id.action_settings:
                startActivity(new Intent(instance, SettingsActivity.class));
                break;
            default:
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    private void updateDeviceInfo(){

        boolean update=getSharedPreferences("service",MODE_PRIVATE).getBoolean("update_device_info_using_service",false);
        if(update)return;

        //android version
        Field[]fields=Build.VERSION_CODES.class.getFields();
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

        final String device_info=device_name+"/"+android_version+"/"+batteryStats.getLevel()+"/"+status+"/"+timestamp;

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                UserEntity userEntity=userDatabase.userDao().getUserById(1);
                userEntity.setDevice(device_info);
                userDatabase.userDao().updateUser(userEntity);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Map<String,Object> map=new HashMap<>();
                        map.put("device",device_info);

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
                });
            }
        });

    }

    private void shareImage(Bitmap bitmap){

        try{
            File cachePath=new File(getCacheDir(),"images");
            cachePath.mkdirs();
            FileOutputStream stream=new FileOutputStream(cachePath+"/image.png");
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);
            stream.close();
        }catch (IOException e){
            e.printStackTrace();
        }

        File imagePath=new File(getCacheDir(),"images");
        File newFile=new File(imagePath,"image.png");
        Uri contentUri= FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID+".fileprovider",newFile);

        if(contentUri!=null){

            Intent intent=new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(contentUri,getContentResolver().getType(contentUri));
            intent.putExtra(Intent.EXTRA_STREAM,contentUri);
            intent.setType("image/png");
            startActivity(Intent.createChooser(intent,"Share using"));

        }

    }

    private Bitmap getSharableBitmapFromView(View view,Bitmap bitmap){

        CircleImageView pic = view.findViewById(R.id.pic);
        if(bitmap!=null) {
            pic.setImageBitmap(bitmap);
        }else{
            pic.setImageResource(R.mipmap.logo);
        }

        DisplayMetrics displayMetrics=new DisplayMetrics();
        WindowManager windowManager=getWindowManager();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        view.measure(View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.EXACTLY)
                ,View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.EXACTLY));
        view.layout(0,0,view.getMeasuredWidth(),view.getMeasuredHeight());

        Bitmap returnedBitmap=Bitmap.createBitmap(view.getMeasuredWidth(),view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(returnedBitmap);
        view.draw(canvas);
        return returnedBitmap;

    }

    @Override
    protected void onDestroy() {

        try {
            FirebaseDatabase.getInstance().getReference().child("users")
                    .child(mAuth.getCurrentUser().getUid())
                    .child("requests")
                    .removeEventListener(this);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            super.onDestroy();
        }

    }

    @Override
    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        getSharedPreferences("Request",MODE_PRIVATE).edit().putString("count",String.valueOf(dataSnapshot.getChildrenCount())).apply();

        count=getSharedPreferences("Request",MODE_PRIVATE).getString("count","0");
        try{

            if(count!="0") {
                if (icon != null) {
                    icon.setImageResource(R.drawable.ic_notifications_24dp);
                }
                if (badge != null) {
                    badge.setVisibility(View.VISIBLE);
                }
            }else{
                if (icon != null) {
                    icon.setImageResource(R.drawable.ic_notifications_none_24dp);
                }
                if (badge != null) {
                    badge.setVisibility(View.INVISIBLE);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }
}
