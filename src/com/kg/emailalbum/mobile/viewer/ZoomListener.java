/*
 * Copyright (c) 2010, Sony Ericsson Mobile Communication AB. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *
 *    * Redistributions of source code must retain the above copyright notice, this 
 *      list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 *    * Neither the name of the Sony Ericsson Mobile Communication AB nor the names
 *      of its contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.kg.emailalbum.mobile.viewer;

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import com.kg.emailalbum.mobile.util.Compatibility;

public class ZoomListener implements View.OnTouchListener {

    public enum ControlType {
        PAN, UNDEFINED, ZOOM
    }

    private ControlType mControlType = ControlType.ZOOM;

    /** X-coordinate of latest down event */
    private float mDownX;
    /** Y-coordinate of latest down event */
    private float mDownY;
    private final int mScaledMaximumFlingVelocity;
    private VelocityTracker mVelocityTracker;

    private float mX;

    private float mY;

    /** Zoom control to manipulate */
    private ZoomControl mZoomControl;

    public ZoomListener(Context context) {
        mScaledMaximumFlingVelocity = Compatibility.getScaledMaximumFlingVelocity(context);
    }

    public ControlType getControlType() {
        return mControlType;
    }

    public boolean onTouch(View v, MotionEvent event) {
        final int action = event.getAction();
        final float x = event.getX();
        final float y = event.getY();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mZoomControl.stopFling();
            mDownX = x;
            mDownY = y;
            mX = x;
            mY = y;
            return false;

        case MotionEvent.ACTION_MOVE: {
            final float dx = (x - mX) / v.getWidth();
            final float dy = (y - mY) / v.getHeight();

            if (mControlType == ControlType.ZOOM) {
                mZoomControl.zoom((float) Math.pow(20, -dy),
                        mDownX / v.getWidth(), mDownY / v.getHeight());
            } else {
                mZoomControl.pan(-dx, -dy);
            }

            mX = x;
            mY = y;
            break;
        }

        case MotionEvent.ACTION_UP:
            if (mControlType == ControlType.PAN) {
                mVelocityTracker.computeCurrentVelocity(1000,
                        mScaledMaximumFlingVelocity);
                mZoomControl.startFling(
                        -mVelocityTracker.getXVelocity() / v.getWidth(),
                        -mVelocityTracker.getYVelocity() / v.getHeight());
            } else {
                mZoomControl.startFling(0, 0);
            }
            mVelocityTracker.recycle();
            mVelocityTracker = null;
            mControlType = ControlType.UNDEFINED;
            break;

        default:
            mVelocityTracker.recycle();
            mVelocityTracker = null;
            mControlType = ControlType.UNDEFINED;
            break;

        }

        return true;
    }

    public void setControlType(ControlType controlType) {
        mControlType = controlType;
    }

    /**
     * Sets the zoom control to manipulate
     * 
     * @param control
     *            Zoom control
     */
    public void setZoomControl(ZoomControl control) {
        mZoomControl = control;
    }
}
