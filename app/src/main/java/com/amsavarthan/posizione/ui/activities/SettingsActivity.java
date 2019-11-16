package com.amsavarthan.posizione.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.receivers.ManageServiceReceiver;
import com.amsavarthan.posizione.room.fav.FavDatabase;
import com.amsavarthan.posizione.room.friends.FriendDatabase;
import com.amsavarthan.posizione.room.friends.FriendEntity;
import com.amsavarthan.posizione.room.user.UserDatabase;
import com.amsavarthan.posizione.room.user.UserEntity;
import com.amsavarthan.posizione.services.AccidentDetectionService;
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

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.amsavarthan.posizione.utils.Utils.isMyServiceRunning;

public class SettingsActivity extends AppCompatActivity {

    private RadioButton ll_r1,ll_r2,di_r1,di_r2,ps_r1,ps_r2,ps_r3;
    private SharedPreferences.Editor service_editor,speed_editor,privacy_editor;
    private TextView walk_speed_txt,jog_speed_txt,run_speed_txt,name,phone;
    private int walk_speed,jog_speed,run_speed;
    private String who_can_track;
    private CircleImageView pic;
    private UserDatabase userDatabase;
    private FirebaseAuth mAuth;
    private ProgressDialog mDialog;
    private FriendDatabase friendDatabase;
    public Switch aSwitch;
    static SettingsActivity instance;
    private FavDatabase favDatabase;

    public static SettingsActivity getInstance() {
        return instance;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if(MainActivity.instance==null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }else {
            super.onBackPressed();
        }
    }

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        instance=this;
        getSupportActionBar().setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        userDatabase=UserDatabase.getInstance(this);
        friendDatabase=FriendDatabase.getInstance(this);
        mAuth=FirebaseAuth.getInstance();
        favDatabase=FavDatabase.getInstance(this);

        aSwitch=findViewById(R.id.acci_switch);
        ll_r1=findViewById(R.id.ll_r1);
        ll_r2=findViewById(R.id.ll_r2);
        di_r1=findViewById(R.id.di_r1);
        di_r2=findViewById(R.id.di_r2);
        ps_r1=findViewById(R.id.ps_r1);
        ps_r2=findViewById(R.id.ps_r2);
        ps_r3=findViewById(R.id.ps_r3);
        walk_speed_txt=findViewById(R.id.walk_speed_txt);
        jog_speed_txt=findViewById(R.id.jog_speed_txt);
        run_speed_txt=findViewById(R.id.run_speed_txt);
        name=findViewById(R.id.name);
        phone=findViewById(R.id.phone);
        pic=findViewById(R.id.pic);
        TextView walk_speed_txtview = findViewById(R.id.walk_speed);
        final TextView jog_speed_txtview = findViewById(R.id.jog_speed);
        final TextView run_speed_txtview = findViewById(R.id.run_speed);

        privacy_editor=getSharedPreferences("privacy",MODE_PRIVATE).edit();
        service_editor=getSharedPreferences("service",MODE_PRIVATE).edit();
        speed_editor=getSharedPreferences("speeds",MODE_PRIVATE).edit();

        who_can_track=getSharedPreferences("privacy",MODE_PRIVATE).getString("who_can_track","2");

        walk_speed=getSharedPreferences("speeds",MODE_PRIVATE).getInt("walk_speed",10);
        jog_speed=getSharedPreferences("speeds",MODE_PRIVATE).getInt("jog_speed",35);
        run_speed=getSharedPreferences("speeds",MODE_PRIVATE).getInt("run_speed",75);
        boolean battery_saver = getSharedPreferences("service", MODE_PRIVATE).getBoolean("battery_saving", true);
        boolean update = getSharedPreferences("service", MODE_PRIVATE).getBoolean("update_device_info_using_service", false);

        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, final boolean b) {
                if (b) {
                    aSwitch.setText(getResources().getString(R.string.accident_detection_off));
                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {

                            final boolean isEmpty=favDatabase.favDao().getFavList().isEmpty();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(!isEmpty){
                                        startAccidentService();
                                    }else{
                                        Toast.makeText(SettingsActivity.this, "Cannot start service without favourites", Toast.LENGTH_SHORT).show();
                                        aSwitch.setChecked(false);
                                    }
                                }
                            });

                        }
                    });


                }else{
                    aSwitch.setText(getResources().getString(R.string.accident_detection_on));
                    stopAccidentService();
                }
            }
        });
        if(isMyServiceRunning(AccidentDetectionService.class,this)){
            aSwitch.setText(getResources().getString(R.string.accident_detection_off));
            aSwitch.setChecked(true);
        }else{
            aSwitch.setText(getResources().getString(R.string.accident_detection_on));
            aSwitch.setChecked(false);
        }

        mDialog=new ProgressDialog(this);
        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setIndeterminate(true);

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

        if(update){
            di_r2.setChecked(true);
        }else{
            di_r1.setChecked(true);
        }

        if(battery_saver){
            ll_r2.setChecked(true);
        }else{
            ll_r1.setChecked(true);
        }

        switch (who_can_track) {
            case "1":
                ps_r1.setChecked(true);
                break;
            case "2":
                ps_r2.setChecked(true);
                break;
            case "3":
                ps_r3.setChecked(true);
                break;
        }


        walk_speed_txt.setText(String.format("%s km/hr", String.valueOf(walk_speed)));
        jog_speed_txt.setText(String.format("%s km/hr", String.valueOf(jog_speed)));
        run_speed_txt.setText(String.format("%s km/hr", String.valueOf(run_speed)));

        String pass=getSharedPreferences("lock",MODE_PRIVATE).getString("password","0");
        if(!pass.equalsIgnoreCase("0")){

            ps_r1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(SettingsActivity.this, "Disable parent lock to change settings", Toast.LENGTH_SHORT).show();
                }
            });

            ps_r2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(SettingsActivity.this, "Disable parent lock to change settings", Toast.LENGTH_SHORT).show();
                }
            });

            ps_r3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(SettingsActivity.this, "Disable parent lock to change settings", Toast.LENGTH_SHORT).show();
                }
            });

            ps_r1.setEnabled(false);
            ps_r2.setEnabled(false);
            ps_r3.setEnabled(false);
        }else{
            ps_r1.setEnabled(true);
            ps_r2.setEnabled(true);
            ps_r3.setEnabled(true);

            ps_r1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                    if(b){

                        mDialog.setMessage("Please wait..");
                        mDialog.show();

                        Map<String,Object>map=new HashMap<>();
                        map.put("who_can_track","1");

                        FirebaseDatabase.getInstance().getReference().child("users")
                                .child(mAuth.getCurrentUser().getUid())
                                .updateChildren(map, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                        mDialog.dismiss();
                                        if(databaseError!=null){
                                            Toast.makeText(SettingsActivity.this, "Some error occurred: "+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        if(MainActivity.getInstance()!=null) {
                                            MainActivity.getInstance().finish();
                                            MainActivity.instance = null;
                                        }
                                        privacy_editor.putString("who_can_track","1").apply();

                                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                userDatabase.userDao().getUserById(1).setWho_can_track("1");
                                            }
                                        });

                                    }
                                });

                    }

                }
            });

            ps_r2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                    if(b){

                        mDialog.setMessage("Please wait..");
                        mDialog.show();

                        Map<String,Object>map=new HashMap<>();
                        map.put("who_can_track","2");

                        FirebaseDatabase.getInstance().getReference().child("users")
                                .child(mAuth.getCurrentUser().getUid())
                                .updateChildren(map, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                        mDialog.dismiss();
                                        if(databaseError!=null){
                                            Toast.makeText(SettingsActivity.this, "Some error occurred: "+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        if(MainActivity.getInstance()!=null) {
                                            MainActivity.getInstance().finish();
                                            MainActivity.instance = null;
                                        }
                                        privacy_editor.putString("who_can_track","2").apply();

                                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                userDatabase.userDao().getUserById(1).setWho_can_track("2");
                                            }
                                        });

                                    }
                                });

                    }

                }
            });

            ps_r3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                    if(b){

                        mDialog.setMessage("Please wait..");
                        mDialog.show();

                        Map<String,Object>map=new HashMap<>();
                        map.put("who_can_track","3");

                        FirebaseDatabase.getInstance().getReference().child("users")
                                .child(mAuth.getCurrentUser().getUid())
                                .updateChildren(map, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                        FirebaseDatabase.getInstance().getReference()
                                                .child("users")
                                                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                .child("trackers")
                                                .removeValue(new DatabaseReference.CompletionListener() {
                                                    @Override
                                                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                                        mDialog.dismiss();
                                                        if(databaseError!=null){
                                                            Toast.makeText(SettingsActivity.this, "Some error occurred: "+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                            return;
                                                        }

                                                        if(MainActivity.getInstance()!=null) {
                                                            MainActivity.getInstance().finish();
                                                            MainActivity.instance = null;
                                                        }
                                                        privacy_editor.putString("who_can_track","3").apply();
                                                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                userDatabase.userDao().getUserById(1).setWho_can_track("3");
                                                            }
                                                        });
                                                    }
                                                });


                                    }
                                });

                    }

                }
            });

        }


        walk_speed_txtview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new MaterialDialog.Builder(SettingsActivity.this)
                        .title("Walking Speed")
                        .positiveText("Ok")
                        .negativeText("Cancel")
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .input("Speed in km/hr", walk_speed_txt.getText().toString().replace(" km/hr",""), new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                String speed=input.toString().replace(" ","");

                                if(TextUtils.isEmpty(speed)){
                                    Toast.makeText(SettingsActivity.this, "Invalid speed", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if(Integer.parseInt(speed)>=100 && Integer.parseInt(speed)<=1){
                                    Toast.makeText(SettingsActivity.this, "Invalid speed", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                speed_editor.putInt("walk_speed",Integer.parseInt(speed)).apply();
                                walk_speed_txt.setText(String.format("%s km/hr",speed));
                            }
                        })
                        .show();

            }
        });

        jog_speed_txtview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new MaterialDialog.Builder(SettingsActivity.this)
                        .title("Jogging Speed")
                        .positiveText("Ok")
                        .negativeText("Cancel")
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .input("Speed in km/hr", jog_speed_txt.getText().toString().replace(" km/hr",""), new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                String speed=input.toString().replace(" ","");

                                if(TextUtils.isEmpty(speed)){
                                    Toast.makeText(SettingsActivity.this, "Invalid speed", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if(Integer.parseInt(speed)>=100 && Integer.parseInt(speed)<=1){
                                    Toast.makeText(SettingsActivity.this, "Invalid speed", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                speed_editor.putInt("jog_speed",Integer.parseInt(speed)).apply();
                                jog_speed_txt.setText(String.format("%s km/hr", speed));

                            }
                        })
                        .show();

            }
        });

        run_speed_txtview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new MaterialDialog.Builder(SettingsActivity.this)
                        .title("Running Speed")
                        .positiveText("Ok")
                        .negativeText("Cancel")
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .input("Speed in km/hr", run_speed_txt.getText().toString().replace(" km/hr",""), new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                String speed=input.toString().replace(" ","");

                                if(TextUtils.isEmpty(speed)){
                                    Toast.makeText(SettingsActivity.this, "Invalid speed", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if(Integer.parseInt(speed)>=100 && Integer.parseInt(speed)<=1){
                                    Toast.makeText(SettingsActivity.this, "Invalid speed", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                speed_editor.putInt("run_speed",Integer.parseInt(speed)).apply();
                                run_speed_txt.setText(String.format("%s km/hr", speed));

                            }
                        })
                        .show();

            }
        });

        ll_r1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    service_editor.putBoolean("battery_saving",false).apply();
                }
            }
        });

        ll_r2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    service_editor.putBoolean("battery_saving",true).apply();
                }
            }
        });

        di_r1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    service_editor.putBoolean("update_device_info_using_service",false).apply();
                }
            }
        });

        di_r2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    service_editor.putBoolean("update_device_info_using_service",true).apply();
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_reset:

                if(Utils.isOnline(this)) ps_r2.setChecked(true);

                Toast.makeText(this, "Changed to defaults", Toast.LENGTH_SHORT).show();

                speed_editor.putInt("walk_speed",10).apply();
                speed_editor.putInt("jog_speed",35).apply();
                speed_editor.putInt("run_speed",75).apply();
                service_editor.putBoolean("update_device_info_using_service",false).apply();
                service_editor.putBoolean("battery_saving",true).apply();

                di_r1.setChecked(true);
                di_r2.setChecked(false);
                ll_r2.setChecked(true);
                ll_r1.setChecked(false);

                walk_speed_txt.setText("10 km/hr");
                jog_speed_txt.setText("35 km/hr");
                run_speed_txt.setText("75 km/hr");

                break;
            default:
        }

        return super.onOptionsItemSelected(item);
    }

    public void openProfileSettings(View view) {

        ActivityOptionsCompat optionsCompat=ActivityOptionsCompat.makeSceneTransitionAnimation(SettingsActivity.this,pic,"image");
        startActivity(new Intent(getApplicationContext(), ProfileSettings.class),optionsCompat.toBundle());

    }

    private void stopService() {

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("com.amsavarthan.posizione.STOP");
        broadcastIntent.setClass(getApplicationContext(), ManageServiceReceiver.class);
        sendBroadcast(broadcastIntent);

    }

    private void startAccidentService() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("com.amsavarthan.posizione.accident_detector.START");
        broadcastIntent.setClass(getApplicationContext(), ManageServiceReceiver.class);
        sendBroadcast(broadcastIntent);
    }

    private void stopAccidentService() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("com.amsavarthan.posizione.accident_detector.STOP");
        broadcastIntent.setClass(getApplicationContext(), ManageServiceReceiver.class);
        sendBroadcast(broadcastIntent);
    }

    public void logout(View view) {

        if(!Utils.isOnline(this)){
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Logout")
                .setMessage("Are you sure do you want to logout from this device?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        mDialog.setMessage("Logging out...");
                        mDialog.show();

                        stopService();
                        stopAccidentService();

                        Map<String,Object> map=new HashMap<>();
                        map.put("token","");
                        FirebaseDatabase.getInstance().getReference().child("users")
                                .child(mAuth.getCurrentUser().getUid())
                                .updateChildren(map)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                UserEntity currentuser=userDatabase.userDao().getUserById(1);
                                                userDatabase.userDao().deleteUser(currentuser);
                                                for(FriendEntity friendEntity:friendDatabase.friendDao().getFriendsList()){
                                                    friendDatabase.friendDao().deleteUser(friendEntity);
                                                }
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        mAuth.signOut();
                                                        mDialog.dismiss();
                                                        startActivity(new Intent(getApplicationContext(),SplashScreen.class));
                                                        try {
                                                            MainActivity.getInstance().finish();
                                                        }catch (Exception e){
                                                            e.printStackTrace();
                                                        }
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
                                        e.printStackTrace();
                                        mDialog.dismiss();
                                        Toast.makeText(SettingsActivity.this, "Error: "+e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        AlertDialog alertDialog=builder.create();
        alertDialog.show();

    }
}