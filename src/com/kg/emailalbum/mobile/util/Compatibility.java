package com.kg.emailalbum.mobile.util;

import java.lang.reflect.Field;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.MotionEvent;

public class Compatibility {
    private static final String LOG_TAG = Compatibility.class.getSimpleName();

    public static String getActionSendMultiple() {
        try {
            Field actionSendMultipleField = Intent.class
                    .getField("ACTION_SEND_MULTIPLE");
            String actionSendMultipleValue = (String) actionSendMultipleField
                    .get(null);

            return actionSendMultipleValue;
        } catch (SecurityException e) {
            Log.w(LOG_TAG, "Error : ", e);
            return null;
        } catch (NoSuchFieldException e) {
            Log.w(LOG_TAG, "Error : ", e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.w(LOG_TAG, "Error : ", e);
            return null;
        } catch (IllegalAccessException e) {
            Log.w(LOG_TAG, "Error : ", e);
            return null;
        }
    }

    public static boolean isSendMultipleAppAvailable(Context ctx) {
        String action = getActionSendMultiple();
        if (action == null) {
            return false;
        } else {
            Intent i = new Intent(Compatibility.getActionSendMultiple());
            i.setType("image/jpeg");
            List<ResolveInfo> activities = ctx.getPackageManager().queryIntentActivities(i, 0);
            // If there is only 1 activity, it is EmailAlbum !
            if( activities.size() > 1) {
                return true;
            } else {
                return false;
            }
        }
    }
    
    public static ScaleGestureDetector getScaleGestureDetector(Context context, ScaleGestureDetector.OnScaleGestureListener listener) {
        try {
            // If multitouch is supported, this field exist
            MotionEvent.class.getField("ACTION_POINTER_1_DOWN");
            Log.d(LOG_TAG, "Looks like multitouch is supported.");
            return new ScaleGestureDetector(context, listener);
        } catch (SecurityException e) {
            Log.w(LOG_TAG, "Error : ", e);
            return null;
        } catch (NoSuchFieldException e) {
            Log.w(LOG_TAG, "Error : ", e);
            return null;
        }
    }
}
