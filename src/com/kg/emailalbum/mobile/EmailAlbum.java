/**
 * Copyright 2009, 2010 Kevin Gaudin
 *
 * This file is part of EmailAlbum.
 *
 * EmailAlbum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EmailAlbum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EmailAlbum.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kg.emailalbum.mobile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.kg.emailalbum.mobile.creator.EmailAlbumEditor;
import com.kg.emailalbum.mobile.viewer.EmailAlbumViewer;
import com.kg.emailalbum.mobile.viewer.ShowPics;

/**
 * Application main menu.
 * 
 * @author Kevin Gaudin
 * 
 */
public class EmailAlbum extends Activity {
    protected static final Uri URI_DEMO = Uri
            .parse("http://www.gaudin.tv/storage/android/curious-creature.jar");
    private static final int MENU_PREFS_ID = 0;

    /**
     * Initialisation of buttons behaviours.
     */
    private void initMainMenu() {
        // Open an existing album archive
        Button btn = (Button) findViewById(R.id.BtnOpenAlbum);
        btn.getBackground().setDither(true);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),
                        EmailAlbumViewer.class);
                startActivity(i);
            }

        });

        // View Gallery
        btn = (Button) findViewById(R.id.BtnOpenGallery);
        btn.getBackground().setDither(true);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),
                        ShowPics.class);
                i.putExtra("ALBUM", Media.EXTERNAL_CONTENT_URI);
                startActivity(i);
            }

        });

        // View TagFilter
        btn = (Button) findViewById(R.id.BtnOpenTagFilter);
        btn.getBackground().setDither(true);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),
                        ShowPics.class);
                i.putExtra("ALBUM", Uri.parse("content://TAGS"));
                startActivity(i);
            }

        });

        // Create a new album
        btn = (Button) findViewById(R.id.BtnCreateAlbum);
        btn.getBackground().setDither(true);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),
                        EmailAlbumEditor.class);
                startActivity(i);
            }

        });

        // Download a demo album archive
        btn = (Button) findViewById(R.id.BtnDemoAlbum);
        btn.getBackground().setDither(true);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW, URI_DEMO);
                startActivity(i);
            }

        });

        // Display the 'about' dialog
        btn = (Button) findViewById(R.id.BtnAbout);
        btn.getBackground().setDither(true);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),
                        AboutDialog.class);
                startActivity(intent);
            }

        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
        findViewById(R.id.main_menu_root).getBackground().setDither(true);
        initMainMenu();
    }
    
    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuItem item = menu.add(0, MENU_PREFS_ID, 0, R.string.menu_prefs);
        item.setIcon(android.R.drawable.ic_menu_preferences);
        return result;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_PREFS_ID:
            startPreferencesActivity();
            return true;
        default:
            return false;
        }
    }
    
    /**
     * Start the settings activity.
     */
    private void startPreferencesActivity() {
        Intent i = new Intent(getApplicationContext(),
                EmailAlbumPreferences.class);
        startActivity(i);
    }
}
