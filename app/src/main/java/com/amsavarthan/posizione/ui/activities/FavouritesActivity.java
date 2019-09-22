package com.amsavarthan.posizione.ui.activities;

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
import com.google.firebase.auth.FirebaseAuth;

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

                    }
                });

            }
        });

    }

}
