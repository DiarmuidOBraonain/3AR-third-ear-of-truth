/**
 * This is an Android User Interface Activity
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
import android.widget.TextView;
import android.widget.Toast;

import com.example.thirdearoftruth.R;
import com.example.thirdearoftruth.notifications.LoginDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * @author dermotbrennan
 *
 * The class from which a registered user can login to the app and choose to either add acoustic events/sounds
 * and listen/ detect them on the current device, or simply receive notifications when they occur.
 *
 * To prevent
 */
public class LoginActivity extends AppCompatActivity implements LoginDialog.LoginDialogListener {
    // variables
    private static final String TAG = "LOGIN";
    EditText email, pWord;
    Button btnLogin;
    TextView tvForgotPassword;

    // Firebase variables
    FirebaseAuth mFirebaseAuth;

    boolean receiving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d(TAG, "Instance of LoginActivity created.");
        
        // animate the background
        RelativeLayout myLayout = findViewById(R.id.layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) myLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();
        Log.d(TAG, "Background animated");

        // setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Log-In");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Log.d(TAG, "Toolbar successfully initialised");

        // Authenticate the user credentials
        mFirebaseAuth = FirebaseAuth.getInstance();

        // associate variables with ids
        email = findViewById(R.id.email);
        pWord = findViewById(R.id.pword);
        btnLogin = findViewById(R.id.btn_login);
        tvForgotPassword = findViewById(R.id.forgot_password);

        btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt_email = email.getText().toString();
                String txt_password = pWord.getText().toString();

                if(TextUtils.isEmpty(txt_email) || TextUtils.isEmpty(txt_password)){
                    Toast.makeText(LoginActivity.this, "All fields are required to Log In", Toast.LENGTH_SHORT).show();
                } else{
                    mFirebaseAuth.signInWithEmailAndPassword(txt_email, txt_password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if(task.isSuccessful()){
                                        Toast.makeText(LoginActivity.this, "Logged-In", Toast.LENGTH_SHORT).show();

                                        // open the dialog to let the user choose their behaviour on this device
                                        openLoginDialog();
                                    } else{
                                        Toast.makeText(LoginActivity.this, "Authentication Unsuccessful", Toast.LENGTH_SHORT).show();
                                    }
                                } // end onComplete
                            });
                } // end if statement
            } // end OnClick
        });


        // redirect user to a password reset page
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // direct the user to the password reset activity
                Log.d(TAG, "Forgot password text clicked. Redirecting to activity to reset password");
                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
            }
        });
    } // end onCreate


    /**
     * After authentication and the user has decided if they are adding sounds and listening or simply
     * being notified to the occurrence of sounds from another device, they are directed to the home screen
     * (main activity) which will either show a message when only receiving or the 2 buttons for adding/detecting
     *
     *
     * @param receiving
     */
    public void loginToThirdEar(boolean receiving){
        // resubscribe the user to the topic of their userID!!!!!!!!
        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        mainIntent.putExtra("receiving", receiving); // the boolean value that determines how the ui will appear upon login
        startActivity(mainIntent);
        finish();
    } // end login to third ear


    /**
     * Calls the loginDialog that gives the user the choice of either listening/adding or simply receiving
     */
    public void openLoginDialog(){
        LoginDialog loginDialog = new LoginDialog();
        loginDialog.show(getSupportFragmentManager(), "Login Dialog");
    } // end openLoginDialog method


    @Override
    public void onListeningClicked() {
        receiving = false;
        loginToThirdEar(receiving);

    } // end onListeningClicked

    @Override
    public void onReceivingClicked() {
        receiving = true;
        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
        if(mUser != null) {
            String myUserId = mUser.getUid();
            FirebaseMessaging.getInstance().subscribeToTopic(myUserId); // check if the user is still subscribed
        }
        loginToThirdEar(receiving);

    } // end onReceivingClicked




} // end Login Activity
