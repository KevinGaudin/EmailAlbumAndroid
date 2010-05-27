package com.kg.emailalbum.mobile.viewer;

import android.view.MotionEvent;
import android.view.View;

public class SimpleZoomListener implements View.OnTouchListener {

    public enum ControlType {
        PAN, ZOOM
    }

    private ControlType mControlType = ControlType.ZOOM;

    private ZoomState mState;

    private float mX;
    private float mY;

    public void setZoomState(ZoomState state) {
        mState = state;
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
                mX = x;
                mY = y;
                break;

            case MotionEvent.ACTION_MOVE: {
                final float dx = (x - mX) / v.getWidth();
                final float dy = (y - mY) / v.getHeight();

                if (mControlType == ControlType.ZOOM) {
                    mState.setZoom(mState.getZoom() * (float)Math.pow(20, -dy));
                    mState.notifyObservers();
                } else {
                    mState.setPanX(mState.getPanX() - dx);
                    mState.setPanY(mState.getPanY() - dy);
                    mState.notifyObservers();
                }

                mX = x;
                mY = y;
                break;
            }
            
        }

        return true;
    }

}
