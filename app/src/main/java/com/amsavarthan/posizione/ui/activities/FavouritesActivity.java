package com.amsavarthan.posizione.ui.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.adapters.FavouritesRecyclerAdapter;
import com.amsavarthan.posizione.adapters.TrackersRecyclerAdapter;
import com.amsavarthan.posizione.models.Tracker;
import com.amsavarthan.posizione.room.fav.FavDatabase;
import com.amsavarthan.posizione.room.fav.FavEntity;
import com.amsavarthan.posizione.utils.AppExecutors;
import com.amsavarthan.posizione.utils.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FavouritesActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;
    List<FavEntity> favEntities=new ArrayList<>();
    FavouritesRecyclerAdapter mAdapter;
    FirebaseAuth mAuth;
    FavDatabase favDatabase;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites);

        getSupportActionBar().setTitle("Favourites");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mAuth=FirebaseAuth.getInstance();
        favDatabase=FavDatabase.getInstance(this);
        recyclerView=findViewById(R.id.recyclerview);
        swipeRefreshLayout=findViewById(R.id.refreshLayout);

        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        layoutManager.setSmoothScrollbarEnabled(true);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);

        mAdapter=new FavouritesRecyclerAdapter(this,favEntities);
        recyclerView.setAdapter(mAdapter);
        findViewById(R.id.emptyLayout).setVisibility(View.GONE);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getFavourites();
            }
        });

        getFavourites();
        checkFavouritesIfAvailableAsFriend();

    }

    private void checkFavouritesIfAvailableAsFriend(){
        if(!Utils.isOnline(this)){
            return;
        }

        for(final FavEntity favEntity:favEntities){

            FirebaseDatabase.getInstance()
                    .getReference()
                    .child("users")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .child("friends")
                    .child(favEntity.getUnique_id())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(!dataSnapshot.exists()){
                                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        favDatabase.favDao().deleteUser(favEntity);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                favEntities.remove(favEntity);
                                                mAdapter.notifyDataSetChanged();
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

    }

    private void getFavourites() {

        swipeRefreshLayout.setRefreshing(true);
        favEntities.clear();
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {

                favEntities.addAll(favDatabase.favDao().getFavList());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        swipeRefreshLayout.setRefreshing(false);
                        if(favEntities.isEmpty()){
                            findViewById(R.id.emptyLayout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.emptyLayout).setAlpha(0.0f);
                            findViewById(R.id.emptyLayout).animate()
                                    .setDuration(400)
                                    .alpha(1.0f)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            findViewById(R.id.emptyLayout).setVisibility(View.VISIBLE);
                                        }
                                    })
                                    .start();
                            return;
                        }

                        mAdapter.notifyDataSetChanged();
                        checkFavouritesIfAvailableAsFriend();

                    }
                });

            }
        });

    }

}
