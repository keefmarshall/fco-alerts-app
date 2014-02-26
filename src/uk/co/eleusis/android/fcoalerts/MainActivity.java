package uk.co.eleusis.android.fcoalerts;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import uk.co.eleusis.android.util.Messager;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.gson.Gson;
import com.markupartist.android.widget.PullToRefreshListView;
import com.markupartist.android.widget.PullToRefreshListView.OnRefreshListener;

/**
 * MainActivity for FCO alerts - this is mostly a copy of DemoActivity from
 * the Google code example, but adjusted for my needs.
 * 
 * A lot of it is concerned with the GCM registration process - this registers the
 * current device with Google's Cloud Messaging system. Our application server at 
 * the other end posts messages to GCM, with a device-specific key so that GCM 
 * knows where to post them to.
 * 
 * @author keithm
 *
 */
public class MainActivity extends ListActivity implements RegidChangeListener
{

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    private static final String SENDER_ID = SecretKeys.GOOGLE_PROJECT_NUMBER;

    /**
     * Tag used on log messages.
     */
    private static final String TAG = "FCOAlerts";

    private Context context;
    private String regid;
    
    private GCMRegistration gcmreg;
    
    private SimpleAdapter alertListAdapter;
    private List<Map<String, Object>> alerts;
    
    private PreferenceChangeListener preferenceListener;
    private Notifier notifier;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        initialiseUI();
        
        context = getApplicationContext();

        preferenceListener = new PreferenceChangeListener();  
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener);

        gcmreg = new GCMRegistration(this, SENDER_ID);
        
        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (gcmreg.checkPlayServices()) 
        {
        	gcmreg.init();
            regid = gcmreg.getRegistrationId(context);

            if (regid.isEmpty()) 
            {
            	gcmreg.addRegidChangeListener(this);
                gcmreg.registerInBackground(context);
            }
        }
        else 
        {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
        
        notifier = new Notifier(this);
    }
    
    private void initialiseUI()
    {
        setContentView(R.layout.activity_main);
        getActionBar().setTitle(R.string.app_title);
        
        // Special pull-to-refresh functionality, fires when pulled
        // Requires external library, see https://github.com/johannilsson/android-pulltorefresh
        ((PullToRefreshListView) getListView()).setOnRefreshListener(new OnRefreshListener() 
        {
            @Override
            public void onRefresh() 
            {
            	Log.v(TAG, "pull-to-refresh: onRefresh() called..");
            	
                // Do work to refresh the list here.
                fetchLatestAlerts();
            }
        });
    }
    
    /////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////
    // LIFECYCLE EVENTS

    @Override
    protected void onStart() 
    {
        super.onStart();
        
        // Fetch the latest feeds and show them on the home page
        fetchLatestAlerts();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
    	// NOTE! the pull-to-refresh thing seems to add one extra row, so we have
    	// to take one off the list position to get the right item here!
        if (position > 0)
        {
        	Map<String, Object> alert = alerts.get(position - 1);
        	Uri uri = Uri.parse((String)alert.get("link"));
        	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        	startActivity(intent);
        }
        // else do nothing, the list is probably still initialising..
    }
    
    @Override
    protected void onResume() 
    {
        super.onResume();
        // Check device for Play Services APK.
        gcmreg.checkPlayServices();
        fetchLatestAlerts();
    }

    /**
     * this is called when the screen rotates or the keyboard comes up.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        initialiseUI();
    }


    /////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////
    // ALERTS

    private void fetchLatestAlerts()
    {
    	AsyncTask<String, Integer, List<Map<String, Object>>> requestTask = 
    			new AsyncTask<String, Integer, List<Map<String, Object>>>()
    	{

    		@Override
    		protected List<Map<String, Object>> doInBackground(String... params) 
    		{
    	        ServerComms comms = new ServerComms();
    			String content = "";
    			try 
    			{
    				// need regid to be set, if app version changes, or app is new, we must wait
    				int retries = 10;
    				long delay = 100; 
    				while ( (MainActivity.this.regid == null || MainActivity.this.regid.isEmpty()) 
    						&& retries > 0)
    				{
    					Log.d(TAG, "Retrying for regid..");
    					Thread.sleep(delay);
    					retries--;
    				}

    				String url = params[0];
    				if (regid != null && !regid.isEmpty()) // hopefully should be set
    				{
    					url = params[0] + "byDevice/" + MainActivity.this.regid;
    				}
    				Log.d(TAG, "Got URL = " + url);
    				
    				content = comms.getRequest(url);
    			}
    			catch (IOException e) 
    			{
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			} 
    			catch (InterruptedException e) 
    			{
    				// Shouldn't happen
					e.printStackTrace();
				}
    			
    			List<Map<String, Object>> alerts = comms.parseJsonList(content);
    	    	cleanUpAlerts(alerts);
    			return alerts;
    		}
    		
    	    protected void onPostExecute(List<Map<String, Object>> alerts)
    	    {
    	    	MainActivity.this.alerts = alerts;
    	    	drawAlerts();
    	        notifier.clearNotification();
    	    	((PullToRefreshListView) getListView()).onRefreshComplete();
    	    }

			private void cleanUpAlerts(List<Map<String, Object>> alerts) {
				// date is: 2013-12-19T14:52:18Z
    	    	SimpleDateFormat dateParser =  
    	    			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.UK);
    	    	dateParser.setTimeZone(TimeZone.getTimeZone("Europe/London"));
    	    	DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(context);
    	    	DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
    	    	
    	    	for (Map<String, Object> alert : alerts)
    	    	{
    	    		alert.put("description", Html.fromHtml((String)alert.get("description")));
    	    		String dateString, timeString;
					try {
						Date date = dateParser.parse((String)alert.get("date"));
						dateString = dateFormat.format(date);
						timeString = timeFormat.format(date);
					} catch (ParseException e) {
						Log.e(TAG, "date parse error for date " + alert.get("date") + ": ", e);
						dateString = (String)alert.get("date");
						timeString = "";
					}
    	    		alert.put("date", dateString + " " + timeString);
    	    	}
			}

    	};
    	
    	requestTask.execute(Constants.SERVER_URL + "latest/");
    }
        
    private void drawAlerts()
    {
    	// need to strip HTML from descriptions - this is hacky, probably should build
    	// my own list adapter..
    	Log.v(TAG, "Drawing alerts list.");
    	
    	alertListAdapter = new SimpleAdapter(
                this, alerts, R.layout.alert_item, 
                new String[] { "title", "description", "date" },
                new int[] { R.id.title, R.id.description, R.id.timestamp });
    	setListAdapter(alertListAdapter);
    }
    

    /////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////
    // MENU SETTINGS
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        // Handle item selection
        switch (item.getItemId()) 
        {
        case R.id.action_settings:
            startSettingsDisplay();
            return true;
            
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void startSettingsDisplay()
    {
        Intent settingsIntent = new Intent().setClass(this, SettingsDisplay.class);
        startActivity(settingsIntent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
    }
    

    /////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////
    // Preferences
    
    private class PreferenceChangeListener implements OnSharedPreferenceChangeListener 
    {
        @SuppressWarnings("unchecked") // casting to Set<String>
		@Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) 
        {
            // Function to manage changes
            //somethingChanged();
        	Log.d(TAG, "Preference changed, key = " + key);
        	if (key.equals("countries"))
        	{
	        	Object thing = prefs.getStringSet(key, null);
	        	Log.d(TAG, "'" + key + "' contains object of class: " + thing.getClass().getName());
	        	Log.d(TAG, ".. and contents are: " + thing.toString());
	        	// turns out 'countries' is a HashSet, containing the contents of the checked 
	        	// entries e.g.: [Angola, Barbados, Turkey, Afghanistan, American Samoa, Bahrain]
	        	if (thing != null)
	        	{
	        		saveCountriesToServer((Set<String>)thing);
	        	}
        	}
        	else if (key.equals("all_countries_checkbox"))
        	{
        		boolean checked = prefs.getBoolean("all_countries_checkbox", false);
        		if (checked)
        		{
        			saveCountriesToServer((Set<String>) prefs.getStringSet("countries", null));
        		}
        		else
        		{
        			removeCountriesFromServer();
        		}
        		
        	}
        }
    }


	private void saveCountriesToServer(final Set<String> countries) 
	{
		// TODO: something sensible if there's no network connection! Leave it for now, this is
		// only a prototype at the moment
		
		// We're sending the country list to the server with the regid:
    	AsyncTask<String, Integer, String> requestTask = 
    			new AsyncTask<String, Integer, String>()
    	{

    		@Override
    		protected String doInBackground(String... params) 
    		{
    	        ServerComms comms = new ServerComms();
    			String content = "";
    			try 
    			{
    				Map<String, String> postParams = new HashMap<String, String>();
    				postParams.put("countries", new Gson().toJson(countries));
    				postParams.put("regid", MainActivity.this.regid);
    				content = comms.postRequest(params[0], postParams);
    			}
    			catch (IOException e) 
    			{
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    			return content;
    		}
    		
    		@Override
    	    protected void onPostExecute(String content)
    	    {
    			Messager.toast(MainActivity.this, 
    					"Country preferences saved successfully to server");
    	    }

    	};
    	
    	requestTask.execute(Constants.SERVER_URL + "countries");
	}

	private void removeCountriesFromServer() 
	{
		// TODO: something sensible if there's no network connection! Leave it for now, this is
		// only a prototype at the moment
		
		// We're just sending the regid
    	AsyncTask<String, Integer, String> requestTask = 
    			new AsyncTask<String, Integer, String>()
    	{

    		@Override
    		protected String doInBackground(String... params) 
    		{
    	        ServerComms comms = new ServerComms();
    			String content = "";
    			try 
    			{
    				Map<String, String> postParams = new HashMap<String, String>();
    				postParams.put("regid", MainActivity.this.regid);
    				content = comms.postRequest(params[0], postParams);
    			}
    			catch (IOException e) 
    			{
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    			return content;
    		}
    		
    		@Override
    	    protected void onPostExecute(String content)
    	    {
    			Messager.toast(MainActivity.this, 
    					"Country preferences removed successfully from server");
    	    }

    	};
    	
    	requestTask.execute(Constants.SERVER_URL + "removeCountries");
	}

	@Override
	public void regidChanged(String regid) 
	{
		Log.i(TAG, "Received new regid: " + regid);
		this.regid = regid;
	}

}
