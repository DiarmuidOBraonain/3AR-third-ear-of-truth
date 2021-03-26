package com.example.thirdearoftruth.notifications;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

public class LoginDialog extends AppCompatDialogFragment {

    private LoginDialogListener loginDialogListener;
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Welcome Back")
                .setMessage("Will you be Listening for sounds with this device or simply receiving notifications?")
                .setPositiveButton("Listening", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loginDialogListener.onListeningClicked();
                    }
                })
                .setNegativeButton("Receiving", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loginDialogListener.onReceivingClicked();
                    }
                });

        return builder.create();

    } // end onCreate


    /**
     * Interface that allows communication between the dialog and the activity that shows it
     */
    public interface LoginDialogListener{
        void onListeningClicked();
        void onReceivingClicked();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            loginDialogListener = (LoginDialogListener) context;
        }catch (ClassCastException e){
            throw new ClassCastException(context.toString()+" must implement LoginDialogListener");
        }

    }
}
