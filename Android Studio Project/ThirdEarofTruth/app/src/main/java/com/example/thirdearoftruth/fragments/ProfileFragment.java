package com.example.thirdearoftruth.fragments;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.thirdearoftruth.R;
import com.example.thirdearoftruth.models.AcousticEvent;
import com.example.thirdearoftruth.models.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

/**
 * @author dermotbrennan
 *
 * A fragment of the MainActivity representing the User's profile page. Basic user data can be viewed
 * here and the user's profile picture can be changed by uploading a new image to Firebase Cloud Storage.
 * The database reference to the current user is updated accordingly to account for the new ImageURL
 */
public class ProfileFragment extends Fragment {
    // constant variables
    private static final String TAG = "PROFILE_FRAGMENT";

    // UI variables
    CircleImageView profileImage;
    TextView tvUserName, tvEmail, tvSoundsNum, tvDefaultsNum;

    // Authentication, Storage and Database Variables
    DatabaseReference mDatabaseReference;
    FirebaseUser mFirebaseUser;
    String mUserId, mUserEmail, myUploadedSounds, myDefaults;
    StorageReference mStorageReference;

    // Image Upload variables
    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask<UploadTask.TaskSnapshot> uploadTask;

    List<AcousticEvent> mAcousticEvents;
    List<AcousticEvent> defaultEvents;


    /**
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the view
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // associate vars with ids
        profileImage = view.findViewById(R.id.profile_image);
        tvUserName = view.findViewById(R.id.username);
        tvEmail = view.findViewById(R.id.email);
        tvSoundsNum = view.findViewById(R.id.sounds_num);
        tvDefaultsNum = view.findViewById(R.id.defaults_num);

        // handle firebase storage
        mStorageReference = FirebaseStorage.getInstance().getReference("profiles");

        // connect to firebase to get logged-in user's details
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserId = mFirebaseUser.getUid();

        mDatabaseReference = FirebaseDatabase.getInstance().getReference("Users").child(mUserId);
        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                tvUserName.setText(user.getUsername());
                if (user.getImageURL().equals("default")) {
                    profileImage.setImageResource(R.drawable.ic_add_profile_pic);
                } else {
                    Glide.with(getContext()).load(user.getImageURL()).into(profileImage);
                }
            } // end onDataChange

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Error", "User not found in database");
            } // end onCancelled
        }); // end user's ValueEventListener

        // show the user's email address in the profile tab
        mUserEmail = mFirebaseUser.getEmail();
        tvEmail.setText(mUserEmail);

        // establish and display a count of both the user's uploaded events and the number of
        // default events in their library of Acoustic Events
        countAcousticEvents();

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImage();
            }
        });

        return view;
    }


    /**
     * Count the number of Acoustic Events added to the database by the user and the number of default
     * Acoustic Events present in their library and display them in the textviews on the UI.
     */
    private void countAcousticEvents(){
        // display the number of Sounds (AcousticEvents) this user has saved
        mAcousticEvents = new ArrayList<>();
        defaultEvents = new ArrayList<>();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference("AcousticEvents").child(mUserId);
        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mAcousticEvents.clear();
                defaultEvents.clear();
                if(!snapshot.exists()){
                    tvSoundsNum.setText("0 Sounds added");
                    tvDefaultsNum.setText("0 Default Sounds");
                } else {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        AcousticEvent acousticEvent = dataSnapshot.getValue(AcousticEvent.class);
                        if(!acousticEvent.isDefaultEvent()) {
                            // if the sound is not a default i.e. it was added by the user, put it in
                            // the list
                            mAcousticEvents.add(acousticEvent);
                        } else{
                            defaultEvents.add(acousticEvent);
                        }
                    } // end snapshot for loop
                    myUploadedSounds = String.valueOf(mAcousticEvents.size());
                    tvSoundsNum.setText(myUploadedSounds+" Sounds Added");

                    myDefaults = String.valueOf(defaultEvents.size());
                    tvDefaultsNum.setText(myDefaults +" Default Sounds");
                } // end if statement

            } // end onDataChange

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Error", "No Acoustic Events found in database for this user");
            } // end onCancelled
        }); // end user's ValueEventListener



    } // end countAcousticEvents



    /**
     * opens the image to be uploaded to Firebase Storage and to be used as the new profile image
     */
    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);

    }

    /**
     * Retrieve the file extension of the image to be uploaded
     * @param uri
     * @return
     */
    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    /**
     * Uploads the selected image to Cloud Storage to be used as the user's new profile picture
     */
    private void uploadImage() {
        final ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Uploading");
        progressDialog.show();

        if (imageUri != null) {
            final StorageReference fileReference = mStorageReference.child(System.currentTimeMillis()
                    + "." + getFileExtension(imageUri));

            uploadTask = fileReference.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException());
                    }

                    return fileReference.getDownloadUrl();

                } // end task

            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        String mUri = downloadUri.toString();

                        // update the profile image url in the database reference for this user
                        mDatabaseReference = FirebaseDatabase.getInstance().getReference("Users").child(mFirebaseUser.getUid());
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("imageURL", mUri);
                        mDatabaseReference.updateChildren(map);

                        progressDialog.dismiss();
                    } else {
                        Toast.makeText(getContext(), "Failed to Upload!", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }

                }// end onComplete

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }); // end uploadtask methods

        } else {
            Toast.makeText(getContext(), "No Image Selected", Toast.LENGTH_SHORT).show();

        } // end if

    } // end uploadImage method


    /**
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            if (uploadTask != null && uploadTask.isInProgress()) {
                Toast.makeText(getContext(), "Upload in Progress", Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }

        }


    } // end onActivityResult



}