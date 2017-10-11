package com.example.android.safetyalert;

import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Divyani on 08-10-2017.
 */
public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {

    private static final String TAG="MyFirebaseIIDService";
    private static final String MY_PREFS_NAME = "MyPrefsFile";

    RequestQueue requestQueue;

    String url = "http://shielded.6te.net/fcm_insert";
    @Override
    public void onTokenRefresh() {
        String recent_token = FirebaseInstanceId.getInstance().getToken();

        requestQueue = Volley.newRequestQueue(this);
        Log.d(TAG,"Refreshed Token"+recent_token);
        saveToSharedPref(recent_token);
        //sendregisterationTokenToServer(recent_token);
    }
    private void sendregisterationTokenToServer(final String token)
    {

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
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
                params.put("token", token);
                return params;
            }
        };
        requestQueue.add(stringRequest);


    }

    private void saveToSharedPref(String token) {


        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();

        editor.putString("token", token);
        editor.commit();




    }



}