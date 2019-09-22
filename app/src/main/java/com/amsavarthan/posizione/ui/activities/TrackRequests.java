package com.amsavarthan.posizione.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.amsavarthan.posizione.models.Request;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TrackRequests extends AppCompatActivity{

    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;
    List<Request> requestList=new ArrayList<>();
    RequestsRecyclerAdapter mAdapter;
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
        setContentView(R.layout.activity_track_requests);

        getSupportActionBar().setTitle("Requests");
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

        mAdapter=new RequestsRecyclerAdapter(this,requestList);
        recyclerView.setAdapter(mAdapter);
        findViewById(R.id.emptyLayout).setVisibility(View.GONE);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getRequests();
            }
        });

        swipeRefreshLayout.setRefreshing(true);
        getRequests();
    }

    private void getRequests() {

        requestList.clear();
        FirebaseDatabase.getInstance().getReference().child("users")
                .child(mAuth.getCurrentUser().getUid())
                .child("requests")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        getSharedPreferences("Request",MODE_PRIVATE).edit().putString("count",String.valueOf(dataSnapshot.getChildrenCount())).apply();
                        requestList.clear();
                        swipeRefreshLayout.setRefreshing(false);
                        for(DataSnapshot requestData:dataSnapshot.getChildren()){
                            Request request=requestData.getValue(Request.class);
                            requestList.add(request);
                            mAdapter.notifyDataSetChanged();
                        }

                        if(requestList.isEmpty()){
                            findViewById(R.id.emptyLayout).setVisibility(View.VISIBLE);
                        }else{
                            findViewById(R.id.emptyLayout).setVisibility(View.GONE);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(TrackRequests.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }
}
