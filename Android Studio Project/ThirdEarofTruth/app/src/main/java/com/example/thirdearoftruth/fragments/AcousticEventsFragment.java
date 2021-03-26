/**
 * Fragment of the User Interface's Home Screen
 */
package com.example.thirdearoftruth.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.thirdearoftruth.R;
import com.example.thirdearoftruth.adapters.EventAdapter;
import com.example.thirdearoftruth.models.AcousticEvent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dermotbrennan
 *
 * A fragment of the MainActivity that displays a list of items representing the Acoustic Events
 * added by the User for the purposes of data management. When long-pressed the User has the option
 * to delete an Acoustic Event from their library in the database.
 *
 * Only non-default events are shown and are therefor the only events that can be deleted by the user.
 *
 */
public class AcousticEventsFragment extends Fragment {
    // instance vars
    private RecyclerView recyclerView;

    private EventAdapter eventAdapter;

    private List<AcousticEvent> mEvents;

    TextView tvDisplayEvents;

    // methods

    /**
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_acoustic_events, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mEvents = new ArrayList<>();

        // retrieve the users from firebase
        readEvents();
        tvDisplayEvents = view.findViewById(R.id.display_events);


        return view;

    } // end onCreateView


    /**
     * Gather the acoustic events added by the user (NO DEFAULTS) and adapt them into views.
     * This allows the user to delete any sounds/events they no longer wish to be notified of or are
     * being alerted to incorrectly
     */
    private void readEvents() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d("EVENT_FRAG", "User events : "+firebaseUser.getUid());

        DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference("AcousticEvents").child(firebaseUser.getUid());
        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                    mEvents.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        final AcousticEvent acousticEvent = dataSnapshot.getValue(AcousticEvent.class);
                        assert acousticEvent != null;
                        if(acousticEvent.isDefaultEvent()==false) {
                            mEvents.add(acousticEvent);
                            Log.d("EVENT_FRAG", "Event was added");
                        } // end if

                        if (mEvents.isEmpty()){
                            String displayMessage = "No Sounds Added Yet";

                            tvDisplayEvents.setText(displayMessage);
                        }

                    } // end enhanced for

                    eventAdapter = new EventAdapter(getContext(), mEvents);
                    recyclerView.setAdapter(eventAdapter);

            } // end onDataChange

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Error", "Could not read events from Database");
            }
        }); // end mDatabaseReference valueEventListener


    } // end readUsers





} //  end fragment