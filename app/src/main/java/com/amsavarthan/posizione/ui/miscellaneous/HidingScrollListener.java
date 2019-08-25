package com.amsavarthan.posizione.ui.miscellaneous;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public abstract class HidingScrollListener extends RecyclerView.OnScrollListener {

    public boolean mControlsVisible=true;
    private int HIDE_THRESHOLD;
    private int mScrolledDistance=0;

    public HidingScrollListener(int threshold) {
        this.HIDE_THRESHOLD = threshold;
    }

    public HidingScrollListener() { }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        
        int firstVisibleItem=((LinearLayoutManager)recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        if(firstVisibleItem==0){
            if(!mControlsVisible){
                onShow();
                mControlsVisible=true;
            }
        }else {

            if (mScrolledDistance > HIDE_THRESHOLD && mControlsVisible) {
                onHide();
                mControlsVisible = false;
                mScrolledDistance = 0;
            } else if (mScrolledDistance < -HIDE_THRESHOLD && !mControlsVisible) {
                onShow();
                mControlsVisible = true;
                mScrolledDistance = 0;
            }
        }
        if((mControlsVisible&&dy>0)||(!mControlsVisible&&dy<0)){
            mScrolledDistance+=dy;
        }
        
    }

    protected abstract void onHide();

    protected abstract void onShow();

    public void resetScrollDistance(){
        
        mControlsVisible=true;
        mScrolledDistance=0;
        
    }
    
}
