package com.amsavarthan.posizione.adapters;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.models.Request;
import com.amsavarthan.posizione.models.Tracker;
import com.amsavarthan.posizione.models.User;
import com.amsavarthan.posizione.room.friends.FriendDatabase;
import com.amsavarthan.posizione.room.user.UserDatabase;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.amsavarthan.posizione.utils.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.Context.MODE_PRIVATE;

public class TrackersRecyclerAdapter extends RecyclerView.Adapter<TrackersRecyclerAdapter.MyViewHolder> {

    Context context;
    FriendDatabase friendDatabase;
    List<Tracker> trackersList;
    private UserDatabase userDatabase;
    private String unique_id;

    public TrackersRecyclerAdapter(Context context, List<Tracker> trackersList) {
        this.context = context;
        this.trackersList=trackersList;
        friendDatabase=FriendDatabase.getInstance(context);
        userDatabase=UserDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public TrackersRecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trackers,parent,false));

    }

    @Override
    public void onBindViewHolder(@NonNull final TrackersRecyclerAdapter.MyViewHolder holder, int position) {

        final Tracker tracker=trackersList.get(position);

        final ProgressDialog mDialog=new ProgressDialog(context);
        mDialog.setIndeterminate(true);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(false);
        mDialog.setMessage("Please wait..");

        FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(tracker.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        final User user=dataSnapshot.getValue(User.class);

                        Glide.with(context)
                                .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_invert))
                                .asBitmap()
                                .load(user.getImage())
                                .into(holder.pic);

                        holder.name.setText(user.getName());
                        holder.timestamp.setText(new PrettyTime().format(new Date(tracker.getTimestampAsLong())));

                        holder.revoke.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {


                                AlertDialog.Builder builder=new AlertDialog.Builder(context);
                                builder.setTitle("Revoke")
                                        .setMessage("Are you sure do want to revoke "+user.getName()+" from tracking you?")
                                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                mDialog.show();
                                                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        final String uniqueId=userDatabase.userDao().getUserById(1).getUnique_id();
                                                        ((AppCompatActivity)context).runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {

                                                                FirebaseDatabase.getInstance().getReference()
                                                                        .child("users")
                                                                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                                        .child("trackers")
                                                                        .child(tracker.getUid())
                                                                        .removeValue(new DatabaseReference.CompletionListener() {
                                                                            @Override
                                                                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                                                                if(databaseError!=null){
                                                                                    mDialog.dismiss();
                                                                                    Toast.makeText(context, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                    return;
                                                                                }

                                                                                FirebaseDatabase.getInstance().getReference()
                                                                                        .child("users")
                                                                                        .child(tracker.getUid())
                                                                                        .child("friends")
                                                                                        .child(uniqueId)
                                                                                        .removeValue(new DatabaseReference.CompletionListener() {
                                                                                            @Override
                                                                                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                                                                                mDialog.dismiss();

                                                                                                if(databaseError!=null){
                                                                                                    Toast.makeText(context, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                                    return;
                                                                                                }

                                                                                                trackersList.remove(tracker);
                                                                                                notifyDataSetChanged();

                                                                                            }
                                                                                        });

                                                                            }
                                                                        });


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
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }
    @Override
    public int getItemCount() {
        return trackersList.size();
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
        TextView name,timestamp;
        ImageView revoke;
        RelativeLayout item;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            item=itemView.findViewById(R.id.item);
            pic = itemView.findViewById(R.id.pic);
            name = itemView.findViewById(R.id.name);
            timestamp = itemView.findViewById(R.id.timestamp);
            revoke = itemView.findViewById(R.id.revoke);

        }
    }
}
