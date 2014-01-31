package uk.co.eleusis.android.fcoalerts;

import uk.co.eleusis.android.util.DebugUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Very simple receiver - when a user clicks on a notification with only a single
 * alert, we forward them straight to the corresponding URL.
 * 
 * However, if we do it right from the notification, we won't clear the alert from
 * the store, so we have to do that as well here first, before forwarding them on.
 * 
 * @author keithm
 *
 */
public class NotificationForwardReceiver extends BroadcastReceiver
{
    private static final String TAG = "NotificationDeleteReceiver";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        // first off, just check that we got here
        Log.d(TAG, "received forward-notif intent: ");
        DebugUtils.printIntent(intent);

        // delete the notification:
        new NotificationStore(context).removeAllNotifications();
        
        // now forward.. 
        Intent forwardIntent;
        if (intent.getData() != null)
        {
            Log.d(TAG, "Got data URI, forwarding to URL..");
            forwardIntent = new Intent(Intent.ACTION_VIEW, intent.getData());
        }
        else
        {
            Log.d(TAG, "No data URI, back to MainActivity..");
            forwardIntent = new Intent(context, MainActivity.class);
        }
        
        forwardIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        forwardIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); 

        context.startActivity(forwardIntent);
    }

}
