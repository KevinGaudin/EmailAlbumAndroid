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
import android.view.View;
import android.widget.Button;

import com.kg.emailalbum.mobile.creator.EmailAlbumEditor;
import com.kg.emailalbum.mobile.viewer.EmailAlbumViewer;

/**
 * Application main menu.
 * 
 * @author Kevin Gaudin
 * 
 */
public class EmailAlbum extends Activity {
    protected static final Uri URI_DEMO = Uri
            .parse("http://www.gaudin.tv/storage/android/curious-creature.jar");

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

}
