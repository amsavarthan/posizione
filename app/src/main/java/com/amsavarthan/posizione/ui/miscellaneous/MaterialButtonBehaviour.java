package com.amsavarthan.posizione.ui.miscellaneous;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

public class MaterialButtonBehaviour extends CoordinatorLayout.Behavior<MaterialButton> {
    private ValueAnimator valueAnimator = new ValueAnimator();
    private int           expandWidth;
    private int           collapseWidth;

    public MaterialButtonBehaviour() {
    }

    public MaterialButtonBehaviour(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull MaterialButton child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return true;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, MaterialButton child, View dependency) {

        if (expandWidth == 0 && child.getMeasuredWidth() != 0) {
            expandWidth = child.getMeasuredWidth();
        }

        if (collapseWidth == 0 && child.getMinWidth() != 0) {
            collapseWidth = child.getMinWidth();
        }

        return dependency instanceof RecyclerView;

    }


    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout,
                               MaterialButton child, View target, int dxConsumed,
                               int dyConsumed, int dxUnconsumed, int dy) {
        if (dyConsumed < 0) {
            //Scrolling down
            measure(child, 170, expandWidth);

        } else if (dyConsumed > 0) {
            // Scrolling up
            measure(child, 170, collapseWidth);


        }
    }

    private void measure(final View v, int duration, int width) {

        if (valueAnimator.isRunning()) {
            return;
        }


        int preWidth = v.getMeasuredWidth();
        valueAnimator.setIntValues(preWidth, width);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                v.getLayoutParams().width = (int) animation.getAnimatedValue();
                v.requestLayout();
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

}

