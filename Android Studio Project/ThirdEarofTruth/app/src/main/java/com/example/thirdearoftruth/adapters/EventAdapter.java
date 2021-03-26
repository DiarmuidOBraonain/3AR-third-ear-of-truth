package com.example.thirdearoftruth.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thirdearoftruth.R;
import com.example.thirdearoftruth.models.AcousticEvent;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder>{

    // firebase vars
    FirebaseUser mUser;

    // instance vars
    private Context mContext;
    private List<AcousticEvent> mEvents;

    // vars
    String theLastMessage;


    // constructors
    public EventAdapter(Context mContext, List<AcousticEvent> mEvents){
        this.mContext = mContext;
        this.mEvents = mEvents;

        Log.d("EVENT_ADAPTER", "mEvents: "+getItemCount());
        mUser = FirebaseAuth.getInstance().getCurrentUser();


    } // end constructor


    public List<AcousticEvent> getmEvents() {
        return mEvents;
    }

    public void setmEvents(List<AcousticEvent> mEvents) {
        this.mEvents = mEvents;
    }

    // methods
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.event_item, parent, false);
        return new EventAdapter.ViewHolder(view);
    } // end onCreateViewHolder

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        final AcousticEvent acousticEvent =  mEvents.get(position);
        final String eventId = acousticEvent.getId();
        String name = acousticEvent.getName();
        String duration = String.valueOf(acousticEvent.getDuration())+" seconds";
        Log.d("EVENT_ADAPTER", "Event in list : "+eventId+" "+name);

        //final String eventId = acouvent.getId();
        holder.tvEventName.setText(name);
        holder.tvDuration.setText(duration);


        // when the view of the Acoustic Event is longpressed, the user has the option to delete it
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure you want to delete this Sound?");
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("AcousticEvents").child(mUser.getUid());
                        eventRef.child(eventId)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Event was deleted
                                        Toast.makeText(mContext, "Sound Deleted", Toast.LENGTH_SHORT).show();
                                    } // end onSuccess
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Event was not deleted
                                        Toast.makeText(mContext, "Sound Could not be deleted", Toast.LENGTH_SHORT).show();
                                    } //  end onFailure
                                });
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show(); // finally show the dialog to the user
                return false;
            }
        }); // end on longclick


    } // end onBindViewHolder

    @Override
    public int getItemCount() {
        return mEvents.size();
    }

    /**
     * class to hold the user data to be displayed in an event_item layout file
     */
    public class  ViewHolder extends RecyclerView.ViewHolder{

        // instance vars
        public TextView tvEventName, tvDuration;

        // constructors
        public ViewHolder(View itemView){
            super(itemView);

            tvEventName = itemView.findViewById(R.id.event_name);

            tvDuration = itemView.findViewById(R.id.duration);
        } // end constructor


    } // end viewholder class



} // end Adapter
