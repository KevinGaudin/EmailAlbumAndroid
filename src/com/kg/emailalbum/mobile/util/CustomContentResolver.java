package com.kg.emailalbum.mobile.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.kg.oifilemanager.filemanager.util.FileUtils;

public class CustomContentResolver {

    // We need to keep opened archives in a cache because the code which will make use
    // of the InputStream won't be able to close it before requesting a new
    // entry in the same archive. This leads to "data errors" in the native
    // unzip code.
    private static Map<String, ZipFile> archivesCache = new HashMap<String, ZipFile>();

    public static InputStream openInputStream(Context context, Uri uri)
            throws FileNotFoundException {
        String ext = FileUtils.getExtension(uri.getLastPathSegment());
        String entry = uri.getFragment();
        Log.d("CCR", "ext=" + ext + " entry=" + entry);
        if (entry != null
                && (ext.equalsIgnoreCase(".jar")
                        || ext.equalsIgnoreCase(".zip") || ext
                        .equalsIgnoreCase(".cbz"))) {
            try {
                String archiveName = uri.getPath();
                ZipFile archive = archivesCache.get(archiveName);
                if (archive == null) {
                    archive = new ZipFile(archiveName);
                    archivesCache.put(archiveName, archive);
                }
                return ZipUtil.getInputStream(archive, new ZipEntry(entry));
            } catch (IOException e) {
                throw new FileNotFoundException();
            }
        }
        return context.getContentResolver().openInputStream(uri);
    }
}
