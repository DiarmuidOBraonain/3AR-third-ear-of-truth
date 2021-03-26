/**
 * Notification and Cloud Messaging Channel and Request Management
 */
package com.example.thirdearoftruth.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.thirdearoftruth.R;
import com.example.thirdearoftruth.activities.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * When a user receives a notification from the foreground, whatever is written in this class is what
 * will be displayed to the user in the App
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {


    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();


        Intent intent = new Intent(this, MainActivity.class);
        boolean receiving = true;
        intent.putExtra("receiving", receiving);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "TAC")
                .setContentTitle(title)
                .setContentText(body)
                .setColor(Color.TRANSPARENT)
                .setSmallIcon(R.drawable.sound_notification)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // create id
        int notificationId = (int)System.currentTimeMillis();

        // check version of the device
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("TAC",
                    "demo",
                    NotificationManager.IMPORTANCE_HIGH);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // build the notification and notify user
        assert notificationManager != null;
        notificationManager.notify(notificationId, notificationBuilder.build());

        // log time notification sent
        Log.d("NOTIFICATION", "Notification Received by User : "+System.currentTimeMillis());

    }


}

