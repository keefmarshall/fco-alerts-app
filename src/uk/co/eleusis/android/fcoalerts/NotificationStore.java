package uk.co.eleusis.android.fcoalerts;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Initially this was an SQLite DB but that's a bit of overkill for this feature.
 * 
 * We just need to store a list of the undismissed notifications, so we can display
 * them in the INBOX format. When dismissed, we remove them all.
 * 
 * So, we use our own SharedPreferences object, modelled on the way GCMRegistration
 * stores the registration ID. It uses the String type to store a concatenated version
 * of the notification title/desc for each outstanding notification, latest first,
 * separated by '<nsep/>'.
 * 
 * This only works if we can dismiss them all at the same time - this method isn't so
 * good for dismissing one or two.
 *   
 */
public class NotificationStore
{
    private static final String TAG = "NotificationStore";
    
    private static final String NOTIFICATIONS_KEY = "notificiations";
    private static final String SEP = "<nsep/>";
    
    private SharedPreferences prefs;
    
	public NotificationStore(Context context) 
	{
	    prefs = context.getSharedPreferences(
	            NotificationStore.class.getSimpleName(), Context.MODE_PRIVATE);
	}

	public String[] getNotificationStrings()
	{
	    String[] nstrings = null;
	    String notifsAsString = prefs.getString(NOTIFICATIONS_KEY, null);
	    if (notifsAsString != null)
	    {
	        nstrings = notifsAsString.split(SEP);
	    }
	    return nstrings;
	}
	
	public void storeNotification(NotifiedAlert alert)
	{
	    // remove 'Latest update' from description, if present:
	    String description = alert.getDescription().trim();
	    
	    if (description.startsWith("Latest update: "))
	    {
	        description = description.substring("Latest update: ".length());
	    }
	    
	    if (description.startsWith("Summary - "))
	    {
	        description = description.substring("Summary - ".length());
	    }
	    
	    // convert to HTML string:
	    String htmlString = new StringBuilder()
	        .append("<b>")
	        .append(alert.getTitle())
	        .append("</b> ")
	        .append(description)
	        .toString();
	    
	    Log.d(TAG, "Storing htmlString "  + htmlString);
	    
        String notifsAsString = prefs.getString(NOTIFICATIONS_KEY, null);
        if (notifsAsString == null)
        {
            notifsAsString = htmlString;
        }
        else
        {
            notifsAsString = htmlString + SEP + notifsAsString;
        }
        
        prefs.edit().putString(NOTIFICATIONS_KEY, notifsAsString).commit();
	}
	
	public void removeAllNotifications()
	{
	    prefs.edit().remove(NOTIFICATIONS_KEY).commit();
	}
}
