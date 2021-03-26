package com.example.thirdearoftruth.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.thirdearoftruth.R;
import com.example.thirdearoftruth.adapters.PagerAdapter;
import com.example.thirdearoftruth.fragments.AcousticEventsFragment;
import com.example.thirdearoftruth.fragments.HomeFragment;
import com.example.thirdearoftruth.fragments.ProfileFragment;
import com.example.thirdearoftruth.models.AcousticEvent;
import com.example.thirdearoftruth.models.DefaultEventsManager;
import com.example.thirdearoftruth.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * @author dermotbrennan
 *
 * The primary 'Home' page of the application for currently Logged-In existing Users.
 *
 * The Activity itself contains 3 fragments- Home, Sounds and Profile and acts as the central
 * navigation point of the application.
 *
 */
public class MainActivity extends AppCompatActivity {
    // declare variables
    public static final String TAG = "MAIN";

    CircleImageView profileImage;
    TextView username;

    // firebase related variables
    FirebaseUser firebaseUser;
    DatabaseReference reference;
    String myUserId, mUserName;

    /**
     * A boolean passed from the login activity that determines the configuration of the app.
     * If true, the user is subscribed to the topic and will receive notifications
     * If false, the user may add new Acoustic Events or use the Event Detection Service
     */
    boolean receiving;

    // implement methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkThreePermissions();
        setContentView(R.layout.activity_main);
        //  animate the background
        LinearLayout linearLayout = findViewById(R.id.layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) linearLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        // setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.showOverflowMenu();
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        profileImage = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            myUserId = firebaseUser.getUid();
            //FirebaseMessaging.getInstance().subscribeToTopic(myUserId); // check if the user is still subscribed
        }


        // get the current user's details
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                mUserName = user.getUsername();
                username.setText(user.getUsername());
                if (user.getImageURL().equals("default")) {
                    profileImage.setImageResource(R.mipmap.ic_launcher);
                } else {
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profileImage);
                } // end profile image if statement
            } // end onDataChange

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            } // end onCancelled
        });

        // Retrieve the boolean 'receiving' from the intent to determine the appearance of this
        // particular instance's configuration
        Intent loginIntent = getIntent();
        receiving = loginIntent.getBooleanExtra("receiving", false);

        // set up the scrolling tabs view
        // this could be put in a method eg. public void setupTabs() and called in the onCreate()
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager viewPager = findViewById(R.id.pager);
        PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager());


        // setup the home fragment that will appear differently depending on the user's choice after
        // login. Pass the boolean 'receiving' to the fragment to allow it to show or hide the navigation
        // buttons
        HomeFragment homeFragment = new HomeFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("receiving", receiving);
        homeFragment.setArguments(bundle);


        // add the fragments into the pager adapter
        pagerAdapter.addFragment(homeFragment, "Home");
        pagerAdapter.addFragment(new AcousticEventsFragment(), "Sounds");
        pagerAdapter.addFragment(new ProfileFragment(), "Profile");


        // setup the tab layout
        viewPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(viewPager);


        // use to keep track of the tabs in order to customise them
        tabLayout.getTabAt(0).setIcon(R.drawable.home_icon);
        tabLayout.getTabAt(1).setIcon(R.drawable.sounds_icon);
        tabLayout.getTabAt(2).setIcon(R.drawable.profile_icon);

    } // end oncreate


    /**
     * Inflate the options menu to allow the dropdown items to be viewed
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    } // end onCreateOptionsMenu


    /**
     * Handle the behaviour of the dropdown menu items when selected by the user
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

            case R.id.logout:
                FirebaseAuth.getInstance().signOut();

                FirebaseMessaging.getInstance().unsubscribeFromTopic(myUserId); // resubscribe the user on login!!!!!!!!!

                startActivity(new Intent(getApplicationContext(), StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;

        }
        return false;
    }


    /**
     * Just as in the StartActivity, ensure that the 3 permissions necessary for the app to function have not been revoked after
     * user has granted them.
     */
    public void checkThreePermissions() {

        // MICROPHONE/ RECORD AUDIO
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Needed");
                builder.setMessage("Access to device microphone is essential for detecting sounds and registering new ones!");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //grant the permission
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
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
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Needed");
                builder.setMessage("Access to device storage is necessary to record new sounds when registering them and for storing their data when detecting them");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //grant the permission
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
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
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Needed");
                builder.setMessage("Writing to device storage is essential for recording new sounds when registering them");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //grant the permission
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
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


    /**
     * Determines the current status of the user- online or offline, and updates the database entry accordingly
     *
     * @param status
     */
    private void status(String status) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }

    /**
     * Sets the user's status to online
     */
    @Override
    protected void onResume() {
        super.onResume();
        status("online");
    }

    /**
     * Sets the user's status to offline
     */
    @Override
    protected void onPause() {
        super.onPause();
        status("offline");
    }


}