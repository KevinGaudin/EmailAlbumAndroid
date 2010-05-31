package com.kg.emailalbum.mobile.viewer;

import android.view.MotionEvent;
import android.view.View;

public class SimpleZoomListener implements View.OnTouchListener {

    public enum ControlType {
        PAN, ZOOM
    }

    private ControlType mControlType = ControlType.ZOOM;

    private float mX;
    private float mY;

    /** Zoom control to manipulate */
    private BasicZoomControl mZoomControl;

    /** X-coordinate of latest down event */
    private float mDownX;

    /** Y-coordinate of latest down event */
    private float mDownY;

    /**
     * Sets the zoom control to manipulate
     * 
     * @param control
     *            Zoom control
     */
    public void setZoomControl(BasicZoomControl control) {
        mZoomControl = control;
    }

    public void setControlType(ControlType controlType) {
        mControlType = controlType;
    }

    public ControlType getControlType() {
        return mControlType;
    }

    public boolean onTouch(View v, MotionEvent event) {
        final int action = event.getAction();
        final float x = event.getX();
        final float y = event.getY();

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mDownX = x;
            mDownY = y;
            mX = x;
            mY = y;
            return false;

        case MotionEvent.ACTION_MOVE: {
            final float dx = (x - mX) / v.getWidth();
            final float dy = (y - mY) / v.getHeight();

            if (mControlType == ControlType.ZOOM) {
                mZoomControl.zoom((float) Math.pow(20, -dy), mDownX
                        / v.getWidth(), mDownY / v.getHeight());
            } else {
                mZoomControl.pan(-dx, -dy);
            }

            mX = x;
            mY = y;
            break;
        }

        }

        return true;
    }

}
