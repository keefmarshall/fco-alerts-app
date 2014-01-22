/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.eleusis.android.fcoalerts;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }
    public static final String TAG = "GCM Demo";

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + extras.toString());
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // Post notification of received message.
                sendNotification(extras);
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_stat_gcm)
        .setContentTitle("GCM Notification")
        .setStyle(new NotificationCompat.BigTextStyle()
        .bigText(msg))
        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void sendNotification(Bundle bundle) 
    {
    	NotificationPreferences nprefs = readNotificationPreferences();
    	if (nprefs.notifications) {return;}
    	
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        // When the user clicks on the notification, launch the FCO website using
        // the link in the alert
    	Uri uri = Uri.parse((String)bundle.get("link"));
    	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
    	
// 		This commented Intent just launches MainActivity for FCOAlerts
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, MainActivity.class), 0);

        String description = bundle.getString("description");
		NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_action_map)
        .setContentTitle(bundle.getString("title"))
        .setStyle(new NotificationCompat.BigTextStyle().bigText(description))
        .setContentText(description)
        .setDefaults(Notification.DEFAULT_ALL) // Buzz, Flash, Ping, Whee!
        .setContentIntent(contentIntent);

		Notification notification = mBuilder.build();
		notification.flags |= (Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL);
		
		Log.d(TAG, "Got defaults, before prefs = " + notification.defaults);
		if (nprefs.vibrate)
		{
			notification.defaults &= ~(Notification.DEFAULT_VIBRATE);
		}
		
		if (nprefs.sound)
		{
			notification.defaults &= ~(Notification.DEFAULT_SOUND);
		}
		Log.d(TAG, "Got defaults, after prefs = " + notification.defaults);
				
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }
    
    private class NotificationPreferences
    {
    	private boolean notifications = true;
    	private boolean vibrate = true;
    	private boolean sound = true;
    	
    	@Override public String toString()
    	{
    		return ("{'notf': " + notifications + ", 'vibrate': " + vibrate 
    				+ ", 'sound': " + sound + "}");
    	}
    }
    
    private NotificationPreferences readNotificationPreferences()
    {
    	NotificationPreferences nprefs = new NotificationPreferences();
    	
    	// The way I had to write this, the checkboxes are ticked to turn *off* the feature,
    	// so "notifications == true" means "disable all notifications" etc..
    	// [sorry!]
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	nprefs.notifications = sharedPref.getBoolean("notifications_checkbox", false);
    	nprefs.sound = sharedPref.getBoolean("sound_checkbox", false);
    	nprefs.vibrate = sharedPref.getBoolean("vibration_checkbox", false);

    	Log.d(TAG, "Got notification prefs: " + nprefs);
    	
    	return nprefs;
    }
}
