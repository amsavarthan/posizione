package com.amsavarthan.posizione.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amsavarthan.posizione.BuildConfig;
import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.models.User;
import com.amsavarthan.posizione.room.friends.FriendDatabase;
import com.amsavarthan.posizione.room.friends.FriendEntity;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.amsavarthan.posizione.utils.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
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
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.ocpsoft.prettytime.PrettyTime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.graphics.Color.WHITE;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN;

public class PersonDetailView extends FragmentActivity implements OnMapReadyCallback,LocationListener,ChildEventListener {

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private double c_latitude,c_longitude,u_latitude,u_longitude;
    private FriendEntity friendEntity;
    private FloatingActionButton fab;
    private boolean showCurrentLocation=true;
    private FriendDatabase friendDatabase;
    private BitmapDescriptor bitmap;
    private Marker marker;
    private Chip chip;
    private Animation slide_up,slide_down;
    private LinearLayout detailsLayout;
    private FloatingActionButton fullscreen;
    private boolean isFullscreen=false;
    private BottomSheetBehavior bottomSheetBehavior;
    private DisplayMetrics displayMetrics;
    private TextView name,unique_id,latlng_txt,city,device_name,android_version,battery_status,speed,user_status,t_location,t_device;
    private ImageView battery_icon;

    @Override
    public void onBackPressed() {
        if(bottomSheetBehavior.getState()==STATE_EXPANDED){
            bottomSheetBehavior.setState(STATE_HALF_EXPANDED);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_detail_view);

        friendEntity=getIntent().getParcelableExtra("person");
        friendDatabase=FriendDatabase.getInstance(this);

        fab=findViewById(R.id.fab);
        chip=findViewById(R.id.status);
        fullscreen=findViewById(R.id.fullscreen);
        detailsLayout=findViewById(R.id.detailsLayout);

        name=findViewById(R.id.name);
        unique_id=findViewById(R.id.unique_id);
        latlng_txt=findViewById(R.id.latlng);
        city=findViewById(R.id.city);
        device_name=findViewById(R.id.device_name);
        android_version=findViewById(R.id.device_android);
        battery_status=findViewById(R.id.device_charge);
        battery_icon=findViewById(R.id.battery_icon);
        t_location=findViewById(R.id.location_timestamp);
        t_device=findViewById(R.id.device_timestamp);

        chip.setText(String.format("Showing %s's location", friendEntity.getName()));
        checkPermission();

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        checkWhoCanTrack();

        String[] device_details = friendEntity.getDevice().split("/");
        try {
            String[] loc_details = friendEntity.getLocation().split("/");
            u_latitude = Double.valueOf(loc_details[0]);
            u_longitude = Double.valueOf(loc_details[1]);
            t_location.setText(String.format("Last updated : %s", new PrettyTime().format(new Date(Long.parseLong(loc_details[5])))));
            t_device.setText(String.format("Last updated : %s", new PrettyTime().format(new Date(Long.parseLong(device_details[4])))));
            getAndSetCityName(u_latitude,u_longitude);
        }catch (Exception e){
            finish();
            Toast.makeText(this, "No location updates found", Toast.LENGTH_SHORT).show();
        }

        name.setText(friendEntity.getName());
        unique_id.setText(String.format("Unique ID: %s", friendEntity.getUnique_id()));
        latlng_txt.setText(String.format("%s,%s", u_latitude, u_longitude));
        device_name.setText(device_details[0]);
        android_version.setText(device_details[1]);
        if(device_details[3].equals("Charging")) {
            battery_status.setText(String.format("%s • %s%%", device_details[3], device_details[2]));
        }else{
            battery_status.setText(String.format("%s%%", device_details[2]));
        }
        setBatteryIcon(Integer.parseInt(device_details[2]), device_details[3].equals("Charging"));

        slide_up= AnimationUtils.loadAnimation(this,R.anim.slide_up);
        slide_down= AnimationUtils.loadAnimation(this,R.anim.slide_down);

        displayMetrics=new DisplayMetrics();
        WindowManager windowManager=getWindowManager();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        bottomSheetBehavior=BottomSheetBehavior.from(detailsLayout);
        bottomSheetBehavior.setPeekHeight(displayMetrics.heightPixels/2);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {

                if(i==STATE_HIDDEN){
                    isFullscreen=true;
                    detailsLayout.setVisibility(View.GONE);
                    fullscreen.setImageDrawable(getResources().getDrawable(R.drawable.ic_fullscreen_exit_black_24dp));
                    mMap.getUiSettings().setAllGesturesEnabled(true);
                    LatLng latLng=new LatLng(u_latitude, u_longitude);
                    mMap.setPadding(0,0,0,0);
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().target(latLng).zoom(17).build()),1000,null);
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {

                fullscreen.setAlpha(1-v);
                chip.setAlpha(1-v);
                fab.setAlpha(1-v);

            }
        });

        mapFragment.getMapAsync(this);
    }

    private void checkWhoCanTrack() {
        switch (Integer.parseInt(friendEntity.getWho_can_track())){

            case 1:
                //every one is allowed to track this user
                break;
            case 2:
                //if already added remain added without asking permission
                break;
            case 3:
                //user changed to nobody can track so delete this user from current user friends list
                new MaterialDialog.Builder(PersonDetailView.this)
                        .title("Privacy enabled")
                        .content(String.format("%s has changed their privacy settings in such way that no one can track them and now they will be removed from your friends list",friendEntity.getName()))
                        .positiveText("Ok")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                                FirebaseDatabase.getInstance().getReference()
                                        .child("users")
                                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .child("friends")
                                        .child(friendEntity.getUnique_id())
                                        .removeValue(new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                                if(databaseError!=null){
                                                    databaseError.toException().printStackTrace();
                                                    Toast.makeText(PersonDetailView.this, "Error occurred: "+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                    return;
                                                }

                                                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        friendDatabase.friendDao().deleteUser(friendEntity);
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                finish();
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                        });

                            }
                        })
                        .cancelable(false)
                        .canceledOnTouchOutside(false)
                        .show();

        }
    }

    private void getAndSetCityName(double latitude, double longitude) {

        Geocoder geocoder=new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses=geocoder.getFromLocation(latitude,longitude,1);
            String address=String.format("%s, %s, %s",addresses.get(0).getLocality(), addresses.get(0).getAdminArea(), addresses.get(0).getCountryName());
            city.setText(address.replace("null, ",""));

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void setBatteryIcon(int charge,boolean charging) {

        if(charge<=10){
            if(charging){
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_charging_20_black_24dp));
            }else{
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_alert_24dp));
            }
        }else if(charge<=25){
            if(charging){
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_charging_20_black_24dp));
            }else{
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_20_black_24dp));
            }
        }else if(charge<=35){
            if(charging){
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_charging_30_black_24dp));
            }else{
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_30_black_24dp));
            }
        }else if(charge<=55){
            if(charging){
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_charging_50_black_24dp));
            }else{
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_50_black_24dp));
            }
        }else if(charge<=75){
            if(charging){
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_charging_60_black_24dp));
            }else{
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_60_black_24dp));
            }
        }else if(charge<=85){
            if(charging){
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_charging_80_black_24dp));
            }else{
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_80_black_24dp));
            }
        }else if(charge<=95){
            if(charging){
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_charging_90_black_24dp));
            }else{
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_90_black_24dp));
            }
        }else if(charge==100){
            if(charging){
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_charging_full_black_24dp));
            }else{
                battery_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_battery_full_black_24dp));
            }
        }

    }

    private void checkPermission() {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .check();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseDatabase.getInstance().getReference().child("users")
                .orderByChild("unique_id")
                .equalTo(friendEntity.getUnique_id())
                .removeEventListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        FirebaseDatabase.getInstance().getReference().child("users")
                .orderByChild("unique_id")
                .equalTo(friendEntity.getUnique_id())
                .removeEventListener(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        MapsInitializer.initialize(this);

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle));
            if (!success) {
                Log.e("Map", "map parse failed");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("Map", "can't find map");
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                checkPermission();
                return;
            }
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setAllGesturesEnabled(false);

        showCurrentLocation=true;
        chip.setText(String.format("Showing %s's location", friendEntity.getName()));
        fab.setImageResource(R.drawable.ic_my_location_black_24dp);
        bottomSheetBehavior.setState(STATE_HALF_EXPANDED);

        if(!isFullscreen){
            detailsLayout.setVisibility(View.VISIBLE);
            bottomSheetBehavior.setState(STATE_HALF_EXPANDED);
            fullscreen.setImageDrawable(getResources().getDrawable(R.drawable.ic_fullscreen_black_24dp));
            mMap.getUiSettings().setAllGesturesEnabled(false);
            mMap.setPadding(0,0,0,displayMetrics.heightPixels/2);
            LatLng latLng=new LatLng(u_latitude, u_longitude);
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().target(latLng).zoom(17).build()),1000,null);
        }else{
            detailsLayout.setVisibility(View.GONE);
            bottomSheetBehavior.setState(STATE_HIDDEN);
            fullscreen.setImageDrawable(getResources().getDrawable(R.drawable.ic_fullscreen_exit_black_24dp));
            mMap.getUiSettings().setAllGesturesEnabled(true);
            LatLng latLng=new LatLng(u_latitude, u_longitude);
            mMap.setPadding(0,0,0,0);
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().target(latLng).zoom(17).build()),1000,null);
        }

        FirebaseDatabase.getInstance().getReference().child("users")
                .orderByChild("unique_id")
                .equalTo(friendEntity.getUnique_id())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(final DataSnapshot details:dataSnapshot.getChildren()) {

                            final User user = details.getValue(User.class);
                            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                @Override
                                public void run() {
                                    friendEntity.setName(user.getName());
                                    friendEntity.setPic(user.getImage());
                                    friendEntity.setUnique_id(user.getUnique_id());
                                    friendEntity.setLocation(user.getLocation());
                                    friendEntity.setDevice(user.getDevice());
                                    friendDatabase.friendDao().updateUser(friendEntity);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            try {
                                                String[] loc_details = friendEntity.getLocation().split("/");
                                                u_latitude = Double.valueOf(loc_details[0]);
                                                u_longitude = Double.valueOf(loc_details[1]);
                                                t_location.setText(String.format("Last updated : %s", new PrettyTime().format(new Date(Long.parseLong(loc_details[5])))));
                                                getAndSetCityName(u_latitude,u_longitude);
                                            }catch (Exception e){
                                                e.printStackTrace();
                                            }

                                            String[] device_details=friendEntity.getDevice().split("/");

                                            try{
                                                t_device.setText(String.format("Last updated : %s", new PrettyTime().format(new Date(Long.parseLong(device_details[4])))));
                                            }catch (Exception e){
                                                e.printStackTrace();
                                            }
                                            if(device_details[3].equals("Charging")) {
                                                battery_status.setText(String.format("%s • %s%%", device_details[3], device_details[2]));
                                            }else{
                                                battery_status.setText(String.format("%s%%", device_details[2]));
                                            }
                                            setBatteryIcon(Integer.parseInt(device_details[2]), device_details[3].equals("Charging"));
                                            name.setText(friendEntity.getName());
                                            unique_id.setText(String.format("Unique ID: %s", friendEntity.getUnique_id()));
                                            latlng_txt.setText(String.format("%s,%s", u_latitude, u_longitude));
                                            device_name.setText(device_details[0]);
                                            android_version.setText(device_details[1]);

                                            loadCustomBitmapMarkerAndAddMarker();
                                        }
                                    });

                                }
                            });

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    private void loadCustomBitmapMarkerAndAddMarker() {

        if(mMap==null)return;

        Glide.with(getApplicationContext())
                .setDefaultRequestOptions(new RequestOptions().override(95,95))
                .asBitmap()
                .load(friendEntity.getPic())
                .into(new SimpleTarget<Bitmap>() {
                    @TargetApi(Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        View markerView=((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.map_marker,null);
                        bitmap=BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(markerView,resource));
                        updateFriendMarker(u_latitude,u_longitude);
                    }
                });


    }

    private void updateFriendMarker(double latitude, double longitude) {

        LatLng latlng = new LatLng(latitude, longitude);

        if(marker!=null) {
            marker.setPosition(latlng);
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().target(latlng).zoom(17).build()), 1000, null);
            return;
        }

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title(friendEntity.getName());
        markerOptions.flat(true);
        markerOptions.position(latlng);
        markerOptions.icon(bitmap);
        marker=mMap.addMarker(markerOptions);

    }

    private Bitmap getMarkerBitmapFromView(View view,Bitmap bitmap){

        CircleImageView pic=view.findViewById(R.id.pic);
        pic.setImageBitmap(bitmap);

        view.measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
        view.layout(0,0,view.getMeasuredWidth(),view.getMeasuredHeight());
        view.buildDrawingCache();

        Bitmap returnedBitmap=Bitmap.createBitmap(view.getMeasuredWidth(),view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(returnedBitmap);
        canvas.drawColor(WHITE, PorterDuff.Mode.SRC_IN);
        Drawable drawable= view.getBackground();
        if(drawable!=null){
            drawable.draw(canvas);
        }
        view.draw(canvas);
        return returnedBitmap;

    }

    private Bitmap getSharableBitmapFromView(View view,Bitmap bitmap){

        CircleImageView pic = view.findViewById(R.id.pic);
        if(bitmap!=null) {
            pic.setImageBitmap(bitmap);
        }else{
            pic.setImageResource(R.mipmap.logo);
        }

        view.measure(View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.EXACTLY)
                ,View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.EXACTLY));
        view.layout(0,0,view.getMeasuredWidth(),view.getMeasuredHeight());

        Bitmap returnedBitmap=Bitmap.createBitmap(view.getMeasuredWidth(),view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(returnedBitmap);
        view.draw(canvas);
        return returnedBitmap;

    }

    public void getMyLocation(View view) {

        if(showCurrentLocation) {

            showCurrentLocation=false;
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.showing_your_location), Toast.LENGTH_SHORT).show();
            fab.setImageResource(R.drawable.ic_my_location_24dp);

            LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true));
            boolean isGPSEnable = locationManager.isProviderEnabled(bestProvider);

            if (!isGPSEnable) {
                if (Utils.locationServicesStatusCheck(getApplicationContext())) getMyLocation(view);
            }

            Location location = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    checkPermission();
                    return;
                }
            }
            locationManager.requestSingleUpdate(bestProvider, this, getMainLooper());
            location = locationManager.getLastKnownLocation(bestProvider);
            if (location != null) {
                Log.i("location", location.getLatitude() + "/" + location.getLongitude());
                c_latitude = location.getLatitude();
                c_longitude = location.getLongitude();
                LatLng latLng=new LatLng(c_latitude, c_longitude);
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().target(latLng).zoom(17).build()),1000,null);
            }

        }else{

            showCurrentLocation=true;
            Toast.makeText(getApplicationContext(), String.format("Showing %s's location", friendEntity.getName()), Toast.LENGTH_SHORT).show();
            chip.setText(String.format("Showing %s's location", friendEntity.getName()));
            fab.setImageResource(R.drawable.ic_my_location_black_24dp);

            updateFriendMarker(u_latitude,u_longitude);

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        FirebaseDatabase.getInstance().getReference().child("users")
                .orderByChild("unique_id")
                .equalTo(friendEntity.getUnique_id())
                .addChildEventListener(this);

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        c_latitude = location.getLatitude();
        c_longitude = location.getLongitude();
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        if(!dataSnapshot.exists()){
            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    friendDatabase.friendDao().deleteUser(friendEntity);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(PersonDetailView.this, "Account has been deleted", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
            });
            return;
        }

        final User user = dataSnapshot.getValue(User.class);

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                friendEntity.setName(user.getName());
                friendEntity.setPic(user.getImage());
                friendEntity.setUnique_id(user.getUnique_id());
                friendEntity.setLocation(user.getLocation());
                friendEntity.setDevice(user.getDevice());
                friendEntity.setWho_can_track(user.getWho_can_track());
                friendDatabase.friendDao().updateUser(friendEntity);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        checkWhoCanTrack();
                        String[] loc_details=friendEntity.getLocation().split("/");
                        String[] device_details=friendEntity.getDevice().split("/");
                        u_latitude=Double.valueOf(loc_details[0]);
                        u_longitude=Double.valueOf(loc_details[1]);

                        if(device_details[3].equals("Charging")) {
                            battery_status.setText(String.format("%s • %s%%", device_details[3], device_details[2]));
                        }else{
                            battery_status.setText(String.format("%s%%", device_details[2]));
                        }
                        try{
                            t_location.setText(String.format("Last updated : %s", new PrettyTime().format(new Date(Long.parseLong(loc_details[5])))));
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                        try{
                            t_device.setText(String.format("Last updated : %s", new PrettyTime().format(new Date(Long.parseLong(device_details[4])))));
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        setBatteryIcon(Integer.parseInt(device_details[2]), device_details[3].equals("Charging"));
                        unique_id.setText(String.format("Unique ID: %s", friendEntity.getUnique_id()));
                        name.setText(friendEntity.getName());
                        latlng_txt.setText(String.format("%s,%s", u_latitude, u_longitude));
                        getAndSetCityName(u_latitude,u_longitude);
                        device_name.setText(device_details[0]);
                        android_version.setText(device_details[1]);

                        if(mMap!=null) {
                            if (bitmap != null)
                                updateFriendMarker(u_latitude, u_longitude);
                        }
                    }
                });

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
    @Override
    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

    }
    @Override
    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

        if(!dataSnapshot.exists()){
            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    friendDatabase.friendDao().deleteUser(friendEntity);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(PersonDetailView.this, "Account has been deleted", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
            });
        }

    }
    @Override
    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

    }
    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }

    public void goFullscreen(View view) {

        showCurrentLocation = true;
        chip.setText(String.format("Showing %s's location", friendEntity.getName()));
        fab.setImageResource(R.drawable.ic_my_location_black_24dp);

        updateFriendMarker(u_latitude, u_longitude);

        if (isFullscreen) {
            isFullscreen = false;
            bottomSheetBehavior.setState(STATE_HALF_EXPANDED);
            fullscreen.setImageDrawable(getResources().getDrawable(R.drawable.ic_fullscreen_black_24dp));
            slide_up.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    detailsLayout.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mMap.getUiSettings().setAllGesturesEnabled(false);
            detailsLayout.startAnimation(slide_up);
            mMap.setPadding(0,0,0,displayMetrics.heightPixels/2);
            LatLng latLng = new LatLng(u_latitude, u_longitude);
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().target(latLng).zoom(17).build()), 1000, null);
        } else {
            isFullscreen = true;
            bottomSheetBehavior.setState(STATE_HIDDEN);
            fullscreen.setImageDrawable(getResources().getDrawable(R.drawable.ic_fullscreen_exit_black_24dp));
            slide_down.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    detailsLayout.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mMap.getUiSettings().setAllGesturesEnabled(true);
            LatLng latLng = new LatLng(u_latitude, u_longitude);
            mMap.setPadding(0, 0, 0, 0);
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().target(latLng).zoom(17).build()), 1000, null);
            detailsLayout.startAnimation(slide_down);
        }

    }

    public void shareAction(View view) {

        if(TextUtils.isEmpty(friendEntity.getLocation())){
            Intent intent=new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT,"This is "+friendEntity.getName()+"'s Posizione unique id : "+friendEntity.getUnique_id());
            startActivity(Intent.createChooser(intent,"Share using"));
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(PersonDetailView.this);
        builder.setTitle("Share Details")
                .setMessage("What details do you want to share?")
                .setCancelable(true)
                .setNegativeButton("Location", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        final ProgressDialog progressDialog=new ProgressDialog(PersonDetailView.this);
                        progressDialog.setMessage("Please wait...");
                        progressDialog.setIndeterminate(true);
                        progressDialog.setCancelable(false);
                        progressDialog.setCanceledOnTouchOutside(false);

                        // lat/lng --> seperatedText[0] is lat , seperatedText[1] is lng
                        // seperatedText[2] is altitude, seperatedText[3] is speed, seperatedText[4] is status
                        final String[] seperatedText=friendEntity.getLocation().split("/");

                        if(Utils.isOnline(getApplicationContext())) {
                            progressDialog.show();

                            Glide.with(getApplicationContext())
                                    .asBitmap()
                                    .load(friendEntity.getPic())
                                    .into(new SimpleTarget<Bitmap>() {
                                        @Override
                                        public void onResourceReady(@NonNull final Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                                            progressDialog.dismiss();
                                            final View custom_view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.share_image_location, null);
                                            ConstraintLayout layout = custom_view.findViewById(R.id.mainLayout);
                                            TextView textView = custom_view.findViewById(R.id.text);
                                            TextView city = custom_view.findViewById(R.id.city);

                                            textView.setText(String.format("%s is at", friendEntity.getName()));

                                            Geocoder geocoder=new Geocoder(getApplicationContext(), Locale.getDefault());
                                            try {
                                                List<Address> addresses=geocoder.getFromLocation(Double.parseDouble(seperatedText[0]),Double.parseDouble(seperatedText[1]),1);
                                                if(!addresses.get(0).getSubLocality().equals("null")) {
                                                    city.setText(String.format("%s, %s", addresses.get(0).getSubLocality(), addresses.get(0).getLocality()));
                                                }else {
                                                    city.setText(String.format("%s", addresses.get(0).getLocality()));
                                                }
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                            shareLocationImage(getSharableBitmapFromView(layout, resource),seperatedText[0],seperatedText[1]);

                                        }
                                    });
                        }else{

                            final View custom_view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.share_image_location, null);
                            ConstraintLayout layout = custom_view.findViewById(R.id.mainLayout);
                            TextView textView = custom_view.findViewById(R.id.text);
                            TextView city = custom_view.findViewById(R.id.city);

                            textView.setText(String.format("%s is at", friendEntity.getName()));
                            Geocoder geocoder=new Geocoder(getApplicationContext(), Locale.getDefault());
                            try {
                                List<Address> addresses=geocoder.getFromLocation(Double.parseDouble(seperatedText[0]),Double.parseDouble(seperatedText[1]),1);
                                if(!addresses.get(0).getSubLocality().equals("null")) {
                                    city.setText(String.format("%s, %s", addresses.get(0).getSubLocality(), addresses.get(0).getLocality()));
                                }else {
                                    city.setText(String.format("%s", addresses.get(0).getLocality()));
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            shareLocationImage(getSharableBitmapFromView(layout, null),seperatedText[0],seperatedText[1]);


                        }

                    }
                })
                .setPositiveButton("ID", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        final ProgressDialog progressDialog=new ProgressDialog(PersonDetailView.this);
                        progressDialog.setMessage("Please wait...");
                        progressDialog.setIndeterminate(true);
                        progressDialog.setCancelable(false);
                        progressDialog.setCanceledOnTouchOutside(false);

                        if(Utils.isOnline(getApplicationContext())) {
                            progressDialog.show();

                            Glide.with(getApplicationContext())
                                    .asBitmap()
                                    .load(friendEntity.getPic())
                                    .into(new SimpleTarget<Bitmap>() {
                                        @Override
                                        public void onResourceReady(@NonNull final Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                                            progressDialog.dismiss();
                                            final View custom_view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.share_image_id, null);
                                            ConstraintLayout layout = custom_view.findViewById(R.id.mainLayout);
                                            TextView textView = custom_view.findViewById(R.id.text);
                                            TextView id = custom_view.findViewById(R.id.unique_id);

                                            textView.setText(String.format("This is %s's unique id", friendEntity.getName()));
                                            id.setText(friendEntity.getUnique_id());

                                            shareImage(getSharableBitmapFromView(layout, resource));

                                        }
                                    });
                        }else{

                            final View custom_view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.share_image_id, null);
                            ConstraintLayout layout = custom_view.findViewById(R.id.mainLayout);
                            TextView textView = custom_view.findViewById(R.id.text);
                            TextView id = custom_view.findViewById(R.id.unique_id);

                            textView.setText(String.format("This is %s's unique id", friendEntity.getName()));
                            id.setText(friendEntity.getUnique_id());

                            shareImage(getSharableBitmapFromView(layout, null));

                        }

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void shareImage(Bitmap bitmap){

        try{
            File cachePath=new File(this.getCacheDir(),"images");
            cachePath.mkdirs();
            FileOutputStream stream=new FileOutputStream(cachePath+"/image.png");
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);
            stream.close();
        }catch (IOException e){
            e.printStackTrace();
        }

        File imagePath=new File(this.getCacheDir(),"images");
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
    private void shareLocationImage(Bitmap bitmap,String latitude,String longitude){

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
            intent.putExtra(Intent.EXTRA_TEXT,"https://maps.google.com/maps?daddr="+latitude+","+longitude);
            intent.setType("image/png");
            startActivity(Intent.createChooser(intent,"Share using"));

        }

    }


}
