package com.kg.emailalbum.mobile.util;

import java.io.File;
import java.util.ArrayList;

import com.kg.oifilemanager.filemanager.FileManagerProvider;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class IntentHelper {
    private final static String LOG_TAG = IntentHelper.class.getSimpleName();
    
    
    /**
     * @param folder
     */
    public static void sendAllPicturesInFolder(Context ctx, File folder, String subject, String body) {
        ArrayList<Uri> uris = new ArrayList<Uri>();
        for (File item : folder.listFiles()) {
            uris.add(FileManagerProvider.getContentUri(item));
        }

        Intent sendIntent = new Intent(Compatibility.getActionSendMultiple());

        sendIntent.putExtra(Intent.EXTRA_TEXT, body);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        Log.d(LOG_TAG, "Sending list of Uris : " + uris.toString());
        sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        sendIntent.setType("image/*");
        ctx.startActivity(sendIntent);
    }
}
