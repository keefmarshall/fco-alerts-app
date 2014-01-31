package uk.co.eleusis.android.fcoalerts;

import uk.co.eleusis.android.util.DebugUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * We need to know when a notification has been cleared, so we no longer show
 * it in the 'inbox' style when the next one comes along.
 * 
 * We could write this as a WakefulBroadcastReceiver that starts an IntentService
 * to do the actual work, but in reality it isn't going to do anything that takes
 * any time, so for now we'll keep the functionality local to this Receiver. See
 * the GCMIntent* classes for an alternative example.
 * 
 * @author keithm
 *
 */
public class NotificationDeleteReceiver extends BroadcastReceiver 
{
	private static final String TAG = "NotificationDeleteReceiver";
	
    @Override
    public void onReceive(Context context, Intent intent) 
    {
    	// first off, just check that we got here
    	Log.d(TAG, "received del-notif intent: ");
    	DebugUtils.printIntent(intent);

    	NotificationStore store = new NotificationStore(context);
    	store.removeAllNotifications();
    }
}