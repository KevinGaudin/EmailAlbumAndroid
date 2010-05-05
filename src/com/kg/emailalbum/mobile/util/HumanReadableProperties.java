package com.kg.emailalbum.mobile.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import android.util.Log;

public class HumanReadableProperties extends Properties {

    /**
     * 
     */
    private static final long serialVersionUID = 8935971935007514957L;

    public synchronized void loadHumanReadable(InputStream in)
            throws IOException {
        BufferedReader bReader = new BufferedReader(new InputStreamReader(in,
                Charset.forName("UTF-8")));
        String curLine = null;
        while ((curLine = bReader.readLine()) != null) {
            if (!curLine.startsWith("!") && !curLine.startsWith("#")
                    && !(curLine.length() < 3)) {
                int iSeparator = curLine.indexOf("=");
                if (iSeparator == -1) {
                    iSeparator = curLine.indexOf(":");
                }
                Log.d("HRP", "Separator found on char " + iSeparator);
                if (iSeparator >= 0 && iSeparator < curLine.length()) {
                    String key = curLine.substring(0, iSeparator).trim();
                    String value = curLine.substring(iSeparator + 1).trim();
                    Log.d("HRP", "Found K / V : " + key + " / " + value);
                    if (key != null && !"".equals(key)) {
                        put(key, value);
                    }
                }
            } else {
                Log.d("HRP", "Skipping comment line...");
            }
        }
    }

    public synchronized void storeHumanReadable(Writer wrt, String comment, String lineTerminator)
            throws IOException {
        if(lineTerminator == null) {
            lineTerminator = "";
        }
        
        PrintWriter pWriter = new PrintWriter(wrt);
        if (comment != null) {
            Log.d("HRP", "Writing comment line");
            pWriter.println("# " + comment + lineTerminator);
            pWriter.println(lineTerminator);
        }
        SortedSet<Object> keys = new TreeSet<Object>(keySet());
        Log.d("HRP", "Number of keys : " + keys.size());
        for (Object key : keys) {
            String value = getProperty((String) key);
            Log.d("HRP", "Writing : " + (String) key + ": " + value);
            pWriter.println((String) key + ": " + value + lineTerminator);
        }
        pWriter.flush();
    }

    public synchronized void storeHumanReadable(OutputStream out, String comment)
            throws IOException {

        storeHumanReadable(new OutputStreamWriter(out, "UTF-8"), comment, "\r");
    }
}
