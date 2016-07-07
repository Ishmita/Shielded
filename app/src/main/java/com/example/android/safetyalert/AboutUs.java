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

        import android.Manifest;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.net.Uri;
        import android.support.v4.app.ActivityCompat;
        import android.support.v4.content.ContextCompat;
        import android.support.v7.app.ActionBarActivity;
        import android.os.Bundle;
        import android.view.Menu;
        import android.view.MenuItem;
        import android.location.Location;
        import android.util.Log;
        import android.view.View;
        import android.widget.Button;
        import android.widget.TextView;
        import android.widget.Toast;

        import com.android.volley.Request;
        import com.android.volley.RequestQueue;
        import com.android.volley.Response;
        import com.android.volley.VolleyError;
        import com.android.volley.toolbox.JsonRequest;
        import com.android.volley.toolbox.StringRequest;
        import com.android.volley.toolbox.Volley;
        import com.google.android.gms.appdatasearch.GetRecentContextCall;
        import com.google.android.gms.common.ConnectionResult;
        import com.google.android.gms.common.api.GoogleApiClient;
        import com.google.android.gms.location.LocationListener;
        import com.google.android.gms.location.LocationRequest;
        import com.google.android.gms.location.LocationServices;

        import org.json.JSONArray;
        import org.json.JSONException;
        import org.json.JSONObject;

        import java.util.Locale;

public class AboutUs extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {


    private final String LOG_TAG ="AboutUs";
    TextView txtOutput, name, number;
    Button openMapButton,retrieve;
    private GoogleApiClient mGoogleApiClient;
    Location mLocation;
    private static final int REQUEST_CODE_LOCATION = 2;
    private LocationRequest mLocationRequest;
    String url = "http://shielded.coolpage.biz/status_read.php";

    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        openMapButton = (Button)findViewById(R.id.open_map);
        txtOutput = (TextView) findViewById(R.id.location_textView);
        name = (TextView) findViewById(R.id.name_textView);
        number = (TextView) findViewById(R.id.number_textView);
        retrieve = (Button) findViewById(R.id.retrieve);
        requestQueue = Volley.newRequestQueue(this);
        retrieve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToDb();
            }
        });
    }



    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mGoogleApiClient.connect();
        Log.d(LOG_TAG,"onStart: ");
    }

    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000); // Update location every second

        Log.d(LOG_TAG,"in onConnected: ");

        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION  },
                    REQUEST_CODE_LOCATION );
        }else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            //Location mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            //txtOutput.setText(mLocation.toString());
                    Log.d(LOG_TAG, "calling fusedLocationApi");
        }
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(LOG_TAG, "GoogleApiClient connection has failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        Log.d(LOG_TAG,"in onLocationChanged: ");
        Log.d(LOG_TAG, mLocation.toString());
        txtOutput.setText(Double.toString(mLocation.getLatitude()));
        openMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String uri = "geo:" + mLocation.getLatitude() + "," + mLocation.getLongitude();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);

            }
        });

        //txtOutput.setText(Double.toString(location.getLatitude()));
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void connectToDb(){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                Toast.makeText(AboutUs.this , ""+s,Toast.LENGTH_LONG).show();
                getFields(s);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(AboutUs.this, ""+volleyError, Toast.LENGTH_LONG).show();

            }
        });

        requestQueue.add(stringRequest);
    }

    public void getFields(String s){

        try{
            JSONObject jsonObject = new JSONObject(s);
            JSONArray jsonArray = jsonObject.getJSONArray("result");
            if(jsonArray.length()!=0) {
                JSONObject personInDanger = jsonArray.getJSONObject(0);
                name.setText("" + personInDanger.getString("name"));
                number.setText("" + personInDanger.getString("phno"));
            }
        }catch(JSONException je){
            je.printStackTrace();
        }
    }
}
