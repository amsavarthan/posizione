package com.amsavarthan.posizione.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.models.Request;
import com.amsavarthan.posizione.room.friends.FriendDatabase;
import com.amsavarthan.posizione.room.user.UserDatabase;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.amsavarthan.posizione.utils.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import static android.content.Context.MODE_PRIVATE;

public class RequestsRecyclerAdapter extends RecyclerView.Adapter<RequestsRecyclerAdapter.MyViewHolder> {

    Context context;
    FriendDatabase friendDatabase;
    List<Request> requestList;
    private UserDatabase userDatabase;
    private String unique_id;

    public RequestsRecyclerAdapter(Context context,List<Request> requestList) {
        this.context = context;
        this.requestList=requestList;
        friendDatabase=FriendDatabase.getInstance(context);
        userDatabase=UserDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public RequestsRecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request,parent,false));

    }

    @Override
    public void onBindViewHolder(@NonNull final RequestsRecyclerAdapter.MyViewHolder holder, int position) {

        final Request request=requestList.get(position);
        final String[] data=request.getData().split("/");
        final String userId=data[0];
        final String name=data[1];
        String image=request.getData().replace(userId+"/"+name+"/","");

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                unique_id=userDatabase.userDao().getUserById(1).getUnique_id();
            }
        });

        final ProgressDialog mDialog=new ProgressDialog(context);
        mDialog.setIndeterminate(true);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(false);
        mDialog.setMessage("Please wait..");

        holder.name.setText(name);
        holder.timestamp.setText(new PrettyTime().format(new Date(request.getTimestampAsLong())));

        Glide.with(context)
                .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_invert))
                .asBitmap()
                .load(image)
                .into(holder.pic);

        holder.accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //remove request document from current user and change value to true at @userId document

                new MaterialDialog.Builder(context)
                        .title("Accept")
                        .content("Are you sure do you want to allow "+name+" to track you?")
                        .positiveText("yes")
                        .negativeText("no")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                                if(!Utils.isOnline(context)){
                                    Toast.makeText(context, "No connection", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                mDialog.show();
                                FirebaseDatabase.getInstance().getReference()
                                        .child("users")
                                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .child("requests")
                                        .child(userId)
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
                                                        .child(userId)
                                                        .child("friends")
                                                        .child(unique_id)
                                                        .setValue(true, new DatabaseReference.CompletionListener() {
                                                            @Override
                                                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                                                Map<String,Object> map=new HashMap<>();
                                                                map.put("timestamp", ServerValue.TIMESTAMP);
                                                                FirebaseDatabase.getInstance().getReference()
                                                                        .child("users")
                                                                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                                        .child("trackers")
                                                                        .child(userId)
                                                                        .setValue(map, new DatabaseReference.CompletionListener() {
                                                                            @Override
                                                                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                                                                mDialog.dismiss();
                                                                                if(databaseError!=null){
                                                                                    Toast.makeText(context, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                    return;
                                                                                }

                                                                                requestList.remove(request);
                                                                                notifyDataSetChanged();

                                                                                context.getSharedPreferences("Request",MODE_PRIVATE).edit().putString("count",String.valueOf(requestList.size())).apply();
                                                                                Toast.makeText(context, "Request accepted", Toast.LENGTH_SHORT).show();


                                                                            }
                                                                        });

                                                            }
                                                        });

                                            }
                                        });

                            }
                        })
                        .show();
            }
        });

        holder.reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //remove request document from current user

                new MaterialDialog.Builder(context)
                        .title("Reject")
                        .content("Are you sure do you want to reject "+name+" 's request?")
                        .positiveText("yes")
                        .negativeText("no")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                                if(!Utils.isOnline(context)){
                                    Toast.makeText(context, "No connection", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                mDialog.show();
                                FirebaseDatabase.getInstance().getReference()
                                        .child("users")
                                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .child("requests")
                                        .child(userId)
                                        .removeValue(new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                                if(databaseError!=null){
                                                    mDialog.dismiss();
                                                    Toast.makeText(context, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                    return;
                                                }

                                                FirebaseDatabase.getInstance().getReference().child("users")
                                                        .child(userId)
                                                        .child("friends")
                                                        .child(unique_id)
                                                        .removeValue(new DatabaseReference.CompletionListener() {
                                                            @Override
                                                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                                                mDialog.dismiss();
                                                                if(databaseError!=null){
                                                                    Toast.makeText(context, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    return;
                                                                }

                                                                requestList.remove(request);
                                                                notifyDataSetChanged();
                                                                context.getSharedPreferences("Request",MODE_PRIVATE).edit().putString("count",String.valueOf(requestList.size())).apply();
                                                                Toast.makeText(context, "Request declined", Toast.LENGTH_SHORT).show();

                                                            }
                                                        });
                                            }
                                        });

                            }
                        })
                        .show();

            }
        });

    }
    @Override
    public int getItemCount() {
        return requestList.size();
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
        ImageView accept,reject;
        RelativeLayout item;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            item=itemView.findViewById(R.id.item);
            pic = itemView.findViewById(R.id.pic);
            name = itemView.findViewById(R.id.name);
            timestamp = itemView.findViewById(R.id.timestamp);
            accept = itemView.findViewById(R.id.accept);
            reject = itemView.findViewById(R.id.reject);

        }
    }
}
