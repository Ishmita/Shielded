package com.example.android.safetyalert;

import android.app.Dialog;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity!";
    RequestQueue requestQueue;
    String url = "http://shielded.coolpage.biz/insert_signup.php", mac;
    String myName,myAge,myPhoneNumber,myEmail,myPassword,confirmPass;
    EditText name,age,phoneNumber,email,password,confirmPassword;
    Button signUpButton;
    Intent serviceIntent;
    GeofenceRequester mGeofenceRequester;
    boolean nameFilled, ageFilled, phoneFilled, emailFilled, passFilled, correctPass, isMac;
    public static -===============

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        name = (EditText)findViewById(R.id.name_editText);
        age = (EditText)findViewById(R.id.age_editText);
        phoneNumber = (EditText)findViewById(R.id.phone_number_editText);
        email = (EditText)findViewById(R.id.email_editText);
        password = (EditText) findViewById(R.id.password_editText);
        confirmPassword = (EditText) findViewById(R.id.confirm_password_editText);
        signUpButton = (Button) findViewById(R.id.sign_up_button);
        requestQueue = Volley.newRequestQueue(this);
        mGeofenceRequester = new GeofenceRequester(this);
        GPSPollingService pollingService = new GPSPollingService(this);

        serviceIntent = new Intent(MainActivity.this, GPSPollingService.class);
        startService(serviceIntent);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                myName = name.getText().toString();
                if (!myName.matches("^\\s*$")) {
                    nameFilled = true;
                }else{
                    nameFilled = false;
                }
                myAge = age.getText().toString();
                if (!myAge.matches("^\\s*$")) {
                    ageFilled = true;
                }else{
                    ageFilled = false;
                }
                myPhoneNumber = phoneNumber.getText().toString();
                if (!myPhoneNumber.matches("^\\s*$")) {
                    phoneFilled = true;
                }else{
                    phoneFilled = false;
                }
                myEmail = email.getText().toString();
                if (!myEmail.matches("^\\s*$")) {
                    emailFilled = true;
                }else{
                    emailFilled = false;
                }
                myPassword = password.getText().toString();
                if (!myPassword.matches("^\\s*$")) {
                    passFilled = true;
                }else{
                    passFilled = false;
                }
                confirmPass = confirmPassword.getText().toString();
                if (!myPassword.equals(confirmPass)) {
                    Toast.makeText(MainActivity.this, "Incorrect password, Try again", Toast.LENGTH_LONG).show();
                    correctPass = false;
                } else {
                    correctPass = true;
                }
                checkMac();
            }
            });
    }
    public String generateId(){
        Random id = new Random();
        return String.valueOf(((1+id.nextInt(9))*10000)+id.nextInt(10000));
    }

    public void saveToDB(){
        if (nameFilled && ageFilled && phoneFilled && emailFilled && passFilled) {
            if(correctPass) {
                Log.d(TAG,"all ok");
                StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        Toast.makeText(MainActivity.this, "" + s, Toast.LENGTH_LONG).show();

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Toast.makeText(MainActivity.this, "" + volleyError, Toast.LENGTH_LONG).show();

                    }
                }) {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("id",generateId());
                        params.put("name", myName);
                        params.put("age", myAge);
                        params.put("phno", myPhoneNumber);
                        params.put("email", myEmail);
                        params.put("password", myPassword);
                        params.put("mac",mac);
                        return params;
                    }
                };


                requestQueue.add(stringRequest);
                //new GPSPollingService(this);
                //serviceIntent = new Intent(MainActivity.this, GPSPollingService.class);
                //startService(serviceIntent);

            }else{
                Log.d(TAG,"problem in mac or pass");
            }
        }else{
            Toast.makeText(MainActivity.this, "All fields not filled in ", Toast.LENGTH_LONG).show();
        }
    }


    public void checkMac(){
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.mac_address_dialog);
        dialog.setTitle("Enter Mac-address");
        dialog.setCancelable(true);
        Button positiveButton = (Button)dialog.findViewById(R.id.ok_button);
        Button negativeButton = (Button)dialog.findViewById(R.id.cancel_button);
        final EditText macAddress = (EditText)dialog.findViewById(R.id.mac_editText);
        Window window2 = dialog.getWindow();
        window2.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        dialog.show();
        positiveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mac = macAddress.getText().toString();
                if(!mac.matches("^\\s*$")){
                    isMac = true;
                    saveToDB();
                }else{
                    Toast.makeText(MainActivity.this, "Mac Address not provided", Toast.LENGTH_LONG).show();
                    checkMac();
                    isMac = false;
                }
                Log.d(TAG, "isMac: "+isMac);
                dialog.dismiss();
            }
        });

        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });


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
            stopService(serviceIntent);
            return true;
        }

        else if(id == R.id.dash_board) {
            Intent dashBoardIntent = new Intent(MainActivity.this, DashBoard.class);
            startActivity(dashBoardIntent);
        }

        else if (id == R.id.about_us) {
            Intent aboutUsIntent = new Intent(MainActivity.this, AboutUs.class);
            startActivity(aboutUsIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
