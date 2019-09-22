package com.amsavarthan.posizione.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.room.fav.FavDatabase;
import com.amsavarthan.posizione.room.fav.FavEntity;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class FavouritesRecyclerAdapter extends RecyclerView.Adapter<FavouritesRecyclerAdapter.MyViewHolder> {

    Context context;
    List<FavEntity> favEntities;
    FavDatabase favDatabase;

    public FavouritesRecyclerAdapter(Context context, List<FavEntity> favEntities) {
        this.context = context;
        this.favEntities = favEntities;
        favDatabase=FavDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public FavouritesRecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trackers,parent,false));

    }

    @Override
    public void onBindViewHolder(@NonNull final FavouritesRecyclerAdapter.MyViewHolder holder, int position) {

        final FavEntity favEntity=favEntities.get(position);

        holder.name.setText(favEntity.getName());
        holder.unique_id.setText(favEntity.getUnique_id());

        Glide.with(context)
                .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_invert))
                .asBitmap()
                .load(favEntity.getPic())
                .into(holder.pic);

        holder.revoke.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder=new AlertDialog.Builder(context);
                builder.setTitle("Remove")
                        .setMessage("Are you sure do want to remove "+favEntity.getName()+" from your favourites list?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                    @Override
                                    public void run() {

                                        favDatabase.favDao().deleteUser(favEntity);
                                        AppExecutors.getInstance().mainThread().execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(context, "Removed "+favEntity.getName()+" from favourites", Toast.LENGTH_SHORT).show();
                                                favEntities.remove(favEntity);
                                                notifyDataSetChanged();
                                            }
                                        });
                                    }
                                });

                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //do nothing
                            }
                        });

                AlertDialog alertDialog=builder.create();
                alertDialog.show();
            }
        });
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
        TextView name,unique_id;
        ImageView revoke;
        RelativeLayout item;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            item=itemView.findViewById(R.id.item);
            pic = itemView.findViewById(R.id.pic);
            name = itemView.findViewById(R.id.name);
            unique_id = itemView.findViewById(R.id.timestamp);
            revoke = itemView.findViewById(R.id.revoke);

        }
    }
}
