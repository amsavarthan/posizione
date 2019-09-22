package com.amsavarthan.posizione.ui.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.amsavarthan.posizione.R;
import com.amsavarthan.posizione.adapters.RequestsRecyclerAdapter;
import com.amsavarthan.posizione.adapters.TrackersRecyclerAdapter;
import com.amsavarthan.posizione.models.Tracker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageTrackers extends AppCompatActivity {

    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;
    List<Tracker> trackersList=new ArrayList<>();
    TrackersRecyclerAdapter mAdapter;
    FirebaseAuth mAuth;

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_trackers);

        getSupportActionBar().setTitle("Manage Trackers");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mAuth=FirebaseAuth.getInstance();
        recyclerView=findViewById(R.id.recyclerview);
        swipeRefreshLayout=findViewById(R.id.refreshLayout);

        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        layoutManager.setSmoothScrollbarEnabled(true);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);

        mAdapter=new TrackersRecyclerAdapter(this,trackersList);
        recyclerView.setAdapter(mAdapter);
        findViewById(R.id.emptyLayout).setVisibility(View.GONE);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                gettrackerss();
            }
        });

        gettrackerss();
    }

    private void gettrackerss() {

        swipeRefreshLayout.setRefreshing(true);
        trackersList.clear();
        FirebaseDatabase.getInstance().getReference().child("users")
                .child(mAuth.getCurrentUser().getUid())
                .child("trackers")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        trackersList.clear();
                        for(DataSnapshot trackerData:dataSnapshot.getChildren()){
                            Tracker tracker=trackerData.getValue(Tracker.class);
                            tracker.setUid(trackerData.getKey());
                            trackersList.add(tracker);
                            swipeRefreshLayout.setRefreshing(false);
                            mAdapter.notifyDataSetChanged();
                        }

                        if(trackersList.isEmpty()){
                            swipeRefreshLayout.setRefreshing(false);
                            findViewById(R.id.emptyLayout).setVisibility(View.VISIBLE);
                        }else{
                            swipeRefreshLayout.setRefreshing(false);
                            findViewById(R.id.emptyLayout).setVisibility(View.GONE);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(ManageTrackers.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }
}
