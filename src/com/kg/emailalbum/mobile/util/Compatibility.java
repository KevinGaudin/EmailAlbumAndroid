package com.kg.emailalbum.mobile.util;

import java.lang.reflect.Field;

import android.content.Intent;
import android.util.Log;

public class Compatibility {
    private static final String LOG_TAG = Compatibility.class.getSimpleName();
    
    public static String getActionSendMultiple() {
        try {
            Field actionSendMultipleField = Intent.class.getField("ACTION_SEND_MULTIPLE");
            String actionSendMultipleValue = (String)actionSendMultipleField.get(null);
            
            return actionSendMultipleValue;
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Error : ", e);
            return null;
        } catch (NoSuchFieldException e) {
            Log.e(LOG_TAG, "Error : ", e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Error : ", e);
            return null;
        } catch (IllegalAccessException e) {
            Log.e(LOG_TAG, "Error : ", e);
            return null;
        }
    }
}
