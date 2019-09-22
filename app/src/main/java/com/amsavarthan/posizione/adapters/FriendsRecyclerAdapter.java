package com.amsavarthan.posizione.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.amsavarthan.posizione.BuildConfig;
import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.room.fav.FavDatabase;
import com.amsavarthan.posizione.room.fav.FavEntity;
import com.amsavarthan.posizione.room.friends.FriendDatabase;
import com.amsavarthan.posizione.room.friends.FriendEntity;
import com.amsavarthan.posizione.ui.activities.PersonDetailView;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.amsavarthan.posizione.utils.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static com.amsavarthan.posizione.utils.Utils.dpTopx;

public class FriendsRecyclerAdapter extends RecyclerView.Adapter<FriendsRecyclerAdapter.MyViewHolder> {

    Context context;
    List<FriendEntity> friendEntities;
    FriendDatabase friendDatabase;
    FavDatabase favDatabase;

    public FriendsRecyclerAdapter(Context context, List<FriendEntity> friendEntities) {
        this.context = context;
        this.friendEntities = friendEntities;
        friendDatabase=FriendDatabase.getInstance(context);
        favDatabase=FavDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public FriendsRecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend,parent,false));

    }

    @Override
    public void onBindViewHolder(@NonNull final FriendsRecyclerAdapter.MyViewHolder holder, int position) {

        final FriendEntity friendEntity=friendEntities.get(position);
        //Last item
        if(friendEntities.size() > 1 && position == friendEntities.size() - 1){
            holder.itemView.setPadding(0,0,0, dpTopx(context,80));
        }

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                FavEntity favEntity=favDatabase.favDao().getFavByUniqueId(friendEntity.getUnique_id());
                if (favEntity!=null)
                {
                    holder.fav.setImageResource(R.drawable.ic_star_black_24dp);
                }else{
                    holder.fav.setImageResource(R.drawable.ic_star_border_black_24dp);
                }
            }
        });

        holder.name.setText(friendEntity.getName());
        holder.unique_id.setText(friendEntity.getUnique_id());

        Glide.with(context)
                .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art))
                .load(friendEntity.getPic())
                .into(holder.pic);

        holder.fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                boolean firstrun=context.getSharedPreferences("favourites",Context.MODE_PRIVATE).getBoolean("firstrun",true);

                if(firstrun){

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Note")
                            .setMessage("People you add to favourites are not added synced across your devices")
                            .setCancelable(true)
                            .setPositiveButton("Got it", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    context.getSharedPreferences("favourites",Context.MODE_PRIVATE)
                                            .edit().putBoolean("firstrun",false)
                                            .apply();
                                }
                            })
                            .setCancelable(false);
                    AlertDialog dialog = builder.create();
                    dialog.show();

                }

                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        FavEntity favEntity=favDatabase.favDao().getFavByUniqueId(friendEntity.getUnique_id());
                        if (favEntity==null)
                        {

                            FavEntity favourite=new FavEntity();
                            favourite.setName(friendEntity.getName());
                            favourite.setLocation(friendEntity.getLocation());
                            favourite.setPic(friendEntity.getPic());
                            favourite.setDevice(friendEntity.getDevice());
                            favourite.setUnique_id(friendEntity.getUnique_id());
                            favourite.setPhone(friendEntity.getPhone());
                            favourite.setWho_can_track(friendEntity.getWho_can_track());

                            favDatabase.favDao().addUser(favourite);
                            AppExecutors.getInstance().mainThread().execute(new Runnable() {
                                @Override
                                public void run() {
                                    holder.fav.setImageResource(R.drawable.ic_star_black_24dp);
                                    Toast.makeText(context, "Added to favourites", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        else
                        {
                            favDatabase.favDao().deleteUser(favEntity);
                            AppExecutors.getInstance().mainThread().execute(new Runnable() {
                                @Override
                                public void run() {
                                    holder.fav.setImageResource(R.drawable.ic_star_border_black_24dp);
                                    Toast.makeText(context, "Removed from favourites", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });

            }
        });

        holder.share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(TextUtils.isEmpty(friendEntity.getLocation())){
                    Intent intent=new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT,"This is "+friendEntity.getName()+"'s Posizione unique id : "+friendEntity.getUnique_id());
                    context.startActivity(Intent.createChooser(intent,"Share using"));
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Share Details")
                        .setMessage("What details do you want to share?")
                        .setCancelable(true)
                        .setNegativeButton("Location", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                final ProgressDialog progressDialog=new ProgressDialog(context);
                                progressDialog.setMessage("Please wait...");
                                progressDialog.setIndeterminate(true);
                                progressDialog.setCancelable(false);
                                progressDialog.setCanceledOnTouchOutside(false);

                                // lat/lng --> seperatedText[0] is lat , seperatedText[1] is lng
                                // seperatedText[2] is altitude, seperatedText[3] is speed, seperatedText[4] is status
                                final String[] seperatedText=friendEntity.getLocation().split("/");

                                if(Utils.isOnline(context)) {
                                    progressDialog.show();

                                    Glide.with(context)
                                            .asBitmap()
                                            .load(friendEntity.getPic())
                                            .into(new SimpleTarget<Bitmap>() {
                                                @Override
                                                public void onResourceReady(@NonNull final Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                                                    progressDialog.dismiss();
                                                    final View custom_view = ((LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.share_image_location, null);
                                                    ConstraintLayout layout = custom_view.findViewById(R.id.mainLayout);
                                                    TextView textView = custom_view.findViewById(R.id.text);
                                                    TextView city = custom_view.findViewById(R.id.city);

                                                    textView.setText(String.format("%s is at", friendEntity.getName()));

                                                    Geocoder geocoder=new Geocoder(context, Locale.getDefault());
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

                                    final View custom_view = ((LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.share_image_location, null);
                                    ConstraintLayout layout = custom_view.findViewById(R.id.mainLayout);
                                    TextView textView = custom_view.findViewById(R.id.text);
                                    TextView city = custom_view.findViewById(R.id.city);

                                    textView.setText(String.format("%s is at", friendEntity.getName()));
                                    Geocoder geocoder=new Geocoder(context, Locale.getDefault());
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

                                final ProgressDialog progressDialog=new ProgressDialog(context);
                                progressDialog.setMessage("Please wait...");
                                progressDialog.setIndeterminate(true);
                                progressDialog.setCancelable(false);
                                progressDialog.setCanceledOnTouchOutside(false);

                                if(Utils.isOnline(context)) {
                                    progressDialog.show();

                                    Glide.with(context)
                                            .asBitmap()
                                            .load(friendEntity.getPic())
                                            .into(new SimpleTarget<Bitmap>() {
                                                @Override
                                                public void onResourceReady(@NonNull final Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                                                    progressDialog.dismiss();
                                                    final View custom_view = ((LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.share_image_id, null);
                                                    ConstraintLayout layout = custom_view.findViewById(R.id.mainLayout);
                                                    TextView textView = custom_view.findViewById(R.id.text);
                                                    TextView id = custom_view.findViewById(R.id.unique_id);

                                                    textView.setText(String.format("This is %s's unique id", friendEntity.getName()));
                                                    id.setText(friendEntity.getUnique_id());

                                                    shareImage(getSharableBitmapFromView(layout, resource));

                                                }
                                            });
                                }else{

                                    final View custom_view = ((LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.share_image_id, null);
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
        });

        holder.item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Bundle bundle=new Bundle();
                bundle.putParcelable("person",friendEntity);
                context.startActivity(new Intent(context, PersonDetailView.class).putExtras(bundle));

            }
        });

        holder.item.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Remove")
                        .setMessage("Are you sure do you want to remove this user?")
                        .setCancelable(true)
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                if(!Utils.isOnline(context)){
                                    Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                final ProgressDialog mDialog=new ProgressDialog(context);
                                mDialog.setCancelable(false);
                                mDialog.setCanceledOnTouchOutside(false);
                                mDialog.setIndeterminate(true);
                                mDialog.setMessage("Syncing...");
                                mDialog.show();

                                FirebaseDatabase.getInstance().getReference()
                                        .child("users")
                                        .orderByChild(friendEntity.getUnique_id())
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                if(!dataSnapshot.exists()){
                                                    mDialog.dismiss();
                                                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            friendDatabase.friendDao().deleteUser(friendEntity);
                                                        }
                                                    });

                                                    friendEntities.remove(holder.getAdapterPosition());
                                                    notifyDataSetChanged();
                                                    Toast.makeText(context, "Account has been deleted", Toast.LENGTH_SHORT).show();
                                                    return;
                                                }

                                                for(DataSnapshot result:dataSnapshot.getChildren()){

                                                    if(!result.exists()){
                                                        mDialog.dismiss();
                                                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                friendDatabase.friendDao().deleteUser(friendEntity);
                                                            }
                                                        });

                                                        friendEntities.remove(holder.getAdapterPosition());
                                                        notifyDataSetChanged();
                                                        Toast.makeText(context, "Account has been deleted", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }

                                                    FirebaseDatabase.getInstance().getReference()
                                                            .child("users")
                                                            .child(result.getKey())
                                                            .child("trackers")
                                                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                            .removeValue(new DatabaseReference.CompletionListener() {
                                                                @Override
                                                                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                                                    if(databaseError!=null){
                                                                        mDialog.dismiss();
                                                                        Toast.makeText(context, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                                        return;
                                                                    }

                                                                    FirebaseDatabase.getInstance().getReference().child("users")
                                                                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                                            .child("friends")
                                                                            .child(friendEntity.getUnique_id())
                                                                            .removeValue(new DatabaseReference.CompletionListener() {
                                                                                @Override
                                                                                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                                                                    mDialog.dismiss();
                                                                                    if(databaseError!=null){
                                                                                        Toast.makeText(context, "Error: "+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                        databaseError.toException().printStackTrace();
                                                                                        return;
                                                                                    }

                                                                                    try {
                                                                                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                                                            @Override
                                                                                            public void run() {
                                                                                                friendDatabase.friendDao().deleteUser(friendEntity);
                                                                                            }
                                                                                        });

                                                                                        friendEntities.remove(holder.getAdapterPosition());
                                                                                    }catch (Exception e){
                                                                                        e.printStackTrace();
                                                                                    }
                                                                                    notifyDataSetChanged();
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
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
                return false;
            }
        });

    }

    //saves to cache
    //requires provider (see manifest)
    //requires a filepaths.xml (see res/xml)
    private void shareImage(Bitmap bitmap){

        try{
            File cachePath=new File(context.getCacheDir(),"images");
            cachePath.mkdirs();
            FileOutputStream stream=new FileOutputStream(cachePath+"/image.png");
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);
            stream.close();
        }catch (IOException e){
            e.printStackTrace();
        }

        File imagePath=new File(context.getCacheDir(),"images");
        File newFile=new File(imagePath,"image.png");
        Uri contentUri= FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID+".fileprovider",newFile);

        if(contentUri!=null){

            Intent intent=new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(contentUri,context.getContentResolver().getType(contentUri));
            intent.putExtra(Intent.EXTRA_STREAM,contentUri);
            intent.setType("image/png");
            context.startActivity(Intent.createChooser(intent,"Share using"));

        }

    }

    private void shareLocationImage(Bitmap bitmap,String latitude,String longitude){

        try{
            File cachePath=new File(context.getCacheDir(),"images");
            cachePath.mkdirs();
            FileOutputStream stream=new FileOutputStream(cachePath+"/image.png");
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);
            stream.close();
        }catch (IOException e){
            e.printStackTrace();
        }

        File imagePath=new File(context.getCacheDir(),"images");
        File newFile=new File(imagePath,"image.png");
        Uri contentUri= FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID+".fileprovider",newFile);

        if(contentUri!=null){

            Intent intent=new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(contentUri,context.getContentResolver().getType(contentUri));
            intent.putExtra(Intent.EXTRA_STREAM,contentUri);
            intent.putExtra(Intent.EXTRA_TEXT,"https://maps.google.com/maps?daddr="+latitude+","+longitude);
            intent.setType("image/png");
            context.startActivity(Intent.createChooser(intent,"Share using"));

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
        WindowManager windowManager=((AppCompatActivity)context).getWindowManager();
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
    public int getItemCount() {
        return friendEntities.size();
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
        TextView name,unique_id;
        ImageView share,fav;
        RelativeLayout item;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            item=itemView.findViewById(R.id.item);
            fav=itemView.findViewById(R.id.star);
            pic = itemView.findViewById(R.id.pic);
            name = itemView.findViewById(R.id.name);
            unique_id = itemView.findViewById(R.id.id);
            share = itemView.findViewById(R.id.share);

        }
    }
}
