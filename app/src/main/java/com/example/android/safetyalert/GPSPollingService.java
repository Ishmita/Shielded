package com.example.android.safetyalert;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GestureRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ishmita on 06-07-2016.
 */
public class GPSPollingService extends Service implements ResultCallback<Status>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleApiClient mGoogleApiClient;
    private PowerManager.WakeLock mWakeLock;
    private LocationRequest mLocationRequest;
    // Flag that indicates if a request is underway.
    private boolean mInProgress;
    private final String TAG = "GPSPollingService";

    private PendingIntent mGeofencePendingIntent;
    String url = "http://shielded.coolpage.biz/status_read.php", urlToSaveLocation = "http://shielded.coolpage.biz/lat_long.php";
    private static final int REQUEST_CODE_LOCATION = 2;
    private Boolean servicesAvailable = false;
    private Activity mActivity;

    String id;
    public static final String MY_PREFS_NAME = "MyPrefsFile";
    ArrayList<Geofence> mCurrentGeofences = new ArrayList<Geofence>();;
    RequestQueue requestQueue, requestQueue2;
    Location mLocation;

    public GPSPollingService(){}

    public GPSPollingService(Activity mActivity){
        super();
        this.mActivity = mActivity;

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInProgress = false;
        requestQueue = Volley.newRequestQueue(this);
        requestQueue2 = Volley.newRequestQueue(this);
        // Instantiate the current List of geofences
        mCurrentGeofences = new ArrayList<Geofence>();

        mGeofencePendingIntent = null;
        setUpLocationClientIfNeeded();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mGoogleApiClient.connect();
        Log.d(TAG,"onStartCommand: ");
        PowerManager mgr = (PowerManager)getSystemService(Context.POWER_SERVICE);
        /*
        WakeLock is reference counted so we don't want to create multiple WakeLocks. So do a check before initializing and acquiring.
        This will fix the "java.lang.Exception: WakeLock finalized while still held: MyWakeLock" error that you may find.
        */
        if (this.mWakeLock == null) { //**Added this
            this.mWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        }

        if (!this.mWakeLock.isHeld()) { //**Added this
            this.mWakeLock.acquire();
        }

        if(!servicesAvailable || mGoogleApiClient.isConnected() || mInProgress)
            return START_STICKY;

        setUpLocationClientIfNeeded();
        if(!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting() && !mInProgress)
        {
            //appendLog(DateFormat.getDateTimeInstance().format(new Date()) + ": Started", Constants.LOG_FILE);
            mInProgress = true;
            mGoogleApiClient.connect();
        }

        return START_STICKY;


    }

    private void setUpLocationClientIfNeeded()
    {
        if(mGoogleApiClient == null)
            googleApiClientBuild();
    }
    protected synchronized void googleApiClientBuild(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(120000); // Update location every 120 seconds i.e. 2 minutes

        Log.d(TAG,"in onConnected: ");

        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( mActivity, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION  },
                    REQUEST_CODE_LOCATION );
        }else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            //Location mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            //txtOutput.setText(mLocation.toString());
            Log.d(TAG, "calling fusedLocationApi");
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

        // Turn off the request flag
        mInProgress = false;
        // Destroy the current location client
        mGoogleApiClient = null;
        Log.d(TAG, "onConnectionSuspended ");
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;

        Log.d(TAG,"in onLocationChanged: ");
        Log.d(TAG, mLocation.toString());
        // save new location to db
        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String restoredText = prefs.getString("id", null);
        if (restoredText != null) {
            id = prefs.getString("id", "No id");//"No id" is the default value.
            Log.d(TAG, "id saved in shared prefs: " + id);
        }
        StringRequest stringRequest = new StringRequest(Request.Method.POST, urlToSaveLocation, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {

                Log.d(TAG, "Server response: "+s);
                Toast.makeText(getApplicationContext(), ""+ s, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, "volley error: " , volleyError);
            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("id", id );
                params.put("latitude", String.valueOf(mLocation.getLatitude()));
                params.put("longitude", String.valueOf(mLocation.getLongitude()));
                return params;
            }
        };
        requestQueue2.add(stringRequest);


        connectToDb();          // checking for status 1



    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        mInProgress = false;

        Log.d(TAG, "onConnectionFailed");
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            Log.d(TAG, "google play services has solution");

            // If no resolution is available, display an error dialog
        } else {

            Log.d(TAG, "no solution for connection failure");
        }
    }

    @Override
    public void onDestroy() {
        // Turn off the request flag
        this.mInProgress = false;

        if (this.servicesAvailable && this.mGoogleApiClient != null) {
            this.mGoogleApiClient.unregisterConnectionCallbacks(this);
            this.mGoogleApiClient.unregisterConnectionFailedListener(this);
            this.mGoogleApiClient.disconnect();
            // Destroy the current location client
            this.mGoogleApiClient = null;
        }
        // Display the connection status
        // Toast.makeText(this, DateFormat.getDateTimeInstance().format(new Date()) + ":
        // Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();

        if (this.mWakeLock != null) {
            this.mWakeLock.release();
            this.mWakeLock = null;
        }

        super.onDestroy();
    }
    public void connectToDb(){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                Toast.makeText(getApplicationContext() , ""+s,Toast.LENGTH_LONG).show();
                Log.d(TAG, ""+s);
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    JSONArray jsonArray = jsonObject.getJSONArray("result");
                    if(jsonArray.length()!=0){
                        Log.d(TAG,"array length not 0");
                        JSONObject personInDanger = jsonArray.getJSONObject(0);
                        setGeofence(Double.valueOf(personInDanger.getString("latitude")), Double.valueOf(personInDanger.getString("longitude")));
                    }
            }catch (JSONException je) {
                // do something with it
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(getApplicationContext(), ""+volleyError, Toast.LENGTH_LONG).show();

            }
        });

        requestQueue.add(stringRequest);
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            // In debug mode, log the status
            Log.d(GeofenceUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;

            // Google Play services was not available for some reason
        } else {

            // Display an error dialog
            Toast.makeText(getApplicationContext(), "Google play services not available", Toast.LENGTH_SHORT).show();
            //Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, (Activity) getApplicationContext(), 0);
            //if (dialog != null) {
            //    ErrorDialogFragment errorFragment = new ErrorDialogFragment();
            //    errorFragment.setDialog(dialog);
            //    errorFragment.show(getSupportFragmentManager(), GeofenceUtils.APPTAG);
            }
            return false;
        }

    public void setGeofence(Double latitude , Double longitude){

        Log.d(TAG, "in setGeofence");
        if (!servicesConnected()) {

            return;
        }

        SimpleGeofence newGeofence = new SimpleGeofence(
                "1",
                // Get latitude, longitude, and radius from the db
                latitude,
                longitude,
                Float.valueOf("50.0"),
                // Set the expiration time
                Geofence.NEVER_EXPIRE,
                // Detect both entry and exit transitions
                Geofence.GEOFENCE_TRANSITION_ENTER
        );



        mCurrentGeofences.add(newGeofence.toGeofence());
        // Start the request. Fail if there's already a request in progress
        try {
            // Try to add geofences
            addGeofences(mCurrentGeofences);
            continueAddGeofences();
        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.add_geofences_already_requested_error,
                    Toast.LENGTH_LONG).show();
        }

    }

    public void addGeofences(ArrayList<Geofence> geofences) throws UnsupportedOperationException {
        Log.d(TAG, "in addGeofences");
        if (!mInProgress) {

            // Toggle the flag and continue
            mInProgress = true;

            // Request a connection to Location Services
            requestConnection();

            // If a request is in progress
        } else {

            // Throw an exception and stop the request
            throw new UnsupportedOperationException();
        }
    }

    private void requestConnection()
    {   Log.d(TAG, "in requestConnection");
        getLocationClient().connect();
    }

    private GoogleApiClient getLocationClient() {
        if (mGoogleApiClient == null) {

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        return mGoogleApiClient;

    }

    private void continueAddGeofences() {

        Log.d(TAG, "in continueAddGeofence");
        // Get a PendingIntent that Location Services issues when a geofence transition occurs
        mGeofencePendingIntent = createRequestPendingIntent();
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(mActivity, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    mCurrentGeofences,
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    mGeofencePendingIntent
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }

    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(GeofenceUtils.APPTAG , "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    public void onResult(Status status) {
        Intent broadcastIntent = new Intent();

        // Temp storage for messages
        String msg;


        if (status.isSuccess()) {

            Toast.makeText(
                    this.getApplicationContext(), "Geofences added",
                    Toast.LENGTH_SHORT
            ).show();

            msg = this.getApplicationContext().getString(R.string.add_geofences_result_success, status);

            // In debug mode, log the result
            Log.d(GeofenceUtils.APPTAG, msg);

            mInProgress = false;
            // Create an Intent to broadcast to the app
            //broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCES_ADDED)
            //        .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
            //        .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, msg);
            // If adding the geofences failed
        } else {

            /*
             * Create a message containing the error code and the list
             * of geofence IDs you tried to add
             */
            msg = mActivity.getString(
                    R.string.add_geofences_result_failure,
                    status.getStatusCode(), status.getStatusMessage());

            // Log an error
            Log.e(GeofenceUtils.APPTAG, msg);

        }

    }

    private PendingIntent createRequestPendingIntent() {

        // If the PendingIntent already exists
        if (null != mGeofencePendingIntent) {

            // Return the existing intent
            return mGeofencePendingIntent;

            // If no PendingIntent exists
        } else {
            Intent intent = new Intent("com.example.android.geofence.ACTION_RECEIVE_GEOFENCE");
            return PendingIntent.getBroadcast(
                    this.getApplicationContext(),
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }



}
