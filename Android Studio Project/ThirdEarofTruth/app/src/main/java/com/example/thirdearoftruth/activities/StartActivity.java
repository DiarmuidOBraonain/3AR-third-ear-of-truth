package com.example.thirdearoftruth.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.thirdearoftruth.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * @author dermotbrennan
 *
 * Start/home page of the application displayed to the user upon opening the app if they are not
 * currently signed in.
 *
 * 2 Buttons are presented that direct the User to the Log-In page if they are a Returning Existing User
 * or to the Registration page if they are a New User.
 */
public class StartActivity extends AppCompatActivity implements View.OnClickListener {

    // constant variables
    public static final String TAG = "START";

    //UI variables
    Button btnLogin, btnRegister;

    // Authentication variables
    FirebaseUser firebaseUser;


    // Methods

    /**
     * Checks if the user is currently signed in on the Firebase Authentication Server. If this is the
     * case, the view is moved to the MainActivity.
     */
    @Override
    protected void onStart(){
        super.onStart();
        // log time when app was started for NFR
        Log.d(TAG, "Application Start : "+System.currentTimeMillis());

        // check if the user is already logged in through Firebase Authentication
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if(firebaseUser != null){
            Intent mainIntent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(mainIntent);
            finish();
        }

    }

    /**
     * User Interface components are initialised and associated with variables.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkThreePermissions(); // checks the Microphone and storage permissions which are essential for app functionality
        setContentView(R.layout.activity_start);
        // animate the background
        LinearLayout myLayout = findViewById(R.id.layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) myLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();


        // assign variables
        btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(this);
        btnRegister = findViewById(R.id.btn_register);
        btnRegister.setOnClickListener(this);

        Log.d(TAG, "Application Opened : "+System.currentTimeMillis());
    }


    /**
     *  Assign the onClick behaviour of the 2 buttons.
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        // default method for handling onClick Events..
        switch (v.getId()) {
            case R.id.btn_login:

                // direct the user to the login page
                Log.v(TAG, "Heading to Login activity : "+System.currentTimeMillis());
                startActivity(new Intent(StartActivity.this, LoginActivity.class));
                break;

            case R.id.btn_register:

                // direct the user to the registration page
                Log.v(TAG, "Heading to Registration activity : "+System.currentTimeMillis());
                startActivity(new Intent(StartActivity.this, RegisterActivity.class));
                break;

            default:
                break;
        }

    }

    /**
     * Asks the user to grant permissions to Record Audio, Read from and Write to External Storage.
     *
     * Ensure that the 3 permissions necessary for the app to function have not been revoked after
     * asking user to grant permissions
     */
    public void checkThreePermissions(){

        // MICROPHONE/ RECORD AUDIO
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(StartActivity.this, Manifest.permission.RECORD_AUDIO)){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Needed");
                builder.setMessage("Access to device microphone is essential for detecting sounds and registering new ones!");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //grant the permission
                        ActivityCompat.requestPermissions(StartActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                        Log.v(TAG, "RECORD_AUDIO permission was granted after some persuasion");

                    } // end onClick
                }); // end positive button
                builder.setNegativeButton("Cancel", null);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                Log.v(TAG, "Rationale for RECORD_AUDIO permission was shown");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                Log.v(TAG, "RECORD_AUDIO permission was granted first time around ");

            } // end inner if statement
        } // end microphone permission check

        // STORAGE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(StartActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Needed");
                builder.setMessage("Access to device storage is necessary to record new sounds when registering them and for storing their data when detecting them");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //grant the permission
                        ActivityCompat.requestPermissions(StartActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                        Log.v(TAG, "READ_EXTERNAL_STORAGE permission was granted after some persuasion");

                    } // end onClick
                }); // end positive button
                builder.setNegativeButton("Cancel", null);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                Log.v(TAG, "Rationale for READ_EXTERNAL_STORAGE permission was shown");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                Log.v(TAG, "READ_EXTERNAL_STORAGE permission was granted first time around ");

            } // end inner if statement
        } // end storage permission check

        // WRITE STORAGE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(StartActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Needed");
                builder.setMessage("Writing to device storage is essential for recording new sounds when registering them");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //grant the permission
                        ActivityCompat.requestPermissions(StartActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        Log.v(TAG, "WRITE_EXTERNAL_STORAGE permission was granted after some persuasion");

                    } // end onClick
                }); // end positive button
                builder.setNegativeButton("Cancel", null);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                Log.v(TAG, "Rationale for WRITE_EXTERNAL_STORAGE permission was shown");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                Log.v(TAG, "WRITE_EXTERNAL_STORAGE permission was granted first time around ");

            } // end inner if statement
        } // end storage permission check

    } // end checkThreePermissions








} // end Activity