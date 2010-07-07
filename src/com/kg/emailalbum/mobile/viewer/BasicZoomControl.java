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

import java.util.Observable;
import java.util.Observer;

import android.os.Handler;
import android.os.SystemClock;

import com.kg.emailalbum.mobile.util.SpringDynamics;

public class BasicZoomControl implements Observer {

    private static final int FPS = 50;

    private static final float MAX_ZOOM = 16;

    private static final float MIN_ZOOM = 1;

    private static final float PAN_OUTSIDE_SNAP_FACTOR = .4f;

    private static final float REST_POSITION_TOLERANCE = 0.01f;
    private static final float REST_VELOCITY_TOLERANCE = 0.004f;

    private AspectQuotient mAspectQuotient;

    private final Handler mHandler = new Handler();

    private final SpringDynamics mPanDynamicsX = new SpringDynamics();

    private final SpringDynamics mPanDynamicsY = new SpringDynamics();

    private float mPanMaxX;
    private float mPanMaxY;
    private float mPanMinX;
    private float mPanMinY;
    private final ZoomState mState = new ZoomState();

    private final Runnable mUpdateRunnable = new Runnable() {
        public void run() {
            final long startTime = SystemClock.uptimeMillis();
            mPanDynamicsX.update(startTime);
            mPanDynamicsY.update(startTime);
            final boolean isAtRest = mPanDynamicsX.isAtRest(
                    REST_VELOCITY_TOLERANCE, REST_POSITION_TOLERANCE)
                    && mPanDynamicsY.isAtRest(REST_VELOCITY_TOLERANCE,
                            REST_POSITION_TOLERANCE);
            mState.setPanX(mPanDynamicsX.getPosition());
            mState.setPanY(mPanDynamicsY.getPosition());

            if (!isAtRest) {
                final long stopTime = SystemClock.uptimeMillis();
                mHandler.postDelayed(mUpdateRunnable, 1000 / FPS
                        - (stopTime - startTime));
            }

            mState.notifyObservers();
        }
    };

    public BasicZoomControl() {
        mPanDynamicsX.setFriction(2f);
        mPanDynamicsY.setFriction(2f);
        mPanDynamicsX.setSpring(50f, 1f);
        mPanDynamicsY.setSpring(50f, 1f);
    }

    private float getMaxPanDelta(float zoom) {
        return Math.max(0f, .5f * ((zoom - 1) / zoom));
    }

    public ZoomState getZoomState() {
        return mState;
    }

    public boolean isZoomed() {
        return mState.isZoomed();
    }

    private void limitZoom() {
        if (mState.getZoom() > MAX_ZOOM) {
            mState.setZoom(MAX_ZOOM);
        }

        if (mState.getZoom() < MIN_ZOOM) {
            mState.setZoom(MIN_ZOOM);
        }

    }

    public void pan(float dx, float dy) {
        final float aspectQuotient = mAspectQuotient.get();

        dx /= mState.getZoomX(aspectQuotient);
        dy /= mState.getZoomY(aspectQuotient);

        if (mState.getPanX() > mPanMaxX && dx > 0
                || mState.getPanX() < mPanMinX && dx < 0) {
            dx *= PAN_OUTSIDE_SNAP_FACTOR;
        }
        if (mState.getPanY() > mPanMaxY && dy > 0
                || mState.getPanY() < mPanMinY && dy < 0) {
            dy *= PAN_OUTSIDE_SNAP_FACTOR;
        }

        final float newPanX = mState.getPanX() + dx;
        final float newPanY = mState.getPanY() + dy;

        mState.setPanX(newPanX);
        mState.setPanY(newPanY);

        mState.notifyObservers();
    }

    public void setAspectQuotient(AspectQuotient aspectQuotient) {
        if (mAspectQuotient != null) {
            mAspectQuotient.deleteObserver(this);
        }

        mAspectQuotient = aspectQuotient;
        mAspectQuotient.addObserver(this);
    }

    public void startFling(float vx, float vy) {
        final float aspectQuotient = mAspectQuotient.get();
        final long now = SystemClock.uptimeMillis();

        mPanDynamicsX.setState(mState.getPanX(),
                vx / mState.getZoomX(aspectQuotient), now);
        mPanDynamicsY.setState(mState.getPanY(),
                vy / mState.getZoomY(aspectQuotient), now);

        mPanDynamicsX.setMinPosition(mPanMinX);
        mPanDynamicsX.setMaxPosition(mPanMaxX);
        mPanDynamicsY.setMinPosition(mPanMinY);
        mPanDynamicsY.setMaxPosition(mPanMaxY);

        mHandler.post(mUpdateRunnable);
    }
    public void stopFling() {
        mHandler.removeCallbacks(mUpdateRunnable);
    }
    public void update(Observable observable, Object data) {
        limitZoom();
    }

    private void updatePanLimits() {
        final float aspectQuotient = mAspectQuotient.get();

        final float zoomX = mState.getZoomX(aspectQuotient);
        final float zoomY = mState.getZoomY(aspectQuotient);

        mPanMinX = .5f - getMaxPanDelta(zoomX);
        mPanMaxX = .5f + getMaxPanDelta(zoomX);
        mPanMinY = .5f - getMaxPanDelta(zoomY);
        mPanMaxY = .5f + getMaxPanDelta(zoomY);
    }

    public void zoom(float f, float x, float y) {
        final float aspectQuotient = mAspectQuotient.get();

        final float prevZoomX = mState.getZoomX(aspectQuotient);
        final float prevZoomY = mState.getZoomY(aspectQuotient);

        mState.setZoom(mState.getZoom() * f);
        limitZoom();

        final float newZoomX = mState.getZoomX(aspectQuotient);
        final float newZoomY = mState.getZoomY(aspectQuotient);

        mState.setPanX(mState.getPanX() + (x - .5f)
                * (1f / prevZoomX - 1f / newZoomX));
        mState.setPanY(mState.getPanY() + (y - .5f)
                * (1f / prevZoomY - 1f / newZoomY));

        updatePanLimits();

        mState.notifyObservers();
    }
}
