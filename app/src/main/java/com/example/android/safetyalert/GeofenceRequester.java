package com.example.android.safetyalert;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for connecting to Location Services and requesting geofences.
 * <b>
 * Note: Clients must ensure that Google Play services is available before requesting geofences.
 * </b> Use GooglePlayServicesUtil.isGooglePlayServicesAvailable() to check.
 *
 *
 * To use a GeofenceRequester, instantiate it and call AddGeofence(). Everything else is done
 * automatically.
 *
 */
public class GeofenceRequester
        implements
        ResultCallback<Status>,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Storage for a reference to the calling client
    private final Activity mActivity;

    // Stores the PendingIntent used to send geofence transitions back to the app
    private PendingIntent mGeofencePendingIntent;

    // Stores the current list of geofences
    private ArrayList<Geofence> mCurrentGeofences;

    // Stores the current instantiation of the location client
    private GoogleApiClient mLocationClient;

    /*
     * Flag that indicates whether an add or remove request is underway. Check this
     * flag before attempting to start a new request.
     */
    private boolean mInProgress;

    public GeofenceRequester(Activity activityContext) {
        // Save the context
        mActivity = activityContext;

        // Initialize the globals to null
        mGeofencePendingIntent = null;
        mLocationClient = null;
        mInProgress = false;
    }

    /**
     * Set the "in progress" flag from a caller. This allows callers to re-set a
     * request that failed but was later fixed.
     *
     * @param flag Turn the in progress flag on or off.
     */
    public void setInProgressFlag(boolean flag) {
        // Set the "In Progress" flag.
        mInProgress = flag;
    }

    /**
     * Get the current in progress status.
     *
     * @return The current value of the in progress flag.
     */
    public boolean getInProgressFlag() {
        return mInProgress;
    }

    /**
     * Returns the current PendingIntent to the caller.
     *
     * @return The PendingIntent used to create the current set of geofences
     */
    public PendingIntent getRequestPendingIntent() {
        return createRequestPendingIntent();
    }

    /**
     * Start adding geofences. Save the geofences, then start adding them by requesting a
     * connection
     *
     * @param geofences A List of one or more geofences to add
     */
    public void addGeofences(List<Geofence> geofences) throws UnsupportedOperationException {

        /*
         * Save the geofences so that they can be sent to Location Services once the
         * connection is available.
         */
        mCurrentGeofences = (ArrayList<Geofence>) geofences;

        // If a request is not already in progress
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

    /**
     * Request a connection to Location Services. This call returns immediately,
     * but the request is not complete until onConnected() or onConnectionFailure() is called.
     */
    private void requestConnection() {
        getLocationClient().connect();
    }

    /**
     * Get the current location client, or create a new one if necessary.
     *
     * @return A LocationClient object
     */
    private GoogleApiClient getLocationClient() {
        if (mLocationClient == null) {

            mLocationClient = new GoogleApiClient.Builder(mActivity)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        return mLocationClient;

    }
    /**
     * Once the connection is available, send a request to add the Geofences
     */
    private void continueAddGeofences() {

        // Get a PendingIntent that Location Services issues when a geofence transition occurs
        mGeofencePendingIntent = createRequestPendingIntent();
        if (!mLocationClient.isConnected()) {
            Toast.makeText(mActivity, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mLocationClient,
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
                    mActivity, "Geofences added",
                    Toast.LENGTH_SHORT
            ).show();

            msg = mActivity.getString(R.string.add_geofences_result_success, status);

            // In debug mode, log the result
            Log.d(GeofenceUtils.APPTAG, msg);

            // Create an Intent to broadcast to the app
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCES_ADDED)
                    .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                    .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, msg);
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

            // Create an Intent to broadcast to the app
            //broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_ERROR)
            //        .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
            //        .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, msg);
        }

        // Broadcast whichever result occurred
        //LocalBroadcastManager.getInstance(mActivity).sendBroadcast(broadcastIntent);

        // Disconnect the location client
        requestDisconnection();

    }
    /**
     * Get a location client and disconnect from Location Services
     */
    private void requestDisconnection() {

        // A request is no longer in progress
        mInProgress = false;

        getLocationClient().disconnect();
    }

    /*
     * Called by Location Services once the location client is connected.
     *
     * Continue by adding the requested geofences.
     */
    @Override
    public void onConnected(Bundle arg0) {
        // If debugging, log the connection

        Log.d(GeofenceUtils.APPTAG, mActivity.getString(R.string.connected));

        // Continue adding the geofences
        continueAddGeofences();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * Get a PendingIntent to send with the request to add Geofences. Location Services issues
     * the Intent inside this PendingIntent whenever a geofence transition occurs for the current
     * list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent createRequestPendingIntent() {

        // If the PendingIntent already exists
        if (null != mGeofencePendingIntent) {

            // Return the existing intent
            return mGeofencePendingIntent;

            // If no PendingIntent exists
        } else {
            Intent intent = new Intent("com.example.android.geofence.ACTION_RECEIVE_GEOFENCE");
            return PendingIntent.getBroadcast(
                    mActivity,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    /*
     * Implementation of OnConnectionFailedListener.onConnectionFailed
     * If a connection or disconnection request fails, report the error
     * connectionResult is passed in from Location Services
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        // Turn off the request flag
        mInProgress = false;

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {

            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(mActivity,
                        GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

            /*
             * Thrown if Google Play services canceled the original
             * PendingIntent
             */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }

        /*
         * If no resolution is available, put the error code in
         * an error Intent and broadcast it back to the main Activity.
         * The Activity then displays an error dialog.
         * is out of date.
         */
        } else {

            Toast.makeText(mActivity, "connection error in GeofenceRequester",Toast.LENGTH_SHORT).show();
        //    Intent errorBroadcastIntent = new Intent(GeofenceUtils.ACTION_CONNECTION_ERROR);
        //    errorBroadcastIntent.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
        //            .putExtra(GeofenceUtils.EXTRA_CONNECTION_ERROR_CODE,
        //                    connectionResult.getErrorCode());
        //    LocalBroadcastManager.getInstance(mActivity).sendBroadcast(errorBroadcastIntent);
        }
    }


}
