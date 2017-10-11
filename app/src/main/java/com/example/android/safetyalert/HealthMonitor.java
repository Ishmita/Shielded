package com.example.android.safetyalert;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class HealthMonitor extends AppCompatActivity {

    final String url = "http://shielded.6te.net/monitor.php";
    TextView beatValue, gsrValue;
    RequestQueue requestQueue;
    public static final String MY_PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_monitor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        beatValue = (TextView)findViewById(R.id.beat_value_textView);
        gsrValue = (TextView)findViewById(R.id.gsr_value_textView);
        requestQueue = Volley.newRequestQueue(this);

    }

    public void refresh(View view){
        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    JSONArray jsonArray = jsonObject.getJSONArray("result");
                    for(int i = 0; i<jsonArray.length(); i++) {
                        JSONObject person = jsonArray.getJSONObject(i);
                        beatValue.setText(person.getString("beat"));
                        gsrValue.setText(person.getString("gsr"));
                    }
                }catch (JSONException je){

                    je.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                if (getAccountId() != null) {
                    params.put("id", getAccountId());
                }else{

                    Toast.makeText(HealthMonitor.this, "Please make an account first", Toast.LENGTH_SHORT).show();

                }
                return params;
            }
        };
        requestQueue.add(request);

    }

    public String getAccountId(){
        SharedPreferences preferences = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String restoredText = preferences.getString("id", null);
        if (restoredText != null) {
            return restoredText;
        }else{
            return null;
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
            Intent profile = new Intent(HealthMonitor.this, MainActivity.class);
            startActivity(profile);
            return true;
        }

        else if(id == R.id.dash_board) {
            Intent dashBoardIntent = new Intent(HealthMonitor.this, DashBoard.class);
            startActivity(dashBoardIntent);
            return true;
        }

        else if (id == R.id.about_us) {
            Intent aboutUsIntent = new Intent(HealthMonitor.this, AboutUs.class);
            startActivity(aboutUsIntent);
            return true;
        }

        else if(id == R.id.health_check) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
