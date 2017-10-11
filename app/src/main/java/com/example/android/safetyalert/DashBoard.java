package com.example.android.safetyalert;

import android.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DashBoard extends AppCompatActivity {

    private final String TAG = "DashBoard";
    private final int MY_LOCATION_PERMISSION = 101;
    Intent serviceIntent;
    ListView listViewPeople;
    ArrayList<Person> people = new ArrayList<Person>();
    String url = "http://shielded.6te.net/status_read.php";
    RequestQueue requestQueue;
    CustomAdapter adapter;
    //Button maps;
    String ids, restoredText;
    public static final String MY_PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dash_board_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listViewPeople = (ListView) findViewById(R.id.listView_people);
        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        restoredText = prefs.getString("id", null);
        if (restoredText != null) {
            //id = prefs.getString("id", "No id");//"No id" is the default value.
            checkLocationSettings();
            //Log.d(TAG, "id saved in shared prefs: " + id);
        }else {
            Log.d(TAG, "no account associated");
            listViewPeople.setVisibility(View.INVISIBLE);
            createDialog();

        }

        if(super.getIntent().getExtras()!= null) {
            ids = (String) super.getIntent().getExtras().get("id");
        }
        Log.d(TAG, "ids in dashboard "+ids);
        requestQueue = Volley.newRequestQueue(this);
        getListOfPeopleInDanger();

    }

    public void createDialog(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(DashBoard.this);
        builder.setTitle("Create Account");
        builder.setMessage("Welcome, create an account to continue");
        builder.setCancelable(false);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent myIntent = new Intent(DashBoard.this, MainActivity.class);
                startActivity(myIntent);
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                createDialog();
            }
        });
        builder.show();

    }

    public void getListOfPeopleInDanger(){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                //Toast.makeText(DashBoard.this , ""+s,Toast.LENGTH_LONG).show();
                getFields(s);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(DashBoard.this, "" + volleyError, Toast.LENGTH_LONG).show();

            }
        });

        requestQueue.add(stringRequest);
    }

    public void getFields(String s){

        try{
            JSONObject jsonObject = new JSONObject(s);
            JSONArray jsonArray = jsonObject.getJSONArray("result");

            if(jsonArray.length()!=0) {
                for(int i=0 ; i<jsonArray.length() ; i++) {
                    JSONObject personInDanger = jsonArray.getJSONObject(i);
                    //name.setText("" + personInDanger.getString("name"));
                    //number.setText("" + personInDanger.getString("phno"));
                    Person person = new Person();
                    if(ids!= null) {
                        if (ids.equalsIgnoreCase(personInDanger.getString("id"))) {
                            Log.d(TAG, "whose id matches: " + personInDanger.getString("name"));
                            person.setGeoStatus(true);
                            person.setColor(DashBoard.this.getResources().getColor(R.color.colorPrimary));
                        } else {
                            person.setColor(DashBoard.this.getResources().getColor(R.color.notInDanger));
                            person.setGeoStatus(false);
                        }
                    }
                    person.setName(personInDanger.getString("name"));
                    person.setPhone(personInDanger.getString("phno"));
                    person.setAge(personInDanger.getString("age"));
                    person.setLatitude(personInDanger.getString("latitude"));
                    person.setLongitude(personInDanger.getString("longitude"));
                    Log.d(TAG, "latitude: " + person.getLatitude() + "longitude: " + person.getLongitude());
                    people.add(person);
                }
                Log.d(TAG, "size of list: "+ people.size());
                adapter = new CustomAdapter(DashBoard.this, R.layout.content_dash_board, people);
                listViewPeople.setAdapter(adapter);

            }
        }catch(JSONException je){
            je.printStackTrace();
        }
    }


    public void checkLocationSettings(){
        // checking location permission
        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionCheck ==  PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission granted");
            GPSPollingService pollingService = new GPSPollingService(this);

            serviceIntent = new Intent(DashBoard.this, GPSPollingService.class);
            startService(serviceIntent);
        }else if ( permissionCheck ==  PackageManager.PERMISSION_DENIED){
            Log.d(TAG, "permission denied, now requesting permission");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_LOCATION_PERMISSION);

        }
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
            Intent profileIntent = new Intent(DashBoard.this, MainActivity.class);
            startActivity(profileIntent);
            return true;
        }

        else if(id == R.id.dash_board) {
            stopService(serviceIntent);
            return true;
        }

        else if (id == R.id.about_us) {
            Intent aboutUsIntent = new Intent(DashBoard.this, AboutUs.class);
            startActivity(aboutUsIntent);
            return true;
        }
        else if(id == R.id.health_check) {
            Intent monitorIntent = new Intent(DashBoard.this, HealthMonitor.class);
            startActivity(monitorIntent);
        }

        return super.onOptionsItemSelected(item);
    }

/*    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000); // Update location every second

        Log.d(LOG_TAG,"in onConnected: ");

        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

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
    public void onLocationChanged(Location location) {

        mLocation = location;
        Log.d(LOG_TAG,"in onLocationChanged: ");
        Log.d(LOG_TAG, mLocation.toString());
        openMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String uri = "geo:" + mLocation.getLatitude() + "," + mLocation.getLongitude();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);

            }
        });

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(LOG_TAG, "GoogleApiClient connection has failed");
    }*/
}
