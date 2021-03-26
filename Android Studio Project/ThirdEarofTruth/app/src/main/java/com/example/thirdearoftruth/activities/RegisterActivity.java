/**
 * This is an Android User Interface activity
 */
package com.example.thirdearoftruth.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.thirdearoftruth.R;
import com.example.thirdearoftruth.models.DefaultEventsManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;

/**
 * @author dermotbrennan
 *
 * The activity from which the user creates a new account and can begin to add sounds, detect sounds
 * or receive notifications from the device detecting them about the sounds it has detected.
 *
 * The Firebase Authentication service handles the creation of the user account on the server and
 * Firebase Realtime Database is used to store a reference to this user which allows Acoustic Event
 * data to be associated with them.
 *
 * A selection of 12 prerecorded and feature-extracted Acoustic Events are added to the new User's
 * library of Acoustic Events as defaults to allow the user to begin to use the application immediately
 * without having added any Acoustic Events specific to their household.
 *
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "REGISTER";
    public final int MIN_PASSWORD_LENGTH = 6;

    //UI variables
    EditText username, email, pWord;
    Button btnRegister;

    // Firebase variables
    FirebaseAuth mFirebaseAuth;
    DatabaseReference mDatabaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // animate the background
        RelativeLayout myLayout = findViewById(R.id.layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) myLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        // setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Register");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // associate editTexts and button with variables
        // might be worth getting extra userdata like name and age
        username = findViewById(R.id.username);
        email = findViewById(R.id.email);
        pWord = findViewById(R.id.pword);
        btnRegister = findViewById(R.id.btn_register);


        btnRegister = findViewById(R.id.btn_register);
        // get instance of Firebase Authentication
        mFirebaseAuth = FirebaseAuth.getInstance();


        // set onclick for register button
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt_username = username.getText().toString();
                String txt_email = email.getText().toString();
                String txt_password = pWord.getText().toString();

                if(TextUtils.isEmpty(txt_username) || TextUtils.isEmpty(txt_email) || TextUtils.isEmpty(txt_password)){
                    Toast.makeText(RegisterActivity.this, "All fields are required to register", Toast.LENGTH_SHORT).show();
                    Log.v(TAG, "Register button clicked but fields were empty");
                } else if (txt_password.length() < MIN_PASSWORD_LENGTH){
                    Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
                    Log.v(TAG, "Register button clicked but the password was too short");
                } else{
                    register(txt_username, txt_email, txt_password);
                }
            }
        });


    } // end onCreate method


    /**
     * Creates a new User in the Firebase Authentication project and a new reference to A User in
     * the Firebase Realtime Database
     *
     * @param username
     * @param email
     * @param pWord
     */
    private void register(final String username, String email, String pWord){
        mFirebaseAuth.createUserWithEmailAndPassword(email, pWord)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            final FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
                            final String userId = firebaseUser.getUid();
                            // use the firebase user's id as the topic for notifications
                            FirebaseMessaging.getInstance().subscribeToTopic(userId);

                            mDatabaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId);

                            HashMap<String, String> hashMap = new HashMap<>();
                            hashMap.put("id", userId);
                            hashMap.put("username", username);
                            hashMap.put("imageURL", "default");
                            hashMap.put("status", "offline");
                            hashMap.put("search", username.toLowerCase());

                            mDatabaseReference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        Log.d(TAG, "Registration successful. Signing in and heading to main method");

                                        // subscribe user to topic push notifications on their first time!
                                        FirebaseMessaging.getInstance().subscribeToTopic(userId);

                                        // add the small library of default acoustic events for new the new user
                                        DefaultEventsManager eventsManager = new DefaultEventsManager(firebaseUser);
                                        eventsManager.addDefaultAcousticEvents();

                                        Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        mainIntent.putExtra("receiving", false);
                                        startActivity(mainIntent);
                                        finish();
                                    } //end if

                                } // end INNER onComplete
                            }); // end mDatabaseReference oncompletelistener
                        } else{
                            Toast.makeText(RegisterActivity.this, "Registration with this email or password is not possible", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "There was a problem registering with this email address or password. Email address very likely already registered to an existing user");
                        }// end OUTER if statement
                    } // end OUTER onComplete for createUserWithEmailAndPassword

                }); // end mFirebaseAuth oncompletelistener

    } // end register method





} // end RegisterActivity