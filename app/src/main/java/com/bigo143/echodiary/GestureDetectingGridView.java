package com.bigo143.echodiary;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.GridView;

public class GestureDetectingGridView extends GridView {

    private GestureDetector gestureDetector;

    public GestureDetectingGridView(Context context) {
        super(context);
        init(context);
    }

    public GestureDetectingGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GestureDetectingGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    // Override to intercept touch events when a gesture is detected
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // If gestureDetector detects a gesture, intercept touch event here
        boolean gestureDetected = gestureDetector.onTouchEvent(ev);
        if (gestureDetected) {
            return true; // Intercept event, so onTouchEvent gets it
        }
        return super.onInterceptTouchEvent(ev);
    }

    // Override to handle the gesture
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.onTouchEvent(ev);
    }

    private void onSwipeLeft() {
        // Callback or event to notify swipe left
        if (swipeListener != null) swipeListener.onSwipeLeft();
    }

    private void onSwipeRight() {
        // Callback or event to notify swipe right
        if (swipeListener != null) swipeListener.onSwipeRight();
    }

    // Interface for swipe callbacks
    public interface OnSwipeListener {
        void onSwipeLeft();
        void onSwipeRight();
    }

    private OnSwipeListener swipeListener;

    public void setOnSwipeListener(OnSwipeListener listener) {
        this.swipeListener = listener;
    }
}
