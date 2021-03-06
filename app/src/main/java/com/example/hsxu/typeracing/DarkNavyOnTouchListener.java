package com.example.hsxu.typeracing;

import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

/**
 * Created by hsxu on 11/7/2014.
 */
public class DarkNavyOnTouchListener implements View.OnTouchListener {
    private Button mButton;

    public DarkNavyOnTouchListener(Button button) {
        super();
        mButton = button;
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            mButton.setBackgroundColor(Color.parseColor("#6086ae"));
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            mButton.setBackgroundColor(Color.parseColor("#2c3e50"));
        }
        return false;
    }

}