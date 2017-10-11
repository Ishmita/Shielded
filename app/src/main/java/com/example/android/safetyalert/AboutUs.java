package com.example.android.safetyalert;

/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


        import android.content.Intent;
        import android.os.Bundle;
        import android.support.v7.app.AppCompatActivity;
        import android.view.Menu;
        import android.view.MenuItem;
        import android.util.Log;

public class AboutUs extends AppCompatActivity {


    private final String LOG_TAG ="AboutUs";
    //TextView aboutTextView, hyperlink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

    }



    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG,"onStart: ");
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.profile_settings) {

            Intent profile = new Intent(AboutUs.this, MainActivity.class);
            startActivity(profile);
            return true;
        }

        else if(id == R.id.dash_board) {
            Intent dashBoardIntent = new Intent(AboutUs.this, DashBoard.class);
            startActivity(dashBoardIntent);
        }

        else if (id == R.id.about_us) {
            Intent aboutUsIntent = new Intent(AboutUs.this, AboutUs.class);
            startActivity(aboutUsIntent);
        }

        else if(id == R.id.health_check) {
            Intent monitorIntent = new Intent(AboutUs.this, HealthMonitor.class);
            startActivity(monitorIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
