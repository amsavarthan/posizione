package com.amsavarthan.posizione.adapters;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.room.fav.FavDatabase;
import com.amsavarthan.posizione.room.fav.FavEntity;
import com.amsavarthan.posizione.services.LocationService;
import com.amsavarthan.posizione.ui.activities.MainActivity;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
import static com.amsavarthan.posizione.ui.activities.MainActivity.getInstance;
import static com.amsavarthan.posizione.utils.Utils.isMyServiceRunning;


public class AcciContactsRecyclerAdapter extends RecyclerView.Adapter<AcciContactsRecyclerAdapter.MyViewHolder> {

    Context context;
    List<FavEntity> favEntities;

    public AcciContactsRecyclerAdapter(Context context, List<FavEntity> favEntities) {
        this.context = context;
        this.favEntities = favEntities;
    }

    @NonNull
    @Override
    public AcciContactsRecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_circle_user, parent, false));

    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {

        FavEntity favEntity = favEntities.get(position);
        holder.name.setText(favEntity.getName());
        Glide.with(context)
                .load(favEntity.getPic())
                .into(holder.pic);
        holder.pbar.setVisibility(View.GONE);


        requestSMSPermission(favEntity);

    }

    private void requestSMSPermission(final FavEntity favEntity) {
        Dexter.withActivity((AppCompatActivity)context)
                .withPermission(Manifest.permission.SEND_SMS)
                .withListener(new PermissionListener() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage("+917373072557",null,"Test message from Posizione",null,null);

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getInstance());
                        builder.setTitle("Permission required")
                                .setMessage("SMS permission has been denied permanently, please enable it")
                                .setIcon(R.drawable.ic_sms_failed_black_24dp)
                                .setCancelable(true)
                                .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Uri uri= Uri.fromParts("package",context.getPackageName(), null);
                                        ((AppCompatActivity)context).startActivityForResult(new Intent(ACTION_APPLICATION_DETAILS_SETTINGS).setData(uri),101);
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


    @Override
    public int getItemCount() {
        return favEntities.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        CircleImageView pic;
        TextView name;
        ProgressBar pbar;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            pic = itemView.findViewById(R.id.pic);
            name = itemView.findViewById(R.id.name);
            pbar=itemView.findViewById(R.id.pbar);

        }
    }
}
