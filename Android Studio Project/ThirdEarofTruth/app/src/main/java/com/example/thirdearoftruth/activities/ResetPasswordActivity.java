package com.example.thirdearoftruth.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.thirdearoftruth.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {
    // variables
    public static final String TAG = "RESET_PASSWORD";

    EditText send_email;
    Button btnReset;

    FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        initialiseToolbarAndBackground();


        // associate vars with ids
        send_email = findViewById(R.id.send_email);
        btnReset = findViewById(R.id.btn_reset);

        // connect to firebase to retrieve this user's email address
        firebaseAuth = FirebaseAuth.getInstance();

        // buttons
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = send_email.getText().toString();

                if (email.equals("")){
                    Toast.makeText(ResetPasswordActivity.this, "Email Address required!", Toast.LENGTH_SHORT).show();
                } else {
                    firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(ResetPasswordActivity.this, "Please check Your email inbox for instructions on how to reset your password", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
                            } else{
                                String error = task.getException().getMessage();
                                Toast.makeText(ResetPasswordActivity.this, error, Toast.LENGTH_SHORT).show();
                            } // end if

                        } // end onComplete method
                    }); // end onCompleteListener
                } // end if
            } // end onClick method
        }); // end btn onClickListener



    } // end onCreate



    /**
     * Prepares the toolbar for this activity and animates the background
     */
    public void initialiseToolbarAndBackground(){
        Log.d(TAG, "Instance of LoginActivity created.");
        // animate the background
        LinearLayout myLayout = findViewById(R.id.layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) myLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();
        Log.d(TAG, "Background animated");

        // setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Reset Password");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Log.d(TAG, "Toolbar successfully initialised");

    } // end initialiseToolbarAndBackground() method



} // end activity