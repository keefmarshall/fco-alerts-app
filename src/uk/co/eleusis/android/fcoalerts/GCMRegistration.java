package uk.co.eleusis.android.fcoalerts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.http.HttpStatus;

import uk.co.eleusis.android.util.Messager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * The Goodle demo app has all this functionality in the main activity class
 * - I find that a lot of clutter, getting in the way of any user interactions,
 * and I also suspect this will be largely re-usable in other apps if I do the
 * same type of functionality, so I've split out the relevant code into this class.
 * 
 * It handles registering the app+device combination with Google's Cloud Messaging
 * system
 * 
 * @author keithm
 *
 */
public class GCMRegistration 
{
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static final String TAG = "GCMRegistration";

    // Initialised in constructor
	private Activity activity;
	private String senderId;
	
	// Initialised in init() method
    private GoogleCloudMessaging gcm;
    
    // Optional, set by caller if required:
	private RegidChangeListener listener;

	

	public GCMRegistration(Activity activity, String senderId)
	{
		this.activity = activity;
		this.senderId = senderId;
	}
	
	public void init()
	{
        this.gcm = GoogleCloudMessaging.getInstance(activity);
	}
	
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public boolean checkPlayServices() 
    {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) 
        {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) 
            {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            else 
            {
                Log.i(TAG, "This device is not supported.");
                activity.finish();
            }
            return false;
        }
        return true;
    }


    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) 
    {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    public String getRegistrationId(Context context) 
    {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) 
        {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) 
        {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    public void registerInBackground(final Context context) 
    {
        new AsyncTask<Void, Void, String>() 
        {
            @Override
            protected String doInBackground(Void... params) 
            {
                String msg = "";
                try 
                {
                    if (gcm == null) 
                    {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    String regid = gcm.register(senderId);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend(regid);

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);

                    if (listener != null)
                	{
                		listener.regidChanged(regid);
                	}
                } 
                catch (IOException ex) 
                {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) 
            {
                Messager.toast(activity, msg + "\n");
            }
            
        }.execute(null, null, null);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) 
    {
        try 
        {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } 
        catch (NameNotFoundException e) 
        {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) 
    {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return activity.getSharedPreferences(GCMRegistration.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend(String regid)
    {
    	// This needs some work to make it more secure - we don't really want just anyone
    	// able to send a random ID into this URL. But this is a prototype for now..
    	// Also needs some better error handling - if we can't save the regid online, we
    	// need to log that fact locally because no notification will get sent until we  get
    	// the ID to the online server.
    	try 
    	{
			URL url = new URL(Constants.SERVER_URL + "register/");
			String params = "regid=" + URLEncoder.encode(regid, "UTF-8");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", Integer.toString(params.getBytes().length));

			conn.connect();
			OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());

			writer.write(params);
			writer.flush();

			// flush the input stream, ignoring the content
			BufferedReader reader = 
					new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder content = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) 
			{
				content.append(line);
			}
			writer.close();
			reader.close();         
			
			if (conn.getResponseCode() != HttpStatus.SC_OK)
			{
				String errmsg = 
						"Error sending registration code: " + conn.getResponseCode() + "\n" +
						"Response was\n" + content;
				Log.e(TAG, errmsg);
				throw new RuntimeException(errmsg);
			}
			
			conn.disconnect();
		} 
    	catch (MalformedURLException e) 
    	{
    		// won't happen
			Log.e(TAG, "Error in URL!", e);
		}
    	catch (IOException e)
    	{
    		// error with URL connection, not much we can do, inform user:
    		Messager.simpleErrorAlert(activity, R.string.register_error);
    		throw new RuntimeException("Registration failed!", e);
    	}
    }

    public void addRegidChangeListener(RegidChangeListener listener)
    {
    	this.listener = listener;
    }
}
