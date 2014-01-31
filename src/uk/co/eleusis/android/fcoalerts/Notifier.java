package uk.co.eleusis.android.fcoalerts;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.util.Log;

/**
 * Managing multiple notifications turns out to be hard - we need to 
 * encapsulate this stuff here.
 * 
 * Some of this originally came from Google's example GcmIntentService
 * 
 * @author keithm
 *
 */
public class Notifier 
{
	public static final String TAG = "Notifier";
    public static final int NOTIFICATION_ID = 1;
    
    private NotificationManager mNotificationManager;
    private NotificationStore mNotificationStore;

    private Context context;
    
    public Notifier(Context context)
    {
    	this.context = context;
        mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationStore = new NotificationStore(context);
    }
    
    // Put the message into a notification and post it. Use this for the simplest
    // of messages, or errors. We don't use this for FCO alerts.
    public void sendSimpleMessage(String msg) 
    {
        PendingIntent contentIntent = 
        		PendingIntent.getActivity(context, 0,
        				new Intent(context, MainActivity.class), 0);

        NotificationCompat.Builder builder = 
        		new NotificationCompat.Builder(context)
			        .setSmallIcon(R.drawable.ic_stat_gcm)
			        .setContentTitle("GCM Notification")
			        .setStyle(new NotificationCompat.BigTextStyle()
			        .bigText(msg))
			        .setContentText(msg);

        builder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void sendNotification(NotifiedAlert alert) 
    {
    	NotificationPreferences nprefs = readNotificationPreferences(context);
    	if (nprefs.notifications) {return;}

    	mNotificationStore.storeNotification(alert);
    	
    	String[] nstrings = mNotificationStore.getNotificationStrings();
    	Notification notification;
    	if (nstrings == null || nstrings.length == 1)
    	{
    	    notification = buildSingleNotification(alert);
    	}
    	else
    	{
    	    notification = buildInboxNotification(nstrings);
    	}
        
		notification.flags |= (Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL);
		
		// We need to know when the notification goes away:
		Intent deleteIntent = new Intent(context, NotificationDeleteReceiver.class);
		deleteIntent.setData(Uri.parse(alert.getMessageId()));
		notification.deleteIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0);
		
		// Adjust notification behaviour based on preferences:
		modifyNotification(notification, nprefs);

        mNotificationManager.notify(NOTIFICATION_ID, notification);
//		mNotificationManager.notify(alert.getTitle(), NOTIFICATION_ID, notification);
    }

    private Notification buildInboxNotification(String[] nstrings)
    {
        // When the user clicks on the notification, launch the MainActivity class
//        Intent intent = new Intent(context, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // try to get back to the same one
//        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Intent intent = new Intent(context, NotificationForwardReceiver.class);
        PendingIntent contentIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        for (int i = 0; i < Math.min(6,  nstrings.length); i++)
        {
            style.addLine(Html.fromHtml(nstrings[i]));
        }
        
        style.setSummaryText("FCO Alerts");
        if (nstrings.length > 6)
        {
            style.setSummaryText("+" + (nstrings.length - 6) + " more");
        }
        
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_action_map)
                    .setContentTitle(nstrings.length + " new alerts")
                    .setStyle(style)
                    .setContentText("FCO Alerts")
                    .setContentInfo(Integer.toString(nstrings.length))
                    .setDefaults(Notification.DEFAULT_ALL) // Buzz, Flash, Ping, Whee!
                    .setContentIntent(contentIntent)
                    .setNumber(nstrings.length);
        

        Notification notification = builder.build();
        return notification;
    }

    private Notification buildSingleNotification(NotifiedAlert alert)
    {
        // When the user clicks on the notification, launch the FCO website using
        // the link in the alert. This class of ours also ensures that the notification
        // gets cleared from the store as well.
    	Intent intent = new Intent(context, NotificationForwardReceiver.class);
    	intent.setData(Uri.parse(alert.getLink()));
    	
    	PendingIntent contentIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        String description = alert.getDescription();
		NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
			        .setSmallIcon(R.drawable.ic_action_map)
			        .setContentTitle(alert.getTitle())
			        .setContentText(description)
			        .setStyle(new NotificationCompat.BigTextStyle().bigText(description))
			        .setDefaults(Notification.DEFAULT_ALL) // Buzz, Flash, Ping, Whee!
			        .setContentIntent(contentIntent);

		Notification notification = builder.build();
        return notification;
    }
    
    public void clearNotification()
    {
        mNotificationManager.cancel(NOTIFICATION_ID);
        mNotificationStore.removeAllNotifications();
    }
    
    /////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////
    // Notification preferences
    
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
    
    private NotificationPreferences readNotificationPreferences(Context context)
    {
    	NotificationPreferences nprefs = new NotificationPreferences();
    	
    	// The way I had to write this, the checkboxes are ticked to turn *off* the feature,
    	// so "notifications == true" means "disable all notifications" etc..
    	// [sorry!]
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    	nprefs.notifications = sharedPref.getBoolean("notifications_checkbox", false);
    	nprefs.sound = sharedPref.getBoolean("sound_checkbox", false);
    	nprefs.vibrate = sharedPref.getBoolean("vibration_checkbox", false);

    	Log.d(TAG, "Got notification prefs: " + nprefs);
    	
    	return nprefs;
    }

    private void modifyNotification(Notification notification, NotificationPreferences nprefs)
    {
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
    }
}
